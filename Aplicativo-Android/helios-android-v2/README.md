# HELIOS: aplicativo Android (versão 0.4.10)

Aplicativo de detecção de sonolência e distração ao volante. Roda inteiro no carro, sem internet: a câmera (do próprio aparelho ou uma ESP32-CAM) manda as imagens, o MediaPipe encontra os 478 pontos do rosto e o domínio em Kotlin decide o estado do motorista, dispara o alarme, fala o alerta e guarda a análise da viagem.

Esta pasta junta o app Kotlin da pasta `Aplicativo-Android` do repositório com o que só existia no protótipo Python (estado DESATENTO, direção do rosto, padrão sonoro de desatenção e foto nos alertas), mais as integrações escolhidas na análise de projetos externos. O repositório original não foi alterado.

## Pipeline

```
Captura de imagem → Extração de características → Análise dos sinais → Classificação do estado → Alerta
 (CameraX / ESP32,   (MediaPipe: EAR, MAR, ECF,     (+ acelerômetro      (ATENTO, FADIGA, CRÍTICO,   (bipe que escala,
  pool de bitmaps,    pose da cabeça)                do aparelho)         DESATENTO, DISTRAÍDO)        voz, vibração, buzzer)
  realce OpenCV)                                                                    ↓
                                                     sessão: tempo em cada estado, eventos, janelas de 30 s,
                                                     fotos dos alertas, histórico em SQLite, CSV
```

| Etapa | Onde está |
|-------|-----------|
| Captura | `app/.../adaptadores/camera/FonteCameraX.kt` + `PoolBitmaps.kt`, `app/.../adaptadores/esp32/Esp32.kt` |
| Realce (OpenCV, opcional) | `app/.../adaptadores/opencv/RealceOpenCv.kt` (CLAHE no canal L) |
| Extração | `app/.../adaptadores/mediapipe/DetectorMediapipe.kt` + `protocolos/MapeadorFaceMesh.kt` + `dominio/geometria` |
| Análise | `dominio/sinais`, `dominio/eventos`, `dominio/inercial` |
| Classificação | `dominio/classificacao/Classificador.kt`, orquestrado por `dominio/monitor/MonitorMotorista.kt` |
| Alerta | `protocolos/Alarme.kt` (padrão, escalada e falas) + `app/.../adaptadores/alarme`, `app/.../adaptadores/voz` |
| Sessão e análise da viagem | `dominio/sessao/Sessao.kt` (agregador, janelas de 30 s, regra das fotos) |
| Histórico, fotos e CSV | `protocolos/Registros.kt` (CSV, nomes, retenção) + `app/.../adaptadores/banco`, `app/.../adaptadores/fotos` |

## O que mudou na versão 0.4.10

**Assistente de instalação redesenhado e sem travar (ADR-013, revisão).** No teste no celular, o assistente ficava preso em "Pare o carro para instalar a câmera", sem botão: com o celular na mão, o sensor nunca volta a "parado". Agora o movimento só mostra um aviso no topo, e a calibração continua exigindo a câmera parada. A tela ganhou um trilho com os cinco passos, um desenho do painel visto do motorista (onde fica a câmera e para onde olhar em cada lugar do carro), a ação principal sempre à vista e a nota num anel com as três partes em barras.

## O que mudou na versão 0.4.9

**Mais dados e personalização (Parte D).** Novo `quadros.csv`, desligado por padrão: uma linha por imagem analisada, com estado, motivos, métricas, pose, olhar e zona, as 52 expressões do rosto (blendshapes), latência, CPU ou GPU, aceleração, CNN, perfil e sensibilidade, sem imagem nem pontos do rosto. Só grava com a autorização das fotos, porque as expressões do rosto são dado pessoal (LGPD); o app não usa a internet e o arquivo fica no celular, em Download/HELIOS. Ajustes mostra o tamanho estimado (cerca de 43 MB por hora). Novo `instalacao.csv` (um perfil por linha, com calibração e zonas), exportado com o histórico e com o backup de perfis. O `janelas.csv` ganha `zona_espelho_ms`, `zona_painel_ms`, `zona_outra_ms`, `manobra_ms`, `perfil` e `sensibilidade` (banco na versão 6). Em Ajustes: sensibilidade suave, padrão ou alta (muda só distração e cansaço acumulado, nunca o sono), tolerância do espelho (2, 3 ou 4 s), tempo da manobra (15 a 60 s) e volume mínimo do bipe (50%, 70% ou 100%); o app avisa ao começar a viagem se o volume de alarme do celular estiver no zero.

## O que mudou na versão 0.4.8

**Distração por zonas e manobra (`docs/ADR-014-distracao-por-zonas.md`).** Olhadas para os espelhos e o painel gravados na instalação só viram DISTRAÍDO depois de 3 s seguidos e não somam no tempo fora da via; olhadas para outra direção seguem as regras de antes. Uma zona nunca apaga sinal de sono. Novo botão "Manobra" no Menu: 30 s sem alerta de distração (o de sono continua), registrado no `eventos.csv`. Eventos de cabeça virada e rosto ausente ganham `yaw_max_graus`. A tela diz a zona: "Olhando o espelho esquerdo há 4 s".

## O que mudou na versão 0.4.7

**Instalação da câmera e calibração obrigatória (`docs/ADR-013-instalacao-e-calibracao.md`).** Sem instalação, a viagem não começa: o app abre num assistente de cinco passos, só com o carro parado (posição da câmera, enquadramento ao vivo com cinco verificações e dicas, calibração de 10 s olhando a estrada, zonas do retrovisor, dos espelhos e do painel, e um resumo com nota de 0 a 100). Instalação com nota abaixo de 60 não pode ser usada. Cada instalação vira um perfil (vários carros ou motoristas), com backup em arquivo em Ajustes › Instalação da câmera. Toda viagem começa com uma recalibração automática de 5 s. A calibração antiga foi descartada. Investigação da CNN (adendo do ADR-011): não era bug do app; o modelo não generaliza para óculos, pouco contraste e ruído.

## O que mudou na versão 0.4.6

**Nova tela de direção (Fase 2 do `docs/ADR-012-interface.md`).** Selo de estado com ícone, palavra e cor no terço de cima; abaixo, o motivo principal em linguagem simples ("Olhar fora da via há 3 s"); embaixo, PERCLOS 60 s, piscadas por minuto, tempo de viagem e o Menu de 76 dp, cujo lado se escolhe em Ajustes › Tela de direção. O visor ao vivo virou uma miniatura que só aparece com o carro parado e amplia ao toque; o olho animado saiu. O `TelaDirecaoTest` (Robolectric, na JVM) mede os alvos de toque, o tamanho e o comprimento dos textos e o visor em movimento, no celular e na central. Os testes do app rodam em Java 21 (o Gradle baixa o JDK sozinho).

## O que mudou na versão 0.4.5

**Tokens de design (Fase 1 da nova interface, `docs/ADR-012-interface.md`).** As cores saíram do app para `protocolos/Cores.kt`, com um tema escuro (padrão) e um claro (Ajustes › Tela de direção › "Tema claro"). O `CoresTest` confere o contraste WCAG 2.1 de todos os pares de texto e fundo (4,5:1) e de borda (3:1) nos dois temas. Cada estado ganhou um ícone próprio, além da palavra e da cor. A fonte passou a ser a Atkinson Hyperlegible Next, que vai dentro do APK. Correções: Ajustes e Histórico mostravam texto preto sobre o fundo escuro, e o vermelho do CRÍTICO não passava no contraste. Capturas de antes e depois em `docs/interface/fase1/`, geradas por `gradle :app:updateDebugScreenshotTest`.

## O que mudou na versão 0.4.4

**CNN do olho em modo sombra.** Ajustes › Análise › "Rede neural do olho (experimental)", desligada por padrão. Ligada, a rede treinada pelo grupo (`scripts/experimento_olhos`) recebe o recorte de cada olho a cada quadro e diz a probabilidade de ele estar fechado. Ela **não participa da classificação nem do alarme**: o valor vai para o visor, para a foto anotada e para o `janelas.csv`, e um teste prova que a mesma sequência de quadros dá os mesmos estados e motivos com e sem ela. Se a rede entra na regra é decisão para uma ADR futura, depois dos números (veja `docs/ADR-011-cnn-olho-modo-sombra.md`).

- Visor e foto anotada: a linha "CNN: dir. 0,93 · esq. 0,91" e, no canto superior direito, os dois recortes 64x64 que a rede viu, com a borda verde (aberto) ou vermelha (fechado) pela decisão dela (corte 0,5).
- `janelas.csv`: `cnn_fechado_media` e `cnn_discordancia`, vazias com a CNN desligada.
- Runtime: LiteRT 1.4.2 (`Interpreter`), só na CPU, sem Google Play Services. O modelo fica em `assets/olho_cnn.tflite`, sem compressão e com o SHA-256 conferido no build, como o `face_landmarker.task`.
- Modelo: a rede `media` do experimento (288.353 parâmetros, 303 KB em int8), treinada só com a CEW; no teste, F1 de 94,7% e AUC de 0,985 por olho.
- APK de release: cresceu 7.470.786 bytes (cerca de 7,1 MiB) em relação à 0.4.3. O runtime nativo pesa 4,5 MB no arm64-v8a e 2,6 MB no armeabi-v7a; o modelo, 0,3 MB. Com AAB ou APK dividido por ABI, um celular arm64 baixa uns 4,8 MB a mais.
- Para medir o custo da rede no aparelho: duas viagens de 2 minutos no mesmo celular, visor desligado nas duas, uma com a CNN ligada e outra desligada; compare `fps_medio`, `latencia_media_ms` e `latencia_p95_ms` do `janelas.csv`.

**Correção: realce OpenCV.** Ligar "Realce OpenCV" fazia o app fechar ao abrir (`UnsatisfiedLinkError`): o `RealceOpenCv` criava os `Mat` antes de carregar a biblioteca nativa do OpenCV. Agora carrega primeiro.
- Banco na versão 4: as duas colunas novas da janela ficam vazias (NULL) nas viagens gravadas antes.

## O que mudou na versão 0.4.3

**CSV com tempo por estado, fps e latência.** O `janelas.csv` passou a trazer, em cada janela de 30 s, o tempo em cada estado e o desempenho medido durante a viagem; o `eventos.csv` passou a trazer o estado em que o evento começou. As colunas estão explicadas em "Exportação CSV", na seção da versão 0.4.0. A classificação, o alarme e as telas não mudaram.

O banco foi para a versão 3. Viagens gravadas antes desta versão continuam no histórico: as janelas antigas exportam zero nas colunas novas e os eventos antigos saem com `estado_inicio` vazio.

## O que mudou na versão 0.4.2

**Tamanho do texto na tela de direção.** Em Ajustes › Tela de direção há três opções de escala do texto: normal, grande e muito grande (1×, 1,25× e 1,5×). A opção resolve o caso de centrais multimídia montadas mais longe do motorista, onde a leitura de relance é difícil.

**Extração de `GravacaoViagem`.** A lógica de sessão, eventos, janelas de 30 s, fotos e retenção saiu do `HeliosViewModel` para uma classe própria (`GravacaoViagem`). O ViewModel foi de 830 para 653 linhas. O comportamento do app não mudou.

**Testes JVM de `GravacaoViagem`.** Quatro casos com `StandardTestDispatcher`, sem Robolectric:
- abre sessão na primeira leitura;
- não registra com a viagem encerrada;
- `finalizar` devolve o id e grava a janela parcial;
- retenção apaga o arquivo antes do registro no banco.

Para habilitar os testes sem Android, `RepositorioAjustes` passou a implementar a interface `UsoMonitoramento` (dois métodos: `usoTotalMs` e `salvarUsoTotal`) e `GravacaoViagem` recebe a diferença epoch/elapsedRealtime como parâmetro injetável.

## O que mudou na versão 0.4.1 (fotos que não apareciam)

No teste da 0.4.0 nenhuma foto foi gravada. A causa mais provável: a autorização das fotos só era guardada ao tocar em "Salvar" nos Ajustes, e sair pelo voltar descartava a mudança. Agora:
- na primeira abertura, depois da permissão da câmera, o app pergunta se pode guardar fotos. Ao permitir, liga as fotos na pasta `Pictures/HELIOS` e grava na hora uma foto de teste (`helios-teste-...jpg` e a versão `-analise`), o que cria a pasta e prova que a gravação funciona;
- os ajustes de fotos valem no mesmo instante, sem precisar do "Salvar";
- Ajustes › Fotos dos alertas tem o botão "Gravar foto de teste agora" e mostra quantas fotos estão guardadas, onde e qual foi a última; o painel do Menu mostra a mesma linha;
- com a retenção "Ao encerrar cada viagem", os Ajustes avisam que as fotos somem ao encerrar.

No Android 10 ou superior (o mínimo do app) gravar na pasta Pictures/HELIOS não exige permissão de armazenamento do sistema: a pergunta do app é o consentimento do motorista (LGPD).

## O que mudou na versão 0.4.0 (depois do segundo teste no celular)

**Menu fixo.** O botão "Encerrar viagem" aparecia e sumia porque o app escondia o menu sempre que o acelerômetro passava de 0,35 m/s², o que acontece com o celular na mão ou o motor ligado parado. Agora há um botão "Menu" sempre no canto inferior direito. Ele abre um painel com os números da viagem em andamento (tempo monitorado, tempo em alerta, cochilos, bocejos, distrações, PERCLOS médio e piscadas no último minuto) e as ações:
- Pausar e Retomar: para a câmera e o alarme numa parada para descanso, sem fechar a viagem;
- Encerrar: segure o botão por 2 s (a barra vermelha enche), para um toque sem querer não acabar com a viagem;
- Histórico e Ajustes: só com o carro parado, mas o sensor nunca tira você de uma tela que já está aberta.

O sensor de movimento ganhou histerese: liga acima de 0,5 m/s² e só volta a "parado" depois de 8 s seguidos abaixo de 0,25 m/s². Depois de encerrar, a próxima viagem começa pelo Menu › Nova viagem, e não mais sozinha ao voltar do histórico.

**Pasta fixa para fotos e CSV.** Ligar o consentimento não abria o seletor de pasta, e sem pasta nenhuma foto era gravada. Agora, com o consentimento ligado, o app grava sozinho na memória interna do celular:
- fotos em `Pictures/HELIOS`, que aparecem na galeria, no app Arquivos e no PC pelo cabo USB;
- CSV em `Download/HELIOS`.

Não precisa de permissão nem de seletor (o Android cria a pasta pelo MediaStore). Em Ajustes dá para trocar por outra pasta e voltar para a pasta HELIOS. Encerrar a viagem não apaga nada, exceto com a retenção "Ao encerrar cada viagem".

**Fotos no histórico.** O resumo da viagem lista os eventos (microssono, sono, bocejo, cabeça caída, rosto fora, cabeça virada, olhar fora da via) com a hora, a duração e a miniatura da foto daquele momento (a versão com a análise desenhada). Tocar na miniatura abre a foto em tela cheia. As fotos de alertas de fadiga acumulada, que não têm um evento único, aparecem em "Outras fotos".

**Análises novas.** O app lia 18 dos 478 pontos e 3 dos 52 blendshapes do MediaPipe. Passou a ler 20 pontos (os centros das íris) e 9 blendshapes:

| Análise | Como funciona | Leva a |
|---|---|---|
| Direção do olhar | Posição da íris entre os cantos do olho (horizontal) e os blendshapes de olhar para cima e para baixo (vertical), sempre em relação à calibração | DISTRAÍDO com o olhar fora da via por 3 s seguidos, ou 10 s somados em 30 s (faixas de distração da Euro NCAP) |
| Piscadas por minuto | Quantidade e duração média das piscadas no último minuto | FADIGA com 5 piscadas ou mais e média de 250 ms ou mais |
| Olhos semicerrados | Abertura do olho abaixo de 80% da calibrada, sem o músculo de apertar o olho (eyeSquint) ativo | FADIGA com os olhos semicerrados em metade do último minuto |

Cuidados para uma análise não atrapalhar a outra:
- com os olhos fechados o olhar não é medido, então sono nunca vira distração;
- com o olho baixo o olhar vertical é ignorado, porque o blendshape de olhar para baixo sobe junto com a pálpebra;
- apertar o olho contra o sol (eyeSquint alto) não conta como olho semicerrado.

A calibração agora também guarda para onde a íris aponta olhando a estrada. Com o celular de lado no painel, olhar a estrada não é olhar para a câmera, então **recalibre olhando para a estrada**. As janelas de 30 s do CSV ganharam as colunas `olhar_fora` e `semicerrado`, e o visor mostra a íris em âmbar quando o olhar sai da via.

## O que mudou na versão 0.3.0 (depois do primeiro teste no celular)

**Olho fechado em escada.** No teste, fechar os olhos por pouco tempo já disparava o alarme forte. Agora a regra segue as faixas do protocolo de monitoramento do motorista da Euro NCAP (fechamento de 1 a 2 s é microssono, de 3 s ou mais é sono, de 6 s ou mais é motorista que não responde):

| Olhos fechados | O que acontece |
|---|---|
| até 1,5 s | Nada toca. Fechamentos de 0,5 a 1 s contam como piscada lenta e, de 1 s em diante, como microssono; os dois só pesam na FADIGA |
| de 1,5 s a 3 s | ATENÇÃO: bipe curto, grave e baixo (660 Hz, metade do volume) e a voz "Atenção, abra os olhos" |
| 3 s ou mais | CRÍTICO: alarme forte de 2000 Hz e voz |
| 6 s ou mais | Alarme no máximo (o CRÍTICO escala aos 1,5 s e aos 3 s dentro dele) |

A regra antiga que levava ao CRÍTICO quando o PERCLOS de 5 s passava de 40% saiu. Era ela que segurava o alarme alguns segundos depois de abrir os olhos. O PERCLOS de 5 s continua calculado e exportado no CSV.

**Cada olho avaliado separado.** Cada olho tem o seu EAR, o seu blendshape de piscada e o seu limiar calibrado (a calibração agora guarda um valor por olho). O app só considera "olho fechado" quando os dois estão fechados, então piscar um olho não conta. Com a cabeça de lado, o olho mais distante aparece encurtado na imagem e o EAR dele deixa de ser confiável: quando um olho tem menos de 60% da largura do outro, só o mais largo decide.

**Volta do motorista.** Quando o motorista sai de um alerta urgente (CRÍTICO, ATENÇÃO, DESATENTO ou DISTRAÍDO), o alarme para no frame seguinte e toca um som curto de duas notas subindo. Depois de um CRÍTICO, o app fica 2 min em FADIGA "cochilou há pouco", com um bipe suave a cada 20 s e a voz pedindo uma pausa a cada minuto, porque quem cochilou continua em risco. Dois microssonos em 5 min também levam à FADIGA.

**Visor de análise.** Na tela de direção aparece a imagem que o MediaPipe acabou de analisar, com o desenho por cima:
- contorno do rosto, sobrancelhas, íris, nariz e boca;
- os dois olhos, verdes quando abertos e vermelhos quando fechados, cada um pelo seu estado;
- os 18 pontos usados no cálculo, em branco;
- os eixos da cabeça a partir da ponta do nariz (x vermelho, y verde, z azul).

Ao lado fica o painel com o EAR e a piscada de cada olho, qual olho foi avaliado, há quanto tempo estão fechados, MAR, abertura da mandíbula, pitch, yaw, roll e PERCLOS de 5 s e de 60 s. Liga e desliga em Ajustes > Análise. A imagem não é espelhada: aparece como a câmera vê.

**Fotos dos alertas.** A foto não é analisada depois de gravada; toda a análise acontece ao vivo. Ao entrar num alerta, o app grava duas fotos:
- a crua, do frame original;
- a `-analise`, com a malha, os eixos e o painel de métricas desenhados, que mostra por que o app alertou.

Cada uma conta como uma foto no limite de quantidade, então 100 fotos equivalem a 50 alertas.

**Proteções acrescentadas nesta versão** (não pedidas, ficam registradas para a equipe decidir):
- se a câmera perde o rosto por mais de 1 s, o tempo sem rosto não conta como olho fechado nem como bocejo;
- se o rosto some por instantes durante um alerta, o alarme continua como estava, em vez de parar e voltar;
- um solavanco durante a FADIGA pós-sono não vira CRÍTICO;
- uma falha ao desenhar o visor nunca derruba o alarme.

## O que mudou na versão 0.2.0

**Estados novos.** DESATENTO quando o rosto some por 2 s ou mais (regra do protótipo Python) e DISTRAÍDO quando a cabeça fica virada mais de 20° para o lado por 2 s, medido em relação ao yaw calibrado. A ordem de prioridade é CRÍTICO, DISTRAÍDO, FADIGA e ATENTO; DESATENTO vale quando não há rosto. Os dois usam o padrão sonoro de desatenção do Python (1200 Hz, 150 ms de bipe, 850 ms de silêncio). Esses valores ainda não foram validados com motoristas e mudam o texto do TCC: a equipe precisa saber.

**Alarme que escala.** Se o motorista não reage, aos 5 s o volume sobe e o silêncio entre bipes cai pela metade; aos 10 s, de novo. A ideia vem do monitoramento do openpilot.

**Voz em português.** Depois do bipe, o app fala a frase do alerta ("Acorde! Pare o carro em um lugar seguro.") pelo TextToSpeech do próprio Android e repete a cada 12, 8 ou 4 s conforme o nível. Pode ser desligada nos ajustes. Para funcionar sem internet, a voz pt-BR precisa estar baixada no aparelho (Configurações > Sistema > Idioma > Saída de texto para voz).

**Análise da viagem.** Cada viagem vira uma sessão com tempo em cada estado, eventos por tipo e uma janela a cada 30 s com PERCLOS, piscadas por minuto, duração média da piscada, bocejos por minuto e desvio do pitch. Fica em SQLite, no armazenamento privado do app. A tela de histórico (só com o carro parado) mostra a lista de viagens, o resumo, o gráfico do PERCLOS e a nota KSS que o motorista dá ao fim.

**Fotos dos alertas.** Com o consentimento ligado e uma pasta escolhida pelo seletor do Android (Downloads, por exemplo), o app grava uma foto ao entrar em CRÍTICO, FADIGA, DESATENTO ou DISTRAÍDO, no máximo uma a cada 30 s. O limite padrão é de 100 fotos e a retenção pode ser 2 h ou 3 h de uso, ao encerrar cada viagem ou só pela quantidade. Ajustes > Dados guardados tem o botão que apaga todas as fotos e o histórico. Numa pasta pública a galeria e o backup do celular também veem as fotos; isso precisa constar no TCC por causa da LGPD.

**Exportação CSV.** Separador vírgula e ponto decimal, para ler direto no Python (colunas da versão 0.4.4):

- `helios-eventos-*.csv`: `sessao,inicio,duracao_ms,tipo,estado_inicio,estado_fim,perclos_5s,perclos_60s,foto`
- `helios-janelas-*.csv`: `sessao,inicio,perclos_60s,piscadas_min,duracao_media_piscada_ms,bocejos_min,desvio_pitch,olhar_fora,semicerrado,kss,tempo_atento_ms,tempo_atencao_ms,tempo_fadiga_ms,tempo_critico_ms,tempo_distraido_ms,tempo_desatento_ms,tempo_sem_rosto_ms,fps_medio,latencia_media_ms,latencia_p95_ms,cnn_fechado_media,cnn_discordancia`

O horário sai em ISO 8601 com fuso (`2026-10-14T23:10:05.123-03:00`). A coluna `foto` traz o nome da foto tirada durante o evento, quando houver.

**Estado do evento.** O evento só é gravado quando termina, então há dois estados:
- `estado_inicio`: o estado da leitura em que o evento começou (para um microssono, a leitura em que os olhos fecharam). Vazio nos eventos gravados antes da 0.4.3.
- `estado_fim`: o estado da leitura em que o evento terminou. É a antiga coluna `estado`, com o mesmo conteúdo. Não serve para dizer quão grave foi o evento: um microssono de 2,6 s passa por ATENCAO, mas na leitura em que os olhos abrem o estado já voltou a ATENTO.

**Tempo em cada estado.** As colunas `tempo_*_ms` somam, em milissegundos, o tempo de cada estado dentro da janela. Cada intervalo entre duas leituras conta para o estado da primeira; intervalos maiores que 1 s (app pausado, câmera caiu) não contam, e um intervalo que cruza a fronteira entre duas janelas é repartido entre elas. A soma das sete colunas é a duração útil da janela, e é ela que deve entrar na distribuição de estados, no lugar da contagem de eventos.

**fps e latência.**
- `fps_medio`: leituras na janela divididas pela duração real da janela, em segundos (a última janela da viagem costuma ser mais curta).
- `latencia_media_ms` e `latencia_p95_ms`: do instante da imagem até a leitura chegar ao domínio (`SystemClock.elapsedRealtime()` no `HeliosViewModel.processar` menos o horário da imagem). O p95 é o valor na posição ⌈0,95 × n⌉ da lista ordenada das n leituras da janela. Com a câmera do aparelho o relógio começa na captura. Com a ESP32-CAM começa quando o JPEG chega ao celular: a captura na placa e a transmissão por Wi-Fi ficam de fora.
- Janela sem leitura: zero nas três colunas.

**CNN do olho (modo sombra).**
- `cnn_fechado_media`: média, nos quadros da janela com a CNN ligada, da probabilidade de olho fechado (média dos dois olhos).
- `cnn_discordancia`: fração desses quadros em que a decisão da CNN (média dos dois olhos maior ou igual a 0,5) discordou do olho fechado do app (EAR, ECF e o olho avaliado, o mesmo critério do estado).
- As duas ficam vazias com a CNN desligada e nas viagens gravadas antes da 0.4.4.

**Desempenho.**
- A câmera copia os pixels para um conjunto de 3 bitmaps reaproveitados em vez de criar 1,2 MB novos a cada frame.
- O mapeador lê só os 18 pontos usados da malha, e não os 478.
- O realce OpenCV escreve no próprio bitmap.

Ajustes > Desempenho liga e desliga o pool e mostra na tela fps, frames descartados, memória e latência entre a captura e o resultado. Assim dá para medir o antes e o depois no mesmo aparelho.

**Encerrar viagem.** Com o carro parado, o rodapé mostra "Encerrar viagem", que para a câmera e abre o resumo. "Voltar à direção" começa uma viagem nova.

**Proteções acrescentadas.** São comportamentos que não foram pedidos explicitamente; ficam registrados para a equipe decidir:
- se nenhuma leitura chega por 3 s (câmera caiu, Wi-Fi da ESP32 perdido), o alarme para e a tela volta a "procurando o rosto";
- um frame com erro no OpenCV ou no MediaPipe é descartado sem derrubar o app;
- a tarefa de build `verificarModelo` falha se o `face_landmarker.task` não for exatamente o testado (SHA-256), ideia tirada do DriveSense.

**Build.** O release leva só as arquiteturas ARM (arm64-v8a e armeabi-v7a), com `shrinkResources`. O debug continua com todas, para o emulador.

**Telemetria do MediaPipe.** Continua bloqueada pelo manifesto. A exclusão do módulo `transport-backend-cct` no Gradle foi testada e descartada: no MediaPipe 0.10.35 o `TaskRunner.create` sempre cria o `RemoteLoggingClient`, que lê `CCTDestination.INSTANCE`, e sem essa classe o app fecharia ao abrir. Isso foi conferido no bytecode da biblioteca.

## Números desta versão

Medidos na compilação, sem aparelho:

| | 0.1.0 | 0.2.0 a 0.4.0 | 0.4.2 |
|---|---|---|---|
| Testes automáticos | 67 | 124 na 0.3.0 e 156 na 0.4.0, todos passando | 160 (+ 4 JVM de `GravacaoViagem`) |
| Cobertura de linhas e ramos (Kover) | 98,1% e 99,1% | 100% nos dois módulos | 100% nos dois módulos |
| APK de release (sem assinatura) | 197 MB | 69 MB | não medido |
| APK de debug (4 arquiteturas) | 231 MB | 239 MB | não medido |

Do APK de release, cerca de 41 MB são do OpenCV e 18 MB do MediaPipe (bibliotecas nativas das duas arquiteturas ARM). O OpenCV ficou por decisão da equipe; trocá-lo por um CLAHE em Kotlin é a maior redução possível.

O que não pôde ser rodado aqui, por falta das ferramentas offline: detekt, Pitest e o app num aparelho. Rode `./gradlew qualidade` no Android Studio e teste no celular antes de citar números de desempenho.

## Como abrir

1. Abra esta pasta (`helios-android-v2`) no Android Studio com suporte a AGP 9.1. Ele usa o SDK já instalado e baixa o que faltar na primeira sincronização.
2. Rode no celular com a câmera frontal virada para o rosto.
3. Em Menu › Ajustes: calibre (5 s olhando para a estrada, como se estivesse dirigindo), escolha se quer voz e, se for testar as fotos, ligue o consentimento. As fotos vão para `Pictures/HELIOS` e os CSV para `Download/HELIOS`.

Testes rápidos: `./gradlew :dominio:test :protocolos:test`. Esteira completa: `./gradlew qualidade`.

## Roteiro de teste no aparelho

1. Olhos abertos por 1 min: ATENTO, sem som.
2. Feche os olhos por 1 s e abra: nada toca (piscada lenta). Feche por 2 s: ATENÇÃO, bipe baixo. Feche por mais de 3 s: ACORDE, alarme forte; continue fechado até 6 s e confira a escalada. Ao abrir: som de retorno e FADIGA "cochilou há pouco" por 2 min.
2b. Pisque um olho só: o visor mostra aquele olho em vermelho, mas o estado não muda.
3. Saia da frente da câmera por 3 s: SEM ROSTO e depois DESATENTO, com o bipe de 1200 Hz.
4. Vire a cabeça para o lado por 3 s: DISTRAÍDO.
5. Três bocejos em poucos minutos: FADIGA.
5b. Olhe para o colo (como para um celular) por 4 s: DISTRAÍDO, "olhar fora da via", com a íris em âmbar no visor. Olhe de relance para o lado várias vezes, 2 s fora e 1 s na estrada, por 20 s: DISTRAÍDO, "muitas olhadas para fora da via".
5c. Mantenha os olhos meio fechados, sem apertar, por 40 s: FADIGA, "olhos semicerrados".
6. Menu › Pausar e depois Retomar: a câmera para e volta na mesma viagem. Menu › segure "Encerrar" por 2 s: confira o resumo com as miniaturas, abra uma foto, dê a nota KSS e exporte o CSV. Veja as fotos na galeria (álbum HELIOS) e o CSV em Download/HELIOS.
7. Com o diagnóstico ligado, anote fps e memória com o pool ligado e desligado (2 min cada).

## Limitações conhecidas

- Os limiares de DESATENTO e DISTRAÍDO (2 s, 20°) e o padrão sonoro deles são propostas, não valores validados.
- Óculos escuros, pouca luz sem infravermelho e câmera fora do eixo do rosto prejudicam a detecção.
- A voz depende do motor de TTS instalado no aparelho.
- A retenção das fotos conta tempo de uso do app, não tempo de relógio.
- Os limiares das análises novas (olhar 0,12 e 0,35, piscada média de 250 ms, olho semicerrado a 80%) são hipóteses da equipe. As faixas de 3 s e 10 s em 30 s são da Euro NCAP, mas o app não distingue olhar o espelho de olhar o celular e conta os dois.
- Na pasta `Pictures/HELIOS` a galeria e o backup do celular (Google Fotos, por exemplo) enxergam as fotos. Se o app for desinstalado e instalado de novo, ele perde o direito de apagar as fotos antigas dessa pasta: apague à mão.

## Skills para o Claude Code

A pasta `.claude/skills/` já traz as skills que este projeto usa, carregadas automaticamente quando o Claude Code é aberto aqui:

| Skill | Origem | Para quê |
|-------|--------|----------|
| `helios-desenvolvimento` | deste projeto | Regras de arquitetura, metas de qualidade e testes |
| `camerax` | android/skills | ImageAnalysis, rotação, testes com fakes |
| `edge-to-edge` | android/skills | Tela cheia na central e no celular |
| `testing-setup` | android/skills | Testes instrumentados e de UI |
| `android-permissions-security` | android/skills | Permissões de câmera, rede e sensores |
| `android-profiler` | android/skills | Medir fps, CPU e temperatura na central (RNF04) |
| `r8-analyzer` | android/skills | APK de release enxuto |
| `agp-9-upgrade` | android/skills | Build com AGP 9 e Kotlin embutido |
| `adaptive`, `styles`, `navigation-3` | android/skills | Telas para tamanhos diferentes, tema, navegação quando o histórico entrar |
| `android-cli` | android/skills | Instalar e atualizar as skills do Android pelo terminal |
| `frontend-design` | anthropics/skills | Direção visual da interface |

Para atualizar as do Android: `android skills update --all --project=.`
