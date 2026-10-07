"""
ports/face_detector_port.py — Contrato para detectores faciais

Define a interface (porta de entrada) que qualquer detector facial
deve implementar para ser usado pelo sistema HELIOS.

Ao depender desta interface em vez de uma implementacao concreta,
o dominio permanece isolado de frameworks como MediaPipe ou dlib.
Trocar o detector nao requer tocar em nenhuma logica de negocio.
"""

from abc import ABC, abstractmethod
from typing import Optional

import numpy as np

from domain.types import AmostraFacial


class PortaDetectorFacial(ABC):
    """
    Interface abstrata para detectores faciais.

    Implementacoes concretas (ex: MediaPipe, dlib, OpenCV Haar) devem
    herdar desta classe e implementar os dois metodos abaixo.
    """

    @abstractmethod
    def detectar(self, frame: np.ndarray) -> Optional[AmostraFacial]:
        """
        Recebe um frame de video no formato BGR (padrao OpenCV) e retorna
        os landmarks dos olhos do primeiro rosto encontrado, ou None
        se nenhum rosto for detectado.
        """
        ...

    @abstractmethod
    def liberar(self) -> None:
        """Libera recursos alocados pelo detector (modelos, conexoes, etc.)."""
        ...
