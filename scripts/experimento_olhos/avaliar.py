"""Passo 4: compara os classificadores na partição de teste.

Classificadores (positivo = olho fechado):
  1. EAR < 0,19                 limiar do app (Medina, 2025)
  2. EAR < limiar ótimo         limiar de Youden escolhido na validação, aplicado no teste
  3. ECF > 0,40                 blendshape eyeBlink do MediaPipe, limiar do app
  4. Regra do app               EAR < 0,19 ou ECF > 0,40 (critério de olho fechado do HELIOS)
  5. CNN do grupo               probabilidade média dos dois olhos acima do limiar de Youden da validação

Saídas em scripts/experimento_olhos/resultados/: metricas.csv, metricas.md, deteccao.csv,
matriz_confusao.png e curvas_roc.png. Nenhuma imagem de rosto é gravada ali.

Uso:
    python scripts/experimento_olhos/avaliar.py
    python scripts/experimento_olhos/avaliar.py --sem-cnn
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd

from comum import (
    LIMIAR_ECF_APP,
    LIMIAR_EAR_APP,
    PASTA_MODELOS,
    PASTA_PROCESSADOS,
    PASTA_RESULTADOS,
    calcular_metricas,
    limiar_youden,
)



def _num(valor: float, casas: int) -> str:
    return f"{valor:.{casas}f}".replace(".", ",")


def previsoes(tabela: pd.DataFrame, limiar_ear_otimo: float, limiar_cnn: float = 0.5) -> dict[str, np.ndarray]:
    """Decisão de cada classificador para as linhas com rosto detectado."""
    ear = tabela["ear_medio"].to_numpy()
    ecf = tabela["ecf_medio"].to_numpy()
    saida = {
        f"EAR < {_num(LIMIAR_EAR_APP, 2)} (app)": ear < LIMIAR_EAR_APP,
        f"EAR < {_num(limiar_ear_otimo, 3)} (Youden na validação)": ear < limiar_ear_otimo,
        f"ECF > {_num(LIMIAR_ECF_APP, 2)} (app)": ecf > LIMIAR_ECF_APP,
        "Regra do app (EAR ou ECF)": (ear < LIMIAR_EAR_APP) | (ecf > LIMIAR_ECF_APP),
    }
    if "prob_cnn" in tabela:
        saida["CNN do grupo"] = tabela["prob_cnn"].to_numpy() >= limiar_cnn
    return saida


def tabela_metricas(tabela: pd.DataFrame, limiar_ear_otimo: float, limiar_cnn: float = 0.5) -> pd.DataFrame:
    com_rosto = tabela[tabela["rosto"] == 1]
    verdadeiro = com_rosto["rotulo"].to_numpy()
    tempo_mp = float(com_rosto["tempo_mediapipe_ms"].mean())
    tempo_cnn = float(com_rosto["tempo_cnn_ms"].mean()) if "tempo_cnn_ms" in com_rosto else float("nan")
    linhas = []
    for nome, previsto in previsoes(com_rosto, limiar_ear_otimo, limiar_cnn).items():
        tempo = tempo_mp + (tempo_cnn if nome == "CNN do grupo" else 0.0)
        linhas.append({"classificador": nome, **calcular_metricas(verdadeiro, previsto).como_dict(), "tempo_ms": tempo})
    return pd.DataFrame(linhas)


def tabela_deteccao(tabela: pd.DataFrame) -> pd.DataFrame:
    """Em quantas imagens o MediaPipe achou o rosto. Sem rosto, nenhum classificador decide."""
    grupos = tabela.groupby("rotulo")["rosto"].agg(["size", "sum", "mean"])
    grupos.index = grupos.index.map({0: "aberto", 1: "fechado"})
    grupos.columns = ["imagens", "com_rosto", "taxa_deteccao"]
    return grupos


def markdown(metricas: pd.DataFrame) -> str:
    def pct(v: float) -> str:
        return "—" if np.isnan(v) else f"{v * 100:.1f}%".replace(".", ",")

    def ms(v: float) -> str:
        return "—" if np.isnan(v) else f"{v:.1f}".replace(".", ",")

    linhas = [
        "| Classificador | Acurácia | Precisão | Sensibilidade | Especificidade | F1 | Tempo/imagem (ms) |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for m in metricas.itertuples(index=False):
        linhas.append(
            f"| {m.classificador} | {pct(m.acuracia)} | {pct(m.precisao)} | {pct(m.sensibilidade)} | "
            f"{pct(m.especificidade)} | {pct(m.f1)} | {ms(m.tempo_ms)} |"
        )
    return "\n".join(linhas) + "\n"


def salvar_matrizes(tabela: pd.DataFrame, limiar_ear_otimo: float, limiar_cnn: float, destino: Path) -> None:
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    com_rosto = tabela[tabela["rosto"] == 1]
    verdadeiro = com_rosto["rotulo"].to_numpy()
    itens = list(previsoes(com_rosto, limiar_ear_otimo, limiar_cnn).items())
    figura, eixos = plt.subplots(1, len(itens), figsize=(3.6 * len(itens), 3.6))
    for eixo, (nome, previsto) in zip(np.atleast_1d(eixos), itens):
        m = calcular_metricas(verdadeiro, previsto)
        matriz = np.array([[m.vn, m.fp], [m.fn, m.vp]])
        eixo.imshow(matriz, cmap="Blues")
        for (i, j), valor in np.ndenumerate(matriz):
            eixo.text(j, i, str(valor), ha="center", va="center", color="white" if valor > matriz.max() / 2 else "black")
        eixo.set_xticks([0, 1], ["aberto", "fechado"])
        eixo.set_yticks([0, 1], ["aberto", "fechado"])
        eixo.set_xlabel("Previsto")
        eixo.set_ylabel("Real")
        eixo.set_title(nome, fontsize=9)
    figura.tight_layout()
    figura.savefig(destino, dpi=150)
    plt.close(figura)


def salvar_roc(tabela: pd.DataFrame, destino: Path) -> dict[str, float]:
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from sklearn.metrics import roc_auc_score, roc_curve

    com_rosto = tabela[tabela["rosto"] == 1]
    verdadeiro = com_rosto["rotulo"].to_numpy()
    pontuacoes = {"EAR (invertido)": -com_rosto["ear_medio"].to_numpy(), "ECF": com_rosto["ecf_medio"].to_numpy()}
    if "prob_cnn" in com_rosto:
        pontuacoes["CNN do grupo"] = com_rosto["prob_cnn"].to_numpy()
    figura, eixo = plt.subplots(figsize=(5, 5))
    aucs = {}
    for nome, pontuacao in pontuacoes.items():
        fpr, tpr, _ = roc_curve(verdadeiro, pontuacao)
        aucs[nome] = float(roc_auc_score(verdadeiro, pontuacao))
        eixo.plot(fpr, tpr, label=f"{nome} (AUC {aucs[nome]:.3f})".replace(".", ","))
    eixo.plot([0, 1], [0, 1], linestyle="--", color="gray")
    eixo.set_xlabel("1 − especificidade")
    eixo.set_ylabel("Sensibilidade")
    eixo.legend(loc="lower right")
    figura.tight_layout()
    figura.savefig(destino, dpi=150)
    plt.close(figura)
    return aucs


def juntar_cnn(tabela: pd.DataFrame, caminho_modelo: Path) -> pd.DataFrame:
    from tensorflow import keras

    from treinar_cnn import prever

    modelo = keras.models.load_model(caminho_modelo)
    return tabela.merge(prever(modelo, tabela), on="id", how="left")


def avaliar(tabela: pd.DataFrame, pasta: Path, caminho_modelo: Path | None) -> pd.DataFrame:
    # Limiares escolhidos só com a validação; o teste não participa de nenhuma escolha.
    validacao = tabela[(tabela["particao"] == "validacao") & (tabela["rosto"] == 1)].copy()
    rotulos_validacao = validacao["rotulo"].to_numpy()
    limiar_ear_otimo = limiar_youden(validacao["ear_medio"].to_numpy(), rotulos_validacao, maior_e_fechado=False)
    teste = tabela[tabela["particao"] == "teste"].copy()
    limiar_cnn = 0.5
    if caminho_modelo is not None:
        validacao = juntar_cnn(validacao, caminho_modelo)
        limiar_cnn = limiar_youden(validacao["prob_cnn"].to_numpy(), rotulos_validacao, maior_e_fechado=True)
        teste = juntar_cnn(teste, caminho_modelo)

    pasta.mkdir(parents=True, exist_ok=True)
    metricas = tabela_metricas(teste, limiar_ear_otimo, limiar_cnn)
    metricas.to_csv(pasta / "metricas.csv", index=False)
    (pasta / "metricas.md").write_text(markdown(metricas), encoding="utf-8")
    tabela_deteccao(teste).to_csv(pasta / "deteccao.csv")
    salvar_matrizes(teste, limiar_ear_otimo, limiar_cnn, pasta / "matriz_confusao.png")
    aucs = salvar_roc(teste, pasta / "curvas_roc.png")
    resumo = {
        "limiar_ear_youden_validacao": limiar_ear_otimo,
        "limiar_cnn_youden_validacao": limiar_cnn,
        "auc": aucs,
        "imagens_teste": int(len(teste)),
    }
    (pasta / "resumo.json").write_text(json.dumps(resumo, indent=2, ensure_ascii=False), encoding="utf-8")
    return metricas


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--caracteristicas", type=Path, default=PASTA_PROCESSADOS / "caracteristicas.csv")
    parser.add_argument("--modelo", type=Path, default=PASTA_MODELOS / "cnn_olhos.keras")
    parser.add_argument("--sem-cnn", action="store_true", help="avaliar só EAR e ECF")
    parser.add_argument("--saida", type=Path, default=PASTA_RESULTADOS)
    args = parser.parse_args()

    tabela = pd.read_csv(args.caracteristicas)
    metricas = avaliar(tabela, args.saida, None if args.sem_cnn else args.modelo)
    print(markdown(metricas))
    print(f"Resultados em {args.saida}")


if __name__ == "__main__":
    main()
