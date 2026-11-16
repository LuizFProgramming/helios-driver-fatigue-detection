package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.calibracao.Desvios
import kotlin.math.roundToInt

enum class Qualidade { BOA, ACEITAVEL, REFAZER }

/** Pontos de cada parte da nota, para o resumo mostrar o que falhou (máximos: 60, 25 e 15). */
data class PartesNota(val verificacoes: Int, val estabilidade: Int, val zonas: Int)

data class NotaInstalacao(val pontos: Int, val qualidade: Qualidade, val partes: PartesNota)

private const val PESO_VERIFICACOES = 60.0
private const val PESO_ESTABILIDADE = 25.0
private const val PESO_ZONAS = 15.0

/**
 * Nota de 0 a 100 da instalação (ADR-013): as verificações valem 60 (fração dos quadros da calibração com as
 * cinco certas), a estabilidade 25 (maior desvio de pitch e yaw, de 3 a 8 graus) e as zonas 15 (cada uma, um quarto).
 */
fun notaInstalacao(
    fracaoVerificacoesOk: Double,
    desvios: Desvios,
    zonasGravadas: Int,
    cfg: ConfiguracaoInstalacao,
): NotaInstalacao {
    val desvio = maxOf(desvios.pitch, desvios.yaw)
    val estabilidade = ((cfg.desvioRuimGraus - desvio) / (cfg.desvioRuimGraus - cfg.desvioBomGraus)).coerceIn(0.0, 1.0)
    val verificacoes = PESO_VERIFICACOES * fracaoVerificacoesOk
    val estavel = PESO_ESTABILIDADE * estabilidade
    val zonas = PESO_ZONAS * zonasGravadas / Zona.entries.size
    val pontos = (verificacoes + estavel + zonas).roundToInt()
    return NotaInstalacao(
        pontos,
        qualidade(pontos, cfg),
        PartesNota(verificacoes.roundToInt(), estavel.roundToInt(), zonas.roundToInt()),
    )
}

fun qualidade(pontos: Int, cfg: ConfiguracaoInstalacao): Qualidade = when {
    pontos >= cfg.notaBoa -> Qualidade.BOA
    pontos >= cfg.notaMinima -> Qualidade.ACEITAVEL
    else -> Qualidade.REFAZER
}
