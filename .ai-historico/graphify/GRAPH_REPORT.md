# Graph Report - helios-android-v2  (2026-11-16)

## Corpus Check
- 99 files · ~643,017 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .xml 18, .properties 3, (none) 2)

## Summary
- 1821 nodes · 5163 edges · 75 communities (51 shown, 24 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 364 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `70eef8c0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- CapturasTelas.kt
- RecalibracaoViagem
- TelaDirecaoTest
- OlhoCnnTest
- planoDeAlerta
- Ponto2D
- HeliosViewModel
- TelaInstalacao.kt
- TelaHistorico.kt
- FakeBanco
- Motivo
- MonitorMotorista
- TelaDirecao.kt
- BancoHelios
- AgregadorSessao
- HELIOS: aplicativo Android (versão 0.4.10)
- AnalisadorMovimento
- Tema.kt
- FonteCameraX.kt
- Esp32.kt
- TelaAjustes.kt
- Perclos
- MenuViagem.kt
- amostra
- MainActivity.kt
- PerfilInstalacao
- LinhaJanela
- GravacaoViagemTest.kt
- GravadorArquivos
- DetectorMediapipe.kt
- GravacaoViagem
- CoresTest
- Registros.kt
- MonitorMotorista.kt
- ZonaOlhar
- ExtratorJpeg
- DesenhistaMalha
- Evento
- EsquemaBancoTest
- ConfiguracaoInstalacao
- Leitura
- Assistente
- Sessao.kt
- EstadoMotorista
- MapeadorFaceMesh
- HeliosViewModel.kt
- GravacaoViagem.kt
- VerificacaoTest
- PoseCabeca
- VozAndroid
- Visor.kt
- Portas.kt
- ColetorZona
- modeloDeTela
- TipoEvento
- ClassificadorOlhoLiteRt.kt
- CaixaRosto
- PosicaoCamera
- DadosTest
- OlhosCnn
- Próximos passos
- DesenhistaMalha.kt
- ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor
- PoolBitmaps
- ADR-007: integrações da versão 0.2.0
- ADR-008: escada do olho fechado, olhos independentes e volta do motorista
- ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados
- ADR-010: frases de alerta geradas fora do carro
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
- `O que mudou na versão 0.4.6` --references--> `TelaDirecaoTest`  [INFERRED]
  README.md → app/src/test/kotlin/br/unicid/helios/app/ui/TelaDirecaoTest.kt
- `Decisão` --references--> `ConfiguracaoDeteccao`  [INFERRED]
  docs/ADR-014-distracao-por-zonas.md → dominio/src/main/kotlin/br/unicid/helios/dominio/ConfiguracaoDeteccao.kt
- `O que mudou na versão 0.4.5` --references--> `CoresTest`  [INFERRED]
  README.md → protocolos/src/test/kotlin/br/unicid/helios/protocolos/CoresTest.kt
- `O que mudou na versão 0.4.2` --references--> `UsoMonitoramento`  [INFERRED]
  README.md → app/src/main/kotlin/br/unicid/helios/app/Ajustes.kt

## Import Cycles
- None detected.

## Communities (75 total, 24 thin omitted)

### Community 0 - "CapturasTelas.kt"
Cohesion: 0.05
Nodes (47): EstadoInstalacao, QuadroVisor, Ajustes(), assistente(), Direcao(), DirecaoAtento(), DirecaoCritico(), DirecaoDistraido() (+39 more)

### Community 1 - "RecalibracaoViagem"
Cohesion: 0.06
Nodes (18): ADR-013: instalação da câmera e calibração obrigatória, Consequências, Contexto, Decisão, Revisão na 0.4.10: movimento vira aviso, Verificação, Calibrador, desvioPadrao() (+10 more)

### Community 2 - "TelaDirecaoTest"
Cohesion: 0.07
Nodes (32): Diagnostico, TelaUi, MainActivity, AcoesMenu, estadoInicial(), TelaDirecao(), Alvo, Espaco (+24 more)

### Community 3 - "OlhoCnnTest"
Cohesion: 0.06
Nodes (12): Carro, Adendo de 16/11/2026: olhos abertos chamados de fechados, ADR-011: CNN do olho em modo sombra, Consequências, Contexto, Fora desta decisão, argbDeCinza(), CaixaRecorte (+4 more)

### Community 4 - "planoDeAlerta"
Cohesion: 0.08
Nodes (15): AlarmeAndroid, SaidaAlarme, comVolumeMinimo(), ControladorFala, gerarCicloPcm(), gerarRetornoPcm(), padraoPara(), PadraoSonoro (+7 more)

### Community 5 - "Ponto2D"
Cohesion: 0.09
Nodes (15): distancia(), earMedio(), poseDaMatriz(), posicaoIris(), razaoAspecto(), OlhoAvaliado, AMBOS, DIREITO (+7 more)

### Community 7 - "TelaInstalacao.kt"
Cohesion: 0.10
Nodes (26): AcaoCalibracao(), AcaoZonas(), AcoesInstalacao, AcoesPerfis, AnelDaNota(), AvisoMovimento(), Cartao(), ComImagem() (+18 more)

### Community 8 - "TelaHistorico.kt"
Cohesion: 0.08
Nodes (16): dataTexto(), duracaoTexto(), horaTexto(), pctTexto(), data(), Detalhe(), duracao(), FotoCheia() (+8 more)

### Community 9 - "FakeBanco"
Cohesion: 0.09
Nodes (7): ContextoLeitura, FakeBanco, FakeGravador, FakeUso, GravacaoViagemTest, FakeBanco, FakeGravador

### Community 10 - "Motivo"
Cohesion: 0.09
Nodes (22): Decisão, Classificacao, Classificador, Indicadores, Motivo, BOCEJOS_FREQUENTES, CABECA_CAIDA, CABECA_VIRADA (+14 more)

### Community 11 - "MonitorMotorista"
Cohesion: 0.17
Nodes (4): MonitorMotorista, Olho, Quadro, MonitorMotoristaTest

### Community 12 - "TelaDirecao.kt"
Cohesion: 0.08
Nodes (12): Chip(), Chips(), ConteudoDirecao(), desenhoDoIcone(), FaixaInferior(), Indicador(), Miniatura(), pulsoCritico() (+4 more)

### Community 13 - "BancoHelios"
Cohesion: 0.08
Nodes (5): BancoHelios, EsquemaBanco, FotoSalva, RepositorioViagens, SessaoGravada

### Community 14 - "AgregadorSessao"
Cohesion: 0.16
Nodes (11): AgregadorSessao, percentil95(), AgregadorSessaoBordasTest, AgregadorSessaoTest, CnnNaJanelaTest, DesempenhoNaJanelaTest, EstadoNoInicioDoEventoTest, leitura() (+3 more)

### Community 15 - "HELIOS: aplicativo Android (versão 0.4.10)"
Cohesion: 0.06
Nodes (21): RealceOpenCv, PreProcessador, Como abrir, HELIOS: aplicativo Android (versão 0.4.10), Limitações conhecidas, Números desta versão, O que mudou na versão 0.2.0, O que mudou na versão 0.3.0 (depois do primeiro teste no celular) (+13 more)

### Community 16 - "AnalisadorMovimento"
Cohesion: 0.09
Nodes (6): AcelerometroAndroid, SensorEventListener, FonteInercial, Amostra, AnalisadorMovimento, AnalisadorMovimentoTest

### Community 17 - "Tema.kt"
Cohesion: 0.07
Nodes (11): cor(), esquema(), fundoDoSinal(), Tipos, Tom, AMARELO, AMBAR, LARANJA (+3 more)

### Community 18 - "FonteCameraX.kt"
Cohesion: 0.10
Nodes (7): FonteCameraX, Conexao, CONECTADA, CONECTANDO, DESCONECTADA, FonteDeImagens, Imagem

### Community 19 - "Esp32.kt"
Cohesion: 0.10
Nodes (4): BuzzerEsp32, FonteEsp32, RedeHeliosCam, NetworkCallback

### Community 20 - "TelaAjustes.kt"
Cohesion: 0.12
Nodes (11): EstadoFotos, Campo(), Interruptor(), Opcao(), rememberSeletorDePasta(), Secao(), SecaoAlertas(), SecaoFotos() (+3 more)

### Community 21 - "Perclos"
Cohesion: 0.12
Nodes (8): ContadorEmJanela, EstatisticaPiscadas, Perclos, Piscada, ResumoPiscadas, Segmento, SuavizadorEma, SinaisTest

### Community 22 - "MenuViagem.kt"
Cohesion: 0.09
Nodes (7): ViagemAtual, BotaoGrande(), BotaoMenu(), BotaoSegurar(), Numero(), NumerosDaViagem(), PainelMenu()

### Community 23 - "amostra"
Cohesion: 0.19
Nodes (5): Calibracao, amostra(), instantes(), CnnEmModoSombraTest, OlharEPiscadasTest

### Community 24 - "MainActivity.kt"
Cohesion: 0.09
Nodes (8): Aviso(), HeliosApp(), PedirCamera(), PerguntaFotos(), Tela, AJUSTES, DIRECAO, HISTORICO

### Community 25 - "PerfilInstalacao"
Cohesion: 0.10
Nodes (20): ConfigEsp32, Ajustes, AjustesFotos, RepositorioAjustes, TipoFonte, CAMERA_DO_APARELHO, ESP32_CAM, UsoMonitoramento (+12 more)

### Community 26 - "LinhaJanela"
Cohesion: 0.16
Nodes (4): Csv, LinhaEvento, LinhaJanela, CsvTest

### Community 27 - "GravacaoViagemTest.kt"
Cohesion: 0.17
Nodes (6): Zona, ESPELHO_DIREITO, ESPELHO_ESQUERDO, PAINEL, RETROVISOR, MetricasOlhar

### Community 30 - "GravacaoViagem"
Cohesion: 0.19
Nodes (4): GravacaoViagem, PedidoFoto, SessaoAtiva, O que mudou na versão 0.4.2

### Community 31 - "CoresTest"
Cohesion: 0.17
Nodes (8): canal(), contrasteWcag(), CorDeSinal, corDoPulso(), EsquemaCores, luminancia(), misturar(), CoresTest

### Community 32 - "Registros.kt"
Cohesion: 0.20
Nodes (10): DetalheViagem, EventoDetalhe, fotoDoEvento(), FotoGuardada, FotoRegistrada, fotosDoEvento(), miniatura(), nomeFoto() (+2 more)

### Community 33 - "MonitorMotorista.kt"
Cohesion: 0.19
Nodes (10): ConfiguracaoDeteccao, classificarFechamento(), DetectorSustentado, duracaoEmCurso(), EmCurso, Encerrada, Fase, Inativa (+2 more)

### Community 34 - "ZonaOlhar"
Cohesion: 0.26
Nodes (4): distancia(), zonaMaisProxima(), ZonaOlhar, ZonasEManobraTest

### Community 36 - "DesenhistaMalha"
Cohesion: 0.27
Nodes (3): DesenhistaMalha, Malha, Pontos

### Community 38 - "EsquemaBancoTest"
Cohesion: 0.18
Nodes (7): EsquemaBancoTest, ADR-014: distração com zonas do carro e manobra, Consequências, Contexto, Decisão, Manobra: impacto para o motorista e para o TCC, Verificação

### Community 39 - "ConfiguracaoInstalacao"
Cohesion: 0.18
Nodes (17): angulo(), ConfiguracaoInstalacao, dentro(), Dica, ABAIXAR, AFASTAR, APROXIMAR, CENTRALIZAR (+9 more)

### Community 40 - "Leitura"
Cohesion: 0.26
Nodes (5): Leitura, Metricas, Totais, ZonasNaJanelaTest, VisorTest

### Community 42 - "Sessao.kt"
Cohesion: 0.23
Nodes (5): AcumuladorJanela, destino(), JanelaResumo, media(), ResumoSessao

### Community 43 - "EstadoMotorista"
Cohesion: 0.15
Nodes (13): PoliticaEvidencia, EstadoMotorista, ATENCAO, ATENTO, CRITICO, DESATENTO, DISTRAIDO, FADIGA (+5 more)

### Community 44 - "MapeadorFaceMesh"
Cohesion: 0.24
Nodes (3): MapeadorFaceMesh, ResultadoFaceMesh, MapeadorFaceMeshTest

### Community 50 - "Visor.kt"
Cohesion: 0.31
Nodes (12): abertura(), cnn(), Eixos2D, eixosDaCabeca(), graus(), linhasDeMetricas(), num(), olhar() (+4 more)

### Community 51 - "Portas.kt"
Cohesion: 0.19
Nodes (3): ClassificadorOlho, ConsumidorVisual, DetectorFacial

### Community 54 - "TipoEvento"
Cohesion: 0.17
Nodes (12): TipoEvento, BOCEJO, CABECA_CAIDA, CABECA_VIRADA, EVENTO_BRUSCO, MANOBRA, MICROSSONO, OLHAR_DESVIADO (+4 more)

### Community 56 - "CaixaRosto"
Cohesion: 0.40
Nodes (3): CaixaRosto, brilhoDoRosto(), EnquadramentoTest

### Community 57 - "PosicaoCamera"
Cohesion: 0.22
Nodes (8): LadoMotorista, DIREITO, ESQUERDO, PosicaoCamera, CENTRO_PAINEL, COLUNA_VOLANTE, PILAR_A, RETROVISOR

### Community 60 - "Próximos passos"
Cohesion: 0.25
Nodes (7): Prompt 1. Primeira build e correções, Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04), Prompt 3. Teste dourado contra o Python, Prompt 4. Firmware da ESP32-CAM (Fase 4), Prompt 5. Histórico local e CSV (Fase 5), Prompt 6. Halstead e duplicação, Próximos passos

### Community 61 - "DesenhistaMalha.kt"
Cohesion: 0.33
Nodes (3): BuffersVisor, IndicesFaceMesh, cnnFechado()

### Community 62 - "ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor"
Cohesion: 0.29
Nodes (6): ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor, Consequências nas metas de qualidade, Contexto, Decisão, Motivos, O que se perde

### Community 64 - "ADR-007: integrações da versão 0.2.0"
Cohesion: 0.33
Nodes (5): ADR-007: integrações da versão 0.2.0, Consequências, Contexto, Decisões, Fora desta versão

### Community 65 - "ADR-008: escada do olho fechado, olhos independentes e volta do motorista"
Cohesion: 0.33
Nodes (5): ADR-008: escada do olho fechado, olhos independentes e volta do motorista, Consequências, Contexto, Decisão, Fonte

### Community 66 - "ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados"
Cohesion: 0.33
Nodes (5): ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados, Consequências, Contexto, Decisão, Fontes

### Community 67 - "ADR-010: frases de alerta geradas fora do carro"
Cohesion: 0.33
Nodes (5): ADR-010: frases de alerta geradas fora do carro, Consequências, Contexto, Decisão, Fora desta decisão

## Knowledge Gaps
- **102 isolated node(s):** `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO`, `AJUSTES`, `HISTORICO` (+97 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 405 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **24 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `EstadoMotorista` connect `EstadoMotorista` to `CapturasTelas.kt`, `TelaDirecaoTest`, `planoDeAlerta`, `Ponto2D`, `HeliosViewModel`, `FakeBanco`, `Motivo`, `MonitorMotorista`, `BancoHelios`, `AgregadorSessao`, `amostra`, `LinhaJanela`, `GravacaoViagemTest.kt`, `GravacaoViagem`, `Registros.kt`, `MonitorMotorista.kt`, `ZonaOlhar`, `Evento`, `Leitura`, `Sessao.kt`, `HeliosViewModel.kt`, `GravacaoViagem.kt`, `modeloDeTela`?**
  _High betweenness centrality (0.107) - this node is a cross-community bridge._
- **Are the 54 inferred relationships involving `MonitorMotorista` (e.g. with `HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)` and `.`com ou sem o valor da CNN os estados e os motivos sao os mesmos`()`) actually correct?**
  _`MonitorMotorista` has 54 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CAMERA_DO_APARELHO`, `ESP32_CAM`, `DIRECAO` to the rest of the system?**
  _102 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `CapturasTelas.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.05293040293040293 - nodes in this community are weakly interconnected._
- **Why does `HeliosViewModel` connect `HeliosViewModel` to `CapturasTelas.kt`, `RecalibracaoViagem`, `TelaDirecaoTest`, `planoDeAlerta`, `BancoHelios`, `FonteCameraX.kt`, `Esp32.kt`, `TelaAjustes.kt`, `Perclos`, `MenuViagem.kt`, `MainActivity.kt`, `PerfilInstalacao`, `GravadorArquivos`, `DetectorMediapipe.kt`, `GravacaoViagem`, `Registros.kt`, `MonitorMotorista.kt`, `DesenhistaMalha`, `Leitura`, `Assistente`, `EstadoMotorista`, `HeliosViewModel.kt`, `VozAndroid`, `Portas.kt`, `ClassificadorOlhoLiteRt.kt`, `PosicaoCamera`, `OlhosCnn`, `DesenhistaMalha.kt`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HeliosViewModel` (e.g. with `RepositorioAjustes` and `GravacaoViagem`) actually correct?**
  _`HeliosViewModel` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `RecalibracaoViagem` be split into smaller, more focused modules?**
  _Cohesion score 0.06128364389233954 - nodes in this community are weakly interconnected._