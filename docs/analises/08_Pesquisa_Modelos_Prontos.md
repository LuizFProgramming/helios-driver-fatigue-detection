# HELIOS: pesquisa de modelos prontos para detecção de sonolência

Data: 06/10/2026. Ferramentas: Agent Reach v1.5.0 (busca Exa, API do Hugging Face, API do GitHub, leitor Jina) e busca web para artigos.

> Atenção: as acurácias abaixo foram **informadas pelos autores** de cada modelo. Nenhuma foi medida no HELIOS. Elas servem para o texto do TCC como "trabalhos relacionados", não como resultado nosso.

## Resposta curta

Sim, existem vários modelos prontos. Eles se dividem em quatro tipos, e só um deles se encaixa bem no HELIOS agora:

| Tipo | O que recebe | O que devolve | Serve para o HELIOS? |
|---|---|---|---|
| A. Classificador de olho | recorte do olho | aberto / fechado | Sim, como reforço do EAR |
| B. Classificador de rosto inteiro | uma foto do rosto | sonolento / não sonolento | Pouco: olha uma foto só, sem tempo |
| C. Modelo temporal (vídeo) | sequência de frames ou de medidas | sonolento / normal | Trabalho futuro |
| D. Sistema completo com regras | webcam | estados (atento, cansado, dormindo...) | Ótimo para comparar com o nosso |

**Descoberta importante:** o HELIOS já usa um modelo pré-treinado que faz parte do tipo A. O `face_landmarker.task` do MediaPipe devolve *blendshapes*, e entre elas estão `eyeBlinkLeft`, `eyeBlinkRight` e `jawOpen`. São valores de 0 a 1 que uma rede neural do Google aprendeu a estimar. O protótipo **já combina** `eyeBlink` com o EAR: o olho conta como fechado quando o EAR fica abaixo de 0,19 **ou** o ECF passa de 0,40 (`config.ECF_LIMIAR_OLHO_FECHADO`). O `jawOpen` é extraído, mas ainda não é usado (P6 em `03_Problemas_Encontrados.md`).

## A. Classificadores de olho aberto / fechado

| Modelo | Arquitetura | Treino | Acurácia informada | Licença | Observação |
|---|---|---|---|---|---|
| [OpenVINO `open-closed-eye-0001`](https://docs.openvino.ai/2023.3/omz_models_model_open_closed_eye_0001.html) | CNN pequena, entrada 32×32 | MRL Eye Dataset | 95,84% | Open Model Zoo (Intel) | O mais leve; feito para rodar em tempo real |
| [dima806/closed_eyes_image_detection](https://huggingface.co/dima806/closed_eyes_image_detection) | ViT-base (≈86 milhões de parâmetros) | Kaggle (olhos) | 99,05% | Apache-2.0 | O mais baixado no Hugging Face (1.336); pesado para rodar a 30 fps em CPU |
| [gudisaketh/real-time-driver-drowsiness-detection](https://github.com/gudisaketh/real-time-driver-drowsiness-detection) | MobileNetV2 (`.h5`, Keras) | MRL Eye | ~95–96% | README diz MIT, mas o GitHub não registra licença | Usa **MediaPipe + EAR + CNN juntos**, a mesma ideia que sugerimos |
| [Teen-Different/Driver-Drowsiness-Detection](https://huggingface.co/Teen-Different/Driver-Drowsiness-Detection) | CNN própria (PyTorch) | UnityEyes (olhos **sintéticos**) | 96,79% | MIT | Os próprios autores avisam que pode falhar com olhos reais |
| [yeeeengyu/YOLO-drowsiness-detector](https://huggingface.co/yeeeengyu/YOLO-drowsiness-detector) | YOLO (detecção) | não informado | não informado | MIT | Detecta rosto e olho aberto/fechado; a sonolência é calculada com MediaPipe |

## B. Classificadores de rosto inteiro (uma foto por vez)

| Modelo | Arquitetura | Treino | Acurácia informada | Licença |
|---|---|---|---|---|
| [mosesb/drowsiness-detection-mobileViT-v2](https://huggingface.co/mosesb/drowsiness-detection-mobileViT-v2) | MobileViT v2 (timm) | Driver Drowsiness Dataset (DDD) + Drowsy Detection Dataset (Kaggle) | não consta no resumo | MIT |
| [mosesb/drowsiness-detection-yolo-cls](https://huggingface.co/mosesb/drowsiness-detection-yolo-cls) | YOLO11x-cls | os mesmos | tabela no model card | MIT |
| [chbh7051/driver-drowsiness-detection](https://huggingface.co/chbh7051/driver-drowsiness-detection) | ViT-base | UTA-RLDD | 97,52% | Apache-2.0 |
| [Teen-Different (MobileNetV2, ResNet18, VGG16)](https://huggingface.co/Teen-Different/Driver-Drowsiness-Detection) | vários | Drowsy Detection Dataset | o melhor é o MobileNetV2 | MIT |

**Por que não usar esses no HELIOS agora:**

1. Eles olham **uma foto isolada**. Sonolência depende de tempo: olho fechado por 0,2 s é uma piscada, por 2 s é perigo. O PERCLOS e a máquina de estados do protótipo já tratam o tempo.
2. Os conjuntos de dados (DDD, RLDD) são frames tirados de vídeos. Quando frames da **mesma pessoa** aparecem no treino e no teste, a acurácia fica inflada. Os model cards não dizem se separaram por pessoa.
3. Eles nunca viram nosso ângulo de câmera, nossa iluminação nem motoristas brasileiros.

## C. Modelos temporais (olham uma sequência)

| Modelo | Ideia | Resultado informado | Observação |
|---|---|---|---|
| [Tandon-A/Drowsiness-Detection-Mediapipe](https://github.com/Tandon-A/Drowsiness-Detection-Mediapipe) (MIT) | Medidas do MediaPipe + LSTM sobre 5 frames, com calibração inicial | não informado | Arquitetura parecida com a nossa, mas com aprendizado |
| [Artigo: blendshapes do MediaPipe + BiLSTM com atenção](https://openaccess.cms-conferences.org/publications/book/978-1-964867-80-9/article/978-1-964867-80-9_7) | Usa só as blendshapes como entrada | 86,9% por janela, 88,9% por vídeo, sensibilidade 91,1% | Mostra que as blendshapes que já temos funcionam como sinal |
| [koreashin/Driver_monitoring](https://huggingface.co/koreashin/Driver_monitoring) (Apache-2.0) | Video Swin Transformer, 30 frames, 5 classes (normal, sonolento, celular...) | 98,05% | 27,8 milhões de parâmetros; treinado com GPUs de servidor; pesado demais para nós |

Estes são o caminho natural do "trabalho futuro" que o TCC já prevê (classificador treinado em cima das medidas).

## D. Sistemas completos baseados em regras (iguais ao HELIOS)

| Projeto | O que faz | Por que importa |
|---|---|---|
| [e-candeloro/Driver-State-Detection](https://github.com/e-candeloro/Driver-State-Detection) (MIT, 155 estrelas, atualizado em 08/2026) | MediaPipe Face Landmarker (478 pontos), EAR, PERCLOS (limiar 0,2), direção do olhar e pose da cabeça; estados Normal / Cansado / Dormindo / Olhando para o lado / Distraído | É o projeto aberto mais parecido com o HELIOS. Bom para comparar limiares e citar como trabalho relacionado |
| [anup42/DriveSense](https://github.com/anup42/DriveSense) (Apache-2.0, Android) | ML Kit + MediaPipe no celular; alerta depois de 1,5 s de olho fechado, medido por relógio | Usa tempo em vez de contagem de frames, a mesma correção do nosso P2 |
| [akshaybahadur21/Drowsiness_Detection](https://github.com/akshaybahadur21/Drowsiness_Detection) (MIT, 546 estrelas) | EAR clássico com dlib | O mais popular; versão antiga da técnica |
| [openpilot (comma.ai)](https://github.com/commaai/openpilot) (MIT) | Monitoramento do motorista de um sistema de assistência real | Referência da indústria; o modelo é feito para a câmera infravermelha do aparelho deles e não é fácil reaproveitar |

## Conjuntos de dados públicos (para citar ou testar)

- **UTA-RLDD**: cerca de 30 horas de vídeo de 60 pessoas, o maior conjunto realista ([página](https://www.v7labs.com/open-datasets/uta-rldd)).
- **NTHU-DDD** e **YawDD** (bocejo): os mais citados nas revisões.
- **MRL Eye Dataset**: recortes de olhos, usado pelo modelo da OpenVINO.

As revisões recentes (por exemplo, [esta da MDPI, 2025](https://www.mdpi.com/2076-3417/15/16/9018)) mostram a área migrando de regras para redes CNN+LSTM, mas com o mesmo problema: acurácia alta no próprio conjunto de dados e queda quando se muda de conjunto.

## Recomendação para o HELIOS

Seguindo a regra de uma etapa por vez e o cronograma (R4S1 termina em 11/10):

1. **Agora (sem mudar nada):** terminar a Tarefa 1 (ligar o protótipo ao `webcam_api.py`). Nenhum modelo desta lista substitui essa etapa.
2. **Melhoria barata (R4S2, depois da validação inicial):** a regra "EAR **ou** ECF" já existe. O que falta é usar o `jawOpen` como segundo sinal de bocejo (P6) e conferir nos vídeos se o limiar de 0,40 do ECF é bom. Não precisa baixar nada. Mudar um limiar é mudança de lógica, então a equipe precisa ser avisada (o texto do TCC cita os limiares).
3. **Para o texto do TCC:** citar o e-candeloro, o modelo da OpenVINO e o artigo das blendshapes como trabalhos relacionados. Explicar por que o HELIOS não usou um classificador de foto inteira (itens 1 a 3 da seção B).
4. **Trabalho futuro:** um modelo temporal (LSTM) treinado com as medidas que o HELIOS já calcula, como o Tandon-A ou o artigo das blendshapes.

## Limites desta pesquisa

- O Exa (busca do Agent Reach) atingiu o limite gratuito depois de 2 buscas; o restante foi feito pelas APIs do Hugging Face e do GitHub e pela busca web.
- GitHub sem login (`gh auth login` não feito): só busca pública de repositórios, sem busca dentro do código.
- Twitter, Reddit e similares não estão configurados no Agent Reach, então discussões da comunidade não foram consultadas.
- Nenhum modelo foi baixado nem testado.
