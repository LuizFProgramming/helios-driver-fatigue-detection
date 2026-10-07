"""
domain/head_pose.py — Direcao do rosto a partir da pose da cabeca

O MediaPipe devolve uma matriz 4x4 que gira e move um rosto 3D padrao
ate a posicao do rosto na camera. Daqui saem dois angulos:
  yaw   — giro para os lados
  pitch — inclinacao para cima ou para baixo

Sem dependencias externas — apenas matematica da biblioteca padrao.
"""

import math
from typing import Optional, Sequence

from config import (
    GRAUS_PARA_SAIR_DA_FRENTE,
    INVERTER_ESQUERDA_DIREITA,
    INVERTER_CIMA_BAIXO,
    DIRECAO_SEM_ROSTO,
)


def angulos_da_matriz(matriz: Sequence[Sequence[float]]) -> tuple[float, float]:
    """
    Converte a parte de rotacao (3x3 do canto) da matriz em (yaw, pitch), em graus.

    Decomposicao R = Rz(roll) * Ry(yaw) * Rx(pitch):
        pitch = atan2(R[2][1], R[2][2])
        yaw   = atan2(-R[2][0], sqrt(R[2][1]^2 + R[2][2]^2))
    """
    r21, r22, r20 = matriz[2][1], matriz[2][2], matriz[2][0]
    pitch = math.degrees(math.atan2(r21, r22))
    yaw   = math.degrees(math.atan2(-r20, math.hypot(r21, r22)))
    return yaw, pitch


def direcao_do_rosto(yaw: Optional[float], pitch: Optional[float]) -> str:
    """
    Nome da direcao: frente, esquerda, direita, cima, baixo ou diagonais
    como cima_esquerda e baixo_direita. Sem angulos (sem rosto) -> DIRECAO_SEM_ROSTO.
    """
    if yaw is None or pitch is None:
        return DIRECAO_SEM_ROSTO

    if INVERTER_ESQUERDA_DIREITA:
        yaw = -yaw
    if INVERTER_CIMA_BAIXO:
        pitch = -pitch

    vertical = ""
    if pitch > GRAUS_PARA_SAIR_DA_FRENTE:
        vertical = "cima"
    elif pitch < -GRAUS_PARA_SAIR_DA_FRENTE:
        vertical = "baixo"

    horizontal = ""
    if yaw > GRAUS_PARA_SAIR_DA_FRENTE:
        horizontal = "esquerda"
    elif yaw < -GRAUS_PARA_SAIR_DA_FRENTE:
        horizontal = "direita"

    partes = [p for p in (vertical, horizontal) if p]
    return "_".join(partes) if partes else "frente"
