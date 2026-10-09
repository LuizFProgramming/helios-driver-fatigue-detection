# HELIOS — contexto do projeto para o Claude Code

TCC de Ciência da Computação (UNICID, 2026): "Detecção Preditiva de Fadiga ao Volante". O sistema detecta sonolência do motorista por visão computacional e alerta no celular.

Este arquivo resume a análise de HELIOS.zip, helios-mobile.zip, TCC (1).7z, Relatorio_Projeto_HELIOS.pdf e Documentação - TCC.docx (06/10/2026). Atualize-o conforme as tarefas forem concluídas.

- Responda sempre em português do Brasil e explique as mudanças em linguagem simples: parte da equipe está aprendendo.
- Ambiente da equipe: Windows, Python (`.venv` na raiz), Node/Expo, VS Code.
- Regra da equipe: uma etapa por vez, testada antes de passar para a próxima.

## Estrutura da pasta

```
HELIOS/
├── CLAUDE.md
├── requirements.txt             dependências Python de todo o projeto
├── backend/main.py              API FastAPI (porta 8000)
├── esp32cam/webcam_api.py       captura a webcam, analisa e envia para a API
├── tests/                       testes pytest (python -m pytest tests -v)
├── helios-mobile/               app Expo; toda a interface está em src/app/index.tsx
├── prototipo_desktop_python/    detector MediaPipe (veio do TCC.7z, pasta 03_Codigo)
└── docs/                        análises (veio do TCC.7z, pasta 02_Documentacao) + 08_Pesquisa_Modelos_Prontos.md
```

## Situação em 06/10/2026

Cronograma: Release 4, Sprint 1 (28/09 a 11/10). Depois: R4S2 (12/10 a 25/10) testes e ajustes; R4S3 (26/10 a 08/11) consolidação do documento; R4S4 (09/11 a 22/11) revisão ABNT e entrega final.

- `prototipo_desktop_python` funciona sozinho: MediaPipe Face Landmarker (`face_landmarker.task`, 478 pontos + blendshapes), EAR, MAR, PERCLOS e ECF. "Olho fechado" = EAR < 0,19 **ou** ECF > 0,40. Máquina de estados ATENTO / PISCANDO / SONOLENTO / SEM FACE em `domain/drowsiness_monitor.py`. Arquitetura hexagonal (`domain/`, `ports/`, `adapters/`); limiares em `config.py`; ponto de entrada `helios_main.py`.
- `backend/main.py` (v2.0, 06/10): `POST /frames` (multipart: `file`, `frame_id`, `device_id`, `status_motorista`, `categoria`, `direcao_rosto`, `capturado_em`, `ear`, `ecf`, `mar`, `perclos`, `segundos_em_alerta`, `total_piscadas`, `total_bocejos`); `GET /frames`, `GET /frames/latest`, `GET /frames/{frame_id}` (rotas do app Expo, mantidas); `GET /leituras/latest` (JSON para o app Kotlin); `GET /eventos`, `/eventos/resumo`, `/eventos/{pasta}/{arquivo}`; `GET /painel` (painel web em `backend/painel.html`); CORS liberado; `device_id` validado. Fotos em `backend/uploads/frames/`; alertas copiados para `backend/uploads/eventos/{sonolencia,desatento,dormindo}/` com nome `direção_rosto-TIPO-AAAA-MM-DD-HH-MM-SS.jpg` + `.json` dos dados.
- Categorias (script da câmera → API): ATENTO, PISCANDO, SEM_ROSTO, DESATENTO (sem rosto ≥ 2 s), SONOLENCIA (SONOLENTO por PERCLOS ou bocejo), DORMINDO (SONOLENTO por olhos fechados sem parar). Direção do rosto pela matriz de pose do MediaPipe (`domain/head_pose.py`, limiar 20° em `config.py`); **sinais de esquerda/direita e cima/baixo ainda não conferidos com rosto real**.
- `esp32cam/webcam_api.py` (Tarefa 1, código escrito em 06/10, **falta testar com a webcam**): analisa todo frame com `DetectorFacialMediaPipe` + `MonitorDeSonolencia`, envia a cada 2 s (relógio) e na hora em que o estado vira SONOLENTO; POST numa thread separada; PERCLOS enviado em %; janela com `renderizar_monitor`, sai com `q`.
- `webcam_api.py` roda em 3 threads: `LeitorDeCamera` (guarda só o frame mais novo), `Analisador` (MediaPipe + regras + envio, limitado a `FPS_ALVO = 30` do `config.py`) e a tela no `main()`. Medido com câmera simulada a 30 FPS: análise 30 FPS (MediaPipe ≈ 31 ms por frame em CPU). Frames com largura > 640 são reduzidos só para a detecção (`LARGURA_MAXIMA_PARA_DETECCAO`).
- Janela da câmera: `adapters/hud_veicular.py` (estilo painel de veículo: barra superior com categoria, hora e FPS; malha de 478 pontos do MediaPipe, contorno brilhante, linha de varredura e cantoneiras; olhos, íris e boca; indicadores com linha e ponto em cada pálpebra (EAR do olho), boca (MAR) e cabeça (direção e ângulos); mostradores EAR/ECF/PERCLOS/MAR; bússola; moldura pulsando em alerta). Desenha numa cópia: a foto enviada à API é o frame cru. ~15 ms por frame em 640×480, em paralelo à análise. `helios_main.py` continua usando o `opencv_hud.py` antigo.
- **Alerta sonoro no PC (08/10/2026):** `webcam_api.py` toca um bipe por categoria (DORMINDO 2000 Hz, SONOLENCIA 880 Hz, DESATENTO 1200 Hz; valores em `PADROES_SONOROS` no `config.py`). Código: `domain/alert_pattern.py`, `domain/alert_controller.py`, `ports/alert_output_port.py`, `adapters/audio_alert.py` (só Windows, via `winsound`). O som para 1 s depois do fim do alerta; tecla `m` na janela da câmera silencia. `python testar_alerta.py` (em `prototipo_desktop_python/`) toca os 3 sons sem câmera. 85 testes (`tests/test_alerta.py` tem 40). Padrão de DESATENTO é proposta, não validada; `helios_main.py`, painel web e app Expo não tocam som. Detalhes em `docs/atualizacoes/2026-10-08.md`.
- `helios-mobile`: telas home / config / monitoring; consulta `/frames/latest` a cada 2 s e vibra quando `status_motorista === "Sonolência Detectada"`.
- App Kotlin: em desenvolvimento por outra parte do grupo. Escopo e contrato JSON propostos no doc "HELIOS — Escopo inicial da integração com o app Kotlin" (aguardando aprovação).

## Direção recomendada

- Não treinar modelo do zero agora. O "modelo" é o MediaPipe pré-treinado mais as regras do protótipo (EAR: Soukupová & Čech, 2016; PERCLOS: Dinges & Grace, 1998). Um classificador treinado (Random Forest, LSTM) fica como trabalho futuro.
- A análise roda em todos os frames, no script da câmera; a API recebe só o resultado.
- Fase A (entrega): notebook analisa, API repassa, app lê. Fase B (opcional): MediaPipe dentro do app Kotlin (`com.google.mediapipe:tasks-vision`).

## Tarefas, em ordem

1. Integrar o protótipo ao `esp32cam/webcam_api.py` — código escrito; falta rodar com webcam real.
2. Correções pequenas: mesmo `deviceId` no app e no script (`ESP32CAM_01` × `WEBCAM_NOTEBOOK`); `!= null` em `ear`, `mar`, `perclos` no app; `CORSMiddleware` no backend; (opcional) "Testar Conexão" usar o retorno de `verificarStatusApi`.
3. Antes da validação: corrigir P2 (frames → tempo) e P3 (EMA esconde piscadas), descritos em `docs/03_Problemas_Encontrados.md`. Avisar a equipe sobre mudança de limiar ou lógica (o texto do TCC cita os valores).
4. Validação (R4S2): vídeos curtos rotulados; acurácia, precisão, sensibilidade, especificidade, falsos positivos e tempo de resposta. Os números de acurácia em `config.py` são do artigo de referência, não do HELIOS.

## Ambiente e execução

- O `.venv` que veio no HELIOS.zip aponta para o Python de outra máquina e não funciona aqui. Recriar com `py -3.12 -m venv .venv` e `python -m pip install -r requirements.txt` (este PC tem só o Python 3.12).
- Ativar antes de rodar Python: `.venv\Scripts\activate`.
- Antes de mexer no app Expo, ler `helios-mobile/AGENTS.md` (Expo SDK 57; instalar pacotes com `npx expo install`).
- Rodar em 3 terminais:
  1. `cd backend` e depois `uvicorn main:app --reload --host 0.0.0.0 --port 8000`
  2. `python esp32cam/webcam_api.py`
  3. `cd helios-mobile` e depois `npx expo start`
- Celular e PC na mesma rede Wi-Fi. No app: Configurações → `http://<IP-do-PC>:8000`.
