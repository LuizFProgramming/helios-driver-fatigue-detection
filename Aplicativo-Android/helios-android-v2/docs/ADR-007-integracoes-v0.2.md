# ADR-007: integrações da versão 0.2.0

Data: 15/10/2026. Situação: aceita pela equipe em 14/10/2026 (decisões listadas abaixo).

## Contexto

O repositório tinha dois aplicativos com partes diferentes: o protótipo Python, que é o que a equipe roda, e o app Kotlin, mais completo no domínio. A análise de projetos externos (documento "HELIOS, Integrações externas") listou o que vale acoplar. A versão 0.2.0 junta tudo no app Kotlin, que passa a ser o aplicativo principal, e continua offline.

## Decisões

| Tema | Decisão | Motivo |
|------|---------|--------|
| Estados | DESATENTO (rosto fora por 2 s) e DISTRAÍDO (yaw acima de 20° por 2 s) entram no domínio | Já existiam no Python e no Driver-State-Detection; o Kotlin não tinha distração |
| Alarme | Escala aos 5 e aos 10 s no mesmo alerta | Motorista que não reage precisa de um alarme mais insistente |
| Voz | TextToSpeech do Android | Zero dependência nova; funciona offline com a voz instalada |
| Histórico | SQLite nativo, sem Room | Mesma função para o que o app precisa, sem KSP nem biblioteca extra |
| Gráfico | Desenhado em Compose, sem Vico | Um gráfico de linha simples não justifica uma biblioteca |
| Fotos | Pasta escolhida pelo usuário (SAF), com consentimento, limite de quantidade e retenção por tempo de uso | Pedido da equipe; o SAF não exige permissão de armazenamento |
| CSV | Vírgula e ponto decimal | Leitura direta no Python (pandas) |
| OpenCV | Mantido | A equipe prefere a implementação consolidada por enquanto, mesmo com cerca de 41 MB a mais no APK |
| Bitmaps | Pool de 3, com chave liga/desliga e diagnóstico na tela | Reduz alocação por frame; o liga/desliga permite medir o efeito |
| Telemetria | Bloqueio só pelo manifesto | Excluir o módulo `transport-backend-cct` faria o app fechar ao abrir (conferido no bytecode do MediaPipe 0.10.35) |
| Arquiteturas | Release só ARM | Celulares e centrais multimídia; x86 só no debug para o emulador |

## Fora desta versão

- **Zero-DCE + LiteRT.** O modelo pesa 0,2 MB, mas o runtime soma alguns MB por arquitetura. Não acelera nada, só pode melhorar a detecção no lusco-fusco. Vale um experimento separado com vídeos escuros.
- **LLM local** (Gemma pelo LiteRT-LM). Os modelos têm de 0,5 a 3 GB e pedem celular de ponta.
- **Room e Vico.** Não fazem falta para o que o app precisa hoje.
- **Activity Recognition.** Depende do Google Play Services e conflita com o RNF01.
- **IA generativa.** Fica como notebook Python offline para testar a robustez do detector, numa tarefa separada.

## Consequências

- Os novos limiares (2 s, 20°) e o padrão sonoro de desatenção precisam de validação com vídeos rotulados antes de ir para o texto do TCC como resultado.
- A exportação CSV alimenta o notebook de validação da R4S2.
