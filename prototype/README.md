# Protótipo visual

Telas, fluxos, mockups e demonstrações da interface (alertas visuais/sonoros, monitoramento facial, etc.).
Exporte arquivos finais (PNG/PDF) ou coloque o link do Figma aqui.

## Protótipo funcional

| Pasta | Conteúdo |
| --- | --- |
| `backend/` | API FastAPI v2.0 (`main.py`, porta 8000) e painel web (`painel.html`, em `/painel`) |
| `esp32cam/` | `webcam_api.py`: lê a câmera (webcam ou DroidCam), analisa todo frame com o detector e envia o resultado à API |
| `prototipo_desktop_python/` | Detector MediaPipe + regras (EAR, ECF, MAR, PERCLOS, direção do rosto). Limiares em `config.py` |
| `tests/` | Testes pytest (rodam sem câmera, sem MediaPipe e sem API no ar) |
| `mobile/` | App Expo / React Native |

Detalhes e histórico: [`docs/atualizacoes/`](../docs/atualizacoes/). Análises do código: [`docs/analises/`](../docs/analises/).

### Como rodar (Windows, dentro de `prototype/`)

1. Criar o ambiente: `py -3.12 -m venv .venv` e `.venv\Scripts\python.exe -m pip install -r requirements.txt`
2. Baixar o modelo `face_landmarker.task` (ver [`models/README.md`](../models/README.md))
3. Em 3 terminais:
   - `cd backend; ..\.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000`
   - `.venv\Scripts\python.exe esp32cam\webcam_api.py` (ajuste `CAMERA_INDEX` no topo do arquivo)
   - `cd mobile; npx expo start`
4. Testes: `.venv\Scripts\python.exe -m pytest tests -v`

As fotos capturadas ficam em `backend/uploads/` e **não vão para o Git** (rostos de pessoas, LGPD).
