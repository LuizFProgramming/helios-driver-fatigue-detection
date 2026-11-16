# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 109 files · ~720,974 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1957 nodes · 5618 edges · 87 communities (61 shown, 26 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 397 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `46fc9369`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Instalacao.kt
- TelaDirecaoTest
- planoDeAlerta
- HeliosViewModel
- Ponto2D
- MonitorMotorista
- TelaDirecao.kt
- Motivo
- FonteCameraX.kt
- TelaHistorico.kt
- CapturasTelas.kt
- BancoHelios
- Tarefa
- AgregadorSessao
- HeliosViewModel.kt
- Calibracao
- AnalisadorMovimento
- DetectorMediapipe.kt
- GravacaoViagem
- Tema.kt
- MonitorMotorista.kt
- FakeBanco
- Esp32.kt
- Perclos
- Composable
- PosicaoCamera
- MenuViagem.kt
- GravadorArquivos
- Registros.kt
- CoresTest
- MainActivity.kt
- amostra
- LinhaJanela
- Ajustes
- Calibrador
- ExtratorJpeg
- Metricas
- Ajustes.kt
- HELIOS: aplicativo Android (versão 0.4.13)
- PerfilInstalacao
- Evento
- GravacaoViagemTest.kt
- EsquemaBancoTest
- ConfiguracaoInstalacao
- RecalibracaoViagem
- Sessao.kt
- Assistente
- EstadoMotorista
- MapeadorFaceMesh
- TelaAjustes
- GravacaoViagem.kt
- PoseCabeca
- VerificacaoTest
- RealceOpenCv.kt
- VozAndroid
- PassoRoteiro
- ColetorZona
- ZonasEManobraTest
- Visor.kt
- TelaInstalacaoTest
- modeloDeTela
- NotaInstalacao
- ConducaoRoteiroTest
- Verificação
- ClassificadorOlhoLiteRt.kt
- CaixaRosto
- RoteiroTest
- DadosTest
- Próximos passos
- RecortesOlhos
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro
- Verificacao
- Tela
- MalhaVisor
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
10. `TelaDirecaoTest` - 40 edges

## Surprising Connections (you probably didn't know these)
- `O que mudou na versão 0.4.6` --references--> `TelaDirecaoTest`  [INFERRED]
  README.md → app/src/test/kotlin/br/unicid/helios/app/ui/TelaDirecaoTest.kt
- `Decisão` --references--> `ConfiguracaoDeteccao`  [INFERRED]
  docs/ADR-014-distracao-por-zonas.md → dominio/src/main/kotlin/br/unicid/helios/dominio/ConfiguracaoDeteccao.kt
- `O que mudou na versão 0.4.5` --references--> `CoresTest`  [INFERRED]
  README.md → protocolos/src/test/kotlin/br/unicid/helios/protocolos/CoresTest.kt
- `O que mudou na versão 0.4.2` --references--> `UsoMonitoramento`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/Ajustes.kt
- `O que mudou na versão 0.4.2` --references--> `RepositorioAjustes`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/Ajustes.kt

## Import Cycles
- None detected.

## Communities (87 total, 26 thin omitted)

### Community 0 - "Instalacao.kt"
Cohesion: 0.05
Nodes (15): BuffersVisor, DesenhistaMalha, Pontos, Adendo de 16/11/2026: olhos abertos chamados de fechados, ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Fora desta decisão (+7 more)

### Community 1 - "TelaDirecaoTest"
Cohesion: 0.07
Nodes (27): MainActivity, Alvo, Espaco, TemaHelios(), TelaDirecaoTest, Achados da Fase 1, ADR-012: Interface guiada por diretrizes publicadas, Capturas (+19 more)

### Community 2 - "planoDeAlerta"
Cohesion: 0.08
Nodes (14): AlarmeAndroid, SaidaAlarme, comVolumeMinimo(), ControladorFala, gerarCicloPcm(), gerarRetornoPcm(), padraoPara(), PadraoSonoro (+6 more)

### Community 3 - "HeliosViewModel"
Cohesion: 0.07
Nodes (3): HeliosViewModel, ViagemAtual, SessaoGravada

### Community 5 - "Ponto2D"
Cohesion: 0.10
Nodes (15): distancia(), earMedio(), poseDaMatriz(), posicaoIris(), razaoAspecto(), OlhoAvaliado, AMBOS, DIREITO (+7 more)

### Community 6 - "MonitorMotorista"
Cohesion: 0.17
Nodes (4): MonitorMotorista, Olho, Quadro, MonitorMotoristaTest

### Community 7 - "TelaDirecao.kt"
Cohesion: 0.09
Nodes (18): EstadoFotos, TelaUi, AcoesMenu, Chip(), Chips(), ConteudoDirecao(), desenhoDoIcone(), estadoInicial() (+10 more)

### Community 8 - "Motivo"
Cohesion: 0.09
Nodes (22): Decisão, Classificacao, Classificador, Indicadores, Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA (+14 more)

### Community 9 - "FonteCameraX.kt"
Cohesion: 0.08
Nodes (9): FonteCameraX, Conexao, CONECTADA, CONECTANDO, DESCONECTADA, ConsumidorVisual, DetectorFacial, FonteDeImagens (+1 more)

### Community 10 - "TelaHistorico.kt"
Cohesion: 0.10
Nodes (15): duracaoTexto(), horaTexto(), pctTexto(), data(), Detalhe(), duracao(), FotoCheia(), GraficoPerclos() (+7 more)

### Community 11 - "CapturasTelas.kt"
Cohesion: 0.20
Nodes (27): QuadroVisor, Ajustes(), Direcao(), DirecaoAtento(), DirecaoCritico(), DirecaoDistraido(), DirecaoEstreitaComVisor(), DirecaoFadiga() (+19 more)

### Community 12 - "BancoHelios"
Cohesion: 0.08
Nodes (4): BancoHelios, EsquemaBanco, FotoSalva, RepositorioViagens

### Community 13 - "Tarefa"
Cohesion: 0.08
Nodes (23): AlertaEsperado, BOCEJO, DISTRACAO, MICROSSONO, NENHUM, SONO, IntervaloRoteiro, OlhoEsperado (+15 more)

### Community 14 - "AgregadorSessao"
Cohesion: 0.17
Nodes (10): AgregadorSessao, AgregadorSessaoBordasTest, AgregadorSessaoTest, CnnNaJanelaTest, DesempenhoNaJanelaTest, EstadoNoInicioDoEventoTest, leitura(), metricas() (+2 more)

### Community 15 - "HeliosViewModel.kt"
Cohesion: 0.08
Nodes (6): Malha, Diagnostico, quer(), receber(), OlhosCnn, sinalDeRetorno()

### Community 16 - "Calibracao"
Cohesion: 0.15
Nodes (11): Calibracao, distancia(), Zona, ESPELHO_DIREITO, ESPELHO_ESQUERDO, PAINEL, RETROVISOR, zonaMaisProxima() (+3 more)

### Community 17 - "AnalisadorMovimento"
Cohesion: 0.09
Nodes (6): AcelerometroAndroid, SensorEventListener, FonteInercial, Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 18 - "DetectorMediapipe.kt"
Cohesion: 0.11
Nodes (4): DetectorMediapipe, ClassificadorOlho, Blendshapes, IndicesFaceMesh

### Community 19 - "GravacaoViagem"
Cohesion: 0.14
Nodes (6): ArquivoCrescente, GravacaoViagem, PedidoFoto, SessaoAtiva, Números desta versão, O que mudou na versão 0.4.2

### Community 20 - "Tema.kt"
Cohesion: 0.07
Nodes (11): cor(), esquema(), textoDoSinal(), Tipos, Tom, AMARELO, AMBAR, LARANJA (+3 more)

### Community 21 - "MonitorMotorista.kt"
Cohesion: 0.10
Nodes (22): ConfiguracaoDeteccao, classificarFechamento(), DetectorSustentado, duracaoEmCurso(), EmCurso, Encerrada, Fase, Inativa (+14 more)

### Community 22 - "FakeBanco"
Cohesion: 0.16
Nodes (5): AjustesFotos, ContextoLeitura, FakeBanco, GravacaoViagemTest, FakeBanco

### Community 23 - "Esp32.kt"
Cohesion: 0.10
Nodes (4): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback

### Community 25 - "Perclos"
Cohesion: 0.13
Nodes (7): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SinaisTest

### Community 26 - "Composable"
Cohesion: 0.15
Nodes (27): EstadoInstalacao, AcaoCalibracao(), AcaoZonas(), AcoesInstalacao, AnelDaNota(), AvisoMovimento(), ComImagem(), EtapaCalibracao() (+19 more)

### Community 27 - "PosicaoCamera"
Cohesion: 0.12
Nodes (14): Alternativa(), Carro, Cartao(), DesenhoDoCarro(), EtapaPosicao(), LadoMotorista, DIREITO, ESQUERDO (+6 more)

### Community 28 - "MenuViagem.kt"
Cohesion: 0.10
Nodes (7): BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu(), textoEstadoFotos()

### Community 30 - "Registros.kt"
Cohesion: 0.13
Nodes (15): DetalheViagem, EventoDetalhe, fotoDoEvento(), FotoGuardada, FotoRegistrada, fotosDoEvento(), miniatura(), nomeFoto() (+7 more)

### Community 31 - "CoresTest"
Cohesion: 0.16
Nodes (8): canal(), contrasteWcag(), CorDeSinal, corDoPulso(), EsquemaCores, luminancia(), misturar(), CoresTest

### Community 32 - "MainActivity.kt"
Cohesion: 0.11
Nodes (4): Aviso(), HeliosApp(), PedirCamera(), PerguntaFotos()

### Community 33 - "amostra"
Cohesion: 0.23
Nodes (3): amostra(), CnnEmModoSombraTest, OlharEPiscadasTest

### Community 34 - "LinhaJanela"
Cohesion: 0.21
Nodes (4): Csv, LinhaEvento, LinhaJanela, CsvTest

### Community 35 - "Ajustes"
Cohesion: 0.18
Nodes (9): Ajustes, AcoesRoteiro, Concluido(), EmAndamento(), Esperando(), Inicio(), Opcao(), TelaRoteiro() (+1 more)

### Community 36 - "Calibrador"
Cohesion: 0.17
Nodes (6): Calibrador, desvioPadrao(), mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 38 - "Metricas"
Cohesion: 0.23
Nodes (6): Leitura, Metricas, MetricasOlhar, Totais, ZonasNaJanelaTest, VisorTest

### Community 39 - "Ajustes.kt"
Cohesion: 0.11
Nodes (12): ConfigEsp32, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento, Ajuste, com() (+4 more)

### Community 40 - "HELIOS: aplicativo Android (versão 0.4.13)"
Cohesion: 0.10
Nodes (20): Como abrir, HELIOS: aplicativo Android (versão 0.4.13), Limitações conhecidas, O que mudou na versão 0.2.0, O que mudou na versão 0.3.0 (depois do primeiro teste no celular), O que mudou na versão 0.4.0 (depois do segundo teste no celular), O que mudou na versão 0.4.10, O que mudou na versão 0.4.11 (+12 more)

### Community 41 - "PerfilInstalacao"
Cohesion: 0.24
Nodes (4): Desvios, PerfilInstalacao, ArquivoPerfis, ArquivoPerfisTest

### Community 43 - "GravacaoViagemTest.kt"
Cohesion: 0.11
Nodes (3): FakeGravador, FakeUso, FakeGravador

### Community 44 - "EsquemaBancoTest"
Cohesion: 0.18
Nodes (7): EsquemaBancoTest, ADR-014: distração com zonas do carro e manobra, Consequências, Contexto, Decisão, Manobra: impacto para o motorista e para o TCC, Verificação

### Community 45 - "ConfiguracaoInstalacao"
Cohesion: 0.18
Nodes (17): angulo(), ConfiguracaoInstalacao, dentro(), Dica, ABAIXAR, AFASTAR, APROXIMAR, CENTRALIZAR (+9 more)

### Community 46 - "RecalibracaoViagem"
Cohesion: 0.26
Nodes (3): Pose, RecalibracaoViagem, RecalibracaoViagemTest

### Community 47 - "Sessao.kt"
Cohesion: 0.22
Nodes (6): AcumuladorJanela, destino(), JanelaResumo, media(), percentil95(), ResumoSessao

### Community 49 - "EstadoMotorista"
Cohesion: 0.15
Nodes (13): PoliticaEvidencia, EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO, FADIGA (+5 more)

### Community 50 - "MapeadorFaceMesh"
Cohesion: 0.24
Nodes (3): MapeadorFaceMesh, ResultadoFaceMesh, MapeadorFaceMeshTest

### Community 51 - "TelaAjustes"
Cohesion: 0.26
Nodes (14): dataTexto(), Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoAlertas(), SecaoFotos() (+6 more)

### Community 55 - "RealceOpenCv.kt"
Cohesion: 0.16
Nodes (3): RealceOpenCv, PreProcessador, O que mudou na versão 0.4.4

### Community 57 - "PassoRoteiro"
Cohesion: 0.31
Nodes (7): ConducaoRoteiro, EstadoRoteiro, PassoRoteiro, AvisoRoteiro, avisosDoRoteiro(), Bipe, Falar

### Community 60 - "Visor.kt"
Cohesion: 0.31
Nodes (12): abertura(), cnn(), Eixos2D, eixosDaCabeca(), graus(), linhasDeMetricas(), num(), olhar() (+4 more)

### Community 63 - "NotaInstalacao"
Cohesion: 0.30
Nodes (10): assistente(), InstalacaoResumo(), InstalacaoResumoEstreito(), InstalacaoResumoRefazer(), NotaInstalacao, PartesNota, Qualidade, ACEITAVEL (+2 more)

### Community 64 - "ConducaoRoteiroTest"
Cohesion: 0.21
Nodes (6): ConducaoRoteiroTest, ADR-015: roteiro de teste guiado e coleta para a CNN com óculos, Consequências, Contexto, Decisão, Verificação

### Community 65 - "Verificação"
Cohesion: 0.18
Nodes (7): ADR-013: instalação da câmera e calibração obrigatória, Consequências, Contexto, Decisão, Revisão na 0.4.10: movimento vira aviso, Verificação, TextosInstalacaoTest

### Community 67 - "CaixaRosto"
Cohesion: 0.44
Nodes (3): CaixaRosto, brilhoDoRosto(), EnquadramentoTest

### Community 70 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 72 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 74 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 75 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 76 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 77 - "ADR-010: frases de alerta geradas fora do carro"
Cohesion: 0.33
Nodes (5): ADR-010: frases de alerta geradas fora do carro, Consequências, Contexto, Decisão, Fora desta decisão

### Community 78 - "Verificacao"
Cohesion: 0.33
Nodes (6): Verificacao, ANGULO, CAMERA_PARADA, LUZ, ROSTO_INTEIRO, TAMANHO

### Community 79 - "Tela"
Cohesion: 0.40
Nodes (5): Tela, AJUSTES, DIRECAO, HISTORICO, ROTEIRO

## Knowledge Gaps
- **120 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+115 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 434 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **26 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `HeliosViewModel` connect `HeliosViewModel` to `Instalacao.kt`, `planoDeAlerta`, `TelaDirecao.kt`, `FonteCameraX.kt`, `CapturasTelas.kt`, `BancoHelios`, `HeliosViewModel.kt`, `DetectorMediapipe.kt`, `GravacaoViagem`, `MonitorMotorista.kt`, `Esp32.kt`, `Perclos`, `Composable`, `PosicaoCamera`, `GravadorArquivos`, `Registros.kt`, `MainActivity.kt`, `Ajustes`, `Metricas`, `Ajustes.kt`, `RecalibracaoViagem`, `Assistente`, `EstadoMotorista`, `RealceOpenCv.kt`, `VozAndroid`, `PassoRoteiro`?**
  _High betweenness centrality (0.110) - this node is a cross-community bridge._
- **Are the 54 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 54 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _120 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Instalacao.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.05473684210526316 - nodes in this community are weakly interconnected._
- **Why does `EstadoMotorista` connect `EstadoMotorista` to `TelaDirecaoTest`, `planoDeAlerta`, `HeliosViewModel`, `Ponto2D`, `MonitorMotorista`, `Motivo`, `CapturasTelas.kt`, `BancoHelios`, `Tarefa`, `AgregadorSessao`, `HeliosViewModel.kt`, `Calibracao`, `GravacaoViagem`, `MonitorMotorista.kt`, `FakeBanco`, `TelaDirecaoTest.kt`, `Registros.kt`, `CoresTest`, `LinhaJanela`, `Metricas`, `Evento`, `GravacaoViagemTest.kt`, `Sessao.kt`, `GravacaoViagem.kt`, `modeloDeTela`?**
  _High betweenness centrality (0.100) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `ConducaoRoteiro`) actually correct?**
  _`HeliosViewModel` has 4 INFERRED edges - model-reasoned connections that need verification._
- **Should `TelaDirecaoTest` be split into smaller, more focused modules?**
  _Cohesion score 0.07219662058371736 - nodes in this community are weakly interconnected._