# ADR-010: frases de alerta geradas fora do carro

Data: 14/11/2026. Situação: proposta para revisão do grupo.

## Contexto

O aplicativo precisa emitir falas curtas e adequadas para os estados de
atenção, fadiga, desatenção, distração e sono crítico. Um modelo de linguagem
foi usado fora do carro para propor as frases, respeitando limites de palavras
e a revisão humana da equipe.

## Decisão

As frases aprovadas ficam gravadas no código em bancos separados por estado.
Durante a viagem, o aplicativo não chama um LLM nem gera texto: ele sorteia
uma frase pronta do banco correspondente, sem repetir até consumir a rodada e
sem iniciar a próxima rodada pela última frase pronunciada.

O TextToSpeech do Android continua sendo o canal de áudio. A ESP32-CAM recebe
somente os códigos numéricos de alerta para o buzzer e o OLED; ela não recebe
nem sintetiza as frases.

## Consequências

- O comportamento é offline, determinístico quanto aos bancos e auditável.
- A revisão das frases precisa ocorrer antes de uma versão final do aplicativo.
- O TCC deve descrever a IA generativa como apoio à criação das frases fora do
  carro, e não como um modelo executado em tempo real no veículo.

## Fora desta decisão

Um LLM local ou remoto durante a viagem. Essa alternativa exigiria avaliar
latência, conectividade, consumo e segurança antes de ser considerada.
