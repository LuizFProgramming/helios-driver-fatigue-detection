# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 77 files · ~236,634 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 28 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1344 nodes · 3492 edges · 54 communities (37 shown, 17 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 274 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `9d564694`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MonitorMotorista
- Leitura
- EstadoMotorista
- OlhoCnnTest
- GravacaoViagem
- CoresTest
- Ponto2D
- TelaDirecao.kt
- Tema.kt
- TelaHistorico.kt
- MainActivity.kt
- AnalisadorMovimento
- Esp32.kt
- TelaAjustes.kt
- CapturasTelas.kt
- MenuViagem.kt
- DetectorMediapipe.kt
- HeliosViewModel
- Perclos
- LinhaJanela
- GravadorArquivos
- FonteCameraX.kt
- FakeBanco
- HeliosViewModel.kt
- LinhaEvento
- ExtratorJpeg
- Registros.kt
- Calibrador
- Portas.kt
- BancoHelios
- RealceOpenCv.kt
- HELIOS: aplicativo Android (versão 0.4.5)
- ClassificadorTest
- Motivo
- GravacaoViagemTest.kt
- Classificador
- MonitorMotorista.kt
- VozAndroid
- EsquemaBancoTest
- ClassificadorOlho
- TipoEvento
- Fase
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
3. `EstadoMotorista` - 56 edges
4. `Leitura` - 44 edges
5. `amostra()` - 38 edges
6. `Motivo` - 36 edges
7. `MonitorMotoristaTest` - 35 edges
8. `GravacaoViagem` - 33 edges
9. `Ponto2D` - 33 edges
10. `TipoEvento` - 31 edges

## Surprising Connections (you probably didn't know these)
- `Números desta versão` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `O que mudou na versão 0.4.5` --references--> `CoresTest`  [INFERRED]
  README.md → protocolos/src/test/kotlin/br/unicid/helios/protocolos/CoresTest.kt
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
Nodes (17): Ferramentas obrigatórias, HELIOS: app Android de detecção de sonolência (TCC UNICID 2026), Calibracao, MonitorMotorista, Olho, OlhoAvaliado, AMBOS, DIREITO (+9 more)

### Community 1 - "Leitura"
Cohesion: 0.07
Nodes (34): Leitura, Metricas, MetricasOlhar, Totais, AcumuladorJanela, AgregadorSessao, JanelaResumo, media() (+26 more)

### Community 2 - "EstadoMotorista"
Cohesion: 0.05
Nodes (25): AlarmeAndroid, SaidaAlarme, EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO (+17 more)

### Community 3 - "OlhoCnnTest"
Cohesion: 0.06
Nodes (11): BuffersVisor, DesenhistaMalha, Malha, Pontos, receber(), OlhosCnn, argbDeCinza(), CaixaRecorte (+3 more)

### Community 4 - "GravacaoViagem"
Cohesion: 0.07
Nodes (21): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento (+13 more)

### Community 5 - "CoresTest"
Cohesion: 0.07
Nodes (30): MainActivity, Alvo, Espaco, ADR-012: Interface guiada por diretrizes publicadas, Capturas, Contexto, Decisões da Fase 1 (tokens de design), Fontes (+22 more)

### Community 6 - "Ponto2D"
Cohesion: 0.10
Nodes (12): distancia(), earMedio(), poseDaMatriz(), posicaoIris(), razaoAspecto(), Ponto2D, SeisPontos, seisComRazao() (+4 more)

### Community 7 - "TelaDirecao.kt"
Cohesion: 0.12
Nodes (17): Diagnostico, QuadroVisor, TelaUi, AcoesMenu, Calibrando(), ConteudoDirecao(), estadoInicial(), Olho() (+9 more)

### Community 8 - "Tema.kt"
Cohesion: 0.07
Nodes (14): cor(), esquema(), fundoDoSinal(), TemaHelios(), textoDoSinal(), Tipos, Achados da Fase 1, Tom (+6 more)

### Community 9 - "TelaHistorico.kt"
Cohesion: 0.11
Nodes (13): horaTexto(), data(), Detalhe(), duracao(), FotoCheia(), GraficoPerclos(), Linha(), LinhaEvento() (+5 more)

### Community 10 - "MainActivity.kt"
Cohesion: 0.07
Nodes (8): Aviso(), HeliosApp(), PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO, HISTORICO

### Community 11 - "AnalisadorMovimento"
Cohesion: 0.09
Nodes (6): AcelerometroAndroid, SensorEventListener, FonteInercial, Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 12 - "Esp32.kt"
Cohesion: 0.09
Nodes (4): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback

### Community 13 - "TelaAjustes.kt"
Cohesion: 0.10
Nodes (9): EstadoFotos, Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoFotos(), TelaAjustes() (+1 more)

### Community 14 - "CapturasTelas.kt"
Cohesion: 0.18
Nodes (16): Ajustes(), Direcao(), DirecaoAtento(), DirecaoCritico(), DirecaoDistraido(), DirecaoFadiga(), DirecaoSemRosto(), HistoricoDetalhe() (+8 more)

### Community 15 - "MenuViagem.kt"
Cohesion: 0.10
Nodes (7): ViagemAtual, BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu()

### Community 16 - "DetectorMediapipe.kt"
Cohesion: 0.12
Nodes (3): DetectorMediapipe, Blendshapes, IndicesFaceMesh

### Community 18 - "Perclos"
Cohesion: 0.14
Nodes (7): EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SuavizadorEma, SinaisTest

### Community 19 - "LinhaJanela"
Cohesion: 0.16
Nodes (3): Csv, LinhaJanela, CsvTest

### Community 22 - "FakeBanco"
Cohesion: 0.18
Nodes (4): FakeBanco, GravacaoViagemTest, FakeBanco, FakeGravador

### Community 24 - "LinhaEvento"
Cohesion: 0.12
Nodes (5): EsquemaBanco, FotoSalva, RepositorioViagens, SessaoGravada, LinhaEvento

### Community 26 - "Registros.kt"
Cohesion: 0.18
Nodes (9): DetalheViagem, EventoDetalhe, dataTexto(), duracaoTexto(), pctTexto(), fotoDoEvento(), FotoRegistrada, fotosDoEvento() (+1 more)

### Community 27 - "Calibrador"
Cohesion: 0.22
Nodes (5): Calibrador, mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 28 - "Portas.kt"
Cohesion: 0.15
Nodes (7): Conexao, CONECTADA, CONECTANDO, DESCONECTADA, ConsumidorVisual, DetectorFacial, FonteDeImagens

### Community 30 - "RealceOpenCv.kt"
Cohesion: 0.15
Nodes (3): RealceOpenCv, PreProcessador, O que mudou na versão 0.4.4

### Community 31 - "HELIOS: aplicativo Android (versão 0.4.5)"
Cohesion: 0.12
Nodes (13): Como abrir, HELIOS: aplicativo Android (versão 0.4.5), Limitações conhecidas, Números desta versão, O que mudou na versão 0.2.0, O que mudou na versão 0.3.0 (depois do primeiro teste no celular), O que mudou na versão 0.4.0 (depois do segundo teste no celular), O que mudou na versão 0.4.1 (fotos que não apareciam) (+5 more)

### Community 33 - "Motivo"
Cohesion: 0.12
Nodes (16): Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA, DISTRACAO_ACUMULADA, EVENTO_BRUSCO_COM_FADIGA, MICROSSONOS_FREQUENTES, OLHAR_DESVIADO (+8 more)

### Community 35 - "Classificador"
Cohesion: 0.25
Nodes (7): ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Decisão, Fora desta decisão, Classificador, Indicadores

### Community 36 - "MonitorMotorista.kt"
Cohesion: 0.26
Nodes (4): ConfiguracaoDeteccao, classificarFechamento(), duracaoEmCurso(), EventosTest

### Community 40 - "TipoEvento"
Cohesion: 0.18
Nodes (11): TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MICROSSONO, OLHAR_DESVIADO, PISCADA (+3 more)

### Community 41 - "Fase"
Cohesion: 0.42
Nodes (5): DetectorSustentado, EmCurso, Encerrada, Fase, Inativa

### Community 42 - "Próximos passos"
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
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 333 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **17 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MonitorMotorista` connect `MonitorMotorista` to `Leitura`, `EstadoMotorista`, `Classificador`, `MonitorMotorista.kt`, `Fase`, `AnalisadorMovimento`, `HeliosViewModel`, `Perclos`, `HeliosViewModel.kt`?**
  _High betweenness centrality (0.137) - this node is a cross-community bridge._
- **Are the 52 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 52 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _65 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MonitorMotorista` be split into smaller, more focused modules?**
  _Cohesion score 0.07148514851485148 - nodes in this community are weakly interconnected._
- **Why does `HeliosViewModel` connect `HeliosViewModel` to `MonitorMotorista`, `Leitura`, `EstadoMotorista`, `OlhoCnnTest`, `GravacaoViagem`, `TelaDirecao.kt`, `MainActivity.kt`, `Esp32.kt`, `TelaAjustes.kt`, `MenuViagem.kt`, `DetectorMediapipe.kt`, `GravadorArquivos`, `HeliosViewModel.kt`, `LinhaEvento`, `Registros.kt`, `Calibrador`, `Portas.kt`, `BancoHelios`, `RealceOpenCv.kt`, `HELIOS: aplicativo Android (versão 0.4.5)`, `VozAndroid`, `ClassificadorOlho`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `Leitura` be split into smaller, more focused modules?**
  _Cohesion score 0.06703146374829001 - nodes in this community are weakly interconnected._