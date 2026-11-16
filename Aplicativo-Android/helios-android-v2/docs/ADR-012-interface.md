# ADR-012: Interface guiada por diretrizes publicadas

Data: 16/11/2026. Situação: Fase 1 (tokens de design) entregue no app 0.4.5; Fases 2 a 4 em andamento.

## Contexto

A interface do HELIOS foi desenhada por gosto, sem uma regra escrita por trás de cada escolha. O TCC precisa
justificar o layout com fontes, e o app é usado num carro em movimento, onde uma olhada longa na tela é um risco.
Este ADR fixa oito regras de base, cada uma com a fonte, e registra onde cada regra foi aplicada e como foi
verificada. Nenhuma regra de classificação, alarme ou gravação muda neste trabalho.

## Regras

1. **Leitura de relance.** O motorista entende o estado em até 2 s e o app responde a um toque em até 0,25 s
   (Google, Android for Cars, Design principles). No máximo 2 s por olhada e 12 s no total por tarefa (NHTSA, 2013).
2. **Nada de imagem dinâmica com o carro em movimento** (Android for Cars). O visor ao vivo da câmera não aparece
   na tela de direção com o carro andando; vale o sinal `emMovimento` do app, com a histerese atual.
3. **Alvos de toque.** 76 x 76 dp no mínimo em tudo que se usa com o carro ligado (Android for Cars, Sizing) e
   48 x 48 dp nas telas usadas com o carro parado (Material 3; Android Accessibility). Alvos não se sobrepõem.
4. **Texto.** Na tela de direção, texto principal com pelo menos 32 dp e secundário com 24 dp, no máximo
   120 caracteres por item (Android for Cars, Visual principles). Nas outras telas, a escala tipográfica do
   Material 3, com corpo nunca abaixo de 14 sp.
5. **Contraste.** 4,5:1 para texto e 3:1 para ícones e bordas (W3C, WCAG 2.1, critérios 1.4.3 e 1.4.11). O estado
   nunca é passado só por cor: palavra, ícone e cor.
6. **Tema escuro como padrão**, com tema claro opcional em Ajustes. Cores por papel do Material 3, sem cor dinâmica
   do sistema: as cores de estado são fixas para o motorista aprender o significado.
7. **Hierarquia e escolhas.** Uma informação principal por tela de direção; no máximo 4 ações por painel
   (Hick, 1952); botões mais usados perto da borda e grandes (Fitts, 1954); espaçamento em grade de 8 dp.
8. **Movimento.** Transições de 200 a 300 ms; durante um alerta, só a pulsação do selo de estado.

## Decisões da Fase 1 (tokens de design)

**Cores em `protocolos/Cores.kt`, como dados puros.** Os dois esquemas (`TEMA_ESCURO` e `TEMA_CLARO`) definem
todos os papéis do Material 3 que algum componente do app usa. O `Tema.kt` só converte esses valores para o
`ColorScheme`. Assim o teste de contraste roda na JVM, sem Android, e confere exatamente as cores que o app mostra.

**O primary é azul, não âmbar.** Antes o primary era o âmbar da FADIGA, e um botão comum tinha a cor de um
alerta. Agora o primary é o azul das placas de indicação de serviços, que no trânsito quer dizer "informação".
Âmbar, laranja e vermelho ficam reservados aos estados.

**As cores de estado são as mesmas nos dois temas.** O selo é preenchido com a cor do estado, então o contraste
depende só do par texto e fundo do selo, não do fundo da tela. Seis tons, tirados da sinalização viária: verde
(ATENTO), amarelo (ATENÇÃO), âmbar (FADIGA), laranja (DESATENTO e DISTRAÍDO), vermelho (CRÍTICO) e cinza
(SEM ROSTO e estados do app). O vermelho passou a ser escuro com texto branco: o pulso do alerta escurece o
fundo, e com texto branco o contraste só aumenta durante o pulso.

**Cada estado tem um ícone próprio** (`Icone` em `ModeloDeTela.kt`), além da palavra e do tom. Na Fase 1 o ícone
existe no modelo de tela; ele passa a ser desenhado no selo na Fase 2. DESATENTO e
DISTRAÍDO dividem o laranja, mas não o ícone. Os ícones são Material Symbols Rounded preenchidos (Apache 2.0),
copiados como vetores para `res/drawable`, sem biblioteca nova.

**Fonte Atkinson Hyperlegible Next** (Braille Institute, licença OFL, em `docs/licencas`). Foi desenhada para que
letras parecidas (I, l e 1; O e 0; rn e m) não se confundam, o que importa numa leitura de relance. Tem o zero
cortado e algarismos tabulares, então números que mudam não "tremem" na tela. É um arquivo variável de 112 KB,
dentro do APK, sem download. A palavra do estado usa o ajuste automático de tamanho do Compose, que mede o texto
na fonte real e preenche a largura sem cortar a palavra.

**Tipografia, formas e espaços** (`Tema.kt`):
- escala do Material 3 com `bodySmall`, `labelMedium` e `labelSmall` elevados a 14 sp;
- tamanhos de direção `Tipos.principal` (32 sp), `Tipos.secundario` (24 sp) e `Tipos.numero` (48 sp, tabular),
  em sp para respeitar a escala de texto do sistema e a de Ajustes;
- cantos de 16 a 28 dp;
- `Espaco` (8, 16, 24 e 32 dp) e `Alvo` (76 dp para a direção, 48 dp para o carro parado).

**Tema claro em Ajustes › Tela de direção**, com o escuro como padrão. O tema é do app e não do sistema, então a
`MainActivity` ajusta a cor dos ícones da barra de status e de navegação ao tema escolhido.

**Ficou de fora:** a Styles API do Compose (experimental e só para componentes próprios; os papéis do Material 3
resolvem) e os painéis adaptativos com Navigation 3 (o app não usa Navigation 3). A central larga é tratada pelo
tamanho da janela, dentro das próprias telas.

## Achados da Fase 1

- **Texto preto sobre fundo escuro em Ajustes e Histórico** (versão 0.4.4). O app não usava `Surface`, e no
  Material 3 a cor de texto padrão, sem `Surface`, é preta: contraste perto de 1:1. As capturas do "antes"
  mostram o problema. O `TemaHelios` agora envolve o conteúdo num `Surface`.
- **Duas cores antigas abaixo do mínimo:** o vermelho do CRÍTICO com o fundo escuro dava 3,77:1, nos dois
  sentidos, e o cinza "sem sinal" usado como texto dava 3,01:1.
- **Texto com transparência.** O motivo e o rodapé da tela de direção usavam a cor do texto com 65% a 80% de
  opacidade. No CRÍTICO, o rodapé dava 3,54:1. Esses textos passaram a usar `onSurfaceVariant`, que é testado,
  ou o branco do selo durante o alerta.

## Onde cada regra foi aplicada e como foi verificada

| Regra | Onde | Verificação | Fase |
|---|---|---|---|
| 1. Relance (2 s, 0,25 s, 12 s) | Tela de direção | Pendente | 2 |
| 2. Sem visor com o carro andando | Tela de direção | Pendente | 2 |
| 3. Alvos de 76 e 48 dp | Tokens `Alvo.direcao` e `Alvo.parado` (`Tema.kt`) | Medida do alvo em dp nas telas | 1 (tokens), 2 e 3 |
| 4. Texto 32/24 dp, corpo ≥ 14 sp | `Tipos.principal`, `Tipos.secundario`; escala do Material 3 com mínimo de 14 sp (`Tema.kt`) | Leitura do código; aplicação na direção na Fase 2 (o rodapé de 16 sp sai lá) | 1 (tokens), 2 |
| 5. Contraste 4,5:1 e 3:1; palavra, ícone e cor | `Cores.kt`; `Icone` e `Tom` no `EstadoTela` | `CoresTest`: todo par de texto dos dois temas ≥ 4,5:1 (13 pares fixos mais 4 cores sobre 8 superfícies, por tema); `outline` sobre as 8 superfícies ≥ 3:1; selo de cada tom ≥ 4,5:1; selo do CRÍTICO ≥ 4,5:1 no ponto mais escuro do pulso. `ModeloDeTelaTest`: um ícone diferente por estado | 1 (cores e modelo), 2 (ícone desenhado no selo) |
| 6. Escuro padrão, claro opcional, sem cor dinâmica | `TemaHelios(escuro)`, `Ajustes.temaClaro`, `MainActivity` | `CoresTest` nos dois temas; capturas em `docs/interface/fase1/depois` | 1 |
| 7. Uma informação principal, ≤ 4 ações, Fitts, grade de 8 dp | Token `Espaco` (`Tema.kt`) | Pendente nas telas | 1 (tokens), 2 e 3 |
| 8. Movimento de 200 a 300 ms; só o selo pulsa | Pulso do CRÍTICO por escurecimento (`corDoPulso`, 250 ms cada sentido) | `CoresTest` no ponto mais escuro; o pulso passa a ser só do selo na Fase 2 | 1 (pulso), 2 |

`outlineVariant` fica fora do teste de 3:1: é divisória decorativa, que o critério 1.4.11 não cobre.

## Capturas

`docs/interface/fase1/antes` (tema escuro, como estava na 0.4.4) e `docs/interface/fase1/depois` (escuro e claro).
Cada tela foi capturada em dois tamanhos: celular em paisagem (915 x 412 dp) e central larga (1280 x 720 dp).
As capturas saem do screenshot test oficial do Compose (`app/src/screenshotTest`, plugin
`com.android.compose.screenshot`), com dados inventados e sem rosto de pessoa:

- gravar: `gradle :app:updateDebugScreenshotTest`;
- comparar com as de referência: `gradle :app:validateDebugScreenshotTest`.

## Fontes

- GOOGLE. Android for Cars: Design principles; Sizing; Visual principles. Disponível em:
  developers.google.com/cars/design.
- NATIONAL HIGHWAY TRAFFIC SAFETY ADMINISTRATION (NHTSA). Visual-Manual NHTSA Driver Distraction Guidelines for
  In-Vehicle Electronic Devices. Washington, DC, 2013.
- W3C. Web Content Accessibility Guidelines (WCAG) 2.1. Critérios 1.4.3 (Contraste mínimo) e 1.4.11 (Contraste
  não textual). 2018.
- GOOGLE. Material Design 3: Color roles; Typography; Accessibility. Disponível em: m3.material.io.
- GOOGLE. Android Accessibility: Make apps more accessible (alvos de toque de 48 dp). Disponível em:
  developer.android.com.
- FITTS, P. M. The information capacity of the human motor system in controlling the amplitude of movement.
  Journal of Experimental Psychology, v. 47, n. 6, p. 381-391, 1954.
- HICK, W. E. On the rate of gain of information. Quarterly Journal of Experimental Psychology, v. 4, n. 1,
  p. 11-26, 1952.
- BRAILLE INSTITUTE OF AMERICA. Atkinson Hyperlegible Next. Licença SIL Open Font License 1.1, 2025.
