# Auditoria Ponytail: helios-android-v2 (18/10/2026)

**O que o repositório faz:** app Android em Kotlin que lê a câmera do aparelho ou da ESP32-CAM, extrai a malha facial com MediaPipe e decide ATENTO / FADIGA / CRÍTICO no módulo `dominio`, tocando alarme, voz e buzzer. Grava sessões, eventos, janelas e fotos no próprio aparelho. Carga assumida: um motorista, um aparelho, cerca de 10 a 30 quadros por segundo.

Base lida pelo grafo (Graphify): `MonitorMotorista` (95 ligações) e `HeliosViewModel` (79) são os dois centros. Não há ciclo de import entre módulos. O `app` depende de `dominio` e `protocolos`, e nunca o contrário, como o ADR-006 pede.

## Feito nesta sessão (só `HeliosViewModel.kt`, comportamento igual)

| # | Mudança | Motivo |
|---|---------|--------|
| R1 | `retomar()` e `novaViagem()` chamam `recomecarMonitorECaptura()` | O mesmo bloco de reinício estava copiado nos dois; mudar um e esquecer o outro faria a viagem retomada e a nova se comportarem diferente |
| R2 | `alterarAjustes { }` no lugar de `_ajustes.update` + `repositorio.salvar` | O par aparecia 4 vezes; esquecer o `salvar` perde a calibração ou o consentimento das fotos ao fechar o app |
| R3 | `fotosGuardadas()` | A mesma conversão para a política de retenção aparecia em 2 lugares |

Fim de linha CRLF preservado. Diff: +20 / −13 linhas.

## Deve corrigir

Nenhum bug confirmado com caso concreto nesta leitura.

## Deveria corrigir (não feito, esperando aprovação)

1. **`HeliosViewModel` faz cinco trabalhos** (captura, alarme, sessão, fotos, histórico) em 830 linhas. Sugestão de menor custo: tirar só o bloco "Sessão (histórico, fotos)" para uma classe `GravacaoViagem` no próprio pacote `app`, sem nova porta nem interface. Ganha teste de unidade para a retenção de fotos, que hoje só roda no aparelho.
2. **`gravarFoto` lê `banco.fotos()` inteiro a cada foto** para aplicar a retenção. Com o limite de fotos dos ajustes isso é pequeno; vira problema só se o limite passar de alguns milhares.
3. **`_to_delete/GravadorSaf.kt.antigo`**: código morto fora do build. Apagar quando a exclusão de arquivos for liberada.

## Não verificado

- A compilação. A máquina desta sessão não tem acesso ao Maven nem ao serviço de distribuição do Gradle, e o projeto pede Gradle 9.3.1 com JDK 25 (`gradle-daemon-jvm.properties`). Rodar no Android Studio: `./gradlew :dominio:test :protocolos:test :app:compileDebugKotlin`.
- Telas Compose e adaptadores (`FonteCameraX`, `Esp32`, `DetectorMediapipe`) foram vistos só pelo grafo, não linha a linha.

## Segunda parte (aprovada pelo Alexus)

- **Feito:** extração de `GravacaoViagem`. Mesma ordem de gravação em disco, mesma trava da sessão, mesma verificação de "viagem encerrada" dentro da trava. O ViewModel ficou só com captura, alarme, tela e histórico.
- **Feito:** escala do texto da tela de direção. A palavra do estado continua ocupando a largura (o tamanho é dividido pela escala), o resto do texto cresce.
- **Revisão (agente code-reviewer):** sem ciclo novo no grafo; `GravacaoViagem` depende das portas `RepositorioViagens` e `GravadorFotos`, não dos adaptadores. Ponto de atenção: `finalizar()` é chamado da thread principal no `onCleared`, como antes.
- **Segurança (agente security-auditor), só relatado, nada mudado:** a senha do Wi-Fi da ESP32 fica em SharedPreferences privado, em texto puro. Para um app offline isso é aceitável; se quiser, dá para trocar por EncryptedSharedPreferences.
- **Testes (agente test-engineer):** o maior buraco é a retenção de fotos e o fechamento de sessão, que só rodam no aparelho. Se `GravacaoViagem` receber o relógio por parâmetro, dá para testar na JVM com Robolectric.
