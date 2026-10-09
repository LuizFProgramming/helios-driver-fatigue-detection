"""
testar_alerta.py — Ouça cada som de alerta, sem precisar de câmera

Toca em sequência o som de DESATENTO, SONOLENCIA e DORMINDO, alguns segundos
cada, para ajustar o volume do PC e decidir se os padrões de config.py
(PADROES_SONOROS) estão bons.

Para executar (Windows):
  python testar_alerta.py

Para só gerar os arquivos .wav (qualquer sistema), numa pasta:
  python testar_alerta.py --exportar sons_de_alerta
"""

import sys
import time
from pathlib import Path

from adapters.audio_alert import criar_saida_de_alerta
from config import PADROES_SONOROS
from domain.alert_pattern import gerar_wav, padrao_para_categoria

SEGUNDOS_POR_SOM = 4
ORDEM = ["DESATENTO", "SONOLENCIA", "DORMINDO"]   # do menos para o mais urgente


def _repeticoes_para(padrao, segundos: float) -> int:
    return max(1, round(segundos * 1000 / padrao.duracao_do_ciclo_ms))


def exportar(pasta: Path) -> None:
    pasta.mkdir(parents=True, exist_ok=True)
    for categoria in ORDEM:
        padrao = padrao_para_categoria(categoria)
        arquivo = pasta / f"alerta_{categoria.lower()}.wav"
        arquivo.write_bytes(gerar_wav(padrao, _repeticoes_para(padrao, SEGUNDOS_POR_SOM)))
        print(f"Gerado: {arquivo}")


def tocar_todos() -> None:
    saida = criar_saida_de_alerta()
    try:
        for categoria in ORDEM:
            padrao = padrao_para_categoria(categoria)
            print(f"▶ {categoria}: {padrao.frequencia_hz} Hz, bipe de {padrao.ligado_ms} ms "
                  f"a cada {padrao.duracao_do_ciclo_ms} ms, volume {padrao.volume:.0%}")
            saida.tocar(padrao)
            time.sleep(SEGUNDOS_POR_SOM)
            saida.parar()
            time.sleep(0.7)
    finally:
        saida.liberar()
    print("Fim. Para mudar os sons, edite PADROES_SONOROS em config.py.")


if __name__ == "__main__":
    assert set(ORDEM) == set(PADROES_SONOROS), "ORDEM precisa listar todas as categorias do config.py"
    if len(sys.argv) >= 3 and sys.argv[1] == "--exportar":
        exportar(Path(sys.argv[2]))
    else:
        tocar_todos()
