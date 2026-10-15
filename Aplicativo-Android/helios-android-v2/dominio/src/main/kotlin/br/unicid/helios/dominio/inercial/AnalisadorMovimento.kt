package br.unicid.helios.dominio.inercial

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import kotlin.math.sqrt

/**
 * Sensores inerciais do aparelho (acelerômetro linear, já sem a gravidade).
 *
 * - [emMovimento]: RMS da aceleração na janela acima do limiar, com histerese: só volta a "parado"
 *   depois de [ConfiguracaoDeteccao.paradoMinMs] seguidos abaixo de [ConfiguracaoDeteccao.limiarParadoRms].
 *   Usado para liberar histórico e ajustes só com o carro parado (RF09).
 * - [eventoBruscoRecente]: pico acima do limiar (freada ou desvio forte). Somado a sinais
 *   de fadiga, eleva o estado para CRÍTICO.
 */
class AnalisadorMovimento(private val cfg: ConfiguracaoDeteccao) {
    private data class Amostra(val timestampMs: Long, val quadrado: Double)

    private val janela = ArrayDeque<Amostra>()
    private var somaQuadrados = 0.0
    private var ultimoEventoBruscoMs: Long? = null
    private var movendo = false
    private var inicioCalmoMs: Long? = null

    /** Devolve true se esta amostra for um evento brusco novo. */
    fun registrar(timestampMs: Long, magnitude: Double): Boolean {
        val quadrado = magnitude * magnitude
        janela.addLast(Amostra(timestampMs, quadrado))
        somaQuadrados += quadrado
        descartarAntesDe(timestampMs - cfg.janelaMovimentoMs)
        atualizarMovimento(timestampMs, rms())
        return marcarSeBrusco(timestampMs, magnitude)
    }

    fun emMovimento(): Boolean = movendo

    private fun atualizarMovimento(t: Long, rms: Double) {
        inicioCalmoMs = if (rms < cfg.limiarParadoRms) inicioCalmoMs ?: t else null
        movendo = when {
            rms > cfg.limiarMovimentoRms -> true
            movendo -> inicioCalmoMs?.let { t - it < cfg.paradoMinMs } ?: true
            else -> false
        }
    }

    fun eventoBruscoRecente(agoraMs: Long): Boolean =
        ultimoEventoBruscoMs?.let { agoraMs - it < cfg.janelaEventoBruscoMs } ?: false

    /** Só é chamado logo depois de uma amostra entrar, então a janela nunca está vazia aqui. */
    private fun rms(): Double = sqrt(somaQuadrados.coerceAtLeast(0.0) / janela.size)

    private fun marcarSeBrusco(timestampMs: Long, magnitude: Double): Boolean {
        if (magnitude < cfg.limiarEventoBrusco) return false
        val novo = !eventoBruscoRecente(timestampMs)
        ultimoEventoBruscoMs = timestampMs
        return novo
    }

    private fun descartarAntesDe(limiteMs: Long) {
        // A amostra recém-adicionada nunca sai (limite < timestamp dela), então a janela não esvazia aqui.
        while (janela.first().timestampMs < limiteMs) {
            somaQuadrados -= janela.removeFirst().quadrado
        }
    }
}
