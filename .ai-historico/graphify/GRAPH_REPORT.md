# Graph Report - helios-android-v2  (2026-11-15)

## Corpus Check
- 69 files · ~45,211 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: .xml 6, .properties 3, (none) 2)

## Summary
- 1175 nodes · 3046 edges · 59 communities (39 shown, 20 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 227 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `33d4ee61`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Ponto2D
- planoDeAlerta
- MonitorMotorista
- GravacaoViagemTest.kt
- TelaDirecao.kt
- MainActivity.kt
- Esp32.kt
- HeliosViewModel
- Perclos
- MenuViagem.kt
- AgregadorSessao
- DesenhistaMalha
- TelaAjustes.kt
- TelaHistorico.kt
- DetectorMediapipe.kt
- FonteCameraX.kt
- GravacaoViagem
- GravadorArquivos
- LinhaEvento
- Detalhe
- LinhaJanela
- Evento
- Leitura
- ExtratorJpeg
- Calibrador
- AcelerometroAndroid.kt
- Portas.kt
- AnalisadorMovimento
- BancoHelios
- GravacaoViagem.kt
- ClassificadorTest
- Sessao.kt
- Motivo
- RealceOpenCv.kt
- EstadoMotorista
- MonitorMotorista.kt
- EsquemaBancoTest
- Tema.kt
- HELIOS: aplicativo Android (versão 0.4.3)
- Registros.kt
- TipoEvento
- .aoReceberVisual
- Próximos passos
- Classificador
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro
- PoliticaEvidencia
- MalhaVisor

## God Nodes (most connected - your core abstractions)
1. `MonitorMotorista` - 96 edges
2. `HeliosViewModel` - 72 edges
3. `EstadoMotorista` - 51 edges
4. `Leitura` - 39 edges
5. `amostra()` - 36 edges
6. `MonitorMotoristaTest` - 35 edges
7. `Motivo` - 34 edges
8. `GravacaoViagem` - 33 edges
9. `TipoEvento` - 31 edges
10. `Evento` - 30 edges

## Surprising Connections (you probably didn't know these)
- `Números desta versão` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `O que mudou na versão 0.4.2` --references--> `UsoMonitoramento`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/Ajustes.kt
- `O que mudou na versão 0.4.2` --references--> `RepositorioAjustes`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/Ajustes.kt
- `O que mudou na versão 0.4.2` --references--> `HeliosViewModel`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/HeliosViewModel.kt
- `O que mudou na versão 0.4.2` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt

## Import Cycles
- None detected.

## Communities (59 total, 20 thin omitted)

### Community 0 - "Ponto2D"
Cohesion: 0.07
Nodes (19): distancia(), earMedio(), poseDaMatriz(), posicaoIris(), razaoAspecto(), OlhoAvaliado, AMBOS, DIREITO (+11 more)

### Community 1 - "planoDeAlerta"
Cohesion: 0.06
Nodes (16): AlarmeAndroid, VozAndroid, SaidaAlarme, SaidaVoz, ControladorFala, gerarCicloPcm(), gerarRetornoPcm(), padraoPara() (+8 more)

### Community 2 - "MonitorMotorista"
Cohesion: 0.12
Nodes (9): Ferramentas obrigatórias, HELIOS: app Android de detecção de sonolência (TCC UNICID 2026), Calibracao, MonitorMotorista, Olho, Quadro, amostra(), MonitorMotoristaTest (+1 more)

### Community 3 - "GravacaoViagemTest.kt"
Cohesion: 0.05
Nodes (19): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento (+11 more)

### Community 4 - "TelaDirecao.kt"
Cohesion: 0.12
Nodes (17): Diagnostico, QuadroVisor, TelaUi, AcoesMenu, Calibrando(), ConteudoDirecao(), estadoInicial(), Olho() (+9 more)

### Community 5 - "MainActivity.kt"
Cohesion: 0.08
Nodes (9): Aviso(), HeliosApp(), MainActivity, PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO (+1 more)

### Community 6 - "Esp32.kt"
Cohesion: 0.10
Nodes (4): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback

### Community 8 - "Perclos"
Cohesion: 0.12
Nodes (8): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SuavizadorEma, SinaisTest

### Community 9 - "MenuViagem.kt"
Cohesion: 0.10
Nodes (9): EstadoFotos, ViagemAtual, BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu() (+1 more)

### Community 10 - "AgregadorSessao"
Cohesion: 0.20
Nodes (9): AgregadorSessao, instantes(), AgregadorSessaoBordasTest, AgregadorSessaoTest, DesempenhoNaJanelaTest, EstadoNoInicioDoEventoTest, leitura(), metricas() (+1 more)

### Community 11 - "DesenhistaMalha"
Cohesion: 0.17
Nodes (7): BuffersVisor, DesenhistaMalha, Malha, Pontos, IndicesFaceMesh, Eixos2D, eixosDaCabeca()

### Community 12 - "TelaAjustes.kt"
Cohesion: 0.11
Nodes (7): Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoFotos(), TelaAjustes()

### Community 13 - "TelaHistorico.kt"
Cohesion: 0.07
Nodes (3): cronometro(), detalhePadrao(), Paleta

### Community 14 - "DetectorMediapipe.kt"
Cohesion: 0.13
Nodes (3): DetectorMediapipe, Blendshapes, ResultadoFaceMesh

### Community 16 - "GravacaoViagem"
Cohesion: 0.20
Nodes (4): GravacaoViagem, PedidoFoto, SessaoAtiva, O que mudou na versão 0.4.2

### Community 18 - "LinhaEvento"
Cohesion: 0.11
Nodes (5): EsquemaBanco, FotoSalva, RepositorioViagens, SessaoGravada, LinhaEvento

### Community 19 - "Detalhe"
Cohesion: 0.17
Nodes (16): dataTexto(), duracaoTexto(), horaTexto(), pctTexto(), data(), Detalhe(), duracao(), FotoCheia() (+8 more)

### Community 20 - "LinhaJanela"
Cohesion: 0.21
Nodes (3): Csv, LinhaJanela, CsvTest

### Community 21 - "Evento"
Cohesion: 0.17
Nodes (6): DetectorSustentado, EmCurso, Encerrada, Fase, Inativa, Evento

### Community 22 - "Leitura"
Cohesion: 0.28
Nodes (14): Leitura, Metricas, MetricasOlhar, Totais, PoseCabeca, abertura(), graus(), linhasDeMetricas() (+6 more)

### Community 24 - "Calibrador"
Cohesion: 0.22
Nodes (5): Calibrador, mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 25 - "AcelerometroAndroid.kt"
Cohesion: 0.15
Nodes (3): AcelerometroAndroid, SensorEventListener, FonteInercial

### Community 26 - "Portas.kt"
Cohesion: 0.16
Nodes (7): Conexao, CONECTADA, CONECTANDO, DESCONECTADA, ConsumidorVisual, DetectorFacial, FonteDeImagens

### Community 27 - "AnalisadorMovimento"
Cohesion: 0.22
Nodes (3): Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 29 - "GravacaoViagem.kt"
Cohesion: 0.19
Nodes (4): FotoGuardada, nomeFoto(), PoliticaRetencao, FotosTest

### Community 31 - "Sessao.kt"
Cohesion: 0.24
Nodes (5): AcumuladorJanela, JanelaResumo, media(), percentil95(), ResumoSessao

### Community 32 - "Motivo"
Cohesion: 0.12
Nodes (16): Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA, DISTRACAO_ACUMULADA, EVENTO_BRUSCO_COM_FADIGA, MICROSSONOS_FREQUENTES, OLHAR_DESVIADO (+8 more)

### Community 34 - "EstadoMotorista"
Cohesion: 0.14
Nodes (9): EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO, FADIGA, SEM_ROSTO (+1 more)

### Community 37 - "MonitorMotorista.kt"
Cohesion: 0.26
Nodes (4): ConfiguracaoDeteccao, classificarFechamento(), duracaoEmCurso(), EventosTest

### Community 39 - "Tema.kt"
Cohesion: 0.20
Nodes (3): cor(), TemaHelios(), Tipos

### Community 40 - "HELIOS: aplicativo Android (versão 0.4.3)"
Cohesion: 0.17
Nodes (11): Como abrir, HELIOS: aplicativo Android (versão 0.4.3), Limitações conhecidas, Números desta versão, O que mudou na versão 0.3.0 (depois do primeiro teste no celular), O que mudou na versão 0.4.0 (depois do segundo teste no celular), O que mudou na versão 0.4.1 (fotos que não apareciam), O que mudou na versão 0.4.3 (+3 more)

### Community 41 - "Registros.kt"
Cohesion: 0.40
Nodes (6): DetalheViagem, EventoDetalhe, fotoDoEvento(), FotoRegistrada, fotosDoEvento(), miniatura()

### Community 42 - "TipoEvento"
Cohesion: 0.18
Nodes (11): TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MICROSSONO, OLHAR_DESVIADO, PISCADA (+3 more)

### Community 44 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 46 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 48 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 49 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 50 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 51 - "ADR-010: frases de alerta geradas fora do carro"
Cohesion: 0.33
Nodes (5): ADR-010: frases de alerta geradas fora do carro, Consequências, Contexto, Decisão, Fora desta decisão

## Knowledge Gaps
- **48 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+43 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 281 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **20 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HeliosViewModel` connect `HeliosViewModel` to `planoDeAlerta`, `MonitorMotorista`, `GravacaoViagemTest.kt`, `TelaDirecao.kt`, `MainActivity.kt`, `Esp32.kt`, `Perclos`, `MenuViagem.kt`, `DesenhistaMalha`, `DetectorMediapipe.kt`, `GravacaoViagem`, `GravadorArquivos`, `LinhaEvento`, `LinhaJanela`, `Leitura`, `Calibrador`, `Portas.kt`, `BancoHelios`, `RealceOpenCv.kt`, `EstadoMotorista`, `HeliosViewModel.kt`, `Registros.kt`, `.aoReceberVisual`?**
  _High betweenness centrality (0.185) - this node is a cross-community bridge._
- **Are the 51 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`ao abrir os olhos depois do critico o alarme cai na hora e vem a fadiga pos-sono por 2 min`()`) actually correct?**
  _`MonitorMotorista` has 51 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _48 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Ponto2D` be split into smaller, more focused modules?**
  _Cohesion score 0.06812291743798593 - nodes in this community are weakly interconnected._
- **Why does `MonitorMotorista` connect `MonitorMotorista` to `Ponto2D`, `EstadoMotorista`, `HeliosViewModel.kt`, `MonitorMotorista.kt`, `HeliosViewModel`, `Perclos`, `AgregadorSessao`, `Classificador`, `Evento`, `Leitura`, `AnalisadorMovimento`?**
  _High betweenness centrality (0.169) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `planoDeAlerta` be split into smaller, more focused modules?**
  _Cohesion score 0.05706760316066725 - nodes in this community are weakly interconnected._