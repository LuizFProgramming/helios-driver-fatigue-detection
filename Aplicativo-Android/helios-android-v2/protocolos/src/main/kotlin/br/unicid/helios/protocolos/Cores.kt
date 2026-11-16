package br.unicid.helios.protocolos

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Tokens de cor do HELIOS em ARGB (ADR-012, regras 5 e 6). O app só usa estas cores, por papel do
 * Material 3, sem cor dinâmica do sistema; o CoresTest confere o contraste WCAG de todos os pares.
 */
data class EsquemaCores(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val surfaceContainerLowest: Long,
    val surfaceContainerLow: Long,
    val surfaceContainer: Long,
    val surfaceContainerHigh: Long,
    val surfaceContainerHighest: Long,
    val outline: Long,
    val outlineVariant: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val inverseSurface: Long,
    val inverseOnSurface: Long,
    val inversePrimary: Long,
)

/**
 * Noite na estrada, o padrão: asfalto no fundo (escuro sem ser preto, para não ofuscar) e farol no texto.
 * O primary é o azul das placas de serviço, que no trânsito quer dizer "informação": o âmbar e o
 * vermelho ficam só para os estados, para que nenhum botão pareça um alerta.
 */
val TEMA_ESCURO = EsquemaCores(
    primary = 0xFF8CB8F2, onPrimary = 0xFF0E2A4A,
    primaryContainer = 0xFF1F3F66, onPrimaryContainer = 0xFFD4E4FF,
    secondary = 0xFFB8C4D0, onSecondary = 0xFF1B2026,
    secondaryContainer = 0xFF3A4652, onSecondaryContainer = 0xFFDCE5EE,
    background = 0xFF1B2026, onBackground = 0xFFEDE9E1,
    surface = 0xFF1B2026, onSurface = 0xFFEDE9E1,
    surfaceVariant = 0xFF363F49, onSurfaceVariant = 0xFFA9B3BD,
    surfaceContainerLowest = 0xFF15191E, surfaceContainerLow = 0xFF1F252C, surfaceContainer = 0xFF252C34,
    surfaceContainerHigh = 0xFF2D353E, surfaceContainerHighest = 0xFF363F49,
    outline = 0xFF828D99, outlineVariant = 0xFF3E4751,
    error = 0xFFFF8A7A, onError = 0xFF5C0F0A,
    errorContainer = 0xFF8C1D18, onErrorContainer = 0xFFFFDAD6,
    inverseSurface = 0xFFEDE9E1, inverseOnSurface = 0xFF1B2026, inversePrimary = 0xFF1F5FAF,
)

/** Dia, opcional em Ajustes: concreto claro e frio no fundo, asfalto no texto. */
val TEMA_CLARO = EsquemaCores(
    primary = 0xFF1F5FAF, onPrimary = 0xFFFFFFFF,
    primaryContainer = 0xFFD4E4FF, onPrimaryContainer = 0xFF0B2545,
    secondary = 0xFF4A5866, onSecondary = 0xFFFFFFFF,
    secondaryContainer = 0xFFD3DEE9, onSecondaryContainer = 0xFF0F1D2A,
    background = 0xFFF3F5F7, onBackground = 0xFF1B2026,
    surface = 0xFFF3F5F7, onSurface = 0xFF1B2026,
    surfaceVariant = 0xFFD8DEE4, onSurfaceVariant = 0xFF45505B,
    surfaceContainerLowest = 0xFFFFFFFF, surfaceContainerLow = 0xFFEDF0F3, surfaceContainer = 0xFFE6EAEE,
    surfaceContainerHigh = 0xFFDFE4E9, surfaceContainerHighest = 0xFFD8DEE4,
    outline = 0xFF6B7682, outlineVariant = 0xFFC3CBD3,
    error = 0xFFB3261E, onError = 0xFFFFFFFF,
    errorContainer = 0xFFFFDAD6, onErrorContainer = 0xFF410E0B,
    inverseSurface = 0xFF2C333A, inverseOnSurface = 0xFFEEF1F4, inversePrimary = 0xFF8CB8F2,
)

/** Tons da sinalização viária usados nos estados. Iguais nos dois temas, para o motorista aprender o significado. */
enum class Tom { VERDE, AMARELO, AMBAR, LARANJA, VERMELHO, NEUTRO }

/** Selo de estado: fundo na cor do tom; palavra e ícone na cor [texto]. */
data class CorDeSinal(val fundo: Long, val texto: Long)

private const val ASFALTO = 0xFF1B2026

val SINAIS = mapOf(
    Tom.VERDE to CorDeSinal(0xFF3FB27F, ASFALTO),
    Tom.AMARELO to CorDeSinal(0xFFFFD34D, ASFALTO),
    Tom.AMBAR to CorDeSinal(0xFFF2A900, ASFALTO),
    Tom.LARANJA to CorDeSinal(0xFFF07B1D, ASFALTO),
    // Vermelho escuro com texto branco: o pulso escurece o fundo e o contraste só aumenta.
    Tom.VERMELHO to CorDeSinal(0xFFB3261E, 0xFFFFFFFF),
    Tom.NEUTRO to CorDeSinal(0xFF8E99A4, ASFALTO),
)

/** Quanto o pulso do alerta escurece o selo no ponto mais fundo (regra 8: a única animação durante um alerta). */
const val ESCURECIMENTO_MAXIMO_PULSO = 0.45

private const val PRETO = 0xFF000000

fun corDoPulso(fundo: Long, fracao: Double): Long = misturar(fundo, PRETO, fracao)

/** Interpolação linear canal a canal, opaca: [fracao] 0 devolve [de], 1 devolve [para]. */
fun misturar(de: Long, para: Long, fracao: Double): Long =
    DESLOCAMENTOS_RGB.fold(PRETO) { cor, desloca ->
        val a = (de shr desloca) and 0xFF
        val b = (para shr desloca) and 0xFF
        cor or ((a + (b - a) * fracao).roundToLong() shl desloca)
    }

/** Razão de contraste da WCAG 2.1 (critérios 1.4.3 e 1.4.11), de 1 a 21. */
fun contrasteWcag(cor1: Long, cor2: Long): Double {
    val l1 = luminancia(cor1)
    val l2 = luminancia(cor2)
    return (maxOf(l1, l2) + REFLEXO) / (minOf(l1, l2) + REFLEXO)
}

// Constantes da definição de luminância relativa da WCAG 2.1 (sRGB).
private val DESLOCAMENTOS_RGB = intArrayOf(16, 8, 0)
private const val PESO_R = 0.2126
private const val PESO_G = 0.7152
private const val PESO_B = 0.0722
private const val REFLEXO = 0.05
private const val MAX_CANAL = 255.0
// O limiar 0,04045 da WCAG cai entre os canais 10 (0,0392) e 11 (0,0431) de 255: comparar no inteiro é o mesmo.
private const val ULTIMO_CANAL_LINEAR = 10L
private const val DIVISOR_LINEAR = 12.92
private const val DESLOCAMENTO_GAMA = 0.055
private const val GAMA = 2.4

private fun luminancia(argb: Long): Double {
    val (r, g, b) = DESLOCAMENTOS_RGB.map { canal(argb shr it) }
    return PESO_R * r + PESO_G * g + PESO_B * b
}

private fun canal(valor: Long): Double {
    val inteiro = valor and 0xFF
    val c = inteiro / MAX_CANAL
    if (inteiro <= ULTIMO_CANAL_LINEAR) return c / DIVISOR_LINEAR
    return ((c + DESLOCAMENTO_GAMA) / (1 + DESLOCAMENTO_GAMA)).pow(GAMA)
}
