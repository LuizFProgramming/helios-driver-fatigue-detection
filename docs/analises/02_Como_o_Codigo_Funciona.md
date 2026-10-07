# 02. Como o código funciona, peça por peça

## Parte A. Protótipo desktop (`03_Codigo/prototipo_desktop_python`)

### A.1 Caminho de um frame

```
Webcam (cv2.VideoCapture)
   │ frame BGR
   ▼
DetectorFacialMediaPipe.detectar()        adapters/mediapipe_face_detector.py
   │ converte BGR→RGB, roda o modelo face_landmarker.task (478 pontos + 52 blendshapes)
   │ separa: olho direito (6 pts), olho esquerdo (6 pts), boca (6 pts),
   │         íris (2×5 pts), todos os 478 pts, blendshapes de piscada
   ▼ AmostraFacial  (ou None se não achou rosto)
MonitorDeSonolencia.processar()           domain/drowsiness_monitor.py
   │ EAR médio dos 2 olhos → suaviza (EMA)
   │ MAR da boca → suaviza (EMA)
   │ ECF (média eyeBlinkLeft/Right) → suaviza (EMA)
   │ olho_fechado = EAR < 0.19  OU  ECF > 0.40
   │ atualiza PERCLOS, contador de frames fechados, piscadas, bocejos
   ▼ LeituraMonitoramento (estado, ear, mar, ecf, perclos, piscadas, bocejos...)
renderizar_monitor()  e  renderizar_mapeamento()   adapters/opencv_hud.py
   ▼
2 janelas na tela. Tecla Q encerra e imprime o resumo da sessão.
```

### A.2 Arquivo por arquivo

| Arquivo | Papel na arquitetura hexagonal | Resumo |
|---------|-------------------------------|--------|
| `config.py` | Configuração | Todos os limiares, índices de landmarks e cores. É o único lugar para ajustar sensibilidade |
| `domain/types.py` | Domínio | Tipos imutáveis: `LandmarksOlho`, `LandmarksBoca`, `BlendShapeOlhos`, `AmostraFacial`, `EstadoMotorista` (ATENTO, PISCANDO, SONOLENTO, SEM FACE), `LeituraMonitoramento` |
| `domain/ear_calculator.py` | Domínio | Fórmula de razão de aspecto de 6 pontos, usada tanto para EAR quanto para MAR |
| `domain/drowsiness_monitor.py` | Domínio | Máquina de estados. Recebe uma amostra por frame e devolve a leitura |
| `ports/face_detector_port.py` | Porta | Interface abstrata `PortaDetectorFacial` com `detectar()` e `liberar()` |
| `adapters/mediapipe_face_detector.py` | Adaptador de entrada | Implementa a porta usando MediaPipe Tasks em modo VIDEO |
| `adapters/opencv_hud.py` | Adaptador de saída | Desenha as janelas Monitor e Mapeamento |
| `helios_main.py` | Composição | Monta as peças e roda o laço da câmera |
| `face_landmarker.task` | Modelo | Rede do MediaPipe, float16, ~3,7 MB |

A grande vantagem dessa organização para o app mobile: **o domínio (`types`, `ear_calculator`, `drowsiness_monitor`) não depende de câmera nem de tela**. Ele pode ser traduzido quase linha a linha para TypeScript ou Kotlin e testado isoladamente.

### A.3 Os indicadores

**EAR (Eye Aspect Ratio)**, de Soukupová e Čech (2016). Seis pontos por olho:

```
      p2  p3
 p1 ·        · p4          EAR = (|p2-p6| + |p3-p5|) / (2·|p1-p4|)
      p6  p5
```

Olho aberto fica perto de 0,30 a 0,42. Fechado cai para menos de 0,10. O limiar usado é 0,19. Índices MediaPipe: olho direito da imagem `[33, 160, 158, 133, 153, 144]`, esquerdo `[362, 385, 387, 263, 373, 380]`.

**MAR (Mouth Aspect Ratio)**, mesma fórmula aplicada à boca. O código usa o **lábio interno** `[78, 81, 13, 308, 312, 14]`, porque o conjunto externo incluía o ponto 0 (acima do lábio) e dava MAR alto em rosto com barba. Bocejo: MAR > 0,50 por 15 frames seguidos (~0,5 s a 30 fps).

**ECF (Eye Closure Factor)**, nome dado no projeto ao blendshape `eyeBlinkLeft/Right` que a própria rede do MediaPipe calcula (0 = aberto, 1 = fechado). Serve de segunda opinião quando o EAR falha, por exemplo com óculos.

**PERCLOS**, percentual de frames com olho fechado numa janela deslizante de 150 frames (~5 s a 30 fps). Acima de 40% gera alerta.

**EMA (média móvel exponencial)**, `novo = 0,25·atual + 0,75·anterior`. Tira o tremor da medição, mas atrasa a resposta (ver problema P3 no arquivo 03).

### A.4 Regras da máquina de estados

| Situação | Estado |
|----------|--------|
| Nenhum rosto | SEM FACE |
| Olho fechado por 30+ frames seguidos (~1 s) **ou** PERCLOS > 40% | SONOLENTO (banner vermelho com contador de segundos) |
| Olho fechado por 1 a 29 frames | PISCANDO |
| Caso contrário | ATENTO |
| Ao reabrir após 2 a 29 frames fechado | conta +1 piscada |
| MAR acima do limiar por 15+ frames | bocejando; ao fechar a boca conta +1 bocejo |

Repare que hoje **não existe Machine Learning**: tudo é regra com limiar fixo. Isso corresponde às etapas 1 a 8 e 12 a 13 do roteiro. As etapas de dataset, treino e avaliação (9 a 11) ainda não começaram.

### A.5 Origem dos números

O limiar 0,19 e as métricas que aparecem no README (acurácia 95,47%, recall 90%, AUC 93,15%) **vêm do trabalho de Seminario Medina (2025)**, medidas no Driver Inattention Detection Dataset. Não são medições do HELIOS. No texto do TCC isso precisa aparecer como referência, e o HELIOS precisa da própria validação (etapa 11).

## Parte B. Backend e simulador (`03_Codigo/backend_fastapi_esp32`)

### B.1 `backend/main.py` (FastAPI)

| Rota | O que faz |
|------|-----------|
| `GET /` | Health check: `{"sistema": "HELIOS", "status": "online"}` |
| `POST /frames` | Recebe `file` (imagem), `frame_id` e `device_id` via multipart. Salva em `uploads/{device_id}_frame_{frame_id}.jpg` e guarda os metadados numa lista em memória |
| `GET /frames` | Lista os metadados recebidos |
| `GET /frames/{frame_id}` | Devolve a imagem do frame |

Não há análise de rosto aqui. A API só armazena.

### B.2 `esp32cam/simulador.py`

Lê todos os `.jpg` da pasta `imagens/` (hoje vazia), e envia um a cada 2 segundos para `http://127.0.0.1:8000/frames` com `device_id = ESP32CAM_01`. Serve para testar a API sem a placa física.

### B.3 Como rodar

```bash
cd 03_Codigo/backend_fastapi_esp32
python -m venv .venv && .venv\Scripts\activate
pip install fastapi uvicorn python-multipart requests
cd backend && uvicorn main:app --reload
# outro terminal, com fotos .jpg em esp32cam/imagens
cd esp32cam && python simulador.py
```

Observação: o backend cria `uploads/` relativo à pasta de onde o uvicorn é executado, por isso existem três pastas `uploads` vazias espalhadas no projeto original.

## Parte C. Protótipo desktop: como rodar

```bash
cd 03_Codigo/prototipo_desktop_python
pip uninstall mediapipe opencv-python opencv-contrib-python -y
pip install mediapipe opencv-python numpy
python helios_main.py
```

O modelo `face_landmarker.task` já está na pasta.
