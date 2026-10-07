"""
Testes do domínio do protótipo: direção do rosto e causa do alerta.

Para executar, na raiz do projeto:
  .venv\\Scripts\\python.exe -m pytest tests -v
"""

import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "prototipo_desktop_python"))

from domain import head_pose  # noqa: E402
from domain.drowsiness_monitor import MonitorDeSonolencia  # noqa: E402
from domain.head_pose import angulos_da_matriz, direcao_do_rosto  # noqa: E402
from domain.types import (  # noqa: E402
    CAUSA_OLHOS_FECHADOS, CAUSA_PERCLOS, AmostraFacial, BlendShapeOlhos,
    EstadoMotorista, LandmarksBoca, LandmarksOlho,
)


# ── Direção do rosto ──────────────────────────────────────────────────────────

def test_rosto_de_frente():
    assert direcao_do_rosto(5.0, -8.0) == "frente"


def test_rosto_para_os_lados():
    assert direcao_do_rosto(30.0, 0.0) == "esquerda"
    assert direcao_do_rosto(-30.0, 0.0) == "direita"


def test_rosto_para_cima_e_para_baixo():
    assert direcao_do_rosto(0.0, 25.0) == "cima"
    assert direcao_do_rosto(0.0, -25.0) == "baixo"


def test_diagonais():
    assert direcao_do_rosto(30.0, 25.0) == "cima_esquerda"
    assert direcao_do_rosto(-30.0, -25.0) == "baixo_direita"


def test_sem_angulos_e_sem_rosto():
    assert direcao_do_rosto(None, None) == "sem_rosto"


def test_inverter_esquerda_direita(monkeypatch):
    monkeypatch.setattr(head_pose, "INVERTER_ESQUERDA_DIREITA", True)
    assert direcao_do_rosto(30.0, 0.0) == "direita"


def test_matriz_sem_giro_da_zero_grau():
    identidade = [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
    yaw, pitch = angulos_da_matriz(identidade)
    assert abs(yaw) < 1e-9 and abs(pitch) < 1e-9


def test_matriz_com_giro_para_o_lado():
    a = math.radians(30)
    giro_y = [[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]]
    yaw, pitch = angulos_da_matriz(giro_y)
    assert round(yaw, 6) == 30.0 and round(pitch, 6) == 0.0


def test_matriz_com_inclinacao():
    a = math.radians(25)
    giro_x = [[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]]
    yaw, pitch = angulos_da_matriz(giro_x)
    assert round(yaw, 6) == 0.0 and round(pitch, 6) == 25.0


# ── Causa do alerta ───────────────────────────────────────────────────────────

def _olho(abertura: float) -> LandmarksOlho:
    # Olho de 10 px de largura; EAR = abertura / 10.
    return LandmarksOlho(p1=(0, 0), p2=(3, -abertura / 2), p3=(7, -abertura / 2),
                         p4=(10, 0), p5=(7, abertura / 2), p6=(3, abertura / 2))


def _amostra(fechado: bool) -> AmostraFacial:
    olho = _olho(0.1 if fechado else 3.0)
    piscada = 0.9 if fechado else 0.0
    boca = LandmarksBoca(p1=(0, 0), p2=(3, -0.5), p3=(7, -0.5), p4=(10, 0), p5=(7, 0.5), p6=(3, 0.5))
    return AmostraFacial(
        olho_direito=olho, olho_esquerdo=olho, boca=boca, landmarks_todos=(),
        blend_shapes=BlendShapeOlhos(piscada, piscada, 0.0),
        iris_direita=(), iris_esquerda=(), yaw_graus=12.0, pitch_graus=-3.0,
    )


def test_olhos_abertos_nao_tem_causa():
    leitura = MonitorDeSonolencia().processar(_amostra(fechado=False))
    assert leitura.estado == EstadoMotorista.ATENTO
    assert leitura.causa_alerta is None


def test_olhos_fechados_sem_parar_tem_causa_olhos_fechados():
    monitor = MonitorDeSonolencia()
    for _ in range(40):
        leitura = monitor.processar(_amostra(fechado=True))
    assert leitura.estado == EstadoMotorista.SONOLENTO
    assert leitura.causa_alerta == CAUSA_OLHOS_FECHADOS


def test_muitas_piscadas_longas_tem_causa_perclos():
    # 20 frames fechados e 10 abertos, repetido: nunca 30 fechados seguidos,
    # mas o olho fica fechado mais de 40% do tempo.
    monitor = MonitorDeSonolencia()
    causas = set()
    for _ in range(5):
        for fechado in [True] * 20 + [False] * 10:
            leitura = monitor.processar(_amostra(fechado))
            if leitura.estado == EstadoMotorista.SONOLENTO:
                causas.add(leitura.causa_alerta)
    assert causas == {CAUSA_PERCLOS}


def test_angulos_da_amostra_chegam_na_leitura():
    leitura = MonitorDeSonolencia().processar(_amostra(fechado=False))
    assert (leitura.yaw_graus, leitura.pitch_graus) == (12.0, -3.0)
