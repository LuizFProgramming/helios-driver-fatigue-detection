package br.unicid.helios.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss")
private const val UM_MINUTO_MS = 60_000L
private const val UMA_HORA_MIN = 60L
private const val UM_SEGUNDO_MS = 1_000L
private const val CEM = 100

internal fun dataTexto(epochMs: Long): String = FORMATO_DATA.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

internal fun horaTexto(epochMs: Long): String = FORMATO_HORA.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

internal fun duracaoTexto(ms: Long): String {
    val min = ms / UM_MINUTO_MS
    return when {
        min >= UMA_HORA_MIN -> "${min / UMA_HORA_MIN} h ${min % UMA_HORA_MIN} min"
        min > 0 -> "$min min"
        else -> "${ms / UM_SEGUNDO_MS} s"
    }
}

internal fun pctTexto(fracao: Double): String = "${(fracao * CEM).roundToInt()}%"
