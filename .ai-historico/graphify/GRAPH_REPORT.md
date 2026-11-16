# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 99 files · ~521,490 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1784 nodes · 5055 edges · 77 communities (53 shown, 24 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 364 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `78d62506`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- TelaDirecaoTest.kt
- Ponto2D
- AgregadorSessao
- planoDeAlerta
- OlhoCnnTest
- HeliosViewModel
- PerfilInstalacao
- TelaDirecao.kt
- TelaHistorico.kt
- CapturasTelas.kt
- FakeBanco
- TelaAjustes.kt
- amostra
- MainActivity.kt
- TelaInstalacao.kt
- Esp32.kt
- DetectorMediapipe.kt
- Perclos
- MonitorMotoristaTest
- MenuViagem.kt
- GravacaoViagem
- CoresTest
- FonteCameraX.kt
- VozAndroid
- GravacaoViagem.kt
- ClassificadorTest
- MonitorMotorista
- HeliosViewModel.kt
- Tema.kt
- Icone
- LinhaJanela
- TipoEvento
- BancoHelios
- PosicaoCamera
- Calibrador
- MonitorMotorista.kt
- Zona
- LinhaEvento
- AmostraFacial
- ExtratorJpeg
- Leitura
- Motivo
- EsquemaBancoTest
- ConfiguracaoInstalacao
- RecalibracaoViagem
- Desvios
- HELIOS: aplicativo Android (versão 0.4.9)
- AnalisadorMovimento
- Assistente
- RealceOpenCv.kt
- VerificacaoTest
- GravadorArquivos
- AcelerometroAndroid.kt
- Portas.kt
- CaixaRosto
- PoseCabeca
- EstadoMotorista
- ZonasEManobraTest
- Visor.kt
- modeloDeTela
- Próximos passos
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro
- GravadorFotos
- MalhaVisor
- HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)

## God Nodes (most connected - your core abstractions)
1. `MonitorMotorista` - 107 edges
2. `HeliosViewModel` - 95 edges
3. `EstadoMotorista` - 65 edges
4. `Leitura` - 50 edges
5. `Metricas` - 48 edges
6. `amostra()` - 48 edges
7. `Motivo` - 46 edges
8. `PoseCabeca` - 44 edges
9. `GravacaoViagem` - 40 edges
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

## Communities (77 total, 24 thin omitted)

### Community 0 - "TelaDirecaoTest.kt"
Cohesion: 0.06
Nodes (10): Diagnostico, TemaHelios(), TelaDirecaoTest, TelaInstalacaoTest, NotaInstalacao, PartesNota, Qualidade, ACEITAVEL (+2 more)

### Community 1 - "Ponto2D"
Cohesion: 0.06
Nodes (18): Adendo de 16/11/2026: olhos abertos chamados de fechados, ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Fora desta decisão, poseDaMatriz(), posicaoIris(), Ponto2D (+10 more)

### Community 2 - "AgregadorSessao"
Cohesion: 0.10
Nodes (17): AcumuladorJanela, AgregadorSessao, destino(), JanelaResumo, media(), percentil95(), PoliticaEvidencia, ResumoSessao (+9 more)

### Community 3 - "planoDeAlerta"
Cohesion: 0.08
Nodes (15): AlarmeAndroid, SaidaAlarme, comVolumeMinimo(), ControladorFala, gerarCicloPcm(), gerarRetornoPcm(), padraoPara(), PadraoSonoro (+7 more)

### Community 4 - "OlhoCnnTest"
Cohesion: 0.09
Nodes (9): BuffersVisor, DesenhistaMalha, Malha, Pontos, argbDeCinza(), CaixaRecorte, cnnFechado(), RecorteOlho (+1 more)

### Community 6 - "PerfilInstalacao"
Cohesion: 0.06
Nodes (19): RepositorioAjustes, UsoMonitoramento, FakeUso, PerfilInstalacao, Ajuste, com(), Sensibilidade, ALTA (+11 more)

### Community 7 - "TelaDirecao.kt"
Cohesion: 0.08
Nodes (27): QuadroVisor, TelaUi, AcoesMenu, Chip(), Chips(), ConteudoDirecao(), desenhoDoIcone(), estadoInicial() (+19 more)

### Community 8 - "TelaHistorico.kt"
Cohesion: 0.09
Nodes (15): duracaoTexto(), horaTexto(), pctTexto(), data(), Detalhe(), duracao(), FotoCheia(), GraficoPerclos() (+7 more)

### Community 9 - "CapturasTelas.kt"
Cohesion: 0.16
Nodes (27): Ajustes(), assistente(), Direcao(), DirecaoAtento(), DirecaoCritico(), DirecaoDistraido(), DirecaoFadiga(), DirecaoParadoComVisor() (+19 more)

### Community 10 - "FakeBanco"
Cohesion: 0.10
Nodes (6): ContextoLeitura, FakeBanco, FakeGravador, GravacaoViagemTest, FakeBanco, FakeGravador

### Community 11 - "TelaAjustes.kt"
Cohesion: 0.10
Nodes (17): ConfigEsp32, Ajustes, AjustesFotos, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, EstadoFotos, Campo() (+9 more)

### Community 12 - "amostra"
Cohesion: 0.16
Nodes (6): Calibracao, Quadro, amostra(), instantes(), CnnEmModoSombraTest, OlharEPiscadasTest

### Community 13 - "MainActivity.kt"
Cohesion: 0.08
Nodes (9): Aviso(), HeliosApp(), MainActivity, PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO (+1 more)

### Community 14 - "TelaInstalacao.kt"
Cohesion: 0.14
Nodes (18): EstadoInstalacao, dataTexto(), AcoesInstalacao, AcoesPerfis, Cabecalho(), ComImagem(), DialogoRenomear(), Escolha() (+10 more)

### Community 15 - "Esp32.kt"
Cohesion: 0.10
Nodes (4): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback

### Community 16 - "DetectorMediapipe.kt"
Cohesion: 0.12
Nodes (3): DetectorMediapipe, ConsumidorVisual, DetectorFacial

### Community 17 - "Perclos"
Cohesion: 0.13
Nodes (7): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SinaisTest

### Community 19 - "MenuViagem.kt"
Cohesion: 0.11
Nodes (7): ViagemAtual, BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu()

### Community 20 - "GravacaoViagem"
Cohesion: 0.19
Nodes (4): GravacaoViagem, PedidoFoto, SessaoAtiva, O que mudou na versão 0.4.2

### Community 21 - "CoresTest"
Cohesion: 0.17
Nodes (8): canal(), contrasteWcag(), CorDeSinal, corDoPulso(), EsquemaCores, luminancia(), misturar(), CoresTest

### Community 23 - "VozAndroid"
Cohesion: 0.09
Nodes (4): ClassificadorOlhoLiteRt, VozAndroid, ClassificadorOlho, SaidaVoz

### Community 24 - "GravacaoViagem.kt"
Cohesion: 0.09
Nodes (3): FotoSalva, RepositorioViagens, SessaoGravada

### Community 25 - "ClassificadorTest"
Cohesion: 0.19
Nodes (5): Decisão, Classificacao, Classificador, Indicadores, ClassificadorTest

### Community 26 - "MonitorMotorista"
Cohesion: 0.19
Nodes (3): MonitorMotorista, Olhada, Evento

### Community 27 - "HeliosViewModel.kt"
Cohesion: 0.11
Nodes (3): quer(), receber(), OlhosCnn

### Community 29 - "Icone"
Cohesion: 0.09
Nodes (23): Alvo, Espaco, Achados da Fase 1, ADR-012: Interface guiada por diretrizes publicadas, Capturas, Contexto, Decisões da Fase 1 (tokens de design), Decisões da Fase 2 (tela de direção) (+15 more)

### Community 31 - "LinhaJanela"
Cohesion: 0.19
Nodes (3): RegistroQuadros, Csv, LinhaJanela

### Community 32 - "TipoEvento"
Cohesion: 0.14
Nodes (18): DetalheViagem, EventoDetalhe, TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MANOBRA (+10 more)

### Community 34 - "PosicaoCamera"
Cohesion: 0.10
Nodes (14): LadoMotorista, DIREITO, ESQUERDO, PosicaoCamera, CENTRO_PAINEL, COLUNA_VOLANTE, PILAR_A, RETROVISOR (+6 more)

### Community 35 - "Calibrador"
Cohesion: 0.19
Nodes (5): Calibrador, mediana(), medianaOuNulo(), medianaOuZero(), CalibradorTest

### Community 36 - "MonitorMotorista.kt"
Cohesion: 0.17
Nodes (10): ConfiguracaoDeteccao, classificarFechamento(), DetectorSustentado, duracaoEmCurso(), EmCurso, Encerrada, Fase, Inativa (+2 more)

### Community 37 - "Zona"
Cohesion: 0.17
Nodes (10): distancia(), Zona, ESPELHO_DIREITO, ESPELHO_ESQUERDO, PAINEL, RETROVISOR, zonaMaisProxima(), ZonaOlhar (+2 more)

### Community 38 - "LinhaEvento"
Cohesion: 0.18
Nodes (6): FotoGuardada, LinhaEvento, nomeFoto(), PoliticaRetencao, CsvTest, FotosTest

### Community 39 - "AmostraFacial"
Cohesion: 0.15
Nodes (10): distancia(), earMedio(), razaoAspecto(), Olho, OlhoAvaliado, AMBOS, DIREITO, ESQUERDO (+2 more)

### Community 41 - "Leitura"
Cohesion: 0.25
Nodes (5): Leitura, Metricas, Totais, ZonasNaJanelaTest, VisorTest

### Community 42 - "Motivo"
Cohesion: 0.11
Nodes (18): Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA, DISTRACAO_ACUMULADA, EVENTO_BRUSCO_COM_FADIGA, MICROSSONOS_FREQUENTES, OLHAR_DESVIADO (+10 more)

### Community 43 - "EsquemaBancoTest"
Cohesion: 0.18
Nodes (7): EsquemaBancoTest, ADR-014: distração com zonas do carro e manobra, Consequências, Contexto, Decisão, Manobra: impacto para o motorista e para o TCC, Verificação

### Community 44 - "ConfiguracaoInstalacao"
Cohesion: 0.18
Nodes (17): angulo(), ConfiguracaoInstalacao, dentro(), Dica, ABAIXAR, AFASTAR, APROXIMAR, CENTRALIZAR (+9 more)

### Community 45 - "RecalibracaoViagem"
Cohesion: 0.26
Nodes (3): Pose, RecalibracaoViagem, RecalibracaoViagemTest

### Community 46 - "Desvios"
Cohesion: 0.20
Nodes (4): desvioPadrao(), Desvios, ColetorZona, NotaEZonaTest

### Community 47 - "HELIOS: aplicativo Android (versão 0.4.9)"
Cohesion: 0.11
Nodes (17): Como abrir, HELIOS: aplicativo Android (versão 0.4.9), Limitações conhecidas, Números desta versão, O que mudou na versão 0.2.0, O que mudou na versão 0.3.0 (depois do primeiro teste no celular), O que mudou na versão 0.4.0 (depois do segundo teste no celular), O que mudou na versão 0.4.1 (fotos que não apareciam) (+9 more)

### Community 48 - "AnalisadorMovimento"
Cohesion: 0.23
Nodes (3): Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 50 - "RealceOpenCv.kt"
Cohesion: 0.15
Nodes (3): RealceOpenCv, PreProcessador, O que mudou na versão 0.4.4

### Community 54 - "Portas.kt"
Cohesion: 0.17
Nodes (6): Conexao, CONECTADA, CONECTANDO, DESCONECTADA, FonteDeImagens, FonteInercial

### Community 55 - "CaixaRosto"
Cohesion: 0.21
Nodes (8): ADR-013: instalação da câmera e calibração obrigatória, Consequências, Contexto, Decisão, Verificação, CaixaRosto, brilhoDoRosto(), EnquadramentoTest

### Community 57 - "EstadoMotorista"
Cohesion: 0.19
Nodes (12): EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO, FADIGA, SEM_ROSTO (+4 more)

### Community 59 - "Visor.kt"
Cohesion: 0.31
Nodes (12): abertura(), cnn(), Eixos2D, eixosDaCabeca(), graus(), linhasDeMetricas(), num(), olhar() (+4 more)

### Community 62 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 63 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 65 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 66 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 67 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 68 - "ADR-010: frases de alerta geradas fora do carro"
Cohesion: 0.33
Nodes (5): ADR-010: frases de alerta geradas fora do carro, Consequências, Contexto, Decisão, Fora desta decisão

## Knowledge Gaps
- **102 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+97 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 393 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **24 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `EstadoMotorista` connect `EstadoMotorista` to `TelaDirecaoTest.kt`, `AgregadorSessao`, `planoDeAlerta`, `HeliosViewModel`, `CapturasTelas.kt`, `FakeBanco`, `amostra`, `GravacaoViagem`, `GravacaoViagem.kt`, `ClassificadorTest`, `MonitorMotorista`, `HeliosViewModel.kt`, `GravacaoViagemTest.kt`, `LinhaJanela`, `TipoEvento`, `BancoHelios`, `MonitorMotorista.kt`, `Zona`, `LinhaEvento`, `AmostraFacial`, `Leitura`, `Motivo`, `modeloDeTela`?**
  _High betweenness centrality (0.104) - this node is a cross-community bridge._
- **Are the 54 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 54 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _102 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `TelaDirecaoTest.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.060828680575962385 - nodes in this community are weakly interconnected._
- **Why does `HeliosViewModel` connect `HeliosViewModel` to `planoDeAlerta`, `OlhoCnnTest`, `PerfilInstalacao`, `TelaDirecao.kt`, `TelaAjustes.kt`, `MainActivity.kt`, `TelaInstalacao.kt`, `Esp32.kt`, `DetectorMediapipe.kt`, `Perclos`, `MenuViagem.kt`, `GravacaoViagem`, `VozAndroid`, `GravacaoViagem.kt`, `HeliosViewModel.kt`, `TipoEvento`, `BancoHelios`, `PosicaoCamera`, `MonitorMotorista.kt`, `Leitura`, `RecalibracaoViagem`, `Assistente`, `GravadorArquivos`, `Portas.kt`, `EstadoMotorista`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `Ponto2D` be split into smaller, more focused modules?**
  _Cohesion score 0.06025039123630673 - nodes in this community are weakly interconnected._