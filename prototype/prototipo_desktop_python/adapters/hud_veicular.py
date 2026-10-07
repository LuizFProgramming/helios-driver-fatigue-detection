"""
adapters/hud_veicular.py — Tela da camera no estilo painel de veiculo

Desenha SOBRE UMA COPIA do frame (a foto enviada para a API continua crua):
  - barra superior: marca, categoria do motorista em destaque, hora e FPS
  - destaque do rosto inteiro, discreto: malha de 478 pontos, contorno fino,
    cantoneiras de rastreamento, olhos, iris e boca
  - indicadores com linha e ponto perto das palpebras, da boca e da cabeca
  - barra inferior: mostradores (EAR, ECF, PERCLOS, MAR), contadores e uma
    bussola com a direcao do rosto
  - em alerta: moldura vermelha pulsando e faixa com o tipo do alerta

Os textos usam a fonte do Windows (Bahnschrift) via Pillow, que aceita acentos.
Sem a fonte, usa a fonte padrao do Pillow.
"""

import math
import time
from datetime import datetime
from functools import lru_cache
from typing import Optional

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

from domain.ear_calculator import calcular_ear
from domain.types import LeituraMonitoramento
from config import (
    EAR_LIMIAR_OLHO_FECHADO,
    ECF_LIMIAR_OLHO_FECHADO,
    INVERTER_CIMA_BAIXO,
    INVERTER_ESQUERDA_DIREITA,
    MAR_LIMIAR_BOCEJO,
    PERCLOS_LIMIAR_RISCO_ELEVADO,
)

# ── Indices do MediaPipe Face Mesh (478 pontos) ───────────────────────────────

CONTORNO_DO_ROSTO = [10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365,
                     379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93,
                     234, 127, 162, 21, 54, 103, 67, 109]
OLHO_A = [33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246]
OLHO_B = [362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398]
LABIOS = [61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409, 270, 269, 267, 0, 37, 39, 40, 185]

# ── Cores (RGB) ───────────────────────────────────────────────────────────────

BRANCO = (230, 236, 247)
SUAVE = (138, 155, 187)
FUNDO = (10, 14, 24)
CIANO = (56, 189, 248)
VERDE = (34, 197, 94)
AMARELO = (234, 179, 8)
LARANJA = (249, 115, 22)
VERMELHO = (239, 68, 68)
MAGENTA = (217, 70, 239)
CINZA = (148, 163, 184)

COR_POR_CATEGORIA = {
    "ATENTO": VERDE, "PISCANDO": AMARELO, "SEM_ROSTO": CINZA,
    "DESATENTO": LARANJA, "SONOLENCIA": VERMELHO, "DORMINDO": MAGENTA,
}
NOME_POR_CATEGORIA = {
    "ATENTO": "ATENTO", "PISCANDO": "PISCANDO", "SEM_ROSTO": "SEM ROSTO",
    "DESATENTO": "DESATENTO", "SONOLENCIA": "SONOLÊNCIA", "DORMINDO": "DORMINDO",
}
CATEGORIAS_DE_ALERTA = {"DESATENTO", "SONOLENCIA", "DORMINDO"}

FONTES = {
    False: ["C:/Windows/Fonts/bahnschrift.ttf", "C:/Windows/Fonts/segoeui.ttf"],
    True:  ["C:/Windows/Fonts/bahnschrift.ttf", "C:/Windows/Fonts/segoeuib.ttf"],
}
_cache_de_fontes: dict = {}


def _bgr(rgb: tuple) -> tuple:
    return (rgb[2], rgb[1], rgb[0])


def _decimal(valor: float, casas: int = 2) -> str:
    """Número com vírgula, como se escreve no Brasil: 0,31."""
    return f"{valor:.{casas}f}".replace(".", ",")


def _fonte(tamanho: int, negrito: bool):
    chave = (tamanho, negrito)
    if chave not in _cache_de_fontes:
        fonte = None
        for caminho in FONTES[negrito]:
            try:
                fonte = ImageFont.truetype(caminho, tamanho)
                if negrito and "bahnschrift" in caminho:
                    fonte.set_variation_by_name("Bold")
                break
            except (OSError, ValueError):
                continue
        _cache_de_fontes[chave] = fonte or ImageFont.load_default()
    return _cache_de_fontes[chave]


@lru_cache(maxsize=2048)
def _carimbo(texto: str, tamanho: int, negrito: bool, ancora: str):
    """
    Desenha o texto uma unica vez numa mascara (0 a 1) e guarda no cache.
    Nos proximos frames o mesmo texto so e colado: muito mais rapido que
    converter a tela inteira para o Pillow a cada frame.
    """
    fonte = _fonte(tamanho, negrito)
    x0, y0, x1, y1 = fonte.getbbox(texto, anchor=ancora)
    if x1 <= x0 or y1 <= y0:
        return None
    mascara = Image.new("L", (x1 - x0, y1 - y0), 0)
    ImageDraw.Draw(mascara).text((-x0, -y0), texto, font=fonte, fill=255, anchor=ancora)
    return x0, y0, np.asarray(mascara, dtype=np.float32)[..., None] / 255.0


class _Textos:
    """Junta os textos do frame e cola todos no final, por cima dos desenhos."""

    def __init__(self) -> None:
        self._fila: list = []

    def escrever(self, x, y, texto, tamanho, cor=BRANCO, negrito=False, ancora="la", sombra=False):
        """sombra=True: uma cópia escura 1 px abaixo, para ler o texto sobre a pele ou fundo claro."""
        if sombra:
            self._fila.append((int(x) + 1, int(y) + 1, texto, max(int(tamanho), 8), FUNDO, negrito, ancora))
        self._fila.append((int(x), int(y), texto, max(int(tamanho), 8), cor, negrito, ancora))

    def desenhar(self, tela: np.ndarray) -> np.ndarray:
        altura, largura = tela.shape[:2]
        for x, y, texto, tamanho, cor, negrito, ancora in self._fila:
            carimbo = _carimbo(texto, tamanho, negrito, ancora)
            if carimbo is None:
                continue
            dx, dy, alfa = carimbo
            x1, y1 = x + dx, y + dy
            x2, y2 = x1 + alfa.shape[1], y1 + alfa.shape[0]
            # Corta o que passar da borda da tela.
            cx1, cy1, cx2, cy2 = max(x1, 0), max(y1, 0), min(x2, largura), min(y2, altura)
            if cx1 >= cx2 or cy1 >= cy2:
                continue
            a = alfa[cy1 - y1:cy2 - y1, cx1 - x1:cx2 - x1]
            regiao = tela[cy1:cy2, cx1:cx2].astype(np.float32)
            tela[cy1:cy2, cx1:cx2] = (regiao * (1 - a) + np.array(_bgr(cor), np.float32) * a).astype(np.uint8)
        return tela


# ── Entrada ───────────────────────────────────────────────────────────────────

def renderizar_painel_veicular(
    frame: np.ndarray,
    leitura: LeituraMonitoramento,
    categoria: str,
    direcao: str,
    fps: Optional[float] = None,
) -> np.ndarray:
    """Devolve uma NOVA imagem para a tela. O frame original nao e alterado."""
    tela = frame.copy()
    altura, largura = tela.shape[:2]
    # Desenhado para 640x480; em telas largas (16:9) a altura é que limita.
    escala = min(largura / 640, altura / 480)
    textos = _Textos()
    cor = COR_POR_CATEGORIA.get(categoria, CINZA)
    alerta = categoria in CATEGORIAS_DE_ALERTA

    topo, base = int(52 * escala), int(112 * escala)
    # Em alerta, a faixa do alerta ocupa o lugar do rótulo do rosto.
    _destacar_rosto(tela, leitura, cor, direcao, escala, textos,
                    faixa_livre=(topo, altura - base), mostrar_rotulo=not alerta)

    _fundo_translucido(tela, [(0, 0, largura, topo), (0, altura - base, largura, altura)], 0.72)
    _barra_superior(tela, categoria, cor, fps, escala, textos)
    _barra_inferior(tela, leitura, direcao, altura - base, escala, textos)
    if alerta:
        _moldura_de_alerta(tela, leitura, categoria, cor, topo, escala, textos)

    return textos.desenhar(tela)


# ── Rosto ─────────────────────────────────────────────────────────────────────

def _olho_fechado(leitura: LeituraMonitoramento) -> bool:
    return leitura.ear_medio < EAR_LIMIAR_OLHO_FECHADO or leitura.ecf_medio > ECF_LIMIAR_OLHO_FECHADO


def _destacar_rosto(tela, leitura, cor, direcao, escala, textos, faixa_livre, mostrar_rotulo=True):
    lm = leitura.landmarks_todos
    if not lm:
        return

    linha = cv2.LINE_AA
    todos = np.asarray(lm, dtype=np.float32)[:, :2].astype(np.int32)
    contorno = todos[CONTORNO_DO_ROSTO]
    x, y, w, h = cv2.boundingRect(contorno)

    # Malha do rosto inteiro, bem discreta: aparece sem esconder o rosto.
    def malha(camada, ox, oy):
        desloca = np.array([ox, oy], np.int32)
        cv2.polylines(camada, todos[_arestas_da_malha()] - desloca, False, _bgr(BRANCO), 1, cv2.LINE_8)
    _misturar_na_regiao(tela, malha, x - 2, y - 2, x + w + 3, y + h + 3, 0.12)

    # Contorno do rosto: uma linha fina na cor da categoria.
    cv2.polylines(tela, [contorno], True, _bgr(cor), 1, linha)

    # Olhos e boca: linha fina. A cor só muda quando importa (olho fechado, bocejo).
    cor_olho = VERMELHO if _olho_fechado(leitura) else CIANO
    cv2.polylines(tela, [todos[OLHO_A], todos[OLHO_B]], True, _bgr(cor_olho), 1, linha)
    for iris in (leitura.iris_direita, leitura.iris_esquerda):
        if iris and not _olho_fechado(leitura):
            cv2.circle(tela, (int(iris[0][0]), int(iris[0][1])), 2, _bgr(cor_olho), -1, linha)
    cor_boca = AMARELO if leitura.esta_bocejando else CIANO
    cv2.polylines(tela, [todos[LABIOS]], True, _bgr(cor_boca), 1, linha)

    _indicadores(tela, leitura, todos, (x, y, w, h), direcao, escala, textos, faixa_livre)

    # Cantoneiras de rastreamento ao redor do rosto (estilo ADAS), curtas e finas.
    folga = int(0.08 * w)
    x1, y1, x2, y2 = x - folga, y - folga, x + w + folga, y + h + folga
    perna = int(0.10 * (x2 - x1))
    espessura = max(1, int(2 * escala))
    for (cx, cy, dx, dy) in ((x1, y1, 1, 1), (x2, y1, -1, 1), (x1, y2, 1, -1), (x2, y2, -1, -1)):
        cv2.line(tela, (cx, cy), (cx + dx * perna, cy), _bgr(cor), espessura, linha)
        cv2.line(tela, (cx, cy), (cx, cy + dy * perna), _bgr(cor), espessura, linha)

    if mostrar_rotulo:
        textos.escrever(x1, y1 - 8 * escala, "MOTORISTA", 11 * escala, cor, negrito=True, ancora="ld", sombra=True)


@lru_cache(maxsize=1)
def _arestas_da_malha() -> np.ndarray:
    """As 2556 ligações entre os 478 pontos que formam a malha do rosto (lista do MediaPipe)."""
    from mediapipe.tasks.python.vision import FaceLandmarksConnections
    return np.array([(c.start, c.end) for c in FaceLandmarksConnections.FACE_LANDMARKS_TESSELATION], np.int32)


# ── Indicadores perto das pálpebras, boca e cabeça ────────────────────────────

# Pontos onde cada indicador "encosta": centro da pálpebra de cima, canto da boca e testa.
PALPEBRA_A, PALPEBRA_B = 159, 386
CANTO_DA_BOCA_A, TOPO_DA_TESTA = 61, 10


def _indicadores(tela, leitura, todos, caixa, direcao, escala, textos, faixa_livre):
    """Rótulos com linha e ponto, como num laudo de análise facial."""
    x, y, w, h = caixa
    olho_a, olho_b = leitura.olho_direito, leitura.olho_esquerdo  # ordem dos LANDMARKS_OLHO_*_NA_IMAGEM

    def palpebra(olho):
        if olho is None:
            return "—", SUAVE
        ear = calcular_ear(olho)
        fechada = ear < EAR_LIMIAR_OLHO_FECHADO
        return f"EAR {_decimal(ear)} · {'fechada' if fechada else 'aberta'}", VERMELHO if fechada else VERDE

    if leitura.esta_bocejando:
        boca, cor_boca = "bocejo", AMARELO
    elif leitura.mar > MAR_LIMIAR_BOCEJO:
        boca, cor_boca = "aberta", AMARELO
    else:
        boca, cor_boca = "fechada", VERDE

    if leitura.yaw_graus is None:
        cabeca, cor_cabeca = direcao.replace("_", " "), SUAVE
    else:
        cabeca = f"{direcao.replace('_', ' ')} · {leitura.yaw_graus:+.0f}° / {leitura.pitch_graus:+.0f}°"
        cor_cabeca = VERDE if direcao == "frente" else LARANJA

    texto_a, cor_a = palpebra(olho_a)
    texto_b, cor_b = palpebra(olho_b)
    # O lado de cada olho na tela depende da câmera: decide pelo x do ponto.
    pa, pb = todos[PALPEBRA_A], todos[PALPEBRA_B]
    lado_a = "esquerda" if pa[0] <= pb[0] else "direita"
    lado_b = "direita" if lado_a == "esquerda" else "esquerda"
    lado_boca = lado_a
    lado_cabeca = lado_b

    itens = [
        (pa, lado_a, "PÁLPEBRA", texto_a, cor_a),
        (pb, lado_b, "PÁLPEBRA", texto_b, cor_b),
        (todos[CANTO_DA_BOCA_A], lado_boca, "BOCA", f"MAR {_decimal(leitura.mar)} · {boca}", cor_boca),
        (todos[TOPO_DA_TESTA], lado_cabeca, "CABEÇA", cabeca, cor_cabeca),
    ]
    for lado in ("esquerda", "direita"):
        do_lado = sorted((i for i in itens if i[1] == lado), key=lambda i: i[0][1])
        alturas = _espalhar_na_vertical([int(i[0][1]) - int(6 * escala) for i in do_lado], int(36 * escala),
                                        faixa_livre[0] + int(34 * escala), faixa_livre[1] - int(10 * escala))
        for (ponto, _, titulo, valor, cor_valor), y_linha in zip(do_lado, alturas):
            _indicador(tela, textos, ponto, lado, titulo, valor, cor_valor, x, x + w, escala, y_linha)


def _espalhar_na_vertical(desejados: list, espaco: int, minimo: int, maximo: int) -> list:
    """Afasta rótulos do mesmo lado para não ficarem um em cima do outro (lista já em ordem de y)."""
    alturas = []
    for y_desejado in desejados:
        y = max(y_desejado, minimo, (alturas[-1] + espaco) if alturas else minimo)
        alturas.append(y)
    # Se passou do limite de baixo, empurra todo o grupo para cima.
    excesso = (alturas[-1] - maximo) if alturas else 0
    if excesso > 0:
        alturas = [max(minimo, a - excesso) for a in alturas]
    return alturas


def _indicador(tela, textos, ponto, lado, titulo, valor, cor_valor, x_rosto_1, x_rosto_2, escala, y_linha):
    """Um rótulo de duas linhas fora do rosto, sublinhado, com uma linha até o ponto."""
    altura, largura = tela.shape[:2]
    px, py = int(ponto[0]), int(ponto[1])
    tamanho_titulo, tamanho_valor = int(10 * escala), int(12 * escala)
    largura_texto = int(max(_fonte(tamanho_titulo, False).getlength(titulo),
                            _fonte(tamanho_valor, True).getlength(valor)))
    margem = int(10 * escala)

    if lado == "esquerda":
        x_fim = min(x_rosto_1 - int(14 * escala), px - int(30 * escala))
        x_fim = max(x_fim, largura_texto + margem)
        x_ini = x_fim - largura_texto
        x_encosta = x_fim
    else:
        x_ini = max(x_rosto_2 + int(14 * escala), px + int(30 * escala))
        x_ini = min(x_ini, largura - largura_texto - margem)
        x_fim = x_ini + largura_texto
        x_encosta = x_ini

    # Linhas e ponto em cinza claro: guiam o olho sem chamar mais atenção que o texto.
    cor_linha = _bgr(SUAVE)
    cv2.line(tela, (x_ini, y_linha), (x_fim, y_linha), cor_linha, 1, cv2.LINE_AA)
    cv2.line(tela, (x_encosta, y_linha), (px, py), cor_linha, 1, cv2.LINE_AA)
    cv2.circle(tela, (px, py), max(2, int(3 * escala)), _bgr(BRANCO), -1, cv2.LINE_AA)

    ancora = "rs" if lado == "esquerda" else "ls"
    x_texto = x_fim if lado == "esquerda" else x_ini
    textos.escrever(x_texto, y_linha - 18 * escala, titulo, tamanho_titulo, SUAVE, ancora=ancora, sombra=True)
    textos.escrever(x_texto, y_linha - 4 * escala, valor, tamanho_valor, cor_valor, negrito=True,
                    ancora=ancora, sombra=True)


# ── Barras ────────────────────────────────────────────────────────────────────

def _fundo_translucido(tela, retangulos, opacidade):
    """Escurece só os retângulos pedidos (mais rápido que copiar a tela inteira)."""
    fundo = np.array(_bgr(FUNDO), np.float32)
    for x1, y1, x2, y2 in retangulos:
        regiao = tela[y1:y2, x1:x2].astype(np.float32)
        tela[y1:y2, x1:x2] = (regiao * (1 - opacidade) + fundo * opacidade).astype(np.uint8)


def _misturar_na_regiao(tela, camada_desenho, x1, y1, x2, y2, opacidade):
    """addWeighted só dentro do retângulo: a camada é desenhada numa cópia pequena."""
    altura, largura = tela.shape[:2]
    x1, y1, x2, y2 = max(int(x1), 0), max(int(y1), 0), min(int(x2), largura), min(int(y2), altura)
    if x1 >= x2 or y1 >= y2:
        return
    regiao = tela[y1:y2, x1:x2]
    camada = regiao.copy()
    camada_desenho(camada, x1, y1)
    cv2.addWeighted(camada, opacidade, regiao, 1 - opacidade, 0, regiao)


def _retangulo_arredondado(img, x1, y1, x2, y2, raio, cor_bgr, espessura=-1):
    raio = int(min(raio, (x2 - x1) / 2, (y2 - y1) / 2))
    if espessura < 0:
        cv2.rectangle(img, (x1 + raio, y1), (x2 - raio, y2), cor_bgr, -1)
        cv2.rectangle(img, (x1, y1 + raio), (x2, y2 - raio), cor_bgr, -1)
    else:
        cv2.line(img, (x1 + raio, y1), (x2 - raio, y1), cor_bgr, espessura, cv2.LINE_AA)
        cv2.line(img, (x1 + raio, y2), (x2 - raio, y2), cor_bgr, espessura, cv2.LINE_AA)
        cv2.line(img, (x1, y1 + raio), (x1, y2 - raio), cor_bgr, espessura, cv2.LINE_AA)
        cv2.line(img, (x2, y1 + raio), (x2, y2 - raio), cor_bgr, espessura, cv2.LINE_AA)
    for cx, cy, angulo in ((x1 + raio, y1 + raio, 180), (x2 - raio, y1 + raio, 270),
                           (x2 - raio, y2 - raio, 0), (x1 + raio, y2 - raio, 90)):
        cv2.ellipse(img, (cx, cy), (raio, raio), angulo, 0, 90, cor_bgr, espessura, cv2.LINE_AA)


def _barra_superior(tela, categoria, cor, fps, escala, textos):
    largura = tela.shape[1]
    meio = 26 * escala

    cv2.circle(tela, (int(24 * escala), int(meio)), int(9 * escala), _bgr(AMARELO), -1, cv2.LINE_AA)
    textos.escrever(40 * escala, meio - 9 * escala, "HELIOS", 17 * escala, BRANCO, negrito=True)
    textos.escrever(40 * escala, meio + 9 * escala, "MONITOR DO MOTORISTA", 9 * escala, SUAVE)

    nome = NOME_POR_CATEGORIA.get(categoria, categoria)
    meia_largura = int(78 * escala)
    x1, x2 = largura // 2 - meia_largura, largura // 2 + meia_largura
    y1, y2 = int(10 * escala), int(42 * escala)
    _misturar_na_regiao(
        tela, lambda c, ox, oy: _retangulo_arredondado(c, x1 - ox, y1 - oy, x2 - ox, y2 - oy, 16 * escala, _bgr(cor)),
        x1, y1, x2 + 1, y2 + 1, 0.22,
    )
    _retangulo_arredondado(tela, x1, y1, x2, y2, 16 * escala, _bgr(cor), espessura=max(1, int(2 * escala)))
    textos.escrever(largura // 2, meio, nome, 17 * escala, cor, negrito=True, ancora="mm")

    hora = datetime.now().strftime("%H:%M:%S")
    textos.escrever(largura - 16 * escala, meio - 8 * escala, hora, 16 * escala, BRANCO, negrito=True, ancora="rm")
    texto_fps = f"{fps:.0f} FPS" if fps else "— FPS"
    textos.escrever(largura - 16 * escala, meio + 11 * escala, texto_fps, 10 * escala, SUAVE, ancora="rm")


def _barra_inferior(tela, leitura, direcao, y0, escala, textos):
    largura = tela.shape[1]
    centro_y = y0 + int(50 * escala)
    raio = int(27 * escala)

    mostradores = [
        ("EAR", leitura.ear_medio, 0.45, EAR_LIMIAR_OLHO_FECHADO, False, _decimal(leitura.ear_medio)),
        ("ECF", leitura.ecf_medio, 1.0, ECF_LIMIAR_OLHO_FECHADO, True, _decimal(leitura.ecf_medio)),
        ("PERCLOS", leitura.perclos, 1.0, PERCLOS_LIMIAR_RISCO_ELEVADO, True, f"{leitura.perclos * 100:.0f}%"),
        ("MAR", leitura.mar, 1.0, MAR_LIMIAR_BOCEJO, True, _decimal(leitura.mar)),
    ]
    for i, (nome, valor, maximo, limiar, alto_e_ruim, texto) in enumerate(mostradores):
        cx = int((52 + i * 86) * escala)
        _mostrador(tela, cx, centro_y, raio, valor / maximo, limiar / maximo, alto_e_ruim, escala)
        textos.escrever(cx, centro_y, texto, 13 * escala, BRANCO, negrito=True, ancora="mm")
        textos.escrever(cx, centro_y + raio + 12 * escala, nome, 10 * escala, SUAVE, ancora="mm")

    x_contadores = int(392 * escala)
    textos.escrever(x_contadores, centro_y - 22 * escala, "PISCADAS", 9 * escala, SUAVE)
    textos.escrever(x_contadores, centro_y - 10 * escala, str(leitura.total_piscadas), 18 * escala, BRANCO, negrito=True)
    textos.escrever(x_contadores, centro_y + 14 * escala, "BOCEJOS", 9 * escala, SUAVE)
    textos.escrever(x_contadores, centro_y + 26 * escala, str(leitura.total_bocejos), 18 * escala, BRANCO, negrito=True)

    _bussola(tela, largura - int(72 * escala), centro_y, int(30 * escala), leitura, direcao, escala, textos)


def _mostrador(tela, cx, cy, raio, proporcao, proporcao_limiar, alto_e_ruim, escala):
    """Arco de 270 graus, como um velocimetro; um risco marca o limiar."""
    inicio, varredura = 135, 270
    proporcao = max(0.0, min(1.0, proporcao))
    ruim = proporcao > proporcao_limiar if alto_e_ruim else proporcao < proporcao_limiar
    cor = VERMELHO if ruim else VERDE
    espessura = max(3, int(5 * escala))
    cv2.ellipse(tela, (cx, cy), (raio, raio), 0, inicio, inicio + varredura, _bgr((40, 52, 78)), espessura, cv2.LINE_AA)
    if proporcao > 0:
        cv2.ellipse(tela, (cx, cy), (raio, raio), 0, inicio, inicio + varredura * proporcao, _bgr(cor), espessura, cv2.LINE_AA)
    angulo = math.radians(inicio + varredura * proporcao_limiar)
    p1 = (int(cx + (raio - 7 * escala) * math.cos(angulo)), int(cy + (raio - 7 * escala) * math.sin(angulo)))
    p2 = (int(cx + (raio + 7 * escala) * math.cos(angulo)), int(cy + (raio + 7 * escala) * math.sin(angulo)))
    cv2.line(tela, p1, p2, _bgr(BRANCO), max(1, int(2 * escala)), cv2.LINE_AA)


def _bussola(tela, cx, cy, raio, leitura, direcao, escala, textos):
    """Circulo com um ponto: centro = rosto de frente; o ponto anda para onde o rosto gira."""
    linha = cv2.LINE_AA
    cv2.circle(tela, (cx, cy), raio, _bgr(SUAVE), 1, linha)
    cv2.circle(tela, (cx, cy), raio // 2, _bgr((40, 52, 78)), 1, linha)
    cv2.line(tela, (cx - raio, cy), (cx + raio, cy), _bgr((40, 52, 78)), 1, linha)
    cv2.line(tela, (cx, cy - raio), (cx, cy + raio), _bgr((40, 52, 78)), 1, linha)

    if leitura.yaw_graus is None or leitura.pitch_graus is None:
        cor = CINZA
    else:
        # Mesma convencao de domain/head_pose.py: depois das inversoes do config,
        # yaw positivo = "esquerda" (ponto vai para a esquerda) e
        # pitch positivo = "cima" (ponto sobe). 45 graus chega na borda.
        yaw = -leitura.yaw_graus if INVERTER_ESQUERDA_DIREITA else leitura.yaw_graus
        pitch = -leitura.pitch_graus if INVERTER_CIMA_BAIXO else leitura.pitch_graus
        dx = -max(-1.0, min(1.0, yaw / 45)) * raio
        dy = -max(-1.0, min(1.0, pitch / 45)) * raio
        cor = VERDE if direcao == "frente" else LARANJA
        cv2.circle(tela, (int(cx + dx), int(cy + dy)), max(4, int(6 * escala)), _bgr(cor), -1, linha)

    textos.escrever(cx, cy + raio + 12 * escala, direcao.replace("_", " ").upper(), 10 * escala, cor, negrito=True, ancora="mm")
    textos.escrever(cx - raio - 10 * escala, cy, "ROSTO", 9 * escala, SUAVE, ancora="rm")


# ── Alerta ────────────────────────────────────────────────────────────────────

DESCRICAO_DO_ALERTA = {
    "DORMINDO": "Olhos fechados sem parar",
    "SONOLENCIA": "Sinais de sonolência: faça uma pausa",
    "DESATENTO": "Rosto fora da câmera",
}


def _moldura_de_alerta(tela, leitura, categoria, cor, topo, escala, textos):
    altura, largura = tela.shape[:2]
    pulso = 0.5 + 0.5 * math.sin(time.monotonic() * 7)

    # Moldura que pulsa: a cor fica mais forte e mais fraca, umas 1,1 vezes por segundo.
    intensidade = 0.4 + 0.6 * pulso
    cor_moldura = tuple(int(c * intensidade) for c in _bgr(cor))
    cv2.rectangle(tela, (0, 0), (largura - 1, altura - 1), cor_moldura, max(6, int(12 * escala)))

    y1, y2 = topo + int(10 * escala), topo + int(56 * escala)
    x1, x2 = int(largura * 0.2), int(largura * 0.8)
    _misturar_na_regiao(
        tela, lambda c, ox, oy: _retangulo_arredondado(c, x1 - ox, y1 - oy, x2 - ox, y2 - oy, 12 * escala, _bgr(cor)),
        x1, y1, x2 + 1, y2 + 1, 0.85,
    )

    titulo = f"ALERTA · {NOME_POR_CATEGORIA[categoria]}"
    detalhe = DESCRICAO_DO_ALERTA[categoria]
    if categoria == "DORMINDO" and leitura.segundos_em_alerta:
        detalhe += f" há {_decimal(leitura.segundos_em_alerta, 1)} s"
    elif categoria == "SONOLENCIA":
        detalhe = f"PERCLOS {leitura.perclos * 100:.0f}% · faça uma pausa"
    textos.escrever(largura // 2, y1 + 15 * escala, titulo, 17 * escala, BRANCO, negrito=True, ancora="mm")
    textos.escrever(largura // 2, y1 + 34 * escala, detalhe, 11 * escala, BRANCO, ancora="mm")
