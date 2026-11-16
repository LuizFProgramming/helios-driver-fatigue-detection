package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import kotlin.math.roundToInt

/** Ícone do selo de estado (regra 5 do ADR-012: palavra, ícone e cor). O app liga cada um a um Material Symbol. */
enum class Icone {
    CONFIRMADO, ATENCAO, SONO, ALERTA, PESSOA_AUSENTE, OLHAR_FORA, CAMERA_FRONTAL, PAUSA, PARADO, SEM_CAMERA, AGUARDE,
}

/** Tudo que a tela de direção desenha. Sem gráficos durante a direção. */
data class EstadoTela(
    val titulo: String,
    val detalhe: String,
    val tom: Tom,
    val icone: Icone,
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

private val TONS = mapOf(
    EstadoMotorista.ATENTO to Tom.VERDE,
    EstadoMotorista.FADIGA to Tom.AMBAR,
    EstadoMotorista.CRITICO to Tom.VERMELHO,
    EstadoMotorista.DESATENTO to Tom.LARANJA,
    EstadoMotorista.DISTRAIDO to Tom.LARANJA,
    EstadoMotorista.SEM_ROSTO to Tom.NEUTRO,
    EstadoMotorista.ATENCAO to Tom.AMARELO,
)

private val ICONES = mapOf(
    EstadoMotorista.ATENTO to Icone.CONFIRMADO,
    EstadoMotorista.FADIGA to Icone.SONO,
    EstadoMotorista.CRITICO to Icone.ALERTA,
    EstadoMotorista.DESATENTO to Icone.PESSOA_AUSENTE,
    EstadoMotorista.DISTRAIDO to Icone.OLHAR_FORA,
    EstadoMotorista.SEM_ROSTO to Icone.CAMERA_FRONTAL,
    EstadoMotorista.ATENCAO to Icone.ATENCAO,
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
        tom = TONS.getValue(estado),
        icone = ICONES.getValue(estado),
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
