"""Passo 5: mede quanto cada classificador piora em condições adversas.

Gera versões alteradas das imagens de teste e repete extração e avaliação:
  - pouca_luz:       imagem escurecida com curva gama, como a cabine à noite sem IR
  - sombra:          metade do rosto escurecida em degradê, como sol lateral
  - ruido_desfoque:  desfoque, ruído de sensor e JPEG forte, como o stream da ESP32-CAM
  - gerada_ia:       (opcional) imagens produzidas por um modelo generativo a partir das de teste,
                     com os mesmos nomes de arquivo, passadas em --pasta-gerada (decisão B do grupo)

As três primeiras são alterações fotométricas determinísticas, não IA generativa. Só a quarta
pode ser descrita no TCC como uso de IA generativa.

Uso (depois de avaliar.py, que grava o limiar de Youden usado aqui):
    python scripts/experimento_olhos/robustez.py
    python scripts/experimento_olhos/robustez.py --pasta-gerada data/synthetic/olhos_ia
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

import cv2
import numpy as np
import pandas as pd

from comum import PASTA_MODELOS, PASTA_PROCESSADOS, PASTA_RESULTADOS, SEMENTE

COLUNAS = ["condicao", "classificador", "taxa_deteccao", "acuracia", "sensibilidade", "especificidade", "f1"]


def pouca_luz(imagem: np.ndarray) -> np.ndarray:
    normalizada = imagem.astype("float32") / 255.0
    return np.clip(255.0 * 0.6 * normalizada**2.2, 0, 255).astype("uint8")


def sombra(imagem: np.ndarray) -> np.ndarray:
    largura = imagem.shape[1]
    fator = np.clip(np.linspace(0.25, 1.4, largura), 0.25, 1.0).astype("float32")
    return np.clip(imagem.astype("float32") * fator[np.newaxis, :, np.newaxis], 0, 255).astype("uint8")


def ruido_desfoque(imagem: np.ndarray, gerador: np.random.Generator) -> np.ndarray:
    borrada = cv2.GaussianBlur(imagem, (5, 5), 1.2).astype("float32")
    com_ruido = np.clip(borrada + gerador.normal(0.0, 12.0, imagem.shape), 0, 255).astype("uint8")
    ok, jpeg = cv2.imencode(".jpg", com_ruido, [cv2.IMWRITE_JPEG_QUALITY, 25])
    return cv2.imdecode(jpeg, cv2.IMREAD_COLOR) if ok else com_ruido


def gerar_condicao(teste: pd.DataFrame, condicao: str, pasta: Path) -> pd.DataFrame:
    gerador = np.random.default_rng(SEMENTE)
    caminhos = []
    for linha in teste.itertuples(index=False):
        imagem = cv2.imread(linha.caminho)
        if condicao == "pouca_luz":
            alterada = pouca_luz(imagem)
        elif condicao == "sombra":
            alterada = sombra(imagem)
        else:
            alterada = ruido_desfoque(imagem, gerador)
        destino = pasta / linha.id
        destino.parent.mkdir(parents=True, exist_ok=True)
        cv2.imwrite(str(destino), alterada)
        caminhos.append(str(destino))
    return teste.assign(caminho=caminhos)


def apontar_pasta_gerada(teste: pd.DataFrame, pasta_gerada: Path) -> pd.DataFrame:
    caminhos = [pasta_gerada / i for i in teste["id"]]
    faltando = [str(c) for c in caminhos if not c.exists()]
    if faltando:
        raise FileNotFoundError(f"{len(faltando)} imagens geradas não encontradas, por exemplo {faltando[0]}")
    return teste.assign(caminho=[str(c) for c in caminhos])


def main() -> None:
    from avaliar import juntar_cnn, tabela_metricas
    from extrair import extrair_lote

    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--manifesto", type=Path, default=PASTA_PROCESSADOS / "manifesto.csv")
    parser.add_argument("--modelo", type=Path, default=PASTA_MODELOS / "cnn_olhos.keras")
    parser.add_argument("--sem-cnn", action="store_true")
    parser.add_argument("--pasta-gerada", type=Path, help="imagens de teste alteradas por IA generativa")
    args = parser.parse_args()

    resumo = json.loads((PASTA_RESULTADOS / "resumo.json").read_text(encoding="utf-8"))
    limiar_ear_otimo = float(resumo["limiar_ear_youden_validacao"])
    limiar_cnn = float(resumo.get("limiar_cnn_youden_validacao", 0.5))
    teste = pd.read_csv(args.manifesto).query("particao == 'teste'")
    base = PASTA_PROCESSADOS / "robustez"

    condicoes: dict[str, pd.DataFrame] = {c: gerar_condicao(teste, c, base / c / "imagens") for c in ("pouca_luz", "sombra", "ruido_desfoque")}
    if args.pasta_gerada:
        condicoes["gerada_ia"] = apontar_pasta_gerada(teste, args.pasta_gerada)

    linhas = []
    for condicao, manifesto in condicoes.items():
        print(f"Condição {condicao}...")
        tabela = extrair_lote(manifesto, base / condicao / "recortes")
        if not args.sem_cnn:
            tabela = juntar_cnn(tabela, args.modelo)
        deteccao = float(tabela["rosto"].mean())
        for m in tabela_metricas(tabela, limiar_ear_otimo, limiar_cnn).itertuples(index=False):
            linhas.append([condicao, m.classificador, deteccao, m.acuracia, m.sensibilidade, m.especificidade, m.f1])

    resultado = pd.DataFrame(linhas, columns=COLUNAS)
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)
    resultado.to_csv(PASTA_RESULTADOS / "robustez.csv", index=False)
    print(resultado.to_string(index=False, float_format=lambda v: f"{v:.3f}"))


if __name__ == "__main__":
    main()
