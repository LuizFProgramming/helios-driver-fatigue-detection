# Diário de sessões

## 2026-10-18
- Regra fixada: Ponytail, Graphify e agent-skills (VERIFY) obrigatórios neste projeto.
- Grafo gerado do `helios-android-v2` (código, local, sem LLM).
- Auditoria Ponytail e refatorações R1 a R3 no `HeliosViewModel.kt`.
- Pendente: compilar e rodar os testes no Android Studio; decidir sobre a extração de `GravacaoViagem`.

## 2026-10-18 (segunda parte)
- `GravacaoViagem.kt` criado: sessão, eventos, janelas, fotos e retenção saíram do `HeliosViewModel` (830 → 653 linhas).
- `_to_delete/GravadorSaf.kt.antigo` apagado.
- Nova personalização: tamanho do texto da tela de direção (normal, grande, muito grande) em Ajustes › Tela de direção.
- agent-skills (3 agentes, 9 skills) e Ponytail (4 skills) copiados para `helios-android-v2/.claude`; `CLAUDE.md` passou a exigir as ferramentas.
- Grafo refeito: 1083 nós, 2810 ligações.
- Pendente: compilar no Android Studio (prompt pronto em `prompts-claude-code.md`).

## 2026-10-18 (terceira parte)
- Compilação validada via Claude Code: `:dominio:test`, `:protocolos:test` e `:app:compileDebugKotlin` — BUILD SUCCESSFUL (46 s).
- Detekt: 55 erros, todos pré-existentes; nenhum introduzido pelas mudanças de hoje.
  - Fontes: `MaxLineLength` espalhado em `adaptadores/` e `ui/`; `LongMethod`/`CognitiveComplexMethod` em `TelaAjustes` e `TelaDirecao` (contagens já ultrapassavam o limite antes das adições de hoje); `MatchingDeclarationName` em `MenuViagem.kt`; `TooGenericExceptionCaught` em `DetectorMediapipe.kt`.
  - Próximo passo separado: resolver detekt (fora do escopo desta sessão).
- Gradlew ausente no repositório; usando o binário em cache (`~/.gradle/wrapper/dists/gradle-9.3.1-bin/…/bin/gradle`). Criar `gradlew` / `gradlew.bat` antes do próximo CI.

## 2026-10-18 (quarta parte)
- Testes JVM de `GravacaoViagem` criados (4 casos, todos verdes, sem Robolectric):
  - `abre sessão na primeira leitura` · `não registra com viagem encerrada`
  - `finalizar devolve o id e grava a janela parcial` · `retenção apaga arquivo antes do registro`
- Mudanças de produção mínimas para habilitar os testes:
  - `Ajustes.kt`: interface `UsoMonitoramento` (2 métodos); `RepositorioAjustes` implementa-a.
  - `GravacaoViagem.kt`: aceita `UsoMonitoramento` em vez de `RepositorioAjustes`; parâmetro `epochOffsetMs: () -> Long` (default = `SystemClock`-based, injetável em testes).
  - `libs.versions.toml` + `app/build.gradle.kts`: `kotlinx-coroutines-test` adicionado.
- Test 4 (retenção) usa `Retencao.AO_ENCERRAR` + banco pré-semeado: exercita `excluirFotos` via `finalizar()` sem Bitmap.

## 2026-11-13
- Firmware da ESP32-CAM unificado: o `.ino` e o `src/main.cpp` tinham virado dois programas. Agora há uma fonte só em `firmware/esp32-cam/arduino/helios_cam/` (também `src_dir` do PlatformIO), com OLED, buzzer ativo/passivo no GPIO 13, `X-Timestamp` no MJPEG e modo demonstração opcional.
- Removidos do código o nome e a senha da rede de casa que entraram no commit `e291eb7`; o modo estação lê `secrets.h` (no `.gitignore`). A senha continua no histórico do git: trocar a senha do roteador.
- Compilação verificada com arduino-cli e core esp32 2.0.17 nas variantes de buzzer, demo e modo estação, sem avisos. Não testado na placa.
- Experimento olho aberto/fechado em `scripts/experimento_olhos/` (EAR, ECF, regra do app, CNN MobileNetV2), 18 testes; pipeline rodado de ponta a ponta com dados sintéticos. Falta rodar com a base real (CEW) e conferir a licença.
- Ferramentas de E:\ai-tools (Graphify, Ponytail) não disponíveis nesta sessão (computador não conectado); grafo não regenerado.

## 2026-11-15
- App 0.4.3 (versionCode 7): o CSV passou a trazer o que o capítulo de resultados precisa.
  - `janelas.csv`: `tempo_atento_ms` ... `tempo_sem_rosto_ms` (mesma regra de lacuna do `AgregadorSessao.acumularTempo`, intervalo repartido na fronteira entre janelas), `fps_medio`, `latencia_media_ms`, `latencia_p95_ms` (posto ceil(0,95 n)).
  - `eventos.csv`: `estado_inicio` (novo, vem de `AgregadorSessao.estadoEm`) e `estado_fim` (a antiga `estado`, mesmo conteúdo).
  - Latência medida no `HeliosViewModel.processar` com `elapsedRealtime` lido numa variável local (o vigia de sinal zera `ultimaLeituraMs` em outra corrotina). Com a ESP32-CAM o relógio começa na chegada do JPEG.
  - Banco versão 3: SQL foi para `EsquemaBanco` (mesmo arquivo do `BancoHelios`); ALTER TABLE com DEFAULT. Teste roda o SQL num SQLite de verdade com `sqlite-jdbc` (só `testImplementation`).
- Achado para o TCC: no monitor real, um microssono de 2,6 s passa por ATENCAO mas começa e termina em ATENTO (o alarme cai na leitura em que os olhos abrem). `estado_fim` não mede a gravidade do evento; usar o tempo por estado das janelas.
- Testes: 179 (dominio 120, protocolos 52, app 7), todos verdes. Antes: 162.
- `qualidade` continua vermelha por pendências anteriores a esta sessão, nenhuma nova: detekt (dominio 25, protocolos 25, app 56; antes 25/29/57), `:protocolos:koverVerify` com ramos em 98,75% (falta 2 ramos em `Alarme.kt:113`; antes 98,70%), Pitest abaixo de 100% (dominio 623/694, protocolos 249/287). `:dominio:koverVerify` passa (100% linha e ramo). Nenhum mutante novo sobrevivente nos arquivos tocados.
- Grafo refeito: 1175 nós, 3046 ligações. Criado `.graphifyignore` para deixar `.claude/` fora (as skills somavam 1090 nós).
