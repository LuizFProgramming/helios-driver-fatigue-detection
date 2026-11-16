package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Evidência da regra 5 do ADR-012 (WCAG 2.1, critérios 1.4.3 e 1.4.11): todo texto passa 4,5:1
 * e toda borda ou ícone passa 3:1, nos dois temas e em todos os estados da tela de direção.
 */
class CoresTest {

    private val branco = 0xFFFFFFFF
    private val preto = 0xFF000000

    @Test
    fun `preto e branco dao 21 para 1 e a mesma cor da 1 para 1`() {
        assertEquals(21.0, contrasteWcag(preto, branco), 1e-9)
        assertEquals(21.0, contrasteWcag(branco, preto), 1e-9)
        assertEquals(1.0, contrasteWcag(0xFF777777, 0xFF777777), 1e-9)
    }

    @Test
    fun `confere com os valores de referencia do WebAIM`() {
        assertEquals(4.48, contrasteWcag(0xFF777777, branco), 0.005)
        assertEquals(4.00, contrasteWcag(0xFFFF0000, branco), 0.005)
        assertEquals(15.30, contrasteWcag(0xFF00FF00, preto), 0.005)
        assertEquals(8.59, contrasteWcag(0xFF0000FF, branco), 0.005)
    }

    @Test
    fun `canal 10 de 255 ainda fica no trecho linear da curva sRGB`() {
        // 10/255 = 0,0392, abaixo do limiar 0,04045: luminância 0,0392 / 12,92 (o trecho de potência daria 1,06069).
        assertEquals((10 / 255.0 / 12.92 + 0.05) / 0.05, contrasteWcag(0xFF0A0A0A, preto), 1e-9)
    }

    @Test
    fun `misturar vai de uma cor a outra`() {
        assertEquals(0xFF336699, misturar(0xFF336699, preto, 0.0))
        assertEquals(preto, misturar(0xFF336699, preto, 1.0))
        assertEquals(branco, misturar(preto, branco, 1.0))
        assertEquals(0xFF808080, misturar(branco, preto, 0.498))
    }

    @Test
    fun `o pulso escurece a cor em direcao ao preto`() {
        assertEquals(0xFF336699, corDoPulso(0xFF336699, 0.0))
        val maximo = ESCURECIMENTO_MAXIMO_PULSO
        assertEquals(misturar(0xFF336699, preto, maximo), corDoPulso(0xFF336699, maximo))
    }

    @Test
    fun `divisoria e mais discreta que a borda nos dois temas`() {
        TEMAS.forEach { (nome, t) ->
            assertTrue(contrasteWcag(t.outlineVariant, t.surface) < contrasteWcag(t.outline, t.surface), nome)
        }
    }

    @Test
    fun `todo texto do tema passa 4,5 para 1`() {
        val falhas = TEMAS.flatMap { (nome, t) -> abaixoDe(TEXTO, paresDeTexto(t)).map { "$nome $it" } }
        assertTrue(falhas.isEmpty(), "abaixo de 4,5:1: $falhas")
    }

    @Test
    fun `toda borda e todo icone do tema passam 3 para 1`() {
        val falhas = TEMAS.flatMap { (nome, t) -> abaixoDe(ICONE, paresDeIcone(t)).map { "$nome $it" } }
        assertTrue(falhas.isEmpty(), "abaixo de 3:1: $falhas")
    }

    @Test
    fun `selo de cada estado passa 4,5 para 1 no texto e no icone`() {
        val falhas = Tom.entries.filter { contrasteWcag(SINAIS.getValue(it).texto, SINAIS.getValue(it).fundo) < TEXTO }
        assertTrue(falhas.isEmpty(), "selos abaixo de 4,5:1: $falhas")
    }

    @Test
    fun `selo que pulsa continua legivel no ponto mais escuro do pulso`() {
        val pulsam = EstadoMotorista.entries.map(::tela).filter { it.telaCheia }.map { it.tom }
        assertEquals(listOf(Tom.VERMELHO), pulsam)
        pulsam.forEach {
            val sinal = SINAIS.getValue(it)
            assertTrue(contrasteWcag(sinal.texto, corDoPulso(sinal.fundo, ESCURECIMENTO_MAXIMO_PULSO)) >= TEXTO)
        }
    }

    @Test
    fun `cores de estado sao as mesmas nos dois temas e cada tom e unico`() {
        assertEquals(Tom.entries.size, SINAIS.values.map { it.fundo }.toSet().size)
    }

    private fun abaixoDe(minimo: Double, pares: List<Triple<String, Long, Long>>) =
        pares.filter { (_, frente, fundo) -> contrasteWcag(frente, fundo) < minimo }.map { it.first }

    private fun tela(estado: EstadoMotorista) =
        modeloDeTela(Leitura(0, estado, emptySet(), null, Totais(0, 0, 0, 0), 0.0, false, emptyList()))

    private companion object {
        const val TEXTO = 4.5
        const val ICONE = 3.0
        val TEMAS = listOf("escuro" to TEMA_ESCURO, "claro" to TEMA_CLARO)

        /** Fundos onde o app escreve texto comum (telas, cartões, painéis, diálogos). */
        fun superficies(t: EsquemaCores) = listOf(
            "background" to t.background, "surface" to t.surface, "surfaceVariant" to t.surfaceVariant,
            "surfaceContainerLowest" to t.surfaceContainerLowest, "surfaceContainerLow" to t.surfaceContainerLow,
            "surfaceContainer" to t.surfaceContainer, "surfaceContainerHigh" to t.surfaceContainerHigh,
            "surfaceContainerHighest" to t.surfaceContainerHighest,
        )

        fun paresDeTexto(t: EsquemaCores): List<Triple<String, Long, Long>> = listOf(
            Triple("onPrimary/primary", t.onPrimary, t.primary),
            Triple("onPrimaryContainer/primaryContainer", t.onPrimaryContainer, t.primaryContainer),
            Triple("onSecondary/secondary", t.onSecondary, t.secondary),
            Triple("onSecondaryContainer/secondaryContainer", t.onSecondaryContainer, t.secondaryContainer),
            Triple("onError/error", t.onError, t.error),
            Triple("onErrorContainer/errorContainer", t.onErrorContainer, t.errorContainer),
            Triple("onBackground/background", t.onBackground, t.background),
            Triple("inverseOnSurface/inverseSurface", t.inverseOnSurface, t.inverseSurface),
            Triple("inversePrimary/inverseSurface", t.inversePrimary, t.inverseSurface),
        ) + listOf(
            "onSurface" to t.onSurface,
            "onSurfaceVariant" to t.onSurfaceVariant,
            "primary" to t.primary,
            "error" to t.error,
        ).flatMap { (nome, frente) -> superficies(t).map { (sup, fundo) -> Triple("$nome/$sup", frente, fundo) } }

        /** outlineVariant fica de fora: é divisória decorativa, que a WCAG 1.4.11 não cobre. */
        fun paresDeIcone(t: EsquemaCores) =
            superficies(t).map { (sup, fundo) -> Triple("outline/$sup", t.outline, fundo) }
    }
}
