package br.unicid.helios.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import br.unicid.helios.app.Diagnostico
import br.unicid.helios.app.EstadoFotos
import br.unicid.helios.app.QuadroVisor
import br.unicid.helios.app.TelaUi
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.protocolos.modeloDeTela
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Evidência das regras 2, 3, 4, 5 e 7 do ADR-012 na tela de direção, medida na JVM (Robolectric).
 * Celular em paisagem (915 x 412 dp) por padrão; os testes de medida repetem na central (1280 x 720 dp).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = CELULAR)
// Gráficos nativos: no modo legado o Robolectric não mede texto de verdade e a largura não depende da fonte.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TelaDirecaoTest {

    @get:Rule
    val regra = createComposeRule()

    private val fotos = EstadoFotos(ligadas = true, onde = "memória interna", gravadas = 0, ultima = null)
    private val acoes = AcoesMenu({}, {}, {}, {}, {}, {}, {})
    private val diagnostico =
        Diagnostico(fps = 24, descartados = 0, memoriaMb = 210, latenciaMs = 38, poolDeBitmaps = true)
    private val visor = QuadroVisor(Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888), 1)

    private fun ui(
        estado: EstadoMotorista = EstadoMotorista.FADIGA,
        motivos: Set<Motivo> = setOf(Motivo.PERCLOS_FADIGA),
        emMovimento: Boolean = false,
    ) = TelaUi(
        estado = modeloDeTela(Leitura(0, estado, motivos, null, Totais(0, 0, 0, 0), 3.0, emMovimento, emptyList())),
        conexao = Conexao.CONECTADA,
        fps = 24,
        emMovimento = emMovimento,
        capturando = true,
        progressoCalibracao = 0.4,
        duracaoViagemMs = 47 * 60_000L,
    )

    private fun mostrar(ui: TelaUi, visor: QuadroVisor? = null, menuNaEsquerda: Boolean = false) =
        regra.setContent {
            TemaHelios { TelaDirecao(ui, visor, null, fotos, acoes, menuNaEsquerda = menuNaEsquerda) }
        }

    // --- Regra 3: alvos de 76 dp, sem sobreposição -----------------------------------------------------

    @Test
    fun `todo alvo de toque tem pelo menos 76 dp e nenhum se sobrepoe`() = conferirAlvos()

    @Test
    @Config(qualifiers = CENTRAL)
    fun `na central todo alvo de toque tem pelo menos 76 dp e nenhum se sobrepoe`() = conferirAlvos()

    /** Celular menor (vídeo de 16/11): com a miniatura, o Menu ficava com as letras empilhadas e esmagava o selo. */
    @Test
    @Config(qualifiers = "w720dp-h340dp-land-420dpi")
    fun `em celular estreito com o visor o Menu fica numa linha e o selo nao e esmagado`() {
        mostrar(ui(EstadoMotorista.DISTRAIDO, setOf(Motivo.CABECA_VIRADA)), visor)
        val menu = regra.onNode(hasClickAction() and hasContentDescription("Menu")).fetchSemanticsNode().boundsInRoot
        val medida = "Menu de ${emDp(menu.width)} x ${emDp(menu.height)} dp"
        assertTrue(medida, emDp(menu.height) <= 96f && emDp(menu.width) >= 76f)
        val icone = regra.onNodeWithTag("icone_estado", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("ícone do selo com ${emDp(icone.height)} dp", emDp(icone.height) >= 32f)
    }

    /** Revisão do ADR-012 na 0.4.13: o sensor de movimento recomenda, não impede. */
    @Test
    fun `com o celular se mexendo o menu avisa mas nao bloqueia historico e ajustes`() {
        regra.setContent {
            TemaHelios { PainelMenu(ui(emMovimento = true), null, fotos, acoes, aoFechar = {}) }
        }
        regra.onNodeWithText("só com o carro parado", substring = true).assertExists()
        regra.onNodeWithText("Histórico").assertIsEnabled()
        regra.onNodeWithText("Ajustes").assertIsEnabled()
    }

    private fun conferirAlvos() {
        mostrar(ui(), visor) // parado e com o visor: é quando a tela tem mais alvos (Menu e miniatura)
        val alvos = regra.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals("Menu e miniatura do visor", 2, alvos.size)
        alvos.forEach {
            val (largura, altura) = emDp(it.width) to emDp(it.height)
            assertTrue("alvo de $largura x $altura dp", minOf(largura, altura) >= 76f)
        }
        alvos.forEachIndexed { i, a ->
            alvos.drop(i + 1).forEach { b -> assertTrue("alvos sobrepostos", !a.sobrepoe(b)) }
        }
    }

    // --- Regra 4: texto de 32 e 24 sp, até 120 caracteres --------------------------------------------

    @Test
    fun `nenhum texto fica abaixo de 24 sp nem passa de 120 caracteres`() = conferirTextos()

    @Test
    @Config(qualifiers = CENTRAL)
    fun `na central nenhum texto fica abaixo de 24 sp nem passa de 120 caracteres`() = conferirTextos()

    private fun conferirTextos() {
        mostrar(ui(), visor)
        val textos = textosNaTela()
        assertTrue("a tela não tem texto", textos.isNotEmpty())
        textos.forEach { t ->
            assertTrue("\"${t.layoutInput.text}\" abaixo de 24 sp", t.atinge(24f))
            assertTrue("\"${t.layoutInput.text}\" passa de 120 caracteres", t.layoutInput.text.length <= 120)
        }
    }

    @Test
    fun `palavra do estado e linha de apoio tem pelo menos 32 sp`() {
        mostrar(ui())
        val textos = textosNaTela().associateBy { it.layoutInput.text.text }
        listOf("FADIGA", "Olhos pesados").forEach {
            val t = textos[it] ?: error("\"$it\" não está na tela; há ${textos.keys}")
            assertTrue("\"$it\" abaixo de 32 sp", t.atinge(32f))
        }
    }

    // --- Regra 5: palavra, ícone e cor ----------------------------------------------------------------

    @Test
    fun `selo mostra a palavra e o icone do estado`() {
        mostrar(ui(EstadoMotorista.DISTRAIDO, setOf(Motivo.OLHAR_DESVIADO)))
        regra.onNodeWithText("DISTRAÍDO").assertExists()
        regra.onNodeWithTag("icone_estado", useUnmergedTree = true).assertExists()
    }

    // --- Regra 2: nada de imagem ao vivo com o carro em movimento -------------------------------------

    @Test
    fun `visor nao aparece com o carro em movimento`() {
        mostrar(ui(emMovimento = true), visor)
        regra.onNodeWithContentDescription(VISOR).assertDoesNotExist()
    }

    @Test
    fun `parado o visor e uma miniatura que amplia ao toque`() {
        mostrar(ui(), visor)
        regra.onNodeWithContentDescription(VISOR).assertExists().performClick()
        regra.onNodeWithContentDescription(VISOR_AMPLIADO).assertExists()
    }

    @Test
    fun `visor ampliado fecha quando o carro anda e nao reabre sozinho ao parar`() {
        var tela by mutableStateOf(ui())
        regra.setContent { TemaHelios { TelaDirecao(tela, visor, null, fotos, acoes) } }
        regra.onNodeWithContentDescription(VISOR).performClick()
        tela = ui(emMovimento = true)
        regra.waitForIdle()
        regra.onNodeWithContentDescription(VISOR_AMPLIADO).assertDoesNotExist()
        tela = ui()
        regra.waitForIdle()
        regra.onNodeWithContentDescription(VISOR_AMPLIADO).assertDoesNotExist()
    }

    @Test
    fun `diagnostico de desempenho some com o carro em movimento`() {
        mostrar(ui(emMovimento = true).copy(diagnostico = diagnostico))
        regra.onNodeWithText("210 MB", substring = true).assertDoesNotExist()
    }

    @Test
    fun `diagnostico de desempenho aparece com o carro parado`() {
        mostrar(ui().copy(diagnostico = diagnostico))
        regra.onNodeWithText("210 MB", substring = true).assertExists()
    }

    // --- Regra 7: uma informação principal, Menu no canto da mão --------------------------------------

    @Test
    fun `faixa de baixo mostra perclos, piscadas e tempo de viagem`() {
        mostrar(ui())
        listOf("PERCLOS 60 s", "Piscadas/min", "Viagem", "47 min").forEach { regra.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `menu fica a direita por padrao`() {
        mostrar(ui())
        assertTrue(centroDoMenu() > larguraDaTela() / 2)
    }

    @Test
    fun `menu fica a esquerda quando escolhido em ajustes`() {
        mostrar(ui(), menuNaEsquerda = true)
        assertTrue(centroDoMenu() < larguraDaTela() / 2)
    }

    @Test
    fun `com o maior texto de ajustes o menu continua inteiro na tela e com 76 dp`() {
        regra.setContent { TemaHelios { TelaDirecao(ui(), visor, null, fotos, acoes, escalaTexto = 1.5f) } }
        val menu = regra.onNode(hasClickAction() and hasText("Menu")).fetchSemanticsNode().boundsInRoot
        assertTrue("Menu sai da tela: $menu", menu.right <= larguraDaTela() + 0.5f && menu.left >= 0f)
        assertTrue("Menu com ${emDp(menu.width)} dp", emDp(menu.width) >= 76f && emDp(menu.height) >= 76f)
    }

    @Test
    fun `manobra aparece como chip com os segundos que faltam`() {
        mostrar(ui().copy(manobraRestanteMs = 23_400))
        regra.onNodeWithText("Manobra: 24 s").assertExists()
    }

    @Test
    fun `botao manobra do menu tem 76 dp`() {
        mostrar(ui())
        regra.onNode(hasClickAction() and hasText("Menu")).performClick()
        val botao = regra.onNode(hasClickAction() and hasText("Manobra", substring = true))
            .fetchSemanticsNode().boundsInRoot
        assertTrue("botão de ${emDp(botao.height)} dp", emDp(botao.height) >= 76f && emDp(botao.width) >= 76f)
    }

    private fun centroDoMenu() =
        regra.onNodeWithText("Menu", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center.x

    private fun larguraDaTela() = regra.onRoot().fetchSemanticsNode().boundsInRoot.width

    private fun emDp(px: Float) = px / regra.density.density

    private fun Rect.sobrepoe(outro: Rect) =
        left < outro.right && outro.left < right && top < outro.bottom && outro.top < bottom

    /**
     * O texto tem pelo menos [minimoSp]? Com tamanho automático (a palavra do estado) a semântica traz o estilo
     * sem o tamanho escolhido; aí compara a largura na tela com a do mesmo texto medido no mínimo, que cresce
     * com a fonte.
     */
    private fun TextLayoutResult.atinge(minimoSp: Float): Boolean {
        val estilo = layoutInput.style
        if (estilo.fontSize.isSpecified) return estilo.fontSize.value >= minimoSp
        val medidor = TextMeasurer(layoutInput.fontFamilyResolver, layoutInput.density, layoutInput.layoutDirection)
        val noMinimo = medidor.measure(layoutInput.text, estilo.copy(fontSize = minimoSp.sp), maxLines = 1)
        return largura() >= noMinimo.largura() - 0.5f
    }

    private fun TextLayoutResult.largura() = getLineRight(0) - getLineLeft(0)

    private fun textosNaTela(): List<TextLayoutResult> {
        fun SemanticsNode.todos(): List<SemanticsNode> = listOf(this) + children.flatMap { it.todos() }
        return regra.onRoot(useUnmergedTree = true).fetchSemanticsNode().todos().mapNotNull { no ->
            no.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.let { acao ->
                mutableListOf<TextLayoutResult>().also { acao(it) }.firstOrNull()
            }
        }
    }
}

private const val CELULAR = "w915dp-h412dp-land-420dpi"
private const val CENTRAL = "w1280dp-h720dp-land-mdpi"
private const val VISOR = "câmera ao vivo; toque para ampliar"
private const val VISOR_AMPLIADO = "câmera ao vivo ampliada"
