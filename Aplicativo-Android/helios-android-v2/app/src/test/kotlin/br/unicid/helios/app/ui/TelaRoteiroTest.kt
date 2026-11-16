package br.unicid.helios.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.unicid.helios.app.Ajustes
import br.unicid.helios.app.EstadoRoteiro
import br.unicid.helios.dominio.validacao.PassoRoteiro
import br.unicid.helios.dominio.validacao.Tarefa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Roteiro de teste (ADR-015): pedido de óculos e consentimento, tarefa em andamento e fim, com alvos de 48 dp. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w915dp-h412dp-land-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TelaRoteiroTest {

    @get:Rule
    val regra = createComposeRule()

    @Test
    fun `antes de comecar pede oculos e consentimento e guarda a escolha`() {
        var salvo: Ajustes? = null
        var comecou = false
        regra.setContent {
            TemaHelios { TelaRoteiro(null, Ajustes(), AcoesRoteiro({ salvo = it }, { comecou = true }, {}, {})) }
        }
        regra.onNodeWithText("Estou de óculos").performClick()
        assertEquals(true, salvo?.oculos)
        regra.onNodeWithText("Começar o roteiro").performClick()
        assertTrue(comecou)
        regra.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach {
            val altura = it.boundsInRoot.height / regra.density.density
            assertTrue("alvo de $altura dp", altura >= 48f)
        }
    }

    @Test
    fun `tarefa mostra a instrucao e a contagem em segundos inteiros`() {
        val passo = PassoRoteiro(3, Tarefa.FECHAR_CURTO, preparando = true, restanteMs = 2_100)
        val acoes = AcoesRoteiro({}, {}, {}, {})
        regra.setContent { TemaHelios { TelaRoteiro(EstadoRoteiro(passo, 15), Ajustes(), acoes) } }
        regra.onNodeWithText("Prepare-se").assertIsDisplayed()
        regra.onNodeWithText("Feche os olhos e abra no bipe").assertIsDisplayed()
        regra.onNodeWithText("3 s").assertIsDisplayed()
        regra.onNodeWithText("Tarefa 4 de 15").assertIsDisplayed()
    }

    @Test
    fun `fim diz onde estao os arquivos`() {
        regra.setContent {
            TemaHelios {
                TelaRoteiro(EstadoRoteiro(null, 15, concluido = true), Ajustes(), AcoesRoteiro({}, {}, {}, {}))
            }
        }
        regra.onNodeWithText("Roteiro concluído").assertIsDisplayed()
        regra.onNodeWithText("Os arquivos de roteiro estão", substring = true).assertIsDisplayed()
    }
}
