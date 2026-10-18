# Prompts prontos (colar no Claude Code aberto em Aplicativo-Android/helios-android-v2)

## 1. Compilar e validar as mudanças de 18/10
```
Leia CLAUDE.md e .ai-historico/sessoes.md. Use as skills ponytail e code-review-and-quality.
Rode ./gradlew :dominio:test :protocolos:test :app:compileDebugKotlin :app:detekt.
Se falhar por causa de GravacaoViagem.kt, HeliosViewModel.kt, Ajustes.kt, TelaAjustes.kt, TelaDirecao.kt ou MainActivity.kt,
corrija com a menor mudança possível sem alterar comportamento. Depois rode graphify update . e anote o resultado no sessoes.md.
```

## 2. Testes da GravacaoViagem (skill test-driven-development, agente test-engineer)
```
Escreva testes JVM para GravacaoViagem com dublês de RepositorioViagens e GravadorFotos e um CoroutineScope
de teste (StandardTestDispatcher). Casos: abre sessão na primeira leitura; não registra com viagem encerrada;
finalizar devolve o id e grava a janela parcial; retenção apaga arquivo antes do registro. Se o Bitmap impedir,
use Robolectric só nesse teste. Ponytail: nenhuma abstração nova além do necessário para testar.
```

## 3. Próximas personalizações sugeridas (escolha antes de rodar)
```
Implemente só a opção que eu marcar, com a skill ponytail:
[ ] Volume mínimo do alarme: subir o volume do canal de alarme até um piso configurável ao iniciar a viagem.
[ ] Sensibilidade (suave / padrão / alta) mapeada para ConfiguracaoDeteccao, registrada no CSV da viagem para o TCC.
[ ] Cor de destaque no estado ATENTO escolhida pelo motorista (sem mexer em FADIGA e CRÍTICO).
[ ] Tela de direção espelhada (texto à direita) para centrais montadas à esquerda.
```
