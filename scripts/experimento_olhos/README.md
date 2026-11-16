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

Para treinar a CNN com mais imagens, a **MRL Eye** (Fusek, 2018) vai em `data/raw/MRL/`, com as pastas por pessoa como
vêm no download (`s0001/s0001_00001_0_0_0_0_0_01.png`). Os arquivos já são o recorte do olho, então só são
redimensionados para 64x64. O rótulo é o 5º campo do nome (na MRL, 0 é fechado e 1 é aberto). A MRL entra **só no
treino**: validação e teste continuam na CEW, divididos por pessoa, para os números serem comparáveis com o EAR. Ela é
infravermelha, outra câmera e outra luz; por isso a comparação mede com e sem ela. Confira a licença antes de usar.

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
| 7. Tamanhos de CNN | `python scripts/experimento_olhos/comparar_tamanhos.py --mrl data/raw/MRL` | `resultados/tamanhos.*`, `models/olho_cnn.tflite`, `models/olho_cnn_dourado.csv` |

`treinar_cnn.py --arquitetura` aceita quatro redes, todas com a mesma entrada (64x64x1, cinza de 0 a 255) e a mesma
saída (`prob_fechado`), para o app trocar de modelo sem mudar código:

| Arquitetura | O que é |
| --- | --- |
| `pequena` | 3 blocos convolucionais (16, 32, 64 filtros), do zero. Treina em poucos minutos sem GPU |
| `media` | 3 blocos de duas convoluções (32, 64, 128 filtros), mesma cabeça, do zero |
| `mobilenet` (padrão) | MobileNetV2 alpha 0,35 com pesos do ImageNet e ajuste fino |
| `mobilenet_100` | MobileNetV2 alpha 1,0, mesma receita |

Com `--mrl <pasta>` os recortes da MRL entram no treino; o terminal e o `treino_resumo.json` mostram quantos olhos de
cada base entraram. Antes de treinar, o script mede alguns passos e mostra o tempo estimado. A `mobilenet_100` com a
MRL fica limitada a 10 épocas, porque numa CPU cada época leva muito tempo.

## Resultados

Tudo vai para `scripts/experimento_olhos/resultados/`. Só há números e gráficos ali, nenhum rosto, então essa pasta
pode ir para o git e as figuras podem ir direto para o capítulo de resultados:

- `metricas.md` / `metricas.csv`: acurácia, precisão, sensibilidade, especificidade, F1 e tempo por imagem
- `deteccao.csv`: em quantas imagens o MediaPipe achou o rosto (sem rosto, nenhum classificador decide)
- `matriz_confusao.png`, `curvas_roc.png`, `resumo.json` (limiar de Youden e AUC)
- `treino_curvas.png`, `treino_historico.csv`, `treino_resumo.json`
- `robustez.csv`
- `tflite_conferencia.csv`: mesma decisão no Keras e no TFLite int8
- `tamanhos.csv`, `tamanhos.md`, `tamanhos.png`: comparação de tamanhos de CNN (veja abaixo)

Positivo = olho fechado. Sensibilidade é a fração de olhos fechados que o classificador encontrou; especificidade é a
fração de olhos abertos que ele não confundiu com fechados.

A tabela do `avaliar.py` (`metricas.md`) é **por imagem**: a CNN decide pela média dos dois olhos, com o corte de
Youden da validação, para comparar com EAR e ECF, que também decidem pela imagem. A tabela de tamanhos é **por olho**
(cada recorte é uma amostra), com corte 0,5, porque é assim que o app usa a rede. Os números das duas não se comparam
diretamente.

## Escolha do modelo para o app

O grupo aceita até 1 MB a mais no APK. Mais parâmetros não garantem mais acerto: com recortes de 64x64 e poucos
milhares de imagens, uma rede grande pode decorar o treino. Por isso o `comparar_tamanhos.py` treina as quatro
arquiteturas, cada uma só com a CEW e com CEW + MRL, e escolhe pela validação:

- **regra:** o modelo do app é o de **maior F1 na validação** entre os `.tflite` de **até 1024 KB**. No empate, o
  menor. A `mobilenet_100` entra na curva, mas não pode ser escolhida;
- **o teste só é olhado depois da escolha**. Ele aparece na tabela para relatar o desempenho, não para escolher. Escolher
  pelo teste deixaria o número do capítulo otimista;
- todas as métricas são medidas no `.tflite` int8, que é o que vai para o app, e não no Keras.

A tabela traz, para cada rede: parâmetros, tamanho do `.tflite` em KB, F1 na validação, acurácia, sensibilidade,
especificidade, F1 e AUC no teste da CEW, e o tempo médio por recorte no PC (uma chamada por recorte, como no app). O
gráfico `tamanhos.png` mostra tamanho (eixo log) contra F1, com a linha do limite de 1024 KB.

**A conta do tamanho.** No int8 cada peso ocupa cerca de 1 byte; no float32, 4 bytes. Então o `.tflite` int8 tem
mais ou menos 1 KB por mil parâmetros, mais um acréscimo: os vieses ficam em int32 (4 bytes cada) e cada camada guarda
as escalas de quantização e a descrição do grafo. Quanto mais camadas, maior o acréscimo. Medido nas quatro redes (o
tamanho não depende dos dados de treino):

| Arquitetura | Parâmetros | `.tflite` int8 | Em float32 seria |
| --- | --- | --- | --- |
| `pequena` | 23.809 | 31 KB | ~93 KB |
| `media` | 288.353 | 298 KB | ~1,1 MB |
| `mobilenet` | 411.489 | 583 KB | ~1,6 MB |
| `mobilenet_100` | 2.259.265 | 2.591 KB | ~8,6 MB |

A `mobilenet_100` passa do limite mesmo em int8; as outras três cabem.

### Resultado da comparação (16/11/2026)

Tabela completa em `resultados/tamanhos.md` e gráfico em `resultados/tamanhos.png`. Base CEW (versão 100x100, 2.423
rostos, rosto achado em 98,3%), MRL Eye com 84.898 recortes no treino.

- **Escolhido: `media` treinada só com a CEW**, 303 KB, F1 de 91,9% na validação. No teste (por olho, corte 0,5):
  acurácia 94,8%, sensibilidade 94,0%, especificidade 95,6%, F1 94,7%, AUC 0,985; 2,9 ms por recorte no PC.
- **A MRL não ajudou.** Com ela no treino, a `media` caiu para F1 90,8% no teste e a `pequena` de 78,4% para 74,9%.
  As imagens da MRL são infravermelhas, de outras câmeras; a validação e o teste são fotos coloridas da CEW.
- **As MobileNet perdem muito na conversão para int8.** No Keras elas vão bem no teste (AUC 0,989 a alpha 0,35 e 0,985
  a alpha 1,0), mas o `.tflite` int8 diz "fechado" para quase tudo (probabilidade média entre 0,86 e 0,98; AUC 0,887
  e 0,931).
  A calibração da entrada não é a causa: com e sem os recortes preto e branco, e com só dois pixels nos extremos, o
  resultado foi o mesmo (AUC entre 0,884 e 0,887). A `media` quase não perde (AUC 0,996 no Keras, 0,985 no int8, mesma
  decisão em 96% dos olhos). Para o capítulo 6, a curva "tamanho x acerto" das MobileNet mede a quantização, não a
  arquitetura. Trabalho futuro: treino ciente de quantização ou float16 (a `mobilenet` alpha 0,35 ficaria perto de
  1 MB: 2 bytes por parâmetro mais o acréscimo das camadas, no limite do app).
- **`mobilenet_100` com a MRL não foi treinada**: o treino foi interrompido por falta de memória na época 5 de 10. Ela
  não poderia ser escolhida (2,6 MB) e as outras sete foram medidas com `comparar_tamanhos.py --retomar`.

O modelo escolhido é copiado para `models/olho_cnn.tflite`. A entrada é 1x64x64x1 em `uint8` com escala 1 e ponto zero 0:
o app manda os bytes do recorte sem conversão. O `exportar_tflite.py` garante isso calibrando com um recorte todo preto e
um todo branco, e um teste confere a escala em todas as arquiteturas.

`models/olho_cnn_dourado.csv` é a conferência do app: para 100 rostos do teste (dois olhos cada), o nome do recorte, a
probabilidade que o `.tflite` deu e os 4096 pixels em hexadecimal. **Esse CSV tem pixels de olhos de pessoas reais.**
A pasta `models/` não vai para o git, e o CSV não deve ir para o repositório nem para o APK sem o grupo decidir (LGPD e
licença da CEW).

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
- FUSEK, R. Pupil localization using geodesic distance. In: ISVC, 2018. (Artigo indicado pelos autores da MRL Eye.)
- KARTYNNIK, Y. et al. Real-time facial surface geometry from monocular video on mobile GPUs. arXiv:1907.06724, 2019.
- SANDLER, M. et al. MobileNetV2: inverted residuals and linear bottlenecks. In: CVPR, 2018.
- SONG, F. et al. Eyes closeness detection from still images with multi-scale histograms of principal oriented
  gradients. Pattern Recognition, v. 47, n. 9, p. 2825-2838, 2014.
- SOUKUPOVÁ, T.; ČECH, J. Real-time eye blink detection using facial landmarks. 2016.
- YOUDEN, W. J. Index for rating diagnostic tests. Cancer, v. 3, n. 1, p. 32-35, 1950.

Confira cada referência na fonte antes de colocar no TCC.
