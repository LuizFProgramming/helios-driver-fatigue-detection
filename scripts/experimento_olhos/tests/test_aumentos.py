import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from aumentos import (  # noqa: E402
    aumentar_lote,
    aumentar_real,
    camera_de_baixo,
    contraluz,
    oculos,
    pouco_contraste,
    ruido_e_desfoque,
)


def olho_desenhado() -> np.ndarray:
    """Fundo claro com uma elipse escura no meio: contraste alto, como um olho aberto nítido."""
    y, x = np.mgrid[0:64, 0:64]
    img = np.full((64, 64), 180, np.uint8)
    img[((x - 32) / 18.0) ** 2 + ((y - 32) / 8.0) ** 2 < 1] = 60
    return img


def test_todo_aumento_devolve_uint8_64x64():
    rng = np.random.default_rng(1)
    for funcao in (pouco_contraste, contraluz, ruido_e_desfoque, camera_de_baixo, oculos, aumentar_real):
        saida = funcao(olho_desenhado(), rng)
        assert saida.shape == (64, 64) and saida.dtype == np.uint8, funcao.__name__


def test_pouco_contraste_nunca_aumenta_o_desvio():
    rng = np.random.default_rng(2)
    base = olho_desenhado()
    assert all(pouco_contraste(base, rng).std() <= base.std() + 1e-6 for _ in range(50))


def test_oculos_desenha_tracos_escuros_fora_do_olho():
    rng = np.random.default_rng(3)
    base = olho_desenhado()
    mudou = [(oculos(base, rng).astype(int) - base).min() for _ in range(20)]
    assert all(m <= -40 for m in mudou)  # a armação escurece ao menos um ponto do fundo claro


def test_camera_de_baixo_achata_o_olho():
    rng = np.random.default_rng(4)
    base = olho_desenhado()
    alturas = [int((camera_de_baixo(base, rng)[:, 32] < 120).sum()) for _ in range(30)]
    assert max(alturas) <= 16 and min(alturas) < 16


def test_lote_com_a_mesma_semente_e_igual_e_aceita_canal():
    lote = np.stack([olho_desenhado()[..., np.newaxis]] * 4)
    a, b = aumentar_lote(lote, 7), aumentar_lote(lote, 7)
    assert a.shape == lote.shape and np.array_equal(a, b)
    assert not np.array_equal(a, aumentar_lote(lote, 8))
    assert aumentar_lote(lote, 7, oculos).shape == lote.shape  # função de um aumento só, com canal
