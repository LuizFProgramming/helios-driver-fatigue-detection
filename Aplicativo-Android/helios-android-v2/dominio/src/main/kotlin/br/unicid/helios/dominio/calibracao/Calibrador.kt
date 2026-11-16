package br.unicid.helios.dominio.calibracao

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Referência do motorista: EAR com olhos abertos e pitch/yaw olhando para a frente (RF12).
 * [earDireito] e [earEsquerdo] são a referência de cada olho; nulos em calibrações antigas (vale a média).
 * [olharHorizontal] e [olharVertical] são para onde a íris aponta olhando a estrada: com o celular de lado
 * no painel, olhar a estrada não é olhar para a câmera. Nulos = olhar para a câmera (0,5 e 0).
 */
data class Calibracao(
    val earReferencia: Double,
    val pitchReferencia: Double,
    val yawReferencia: Double = 0.0,
    val earDireito: Double? = null,
    val earEsquerdo: Double? = null,
    val olharHorizontal: Double? = null,
    val olharVertical: Double? = null,
)

/** Desvio padrão de cada sinal durante a calibração: mostra se o motorista ficou parado (ADR-013). */
data class Desvios(val pitch: Double, val yaw: Double, val olharHorizontal: Double?, val olharVertical: Double?)

/**
 * Junta amostras durante [duracaoMs] (5 s por padrão) com o motorista olhando para a frente.
 * Usa a mediana, que ignora as piscadas que acontecerem no meio da calibração.
 */
class Calibrador(
    private val inicioMs: Long,
    private val duracaoMs: Long = 5_000,
    private val minimoAmostras: Int = 15,
) {
    private val ears = mutableListOf<Double>()
    private val pitches = mutableListOf<Double>()
    private val yaws = mutableListOf<Double>()
    private val direitos = mutableListOf<Double>()
    private val esquerdos = mutableListOf<Double>()
    private val horizontais = mutableListOf<Double>()
    private val verticais = mutableListOf<Double>()

    @Suppress("LongParameterList") // uma amostra de cada sinal calibrado, todos opcionais menos o EAR
    fun adicionar(
        earBruto: Double,
        pitchGraus: Double?,
        yawGraus: Double? = null,
        earDireito: Double? = null,
        earEsquerdo: Double? = null,
        olharHorizontal: Double? = null,
        olharVertical: Double? = null,
    ) {
        ears += earBruto
        pitchGraus?.let { pitches += it }
        yawGraus?.let { yaws += it }
        earDireito?.let { direitos += it }
        earEsquerdo?.let { esquerdos += it }
        olharHorizontal?.let { horizontais += it }
        olharVertical?.let { verticais += it }
    }

    fun progresso(agoraMs: Long): Double = ((agoraMs - inicioMs).toDouble() / duracaoMs).coerceIn(0.0, 1.0)

    fun desvios() = Desvios(
        desvioPadrao(pitches),
        desvioPadrao(yaws),
        horizontais.takeIf { it.isNotEmpty() }?.let(::desvioPadrao),
        verticais.takeIf { it.isNotEmpty() }?.let(::desvioPadrao),
    )

    /** Nulo enquanto o tempo não acabou ou se faltaram amostras (rosto não apareceu). */
    fun resultado(agoraMs: Long): Calibracao? {
        if (progresso(agoraMs) < 1.0 || ears.size < minimoAmostras) return null
        return Calibracao(
            mediana(ears),
            medianaOuZero(pitches),
            medianaOuZero(yaws),
            medianaOuNulo(direitos),
            medianaOuNulo(esquerdos),
            medianaOuNulo(horizontais),
            medianaOuNulo(verticais),
        )
    }
}

private fun medianaOuNulo(valores: List<Double>): Double? = if (valores.isEmpty()) null else mediana(valores)

private fun medianaOuZero(valores: List<Double>): Double = if (valores.isEmpty()) 0.0 else mediana(valores)

/** Desvio padrão da população; zero sem amostras. */
internal fun desvioPadrao(valores: List<Double>): Double {
    if (valores.isEmpty()) return 0.0
    val media = valores.average()
    return sqrt(valores.sumOf { (it - media).pow(2) } / valores.size)
}

internal fun mediana(valores: List<Double>): Double {
    val ordenados = valores.sorted()
    val meio = ordenados.size / 2
    return if (ordenados.size % 2 == 1) ordenados[meio] else (ordenados[meio - 1] + ordenados[meio]) / 2.0
}
