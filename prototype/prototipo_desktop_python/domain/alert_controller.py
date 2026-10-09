"""
domain/alert_controller.py — Decide quando o alerta sonoro toca e quando para

Recebe a categoria de cada frame (ATENTO, PISCANDO, DESATENTO, SONOLENCIA,
DORMINDO...) e manda para a saída de alerta só o que mudou:

  - entrou numa categoria de alerta  → começa a tocar o padrão dela;
  - mudou de uma categoria de alerta para outra → troca o padrão na hora;
  - a categoria voltou ao normal → continua tocando por mais
    SEGUNDOS_SEM_ALERTA_PARA_SILENCIAR segundos e só então para.

Esse último passo existe porque a categoria muda a 30 fps: sem ele, um olho
que abre por 1 frame cortaria o som e logo o recomeçaria.
"""

from typing import Optional

from config import ALERTA_SONORO_ATIVO, SEGUNDOS_SEM_ALERTA_PARA_SILENCIAR
from domain.alert_pattern import PadraoSonoro, padrao_para_categoria
from ports.alert_output_port import PortaSaidaDeAlerta


class ControladorDeAlerta:

    def __init__(
        self,
        saida: PortaSaidaDeAlerta,
        ativo: bool = ALERTA_SONORO_ATIVO,
        segundos_para_silenciar: float = SEGUNDOS_SEM_ALERTA_PARA_SILENCIAR,
    ) -> None:
        self._saida = saida
        self._mudo = not ativo
        self._segundos_para_silenciar = segundos_para_silenciar
        self._tocando: Optional[PadraoSonoro] = None
        self._ultimo_alerta: float = 0.0

    @property
    def mudo(self) -> bool:
        return self._mudo

    @property
    def tocando(self) -> Optional[PadraoSonoro]:
        """Padrão que está tocando agora, ou None."""
        return self._tocando

    def atualizar(self, categoria: str, agora: float) -> None:
        """Chamar uma vez por frame. `agora` em segundos (time.monotonic())."""
        if self._mudo:
            return

        padrao = padrao_para_categoria(categoria)
        if padrao is not None:
            self._ultimo_alerta = agora
            if padrao != self._tocando:
                self._saida.tocar(padrao)
                self._tocando = padrao
            return

        if self._tocando is not None and agora - self._ultimo_alerta >= self._segundos_para_silenciar:
            self._silenciar()

    def alternar_mudo(self) -> bool:
        """Liga/desliga o mudo. Devolve True se ficou mudo. Mudar para mudo corta o som na hora."""
        self._mudo = not self._mudo
        if self._mudo:
            self._silenciar()
        return self._mudo

    def liberar(self) -> None:
        self._silenciar()
        self._saida.liberar()

    def _silenciar(self) -> None:
        self._saida.parar()
        self._tocando = None
