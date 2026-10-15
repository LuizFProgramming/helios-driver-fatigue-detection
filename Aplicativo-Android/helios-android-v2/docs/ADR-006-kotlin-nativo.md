# ADR-006: app em Kotlin nativo no lugar de TypeScript + Capacitor

Data: 06/10/2026. Situação: aceita. Substitui a pilha da seção 3 do documento 08.

## Contexto

O documento 08 escolheu TypeScript, Capacitor e `@mediapipe/tasks-vision` (Web) porque as metas de qualidade tinham ferramentas maduras nesse ecossistema. A dúvida que ficou aberta no próprio documento era o desempenho em central multimídia fraca, além do risco do Android mandar o tráfego da câmera pelos dados móveis (spike 0.2).

## Decisão

Escrever o app em Kotlin, com Jetpack Compose, CameraX e o MediaPipe Tasks nativo para Android. O domínio fica em módulos JVM puros (`dominio` e `protocolos`), sem dependência do Android.

## Motivos

1. **Desempenho.** O MediaPipe nativo usa o delegate de GPU direto. A versão Web roda dentro de um WebView, passa cada frame por canvas e executa o modelo em WASM ou WebGL. Em central com processador fraco essa diferença decide se o RNF04 (10 fps) é cumprido.
2. **Rede da câmera.** Com `WifiNetworkSpecifier` o app pede a rede `HELIOS-CAM` só para si e abre o stream com `network.openConnection`. O resto do aparelho continua usando os dados móveis. No Capacitor isso exigiria um plugin nativo de qualquer jeito.
3. **Câmera.** O CameraX entrega o frame já rotacionado em RGBA, descarta frames velhos sozinho e funciona com o ciclo de vida do Android.
4. **Mesma arquitetura.** O desenho hexagonal e o padrão Humble Object continuam iguais; só muda a linguagem.

## Consequências nas metas de qualidade

| Meta | Antes (TS) | Agora (Kotlin) |
|------|-----------|----------------|
| CC e cognitiva < 22 | ESLint + sonarjs | detekt 2.0 |
| Cobertura 100% | Vitest v8 | Kover |
| 0 mutantes | StrykerJS | Pitest. O Kotlin gera bytecode extra (checagens de nulo, métodos de data class) que pode virar mutante impossível de matar; esses casos são excluídos com justificativa |
| 0 `any`/`unknown` | regra do ESLint | `Any`, `!!` e casts não verificados barrados pela tarefa `verificarTiposProibidos` |
| Halstead < 80 | script próprio em TS | ainda a fazer (script sobre a PSI do Kotlin ou o detekt com regra própria) |
| 0 duplicação | jscpd | jscpd também lê Kotlin |

## O que se perde

O teste dourado contra o Python continua possível (CSV de EAR/MAR lido nos testes JVM). Perde-se a opção de rodar o app no navegador do notebook para demonstração; para isso o protótipo Python segue disponível.
