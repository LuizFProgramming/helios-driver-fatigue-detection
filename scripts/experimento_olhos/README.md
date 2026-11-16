# Experimento: olho aberto ou fechado

Compara, na mesma base pública e com divisão por pessoa, os critérios que o HELIOS usa para decidir que o olho
está fechado e uma CNN treinada pelo grupo. É o experimento proposto em "HELIOS: reta final do TCC" (decisões A e B).

| # | Classificador | Origem |
| --- | --- | --- |
| 1 | EAR < 0,19 | Limiar do app (`ConfiguracaoDeteccao.kt`, Medina, 2025) |
| 2 | EAR < limiar de Youden | Limiar escolhido na validação e aplicado no teste (Youden, 1950) |
| 3 | ECF > 0,40 | Blendshape `eyeBlink` do MediaPipe, limiar do app |
| 4 | Regra do app | EAR < 0,19 ou ECF > 0,40, o critério de olho fechado do HELIOS |
| 5 | CNN do grupo | MobileNetV2 com transfer learning (Sandler et al., 2018) sobre o recorte de cada olho; corte de Youden escolhido na validação |

Os índices dos pontos, a fórmula do EAR (em pixels) e os limiares são os mesmos do app Android. Os classificadores
1 a 4 olham uma imagem parada; no app eles ainda passam pelas regras de tempo (escada de 1,5 s, 3 s e 6 s), que este
experimento não mede.

## Base de dados

A base precisa ter o **rosto inteiro**, porque EAR e ECF dependem da malha facial. Bases só com o recorte do olho
(como a MRL Eye) servem para treinar a CNN, mas não para comparar com o EAR.

Sugestão: **CEW, Closed Eyes In The Wild** (Song et al., 2014), com cerca de 2.400 rostos de olhos abertos e
fechados. Confira a licença antes de usar e cite a base no TCC. Coloque assim (a pasta `data/` não vai para o git):

```
data/raw/CEW/
├── closedFace/
└── openFace/
```

Outra base com rosto inteiro também serve, nas pastas `fechado/` e `aberto/`. Se os arquivos seguirem o padrão
`Nome_Pessoa_0001.jpg`, o script reconhece a pessoa; senão, cada arquivo conta como uma pessoa.

## Como rodar

Python 3.10 a 3.12. Na raiz do repositório:

```powershell
python -m venv .venv-experimento
.venv-experimento\Scripts\activate
pip install -r scripts/experimento_olhos/requirements.txt
```

O modelo `models/face_landmarker.task` precisa estar no lugar (veja `models/README.md`).

| Passo | Comando | Gera |
| --- | --- | --- |
| 1. Manifesto e divisão | `python scripts/experimento_olhos/manifesto.py --base data/raw/CEW` | `data/processed/experimento_olhos/manifesto.csv` (70% treino, 15% validação, 15% teste, por pessoa) |
| 2. MediaPipe | `python scripts/experimento_olhos/extrair.py` | `caracteristicas.csv` (EAR, ECF, tempo) e os recortes dos olhos |
| 3. Treino da CNN | `python scripts/experimento_olhos/treinar_cnn.py` | `models/cnn_olhos.keras`, curvas de treino |
| 4. Avaliação | `python scripts/experimento_olhos/avaliar.py` | tabela de métricas, matrizes de confusão, curvas ROC |
| 5. Robustez | `python scripts/experimento_olhos/robustez.py` | queda de desempenho com pouca luz, sombra e ruído |
| 6. TFLite (opcional) | `python scripts/experimento_olhos/exportar_tflite.py` | `models/cnn_olhos_int8.tflite` para o app |

Sem GPU, `treinar_cnn.py --arquitetura pequena` treina uma CNN menor do zero em poucos minutos. Ela também serve como
segunda linha de comparação no TCC (rede pequena do zero contra MobileNetV2 pré-treinada).

## Resultados

Tudo vai para `scripts/experimento_olhos/resultados/`. Só há números e gráficos ali, nenhum rosto, então essa pasta
pode ir para o git e as figuras podem ir direto para o capítulo de resultados:

- `metricas.md` / `metricas.csv`: acurácia, precisão, sensibilidade, especificidade, F1 e tempo por imagem
- `deteccao.csv`: em quantas imagens o MediaPipe achou o rosto (sem rosto, nenhum classificador decide)
- `matriz_confusao.png`, `curvas_roc.png`, `resumo.json` (limiar de Youden e AUC)
- `treino_curvas.png`, `treino_historico.csv`, `treino_resumo.json`
- `robustez.csv`
- `tflite_conferencia.csv`: mesma decisão no Keras e no TFLite int8

Positivo = olho fechado. Sensibilidade é a fração de olhos fechados que o classificador encontrou; especificidade é a
fração de olhos abertos que ele não confundiu com fechados.

## IA generativa (decisão B)

As condições `pouca_luz`, `sombra` e `ruido_desfoque` são alterações de imagem fixas, não IA generativa. Para usar IA
generativa como o objetivo específico (d) do TCC descreve, gere versões das imagens de teste com um modelo generativo
(por exemplo, pedindo pouca luz, óculos ou sombra), salve em uma pasta com os mesmos nomes e subpastas do teste e rode:

```powershell
python scripts/experimento_olhos/robustez.py --pasta-gerada data/synthetic/olhos_ia
```

Registre no TCC qual modelo gerou as imagens, com que instrução e quantas foram descartadas por defeito.

## Testes

```powershell
python -m pytest scripts/experimento_olhos/tests
```

## Referências para o TCC

- BAZAREVSKY, V. et al. BlazeFace: sub-millisecond neural face detection on mobile GPUs. arXiv:1907.05047, 2019.
- KARTYNNIK, Y. et al. Real-time facial surface geometry from monocular video on mobile GPUs. arXiv:1907.06724, 2019.
- SANDLER, M. et al. MobileNetV2: inverted residuals and linear bottlenecks. In: CVPR, 2018.
- SONG, F. et al. Eyes closeness detection from still images with multi-scale histograms of principal oriented
  gradients. Pattern Recognition, v. 47, n. 9, p. 2825-2838, 2014.
- SOUKUPOVÁ, T.; ČECH, J. Real-time eye blink detection using facial landmarks. 2016.
- YOUDEN, W. J. Index for rating diagnostic tests. Cancer, v. 3, n. 1, p. 32-35, 1950.

Confira cada referência na fonte antes de colocar no TCC.
