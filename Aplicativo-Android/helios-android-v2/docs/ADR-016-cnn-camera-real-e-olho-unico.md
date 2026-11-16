# ADR-016: CNN retreinada para a câmera real e opção de um olho só

Data: 16/11/2026. Situação: implementada no app 0.4.14; a CNN continua em modo sombra.

## Contexto

O modelo do ADR-011, treinado só com a CEW, não servia para a câmera do celular. Nos testes de 16/11 ele chamou de
fechado o olho aberto em 46% dos quadros com óculos e em 96% a 100% dos quadros sem óculos, com a câmera baixa e a
luz do teto atrás da cabeça. Em 21 fotos originais do app, passadas pelo mesmo recorte do treino (36 olhos abertos
pelo EAR), errou 92%. O recorte real tem contraste de 20,7 (desvio padrão do cinza), contra cerca de 34 na CEW.

À parte, o grupo levantou o caso de quem usa um olho só. A regra do app, com os dois olhos visíveis, só conta olho
fechado quando os dois fecham: piscar ou manter um olho fechado não é sono. Para quem tem uma prótese ou um olho que
não fecha, isso significa que o app nunca vê "os dois fechados" e nunca detecta o sono.

## Decisão

**Retreino com aumentos da câmera real** (`scripts/experimento_olhos/treinar_real.py` e `aumentos.py`). Mesma rede
`media` e mesma entrada (64x64x1, cinza de 0 a 255), para trocar só o arquivo `olho_cnn.tflite`:

- Aumentos em cada época, iguais para olho aberto e fechado: armação de óculos desenhada (inteira ou meio aro, com
  ponte ou haste), lente levemente escura, reflexo claro na lente, contraluz (gradiente de luz), pouco contraste
  (30% a 100%) com deslocamento de brilho, ruído e desfoque, e achatamento vertical da câmera de baixo.
- Dados: 3.332 olhos de treino da CEW e 24.000 da MRL Eye, com prioridade para os recortes da MRL marcados com
  óculos ou reflexo. A MRL entra só no treino.
- Validação com a CEW limpa e duas cópias aumentadas, para a escolha da época não favorecer só a foto limpa.
  25 épocas; melhor AUC na validação de 0,992.

**Resultado** (teste da CEW dividido por pessoa; "falso fechado" é o olho aberto chamado de fechado):

| Condição | Atual: F1 | Atual: falso fechado | Novo: F1 | Novo: falso fechado |
|---|---|---|---|---|
| CEW limpo | 0,947 | 4,4% | 0,950 | 0,8% |
| CEW com os aumentos | 0,773 | 49,5% | 0,930 | 3,6% |
| CEW só com óculos desenhados | 0,726 | 58,2% | 0,918 | 3,3% |
| Fotos reais do app (36 olhos abertos) | sem olho fechado | 91,7% | sem olho fechado | 25,0% |

O modelo novo substitui o do ADR-011 no app (310.192 bytes, SHA-256 conferido no build). **Continua em modo sombra**:
25% de falso fechado nas fotos reais ainda é muito para decidir alarme, e não há olho fechado real rotulado.

**Opção de um olho só** (Ajustes › Acessibilidade): "Os dois olhos (padrão)", "Só o meu olho direito" ou "Só o meu
olho esquerdo". Com um olho escolhido (`ConfiguracaoDeteccao.olhoUnico`), só ele decide olho fechado, abertura e
olhar; o outro pode ficar sempre aberto ou sempre fechado sem alerta. A escolha vence a troca automática de olho com
a cabeça de lado. Vale a partir da próxima viagem. No padrão, nada muda.

- O `quadros.csv` ganha a coluna `olho_avaliado` (AMBOS, DIREITO, ESQUERDO).
- A CNN passa a ser comparada pelo olho avaliado: nas janelas (`cnn_fechado_media`, `cnn_discordancia`) e no script
  `scripts/analise_logs` vale a probabilidade desse olho; com os dois, a média.
- "Direito" no código é o olho do lado esquerdo da imagem, que é o olho direito do motorista quando a câmera não
  espelha. A linha "Avaliado" do visor permite conferir no aparelho.

## Consequências

- As licenças da CEW (só pesquisa, sem uso comercial) e da MRL Eye (uso acadêmico) precisam constar no TCC.
- Os óculos do treino são desenhados. Óculos reais só entram quando o grupo rodar o roteiro de teste (ADR-015) com a
  autorização dos recortes; o `recortes.csv` é o teste que falta, com olho fechado e com óculos de verdade.
- A MRL é infravermelha. Ela ajudou nos números, mas a câmera do app é de luz visível.
- Para a CNN entrar na regra, a meta sugerida continua a do plano: F1 de 0,90 ou mais com óculos no roteiro e
  discordância abaixo de 10% em viagens reais.

## Verificação

- Experimento: `tests/test_aumentos.py` (formato, contraste, armação, achatamento, semente); comparação em
  `scripts/experimento_olhos/resultados/real.md`; conjunto real por `fotos_reais.py`.
- Domínio: `OlhoUnicoTest` (padrão igual a antes; olho escolhido fechado é sono com o outro aberto; o outro olho
  fechado não alerta; a escolha vale com a cabeça de lado) e `CnnNaJanelaTest` (CNN pelo olho avaliado).
- Protocolos: coluna `olho_avaliado`. Script: `test_cnn_vale_pelo_olho_avaliado_quando_o_app_usa_um_so`.
- Não testado no aparelho: o modelo novo em viagem e a opção de um olho só.
