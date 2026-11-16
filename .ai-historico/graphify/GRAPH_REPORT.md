# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 109 files · ~691,335 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1951 nodes · 5591 edges · 83 communities (58 shown, 25 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 395 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `df99d5b3`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- CapturasTelas.kt
- TelaDirecaoTest.kt
- Ponto2D
- MenuViagem.kt
- planoDeAlerta
- OlhoCnnTest
- HeliosViewModel
- TelaInstalacao.kt
- HELIOS: aplicativo Android (versão 0.4.12)
- TelaHistorico.kt
- FakeBanco
- TelaDirecao.kt
- FonteCameraX.kt
- BancoHelios
- AgregadorSessao
- Tema.kt
- MainActivity.kt
- AnalisadorMovimento
- Quadro
- Ajustes
- GravacaoViagem
- DetectorMediapipe.kt
- Esp32.kt
- MonitorMotorista
- GravacaoViagemTest.kt
- EstadoMotorista
- LinhaJanela
- GravadorArquivos
- PerfilInstalacao
- Metricas
- amostra
- ConfiguracaoInstalacao
- Perclos
- ClassificadorTest
- Calibrador
- Tarefa
- TipoEvento
- HeliosViewModel.kt
- Leitura
- ExtratorJpeg
- EsquemaBancoTest
- MonitorMotorista.kt
- RecalibracaoViagem
- Motivo
- GravacaoViagem.kt
- PosicaoCamera
- Assistente
- AmostraFacial
- PassoRoteiro
- VerificacaoTest
- RealceOpenCv.kt
- VozAndroid
- Zona
- PoseCabeca
- TelaRoteiro
- ColetorZona
- ZonasEManobraTest
- OlhosCnn
- TelaRoteiroTest.kt
- ConducaoRoteiroTest
- Verificação
- RoteiroCsvTest
- ClassificadorOlhoLiteRt.kt
- TelaAjustes
- CaixaRosto
- RoteiroTest
- DadosTest
- Próximos passos
- Carro
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro
- MonitorMotoristaTest.kt
- RegistroQuadros
- .linhas

## God Nodes (most connected - your core abstractions)
1. `MonitorMotorista` - 107 edges
2. `HeliosViewModel` - 100 edges
3. `EstadoMotorista` - 66 edges
4. `Metricas` - 53 edges
5. `Leitura` - 51 edges
6. `amostra()` - 50 edges
7. `Motivo` - 46 edges
8. `PoseCabeca` - 44 edges
9. `GravacaoViagem` - 42 edges
10. `TelaDirecaoTest` - 39 edges

## Surprising Connections (you probably didn't know these)
- `Números desta versão` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `Achados da Fase 1` --references--> `TemaHelios()`  [INFERRED]
  docs/ADR-012-interface.md → app/src/main/kotlin/br/unicid/helios/app/ui/Tema.kt
- `O que mudou na versão 0.4.6` --references--> `TelaDirecaoTest`  [INFERRED]
  README.md → app/src/test/kotlin/br/unicid/helios/app/ui/TelaDirecaoTest.kt
- `Decisão` --references--> `ConfiguracaoDeteccao`  [INFERRED]
  docs/ADR-014-distracao-por-zonas.md → dominio/src/main/kotlin/br/unicid/helios/dominio/ConfiguracaoDeteccao.kt
- `O que mudou na versão 0.4.2` --references--> `UsoMonitoramento`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/Ajustes.kt

## Import Cycles
- None detected.

## Communities (83 total, 25 thin omitted)

### Community 0 - "CapturasTelas.kt"
Cohesion: 0.09
Nodes (35): Ajustes(), assistente(), Direcao(), DirecaoAtento(), DirecaoCritico(), DirecaoDistraido(), DirecaoEstreitaComVisor(), DirecaoFadiga() (+27 more)

### Community 1 - "TelaDirecaoTest.kt"
Cohesion: 0.06
Nodes (25): MainActivity, Alvo, Espaco, TelaDirecaoTest, Achados da Fase 1, ADR-012: Interface guiada por diretrizes publicadas, Capturas, Contexto (+17 more)

### Community 2 - "Ponto2D"
Cohesion: 0.07
Nodes (19): Adendo de 16/11/2026: olhos abertos chamados de fechados, ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Fora desta decisão, distancia(), earMedio(), poseDaMatriz() (+11 more)

### Community 3 - "MenuViagem.kt"
Cohesion: 0.06
Nodes (7): BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu(), textoEstadoFotos()

### Community 4 - "planoDeAlerta"
Cohesion: 0.08
Nodes (14): AlarmeAndroid, SaidaAlarme, comVolumeMinimo(), ControladorFala, gerarCicloPcm(), gerarRetornoPcm(), padraoPara(), PadraoSonoro (+6 more)

### Community 5 - "OlhoCnnTest"
Cohesion: 0.09
Nodes (9): BuffersVisor, DesenhistaMalha, Malha, Pontos, argbDeCinza(), CaixaRecorte, cnnFechado(), RecorteOlho (+1 more)

### Community 6 - "HeliosViewModel"
Cohesion: 0.07
Nodes (3): EstadoInstalacao, HeliosViewModel, ViagemAtual

### Community 7 - "TelaInstalacao.kt"
Cohesion: 0.09
Nodes (33): AcaoCalibracao(), AcaoZonas(), AcoesInstalacao, AcoesPerfis, AnelDaNota(), AvisoMovimento(), Cartao(), ComImagem() (+25 more)

### Community 8 - "HELIOS: aplicativo Android (versão 0.4.12)"
Cohesion: 0.07
Nodes (28): canal(), contrasteWcag(), CorDeSinal, corDoPulso(), EsquemaCores, luminancia(), misturar(), CoresTest (+20 more)

### Community 9 - "TelaHistorico.kt"
Cohesion: 0.08
Nodes (16): dataTexto(), duracaoTexto(), horaTexto(), pctTexto(), data(), Detalhe(), duracao(), FotoCheia() (+8 more)

### Community 10 - "FakeBanco"
Cohesion: 0.11
Nodes (7): ContextoLeitura, FakeBanco, FakeGravador, GravacaoViagemTest, FakeBanco, FakeGravador, RecortesOlhos

### Community 11 - "TelaDirecao.kt"
Cohesion: 0.09
Nodes (17): EstadoFotos, QuadroVisor, TelaUi, AcoesMenu, Chip(), Chips(), ConteudoDirecao(), desenhoDoIcone() (+9 more)

### Community 12 - "FonteCameraX.kt"
Cohesion: 0.08
Nodes (9): FonteCameraX, Conexao, CONECTADA, CONECTANDO, DESCONECTADA, ConsumidorVisual, DetectorFacial, FonteDeImagens (+1 more)

### Community 13 - "BancoHelios"
Cohesion: 0.08
Nodes (5): BancoHelios, EsquemaBanco, FotoSalva, RepositorioViagens, SessaoGravada

### Community 14 - "AgregadorSessao"
Cohesion: 0.17
Nodes (10): AgregadorSessao, AgregadorSessaoBordasTest, AgregadorSessaoTest, CnnNaJanelaTest, DesempenhoNaJanelaTest, EstadoNoInicioDoEventoTest, leitura(), metricas() (+2 more)

### Community 15 - "Tema.kt"
Cohesion: 0.07
Nodes (13): Selo(), cor(), esquema(), fundoDoSinal(), textoDoSinal(), Tipos, Tom, AMARELO (+5 more)

### Community 16 - "MainActivity.kt"
Cohesion: 0.07
Nodes (9): Aviso(), HeliosApp(), PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO, HISTORICO (+1 more)

### Community 17 - "AnalisadorMovimento"
Cohesion: 0.09
Nodes (6): AcelerometroAndroid, SensorEventListener, FonteInercial, Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 19 - "Ajustes"
Cohesion: 0.08
Nodes (20): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento (+12 more)

### Community 20 - "GravacaoViagem"
Cohesion: 0.15
Nodes (5): ArquivoCrescente, GravacaoViagem, PedidoFoto, SessaoAtiva, O que mudou na versão 0.4.2

### Community 21 - "DetectorMediapipe.kt"
Cohesion: 0.12
Nodes (4): DetectorMediapipe, ClassificadorOlho, Blendshapes, IndicesFaceMesh

### Community 22 - "Esp32.kt"
Cohesion: 0.10
Nodes (4): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback

### Community 23 - "MonitorMotorista"
Cohesion: 0.14
Nodes (8): Ferramentas obrigatórias, HELIOS: app Android de detecção de sonolência (TCC UNICID 2026), Encerrada, Fase, Inativa, MonitorMotorista, Olhada, Evento

### Community 25 - "EstadoMotorista"
Cohesion: 0.14
Nodes (14): EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO, FADIGA, SEM_ROSTO (+6 more)

### Community 26 - "LinhaJanela"
Cohesion: 0.17
Nodes (4): Csv, LinhaEvento, LinhaJanela, CsvTest

### Community 28 - "PerfilInstalacao"
Cohesion: 0.18
Nodes (7): Desvios, PerfilInstalacao, distancia(), zonaMaisProxima(), ZonaOlhar, ArquivoPerfis, ArquivoPerfisTest

### Community 29 - "Metricas"
Cohesion: 0.16
Nodes (16): Metricas, Totais, abertura(), cnn(), Eixos2D, eixosDaCabeca(), graus(), linhasDeMetricas() (+8 more)

### Community 30 - "amostra"
Cohesion: 0.21
Nodes (3): Calibracao, amostra(), OlharEPiscadasTest

### Community 31 - "ConfiguracaoInstalacao"
Cohesion: 0.12
Nodes (24): angulo(), ConfiguracaoInstalacao, dentro(), Dica, ABAIXAR, AFASTAR, APROXIMAR, CENTRALIZAR (+16 more)

### Community 32 - "Perclos"
Cohesion: 0.15
Nodes (6): EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SinaisTest

### Community 33 - "ClassificadorTest"
Cohesion: 0.19
Nodes (5): Decisão, Classificacao, Classificador, Indicadores, ClassificadorTest

### Community 34 - "Calibrador"
Cohesion: 0.17
Nodes (6): Calibrador, desvioPadrao(), mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 35 - "Tarefa"
Cohesion: 0.10
Nodes (20): AlertaEsperado, BOCEJO, DISTRACAO, MICROSSONO, NENHUM, SONO, OlhoEsperado, ABERTO (+12 more)

### Community 36 - "TipoEvento"
Cohesion: 0.14
Nodes (18): DetalheViagem, EventoDetalhe, TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MANOBRA (+10 more)

### Community 37 - "HeliosViewModel.kt"
Cohesion: 0.10
Nodes (3): Diagnostico, ContadorEmJanela, sinalDeRetorno()

### Community 38 - "Leitura"
Cohesion: 0.20
Nodes (8): Leitura, AcumuladorJanela, destino(), JanelaResumo, media(), percentil95(), PoliticaEvidencia, ResumoSessao

### Community 40 - "EsquemaBancoTest"
Cohesion: 0.18
Nodes (7): EsquemaBancoTest, ADR-014: distração com zonas do carro e manobra, Consequências, Contexto, Decisão, Manobra: impacto para o motorista e para o TCC, Verificação

### Community 41 - "MonitorMotorista.kt"
Cohesion: 0.18
Nodes (7): ConfiguracaoDeteccao, classificarFechamento(), DetectorSustentado, duracaoEmCurso(), EmCurso, SuavizadorEma, EventosTest

### Community 42 - "RecalibracaoViagem"
Cohesion: 0.26
Nodes (3): Pose, RecalibracaoViagem, RecalibracaoViagemTest

### Community 43 - "Motivo"
Cohesion: 0.11
Nodes (18): Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA, DISTRACAO_ACUMULADA, EVENTO_BRUSCO_COM_FADIGA, MICROSSONOS_FREQUENTES, OLHAR_DESVIADO (+10 more)

### Community 44 - "GravacaoViagem.kt"
Cohesion: 0.18
Nodes (4): FotoGuardada, nomeFoto(), PoliticaRetencao, FotosTest

### Community 45 - "PosicaoCamera"
Cohesion: 0.14
Nodes (8): LadoMotorista, DIREITO, ESQUERDO, PosicaoCamera, CENTRO_PAINEL, COLUNA_VOLANTE, PILAR_A, RETROVISOR

### Community 47 - "AmostraFacial"
Cohesion: 0.18
Nodes (7): Olho, OlhoAvaliado, AMBOS, DIREITO, ESQUERDO, Sinais, AmostraFacial

### Community 48 - "PassoRoteiro"
Cohesion: 0.26
Nodes (8): ConducaoRoteiro, EstadoRoteiro, Decisão, PassoRoteiro, AvisoRoteiro, avisosDoRoteiro(), Bipe, Falar

### Community 50 - "RealceOpenCv.kt"
Cohesion: 0.16
Nodes (3): RealceOpenCv, PreProcessador, O que mudou na versão 0.4.4

### Community 52 - "Zona"
Cohesion: 0.23
Nodes (7): Zona, ESPELHO_DIREITO, ESPELHO_ESQUERDO, PAINEL, RETROVISOR, MetricasOlhar, ZonasNaJanelaTest

### Community 54 - "TelaRoteiro"
Cohesion: 0.29
Nodes (8): AcoesRoteiro, Concluido(), EmAndamento(), Esperando(), Inicio(), Opcao(), TelaRoteiro(), TemaHelios()

### Community 57 - "OlhosCnn"
Cohesion: 0.21
Nodes (3): quer(), receber(), OlhosCnn

### Community 59 - "ConducaoRoteiroTest"
Cohesion: 0.24
Nodes (5): ConducaoRoteiroTest, ADR-015: roteiro de teste guiado e coleta para a CNN com óculos, Consequências, Contexto, Verificação

### Community 60 - "Verificação"
Cohesion: 0.18
Nodes (7): ADR-013: instalação da câmera e calibração obrigatória, Consequências, Contexto, Decisão, Revisão na 0.4.10: movimento vira aviso, Verificação, TextosInstalacaoTest

### Community 63 - "TelaAjustes"
Cohesion: 0.53
Nodes (9): Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoAlertas(), SecaoFotos(), SecaoQuadros() (+1 more)

### Community 64 - "CaixaRosto"
Cohesion: 0.44
Nodes (3): CaixaRosto, brilhoDoRosto(), EnquadramentoTest

### Community 67 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 69 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

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

## Knowledge Gaps
- **120 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+115 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 433 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **25 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HeliosViewModel` connect `HeliosViewModel` to `planoDeAlerta`, `OlhoCnnTest`, `TelaDirecao.kt`, `FonteCameraX.kt`, `BancoHelios`, `MainActivity.kt`, `Ajustes`, `GravacaoViagem`, `DetectorMediapipe.kt`, `Esp32.kt`, `EstadoMotorista`, `GravadorArquivos`, `TipoEvento`, `HeliosViewModel.kt`, `Leitura`, `MonitorMotorista.kt`, `RecalibracaoViagem`, `PosicaoCamera`, `Assistente`, `PassoRoteiro`, `RealceOpenCv.kt`, `VozAndroid`, `OlhosCnn`?**
  _High betweenness centrality (0.126) - this node is a cross-community bridge._
- **Are the 54 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 54 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _120 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `CapturasTelas.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.08942161339421613 - nodes in this community are weakly interconnected._
- **Why does `EstadoMotorista` connect `EstadoMotorista` to `CapturasTelas.kt`, `TelaDirecaoTest.kt`, `planoDeAlerta`, `HeliosViewModel`, `FakeBanco`, `BancoHelios`, `AgregadorSessao`, `GravacaoViagem`, `MonitorMotorista`, `GravacaoViagemTest.kt`, `LinhaJanela`, `Metricas`, `ClassificadorTest`, `TipoEvento`, `HeliosViewModel.kt`, `Leitura`, `MonitorMotorista.kt`, `Motivo`, `GravacaoViagem.kt`, `Zona`, `MonitorMotoristaTest.kt`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `ConducaoRoteiro`) actually correct?**
  _`HeliosViewModel` has 4 INFERRED edges - model-reasoned connections that need verification._
- **Should `TelaDirecaoTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.057902973395931145 - nodes in this community are weakly interconnected._