"""
domain/alert_pattern.py — Padrão sonoro de cada categoria de alerta

Módulo puro: não toca em som nenhum, só descreve e gera o sinal do bipe.
Quem toca é um adaptador (adapters/audio_alert.py), por trás da porta
ports/alert_output_port.py.

A ideia é a mesma do app Android (protocolos/Alarme.kt): cada categoria tem
um padrão (tom + bipe + silêncio) e o ciclo é gerado em PCM 16 bits mono.
Os valores ficam em config.py (PADROES_SONOROS).
"""

import io
import math
import sys
import wave
from array import array
from dataclasses import dataclass
from typing import Optional

from config import (
    FADE_DO_BIPE_MS,
    PADROES_SONOROS,
    TAXA_AMOSTRAGEM_ALERTA_HZ,
)

AMPLITUDE_MAXIMA = 32767   # maior valor de uma amostra de 16 bits com sinal


@dataclass(frozen=True)
class PadraoSonoro:
    """Um bipe que se repete: tom durante `ligado_ms`, silêncio durante `desligado_ms`."""
    frequencia_hz: int
    ligado_ms: int
    desligado_ms: int
    volume: float          # 0.0 a 1.0

    @property
    def duracao_do_ciclo_ms(self) -> int:
        return self.ligado_ms + self.desligado_ms


def padrao_para_categoria(categoria: str) -> Optional[PadraoSonoro]:
    """Padrão da categoria, ou None se ela deve ficar em silêncio (ATENTO, PISCANDO, SEM_ROSTO)."""
    dados = PADROES_SONOROS.get(categoria)
    return None if dados is None else PadraoSonoro(**dados)


def gerar_ciclo_pcm(padrao: PadraoSonoro, taxa_hz: int = TAXA_AMOSTRAGEM_ALERTA_HZ) -> array:
    """
    Um ciclo do alarme: o bipe seguido do silêncio, em PCM 16 bits mono.

    O início e o fim do bipe têm uma rampa curta (FADE_DO_BIPE_MS) para não
    estalar no alto-falante.
    """
    amostras_ligado = taxa_hz * padrao.ligado_ms // 1000
    amostras_total = amostras_ligado + taxa_hz * padrao.desligado_ms // 1000
    amostras_fade = min(taxa_hz * FADE_DO_BIPE_MS // 1000, amostras_ligado // 2)
    amplitude = AMPLITUDE_MAXIMA * max(0.0, min(1.0, padrao.volume))

    ciclo = array("h", bytes(2 * amostras_total))      # já começa todo em silêncio
    for i in range(amostras_ligado):
        ganho = 1.0
        if amostras_fade:
            ganho = min(1.0, i / amostras_fade, (amostras_ligado - 1 - i) / amostras_fade)
        ciclo[i] = int(amplitude * ganho * math.sin(2 * math.pi * padrao.frequencia_hz * i / taxa_hz))
    return ciclo


def gerar_wav(padrao: PadraoSonoro, repeticoes: int = 1,
              taxa_hz: int = TAXA_AMOSTRAGEM_ALERTA_HZ) -> bytes:
    """Arquivo WAV (mono, 16 bits) com `repeticoes` ciclos do padrão."""
    ciclo = gerar_ciclo_pcm(padrao, taxa_hz)
    if sys.byteorder == "big":      # o formato WAV guarda as amostras em little-endian
        ciclo.byteswap()
    saida = io.BytesIO()
    with wave.open(saida, "wb") as arquivo:
        arquivo.setnchannels(1)
        arquivo.setsampwidth(2)
        arquivo.setframerate(taxa_hz)
        arquivo.writeframes(ciclo.tobytes() * max(1, repeticoes))
    return saida.getvalue()
