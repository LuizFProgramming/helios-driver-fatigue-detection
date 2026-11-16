"""Passo 7: compara tamanhos de CNN para o app (tamanho x acerto x tempo).

Treina as quatro arquiteturas do treinar_cnn.py só com a CEW e com CEW + MRL Eye, exporta cada uma em int8 e
mede o .tflite, que é o que vai para o app. Métricas por olho, com corte 0,5 (o mesmo do app): F1 na validação,
que escolhe o modelo, e acurácia, sensibilidade, especificidade, F1 e AUC no teste da CEW, só para relatar.

Escolhe o maior F1 na validação com .tflite de até 1024 KB (a mobilenet_100 entra na curva, mas não pode ser
escolhida), copia para models/olho_cnn.tflite e grava models/olho_cnn_dourado.csv (recortes do teste em uint8 e a
probabilidade esperada) para o teste de paridade do app.

Com --retomar (o treino parou no meio, por exemplo por falta de memória), nada é treinado: usa os modelos já
salvos em models/comparacao e registra na tabela as combinações sem modelo como "não treinada".

Uso:
    python scripts/experimento_olhos/comparar_tamanhos.py --mrl data/raw/MRL
    python scripts/experimento_olhos/comparar_tamanhos.py --mrl data/raw/MRL --retomar
"""
from __future__ import annotations

import argparse
import shutil
import time
from pathlib import Path

import numpy as np
import pandas as pd

from comum import (
    LIMITE_APP_KB,
    PASTA_DADOS,
    PASTA_MODELOS,
    PASTA_PROCESSADOS,
    PASTA_RESULTADOS,
    avaliar_probabilidades,
    escolher_modelo,
    pixels_hex,
)
from exportar_tflite import converter, prever_tflite
from treinar_cnn import ARQUITETURAS, carregar_recortes, treinar

BASES = ("cew", "cew+mrl")
METRICAS_TESTE = ("acuracia", "sensibilidade", "especificidade", "f1", "auc")
AVISO_DOURADO = (
    "ATENÇÃO: {0} tem pixels de olhos de pessoas reais (teste da CEW). Não coloque no git nem no APK sem o grupo "
    "decidir (LGPD e licença da CEW)."
)


def comparar(
    tabela: pd.DataFrame, mrl: Path, epocas: int, pesos_imagenet: bool, pasta: Path, retomar: bool = False
) -> tuple[pd.DataFrame, list[str]]:
    """Uma linha por arquitetura e base de treino (os .keras e .tflite ficam em `pasta`) e as que ficaram sem modelo."""
    x_treino, _ = carregar_recortes(tabela[tabela["particao"] == "treino"])
    x_val, y_val = carregar_recortes(tabela[tabela["particao"] == "validacao"])
    x_teste, y_teste = carregar_recortes(tabela[tabela["particao"] == "teste"])
    pasta.mkdir(parents=True, exist_ok=True)
    linhas, faltando = [], []
    for base in BASES:
        for arquitetura in ARQUITETURAS:
            nome = f"{arquitetura}_{base.replace('+', '_')}"
            salvo_keras, salvo_tflite = pasta / f"{nome}.keras", pasta / f"{nome}.tflite"
            print(f"\n=== {arquitetura}, treino {base} ===")
            if retomar:
                if not (salvo_keras.exists() and salvo_tflite.exists()):
                    faltando.append(f"{arquitetura} (treino {base})")
                    print("sem modelo salvo: fica fora da tabela")
                    continue
                from tensorflow import keras

                modelo, conteudo = keras.models.load_model(salvo_keras), salvo_tflite.read_bytes()
                contagens = {"cew": len(x_treino), "mrl": len(list(mrl.rglob("*.png"))) if base == "cew+mrl" else 0}
            else:
                modelo, _, contagens = treinar(
                    tabela, arquitetura, epocas, pesos_imagenet, salvo_keras, mrl if base == "cew+mrl" else None
                )
                conteudo = converter(modelo, x_treino)
                salvo_tflite.write_bytes(conteudo)
            prob_val = prever_tflite(conteudo, x_val)
            inicio = time.perf_counter()
            prob_teste = prever_tflite(conteudo, x_teste)  # um recorte por chamada, como no app
            tempo_ms = (time.perf_counter() - inicio) * 1000.0 / len(x_teste)
            teste = avaliar_probabilidades(y_teste, prob_teste)
            linhas.append(
                {
                    "arquitetura": arquitetura,
                    "base_treino": base,
                    "imagens_cew": contagens["cew"],
                    "imagens_mrl": contagens["mrl"],
                    "parametros": int(modelo.count_params()),
                    "tamanho_kb": len(conteudo) / 1024.0,
                    "f1_validacao": avaliar_probabilidades(y_val, prob_val)["f1"],
                    **{m: teste[m] for m in METRICAS_TESTE},
                    "tempo_ms_por_recorte": tempo_ms,
                    "tflite": f"{nome}.tflite",
                }
            )
    return pd.DataFrame(linhas), faltando


def _num(valor: float, casas: int) -> str:
    return "—" if np.isnan(valor) else f"{valor:.{casas}f}".replace(".", ",")


def markdown(tabela: pd.DataFrame, escolhido: pd.Series, faltando: list[str] = ()) -> str:
    def pct(v: float) -> str:
        return "—" if np.isnan(v) else f"{v * 100:.1f}%".replace(".", ",")

    linhas = [
        f"Escolhido: {escolhido['arquitetura']} (treino {escolhido['base_treino']}), {_num(escolhido['tamanho_kb'], 0)} KB, "
        f"F1 na validação {pct(escolhido['f1_validacao'])}. Regra: maior F1 na validação com .tflite até "
        f"{LIMITE_APP_KB} KB; o teste só foi olhado depois da escolha.",
        "",
        "Métricas por olho (cada recorte é uma amostra), corte 0,5. Teste e validação da CEW, divididos por pessoa.",
        "",
        "| Arquitetura | Treino | Parâmetros | .tflite (KB) | F1 validação | Acurácia | Sensibilidade | "
        "Especificidade | F1 | AUC | ms/recorte (PC) |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for m in tabela.itertuples(index=False):
        parametros = f"{m.parametros:,}".replace(",", ".")
        linhas.append(
            f"| {m.arquitetura} | {m.base_treino} | {parametros} | {_num(m.tamanho_kb, 0)} | {pct(m.f1_validacao)} | "
            f"{pct(m.acuracia)} | {pct(m.sensibilidade)} | {pct(m.especificidade)} | {pct(m.f1)} | "
            f"{_num(m.auc, 3)} | {_num(m.tempo_ms_por_recorte, 2)} |"
        )
    if faltando:
        linhas += ["", *(f"Não treinada: {f}. O treino parou antes (falta de memória); não entra na tabela." for f in faltando)]
    return "\n".join(linhas) + "\n"


def salvar_grafico(tabela: pd.DataFrame, destino: Path) -> None:
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    figura, eixo = plt.subplots(figsize=(7, 4.5))
    for base, marcador in zip(BASES, ("o", "s")):
        parte = tabela[tabela["base_treino"] == base]
        eixo.scatter(parte["tamanho_kb"], parte["f1"], marker=marcador, label=f"treino {base}")
        for m in parte.itertuples(index=False):
            eixo.annotate(m.arquitetura, (m.tamanho_kb, m.f1), textcoords="offset points", xytext=(5, 4), fontsize=8)
    eixo.axvline(LIMITE_APP_KB, color="gray", linestyle="--", linewidth=1, label=f"limite do app ({LIMITE_APP_KB} KB)")
    eixo.set_xscale("log")
    eixo.set_xlabel(".tflite int8 (KB, escala log)")
    eixo.set_ylabel("F1 no teste (por olho, corte 0,5)")
    eixo.legend(fontsize=8)
    figura.tight_layout()
    figura.savefig(destino, dpi=150)
    plt.close(figura)


def gravar_dourado(tabela: pd.DataFrame, conteudo: bytes, destino: Path, limite: int = 100) -> None:
    """Recortes do teste em uint8 (hexadecimal, 4096 bytes) e a probabilidade que o .tflite deu para cada um."""
    teste = tabela[(tabela["particao"] == "teste") & (tabela["rosto"] == 1)].head(limite)
    x, _ = carregar_recortes(teste)
    recortes = list(teste["recorte_dir"]) + list(teste["recorte_esq"])
    pd.DataFrame(
        {
            "recorte": [Path(r).name for r in recortes],
            "prob_fechado": prever_tflite(conteudo, x),
            "pixels": [pixels_hex(imagem) for imagem in x],
        }
    ).to_csv(destino, index=False)


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--caracteristicas", type=Path, default=PASTA_PROCESSADOS / "caracteristicas.csv")
    parser.add_argument("--mrl", type=Path, default=PASTA_DADOS / "raw" / "MRL")
    parser.add_argument("--epocas", type=int, default=30)
    parser.add_argument("--sem-imagenet", action="store_true", help="não baixar os pesos do ImageNet")
    parser.add_argument("--resultados", type=Path, default=PASTA_RESULTADOS)
    parser.add_argument("--modelos", type=Path, default=PASTA_MODELOS)
    parser.add_argument("--retomar", action="store_true", help="usa os modelos já salvos, sem treinar")
    args = parser.parse_args(argv)

    tabela = pd.read_csv(args.caracteristicas)
    comparacao, faltando = comparar(
        tabela, args.mrl, args.epocas, not args.sem_imagenet, args.modelos / "comparacao", args.retomar
    )
    escolhido = escolher_modelo(comparacao)

    args.resultados.mkdir(parents=True, exist_ok=True)
    comparacao.to_csv(args.resultados / "tamanhos.csv", index=False)
    texto = markdown(comparacao, escolhido, faltando)
    (args.resultados / "tamanhos.md").write_text(texto, encoding="utf-8")
    salvar_grafico(comparacao, args.resultados / "tamanhos.png")

    origem = args.modelos / "comparacao" / escolhido["tflite"]
    shutil.copyfile(origem, args.modelos / "olho_cnn.tflite")
    dourado = args.modelos / "olho_cnn_dourado.csv"
    gravar_dourado(tabela, origem.read_bytes(), dourado)
    print("\n" + texto)
    print(f"{origem.name} copiado para {args.modelos / 'olho_cnn.tflite'}; conferência em {dourado}")
    print(AVISO_DOURADO.format(dourado))


if __name__ == "__main__":
    main()
