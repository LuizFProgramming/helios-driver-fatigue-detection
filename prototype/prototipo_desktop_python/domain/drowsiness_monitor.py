"""
domain/drowsiness_monitor.py — Maquina de estados de sonolencia e fadiga

Monitora EAR (olhos), ECF (blend shapes), MAR (boca/bocejo) e PERCLOS
frame a frame, com suavizacao temporal EMA para reducao de jitter.
"""

import time
from typing import Optional

from .types import (
    AmostraFacial, EstadoMotorista, LeituraMonitoramento,
    CAUSA_OLHOS_FECHADOS, CAUSA_PERCLOS,
)
from .ear_calculator import calcular_ear, calcular_mar
from config import (
    EAR_LIMIAR_OLHO_FECHADO,
    ECF_LIMIAR_OLHO_FECHADO,
    EMA_ALPHA,
    FRAMES_MINIMOS_PARA_CONTAR_PISCADA,
    FRAMES_CONSECUTIVOS_PARA_ALERTA_SONOLENCIA,
    PERCLOS_TAMANHO_DA_JANELA_EM_FRAMES,
    PERCLOS_LIMIAR_RISCO_ELEVADO,
    MAR_LIMIAR_BOCEJO,
    FRAMES_MINIMOS_PARA_CONFIRMAR_BOCEJO,
)


class MonitorDeSonolencia:
    """
    Maquina de estados que classifica o nivel de atencao do motorista.

    Sinais monitorados:
      - EAR geometrico (suavizado por EMA) — olho aberto/fechado
      - ECF (Eye Closure Factor do blend shape) — confirmacao neural
      - MAR geometrico (suavizado por EMA) — abertura da boca / bocejo
      - PERCLOS — percentual de fechamento nos ultimos ~5s
    """

    def __init__(self) -> None:
        # ── Estado dos olhos ──────────────────────────────────────────────────
        self._frames_olho_fechado:  int            = 0
        self._total_piscadas:       int            = 0
        self._em_estado_sonolento:  bool           = False
        self._inicio_sonolencia:    Optional[float] = None
        self._historico_perclos:    list[bool]     = []
        self._perclos_atual:        float          = 0.0

        # ── Estado da boca ────────────────────────────────────────────────────
        self._frames_boca_aberta:   int            = 0
        self._esta_bocejando:       bool           = False
        self._total_bocejos:        int            = 0

        # ── EMA — valores suavizados ──────────────────────────────────────────
        self._ear_ema:  Optional[float] = None   # inicializado no primeiro frame
        self._mar_ema:  Optional[float] = None
        self._ecf_ema:  Optional[float] = None

    # ── Interface publica ──────────────────────────────────────────────────────

    def processar(self, amostra: Optional[AmostraFacial]) -> LeituraMonitoramento:
        if amostra is None:
            return self._leitura_sem_rosto()

        ear = self._suavizar('ear', calcular_ear(amostra.olho_direito),
                                    calcular_ear(amostra.olho_esquerdo))
        mar = self._suavizar_escalar('mar', calcular_mar(amostra.boca))
        ecf = self._suavizar_escalar('ecf', amostra.blend_shapes.media)

        # Olho esta fechado se EAR geometrico OU ECF neural indicar fechamento
        olho_fechado = ear < EAR_LIMIAR_OLHO_FECHADO or ecf > ECF_LIMIAR_OLHO_FECHADO

        self._atualizar_perclos(olho_fechado)
        self._atualizar_estado_olhos(olho_fechado)
        self._atualizar_boca(mar)
        estado, segundos = self._determinar_estado(ear)

        return LeituraMonitoramento(
            causa_alerta       = self._causa_alerta(estado),
            yaw_graus          = amostra.yaw_graus,
            pitch_graus        = amostra.pitch_graus,
            estado             = estado,
            ear_medio          = ear,
            mar                = mar,
            ecf_medio          = ecf,
            perclos            = self._perclos_atual,
            total_piscadas     = self._total_piscadas,
            total_bocejos      = self._total_bocejos,
            segundos_em_alerta = segundos,
            esta_bocejando     = self._esta_bocejando,
            olho_direito       = amostra.olho_direito,
            olho_esquerdo      = amostra.olho_esquerdo,
            boca               = amostra.boca,
            landmarks_todos    = amostra.landmarks_todos,
            iris_direita       = amostra.iris_direita,
            iris_esquerda      = amostra.iris_esquerda,
        )

    # ── Suavizacao EMA ────────────────────────────────────────────────────────

    def _suavizar(self, chave: str, val_direito: float, val_esquerdo: float) -> float:
        """EMA sobre a media de dois valores (ex: EAR direito e esquerdo)."""
        media = (val_direito + val_esquerdo) / 2.0
        return self._suavizar_escalar(chave, media)

    def _suavizar_escalar(self, chave: str, valor: float) -> float:
        """EMA: novo = alpha * atual + (1-alpha) * anterior."""
        attr = f'_{chave}_ema'
        anterior = getattr(self, attr)
        if anterior is None:
            setattr(self, attr, valor)
            return valor
        suavizado = EMA_ALPHA * valor + (1.0 - EMA_ALPHA) * anterior
        setattr(self, attr, suavizado)
        return suavizado

    # ── Logica dos olhos ───────────────────────────────────────────────────────

    def _atualizar_perclos(self, olho_fechado: bool) -> None:
        self._historico_perclos.append(olho_fechado)
        if len(self._historico_perclos) > PERCLOS_TAMANHO_DA_JANELA_EM_FRAMES:
            self._historico_perclos.pop(0)
        self._perclos_atual = sum(self._historico_perclos) / len(self._historico_perclos)

    def _atualizar_estado_olhos(self, olho_fechado: bool) -> None:
        if olho_fechado:
            self._frames_olho_fechado += 1
            if (self._frames_olho_fechado >= FRAMES_CONSECUTIVOS_PARA_ALERTA_SONOLENCIA
                    and not self._em_estado_sonolento):
                self._em_estado_sonolento = True
                self._inicio_sonolencia   = time.monotonic()
        else:
            fechamento = self._frames_olho_fechado
            if FRAMES_MINIMOS_PARA_CONTAR_PISCADA <= fechamento < FRAMES_CONSECUTIVOS_PARA_ALERTA_SONOLENCIA:
                self._total_piscadas += 1
            self._frames_olho_fechado = 0
            self._em_estado_sonolento = False
            self._inicio_sonolencia   = None

    # ── Logica da boca ────────────────────────────────────────────────────────

    def _atualizar_boca(self, mar: float) -> None:
        if mar > MAR_LIMIAR_BOCEJO:
            self._frames_boca_aberta += 1
            if self._frames_boca_aberta >= FRAMES_MINIMOS_PARA_CONFIRMAR_BOCEJO:
                self._esta_bocejando = True
        else:
            if self._esta_bocejando:
                self._total_bocejos += 1
            self._esta_bocejando   = False
            self._frames_boca_aberta = 0

    # ── Estado final ──────────────────────────────────────────────────────────

    def _determinar_estado(self, ear: float):
        sonolento = (
            self._em_estado_sonolento
            or self._perclos_atual > PERCLOS_LIMIAR_RISCO_ELEVADO
        )
        if sonolento:
            inicio  = self._inicio_sonolencia or time.monotonic()
            return EstadoMotorista.SONOLENTO, time.monotonic() - inicio
        if self._frames_olho_fechado > 0:
            return EstadoMotorista.PISCANDO, 0.0
        return EstadoMotorista.ATENTO, 0.0

    def _causa_alerta(self, estado: EstadoMotorista) -> Optional[str]:
        """Qual regra disparou o SONOLENTO. Olhos fechados sem parar tem prioridade."""
        if estado != EstadoMotorista.SONOLENTO:
            return None
        if self._em_estado_sonolento:
            return CAUSA_OLHOS_FECHADOS
        return CAUSA_PERCLOS

    def _leitura_sem_rosto(self) -> LeituraMonitoramento:
        return LeituraMonitoramento(
            estado             = EstadoMotorista.SEM_FACE,
            ear_medio          = 0.0,
            mar                = 0.0,
            ecf_medio          = 0.0,
            perclos            = self._perclos_atual,
            total_piscadas     = self._total_piscadas,
            total_bocejos      = self._total_bocejos,
            segundos_em_alerta = 0.0,
            esta_bocejando     = False,
        )
