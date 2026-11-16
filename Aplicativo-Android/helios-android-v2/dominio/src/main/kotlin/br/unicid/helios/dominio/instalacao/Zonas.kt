package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.calibracao.desvioPadrao
import br.unicid.helios.dominio.calibracao.mediana
import br.unicid.helios.dominio.tipos.PoseCabeca

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
