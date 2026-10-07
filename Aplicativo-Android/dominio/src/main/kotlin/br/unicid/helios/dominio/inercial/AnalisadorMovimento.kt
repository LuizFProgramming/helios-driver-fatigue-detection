package br.unicid.helios.dominio.inercial

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import kotlin.math.sqrt

/**
 * Sensores inerciais do aparelho (acelerômetro linear, já sem a gravidade).
 *
 * - [emMovimento]: RMS da aceleração na janela acima do limiar. Usado para liberar o
 *   histórico só com o carro parado (RF09).
 * - [eventoBruscoRecente]: pico acima do limiar (freada ou desvio forte). Somado a sinais
 *   de fadiga, eleva o estado para CRÍTICO.
 */
class AnalisadorMovimento(private val cfg: ConfiguracaoDeteccao) {
    private data class Amostra(val timestampMs: Long, val quadrado: Double)

    private val janela = ArrayDeque<Amostra>()
    private var somaQuadrados = 0.0
    private var ultimoEventoBruscoMs: Long? = null

    /** Devolve true se esta amostra for um evento brusco novo. */
    fun registrar(timestampMs: Long, magnitude: Double): Boolean {
        val quadrado = magnitude * magnitude
        janela.addLast(Amostra(timestampMs, quadrado))
        somaQuadrados += quadrado
        descartarAntesDe(timestampMs - cfg.janelaMovimentoMs)
        return marcarSeBrusco(timestampMs, magnitude)
    }

    fun emMovimento(): Boolean = rms() > cfg.limiarMovimentoRms

    fun eventoBruscoRecente(agoraMs: Long): Boolean =
        ultimoEventoBruscoMs?.let { agoraMs - it < cfg.janelaEventoBruscoMs } ?: false

    private fun rms(): Double = if (janela.isEmpty()) 0.0 else sqrt(somaQuadrados.coerceAtLeast(0.0) / janela.size)

    private fun marcarSeBrusco(timestampMs: Long, magnitude: Double): Boolean {
        if (magnitude < cfg.limiarEventoBrusco) return false
        val novo = !eventoBruscoRecente(timestampMs)
        ultimoEventoBruscoMs = timestampMs
        return novo
    }

    private fun descartarAntesDe(limiteMs: Long) {
        while (janela.firstOrNull()?.let { it.timestampMs < limiteMs } == true) {
            somaQuadrados -= janela.removeFirst().quadrado
        }
    }
}
