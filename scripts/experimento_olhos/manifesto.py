"""Passo 1: lista as imagens da base, com rótulo, pessoa e partição (treino, validação, teste).

Aceita dois formatos de pasta:
  - CEW (Closed Eyes In The Wild): subpastas `closedFace/` e `openFace/`.
  - Genérico: subpastas `fechado/` e `aberto/`.

Uso:
    python scripts/experimento_olhos/manifesto.py --base data/raw/CEW
"""
from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd

from comum import PASTA_PROCESSADOS, ROTULO_ABERTO, ROTULO_FECHADO, dividir_por_pessoa, pessoa_do_arquivo

PASTAS_POR_ROTULO = {
    ROTULO_FECHADO: ("closedFace", "fechado"),
    ROTULO_ABERTO: ("openFace", "aberto"),
}
EXTENSOES = {".jpg", ".jpeg", ".png", ".bmp"}


def montar_manifesto(base: Path) -> pd.DataFrame:
    linhas = []
    for rotulo, nomes in PASTAS_POR_ROTULO.items():
        pasta = next((base / n for n in nomes if (base / n).is_dir()), None)
        if pasta is None:
            raise FileNotFoundError(f"nenhuma das pastas {nomes} existe em {base}")
        for arquivo in sorted(pasta.rglob("*")):
            if arquivo.suffix.lower() in EXTENSOES:
                linhas.append(
                    {
                        "id": f"{pasta.name}/{arquivo.relative_to(pasta).as_posix()}",
                        "caminho": str(arquivo.resolve()),
                        "rotulo": rotulo,
                        # Prefixo pela classe: no CEW, nomes iguais em pastas diferentes não são a mesma pessoa.
                        "pessoa": f"{pasta.name}:{pessoa_do_arquivo(arquivo.name)}",
                    }
                )
    if not linhas:
        raise ValueError(f"nenhuma imagem encontrada em {base}")
    tabela = pd.DataFrame(linhas)
    tabela["particao"] = dividir_por_pessoa(tabela["pessoa"], tabela["rotulo"])
    return tabela


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", type=Path, required=True, help="pasta da base de imagens")
    parser.add_argument("--saida", type=Path, default=PASTA_PROCESSADOS / "manifesto.csv")
    args = parser.parse_args()

    tabela = montar_manifesto(args.base)
    args.saida.parent.mkdir(parents=True, exist_ok=True)
    tabela.to_csv(args.saida, index=False)
    resumo = tabela.groupby(["particao", "rotulo"]).size().unstack(fill_value=0)
    resumo.columns = ["aberto", "fechado"]
    print(resumo)
    print(f"{tabela['pessoa'].nunique()} pessoas, {len(tabela)} imagens -> {args.saida}")


if __name__ == "__main__":
    main()
