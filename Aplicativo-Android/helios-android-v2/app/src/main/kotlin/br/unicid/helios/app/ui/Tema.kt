package br.unicid.helios.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import br.unicid.helios.protocolos.Paleta

fun cor(argb: Long) = Color(argb)

val Asfalto = cor(Paleta.ASFALTO)
val Grafite = cor(Paleta.GRAFITE)
val Farol = cor(Paleta.FAROL)
val Nevoa = cor(Paleta.NEVOA)

/**
 * Tipografia da tela de direção: uma família só (a sans do sistema, que existe em qualquer
 * central Android e não precisa de download). O estado é a peça gráfica principal:
 * peso Black, entrelinha apertada e espaçamento negativo, como as letras pintadas no asfalto.
 * Números com algarismos tabulares para não "tremerem" quando mudam.
 */
object Tipos {
    val estado = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.04).em,
        lineHeight = 0.9.em,
    )
    val detalhe = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 28.sp)
    val rodape = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        fontFeatureSettings = "tnum",
    )
}

@Composable
fun TemaHelios(conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = cor(Paleta.AMBAR),
            onPrimary = Asfalto,
            background = Asfalto,
            onBackground = Farol,
            surface = Grafite,
            onSurface = Farol,
            onSurfaceVariant = Nevoa,
            error = cor(Paleta.LUZ_DE_FREIO),
        ),
        content = conteudo,
    )
}
