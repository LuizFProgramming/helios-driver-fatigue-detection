# ADR-013: instalação da câmera e calibração obrigatória

Data: 16/11/2026. Situação: implementada no app 0.4.7; valores a validar no carro.

## Contexto

No teste de 16/11 às 09:28 (celular apoiado na mesa, câmera abaixo do rosto) o painel da foto anotada mostrou o
desvio do olhar vertical igual ao valor bruto e o pitch absoluto (-21 graus, "cabeça caindo" com a cabeça normal).
A sessão rodou sem calibração: ela só acontecia quando o usuário pedia, e sem ela as referências valiam zero. O app
julgava o motorista sem saber onde a câmera estava.

## Decisão

**Sem instalação, a viagem não começa.** O app abre direto no assistente; "Nova viagem" sem perfil também abre o
assistente. Enquanto ele está aberto, nenhum quadro vai para o alarme, para a tela de direção ou para a gravação.

**Assistente em cinco passos, só com o carro parado** (domínio em `dominio/instalacao`, testado na JVM; o app só
desenha):

1. **Posição da câmera:** atrás do volante, centro do painel, pilar A do lado do motorista ou perto do retrovisor;
   e o lado do motorista (esquerdo por padrão).
2. **Enquadramento ao vivo,** com um guia oval e cinco verificações, cada uma com ícone, nome e dica de correção:
   | Verificação | Certo quando | Dica |
   |---|---|---|
   | Rosto inteiro | caixa do contorno do rosto a 2% ou mais da borda | Centralize o rosto (com uma seta na imagem) |
   | Tamanho | rosto de 20% a 60% da largura da imagem | Aproxime ou afaste a câmera |
   | Luz | brilho médio do rosto de 60 a 220 (0 a 255) | Falta luz / luz forte demais |
   | Ângulo | giro e inclinação da cabeça, olhando a estrada, dentro da faixa da posição | Leve a câmera mais para a frente do rosto; suba ou abaixe a câmera |
   | Câmera parada | RMS da aceleração até 0,25 m/s² (o mesmo da histerese de movimento) | Fixe a câmera no suporte |

   Faixas de ângulo por posição (hipóteses da equipe): atrás do volante, giro até 15 graus e inclinação de -35 a 0;
   centro do painel, 45 e de -30 a 5; pilar A, 45 e de -15 a 20; retrovisor, 35 e de -5 a 35. O giro vale para os dois
   lados, porque o sinal do yaw depende de a imagem vir espelhada; a inclinação negativa é câmera abaixo do rosto.
   A calibração só é liberada depois de 1 s com as cinco verificações certas.
3. **Calibração de 10 s olhando a estrada:** a mediana de pitch, yaw, olhar e EAR de cada olho (o `Calibrador` de
   antes), mais o desvio padrão de cada sinal, para saber se o motorista ficou parado.
4. **Zonas de olhar do carro:** retrovisor interno, espelho esquerdo, espelho direito e painel. Para cada uma, o
   motorista toca em Gravar e olha para lá: depois de 1 s para levar os olhos, o app junta 2 s. Cada zona guarda o
   centro (mediana) e o espalhamento (2 desvios padrão, no mínimo 5 graus e 0,05 de olhar) **relativos à referência
   da estrada**: a recalibração curta de cada viagem move a referência e as zonas vão junto. Cada zona pode ser pulada.
5. **Resumo com a nota de 0 a 100:** verificações valem 60 (fração dos quadros da calibração com as cinco certas),
   estabilidade 25 (maior desvio de pitch e yaw: até 3 graus vale tudo, 8 ou mais nada) e zonas 15 (um quarto por
   zona). Boa a partir de 80, aceitável de 60 a 79, refazer abaixo de 60. A tela mostra os pontos de cada parte.

**Instalação para refazer não vira perfil** (decisão do grupo): o botão "Usar esta instalação" fica bloqueado e a
tela mostra a parte que falhou.

**Perfis.** Cada instalação aceita vira um perfil (nome, posição, lado, data, calibração, desvios, zonas, nota),
guardado nas preferências do app. Pode haver vários (dois carros, dois motoristas); Ajustes › Instalação da câmera
escolhe, renomeia, apaga e faz uma nova. Apagar o ativo passa para outro; sem nenhum, abre o assistente.

**Backup em arquivo.** O app tem `allowBackup="false"`, então o backup do Android não leva os perfis. "Exportar
perfis" grava um `.txt` em Download/HELIOS (ou na pasta escolhida); "Importar perfis" lê um arquivo desses. O arquivo
vem de fora do app: a leitura (`ArquivoPerfis`, em `protocolos`) confere cada campo e descarta o perfil inválido
sem perder os outros, e o app lê no máximo 5.000 linhas.

**Recalibração de 5 s em toda viagem** (`RecalibracaoViagem`): começa sozinha quando a pose fica estável (variação
de até 3 graus por 1 s) com o carro parado; até terminar vale a calibração do perfil. Se o carro andar no meio,
cancela; se faltarem amostras (rosto sumiu), recomeça. O chip "Calibrando" da tela de direção mostra o progresso.

**A calibração antiga é descartada** (decisão do grupo): ela não tinha posição nem zonas. As chaves dela saem das
preferências no primeiro "salvar".

## Consequências

- Quem atualizar o app passa uma vez pelo assistente antes da primeira viagem.
- Todos os limiares deste ADR estão em `ConfiguracaoInstalacao`, dentro da `ConfiguracaoDeteccao`, e são hipóteses
  da equipe: precisam de medida no carro (as três posições mais comuns, dia e noite).
- As zonas ainda não mudam a classificação: isso é a Parte C (ADR-014).
- O detector só lê os 36 pontos do contorno do rosto e calcula o brilho com o assistente aberto; na direção continua
  lendo 20 pontos.

## Verificação

- Domínio: `VerificacaoTest`, `NotaEZonaTest`, `AssistenteTest`, `RecalibracaoViagemTest` e o desvio no
  `CalibradorTest` (Kover de 100% de linha e ramo; nenhum mutante sobrevivente nos arquivos novos).
- Protocolos: `ArquivoPerfisTest` (ida e volta, perfis quebrados, notas 0 e 100, chave opcional ausente),
  `EnquadramentoTest` (caixa do contorno, brilho com amostragem) e `TextosInstalacaoTest`.
- App: `TelaInstalacaoTest` (Robolectric): em todas as etapas os alvos têm 48 dp e o texto 14 sp; a verificação que
  falha mostra ícone, nome e dica; "Usar esta instalação" bloqueado na nota para refazer; com o carro andando o
  assistente pede para parar. Capturas em `docs/interface/instalacao`.
- Não testado no aparelho: o assistente com a câmera de verdade, as faixas de ângulo e o tempo da recalibração.
