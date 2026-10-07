"""
domain/ear_calculator.py — Formulas de Aspect Ratio para olhos e boca

Implementacao pura de Soukupova & Cech (2016) para o EAR e da extensao
para o MAR (Mouth Aspect Ratio), usada na deteccao de bocejo.
Sem dependencias externas — apenas matematica da biblioteca padrao.
"""

import math
from .types import LandmarksOlho, LandmarksBoca


# ── Formula base (compartilhada por EAR e MAR) ────────────────────────────────

def _distancia_euclidiana(ponto_a: tuple, ponto_b: tuple) -> float:
    return math.sqrt((ponto_a[0] - ponto_b[0]) ** 2 + (ponto_a[1] - ponto_b[1]) ** 2)


def _razao_aspecto_seis_pontos(pontos: list) -> float:
    """
    Formula generica de Aspect Ratio para qualquer regiao com 6 landmarks.

    Soukupova & Cech (2016):
        AR = (||p2 - p6|| + ||p3 - p5||) / (2 * ||p1 - p4||)

    Onde p1..p6 sao pontos em sentido horario, com p1 e p4 sendo
    os cantos (distancia horizontal) e os demais pares sendo
    as distancias verticais opostas.
    """
    distancia_vertical_a = _distancia_euclidiana(pontos[1], pontos[5])
    distancia_vertical_b = _distancia_euclidiana(pontos[2], pontos[4])
    distancia_horizontal = _distancia_euclidiana(pontos[0], pontos[3])

    denominador = 2.0 * distancia_horizontal
    if denominador < 1e-8:
        return 0.0

    return (distancia_vertical_a + distancia_vertical_b) / denominador


# ── Interface publica ─────────────────────────────────────────────────────────

def calcular_ear(olho: LandmarksOlho) -> float:
    """
    Eye Aspect Ratio — mede a abertura do olho.
    Valores tipicos: ~0.30-0.42 (aberto) | < 0.19 (fechado)
    """
    return _razao_aspecto_seis_pontos(olho.como_sequencia())


def calcular_mar(boca: LandmarksBoca) -> float:
    """
    Mouth Aspect Ratio — mede a abertura da boca.
    Valores tipicos: ~0.20-0.35 (fechada) | > 0.55 (bocejo)
    Fonte: Costa (UFPA 2024) — indicador de fadiga bucal
    """
    return _razao_aspecto_seis_pontos(boca.como_sequencia())
