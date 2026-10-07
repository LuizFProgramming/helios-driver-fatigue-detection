"""
domain/types.py — Tipos de dominio puros do sistema HELIOS

Este modulo nao depende de nenhuma biblioteca externa (sem OpenCV, sem
MediaPipe, sem NumPy). E a linguagem ubiqua do problema: qualquer
desenvolvedor pode entender o que o sistema monitora so lendo estes tipos.
"""

from dataclasses import dataclass
from enum import Enum
from typing import Optional, Tuple

Ponto2D = Tuple[float, float]
Ponto3D = Tuple[float, float, float]   # (x_pixel, y_pixel, z_profundidade)


@dataclass(frozen=True)
class LandmarksOlho:
    """
    Seis landmarks periorbitais que definem a geometria de um olho.

    Ordem conforme Soukupova & Cech (2016), sentido horario partindo
    do canto lateral:
      p1 — canto lateral  (externo)
      p2 — palpebra superior, lado interno
      p3 — palpebra superior, lado externo
      p4 — canto medial   (interno)
      p5 — palpebra inferior, lado externo
      p6 — palpebra inferior, lado interno
    """
    p1: Ponto2D
    p2: Ponto2D
    p3: Ponto2D
    p4: Ponto2D
    p5: Ponto2D
    p6: Ponto2D

    def como_sequencia(self) -> list:
        return [self.p1, self.p2, self.p3, self.p4, self.p5, self.p6]


@dataclass(frozen=True)
class LandmarksBoca:
    """
    Seis landmarks peribucais para calculo do MAR (Mouth Aspect Ratio).

    Mesma estrutura geometrica do LandmarksOlho, aplicada a boca:
      p1 — canto esquerdo
      p2 — labio superior, lado esquerdo
      p3 — labio superior, lado direito
      p4 — canto direito
      p5 — labio inferior, lado direito
      p6 — labio inferior, lado esquerdo
    """
    p1: Ponto2D
    p2: Ponto2D
    p3: Ponto2D
    p4: Ponto2D
    p5: Ponto2D
    p6: Ponto2D

    def como_sequencia(self) -> list:
        return [self.p1, self.p2, self.p3, self.p4, self.p5, self.p6]


@dataclass(frozen=True)
class BlendShapeOlhos:
    """
    Coeficientes de fechamento ocular calculados diretamente pela rede neural
    do MediaPipe (blend shapes ARKit). Mais robusto que o EAR geometrico para
    rostos com oculos, barba ou morfologia ocular atipica.

      0.0 = olho completamente aberto
      1.0 = olho completamente fechado
    """
    piscada_direito:  float   # eyeBlinkLeft  na nomenclatura MediaPipe (camera)
    piscada_esquerdo: float   # eyeBlinkRight na nomenclatura MediaPipe (camera)
    abertura_mandibula: float # jawOpen (0=fechada, 1=aberta) — sinal de bocejo

    @property
    def media(self) -> float:
        return (self.piscada_direito + self.piscada_esquerdo) / 2.0


@dataclass(frozen=True)
class AmostraFacial:
    """
    Dados faciais extraidos de um unico frame de video.

    Contem os landmarks especificos para EAR/MAR, o conjunto completo
    de todos os 478 landmarks para visualizacao, blend shapes do modelo
    e landmarks da iris para visualizacao precisa dos olhos.
    """
    olho_direito:    LandmarksOlho
    olho_esquerdo:   LandmarksOlho
    boca:            LandmarksBoca
    landmarks_todos: tuple          # 478 tuplas Ponto3D (x_px, y_px, z)
    blend_shapes:    BlendShapeOlhos
    iris_direita:    tuple          # 5 tuplas Ponto3D — iris olho direito (camera)
    iris_esquerda:   tuple          # 5 tuplas Ponto3D — iris olho esquerdo (camera)
    yaw_graus:       Optional[float] = None   # giro para os lados (pose da cabeca)
    pitch_graus:     Optional[float] = None   # inclinacao para cima/baixo


class EstadoMotorista(Enum):
    """Estados primarios do motorista detectados pelo monitor de sonolencia."""
    ATENTO    = "ATENTO"
    PISCANDO  = "PISCANDO"
    SONOLENTO = "SONOLENTO"
    SEM_FACE  = "SEM FACE"


@dataclass
class LeituraMonitoramento:
    """
    Resultado completo de uma iteracao de monitoramento.

    Produzida pelo dominio (MonitorDeSonolencia) e consumida pelos
    adaptadores de saida — janela monitor e janela de mapeamento.
    """
    estado:             EstadoMotorista
    ear_medio:          float          # EAR geometrico suavizado por EMA
    mar:                float          # MAR geometrico suavizado por EMA
    ecf_medio:          float          # Eye Closure Factor (blend shape neural)
    perclos:            float
    total_piscadas:     int
    total_bocejos:      int
    segundos_em_alerta: float
    esta_bocejando:     bool
    olho_direito:       Optional[LandmarksOlho]     = None
    olho_esquerdo:      Optional[LandmarksOlho]     = None
    boca:               Optional[LandmarksBoca]     = None
    landmarks_todos:    Optional[tuple]             = None
    iris_direita:       Optional[tuple]             = None
    iris_esquerda:      Optional[tuple]             = None
    # Qual regra deixou o estado SONOLENTO: CAUSA_OLHOS_FECHADOS ou CAUSA_PERCLOS.
    # None quando o estado nao e SONOLENTO.
    causa_alerta:       Optional[str]               = None
    # Pose da cabeca, em graus. None quando nao ha rosto.
    yaw_graus:          Optional[float]             = None
    pitch_graus:        Optional[float]             = None


# Causas possiveis do estado SONOLENTO (campo causa_alerta)
CAUSA_OLHOS_FECHADOS = "OLHOS_FECHADOS"   # olhos fechados sem parar (microssono)
CAUSA_PERCLOS        = "PERCLOS"          # muito tempo de olho fechado na janela
