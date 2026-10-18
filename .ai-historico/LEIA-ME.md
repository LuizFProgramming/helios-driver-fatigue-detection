# .ai-historico: memória de trabalho do HELIOS (app Kotlin)

Pasta separada do código. Serve para a próxima sessão começar daqui sem reler o repositório inteiro.

## Ferramentas obrigatórias (ordem do Alexus, 18/10/2026)

- **Ponytail** (`E:\ai-tools\ponytail`): toda mudança de código segue a regra da menor alteração completa. Auditorias com `ponytail-audit`.
- **Graphify** (`E:\ai-tools\graphify`): grafo do código em `graphify/`. Consultar o grafo antes de abrir arquivos.
- **agent-skills** (`E:\ai-tools\agent-skills`), em especial a fase VERIFY (`test-driven-development`, `code-review-and-quality`): nada é dado como pronto sem `./gradlew :dominio:test :protocolos:test` verde.

## Como usar o grafo

```
pip install graphifyy            # uma vez
cd Aplicativo-Android/helios-android-v2
graphify update .                # refaz o grafo depois de mudar código (sem LLM, local)
graphify explain "MonitorMotorista" --graph ../../.ai-historico/graphify/graph.json
graphify path "HeliosViewModel" "Classificador" --graph ../../.ai-historico/graphify/graph.json
```

`graphify/graph.html` abre no navegador e mostra o mapa interativo.

## Arquivos

| Arquivo | Conteúdo |
|---------|----------|
| `graphify/GRAPH_REPORT.md` | Resumo do grafo: 1061 nós, 2757 arestas, 42 comunidades, sem ciclos de import |
| `graphify/graph.json`, `graph.html` | Grafo completo |
| `auditoria-ponytail-2026-10-18.md` | Achados e o que foi feito |
| `sessoes.md` | Diário das sessões (anexar no fim a cada sessão) |
| `app-src.tgz` | Cópia do código usada para gerar o grafo em 18/10. Pode apagar |
