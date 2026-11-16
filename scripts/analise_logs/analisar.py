"""Relatório do roteiro de teste do HELIOS (ADR-015).

Cruza o `roteiro.csv` (o que a pessoa fez e o que o app devia ver) com o `quadros.csv` (o que o app viu, quadro a
quadro) e o `eventos.csv` exportado do histórico. Mede, com e sem óculos:

- olho aberto ou fechado: acurácia, precisão, recall, F1 e "falso fechado" da regra do app e da CNN;
- alertas esperados: se o evento saiu e quanto tempo o estado de alerta levou para aparecer;
- alarmes falsos: fração dos quadros em alerta nas tarefas que não deviam alertar.

Uso: python scripts/analise_logs/analisar.py <pasta com os CSV>  (grava <pasta>/relatorio.md)
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import pandas as pd

MARGEM_MS = 700  # tempo de reação no começo de cada tarefa: fora do rótulo
FOLGA_MS = 2_000  # evento que sai logo depois do fim da tarefa ainda conta
CORTE_CNN = 0.5
EM_ALERTA = {"ATENCAO", "CRITICO", "DISTRAIDO", "DESATENTO"}  # FADIGA é cansaço acumulado, não alarme falso

EVENTOS_DO_ALERTA = {
    "MICROSSONO": {"MICROSSONO", "SONO"},
    "SONO": {"SONO"},
    "DISTRACAO": {"OLHAR_DESVIADO", "CABECA_VIRADA", "ROSTO_AUSENTE"},
    "BOCEJO": {"BOCEJO"},
}
ESTADOS_DO_ALERTA = {
    "MICROSSONO": {"ATENCAO", "CRITICO"},
    "SONO": {"CRITICO"},
    "DISTRACAO": {"DISTRAIDO", "DESATENTO"},
    "BOCEJO": set(),
}


def _ms(coluna: pd.Series) -> pd.Series:
    t = pd.to_datetime(coluna, format="ISO8601", utc=True)
    return (t - pd.Timestamp("1970-01-01", tz="UTC")) // pd.Timedelta(milliseconds=1)


def rotular(quadros: pd.DataFrame, roteiro: pd.DataFrame, margem_ms: int = MARGEM_MS) -> pd.DataFrame:
    """Cada quadro ganha a tarefa e o olho esperado; fora das tarefas (ou na margem do começo), vazio."""
    q = quadros.copy()
    q["t"] = _ms(q["inicio"])
    q["tarefa"] = None
    q["olho_esperado"] = None
    q["alerta_esperado"] = None
    for _, r in roteiro.iterrows():
        ini, fim = _ms(pd.Series([r["inicio"], r["fim"]]))
        dentro = (q["t"] >= ini + margem_ms) & (q["t"] < fim)
        q.loc[dentro, "tarefa"] = r["tarefa"]
        q.loc[dentro, "olho_esperado"] = r["olho_esperado"] if isinstance(r["olho_esperado"], str) else None
        q.loc[dentro, "alerta_esperado"] = r["alerta_esperado"]
    return q


def _metricas(nome: str, oculos, esperado_fechado: pd.Series, previsto_fechado: pd.Series) -> dict:
    vp = int((esperado_fechado & previsto_fechado).sum())
    fp = int((~esperado_fechado & previsto_fechado).sum())
    vn = int((~esperado_fechado & ~previsto_fechado).sum())
    fn = int((esperado_fechado & ~previsto_fechado).sum())
    n = vp + fp + vn + fn
    precisao = vp / (vp + fp) if vp + fp else float("nan")
    recall = vp / (vp + fn) if vp + fn else float("nan")
    f1 = 2 * precisao * recall / (precisao + recall) if vp else 0.0
    return {
        "detector": nome, "oculos": oculos, "n": n, "acuracia": (vp + vn) / n if n else float("nan"),
        "precisao": precisao, "recall": recall, "f1": f1,
        "falso_fechado": fp / (fp + vn) if fp + vn else float("nan"),
    }


def metricas_olho(rotulados: pd.DataFrame) -> pd.DataFrame:
    """Regra do app (coluna olho_fechado) e CNN (média dos dois olhos >= 0,5), por óculos."""
    q = rotulados[rotulados["olho_esperado"].notna()]
    linhas = []
    for oculos, g in q.groupby("oculos"):
        esperado = g["olho_esperado"] == "FECHADO"
        linhas.append(_metricas("app", oculos, esperado, g["olho_fechado"].astype(int) == 1))
        com_cnn = g[g["cnn_dir"].notna() & g["cnn_esq"].notna()]
        if len(com_cnn):
            media = (com_cnn["cnn_dir"].astype(float) + com_cnn["cnn_esq"].astype(float)) / 2
            linhas.append(_metricas("cnn", oculos, esperado[com_cnn.index], media >= CORTE_CNN))
    return pd.DataFrame(linhas)


def alertas(roteiro: pd.DataFrame, quadros: pd.DataFrame, eventos: pd.DataFrame) -> pd.DataFrame:
    """Para cada tarefa que devia alertar: se o evento saiu e o atraso até o primeiro estado de alerta."""
    q = quadros.assign(t=_ms(quadros["inicio"]))
    sessoes = set(roteiro["sessao"])
    e = eventos[eventos["sessao"].isin(sessoes)]
    e = e.assign(t=_ms(e["inicio"]))
    linhas = []
    for _, r in roteiro[roteiro["alerta_esperado"] != "NENHUM"].iterrows():
        ini, fim = _ms(pd.Series([r["inicio"], r["fim"]]))
        alerta = r["alerta_esperado"]
        janela = e[(e["t"] >= ini - MARGEM_MS) & (e["t"] <= fim + FOLGA_MS)]
        estados = q[(q["t"] >= ini) & (q["t"] <= fim + FOLGA_MS) & q["estado"].isin(ESTADOS_DO_ALERTA[alerta])]
        linhas.append({
            "tarefa": r["tarefa"], "alerta": alerta, "oculos": r["oculos"],
            "evento": bool(janela["tipo"].isin(EVENTOS_DO_ALERTA[alerta]).any()),
            "atraso_estado_ms": int(estados["t"].min() - ini) if len(estados) else None,
        })
    return pd.DataFrame(linhas)


def falsos_alarmes(rotulados: pd.DataFrame) -> pd.DataFrame:
    """Nas tarefas sem alerta esperado: quantos quadros e que fração deles ficou em alerta."""
    q = rotulados[rotulados["alerta_esperado"] == "NENHUM"]
    g = q.groupby("tarefa")["estado"]
    return pd.DataFrame({
        "quadros": g.size(),
        "fracao_em_alerta": g.apply(lambda s: s.isin(EM_ALERTA).mean()),
    }).reset_index()


def _tabela(df: pd.DataFrame) -> str:
    if df.empty:
        return "(sem dados)\n"
    def celula(v):
        return f"{v:.3f}" if isinstance(v, float) else str(v)
    cab = "| " + " | ".join(df.columns) + " |"
    sep = "|" + "---|" * len(df.columns)
    corpo = ["| " + " | ".join(celula(v) for v in linha) + " |" for linha in df.itertuples(index=False)]
    return "\n".join([cab, sep, *corpo]) + "\n"


def relatorio(roteiro: pd.DataFrame, quadros: pd.DataFrame, eventos: pd.DataFrame) -> str:
    rot = rotular(quadros, roteiro)
    return (
        "# Relatório do roteiro de teste\n\n"
        f"Sessão {', '.join(map(str, sorted(set(roteiro['sessao']))))}; {len(quadros)} quadros; "
        f"margem de {MARGEM_MS} ms no começo de cada tarefa.\n\n"
        "## Olho aberto ou fechado\n\n" + _tabela(metricas_olho(rot)) +
        "\n`falso_fechado`: fração dos quadros de olho aberto chamados de fechado.\n\n"
        "## Alertas esperados\n\n" + _tabela(alertas(roteiro, quadros, eventos)) +
        "\n## Alarmes falsos\n\n" + _tabela(falsos_alarmes(rot))
    )


def ler_pasta(pasta: Path) -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    """O roteiro mais recente da pasta, o quadros.csv da mesma viagem e os eventos exportados dela."""
    roteiros = sorted(pasta.glob("helios-roteiro-viagem-*.csv"))
    if not roteiros:
        raise SystemExit(f"Nenhum helios-roteiro-viagem-*.csv em {pasta}: rode o roteiro de teste no app.")
    roteiro = pd.read_csv(roteiros[-1])
    sessao = int(roteiro["sessao"].iloc[0])
    caminho_quadros = pasta / f"helios-quadros-viagem-{sessao}.csv"
    if not caminho_quadros.exists():
        raise SystemExit(f"Falta {caminho_quadros.name}: o roteiro foi feito sem a autorização dos dados?")
    quadros = pd.read_csv(caminho_quadros)
    eventos = pd.concat([pd.read_csv(p) for p in pasta.glob("helios-eventos-*.csv")] or [pd.DataFrame(
        columns=["sessao", "inicio", "duracao_ms", "tipo"])], ignore_index=True)
    eventos = eventos[eventos["sessao"] == sessao].drop_duplicates()
    return roteiro, quadros, eventos


def main(argv: list[str] | None = None) -> None:
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("pasta", type=Path)
    args = p.parse_args(argv)
    texto = relatorio(*ler_pasta(args.pasta))
    (args.pasta / "relatorio.md").write_text(texto, encoding="utf-8")
    print(texto)


if __name__ == "__main__":
    main(sys.argv[1:])
