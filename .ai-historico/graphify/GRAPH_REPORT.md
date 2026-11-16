# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 73 files · ~49,774 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: .xml 6, .properties 3, (none) 2)

## Summary
- 1248 nodes · 3251 edges · 54 communities (37 shown, 17 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 242 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a6900ba2`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MonitorMotorista
- OlhoCnnTest
- EstadoMotorista
- planoDeAlerta
- Motivo
- GravacaoViagemTest.kt
- TelaDirecao.kt
- BancoHelios
- MainActivity.kt
- TelaHistorico.kt
- Calibrador
- Esp32.kt
- TelaAjustes.kt
- Perclos
- MenuViagem.kt
- DetectorMediapipe.kt
- HeliosViewModel.kt
- HeliosViewModel
- GravacaoViagem
- LinhaJanela
- Metricas
- Ajustes
- GravadorArquivos
- Ponto2D
- FonteCameraX.kt
- ExtratorJpeg
- Portas.kt
- GeometriaTest
- AcelerometroAndroid.kt
- AnalisadorMovimento
- RealceOpenCv.kt
- HELIOS: aplicativo Android (versão 0.4.4)
- Tema.kt
- OlharEPiscadasTest.kt
- Test
- VozAndroid
- ClassificadorOlho
- .extrair
- TipoEvento
- PoliticaRetencao
- Próximos passos
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- modeloDeTela
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro

## God Nodes (most connected - your core abstractions)
1. `MonitorMotorista` - 97 edges
2. `HeliosViewModel` - 74 edges
3. `EstadoMotorista` - 52 edges
4. `Leitura` - 40 edges
5. `amostra()` - 38 edges
6. `MonitorMotoristaTest` - 35 edges
7. `Motivo` - 34 edges
8. `GravacaoViagem` - 33 edges
9. `Ponto2D` - 33 edges
10. `TipoEvento` - 31 edges

## Surprising Connections (you probably didn't know these)
- `Números desta versão` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `O que mudou na versão 0.4.2` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `O que mudou na versão 0.4.2` --references--> `HeliosViewModel`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/HeliosViewModel.kt
- `Decisão` --references--> `DetectorMediapipe`  [INFERRED]
  docs/ADR-011-cnn-olho-modo-sombra.md → app/src/main/kotlin/br/unicid/helios/app/adaptadores/mediapipe/DetectorMediapipe.kt
- `O que mudou na versão 0.4.4` --references--> `RealceOpenCv`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/adaptadores/opencv/RealceOpenCv.kt

## Import Cycles
- None detected.

## Communities (54 total, 17 thin omitted)

### Community 0 - "MonitorMotorista"
Cohesion: 0.07
Nodes (19): Ferramentas obrigatórias, HELIOS: app Android de detecção de sonolência (TCC UNICID 2026), Calibracao, ConfiguracaoDeteccao, classificarFechamento(), DetectorSustentado, EmCurso, Encerrada (+11 more)

### Community 1 - "OlhoCnnTest"
Cohesion: 0.06
Nodes (12): BuffersVisor, DesenhistaMalha, Malha, Pontos, quer(), receber(), OlhosCnn, argbDeCinza() (+4 more)

### Community 2 - "EstadoMotorista"
Cohesion: 0.08
Nodes (27): Leitura, AcumuladorJanela, AgregadorSessao, JanelaResumo, media(), percentil95(), PoliticaEvidencia, ResumoSessao (+19 more)

### Community 3 - "planoDeAlerta"
Cohesion: 0.08
Nodes (14): AlarmeAndroid, SaidaAlarme, ControladorFala, gerarCicloPcm(), gerarRetornoPcm(), padraoPara(), PadraoSonoro, PlanoAlerta (+6 more)

### Community 4 - "Motivo"
Cohesion: 0.08
Nodes (25): ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Decisão, Fora desta decisão, Classificacao, Classificador, Indicadores (+17 more)

### Community 5 - "GravacaoViagemTest.kt"
Cohesion: 0.09
Nodes (6): FakeBanco, FakeGravador, FakeUso, GravacaoViagemTest, FakeBanco, FakeGravador

### Community 6 - "TelaDirecao.kt"
Cohesion: 0.11
Nodes (16): QuadroVisor, TelaUi, AcoesMenu, Calibrando(), ConteudoDirecao(), estadoInicial(), Olho(), PainelDiagnostico() (+8 more)

### Community 7 - "BancoHelios"
Cohesion: 0.09
Nodes (6): BancoHelios, EsquemaBanco, FotoSalva, RepositorioViagens, SessaoGravada, LinhaEvento

### Community 8 - "MainActivity.kt"
Cohesion: 0.08
Nodes (9): Aviso(), HeliosApp(), MainActivity, PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO (+1 more)

### Community 9 - "TelaHistorico.kt"
Cohesion: 0.11
Nodes (13): horaTexto(), data(), Detalhe(), duracao(), FotoCheia(), GraficoPerclos(), Linha(), LinhaEvento() (+5 more)

### Community 10 - "Calibrador"
Cohesion: 0.12
Nodes (6): EsquemaBancoTest, Calibrador, mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 11 - "Esp32.kt"
Cohesion: 0.10
Nodes (5): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback, Imagem

### Community 12 - "TelaAjustes.kt"
Cohesion: 0.10
Nodes (9): EstadoFotos, Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoFotos(), TelaAjustes() (+1 more)

### Community 13 - "Perclos"
Cohesion: 0.12
Nodes (8): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SuavizadorEma, SinaisTest

### Community 14 - "MenuViagem.kt"
Cohesion: 0.10
Nodes (8): ViagemAtual, BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu(), cor()

### Community 15 - "DetectorMediapipe.kt"
Cohesion: 0.12
Nodes (4): DetectorMediapipe, Blendshapes, IndicesFaceMesh, ResultadoFaceMesh

### Community 16 - "HeliosViewModel.kt"
Cohesion: 0.12
Nodes (9): DetalheViagem, EventoDetalhe, dataTexto(), duracaoTexto(), pctTexto(), fotoDoEvento(), FotoRegistrada, fotosDoEvento() (+1 more)

### Community 18 - "GravacaoViagem"
Cohesion: 0.17
Nodes (3): GravacaoViagem, PedidoFoto, SessaoAtiva

### Community 19 - "LinhaJanela"
Cohesion: 0.15
Nodes (3): Csv, LinhaJanela, CsvTest

### Community 20 - "Metricas"
Cohesion: 0.17
Nodes (16): Metricas, MetricasOlhar, abertura(), cnn(), Eixos2D, eixosDaCabeca(), graus(), linhasDeMetricas() (+8 more)

### Community 21 - "Ajustes"
Cohesion: 0.13
Nodes (14): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento (+6 more)

### Community 23 - "Ponto2D"
Cohesion: 0.22
Nodes (10): duracaoEmCurso(), distancia(), earMedio(), posicaoIris(), razaoAspecto(), AmostraFacial, Ponto2D, PoseCabeca (+2 more)

### Community 26 - "Portas.kt"
Cohesion: 0.15
Nodes (7): Conexao, CONECTADA, CONECTANDO, DESCONECTADA, ConsumidorVisual, DetectorFacial, FonteDeImagens

### Community 28 - "AcelerometroAndroid.kt"
Cohesion: 0.15
Nodes (3): AcelerometroAndroid, SensorEventListener, FonteInercial

### Community 29 - "AnalisadorMovimento"
Cohesion: 0.22
Nodes (3): Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 30 - "RealceOpenCv.kt"
Cohesion: 0.15
Nodes (3): RealceOpenCv, PreProcessador, O que mudou na versão 0.4.4

### Community 31 - "HELIOS: aplicativo Android (versão 0.4.4)"
Cohesion: 0.12
Nodes (13): Diagnostico, Como abrir, HELIOS: aplicativo Android (versão 0.4.4), Limitações conhecidas, Números desta versão, O que mudou na versão 0.2.0, O que mudou na versão 0.3.0 (depois do primeiro teste no celular), O que mudou na versão 0.4.0 (depois do segundo teste no celular) (+5 more)

### Community 32 - "Tema.kt"
Cohesion: 0.13
Nodes (5): TemaHelios(), Tipos, cronometro(), detalhePadrao(), Paleta

### Community 37 - ".extrair"
Cohesion: 0.21
Nodes (5): OlhoAvaliado, AMBOS, DIREITO, ESQUERDO, Sinais

### Community 38 - "TipoEvento"
Cohesion: 0.18
Nodes (11): TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MICROSSONO, OLHAR_DESVIADO, PISCADA (+3 more)

### Community 39 - "PoliticaRetencao"
Cohesion: 0.33
Nodes (4): FotoGuardada, nomeFoto(), PoliticaRetencao, FotosTest

### Community 40 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 42 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 45 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 46 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 47 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 48 - "ADR-010: frases de alerta geradas fora do carro"
Cohesion: 0.33
Nodes (5): ADR-010: frases de alerta geradas fora do carro, Consequências, Contexto, Decisão, Fora desta decisão

## Knowledge Gaps
- **48 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+43 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 292 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **17 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MonitorMotorista` connect `MonitorMotorista` to `OlharEPiscadasTest.kt`, `EstadoMotorista`, `Motivo`, `.extrair`, `.iniciarCaptura`, `Perclos`, `HeliosViewModel.kt`, `HeliosViewModel`, `Ponto2D`, `AnalisadorMovimento`?**
  _High betweenness centrality (0.155) - this node is a cross-community bridge._
- **Are the 52 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 52 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _48 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MonitorMotorista` be split into smaller, more focused modules?**
  _Cohesion score 0.06594956797742903 - nodes in this community are weakly interconnected._
- **Why does `HeliosViewModel` connect `HeliosViewModel` to `MonitorMotorista`, `OlhoCnnTest`, `EstadoMotorista`, `planoDeAlerta`, `TelaDirecao.kt`, `BancoHelios`, `MainActivity.kt`, `Calibrador`, `Esp32.kt`, `TelaAjustes.kt`, `Perclos`, `MenuViagem.kt`, `DetectorMediapipe.kt`, `HeliosViewModel.kt`, `GravacaoViagem`, `LinhaJanela`, `Ajustes`, `GravadorArquivos`, `Portas.kt`, `RealceOpenCv.kt`, `HELIOS: aplicativo Android (versão 0.4.4)`, `VozAndroid`, `ClassificadorOlho`, `.iniciarCaptura`?**
  _High betweenness centrality (0.153) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `OlhoCnnTest` be split into smaller, more focused modules?**
  _Cohesion score 0.05821917808219178 - nodes in this community are weakly interconnected._