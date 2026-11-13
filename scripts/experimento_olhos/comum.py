"""Partes compartilhadas do experimento olho aberto/fechado.

Os índices dos pontos e os limiares são os mesmos do app Android
(protocolos/MapeadorFaceMesh.kt e dominio/ConfiguracaoDeteccao.kt), para que o
experimento meça o que o HELIOS faz de verdade.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import pandas as pd

RAIZ_REPO = Path(__file__).resolve().parents[2]
PASTA_DADOS = RAIZ_REPO / "data"
PASTA_PROCESSADOS = PASTA_DADOS / "processed" / "experimento_olhos"
PASTA_MODELOS = RAIZ_REPO / "models"
PASTA_RESULTADOS = Path(__file__).resolve().parent / "resultados"
MODELO_MEDIAPIPE = PASTA_MODELOS / "face_landmarker.task"

# Seis pontos do EAR (p1..p6) de cada olho, lado da imagem. Iguais ao app.
OLHO_DIREITO_IMAGEM = (33, 160, 158, 133, 153, 144)
OLHO_ESQUERDO_IMAGEM = (362, 385, 387, 263, 373, 380)

# Contorno completo de cada olho, só para recortar a região que vai para a CNN.
CONTORNO_DIREITO_IMAGEM = (33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246)
CONTORNO_ESQUERDO_IMAGEM = (362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398)

# Limiares do app (ConfiguracaoDeteccao.kt).
LIMIAR_EAR_APP = 0.19
LIMIAR_ECF_APP = 0.40

ROTULO_ABERTO = 0
ROTULO_FECHADO = 1
NOMES_ROTULOS = {ROTULO_ABERTO: "aberto", ROTULO_FECHADO: "fechado"}

TAMANHO_RECORTE = 64
SEMENTE = 42

_PADRAO_PESSOA = re.compile(r"^(?P<pessoa>.+?)_\d{3,5}$")


def razao_aspecto(pontos: np.ndarray) -> float:
    """EAR = (|p2 - p6| + |p3 - p5|) / (2 |p1 - p4|), Soukupová e Čech (2016).

    `pontos` tem forma (6, 2), em pixels e na ordem p1..p6.
    """
    p1, p2, p3, p4, p5, p6 = pontos
    horizontal = 2.0 * float(np.linalg.norm(p1 - p4))
    if horizontal < 1e-8:
        return 0.0
    return float((np.linalg.norm(p2 - p6) + np.linalg.norm(p3 - p5)) / horizontal)


def pessoa_do_arquivo(nome: str) -> str:
    """Identificador da pessoa a partir do nome do arquivo.

    Segue a convenção do LFW (de onde vêm as faces abertas do CEW): `Nome_Sobrenome_0001.jpg`
    vira `Nome_Sobrenome`. Sem esse padrão, cada arquivo conta como uma pessoa diferente.
    """
    radical = Path(nome).stem
    achado = _PADRAO_PESSOA.match(radical)
    return achado.group("pessoa") if achado else radical


@dataclass(frozen=True)
class Metricas:
    n: int
    vp: int
    fp: int
    vn: int
    fn: int

    @property
    def acuracia(self) -> float:
        return _divide(self.vp + self.vn, self.n)

    @property
    def precisao(self) -> float:
        return _divide(self.vp, self.vp + self.fp)

    @property
    def sensibilidade(self) -> float:
        return _divide(self.vp, self.vp + self.fn)

    @property
    def especificidade(self) -> float:
        return _divide(self.vn, self.vn + self.fp)

    @property
    def f1(self) -> float:
        return _divide(2 * self.precisao * self.sensibilidade, self.precisao + self.sensibilidade)

    def como_dict(self) -> dict[str, float]:
        return {
            "n": self.n,
            "acuracia": self.acuracia,
            "precisao": self.precisao,
            "sensibilidade": self.sensibilidade,
            "especificidade": self.especificidade,
            "f1": self.f1,
            "vp": self.vp,
            "fp": self.fp,
            "vn": self.vn,
            "fn": self.fn,
        }


def _divide(a: float, b: float) -> float:
    return float(a) / float(b) if b else float("nan")


def calcular_metricas(verdadeiro: np.ndarray, previsto: np.ndarray) -> Metricas:
    """Classe positiva = olho fechado (é o que o sistema precisa encontrar)."""
    v = np.asarray(verdadeiro).astype(int)
    p = np.asarray(previsto).astype(int)
    if v.shape != p.shape:
        raise ValueError("verdadeiro e previsto precisam ter o mesmo tamanho")
    return Metricas(
        n=int(v.size),
        vp=int(np.sum((v == 1) & (p == 1))),
        fp=int(np.sum((v == 0) & (p == 1))),
        vn=int(np.sum((v == 0) & (p == 0))),
        fn=int(np.sum((v == 1) & (p == 0))),
    )


def limiar_youden(pontuacao: np.ndarray, verdadeiro: np.ndarray, maior_e_fechado: bool) -> float:
    """Limiar que maximiza sensibilidade + especificidade - 1 (índice de Youden).

    `maior_e_fechado` é falso para o EAR (olho fechado tem EAR baixo) e verdadeiro para ECF e CNN.
    """
    s = np.asarray(pontuacao, dtype=float)
    v = np.asarray(verdadeiro).astype(int)
    if s.size == 0 or len(np.unique(v)) < 2:
        raise ValueError("são necessárias amostras das duas classes")
    candidatos = np.unique(s)
    melhor_limiar, melhor_j = float(candidatos[0]), -np.inf
    for limiar in candidatos:
        previsto = s >= limiar if maior_e_fechado else s <= limiar
        m = calcular_metricas(v, previsto)
        j = m.sensibilidade + m.especificidade - 1.0
        if j > melhor_j:
            melhor_limiar, melhor_j = float(limiar), j
    return melhor_limiar


def dividir_por_pessoa(
    pessoas: pd.Series, rotulos: pd.Series, fracao_validacao: float = 0.15, fracao_teste: float = 0.15
) -> pd.Series:
    """Separa treino, validação e teste sem que a mesma pessoa apareça em duas partições.

    Sorteia pessoas (não imagens), separadamente para cada classe majoritária da pessoa,
    para manter a proporção aberto/fechado parecida nas três partições.
    """
    if len(pessoas) != len(rotulos):
        raise ValueError("pessoas e rotulos precisam ter o mesmo tamanho")
    gerador = np.random.default_rng(SEMENTE)
    classe_da_pessoa = rotulos.groupby(pessoas).agg(lambda r: int(round(r.mean())))
    particao_da_pessoa: dict[str, str] = {}
    for _, grupo in classe_da_pessoa.groupby(classe_da_pessoa):
        nomes = np.array(sorted(grupo.index))
        gerador.shuffle(nomes)
        n_teste = int(round(len(nomes) * fracao_teste))
        n_validacao = int(round(len(nomes) * fracao_validacao))
        for i, nome in enumerate(nomes):
            if i < n_teste:
                particao_da_pessoa[nome] = "teste"
            elif i < n_teste + n_validacao:
                particao_da_pessoa[nome] = "validacao"
            else:
                particao_da_pessoa[nome] = "treino"
    return pessoas.map(particao_da_pessoa)
