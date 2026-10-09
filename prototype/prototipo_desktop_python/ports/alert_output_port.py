"""
ports/alert_output_port.py — Contrato para saídas de alerta

Qualquer forma de avisar o motorista (alto-falante do PC, buzzer da
ESP32-CAM, vibração do celular) implementa esta interface. O controlador
(domain/alert_controller.py) só conhece este contrato, então trocar o
alto-falante por outra saída não mexe em nenhuma regra.
"""

from abc import ABC, abstractmethod

from domain.alert_pattern import PadraoSonoro


class PortaSaidaDeAlerta(ABC):
    """Interface abstrata para saídas de alerta."""

    @abstractmethod
    def tocar(self, padrao: PadraoSonoro) -> None:
        """Começa a repetir o padrão sem parar, até alguém chamar parar() ou tocar() com outro."""
        ...

    @abstractmethod
    def parar(self) -> None:
        """Silencia o alerta. Pode ser chamado mesmo sem nada tocando."""
        ...

    @abstractmethod
    def liberar(self) -> None:
        """Para o alerta e libera arquivos e outros recursos."""
        ...
