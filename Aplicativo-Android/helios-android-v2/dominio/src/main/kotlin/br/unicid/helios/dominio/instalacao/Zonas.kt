package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.calibracao.desvioPadrao
import br.unicid.helios.dominio.calibracao.mediana
import br.unicid.helios.dominio.tipos.PoseCabeca
import kotlin.math.pow

/** Zonas de olhar ligadas à direção (Euro NCAP), gravadas na instalação. A estrada é a referência. */
enum class Zona { RETROVISOR, ESPELHO_ESQUERDO, ESPELHO_DIREITO, PAINEL }

/**
 * Centro e espalhamento de uma zona, **relativos à referência da estrada**: a recalibração curta de cada viagem
 * move a referência e as zonas vão junto. Olhar nulo = não medido (olho baixo ou fechado); a zona usa só a cabeça.
 */
data class ZonaOlhar(
    val zona: Zona,
    val yaw: Double,
    val pitch: Double,
    val olharHorizontal: Double?,
    val olharVertical: Double?,
    val espalhamentoYaw: Double,
    val espalhamentoPitch: Double,
    val espalhamentoOlhar: Double,
)

private const val CENTRO_DO_OLHO = 0.5

/**
 * A zona onde está a olhada, ou nula se ela não cai em nenhuma (ADR-014). Tudo relativo à estrada. A distância é
 * normalizada pelo espalhamento de cada zona; dentro é até 1, e a mais próxima ganha. O olhar só entra na conta
 * quando foi medido agora e na gravação da zona (olho baixo ou fechado não mede): sem ele vale só a cabeça.
 */
fun zonaMaisProxima(yaw: Double, pitch: Double, olharH: Double?, olharV: Double?, zonas: List<ZonaOlhar>): Zona? =
    zonas.map { z -> z.zona to distancia(z, yaw, pitch, olharH, olharV) }
        .filter { it.second <= 1.0 }
        .sortedBy { it.second }
        .firstOrNull()?.first

private fun distancia(z: ZonaOlhar, yaw: Double, pitch: Double, olharH: Double?, olharV: Double?): Double {
    fun termo(medido: Double?, centro: Double?, espalhamento: Double): Double =
        if (medido == null || centro == null) 0.0 else ((medido - centro) / espalhamento).pow(2)
    return termo(yaw, z.yaw, z.espalhamentoYaw) + termo(pitch, z.pitch, z.espalhamentoPitch) +
        termo(olharH, z.olharHorizontal, z.espalhamentoOlhar) + termo(olharV, z.olharVertical, z.espalhamentoOlhar)
}

/**
 * Junta 2 s de amostras com o motorista olhando para a [zona], depois de 1 s de espera para ele levar os olhos
 * até lá (o mesmo critério de mediana do [br.unicid.helios.dominio.calibracao.Calibrador]).
 */
class ColetorZona(
    private val zona: Zona,
    private val inicioMs: Long,
    private val estrada: Calibracao,
    private val cfg: ConfiguracaoInstalacao,
) {
    private val yaws = mutableListOf<Double>()
    private val pitches = mutableListOf<Double>()
    private val horizontais = mutableListOf<Double>()
    private val verticais = mutableListOf<Double>()

    fun adicionar(t: Long, pose: PoseCabeca?, olharHorizontal: Double?, olharVertical: Double?) {
        if (t < inicioMs + cfg.esperaZonaMs || pose == null) return
        yaws += pose.yawGraus - estrada.yawReferencia
        pitches += pose.pitchGraus - estrada.pitchReferencia
        olharHorizontal?.let { horizontais += it - (estrada.olharHorizontal ?: CENTRO_DO_OLHO) }
        olharVertical?.let { verticais += it - (estrada.olharVertical ?: 0.0) }
    }

    /** De 0 a 1, contado depois da espera. */
    fun progresso(t: Long): Double = ((t - inicioMs - cfg.esperaZonaMs).toDouble() / cfg.zonaMs).coerceIn(0.0, 1.0)

    /** Nulo enquanto a coleta não acabou ou se faltaram amostras (rosto sumiu). */
    fun resultado(t: Long): ZonaOlhar? {
        if (progresso(t) < 1.0 || yaws.size < cfg.minimoAmostrasZona) return null
        return ZonaOlhar(
            zona,
            mediana(yaws),
            mediana(pitches),
            horizontais.takeIf { it.isNotEmpty() }?.let(::mediana),
            verticais.takeIf { it.isNotEmpty() }?.let(::mediana),
            espalhamento(desvioPadrao(yaws), cfg.espalhamentoMinGraus),
            espalhamento(desvioPadrao(pitches), cfg.espalhamentoMinGraus),
            espalhamento(maxOf(desvioPadrao(horizontais), desvioPadrao(verticais)), cfg.espalhamentoMinOlhar),
        )
    }

    private fun espalhamento(desvio: Double, minimo: Double) = maxOf(cfg.fatorEspalhamento * desvio, minimo)
}
