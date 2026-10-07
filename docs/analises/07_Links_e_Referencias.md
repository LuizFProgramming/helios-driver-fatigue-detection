# 07. Links e referências

## Links enviados

| Link | O que é | Uso no HELIOS |
|------|---------|---------------|
| [MediaPipe Face Detector](https://developers.google.com/edge/mediapipe/solutions/vision/face_detector) | Modelo BlazeFace (short range, entrada 128×128). Devolve caixa do rosto e 6 pontos (olhos, nariz, boca, orelhas). ~3 ms em CPU no Pixel 6. Android, iOS, Web e Python | **Não é necessário.** O Face Landmarker já detecta o rosto e dá os 478 pontos. Os 6 pontos do Face Detector não permitem calcular EAR. Pode ser citado no TCC como alternativa avaliada e descartada |
| [MediaPipe Face Landmarker Web/JS](https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker/web_js) | Pacote npm `@mediapipe/tasks-vision`. Modos IMAGE e VIDEO; opções `numFaces`, `outputFaceBlendshapes` (52 valores), `outputFacialTransformationMatrixes` (pose da cabeça); `detectForVideo(video, timestamp)` | **Base do app.** Mesmo modelo `face_landmarker.task` do protótipo Python |
| [OmniRoute](https://github.com/diegosouzapw/OmniRoute) | Gateway de IA em TypeScript/Node (licença MIT) que roteia pedidos para centenas de provedores de LLM, com fallback e compressão de tokens | **Sem relação com a detecção de fadiga.** Serve, no máximo, como ferramenta de desenvolvimento (usar vários LLMs para programar). Não recomendo citar como parte da arquitetura |

## Trechos de código úteis (Face Landmarker Web)

```ts
import { FaceLandmarker, FilesetResolver } from "@mediapipe/tasks-vision";

const vision = await FilesetResolver.forVisionTasks(
  "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm"
);
const landmarker = await FaceLandmarker.createFromOptions(vision, {
  baseOptions: { modelAssetPath: "models/face_landmarker.task", delegate: "GPU" },
  runningMode: "VIDEO",
  numFaces: 1,
  outputFaceBlendshapes: true,              // ECF (eyeBlinkLeft/Right, jawOpen)
  outputFacialTransformationMatrixes: true, // pose da cabeça
});

const resultado = landmarker.detectForVideo(videoOuCanvas, performance.now());
// resultado.faceLandmarks[0]          → 478 pontos {x, y, z} normalizados
// resultado.faceBlendshapes[0]        → categorias com score 0..1
// resultado.facialTransformationMatrixes[0] → matriz 4x4
```

No app final o arquivo `.task` e os arquivos WASM devem ficar **dentro do APK**, não na CDN, porque o carro pode estar sem internet.

## Documentação de plataforma consultada

- [Android Automotive OS: apps para carro estacionado](https://developer.android.com/training/cars/parked/automotive-os). Apps de terceiros nessa categoria são bloqueados com o carro em movimento e os carros geralmente não dão acesso às câmeras.
- [Android for Cars App Library](https://developer.android.com/training/cars/apps). Apps de Android Auto usam modelos de tela prontos.
- [ESP32 Camera Streaming: MJPEG](https://zbotic.in/esp32-camera-streaming-mjpeg-video-to-browser-tutorial/). Tabela de fps por resolução na ESP32-CAM.

## Referências científicas já usadas no README do protótipo

1. SOUKUPOVÁ, T.; ČECH, J. Real-Time Eye Blink Detection using Facial Landmarks. 21st Computer Vision Winter Workshop, 2016.
2. SEMINARIO MEDINA, A. V. Sistema Autónomo de Detección de Somnolencia Basado en Relación de Aspecto Ocular (EAR). Universidad Tecnológica del Perú, 2025.
3. COSTA, L. C. Monitoramento Inteligente de Fadiga: Detecção de Sonolência em Motoristas com IA e Visão Computacional. TCC, UFPA, 2024.
4. PINHEIRO, J. L. S. Detecção de Sonolência ao Volante: Uma Solução Prática com Visão Computacional e Raspberry Pi. TCC, UNILAB, 2024.
5. GOOGLE. MediaPipe Face Landmarker. Documentação on-line.
6. DINGES, D. F. et al. PERCLOS: A Valid Psychophysiological Measure of Alertness as Assessed by Psychomotor Vigilance. FHWA, 1998.

Antes de entregar, conferir cada referência no original (páginas, ano, título exato) e formatar em ABNT.
