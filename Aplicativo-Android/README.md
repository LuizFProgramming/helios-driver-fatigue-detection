# HELIOS: aplicativo Android

Base do aplicativo de detecção de sonolência e fadiga ao volante. Roda inteiro no carro, sem internet: a câmera (do próprio aparelho ou uma ESP32-CAM) manda as imagens, o MediaPipe encontra os 478 pontos do rosto e o domínio em Kotlin decide o estado do motorista e dispara o alarme.

## Pipeline

```
Captura de imagem → Extração de características → Análise dos sinais → Classificação do estado → Alerta
 (CameraX / ESP32)   (MediaPipe: EAR, MAR, ECF,    (+ acelerômetro     (ATENTO / FADIGA /       (som, vibração,
  + realce OpenCV     pose da cabeça)               do aparelho)        CRÍTICO)                tela, buzzer)
```

No texto do TCC o primeiro estado aparece como "alerta". No código ele se chama `ATENTO` para não ser confundido com o alarme, que é a etapa seguinte.

| Etapa | Onde está |
|-------|-----------|
| Captura | `app/.../adaptadores/camera/FonteCameraX.kt`, `app/.../adaptadores/esp32/Esp32.kt` |
| Realce (OpenCV, opcional) | `app/.../adaptadores/opencv/RealceOpenCv.kt` (CLAHE no canal L) |
| Extração | `app/.../adaptadores/mediapipe/DetectorMediapipe.kt` + `protocolos/MapeadorFaceMesh.kt` + `dominio/geometria` |
| Análise | `dominio/sinais`, `dominio/eventos`, `dominio/inercial` |
| Classificação | `dominio/classificacao/Classificador.kt`, orquestrado por `dominio/monitor/MonitorMotorista.kt` |
| Alerta | `protocolos/Alarme.kt` + `app/.../adaptadores/alarme/AlarmeAndroid.kt`, `protocolos/ModeloDeTela.kt` + `app/.../ui/TelaDirecao.kt` |

## Por que Kotlin nativo e não TypeScript + Capacitor

O documento 08 previa TypeScript com MediaPipe Web dentro de um WebView. Esta base troca para Kotlin nativo (registro em `docs/ADR-006-kotlin-nativo.md`). Em resumo: o MediaPipe nativo usa a GPU do aparelho sem passar por WebView e WASM, o que pesa nas centrais multimídia mais fracas; o Android permite prender o tráfego do app na rede da câmera (`WifiNetworkSpecifier`), resolvendo o risco do spike 0.2; e o domínio continua puro e testável na JVM, como era a ideia do TypeScript.

## O que já está pronto

- Domínio portado do protótipo Python com as correções P1 a P13 do documento 03: tudo por tempo, piscada sobre o EAR bruto, PERCLOS ponderado por tempo, configuração injetada, início do crítico registrado qualquer que seja o gatilho, `olhoFechado` exposto para a tela.
- Novidades em relação ao protótipo: pose da cabeça pela matriz do MediaPipe, calibração de 5 s (RF12), piscadas longas, PERCLOS de 60 s para fadiga, acelerômetro (carro parado ou em movimento e eventos bruscos), três níveis de estado.
- 67 testes no domínio e nos protocolos, verificados com o compilador Kotlin 2.2.20.
- App Android com câmera do aparelho, leitura MJPEG da ESP32-CAM com reconexão, realce OpenCV opcional, alarme sonoro no canal de alarme, vibração, buzzer da câmera, tela de direção mínima e tela de ajustes que só abre com o carro parado.

## O que ainda não foi feito

- O módulo `app` não foi compilado aqui (o ambiente não tinha o SDK do Android). A primeira sincronização no Android Studio pode apontar ajustes pequenos de API.
- Histórico em banco local e exportação CSV (Fase 5), firmware próprio da ESP32 (Fase 4), classificador treinado (Fase 6). Prompts prontos em `docs/PROXIMOS_PASSOS.md`.
- Medição automática de Halstead e de duplicação.

## Como abrir

1. Copie `../03_Codigo/prototipo_desktop_python/face_landmarker.task` para `app/src/main/assets/` (já copiado nesta pasta).
2. Abra a pasta `04_App_HELIOS` no Android Studio (versão com suporte a AGP 9.1). Ele baixa o Gradle 9.3.1 e as dependências na primeira sincronização.
3. Rode no celular com a câmera frontal virada para o rosto. Para a ESP32-CAM, use o exemplo `CameraWebServer` em modo ponto de acesso com o nome `HELIOS-CAM` e escolha "ESP32-CAM pelo Wi-Fi" nos ajustes.

Testes rápidos: `./gradlew :dominio:test :protocolos:test`. Esteira completa: `./gradlew qualidade`.

## Skills para o Claude Code

A pasta `.claude/skills/` já traz as skills que este projeto usa, carregadas automaticamente quando o Claude Code é aberto aqui:

| Skill | Origem | Para quê |
|-------|--------|----------|
| `helios-desenvolvimento` | deste projeto | Regras de arquitetura, metas de qualidade e testes |
| `camerax` | android/skills | ImageAnalysis, rotação, testes com fakes |
| `edge-to-edge` | android/skills | Tela cheia na central e no celular |
| `testing-setup` | android/skills | Testes instrumentados e de UI |
| `android-permissions-security` | android/skills | Permissões de câmera, rede e sensores |
| `android-profiler` | android/skills | Medir fps, CPU e temperatura na central (RNF04) |
| `r8-analyzer` | android/skills | APK de release enxuto |
| `agp-9-upgrade` | android/skills | Build com AGP 9 e Kotlin embutido |
| `adaptive`, `styles`, `navigation-3` | android/skills | Telas para tamanhos diferentes, tema, navegação quando o histórico entrar |
| `android-cli` | android/skills | Instalar e atualizar as skills do Android pelo terminal |
| `frontend-design` | anthropics/skills | Direção visual da interface |

Para atualizar as do Android: `android skills update --all --project=.`
