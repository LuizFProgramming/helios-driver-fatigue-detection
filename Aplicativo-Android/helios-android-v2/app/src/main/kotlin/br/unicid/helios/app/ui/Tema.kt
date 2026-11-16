package br.unicid.helios.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import br.unicid.helios.app.R
import br.unicid.helios.protocolos.EsquemaCores
import br.unicid.helios.protocolos.SINAIS
import br.unicid.helios.protocolos.TEMA_CLARO
import br.unicid.helios.protocolos.TEMA_ESCURO
import br.unicid.helios.protocolos.Tom

fun cor(argb: Long) = Color(argb)

/** Fundo e texto do selo de um estado. Os mesmos nos dois temas (ADR-012, regra 6). */
fun fundoDoSinal(tom: Tom) = cor(SINAIS.getValue(tom).fundo)
fun textoDoSinal(tom: Tom) = cor(SINAIS.getValue(tom).texto)

/**
 * Atkinson Hyperlegible Next (Braille Institute, licença OFL; docs/licencas): desenhada para que
 * letras parecidas (I, l, 1; O, 0; rn, m) não se confundam numa olhada rápida. Arquivo variável único,
 * dentro do APK, sem download.
 */
private fun peso(w: Int) = Font(
    R.font.atkinson_hyperlegible_next,
    FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

val Atkinson = FontFamily(peso(400), peso(500), peso(600), peso(700), peso(800))

/** Espaçamentos em grade de 8 dp (ADR-012, regra 7). */
object Espaco {
    val p = 8.dp
    val m = 16.dp
    val g = 24.dp
    val gg = 32.dp
}

/** Alvos de toque mínimos (ADR-012, regra 3): Android for Cars com o carro ligado; Material 3 com ele parado. */
object Alvo {
    val direcao = 76.dp
    val parado = 48.dp
}

/**
 * Tamanhos da tela de direção (ADR-012, regra 4), em sp para respeitarem a escala de texto do sistema
 * e a de Ajustes. Algarismos tabulares nos números, para não "tremerem" quando mudam.
 */
object Tipos {
    /** Palavra do estado: a peça gráfica principal, com peso máximo e entrelinha apertada. */
    val estado = TextStyle(
        fontFamily = Atkinson,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
    )
    val principal = TextStyle(
        fontFamily = Atkinson, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp,
    )
    val secundario = TextStyle(
        fontFamily = Atkinson, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp,
    )
    val numero = TextStyle(
        fontFamily = Atkinson,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        fontFeatureSettings = "tnum",
    )
    val detalhe = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Medium, fontSize = 28.sp)
    val rodape = TextStyle(fontFamily = Atkinson, fontSize = 16.sp, fontFeatureSettings = "tnum")
}

/** Escala do Material 3 na Atkinson, com o corpo e os rótulos nunca abaixo de 14 sp (ADR-012, regra 4). */
private val tipografia = Typography().run {
    val a = { t: TextStyle -> t.copy(fontFamily = Atkinson) }
    val minimo = { t: TextStyle -> a(t).copy(fontSize = 14.sp, lineHeight = 20.sp) }
    Typography(
        displayLarge = a(displayLarge), displayMedium = a(displayMedium), displaySmall = a(displaySmall),
        headlineLarge = a(headlineLarge), headlineMedium = a(headlineMedium), headlineSmall = a(headlineSmall),
        titleLarge = a(titleLarge), titleMedium = a(titleMedium), titleSmall = a(titleSmall),
        bodyLarge = a(bodyLarge), bodyMedium = a(bodyMedium), bodySmall = minimo(bodySmall),
        labelLarge = a(labelLarge), labelMedium = minimo(labelMedium), labelSmall = minimo(labelSmall),
    )
}

/** Cantos de 16 a 28 dp: maiores nos painéis, menores nos controles. */
private val formas = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun esquema(e: EsquemaCores, escuro: Boolean): ColorScheme =
    (if (escuro) darkColorScheme() else lightColorScheme()).copy(
        primary = cor(e.primary), onPrimary = cor(e.onPrimary),
        primaryContainer = cor(e.primaryContainer), onPrimaryContainer = cor(e.onPrimaryContainer),
        inversePrimary = cor(e.inversePrimary),
        secondary = cor(e.secondary), onSecondary = cor(e.onSecondary),
        secondaryContainer = cor(e.secondaryContainer), onSecondaryContainer = cor(e.onSecondaryContainer),
        // tertiary: o app não usa; repete o secondary para nenhum componente cair no roxo padrão do Material.
        tertiary = cor(e.secondary), onTertiary = cor(e.onSecondary),
        tertiaryContainer = cor(e.secondaryContainer), onTertiaryContainer = cor(e.onSecondaryContainer),
        background = cor(e.background), onBackground = cor(e.onBackground),
        surface = cor(e.surface), onSurface = cor(e.onSurface),
        surfaceVariant = cor(e.surfaceVariant), onSurfaceVariant = cor(e.onSurfaceVariant),
        // surfaceTint igual ao fundo: sem a camada colorida que o Material soma às superfícies elevadas.
        surfaceTint = cor(e.surface),
        inverseSurface = cor(e.inverseSurface), inverseOnSurface = cor(e.inverseOnSurface),
        error = cor(e.error), onError = cor(e.onError),
        errorContainer = cor(e.errorContainer), onErrorContainer = cor(e.onErrorContainer),
        outline = cor(e.outline), outlineVariant = cor(e.outlineVariant),
        surfaceBright = cor(if (escuro) e.surfaceContainerHighest else e.surfaceContainerLowest),
        surfaceDim = cor(if (escuro) e.surfaceContainerLowest else e.surfaceContainerHighest),
        surfaceContainerLowest = cor(e.surfaceContainerLowest), surfaceContainerLow = cor(e.surfaceContainerLow),
        surfaceContainer = cor(e.surfaceContainer), surfaceContainerHigh = cor(e.surfaceContainerHigh),
        surfaceContainerHighest = cor(e.surfaceContainerHighest),
    )

/**
 * Tema do HELIOS: escuro por padrão, claro opcional, nunca a cor dinâmica do sistema (ADR-012, regra 6).
 * O Surface define a cor do texto padrão; sem ele o Material 3 escreve em preto.
 */
@Composable
fun TemaHelios(escuro: Boolean = true, conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = esquema(if (escuro) TEMA_ESCURO else TEMA_CLARO, escuro),
        typography = tipografia,
        shapes = formas,
    ) {
        Surface(color = MaterialTheme.colorScheme.background, content = conteudo)
    }
}
