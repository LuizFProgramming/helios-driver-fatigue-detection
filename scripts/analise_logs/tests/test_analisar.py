import sys
from pathlib import Path

import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from analisar import (  # noqa: E402
    alertas,
    falsos_alarmes,
    ler_pasta,
    metricas_olho,
    relatorio,
    rotular,
)

BASE = pd.Timestamp("2026-11-16T12:00:00-03:00")


def hora(ms: int) -> str:
    return (BASE + pd.Timedelta(milliseconds=ms)).isoformat(timespec="milliseconds")


def roteiro() -> pd.DataFrame:
    linhas = [
        ("ESTRADA", 0, 2_000, "ABERTO", "NENHUM"),
        ("FECHAR_LONGO", 3_000, 7_000, "FECHADO", "SONO"),
        ("OLHAR_DE_LADO", 8_000, 13_000, "ABERTO", "DISTRACAO"),
    ]
    return pd.DataFrame(
        [
            {"sessao": 1, "inicio": hora(a), "fim": hora(b), "tarefa": t, "olho_esperado": o,
             "alerta_esperado": al, "oculos": 1}
            for t, a, b, o, al in linhas
        ]
    )


def quadro(ms: int, estado="ATENTO", fechado=0, cnn=0.1, oculos=1) -> dict:
    return {"inicio": hora(ms), "estado": estado, "olho_fechado": fechado, "cnn_dir": cnn, "cnn_esq": cnn,
            "oculos": oculos}


def quadros() -> pd.DataFrame:
    q = []
    for ms in range(0, 2_000, 100):  # estrada: o app acerta; a CNN erra a partir de 1 s (óculos)
        q.append(quadro(ms, cnn=0.9 if ms >= 1_000 else 0.1))
    for ms in range(3_000, 7_000, 100):  # olho fechado: CRÍTICO a partir de 6 s
        q.append(quadro(ms, estado="CRITICO" if ms >= 6_000 else "ATENCAO", fechado=1, cnn=0.95))
    for ms in range(8_000, 13_000, 100):  # olhar de lado: DISTRAÍDO a partir de 11,5 s
        q.append(quadro(ms, estado="DISTRAIDO" if ms >= 11_500 else "ATENTO"))
    return pd.DataFrame(q)


def eventos() -> pd.DataFrame:
    return pd.DataFrame([
        {"sessao": 1, "inicio": hora(3_100), "duracao_ms": 3_800, "tipo": "SONO"},
        {"sessao": 2, "inicio": hora(9_000), "duracao_ms": 3_000, "tipo": "OLHAR_DESVIADO"},  # outra viagem
    ])


def test_rotulo_ignora_a_margem_do_inicio_e_o_que_fica_fora_das_tarefas():
    r = rotular(quadros(), roteiro(), margem_ms=500)
    estrada = r[r.tarefa == "ESTRADA"]
    assert len(estrada) == 15  # 500 a 1.900 ms
    assert set(r.olho_esperado.dropna()) == {"ABERTO", "FECHADO"}
    assert r.tarefa.isna().sum() == 5 + 5 + 5  # margens das três tarefas


def test_metricas_da_regra_do_app_e_da_cnn_com_oculos():
    m = metricas_olho(rotular(quadros(), roteiro(), margem_ms=0)).set_index("detector")
    app = m.loc["app"]
    assert app["n"] == 20 + 40 + 50
    assert app["acuracia"] == pytest.approx(1.0)
    assert app["falso_fechado"] == pytest.approx(0.0)
    cnn = m.loc["cnn"]
    assert cnn["recall"] == pytest.approx(1.0)
    assert cnn["falso_fechado"] == pytest.approx(10 / 70)  # 10 dos 70 quadros abertos viraram "fechado"
    assert cnn["precisao"] == pytest.approx(40 / 50)
    assert set(m["oculos"]) == {1}


def test_alertas_esperados_com_atraso_ate_o_primeiro_estado():
    a = alertas(roteiro(), quadros(), eventos()).set_index("tarefa")
    assert bool(a.loc["FECHAR_LONGO", "evento"]) is True
    assert a.loc["FECHAR_LONGO", "atraso_estado_ms"] == 3_000
    assert bool(a.loc["OLHAR_DE_LADO", "evento"]) is False  # o evento é de outra viagem
    assert a.loc["OLHAR_DE_LADO", "atraso_estado_ms"] == 3_500
    assert "ESTRADA" not in a.index


def test_falso_alarme_e_a_fracao_de_quadros_em_alerta_nas_tarefas_sem_alerta():
    q = quadros()
    q.loc[q.inicio == hora(1_500), "estado"] = "DISTRAIDO"
    q.loc[q.inicio == hora(1_600), "estado"] = "FADIGA"  # cansaço acumulado não conta como alarme falso
    f = falsos_alarmes(rotular(q, roteiro(), margem_ms=0)).set_index("tarefa")
    assert f.loc["ESTRADA", "quadros"] == 20
    assert f.loc["ESTRADA", "fracao_em_alerta"] == pytest.approx(1 / 20)


def test_relatorio_e_leitura_da_pasta(tmp_path):
    roteiro().to_csv(tmp_path / "helios-roteiro-viagem-1.csv", index=False)
    quadros().to_csv(tmp_path / "helios-quadros-viagem-1.csv", index=False)
    eventos().to_csv(tmp_path / "helios-eventos-todas-20261116.csv", index=False)
    r, q, e = ler_pasta(tmp_path)
    assert len(r) == 3 and len(q) == 110 and len(e) == 1  # só os eventos da sessão do roteiro
    texto = relatorio(r, q, e)
    assert "| cnn | 1 |" in texto
    assert "FECHAR_LONGO" in texto
    assert "Alarmes falsos" in texto


def test_pasta_sem_roteiro_explica_o_que_falta(tmp_path):
    with pytest.raises(SystemExit, match="roteiro"):
        ler_pasta(tmp_path)
