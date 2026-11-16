package br.unicid.helios.dominio

/**
 * Sensibilidade escolhida em Ajustes (Parte D). Muda só a distração e a fadiga acumulada; os limiares de sono
 * (olho fechado de 1,5, 3 e 6 s, cabeça caída) vêm da Euro NCAP e nunca mudam (decisão do grupo).
 */
enum class Sensibilidade { SUAVE, PADRAO, ALTA }

private data class Ajuste(val olharMs: Long, val giroMs: Long, val acumuladaMs: Long, val perclos: Double)

private val AJUSTES = mapOf(
    Sensibilidade.SUAVE to Ajuste(olharMs = 4_000, giroMs = 3_000, acumuladaMs = 12_000, perclos = 0.20),
    Sensibilidade.ALTA to Ajuste(olharMs = 2_500, giroMs = 1_500, acumuladaMs = 8_000, perclos = 0.12),
)

/** A configuração com a [sensibilidade]; a padrão devolve a própria configuração. */
fun ConfiguracaoDeteccao.com(sensibilidade: Sensibilidade): ConfiguracaoDeteccao {
    val a = AJUSTES[sensibilidade] ?: return this
    return copy(
        olharDesviadoMinMs = a.olharMs,
        giroCabecaMinMs = a.giroMs,
        distracaoAcumuladaMs = a.acumuladaMs,
        limiarPerclosFadiga = a.perclos,
    )
}
