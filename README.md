# 🌅 HELIOS — Driver Fatigue Detection

> Detecção preditiva de fadiga ao volante baseada em visão computacional, sensores inerciais e inteligência artificial.

![status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)
![python](https://img.shields.io/badge/python-3.10%2B-blue)
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
Captura de imagem  →  Extração de características  →  Análise dos sinais  →  Classificação do estado  →  Alerta
 (ESP32-CAM / webcam)   (face, EAR, MAR, PERCLOS)     (+ sensores inerciais)    (alerta / fadiga / crítico)   (visual/sonoro)
```

## 🧰 Tecnologias previstas

| Área | Ferramentas |
| --- | --- |
| Visão computacional | OpenCV, MediaPipe (Face Mesh) |
| Machine Learning | TensorFlow/Keras, PyTorch |
| Análise de dados | NumPy, Pandas, scikit-learn, Matplotlib |
| Hardware embarcado | ESP32-CAM, sensor inercial (IMU) |
| IA Generativa | Ampliação de dados e simulação de condições adversas |
| Gestão | Scrum (releases e sprints), GitHub Projects |

> ⚠️ As escolhas finais serão registradas em [`docs/decisions/`](docs/decisions/).

## 📏 Métricas-chave

- **EAR** (Eye Aspect Ratio) — abertura palpebral
- **PERCLOS** — percentual de tempo com olhos fechados
- **MAR** (Mouth Aspect Ratio) — detecção de bocejos
- **Inclinação da cabeça** — via visão e sensores inerciais
- **Microssono** — fechamento ocular prolongado

Validação: acurácia, precisão, sensibilidade, especificidade, taxa de falsos positivos e tempo de resposta. Detalhes em [`docs/metrics.md`](docs/metrics.md).

## 🗂️ Estrutura do repositório

```
.
├── .github/          # templates de issue e pull request
├── docs/             # arquitetura, métricas, decisões (ADRs), sprints
├── src/helios/       # código-fonte principal (pacote Python)
│   ├── capture/      # captura de vídeo/imagem
│   ├── vision/       # face, landmarks, EAR, MAR, PERCLOS
│   ├── sensors/      # leitura/tratamento de sensores inerciais
│   ├── fusion/       # combinação dos sinais e classificação do estado
│   ├── alerts/       # alertas visuais e sonoros
│   ├── genai/        # IA generativa (data augmentation, simulação)
│   └── config/       # parâmetros e limiares
├── firmware/         # código embarcado (ESP32-CAM)
├── hardware/         # case 3D, esquemas, lista de materiais
├── prototype/        # protótipo visual (telas, fluxos, mockups)
├── data/             # datasets (não versionados)
├── models/           # modelos treinados (não versionados)
├── notebooks/        # experimentos e análises exploratórias
├── tests/            # testes automatizados
└── scripts/          # utilitários (download de dados, treino, avaliação)
```

## 🚀 Como rodar

> 🚧 Em construção. Esta seção será preenchida quando houver a primeira versão executável.

```bash
# clonar o repositório
git clone https://github.com/LuizFProgramming/helios-driver-fatigue-detection.git
cd helios-driver-fatigue-detection

# criar e ativar o ambiente virtual
python -m venv .venv
source .venv/bin/activate        # Linux/macOS
.venv\Scripts\activate           # Windows

# instalar dependências (quando definidas)
pip install -r requirements.txt
```

## 🗓️ Roadmap

| Release | Período | Foco | Status |
| --- | --- | --- | --- |
| R1 – Estruturação inicial | 16/03 – 10/05 | Equipe, escopo, tema | ✅ |
| R2 – Pesquisa e fundamentação | 11/05 – 05/07 | Metodologia, referências, primeiros testes | ✅ |
| R3 – Planejamento técnico | 03/08 – 27/09 | Métricas, arquitetura, protótipo planejado | ✅ |
| R4 – Desenvolvimento e entrega | 28/09 – 22/11 | Protótipo, testes, documento final | 🔄 |

Acompanhamento detalhado no **GitHub Projects** e em [`docs/sprints/`](docs/sprints/).

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

Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir sua primeira branch ou PR.

## 📄 Licença

A definir (ver [`docs/decisions/`](docs/decisions/)).

## 📚 Referências

Lista completa em [`docs/references.md`](docs/references.md).
