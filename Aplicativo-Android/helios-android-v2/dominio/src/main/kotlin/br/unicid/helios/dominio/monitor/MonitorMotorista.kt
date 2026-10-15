package br.unicid.helios.dominio.monitor

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.classificacao.Classificador
import br.unicid.helios.dominio.classificacao.Indicadores
import br.unicid.helios.dominio.eventos.DetectorSustentado
import br.unicid.helios.dominio.eventos.Fase
import br.unicid.helios.dominio.eventos.classificarFechamento
import br.unicid.helios.dominio.eventos.duracaoEmCurso
import br.unicid.helios.dominio.geometria.distancia
import br.unicid.helios.dominio.geometria.earMedio
import br.unicid.helios.dominio.geometria.posicaoIris
import br.unicid.helios.dominio.geometria.razaoAspecto
import br.unicid.helios.dominio.inercial.AnalisadorMovimento
import br.unicid.helios.dominio.sinais.ContadorEmJanela
import br.unicid.helios.dominio.sinais.EstatisticaPiscadas
import br.unicid.helios.dominio.sinais.Perclos
import br.unicid.helios.dominio.sinais.SuavizadorEma
import br.unicid.helios.dominio.tipos.AmostraFacial
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Evento
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.dominio.tipos.PoseCabeca
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.dominio.tipos.TipoEvento
import kotlin.math.abs

private const val CENTRO_DO_OLHO = 0.5

/** Qual olho decidiu "olho fechado" neste frame. Com a cabeça de lado, só o mais visível. */
enum class OlhoAvaliado { AMBOS, DIREITO, ESQUERDO }

/**
 * Números do frame atual. `olhoFechado` é o mesmo critério do estado, para a tela usar (corrige o P5).
 * "Direito" e "esquerdo" seguem a malha do MediaPipe (olho direito do motorista = lado esquerdo da imagem).
 */
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
    val earDireito: Double = earBruto,
    val earEsquerdo: Double = earBruto,
    val ecfDireito: Double = ecf,
    val ecfEsquerdo: Double = ecf,
    val olhoDireitoFechado: Boolean = olhoFechado,
    val olhoEsquerdoFechado: Boolean = olhoFechado,
    val olhoAvaliado: OlhoAvaliado = OlhoAvaliado.AMBOS,
    val olhosFechadosMs: Long = 0,
    val aberturaMandibula: Double = 0.0,
    val olhar: MetricasOlhar = MetricasOlhar(),
    val piscadasNoMinuto: Int = 0,
    val duracaoMediaPiscadaMs: Double = 0.0,
    /** Abertura do olho avaliado em relação à calibrada (1 = como na calibração). */
    val aberturaRelativa: Double = 1.0,
    /** Blendshape eyeSquint do olho avaliado: alto = apertando o olho (sol), não sono. */
    val semicerrar: Double = 0.0,
    val olhosSemicerrados: Boolean = false,
    val fracaoSemicerrado: Double = 0.0,
)

/**
 * Para onde o motorista olha. [horizontal] é a posição da íris no olho (0,5 = centro) e [vertical] a
 * diferença dos blendshapes de olhar para cima e para baixo; os desvios são em relação ao calibrado.
 * Nulos quando não deu para medir (olho fechado, malha sem íris).
 */
data class MetricasOlhar(
    val horizontal: Double? = null,
    val vertical: Double? = null,
    val desvioHorizontal: Double? = null,
    val desvioVertical: Double? = null,
    val fora: Boolean = false,
    val foraMs: Long = 0,
    /** Tempo somado com olhar ou cabeça fora da via nos últimos 30 s. */
    val foraDaViaMs: Long = 0,
)

data class Totais(val piscadas: Int, val piscadasLongas: Int, val microssonos: Int, val bocejos: Int)

data class Leitura(
    val timestampMs: Long,
    val estado: EstadoMotorista,
    val motivos: Set<Motivo>,
    val metricas: Metricas?,
    val totais: Totais,
    /** Há quanto tempo o estado atual começou. Alimenta o cronômetro da tela e a escalada do alarme. */
    val segundosNoEstado: Double,
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
    private val giro = DetectorSustentado()
    private val olhar = DetectorSustentado()
    private val foraDaVia = Perclos(cfg.janelaDistracaoMs, cfg.coberturaMinimaPerclos, cfg.lacunaMaximaMs)
    private val semicerrado = Perclos(cfg.janelaSemicerradoMs, cfg.coberturaMinimaPerclos, cfg.lacunaMaximaMs)
    private val piscadas = EstatisticaPiscadas(cfg.janelaPiscadasMs)
    private val perclosCurto = Perclos(cfg.janelaPerclosCurtoMs, cfg.coberturaMinimaPerclos, cfg.lacunaMaximaMs)
    private val perclosLongo = Perclos(cfg.janelaPerclosLongoMs, cfg.coberturaMinimaPerclos, cfg.lacunaMaximaMs)
    private val bocejosRecentes = ContadorEmJanela(cfg.janelaBocejosMs)
    private val piscadasLongasRecentes = ContadorEmJanela(cfg.janelaPiscadasLongasMs)
    private val microssonosRecentes = ContadorEmJanela(cfg.janelaMicrossonosMs)
    private val emaEar = SuavizadorEma(cfg.alphaEma)
    private val emaMar = SuavizadorEma(cfg.alphaEma)

    private var totais = Totais(0, 0, 0, 0)
    private var estadoAtual: EstadoMotorista? = null
    private var inicioEstadoMs = 0L
    private var inicioSemRostoMs: Long? = null
    private var emSono = false
    private var fimSonoMs: Long? = null
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

    /** Se o carro está andando, mesmo sem frames chegando (viagem pausada, câmera parada). */
    fun emMovimento(): Boolean = movimento.emMovimento()

    fun processar(quadro: Quadro): Leitura {
        val amostra = quadro.amostra ?: return leituraSemRosto(quadro.timestampMs)
        val t = quadro.timestampMs
        val sinais = extrair(amostra)
        val eventos = mutableListOf<Evento>()
        eventos += drenarEventosInerciais()
        rostoVoltou(t)?.let { eventos += it }
        val faseOlhos = olhos.atualizar(t, sinais.olhoFechado)
        registrarFechamento(faseOlhos, t)?.let { eventos += it }
        val faseBoca = boca.atualizar(t, sinais.bocaAberta)
        registrarBocejo(faseBoca, t)?.let { eventos += it }
        val faseCabeca = cabeca.atualizar(t, cabecaAbaixada(amostra.pose))
        eventoCabeca(faseCabeca)?.let { eventos += it }
        val viradaAgora = cabecaVirada(amostra.pose)
        val faseGiro = giro.atualizar(t, viradaAgora)
        eventoGiro(faseGiro)?.let { eventos += it }
        val faseOlhar = olhar.atualizar(t, sinais.olhar.fora)
        eventoOlhar(faseOlhar)?.let { eventos += it }
        foraDaVia.registrar(t, sinais.olhar.fora || viradaAgora)
        val olhosFechadosMs = faseOlhos.duracaoEmCurso()
        val resumoPiscadas = piscadas.resumo(t)
        val metricas = sinais.paraMetricas(
            perclosCurto.registrar(t, sinais.olhoFechado),
            perclosLongo.registrar(t, sinais.olhoFechado),
            amostra,
            olhosFechadosMs,
        ).copy(
            olhar = sinais.olhar.copy(foraMs = faseOlhar.duracaoEmCurso(), foraDaViaMs = foraDaVia.tempoAtivoMs()),
            piscadasNoMinuto = resumoPiscadas.quantidade,
            duracaoMediaPiscadaMs = resumoPiscadas.duracaoMediaMs,
            fracaoSemicerrado = semicerrado.registrar(t, sinais.semicerrados),
        )
        val indicadores = Indicadores(
            olhosFechadosMs = olhosFechadosMs,
            cabecaCaida = faseCabeca.duracaoEmCurso() >= cfg.cabecaCaidaMinMs,
            perclosLongo = metricas.perclosLongo,
            bocejosRecentes = bocejosRecentes.contar(t),
            piscadasLongasRecentes = piscadasLongasRecentes.contar(t),
            microssonosRecentes = microssonosRecentes.contar(t),
            eventoBruscoRecente = movimento.eventoBruscoRecente(t),
            cabecaVirada = faseGiro.duracaoEmCurso() >= cfg.giroCabecaMinMs,
            posSono = emSono || fimSonoMs?.let { t - it < cfg.recuperacaoMs } == true,
            olharDesviadoMs = metricas.olhar.foraMs,
            foraDaViaMs = metricas.olhar.foraDaViaMs,
            piscadasNoMinuto = resumoPiscadas.quantidade,
            duracaoMediaPiscadaMs = resumoPiscadas.duracaoMediaMs,
            fracaoSemicerrado = metricas.fracaoSemicerrado,
        )
        val classificacao = classificador.classificar(indicadores)
        acompanharSono(t, classificacao.estado)
        // Durante o próprio CRÍTICO o "pós-sono" ainda não começou: não aparece como motivo.
        val motivos = classificacao.motivos.let { if (classificacao.estado == EstadoMotorista.CRITICO) it - Motivo.POS_SONO else it }
        return montarLeitura(t, classificacao.estado, motivos, metricas, eventos)
    }

    // ── Extração ─────────────────────────────────────────────────────────────

    private data class Olho(val ear: Double, val ecf: Double, val fechado: Boolean)

    private data class Sinais(
        val earBruto: Double,
        val earExibicao: Double,
        val marExibicao: Double,
        val direito: Olho,
        val esquerdo: Olho,
        val avaliado: OlhoAvaliado,
        val olhoFechado: Boolean,
        val bocaAberta: Boolean,
        val olhar: MetricasOlhar,
        val aberturaRelativa: Double,
        val semicerrar: Double,
        val semicerrados: Boolean,
    ) {
        fun paraMetricas(curto: Double, longo: Double, a: AmostraFacial, olhosFechadosMs: Long) = Metricas(
            earBruto = earBruto,
            earExibicao = earExibicao,
            marExibicao = marExibicao,
            ecf = (direito.ecf + esquerdo.ecf) / 2.0,
            perclosCurto = curto,
            perclosLongo = longo,
            olhoFechado = olhoFechado,
            bocaAberta = bocaAberta,
            pose = a.pose,
            earDireito = direito.ear,
            earEsquerdo = esquerdo.ear,
            ecfDireito = direito.ecf,
            ecfEsquerdo = esquerdo.ecf,
            olhoDireitoFechado = direito.fechado,
            olhoEsquerdoFechado = esquerdo.fechado,
            olhoAvaliado = avaliado,
            olhosFechadosMs = olhosFechadosMs,
            aberturaMandibula = a.aberturaMandibula,
            aberturaRelativa = aberturaRelativa,
            semicerrar = semicerrar,
            olhosSemicerrados = semicerrados,
        )
    }

    /**
     * Cada olho é avaliado sozinho (EAR abaixo do seu limiar ou blendshape de piscada alto).
     * O olho conta como fechado só quando os dois estão fechados: piscar um olho não é sono.
     * Com a cabeça de lado, o olho mais distante aparece encurtado e o EAR dele não é confiável:
     * aí vale só o mais largo.
     */
    private fun extrair(a: AmostraFacial): Sinais {
        val earD = razaoAspecto(a.olhoDireito)
        val earE = razaoAspecto(a.olhoEsquerdo)
        val direito = olho(earD, a.piscadaDireita, calibracao?.earDireito)
        val esquerdo = olho(earE, a.piscadaEsquerda, calibracao?.earEsquerdo)
        val avaliado = olhoAvaliado(a)
        val ear = earMedio(a.olhoDireito, a.olhoEsquerdo)
        val mar = razaoAspecto(a.boca)
        val fechado = when (avaliado) {
            OlhoAvaliado.AMBOS -> direito.fechado && esquerdo.fechado
            OlhoAvaliado.DIREITO -> direito.fechado
            OlhoAvaliado.ESQUERDO -> esquerdo.fechado
        }
        val abertura = escolher(avaliado, earD / referencia(calibracao?.earDireito), earE / referencia(calibracao?.earEsquerdo))
        val semicerrar = escolher(avaliado, a.semicerrarDireito, a.semicerrarEsquerdo)
        // Olho baixo sem o músculo de apertar: pálpebra caindo de sono. Com o músculo ativo, é sol ou riso.
        val semicerrados = !fechado && abertura < cfg.fatorSemicerrado && semicerrar < cfg.limiarMusculoSemicerrar
        return Sinais(
            earBruto = ear,
            earExibicao = emaEar.suavizar(ear),
            marExibicao = emaMar.suavizar(mar),
            direito = direito,
            esquerdo = esquerdo,
            avaliado = avaliado,
            olhoFechado = fechado,
            bocaAberta = mar > cfg.limiarMar || a.aberturaMandibula > cfg.limiarMandibula,
            olhar = if (fechado) MetricasOlhar() else medirOlhar(a, avaliado, confiarNoVertical = abertura >= cfg.fatorSemicerrado),
            aberturaRelativa = abertura,
            semicerrar = semicerrar,
            semicerrados = semicerrados,
        )
    }

    private fun escolher(avaliado: OlhoAvaliado, direito: Double, esquerdo: Double) = when (avaliado) {
        OlhoAvaliado.AMBOS -> (direito + esquerdo) / 2.0
        OlhoAvaliado.DIREITO -> direito
        OlhoAvaliado.ESQUERDO -> esquerdo
    }

    /**
     * Olhar em relação ao calibrado. Com o olho fechado não se mede (a pálpebra cobre a íris).
     * Com o olho baixo o blendshape de "olhar para baixo" sobe junto com a pálpebra, então o vertical
     * só vale com o olho aberto: senão sono viraria distração.
     */
    private fun medirOlhar(a: AmostraFacial, avaliado: OlhoAvaliado, confiarNoVertical: Boolean): MetricasOlhar {
        val horizontal = posicaoHorizontal(a, avaliado)
        val vertical = a.olharVertical
        val desvioH = horizontal?.let { it - (calibracao?.olharHorizontal ?: CENTRO_DO_OLHO) }
        val desvioV = vertical?.let { it - (calibracao?.olharVertical ?: 0.0) }
        val foraH = desvioH?.let { abs(it) > cfg.limiarOlharHorizontal } == true
        val foraV = confiarNoVertical && desvioV?.let { abs(it) > cfg.limiarOlharVertical } == true
        return MetricasOlhar(horizontal, vertical, desvioH, desvioV, fora = foraH || foraV)
    }

    private fun posicaoHorizontal(a: AmostraFacial, avaliado: OlhoAvaliado): Double? {
        val d = a.irisDireita?.let { posicaoIris(a.olhoDireito, it) }
        val e = a.irisEsquerda?.let { posicaoIris(a.olhoEsquerdo, it) }
        return when (avaliado) {
            OlhoAvaliado.AMBOS -> media(d, e)
            OlhoAvaliado.DIREITO -> d
            OlhoAvaliado.ESQUERDO -> e
        }
    }

    private fun media(a: Double?, b: Double?): Double? = when {
        a != null && b != null -> (a + b) / 2.0
        else -> a ?: b
    }

    /** EAR de olho aberto que serve de 100%: o calibrado do olho, a média calibrada, ou o implícito no limiar padrão. */
    private fun referencia(doOlho: Double?): Double =
        doOlho ?: calibracao?.earReferencia ?: (cfg.limiarEar / cfg.fatorLimiarCalibrado)

    private fun olho(ear: Double, ecf: Double, referencia: Double?) =
        Olho(ear, ecf, ear < limiarEar(referencia) || ecf > cfg.limiarEcf)

    private fun olhoAvaliado(a: AmostraFacial): OlhoAvaliado {
        val larguraD = distancia(a.olhoDireito.p1, a.olhoDireito.p4)
        val larguraE = distancia(a.olhoEsquerdo.p1, a.olhoEsquerdo.p4)
        return when {
            larguraE < larguraD * cfg.razaoOlhoUnico -> OlhoAvaliado.DIREITO
            larguraD < larguraE * cfg.razaoOlhoUnico -> OlhoAvaliado.ESQUERDO
            else -> OlhoAvaliado.AMBOS
        }
    }

    /** Limiar do olho: referência calibrada dele, senão a média calibrada, senão o padrão (0,19). */
    private fun limiarEar(referenciaDoOlho: Double?): Double {
        val referencia = referenciaDoOlho ?: calibracao?.earReferencia ?: return cfg.limiarEar
        return referencia * cfg.fatorLimiarCalibrado
    }

    /** Marca o fim de um CRÍTICO para manter a FADIGA pós-sono por [ConfiguracaoDeteccao.recuperacaoMs]. */
    private fun acompanharSono(t: Long, estado: EstadoMotorista) {
        val critico = estado == EstadoMotorista.CRITICO
        if (emSono && !critico) fimSonoMs = t
        emSono = critico
    }

    private fun cabecaAbaixada(pose: PoseCabeca?): Boolean {
        if (pose == null) return false
        val referencia = calibracao?.pitchReferencia ?: 0.0
        return referencia - pose.pitchGraus > cfg.quedaCabecaGraus
    }

    /** Giro para o lado em relação ao yaw calibrado. O sinal não importa: esquerda ou direita. */
    private fun cabecaVirada(pose: PoseCabeca?): Boolean {
        if (pose == null) return false
        val referencia = calibracao?.yawReferencia ?: 0.0
        return abs(pose.yawGraus - referencia) > cfg.giroCabecaGraus
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
        if (tipo == TipoEvento.PISCADA || tipo == TipoEvento.PISCADA_LONGA) piscadas.registrar(t, fase.duracaoMs)
        if (tipo != TipoEvento.PISCADA) piscadasLongasRecentes.registrar(t)
        if (tipo == TipoEvento.MICROSSONO || tipo == TipoEvento.SONO) microssonosRecentes.registrar(t)
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

    private fun eventoGiro(fase: Fase): Evento? {
        if (fase !is Fase.Encerrada || fase.duracaoMs < cfg.giroCabecaMinMs) return null
        return Evento(TipoEvento.CABECA_VIRADA, fase.inicioMs, fase.duracaoMs)
    }

    private fun eventoOlhar(fase: Fase): Evento? {
        if (fase !is Fase.Encerrada || fase.duracaoMs < cfg.olharDesviadoMinMs) return null
        return Evento(TipoEvento.OLHAR_DESVIADO, fase.inicioMs, fase.duracaoMs)
    }

    /**
     * O rosto reapareceu: se ficou fora tempo suficiente para DESATENTO, isso vira evento.
     * Depois de uma lacuna longa, olho, boca e cabeça recomeçam do zero: o tempo sem rosto
     * não pode virar "olho fechado por 3 s" só porque o último frame antes da perda era uma piscada.
     */
    private fun rostoVoltou(t: Long): Evento? {
        val inicio = inicioSemRostoMs ?: return null
        inicioSemRostoMs = null
        val duracao = t - inicio
        if (duracao > cfg.lacunaMaximaMs) listOf(olhos, boca, cabeca, giro, olhar).forEach { it.reiniciar() }
        return if (duracao >= cfg.semRostoParaDesatentoMs) Evento(TipoEvento.ROSTO_AUSENTE, inicio, duracao) else null
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
        val segundos = (t - inicioDoEstado(t, estado)) / 1000.0
        return Leitura(t, estado, motivos, metricas, totais, segundos, movimento.emMovimento(), eventos)
    }

    /** Registra o início de cada estado seja qual for o gatilho (corrige o P4 e vale para todos). */
    private fun inicioDoEstado(t: Long, estado: EstadoMotorista): Long {
        if (estado != estadoAtual) {
            estadoAtual = estado
            inicioEstadoMs = t
        }
        return inicioEstadoMs
    }

    /**
     * Sem rosto os detectores ficam pausados; o PERCLOS ignora a lacuna quando o rosto volta.
     * Ausência curta é SEM_ROSTO (câmera perdeu o rosto); longa é DESATENTO (motorista olhando para fora).
     */
    private fun leituraSemRosto(t: Long): Leitura {
        val inicio = inicioSemRostoMs ?: t.also { inicioSemRostoMs = it }
        val desatento = t - inicio >= cfg.semRostoParaDesatentoMs
        val estado = if (desatento) EstadoMotorista.DESATENTO else EstadoMotorista.SEM_ROSTO
        val motivos = if (desatento) setOf(Motivo.ROSTO_AUSENTE) else emptySet()
        return montarLeitura(t, estado, motivos, null, drenarEventosInerciais())
    }
}
