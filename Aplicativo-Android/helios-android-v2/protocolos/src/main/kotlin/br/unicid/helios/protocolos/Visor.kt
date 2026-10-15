package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.MetricasOlhar
import br.unicid.helios.dominio.monitor.OlhoAvaliado
import br.unicid.helios.dominio.tipos.Ponto2D
import java.util.Locale
import kotlin.math.roundToInt

/** Pontos extras da malha, só para desenhar no visor (os olhos e a boca usam [IndicesFaceMesh]). */
object MalhaVisor {
    /** Dorso do nariz, do ponto entre os olhos até a ponta. */
    val NARIZ = listOf(168, 6, 197, 195, 5, 4, 1)

    /** Base do nariz, de uma narina à outra. */
    val NARINAS = listOf(98, 97, 2, 326, 327)
    const val PONTA_DO_NARIZ = 1

    /** Contorno de cada íris (4 pontos em volta do centro). */
    val IRIS_DIREITA = listOf(469, 470, 471, 472)
    val IRIS_ESQUERDA = listOf(474, 475, 476, 477)

    fun olhoDireito() = IndicesFaceMesh.OLHO_DIREITO_IMAGEM
    fun olhoEsquerdo() = IndicesFaceMesh.OLHO_ESQUERDO_IMAGEM
    fun boca() = IndicesFaceMesh.BOCA_LABIO_INTERNO
}

/** Direção dos três eixos da cabeça projetada na imagem (x para a direita, y para baixo). */
data class Eixos2D(val x: Ponto2D, val y: Ponto2D, val z: Ponto2D)

private const val TAMANHO_COLUNA = 4

/**
 * As três primeiras colunas da matriz de transformação do MediaPipe (coluna maior) são os eixos
 * do rosto. Na imagem o y cresce para baixo, por isso o sinal do y é trocado.
 */
fun eixosDaCabeca(matriz: DoubleArray): Eixos2D {
    fun coluna(c: Int) = Ponto2D(matriz[c * TAMANHO_COLUNA], -matriz[c * TAMANHO_COLUNA + 1])
    return Eixos2D(coluna(0), coluna(1), coluna(2))
}

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private const val CEM = 100

/**
 * Texto do painel de métricas do visor e da foto anotada: o estado e todos os números que o
 * domínio usou para decidir, cada olho separado.
 */
fun linhasDeMetricas(leitura: Leitura): List<String> {
    val tela = modeloDeTela(leitura)
    val cabecalho = "${tela.titulo}: ${tela.detalhe}"
    val m = leitura.metricas ?: return listOf(cabecalho, "Rosto não encontrado")
    val avaliado = when (m.olhoAvaliado) {
        OlhoAvaliado.AMBOS -> "os dois olhos"
        OlhoAvaliado.DIREITO -> "só o olho direito (cabeça de lado)"
        OlhoAvaliado.ESQUERDO -> "só o olho esquerdo (cabeça de lado)"
    }
    val fechados = if (m.olhosFechadosMs > 0) " · fechados há ${num(m.olhosFechadosMs / 1_000.0, 1)} s" else ""
    val pose = m.pose
    val cabeca = if (pose == null) {
        "Cabeça: sem pose"
    } else {
        "Cabeça: pitch ${graus(pose.pitchGraus)} · yaw ${graus(pose.yawGraus)} · roll ${graus(pose.rollGraus)}"
    }
    return listOf(
        cabecalho,
        olho("direito", m.earDireito, m.ecfDireito, m.olhoDireitoFechado),
        olho("esquerdo", m.earEsquerdo, m.ecfEsquerdo, m.olhoEsquerdoFechado),
        "Avaliado: $avaliado$fechados",
        "Boca: MAR ${num(m.marExibicao, 2)} · mandíbula ${num(m.aberturaMandibula, 2)}",
        cabeca,
        "PERCLOS: ${pct(m.perclosCurto)} em 5 s · ${pct(m.perclosLongo)} em 60 s",
        olhar(m.olhar),
        "Fora da via: ${num(m.olhar.foraDaViaMs / MIL, 1)} s nos últimos 30 s",
        "Piscadas: ${m.piscadasNoMinuto} no último minuto · média ${m.duracaoMediaPiscadaMs.roundToInt()} ms",
        abertura(m),
    )
}

private const val MIL = 1_000.0

private fun olhar(o: MetricasOlhar): String {
    if (o.horizontal == null && o.vertical == null) return "Olhar: não medido"
    val partes = listOfNotNull(
        o.horizontal?.let { "íris ${num(it, 2)} (${sinal(o.desvioHorizontal)})" },
        o.vertical?.let { "vertical ${num(it, 2)} (${sinal(o.desvioVertical)})" },
    )
    val onde = if (o.fora) "fora há ${num(o.foraMs / MIL, 1)} s" else "na via"
    return "Olhar: ${partes.joinToString(" · ")} · $onde"
}

private fun abertura(m: Metricas): String {
    val semicerrados = if (m.olhosSemicerrados) "semicerrados" else "normais"
    return "Abertura: ${pct(m.aberturaRelativa)} da calibrada · apertar ${num(m.semicerrar, 2)} · " +
        "$semicerrados (${pct(m.fracaoSemicerrado)} do minuto)"
}

private fun sinal(desvio: Double?): String = "%+.2f".format(PT_BR, desvio ?: 0.0)

private fun olho(lado: String, ear: Double, ecf: Double, fechado: Boolean) =
    "Olho $lado: EAR ${num(ear, 2)} · piscada ${num(ecf, 2)} · ${if (fechado) "fechado" else "aberto"}"

private fun num(valor: Double, casas: Int) = "%.${casas}f".format(PT_BR, valor)

private fun graus(valor: Double) = "${valor.roundToInt()}°"

private fun pct(fracao: Double) = "${(fracao * CEM).roundToInt()}%"
