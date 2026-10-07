package br.unicid.helios.dominio.monitor

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.classificacao.Classificador
import br.unicid.helios.dominio.classificacao.Indicadores
import br.unicid.helios.dominio.eventos.DetectorSustentado
import br.unicid.helios.dominio.eventos.Fase
import br.unicid.helios.dominio.eventos.classificarFechamento
import br.unicid.helios.dominio.eventos.duracaoEmCurso
import br.unicid.helios.dominio.geometria.earMedio
import br.unicid.helios.dominio.geometria.razaoAspecto
import br.unicid.helios.dominio.inercial.AnalisadorMovimento
import br.unicid.helios.dominio.sinais.ContadorEmJanela
import br.unicid.helios.dominio.sinais.Perclos
import br.unicid.helios.dominio.sinais.SuavizadorEma
import br.unicid.helios.dominio.tipos.AmostraFacial
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Evento
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.dominio.tipos.PoseCabeca
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.dominio.tipos.TipoEvento

/** Números do frame atual. `olhoFechado` é o mesmo critério do estado, para a tela usar (corrige o P5). */
data class Metricas(
    val earBruto: Double,
    val earExibicao: Double,
    val marExibicao: Double,
    val ecf: Double,
    val perclosCurto: Double,
    val perclosLongo: Double,
    val olhoFechado: Boolean,
    val bocaAberta: Boolean,
    val pose: PoseCabeca?,
)

data class Totais(val piscadas: Int, val piscadasLongas: Int, val microssonos: Int, val bocejos: Int)

data class Leitura(
    val timestampMs: Long,
    val estado: EstadoMotorista,
    val motivos: Set<Motivo>,
    val metricas: Metricas?,
    val totais: Totais,
    val segundosEmCritico: Double,
    val emMovimento: Boolean,
    val eventos: List<Evento>,
)

/**
 * Orquestra as etapas "Extração de características" → "Análise dos sinais" →
 * "Classificação do estado" do pipeline. Não conhece câmera, MediaPipe nem Android.
 *
 * Não é thread-safe: o app chama [processar] e [registrarAceleracao] sempre da mesma thread.
 */
class MonitorMotorista(
    private val cfg: ConfiguracaoDeteccao = ConfiguracaoDeteccao(),
    private var calibracao: Calibracao? = null,
) {
    private val classificador = Classificador(cfg)
    private val movimento = AnalisadorMovimento(cfg)
    private val olhos = DetectorSustentado()
    private val boca = DetectorSustentado()
    private val cabeca = DetectorSustentado()
    private val perclosCurto = Perclos(cfg.janelaPerclosCurtoMs, cfg.coberturaMinimaPerclos, cfg.lacunaMaximaMs)
    private val perclosLongo = Perclos(cfg.janelaPerclosLongoMs, cfg.coberturaMinimaPerclos, cfg.lacunaMaximaMs)
    private val bocejosRecentes = ContadorEmJanela(cfg.janelaBocejosMs)
    private val piscadasLongasRecentes = ContadorEmJanela(cfg.janelaPiscadasLongasMs)
    private val emaEar = SuavizadorEma(cfg.alphaEma)
    private val emaMar = SuavizadorEma(cfg.alphaEma)

    private var totais = Totais(0, 0, 0, 0)
    private var inicioCriticoMs: Long? = null
    private val eventosInerciais = mutableListOf<Evento>()

    fun calibrar(nova: Calibracao) {
        calibracao = nova
    }

    /** Amostra do acelerômetro linear (m/s²). Pode chegar em qualquer ritmo. */
    fun registrarAceleracao(timestampMs: Long, magnitude: Double) {
        if (movimento.registrar(timestampMs, magnitude)) {
            eventosInerciais += Evento(TipoEvento.EVENTO_BRUSCO, timestampMs, 0)
        }
    }

    fun processar(quadro: Quadro): Leitura {
        val amostra = quadro.amostra ?: return leituraSemRosto(quadro.timestampMs)
        val t = quadro.timestampMs
        val sinais = extrair(amostra)
        val eventos = mutableListOf<Evento>()
        eventos += drenarEventosInerciais()
        val faseOlhos = olhos.atualizar(t, sinais.olhoFechado)
        registrarFechamento(faseOlhos, t)?.let { eventos += it }
        val faseBoca = boca.atualizar(t, sinais.bocaAberta)
        registrarBocejo(faseBoca, t)?.let { eventos += it }
        val faseCabeca = cabeca.atualizar(t, cabecaAbaixada(amostra.pose))
        eventoCabeca(faseCabeca)?.let { eventos += it }
        val metricas = sinais.paraMetricas(
            perclosCurto.registrar(t, sinais.olhoFechado),
            perclosLongo.registrar(t, sinais.olhoFechado),
            amostra.pose,
        )
        val indicadores = Indicadores(
            microssonoEmCurso = faseOlhos.duracaoEmCurso() >= cfg.microssonoMinMs,
            cabecaCaida = faseCabeca.duracaoEmCurso() >= cfg.cabecaCaidaMinMs,
            perclosCurto = metricas.perclosCurto,
            perclosLongo = metricas.perclosLongo,
            bocejosRecentes = bocejosRecentes.contar(t),
            piscadasLongasRecentes = piscadasLongasRecentes.contar(t),
            eventoBruscoRecente = movimento.eventoBruscoRecente(t),
        )
        val classificacao = classificador.classificar(indicadores)
        return montarLeitura(t, classificacao.estado, classificacao.motivos, metricas, eventos)
    }

    // ── Extração ─────────────────────────────────────────────────────────────

    private data class Sinais(
        val earBruto: Double,
        val earExibicao: Double,
        val marExibicao: Double,
        val ecf: Double,
        val olhoFechado: Boolean,
        val bocaAberta: Boolean,
    ) {
        fun paraMetricas(curto: Double, longo: Double, pose: PoseCabeca?) =
            Metricas(earBruto, earExibicao, marExibicao, ecf, curto, longo, olhoFechado, bocaAberta, pose)
    }

    private fun extrair(a: AmostraFacial): Sinais {
        val ear = earMedio(a.olhoDireito, a.olhoEsquerdo)
        val mar = razaoAspecto(a.boca)
        val ecf = (a.piscadaDireita + a.piscadaEsquerda) / 2.0
        return Sinais(
            earBruto = ear,
            earExibicao = emaEar.suavizar(ear),
            marExibicao = emaMar.suavizar(mar),
            ecf = ecf,
            olhoFechado = ear < limiarEar() || ecf > cfg.limiarEcf,
            bocaAberta = mar > cfg.limiarMar || a.aberturaMandibula > cfg.limiarMandibula,
        )
    }

    private fun limiarEar(): Double =
        calibracao?.let { it.earReferencia * cfg.fatorLimiarCalibrado } ?: cfg.limiarEar

    private fun cabecaAbaixada(pose: PoseCabeca?): Boolean {
        if (pose == null) return false
        val referencia = calibracao?.pitchReferencia ?: 0.0
        return referencia - pose.pitchGraus > cfg.quedaCabecaGraus
    }

    // ── Eventos ──────────────────────────────────────────────────────────────

    private fun registrarFechamento(fase: Fase, t: Long): Evento? {
        if (fase !is Fase.Encerrada) return null
        val tipo = classificarFechamento(fase.duracaoMs, cfg) ?: return null
        totais = when (tipo) {
            TipoEvento.PISCADA -> totais.copy(piscadas = totais.piscadas + 1)
            TipoEvento.PISCADA_LONGA -> totais.copy(piscadasLongas = totais.piscadasLongas + 1)
            else -> totais.copy(microssonos = totais.microssonos + 1)
        }
        if (tipo != TipoEvento.PISCADA) piscadasLongasRecentes.registrar(t)
        return Evento(tipo, fase.inicioMs, fase.duracaoMs)
    }

    private fun registrarBocejo(fase: Fase, t: Long): Evento? {
        if (fase !is Fase.Encerrada || fase.duracaoMs < cfg.bocejoMinMs) return null
        totais = totais.copy(bocejos = totais.bocejos + 1)
        bocejosRecentes.registrar(t)
        return Evento(TipoEvento.BOCEJO, fase.inicioMs, fase.duracaoMs)
    }

    private fun eventoCabeca(fase: Fase): Evento? {
        if (fase !is Fase.Encerrada || fase.duracaoMs < cfg.cabecaCaidaMinMs) return null
        return Evento(TipoEvento.CABECA_CAIDA, fase.inicioMs, fase.duracaoMs)
    }

    private fun drenarEventosInerciais(): List<Evento> {
        val copia = eventosInerciais.toList()
        eventosInerciais.clear()
        return copia
    }

    // ── Saída ────────────────────────────────────────────────────────────────

    private fun montarLeitura(
        t: Long,
        estado: EstadoMotorista,
        motivos: Set<Motivo>,
        metricas: Metricas?,
        eventos: List<Evento>,
    ): Leitura {
        val inicio = atualizarInicioCritico(t, estado)
        val segundos = inicio?.let { (t - it) / 1000.0 } ?: 0.0
        return Leitura(t, estado, motivos, metricas, totais, segundos, movimento.emMovimento(), eventos)
    }

    /** Registra o início do CRÍTICO seja qual for o gatilho (corrige o P4). */
    private fun atualizarInicioCritico(t: Long, estado: EstadoMotorista): Long? {
        inicioCriticoMs = if (estado == EstadoMotorista.CRITICO) inicioCriticoMs ?: t else null
        return inicioCriticoMs
    }

    /** Sem rosto os detectores ficam pausados; o PERCLOS ignora a lacuna quando o rosto volta. */
    private fun leituraSemRosto(t: Long): Leitura =
        montarLeitura(t, EstadoMotorista.SEM_ROSTO, emptySet(), null, drenarEventosInerciais())
}
