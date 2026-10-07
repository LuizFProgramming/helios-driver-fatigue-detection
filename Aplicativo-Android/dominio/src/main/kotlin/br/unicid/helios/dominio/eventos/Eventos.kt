package br.unicid.helios.dominio.eventos

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.tipos.TipoEvento

/** Situação de uma condição que precisa durar um tempo mínimo (olho fechado, boca aberta...). */
sealed interface Fase {
    data object Inativa : Fase
    data class EmCurso(val inicioMs: Long, val duracaoMs: Long) : Fase
    data class Encerrada(val inicioMs: Long, val duracaoMs: Long) : Fase
}

/** Acompanha quando uma condição começou e quanto durou, usando o timestamp do frame. */
class DetectorSustentado {
    private var inicioMs: Long? = null

    fun atualizar(timestampMs: Long, ativa: Boolean): Fase {
        val inicio = inicioMs
        return when {
            ativa && inicio == null -> iniciar(timestampMs)
            ativa && inicio != null -> Fase.EmCurso(inicio, timestampMs - inicio)
            inicio != null -> encerrar(inicio, timestampMs)
            else -> Fase.Inativa
        }
    }

    private fun iniciar(timestampMs: Long): Fase {
        inicioMs = timestampMs
        return Fase.EmCurso(timestampMs, 0)
    }

    private fun encerrar(inicio: Long, timestampMs: Long): Fase {
        inicioMs = null
        return Fase.Encerrada(inicio, timestampMs - inicio)
    }
}

fun Fase.duracaoEmCurso(): Long = (this as? Fase.EmCurso)?.duracaoMs ?: 0

/**
 * Classifica um fechamento de olhos já encerrado pela duração.
 * Abaixo de [ConfiguracaoDeteccao.piscadaMinMs] é ruído e devolve nulo.
 */
fun classificarFechamento(duracaoMs: Long, cfg: ConfiguracaoDeteccao): TipoEvento? = when {
    duracaoMs >= cfg.microssonoMinMs -> TipoEvento.MICROSSONO
    duracaoMs >= cfg.piscadaLongaMinMs -> TipoEvento.PISCADA_LONGA
    duracaoMs >= cfg.piscadaMinMs -> TipoEvento.PISCADA
    else -> null
}
