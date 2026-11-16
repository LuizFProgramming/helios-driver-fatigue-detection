"""Conjunto de teste real: fotos originais do app (640x480) passadas pelo mesmo recorte do treino.

As fotos de alerta e de teste que o HELIOS guarda (as sem "-analise") são a câmera de verdade: ângulo, luz,
óculos. O rótulo vem do EAR do MediaPipe na própria foto: fechado abaixo de 0,15, aberto acima de 0,22; o meio
fica de fora por ser duvidoso. Serve para medir um modelo nas condições do carro, não para treinar sozinho.

Uso:
    python scripts/experimento_olhos/fotos_reais.py PASTA_DAS_FOTOS [--modelo models/olho_cnn.tflite]

Grava data/processed/experimento_olhos/reais/ (recortes e reais.csv; a pasta data/ não vai para o git).
"""

from __future__ import annotations

import argparse
from pathlib import Path

import cv2
import numpy as np
import pandas as pd

from comum import CORTE_CNN, MODELO_MEDIAPIPE, PASTA_MODELOS, PASTA_PROCESSADOS, avaliar_probabilidades
from extrair import criar_detector, extrair_imagem

EAR_FECHADO = 0.15
EAR_ABERTO = 0.22


def rotulo_por_ear(ear: float) -> int | None:
    """1 = fechado, 0 = aberto, None = duvidoso (fica fora do teste)."""
    if ear < EAR_FECHADO:
        return 1
    if ear > EAR_ABERTO:
        return 0
    return None


def coletar(pasta: Path, saida: Path) -> pd.DataFrame:
    mp, detector = criar_detector(MODELO_MEDIAPIPE)
    saida.mkdir(parents=True, exist_ok=True)
    linhas = []
    fotos = sorted(p for p in pasta.rglob("*.jp*g") if "-analise" not in p.name and "WhatsApp" not in p.name)
    for foto in fotos:
        imagem = cv2.imread(str(foto))
        if imagem is None:
            continue
        medidas, direito, esquerdo = extrair_imagem(mp, detector, imagem)
        for lado, recorte in (("dir", direito), ("esq", esquerdo)):
            if recorte is None:
                continue
            ear = float(medidas[f"ear_{lado}"])
            caminho = saida / f"{foto.stem}__{lado}.png"
            cv2.imwrite(str(caminho), recorte)
            linhas.append({"foto": foto.name, "olho": lado, "ear": ear, "rotulo": rotulo_por_ear(ear),
                           "recorte": str(caminho), "contraste": float(recorte.std())})
    return pd.DataFrame(linhas)


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("pasta", type=Path)
    parser.add_argument("--modelo", type=Path, default=PASTA_MODELOS / "olho_cnn.tflite")
    args = parser.parse_args(argv)

    from exportar_tflite import prever_tflite

    saida = PASTA_PROCESSADOS / "reais"
    tabela = coletar(args.pasta, saida)
    imagens = np.stack([cv2.imread(c, cv2.IMREAD_GRAYSCALE)[..., np.newaxis] for c in tabela["recorte"]])
    tabela["prob_fechado"] = prever_tflite(args.modelo.read_bytes(), imagens)
    tabela.to_csv(saida / "reais.csv", index=False)

    rotulados = tabela[tabela["rotulo"].notna()]
    abertos = rotulados[rotulados["rotulo"] == 0]
    fechados = rotulados[rotulados["rotulo"] == 1]
    print(f"{tabela['foto'].nunique()} fotos, {len(tabela)} olhos; {len(abertos)} abertos, {len(fechados)} fechados, "
          f"{len(tabela) - len(rotulados)} duvidosos")
    print(f"contraste médio do recorte (desvio padrão do cinza): {tabela['contraste'].mean():.1f}")
    print(f"abertos chamados de fechado: {(abertos['prob_fechado'] >= CORTE_CNN).mean():.0%} "
          f"(mediana da probabilidade {abertos['prob_fechado'].median():.2f})")
    if len(fechados):
        print(f"fechados reconhecidos: {(fechados['prob_fechado'] >= CORTE_CNN).mean():.0%}")
        print(avaliar_probabilidades(rotulados["rotulo"].to_numpy(int), rotulados["prob_fechado"].to_numpy()))


if __name__ == "__main__":
    main()
