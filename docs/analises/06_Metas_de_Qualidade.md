# 06. Metas de qualidade

## 1. As metas

| Métrica | Meta | O que mede |
|---------|------|-----------|
| Complexidade ciclomática | < 22 por função | Quantidade de caminhos (if, for, and/or) |
| Complexidade cognitiva | < 22 por função | Quão difícil é ler: aninhamento pesa mais |
| Dificuldade de Halstead | < 80 | Proporção de operadores/operandos únicos e repetidos |
| Cobertura de testes | 100% | Linhas e ramificações executadas pelos testes |
| CRAP | < 25 | Combina complexidade e falta de cobertura. Com 100% de cobertura, CRAP = complexidade ciclomática |
| Mutantes sobreviventes | 0 | A ferramenta altera o código (ex.: `<` vira `<=`) e os testes precisam falhar. Garante que os testes verificam algo de verdade |
| Código morto | 0 | Funções, variáveis e exports nunca usados |
| Código redundante | 0 | Trechos duplicados |
| `any` / `unknown` | 0 | Tipos que desligam a checagem do TypeScript |

## 2. Medição feita agora no código existente (Python)

Ferramentas: `radon` (complexidade e Halstead) e `vulture` (código morto), rodadas na cópia da pasta `03_Codigo`.

| Métrica | Resultado atual | Situação |
|---------|-----------------|----------|
| Complexidade ciclomática | Máximo 9 (`_mapa_mesh_estrutural`), depois 7 (`renderizar_mapeamento`). Tudo nível A/B | ✅ bem abaixo de 22 |
| Dificuldade de Halstead | Máximo 6,7 (`_monitor_barra_ear`) | ✅ bem abaixo de 80 |
| Cobertura | 0% (não há testes) | ❌ |
| CRAP | Com 0% de cobertura, CRAP = CC² + CC. A função de CC 9 dá 90 | ❌ resolve-se com testes |
| Mutantes | Não medível sem testes | ❌ |
| Código morto | `COR_MAPA_TODOS_LANDMARKS`, `abertura_mandibula`, alias `Ponto3D` só usado em comentário, parâmetro `ear` de `_determinar_estado` | ❌ 4 itens |
| `any`/`unknown` equivalente | `tuple` e `list` sem tipo interno em `types.py` e `ear_calculator.py`; `getattr/setattr` dinâmico na EMA (o vulture acusou `_ear_ema`, `_mar_ema`, `_ecf_ema` como "não usados" justamente por isso) | ❌ |

As rotas do FastAPI aparecem como "não usadas" no vulture, mas é falso positivo: são chamadas pelo framework via decorador.

**Leitura**: o código já é simples (complexidade e Halstead folgados). O que falta é **teste**. Cobertura, CRAP e mutantes se resolvem juntos com uma boa suíte.

## 3. Ferramentas para o app em TypeScript

| Métrica | Ferramenta | Configuração |
|---------|-----------|--------------|
| Complexidade ciclomática | ESLint `complexity` | `["error", 21]` |
| Complexidade cognitiva | `eslint-plugin-sonarjs` | `sonarjs/cognitive-complexity: ["error", 21]` |
| Halstead | `typhonjs-escomplex` ou `ts-complex` em script de CI | Falhar se `difficulty >= 80` |
| Cobertura 100% | Vitest + `@vitest/coverage-v8` | `thresholds: { lines: 100, branches: 100, functions: 100, statements: 100 }` |
| CRAP < 25 | Script que cruza o JSON de cobertura com a complexidade | Com 100% de cobertura, basta CC < 25, que já é garantido pela regra de complexidade |
| Mutantes = 0 | StrykerJS (`@stryker-mutator/vitest-runner`) | `thresholds: { break: 100 }` |
| Código morto | `knip` | Falha em exports, arquivos e dependências não usados |
| Código redundante | `jscpd` | `threshold: 0` |
| `any` / `unknown` | `typescript-eslint` | `no-explicit-any: error`; `no-restricted-syntax` para `TSUnknownKeyword`; `tsconfig` com `strict: true` |

### Cuidado prático com "0 unknown"

Algumas APIs devolvem `unknown` por natureza: `JSON.parse`, `catch (e)`, respostas HTTP. Com a regra de zero `unknown`, a saída é tratar esses pontos **só nos adaptadores** com funções de validação tipadas (type guards que recebem `string` e devolvem o tipo certo) e configurar `useUnknownInCatchVariables` de forma consciente. O domínio fica livre disso naturalmente.

### Onde 100% de cobertura e 0 mutantes são realistas

- **Domínio** (EAR, MAR, PERCLOS, máquina de estados, classificador): sim, totalmente. São funções puras.
- **Adaptadores** (MediaPipe, câmera, canvas, áudio): difícil sem hardware. A forma honesta é testar com dublês (fakes) da porta e deixar os adaptadores finos, só repassando dados. Documentar no TCC que a meta de 100% vale para o núcleo e os adaptadores têm testes de integração manuais.

## 4. Para o Python (se continuar sendo usado para dataset/treino)

| Métrica | Ferramenta |
|---------|-----------|
| CC e Halstead | `radon` / `xenon --max-absolute B` |
| Cognitiva | `flake8-cognitive-complexity` |
| Cobertura | `pytest-cov --cov-fail-under=100 --cov-branch` |
| Mutantes | `mutmut` |
| Código morto | `vulture` |
| Duplicação | `pylint --enable=duplicate-code` ou `jscpd` (funciona com Python também) |
| Tipos (equivalente a `any`) | `mypy --strict` (proíbe `Any` implícito) |
