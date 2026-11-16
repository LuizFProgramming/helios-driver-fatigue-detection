"""Passo 6 (opcional, para levar a CNN ao app): exporta para TensorFlow Lite quantizado em int8.

Gera models/cnn_olhos_int8.tflite e um CSV de conferência (teste dourado) com a probabilidade do
Keras e do TFLite para os mesmos recortes. O app Android pode ler esse CSV num teste para provar que
roda o mesmo modelo. Só os nomes dos recortes vão no CSV, nenhuma imagem.

Uso:
    python scripts/experimento_olhos/exportar_tflite.py
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

from comum import PASTA_MODELOS, PASTA_PROCESSADOS, PASTA_RESULTADOS
from treinar_cnn import carregar_recortes


def converter(modelo, amostras_representativas: np.ndarray) -> bytes:
    import tensorflow as tf

    conversor = tf.lite.TFLiteConverter.from_keras_model(modelo)
    conversor.optimizations = [tf.lite.Optimize.DEFAULT]

    # Um recorte todo preto e um todo branco fixam a entrada em 0..255 (escala 1, ponto zero 0): o app manda
    # os bytes do recorte sem conversão. Sem eles a escala sai do menor e do maior pixel das amostras.
    extremos = np.stack([np.zeros_like(amostras_representativas[0]), np.full_like(amostras_representativas[0], 255)])

    def representativo():
        for amostra in np.concatenate([extremos, amostras_representativas[:300]]):
            yield [amostra[np.newaxis, ...].astype(np.float32)]

    conversor.representative_dataset = representativo
    conversor.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    conversor.inference_input_type = tf.uint8
    conversor.inference_output_type = tf.float32
    return conversor.convert()


def prever_tflite(conteudo: bytes, imagens: np.ndarray) -> np.ndarray:
    import tensorflow as tf

    interprete = tf.lite.Interpreter(model_content=conteudo)
    interprete.allocate_tensors()
    entrada = interprete.get_input_details()[0]
    saida = interprete.get_output_details()[0]
    resultados = []
    for imagem in imagens:
        interprete.set_tensor(entrada["index"], imagem[np.newaxis, ...].astype(np.uint8))
        interprete.invoke()
        resultados.append(float(interprete.get_tensor(saida["index"]).reshape(-1)[0]))
    return np.array(resultados)


def main() -> None:
    from tensorflow import keras

    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--caracteristicas", type=Path, default=PASTA_PROCESSADOS / "caracteristicas.csv")
    parser.add_argument("--modelo", type=Path, default=PASTA_MODELOS / "cnn_olhos.keras")
    parser.add_argument("--saida", type=Path, default=PASTA_MODELOS / "cnn_olhos_int8.tflite")
    args = parser.parse_args()

    tabela = pd.read_csv(args.caracteristicas)
    modelo = keras.models.load_model(args.modelo)
    x_treino, _ = carregar_recortes(tabela[tabela["particao"] == "treino"])
    conteudo = converter(modelo, x_treino)
    args.saida.write_bytes(conteudo)

    teste = tabela[(tabela["particao"] == "teste") & (tabela["rosto"] == 1)].head(100)
    x_teste, _ = carregar_recortes(teste)
    prob_keras = modelo.predict(x_teste, verbose=0).reshape(-1)
    prob_tflite = prever_tflite(conteudo, x_teste)
    recortes = list(teste["recorte_dir"]) + list(teste["recorte_esq"])
    conferencia = pd.DataFrame(
        {"recorte": [Path(r).name for r in recortes], "prob_keras": prob_keras, "prob_tflite_int8": prob_tflite}
    )
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)
    conferencia.to_csv(PASTA_RESULTADOS / "tflite_conferencia.csv", index=False)
    concordancia = float(np.mean((prob_keras > 0.5) == (prob_tflite > 0.5)))
    diferenca = float(np.max(np.abs(prob_keras - prob_tflite)))
    print(f"{args.saida} ({len(conteudo) / 1024:.0f} KB)")
    print(f"Mesma decisão em {concordancia * 100:.1f}% dos olhos; maior diferença de probabilidade {diferenca:.3f}")


if __name__ == "__main__":
    main()
