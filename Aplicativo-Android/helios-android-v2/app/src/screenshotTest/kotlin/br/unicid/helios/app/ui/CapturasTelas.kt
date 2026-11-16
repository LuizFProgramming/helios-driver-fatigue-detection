package br.unicid.helios.app.ui

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import br.unicid.helios.app.DetalheViagem
import br.unicid.helios.app.EstadoFotos
import br.unicid.helios.app.QuadroVisor
import br.unicid.helios.app.TelaUi
import br.unicid.helios.app.ViagemAtual
import br.unicid.helios.app.Ajustes
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.app.portas.SessaoGravada
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.protocolos.LinhaJanela
import br.unicid.helios.protocolos.modeloDeTela
import com.android.tools.screenshot.PreviewTest

/**
 * Capturas das telas para docs/interface/ (ADR-012). Dados inventados e nenhum rosto: o visor fica desligado.
 * Gravar: gradle :app:updateDebugScreenshotTest. Comparar: gradle :app:validateDebugScreenshotTest.
 */
@Preview(name = "celular_escuro", device = CELULAR, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "celular_claro", device = CELULAR, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "central_escuro", device = CENTRAL, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "central_claro", device = CENTRAL, uiMode = UI_MODE_NIGHT_NO)
annotation class Aparelhos

private const val CELULAR = "spec:width=915dp,height=412dp,dpi=420"
private const val CENTRAL = "spec:width=1280dp,height=720dp,dpi=160"

/** O app não segue o tema do sistema; aqui o modo noite da prévia só escolhe qual dos dois temas capturar. */
@Composable
private fun Tema(conteudo: @Composable () -> Unit) = TemaHelios(escuro = isSystemInDarkTheme(), conteudo = conteudo)

private val sessao = SessaoGravada(
    id = 3, inicioEpochMs = 1_794_800_000_000, duracaoMs = 47 * 60_000L, tempoEmAlertaMs = 95_000,
    piscadas = 812, microssonos = 2, bocejos = 5, desatencoes = 1, distracoes = 4,
    perclosMedio = 0.06, perclosMaximo = 0.21, kss = 6,
)
private val fotos = EstadoFotos(ligadas = true, onde = "memória interna", gravadas = 7, ultima = null)
private val acoes = AcoesMenu({}, {}, {}, {}, {}, {}, {})

private val metricas = Metricas(0.3, 0.3, 0.1, 0.5, 0.1, 0.12, false, false, null, piscadasNoMinuto = 17)

private fun ui(estado: EstadoMotorista, vararg motivos: Motivo, segundos: Double = 0.0, andando: Boolean = true) = TelaUi(
    estado = modeloDeTela(Leitura(0, estado, motivos.toSet(), metricas, Totais(0, 0, 0, 0), segundos, andando, emptyList())),
    conexao = Conexao.CONECTADA,
    fps = 24,
    emMovimento = andando,
    capturando = true,
    duracaoViagemMs = 47 * 60_000L,
)

/** Imagem sintética no lugar da câmera (nenhum rosto de pessoa real nas capturas). */
private fun imagemSintetica(): Bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply {
    Canvas(this).drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 640f, 480f, 0xFF3A4652.toInt(), 0xFF8E99A4.toInt(), Shader.TileMode.CLAMP) })
}

@Composable
private fun Direcao(ui: TelaUi, visor: QuadroVisor? = null) = Tema { TelaDirecao(ui, visor, null, fotos, acoes) }

@PreviewTest @Aparelhos @Composable
fun DirecaoAtento() = Direcao(ui(EstadoMotorista.ATENTO))

@PreviewTest @Aparelhos @Composable
fun DirecaoFadiga() = Direcao(ui(EstadoMotorista.FADIGA, Motivo.PERCLOS_FADIGA, Motivo.BOCEJOS_FREQUENTES))

@PreviewTest @Aparelhos @Composable
fun DirecaoCritico() = Direcao(ui(EstadoMotorista.CRITICO, Motivo.SONO, segundos = 2.4))

@PreviewTest @Aparelhos @Composable
fun DirecaoDistraido() = Direcao(ui(EstadoMotorista.DISTRAIDO, Motivo.OLHAR_DESVIADO, segundos = 3.1))

@PreviewTest @Aparelhos @Composable
fun DirecaoParadoComVisor() = Direcao(ui(EstadoMotorista.ATENTO, andando = false), QuadroVisor(imagemSintetica(), 1))

@PreviewTest @Aparelhos @Composable
fun DirecaoSemRosto() = Direcao(ui(EstadoMotorista.SEM_ROSTO, Motivo.ROSTO_AUSENTE))

@PreviewTest @Aparelhos @Composable
fun Menu() = Tema {
    PainelMenu(ui(EstadoMotorista.ATENTO), ViagemAtual(sessao, 17, pausada = false), fotos, acoes, aoFechar = {})
}

@PreviewTest @Aparelhos @Composable
fun HistoricoLista() = Tema {
    TelaHistorico(
        listOf(sessao, sessao.copy(id = 2, duracaoMs = 12 * 60_000L, tempoEmAlertaMs = 0, kss = null)),
        null, null, {}, { _, _ -> }, { _, _ -> }, { null }, { null }, {},
    )
}

@PreviewTest @Aparelhos @Composable
fun HistoricoDetalhe() = Tema {
    val janelas = List(8) { i ->
        LinhaJanela(
            sessao = 3, inicioEpochMs = sessao.inicioEpochMs + i * 30_000L, perclosLongo = 0.03 + i * 0.02,
            piscadasPorMinuto = 15.0 + i, duracaoMediaPiscadaMs = 180.0, bocejosPorMinuto = 0.5, desvioPitchGraus = 2.0, kss = null,
        )
    }
    TelaHistorico(listOf(sessao), DetalheViagem(sessao, janelas, fotos = 0), null, {}, { _, _ -> }, { _, _ -> }, { null }, { null }, {})
}

@PreviewTest @Aparelhos @Composable
fun Ajustes() = Tema { TelaAjustes(Ajustes(), fotos, {}, {}, {}, {}, {}, {}) }
