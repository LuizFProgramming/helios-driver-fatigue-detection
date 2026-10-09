# 🌅 HELIOS — Driver Fatigue Detection

> Detecção preditiva de fadiga ao volante baseada em visão computacional, sensores inerciais e inteligência artificial.

![status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)
![python](https://img.shields.io/badge/python-3.10%E2%80%933.13-blue)
![licença](https://img.shields.io/badge/licen%C3%A7a-a%20definir-lightgrey)

Projeto de **Trabalho de Graduação Interdisciplinar** do curso de Ciência da Computação da **Universidade Cidade de São Paulo (UNICID)**.

**Título do trabalho:** *Detecção Preditiva de Fadiga ao Volante: Uma Abordagem Baseada em Visão Computacional e Modelos de Inteligência Artificial Generativa*
**Orientadora:** Prof.ª Angela Perez Barcellos

---

## 📌 Sobre o projeto

A fadiga e a sonolência ao volante comprometem a atenção, o tempo de reação e a tomada de decisão do condutor. O HELIOS busca **identificar sinais precoces de fadiga e microssonos** e **emitir alertas preventivos em tempo real**, mesmo em condições adversas como baixa luminosidade.

**Pergunta de pesquisa:** como a integração entre Visão Computacional, sensores inerciais e Inteligência Artificial pode contribuir para a detecção preditiva de fadiga e microssonos em motoristas?

## 🔄 Fluxo do sistema

```
Captura de imagem  →  Extração de características  →  Análise dos sinais  →  Classificação  →  Alerta
 (webcam / ESP32-CAM)  (MediaPipe: EAR, ECF, MAR,      (PERCLOS, tempo de      (ATENTO, PISCANDO,   (som, tela,
                        PERCLOS, direção do rosto)      olho fechado)           DESATENTO, SONOLENCIA, vibração no
                                                                                DORMINDO)             app)
```

## ✅ Estado atual (08/10/2026 · Release 4, Sprint 1)

| Parte | Onde | Situação |
| --- | --- | --- |
| Detector facial e regras (EAR, ECF, MAR, PERCLOS, direção do rosto) | [`prototype/prototipo_desktop_python/`](prototype/prototipo_desktop_python/) | Implementado e rodando na webcam. Validação com vídeos rotulados ainda pendente (R4S2) |
| Script da câmera (webcam ou celular via DroidCam, análise a 30 FPS) | [`prototype/esp32cam/webcam_api.py`](prototype/esp32cam/webcam_api.py) | Implementado |
| **Alerta sonoro no PC** | [`prototype/prototipo_desktop_python/`](prototype/prototipo_desktop_python/) | **Novo.** Funciona no Windows. O padrão de DESATENTO é proposta, ainda não validada |
| API FastAPI e painel web | [`prototype/backend/`](prototype/backend/) | Implementado |
| App Expo (React Native) | [`prototype/mobile/`](prototype/mobile/) | Consulta a API e vibra em "Sonolência Detectada". Não toca som |
| App Android (Kotlin nativo) | [`Aplicativo-Android/`](Aplicativo-Android/) | Domínio e 67 testes escritos; o módulo `app` ainda não foi compilado nem testado em aparelho |
| Firmware da ESP32-CAM | [`firmware/`](firmware/) | A fazer |
| Sensores inerciais (IMU) | — | No Android, o acelerômetro do aparelho já tem adaptador; no protótipo Python não há |
| Classificador treinado e IA generativa | — | Não iniciados no código. O "modelo" hoje é o MediaPipe pré-treinado mais regras |

## 🔊 Alerta sonoro

Cada categoria de alerta toca um bipe que se repete até a situação passar. Os valores ficam em `PADROES_SONOROS`, em [`prototype/prototipo_desktop_python/config.py`](prototype/prototipo_desktop_python/config.py).

| Categoria | Quando dispara | Som |
| --- | --- | --- |
| DORMINDO | Olhos fechados sem parar (cerca de 1 s) | 2000 Hz, bipe de 300 ms a cada 400 ms, volume máximo |
| SONOLENCIA | PERCLOS acima de 40% ou bocejo confirmado | 880 Hz, bipe de 200 ms a cada 2,7 s |
| DESATENTO | Rosto fora da câmera por 2 s ou mais | 1200 Hz, bipe de 150 ms a cada 1 s |
| ATENTO, PISCANDO, SEM_ROSTO | — | Silêncio |

Na janela da câmera, a tecla `m` liga e desliga o som. Para ouvir cada um sem câmera: `python testar_alerta.py`, dentro de `prototype/prototipo_desktop_python/`. Detalhes em [`docs/atualizacoes/2026-10-08.md`](docs/atualizacoes/2026-10-08.md).

## 🧰 Tecnologias

| Área | Em uso | Previsto |
| --- | --- | --- |
| Visão computacional | OpenCV, MediaPipe Face Landmarker (478 pontos + blend shapes) | — |
| Protótipo e API | Python, FastAPI, Pillow, NumPy | — |
| Alerta sonoro | `winsound` (já vem no Python para Windows) | Saída para outros sistemas |
| App mobile | Expo / React Native; Kotlin, CameraX e acelerômetro (Android) | — |
| Hardware embarcado | — | ESP32-CAM, sensor inercial (IMU) |
| Machine Learning e IA generativa | — | Classificador treinado (TensorFlow ou PyTorch), ampliação de dados |
| Gestão | Scrum (releases e sprints), GitHub Projects | — |

> As escolhas finais são registradas em [`docs/decisions/`](docs/decisions/).

## 📏 Métricas-chave

- **EAR** (Eye Aspect Ratio): abertura palpebral, olho fechado abaixo de 0,19
- **ECF** (Eye Closure Factor): fechamento ocular medido pelo próprio modelo do MediaPipe
- **PERCLOS**: percentual de tempo com olhos fechados em janela de 5 s
- **MAR** (Mouth Aspect Ratio): detecção de bocejos
- **Direção do rosto**: yaw e pitch pela matriz de pose do MediaPipe
- **Microssono**: olhos fechados sem parar por cerca de 1 s

Validação prevista: acurácia, precisão, sensibilidade, especificidade, taxa de falsos positivos e tempo de resposta. Os limiares e o que ainda falta validar estão em [`docs/metrics.md`](docs/metrics.md).

## 🗂️ Estrutura do repositório

```
.
├── .github/              # templates de issue e pull request
├── Aplicativo-Android/   # app Android em Kotlin (domínio, protocolos, app)
├── prototype/            # protótipo funcional em Python e app Expo
│   ├── prototipo_desktop_python/   # detector, regras e alerta sonoro (domain, ports, adapters)
│   ├── esp32cam/         # script da câmera (webcam_api.py)
│   ├── backend/          # API FastAPI e painel web
│   ├── mobile/           # app Expo / React Native
│   └── tests/            # testes pytest
├── docs/                 # arquitetura, métricas, análises, atualizações, decisões, sprints
├── firmware/             # código embarcado da ESP32-CAM (a fazer)
├── hardware/             # case 3D, esquemas, lista de materiais
├── models/               # modelos (não versionados; ver models/README.md)
├── data/                 # datasets (não versionados)
├── notebooks/            # experimentos e análises exploratórias
├── scripts/              # utilitários
├── src/helios/           # reservado; ainda sem código
└── tests/                # reservado; os testes atuais estão em prototype/tests
```

## 🚀 Como rodar o protótipo (Windows)

O passo a passo completo, com os problemas mais comuns, está em [`prototype/README.md`](prototype/README.md). Resumo, a partir da pasta `prototype/`:

```powershell
# 1. ambiente (use uma versão 3.10 a 3.13; veja py --list)
py -3.12 -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt

# 2. modelo do MediaPipe (ver models/README.md)
# 3. API (terminal 1)
cd backend
..\.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000

# 4. câmera, com alerta sonoro (terminal 2, de volta em prototype/)
.venv\Scripts\python.exe esp32cam\webcam_api.py
```

Testes: `.venv\Scripts\python.exe -m pytest tests -v` (85 testes, rodam sem câmera). O app Android tem instruções próprias em [`Aplicativo-Android/README.md`](Aplicativo-Android/README.md).

## 🗓️ Roadmap

| Release | Período | Foco | Status |
| --- | --- | --- | --- |
| R1 – Estruturação inicial | 16/03 – 10/05 | Equipe, escopo, tema | ✅ |
| R2 – Pesquisa e fundamentação | 11/05 – 05/07 | Metodologia, referências, primeiros testes | ✅ |
| R3 – Planejamento técnico | 03/08 – 27/09 | Métricas, arquitetura, protótipo planejado | ✅ |
| R4 – Desenvolvimento e entrega | 28/09 – 22/11 | Protótipo, testes, documento final | 🔄 |

Próximos marcos: **R4S2** (12/10 a 25/10), testes e ajustes do protótipo; **R4S3** (26/10 a 08/11), consolidação do documento; **R4S4** (09/11 a 22/11), revisão ABNT e entrega final. Acompanhamento no **GitHub Projects**, em [`docs/sprints/`](docs/sprints/) e no [registro de atualizações](docs/atualizacoes/).

## 👥 Equipe

- Alexandre Araujo Torres
- Gabriel Souza Almeida
- Helbert de Sousa Araújo
- Igor Sobral Baldasin
- Juan Nakao Orozco Ospina
- Lucas Tavares de Sá Gomes
- Luiz Fernando de Oliveira Matos
- Matheus Nakao Orozco Ospina
- Pedro Henrique dos Santos Silva

## 🤝 Contribuindo

Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir sua primeira branch ou PR. Fotos e vídeos de pessoas não vão para o repositório (LGPD).

## 📄 Licença

A definir (ver [`docs/decisions/`](docs/decisions/)).

## 📚 Referências

Lista completa em [`docs/references.md`](docs/references.md).
