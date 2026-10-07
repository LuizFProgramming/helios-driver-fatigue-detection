package br.unicid.helios.dominio.calibracao

/** Referência do motorista: EAR com olhos abertos e pitch olhando para a frente (RF12). */
data class Calibracao(val earReferencia: Double, val pitchReferencia: Double)

/**
 * Junta amostras durante [duracaoMs] (5 s por padrão) com o motorista olhando para a frente.
 * Usa a mediana, que ignora as piscadas que acontecerem no meio da calibração.
 */
class Calibrador(private val inicioMs: Long, private val duracaoMs: Long = 5_000, private val minimoAmostras: Int = 15) {
    private val ears = mutableListOf<Double>()
    private val pitches = mutableListOf<Double>()

    fun adicionar(earBruto: Double, pitchGraus: Double?) {
        ears += earBruto
        pitchGraus?.let { pitches += it }
    }

    fun progresso(agoraMs: Long): Double = ((agoraMs - inicioMs).toDouble() / duracaoMs).coerceIn(0.0, 1.0)

    /** Nulo enquanto o tempo não acabou ou se faltaram amostras (rosto não apareceu). */
    fun resultado(agoraMs: Long): Calibracao? {
        if (progresso(agoraMs) < 1.0 || ears.size < minimoAmostras) return null
        return Calibracao(mediana(ears), if (pitches.isEmpty()) 0.0 else mediana(pitches))
    }
}

internal fun mediana(valores: List<Double>): Double {
    val ordenados = valores.sorted()
    val meio = ordenados.size / 2
    return if (ordenados.size % 2 == 1) ordenados[meio] else (ordenados[meio - 1] + ordenados[meio]) / 2.0
}
