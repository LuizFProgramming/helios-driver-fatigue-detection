"""
adapters/mediapipe_face_detector.py — Adapter para MediaPipe FaceLandmarker

Extrai olhos (EAR), boca (MAR), iris (468-477), blend shapes (ECF)
e todos os 478 landmarks do rosto, convertendo para tipos de dominio.
"""

import time
from typing import Optional

import cv2
import mediapipe as mp
import numpy as np

from ports.face_detector_port import PortaDetectorFacial
from domain.types import AmostraFacial, LandmarksOlho, LandmarksBoca, BlendShapeOlhos
from domain.head_pose import angulos_da_matriz
from config import (
    CAMINHO_MODELO_MEDIAPIPE,
    LARGURA_MAXIMA_PARA_DETECCAO,
    NUMERO_MAXIMO_DE_ROSTOS,
    HABILITAR_BLEND_SHAPES,
    LANDMARKS_OLHO_DIREITO_NA_IMAGEM,
    LANDMARKS_OLHO_ESQUERDO_NA_IMAGEM,
    LANDMARKS_BOCA_PARA_MAR,
    INDICES_IRIS_OLHO_DIREITO_CAMERA,
    INDICES_IRIS_OLHO_ESQUERDO_CAMERA,
)

# Nomes dos blend shapes relevantes na nomenclatura ARKit/MediaPipe
_BLEND_PISCADA_DIREITA  = "eyeBlinkLeft"    # lado esq. da camera = olho dir. do motorista
_BLEND_PISCADA_ESQUERDA = "eyeBlinkRight"   # lado dir. da camera = olho esq. do motorista
_BLEND_MANDIBULA        = "jawOpen"


class DetectorFacialMediaPipe(PortaDetectorFacial):
    """
    Adapter que usa MediaPipe FaceLandmarker (478 landmarks) para extrair:
      - Landmarks dos olhos (EAR), boca (MAR), iris (visualizacao)
      - Blend shapes neurais: eyeBlinkLeft/Right, jawOpen (ECF)
      - Todos os 478 landmarks com coordenada z para mapa 3D
    """

    def __init__(self) -> None:
        BaseOptions           = mp.tasks.BaseOptions
        FaceLandmarker        = mp.tasks.vision.FaceLandmarker
        FaceLandmarkerOptions = mp.tasks.vision.FaceLandmarkerOptions
        RunningMode           = mp.tasks.vision.RunningMode

        opcoes = FaceLandmarkerOptions(
            base_options               = BaseOptions(model_asset_path=str(CAMINHO_MODELO_MEDIAPIPE)),
            running_mode               = RunningMode.VIDEO,
            num_faces                  = NUMERO_MAXIMO_DE_ROSTOS,
            output_face_blendshapes    = HABILITAR_BLEND_SHAPES,
            # Matriz da pose da cabeca: dela saem os angulos da direcao do rosto.
            output_facial_transformation_matrixes = True,
        )
        self._detector         = FaceLandmarker.create_from_options(opcoes)
        self._ultimo_timestamp = 0

    # ── Interface ──────────────────────────────────────────────────────────────

    def detectar(self, frame: np.ndarray) -> Optional[AmostraFacial]:
        resultado = self._executar_inferencia(frame)

        if not resultado.face_landmarks:
            return None

        landmarks = resultado.face_landmarks[0]
        h, w      = frame.shape[:2]

        blend_shapes = self._extrair_blend_shapes(resultado)
        iris_d, iris_e = self._extrair_iris(landmarks, w, h)
        yaw, pitch = self._extrair_pose(resultado)

        return AmostraFacial(
            olho_direito  = self._extrair_olho(landmarks, LANDMARKS_OLHO_DIREITO_NA_IMAGEM,  w, h),
            olho_esquerdo = self._extrair_olho(landmarks, LANDMARKS_OLHO_ESQUERDO_NA_IMAGEM, w, h),
            boca          = self._extrair_boca(landmarks, w, h),
            landmarks_todos = self._extrair_todos(landmarks, w, h),
            blend_shapes  = blend_shapes,
            iris_direita  = iris_d,
            iris_esquerda = iris_e,
            yaw_graus     = yaw,
            pitch_graus   = pitch,
        )

    def liberar(self) -> None:
        self._detector.close()

    # ── Inferencia ─────────────────────────────────────────────────────────────

    def _executar_inferencia(self, frame: np.ndarray):
        ts  = self._proximo_timestamp_ms()
        # Os pontos voltam normalizados (0 a 1), entao reduzir aqui nao muda
        # as coordenadas: elas sao multiplicadas pelo tamanho ORIGINAL em detectar().
        largura = frame.shape[1]
        if largura > LARGURA_MAXIMA_PARA_DETECCAO:
            fator = LARGURA_MAXIMA_PARA_DETECCAO / largura
            frame = cv2.resize(frame, None, fx=fator, fy=fator, interpolation=cv2.INTER_AREA)
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        img = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
        return self._detector.detect_for_video(img, ts)

    def _proximo_timestamp_ms(self) -> int:
        ts = int(time.monotonic() * 1000)
        if ts <= self._ultimo_timestamp:
            ts = self._ultimo_timestamp + 1
        self._ultimo_timestamp = ts
        return ts

    # ── Extracao de landmarks ──────────────────────────────────────────────────

    @staticmethod
    def _extrair_olho(lm, indices, w, h) -> LandmarksOlho:
        pts = [(lm[i].x * w, lm[i].y * h) for i in indices]
        return LandmarksOlho(p1=pts[0], p2=pts[1], p3=pts[2],
                             p4=pts[3], p5=pts[4], p6=pts[5])

    @staticmethod
    def _extrair_boca(lm, w, h) -> LandmarksBoca:
        pts = [(lm[i].x * w, lm[i].y * h) for i in LANDMARKS_BOCA_PARA_MAR]
        return LandmarksBoca(p1=pts[0], p2=pts[1], p3=pts[2],
                             p4=pts[3], p5=pts[4], p6=pts[5])

    @staticmethod
    def _extrair_todos(lm, w, h) -> tuple:
        return tuple((l.x * w, l.y * h, l.z) for l in lm)

    @staticmethod
    def _extrair_iris(lm, w, h):
        def iris_pts(indices):
            return tuple((lm[i].x * w, lm[i].y * h, lm[i].z) for i in indices)
        return (
            iris_pts(INDICES_IRIS_OLHO_DIREITO_CAMERA),
            iris_pts(INDICES_IRIS_OLHO_ESQUERDO_CAMERA),
        )

    @staticmethod
    def _extrair_pose(resultado):
        """(yaw, pitch) em graus, ou (None, None) se o modelo nao devolveu a matriz."""
        matrizes = getattr(resultado, "facial_transformation_matrixes", None)
        if not matrizes:
            return None, None
        return angulos_da_matriz(matrizes[0])

    @staticmethod
    def _extrair_blend_shapes(resultado) -> BlendShapeOlhos:
        """
        Le os coeficientes de blend shape do resultado MediaPipe.
        Retorna zeros se blend shapes nao estiverem disponiveis.
        """
        if not resultado.face_blendshapes:
            return BlendShapeOlhos(0.0, 0.0, 0.0)

        scores = {c.category_name: c.score for c in resultado.face_blendshapes[0]}
        return BlendShapeOlhos(
            piscada_direito   = scores.get(_BLEND_PISCADA_DIREITA,  0.0),
            piscada_esquerdo  = scores.get(_BLEND_PISCADA_ESQUERDA, 0.0),
            abertura_mandibula = scores.get(_BLEND_MANDIBULA,       0.0),
        )
