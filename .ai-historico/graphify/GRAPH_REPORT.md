# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 93 files · ~514,240 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1683 nodes · 4673 edges · 85 communities (52 shown, 33 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 347 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `8e079260`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- CapturasTelas.kt
- planoDeAlerta
- HeliosViewModel
- CoresTest
- Motivo
- GravacaoViagemTest.kt
- MonitorMotorista
- BancoHelios
- AgregadorSessao
- HeliosViewModel.kt
- MonitorMotorista.kt
- TelaHistorico.kt
- PerfilInstalacao
- MenuViagem.kt
- DetectorMediapipe.kt
- Perclos
- TelaDirecaoTest
- MainActivity.kt
- TelaDirecao.kt
- amostra
- GravacaoViagem
- Calibrador
- ConfiguracaoInstalacao
- FonteCameraX.kt
- GravadorArquivos
- LinhaJanela
- Esp32.kt
- Tema.kt
- ExtratorJpeg
- OlhoCnnTest
- Portas.kt
- Sessao.kt
- Calibracao
- RecalibracaoViagem
- Test
- DesenhistaMalha
- AmostraFacial
- Evento
- AcelerometroAndroid.kt
- GeometriaTest
- AnalisadorMovimento
- EstadoMotorista
- Leitura
- TelaInstalacao
- Assistente
- VerificacaoTest
- CaixaRosto
- GravacaoViagem.kt
- TelaUi
- Verificação
- PoseCabeca
- Visor.kt
- ColetorZona
- OlhosCnn
- ADR-012: Interface guiada por diretrizes publicadas
- Ponto2D
- OlhoCnnDouradoTest.kt
- EsquemaBancoTest
- Tom
- OlhoCnn.kt
- Icone
- ClassificadorOlhoLiteRt.kt
- FonteEsp32
- TelaAjustes
- Próximos passos
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- Verificacao
- PoolBitmaps
- DesenhistaMalha.kt
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro
- ADR-011: CNN do olho em modo sombra
- MapeadorFaceMesh
- MalhaVisor
- Tela
- HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)

## God Nodes (most connected - your core abstractions)
1. `MonitorMotorista` - 99 edges
2. `HeliosViewModel` - 93 edges
3. `EstadoMotorista` - 60 edges
4. `Leitura` - 46 edges
5. `Motivo` - 41 edges
6. `PoseCabeca` - 40 edges
7. `Metricas` - 38 edges
8. `Ponto2D` - 38 edges
9. `amostra()` - 38 edges
10. `TelaDirecaoTest` - 35 edges

## Surprising Connections (you probably didn't know these)
- `Números desta versão` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `O que mudou na versão 0.4.6` --references--> `TelaDirecaoTest`  [INFERRED]
  README.md → app/src/test/kotlin/br/unicid/helios/app/ui/TelaDirecaoTest.kt
- `Adendo de 16/11/2026: olhos abertos chamados de fechados` --references--> `OlhoCnnDouradoTest`  [INFERRED]
  docs/ADR-011-cnn-olho-modo-sombra.md → protocolos/src/test/kotlin/br/unicid/helios/protocolos/OlhoCnnDouradoTest.kt
- `O que mudou na versão 0.4.2` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `Decisão` --references--> `DetectorMediapipe`  [INFERRED]
  docs/ADR-011-cnn-olho-modo-sombra.md → app/src/main/kotlin/br/unicid/helios/app/adaptadores/mediapipe/DetectorMediapipe.kt

## Import Cycles
- None detected.

## Communities (85 total, 33 thin omitted)

### Community 0 - "CapturasTelas.kt"
Cohesion: 0.06
Nodes (39): EstadoInstalacao, QuadroVisor, Cabecalho(), Ajustes(), assistente(), Direcao(), DirecaoAtento(), DirecaoCritico() (+31 more)

### Community 1 - "planoDeAlerta"
Cohesion: 0.06
Nodes (16): AlarmeAndroid, VozAndroid, SaidaAlarme, SaidaVoz, ControladorFala, gerarCicloPcm(), gerarRetornoPcm(), padraoPara() (+8 more)

### Community 2 - "HeliosViewModel"
Cohesion: 0.05
Nodes (15): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento (+7 more)

### Community 3 - "CoresTest"
Cohesion: 0.06
Nodes (24): RealceOpenCv, canal(), contrasteWcag(), CorDeSinal, corDoPulso(), EsquemaCores, luminancia(), misturar() (+16 more)

### Community 4 - "Motivo"
Cohesion: 0.10
Nodes (21): Decisão, Classificacao, Classificador, Indicadores, Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA (+13 more)

### Community 5 - "GravacaoViagemTest.kt"
Cohesion: 0.09
Nodes (6): FakeBanco, FakeGravador, FakeUso, GravacaoViagemTest, FakeBanco, FakeGravador

### Community 6 - "MonitorMotorista"
Cohesion: 0.20
Nodes (3): MonitorMotorista, Quadro, MonitorMotoristaTest

### Community 7 - "BancoHelios"
Cohesion: 0.09
Nodes (5): BancoHelios, EsquemaBanco, FotoSalva, RepositorioViagens, SessaoGravada

### Community 8 - "AgregadorSessao"
Cohesion: 0.17
Nodes (10): AgregadorSessao, AgregadorSessaoBordasTest, AgregadorSessaoTest, CnnNaJanelaTest, DesempenhoNaJanelaTest, EstadoNoInicioDoEventoTest, leitura(), metricas() (+2 more)

### Community 9 - "HeliosViewModel.kt"
Cohesion: 0.10
Nodes (8): DetalheViagem, EventoDetalhe, duracaoTexto(), pctTexto(), fotoDoEvento(), FotoRegistrada, fotosDoEvento(), miniatura()

### Community 10 - "MonitorMotorista.kt"
Cohesion: 0.10
Nodes (21): ConfiguracaoDeteccao, classificarFechamento(), DetectorSustentado, duracaoEmCurso(), EmCurso, Encerrada, Fase, Inativa (+13 more)

### Community 11 - "TelaHistorico.kt"
Cohesion: 0.12
Nodes (13): horaTexto(), data(), Detalhe(), duracao(), FotoCheia(), GraficoPerclos(), Linha(), LinhaEvento() (+5 more)

### Community 12 - "PerfilInstalacao"
Cohesion: 0.15
Nodes (13): Desvios, PerfilInstalacao, LadoMotorista, DIREITO, ESQUERDO, Zona, ESPELHO_DIREITO, ESPELHO_ESQUERDO (+5 more)

### Community 14 - "MenuViagem.kt"
Cohesion: 0.10
Nodes (7): ViagemAtual, BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu()

### Community 16 - "Perclos"
Cohesion: 0.13
Nodes (7): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SinaisTest

### Community 18 - "MainActivity.kt"
Cohesion: 0.10
Nodes (5): Aviso(), HeliosApp(), PedirCamera(), PerguntaFotos(), AcoesMenu

### Community 20 - "amostra"
Cohesion: 0.22
Nodes (3): amostra(), CnnEmModoSombraTest, OlharEPiscadasTest

### Community 21 - "GravacaoViagem"
Cohesion: 0.19
Nodes (3): GravacaoViagem, PedidoFoto, SessaoAtiva

### Community 22 - "Calibrador"
Cohesion: 0.16
Nodes (6): Calibrador, desvioPadrao(), mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 23 - "ConfiguracaoInstalacao"
Cohesion: 0.13
Nodes (23): AmostraInstalacao, angulo(), ConfiguracaoInstalacao, dentro(), Dica, ABAIXAR, AFASTAR, APROXIMAR (+15 more)

### Community 26 - "LinhaJanela"
Cohesion: 0.19
Nodes (4): Csv, LinhaEvento, LinhaJanela, CsvTest

### Community 28 - "Tema.kt"
Cohesion: 0.10
Nodes (4): esquema(), TemaHelios(), Tipos, Achados da Fase 1

### Community 31 - "Portas.kt"
Cohesion: 0.14
Nodes (8): Conexao, CONECTADA, CONECTANDO, DESCONECTADA, ConsumidorVisual, DetectorFacial, FonteDeImagens, PreProcessador

### Community 32 - "Sessao.kt"
Cohesion: 0.18
Nodes (7): DialogoRenomear(), AcumuladorJanela, JanelaResumo, media(), percentil95(), PoliticaEvidencia, ResumoSessao

### Community 33 - "Calibracao"
Cohesion: 0.25
Nodes (3): Calibracao, instantes(), seisComRazao()

### Community 34 - "RecalibracaoViagem"
Cohesion: 0.26
Nodes (3): Pose, RecalibracaoViagem, RecalibracaoViagemTest

### Community 37 - "AmostraFacial"
Cohesion: 0.18
Nodes (8): MetricasOlhar, Olho, OlhoAvaliado, AMBOS, DIREITO, ESQUERDO, Sinais, AmostraFacial

### Community 39 - "AcelerometroAndroid.kt"
Cohesion: 0.15
Nodes (3): AcelerometroAndroid, SensorEventListener, FonteInercial

### Community 41 - "AnalisadorMovimento"
Cohesion: 0.23
Nodes (3): Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 42 - "EstadoMotorista"
Cohesion: 0.17
Nodes (14): EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO, FADIGA, SEM_ROSTO (+6 more)

### Community 43 - "Leitura"
Cohesion: 0.28
Nodes (5): Leitura, Metricas, Totais, VisorTest, O que mudou na versão 0.2.0

### Community 44 - "TelaInstalacao"
Cohesion: 0.23
Nodes (13): dataTexto(), AcoesInstalacao, AcoesPerfis, ComImagem(), Escolha(), EtapaCalibracao(), EtapaPosicao(), EtapaResumo() (+5 more)

### Community 47 - "CaixaRosto"
Cohesion: 0.23
Nodes (6): CaixaRosto, brilhoDoRosto(), Blendshapes, IndicesFaceMesh, ResultadoFaceMesh, EnquadramentoTest

### Community 48 - "GravacaoViagem.kt"
Cohesion: 0.21
Nodes (4): FotoGuardada, nomeFoto(), PoliticaRetencao, FotosTest

### Community 49 - "TelaUi"
Cohesion: 0.27
Nodes (13): Diagnostico, TelaUi, Chip(), Chips(), ConteudoDirecao(), estadoInicial(), FaixaInferior(), Indicador() (+5 more)

### Community 50 - "Verificação"
Cohesion: 0.15
Nodes (7): ADR-013: instalação da câmera e calibração obrigatória, Consequências, Contexto, Decisão, Verificação, TextosInstalacao, TextosInstalacaoTest

### Community 52 - "Visor.kt"
Cohesion: 0.28
Nodes (12): abertura(), cnn(), Eixos2D, eixosDaCabeca(), graus(), linhasDeMetricas(), num(), olhar() (+4 more)

### Community 54 - "OlhosCnn"
Cohesion: 0.22
Nodes (4): Malha, quer(), receber(), OlhosCnn

### Community 55 - "ADR-012: Interface guiada por diretrizes publicadas"
Cohesion: 0.18
Nodes (11): MainActivity, Alvo, Espaco, ADR-012: Interface guiada por diretrizes publicadas, Capturas, Contexto, Decisões da Fase 1 (tokens de design), Decisões da Fase 2 (tela de direção) (+3 more)

### Community 56 - "Ponto2D"
Cohesion: 0.33
Nodes (6): distancia(), earMedio(), posicaoIris(), razaoAspecto(), Ponto2D, SeisPontos

### Community 59 - "Tom"
Cohesion: 0.20
Nodes (11): Selo(), cor(), fundoDoSinal(), textoDoSinal(), Tom, AMARELO, AMBAR, LARANJA (+3 more)

### Community 61 - "Icone"
Cohesion: 0.17
Nodes (12): Icone, AGUARDE, ALERTA, ATENCAO, CAMERA_FRONTAL, CONFIRMADO, OLHAR_FORA, PARADO (+4 more)

### Community 64 - "TelaAjustes"
Cohesion: 0.44
Nodes (9): EstadoFotos, Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoFotos(), TelaAjustes() (+1 more)

### Community 65 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 66 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 67 - "Verificacao"
Cohesion: 0.29
Nodes (6): Verificacao, ANGULO, CAMERA_PARADA, LUZ, ROSTO_INTEIRO, TAMANHO

### Community 71 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 72 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 73 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 74 - "ADR-010: frases de alerta geradas fora do carro"
Cohesion: 0.33
Nodes (5): ADR-010: frases de alerta geradas fora do carro, Consequências, Contexto, Decisão, Fora desta decisão

### Community 75 - "ADR-011: CNN do olho em modo sombra"
Cohesion: 0.33
Nodes (5): Adendo de 16/11/2026: olhos abertos chamados de fechados, ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Fora desta decisão

### Community 78 - "Tela"
Cohesion: 0.50
Nodes (4): Tela, AJUSTES, DIRECAO, HISTORICO

## Knowledge Gaps
- **97 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+92 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 374 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **33 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HeliosViewModel` connect `HeliosViewModel` to `CapturasTelas.kt`, `planoDeAlerta`, `MonitorMotorista`, `BancoHelios`, `HeliosViewModel.kt`, `MonitorMotorista.kt`, `MenuViagem.kt`, `DetectorMediapipe.kt`, `Perclos`, `MainActivity.kt`, `GravacaoViagem`, `GravadorArquivos`, `Esp32.kt`, `Portas.kt`, `RecalibracaoViagem`, `DesenhistaMalha`, `EstadoMotorista`, `Leitura`, `Assistente`, `TelaUi`, `OlhosCnn`, `TelaAjustes`, `DesenhistaMalha.kt`?**
  _High betweenness centrality (0.126) - this node is a cross-community bridge._
- **Are the 52 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 52 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _97 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `CapturasTelas.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06192259675405743 - nodes in this community are weakly interconnected._
- **Why does `EstadoMotorista` connect `EstadoMotorista` to `CapturasTelas.kt`, `planoDeAlerta`, `HeliosViewModel`, `CoresTest`, `Motivo`, `GravacaoViagemTest.kt`, `MonitorMotorista`, `BancoHelios`, `AgregadorSessao`, `HeliosViewModel.kt`, `MonitorMotorista.kt`, `TelaDirecaoTest`, `GravacaoViagem`, `LinhaJanela`, `Sessao.kt`, `Calibracao`, `Test`, `AmostraFacial`, `Evento`, `Leitura`, `GravacaoViagem.kt`, `OlhoCnnTest.kt`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `planoDeAlerta` be split into smaller, more focused modules?**
  _Cohesion score 0.05834464043419267 - nodes in this community are weakly interconnected._