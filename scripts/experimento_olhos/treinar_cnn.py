"""Passo 3: treina a CNN do grupo para classificar o recorte de um olho em aberto ou fechado.

Duas arquiteturas:
  - mobilenet (padrão): MobileNetV2 (Sandler et al., 2018) com pesos do ImageNet e transfer learning.
    A base fica congelada na primeira fase e as últimas camadas são liberadas na segunda (ajuste fino).
  - pequena: CNN de 3 blocos convolucionais (~100 mil parâmetros), treinada do zero. Serve de linha de base
    e para rodar em máquina fraca.

Treina só com a partição de treino, escolhe a época pela validação e nunca olha o teste.

Uso:
    python scripts/experimento_olhos/treinar_cnn.py
    python scripts/experimento_olhos/treinar_cnn.py --arquitetura pequena --epocas 20
"""
from __future__ import annotations

import argparse
import json
import time
from pathlib import Path

import cv2
import numpy as np
import pandas as pd

from comum import PASTA_MODELOS, PASTA_PROCESSADOS, PASTA_RESULTADOS, SEMENTE, TAMANHO_RECORTE

TAMANHO_MOBILENET = 96


def carregar_recortes(tabela: pd.DataFrame) -> tuple[np.ndarray, np.ndarray]:
    """Os dois olhos de cada imagem com rosto viram duas amostras com o rótulo da imagem."""
    com_rosto = tabela[tabela["rosto"] == 1]
    caminhos = list(com_rosto["recorte_dir"]) + list(com_rosto["recorte_esq"])
    rotulos = np.concatenate([com_rosto["rotulo"].to_numpy(), com_rosto["rotulo"].to_numpy()])
    imagens = np.stack([cv2.imread(c, cv2.IMREAD_GRAYSCALE) for c in caminhos])
    return imagens[..., np.newaxis].astype("float32"), rotulos.astype("float32")


def construir_modelo(arquitetura: str, pesos_imagenet: bool):
    import tensorflow as tf
    from tensorflow import keras
    from tensorflow.keras import layers

    entrada = keras.Input(shape=(TAMANHO_RECORTE, TAMANHO_RECORTE, 1), name="olho_cinza")
    if arquitetura == "pequena":
        x = layers.Rescaling(1.0 / 255)(entrada)
        for filtros in (16, 32, 64):
            x = layers.Conv2D(filtros, 3, padding="same", activation="relu")(x)
            x = layers.BatchNormalization(momentum=0.9)(x)
            x = layers.MaxPooling2D()(x)
        x = layers.GlobalAveragePooling2D()(x)
        x = layers.Dropout(0.3)(x)
        base = None
    elif arquitetura == "mobilenet":
        x = layers.Resizing(TAMANHO_MOBILENET, TAMANHO_MOBILENET)(entrada)
        x = layers.Concatenate()([x, x, x])  # cinza -> 3 canais, como a rede espera
        x = layers.Rescaling(1.0 / 127.5, offset=-1.0)(x)  # mesma escala do treino original da MobileNetV2
        base = keras.applications.MobileNetV2(
            input_shape=(TAMANHO_MOBILENET, TAMANHO_MOBILENET, 3),
            alpha=0.35,
            include_top=False,
            weights="imagenet" if pesos_imagenet else None,
        )
        base.trainable = False
        x = base(x, training=False)
        x = layers.GlobalAveragePooling2D()(x)
        x = layers.Dropout(0.3)(x)
    else:
        raise ValueError(f"arquitetura desconhecida: {arquitetura}")
    saida = layers.Dense(1, activation="sigmoid", name="prob_fechado")(x)
    modelo = keras.Model(entrada, saida, name=f"cnn_olhos_{arquitetura}")
    tf.random.set_seed(SEMENTE)
    return modelo, base


def aumentar(tf, imagem, rotulo):
    """Variações que a câmera do carro produz: espelho, brilho, contraste e pequeno deslocamento."""
    imagem = tf.image.random_flip_left_right(imagem)
    imagem = tf.image.random_brightness(imagem, 40.0)
    imagem = tf.image.random_contrast(imagem, 0.7, 1.3)
    deslocada = tf.image.resize_with_crop_or_pad(imagem, TAMANHO_RECORTE + 8, TAMANHO_RECORTE + 8)
    imagem = tf.image.random_crop(deslocada, (TAMANHO_RECORTE, TAMANHO_RECORTE, 1))
    return tf.clip_by_value(imagem, 0.0, 255.0), rotulo


def compilar(modelo, taxa: float) -> None:
    from tensorflow import keras

    modelo.compile(
        optimizer=keras.optimizers.Adam(taxa),
        loss="binary_crossentropy",
        metrics=["accuracy", keras.metrics.AUC(name="auc")],
    )


def treinar(tabela: pd.DataFrame, arquitetura: str, epocas: int, pesos_imagenet: bool, destino: Path) -> pd.DataFrame:
    import tensorflow as tf
    from tensorflow import keras

    x_treino, y_treino = carregar_recortes(tabela[tabela["particao"] == "treino"])
    x_val, y_val = carregar_recortes(tabela[tabela["particao"] == "validacao"])
    print(f"Treino: {len(x_treino)} olhos | validação: {len(x_val)} olhos")

    positivos = float(y_treino.mean())
    pesos = {0: 0.5 / (1.0 - positivos), 1: 0.5 / positivos}  # compensa classes desbalanceadas
    treino = (
        tf.data.Dataset.from_tensor_slices((x_treino, y_treino))
        .shuffle(len(x_treino), seed=SEMENTE)
        .map(lambda a, b: aumentar(tf, a, b), num_parallel_calls=tf.data.AUTOTUNE)
        .batch(64)
        .prefetch(tf.data.AUTOTUNE)
    )
    validacao = tf.data.Dataset.from_tensor_slices((x_val, y_val)).batch(128)

    modelo, base = construir_modelo(arquitetura, pesos_imagenet)
    parada = keras.callbacks.EarlyStopping(monitor="val_auc", mode="max", patience=5, restore_best_weights=True)
    compilar(modelo, 1e-3)
    historicos = [modelo.fit(treino, validation_data=validacao, epochs=epocas, class_weight=pesos, callbacks=[parada])]
    if base is not None:
        # Ajuste fino: libera as últimas 30 camadas da MobileNetV2 com taxa de aprendizado baixa.
        base.trainable = True
        for camada in base.layers[:-30]:
            camada.trainable = False
        compilar(modelo, 1e-5)
        historicos.append(
            modelo.fit(treino, validation_data=validacao, epochs=max(1, epocas // 2), class_weight=pesos, callbacks=[parada])
        )

    destino.parent.mkdir(parents=True, exist_ok=True)
    modelo.save(destino)
    historico = pd.concat([pd.DataFrame(h.history) for h in historicos], ignore_index=True)
    historico.index.name = "epoca"
    return historico


def prever(modelo, tabela: pd.DataFrame) -> pd.DataFrame:
    """Probabilidade de olho fechado por imagem (média dos dois olhos) e tempo de inferência por imagem."""
    com_rosto = tabela[tabela["rosto"] == 1]
    n = len(com_rosto)
    if n == 0:
        return pd.DataFrame({"id": pd.Series(dtype=str), "prob_cnn": pd.Series(dtype=float), "tempo_cnn_ms": pd.Series(dtype=float)})
    x, _ = carregar_recortes(com_rosto)
    probs = modelo.predict(x, batch_size=256, verbose=0).reshape(-1)
    media = (probs[:n] + probs[n:]) / 2.0
    # Tempo de uma imagem de cada vez (dois olhos), como no app, sem contar a primeira chamada.
    amostras = min(50, n)
    modelo(x[np.r_[0:1, n : n + 1]], training=False)
    inicio = time.perf_counter()
    for i in range(amostras):
        modelo(x[np.r_[i : i + 1, n + i : n + i + 1]], training=False)
    tempo_ms = (time.perf_counter() - inicio) * 1000.0 / max(1, amostras)
    return pd.DataFrame({"id": com_rosto["id"].to_numpy(), "prob_cnn": media, "tempo_cnn_ms": tempo_ms})


def salvar_curvas(historico: pd.DataFrame, destino: Path) -> None:
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    figura, eixos = plt.subplots(1, 2, figsize=(10, 4))
    for eixo, chave, titulo in ((eixos[0], "loss", "Perda"), (eixos[1], "auc", "AUC")):
        eixo.plot(historico.index + 1, historico[chave], label="treino")
        eixo.plot(historico.index + 1, historico[f"val_{chave}"], label="validação")
        eixo.set_xlabel("Época")
        eixo.set_title(titulo)
        eixo.legend()
    figura.tight_layout()
    figura.savefig(destino, dpi=150)
    plt.close(figura)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--caracteristicas", type=Path, default=PASTA_PROCESSADOS / "caracteristicas.csv")
    parser.add_argument("--arquitetura", choices=("mobilenet", "pequena"), default="mobilenet")
    parser.add_argument("--epocas", type=int, default=30)
    parser.add_argument("--sem-imagenet", action="store_true", help="não baixar os pesos do ImageNet")
    parser.add_argument("--modelo", type=Path, default=PASTA_MODELOS / "cnn_olhos.keras")
    args = parser.parse_args()

    tabela = pd.read_csv(args.caracteristicas)
    historico = treinar(tabela, args.arquitetura, args.epocas, not args.sem_imagenet, args.modelo)
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)
    historico.to_csv(PASTA_RESULTADOS / "treino_historico.csv")
    salvar_curvas(historico, PASTA_RESULTADOS / "treino_curvas.png")
    resumo = {
        "arquitetura": args.arquitetura,
        "pesos_imagenet": not args.sem_imagenet,
        "epocas_rodadas": int(len(historico)),
        "melhor_val_auc": float(historico["val_auc"].max()),
    }
    (PASTA_RESULTADOS / "treino_resumo.json").write_text(json.dumps(resumo, indent=2), encoding="utf-8")
    print(f"Modelo salvo em {args.modelo}. Melhor AUC na validação: {resumo['melhor_val_auc']:.3f}")


if __name__ == "__main__":
    main()
