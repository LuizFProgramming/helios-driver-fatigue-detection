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
    const val SETA = 0xFFF07B1D
    const val FAROL_ALTO = 0xFFFFD34D
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
    EstadoMotorista.DESATENTO to "DESATENTO",
    EstadoMotorista.DISTRAIDO to "DISTRAÍDO",
    EstadoMotorista.SEM_ROSTO to "SEM ROSTO",
    EstadoMotorista.ATENCAO to "ATENÇÃO",
)

private val CORES = mapOf(
    EstadoMotorista.ATENTO to Paleta.SINAL_VERDE,
    EstadoMotorista.FADIGA to Paleta.AMBAR,
    EstadoMotorista.CRITICO to Paleta.LUZ_DE_FREIO,
    EstadoMotorista.DESATENTO to Paleta.SETA,
    EstadoMotorista.DISTRAIDO to Paleta.SETA,
    EstadoMotorista.SEM_ROSTO to Paleta.SEM_SINAL,
    EstadoMotorista.ATENCAO to Paleta.FAROL_ALTO,
)

private val TEXTO_MOTIVO = mapOf(
    Motivo.SONO to "olhos fechados",
    Motivo.OLHOS_FECHANDO to "olhos fechando",
    Motivo.CABECA_CAIDA to "cabeça caindo",
    Motivo.PERCLOS_FADIGA to "olhos pesados",
    Motivo.MICROSSONOS_FREQUENTES to "cochilos curtos",
    Motivo.POS_SONO to "cochilou há pouco: faça uma pausa",
    Motivo.BOCEJOS_FREQUENTES to "bocejos frequentes",
    Motivo.PISCADAS_LONGAS_FREQUENTES to "piscadas longas frequentes",
    Motivo.EVENTO_BRUSCO_COM_FADIGA to "manobra brusca",
    Motivo.ROSTO_AUSENTE to "rosto fora da câmera",
    Motivo.CABECA_VIRADA to "cabeça virada para o lado",
    Motivo.OLHAR_DESVIADO to "olhar fora da via",
    Motivo.DISTRACAO_ACUMULADA to "muitas olhadas para fora da via",
    Motivo.PISCADAS_LENTAS to "piscadas demoradas",
    Motivo.OLHOS_SEMICERRADOS to "olhos semicerrados",
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
        cronometro = if (estado in COM_CRONOMETRO) cronometro(leitura.segundosNoEstado) else null,
    )
}

/** Alertas de perigo imediato mostram há quanto tempo duram; a fadiga, acumulada, não. */
private val COM_CRONOMETRO = setOf(EstadoMotorista.CRITICO, EstadoMotorista.DESATENTO, EstadoMotorista.DISTRAIDO)

private fun detalhePadrao(estado: EstadoMotorista): String =
    if (estado == EstadoMotorista.SEM_ROSTO) "ajuste a câmera" else "boa viagem"

private fun cronometro(segundos: Double): String? =
    if (segundos <= 0.0) null else "%.1f s".format(java.util.Locale.ROOT, segundos)
