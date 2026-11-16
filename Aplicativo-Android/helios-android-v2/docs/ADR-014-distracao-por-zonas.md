# ADR-014: distração com zonas do carro e manobra

Data: 16/11/2026. Situação: implementada no app 0.4.8; tolerâncias a validar no carro.

## Contexto

No teste de 16/11 às 09:28, DISTRAÍDO dominou: o olhar foi marcado fora da via em 63% a 100% dos quadros de várias
janelas, com o motorista olhando para a tela. O app não distinguia espelho de celular: virar a cabeça para o espelho
esquerdo dava DISTRAÍDO em 2 s. A Euro NCAP separa distração longa (uma olhada única de 3 s ou mais fora da via) de
curta (VATS: olhadas curtas somando 10 s em 30 s) e trata de forma diferente as olhadas para áreas ligadas à
direção. Com a instalação do ADR-013, o app passou a saber onde ficam os espelhos e o painel de cada carro.

## Decisão

**Zona de cada olhada fora da estrada.** Quando o olhar ou a cabeça saem da estrada, o domínio compara a olhada com
as zonas do perfil (retrovisor interno, espelho esquerdo, espelho direito, painel), tudo relativo à referência da
estrada. A distância é normalizada pelo espalhamento de cada zona; dentro é até 1, e a mais próxima ganha. O olhar
só entra na conta quando foi medido agora e na gravação da zona (olho baixo não mede); sem ele vale a cabeça.

**Tolerâncias separadas** (em `ConfiguracaoDeteccao`):
| Olhada | Vira DISTRAÍDO | Soma no VATS (10 s em 30 s) |
|---|---|---|
| Espelho ou painel (zona da direção) | 3 s seguidos (`toleranciaZonaMs`) | Não |
| Outra direção | como antes: olhar 3 s seguidos, cabeça virada 2 s | Sim |

O painel entrou junto com os espelhos (decisão do grupo): é olhada ligada à direção. Uma cabeça virada para um
espelho segue a tolerância do espelho, não os 2 s da cabeça virada.

**Sono vem antes da zona.** Uma zona nunca apaga sinal de sono: a cabeça que cai na direção do painel continua sendo
cabeça caída (CRÍTICO), e olho fechado continua sendo olho fechado.

**Sem zonas, nada muda.** Perfil sem zonas (todas puladas na instalação) ou monitor sem perfil classificam toda olhada
como "outra direção", com as regras de antes; os testes antigos continuam valendo sem alteração.

**Olhar para trás.** O rosto que vira tanto que some da câmera entra em DESATENTO no mesmo tempo de antes (2 s). Os
eventos de cabeça virada e de rosto ausente guardam o maior giro da cabeça em relação à estrada (`yaw_max_graus` no
`eventos.csv`; banco na versão 5).

**Manobra.** Botão "Manobra: 30 s sem alerta de distração" no Menu (76 dp), para estacionar, dar ré ou olhar para
trás. Por 30 s (`manobraMs`) o classificador não vê os sinais de distração e o rosto ausente não vira DESATENTO.
Os alertas de sono continuam sempre. Tocar de novo com a manobra ativa recomeça os 30 s; o intervalo inteiro vira
um evento `MANOBRA` no `eventos.csv` (início e duração). A tela de direção mostra o chip "Manobra: 24 s".

**Texto na tela.** A linha de apoio diz a zona: "Olhando o espelho esquerdo há 4 s".

## Manobra: impacto para o motorista e para o TCC

**Escolha do grupo: tocar de novo recomeça os 30 s.** Exemplo: toque aos 0 s para dar ré; aos 20 s, ainda
manobrando, novo toque; a manobra vai até os 50 s e vira um evento só (0 s, 50 s). As alternativas eram cancelar
(um toque por engano traria o alerta de volta no meio da ré) ou ignorar o toque (o alerta voltaria aos 30 s numa
manobra longa).

Para o motorista:
- **Ganho:** estacionar em vaga apertada, dar ré com várias idas e vindas ou olhar para trás seguidas vezes sem
  alarme falso; um toque a mais estende, sem precisar acompanhar a contagem.
- **Risco:** usar o botão como "silenciar". Quem toca a cada 30 s fica sem alerta de distração enquanto dirige, por
  exemplo mexendo no celular. O que já limita isso: o sono nunca é suspenso; o botão fica dentro do Menu (dois toques,
  dentro do limite de 2 s por olhada da NHTSA); o chip "Manobra: N s" fica na tela; cada manobra fica no
  `eventos.csv` com a duração.
- **Não implementado (precisa de decisão do grupo):** limitar a manobra ao carro parado ou devagar (o app só sabe
  "em movimento" ou não, sem velocidade), limitar o total de manobra por viagem ou alertar quem a usa demais.

Para o TCC:
- **Métricas:** durante a manobra o tempo em DISTRAÍDO e DESATENTO cai. Os resultados precisam mostrar quanto da
  viagem ficou em manobra (eventos `MANOBRA`; coluna `manobra_ms` por janela na Parte D) e analisar as janelas com e
  sem ela.
- **Validade:** a Euro NCAP não prevê suspensão manual. Distração real durante uma manobra vira falso negativo. Entra
  como limitação, com o tempo total de manobra por viagem e o número de toques nos testes do grupo.
- **Avaliação:** comparar quantos alertas de distração a manobra evitou em manobras de verdade (falsos positivos que
  sumiram) com quantas distrações reais ficaram sem alerta. Os 30 s são hipótese da equipe, a medir.
- **Segurança e ética:** justificar por que o sono nunca é suspenso e discutir o risco de uso como silenciador.

## Consequências

- O motivo novo `OLHAR_ZONA` aparece no histórico e no CSV.
- As colunas de tempo por zona e de manobra no `janelas.csv` (`zona_espelho_ms`, `zona_painel_ms`, `zona_outra_ms`,
  `manobra_ms`) ficam para a Parte D.
- As tolerâncias (3 s para zona, 30 s de manobra) e o espalhamento das zonas são hipóteses da equipe, a medir no
  carro com olhadas reais para espelhos e painel.

## Verificação

- Domínio: `ZonasEManobraTest` (zona mais próxima; olhar medido longe tira a olhada da zona; espelho só distrai
  depois de 3 s; sem zonas igual a antes; espelho e painel fora do VATS; cabeça caída continua CRÍTICO; maior giro
  nos eventos; manobra suspende distração e desatenção mas não o sono; tocar de novo recomeça e vira um evento só;
  zonas relativas à referência). Kover de 100%; nenhum mutante novo sobrevivente.
- Protocolos: linha de apoio com a zona; coluna `yaw_max_graus`. App: chip da manobra e botão de 76 dp no Menu
  (`TelaDirecaoTest`); migração do banco para a versão 5 (`EsquemaBancoTest`).
- Não testado no carro.
