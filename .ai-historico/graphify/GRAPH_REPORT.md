# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 109 files · ~680,495 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1946 nodes · 5570 edges · 89 communities (64 shown, 25 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 395 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `abbfaed1`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- CapturasTelas.kt
- DetectorMediapipe.kt
- planoDeAlerta
- CoresTest
- HeliosViewModel
- TelaInstalacao.kt
- FonteCameraX.kt
- TelaHistorico.kt
- Motivo
- Calibracao
- LinhaJanela
- AgregadorSessao
- TelaDirecao.kt
- HeliosViewModel.kt
- MonitorMotorista
- TelaDirecaoTest
- Tema.kt
- Esp32.kt
- GravacaoViagem
- FakeBanco
- Evento
- MainActivity.kt
- EstadoMotorista
- GravadorArquivos
- OlhoCnnTest
- Ajustes.kt
- Metricas
- Perclos
- ConfiguracaoInstalacao
- TelaDirecaoTest.kt
- Calibrador
- Tarefa
- PerfilInstalacao
- amostra
- RecortesOlhos
- ZonaOlhar
- ExtratorJpeg
- HELIOS: aplicativo Android (versão 0.4.11)
- GravacaoViagemTest.kt
- EsquemaBancoTest
- Ponto2D
- RecalibracaoViagem
- DadosTest
- OlhoCnnDouradoTest.kt
- AcelerometroAndroid.kt
- GeometriaTest
- Leitura
- RealceOpenCv.kt
- GravacaoViagem.kt
- .extrair
- AnalisadorMovimento
- Assistente
- MapeadorFaceMesh
- Ajustes
- PosicaoCamera
- VerificacaoTest
- LinhaEvento
- PassoRoteiro
- ColetorZona
- PoseCabeca
- Visor.kt
- ClassificadorOlho
- Csv
- Totais
- VozAndroid
- TelaAjustes
- ConducaoRoteiroTest
- .aoReceberVisual
- CaixaRosto
- RoteiroTest
- PainelMenu
- Próximos passos
- Carro
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- .leitura
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro
- ADR-013: instalação da câmera e calibração obrigatória
- Tela
- HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)

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
10. `Ponto2D` - 39 edges

## Surprising Connections (you probably didn't know these)
- `Números desta versão` --references--> `GravacaoViagem`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/GravacaoViagem.kt
- `Achados da Fase 1` --references--> `TemaHelios()`  [INFERRED]
  docs/ADR-012-interface.md → app/src/main/kotlin/br/unicid/helios/app/ui/Tema.kt
- `O que mudou na versão 0.4.6` --references--> `TelaDirecaoTest`  [INFERRED]
  README.md → app/src/test/kotlin/br/unicid/helios/app/ui/TelaDirecaoTest.kt
- `Decisão` --references--> `ConfiguracaoDeteccao`  [INFERRED]
  docs/ADR-014-distracao-por-zonas.md → dominio/src/main/kotlin/br/unicid/helios/dominio/ConfiguracaoDeteccao.kt
- `O que mudou na versão 0.4.5` --references--> `CoresTest`  [INFERRED]
  README.md → protocolos/src/test/kotlin/br/unicid/helios/protocolos/CoresTest.kt

## Import Cycles
- None detected.

## Communities (89 total, 25 thin omitted)

### Community 0 - "CapturasTelas.kt"
Cohesion: 0.08
Nodes (40): QuadroVisor, Ajustes(), assistente(), Direcao(), DirecaoAtento(), DirecaoCritico(), DirecaoDistraido(), DirecaoFadiga() (+32 more)

### Community 1 - "DetectorMediapipe.kt"
Cohesion: 0.07
Nodes (8): DetectorMediapipe, DesenhistaMalha, Malha, Pontos, OlhosCnn, Blendshapes, IndicesFaceMesh, cnnFechado()

### Community 2 - "planoDeAlerta"
Cohesion: 0.08
Nodes (14): AlarmeAndroid, SaidaAlarme, comVolumeMinimo(), ControladorFala, gerarCicloPcm(), gerarRetornoPcm(), padraoPara(), PadraoSonoro (+6 more)

### Community 4 - "CoresTest"
Cohesion: 0.06
Nodes (32): MainActivity, Alvo, Espaco, Achados da Fase 1, ADR-012: Interface guiada por diretrizes publicadas, Capturas, Contexto, Decisões da Fase 1 (tokens de design) (+24 more)

### Community 5 - "HeliosViewModel"
Cohesion: 0.07
Nodes (3): EstadoInstalacao, HeliosViewModel, sinalDeRetorno()

### Community 6 - "TelaInstalacao.kt"
Cohesion: 0.10
Nodes (27): AcaoCalibracao(), AcaoZonas(), AcoesInstalacao, AcoesPerfis, AnelDaNota(), AvisoMovimento(), Cartao(), ComImagem() (+19 more)

### Community 7 - "FonteCameraX.kt"
Cohesion: 0.08
Nodes (9): FonteCameraX, Conexao, CONECTADA, CONECTANDO, DESCONECTADA, ConsumidorVisual, DetectorFacial, FonteDeImagens (+1 more)

### Community 8 - "TelaHistorico.kt"
Cohesion: 0.09
Nodes (16): dataTexto(), duracaoTexto(), horaTexto(), pctTexto(), data(), Detalhe(), duracao(), FotoCheia() (+8 more)

### Community 9 - "Motivo"
Cohesion: 0.09
Nodes (22): Decisão, Classificacao, Classificador, Indicadores, Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA (+14 more)

### Community 10 - "Calibracao"
Cohesion: 0.09
Nodes (22): Calibracao, ConfiguracaoDeteccao, classificarFechamento(), duracaoEmCurso(), SuavizadorEma, AmostraFacial, Quadro, TipoEvento (+14 more)

### Community 11 - "LinhaJanela"
Cohesion: 0.08
Nodes (5): BancoHelios, EsquemaBanco, FotoSalva, RepositorioViagens, LinhaJanela

### Community 12 - "AgregadorSessao"
Cohesion: 0.15
Nodes (12): AgregadorSessao, percentil95(), PoliticaEvidencia, AgregadorSessaoBordasTest, AgregadorSessaoTest, CnnNaJanelaTest, DesempenhoNaJanelaTest, EstadoNoInicioDoEventoTest (+4 more)

### Community 13 - "TelaDirecao.kt"
Cohesion: 0.09
Nodes (12): Chip(), Chips(), ConteudoDirecao(), desenhoDoIcone(), FaixaInferior(), Indicador(), Miniatura(), pulsoCritico() (+4 more)

### Community 14 - "HeliosViewModel.kt"
Cohesion: 0.09
Nodes (14): BuffersVisor, DetalheViagem, Diagnostico, EventoDetalhe, TelaUi, ViagemAtual, SessaoGravada, estadoInicial() (+6 more)

### Community 16 - "TelaDirecaoTest"
Cohesion: 0.18
Nodes (3): AcoesMenu, TelaDirecao(), TelaDirecaoTest

### Community 17 - "Tema.kt"
Cohesion: 0.07
Nodes (11): cor(), esquema(), fundoDoSinal(), Tipos, Tom, AMARELO, AMBAR, LARANJA (+3 more)

### Community 18 - "Esp32.kt"
Cohesion: 0.10
Nodes (4): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback

### Community 19 - "GravacaoViagem"
Cohesion: 0.17
Nodes (5): ArquivoCrescente, GravacaoViagem, PedidoFoto, SessaoAtiva, O que mudou na versão 0.4.2

### Community 20 - "FakeBanco"
Cohesion: 0.17
Nodes (4): ContextoLeitura, FakeBanco, GravacaoViagemTest, FakeBanco

### Community 21 - "Evento"
Cohesion: 0.12
Nodes (7): DetectorSustentado, EmCurso, Encerrada, Fase, Inativa, Olhada, Evento

### Community 22 - "MainActivity.kt"
Cohesion: 0.09
Nodes (4): Aviso(), HeliosApp(), PedirCamera(), PerguntaFotos()

### Community 23 - "EstadoMotorista"
Cohesion: 0.14
Nodes (14): EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO, FADIGA, SEM_ROSTO (+6 more)

### Community 25 - "OlhoCnnTest"
Cohesion: 0.15
Nodes (4): argbDeCinza(), CaixaRecorte, RecorteOlho, OlhoCnnTest

### Community 26 - "Ajustes.kt"
Cohesion: 0.09
Nodes (18): ConfigEsp32, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento, Ajuste (+10 more)

### Community 27 - "Metricas"
Cohesion: 0.21
Nodes (7): Zona, ESPELHO_DIREITO, ESPELHO_ESQUERDO, PAINEL, RETROVISOR, Metricas, MetricasOlhar

### Community 28 - "Perclos"
Cohesion: 0.15
Nodes (6): EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SinaisTest

### Community 29 - "ConfiguracaoInstalacao"
Cohesion: 0.12
Nodes (24): angulo(), ConfiguracaoInstalacao, dentro(), Dica, ABAIXAR, AFASTAR, APROXIMAR, CENTRALIZAR (+16 more)

### Community 31 - "Calibrador"
Cohesion: 0.17
Nodes (6): Calibrador, Desvios, mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 32 - "Tarefa"
Cohesion: 0.10
Nodes (21): AlertaEsperado, BOCEJO, DISTRACAO, MICROSSONO, NENHUM, SONO, IntervaloRoteiro, OlhoEsperado (+13 more)

### Community 33 - "PerfilInstalacao"
Cohesion: 0.20
Nodes (5): Verificação, PerfilInstalacao, ArquivoPerfis, ArquivoPerfisTest, TextosInstalacaoTest

### Community 35 - "RecortesOlhos"
Cohesion: 0.17
Nodes (4): RecortesOlhos, RegistroRecortes, TextosRoteiro, RoteiroCsvTest

### Community 36 - "ZonaOlhar"
Cohesion: 0.25
Nodes (4): distancia(), zonaMaisProxima(), ZonaOlhar, ZonasEManobraTest

### Community 38 - "HELIOS: aplicativo Android (versão 0.4.11)"
Cohesion: 0.10
Nodes (19): Como abrir, HELIOS: aplicativo Android (versão 0.4.11), Limitações conhecidas, Números desta versão, O que mudou na versão 0.2.0, O que mudou na versão 0.3.0 (depois do primeiro teste no celular), O que mudou na versão 0.4.0 (depois do segundo teste no celular), O que mudou na versão 0.4.10 (+11 more)

### Community 39 - "GravacaoViagemTest.kt"
Cohesion: 0.11
Nodes (3): FakeGravador, FakeUso, FakeGravador

### Community 40 - "EsquemaBancoTest"
Cohesion: 0.18
Nodes (7): EsquemaBancoTest, ADR-014: distração com zonas do carro e manobra, Consequências, Contexto, Decisão, Manobra: impacto para o motorista e para o TCC, Verificação

### Community 41 - "Ponto2D"
Cohesion: 0.21
Nodes (6): earMedio(), posicaoIris(), razaoAspecto(), Ponto2D, SeisPontos, seisComRazao()

### Community 42 - "RecalibracaoViagem"
Cohesion: 0.26
Nodes (3): Pose, RecalibracaoViagem, RecalibracaoViagemTest

### Community 43 - "DadosTest"
Cohesion: 0.20
Nodes (3): LinhaQuadro, RegistroQuadros, DadosTest

### Community 44 - "OlhoCnnDouradoTest.kt"
Cohesion: 0.17
Nodes (7): Adendo de 16/11/2026: olhos abertos chamados de fechados, ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Fora desta decisão, Caso, OlhoCnnDouradoTest

### Community 45 - "AcelerometroAndroid.kt"
Cohesion: 0.14
Nodes (3): AcelerometroAndroid, SensorEventListener, FonteInercial

### Community 47 - "Leitura"
Cohesion: 0.25
Nodes (6): Leitura, AcumuladorJanela, destino(), JanelaResumo, media(), ResumoSessao

### Community 48 - "RealceOpenCv.kt"
Cohesion: 0.15
Nodes (3): RealceOpenCv, PreProcessador, O que mudou na versão 0.4.4

### Community 49 - "GravacaoViagem.kt"
Cohesion: 0.18
Nodes (4): FotoGuardada, nomeFoto(), PoliticaRetencao, FotosTest

### Community 50 - ".extrair"
Cohesion: 0.15
Nodes (7): distancia(), Olho, OlhoAvaliado, AMBOS, DIREITO, ESQUERDO, Sinais

### Community 51 - "AnalisadorMovimento"
Cohesion: 0.23
Nodes (3): Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 53 - "MapeadorFaceMesh"
Cohesion: 0.24
Nodes (3): MapeadorFaceMesh, ResultadoFaceMesh, MapeadorFaceMeshTest

### Community 54 - "Ajustes"
Cohesion: 0.28
Nodes (9): Ajustes, AcoesRoteiro, Concluido(), EmAndamento(), Esperando(), Inicio(), Opcao(), TelaRoteiro() (+1 more)

### Community 55 - "PosicaoCamera"
Cohesion: 0.15
Nodes (8): LadoMotorista, DIREITO, ESQUERDO, PosicaoCamera, CENTRO_PAINEL, COLUNA_VOLANTE, PILAR_A, RETROVISOR

### Community 58 - "PassoRoteiro"
Cohesion: 0.27
Nodes (8): ConducaoRoteiro, EstadoRoteiro, Decisão, PassoRoteiro, AvisoRoteiro, avisosDoRoteiro(), Bipe, Falar

### Community 59 - "ColetorZona"
Cohesion: 0.23
Nodes (3): desvioPadrao(), ColetorZona, NotaEZonaTest

### Community 61 - "Visor.kt"
Cohesion: 0.31
Nodes (12): abertura(), cnn(), Eixos2D, eixosDaCabeca(), graus(), linhasDeMetricas(), num(), olhar() (+4 more)

### Community 64 - "Totais"
Cohesion: 0.28
Nodes (3): Totais, MalhaVisor, VisorTest

### Community 66 - "TelaAjustes"
Cohesion: 0.42
Nodes (11): EstadoFotos, Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoAlertas(), SecaoFotos() (+3 more)

### Community 67 - "ConducaoRoteiroTest"
Cohesion: 0.24
Nodes (5): ConducaoRoteiroTest, ADR-015: roteiro de teste guiado e coleta para a CNN com óculos, Consequências, Contexto, Verificação

### Community 69 - "CaixaRosto"
Cohesion: 0.44
Nodes (3): CaixaRosto, brilhoDoRosto(), EnquadramentoTest

### Community 71 - "PainelMenu"
Cohesion: 0.43
Nodes (6): BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu()

### Community 72 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 74 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 77 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 78 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 79 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 80 - "ADR-010: frases de alerta geradas fora do carro"
Cohesion: 0.33
Nodes (5): ADR-010: frases de alerta geradas fora do carro, Consequências, Contexto, Decisão, Fora desta decisão

### Community 81 - "ADR-013: instalação da câmera e calibração obrigatória"
Cohesion: 0.33
Nodes (5): ADR-013: instalação da câmera e calibração obrigatória, Consequências, Contexto, Decisão, Revisão na 0.4.10: movimento vira aviso

### Community 82 - "Tela"
Cohesion: 0.40
Nodes (5): Tela, AJUSTES, DIRECAO, HISTORICO, ROTEIRO

## Knowledge Gaps
- **120 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+115 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 432 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **25 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HeliosViewModel` connect `HeliosViewModel` to `CapturasTelas.kt`, `DetectorMediapipe.kt`, `planoDeAlerta`, `FonteCameraX.kt`, `Calibracao`, `LinhaJanela`, `HeliosViewModel.kt`, `Esp32.kt`, `GravacaoViagem`, `MainActivity.kt`, `EstadoMotorista`, `GravadorArquivos`, `Ajustes.kt`, `RecalibracaoViagem`, `Leitura`, `RealceOpenCv.kt`, `Assistente`, `Ajustes`, `PosicaoCamera`, `PassoRoteiro`, `ClassificadorOlho`, `VozAndroid`, `TelaAjustes`, `.aoReceberVisual`?**
  _High betweenness centrality (0.121) - this node is a cross-community bridge._
- **Are the 54 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 54 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _120 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `CapturasTelas.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.07892107892107893 - nodes in this community are weakly interconnected._
- **Why does `EstadoMotorista` connect `EstadoMotorista` to `CapturasTelas.kt`, `planoDeAlerta`, `HeliosViewModel`, `Motivo`, `Calibracao`, `LinhaJanela`, `AgregadorSessao`, `HeliosViewModel.kt`, `MonitorMotorista`, `TelaDirecaoTest`, `GravacaoViagem`, `FakeBanco`, `Evento`, `Metricas`, `TelaDirecaoTest.kt`, `RecortesOlhos`, `GravacaoViagemTest.kt`, `DadosTest`, `Leitura`, `GravacaoViagem.kt`, `LinhaEvento`, `Totais`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `ConducaoRoteiro`) actually correct?**
  _`HeliosViewModel` has 4 INFERRED edges - model-reasoned connections that need verification._
- **Should `DetectorMediapipe.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.07288135593220339 - nodes in this community are weakly interconnected._