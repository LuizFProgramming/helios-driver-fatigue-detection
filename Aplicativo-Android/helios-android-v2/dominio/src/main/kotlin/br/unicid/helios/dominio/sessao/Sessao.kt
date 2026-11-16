package br.unicid.helios.dominio.sessao

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.TipoEvento
import kotlin.math.sqrt

private const val UM_MINUTO_MS = 60_000.0
private const val SEGUNDOS_POR_MINUTO = 60.0
private const val PERCENTIL = 95
private const val CEM = 100
private const val JANELA_PADRAO_MS = 30_000L
private const val LACUNA_PADRAO_MS = 1_000L
private const val INTERVALO_FOTOS_PADRAO_MS = 30_000L

/** Estados que tocam alarme. SEM_ROSTO não entra: é a câmera que perdeu o rosto por instantes. */
fun EstadoMotorista.ehAlerta(): Boolean = this in ESTADOS_DE_ALERTA

private val ESTADOS_COM_FOTO = setOf(
    EstadoMotorista.FADIGA,
    EstadoMotorista.CRITICO,
    EstadoMotorista.DESATENTO,
    EstadoMotorista.DISTRAIDO,
)

/** O aviso curto (ATENCAO) conta como alerta no tempo da viagem, mas não tira foto: dura no máximo 1,5 s. */
private val ESTADOS_DE_ALERTA = ESTADOS_COM_FOTO + EstadoMotorista.ATENCAO

/**
 * Resumo de um trecho fixo da viagem (30 s por padrão). Uma linha destas por janela vira o
 * `janelas.csv` e é o que um classificador treinado (trabalho futuro) usaria como entrada.
 */
data class JanelaResumo(
    val inicioMs: Long,
    val fimMs: Long,
    val perclosLongo: Double,
    val piscadasPorMinuto: Double,
    val duracaoMediaPiscadaMs: Double,
    val bocejosPorMinuto: Double,
    val desvioPitchGraus: Double,
    /** Fração dos frames com o olhar fora da via. */
    val fracaoOlharFora: Double = 0.0,
    /** Fração dos frames com os olhos semicerrados. */
    val fracaoSemicerrado: Double = 0.0,
    /** Tempo em cada estado. Intervalos acima da lacuna máxima não contam: a soma é a duração útil da janela. */
    val tempoPorEstadoMs: Map<EstadoMotorista, Long> = emptyMap(),
    /** Leituras na janela divididas pela duração real, em segundos. */
    val fpsMedio: Double = 0.0,
    /**
     * Latência de cada leitura: do instante da imagem ([Leitura.timestampMs]) até ela chegar ao domínio no app.
     * Com a ESP32-CAM o relógio começa quando o JPEG chega ao aparelho: a captura na placa e o Wi-Fi ficam de fora.
     */
    val latenciaMediaMs: Double = 0.0,
    /** Percentil 95 da latência, ver [percentil95]. */
    val latenciaP95Ms: Long = 0,
)

/** A "análise completa" de uma viagem, montada só com números (nenhuma imagem). */
data class ResumoSessao(
    val inicioMs: Long,
    val fimMs: Long,
    val tempoPorEstadoMs: Map<EstadoMotorista, Long>,
    val eventosPorTipo: Map<TipoEvento, Int>,
    val janelas: List<JanelaResumo>,
) {
    val duracaoMs: Long get() = fimMs - inicioMs
    val tempoEmAlertaMs: Long get() = tempoPorEstadoMs.filterKeys { it.ehAlerta() }.values.sum()
    val perclosMedio: Double get() = if (janelas.isEmpty()) 0.0 else janelas.sumOf { it.perclosLongo } / janelas.size
    val perclosMaximo: Double get() = janelas.maxOfOrNull { it.perclosLongo } ?: 0.0
}

/**
 * Junta as leituras do monitor numa sessão: tempo em cada estado, eventos por tipo e janelas de
 * [janelaMs]. Intervalos maiores que [lacunaMaximaMs] (app pausado, câmera caiu) não contam tempo.
 * Não é thread-safe: usar na mesma thread do monitor.
 */
class AgregadorSessao(
    private val janelaMs: Long = JANELA_PADRAO_MS,
    private val lacunaMaximaMs: Long = LACUNA_PADRAO_MS,
) {
    private var inicioMs: Long? = null
    private var anterior: Leitura? = null
    private val tempoPorEstado = mutableMapOf<EstadoMotorista, Long>()
    private val eventosPorTipo = mutableMapOf<TipoEvento, Int>()
    private val janelas = mutableListOf<JanelaResumo>()
    private var janela: AcumuladorJanela? = null

    // shortcut: guarda todas as mudanças de estado da viagem (poucos KB numa viagem longa); podar as antigas se pesar.
    private val mudancas = mutableListOf<Pair<Long, EstadoMotorista>>()

    /**
     * Devolve a janela que acabou de fechar, se esta leitura passou do limite dela.
     * [latenciaMs] é medida pelo app (ver [JanelaResumo.latenciaMediaMs]).
     */
    fun registrar(leitura: Leitura, latenciaMs: Long = 0L): JanelaResumo? {
        val t = leitura.timestampMs
        val inicio = inicioMs ?: t.also { inicioMs = it }
        val util = acumularTempo(leitura)
        if (mudancas.lastOrNull()?.second != leitura.estado) mudancas += t to leitura.estado
        leitura.eventos.forEach { eventosPorTipo.merge(it.tipo, 1, Int::plus) }
        val atual = janela ?: AcumuladorJanela(inicio, janelaMs)
        util?.let { atual.somarTempo(it, t) }
        val fechada = if (t >= atual.inicioMs + janelaMs) fecharEAbrir(atual, inicio, t, util) else null
        val alvo = janela ?: atual.also { janela = it }
        alvo.adicionar(leitura, latenciaMs)
        anterior = leitura
        return fechada
    }

    /**
     * Estado da última leitura até [t], ou da primeira da viagem se [t] vier antes dela.
     * É o `estado_inicio` de um evento. Chamar depois do primeiro [registrar].
     */
    fun estadoEm(t: Long): EstadoMotorista = (mudancas.lastOrNull { it.first <= t } ?: mudancas.first()).second

    fun resumo(): ResumoSessao {
        val fim = anterior?.timestampMs ?: 0L
        val parcial = janela?.fechar(fim)
        return ResumoSessao(
            inicioMs = inicioMs ?: 0L,
            fimMs = fim,
            tempoPorEstadoMs = tempoPorEstado.toMap(),
            eventosPorTipo = eventosPorTipo.toMap(),
            janelas = janelas + listOfNotNull(parcial),
        )
    }

    /** [util] é a leitura anterior quando o intervalo conta: a parte depois da fronteira vai para a janela nova. */
    private fun fecharEAbrir(atual: AcumuladorJanela, inicio: Long, t: Long, util: Leitura?): JanelaResumo {
        val fechada = atual.fechar(atual.inicioMs + janelaMs)
        janelas += fechada
        val passos = (t - inicio) / janelaMs
        val nova = AcumuladorJanela(inicio + passos * janelaMs, janelaMs)
        util?.let { nova.somarTempo(it, t) }
        janela = nova
        return fechada
    }

    /** Soma o intervalo desde a leitura anterior no estado dela e a devolve; nulo se o intervalo não conta. */
    private fun acumularTempo(leitura: Leitura): Leitura? {
        val ant = anterior?.takeIf { leitura.timestampMs - it.timestampMs in 1..lacunaMaximaMs } ?: return null
        tempoPorEstado.merge(ant.estado, leitura.timestampMs - ant.timestampMs, Long::plus)
        return ant
    }
}

/** Média; sem amostras (janela sem leitura), zero. */
internal fun media(valores: List<Long>): Double = if (valores.isEmpty()) 0.0 else valores.average()

/** Percentil 95 pelo posto mais próximo: o valor na posição ceil(0,95 × n) da lista ordenada. Sem amostras, zero. */
internal fun percentil95(valores: List<Long>): Long =
    if (valores.isEmpty()) 0L else valores.sorted()[(PERCENTIL * valores.size + CEM - 1) / CEM - 1]

/** Acumula uma janela. Desvio padrão pelo método de Welford, sem guardar as amostras. */
private class AcumuladorJanela(val inicioMs: Long, private val janelaMs: Long) {
    private var somaPerclos = 0.0
    private var amostrasPerclos = 0
    private var framesOlharFora = 0
    private var framesSemicerrados = 0
    private var piscadas = 0
    private var somaDuracaoPiscadas = 0L
    private var bocejos = 0
    private var nPitch = 0
    private var mediaPitch = 0.0
    private var m2Pitch = 0.0
    private val latencias = mutableListOf<Long>()
    private val tempoPorEstado = mutableMapOf<EstadoMotorista, Long>()

    /** Parte do intervalo de [anterior] até [ateMs] que cai dentro desta janela, no estado de [anterior]. */
    fun somarTempo(anterior: Leitura, ateMs: Long) {
        val dentro = minOf(ateMs, inicioMs + janelaMs) - maxOf(anterior.timestampMs, inicioMs)
        tempoPorEstado.merge(anterior.estado, dentro, Long::plus)
    }

    fun adicionar(leitura: Leitura, latenciaMs: Long) {
        latencias += latenciaMs
        leitura.metricas?.let { m ->
            somaPerclos += m.perclosLongo
            amostrasPerclos++
            if (m.olhar.fora) framesOlharFora++
            if (m.olhosSemicerrados) framesSemicerrados++
            m.pose?.let { adicionarPitch(it.pitchGraus) }
        }
        leitura.eventos.forEach { e ->
            when (e.tipo) {
                TipoEvento.PISCADA, TipoEvento.PISCADA_LONGA -> {
                    piscadas++
                    somaDuracaoPiscadas += e.duracaoMs
                }
                TipoEvento.BOCEJO -> bocejos++
                else -> Unit
            }
        }
    }

    /** Taxas por minuto sobre a duração real: a última janela de uma viagem costuma ser mais curta. */
    fun fechar(fimMs: Long): JanelaResumo {
        val duracao = minOf(fimMs - inicioMs, janelaMs)
        val porMinuto = if (duracao > 0) UM_MINUTO_MS / duracao else 0.0
        return JanelaResumo(
            inicioMs = inicioMs,
            fimMs = fimMs,
            perclosLongo = if (amostrasPerclos == 0) 0.0 else somaPerclos / amostrasPerclos,
            piscadasPorMinuto = piscadas * porMinuto,
            duracaoMediaPiscadaMs = if (piscadas == 0) 0.0 else somaDuracaoPiscadas.toDouble() / piscadas,
            bocejosPorMinuto = bocejos * porMinuto,
            desvioPitchGraus = if (nPitch == 0) 0.0 else sqrt(m2Pitch / nPitch),
            fracaoOlharFora = fracao(framesOlharFora),
            fracaoSemicerrado = fracao(framesSemicerrados),
            tempoPorEstadoMs = tempoPorEstado.toMap(),
            fpsMedio = latencias.size * porMinuto / SEGUNDOS_POR_MINUTO,
            latenciaMediaMs = media(latencias),
            latenciaP95Ms = percentil95(latencias),
        )
    }

    private fun fracao(frames: Int) = if (amostrasPerclos == 0) 0.0 else frames.toDouble() / amostrasPerclos

    private fun adicionarPitch(valor: Double) {
        nPitch++
        val delta = valor - mediaPitch
        mediaPitch += delta / nPitch
        m2Pitch += delta * (valor - mediaPitch)
    }
}

/**
 * Quando guardar uma foto do motorista (só se ele consentiu, decidido no app).
 * Captura ao ENTRAR num dos [estados], no máximo uma a cada [intervaloMinimoMs],
 * para não encher a pasta durante um alerta longo.
 */
class PoliticaEvidencia(
    private val estados: Set<EstadoMotorista> = ESTADOS_COM_FOTO,
    private val intervaloMinimoMs: Long = INTERVALO_FOTOS_PADRAO_MS,
) {
    private var anterior: EstadoMotorista? = null
    private var ultimaCapturaMs: Long? = null

    fun avaliar(leitura: Leitura): Boolean {
        val entrou = leitura.estado != anterior && leitura.estado in estados
        anterior = leitura.estado
        val liberado = ultimaCapturaMs?.let { leitura.timestampMs - it >= intervaloMinimoMs } ?: true
        if (!entrou || !liberado) return false
        ultimaCapturaMs = leitura.timestampMs
        return true
    }
}
