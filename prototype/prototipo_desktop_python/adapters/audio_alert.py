"""
adapters/audio_alert.py — Alerta sonoro no alto-falante do computador

No Windows usa o módulo `winsound`, que já vem com o Python (nenhuma
instalação extra). O ciclo do padrão é gravado num WAV temporário e tocado
em repetição, em segundo plano: tocar() volta na hora e não trava a análise.

Em outros sistemas o alerta fica em silêncio e um aviso é mostrado uma vez
(o time usa Windows; um adaptador para outro sistema entra pela mesma porta).
"""

import shutil
import sys
import tempfile
from pathlib import Path
from typing import Dict, Optional

from domain.alert_pattern import PadraoSonoro, gerar_wav
from ports.alert_output_port import PortaSaidaDeAlerta


class AlertaSonoroWindows(PortaSaidaDeAlerta):

    def __init__(self, winsound_modulo=None) -> None:
        # `winsound_modulo` existe para os testes trocarem o winsound por um falso.
        if winsound_modulo is None:
            import winsound as winsound_modulo  # só existe no Windows
        self._winsound = winsound_modulo
        self._pasta = Path(tempfile.mkdtemp(prefix="helios_alerta_"))
        self._arquivos: Dict[PadraoSonoro, Path] = {}

    def tocar(self, padrao: PadraoSonoro) -> None:
        w = self._winsound
        # SND_LOOP exige SND_ASYNC. Tocar da memória não funciona em modo assíncrono,
        # por isso o WAV vai para um arquivo.
        w.PlaySound(str(self._arquivo_do(padrao)), w.SND_FILENAME | w.SND_ASYNC | w.SND_LOOP)

    def parar(self) -> None:
        self._winsound.PlaySound(None, self._winsound.SND_PURGE)

    def liberar(self) -> None:
        self.parar()
        shutil.rmtree(self._pasta, ignore_errors=True)
        self._arquivos.clear()

    def _arquivo_do(self, padrao: PadraoSonoro) -> Path:
        """Gera o WAV na primeira vez que o padrão é usado e reaproveita nas próximas."""
        if padrao not in self._arquivos:
            caminho = self._pasta / f"padrao_{len(self._arquivos)}.wav"
            caminho.write_bytes(gerar_wav(padrao))
            self._arquivos[padrao] = caminho
        return self._arquivos[padrao]


class AlertaSilencioso(PortaSaidaDeAlerta):
    """Não toca nada. Usado quando não há como tocar som neste sistema."""

    def tocar(self, padrao: PadraoSonoro) -> None:
        pass

    def parar(self) -> None:
        pass

    def liberar(self) -> None:
        pass


def criar_saida_de_alerta(sistema: Optional[str] = None) -> PortaSaidaDeAlerta:
    """Escolhe a saída de alerta certa para o sistema operacional."""
    if (sistema or sys.platform).startswith("win"):
        try:
            return AlertaSonoroWindows()
        except ImportError:
            pass
    print("⚠️  Alerta sonoro indisponível neste sistema (só há suporte no Windows). "
          "O alerta continua visual.")
    return AlertaSilencioso()
