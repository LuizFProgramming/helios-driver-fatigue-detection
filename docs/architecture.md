# Arquitetura do sistema

> 🚧 Rascunho. Atualizar conforme a arquitetura da Release 3 (R3S3) for consolidada.

## Visão geral

```
[Câmera] ─► capture ─► vision ─┐
                                ├─► fusion ─► alerts
[IMU]    ─► sensors ───────────┘
                         genai (apoio: ampliação de dados e simulação de cenários)
```

## Módulos

| Módulo | Responsabilidade | Entrada | Saída |
| --- | --- | --- | --- |
| `capture` | Obter frames da câmera | Webcam / ESP32-CAM | Frames |
| `vision` | Detectar face, landmarks, EAR, MAR, PERCLOS | Frames | Características faciais |
| `sensors` | Ler e filtrar dados inerciais | Dados do IMU | Inclinação/movimento da cabeça |
| `fusion` | Combinar sinais e classificar o estado | Características + sensores | Estado (alerta / fadiga / crítico) |
| `alerts` | Emitir alerta | Estado | Alerta visual/sonoro |
| `genai` | Gerar dados sintéticos e cenários adversos | Imagens/vídeos | Dataset ampliado |

## Requisitos mínimos do protótipo

- [ ] Captura de vídeo em tempo real
- [ ] Cálculo de EAR e PERCLOS
- [ ] Alerta ao detectar fechamento ocular prolongado
- [ ] (Complementar) Inclinação da cabeça via IMU
- [ ] (Complementar) Detecção de bocejo via MAR

## Limitações conhecidas

Variação de iluminação, ângulo da câmera, óculos/acessórios e diferenças entre dispositivos (Rocha e Domingues, 2022).
