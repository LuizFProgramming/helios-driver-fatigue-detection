package br.unicid.helios.dominio.eventos

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.tipos.TipoEvento

/** Situação de uma condição que precisa durar um tempo mínimo (olho fechado, boca aberta...). */
sealed interface Fase {
    data object Inativa : Fase
    data class EmCurso(val inicioMs: Long, val duracaoMs: Long) : Fase
    data class Encerrada(val inicioMs: Long, val duracaoMs: Long) : Fase
}

/**
 * Acompanha quando uma condição começou e quanto durou, usando o timestamp do frame.
 *
 * Com [toleranciaMs] acima de zero, uma interrupção de até esse tempo não encerra a condição: um quadro com o olho
 * "aberto" no meio de um fechamento longo não o divide em vários eventos curtos. Durante a interrupção a duração
 * fica parada; se a condição não volta, o evento termina no instante em que ela parou.
 */
class DetectorSustentado(private val toleranciaMs: Long = 0) {
    private var inicioMs: Long? = null
    private var pausaMs: Long? = null

    fun atualizar(timestampMs: Long, ativa: Boolean): Fase {
        val inicio = inicioMs
        return when {
            inicio == null -> if (ativa) iniciar(timestampMs) else Fase.Inativa
            ativa -> retomar(inicio, timestampMs)
            else -> interromper(inicio, timestampMs)
        }
    }

    /** Esquece uma condição em curso sem gerar evento (o rosto sumiu e não dá para saber como ela terminou). */
    fun reiniciar() {
        inicioMs = null
        pausaMs = null
    }

    private fun iniciar(timestampMs: Long): Fase {
        inicioMs = timestampMs
        return Fase.EmCurso(timestampMs, 0)
    }

    private fun retomar(inicio: Long, timestampMs: Long): Fase {
        pausaMs = null
        return Fase.EmCurso(inicio, timestampMs - inicio)
    }

    private fun interromper(inicio: Long, timestampMs: Long): Fase {
        val pausa = pausaMs ?: timestampMs.also { pausaMs = it }
        if (timestampMs - pausa < toleranciaMs) return Fase.EmCurso(inicio, pausa - inicio)
        reiniciar()
        return Fase.Encerrada(inicio, pausa - inicio)
    }
}

fun Fase.duracaoEmCurso(): Long = (this as? Fase.EmCurso)?.duracaoMs ?: 0

/**
 * Classifica um fechamento de olhos já encerrado pela duração.
 * Abaixo de [ConfiguracaoDeteccao.piscadaMinMs] é ruído e devolve nulo.
 */
fun classificarFechamento(duracaoMs: Long, cfg: ConfiguracaoDeteccao): TipoEvento? = when {
    duracaoMs >= cfg.sonoMinMs -> TipoEvento.SONO
    duracaoMs >= cfg.microssonoMinMs -> TipoEvento.MICROSSONO
    duracaoMs >= cfg.piscadaLongaMinMs -> TipoEvento.PISCADA_LONGA
    duracaoMs >= cfg.piscadaMinMs -> TipoEvento.PISCADA
    else -> null
}
