"""Aumentos de treino que imitam a câmera do carro (ADR-011, adendo; ADR-016).

A rede treinada só com a CEW chamava de fechado o olho aberto visto por uma câmera de celular: pouco contraste,
contraluz, ruído, câmera de baixo e, principalmente, óculos (a armação escura parece uma pálpebra). Cada função
recebe e devolve um recorte uint8 de 64x64 e não muda o rótulo: os mesmos aumentos caem em olhos abertos e
fechados, para a rede não aprender "armação = aberto" nem "armação = fechado".
"""

from __future__ import annotations

import cv2
import numpy as np

LADO = 64


def pouco_contraste(img: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Aproxima os pixels da média (contraste de 30% a 100%) e desloca o brilho."""
    fator = rng.uniform(0.3, 1.0)
    media = img.mean()
    return np.clip(media + (img.astype(np.float32) - media) * fator + rng.uniform(-40, 40), 0, 255).astype(np.uint8)


def contraluz(img: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Gradiente de luz numa direção ao acaso: um lado do rosto mais escuro que o outro."""
    angulo = rng.uniform(0, 2 * np.pi)
    y, x = np.mgrid[0:LADO, 0:LADO].astype(np.float32) / (LADO - 1) - 0.5
    rampa = x * np.cos(angulo) + y * np.sin(angulo)  # de -0,7 a 0,7
    ganho = 1.0 + rampa * rng.uniform(0.4, 1.2)
    return np.clip(img.astype(np.float32) * ganho, 0, 255).astype(np.uint8)


def ruido_e_desfoque(img: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Sensor pequeno com pouca luz: desfoque leve e ruído."""
    sigma = rng.uniform(0.0, 1.5)
    borrada = cv2.GaussianBlur(img, (0, 0), sigma) if sigma > 0.2 else img
    ruido = rng.normal(0.0, rng.uniform(0.0, 12.0), img.shape)
    return np.clip(borrada.astype(np.float32) + ruido, 0, 255).astype(np.uint8)


def camera_de_baixo(img: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Câmera abaixo do rosto: o olho fica achatado na vertical e sobe ou desce no recorte."""
    altura = int(round(LADO * rng.uniform(0.7, 1.0)))
    achatada = cv2.resize(img, (LADO, altura), interpolation=cv2.INTER_AREA)
    sobra = LADO - altura
    cima = int(rng.integers(0, sobra + 1))
    return cv2.copyMakeBorder(achatada, cima, sobra - cima, 0, 0, cv2.BORDER_REPLICATE)


def oculos(img: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Armação em volta do olho, com a lente um pouco mais escura e, às vezes, um reflexo claro."""
    saida = img.astype(np.float32)
    centro = (int(LADO / 2 + rng.integers(-6, 7)), int(LADO / 2 + rng.integers(-5, 8)))
    eixos = (int(rng.integers(24, 46)), int(rng.integers(14, 30)))  # muitas vezes maior que o recorte
    espessura = int(rng.integers(2, 6))
    cor = float(rng.uniform(10, 90))
    lente = np.zeros((LADO, LADO), np.uint8)
    cv2.ellipse(lente, centro, eixos, 0, 0, 360, 255, -1)
    saida[lente > 0] *= rng.uniform(0.8, 1.0)  # lente levemente fumê
    if rng.random() < 0.5:
        saida = _reflexo(saida, lente, rng)
    armacao = np.zeros((LADO, LADO), np.uint8)
    if rng.random() < 0.3:  # armação só em cima (meio aro)
        cv2.ellipse(armacao, centro, eixos, 0, 180, 360, 255, espessura)
    else:
        cv2.ellipse(armacao, centro, eixos, 0, 0, 360, 255, espessura)
    lado_ponte = -1 if rng.random() < 0.5 else 1  # ponte do nariz ou haste, saindo pela borda
    inicio = (centro[0] + lado_ponte * eixos[0], centro[1] - int(rng.integers(0, 6)))
    cv2.line(armacao, inicio, (inicio[0] + lado_ponte * LADO, inicio[1] - int(rng.integers(-3, 4))), 255, espessura)
    mascara = cv2.GaussianBlur(armacao, (0, 0), 0.8).astype(np.float32) / 255.0
    saida = saida * (1.0 - mascara) + cor * mascara
    return np.clip(saida, 0, 255).astype(np.uint8)


def _reflexo(img: np.ndarray, lente: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Mancha clara dentro da lente (tela, janela ou filtro azul refletido)."""
    y, x = np.mgrid[0:LADO, 0:LADO].astype(np.float32)
    cx, cy = rng.uniform(16, 48), rng.uniform(16, 48)
    sx, sy = rng.uniform(5, 16), rng.uniform(3, 9)
    mancha = np.exp(-(((x - cx) / sx) ** 2 + ((y - cy) / sy) ** 2)) * rng.uniform(40, 120)
    return img + mancha * (lente > 0)


def aumentar_real(img: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Combina os aumentos ao acaso. [img]: uint8 64x64 (ou 64x64x1); devolve o mesmo formato."""
    plano = img.reshape(LADO, LADO)
    if rng.random() < 0.5:
        plano = plano[:, ::-1]
    if rng.random() < 0.5:
        plano = camera_de_baixo(np.ascontiguousarray(plano), rng)
    if rng.random() < 0.45:
        plano = oculos(np.ascontiguousarray(plano), rng)
    if rng.random() < 0.5:
        plano = contraluz(plano, rng)
    if rng.random() < 0.7:
        plano = pouco_contraste(plano, rng)
    if rng.random() < 0.6:
        plano = ruido_e_desfoque(np.ascontiguousarray(plano), rng)
    return np.ascontiguousarray(plano).reshape(img.shape)


def aumentar_lote(imagens: np.ndarray, semente: int, funcao=aumentar_real) -> np.ndarray:
    """Versão fixa (mesma semente, mesmo resultado) para validação e teste."""
    rng = np.random.default_rng(semente)
    return np.stack([funcao(i.reshape(LADO, LADO), rng).reshape(i.shape) for i in imagens.astype(np.uint8)])
