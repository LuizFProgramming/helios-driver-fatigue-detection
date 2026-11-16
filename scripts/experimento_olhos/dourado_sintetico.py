"""Teste dourado sintético do recorte do olho: prova que o app recorta igual ao experimento.

Gera imagens desenhadas (um olho estilizado sobre um fundo com textura, nenhum rosto de pessoa) com os
16 pontos do contorno em posições conhecidas. Para cada caso grava a imagem, o recorte 64x64 que o
`recortar_olho` do experimento produz e a probabilidade do modelo do app (só como registro). O teste
OlhoCnnDouradoTest, no módulo protocolos do app, lê esses arquivos e exige o mesmo recorte.

Como as imagens são desenhadas, os arquivos podem ir para o repositório (a CEW não pode).

Uso:
    python scripts/experimento_olhos/dourado_sintetico.py
"""
from __future__ import annotations

import argparse
import math
from pathlib import Path

import cv2
import numpy as np
import pandas as pd

from comum import CONTORNO_DIREITO_IMAGEM, OLHO_DIREITO_IMAGEM
from extrair import recortar_olho

RAIZ = Path(__file__).resolve().parents[2]
SAIDA_PADRAO = RAIZ / "Aplicativo-Android/helios-android-v2/protocolos/src/test/resources/dourado_olho"
MODELO_APP = RAIZ / "Aplicativo-Android/helios-android-v2/app/src/main/assets/olho_cnn.tflite"

# nome: (largura, altura, centro x, centro y, meia largura do olho, meia altura, inclinação em graus, colorido)
CASOS = {
    "reducao_inteira": (320, 240, 160, 120, 40.0, 14.0, 0.0, False),  # lado 128 px: fator 2
    "reducao_quebrada": (320, 240, 150, 110, 31.0, 11.0, 0.0, False),  # lado ~99 px
    "borda_esquerda_topo": (320, 240, 30, 20, 35.0, 12.0, 0.0, False),  # recorte sai pela esquerda e por cima
    "borda_direita_baixo": (320, 240, 300, 228, 35.0, 12.0, 0.0, False),  # sai pela direita e por baixo
    "ampliacao": (320, 240, 160, 120, 12.0, 4.5, 0.0, False),  # lado ~38 px: olho pequeno, ampliado para 64
    "colorido": (320, 240, 170, 125, 33.0, 12.0, 0.0, True),  # confere a luminância em R, G e B diferentes
    "inclinado": (320, 240, 160, 120, 36.0, 12.0, 20.0, False),  # cabeça inclinada
}


def desenhar(largura, altura, cx, cy, a, b, graus, colorido, semente):
    rng = np.random.default_rng(semente)
    gy, gx = np.mgrid[0:altura, 0:largura]
    # Ruído suave (sorteado em 1/8 da resolução e ampliado): testa o arredondamento sem inflar o PNG.
    ruido = cv2.resize(rng.normal(0, 25, (altura // 8, largura // 8)), (largura, altura), interpolation=cv2.INTER_LINEAR)
    fundo = 90 + 60 * np.sin(gx / 17.0) * np.cos(gy / 23.0) + ruido
    imagem = np.stack([fundo * 0.9, fundo, fundo * 1.1], axis=-1)
    if colorido:
        imagem[..., 0] += 40 * np.sin(gx / 9.0)  # canal B
        imagem[..., 2] -= 30 * np.cos(gy / 7.0)  # canal R
    imagem = np.clip(imagem, 0, 255).astype(np.uint8)
    centro = (int(round(cx)), int(round(cy)))
    cv2.ellipse(imagem, centro, (int(a), int(b)), graus, 0, 360, (225, 230, 235), -1)  # esclera
    cv2.circle(imagem, centro, max(2, int(b * 0.9)), (40, 70, 110), -1)  # íris
    cv2.circle(imagem, centro, max(1, int(b * 0.4)), (10, 10, 10), -1)  # pupila
    cv2.ellipse(imagem, centro, (int(a), int(b)), graus, 180, 360, (30, 25, 20), 2)  # pálpebra de cima
    return imagem


def pontos_do_contorno(cx, cy, a, b, graus):
    """16 pontos na elipse, na ordem do CONTORNO_DIREITO_IMAGEM: canto 33 em 0 rad, canto 133 em pi."""
    t = math.radians(graus)
    pontos = []
    for k in range(16):
        ang = math.pi * k / 8
        x, y = a * math.cos(ang), b * math.sin(ang)
        pontos.append((cx + x * math.cos(t) - y * math.sin(t), cy + x * math.sin(t) + y * math.cos(t)))
    return pontos


def probabilidade(recorte: np.ndarray) -> float:
    import tensorflow as tf

    interprete = tf.lite.Interpreter(model_path=str(MODELO_APP))
    interprete.allocate_tensors()
    interprete.set_tensor(interprete.get_input_details()[0]["index"], recorte.reshape(1, 64, 64, 1))
    interprete.invoke()
    return float(interprete.get_tensor(interprete.get_output_details()[0]["index"]).reshape(-1)[0])


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--saida", type=Path, default=SAIDA_PADRAO)
    parser.add_argument("--sem-modelo", action="store_true", help="não roda o TFLite (sem TensorFlow instalado)")
    args = parser.parse_args()
    args.saida.mkdir(parents=True, exist_ok=True)

    linhas = []
    for semente, (nome, (w, h, cx, cy, a, b, graus, colorido)) in enumerate(CASOS.items()):
        imagem = desenhar(w, h, cx, cy, a, b, graus, colorido, semente)
        contorno = pontos_do_contorno(cx, cy, a, b, graus)
        pontos_px = np.zeros((478, 2))
        for indice, ponto in zip(CONTORNO_DIREITO_IMAGEM, contorno):
            pontos_px[indice] = ponto
        cinza = cv2.cvtColor(imagem, cv2.COLOR_BGR2GRAY)
        recorte = recortar_olho(cinza, pontos_px, CONTORNO_DIREITO_IMAGEM, OLHO_DIREITO_IMAGEM[0], OLHO_DIREITO_IMAGEM[3])
        cv2.imwrite(str(args.saida / f"{nome}.png"), imagem)
        cv2.imwrite(str(args.saida / f"{nome}_esperado.png"), recorte)
        linha = {"caso": nome}
        for k, (x, y) in enumerate(contorno):
            linha[f"x{k}"], linha[f"y{k}"] = round(x, 6), round(y, 6)
        linha["prob_fechado"] = float("nan") if args.sem_modelo else round(probabilidade(recorte), 6)
        linhas.append(linha)
    pd.DataFrame(linhas).to_csv(args.saida / "dourado.csv", index=False)
    print(f"{len(linhas)} casos em {args.saida}")


if __name__ == "__main__":
    main()
