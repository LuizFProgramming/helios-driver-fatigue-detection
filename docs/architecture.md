# Arquitetura do sistema

Estado em 08/10/2026. O repositório tem duas implementações do mesmo pipeline: o **protótipo Python** (o que a equipe roda e testa hoje) e o **app Android em Kotlin** (em desenvolvimento). A visão de produto, com a ESP32-CAM como câmera do carro, está em [`analises/04_Arquitetura_App_Carro.md`](analises/04_Arquitetura_App_Carro.md).

## Pipeline

```
Captura  →  Extração de características  →  Análise  →  Classificação  →  Alerta
```

## Protótipo Python (`prototype/`)

```
 webcam / DroidCam
        │
 LeitorDeCamera (thread: guarda só o frame mais novo)
        │
 Analisador (thread, até 30 FPS)
   ├─ DetectorFacialMediaPipe ── 478 pontos, blend shapes, matriz de pose
   ├─ MonitorDeSonolencia ────── EAR, ECF, MAR, PERCLOS → estado (ATENTO, PISCANDO, SONOLENTO, SEM_FACE)
   ├─ ClassificadorDeAlerta ──── estado + causa + tempo sem rosto → categoria
   │                              (ATENTO, PISCANDO, SEM_ROSTO, DESATENTO, SONOLENCIA, DORMINDO)
   ├─ ControladorDeAlerta ────── categoria → som (alto-falante do PC)
   └─ POST /frames a cada 2 s e ao entrar num alerta ──► API FastAPI ──► painel web e app Expo
        │
 Janela da câmera (hud_veicular.py): desenha só na tela; a foto enviada é o frame cru
```

O núcleo segue arquitetura hexagonal em `prototype/prototipo_desktop_python/`:

| Pasta | Papel | Exemplos |
| --- | --- | --- |
| `domain/` | Regras puras, sem OpenCV nem MediaPipe | `drowsiness_monitor.py`, `ear_calculator.py`, `head_pose.py`, `alert_pattern.py`, `alert_controller.py` |
| `ports/` | Contratos que o domínio exige | `face_detector_port.py`, `alert_output_port.py` |
| `adapters/` | Implementações que falam com o mundo | `mediapipe_face_detector.py`, `hud_veicular.py`, `audio_alert.py` |
| `config.py` | Todos os limiares, cores e sons | `EAR_LIMIAR_OLHO_FECHADO`, `PADROES_SONOROS` |

### Alerta sonoro

| Etapa | Onde |
| --- | --- |
| Categoria de cada frame | `ClassificadorDeAlerta` em `esp32cam/webcam_api.py` |
| Categoria → padrão (frequência, bipe, silêncio, volume) | `padrao_para_categoria()` em `domain/alert_pattern.py`, com os valores de `PADROES_SONOROS` |
| Quando começa, troca e para (espera de 1 s, mudo) | `ControladorDeAlerta` em `domain/alert_controller.py` |
| Contrato da saída | `PortaSaidaDeAlerta` em `ports/alert_output_port.py` |
| Som no alto-falante do Windows | `AlertaSonoroWindows` em `adapters/audio_alert.py` |

## App Android (`Aplicativo-Android/`)

Módulos Gradle: `dominio` (regras puras em Kotlin, sem Android), `protocolos` (padrão sonoro, modelo de tela, mapeamento do Face Mesh) e `app` (câmera, MediaPipe, ESP32, acelerômetro, alarme e telas). Cada etapa do pipeline é uma porta com adaptador trocável. Detalhes e estado em [`Aplicativo-Android/README.md`](../Aplicativo-Android/README.md).

## Componentes ainda não implementados

| Componente | Situação |
| --- | --- |
| Firmware da ESP32-CAM (`/stream`, `/buzzer`) | A fazer (`firmware/`) |
| Sensores inerciais no protótipo Python | Não há; o app Android lê o acelerômetro do aparelho |
| Classificador treinado e IA generativa | Não iniciados |
| Alerta sonoro fora do Windows, no painel web e no app Expo | Não há |

## Limitações conhecidas

Variação de iluminação, ângulo da câmera, óculos e barba, e diferenças entre dispositivos (Rocha e Domingues, 2022). As contagens em frames pressupõem 30 fps (ver [`analises/03_Problemas_Encontrados.md`](analises/03_Problemas_Encontrados.md)).
