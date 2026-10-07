"""
Testes da tela da câmera (adapters/hud_veicular.py).

O mais importante: os desenhos ficam só na tela. A foto que vai para a API
precisa continuar sendo a imagem crua da câmera.

Para executar, na raiz do projeto:
  .venv\\Scripts\\python.exe -m pytest tests -v
"""

import sys
from pathlib import Path

import numpy as np
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "prototipo_desktop_python"))

from adapters.hud_veicular import renderizar_painel_veicular  # noqa: E402
from domain.types import EstadoMotorista, LeituraMonitoramento  # noqa: E402


def _leitura(estado=EstadoMotorista.SEM_FACE) -> LeituraMonitoramento:
    return LeituraMonitoramento(
        estado=estado, ear_medio=0.0, mar=0.0, ecf_medio=0.0, perclos=0.2,
        total_piscadas=0, total_bocejos=0, segundos_em_alerta=0.0, esta_bocejando=False,
    )


@pytest.mark.parametrize("categoria", ["ATENTO", "SEM_ROSTO", "DESATENTO", "SONOLENCIA", "DORMINDO"])
def test_nao_altera_a_foto_original(categoria):
    frame = np.full((480, 640, 3), 90, np.uint8)
    copia = frame.copy()
    tela = renderizar_painel_veicular(frame, _leitura(), categoria, "sem_rosto", fps=20.0)
    assert np.array_equal(frame, copia)
    assert tela.shape == frame.shape
    assert not np.array_equal(tela, frame)


def test_funciona_sem_fps_e_em_outra_resolucao():
    frame = np.zeros((720, 1280, 3), np.uint8)
    tela = renderizar_painel_veicular(frame, _leitura(), "SEM_ROSTO", "sem_rosto", fps=None)
    assert tela.shape == (720, 1280, 3)
