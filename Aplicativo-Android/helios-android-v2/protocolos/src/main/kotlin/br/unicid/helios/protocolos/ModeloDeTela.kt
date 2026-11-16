package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.instalacao.Zona
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import kotlin.math.roundToInt

/** Ícone do selo de estado (regra 5 do ADR-012: palavra, ícone e cor). O app liga cada um a um Material Symbol. */
enum class Icone {
    CONFIRMADO, ATENCAO, SONO, ALERTA, PESSOA_AUSENTE, OLHAR_FORA, CAMERA_FRONTAL, PAUSA, PARADO, SEM_CAMERA, AGUARDE,
}

/**
 * Tudo que a tela de direção desenha. Sem gráficos durante a direção.
 * [detalhe]: todos os motivos, para o visor e a foto anotada. [apoio]: só o motivo principal, para a direção.
 * [telaCheia]: o selo ocupa a tela inteira e pulsa (hoje só no CRÍTICO; decisão em aberto no ADR-012).
 */
data class EstadoTela(
    val titulo: String,
    val detalhe: String,
    val apoio: String,
    val tom: Tom,
    val icone: Icone,
    val telaCheia: Boolean,
    val perclosPercentual: Int,
    val piscadasPorMinuto: Int?,
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
    Motivo.OLHAR_ZONA to "olhando um espelho ou o painel",
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
        apoio = apoio(estado, leitura.motivos, leitura.segundosNoEstado, leitura.metricas?.let { it.olhar.zona }),
        tom = TONS.getValue(estado),
        icone = ICONES.getValue(estado),
        telaCheia = estado == EstadoMotorista.CRITICO,
        perclosPercentual = ((leitura.metricas?.perclosLongo ?: 0.0) * 100).roundToInt(),
        piscadasPorMinuto = leitura.metricas?.piscadasNoMinuto,
    )
}

/** Alertas de perigo imediato mostram há quanto tempo duram; a fadiga, acumulada, não. */
private val COM_CRONOMETRO = setOf(EstadoMotorista.CRITICO, EstadoMotorista.DESATENTO, EstadoMotorista.DISTRAIDO)

private fun detalhePadrao(estado: EstadoMotorista): String =
    if (estado == EstadoMotorista.SEM_ROSTO) "ajuste a câmera" else "boa viagem"

/**
 * Linha de apoio da tela de direção (ADR-012, regras 4 e 7): um motivo só, em linguagem simples, com a primeira
 * letra maiúscula. O principal é o primeiro na ordem do enum [Motivo] (sono e olhos antes de cabeça, bocejo e olhar).
 * Nos alertas urgentes vem junto há quanto tempo duram, em segundos inteiros até 59 s e depois em minutos.
 */
private fun apoio(estado: EstadoMotorista, motivos: Set<Motivo>, segundos: Double, zona: Zona?): String {
    val principal = motivos.sorted().firstOrNull()
    val motivo = when {
        principal == null -> detalhePadrao(estado)
        principal == Motivo.OLHAR_ZONA && zona != null -> "olhando " + ALVO_DA_ZONA.getValue(zona)
        else -> TEXTO_MOTIVO.getValue(principal)
    }
    val texto = motivo[0].uppercaseChar() + motivo.drop(1) // nenhum texto de motivo é vazio
    return if (estado in COM_CRONOMETRO && segundos >= 1.0) "$texto há ${duracao(segundos.toInt())}" else texto
}

private const val UM_MINUTO_S = 60

/** Para onde o motorista olha numa olhada longa de zona (ADR-014): "Olhando o espelho esquerdo há 4 s". */
private val ALVO_DA_ZONA = mapOf(
    Zona.RETROVISOR to "o retrovisor interno",
    Zona.ESPELHO_ESQUERDO to "o espelho esquerdo",
    Zona.ESPELHO_DIREITO to "o espelho direito",
    Zona.PAINEL to "o painel",
)

private fun duracao(segundos: Int) = if (segundos < UM_MINUTO_S) "$segundos s" else "${segundos / UM_MINUTO_S} min"
