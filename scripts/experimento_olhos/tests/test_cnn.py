"""Testes de fumaça com TensorFlow e dados sintéticos: nenhum número daqui vale como resultado."""
import sys
from pathlib import Path

import cv2
import numpy as np
import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

tf = pytest.importorskip("tensorflow")

from comum import ROTULO_ABERTO, ROTULO_FECHADO, TAMANHO_RECORTE  # noqa: E402
from exportar_tflite import converter, prever_tflite  # noqa: E402
from treinar_cnn import ARQUITETURAS, carregar_mrl, carregar_recortes, treinar  # noqa: E402

import comparar_tamanhos  # noqa: E402


def olho_sintetico(gerador, fechado: bool, lado: int = TAMANHO_RECORTE) -> np.ndarray:
    """Pixels entre 60 e 190: sem preto nem branco puro, para a calibração do int8 não achar 0 e 255 sozinha."""
    imagem = gerador.integers(60, 190, size=(lado, lado)).astype(np.uint8)
    meio = lado // 2
    if fechado:
        imagem[meio - 2 : meio + 2, :] = 60
    else:
        imagem[meio - lado // 6 : meio + lado // 6, meio - lado // 6 : meio + lado // 6] = 70
    return imagem


@pytest.fixture
def caracteristicas(tmp_path: Path) -> Path:
    gerador = np.random.default_rng(0)
    pasta = tmp_path / "recortes"
    pasta.mkdir()
    linhas = []
    for particao, n in (("treino", 16), ("validacao", 8), ("teste", 8)):
        for i in range(n):
            rotulo = ROTULO_FECHADO if i % 2 else ROTULO_ABERTO
            caminhos = {}
            for lado in ("dir", "esq"):
                caminho = pasta / f"{particao}_{i}_{lado}.png"
                cv2.imwrite(str(caminho), olho_sintetico(gerador, rotulo == ROTULO_FECHADO))
                caminhos[f"recorte_{lado}"] = str(caminho)
            linhas.append({"id": f"{particao}/{i}.jpg", "particao": particao, "rotulo": rotulo, "rosto": 1, **caminhos})
    linhas.append({"id": "teste/sem_rosto.jpg", "particao": "teste", "rotulo": 0, "rosto": 0})
    destino = tmp_path / "caracteristicas.csv"
    pd.DataFrame(linhas).to_csv(destino, index=False)
    return destino


@pytest.fixture
def mrl(tmp_path: Path) -> Path:
    """Imitação da MRL Eye: uma pasta por pessoa, tamanhos variados, rótulo no 5º campo do nome."""
    gerador = np.random.default_rng(1)
    raiz = tmp_path / "MRL"
    for pessoa in ("s0001", "s0002"):
        (raiz / pessoa).mkdir(parents=True)
        for i in range(6):
            aberto = i % 2
            lado = 50 + 10 * i
            nome = f"{pessoa}_{i:05d}_0_0_{aberto}_0_0_01.png"
            cv2.imwrite(str(raiz / pessoa / nome), olho_sintetico(gerador, not aberto, lado))
    return raiz


def test_mrl_carrega_redimensionada_em_uint8_com_rotulo_invertido(mrl: Path):
    imagens, rotulos = carregar_mrl(mrl)
    assert imagens.shape == (12, TAMANHO_RECORTE, TAMANHO_RECORTE, 1)
    assert imagens.dtype == np.uint8
    assert sorted(rotulos.tolist()) == [0.0] * 6 + [1.0] * 6
    primeira = sorted(mrl.rglob("*.png"))[0]  # s0001_00000_0_0_0_...: fechado
    assert rotulos[0] == ROTULO_FECHADO and "_0_0_0_" in primeira.name


def test_mrl_vazia_e_erro(tmp_path: Path):
    with pytest.raises(FileNotFoundError):
        carregar_mrl(tmp_path)


@pytest.mark.parametrize("arquitetura", ARQUITETURAS)
def test_uma_epoca_de_cada_arquitetura_gera_o_mesmo_contrato_no_tflite(arquitetura, caracteristicas: Path, tmp_path: Path):
    tabela = pd.read_csv(caracteristicas)
    modelo, historico, contagens = treinar(tabela, arquitetura, 1, False, tmp_path / f"{arquitetura}.keras", None)
    assert len(historico) >= 1
    assert contagens == {"cew": 32, "mrl": 0}
    assert modelo.input_shape == (None, TAMANHO_RECORTE, TAMANHO_RECORTE, 1)
    assert modelo.output_shape == (None, 1)
    assert modelo.output_names == ["prob_fechado"]

    x_treino, _ = carregar_recortes(tabela[tabela["particao"] == "treino"])
    conteudo = converter(modelo, x_treino)
    interprete = tf.lite.Interpreter(model_content=conteudo)
    entrada = interprete.get_input_details()[0]
    saida = interprete.get_output_details()[0]
    assert entrada["dtype"] == np.uint8
    assert list(entrada["shape"]) == [1, TAMANHO_RECORTE, TAMANHO_RECORTE, 1]
    # O app manda os bytes de 0 a 255 sem conversão: escala 1 e ponto zero 0.
    assert entrada["quantization"] == pytest.approx((1.0, 0))
    assert saida["dtype"] == np.float32
    assert list(saida["shape"]) == [1, 1]
    prob = prever_tflite(conteudo, x_treino[:4])
    assert np.all((prob >= 0.0) & (prob <= 1.0))


def test_treino_com_mrl_soma_as_imagens_so_no_treino(caracteristicas: Path, mrl: Path, tmp_path: Path):
    tabela = pd.read_csv(caracteristicas)
    _, _, contagens = treinar(tabela, "pequena", 1, False, tmp_path / "p.keras", mrl)
    assert contagens == {"cew": 32, "mrl": 12}


def test_comparacao_completa_escolhe_dentro_do_limite_e_grava_o_dourado(caracteristicas: Path, mrl: Path, tmp_path: Path):
    resultados, modelos = tmp_path / "resultados", tmp_path / "models"
    comparar_tamanhos.main(
        [
            "--caracteristicas", str(caracteristicas), "--mrl", str(mrl), "--epocas", "1", "--sem-imagenet",
            "--resultados", str(resultados), "--modelos", str(modelos),
        ]
    )
    tabela = pd.read_csv(resultados / "tamanhos.csv")
    assert len(tabela) == 8
    assert set(tabela["base_treino"]) == {"cew", "cew+mrl"}
    assert set(tabela["arquitetura"]) == set(ARQUITETURAS)
    for coluna in (
        "parametros", "tamanho_kb", "imagens_cew", "imagens_mrl", "f1_validacao",
        "acuracia", "sensibilidade", "especificidade", "f1", "auc", "tempo_ms_por_recorte",
    ):
        assert coluna in tabela.columns
    assert (tabela.loc[tabela["base_treino"] == "cew", "imagens_mrl"] == 0).all()
    assert (tabela.loc[tabela["base_treino"] == "cew+mrl", "imagens_mrl"] == 12).all()
    assert (resultados / "tamanhos.md").exists()
    assert (resultados / "tamanhos.png").exists()

    escolhido = (modelos / "olho_cnn.tflite").read_bytes()
    assert len(escolhido) <= 1024 * 1024
    texto_md = (resultados / "tamanhos.md").read_text(encoding="utf-8")
    assert "Escolhido:" in texto_md and "mobilenet_100" not in texto_md.split("Escolhido:")[1].splitlines()[0]

    dourado = pd.read_csv(modelos / "olho_cnn_dourado.csv")
    assert list(dourado.columns) == ["recorte", "prob_fechado", "pixels"]
    assert len(dourado) == 16  # os 8 rostos do teste, dois olhos cada
    pixels = np.stack(
        [np.frombuffer(bytes.fromhex(p), dtype=np.uint8).reshape(TAMANHO_RECORTE, TAMANHO_RECORTE, 1) for p in dourado["pixels"]]
    )
    assert prever_tflite(escolhido, pixels) == pytest.approx(dourado["prob_fechado"].to_numpy(), abs=1e-6)

    # Retomada (o treino parou no meio): reaproveita os modelos salvos, sem treinar, e marca o que faltou.
    (modelos / "comparacao" / "mobilenet_100_cew_mrl.tflite").unlink()
    comparar_tamanhos.main(
        [
            "--caracteristicas", str(caracteristicas), "--mrl", str(mrl), "--retomar",
            "--resultados", str(resultados), "--modelos", str(modelos),
        ]
    )
    retomada = pd.read_csv(resultados / "tamanhos.csv")
    assert len(retomada) == 7
    assert not ((retomada["arquitetura"] == "mobilenet_100") & (retomada["base_treino"] == "cew+mrl")).any()
    assert (retomada.loc[retomada["base_treino"] == "cew+mrl", "imagens_mrl"] == 12).all()
    assert "Não treinada: mobilenet_100 (treino cew+mrl)" in (resultados / "tamanhos.md").read_text(encoding="utf-8")
