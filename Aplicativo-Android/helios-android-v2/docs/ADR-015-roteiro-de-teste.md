# ADR-015: roteiro de teste guiado e coleta para a CNN com óculos

Data: 16/11/2026. Situação: implementada no app 0.4.11; ainda não rodada no aparelho.

## Contexto

Os logs do teste de 16/11 às 12:54 (motorista de óculos) mostraram dois problemas que o grupo não conseguia medir:

- A CNN do olho (ADR-011) chamou de "fechado" 464 dos 1.057 quadros em que o app via o olho aberto (44%). Com o olho
  fechado ela acompanhou o app em 88% dos quadros. A armação e o reflexo das lentes entram no recorte, e a rede foi
  treinada com a CEW, quase sem óculos.
- Os logs não dizem o que a pessoa estava fazendo. Sem essa referência, não há como calcular acerto, precisão ou
  atraso dos alertas; só a concordância entre a regra do app e a CNN.

O grupo decidiu que funcionar com óculos é prioridade e dividiu o trabalho em três fases: (1) coletar e validar com
rótulo, (2) retreinar a CNN para óculos, (3) trocar o modelo no app. Esta ADR é a fase 1.

## Decisão

**Roteiro de teste guiado** (Ajustes › Análise › "Roteiro de teste"), com o carro parado e o celular no suporte.
São 15 tarefas em cerca de 2 minutos, cada uma com 3 s de preparo para o rótulo não pegar o tempo de reação:

| Tarefa | Duração | Olho esperado | Alerta esperado |
|---|---|---|---|
| Olhe para a estrada | 6 s | aberto | nenhum |
| Pisque normalmente | 8 s | (varia) | nenhum |
| Feche os olhos e abra no bipe | 2 s | fechado | microssono (passa de 1 s, não chega a 3 s) |
| Feche os olhos por mais tempo | 4 s | fechado | sono e CRÍTICO (passa de 3 s) |
| Espelho esquerdo, espelho direito, painel | 2 s cada | aberto | nenhum (abaixo dos 3 s das zonas) |
| Vire o rosto para a janela | 5 s | aberto | distração (passa de 3 s) |
| Boceje | 6 s | (varia) | bocejo |

Depois de cada tarefa com alerta vem "olhe para a estrada". O app fala a próxima tarefa no preparo e toca um bipe no
começo de cada tarefa e no fim das de olho fechado, para a pessoa saber quando abrir. Os alarmes tocam normalmente:
fazem parte do que se mede. As durações seguem a `ConfiguracaoDeteccao` padrão; com outra sensibilidade, o esperado
muda.

**Arquivos**, em Download/HELIOS (ou na pasta escolhida):

- `roteiro.csv`: uma linha por tarefa feita, com início, fim, olho e alerta esperados e se a pessoa estava de óculos.
- `quadros.csv`: ganha as colunas `oculos` e `tarefa` (`PREPARO` na contagem). Durante o roteiro ele é gravado com o
  consentimento do roteiro, mesmo com o registro por quadro desligado.
- `recortes.csv`: os recortes 64x64 em cinza que a CNN recebe, 5 por segundo, dos dois olhos, só nas tarefas com
  olho esperado; os 4096 bytes vão em base64, com o EAR e a CNN do mesmo quadro. Cerca de 6 MB por roteiro.

**Consentimento próprio, desligado por padrão.** "Autorizo guardar os números do rosto e os recortes dos olhos deste
teste". Os recortes são imagem do olho (dado biométrico, LGPD): só existem durante o roteiro, ficam só no celular, o
app não usa a internet e eles não aparecem na galeria. Sem a autorização, o roteiro grava só o `roteiro.csv`, e os
alertas ficam no `eventos.csv` do histórico.

**Óculos declarados.** Ajustes guarda "Estou de óculos"; vai para o `quadros.csv` de toda viagem e para o roteiro.

**Análise fora do app.** `scripts/analise_logs/analisar.py` lê a pasta e grava `relatorio.md`:

- olho aberto ou fechado, por óculos: acurácia, precisão, recall, F1 e "falso fechado" da regra do app e da CNN,
  ignorando os primeiros 700 ms de cada tarefa;
- alertas esperados: se o evento saiu (até 2 s depois do fim) e o atraso até o primeiro estado de alerta;
- alarmes falsos: fração dos quadros em ATENÇÃO, CRÍTICO, DISTRAÍDO ou DESATENTO nas tarefas sem alerta (FADIGA não
  conta: é cansaço acumulado).

**Onde fica cada coisa.** O roteiro e as tarefas são do domínio (`dominio/validacao`, puro, sem estado). Os textos,
os avisos de voz e bipe (`avisosDoRoteiro`) e os CSV ficam em `protocolos`. A condução fica numa classe do app
(`ConducaoRoteiro`), fora do ViewModel. O detector recorta os olhos durante o roteiro mesmo com a CNN desligada.

## Consequências

- O capítulo de resultados ganha números com referência: a mesma tabela com e sem óculos, para a regra do app e
  para a CNN.
- O `recortes.csv` vira dado de treino e, principalmente, de teste da fase 2. Ele não vai para o git: tem imagem de
  pessoas reais. Para usar recortes de outros membros do grupo, cada um precisa autorizar no próprio aparelho.
- Os rótulos vêm da instrução, não de quem confere a imagem: quem pisca durante "olhe para a estrada" vira um olho
  fechado rotulado como aberto. A margem de 700 ms e o EAR gravado junto ajudam a achar esses casos.

## Verificação

- Domínio: `RoteiroTest` (preparo e tarefa, fim, intervalos, roteiro padrão, esperado de cada tarefa).
- Protocolos: `RoteiroCsvTest` (`roteiro.csv`, colunas novas do `quadros.csv`, `recortes.csv` em base64, textos, voz e
  bipe nas trocas).
- App: `GravacaoViagemTest` (tarefa nos quadros, recortes só com rótulo e 5 por segundo, nada sem consentimento,
  `roteiro.csv` com o horário da viagem), `ConducaoRoteiroTest` (começo, avisos, fim, cancelar), `TelaRoteiroTest`.
- Script: `scripts/analise_logs/tests` (rótulo com margem, métricas, alertas, alarmes falsos, leitura da pasta).
- Não testado no aparelho.
