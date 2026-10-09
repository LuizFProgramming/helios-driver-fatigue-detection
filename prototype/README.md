# Protótipo

Protótipo funcional do HELIOS (detector, alerta, API e apps) e, quando houver, telas, fluxos e mockups da interface.
Exporte arquivos finais de design (PNG/PDF) ou coloque o link do Figma aqui.

## Protótipo funcional

| Pasta | Conteúdo |
| --- | --- |
| `backend/` | API FastAPI v2.0 (`main.py`, porta 8000) e painel web (`painel.html`, em `/painel`) |
| `esp32cam/` | `webcam_api.py`: lê a câmera (webcam ou DroidCam), analisa todo frame com o detector e envia o resultado à API |
| `prototipo_desktop_python/` | Detector MediaPipe + regras (EAR, ECF, MAR, PERCLOS, direção do rosto) e alerta sonoro. Limiares e sons em `config.py` |
| `tests/` | Testes pytest (rodam sem câmera, sem MediaPipe e sem API no ar) |
| `mobile/` | App Expo / React Native |

Detalhes e histórico: [`docs/atualizacoes/`](../docs/atualizacoes/). Análises do código: [`docs/analises/`](../docs/analises/).

### Como rodar (Windows, dentro de `prototype/`)

1. Criar o ambiente. Use o Python 3.12 ou outra versão entre 3.10 e 3.13 (`py --list` mostra as instaladas):
   `py -3.12 -m venv .venv` e `.venv\Scripts\python.exe -m pip install -r requirements.txt`
   - Se aparecer `No suitable Python runtime found`, a versão pedida não está instalada: troque o número (ex.: `py -3.13`) ou instale com `winget install Python.Python.3.12`.
2. Colocar o modelo `face_landmarker.task` em `prototipo_desktop_python/` (ver [`models/README.md`](../models/README.md)). Atalho, já que o app Android guarda uma cópia:
   `copy ..\Aplicativo-Android\app\src\main\assets\face_landmarker.task prototipo_desktop_python\`
   (não faça commit dessa cópia).
3. Em `esp32cam/webcam_api.py`, ajustar `CAMERA_INDEX` no topo: `0` para a webcam do notebook, ou o endereço do DroidCam. Não faça commit do seu valor local.
4. Em 3 terminais:
   - `cd backend; ..\.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000`
   - `.venv\Scripts\python.exe esp32cam\webcam_api.py` (na janela da câmera: `m` liga/desliga o som, `q` sai)
   - `cd mobile; npx expo start`
5. Testes: `.venv\Scripts\python.exe -m pytest tests -v`

Para só testar a análise e o som, os terminais da API e do Expo são opcionais: o alerta sonoro não depende da API.

### Alerta sonoro

Quando a categoria vira DESATENTO, SONOLENCIA ou DORMINDO, o `webcam_api.py` toca um bipe que se repete até a situação passar. O som só para 1 segundo depois do fim do alerta, para não picotar quando a categoria oscila por um frame.

| Categoria | Som (valores em `PADROES_SONOROS`, no `config.py`) |
| --- | --- |
| DORMINDO | 2000 Hz, bipe de 300 ms a cada 400 ms, volume 100% |
| SONOLENCIA | 880 Hz, bipe de 200 ms a cada 2,7 s, volume 60% |
| DESATENTO | 1200 Hz, bipe de 150 ms a cada 1 s, volume 80% (proposta, não validada) |
| ATENTO, PISCANDO, SEM_ROSTO | silêncio |

- **Ouvir sem câmera:** dentro de `prototipo_desktop_python/`, `python testar_alerta.py` toca os três sons. Com `python testar_alerta.py --exportar sons` ele só gera arquivos `.wav`, em qualquer sistema.
- **Mudar um som:** edite `PADROES_SONOROS` e rode de novo. O volume geral continua sendo o do Windows.
- **Só Windows:** o som usa o `winsound`. Em outro sistema o alerta fica em silêncio e um aviso aparece no terminal.
- **Estrutura:** `domain/alert_pattern.py` (padrão e geração do som), `domain/alert_controller.py` (quando toca e para), `ports/alert_output_port.py` (contrato) e `adapters/audio_alert.py` (alto-falante).
- **Fora do escopo por enquanto:** o `helios_main.py` (janela antiga), o painel web e o app Expo não tocam som; o buzzer da ESP32-CAM ainda não está ligado ao Python.

As fotos capturadas ficam em `backend/uploads/` e **não vão para o Git** (rostos de pessoas, LGPD).
