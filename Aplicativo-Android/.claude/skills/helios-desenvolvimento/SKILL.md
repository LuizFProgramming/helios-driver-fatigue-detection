---
name: helios-desenvolvimento
description: Regras do app HELIOS (detecção de sonolência, Kotlin/Android). Use ao criar ou alterar qualquer código em dominio/, protocolos/ ou app/, ao escrever testes, ou ao rodar a esteira de qualidade.
---

# Desenvolvimento no HELIOS

## Onde cada coisa vai

| Módulo | O que entra | O que não entra |
|--------|-------------|-----------------|
| `dominio/` | Regras puras: geometria (EAR, MAR, pose), sinais (EMA, PERCLOS), eventos, classificação, monitor | Android, MediaPipe, OpenCV, relógio do sistema, I/O |
| `protocolos/` | A parte que decide dentro dos adaptadores: extrator MJPEG, mapeamento da malha facial, padrão do alarme, modelo de tela | Android |
| `app/` | Adaptadores finos (CameraX, ESP32, MediaPipe, OpenCV, alarme, sensores) e telas Compose | `if`/laço/conta que decida algo do negócio |

Padrão Humble Object (Meszaros, 2007): se um adaptador do `app/` precisar decidir algo, a decisão vira função pura em `protocolos/` com teste, e o adaptador só repassa dados.

## Regras que não mudam

1. **Tempo, nunca frames.** Toda regra temporal recebe `timestampMs` e compara durações. Teste em 10, 15, 30 fps e fps irregular com o mesmo resultado.
2. **Limiares só em `ConfiguracaoDeteccao`**, injetada no construtor. Nenhuma constante de detecção espalhada.
3. **Detecção usa valores brutos.** A EMA é só para exibir números.
4. **Tudo offline.** Nenhuma chamada de rede além da ESP32-CAM em 192.168.4.1. Nenhuma imagem gravada.
5. **Sem `Any`, `!!` ou cast não verificado** em `src/main` (tarefa `verificarTiposProibidos`).
6. **Uma thread para o domínio.** `MonitorMotorista` não é thread-safe; o ViewModel chama sempre pelo dispatcher `dominio`.
7. Não adicionar validações ou comportamentos defensivos que ninguém pediu sem avisar antes.

## Metas de qualidade (docs 06 e 08 do TCC)

| Meta | Ferramenta | Onde configurar |
|------|-----------|-----------------|
| CC < 22, cognitiva < 22 | detekt (`CyclomaticComplexMethod`, `CognitiveComplexMethod`, `allowedComplexity: 21`) | `config/detekt.yml` |
| Cobertura 100% (linha e ramo) | Kover `koverVerify` | `dominio/` e `protocolos/` `build.gradle.kts` |
| CRAP < 25 | consequência de 100% de cobertura + CC < 22 | — |
| 0 mutantes | Pitest `mutationThreshold = 100` | idem |
| 0 `Any`/`!!` | `verificarTiposProibidos` | `build.gradle.kts` raiz |
| Halstead < 80, 0 duplicação | ainda não automatizado (ver docs/PROXIMOS_PASSOS.md) | — |

Rodar tudo: `./gradlew qualidade`. Só os testes rápidos: `./gradlew :dominio:test :protocolos:test`.

## Testes

- JUnit 5 + `kotlin.test` (`assertEquals`, `assertNotNull` com smart cast). Nomes de teste em frase, com crases.
- Fakes em vez de mocks (recomendação da skill `camerax`).
- Monte cenas com `amostra(...)` e `instantes(...)` de `dominio/src/test/.../Fixtures.kt`.
- Mutante sobrevivente: primeiro reescreva o código; só exclua no Pitest com justificativa, que vai para o apêndice do TCC.

## Skills relacionadas

`camerax`, `edge-to-edge`, `testing-setup`, `android-permissions-security`, `android-profiler` (medir fps e consumo na central), `r8-analyzer` (APK de release), `agp-9-upgrade`, `frontend-design` (identidade visual da tela de direção).
