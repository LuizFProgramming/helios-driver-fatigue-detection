# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 95 files · ~517,913 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1724 nodes · 4833 edges · 73 communities (50 shown, 23 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 354 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `39890c51`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- CapturasTelas.kt
- Ponto2D
- HeliosViewModel
- GravacaoViagem
- modeloDeTela
- OlhoCnnTest
- planoDeAlerta
- RecalibracaoViagem
- TelaDirecao.kt
- Motivo
- BancoHelios
- Quadro
- HeliosViewModel.kt
- Esp32.kt
- TelaDirecaoTest
- AgregadorSessao
- Perclos
- MonitorMotorista
- GravadorArquivos
- FakeBanco
- MenuViagem.kt
- DetectorMediapipe.kt
- TelaHistorico.kt
- CoresTest
- ConfiguracaoInstalacao
- Tema.kt
- PerfilInstalacao
- LinhaEvento
- GravacaoViagemTest.kt
- FonteCameraX.kt
- ConfiguracaoDeteccao
- amostra
- ExtratorJpeg
- Sessao.kt
- Evento
- EsquemaBancoTest
- Portas.kt
- AcelerometroAndroid.kt
- GravacaoViagem.kt
- AnalisadorMovimento
- Assistente
- PosicaoCamera
- VerificacaoTest
- VozAndroid
- PoseCabeca
- TelaAjustes
- ColetorZona
- Visor.kt
- ClassificadorOlho
- HeliosApp
- Zona
- EstadoMotorista
- Leitura
- TipoEvento
- OlhosCnn
- CaixaRosto
- Próximos passos
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- Tom
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro
- ADR-013: instalação da câmera e calibração obrigatória
- MalhaVisor
- HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)

## God Nodes (most connected - your core abstractions)
1. `MonitorMotorista` - 107 edges
2. `HeliosViewModel` - 95 edges
3. `EstadoMotorista` - 61 edges
4. `Leitura` - 46 edges
5. `amostra()` - 46 edges
6. `Motivo` - 43 edges
7. `PoseCabeca` - 42 edges
8. `Metricas` - 39 edges
9. `TelaDirecaoTest` - 38 edges
10. `Ponto2D` - 38 edges

## Surprising Connections (you probably didn't know these)
- `Achados da Fase 1` --references--> `TemaHelios()`  [INFERRED]
  docs/ADR-012-interface.md → app/src/main/kotlin/br/unicid/helios/app/ui/Tema.kt
- `O que mudou na versão 0.4.6` --references--> `TelaDirecaoTest`  [INFERRED]
  README.md → app/src/test/kotlin/br/unicid/helios/app/ui/TelaDirecaoTest.kt
- `Decisão` --references--> `ConfiguracaoDeteccao`  [INFERRED]
  docs/ADR-014-distracao-por-zonas.md → dominio/src/main/kotlin/br/unicid/helios/dominio/ConfiguracaoDeteccao.kt
- `O que mudou na versão 0.4.5` --references--> `CoresTest`  [INFERRED]
  README.md → protocolos/src/test/kotlin/br/unicid/helios/protocolos/CoresTest.kt
- `Números desta versão` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt

## Import Cycles
- None detected.

## Communities (73 total, 23 thin omitted)

### Community 0 - "CapturasTelas.kt"
Cohesion: 0.05
Nodes (49): EstadoInstalacao, QuadroVisor, AcoesInstalacao, Cabecalho(), ComImagem(), Escolha(), EtapaCalibracao(), EtapaPosicao() (+41 more)

### Community 1 - "Ponto2D"
Cohesion: 0.07
Nodes (22): Adendo de 16/11/2026: olhos abertos chamados de fechados, ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Fora desta decisão, distancia(), earMedio(), poseDaMatriz() (+14 more)

### Community 2 - "HeliosViewModel"
Cohesion: 0.05
Nodes (6): RealceOpenCv, Diagnostico, HeliosViewModel, PreProcessador, O que mudou na versão 0.2.0, O que mudou na versão 0.4.4

### Community 3 - "GravacaoViagem"
Cohesion: 0.05
Nodes (33): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento (+25 more)

### Community 4 - "modeloDeTela"
Cohesion: 0.06
Nodes (33): MainActivity, Alvo, Espaco, Achados da Fase 1, ADR-012: Interface guiada por diretrizes publicadas, Capturas, Contexto, Decisões da Fase 1 (tokens de design) (+25 more)

### Community 6 - "OlhoCnnTest"
Cohesion: 0.08
Nodes (10): BuffersVisor, DesenhistaMalha, Malha, Pontos, IndicesFaceMesh, argbDeCinza(), CaixaRecorte, cnnFechado() (+2 more)

### Community 7 - "planoDeAlerta"
Cohesion: 0.08
Nodes (14): AlarmeAndroid, SaidaAlarme, ControladorFala, gerarCicloPcm(), gerarRetornoPcm(), padraoPara(), PadraoSonoro, PlanoAlerta (+6 more)

### Community 8 - "RecalibracaoViagem"
Cohesion: 0.11
Nodes (9): Calibrador, desvioPadrao(), mediana(), medianaOuNulo(), medianaOuZero(), Pose, RecalibracaoViagem, CalibradorTest (+1 more)

### Community 9 - "TelaDirecao.kt"
Cohesion: 0.09
Nodes (18): TelaUi, ViagemAtual, Chip(), Chips(), ConteudoDirecao(), desenhoDoIcone(), estadoInicial(), FaixaInferior() (+10 more)

### Community 10 - "Motivo"
Cohesion: 0.09
Nodes (22): Decisão, Classificacao, Classificador, Indicadores, Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA (+14 more)

### Community 11 - "BancoHelios"
Cohesion: 0.09
Nodes (6): BancoHelios, EsquemaBanco, FotoSalva, RepositorioViagens, SessaoGravada, LinhaJanela

### Community 13 - "HeliosViewModel.kt"
Cohesion: 0.10
Nodes (8): DetalheViagem, EventoDetalhe, duracaoTexto(), pctTexto(), fotoDoEvento(), FotoRegistrada, fotosDoEvento(), miniatura()

### Community 14 - "Esp32.kt"
Cohesion: 0.10
Nodes (5): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback, Imagem

### Community 16 - "AgregadorSessao"
Cohesion: 0.18
Nodes (9): AgregadorSessao, AgregadorSessaoBordasTest, AgregadorSessaoTest, CnnNaJanelaTest, EstadoNoInicioDoEventoTest, leitura(), metricas(), PoliticaEvidenciaTest (+1 more)

### Community 17 - "Perclos"
Cohesion: 0.12
Nodes (8): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SuavizadorEma, SinaisTest

### Community 18 - "MonitorMotorista"
Cohesion: 0.19
Nodes (4): Calibracao, MonitorMotorista, Olho, OlharEPiscadasTest

### Community 19 - "GravadorArquivos"
Cohesion: 0.12
Nodes (3): GravadorArquivos, GravadorFotos, FakeGravador

### Community 20 - "FakeBanco"
Cohesion: 0.13
Nodes (4): FakeBanco, GravacaoViagemTest, FakeBanco, FakeGravador

### Community 21 - "MenuViagem.kt"
Cohesion: 0.10
Nodes (6): BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu()

### Community 23 - "TelaHistorico.kt"
Cohesion: 0.15
Nodes (13): horaTexto(), data(), Detalhe(), duracao(), FotoCheia(), GraficoPerclos(), Linha(), LinhaEvento() (+5 more)

### Community 24 - "CoresTest"
Cohesion: 0.17
Nodes (8): canal(), contrasteWcag(), CorDeSinal, corDoPulso(), EsquemaCores, luminancia(), misturar(), CoresTest

### Community 25 - "ConfiguracaoInstalacao"
Cohesion: 0.12
Nodes (24): angulo(), ConfiguracaoInstalacao, dentro(), Dica, ABAIXAR, AFASTAR, APROXIMAR, CENTRALIZAR (+16 more)

### Community 27 - "PerfilInstalacao"
Cohesion: 0.20
Nodes (6): Verificação, Desvios, PerfilInstalacao, ArquivoPerfis, ArquivoPerfisTest, TextosInstalacaoTest

### Community 28 - "LinhaEvento"
Cohesion: 0.21
Nodes (3): Csv, LinhaEvento, CsvTest

### Community 31 - "ConfiguracaoDeteccao"
Cohesion: 0.17
Nodes (10): ConfiguracaoDeteccao, classificarFechamento(), DetectorSustentado, duracaoEmCurso(), EmCurso, Encerrada, Fase, Inativa (+2 more)

### Community 32 - "amostra"
Cohesion: 0.27
Nodes (4): amostra(), instantes(), CnnEmModoSombraTest, ZonasEManobraTest

### Community 34 - "Sessao.kt"
Cohesion: 0.18
Nodes (6): AcumuladorJanela, JanelaResumo, media(), percentil95(), ResumoSessao, DesempenhoNaJanelaTest

### Community 36 - "EsquemaBancoTest"
Cohesion: 0.20
Nodes (6): EsquemaBancoTest, ADR-014: distração com zonas do carro e manobra, Consequências, Contexto, Decisão, Verificação

### Community 37 - "Portas.kt"
Cohesion: 0.15
Nodes (7): Conexao, CONECTADA, CONECTANDO, DESCONECTADA, ConsumidorVisual, DetectorFacial, FonteDeImagens

### Community 38 - "AcelerometroAndroid.kt"
Cohesion: 0.15
Nodes (3): AcelerometroAndroid, SensorEventListener, FonteInercial

### Community 39 - "GravacaoViagem.kt"
Cohesion: 0.18
Nodes (4): FotoGuardada, nomeFoto(), PoliticaRetencao, FotosTest

### Community 40 - "AnalisadorMovimento"
Cohesion: 0.23
Nodes (3): Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 42 - "PosicaoCamera"
Cohesion: 0.14
Nodes (8): LadoMotorista, DIREITO, ESQUERDO, PosicaoCamera, CENTRO_PAINEL, COLUNA_VOLANTE, PILAR_A, RETROVISOR

### Community 46 - "TelaAjustes"
Cohesion: 0.27
Nodes (13): EstadoFotos, dataTexto(), Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoFotos() (+5 more)

### Community 48 - "Visor.kt"
Cohesion: 0.31
Nodes (12): abertura(), cnn(), Eixos2D, eixosDaCabeca(), graus(), linhasDeMetricas(), num(), olhar() (+4 more)

### Community 50 - "HeliosApp"
Cohesion: 0.18
Nodes (9): Aviso(), HeliosApp(), PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO, HISTORICO (+1 more)

### Community 51 - "Zona"
Cohesion: 0.27
Nodes (8): distancia(), Zona, ESPELHO_DIREITO, ESPELHO_ESQUERDO, PAINEL, RETROVISOR, zonaMaisProxima(), ZonaOlhar

### Community 52 - "EstadoMotorista"
Cohesion: 0.15
Nodes (10): PoliticaEvidencia, EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO, FADIGA (+2 more)

### Community 53 - "Leitura"
Cohesion: 0.42
Nodes (4): Leitura, Metricas, Totais, VisorTest

### Community 54 - "TipoEvento"
Cohesion: 0.17
Nodes (12): TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MANOBRA, MICROSSONO, OLHAR_DESVIADO (+4 more)

### Community 55 - "OlhosCnn"
Cohesion: 0.25
Nodes (3): quer(), receber(), OlhosCnn

### Community 56 - "CaixaRosto"
Cohesion: 0.44
Nodes (3): CaixaRosto, brilhoDoRosto(), EnquadramentoTest

### Community 57 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 58 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 59 - "Tom"
Cohesion: 0.29
Nodes (7): Tom, AMARELO, AMBAR, LARANJA, NEUTRO, VERDE, VERMELHO

### Community 61 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 62 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 63 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 64 - "ADR-010: frases de alerta geradas fora do carro"
Cohesion: 0.33
Nodes (5): ADR-010: frases de alerta geradas fora do carro, Consequências, Contexto, Decisão, Fora desta decisão

### Community 65 - "ADR-013: instalação da câmera e calibração obrigatória"
Cohesion: 0.40
Nodes (4): ADR-013: instalação da câmera e calibração obrigatória, Consequências, Contexto, Decisão

## Knowledge Gaps
- **99 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+94 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 381 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HeliosViewModel` connect `HeliosViewModel` to `CapturasTelas.kt`, `GravacaoViagem`, `OlhoCnnTest`, `planoDeAlerta`, `RecalibracaoViagem`, `TelaDirecao.kt`, `BancoHelios`, `HeliosViewModel.kt`, `Esp32.kt`, `Perclos`, `GravadorArquivos`, `DetectorMediapipe.kt`, `ConfiguracaoDeteccao`, `Portas.kt`, `Assistente`, `VozAndroid`, `TelaAjustes`, `ClassificadorOlho`, `HeliosApp`, `EstadoMotorista`, `Leitura`, `OlhosCnn`?**
  _High betweenness centrality (0.116) - this node is a cross-community bridge._
- **Are the 54 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 54 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _99 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `CapturasTelas.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.050628610261637785 - nodes in this community are weakly interconnected._
- **Why does `EstadoMotorista` connect `EstadoMotorista` to `CapturasTelas.kt`, `Ponto2D`, `HeliosViewModel`, `GravacaoViagem`, `modeloDeTela`, `planoDeAlerta`, `Motivo`, `BancoHelios`, `HeliosViewModel.kt`, `TelaDirecaoTest`, `AgregadorSessao`, `MonitorMotorista`, `FakeBanco`, `LinhaEvento`, `GravacaoViagemTest.kt`, `amostra`, `Sessao.kt`, `Evento`, `GravacaoViagem.kt`, `Zona`, `Leitura`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `Ponto2D` be split into smaller, more focused modules?**
  _Cohesion score 0.06599597585513078 - nodes in this community are weakly interconnected._