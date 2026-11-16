"""Retreino da CNN do olho para a câmera real (ADR-016): óculos, contraluz, pouco contraste, câmera de baixo.

Mesma rede `media` e mesma entrada do modelo do app (64x64x1, cinza de 0 a 255), para trocar só o arquivo.
Difere do treinar_cnn.py em três pontos:
  - os aumentos de `aumentos.py` em cada época, iguais para olho aberto e fechado;
  - a MRL Eye entra no treino, com prioridade para os recortes com óculos ou reflexo (4º e 6º campos do nome);
  - a validação tem a CEW limpa e duas cópias aumentadas, para a escolha da época não favorecer só a foto limpa.

Depois compara o modelo do app com o novo no teste da CEW (limpo, aumentado, só óculos) e nas fotos reais do
app (`fotos_reais.py`), e grava resultados/real.md.

Uso:
    python scripts/experimento_olhos/treinar_real.py [--epocas 25] [--mrl data/raw/MRL] [--max-mrl 24000]
"""

from __future__ import annotations

import argparse
from pathlib import Path

import cv2
import numpy as np
import pandas as pd

from aumentos import aumentar_lote, aumentar_real, oculos
from comum import (
    PASTA_DADOS,
    PASTA_MODELOS,
    PASTA_PROCESSADOS,
    PASTA_RESULTADOS,
    SEMENTE,
    TAMANHO_RECORTE,
    avaliar_probabilidades,
    rotulo_mrl,
)
from treinar_cnn import LOTE, carregar_recortes, compilar, construir_modelo


def escolher_mrl(arquivos: list[Path], maximo: int, rng: np.random.Generator) -> list[Path]:
    """Até [maximo] recortes da MRL: primeiro os com óculos ou reflexo, depois os outros ao acaso."""
    def especial(a: Path) -> bool:
        campos = a.stem.split("_")
        return len(campos) >= 6 and (campos[3] == "1" or campos[5] in ("1", "2"))

    especiais = [a for a in arquivos if especial(a)]
    comuns = [a for a in arquivos if not especial(a)]
    rng.shuffle(especiais)
    rng.shuffle(comuns)
    metade = (maximo * 2) // 3
    escolhidos = especiais[:metade]
    return escolhidos + comuns[: maximo - len(escolhidos)]


def carregar_mrl(pasta: Path, maximo: int, rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray]:
    arquivos = escolher_mrl(sorted(pasta.rglob("*.png")), maximo, rng)
    lado = (TAMANHO_RECORTE, TAMANHO_RECORTE)
    imagens = np.stack([
        cv2.resize(cv2.imread(str(a), cv2.IMREAD_GRAYSCALE), lado, interpolation=cv2.INTER_AREA) for a in arquivos
    ])
    return imagens[..., np.newaxis], np.array([rotulo_mrl(a.name) for a in arquivos], dtype="float32")


def sequencia(x: np.ndarray, y: np.ndarray, pesos: dict[int, float]):
    """Lotes embaralhados a cada época, com os aumentos reais refeitos toda vez."""
    from tensorflow import keras

    class Lotes(keras.utils.PyDataset):
        def __init__(self):
            super().__init__()
            self.rng = np.random.default_rng(SEMENTE)
            self.ordem = self.rng.permutation(len(x))

        def __len__(self):
            return -(-len(x) // LOTE)

        def __getitem__(self, i):
            idx = self.ordem[i * LOTE:(i + 1) * LOTE]
            lote = np.stack([aumentar_real(x[j], self.rng) for j in idx]).astype("float32")
            rotulos = y[idx]
            return lote, rotulos, np.where(rotulos == 1, pesos[1], pesos[0]).astype("float32")

        def on_epoch_end(self):
            self.ordem = self.rng.permutation(len(x))

    return Lotes()


def prever_tflite(conteudo: bytes, imagens: np.ndarray) -> np.ndarray:
    from exportar_tflite import prever_tflite as prever

    return prever(conteudo, imagens)


def comparar(modelos: dict[str, bytes], teste_x: np.ndarray, teste_y: np.ndarray, reais: np.ndarray | None) -> pd.DataFrame:
    """Uma linha por modelo e condição. Nas fotos reais só há olhos abertos: a medida é o falso fechado."""
    condicoes = {
        "CEW limpo": teste_x.astype(np.uint8),
        "CEW com os aumentos": aumentar_lote(teste_x, 99),
        "CEW só com óculos": aumentar_lote(teste_x, 98, oculos),
    }
    linhas = []
    for nome, conteudo in modelos.items():
        for condicao, imagens in condicoes.items():
            m = avaliar_probabilidades(teste_y.astype(int), prever_tflite(conteudo, imagens))
            abertos = prever_tflite(conteudo, imagens[teste_y == 0])
            linhas.append({"modelo": nome, "condicao": condicao, "f1": m["f1"], "auc": m["auc"],
                           "falso_fechado": float((abertos >= 0.5).mean())})
        if reais is not None:
            prob = prever_tflite(conteudo, reais)
            linhas.append({"modelo": nome, "condicao": f"fotos reais do app ({len(reais)} olhos abertos)",
                           "f1": float("nan"), "auc": float("nan"), "falso_fechado": float((prob >= 0.5).mean())})
    return pd.DataFrame(linhas)


def tabela_md(df: pd.DataFrame) -> str:
    def celula(v):
        return f"{v:.3f}" if isinstance(v, float) else str(v)

    linhas = ["| " + " | ".join(df.columns) + " |", "|" + "---|" * len(df.columns)]
    linhas += ["| " + " | ".join(celula(v) for v in linha) + " |" for linha in df.itertuples(index=False)]
    return "\n".join(linhas)


def carregar_reais() -> np.ndarray | None:
    tabela = PASTA_PROCESSADOS / "reais" / "reais.csv"
    if not tabela.exists():
        return None
    abertos = pd.read_csv(tabela).query("rotulo == 0")
    return np.stack([cv2.imread(c, cv2.IMREAD_GRAYSCALE)[..., np.newaxis] for c in abertos["recorte"]])


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--caracteristicas", type=Path, default=PASTA_PROCESSADOS / "caracteristicas.csv")
    parser.add_argument("--mrl", type=Path, default=PASTA_DADOS / "raw" / "MRL")
    parser.add_argument("--max-mrl", type=int, default=24_000)
    parser.add_argument("--epocas", type=int, default=25)
    parser.add_argument("--saida", type=Path, default=PASTA_MODELOS / "olho_cnn_real.tflite")
    parser.add_argument("--so-comparar", action="store_true", help="não treina: compara o modelo salvo em --saida")
    args = parser.parse_args()
    if args.so_comparar:
        relatar(pd.read_csv(args.caracteristicas), args.saida.read_bytes(), args.saida, "Modelo já treinado")
        return

    from tensorflow import keras

    from exportar_tflite import converter

    rng = np.random.default_rng(SEMENTE)
    tabela = pd.read_csv(args.caracteristicas)
    x_cew, y_cew = carregar_recortes(tabela[tabela["particao"] == "treino"])
    x_mrl, y_mrl = carregar_mrl(args.mrl, args.max_mrl, rng)
    x = np.concatenate([x_cew.astype(np.uint8), x_mrl])
    y = np.concatenate([y_cew, y_mrl])
    x_val, y_val = carregar_recortes(tabela[tabela["particao"] == "validacao"])
    val_x = np.concatenate([x_val.astype(np.uint8), aumentar_lote(x_val, 1), aumentar_lote(x_val, 2)]).astype("float32")
    val_y = np.concatenate([y_val, y_val, y_val])
    print(f"Treino: {len(x_cew)} olhos da CEW + {len(x_mrl)} da MRL | validação: {len(val_x)} (limpa e aumentada)")

    positivos = float(y.mean())
    pesos = {0: 0.5 / (1.0 - positivos), 1: 0.5 / positivos}
    modelo, _ = construir_modelo("media", pesos_imagenet=False)
    compilar(modelo, 1e-3)
    parada = keras.callbacks.EarlyStopping(monitor="val_auc", mode="max", patience=6, restore_best_weights=True)
    reduzir = keras.callbacks.ReduceLROnPlateau(monitor="val_auc", mode="max", factor=0.5, patience=3)
    historico = modelo.fit(sequencia(x, y, pesos), validation_data=(val_x, val_y), epochs=args.epocas,
                           callbacks=[parada, reduzir], verbose=2)

    representativas = aumentar_lote(x[rng.choice(len(x), 300, replace=False)], 5)
    novo = converter(modelo, representativas)
    args.saida.write_bytes(novo)
    modelo.save(args.saida.with_suffix(".keras"))

    melhor = max(historico.history["val_auc"])
    resumo = (
        f"Treino: {len(x_cew)} olhos da CEW e {len(x_mrl)} da MRL; {len(historico.history['val_auc'])} épocas; "
        f"melhor AUC na validação (limpa e aumentada): {melhor:.3f}"
    )
    relatar(tabela, novo, args.saida, resumo)


def relatar(tabela: pd.DataFrame, novo: bytes, saida: Path, resumo: str) -> None:
    """Compara o modelo do app com o novo e grava resultados/real.md."""
    x_teste, y_teste = carregar_recortes(tabela[tabela["particao"] == "teste"])
    atual = (PASTA_MODELOS / "olho_cnn_so_cew.tflite").read_bytes()
    modelos = {"atual (só CEW)": atual, "novo (aumentos + MRL)": novo}
    comparacao = comparar(modelos, x_teste, y_teste, carregar_reais())
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)
    comparacao.to_csv(PASTA_RESULTADOS / "real.csv", index=False)
    texto = (
        "# Retreino para a câmera real\n\n"
        f"{resumo}. Modelo: {saida.name}, {len(novo)} bytes.\n\n"
        + tabela_md(comparacao) + "\n"
    )
    (PASTA_RESULTADOS / "real.md").write_text(texto, encoding="utf-8")
    print(texto)


if __name__ == "__main__":
    main()
