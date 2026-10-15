package br.unicid.helios.dominio.sinais

/**
 * Média móvel exponencial. Uma instância por sinal (corrige o P11).
 * Usada só para exibir números estáveis na tela; a detecção usa o valor bruto (corrige o P3).
 */
class SuavizadorEma(private val alpha: Double) {
    private var atual: Double? = null

    fun suavizar(valor: Double): Double {
        val novo = atual?.let { alpha * valor + (1.0 - alpha) * it } ?: valor
        atual = novo
        return novo
    }
}

/**
 * PERCLOS ponderado por tempo: cada amostra vale pelo intervalo até a próxima.
 * Assim o resultado é o mesmo a 10, 15 ou 30 fps (corrige o P2 e o P12).
 *
 * - Intervalos maiores que [lacunaMaximaMs] (rosto perdido, travamento) não entram na conta.
 * - Enquanto a janela não tiver ao menos [coberturaMinima] de tempo observado, o valor é 0,
 *   para um único frame fechado logo no início não virar PERCLOS = 100%.
 */
class Perclos(
    private val janelaMs: Long,
    private val coberturaMinima: Double,
    private val lacunaMaximaMs: Long,
) {
    private data class Segmento(val inicioMs: Long, val fimMs: Long, val fechado: Boolean) {
        val duracaoMs: Long get() = fimMs - inicioMs
    }

    private val segmentos = ArrayDeque<Segmento>()
    private var ultimoMs: Long? = null
    private var ultimoFechado = false
    private var totalMs = 0L
    private var fechadoMs = 0L

    fun registrar(timestampMs: Long, fechado: Boolean): Double {
        ultimoMs?.let { anterior -> adicionarSeValido(anterior, timestampMs) }
        ultimoMs = timestampMs
        ultimoFechado = fechado
        descartarAntesDe(timestampMs - janelaMs)
        return valor()
    }

    fun valor(): Double {
        if (totalMs < janelaMs * coberturaMinima) return 0.0
        return fechadoMs.toDouble() / totalMs
    }

    /** Tempo absoluto com a condição verdadeira dentro da janela (sem exigir cobertura mínima). */
    fun tempoAtivoMs(): Long = fechadoMs

    private fun adicionarSeValido(anteriorMs: Long, atualMs: Long) {
        val intervalo = atualMs - anteriorMs
        if (intervalo <= 0 || intervalo > lacunaMaximaMs) return
        adicionar(Segmento(anteriorMs, atualMs, ultimoFechado))
    }

    private fun descartarAntesDe(limiteMs: Long) {
        while (segmentos.firstOrNull()?.let { it.inicioMs < limiteMs } == true) {
            val primeiro = segmentos.removeFirst()
            subtrair(primeiro)
            if (primeiro.fimMs > limiteMs) {
                val aparado = primeiro.copy(inicioMs = limiteMs)
                segmentos.addFirst(aparado)
                acumular(aparado, +1)
            }
        }
    }

    private fun adicionar(segmento: Segmento) {
        segmentos.addLast(segmento)
        acumular(segmento, +1)
    }

    private fun subtrair(segmento: Segmento) = acumular(segmento, -1)

    private fun acumular(segmento: Segmento, sinal: Int) {
        totalMs += sinal * segmento.duracaoMs
        if (segmento.fechado) fechadoMs += sinal * segmento.duracaoMs
    }
}

/** Conta quantos eventos aconteceram nos últimos [janelaMs]. */
class ContadorEmJanela(private val janelaMs: Long) {
    private val instantes = ArrayDeque<Long>()

    fun registrar(timestampMs: Long) {
        instantes.addLast(timestampMs)
    }

    fun contar(agoraMs: Long): Int {
        while (instantes.firstOrNull()?.let { it <= agoraMs - janelaMs } == true) {
            instantes.removeFirst()
        }
        return instantes.size
    }
}

/** Frequência e duração média das piscadas nos últimos [janelaMs]. */
data class ResumoPiscadas(val quantidade: Int, val duracaoMediaMs: Double)

class EstatisticaPiscadas(private val janelaMs: Long) {
    private data class Piscada(val timestampMs: Long, val duracaoMs: Long)

    private val piscadas = ArrayDeque<Piscada>()
    private var somaMs = 0L

    fun registrar(timestampMs: Long, duracaoMs: Long) {
        piscadas.addLast(Piscada(timestampMs, duracaoMs))
        somaMs += duracaoMs
    }

    fun resumo(agoraMs: Long): ResumoPiscadas {
        while (piscadas.firstOrNull()?.let { it.timestampMs <= agoraMs - janelaMs } == true) {
            somaMs -= piscadas.removeFirst().duracaoMs
        }
        val media = if (piscadas.isEmpty()) 0.0 else somaMs.toDouble() / piscadas.size
        return ResumoPiscadas(piscadas.size, media)
    }
}
