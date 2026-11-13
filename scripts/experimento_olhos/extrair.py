"""Passo 2: roda o MediaPipe Face Landmarker em cada imagem, como o app faz.

Para cada imagem grava: se achou o rosto, EAR de cada olho (em pixels, mesma fórmula e mesmos pontos
do app), ECF de cada olho (blendshapes eyeBlinkLeft/Right), tempo do MediaPipe e o recorte de cada olho
em escala de cinza, que é a entrada da CNN.

Uso:
    python scripts/experimento_olhos/extrair.py
    python scripts/experimento_olhos/extrair.py --manifesto outro.csv --saida outra_pasta
"""
from __future__ import annotations

import argparse
import time
from pathlib import Path

import cv2
import numpy as np
import pandas as pd

from comum import (
    CONTORNO_DIREITO_IMAGEM,
    CONTORNO_ESQUERDO_IMAGEM,
    MODELO_MEDIAPIPE,
    OLHO_DIREITO_IMAGEM,
    OLHO_ESQUERDO_IMAGEM,
    PASTA_PROCESSADOS,
    TAMANHO_RECORTE,
    razao_aspecto,
)

# Lado menor mínimo antes de mandar para o MediaPipe. O CEW tem rostos de 100x100 pixels,
# pequenos demais para a malha; ampliar aproxima do tamanho de rosto que a câmera do carro entrega.
LADO_MINIMO = 256
MARGEM_RECORTE = 1.6
COLUNAS_MEDIDAS = (
    "ear_dir", "ear_esq", "ear_medio", "ecf_dir", "ecf_esq", "ecf_medio", "recorte_dir", "recorte_esq",
)


def criar_detector(modelo: Path):
    import mediapipe as mp
    from mediapipe.tasks import python as mp_tasks
    from mediapipe.tasks.python import vision

    if not modelo.exists():
        raise FileNotFoundError(f"modelo do MediaPipe não encontrado em {modelo} (veja models/README.md)")
    opcoes = vision.FaceLandmarkerOptions(
        base_options=mp_tasks.BaseOptions(model_asset_path=str(modelo)),
        running_mode=vision.RunningMode.IMAGE,
        output_face_blendshapes=True,
        num_faces=1,
    )
    return mp, vision.FaceLandmarker.create_from_options(opcoes)


def ampliar(imagem_bgr: np.ndarray) -> np.ndarray:
    altura, largura = imagem_bgr.shape[:2]
    fator = LADO_MINIMO / min(altura, largura)
    if fator <= 1.0:
        return imagem_bgr
    return cv2.resize(imagem_bgr, None, fx=fator, fy=fator, interpolation=cv2.INTER_CUBIC)


def recortar_olho(cinza: np.ndarray, pontos_px: np.ndarray, contorno: tuple[int, ...], p1: int, p4: int) -> np.ndarray:
    """Quadrado centrado no olho, com lado = MARGEM_RECORTE x largura do olho, redimensionado para a CNN."""
    centro = pontos_px[list(contorno)].mean(axis=0)
    largura_olho = float(np.linalg.norm(pontos_px[p1] - pontos_px[p4]))
    meio = max(4.0, MARGEM_RECORTE * largura_olho / 2.0)
    x0, y0 = int(round(centro[0] - meio)), int(round(centro[1] - meio))
    x1, y1 = int(round(centro[0] + meio)), int(round(centro[1] + meio))
    altura, largura = cinza.shape
    preenchida = cv2.copyMakeBorder(
        cinza, max(0, -y0), max(0, y1 - altura), max(0, -x0), max(0, x1 - largura), cv2.BORDER_REPLICATE
    )
    dx, dy = max(0, -x0), max(0, -y0)
    recorte = preenchida[y0 + dy : y1 + dy, x0 + dx : x1 + dx]
    return cv2.resize(recorte, (TAMANHO_RECORTE, TAMANHO_RECORTE), interpolation=cv2.INTER_AREA)


def extrair_imagem(mp, detector, imagem_bgr: np.ndarray) -> tuple[dict[str, float], np.ndarray | None, np.ndarray | None]:
    imagem_bgr = ampliar(imagem_bgr)
    rgb = cv2.cvtColor(imagem_bgr, cv2.COLOR_BGR2RGB)
    inicio = time.perf_counter()
    resultado = detector.detect(mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb))
    tempo_ms = (time.perf_counter() - inicio) * 1000.0
    if not resultado.face_landmarks:
        return {"rosto": 0, "tempo_mediapipe_ms": tempo_ms}, None, None

    altura, largura = rgb.shape[:2]
    pontos_px = np.array([[p.x * largura, p.y * altura] for p in resultado.face_landmarks[0]], dtype=float)
    blend = {c.category_name: float(c.score) for c in resultado.face_blendshapes[0]} if resultado.face_blendshapes else {}
    ear_dir = razao_aspecto(pontos_px[list(OLHO_DIREITO_IMAGEM)])
    ear_esq = razao_aspecto(pontos_px[list(OLHO_ESQUERDO_IMAGEM)])
    # Mesmo pareamento do MapeadorFaceMesh.kt. O experimento decide pela média dos dois olhos.
    ecf_dir = blend.get("eyeBlinkRight", np.nan)
    ecf_esq = blend.get("eyeBlinkLeft", np.nan)
    cinza = cv2.cvtColor(imagem_bgr, cv2.COLOR_BGR2GRAY)
    recorte_dir = recortar_olho(cinza, pontos_px, CONTORNO_DIREITO_IMAGEM, OLHO_DIREITO_IMAGEM[0], OLHO_DIREITO_IMAGEM[3])
    recorte_esq = recortar_olho(cinza, pontos_px, CONTORNO_ESQUERDO_IMAGEM, OLHO_ESQUERDO_IMAGEM[0], OLHO_ESQUERDO_IMAGEM[3])
    medidas = {
        "rosto": 1,
        "tempo_mediapipe_ms": tempo_ms,
        "ear_dir": ear_dir,
        "ear_esq": ear_esq,
        "ear_medio": (ear_dir + ear_esq) / 2.0,
        "ecf_dir": ecf_dir,
        "ecf_esq": ecf_esq,
        "ecf_medio": (ecf_dir + ecf_esq) / 2.0,
    }
    return medidas, recorte_dir, recorte_esq


def extrair_lote(manifesto: pd.DataFrame, pasta_recortes: Path, modelo: Path = MODELO_MEDIAPIPE) -> pd.DataFrame:
    mp, detector = criar_detector(modelo)
    pasta_recortes.mkdir(parents=True, exist_ok=True)
    linhas = []
    for i, linha in enumerate(manifesto.itertuples(index=False), start=1):
        imagem = cv2.imread(linha.caminho)
        if imagem is None:
            medidas, rd, re_ = {"rosto": 0, "tempo_mediapipe_ms": np.nan}, None, None
        else:
            medidas, rd, re_ = extrair_imagem(mp, detector, imagem)
        base = linha.id.replace("/", "__").rsplit(".", 1)[0]
        if rd is not None and re_ is not None:
            medidas["recorte_dir"] = str(pasta_recortes / f"{base}__dir.png")
            medidas["recorte_esq"] = str(pasta_recortes / f"{base}__esq.png")
            cv2.imwrite(medidas["recorte_dir"], rd)
            cv2.imwrite(medidas["recorte_esq"], re_)
        linhas.append({**linha._asdict(), **medidas})
        if i % 200 == 0:
            print(f"  {i}/{len(manifesto)} imagens")
    detector.close()
    tabela = pd.DataFrame(linhas)
    # Garante as colunas mesmo quando nenhum rosto foi encontrado (ex.: condição muito escura).
    for coluna in COLUNAS_MEDIDAS:
        if coluna not in tabela:
            tabela[coluna] = np.nan
    return tabela


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--manifesto", type=Path, default=PASTA_PROCESSADOS / "manifesto.csv")
    parser.add_argument("--saida", type=Path, default=PASTA_PROCESSADOS)
    parser.add_argument("--modelo", type=Path, default=MODELO_MEDIAPIPE)
    args = parser.parse_args()

    manifesto = pd.read_csv(args.manifesto)
    tabela = extrair_lote(manifesto, args.saida / "recortes", args.modelo)
    destino = args.saida / "caracteristicas.csv"
    tabela.to_csv(destino, index=False)
    taxa = tabela["rosto"].mean() * 100
    print(f"Rosto encontrado em {taxa:.1f}% das {len(tabela)} imagens -> {destino}")


if __name__ == "__main__":
    main()
