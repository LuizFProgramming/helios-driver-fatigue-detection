import math
import sys
from pathlib import Path

import numpy as np
import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from comum import (  # noqa: E402
    calcular_metricas,
    dividir_por_pessoa,
    limiar_youden,
    pessoa_do_arquivo,
    razao_aspecto,
)


def olho(abertura: float) -> np.ndarray:
    """Olho de largura 10 com as duas distâncias verticais iguais a `abertura`."""
    return np.array([[0, 0], [3, -abertura / 2], [7, -abertura / 2], [10, 0], [7, abertura / 2], [3, abertura / 2]], float)


def test_ear_de_olho_conhecido():
    assert razao_aspecto(olho(3.0)) == pytest.approx(3.0 / 10.0)


def test_ear_nao_muda_com_escala_e_translacao():
    base = olho(2.5)
    assert razao_aspecto(base * 7.3 + np.array([120, 45])) == pytest.approx(razao_aspecto(base))


def test_ear_de_olho_degenerado_e_zero():
    assert razao_aspecto(np.zeros((6, 2))) == 0.0


def test_metricas_com_matriz_conhecida():
    verdadeiro = np.array([1, 1, 1, 0, 0, 0, 0, 1])
    previsto = np.array([1, 1, 0, 0, 0, 1, 0, 1])
    m = calcular_metricas(verdadeiro, previsto)
    assert (m.vp, m.fn, m.vn, m.fp) == (3, 1, 3, 1)
    assert m.acuracia == pytest.approx(6 / 8)
    assert m.sensibilidade == pytest.approx(3 / 4)
    assert m.especificidade == pytest.approx(3 / 4)
    assert m.precisao == pytest.approx(3 / 4)
    assert m.f1 == pytest.approx(3 / 4)


def test_metricas_sem_positivos_previstos_dao_nan_na_precisao():
    m = calcular_metricas(np.array([0, 1]), np.array([0, 0]))
    assert math.isnan(m.precisao)


def test_metricas_rejeitam_tamanhos_diferentes():
    with pytest.raises(ValueError):
        calcular_metricas(np.array([0, 1]), np.array([0]))


def test_youden_acha_o_corte_do_ear():
    ear = np.array([0.10, 0.12, 0.15, 0.25, 0.28, 0.30])
    fechado = np.array([1, 1, 1, 0, 0, 0])
    limiar = limiar_youden(ear, fechado, maior_e_fechado=False)
    assert 0.15 <= limiar < 0.25


def test_youden_para_pontuacao_crescente():
    prob = np.array([0.1, 0.2, 0.8, 0.9])
    assert limiar_youden(prob, np.array([0, 0, 1, 1]), maior_e_fechado=True) == pytest.approx(0.8)


def test_youden_exige_duas_classes():
    with pytest.raises(ValueError):
        limiar_youden(np.array([0.1, 0.2]), np.array([1, 1]), maior_e_fechado=False)


@pytest.mark.parametrize(
    ("arquivo", "pessoa"),
    [("Aaron_Eckhart_0001.jpg", "Aaron_Eckhart"), ("closed_eye_0123.jpg", "closed_eye"), ("foto.png", "foto")],
)
def test_pessoa_do_arquivo(arquivo, pessoa):
    assert pessoa_do_arquivo(arquivo) == pessoa


def test_divisao_nao_repete_pessoa_entre_particoes():
    pessoas = pd.Series([f"p{i // 3}" for i in range(300)])
    rotulos = pd.Series([(i // 3) % 2 for i in range(300)])
    particao = dividir_por_pessoa(pessoas, rotulos)
    assert particao.groupby(pessoas).nunique().max() == 1
    proporcao = particao.value_counts(normalize=True)
    assert proporcao["teste"] == pytest.approx(0.15, abs=0.03)
    assert proporcao["validacao"] == pytest.approx(0.15, abs=0.03)
    for nome in ("treino", "validacao", "teste"):
        assert rotulos[particao == nome].mean() == pytest.approx(0.5, abs=0.1)


def test_divisao_e_reprodutivel():
    pessoas = pd.Series([f"p{i}" for i in range(50)])
    rotulos = pd.Series([i % 2 for i in range(50)])
    assert dividir_por_pessoa(pessoas, rotulos).equals(dividir_por_pessoa(pessoas, rotulos))
