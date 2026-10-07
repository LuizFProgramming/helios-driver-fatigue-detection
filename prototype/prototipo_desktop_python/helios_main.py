"""
helios_main.py — Ponto de entrada do sistema HELIOS

Conecta todos os modulos e exibe duas janelas simultaneas:

  HELIOS — Monitor     : vista limpa para uso operacional
  HELIOS — Mapeamento  : vista tecnica com todos os 478 landmarks,
                         regioes coloridas e linhas de medicao EAR/MAR

Para executar:
  python helios_main.py

Para encerrar: pressione 'q' em qualquer janela.
"""

import sys
import cv2

from domain.drowsiness_monitor import MonitorDeSonolencia
from adapters.mediapipe_face_detector import DetectorFacialMediaPipe
from adapters.opencv_hud import renderizar_monitor, renderizar_mapeamento

JANELA_MONITOR     = "HELIOS — Monitor"
JANELA_MAPEAMENTO  = "HELIOS — Mapeamento"
INDICE_DA_WEBCAM   = 0


def main() -> None:
    camera   = _abrir_camera()
    detector = DetectorFacialMediaPipe()
    monitor  = MonitorDeSonolencia()

    cv2.namedWindow(JANELA_MONITOR,    cv2.WINDOW_NORMAL)
    cv2.namedWindow(JANELA_MAPEAMENTO, cv2.WINDOW_NORMAL)

    print("HELIOS iniciado. Pressione 'q' para encerrar.")

    ultima_leitura = None

    try:
        while camera.isOpened():
            sucesso, frame = camera.read()
            if not sucesso:
                print("[AVISO] Nao foi possivel ler o frame da camera.")
                break

            amostra        = detector.detectar(frame)
            ultima_leitura = monitor.processar(amostra)

            cv2.imshow(JANELA_MONITOR,    renderizar_monitor(frame, ultima_leitura))
            cv2.imshow(JANELA_MAPEAMENTO, renderizar_mapeamento(frame, ultima_leitura))

            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

    finally:
        camera.release()
        detector.liberar()
        cv2.destroyAllWindows()

    if ultima_leitura is not None:
        print(f"Sessao encerrada.")
        print(f"  Piscadas detectadas : {ultima_leitura.total_piscadas}")
        print(f"  Bocejos detectados  : {ultima_leitura.total_bocejos}")
        print(f"  PERCLOS final       : {ultima_leitura.perclos * 100:.1f}%")


def _abrir_camera() -> cv2.VideoCapture:
    camera = cv2.VideoCapture(INDICE_DA_WEBCAM)
    if not camera.isOpened():
        print(f"[ERRO] Nao foi possivel acessar a webcam (indice {INDICE_DA_WEBCAM}).")
        sys.exit(1)
    return camera


if __name__ == "__main__":
    main()
