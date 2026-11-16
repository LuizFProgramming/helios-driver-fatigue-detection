# Diário de sessões

## 2026-10-18
- Regra fixada: Ponytail, Graphify e agent-skills (VERIFY) obrigatórios neste projeto.
- Grafo gerado do `helios-android-v2` (código, local, sem LLM).
- Auditoria Ponytail e refatorações R1 a R3 no `HeliosViewModel.kt`.
- Pendente: compilar e rodar os testes no Android Studio; decidir sobre a extração de `GravacaoViagem`.

## 2026-10-18 (segunda parte)
- `GravacaoViagem.kt` criado: sessão, eventos, janelas, fotos e retenção saíram do `HeliosViewModel` (830 → 653 linhas).
- `_to_delete/GravadorSaf.kt.antigo` apagado.
- Nova personalização: tamanho do texto da tela de direção (normal, grande, muito grande) em Ajustes › Tela de direção.
- agent-skills (3 agentes, 9 skills) e Ponytail (4 skills) copiados para `helios-android-v2/.claude`; `CLAUDE.md` passou a exigir as ferramentas.
- Grafo refeito: 1083 nós, 2810 ligações.
- Pendente: compilar no Android Studio (prompt pronto em `prompts-claude-code.md`).

## 2026-10-18 (terceira parte)
- Compilação validada via Claude Code: `:dominio:test`, `:protocolos:test` e `:app:compileDebugKotlin` — BUILD SUCCESSFUL (46 s).
- Detekt: 55 erros, todos pré-existentes; nenhum introduzido pelas mudanças de hoje.
  - Fontes: `MaxLineLength` espalhado em `adaptadores/` e `ui/`; `LongMethod`/`CognitiveComplexMethod` em `TelaAjustes` e `TelaDirecao` (contagens já ultrapassavam o limite antes das adições de hoje); `MatchingDeclarationName` em `MenuViagem.kt`; `TooGenericExceptionCaught` em `DetectorMediapipe.kt`.
  - Próximo passo separado: resolver detekt (fora do escopo desta sessão).
- Gradlew ausente no repositório; usando o binário em cache (`~/.gradle/wrapper/dists/gradle-9.3.1-bin/…/bin/gradle`). Criar `gradlew` / `gradlew.bat` antes do próximo CI.

## 2026-10-18 (quarta parte)
- Testes JVM de `GravacaoViagem` criados (4 casos, todos verdes, sem Robolectric):
  - `abre sessão na primeira leitura` · `não registra com viagem encerrada`
  - `finalizar devolve o id e grava a janela parcial` · `retenção apaga arquivo antes do registro`
- Mudanças de produção mínimas para habilitar os testes:
  - `Ajustes.kt`: interface `UsoMonitoramento` (2 métodos); `RepositorioAjustes` implementa-a.
  - `GravacaoViagem.kt`: aceita `UsoMonitoramento` em vez de `RepositorioAjustes`; parâmetro `epochOffsetMs: () -> Long` (default = `SystemClock`-based, injetável em testes).
  - `libs.versions.toml` + `app/build.gradle.kts`: `kotlinx-coroutines-test` adicionado.
- Test 4 (retenção) usa `Retencao.AO_ENCERRAR` + banco pré-semeado: exercita `excluirFotos` via `finalizar()` sem Bitmap.

## 2026-11-13
- Firmware da ESP32-CAM unificado: o `.ino` e o `src/main.cpp` tinham virado dois programas. Agora há uma fonte só em `firmware/esp32-cam/arduino/helios_cam/` (também `src_dir` do PlatformIO), com OLED, buzzer ativo/passivo no GPIO 13, `X-Timestamp` no MJPEG e modo demonstração opcional.
- Removidos do código o nome e a senha da rede de casa que entraram no commit `e291eb7`; o modo estação lê `secrets.h` (no `.gitignore`). A senha continua no histórico do git: trocar a senha do roteador.
- Compilação verificada com arduino-cli e core esp32 2.0.17 nas variantes de buzzer, demo e modo estação, sem avisos. Não testado na placa.
- Experimento olho aberto/fechado em `scripts/experimento_olhos/` (EAR, ECF, regra do app, CNN MobileNetV2), 18 testes; pipeline rodado de ponta a ponta com dados sintéticos. Falta rodar com a base real (CEW) e conferir a licença.
- Ferramentas de E:\ai-tools (Graphify, Ponytail) não disponíveis nesta sessão (computador não conectado); grafo não regenerado.

## 2026-11-15
- App 0.4.3 (versionCode 7): o CSV passou a trazer o que o capítulo de resultados precisa.
  - `janelas.csv`: `tempo_atento_ms` ... `tempo_sem_rosto_ms` (mesma regra de lacuna do `AgregadorSessao.acumularTempo`, intervalo repartido na fronteira entre janelas), `fps_medio`, `latencia_media_ms`, `latencia_p95_ms` (posto ceil(0,95 n)).
  - `eventos.csv`: `estado_inicio` (novo, vem de `AgregadorSessao.estadoEm`) e `estado_fim` (a antiga `estado`, mesmo conteúdo).
  - Latência medida no `HeliosViewModel.processar` com `elapsedRealtime` lido numa variável local (o vigia de sinal zera `ultimaLeituraMs` em outra corrotina). Com a ESP32-CAM o relógio começa na chegada do JPEG.
  - Banco versão 3: SQL foi para `EsquemaBanco` (mesmo arquivo do `BancoHelios`); ALTER TABLE com DEFAULT. Teste roda o SQL num SQLite de verdade com `sqlite-jdbc` (só `testImplementation`).
- Achado para o TCC: no monitor real, um microssono de 2,6 s passa por ATENCAO mas começa e termina em ATENTO (o alarme cai na leitura em que os olhos abrem). `estado_fim` não mede a gravidade do evento; usar o tempo por estado das janelas.
- Testes: 179 (dominio 120, protocolos 52, app 7), todos verdes. Antes: 162.
- `qualidade` continua vermelha por pendências anteriores a esta sessão, nenhuma nova: detekt (dominio 25, protocolos 25, app 56; antes 25/29/57), `:protocolos:koverVerify` com ramos em 98,75% (falta 2 ramos em `Alarme.kt:113`; antes 98,70%), Pitest abaixo de 100% (dominio 623/694, protocolos 249/287). `:dominio:koverVerify` passa (100% linha e ramo). Nenhum mutante novo sobrevivente nos arquivos tocados.
- Grafo refeito: 1175 nós, 3046 ligações. Criado `.graphifyignore` para deixar `.claude/` fora (as skills somavam 1090 nós).

## 2026-11-15/16 (prompts 1 e 2: tamanhos de CNN e CNN em modo sombra)
- Prompt 1 (`scripts/experimento_olhos`): `treinar_cnn.py` com `pequena`, `media`, `mobilenet` e `mobilenet_100`; `--mrl` só no treino; tempo estimado antes de treinar; `mobilenet_100` com MRL limitada a 10 épocas. `comparar_tamanhos.py` novo, com `--retomar`. Correção da calibração int8 (recorte preto e branco: escala de entrada 1, ponto zero 0; o teste pegou escala 0,741 antes). 44 testes Python.
- Bases baixadas: CEW 100x100 (2.423 rostos, termos: só pesquisa, sem redistribuir) em `data/raw/CEW`; MRL Eye (84.898 recortes, sem licença declarada) em `data/raw/MRL`. `.venv-experimento` com Python 3.10 e TensorFlow 2.18.
- Resultado: escolhida a `media` treinada só com a CEW (303 KB; F1 91,9% na validação; teste F1 94,7%, AUC 0,985). A MRL piorou as redes. As MobileNet vão bem no Keras (AUC 0,985 a 0,989) mas quebram no int8 (dizem "fechado" para quase tudo); a calibração da entrada não é a causa. `mobilenet_100` com MRL não foi treinada: o Claude Code interrompeu o processo por falta de memória; as outras sete foram medidas com `--retomar`.
- Prompt 2 (app 0.4.4, versionCode 8): CNN do olho em modo sombra. LiteRT 1.4.2 (`Interpreter`, CPU): o 2.3.2 exige Kotlin 2.4 e o projeto usa 2.2.20 do AGP 9.1.1. Recorte puro em `protocolos/OlhoCnn.kt` (igual ao `extrair.py`, arredondamento meio-para-par). Valor vai até `Metricas`, não até `Indicadores`; teste prova mesmos estados, motivos e eventos com e sem CNN. `janelas.csv` com `cnn_fechado_media` e `cnn_discordancia`; banco versão 4. Visor e foto: linha "CNN: dir. · esq." e os dois recortes 64x64 com borda pela decisão. ADR-011.
- APK de release: +7.470.786 bytes (runtime nativo 4,5 MB arm64 e 2,6 MB armv7; modelo 0,3 MB).
- Correção antiga achada no teste do usuário: ligar o realce OpenCV fechava o app (`Mat` criado antes de carregar a biblioteca nativa). Corrigido em `RealceOpenCv.kt`.
- CSVs do teste do usuário (16/11 00:04): tempos por estado somam 30.000 ms nas janelas cheias; `estado_inicio` mostra a cabeça caída que levou à fadiga. Falta a última janela da 3ª viagem (exportada com a viagem aberta, a parcial só é gravada ao encerrar); evento em curso no fim da viagem se perde.
- Testes: 197 (dominio 123, protocolos 66, app 8), todos verdes. `qualidade` ainda vermelha só por pendências anteriores: detekt 25/25/56 (igual à base), Kover protocolos 182/184 ramos (`Alarme.kt:113`), Pitest dominio 644/715 e protocolos 332/370. Kover dominio 100%.
- Grafo refeito: 1248 nós, 3251 ligações.
- Não feito (precisa do celular): as duas viagens de 2 minutos com a CNN ligada e desligada e o teste instrumentado de paridade (o CSV dourado não foi liberado e a licença da CEW proíbe redistribuir).
- Pedido de redesenho da análise (visor e foto) foi para o prompt 3, direção "etiquetas no rosto".
- `.claude/settings.local.json` (versionado) recebeu liberações de git, cp, graphify, python do venv e `shutdown.exe /s /t 300`; não foi commitado.

## 2026-11-16 (interface, Fase 1: tokens de design)
- Pedido em 4 fases (direção, menu/histórico/ajustes, modo análise e foto), cada regra com fonte no `docs/ADR-012-interface.md`. Fonte escolhida: Atkinson Hyperlegible Next.
- Capturas no PC com o screenshot test oficial do Compose (`com.android.compose.screenshot` 0.0.1-alpha16, funciona com o AGP 9.1.1): `app/src/screenshotTest/CapturasTelas.kt`, 9 telas, celular 915x412 e central 1280x720, temas escuro e claro. Antes e depois em `docs/interface/fase1/`.
- Achados: Ajustes e Histórico mostravam texto preto sobre o fundo escuro (sem `Surface`, o Material 3 usa preto); o vermelho do CRÍTICO dava 3,77:1 e o cinza "sem sinal" 3,01:1; o rodapé com 65% de opacidade dava 3,54:1 no CRÍTICO.
- `protocolos/Cores.kt`: `EsquemaCores` (`TEMA_ESCURO`, `TEMA_CLARO`), `Tom` e `SINAIS` (iguais nos dois temas), `contrasteWcag`, `misturar`, `corDoPulso`. Primary passou de âmbar (cor da FADIGA) para o azul das placas de serviço. `Paleta` apagada; `EstadoTela.corDestaque` virou `tom` e `icone`.
- App 0.4.5 (versionCode 9): `Tema.kt` com o esquema do Material 3 por papel, tipografia na Atkinson (mínimo de 14 sp), `Tipos.principal` 32 sp, `secundario` 24 sp, `numero` 48 sp, `Espaco`, `Alvo`, formas de 16 a 28 dp; `Surface` no `TemaHelios`. Tema claro em Ajustes › Tela de direção; a `MainActivity` acerta os ícones das barras do sistema. 12 Material Symbols Rounded em `res/drawable`. Palavra do estado com o ajuste automático de tamanho do Compose (mínimo de 32 sp).
- Testes: 208 (dominio 123, protocolos 77, app 8), todos verdes; `CoresTest` com 12 casos. detekt igual à base (protocolos 25, app 56); Kover de protocolos só com os 2 ramos do `Alarme.kt:113`; Pitest de protocolos 399/437, com os mesmos 38 vivos de antes e nenhum no `Cores.kt`.
- Grafo refeito: 1344 nós, 3492 ligações.
- Não commitado. Próximo: plano da Fase 2 (tela de direção).

## 2026-11-16 (interface, Fase 2: tela de direção)
- Fase 1 commitada (`cc06a2c` e `1ee6e74`). Decisão do CRÍTICO (selo em tela cheia ou só no terço de cima) ficou com o grupo; mantido o comportamento atual (tela cheia, só o selo pulsa), num ponto só (`telaCheia`).
- Base de teste de interface na JVM: Robolectric 4.18 (Android 16), `ui-test-junit4`, `junit-vintage-engine`; testes do app em Java 21 por toolchain (o código segue em Java 17); `@GraphicsMode(NATIVE)`, porque no modo legado o Robolectric não mede texto.
- `TelaDirecaoTest` (15 casos): todo alvo clicável ≥ 76 dp e sem sobreposição, nenhum texto < 24 sp nem > 120 caracteres (celular e central), palavra e motivo ≥ 32 sp, selo com palavra e ícone, visor só parado, ampliação não reabre sozinha, diagnóstico só parado, três indicadores, Menu nos dois lados, Menu inteiro com o texto ×1,5. Texto de tamanho automático medido pela largura (a semântica não traz o tamanho escolhido). Cada medida conferida com sabotagem.
- `protocolos`: `EstadoTela.apoio` (um motivo, maiúscula, "há N s" ou "há N min" nos alertas urgentes) e `piscadasPorMinuto`; saíram `cronometro` e `olhoFechado`. Trocas para manter Kover e Pitest: `minByOrNull`, `replaceFirstChar` e `?.let ?:` geravam ramos impossíveis.
- App 0.4.6 (versionCode 10): três zonas (chips, selo até 1/3, apoio; faixa com PERCLOS 60 s, piscadas/min, tempo de viagem e Menu de 76 dp), miniatura do visor 112 x 84 dp só parado, olho animado e painel de métricas fora. `GravacaoViagem.duracaoMs()`, `TelaUi.duracaoViagemMs`, `Ajustes.menuNaEsquerda`.
- Revisão: números com 48 sp competiam com o motivo; reduzidos a 32 sp e motivo na cor principal. Achado e corrigido: a ampliação do visor reabria sozinha quando o carro parava.
- Testes: 227 (dominio 123, protocolos 80, app 24), todos verdes. detekt do app 55 (base 56), protocolos 25; Kover de protocolos só com os 2 ramos do `Alarme.kt:113`; Pitest de protocolos 403/440 (37 vivos, um a menos que a base).
- Capturas da direção em `docs/interface/fase2/depois`; ADR-012 com a Fase 2 e a tabela de verificação. Grafo refeito: 1417 nós, 3700 ligações.
- Não commitado.

## 2026-11-16 (achados do teste das 09:28, Parte A: CNN)
- Fase 2 commitada (`1ddd88a`, `ec8fc63`). Novo pedido em 4 partes: A (CNN), B (instalação e calibração obrigatória), C (distração por zonas), D (dados e personalização).
- Parte A, conclusão: não é bug do app. Contrato do `olho_cnn.tflite` conferido (uint8, escala 1, ponto zero 0; saída `prob_fechado`, 1 = fechado); paridade do recorte provada com dourado sintético (`scripts/experimento_olhos/dourado_sintetico.py` → `protocolos/src/test/resources/dourado_olho`, 7 casos, `OlhoCnnDouradoTest`, diferença máxima 1; sabotagem com margem 1,5 falha); foto original do app no pipeline do experimento deu 0,73/0,98 contra 0,67/1,00 impressos.
- Fotos do autor (E:\TCC-Aplicativo\Helios-Pictures, fora do repo) no pipeline do experimento: 7 de 10 olhos abertos sem óculos com 0,71 a 0,99. Causas medidas: contraste do recorte metade da CEW (19 contra 34); armação de óculos leva olhos abertos da CEW a 92% "fechado"; ruído σ16 a 80%; recortes da CEW muito borrados (laplaciano 15); na MRL, 28% dos abertos viram fechados. CNN fica em modo sombra; adendo no ADR-011.
- Achado à parte: a foto anotada mistura quadros (recortes e malha do quadro atual, texto e cores da `ultimaLeitura`). Fica para a Fase 4 do ADR-012.
- Versão do app não mudou: a Parte A não alterou código de produção. Testes: 229 (dominio 123, protocolos 82, app 24). Grafo: 1429 nós, 3729 ligações. Não commitado.

## 2026-11-16 (Parte B: instalação da câmera e calibração obrigatória, app 0.4.7)
- Parte A commitada (`d10f87d`, `8e07926`). Decisões do grupo: instalação com nota abaixo de 60 bloqueada; calibração antiga descartada; perfis nas preferências do app com backup em arquivo.
- `dominio/instalacao`: `verificar` (rosto inteiro, tamanho 20-60%, brilho 60-220, ângulo por posição, câmera parada), `ColetorZona` (zonas relativas à estrada), `notaInstalacao` (60/25/15, com as partes), `Assistente` (posição, enquadramento, calibração 10 s, zonas, resumo) e `RecalibracaoViagem` (5 s com rosto estável e carro parado; recomeça se faltar amostra). `Calibrador.desvios()`. `CaixaRosto` e caixa/brilho na `AmostraFacial`. Limiares em `ConfiguracaoInstalacao` dentro da `ConfiguracaoDeteccao`. `AnalisadorMovimento.rms()` público (com janela vazia dá 0, antes daria NaN).
- `protocolos`: `ArquivoPerfis` (texto chave=valor; perfil inválido descartado sem perder os outros), contorno do rosto (36 pontos, lidos só com o assistente aberto), `brilhoDoRosto`, `TextosInstalacao`.
- App: assistente no lugar da tela (não por cima: um fundo não bloqueia toque), sem perfil abre sozinho; quadros do assistente não vão para alarme nem gravação; perfis em Ajustes (escolher, renomear, apagar, nova instalação, exportar e importar); recalibração em toda viagem no chip "Calibrando".
- Achados pelos testes e corrigidos: "Refazer" aparecia duas vezes no resumo; o botão de calibrar ficava fora da tela no celular em paisagem; a recalibração travava se o rosto sumisse durante a coleta; ramos impossíveis de funções inline (`all`, `minByOrNull`, `?.let ?:`) e `lateinit` geravam mutantes equivalentes.
- Testes: 283 (dominio 158, protocolos 94, app 31), todos verdes. Kover dominio 100%; protocolos só os 2 ramos do `Alarme.kt:113`. Pitest sem sobrevivente nos arquivos novos (dominio 905/976, protocolos 472/509, iguais ou melhores que a base). detekt dominio 24, protocolos 25, app 54 (base 25/25/55). `TelaInstalacaoTest` conferido com sabotagem (alvo de 40 dp falha).
- ADR-013, README, capturas em `docs/interface/instalacao`. Grafo: 1683 nós, 4673 ligações.
- Não testado no aparelho: assistente com a câmera real, faixas de ângulo, recalibração. Não commitado.

## 2026-11-16 (Parte C: distração por zonas e manobra, app 0.4.8)
- Parte B commitada (`5fde7de`, `39890c5`), branch `feat/cnn-visor-logs`. Decisão: painel tratado como espelho (zona da direção).
- Domínio: `zonaMaisProxima` (distância normalizada, olhar só quando medido); `MonitorMotorista` com zonas do perfil (`definirZonas`): olhada em zona só distrai depois de 3 s (`toleranciaZonaMs`, motivo `OLHAR_ZONA`) e não soma no VATS; outra direção como antes; sem zonas nada muda. Zona nunca apaga sono (cabeça caída para o painel continua CRÍTICO). Eventos de giro e rosto ausente com `yawMaximoGraus`. Manobra (`iniciarManobra`, 30 s): sem DISTRAÍDO e DESATENTO, sono continua; tocar de novo recomeça; evento `MANOBRA`.
- Protocolos: apoio "Olhando o espelho esquerdo há 4 s"; `eventos.csv` com `yaw_max_graus`. App: banco versão 5; botão Manobra de 76 dp no Menu (o `BotaoGrande` do Menu subiu de 64 para 76 dp); chip "Manobra: N s"; monitor recebe as zonas do perfil.
- Qualidade: 297 testes (dominio 168, protocolos 96, app 33). Kover dominio 100%; Pitest igual à base (71 e 37 sobreviventes, nenhum novo); detekt 24/25/54. `processar` dividido (`acompanharOlhada`) para ficar dentro da complexidade.
- ADR-014, README. Não testado no carro. Não commitado.

## 2026-11-16 (Parte D: mais dados e personalização, app 0.4.9)
- Parte C commitada (`b07b077`, `78d6250`); manobra fica como "recomeçar", com o impacto para o motorista e para o TCC no ADR-014.
- Decisões do grupo: sensibilidade muda só distração e fadiga acumulada (sono nunca); piso no volume do bipe e aviso de volume de alarme no zero; `quadros.csv` só com o consentimento das fotos (blendshapes descrevem o rosto, LGPD); app sem internet, dados só no celular.
- Domínio: `Sensibilidade` (suave: olhar 4 s, cabeça 3 s, VATS 12 s, PERCLOS 20%; alta: 2,5 s, 1,5 s, 8 s, 12%); `MetricasOlhar.foraAgora`; janela com `zonaEspelhoMs`, `zonaPainelMs`, `zonaOutraMs`, `manobraMs`; `AmostraFacial.blendshapes`.
- Protocolos: `RegistroQuadros`/`LinhaQuadro` (`quadros.csv`, sem imagem nem pontos), `Csv.instalacao`, colunas novas no `janelas.csv`, `PlanoAlerta.comVolumeMinimo`, `Blendshapes.TODOS` (52), mapeador com todos os blendshapes só com o registro ligado.
- App: banco versão 6; `ContextoLeitura` (perfil, sensibilidade, delegate, aceleração, blendshapes); `quadros.csv` em blocos de 1 s (`criarTexto`/`acrescentar` na porta de arquivos); exportação de CSV e de perfis saiu do ViewModel para a `GravacaoViagem` (limite de tamanho da classe); Ajustes com volume mínimo (50/70/100%), sensibilidade, tolerância do espelho (2/3/4 s), tempo da manobra (15 a 60 s) e registro por quadro (~43 MB/h); aviso de volume de alarme no zero ao começar a captura; botão Manobra mostra o tempo escolhido.
- Qualidade: 310 testes (dominio 172, protocolos 103, app 35). Kover dominio 100%; Pitest igual à base (71 e 37 sobreviventes, comparado num worktree do HEAD); detekt 24/25/54. Capturas de Ajustes regravadas.
- README 0.4.9. Não testado no carro nem no aparelho (gravação em modo de acréscimo "wa" no MediaStore e na pasta escolhida). Não commitado.

## 2026-11-16 (assistente de instalação sem travar e redesenhado, app 0.4.10)
- Parte D commitada (`51cf743`, `70eef8c`).
- Teste no celular: assistente preso em "Pare o carro para instalar a câmera", sem botão. Causa: com o celular na mão o acelerômetro fica "em movimento" (só volta depois de 8 s abaixo de 0,25 m/s²) e, sem perfil, não há Cancelar. Fundo branco com texto preto é o tema claro do app (o escuro é o padrão; preferência possivelmente guardada de teste anterior; aparelho não estava conectado para conferir).
- Decisões do usuário: movimento vira aviso, sem bloquear (a verificação "Câmera parada" segue impedindo calibrar com o celular balançando); redesenho completo do assistente.
- TelaInstalacao: trilho com os cinco passos; desenho do painel visto do motorista (Canvas, proporção fixa) que acende a posição da câmera e o lugar a olhar; ação principal presa embaixo; lado do motorista sob o desenho; nota num anel com as partes em barras. Capturas novas: InstalacaoCelularSeMexendo, InstalacaoCalibrando; docs/interface/instalacao atualizado. ADR-013 com a revisão.
- Qualidade: 311 testes (dominio 172, protocolos 103, app 36); detekt 24/25/54. Não testado no aparelho. Não commitado.
