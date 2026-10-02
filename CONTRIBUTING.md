# Guia de contribuição

Com 9 pessoas no mesmo repositório, combinar regras simples evita conflito e retrabalho.

## 1. Fluxo de trabalho (branches)

- `main` → sempre estável. **Ninguém commita direto nela.**
- Todo trabalho acontece em uma branch própria, criada a partir da `main`, e entra via **Pull Request**.
- Cada PR precisa de **pelo menos 1 aprovação** de outro membro.

### Nome das branches

```
<tipo>/<descricao-curta-em-minusculas>
```

| Tipo | Uso | Exemplo |
| --- | --- | --- |
| `feat` | nova funcionalidade | `feat/calculo-ear` |
| `fix` | correção de bug | `fix/falso-positivo-piscada` |
| `docs` | documentação | `docs/arquitetura-inicial` |
| `exp` | experimento / notebook | `exp/comparacao-mediapipe-dlib` |
| `chore` | configuração, dependências | `chore/adicionar-requirements` |
| `test` | testes | `test/validacao-perclos` |

## 2. Mensagens de commit

Padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/):

```
<tipo>: <descrição curta no imperativo>
```

Exemplos: `feat: adiciona cálculo do EAR`, `docs: descreve fluxo do sistema`, `fix: corrige limiar de PERCLOS`.

## 3. Pull Requests

1. Atualize sua branch com a `main` antes de abrir o PR.
2. Preencha o template do PR.
3. Vincule a issue (`Closes #12`).
4. Aguarde a revisão; resolva os comentários.
5. Após aprovado, use **Squash and merge** e apague a branch.

## 4. Issues e Scrum

- Toda tarefa vira uma **issue**, com label de **área** e de **sprint**.
- Labels sugeridas:
  - Área: `visao`, `sensores`, `fusao`, `alertas`, `genai`, `firmware`, `hardware`, `prototipo`, `docs`, `dados`
  - Sprint: `R4S1`, `R4S2`, `R4S3`, `R4S4`
  - Tipo: `bug`, `feature`, `pesquisa`, `tarefa`
- Use um **GitHub Project** (Board) com colunas: `Backlog → Sprint → Em andamento → Em revisão → Concluído`.

## 5. Dados e modelos

- **Nunca** commitar datasets, vídeos ou rostos de pessoas (privacidade/LGPD).
- Modelos pesados (`.h5`, `.pt`, `.onnx`, `.tflite`) ficam fora do Git; documente onde baixar em `models/README.md`.
- Se precisar versionar arquivos grandes, avalie Git LFS ou links externos.

## 6. Qualidade de código

- Python 3.10+, seguindo PEP 8.
- Funções e módulos com docstrings.
- Teste o que for possível em `tests/`.
- Parâmetros (limiares de EAR, tempos, etc.) ficam em `src/helios/config/`, nunca "soltos" no código.

## 7. Decisões importantes

Escolheu uma biblioteca, um dataset ou uma arquitetura? Registre em `docs/decisions/` usando o modelo de ADR (`0000-template.md`). Isso também ajuda na escrita do TCC.
