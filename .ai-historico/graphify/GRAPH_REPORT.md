# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 79 files · ~306,918 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1429 nodes · 3729 edges · 54 communities (38 shown, 16 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 288 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ec8fc634`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MonitorMotorista
- Ponto2D
- EstadoMotorista
- OlhoCnnTest
- Motivo
- Leitura
- TelaDirecaoTest
- Icone
- GravacaoViagemTest.kt
- TelaDirecao.kt
- BancoHelios
- Esp32.kt
- Tema.kt
- HeliosViewModel.kt
- Perclos
- DetectorMediapipe.kt
- MainActivity.kt
- MenuViagem.kt
- TelaAjustes.kt
- LinhaJanela
- GravacaoViagem
- CoresTest
- HeliosViewModel
- CapturasTelas.kt
- FonteCameraX.kt
- Ajustes
- GravadorArquivos
- TelaHistorico
- ExtratorJpeg
- HELIOS: aplicativo Android (versão 0.4.6)
- Calibrador
- AcelerometroAndroid.kt
- Portas.kt
- AnalisadorMovimento
- RealceOpenCv.kt
- VozAndroid
- EsquemaBancoTest
- ClassificadorOlho
- PoliticaRetencao
- Próximos passos
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro

## God Nodes (most connected - your core abstractions)
1. `MonitorMotorista` - 97 edges
2. `HeliosViewModel` - 74 edges
3. `EstadoMotorista` - 60 edges
4. `Leitura` - 46 edges
5. `Motivo` - 41 edges
6. `amostra()` - 38 edges
7. `Ponto2D` - 36 edges
8. `TelaDirecaoTest` - 35 edges
9. `MonitorMotoristaTest` - 35 edges
10. `GravacaoViagem` - 34 edges

## Surprising Connections (you probably didn't know these)
- `Números desta versão` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `O que mudou na versão 0.4.6` --references--> `TelaDirecaoTest`  [INFERRED]
  README.md → app/src/test/kotlin/br/unicid/helios/app/ui/TelaDirecaoTest.kt
- `O que mudou na versão 0.4.5` --references--> `CoresTest`  [INFERRED]
  README.md → protocolos/src/test/kotlin/br/unicid/helios/protocolos/CoresTest.kt
- `O que mudou na versão 0.4.2` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `O que mudou na versão 0.4.2` --references--> `HeliosViewModel`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/HeliosViewModel.kt

## Import Cycles
- None detected.

## Communities (54 total, 16 thin omitted)

### Community 0 - "MonitorMotorista"
Cohesion: 0.06
Nodes (20): Ferramentas obrigatórias, HELIOS: app Android de detecção de sonolência (TCC UNICID 2026), Calibracao, ConfiguracaoDeteccao, classificarFechamento(), DetectorSustentado, duracaoEmCurso(), EmCurso (+12 more)

### Community 1 - "Ponto2D"
Cohesion: 0.07
Nodes (33): distancia(), earMedio(), poseDaMatriz(), posicaoIris(), razaoAspecto(), Metricas, MetricasOlhar, OlhoAvaliado (+25 more)

### Community 2 - "EstadoMotorista"
Cohesion: 0.05
Nodes (23): AlarmeAndroid, SaidaAlarme, EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO (+15 more)

### Community 3 - "OlhoCnnTest"
Cohesion: 0.06
Nodes (12): BuffersVisor, DesenhistaMalha, Malha, Pontos, quer(), receber(), OlhosCnn, argbDeCinza() (+4 more)

### Community 4 - "Motivo"
Cohesion: 0.06
Nodes (28): Adendo de 16/11/2026: olhos abertos chamados de fechados, ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Decisão, Fora desta decisão, Classificacao, Classificador (+20 more)

### Community 5 - "Leitura"
Cohesion: 0.10
Nodes (18): Leitura, AcumuladorJanela, AgregadorSessao, JanelaResumo, media(), percentil95(), PoliticaEvidencia, ResumoSessao (+10 more)

### Community 6 - "TelaDirecaoTest"
Cohesion: 0.10
Nodes (16): MainActivity, AcoesMenu, TelaDirecao(), Alvo, Espaco, TemaHelios(), TelaDirecaoTest, Achados da Fase 1 (+8 more)

### Community 7 - "Icone"
Cohesion: 0.09
Nodes (20): MapeadorFaceMesh, apoio(), detalhePadrao(), duracao(), EstadoTela, Icone, AGUARDE, ALERTA (+12 more)

### Community 8 - "GravacaoViagemTest.kt"
Cohesion: 0.09
Nodes (6): FakeBanco, FakeGravador, FakeUso, GravacaoViagemTest, FakeBanco, FakeGravador

### Community 9 - "TelaDirecao.kt"
Cohesion: 0.09
Nodes (14): QuadroVisor, Chip(), Chips(), ConteudoDirecao(), desenhoDoIcone(), estadoInicial(), FaixaInferior(), Indicador() (+6 more)

### Community 10 - "BancoHelios"
Cohesion: 0.09
Nodes (5): BancoHelios, EsquemaBanco, FotoSalva, RepositorioViagens, SessaoGravada

### Community 11 - "Esp32.kt"
Cohesion: 0.09
Nodes (4): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback

### Community 12 - "Tema.kt"
Cohesion: 0.07
Nodes (11): cor(), esquema(), fundoDoSinal(), Tipos, Tom, AMARELO, AMBAR, LARANJA (+3 more)

### Community 13 - "HeliosViewModel.kt"
Cohesion: 0.10
Nodes (17): DetalheViagem, EventoDetalhe, TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MICROSSONO (+9 more)

### Community 14 - "Perclos"
Cohesion: 0.12
Nodes (8): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SuavizadorEma, SinaisTest

### Community 15 - "DetectorMediapipe.kt"
Cohesion: 0.12
Nodes (4): DetectorMediapipe, Blendshapes, IndicesFaceMesh, ResultadoFaceMesh

### Community 16 - "MainActivity.kt"
Cohesion: 0.09
Nodes (8): Aviso(), HeliosApp(), PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO, HISTORICO

### Community 17 - "MenuViagem.kt"
Cohesion: 0.10
Nodes (7): ViagemAtual, BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu()

### Community 18 - "TelaAjustes.kt"
Cohesion: 0.11
Nodes (9): EstadoFotos, Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoFotos(), TelaAjustes() (+1 more)

### Community 19 - "LinhaJanela"
Cohesion: 0.15
Nodes (4): Csv, LinhaEvento, LinhaJanela, CsvTest

### Community 20 - "GravacaoViagem"
Cohesion: 0.15
Nodes (4): GravacaoViagem, PedidoFoto, SessaoAtiva, nomeFoto()

### Community 21 - "CoresTest"
Cohesion: 0.16
Nodes (8): canal(), contrasteWcag(), CorDeSinal, corDoPulso(), EsquemaCores, luminancia(), misturar(), CoresTest

### Community 23 - "CapturasTelas.kt"
Cohesion: 0.24
Nodes (15): TelaUi, Ajustes(), Direcao(), DirecaoAtento(), DirecaoCritico(), DirecaoDistraido(), DirecaoFadiga(), DirecaoParadoComVisor() (+7 more)

### Community 25 - "Ajustes"
Cohesion: 0.13
Nodes (14): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento (+6 more)

### Community 27 - "TelaHistorico"
Cohesion: 0.16
Nodes (16): dataTexto(), duracaoTexto(), horaTexto(), pctTexto(), data(), Detalhe(), duracao(), FotoCheia() (+8 more)

### Community 31 - "HELIOS: aplicativo Android (versão 0.4.6)"
Cohesion: 0.11
Nodes (15): Diagnostico, Como abrir, HELIOS: aplicativo Android (versão 0.4.6), Limitações conhecidas, Números desta versão, O que mudou na versão 0.2.0, O que mudou na versão 0.3.0 (depois do primeiro teste no celular), O que mudou na versão 0.4.0 (depois do segundo teste no celular) (+7 more)

### Community 32 - "Calibrador"
Cohesion: 0.22
Nodes (5): Calibrador, mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 33 - "AcelerometroAndroid.kt"
Cohesion: 0.15
Nodes (3): AcelerometroAndroid, SensorEventListener, FonteInercial

### Community 34 - "Portas.kt"
Cohesion: 0.16
Nodes (7): Conexao, CONECTADA, CONECTANDO, DESCONECTADA, ConsumidorVisual, DetectorFacial, FonteDeImagens

### Community 35 - "AnalisadorMovimento"
Cohesion: 0.22
Nodes (3): Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 36 - "RealceOpenCv.kt"
Cohesion: 0.15
Nodes (3): RealceOpenCv, PreProcessador, O que mudou na versão 0.4.4

### Community 40 - "PoliticaRetencao"
Cohesion: 0.33
Nodes (3): FotoGuardada, PoliticaRetencao, FotosTest

### Community 41 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 43 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
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
- **65 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+60 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 355 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **16 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HeliosViewModel` connect `HeliosViewModel` to `MonitorMotorista`, `EstadoMotorista`, `OlhoCnnTest`, `Leitura`, `TelaDirecao.kt`, `BancoHelios`, `Esp32.kt`, `HeliosViewModel.kt`, `Perclos`, `DetectorMediapipe.kt`, `MainActivity.kt`, `MenuViagem.kt`, `TelaAjustes.kt`, `LinhaJanela`, `GravacaoViagem`, `CapturasTelas.kt`, `Ajustes`, `GravadorArquivos`, `HELIOS: aplicativo Android (versão 0.4.6)`, `Calibrador`, `Portas.kt`, `RealceOpenCv.kt`, `VozAndroid`, `ClassificadorOlho`, `.iniciarCaptura`?**
  _High betweenness centrality (0.143) - this node is a cross-community bridge._
- **Are the 52 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 52 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _65 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MonitorMotorista` be split into smaller, more focused modules?**
  _Cohesion score 0.06490134994807892 - nodes in this community are weakly interconnected._
- **Why does `MonitorMotorista` connect `MonitorMotorista` to `Ponto2D`, `EstadoMotorista`, `AnalisadorMovimento`, `Motivo`, `Leitura`, `.iniciarCaptura`, `HeliosViewModel.kt`, `Perclos`, `HeliosViewModel`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `Ponto2D` be split into smaller, more focused modules?**
  _Cohesion score 0.0689873417721519 - nodes in this community are weakly interconnected._