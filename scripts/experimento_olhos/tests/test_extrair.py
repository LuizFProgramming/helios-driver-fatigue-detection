"""Testa o caminho "rosto encontrado" da extração com um detector falso (sem precisar de foto de rosto)."""
import sys
from pathlib import Path
from types import SimpleNamespace

import numpy as np
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from comum import OLHO_DIREITO_IMAGEM, OLHO_ESQUERDO_IMAGEM, TAMANHO_RECORTE  # noqa: E402
from extrair import LADO_MINIMO, ampliar, extrair_imagem, recortar_olho  # noqa: E402


def malha_com_olhos(abertura_dir: float, abertura_esq: float) -> list[SimpleNamespace]:
    """478 pontos normalizados; os seis de cada olho formam um olho de largura 0,1 e a abertura pedida."""
    pontos = [SimpleNamespace(x=0.5, y=0.5) for _ in range(478)]
    for indices, cx, abertura in ((OLHO_DIREITO_IMAGEM, 0.35, abertura_dir), (OLHO_ESQUERDO_IMAGEM, 0.65, abertura_esq)):
        forma = [(-0.05, 0), (-0.02, -abertura / 2), (0.02, -abertura / 2), (0.05, 0), (0.02, abertura / 2), (-0.02, abertura / 2)]
        for indice, (dx, dy) in zip(indices, forma):
            pontos[indice] = SimpleNamespace(x=cx + dx, y=0.4 + dy)
    return pontos


class DetectorFalso:
    def __init__(self, landmarks, blendshapes):
        self.resultado = SimpleNamespace(face_landmarks=landmarks, face_blendshapes=blendshapes)

    def detect(self, _imagem):
        return self.resultado


MP_FALSO = SimpleNamespace(Image=lambda **_: None, ImageFormat=SimpleNamespace(SRGB=1))


def test_extrai_ear_ecf_e_recortes_quando_ha_rosto():
    blend = [[SimpleNamespace(category_name="eyeBlinkRight", score=0.7), SimpleNamespace(category_name="eyeBlinkLeft", score=0.5)]]
    detector = DetectorFalso([malha_com_olhos(0.03, 0.02)], blend)
    imagem = np.zeros((LADO_MINIMO, LADO_MINIMO, 3), np.uint8)

    medidas, recorte_dir, recorte_esq = extrair_imagem(MP_FALSO, detector, imagem)

    assert medidas["rosto"] == 1
    assert medidas["ear_dir"] == pytest.approx(0.03 / 0.1)
    assert medidas["ear_esq"] == pytest.approx(0.02 / 0.1)
    assert medidas["ear_medio"] == pytest.approx(0.25)
    assert (medidas["ecf_dir"], medidas["ecf_esq"], medidas["ecf_medio"]) == pytest.approx((0.7, 0.5, 0.6))
    assert recorte_dir.shape == recorte_esq.shape == (TAMANHO_RECORTE, TAMANHO_RECORTE)


def test_sem_rosto_nao_gera_recortes():
    medidas, recorte_dir, recorte_esq = extrair_imagem(MP_FALSO, DetectorFalso([], []), np.zeros((300, 300, 3), np.uint8))
    assert medidas["rosto"] == 0
    assert recorte_dir is None and recorte_esq is None


def test_recorte_na_borda_da_imagem_tem_tamanho_certo():
    cinza = np.arange(100 * 100, dtype=np.uint32).reshape(100, 100).astype(np.uint8)
    pontos = np.full((478, 2), 50.0)
    pontos[list(OLHO_DIREITO_IMAGEM)] = [[0, 2], [2, 0], [4, 0], [6, 2], [4, 4], [2, 4]]
    recorte = recortar_olho(cinza, pontos, OLHO_DIREITO_IMAGEM, OLHO_DIREITO_IMAGEM[0], OLHO_DIREITO_IMAGEM[3])
    assert recorte.shape == (TAMANHO_RECORTE, TAMANHO_RECORTE)


def test_imagem_pequena_e_ampliada_e_grande_fica_igual():
    assert min(ampliar(np.zeros((100, 120, 3), np.uint8)).shape[:2]) == LADO_MINIMO
    grande = np.zeros((400, 500, 3), np.uint8)
    assert ampliar(grande) is grande
