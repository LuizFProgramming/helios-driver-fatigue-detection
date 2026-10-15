# ADR-009: menu fixo, pasta HELIOS e análises de olhar, piscadas e olhos semicerrados

Data: 15/10/2026. Situação: aceita pela equipe em 15/10/2026, depois do segundo teste no celular (versão 0.3.0).

## Contexto

No teste da versão 0.3.0 apareceram três problemas.

1. **Menu instável.** O botão "Encerrar viagem" aparecia e sumia. O app escondia o menu sempre que o RMS da aceleração dos últimos 2 s passava de 0,35 m/s². Com o celular na mão ou com o motor ligado e o carro parado, esse valor oscila em volta do limiar. Pelo mesmo motivo, o app tirava o usuário da tela de ajustes ou do histórico.
2. **Fotos que não eram gravadas.** O interruptor de consentimento não abria o seletor de pasta, e sem pasta nenhuma foto era gravada. O aviso era um texto pequeno.
3. **Pouca coisa analisada.** A equipe perguntou se a análise usava tudo o que o MediaPipe oferece. Usava 18 dos 478 pontos e 3 dos 52 blendshapes. Ficavam de fora a íris, a direção do olhar e o semicerrar dos olhos.

## Decisão

1. **Menu fixo.** O botão "Menu" fica sempre visível no canto inferior direito e abre um painel com os números da viagem e as ações:
   - "Pausar" e "Retomar": a viagem continua aberta e o monitor recomeça do zero na volta;
   - "Encerrar": é preciso segurar o botão por 2 s;
   - "Histórico" e "Ajustes": desabilitados com o carro andando.

   O sensor nunca troca a tela sozinho. A detecção de movimento ganhou histerese: liga acima de 0,5 m/s² e desliga depois de 8 s seguidos abaixo de 0,25 m/s².
2. **Pasta fixa HELIOS.** Com o consentimento ligado, as fotos vão para `Pictures/HELIOS` e os CSV para `Download/HELIOS`, pelo MediaStore. Isso não pede permissão no Android 10 ou superior, que é o mínimo do app. A pasta escolhida pelo seletor continua disponível como opção.
3. **Histórico com fotos.** Cada evento aparece com a miniatura da foto anotada. O banco não mudou: a foto de cada evento é achada pelo horário, com a mesma regra do CSV.
4. **Análises novas.**
   - **Olhar fora da via.** Usa a posição da íris entre os cantos do olho e os blendshapes eyeLookUp e eyeLookDown, sempre em relação à calibração. Um olhar fora por 3 s seguidos leva a DISTRAÍDO. Somar 10 s fora da via em 30 s, com o olhar ou com a cabeça virada, também leva a DISTRAÍDO. As duas faixas são as de distração longa e de distração curta (VATS) da Euro NCAP.
   - **Piscadas no último minuto.** O app conta as piscadas e calcula a duração média. Leva a FADIGA com 5 piscadas ou mais e média de 250 ms ou mais.
   - **Olhos semicerrados.** O olho está aberto, mas abaixo de 80% da abertura calibrada, sem o blendshape eyeSquint alto. Leva a FADIGA quando ocupa metade do último minuto.

   O olhar não é medido com os olhos fechados. O olhar vertical também não é medido com o olho baixo. As duas regras impedem que sono vire distração.

## Consequências

- **Recalibrar.** A calibração guarda a direção do olhar e precisa ser refeita olhando para a estrada, não para a câmera.
- **Formatos.** O CSV de janelas ganha as colunas `olhar_fora` e `semicerrado`. O banco passa para a versão 2 com `ALTER TABLE`, sem perder o histórico.
- **Privacidade (LGPD).** Fotos em `Pictures/HELIOS` ficam visíveis para a galeria e para o backup do celular. Guardar uma cópia na área privada do app ficou para depois, por decisão da equipe.
- **Fotos depois de reinstalar.** Se o app for reinstalado, ele perde o direito de apagar as fotos antigas da pasta. A retenção e o botão de apagar removem só o registro, e o arquivo precisa ser apagado à mão.
- **Valores sem validação.** Estes limiares são hipóteses da equipe e precisam ser validados com vídeos rotulados:
  - olhar: desvio de 0,12 na horizontal e de 0,35 na vertical;
  - piscada média: 250 ms;
  - olho semicerrado: 80% da abertura calibrada e metade do minuto;
  - histerese do movimento: 0,5 e 0,25 m/s², 8 s.
- **Espelho e celular.** O app não distingue olhar o espelho de olhar o celular e conta os dois. A Euro NCAP só conta as olhadas para áreas que não têm a ver com a direção.

## Fontes

- SEEING MACHINES. Prevalence of Euro NCAP defined distraction in naturalistic driving. Disponível em: https://seeingmachines.com/prevalence-of-euro-ncap-defined-distraction-in-naturalistic-driving/. Acesso em: 15 out. 2026. Traz a distração longa (olhar único de 3 s ou mais) e o VATS (10 s somados em 30 s). Confirmar no protocolo Euro NCAP Safe Driving antes de citar no TCC.
- KLEIFGES, K. et al. BLINKER: automated extraction of ocular indices from EEG enabling large-scale analysis. Frontiers in Neuroscience, v. 11, art. 12, 2017. DOI: 10.3389/fnins.2017.00012. Traz a duração típica da piscada, de 0,1 a 0,5 s, e cita McIntire et al. (2014): a frequência e a duração das piscadas aumentam quando a vigilância cai.
