# 03. Problemas encontrados na análise

Nada foi alterado no código. As cópias em `03_Codigo` são idênticas aos originais (sem `__pycache__` e sem `.venv`). Esta lista é para decidir o que corrigir antes de portar para o app.

Gravidade: **Alta** muda o resultado da detecção ou quebra o sistema. **Média** gera dado errado ou viola as metas de qualidade. **Baixa** é organização ou documentação.

## Protótipo desktop

| # | Gravidade | Onde | Problema | Sugestão |
|---|-----------|------|----------|----------|
| P1 | Alta | `config.py` vs `README.md` vs `ear_calculator.py` | Valores divergentes do MAR. README diz limiar 0,55 e 5 frames, boca fechada 0,20 a 0,35. O `config.py` usa 0,50 e 15 frames, com lábio interno (fechada 0,05 a 0,20). A docstring de `calcular_mar` ainda fala em > 0,55 | Atualizar README e docstring para os valores reais do `config.py`. No TCC, citar só os valores do código |
| P2 | Alta | Toda contagem por frames | PERCLOS (150 frames), sonolência (30 frames) e bocejo (15 frames) assumem 30 fps. Com MediaPipe em CPU ou com ESP32-CAM em VGA (10 a 15 fps), "1 segundo" vira 2 a 3 segundos e o alerta atrasa | Trocar contagens de frames por tempo (timestamps). Ex.: sonolência = olho fechado por ≥ 1,0 s; PERCLOS = janela de 5 s |
| P3 | Alta | `EMA_ALPHA = 0.25` + `FRAMES_MINIMOS_PARA_CONTAR_PISCADA = 2` | A suavização "engole" piscadas rápidas. Simulação: piscada de 2 frames (EAR 0,30→0,08) nunca passa abaixo de 0,19 depois da EMA; piscada de 3 frames fica só 1 frame abaixo e também não conta. A contagem de piscadas sai menor do que a real | Usar o EAR bruto (ou alpha maior, ~0,6) para piscadas e deixar a EMA só para exibição e PERCLOS. Validar com vídeo gravado |
| P4 | Média | `drowsiness_monitor._determinar_estado` | Quando o alerta vem do PERCLOS (e não de 30 frames fechados), `_inicio_sonolencia` é `None` e o contador "SONOLENCIA DETECTADA Xs" fica sempre em 0 s | Registrar o início do alerta também quando o gatilho é o PERCLOS |
| P5 | Média | `opencv_hud._monitor_contornos` e barra de EAR | Contorno vermelho e barra usam só o EAR, mas o estado "olho fechado" usa EAR **ou** ECF. Pode aparecer PISCANDO com contorno ciano | Expor `olho_fechado` na `LeituraMonitoramento` e usar o mesmo critério na tela |
| P6 | Média (código morto) | `BlendShapeOlhos.abertura_mandibula` | `jawOpen` é extraído e nunca usado | Usar como segundo sinal de bocejo (igual ao ECF para os olhos) ou remover |
| P7 | Média (código morto) | `config.COR_MAPA_TODOS_LANDMARKS` | Constante definida e nunca importada | Remover |
| P8 | Média (código morto) | `_determinar_estado(self, ear)` | Parâmetro `ear` não é usado | Remover o parâmetro |
| P9 | Média | `_mapa_mesh_estrutural` | Importa `mediapipe.python.solutions` (API legada) dentro de `try/except Exception: return`. Nas versões novas do MediaPipe essa API pode não existir e o wireframe some sem aviso | Copiar a lista de conexões para o projeto ou registrar o erro |
| P10 | Média (tipagem) | `types.py`, `ear_calculator.py` | `landmarks_todos: tuple`, `como_sequencia() -> list`, `ponto_a: tuple` sem tipo interno. Equivale a `any` implícito | `tuple[Ponto3D, ...]`, `list[Ponto2D]` |
| P11 | Média (tipagem) | `_suavizar_escalar` | Usa `getattr/setattr` com nome montado em string (`f'_{chave}_ema'`). Ferramentas de tipo não conseguem checar | Classe pequena `SuavizadorEMA` com um campo por instância |
| P12 | Baixa | `_historico_perclos` | `list.pop(0)` é O(n) a cada frame | `collections.deque(maxlen=...)` |
| P13 | Baixa | Domínio importa `config` | O README diz "zero dependências externas", mas o domínio lê constantes globais de `config.py`. Funciona, mas dificulta testar com outros limiares | Passar uma `ConfiguracaoDeteccao` (dataclass) no construtor do monitor |
| P14 | Baixa | Comentário em `mediapipe_face_detector.py` | Mapeamento `eyeBlinkLeft` = "olho direito do motorista" deve ser conferido; na convenção ARKit, `Left` é o olho esquerdo da pessoa. Como só a média é usada, não afeta o resultado | Corrigir o comentário depois de testar piscando um olho só |
| P15 | Baixa | `helios_main.py` | `print(f"Sessao encerrada.")` f-string sem variável | Tirar o `f` |
| P16 | Média | Projeto inteiro | Não há nenhum teste. Meta pede 100% de cobertura e 0 mutantes | Ver `06_Metas_de_Qualidade.md` |

## Backend FastAPI e simulador

| # | Gravidade | Onde | Problema | Sugestão |
|---|-----------|------|----------|----------|
| B1 | Alta (segurança, sinalizado e **não corrigido**) | `POST /frames` | `device_id` entra direto no nome do arquivo. Um valor como `../../algo` grava fora de `uploads/` (path traversal). `file.filename` também é usado sem checar se é `None` | Validar `device_id` com regex (`^[A-Za-z0-9_-]{1,32}$`). Só aplicar se você aprovar |
| B2 | Média | `frames = []` | Lista em memória: reiniciar a API perde o histórico, e o mesmo `frame_id` gera duas entradas que apontam para o mesmo arquivo | Banco simples (SQLite) ou deixar explícito que é só protótipo |
| B3 | Média | `GET /frames/{id}` | Sempre responde `image/jpeg`, mesmo se o arquivo salvo for PNG | Usar `mimetypes` |
| B4 | Média | Arquitetura | A API recebe imagem a cada 2 s, mas não analisa nada. Para detectar piscada é preciso 10+ fps; enviar frames por HTTP POST para um servidor não atende o tempo real | No plano novo, a análise vai para o app (ver arquivo 04). A API pode virar histórico de eventos, não de frames |
| B5 | Baixa | `tests/` | Pasta vazia | |
| B6 | Baixa | `simulador.py` | Pasta `imagens/` vazia, então o simulador termina com "Nenhuma imagem JPG encontrada" | Colocar algumas fotos de teste |

## Resumo

O protótipo desktop está bem organizado e a arquitetura hexagonal facilita muito a migração. Os pontos que mais mexem no resultado do TCC são **P2 (fps)** e **P3 (EMA engolindo piscadas)**, porque afetam diretamente as métricas que a banca vai olhar. P1 é o mais fácil de resolver e o mais visível para quem ler o README.
