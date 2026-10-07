# HELIOS — Monitor de Sonolencia ao Volante

**Universidade Cidade de Sao Paulo — Ciencia da Computacao — TCC 2026**

Sistema de deteccao preditiva de fadiga em tempo real via webcam.
Monitora o estado de atencao do motorista frame a frame, detectando
fechamento dos olhos, piscadas, sonolencia e bocejo atraves de metricas
geometricas extraidas por visao computacional.

---

## Indice

1. [Fundamento cientifico](#1-fundamento-cientifico)
2. [Arquitetura do sistema](#2-arquitetura-do-sistema)
3. [Configuracao do ambiente](#3-configuracao-do-ambiente)
4. [Como executar](#4-como-executar)
5. [Interface visual — duas janelas](#5-interface-visual--duas-janelas)
6. [Metricas monitoradas](#6-metricas-monitoradas)
7. [Ajustando os parametros](#7-ajustando-os-parametros)
8. [Referencias](#8-referencias)

---

## 1. Fundamento cientifico

### Eye Aspect Ratio (EAR)

O HELIOS usa a metrica **EAR** proposta por Soukupova & Cech (2016).
Para cada olho, seis landmarks periorbitais sao extraidos pelo MediaPipe
e usados na formula:

```
        ||p2 - p6|| + ||p3 - p5||
EAR  =  ─────────────────────────
               2 · ||p1 - p4||
```

Onde `p1..p6` sao os seis pontos em sentido horario a partir do canto lateral:

```
        p2  p3
   p1 ·      · p4
        p6  p5
```

**Comportamento do EAR:**

| Situacao                   | Valor EAR tipico                        |
|----------------------------|-----------------------------------------|
| Olho completamente aberto  | 0.30 a 0.42                             |
| Piscada normal             | queda rapida, recuperacao em 2-4 frames |
| Olho fechado (sono)        | 0.00 a 0.10                             |
| **Limiar de alerta**       | **< 0.19**                              |

O limiar `0.19` foi determinado pelo **Indice de Youden (J = 0.8631)**
sobre o Driver Inattention Detection Dataset (Seminario Medina, 2025):

| Metrica        | Valor  |
|----------------|--------|
| Acuracia       | 95.47% |
| Recall         | 90.00% |
| Especificidade | 96.31% |
| F1-Score       | 84.11% |
| AUC-ROC        | 93.15% |

### Mouth Aspect Ratio (MAR)

O **MAR** aplica a mesma formula geometrica do EAR, mas sobre seis
landmarks peribucais, medindo a abertura da boca para deteccao de bocejo:

```
        ||p2 - p6|| + ||p3 - p5||
MAR  =  ─────────────────────────
               2 · ||p1 - p4||
```

| Situacao          | Valor MAR tipico |
|-------------------|-----------------|
| Boca fechada      | 0.20 a 0.35     |
| Fala / respiracao | 0.35 a 0.50     |
| **Bocejo**        | **> 0.55**      |

Fonte: Costa, UFPA (2024) — indicadores EAR e MAR para deteccao de fadiga.

### PERCLOS

**PERCLOS** (Percentage of Eye Closure) e o padrao internacional para
avaliacao de fadiga ao volante (Dinges et al., 1998). Mede o percentual
do tempo com olhos fechados dentro de uma janela deslizante de 5 segundos.

```
             frames com EAR < 0.19
PERCLOS  =  ─────────────────────────  × 100%
               total de frames (150)
```

PERCLOS > 40% e associado a degradacao cognitiva critica.

---

## 2. Arquitetura do sistema

O HELIOS adota **Arquitetura Hexagonal** (Ports and Adapters).
O principio central: **o dominio do problema nao depende de nenhum
framework externo**. Trocar o MediaPipe por dlib, ou o OpenCV por
outra biblioteca de display, nao exige alterar nenhuma linha de logica
de deteccao.

```
┌─────────────────────────────────────────────────────────────┐
│                        HELIOS                               │
│                                                             │
│   ┌─────────────────── DOMINIO ───────────────────────┐    │
│   │  (zero dependencias externas)                     │    │
│   │   types.py          — LandmarksOlho, LandmarksBoca│    │
│   │                        AmostraFacial, Leitura...  │    │
│   │   ear_calculator.py — formulas EAR e MAR          │    │
│   │   drowsiness_monitor— maquina de estados          │    │
│   └───────────────────────────────────────────────────┘    │
│           ↑ depende de                  ↓ produz            │
│   ┌──── PORTA ─────────┐       ┌──── TIPOS DE SAIDA ──┐    │
│   │PortaDetectorFacial │       │LeituraMonitoramento  │    │
│   │(ABC)               │       │(dataclass)           │    │
│   └────────┬───────────┘       └──────────┬───────────┘    │
│            │ implementada por             │ consumida por   │
│   ┌──── ADAPTADORES ──────────────────────────────────┐    │
│   │                                                   │    │
│   │   mediapipe_face_detector.py → MediaPipe SDK      │    │
│   │   opencv_hud.py              → renderizar_monitor │    │
│   │                                 renderizar_mapeam.│    │
│   └───────────────────────────────────────────────────┘    │
│                           ↑                                 │
│                    helios_main.py                           │
│           (composicao, loop, duas janelas)                  │
└─────────────────────────────────────────────────────────────┘
```

### Estrutura de arquivos

```
Entrega definitiva/
│
├── config.py                          # Todos os parametros e constantes
│
├── domain/                            # Logica pura — zero dependencias externas
│   ├── types.py                       # LandmarksOlho, LandmarksBoca,
│   │                                  # AmostraFacial, EstadoMotorista,
│   │                                  # LeituraMonitoramento
│   ├── ear_calculator.py              # calcular_ear() e calcular_mar()
│   └── drowsiness_monitor.py         # EAR + MAR + PERCLOS + maquina de estados
│
├── ports/
│   └── face_detector_port.py         # PortaDetectorFacial (ABC)
│
├── adapters/
│   ├── mediapipe_face_detector.py    # Extrai olhos, boca e 478 landmarks
│   └── opencv_hud.py                 # renderizar_monitor() e
│                                      # renderizar_mapeamento()
│
├── helios_main.py                     # Ponto de entrada — abre 2 janelas
├── requirements.txt
├── face_landmarker.task               # Modelo MediaPipe (baixar — ver 3.3)
└── README.md
```

### Fluxo de execucao por frame

```
Webcam (OpenCV)
      │
      ▼ frame BGR
DetectorFacialMediaPipe.detectar()
      │  converte para RGB → roda modelo → extrai:
      │    olho_direito (6 pts)
      │    olho_esquerdo (6 pts)
      │    boca (6 pts para MAR)
      │    landmarks_todos (478 pts com coordenada z)
      ▼ AmostraFacial
MonitorDeSonolencia.processar()
      │  EAR medio → PERCLOS → maquina de estados (olhos)
      │  MAR → deteccao de bocejo
      ▼ LeituraMonitoramento (estado, ear, mar, perclos,
      │                        piscadas, bocejos, etc.)
      ├──▶ renderizar_monitor()    → janela "HELIOS — Monitor"
      └──▶ renderizar_mapeamento() → janela "HELIOS — Mapeamento"
```

---

## 3. Configuracao do ambiente

### 3.1. Requisitos

- Python 3.10 ou 3.13
- Webcam USB, embutida ou smartphone via DroidCam
- Windows 10/11, Linux ou macOS

### 3.2. Instalar dependencias

Execute os comandos abaixo no terminal, **nessa ordem**:

```bash
pip uninstall mediapipe opencv-python opencv-contrib-python -y
pip install mediapipe opencv-python numpy
```

> **Por que desinstalar primeiro?**
> O MediaPipe instala `opencv-contrib-python` como dependencia automatica.
> Ter os dois pacotes OpenCV simultaneamente causa `ImportError` em runtime.
> *(Fonte: HELIOS_Secao8_ConfigAmb.docx v3.0, Sec. 8.3)*

### 3.3. Baixar o modelo MediaPipe

O arquivo `face_landmarker.task` (float16, 478 landmarks, ~4 MB) deve
estar **na mesma pasta que `helios_main.py`**.

**PowerShell (Windows):**
```powershell
Invoke-WebRequest `
  -Uri "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task" `
  -OutFile "face_landmarker.task"
```

**Terminal (Linux / macOS):**
```bash
curl -L -o face_landmarker.task \
  "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task"
```

---

## 4. Como executar

### Iniciar o sistema

```bash
cd "Entrega definitiva"
python helios_main.py
```

Duas janelas abrem simultaneamente:
- **HELIOS — Monitor** — interface limpa para uso operacional
- **HELIOS — Mapeamento** — vista tecnica com mapa de landmarks

Pressione **Q** em qualquer janela para encerrar.

Ao fechar, o terminal exibe um resumo da sessao:

```
Sessao encerrada.
  Piscadas detectadas : 14
  Bocejos detectados  : 2
  PERCLOS final       : 4.7%
```

### Verificar se o ambiente esta correto

```bash
python -c "import mediapipe, cv2, numpy; print('OK')"
```

### Usando smartphone como webcam

Instale o app **DroidCam** no celular e o cliente no Windows.
Conecte ao mesmo Wi-Fi e anote o IP exibido pelo app.
Se a webcam nao for encontrada no indice `0`, altere em `helios_main.py`:

```python
INDICE_DA_WEBCAM = 1   # tente 1, 2 ou 3
```

Para descobrir o indice sem tentativa e erro:

```bash
python -c "
import cv2
for i in range(5):
    c = cv2.VideoCapture(i)
    if c.isOpened(): print(f'Camera no indice {i}')
    c.release()
"
```

### Solucao de problemas

| Erro | Causa provavel | Solucao |
|------|---------------|---------|
| `ModuleNotFoundError: mediapipe` | Pacote nao instalado | `pip install mediapipe` |
| `ImportError: cv2` | Conflito opencv | Desinstalar ambos e reinstalar (ver 3.2) |
| `[ERRO] Nao foi possivel acessar a webcam` | Camera ocupada ou indice errado | Fechar outros programas; ajustar `INDICE_DA_WEBCAM` |
| `FileNotFoundError: face_landmarker.task` | Modelo nao baixado | Executar o comando da secao 3.3 |
| Janela abre mas nao detecta rosto | Iluminacao insuficiente | Garantir iluminacao frontal |
| "BOCEJANDO" ativando sem bocejo | MAR muito sensivel | Aumentar `MAR_LIMIAR_BOCEJO` para `0.65` em `config.py` |

---

## 5. Interface visual — duas janelas

### Janela Monitor (limpa)

Interface para uso operacional. Mostra apenas o essencial.

```
┌────────────────────────────────────────────┬────────────────┐
│                                            │ EAR:    0.312  │
│  ATENTO                                    │ MAR:    0.241  │
│                                            │ PERCLOS: 3.2%  │
│   [feed da webcam com contornos            │ Piscadas: 7    │
│    leves sobre olhos e boca]               │ Bocejos:  1    │
│                                            │                │
│                                            │                │
│  ████████████████░░░░░░░  EAR              │                │
│                 ↑ limiar 0.19              │                │
└────────────────────────────────────────────┴────────────────┘
```

| Elemento | Posicao | Descricao |
|----------|---------|-----------|
| Status | Canto sup. esq. | ATENTO / PISCANDO / SEM FACE |
| Banner vermelho | Topo inteiro | SONOLENCIA DETECTADA + contador de segundos |
| Banner azul (rodape) | Rodape esq. | BOCEJANDO — ativo durante bocejo confirmado |
| Contorno dos olhos | Sobre o rosto | Ciano = aberto, Vermelho = fechado |
| Contorno da boca | Sobre o rosto | Verde = normal, Amarelo = bocejando |
| Painel lateral dir. | Coluna direita | EAR, MAR, PERCLOS, piscadas, bocejos |
| Barra de EAR | Rodape | Preenchimento proporcional; linha vermelha = limiar |

**Estados:**

| Estado | Cor | Condicao de ativacao |
|--------|-----|----------------------|
| `ATENTO` | Verde | EAR acima do limiar |
| `PISCANDO` | Laranja | EAR < limiar por 2 a 29 frames |
| `SEM FACE` | Cinza | Nenhum rosto detectado |
| `SONOLENCIA DETECTADA` | Banner vermelho | EAR < limiar por 30+ frames, ou PERCLOS > 40% |
| `BOCEJANDO` | Banner azul (rodape) | MAR > 0.55 por 5+ frames consecutivos |

---

### Janela Mapeamento (tecnica)

Vista tecnica com todos os 478 landmarks e linhas de medicao.

```
┌──────────────────────────────────────────────────────────────┐
│  HELIOS  MAPEAMENTO FACIAL                                    │
│                                                               │
│   [imagem levemente escurecida com:]                         │
│                                                               │
│   · · · · · (478 pontos cinza, brilho baseado na prof. z)   │
│                                                               │
│   ─── contorno do rosto (violeta)                            │
│   ─── sobrancelhas (amarelo)           ─── nariz (laranja)   │
│   ─── regiao dos olhos (azul)          ─── boca (verde)      │
│                                                               │
│   ●─────────────────────● EAR: p1-p4 (linha azul)           │
│   |  ●               ●  |     p2-p6, p3-p5 (linhas verdes)  │
│   |                     |                                    │
│   ●─────────────────────● MAR: mesma estrutura              │
│                                                               │
├──────────────────────────────────────────────────────────────┤
│ EAR: 0.312  MAR: 0.241  PERCLOS: 3.2%                       │
│ Estado: ATENTO   Piscadas: 7   Bocejos: 1                    │
└──────────────────────────────────────────────────────────────┘
```

| Elemento | Cor | Descricao |
|----------|-----|-----------|
| Todos os landmarks | Cinza (brilho por z) | Efeito 3D: pontos mais proximos sao mais brilhantes |
| Contorno do rosto | Violeta | Oval facial conectada |
| Sobrancelhas | Amarelo | Arco das sobrancelhas |
| Regiao dos olhos | Azul claro | Todos os ~16 landmarks por olho |
| Nariz | Laranja | Ponte e base do nariz |
| Boca | Verde | Labio externo e interno conectados |
| Pontos EAR (6/olho) | Ciano brilhante | Os 6 landmarks usados na formula |
| Pontos MAR (6) | Amarelo brilhante | Os 6 landmarks usados na formula |
| Linha horizontal EAR/MAR | Azul | Distancia p1-p4 (denominador da formula) |
| Linhas verticais EAR/MAR | Verde | Distancias p2-p6 e p3-p5 (numerador) |
| Painel inferior | Fundo escuro | Todas as metricas numericas |

---

## 6. Metricas monitoradas

### EAR — Eye Aspect Ratio

Media dos dois olhos. Quanto menor, mais fechados os olhos.

- Valor estavel acima de 0.25: motorista alerta
- Quedas rapidas e recuperacao: piscadas normais
- Abaixo de 0.19 por mais de 1 segundo: sonolencia

### MAR — Mouth Aspect Ratio

Abertura relativa da boca. Detecta bocejo como indicador complementar de fadiga.

- Abaixo de 0.55: boca em posicao normal
- Acima de 0.55 por 5+ frames: bocejo confirmado
- Bocejos frequentes indicam fadiga mesmo com olhos aparentemente abertos

### PERCLOS — Percentage of Eye Closure

Percentual dos ultimos ~5 segundos com olhos fechados (150 frames).

- 0–10%: normal (piscadas fisiologicas)
- 10–40%: atencao redobrada
- Acima de 40%: risco elevado — alerta ativado

### Piscadas

Ciclos completos de EAR < limiar (2 a 29 frames) com recuperacao.
Normal: 15-20 por minuto. Fadiga aumenta para 30+/min.

### Bocejos

Eventos com MAR > limiar por 5+ frames consecutivos. Normal: menos de
2 por minuto. Indicativo de fadiga mesmo sem sonolencia nos olhos.

---

## 7. Ajustando os parametros

Todos os parametros ficam em **`config.py`**.

### Tabela completa

| Constante | Padrao | O que controla |
|-----------|--------|----------------|
| `EAR_LIMIAR_OLHO_FECHADO` | `0.19` | Fronteira olho aberto/fechado |
| `FRAMES_CONSECUTIVOS_PARA_ALERTA_SONOLENCIA` | `30` | Frames fechados para alerta (~1s) |
| `FRAMES_MINIMOS_PARA_CONTAR_PISCADA` | `2` | Fechamento minimo para contar piscada |
| `PERCLOS_TAMANHO_DA_JANELA_EM_FRAMES` | `150` | Janela do PERCLOS (~5s a 30fps) |
| `PERCLOS_LIMIAR_RISCO_ELEVADO` | `0.40` | PERCLOS maximo sem alerta |
| `MAR_LIMIAR_BOCEJO` | `0.55` | Abertura minima para confirmar bocejo |
| `FRAMES_MINIMOS_PARA_CONFIRMAR_BOCEJO` | `5` | Frames abertos para confirmar bocejo |
| `NUMERO_MAXIMO_DE_ROSTOS` | `1` | Rostos rastreados simultaneamente |

### Perfil sensivel (motoristas de longa distancia)

```python
# config.py
EAR_LIMIAR_OLHO_FECHADO                    = 0.21
FRAMES_CONSECUTIVOS_PARA_ALERTA_SONOLENCIA = 20     # ~0.67s
PERCLOS_LIMIAR_RISCO_ELEVADO               = 0.30
MAR_LIMIAR_BOCEJO                          = 0.50
```

### Perfil tolerante (olhos naturalmente menores, ou com oculos)

```python
# config.py
EAR_LIMIAR_OLHO_FECHADO                    = 0.17
FRAMES_CONSECUTIVOS_PARA_ALERTA_SONOLENCIA = 45     # ~1.5s
PERCLOS_LIMIAR_RISCO_ELEVADO               = 0.50
MAR_LIMIAR_BOCEJO                          = 0.65
```

---

## 8. Referencias

1. SOUKUPOVA, T.; CECH, J. **Real-Time Eye Blink Detection using Facial Landmarks**. *21st Computer Vision Winter Workshop (CVWW)*, Rimske Toplice, Eslovenia, 2016.

2. SEMINARIO MEDINA, A. V. **Sistema Autonomo de Deteccion de Somnolencia Basado en Relacion de Aspecto Ocular (EAR)**. Universidad Tecnologica del Peru, Piura, 2025.

3. COSTA, L. C. **Monitoramento Inteligente de Fadiga: Deteccao de Sonolencia em Motoristas com IA e Visao Computacional**. TCC — Universidade Federal do Para, Tucurui, 2024.

4. PINHEIRO, J. L. S. **Deteccao de Sonolencia ao Volante: Uma Solucao Pratica com Visao Computacional e Raspberry Pi**. TCC — UNILAB, Redencao-CE, 2024.

5. GOOGLE. **MediaPipe Face Landmarker**. Disponivel em: https://ai.google.dev/edge/mediapipe/solutions/vision/face_landmarker. Acesso em: maio 2026.

6. DINGES, D. F. et al. **Perclos: A Valid Psychophysiological Measure of Alertness as Assessed by Psychomotor Vigilance**. Federal Highway Administration, Washington, 1998.
