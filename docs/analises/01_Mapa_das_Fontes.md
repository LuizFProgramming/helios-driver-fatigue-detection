# 01. Mapa das fontes do HELIOS

Este arquivo lista tudo que foi reunido na pasta TCC, de onde veio cada coisa e como as partes se conectam.

## 1. Inventário

| # | Fonte | Onde estava | Onde ficou na pasta TCC | O que é |
|---|-------|-------------|--------------------------|---------|
| 1 | Guia_Implementacao_HELIOS_IA.pdf (5 páginas) | Anexo do chat (enviado duas vezes, arquivos idênticos, mesmo MD5) | `01_Fontes_Originais/` | Roteiro didático da parte de IA: 14 etapas, da webcam ao protótipo integrado |
| 2 | Imagem "Etapa 1 a 12" | Anexo do chat | `01_Fontes_Originais/Etapas_IA_12_passos.png` | Versão resumida do roteiro, com as classes `normal`, `sonolento`, `fadiga` |
| 3 | Projeto "Entrega definitiva" | `Downloads\Entrega definitivav1-1\Entrega definitiva` | `03_Codigo/prototipo_desktop_python/` | Protótipo funcional em Python: MediaPipe + OpenCV, arquitetura hexagonal, EAR, MAR, PERCLOS, ECF |
| 4 | Projeto "HELIOS" | `Downloads\HELIOS\HELIOS` | `03_Codigo/backend_fastapi_esp32/` | API FastAPI que recebe frames + simulador de ESP32-CAM. A `.venv` não foi copiada (é recriável) |
| 5 | Face Detector (MediaPipe) | developers.google.com | `07_Links_e_Referencias.md` | Detector BlazeFace: caixa do rosto + 6 pontos. Não dá landmarks dos olhos |
| 6 | Face Landmarker Web/JS | developers.google.com | `07_Links_e_Referencias.md` | Mesma IA do protótipo Python, mas rodando em JavaScript/TypeScript (`@mediapipe/tasks-vision`) |
| 7 | OmniRoute (GitHub) | github.com/diegosouzapw/OmniRoute | `07_Links_e_Referencias.md` | Gateway de LLMs (roteia pedidos entre provedores de IA). Não tem relação com visão computacional |
| 8 | Metas de qualidade | Mensagem do chat | `06_Metas_de_Qualidade.md` | Complexidade < 22, cobertura 100%, 0 mutantes, 0 `any`/`unknown` etc. |

## 2. Como as peças se encaixam

```
               ROTEIRO (o que fazer)
   PDF 14 etapas  +  imagem 12 etapas
                     │
     ┌───────────────┴────────────────┐
     ▼                                ▼
 IA / visão computacional         Transporte de imagem
 prototipo_desktop_python         backend_fastapi_esp32
 (etapas 1 a 8 e 12-13 prontas    (ESP32-CAM simulada envia
  por regras, sem ML)              JPEG para uma API; a API só
                                   guarda, não analisa)
     │                                │
     └──────────── não conversam ─────┘
                     │
                     ▼
          PRÓXIMO PASSO: app mobile / central multimídia
          que recebe o vídeo da câmera do carro e roda a IA
          (ver 04_Arquitetura_App_Carro.md)
```

O ponto mais importante desta consolidação: **existem duas metades que ainda não se falam**. O protótipo desktop analisa o rosto, mas lê só a webcam local. O backend recebe imagens da ESP32-CAM, mas não analisa nada. O app mobile é justamente a peça que une as duas.

## 3. Relação com o SafeDrive

O HELIOS é a evolução do projeto SafeDrive. No SafeDrive a arquitetura previa três camadas (ESP32-CAM, backend Java/Spring Boot com PostgreSQL e MQTT, painel React). Os arquivos analisados aqui não usam essa pilha: o HELIOS atual está em Python (protótipo) e o plano novo concentra o processamento no celular ou na central do carro. Vale decidir no texto do TCC se o backend Spring Boot continua no escopo (por exemplo, só para histórico de viagens) ou se sai de vez.

## 4. Diferença entre o PDF e a imagem

| Aspecto | PDF (14 etapas) | Imagem (12 etapas) |
|---------|-----------------|---------------------|
| Detecção de piscada e olho fechado | Etapas separadas (5 e 6) | Juntas (etapa 5) |
| MAR e cabeça | Etapas separadas (7 e 8) | Juntas (etapa 6) |
| Classes | Normal, Sonolência, Fadiga | `normal`, `sonolento`, `fadiga` |
| Integração final | Etapa 14 "Integrar tudo" | Etapa 12 é o alerta |
| Algoritmos citados | Random Forest, SVM, KNN, XGBoost, redes neurais | Não cita |
| Métricas | Acurácia, precisão, sensibilidade, especificidade, falsos positivos, tempo de resposta | Não cita |

O conteúdo é o mesmo. O roteiro unificado está em `05_Roteiro_Unificado.md`.
