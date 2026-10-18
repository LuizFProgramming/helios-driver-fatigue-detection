# Graph Report - helios-android-v2  (2026-10-18)

## Corpus Check
- 65 files · ~41,083 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 12 file(s) not represented in the graph (top: .xml 6, .properties 3, (none) 1)

## Summary
- 1083 nodes · 2810 edges · 54 communities (36 shown, 18 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 202 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- MonitorMotorista
- SessaoTest.kt
- HeliosViewModel
- GeometriaTest
- planoDeAlerta
- Motivo
- TelaDirecao.kt
- MainActivity.kt
- TelaHistorico.kt
- MenuViagem.kt
- AnalisadorMovimento
- Perclos
- Esp32.kt
- GravadorArquivos
- DetectorMediapipe.kt
- DesenhistaMalha
- FonteCameraX.kt
- ExtratorJpeg
- TelaAjustes.kt
- Calibrador
- BancoHelios
- Test
- GravacaoViagem
- Registros.kt
- Portas.kt
- RealceOpenCv.kt
- HeliosViewModel.kt
- LinhaEvento
- Tema.kt
- HELIOS: aplicativo Android (versão 0.4.1)
- TipoEvento
- Próximos passos
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- Leitura
- MonitorMotorista.kt
- Test
- Evento
- .extrair
- Ajustes
- AcelerometroAndroid.kt
- VozAndroid
- ConfiguracaoDeteccao
- .aoReceberVisual
- Retencao
- MalhaVisor

## God Nodes (most connected - your core abstractions)
1. `MonitorMotorista` - 94 edges
2. `HeliosViewModel` - 71 edges
3. `EstadoMotorista` - 45 edges
4. `Leitura` - 35 edges
5. `MonitorMotoristaTest` - 35 edges
6. `Motivo` - 34 edges
7. `amostra()` - 34 edges
8. `GravacaoViagem` - 30 edges
9. `TipoEvento` - 30 edges
10. `Ponto2D` - 28 edges

## Surprising Connections (you probably didn't know these)
- `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` --references--> `MonitorMotorista`  [INFERRED]
  CLAUDE.md → dominio/src/main/kotlin/br/unicid/helios/dominio/monitor/MonitorMotorista.kt
- `AjustesFotos` --references--> `Retencao`  [EXTRACTED]
  app/src/main/kotlin/br/unicid/helios/app/Ajustes.kt → protocolos/src/main/kotlin/br/unicid/helios/protocolos/Registros.kt
- `Ajustes` --references--> `Calibracao`  [EXTRACTED]
  app/src/main/kotlin/br/unicid/helios/app/Ajustes.kt → dominio/src/main/kotlin/br/unicid/helios/dominio/calibracao/Calibrador.kt
- `SessaoAtiva` --calls--> `AgregadorSessao`  [EXTRACTED]
  app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt → dominio/src/main/kotlin/br/unicid/helios/dominio/sessao/Sessao.kt
- `SessaoAtiva` --calls--> `PoliticaEvidencia`  [EXTRACTED]
  app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt → dominio/src/main/kotlin/br/unicid/helios/dominio/sessao/Sessao.kt

## Import Cycles
- None detected.

## Communities (54 total, 18 thin omitted)

### Community 0 - "MonitorMotorista"
Cohesion: 0.13
Nodes (8): Ferramentas obrigatórias, HELIOS: app Android de detecção de sonolência (TCC UNICID 2026), Calibracao, MonitorMotorista, Quadro, amostra(), MonitorMotoristaTest, OlharEPiscadasTest

### Community 1 - "SessaoTest.kt"
Cohesion: 0.16
Nodes (10): AcumuladorJanela, AgregadorSessao, JanelaResumo, PoliticaEvidencia, ResumoSessao, AgregadorSessaoBordasTest, AgregadorSessaoTest, leitura() (+2 more)

### Community 3 - "GeometriaTest"
Cohesion: 0.19
Nodes (3): poseDaMatriz(), seisComRazao(), GeometriaTest

### Community 4 - "planoDeAlerta"
Cohesion: 0.06
Nodes (21): AlarmeAndroid, SaidaAlarme, EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO (+13 more)

### Community 5 - "Motivo"
Cohesion: 0.10
Nodes (20): Classificacao, Classificador, Indicadores, Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA, DISTRACAO_ACUMULADA (+12 more)

### Community 6 - "TelaDirecao.kt"
Cohesion: 0.11
Nodes (16): QuadroVisor, TelaUi, AcoesMenu, Calibrando(), ConteudoDirecao(), estadoInicial(), Olho(), PainelDiagnostico() (+8 more)

### Community 7 - "MainActivity.kt"
Cohesion: 0.08
Nodes (9): Aviso(), HeliosApp(), MainActivity, PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO (+1 more)

### Community 8 - "TelaHistorico.kt"
Cohesion: 0.11
Nodes (13): horaTexto(), data(), Detalhe(), duracao(), FotoCheia(), GraficoPerclos(), Linha(), LinhaEvento() (+5 more)

### Community 9 - "MenuViagem.kt"
Cohesion: 0.09
Nodes (9): duracaoTexto(), pctTexto(), BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu() (+1 more)

### Community 10 - "AnalisadorMovimento"
Cohesion: 0.22
Nodes (3): Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 11 - "Perclos"
Cohesion: 0.12
Nodes (9): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SuavizadorEma, instantes() (+1 more)

### Community 12 - "Esp32.kt"
Cohesion: 0.09
Nodes (9): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback, Conexao, CONECTADA, CONECTANDO, DESCONECTADA (+1 more)

### Community 15 - "DesenhistaMalha"
Cohesion: 0.21
Nodes (4): BuffersVisor, DesenhistaMalha, Malha, Pontos

### Community 18 - "TelaAjustes.kt"
Cohesion: 0.11
Nodes (9): EstadoFotos, Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoFotos(), TelaAjustes() (+1 more)

### Community 19 - "Calibrador"
Cohesion: 0.22
Nodes (5): Calibrador, mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 20 - "BancoHelios"
Cohesion: 0.10
Nodes (5): BancoHelios, FotoSalva, RepositorioViagens, SessaoGravada, LinhaEvento

### Community 21 - "Test"
Cohesion: 0.20
Nodes (5): FotoGuardada, nomeFoto(), PoliticaRetencao, CsvTest, FotosTest

### Community 22 - "GravacaoViagem"
Cohesion: 0.15
Nodes (3): GravacaoViagem, PedidoFoto, SessaoAtiva

### Community 23 - "Registros.kt"
Cohesion: 0.40
Nodes (6): DetalheViagem, EventoDetalhe, fotoDoEvento(), FotoRegistrada, fotosDoEvento(), miniatura()

### Community 24 - "Portas.kt"
Cohesion: 0.17
Nodes (3): ConsumidorVisual, DetectorFacial, FonteInercial

### Community 28 - "Tema.kt"
Cohesion: 0.10
Nodes (6): dataTexto(), TemaHelios(), Tipos, cronometro(), detalhePadrao(), Paleta

### Community 29 - "HELIOS: aplicativo Android (versão 0.4.1)"
Cohesion: 0.17
Nodes (11): Como abrir, HELIOS: aplicativo Android (versão 0.4.1), Limitações conhecidas, Números desta versão, O que mudou na versão 0.2.0, O que mudou na versão 0.3.0 (depois do primeiro teste no celular), O que mudou na versão 0.4.0 (depois do segundo teste no celular), O que mudou na versão 0.4.1 (fotos que não apareciam) (+3 more)

### Community 30 - "TipoEvento"
Cohesion: 0.18
Nodes (11): TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MICROSSONO, OLHAR_DESVIADO, PISCADA (+3 more)

### Community 31 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 32 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 34 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 35 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 36 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 42 - "Leitura"
Cohesion: 0.20
Nodes (15): Leitura, Metricas, MetricasOlhar, Totais, abertura(), Eixos2D, eixosDaCabeca(), graus() (+7 more)

### Community 43 - "MonitorMotorista.kt"
Cohesion: 0.20
Nodes (10): distancia(), earMedio(), posicaoIris(), razaoAspecto(), AmostraFacial, Ponto2D, PoseCabeca, SeisPontos (+2 more)

### Community 44 - "Test"
Cohesion: 0.20
Nodes (5): MapeadorFaceMesh, ResultadoFaceMesh, modeloDeTela(), MapeadorFaceMeshTest, ModeloDeTelaTest

### Community 45 - "Evento"
Cohesion: 0.17
Nodes (6): DetectorSustentado, EmCurso, Encerrada, Fase, Inativa, Evento

### Community 46 - ".extrair"
Cohesion: 0.17
Nodes (6): Olho, OlhoAvaliado, AMBOS, DIREITO, ESQUERDO, Sinais

### Community 47 - "Ajustes"
Cohesion: 0.22
Nodes (7): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM

### Community 50 - "ConfiguracaoDeteccao"
Cohesion: 0.29
Nodes (4): ConfiguracaoDeteccao, classificarFechamento(), duracaoEmCurso(), EventosTest

### Community 52 - "Retencao"
Cohesion: 0.40
Nodes (5): Retencao, AO_ENCERRAR, DUAS_HORAS, SO_LIMITE, TRES_HORAS

## Knowledge Gaps
- **48 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+43 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 261 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **18 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HeliosViewModel` connect `HeliosViewModel` to `MonitorMotorista`, `planoDeAlerta`, `TelaDirecao.kt`, `MainActivity.kt`, `Perclos`, `Esp32.kt`, `GravadorArquivos`, `DetectorMediapipe.kt`, `DesenhistaMalha`, `TelaAjustes.kt`, `Calibrador`, `BancoHelios`, `GravacaoViagem`, `Registros.kt`, `HeliosViewModel.kt`, `LinhaEvento`, `Leitura`, `Ajustes`, `VozAndroid`, `.aoReceberVisual`?**
  _High betweenness centrality (0.178) - this node is a cross-community bridge._
- **Are the 51 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`ao abrir os olhos depois do critico o alarme cai na hora e vem a fadiga pos-sono por 2 min`()`) actually correct?**
  _`MonitorMotorista` has 51 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _48 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MonitorMotorista` be split into smaller, more focused modules?**
  _Cohesion score 0.12698412698412698 - nodes in this community are weakly interconnected._
- **Why does `MonitorMotorista` connect `MonitorMotorista` to `HeliosViewModel`, `planoDeAlerta`, `Motivo`, `AnalisadorMovimento`, `MonitorMotorista.kt`, `Leitura`, `Evento`, `.extrair`, `Perclos`, `ConfiguracaoDeteccao`, `HeliosViewModel.kt`?**
  _High betweenness centrality (0.162) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `HeliosViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.11051693404634581 - nodes in this community are weakly interconnected._