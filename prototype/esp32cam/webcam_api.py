"""
webcam_api.py — Simulador da câmera do HELIOS com detecção real

Lê a webcam do notebook (ou o celular pelo DroidCam), analisa TODOS os
frames com o detector do protótipo (MediaPipe Face Landmarker + regras de
EAR, ECF, MAR e PERCLOS), classifica o motorista numa categoria e envia
o resultado para a API:
  - a cada 2 segundos, medidos pelo relógio (sem time.sleep);
  - na hora em que entra numa categoria de alerta (DESATENTO, SONOLENCIA
    ou DORMINDO). A API guarda essas imagens em pastas separadas.

Por que analisar todos os frames? Uma piscada dura de 0,1 a 0,4 s.
Se a análise fosse só nas fotos enviadas (1 a cada 2 s), as piscadas
e o olho fechado passariam despercebidos.

Para executar (com a API já rodando):
  python esp32cam/webcam_api.py

Para encerrar: pressione 'q' na janela da câmera.
"""

import sys
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Optional

import cv2
import requests

# O protótipo faz "from config import ...", então a pasta dele precisa
# estar no caminho de busca do Python antes de importar qualquer módulo dele.
PASTA_PROTOTIPO = Path(__file__).resolve().parent.parent / "prototipo_desktop_python"
sys.path.insert(0, str(PASTA_PROTOTIPO))

from config import FPS_ALVO, SEGUNDOS_SEM_ROSTO_PARA_DESATENTO  # noqa: E402
from domain.head_pose import direcao_do_rosto  # noqa: E402
from domain.types import (  # noqa: E402
    CAUSA_OLHOS_FECHADOS,
    EstadoMotorista,
    LeituraMonitoramento,
)

API_URL = "http://127.0.0.1:8000/frames"
DEVICE_ID = "WEBCAM_NOTEBOOK"
# Número da webcam (0, 1...) ou endereço do DroidCam no celular.
# O DroidCam aceita só uma conexão: feche o navegador e o DroidCam Client antes.
CAMERA_INDEX = "http://192.168.1.15:4747/video"
INTERVALO_DE_ENVIO_EM_SEGUNDOS = 2.0
TEMPO_LIMITE_DA_API_EM_SEGUNDOS = 2.0
JANELA = "HELIOS — Câmera"

# ── Categorias enviadas para a API ────────────────────────────────────────────
# Os mesmos nomes estão em backend/main.py (CATEGORIAS).
ATENTO = "ATENTO"
PISCANDO = "PISCANDO"
SEM_ROSTO = "SEM_ROSTO"        # rosto sumiu há menos de 2 s
DESATENTO = "DESATENTO"        # rosto fora da câmera por 2 s ou mais
SONOLENCIA = "SONOLENCIA"      # PERCLOS alto ou bocejo
DORMINDO = "DORMINDO"          # olhos fechados sem parar (microssono)

CATEGORIAS_DE_ALERTA = {DESATENTO, SONOLENCIA, DORMINDO}

# Texto mostrado no app. "Sonolência Detectada" precisa ser exatamente este,
# porque o app Expo compara o texto para decidir se vibra. Por isso
# SONOLENCIA e DORMINDO usam o mesmo texto; a diferença vai no campo "categoria".
TEXTO_POR_CATEGORIA = {
    ATENTO: "Atento",
    PISCANDO: "Piscando",
    SEM_ROSTO: "Sem rosto",
    DESATENTO: "Desatento",
    SONOLENCIA: "Sonolência Detectada",
    DORMINDO: "Sonolência Detectada",
}


class ClassificadorDeAlerta:
    """
    Transforma cada leitura do detector numa categoria.

    Guarda desde quando o rosto sumiu, porque DESATENTO depende do tempo:
    o rosto sair da câmera por um instante (virar para o retrovisor) é normal.
    """

    def __init__(self) -> None:
        self._inicio_sem_rosto: Optional[float] = None

    def classificar(self, leitura: LeituraMonitoramento, agora: float) -> str:
        if leitura.estado == EstadoMotorista.SEM_FACE:
            if self._inicio_sem_rosto is None:
                self._inicio_sem_rosto = agora
            if agora - self._inicio_sem_rosto >= SEGUNDOS_SEM_ROSTO_PARA_DESATENTO:
                return DESATENTO
            return SEM_ROSTO

        self._inicio_sem_rosto = None
        if leitura.estado == EstadoMotorista.SONOLENTO:
            if leitura.causa_alerta == CAUSA_OLHOS_FECHADOS:
                return DORMINDO
            return SONOLENCIA
        if leitura.esta_bocejando:
            return SONOLENCIA
        if leitura.estado == EstadoMotorista.PISCANDO:
            return PISCANDO
        return ATENTO


def deve_enviar(
    agora: float,
    ultimo_envio: Optional[float],
    categoria: str,
    categoria_anterior: Optional[str],
) -> bool:
    """Envia no primeiro frame, a cada 2 s, ou assim que entra numa categoria de alerta."""
    if ultimo_envio is None:
        return True
    entrou_em_alerta = categoria in CATEGORIAS_DE_ALERTA and categoria != categoria_anterior
    passou_o_intervalo = agora - ultimo_envio >= INTERVALO_DE_ENVIO_EM_SEGUNDOS
    return entrou_em_alerta or passou_o_intervalo


def montar_dados(
    frame_id: int, leitura: LeituraMonitoramento, categoria: str, capturado_em: datetime
) -> dict:
    """Campos do formulário enviado em POST /frames (todos como texto)."""
    return {
        "frame_id": str(frame_id),
        "device_id": DEVICE_ID,
        "categoria": categoria,
        "status_motorista": TEXTO_POR_CATEGORIA[categoria],
        "direcao_rosto": direcao_do_rosto(leitura.yaw_graus, leitura.pitch_graus),
        # Horário em que a foto foi tirada (a API usa no nome do arquivo).
        "capturado_em": capturado_em.isoformat(timespec="seconds"),
        "ear": f"{leitura.ear_medio:.4f}",
        "ecf": f"{leitura.ecf_medio:.4f}",
        "mar": f"{leitura.mar:.4f}",
        # O protótipo calcula o PERCLOS de 0 a 1; o app mostra em %.
        "perclos": f"{leitura.perclos * 100:.1f}",
        "segundos_em_alerta": f"{leitura.segundos_em_alerta:.1f}",
        "total_piscadas": str(leitura.total_piscadas),
        "total_bocejos": str(leitura.total_bocejos),
    }


def enviar_para_api(frame_id: int, imagem_jpg: bytes, dados: dict) -> None:
    """Faz o POST. Roda numa thread separada para a análise não parar esperando a rede."""
    arquivos = {"file": (f"webcam_frame_{frame_id}.jpg", imagem_jpg, "image/jpeg")}
    try:
        resposta = requests.post(
            API_URL, files=arquivos, data=dados, timeout=TEMPO_LIMITE_DA_API_EM_SEGUNDOS
        )
        if resposta.status_code == 200:
            print(
                f"✅ Frame {frame_id} enviado | {dados['categoria']} "
                f"| EAR: {dados['ear']} | PERCLOS: {dados['perclos']}%"
            )
        else:
            print(f"❌ Erro ao enviar: {resposta.status_code} - {resposta.text}")
    except requests.RequestException as erro:
        print(f"❌ Erro de conexão: {erro}")


# ── Execução em paralelo ──────────────────────────────────────────────────────
#
# Três tarefas rodam ao mesmo tempo, para chegar a 30 FPS:
#   1. LeitorDeCamera  — lê a câmera sem parar e guarda só o frame mais novo
#   2. Analisador      — MediaPipe + regras + envio para a API (até FPS_ALVO por segundo)
#   3. main()          — desenha o painel e mostra a janela
# Em fila, uma depois da outra, a soma (câmera + 31 ms do MediaPipe + 9 ms da
# tela) ficava abaixo de 25 FPS.


class MedidorDeFps:
    """Média suave de quantas vezes por segundo uma tarefa se repete."""

    def __init__(self) -> None:
        self.valor: Optional[float] = None
        self._ultimo: Optional[float] = None

    def marcar(self, agora: float) -> None:
        if self._ultimo is not None and agora > self._ultimo:
            instantaneo = 1.0 / (agora - self._ultimo)
            self.valor = instantaneo if self.valor is None else 0.9 * self.valor + 0.1 * instantaneo
        self._ultimo = agora


class LeitorDeCamera(threading.Thread):
    """
    Lê a câmera sem parar e guarda só o frame mais novo.

    Sem isto, a análise esperaria cada leitura da rede (DroidCam) e os frames
    antigos se acumulariam na fila, deixando a imagem atrasada em relação ao motorista.
    """

    def __init__(self, fonte) -> None:
        super().__init__(daemon=True)
        self._camera = cv2.VideoCapture(fonte)
        self._camera.set(cv2.CAP_PROP_BUFFERSIZE, 1)
        self._camera.set(cv2.CAP_PROP_FPS, FPS_ALVO)
        self._condicao = threading.Condition()
        self._frame = None
        self._numero = 0
        self.parar = threading.Event()
        self.falhou = False

    def aberta(self) -> bool:
        return self._camera.isOpened()

    def run(self) -> None:
        while not self.parar.is_set():
            sucesso, frame = self._camera.read()
            if not sucesso:
                self.falhou = True
                self.parar.set()
                break
            with self._condicao:
                self._frame, self._numero = frame, self._numero + 1
                self._condicao.notify_all()
        with self._condicao:
            self._condicao.notify_all()
        self._camera.release()

    def proximo_frame(self, ultimo_numero: int, espera: float = 1.0):
        """Espera um frame mais novo que ultimo_numero. Devolve (numero, frame) ou (ultimo_numero, None)."""
        with self._condicao:
            self._condicao.wait_for(lambda: self._numero > ultimo_numero or self.parar.is_set(), timeout=espera)
            if self._numero > ultimo_numero:
                return self._numero, self._frame
            return ultimo_numero, None


class Analisador(threading.Thread):
    """Analisa os frames, decide a categoria e envia para a API, até FPS_ALVO vezes por segundo."""

    def __init__(self, leitor: LeitorDeCamera, detector, monitor) -> None:
        super().__init__(daemon=True)
        self._leitor = leitor
        self._detector = detector
        self._monitor = monitor
        self._trava = threading.Lock()
        self._resultado = None
        self.fps = MedidorDeFps()

    def ultimo_resultado(self):
        """(frame, leitura, categoria, direcao) da última análise, ou None."""
        with self._trava:
            return self._resultado

    def run(self) -> None:
        classificador = ClassificadorDeAlerta()
        frame_id = 0
        numero = 0
        ultimo_envio: Optional[float] = None
        categoria_anterior: Optional[str] = None
        intervalo = 1.0 / FPS_ALVO
        proximo_ciclo = time.monotonic()

        while not self._leitor.parar.is_set():
            numero, frame = self._leitor.proximo_frame(numero)
            if frame is None:
                continue

            capturado_em = datetime.now().astimezone()
            leitura = self._monitor.processar(self._detector.detectar(frame))
            agora = time.monotonic()
            categoria = classificador.classificar(leitura, agora)

            if deve_enviar(agora, ultimo_envio, categoria, categoria_anterior):
                sucesso, imagem_jpg = cv2.imencode(".jpg", frame)  # foto crua, sem desenhos
                if sucesso:
                    frame_id += 1
                    threading.Thread(
                        target=enviar_para_api,
                        args=(frame_id, imagem_jpg.tobytes(),
                              montar_dados(frame_id, leitura, categoria, capturado_em)),
                        daemon=True,
                    ).start()
                    ultimo_envio = agora
            categoria_anterior = categoria

            self.fps.marcar(agora)
            direcao = direcao_do_rosto(leitura.yaw_graus, leitura.pitch_graus)
            with self._trava:
                self._resultado = (frame, leitura, categoria, direcao)

            # Não passa de FPS_ALVO: as regras contam frames pensando em 30 por segundo.
            proximo_ciclo += intervalo
            folga = proximo_ciclo - time.monotonic()
            if folga > 0:
                time.sleep(folga)
            else:
                proximo_ciclo = time.monotonic()


def main() -> None:
    # Importados aqui porque carregam o MediaPipe, que demora alguns segundos.
    from adapters.mediapipe_face_detector import DetectorFacialMediaPipe
    from adapters.hud_veicular import renderizar_painel_veicular
    from domain.drowsiness_monitor import MonitorDeSonolencia

    leitor = LeitorDeCamera(CAMERA_INDEX)
    if not leitor.aberta():
        print("❌ Não foi possível acessar a câmera.")
        sys.exit(1)

    detector = DetectorFacialMediaPipe()
    analisador = Analisador(leitor, detector, MonitorDeSonolencia())
    leitor.start()
    analisador.start()
    cv2.namedWindow(JANELA, cv2.WINDOW_NORMAL)
    print(f"📷 Câmera conectada e transmitindo para o HELIOS (alvo: {FPS_ALVO} FPS). Pressione 'q' para sair.")

    ultimo_mostrado = None
    try:
        while not leitor.parar.is_set():
            resultado = analisador.ultimo_resultado()
            if resultado is not None and resultado is not ultimo_mostrado:
                ultimo_mostrado = resultado
                frame, leitura, categoria, direcao = resultado
                # Os desenhos ficam só na tela: a foto enviada para a API é o frame cru.
                cv2.imshow(JANELA, renderizar_painel_veicular(frame, leitura, categoria, direcao, analisador.fps.valor))
            if cv2.waitKey(5) & 0xFF == ord("q"):
                break
    finally:
        leitor.parar.set()
        analisador.join(timeout=2)
        leitor.join(timeout=2)
        detector.liberar()
        cv2.destroyAllWindows()
        if leitor.falhou:
            print("❌ A câmera parou de enviar imagens.")


if __name__ == "__main__":
    main()
