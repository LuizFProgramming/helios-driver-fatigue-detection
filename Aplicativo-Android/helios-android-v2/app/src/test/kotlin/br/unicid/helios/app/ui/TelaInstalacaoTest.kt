package br.unicid.helios.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import br.unicid.helios.app.EstadoInstalacao
import br.unicid.helios.app.QuadroVisor
import br.unicid.helios.dominio.instalacao.Dica
import br.unicid.helios.dominio.instalacao.EstadoAssistente
import br.unicid.helios.dominio.instalacao.Etapa
import br.unicid.helios.dominio.instalacao.NotaInstalacao
import br.unicid.helios.dominio.instalacao.PartesNota
import br.unicid.helios.dominio.instalacao.Qualidade
import br.unicid.helios.dominio.instalacao.ResultadoVerificacao
import br.unicid.helios.dominio.instalacao.Verificacao
import br.unicid.helios.dominio.instalacao.Zona
import br.unicid.helios.dominio.tipos.CaixaRosto
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Assistente de instalação (ADR-013), medido na JVM: é usado com o carro parado, então os alvos têm de ter
 * 48 dp (Material 3) e o texto, 14 sp (ADR-012, regras 3 e 4). Cada verificação mostra ícone, nome e dica (regra 5).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w915dp-h412dp-land-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TelaInstalacaoTest {

    @get:Rule
    val regra = createComposeRule()

    private val visor = QuadroVisor(Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888), 1)
    private val acoes = AcoesInstalacao({ _, _ -> }, {}, {}, {}, {}, {}, cancelar = {})

    private val verificacoes = listOf(
        ResultadoVerificacao(Verificacao.ROSTO_INTEIRO, null),
        ResultadoVerificacao(Verificacao.TAMANHO, null),
        ResultadoVerificacao(Verificacao.LUZ, null),
        ResultadoVerificacao(Verificacao.ANGULO, Dica.SUBIR),
        ResultadoVerificacao(Verificacao.CAMERA_PARADA, null),
    )

    private fun estado(etapa: Etapa, nota: NotaInstalacao? = null, podeCalibrar: Boolean = false) = EstadoAssistente(
        etapa = etapa,
        verificacoes = verificacoes,
        podeCalibrar = podeCalibrar,
        calibracaoFalhou = false,
        progresso = if (etapa == Etapa.CALIBRACAO) 0.4 else null,
        zona = Zona.ESPELHO_ESQUERDO.takeIf { etapa == Etapa.ZONAS },
        coletandoZona = false,
        zonaFalhou = false,
        nota = nota,
    )

    private fun mostrar(e: EstadoAssistente, emMovimento: Boolean = false) = regra.setContent {
        TemaHelios { TelaInstalacao(EstadoInstalacao(e, CaixaRosto(0.6, 0.2, 0.9, 0.7)), visor, emMovimento, acoes) }
    }

    private val etapas = listOf(
        estado(Etapa.POSICAO),
        estado(Etapa.ENQUADRAMENTO),
        estado(Etapa.CALIBRACAO),
        estado(Etapa.ZONAS),
        estado(Etapa.RESUMO, NotaInstalacao(89, Qualidade.BOA, PartesNota(60, 25, 4))),
    )

    @Test
    fun `em todas as etapas os alvos tem 48 dp e o texto 14 sp`() {
        var atual by mutableStateOf(etapas.first())
        regra.setContent {
            TemaHelios { TelaInstalacao(EstadoInstalacao(atual, CaixaRosto(0.6, 0.2, 0.9, 0.7)), visor, false, acoes) }
        }
        etapas.forEach { e ->
            atual = e
            regra.waitForIdle()
            val alvos = regra.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { it.boundsInRoot }
            assertTrue("${e.etapa}: sem alvos", alvos.isNotEmpty())
            alvos.forEach {
                val (largura, altura) = it.width / regra.density.density to it.height / regra.density.density
                assertTrue("${e.etapa}: alvo de $largura x $altura dp", minOf(largura, altura) >= 48f)
            }
            textosNaTela().forEach { t ->
                val tamanho = t.layoutInput.style.fontSize
                assertTrue("${e.etapa}: \"${t.layoutInput.text}\" com $tamanho", tamanho.value >= 14f)
            }
        }
    }

    @Test
    fun `verificacao que falha mostra icone, nome e a dica de correcao`() {
        mostrar(estado(Etapa.ENQUADRAMENTO))
        regra.onNodeWithTag("falha_ANGULO", useUnmergedTree = true).assertExists()
        regra.onNodeWithText("Ângulo da câmera").assertExists()
        regra.onNodeWithText("Suba a câmera").assertExists()
        regra.onNodeWithTag("ok_LUZ", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `calibracao so libera com o enquadramento certo`() {
        mostrar(estado(Etapa.ENQUADRAMENTO, podeCalibrar = false))
        regra.onNodeWithText("Começar a calibração").assertIsNotEnabled()
    }

    @Test
    fun `calibracao liberada quando o assistente permite`() {
        mostrar(estado(Etapa.ENQUADRAMENTO, podeCalibrar = true))
        regra.onNodeWithText("Começar a calibração").assertIsEnabled()
    }

    @Test
    fun `instalacao para refazer bloqueia o usar`() {
        mostrar(estado(Etapa.RESUMO, NotaInstalacao(51, Qualidade.REFAZER, PartesNota(36, 0, 15))))
        regra.onNodeWithText("Câmera e rosto parados: 0 de 25").assertExists()
        regra.onNodeWithText("Usar esta instalação").assertIsNotEnabled()
        regra.onNodeWithText("Refazer instalação").assertIsEnabled()
    }

    @Test
    fun `instalacao boa pode ser usada`() {
        mostrar(estado(Etapa.RESUMO, NotaInstalacao(89, Qualidade.BOA, PartesNota(60, 25, 4))))
        regra.onNodeWithText("Usar esta instalação").assertIsEnabled()
    }

    @Test
    fun `com o carro andando o assistente para e pede para parar`() {
        mostrar(estado(Etapa.ENQUADRAMENTO, podeCalibrar = true), emMovimento = true)
        regra.onNodeWithText("Pare o carro para instalar a câmera.").assertExists()
        regra.onNodeWithText("Começar a calibração").assertDoesNotExist()
    }

    private fun textosNaTela(): List<TextLayoutResult> {
        fun SemanticsNode.todos(): List<SemanticsNode> = listOf(this) + children.flatMap { it.todos() }
        return regra.onRoot(useUnmergedTree = true).fetchSemanticsNode().todos().mapNotNull { no ->
            no.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.let { acao ->
                mutableListOf<TextLayoutResult>().also { acao(it) }.firstOrNull()
            }
        }
    }
}
