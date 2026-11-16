# Análise dos logs do roteiro de teste

Mede a precisão do HELIOS com referência: o roteiro de teste do app (ADR-015) diz o que a pessoa fazia a cada
momento, e este script cruza isso com o que o app viu.

## Como usar

1. No app: Ajustes › Análise › "Roteiro de teste". Marque se está de óculos e autorize guardar os dados do teste.
2. Depois do roteiro, exporte o histórico em CSV (Histórico › Exportar), para levar o `eventos.csv`.
3. Copie para uma pasta do computador, da Download/HELIOS do celular: `helios-roteiro-viagem-*.csv`,
   `helios-quadros-viagem-*.csv` (mesmo número) e `helios-eventos-*.csv`.
4. Rode, com o ambiente do experimento (`scripts/experimento_olhos`):

```powershell
.venv-experimento\Scripts\python scripts/analise_logs/analisar.py C:\caminho\da\pasta
```

O relatório sai na tela e em `relatorio.md`, na mesma pasta. Com mais de um roteiro na pasta, usa o mais recente.

## O que o relatório mostra

| Seção | Medida |
|---|---|
| Olho aberto ou fechado | Acurácia, precisão, recall, F1 e falso fechado (olho aberto chamado de fechado) da regra do app e da CNN, com e sem óculos. Os primeiros 700 ms de cada tarefa ficam de fora (tempo de reação). |
| Alertas esperados | Se o evento certo saiu (até 2 s depois do fim da tarefa) e quanto tempo o estado de alerta levou para aparecer. |
| Alarmes falsos | Fração dos quadros em ATENÇÃO, CRÍTICO, DISTRAÍDO ou DESATENTO nas tarefas que não deviam alertar. FADIGA não conta. |

O `recortes.csv` não entra no relatório: ele é o material de treino e teste da CNN com óculos (fase 2). Tem imagem
dos olhos; não vai para o git.

## Testes

```powershell
.venv-experimento\Scripts\python -m pytest scripts/analise_logs/tests -q
```
