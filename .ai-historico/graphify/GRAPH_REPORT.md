# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 78 files · ~285,108 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 28 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1417 nodes · 3700 edges · 61 communities (43 shown, 18 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 287 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1ee6e746`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MonitorMotorista
- EstadoMotorista
- OlhoCnnTest
- AgregadorSessao
- Motivo
- TelaDirecao.kt
- TelaHistorico.kt
- HeliosViewModel.kt
- GravacaoViagemTest.kt
- BancoHelios
- MenuViagem.kt
- MainActivity.kt
- TelaAjustes.kt
- Perclos
- Tema.kt
- HeliosViewModel
- DetectorMediapipe.kt
- TelaDirecaoTest
- CoresTest
- FonteCameraX.kt
- Icone
- Ponto2D
- CapturasTelas.kt
- GravacaoViagem
- LinhaEvento
- GravadorArquivos
- TipoEvento
- ExtratorJpeg
- Ajustes
- Calibrador
- modeloDeTela
- AcelerometroAndroid.kt
- Portas.kt
- AnalisadorMovimento
- MapeadorFaceMesh
- Leitura
- VozAndroid
- GravacaoViagem.kt
- RealceOpenCv.kt
- AmostraFacial
- Visor.kt
- HELIOS: aplicativo Android (versão 0.4.6)
- GeometriaTest
- EsquemaBancoTest
- ClassificadorOlhoLiteRt.kt
- .processar
- Próximos passos
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro
- Retencao
- MalhaVisor

## God Nodes (most connected - your core abstractions)
1. `MonitorMotorista` - 97 edges
2. `HeliosViewModel` - 74 edges
3. `EstadoMotorista` - 60 edges
4. `Leitura` - 46 edges
5. `Motivo` - 41 edges
6. `amostra()` - 38 edges
7. `TelaDirecaoTest` - 35 edges
8. `MonitorMotoristaTest` - 35 edges
9. `GravacaoViagem` - 34 edges
10. `Ponto2D` - 33 edges

## Surprising Connections (you probably didn't know these)
- `Números desta versão` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `Achados da Fase 1` --references--> `TemaHelios()`  [INFERRED]
  docs/ADR-012-interface.md → app/src/main/kotlin/br/unicid/helios/app/ui/Tema.kt
- `O que mudou na versão 0.4.6` --references--> `TelaDirecaoTest`  [INFERRED]
  README.md → app/src/test/kotlin/br/unicid/helios/app/ui/TelaDirecaoTest.kt
- `O que mudou na versão 0.4.5` --references--> `CoresTest`  [INFERRED]
  README.md → protocolos/src/test/kotlin/br/unicid/helios/protocolos/CoresTest.kt
- `O que mudou na versão 0.4.2` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt

## Import Cycles
- None detected.

## Communities (61 total, 18 thin omitted)

### Community 0 - "MonitorMotorista"
Cohesion: 0.06
Nodes (19): Ferramentas obrigatórias, HELIOS: app Android de detecção de sonolência (TCC UNICID 2026), Calibracao, ConfiguracaoDeteccao, classificarFechamento(), DetectorSustentado, duracaoEmCurso(), EmCurso (+11 more)

### Community 1 - "EstadoMotorista"
Cohesion: 0.06
Nodes (23): AlarmeAndroid, SaidaAlarme, EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO (+15 more)

### Community 2 - "OlhoCnnTest"
Cohesion: 0.06
Nodes (12): BuffersVisor, DesenhistaMalha, Malha, Pontos, receber(), OlhosCnn, IndicesFaceMesh, argbDeCinza() (+4 more)

### Community 3 - "AgregadorSessao"
Cohesion: 0.10
Nodes (18): AcumuladorJanela, AgregadorSessao, JanelaResumo, media(), percentil95(), PoliticaEvidencia, ResumoSessao, Evento (+10 more)

### Community 4 - "Motivo"
Cohesion: 0.08
Nodes (25): ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Decisão, Fora desta decisão, Classificacao, Classificador, Indicadores (+17 more)

### Community 5 - "TelaDirecao.kt"
Cohesion: 0.09
Nodes (19): Diagnostico, QuadroVisor, TelaUi, Chip(), Chips(), ConteudoDirecao(), desenhoDoIcone(), estadoInicial() (+11 more)

### Community 6 - "TelaHistorico.kt"
Cohesion: 0.08
Nodes (15): dataTexto(), horaTexto(), pctTexto(), data(), Detalhe(), duracao(), FotoCheia(), GraficoPerclos() (+7 more)

### Community 7 - "HeliosViewModel.kt"
Cohesion: 0.07
Nodes (4): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback

### Community 8 - "GravacaoViagemTest.kt"
Cohesion: 0.09
Nodes (6): FakeBanco, FakeGravador, FakeUso, GravacaoViagemTest, FakeBanco, FakeGravador

### Community 9 - "BancoHelios"
Cohesion: 0.09
Nodes (6): BancoHelios, EsquemaBanco, FotoSalva, RepositorioViagens, SessaoGravada, LinhaJanela

### Community 10 - "MenuViagem.kt"
Cohesion: 0.09
Nodes (9): ViagemAtual, duracaoTexto(), AcoesMenu, BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem() (+1 more)

### Community 11 - "MainActivity.kt"
Cohesion: 0.08
Nodes (8): Aviso(), HeliosApp(), PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO, HISTORICO

### Community 12 - "TelaAjustes.kt"
Cohesion: 0.10
Nodes (9): EstadoFotos, Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoFotos(), TelaAjustes() (+1 more)

### Community 13 - "Perclos"
Cohesion: 0.12
Nodes (8): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SuavizadorEma, SinaisTest

### Community 14 - "Tema.kt"
Cohesion: 0.07
Nodes (9): esquema(), Tipos, Tom, AMARELO, AMBAR, LARANJA, NEUTRO, VERDE (+1 more)

### Community 16 - "DetectorMediapipe.kt"
Cohesion: 0.12
Nodes (3): DetectorMediapipe, ClassificadorOlho, Blendshapes

### Community 18 - "CoresTest"
Cohesion: 0.16
Nodes (8): canal(), contrasteWcag(), CorDeSinal, corDoPulso(), EsquemaCores, luminancia(), misturar(), CoresTest

### Community 19 - "FonteCameraX.kt"
Cohesion: 0.11
Nodes (5): FonteCameraX, Conexao, CONECTADA, CONECTANDO, DESCONECTADA

### Community 20 - "Icone"
Cohesion: 0.09
Nodes (24): MainActivity, Alvo, Espaco, Achados da Fase 1, ADR-012: Interface guiada por diretrizes publicadas, Capturas, Contexto, Decisões da Fase 1 (tokens de design) (+16 more)

### Community 21 - "Ponto2D"
Cohesion: 0.18
Nodes (7): distancia(), earMedio(), posicaoIris(), razaoAspecto(), Ponto2D, SeisPontos, seisComRazao()

### Community 22 - "CapturasTelas.kt"
Cohesion: 0.25
Nodes (14): Ajustes(), Direcao(), DirecaoAtento(), DirecaoCritico(), DirecaoDistraido(), DirecaoFadiga(), DirecaoParadoComVisor(), DirecaoSemRosto() (+6 more)

### Community 23 - "GravacaoViagem"
Cohesion: 0.19
Nodes (3): GravacaoViagem, PedidoFoto, SessaoAtiva

### Community 24 - "LinhaEvento"
Cohesion: 0.17
Nodes (3): Csv, LinhaEvento, CsvTest

### Community 27 - "TipoEvento"
Cohesion: 0.16
Nodes (17): DetalheViagem, EventoDetalhe, TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MICROSSONO (+9 more)

### Community 29 - "Ajustes"
Cohesion: 0.17
Nodes (9): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento (+1 more)

### Community 30 - "Calibrador"
Cohesion: 0.22
Nodes (5): Calibrador, mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 31 - "modeloDeTela"
Cohesion: 0.25
Nodes (6): apoio(), detalhePadrao(), duracao(), EstadoTela, modeloDeTela(), ModeloDeTelaTest

### Community 32 - "AcelerometroAndroid.kt"
Cohesion: 0.15
Nodes (3): AcelerometroAndroid, SensorEventListener, FonteInercial

### Community 33 - "Portas.kt"
Cohesion: 0.19
Nodes (5): ConsumidorVisual, DetectorFacial, FonteDeImagens, Imagem, PreProcessador

### Community 34 - "AnalisadorMovimento"
Cohesion: 0.22
Nodes (3): Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 35 - "MapeadorFaceMesh"
Cohesion: 0.25
Nodes (3): MapeadorFaceMesh, ResultadoFaceMesh, MapeadorFaceMeshTest

### Community 36 - "Leitura"
Cohesion: 0.36
Nodes (6): Leitura, Metricas, MetricasOlhar, Totais, PoseCabeca, VisorTest

### Community 38 - "GravacaoViagem.kt"
Cohesion: 0.21
Nodes (4): FotoGuardada, nomeFoto(), PoliticaRetencao, FotosTest

### Community 40 - "AmostraFacial"
Cohesion: 0.23
Nodes (6): OlhoAvaliado, AMBOS, DIREITO, ESQUERDO, Sinais, AmostraFacial

### Community 41 - "Visor.kt"
Cohesion: 0.31
Nodes (12): abertura(), cnn(), Eixos2D, eixosDaCabeca(), graus(), linhasDeMetricas(), num(), olhar() (+4 more)

### Community 42 - "HELIOS: aplicativo Android (versão 0.4.6)"
Cohesion: 0.14
Nodes (13): Como abrir, HELIOS: aplicativo Android (versão 0.4.6), Limitações conhecidas, Números desta versão, O que mudou na versão 0.3.0 (depois do primeiro teste no celular), O que mudou na versão 0.4.0 (depois do segundo teste no celular), O que mudou na versão 0.4.1 (fotos que não apareciam), O que mudou na versão 0.4.3 (+5 more)

### Community 47 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 48 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 50 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 51 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 52 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 53 - "ADR-010: frases de alerta geradas fora do carro"
Cohesion: 0.33
Nodes (5): ADR-010: frases de alerta geradas fora do carro, Consequências, Contexto, Decisão, Fora desta decisão

### Community 54 - "Retencao"
Cohesion: 0.40
Nodes (5): Retencao, AO_ENCERRAR, DUAS_HORAS, SO_LIMITE, TRES_HORAS

## Knowledge Gaps
- **65 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+60 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 355 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **18 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HeliosViewModel` connect `HeliosViewModel` to `MonitorMotorista`, `EstadoMotorista`, `OlhoCnnTest`, `TelaDirecao.kt`, `HeliosViewModel.kt`, `BancoHelios`, `MenuViagem.kt`, `MainActivity.kt`, `TelaAjustes.kt`, `Perclos`, `DetectorMediapipe.kt`, `GravacaoViagem`, `LinhaEvento`, `GravadorArquivos`, `TipoEvento`, `Ajustes`, `Calibrador`, `Portas.kt`, `Leitura`, `VozAndroid`, `.processar`?**
  _High betweenness centrality (0.141) - this node is a cross-community bridge._
- **Are the 52 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 52 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _65 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MonitorMotorista` be split into smaller, more focused modules?**
  _Cohesion score 0.06433063791554358 - nodes in this community are weakly interconnected._
- **Why does `MonitorMotorista` connect `MonitorMotorista` to `EstadoMotorista`, `AnalisadorMovimento`, `AgregadorSessao`, `Motivo`, `Leitura`, `HeliosViewModel.kt`, `AmostraFacial`, `Perclos`, `HeliosViewModel`, `Ponto2D`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `EstadoMotorista` be split into smaller, more focused modules?**
  _Cohesion score 0.057297297297297295 - nodes in this community are weakly interconnected._