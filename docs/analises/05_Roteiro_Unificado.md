# 05. Roteiro unificado (PDF + imagem + código existente + app)

Legenda: ✅ pronto no protótipo Python · 🟡 parcial · ⬜ não começou

## Fase 1. Visão computacional (já existe em Python)

| # | Etapa | Imagem | PDF | Status | Onde está / observação |
|---|-------|--------|-----|--------|------------------------|
| 1 | Capturar vídeo | 1 | 1 | ✅ | `helios_main.py` (`cv2.VideoCapture`) |
| 2 | Detectar o rosto | 2 | 2 | ✅ | O Face Landmarker já detecta o rosto internamente; não precisa do Face Detector separado |
| 3 | Landmarks dos olhos | 3 | 3 | ✅ | 478 pontos, índices em `config.py` |
| 4 | Calcular EAR | 4 | 4 | ✅ | `ear_calculator.py` |
| 5 | Piscadas e olhos fechados | 5 | 5 e 6 | 🟡 | Funciona, mas a EMA reduz a contagem (problema P3) e depende de 30 fps (P2) |
| 6 | MAR (bocejo) | 6 | 7 | ✅ | Lábio interno; corrigir README (P1) |
| 7 | Inclinação da cabeça | 6 | 8 | ⬜ | Não implementado. Caminho mais simples: `outputFacialTransformationMatrixes: true` no MediaPipe devolve a matriz de pose; dela saem pitch (cabeça caindo), yaw e roll. Alternativa: `solvePnP` do OpenCV com 6 pontos, como previsto no SafeDrive |
| 8 | Alerta visual | 12 | 13 | 🟡 | Banner na tela. Falta alerta **sonoro** |

## Fase 2. Dados e Machine Learning

| # | Etapa | Imagem | PDF | Status | Como fazer |
|---|-------|--------|-----|--------|-----------|
| 9 | Registrar em CSV | 7 | 9 | ⬜ | Adaptador novo de saída que grava por frame: `timestamp, ear, mar, ecf, pitch, yaw, roll, olho_fechado` |
| 10 | Montar dataset | 7 | 9 | ⬜ | Agregar em janelas (ex.: 30 s): PERCLOS, piscadas/min, duração média da piscada, bocejos/min, variação do pitch. Uma linha por janela |
| 11 | Definir classes | 8 | 9 | ⬜ | `normal`, `sonolento`, `fadiga`. **De onde vem o rótulo é a parte mais difícil.** Opções: (a) dataset público com rótulos, como o UTA-RLDD, que tem vídeos em três níveis de alerta; (b) gravações próprias com autoavaliação pela escala KSS (Karolinska) a cada poucos minutos |
| 12 | Treinar ML | 9 | 10 | ⬜ | scikit-learn: comparar Random Forest, SVM, KNN, XGBoost. Separar por **pessoa** (treino e teste com pessoas diferentes), senão a acurácia fica inflada |
| 13 | Avaliar | 10 | 11 | ⬜ | Acurácia, precisão, sensibilidade (recall), especificidade, taxa de falsos positivos, tempo de resposta, matriz de confusão |
| 14 | Exportar modelo para o app | n/a | n/a | ⬜ | Para árvore/Random Forest: exportar as árvores em JSON e avaliar em TS puro no domínio (testável, sem runtime pesado). Para rede neural: TFLite ou ONNX |

Importante: as **regras** (EAR < limiar por 1 s) continuam como alerta imediato de microssono. O **ML** classifica o estado geral em janelas maiores (fadiga se acumulando). Um não substitui o outro, e essa divisão é um bom argumento no texto.

## Fase 3. App mobile / central do carro

| # | Etapa | Status | Detalhe |
|---|-------|--------|---------|
| 15 | Portar o domínio para TypeScript | ⬜ | `types`, `ear_calculator`, `drowsiness_monitor` + correções P1 a P13. Testes primeiro |
| 16 | Adaptador MediaPipe Web | ⬜ | `@mediapipe/tasks-vision`, modo VIDEO, blendshapes ligados |
| 17 | Adaptador câmera local | ⬜ | `getUserMedia`. Permite testar no celular sem a ESP32 |
| 18 | Firmware ESP32-CAM | ⬜ | `/stream` MJPEG QVGA, cabeçalho CORS, `/status`, `/buzzer`, modo STA/SoftAP |
| 19 | Adaptador ESP32-MJPEG | ⬜ | Lê o stream e entrega frames ao detector |
| 20 | Alertas | ⬜ | Som alto + vibração + tela; buzzer na câmera opcional |
| 21 | Empacotar Android (Capacitor) | ⬜ | Tela sempre ligada, orientação paisagem para a central |
| 22 | Testar em celular e central | ⬜ | Medir fps e latência real em cada aparelho |
| 23 | Integração final | ⬜ | Protótipo HELIOS completo |

## Fase 4. Validação para o TCC

| # | Etapa | Status |
|---|-------|--------|
| 24 | Protocolo de testes (participantes, condições de luz, com/sem óculos) | ⬜ |
| 25 | Medir as métricas do item 13 **no próprio HELIOS** | ⬜ |
| 26 | Comparar regras vs ML vs regras + ML | ⬜ |

## Ordem sugerida

Seguindo a regra do PDF ("cada etapa deve funcionar e ser testada antes da seguinte"):

1. Corrigir P1, P2, P3 no Python e gravar alguns vídeos de teste (servem para o resto do projeto inteiro).
2. Etapa 7 (cabeça) e 9 (CSV) no Python, que é onde já está tudo funcionando.
3. Em paralelo: começar o domínio TS com testes (etapa 15), porque não depende de hardware.
4. App com câmera local (16, 17, 20, 21) antes de mexer na ESP32.
5. ESP32 (18, 19).
6. Dataset e ML (10 a 14) quando houver dados.
7. Validação (24 a 26).
