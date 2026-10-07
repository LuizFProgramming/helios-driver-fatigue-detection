# Próximos passos

As fases são as do documento 08, ajustadas para Kotlin. Cada prompt é para colar numa sessão nova do Claude Code aberta na pasta `04_App_HELIOS`; as skills de `.claude/skills` carregam sozinhas.

| Fase | Situação |
|------|----------|
| 0. Esteira e spikes | Esteira configurada (detekt, Kover, Pitest, tipos proibidos). Falta a primeira build no Android Studio e medir fps |
| 1. Domínio | Feito, com 67 testes. Falta o teste dourado contra o Python |
| 2. Pose da cabeça e calibração | Feito no domínio; validar sinais com o celular real |
| 3. MVP com câmera do aparelho | Código pronto; falta rodar no aparelho |
| 4. ESP32-CAM | Leitura MJPEG pronta no app; firmware próprio a fazer |
| 5. Histórico e CSV | A fazer |
| 6. ML | A fazer |
| 7. Instalação fixa | A fazer |

## Prompt 1. Primeira build e correções

```
Abra o projeto e rode ./gradlew :app:assembleDebug. Corrija os erros de compilação do módulo app
sem mudar a arquitetura (dominio e protocolos não dependem de Android; adaptadores do app não
tomam decisões). Depois rode ./gradlew qualidade e me mostre o que falhou em cada etapa
(detekt, koverVerify, pitest, verificarTiposProibidos). Para mutantes do Pitest que sobreviverem,
primeiro tente matar com teste; só proponha exclusão se o mutante for gerado pelo compilador Kotlin,
e me mostre a lista antes de aplicar.
```

## Prompt 2. Medir desempenho no aparelho (RNF03 e RNF04)

```
Use a skill android-profiler. Instale o app debug no aparelho conectado, deixe 2 minutos rodando
com a câmera frontal e meça: quadros por segundo processados (o rodapé mostra), uso de CPU e GPU,
temperatura e memória. Repita com o realce OpenCV ligado. Gere uma tabela em docs/medicoes.md com
aparelho, delegate (GPU/CPU), fps médio, p95 da latência entre a captura e a leitura do domínio.
Para medir a latência, adicione um log temporário com o timestamp da imagem e o momento em que
a Leitura chega, e remova depois.
```

## Prompt 3. Teste dourado contra o Python

```
No protótipo ../03_Codigo/prototipo_desktop_python, crie um script que lê um vídeo, roda o
MediaPipe e grava em CSV, por frame: timestamp_ms, os 18 pontos usados (olhos e lábio interno, em
pixels), eyeBlinkLeft, eyeBlinkRight, jawOpen, EAR e MAR calculados pela função do protótipo.
Copie o CSV para dominio/src/test/resources/dourado.csv e escreva um teste em dominio que lê o
CSV, monta SeisPontos e confere razaoAspecto com tolerância 1e-6.
```

## Prompt 4. Firmware da ESP32-CAM (Fase 4)

```
Crie firmware/ com PlatformIO para ESP32-CAM AI-Thinker (OV2640, PSRAM):
SoftAP "HELIOS-CAM" com senha WPA2 em include/config.h, IP 192.168.4.1;
GET /stream na porta 81 em MJPEG QVGA, qualidade 12; GET /status com fps, clientes, RSSI e
temperatura; POST /buzzer?padrao=N (0 desliga, 1 fadiga, 2 crítico) no GPIO configurável; LED IR.
A lógica pura (tabela de padrões, fps em janela, validação de parâmetros) fica em lib/helios_logica
sem includes do Arduino, com testes Unity no ambiente native. Complexidade < 22 por função.
Os códigos 1 e 2 têm de bater com codigoBuzzer em protocolos/Alarme.kt.
```

## Prompt 5. Histórico local e CSV (Fase 5)

```
Adicione Room ao app (com KSP) para guardar sessões e eventos: Sessao(id, inicio, fim) e
EventoSalvo(sessaoId, tipo, inicioMs, duracaoMs). Crie a porta RepositorioSessoes em app/portas e
grave leitura.eventos no ponto marcado "Fase 5" do HeliosViewModel, fora da thread do domínio.
Crie também o modo coleta: a cada 30 s, uma linha com PERCLOS de 60 s, piscadas por minuto,
duração média da piscada, bocejos por minuto e desvio padrão do pitch, mais um rótulo KSS digitado
com o carro parado. A agregação em janelas fica em dominio (função pura testada). Tela de histórico
só abre com o carro parado, como a de ajustes. Exportar CSV pelo seletor de arquivos do Android.
```

## Prompt 6. Halstead e duplicação

```
Crie o módulo ferramentas/ (Kotlin JVM) com um verificador de dificuldade de Halstead por função
usando o PSI do compilador Kotlin (kotlin-compiler-embeddable): D = (n1/2) × (N2/n2). Falhar se
alguma função de dominio, protocolos ou app passar de 79. Teste o próprio verificador com funções
de exemplo de dificuldade conhecida. Registre a tarefa no "qualidade" da raiz, junto com o jscpd
(threshold 0) sobre */src/main.
```
