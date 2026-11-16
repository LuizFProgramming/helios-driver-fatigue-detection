"""Partes puras da comparação de tamanhos: rótulo da MRL, métricas por olho, escolha do modelo e CSV dourado."""
import sys
from pathlib import Path

import numpy as np
import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from comum import (  # noqa: E402
    LIMITE_APP_KB,
    ROTULO_ABERTO,
    ROTULO_FECHADO,
    TAMANHO_RECORTE,
    avaliar_probabilidades,
    escolher_modelo,
    pixels_hex,
    rotulo_mrl,
)
from treinar_cnn import ARQUITETURAS, epocas_efetivas  # noqa: E402


@pytest.mark.parametrize(
    ("nome", "rotulo"),
    [
        ("s0001_00001_0_0_0_0_0_01.png", ROTULO_FECHADO),  # 5º campo 0 = fechado na MRL
        ("s0037_12345_1_1_1_0_1_02.png", ROTULO_ABERTO),  # 5º campo 1 = aberto na MRL
        ("pasta/s0001/s0001_00002_0_1_0_0_0_01.png", ROTULO_FECHADO),
    ],
)
def test_rotulo_da_mrl_vem_do_quinto_campo_e_e_invertido(nome, rotulo):
    assert rotulo_mrl(nome) == rotulo


@pytest.mark.parametrize("nome", ["foto.png", "s0001_00001_0_0_2_0_0_01.png", "s0001_00001_0_0.png"])
def test_nome_fora_do_padrao_da_mrl_e_rejeitado(nome):
    with pytest.raises(ValueError):
        rotulo_mrl(nome)


def test_metricas_por_olho_usam_o_corte_e_trazem_a_auc():
    verdadeiro = np.array([1, 1, 1, 0, 0, 0])
    prob = np.array([0.9, 0.6, 0.4, 0.3, 0.5, 0.1])  # 0,5 conta como fechado
    m = avaliar_probabilidades(verdadeiro, prob)
    assert (m["vp"], m["fn"], m["vn"], m["fp"]) == (2, 1, 2, 1)
    assert m["acuracia"] == pytest.approx(4 / 6)
    assert m["sensibilidade"] == pytest.approx(2 / 3)
    assert m["especificidade"] == pytest.approx(2 / 3)
    assert m["f1"] == pytest.approx(2 / 3)
    assert m["auc"] == pytest.approx(8 / 9)  # 8 dos 9 pares (fechado, aberto) na ordem certa


def test_auc_de_separacao_perfeita_e_um():
    assert avaliar_probabilidades(np.array([0, 0, 1]), np.array([0.1, 0.2, 0.7]))["auc"] == pytest.approx(1.0)


def tabela_de_modelos(linhas):
    return pd.DataFrame(linhas, columns=["arquitetura", "base_treino", "tamanho_kb", "f1_validacao", "f1"])


def test_escolhe_o_maior_f1_na_validacao_dentro_do_limite_sem_olhar_o_teste():
    tabela = tabela_de_modelos(
        [
            ("pequena", "cew", 100.0, 0.80, 0.99),  # melhor no teste, pior na validação
            ("media", "cew", 400.0, 0.90, 0.70),
            ("mobilenet", "cew", 1500.0, 0.95, 0.95),  # acima de 1024 KB
        ]
    )
    escolhido = escolher_modelo(tabela)
    assert (escolhido["arquitetura"], escolhido["base_treino"]) == ("media", "cew")


def test_limite_de_1024_kb_entra_e_mobilenet_100_nunca_e_escolhida():
    tabela = tabela_de_modelos(
        [
            ("mobilenet_100", "cew+mrl", 900.0, 0.99, 0.99),
            ("mobilenet", "cew", float(LIMITE_APP_KB), 0.85, 0.80),
            ("pequena", "cew", 100.0, 0.85, 0.80),
        ]
    )
    escolhido = escolher_modelo(tabela)
    assert escolhido["arquitetura"] == "pequena"  # empate no F1: fica o menor


def test_sem_candidato_dentro_do_limite_e_erro():
    with pytest.raises(ValueError):
        escolher_modelo(tabela_de_modelos([("mobilenet_100", "cew", 2300.0, 0.9, 0.9)]))


def test_pixels_do_csv_dourado_voltam_iguais():
    recorte = np.arange(TAMANHO_RECORTE * TAMANHO_RECORTE, dtype=np.int64).reshape(TAMANHO_RECORTE, TAMANHO_RECORTE) % 256
    texto = pixels_hex(recorte.astype(np.float32))
    assert len(texto) == 2 * TAMANHO_RECORTE * TAMANHO_RECORTE
    volta = np.frombuffer(bytes.fromhex(texto), dtype=np.uint8).reshape(TAMANHO_RECORTE, TAMANHO_RECORTE)
    assert np.array_equal(volta, recorte)


def test_pixels_de_recorte_com_tamanho_errado_sao_rejeitados():
    with pytest.raises(ValueError):
        pixels_hex(np.zeros((32, 32)))


def test_as_quatro_arquiteturas():
    assert ARQUITETURAS == ("pequena", "media", "mobilenet", "mobilenet_100")


@pytest.mark.parametrize(
    ("arquitetura", "com_mrl", "pedidas", "efetivas"),
    [
        ("mobilenet_100", True, 30, 10),
        ("mobilenet_100", True, 5, 5),
        ("mobilenet_100", False, 30, 30),
        ("mobilenet", True, 30, 30),
    ],
)
def test_mobilenet_100_com_mrl_treina_no_maximo_10_epocas(arquitetura, com_mrl, pedidas, efetivas):
    assert epocas_efetivas(arquitetura, com_mrl, pedidas) == efetivas
