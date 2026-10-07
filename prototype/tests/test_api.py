"""
Testes das regras da API (backend/main.py): nome das fotos de alerta e horário.

Para executar, na raiz do projeto:
  .venv\\Scripts\\python.exe -m pytest tests -v
"""

import sys
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "backend"))

from main import FORMATO_ARQUIVO_DE_EVENTO, interpretar_horario, nome_do_evento  # noqa: E402

HORA = datetime(2026, 10, 6, 22, 13, 33, tzinfo=timezone.utc)


def test_nome_no_formato_direcao_tipo_data(tmp_path):
    assert nome_do_evento("frente", "DORMINDO", HORA, tmp_path) == "frente-DORMINDO-2026-10-06-22-13-33"


def test_sonolencia_aparece_com_acento(tmp_path):
    assert nome_do_evento("baixo_esquerda", "SONOLENCIA", HORA, tmp_path) == \
        "baixo_esquerda-SONOLÊNCIA-2026-10-06-22-13-33"


def test_desatento_sem_rosto(tmp_path):
    assert nome_do_evento("sem_rosto", "DESATENTO", HORA, tmp_path) == "sem_rosto-DESATENTO-2026-10-06-22-13-33"


def test_duas_fotos_no_mesmo_segundo_nao_se_sobrescrevem(tmp_path):
    (tmp_path / "frente-DORMINDO-2026-10-06-22-13-33.jpg").write_bytes(b"x")
    assert nome_do_evento("frente", "DORMINDO", HORA, tmp_path) == "frente-DORMINDO-2026-10-06-22-13-33-2"


def test_nome_gerado_passa_na_validacao_de_download(tmp_path):
    nome = nome_do_evento("cima_direita", "SONOLENCIA", HORA, tmp_path) + ".jpg"
    assert FORMATO_ARQUIVO_DE_EVENTO.match(nome)


def test_validacao_bloqueia_caminhos():
    assert not FORMATO_ARQUIVO_DE_EVENTO.match("../main.py")
    assert not FORMATO_ARQUIVO_DE_EVENTO.match("a/b.jpg")


def test_horario_da_camera_e_usado():
    assert interpretar_horario("2026-10-06T22:13:33+00:00") == HORA


def test_horario_ausente_ou_errado_usa_a_hora_atual():
    for texto in (None, "ontem"):
        assert abs((interpretar_horario(texto) - datetime.now().astimezone()).total_seconds()) < 5
