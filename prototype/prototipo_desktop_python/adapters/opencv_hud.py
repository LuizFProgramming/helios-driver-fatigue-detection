"""
adapters/opencv_hud.py — Renderizacao das duas janelas do HELIOS

Janela MONITOR  — interface limpa para uso real. Mostra contornos dos
                  olhos e boca, estado do motorista e metricas essenciais.

Janela MAPEAMENTO — interface tecnica. Mostra todos os 478 landmarks com
                    efeito de profundidade 3D, regioes faciais coloridas por
                    categoria, linhas de medicao EAR/MAR e metricas completas.
"""

import cv2
import numpy as np

from domain.types import EstadoMotorista, LeituraMonitoramento
from config import (
    EAR_LIMIAR_OLHO_FECHADO,
    EAR_VALOR_MAXIMO_NA_BARRA_VISUAL,
    COR_ESTADO_ATENTO, COR_ESTADO_PISCANDO, COR_ESTADO_SONOLENTO, COR_ESTADO_SEM_FACE,
    COR_VALOR_EAR, COR_VALOR_MAR, COR_METRICAS_SECUNDARIAS,
    COR_CONTORNO_OLHO_ABERTO, COR_CONTORNO_OLHO_FECHADO,
    COR_CONTORNO_BOCA_NORMAL, COR_CONTORNO_BOCA_BOCEJO,
    COR_FUNDO_PAINEL_METRICAS, COR_FUNDO_BARRA_EAR,
    COR_FUNDO_BANNER_ALERTA, COR_FUNDO_BANNER_BOCEJO,
    COR_MAPA_CONTORNO_ROSTO,
    COR_MAPA_SOBRANCELHAS, COR_MAPA_OLHOS, COR_MAPA_NARIZ, COR_MAPA_BOCA,
    COR_MAPA_EAR_PONTOS, COR_MAPA_EAR_LINHA_HORIZ, COR_MAPA_EAR_LINHA_VERT,
    COR_MAPA_MAR_PONTOS, COR_MAPA_MAR_LINHA_HORIZ, COR_MAPA_MAR_LINHA_VERT,
)

_FONTE       = cv2.FONT_HERSHEY_SIMPLEX
_FONTE_MONO  = cv2.FONT_HERSHEY_DUPLEX

_COR_POR_ESTADO = {
    EstadoMotorista.ATENTO:    COR_ESTADO_ATENTO,
    EstadoMotorista.PISCANDO:  COR_ESTADO_PISCANDO,
    EstadoMotorista.SONOLENTO: COR_ESTADO_SONOLENTO,
    EstadoMotorista.SEM_FACE:  COR_ESTADO_SEM_FACE,
}

# ── Indices das regioes faciais para a janela de mapeamento ───────────────────
# (estes sao constantes visuais, nao parametros de deteccao)

_CONTORNO_ROSTO = [
    10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
    397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
    172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109, 10,
]

_SOBRANCELHA_DIR = [46, 53, 52, 65, 55, 70, 63, 105, 66, 107, 55, 46]
_SOBRANCELHA_ESQ = [276, 283, 282, 295, 285, 300, 293, 334, 296, 336, 285, 276]

_OLHO_DIR_TODOS = [33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246, 33]
_OLHO_ESQ_TODOS = [362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398, 362]

_NARIZ_PONTE = [168, 6, 197, 195, 5, 4]
_NARIZ_BASE  = [98, 97, 2, 326, 327, 278, 344, 440, 275, 4, 45, 220, 115, 48, 64, 98]

_BOCA_EXTERNA = [61, 185, 40, 39, 37, 0, 267, 269, 270, 409, 291, 375, 321, 405, 314, 17, 84, 181, 91, 146, 61]
_BOCA_INTERNA = [78, 191, 80, 81, 82, 13, 312, 311, 310, 308, 415, 324, 318, 402, 317, 14, 87, 178, 88, 95, 78]


# =============================================================================
# JANELA MONITOR — interface limpa
# =============================================================================

def renderizar_monitor(frame: np.ndarray, leitura: LeituraMonitoramento) -> np.ndarray:
    """
    Vista limpa para uso operacional.
    Mostra contornos minimos, estado em destaque e painel de metricas.
    """
    saida          = frame.copy()
    altura, largura = saida.shape[:2]

    _monitor_contornos(saida, leitura)
    _monitor_status(saida, leitura, largura)
    _monitor_painel_metricas(saida, leitura, largura)
    _monitor_barra_ear(saida, leitura, largura, altura)
    _monitor_indicador_bocejo(saida, leitura, altura)

    return saida


def _monitor_contornos(frame, leitura):
    """Poligono de 6 pontos ao redor de cada olho e da boca."""
    cor_olho = COR_CONTORNO_OLHO_FECHADO if leitura.ear_medio < EAR_LIMIAR_OLHO_FECHADO else COR_CONTORNO_OLHO_ABERTO
    cor_boca = COR_CONTORNO_BOCA_BOCEJO  if leitura.esta_bocejando else COR_CONTORNO_BOCA_NORMAL

    if leitura.olho_direito:
        _poligono_6pts(frame, leitura.olho_direito.como_sequencia(), cor_olho, espessura=1)
    if leitura.olho_esquerdo:
        _poligono_6pts(frame, leitura.olho_esquerdo.como_sequencia(), cor_olho, espessura=1)
    if leitura.boca:
        _poligono_6pts(frame, leitura.boca.como_sequencia(), cor_boca, espessura=1)


def _monitor_status(frame, leitura, largura):
    cor    = _COR_POR_ESTADO[leitura.estado]
    estado = leitura.estado

    if estado == EstadoMotorista.SONOLENTO:
        cv2.rectangle(frame, (0, 0), (largura, 68), COR_FUNDO_BANNER_ALERTA, -1)
        texto = f"SONOLENCIA DETECTADA  {leitura.segundos_em_alerta:.0f}s"
        cv2.putText(frame, texto, (10, 46), _FONTE, 0.95, (255, 255, 255), 2)
    else:
        cv2.putText(frame, estado.value, (10, 44), _FONTE, 1.2, cor, 3)


def _monitor_painel_metricas(frame, leitura, largura):
    x       = largura - 215
    h_painel = 170
    cv2.rectangle(frame, (x - 8, 0), (largura, h_painel), COR_FUNDO_PAINEL_METRICAS, -1)

    linhas = [
        (f"EAR:     {leitura.ear_medio:.3f}",         COR_VALOR_EAR),
        (f"ECF:     {leitura.ecf_medio:.3f}",         (180, 100, 255)),   # roxo — sinal neural
        (f"MAR:     {leitura.mar:.3f}",               COR_VALOR_MAR),
        (f"PERCLOS: {leitura.perclos * 100:.1f}%",    COR_METRICAS_SECUNDARIAS),
        (f"Piscadas:{leitura.total_piscadas}",          COR_METRICAS_SECUNDARIAS),
        (f"Bocejos: {leitura.total_bocejos}",           COR_METRICAS_SECUNDARIAS),
    ]
    for i, (texto, cor) in enumerate(linhas):
        cv2.putText(frame, texto, (x, 22 + i * 25), _FONTE, 0.55, cor, 1)


def _monitor_barra_ear(frame, leitura, largura, altura):
    largura_max = largura // 2
    y           = altura - 30

    proporcao   = min(leitura.ear_medio / EAR_VALOR_MAXIMO_NA_BARRA_VISUAL, 1.0)
    largura_ear = int(proporcao * largura_max)
    cor_barra   = COR_ESTADO_ATENTO if leitura.ear_medio >= EAR_LIMIAR_OLHO_FECHADO else COR_ESTADO_SONOLENTO

    cv2.rectangle(frame, (10, y), (10 + largura_max,  y + 14), COR_FUNDO_BARRA_EAR, -1)
    cv2.rectangle(frame, (10, y), (10 + largura_ear,  y + 14), cor_barra, -1)
    cv2.rectangle(frame, (10, y), (10 + largura_max,  y + 14), COR_METRICAS_SECUNDARIAS, 1)

    x_limiar = 10 + int((EAR_LIMIAR_OLHO_FECHADO / EAR_VALOR_MAXIMO_NA_BARRA_VISUAL) * largura_max)
    cv2.line(frame, (x_limiar, y - 4), (x_limiar, y + 18), (0, 0, 255), 2)
    cv2.putText(frame, "EAR", (10 + largura_max + 5, y + 11), _FONTE, 0.42, COR_METRICAS_SECUNDARIAS, 1)


def _monitor_indicador_bocejo(frame, leitura, altura):
    if not leitura.esta_bocejando:
        return
    cv2.rectangle(frame, (0, altura - 55), (200, altura), COR_FUNDO_BANNER_BOCEJO, -1)
    cv2.putText(frame, "BOCEJANDO", (8, altura - 28), _FONTE, 0.65, (255, 255, 255), 2)


# =============================================================================
# JANELA MAPEAMENTO — vista tecnica 3D
# =============================================================================

def renderizar_mapeamento(frame: np.ndarray, leitura: LeituraMonitoramento) -> np.ndarray:
    """
    Vista tecnica: todos os 478 landmarks com profundidade 3D,
    mesh estrutural, iris, regioes faciais coloridas e linhas EAR/MAR.
    """
    saida           = frame.copy()
    altura, largura = saida.shape[:2]

    cv2.addWeighted(saida, 0.72, np.zeros_like(saida), 0.28, 0, dst=saida)

    if leitura.landmarks_todos:
        lm = leitura.landmarks_todos
        _mapa_todos_landmarks(saida, lm)
        _mapa_mesh_estrutural(saida, lm)
        _mapa_regioes(saida, lm)

    if leitura.olho_direito and leitura.olho_esquerdo:
        _mapa_medicao_ear(saida, leitura)
    if leitura.boca:
        _mapa_medicao_mar(saida, leitura)

    # Iris: desenhado por cima de tudo para maior destaque
    if leitura.iris_direita:
        _mapa_iris(saida, leitura.iris_direita)
    if leitura.iris_esquerda:
        _mapa_iris(saida, leitura.iris_esquerda)

    _mapa_painel_metricas(saida, leitura, largura, altura)
    _mapa_titulo(saida, largura)

    return saida


def _mapa_todos_landmarks(frame, landmarks):
    """
    Desenha todos os 478 landmarks com tamanho e cor baseados na
    coordenada z (profundidade): pontos mais proximos sao mais brilhantes.
    """
    h, w = frame.shape[:2]
    if not landmarks:
        return

    zs = np.array([z for _, _, z in landmarks], dtype=np.float32)
    # normaliza z para [0,1]: z mais negativo = mais proximo = mais brilhante
    z_min, z_max = zs.min(), zs.max()
    z_range = max(z_max - z_min, 1e-6)
    z_norm  = 1.0 - np.clip((zs - z_min) / z_range, 0, 1)

    for idx, lm in enumerate(landmarks):
        ix, iy = int(lm[0]), int(lm[1])
        if not (0 <= ix < w and 0 <= iy < h):
            continue
        brilho = int(40 + 80 * z_norm[idx])
        cv2.circle(frame, (ix, iy), 1, (brilho, brilho, brilho), -1)


def _mapa_regioes(frame, landmarks):
    """Desenha as regioes faciais com cores distintas por categoria."""

    def polyline(indices, cor, espessura=1):
        pts = _indices_para_pts(landmarks, indices, frame.shape[1], frame.shape[0])
        if pts is not None:
            cv2.polylines(frame, [pts], isClosed=False, color=cor, thickness=espessura)

    def region_dots(indices, cor, radius=2):
        w, h = frame.shape[1], frame.shape[0]
        for i in indices:
            x, y = int(landmarks[i][0]), int(landmarks[i][1])
            if 0 <= x < w and 0 <= y < h:
                cv2.circle(frame, (x, y), radius, cor, -1)

    # Contorno do rosto
    polyline(_CONTORNO_ROSTO,    COR_MAPA_CONTORNO_ROSTO, espessura=1)

    # Sobrancelhas
    region_dots(_SOBRANCELHA_DIR[:-1], COR_MAPA_SOBRANCELHAS, radius=2)
    region_dots(_SOBRANCELHA_ESQ[:-1], COR_MAPA_SOBRANCELHAS, radius=2)
    polyline(_SOBRANCELHA_DIR, COR_MAPA_SOBRANCELHAS)
    polyline(_SOBRANCELHA_ESQ, COR_MAPA_SOBRANCELHAS)

    # Olhos (regiao completa)
    region_dots(_OLHO_DIR_TODOS[:-1], COR_MAPA_OLHOS, radius=2)
    region_dots(_OLHO_ESQ_TODOS[:-1], COR_MAPA_OLHOS, radius=2)
    polyline(_OLHO_DIR_TODOS, COR_MAPA_OLHOS)
    polyline(_OLHO_ESQ_TODOS, COR_MAPA_OLHOS)

    # Nariz
    region_dots(_NARIZ_PONTE, COR_MAPA_NARIZ, radius=2)
    region_dots(_NARIZ_BASE,  COR_MAPA_NARIZ, radius=2)
    polyline(_NARIZ_PONTE, COR_MAPA_NARIZ)

    # Boca
    polyline(_BOCA_EXTERNA, COR_MAPA_BOCA)
    polyline(_BOCA_INTERNA, COR_MAPA_BOCA)


def _mapa_medicao_ear(frame, leitura):
    """
    Desenha os 6 pontos EAR de cada olho com as linhas de medicao da formula:
      Linhas verdes = distancias verticais (numerador)
      Linha azul    = distancia horizontal (denominador)
    """
    for olho in (leitura.olho_direito, leitura.olho_esquerdo):
        pts = olho.como_sequencia()
        pi  = [(int(p[0]), int(p[1])) for p in pts]

        # Pontos EAR em destaque
        for p in pi:
            cv2.circle(frame, p, 4, COR_MAPA_EAR_PONTOS, -1)
            cv2.circle(frame, p, 5, (0, 0, 0), 1)

        # Linha horizontal (p1-p4) — denominador
        cv2.line(frame, pi[0], pi[3], COR_MAPA_EAR_LINHA_HORIZ, 1)

        # Linhas verticais (p2-p6 e p3-p5) — numerador
        cv2.line(frame, pi[1], pi[5], COR_MAPA_EAR_LINHA_VERT, 1)
        cv2.line(frame, pi[2], pi[4], COR_MAPA_EAR_LINHA_VERT, 1)


def _mapa_medicao_mar(frame, leitura):
    """
    Desenha os 6 pontos MAR da boca com as linhas de medicao:
      Linhas verdes = distancias verticais (abertura)
      Linha azul    = distancia horizontal (largura)
    """
    pts = leitura.boca.como_sequencia()
    pi  = [(int(p[0]), int(p[1])) for p in pts]

    for p in pi:
        cv2.circle(frame, p, 4, COR_MAPA_MAR_PONTOS, -1)
        cv2.circle(frame, p, 5, (0, 0, 0), 1)

    cv2.line(frame, pi[0], pi[3], COR_MAPA_MAR_LINHA_HORIZ, 1)
    cv2.line(frame, pi[1], pi[5], COR_MAPA_MAR_LINHA_VERT,  1)
    cv2.line(frame, pi[2], pi[4], COR_MAPA_MAR_LINHA_VERT,  1)


def _mapa_mesh_estrutural(frame, landmarks):
    """
    Desenha as conexoes estruturais do face mesh usando as definicoes
    canonicas do MediaPipe. Cria o efeito de wireframe 3D.
    """
    try:
        from mediapipe.python.solutions import face_mesh_connections as fmc
        conexoes = fmc.FACEMESH_TESSELATION
    except Exception:
        return   # fallback silencioso se API legada nao estiver acessivel

    w, h = frame.shape[1], frame.shape[0]
    for (i, j) in conexoes:
        if i >= len(landmarks) or j >= len(landmarks):
            continue
        x1, y1 = int(landmarks[i][0]), int(landmarks[i][1])
        x2, y2 = int(landmarks[j][0]), int(landmarks[j][1])
        if not (0 <= x1 < w and 0 <= y1 < h and 0 <= x2 < w and 0 <= y2 < h):
            continue
        cv2.line(frame, (x1, y1), (x2, y2), (30, 30, 30), 1)


def _mapa_iris(frame, iris_pts):
    """
    Visualiza a iris usando os 5 landmarks da iris (MediaPipe 468-477).
    Ponto 0 = centro; pontos 1-4 = perimetro.
    Desenha: ponto central + circulo aproximado pelo raio medio.
    """
    if not iris_pts or len(iris_pts) < 5:
        return

    cx, cy = int(iris_pts[0][0]), int(iris_pts[0][1])
    raios  = [
        int(((iris_pts[k][0] - cx) ** 2 + (iris_pts[k][1] - cy) ** 2) ** 0.5)
        for k in range(1, 5)
    ]
    raio = max(1, sum(raios) // 4)

    cv2.circle(frame, (cx, cy), raio,     (0, 255, 255),  1)   # circulo da iris (ciano)
    cv2.circle(frame, (cx, cy), raio // 3, (0, 200, 255), -1)  # pupila estimada (amarelo)
    cv2.circle(frame, (cx, cy), 1,         (255, 255, 255), -1) # centro


def _mapa_painel_metricas(frame, leitura, largura, altura):
    """Painel inferior com todas as metricas na janela de mapeamento."""
    h_painel = 72
    y_painel = altura - h_painel
    overlay  = frame.copy()
    cv2.rectangle(overlay, (0, y_painel), (largura, altura), (10, 10, 10), -1)
    cv2.addWeighted(overlay, 0.85, frame, 0.15, 0, dst=frame)

    estado_txt = f"Estado: {leitura.estado.value}"
    if leitura.esta_bocejando:
        estado_txt += "  |  BOCEJANDO"

    linhas = [
        (f"EAR: {leitura.ear_medio:.3f}   "
         f"ECF: {leitura.ecf_medio:.3f}   "
         f"MAR: {leitura.mar:.3f}   "
         f"PERCLOS: {leitura.perclos*100:.1f}%",                           COR_METRICAS_SECUNDARIAS),
        (f"{estado_txt}   "
         f"Piscadas: {leitura.total_piscadas}   "
         f"Bocejos: {leitura.total_bocejos}",                              COR_METRICAS_SECUNDARIAS),
    ]
    for i, (texto, cor) in enumerate(linhas):
        cv2.putText(frame, texto, (10, y_painel + 20 + i * 24), _FONTE, 0.50, cor, 1)


def _mapa_titulo(frame, largura):
    cv2.putText(frame, "HELIOS  MAPEAMENTO FACIAL", (10, 22), _FONTE_MONO, 0.55, (180, 180, 180), 1)


# =============================================================================
# Utilitarios compartilhados
# =============================================================================

def _poligono_6pts(frame, pontos, cor, espessura):
    pts = np.array([(int(p[0]), int(p[1])) for p in pontos], dtype=np.int32)
    cv2.polylines(frame, [pts], isClosed=True, color=cor, thickness=espessura)


def _indices_para_pts(landmarks, indices, w, h):
    try:
        pts = np.array(
            [(int(landmarks[i][0]), int(landmarks[i][1])) for i in indices],
            dtype=np.int32
        )
        return pts.reshape(-1, 1, 2)
    except (IndexError, TypeError):
        return None
