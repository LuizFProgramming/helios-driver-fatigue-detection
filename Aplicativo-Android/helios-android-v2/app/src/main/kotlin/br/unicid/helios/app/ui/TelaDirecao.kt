package br.unicid.helios.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import br.unicid.helios.app.Diagnostico
import br.unicid.helios.app.QuadroVisor
import br.unicid.helios.app.EstadoFotos
import br.unicid.helios.app.TelaUi
import br.unicid.helios.app.ViagemAtual
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.protocolos.EstadoTela
import br.unicid.helios.protocolos.ESCURECIMENTO_MAXIMO_PULSO
import br.unicid.helios.protocolos.Icone
import br.unicid.helios.protocolos.Tom

/**
 * Tela de direção (RF06, RF07, RF11). Lida de relance, a 60–80 cm, em paisagem.
 * Mostra só o estado, o motivo e um olho que fecha junto com o do motorista.
 * Histórico e gráficos ficam fora daqui (RF09).
 */
@Composable
fun TelaDirecao(
    ui: TelaUi,
    visor: QuadroVisor?,
    viagem: ViagemAtual?,
    fotos: EstadoFotos,
    acoes: AcoesMenu,
    escalaTexto: Float = 1f,
) {
    val densidade = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(densidade.density, densidade.fontScale * escalaTexto)) {
        ConteudoDirecao(ui, visor, viagem, fotos, acoes)
    }
}

@Composable
private fun ConteudoDirecao(
    ui: TelaUi,
    visor: QuadroVisor?,
    viagem: ViagemAtual?,
    fotos: EstadoFotos,
    acoes: AcoesMenu,
) {
    var menuAberto by rememberSaveable { mutableStateOf(false) }
    val estado = ui.estado ?: estadoInicial(ui)
    val fundoNormal = MaterialTheme.colorScheme.background
    val fundo by animateColorAsState(if (estado.telaCheia) fundoDoSinal(estado.tom) else fundoNormal, label = "fundo")
    val escurecimento = pulsoCritico(estado.telaCheia)
    val corTexto = if (estado.telaCheia) textoDoSinal(estado.tom) else MaterialTheme.colorScheme.onBackground
    // Texto de apoio: cor do tema (testada no CoresTest), nunca o texto principal com transparência.
    val corApoio = if (estado.telaCheia) corTexto else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        Modifier
            .fillMaxSize()
            .background(fundo)
            // Véu preto: escurece o vermelho como o corDoPulso do CoresTest, então o texto branco só ganha contraste.
            .background(Color.Black.copy(alpha = escurecimento))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            val pesoTexto = if (visor != null) 1.1f else 1.6f
            Column(Modifier.weight(pesoTexto).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                TituloEstado(estado, corTexto)
                Text(estado.detalhe, style = Tipos.detalhe, color = corApoio)
                estado.cronometro?.let {
                    Text(it, style = Tipos.detalhe.copy(fontSize = 44.sp), color = corTexto)
                }
                if (visor != null) PainelMetricas(ui.metricas.drop(1), corApoio)
            }
            if (visor != null) {
                Visor(visor, Modifier.weight(1.4f).fillMaxHeight().padding(start = 16.dp, bottom = 84.dp))
            } else {
                Olho(
                    fechado = estado.olhoFechado,
                    corIris = if (estado.telaCheia) textoDoSinal(estado.tom) else fundoDoSinal(estado.tom),
                    corPupila = if (estado.telaCheia) fundoDoSinal(estado.tom) else textoDoSinal(estado.tom),
                    corContorno = corTexto,
                    modifier = Modifier.weight(1f).aspectRatio(1.4f),
                )
            }
        }
        Rodape(ui, estado, corApoio, Modifier.align(Alignment.BottomStart).padding(end = 200.dp))
        ui.progressoCalibracao?.let { Calibrando(it, Modifier.align(Alignment.TopStart)) }
        ui.diagnostico?.let { PainelDiagnostico(it, Modifier.align(Alignment.TopEnd)) }
        if (!menuAberto) BotaoMenu({ menuAberto = true }, Modifier.align(Alignment.BottomEnd))
    }
    if (menuAberto) {
        Box(Modifier.fillMaxSize()) {
            // Fundo escurecido: tocar fora do painel fecha o menu. O painel é irmão (não filho), então tocar nele não fecha.
            val veu = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)
            Box(Modifier.fillMaxSize().background(veu).clickable { menuAberto = false })
            PainelMenu(
                ui,
                viagem,
                fotos,
                // Toda ação que troca de tela, retoma ou encerra fecha o painel; pausar deixa aberto para ver os números da parada.
                AcoesMenu(
                    pausar = acoes.pausar,
                    retomar = { menuAberto = false; acoes.retomar() },
                    encerrar = { menuAberto = false; acoes.encerrar() },
                    novaViagem = { menuAberto = false; acoes.novaViagem() },
                    historico = { menuAberto = false; acoes.historico() },
                    ajustes = { menuAberto = false; acoes.ajustes() },
                    atualizarNumeros = acoes.atualizarNumeros,
                ),
                aoFechar = { menuAberto = false },
                modifier = Modifier.align(Alignment.CenterEnd).windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
    }
}

@Composable
private fun TituloEstado(estado: EstadoTela, corTexto: Color) {
    // A palavra do estado ocupa a largura disponível: é o que o motorista vê de canto de olho.
    // O tamanho é medido na fonte real. O mínimo é o do texto principal (32 sp, ADR-012 regra 4): com o maior
    // tamanho de Ajustes (x1,5) e o visor ligado, "DESATENTO" ainda cabe na coluna do celular.
    BasicText(
        estado.titulo,
        style = Tipos.estado.copy(color = corTexto),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = Tipos.principal.fontSize, maxFontSize = 220.sp),
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

/**
 * O olho do HELIOS: as pálpebras fecham quando o sistema considera o olho do motorista fechado
 * (mesmo critério do estado). Serve de retorno imediato de que a câmera está acompanhando.
 */
@Composable
private fun Olho(fechado: Boolean, corIris: Color, corPupila: Color, corContorno: Color, modifier: Modifier) {
    val abertura by animateFloatAsState(if (fechado) 0.06f else 1f, tween(90), label = "palpebra")
    Canvas(modifier.semantics { contentDescription = if (fechado) "olho fechado" else "olho aberto" }) {
        val meioY = size.height / 2
        val altura = size.height * 0.42f * abertura
        val palpebras = Path().apply {
            moveTo(0f, meioY)
            quadraticTo(size.width / 2, meioY - altura * 2, size.width, meioY)
            quadraticTo(size.width / 2, meioY + altura * 2, 0f, meioY)
            close()
        }
        clipPath(palpebras) {
            drawCircle(corIris, radius = size.height * 0.3f, center = Offset(size.width / 2, meioY))
            drawCircle(corPupila, radius = size.height * 0.11f, center = Offset(size.width / 2, meioY))
        }
        drawPath(palpebras, corContorno, style = Stroke(width = 10f, cap = StrokeCap.Round))
    }
}

@Composable
private fun Rodape(ui: TelaUi, estado: EstadoTela, suave: Color, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(textoConexao(ui), style = Tipos.rodape, color = suave)
        Spacer(Modifier.weight(1f))
        Text("olhos fechados ${estado.perclosPercentual}% no último minuto", style = Tipos.rodape, color = suave)
    }
}

/**
 * Visor de análise: a imagem exata que o MediaPipe analisou, com a malha, os pontos usados,
 * os olhos (verde aberto, vermelho fechado) e os eixos da cabeça desenhados por cima.
 */
@Composable
private fun Visor(quadro: QuadroVisor, modifier: Modifier) {
    Image(
        bitmap = quadro.imagem.asImageBitmap(),
        contentDescription = "câmera com a malha do rosto",
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/** Números do frame atual (cada olho, boca, cabeça, PERCLOS), em letra menor: para análise, não para dirigir. */
@Composable
private fun PainelMetricas(linhas: List<String>, corTexto: Color) {
    Column(Modifier.padding(top = 12.dp)) {
        linhas.forEach { Text(it, style = Tipos.rodape.copy(fontSize = 13.sp), color = corTexto) }
    }
}

/** Medição de desempenho na tela (fps, frames descartados, memória e latência), para comparar o pool de bitmaps. */
@Composable
private fun PainelDiagnostico(d: Diagnostico, modifier: Modifier) {
    val corTexto = MaterialTheme.colorScheme.onSurface
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp)) {
        val estilo = Tipos.rodape.copy(fontSize = 16.sp)
        Text("${d.fps} fps · ${d.latenciaMs} ms da captura ao resultado", style = estilo, color = corTexto)
        Text("${d.descartados} frames descartados · ${d.memoriaMb} MB em uso", style = estilo, color = corTexto)
        val pool = if (d.poolDeBitmaps) "pool de bitmaps ligado" else "pool de bitmaps desligado"
        Text(pool, style = estilo, color = corTexto)
    }
}

@Composable
private fun Calibrando(progresso: Double, modifier: Modifier) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp)) {
        Text("Olhe para a estrada como se estivesse dirigindo, com os olhos abertos", style = Tipos.detalhe)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { progresso.toFloat() }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun pulsoCritico(ativo: Boolean): Float {
    if (!ativo) return 0f
    val transicao = rememberInfiniteTransition(label = "critico")
    val alfa by transicao.animateFloat(
        initialValue = 0f,
        targetValue = ESCURECIMENTO_MAXIMO_PULSO.toFloat(),
        animationSpec = infiniteRepeatable(tween(250), RepeatMode.Reverse),
        label = "pulso",
    )
    return alfa
}

private fun textoConexao(ui: TelaUi): String = if (ui.pausada) "câmera desligada na pausa" else when (ui.conexao) {
    Conexao.CONECTADA -> "câmera ok, ${ui.fps} quadros/s" + if (ui.aceleracaoGpu) " na GPU" else " na CPU"
    Conexao.CONECTANDO -> "procurando a câmera"
    Conexao.DESCONECTADA -> "câmera desconectada"
}

private fun estadoInicial(ui: TelaUi) = EstadoTela(
    titulo = when {
        ui.pausada -> "PAUSADO"
        !ui.capturando -> "PARADO"
        ui.conexao == Conexao.DESCONECTADA -> "SEM CÂMERA"
        else -> "INICIANDO"
    },
    detalhe = when {
        ui.pausada -> "viagem em pausa: Menu › Retomar"
        !ui.capturando -> "nenhuma viagem: Menu › Nova viagem"
        ui.conexao == Conexao.DESCONECTADA -> "verifique a câmera nos ajustes"
        else -> "procurando o rosto"
    },
    tom = Tom.NEUTRO,
    icone = when {
        ui.pausada -> Icone.PAUSA
        !ui.capturando -> Icone.PARADO
        ui.conexao == Conexao.DESCONECTADA -> Icone.SEM_CAMERA
        else -> Icone.AGUARDE
    },
    telaCheia = false,
    perclosPercentual = 0,
    olhoFechado = false,
    cronometro = null,
)
