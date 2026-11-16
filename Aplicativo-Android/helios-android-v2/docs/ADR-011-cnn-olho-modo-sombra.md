# ADR-011: CNN do olho em modo sombra

Data: 15/11/2026. Situação: proposta para revisão do grupo.

## Contexto

O experimento `scripts/experimento_olhos` treinou CNNs que dizem se o olho está aberto ou fechado a partir de um
recorte de 64x64 em cinza, e escolheu uma delas pela validação (`comparar_tamanhos.py`, F1 por olho com corte 0,5,
até 1024 KB). O grupo quer ver a rede funcionando no carro antes de decidir se ela entra na regra de olho fechado,
que hoje usa EAR e ECF com a escada de 1,5 s, 3 s e 6 s (ADR-008).

## Decisão

A CNN entra no app em **modo sombra**: roda, grava e aparece no visor, mas não participa da classificação nem do
alarme.

- **Desligada por padrão.** Ajustes › Análise › "Rede neural do olho (experimental)", com o aviso "Só registra e
  mostra; não muda o alarme".
- **Onde roda.** No `DetectorMediapipe`, na thread de resultados do MediaPipe, enquanto o bitmap analisado ainda não
  voltou ao pool. Recorta os dois olhos com as funções puras de `protocolos/OlhoCnn.kt`, iguais ao `recortar_olho`
  do experimento (centro na média dos 16 pontos do contorno, lado de 1,6 x a distância entre os cantos, mínimo de
  8 px, borda replicada, luminância 0,299 R + 0,587 G + 0,114 B, redução por média de área).
- **Até onde o valor vai.** `AmostraFacial` → `Metricas` (campos opcionais; nulo = desligada). `Indicadores` e
  `Classificador` não recebem o valor. Um teste roda a mesma sequência de quadros com e sem a CNN, com a rede
  discordando de propósito do EAR, e exige os mesmos estados, motivos e eventos.
- **Registro.** `janelas.csv` ganha `cnn_fechado_media` e `cnn_discordancia` (fração dos quadros em que a CNN, pela
  média dos dois olhos com corte 0,5, discordou do olho fechado do app). Banco na versão 4.
- **Visor e foto anotada.** A linha "CNN: dir. 0,93 · esq. 0,91" e os dois recortes 64x64 no canto, com a borda
  verde (aberto) ou vermelha (fechado) pela decisão da rede.
- **Runtime.** LiteRT 1.4.2 (`org.tensorflow.lite.Interpreter`), só CPU, uma thread, sem Google Play Services. O
  `tasks-vision` do MediaPipe não roda um `.tflite` qualquer: a API pública só tem tarefas prontas, que exigem
  metadados e fazem o próprio pré-processamento. A documentação do LiteRT recomenda a 2.3.x (`CompiledModel`), mas
  ela foi compilada com Kotlin 2.4 e o projeto usa o Kotlin 2.2.20 que vem no AGP 9.1.1; a mesma página marca a
  1.4.2 como a versão atual do `Interpreter`. Migrar para a 2.x fica para quando o AGP trouxer Kotlin 2.4.
- **Modelo.** `assets/olho_cnn.tflite`, sem compressão (mapeado da memória) e com o SHA-256 conferido no build, como
  o `face_landmarker.task`. É a rede `media` (288.353 parâmetros, 303 KB em int8) treinada só com a CEW, escolhida em
  16/11/2026 pela regra do experimento: F1 de 91,9% na validação; no teste, F1 de 94,7% e AUC de 0,985 por olho.

## Consequências

- O alarme não muda; a CNN só custa tempo de CPU quando está ligada. O fps e a latência das janelas
  (`fps_medio`, `latencia_media_ms`, `latencia_p95_ms`) medem esse custo: viagem de 2 minutos com a CNN ligada e
  outra desligada, mesmo aparelho, visor desligado nas duas.
- O APK de release cresceu 7.470.786 bytes (68.797.277 → 76.268.063, cerca de 7,1 MiB), medido com o mesmo build da
  0.4.3. Quase tudo é o runtime nativo: `libtensorflowlite_jni.so` com 4.475.256 bytes (arm64-v8a) e 2.641.764 bytes
  (armeabi-v7a); o modelo tem 310.208 bytes e o código, 16.716 bytes. Passa da folga de 1 MB pensada para o modelo.
  Com AAB ou APK dividido por ABI, um celular arm64 baixa só uns 4,8 MB a mais.
- Com o realce OpenCV ligado, a CNN recebe o recorte já realçado (CLAHE), diferente das imagens do treino. Para
  medir a rede, deixar o realce desligado; se ele for usado no carro, treinar com o mesmo realce.
- O teste de paridade com `models/olho_cnn_dourado.csv` não entra no repositório: o CSV tem recortes de olhos de
  pessoas reais da base CEW, cuja licença proíbe redistribuir os dados. Ele só deve ir para
  `app/src/androidTest/assets/` se o grupo liberar, e nunca para o git público.
- O modelo foi treinado com a CEW, que só permite uso em pesquisa e proíbe uso comercial. Isso precisa constar no
  TCC. A MRL Eye entrou na comparação, mas não no modelo escolhido.

## Adendo de 16/11/2026: olhos abertos chamados de fechados

**Sintoma.** No teste de 16/11 às 09:28 (celular apoiado na mesa, câmera abaixo do rosto, motorista de óculos), com
os olhos claramente abertos (EAR de 0,25 a 0,30), a CNN deu de 0,53 a 0,89 em média por janela e discordou da regra
em até 99% dos quadros.

**Conferido, nesta ordem:**

| Item | Resultado |
|---|---|
| Escala e ponto zero da entrada, lidos do `olho_cnn.tflite` do APK | `uint8`, escala 1, ponto zero 0: os bytes do recorte entram como estão |
| Bytes | Um canal, 64 x 64, de 0 a 255 |
| Saída | `float32` da sigmoide `prob_fechado`; rótulo 1 = `closedFace` da CEW |
| Cinza | 0,299 R + 0,587 G + 0,114 B, igual ao `cv2.COLOR_BGR2GRAY` |
| Orientação | O app recorta o mesmo bitmap entregue ao MediaPipe, sem rotação à parte; o treino espelha ao acaso |
| Recorte | `OlhoCnnDouradoTest`: 7 imagens desenhadas (redução por fator inteiro e quebrado, borda replicada nos quatro lados, ampliação de olho pequeno, cor, olho inclinado) dão o mesmo recorte do `recortar_olho`, com no máximo 1 nível de cinza de diferença. Com a margem trocada de propósito para 1,5, o teste falha com diferenças de 169 a 199 |
| Ponta a ponta | Uma foto original do app (640 x 480) passada pelo pipeline do experimento deu 0,73 e 0,98; o app imprimiu 0,67 e 1,00 no mesmo instante. A diferença cabe na recompressão da foto |

**Conclusão: não é bug do app.** O próprio pipeline do experimento, sem código do app, dá de 0,71 a 0,99 para 7 de
10 olhos abertos em fotos do autor sem óculos (EAR de 0,25 a 0,35). O modelo acerta 94,8% na CEW de teste, mas não
generaliza para essas imagens. O que os números indicam:

- **Contraste.** Os recortes do autor têm metade do contraste da CEW (desvio padrão do cinza de 19 contra 34). A rede
  chama de fechada uma imagem sem detalhe: cinza uniforme dá 0,65; toda preta, 0,96.
- **Óculos.** Uma armação desenhada sobre os olhos abertos da CEW leva a mediana de 0,03 para 0,96 (92% chamados de
  fechados): a rede aprendeu que um traço escuro e horizontal na região do olho é a pálpebra fechada.
- **Ruído e câmera de baixo.** Ruído de sensor com desvio de 16 leva 80% dos olhos abertos a "fechado"; a câmera de
  baixo para cima, 17%. Óculos, câmera de baixo e ruído juntos: 100%.
- **Base de treino.** Os recortes da CEW são muito borrados (rostos de 100 px ampliados; variância do laplaciano de
  15). Na MRL Eye, que vem de câmeras reais, a mesma rede chama de fechados 28% dos olhos abertos.

**Decisão.** A CNN continua como está, em modo sombra. Para entrar na regra precisaria de treino com óculos, pouco
contraste, ruído e câmera de baixo para cima (por exemplo, dados do próprio grupo com consentimento), e de um novo
teste nestas mesmas condições.

**Achado à parte, na foto anotada.** Os recortes e a malha são do quadro analisado, mas o texto e as cores dos olhos
vêm da última leitura já processada (`ultimaLeitura`), em geral o quadro anterior. Num piscar, a borda do recorte e o
valor escrito discordam (borda verde ao lado de "dir. 0,67"). Fica para a foto anotada da Fase 4 do ADR-012.

## Fora desta decisão

Usar a CNN na regra de olho fechado. Fica para uma ADR futura, com os números de `cnn_discordancia` em viagens reais
e a comparação do capítulo de resultados.

## Adendo de 16/11/2026 (noite): modelo substituído

O modelo descrito aqui (treinado só com a CEW) foi substituído na 0.4.14 pelo retreino do ADR-016, com aumentos da
câmera real e a MRL Eye. O modo sombra e o restante desta decisão continuam valendo.
