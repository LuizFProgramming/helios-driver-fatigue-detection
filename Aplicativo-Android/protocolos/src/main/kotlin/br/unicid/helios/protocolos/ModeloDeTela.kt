package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import kotlin.math.roundToInt

/**
 * Paleta da tela de direção, em ARGB. Tirada do próprio carro: asfalto e grafite no fundo
 * (escuro sem ser preto, para não ofuscar à noite), farol para o texto e as três cores
 * de sinalização viária para os estados.
 */
object Paleta {
    const val ASFALTO = 0xFF1B2026
    const val GRAFITE = 0xFF262D35
    const val FAROL = 0xFFEDE9E1
    const val NEVOA = 0xFF8E99A4
    const val SINAL_VERDE = 0xFF3FB27F
    const val AMBAR = 0xFFF2A900
    const val LUZ_DE_FREIO = 0xFFE5322D
    const val SEM_SINAL = 0xFF5F6B76
}

/** Tudo que a tela de direção desenha. Sem gráficos durante a direção. */
data class EstadoTela(
    val titulo: String,
    val detalhe: String,
    val corDestaque: Long,
    val telaCheia: Boolean,
    val perclosPercentual: Int,
    val olhoFechado: Boolean,
    val cronometro: String?,
)

private val TITULOS = mapOf(
    EstadoMotorista.ATENTO to "ATENTO",
    EstadoMotorista.FADIGA to "FADIGA",
    EstadoMotorista.CRITICO to "ACORDE",
    EstadoMotorista.SEM_ROSTO to "SEM ROSTO",
)

private val CORES = mapOf(
    EstadoMotorista.ATENTO to Paleta.SINAL_VERDE,
    EstadoMotorista.FADIGA to Paleta.AMBAR,
    EstadoMotorista.CRITICO to Paleta.LUZ_DE_FREIO,
    EstadoMotorista.SEM_ROSTO to Paleta.SEM_SINAL,
)

private val TEXTO_MOTIVO = mapOf(
    Motivo.MICROSSONO to "olhos fechados",
    Motivo.CABECA_CAIDA to "cabeça caindo",
    Motivo.PERCLOS_CRITICO to "olhos fechando muito",
    Motivo.PERCLOS_FADIGA to "olhos pesados",
    Motivo.BOCEJOS_FREQUENTES to "bocejos frequentes",
    Motivo.PISCADAS_LONGAS_FREQUENTES to "piscadas lentas",
    Motivo.EVENTO_BRUSCO_COM_FADIGA to "manobra brusca",
)

fun modeloDeTela(leitura: Leitura): EstadoTela {
    val estado = leitura.estado
    val detalhe = leitura.motivos.mapNotNull(TEXTO_MOTIVO::get).joinToString(", ")
        .ifEmpty { detalhePadrao(estado) }
    return EstadoTela(
        titulo = TITULOS.getValue(estado),
        detalhe = detalhe,
        corDestaque = CORES.getValue(estado),
        telaCheia = estado == EstadoMotorista.CRITICO,
        perclosPercentual = ((leitura.metricas?.perclosLongo ?: 0.0) * 100).roundToInt(),
        olhoFechado = leitura.metricas?.olhoFechado ?: false,
        cronometro = cronometro(leitura.segundosEmCritico),
    )
}

private fun detalhePadrao(estado: EstadoMotorista): String =
    if (estado == EstadoMotorista.SEM_ROSTO) "ajuste a câmera" else "boa viagem"

private fun cronometro(segundos: Double): String? =
    if (segundos <= 0.0) null else "%.1f s".format(java.util.Locale.ROOT, segundos)
