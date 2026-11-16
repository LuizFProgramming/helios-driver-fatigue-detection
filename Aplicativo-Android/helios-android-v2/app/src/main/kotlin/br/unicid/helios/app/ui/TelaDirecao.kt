package br.unicid.helios.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.unicid.helios.app.EstadoFotos
import br.unicid.helios.app.QuadroVisor
import br.unicid.helios.app.R
import br.unicid.helios.app.TelaUi
import br.unicid.helios.app.ViagemAtual
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.protocolos.ESCURECIMENTO_MAXIMO_PULSO
import br.unicid.helios.protocolos.EstadoTela
import br.unicid.helios.protocolos.Icone
import br.unicid.helios.protocolos.Tom

/** Transições da tela de direção (ADR-012, regra 8: de 200 a 300 ms). */
private const val TRANSICAO_MS = 250
private const val UM_SEGUNDO_MS = 1_000L

/**
 * Tela de direção (RF06, RF07, RF11; ADR-012). Lida de relance, a 60–80 cm, em paisagem, em três zonas fixas:
 * o selo de estado no terço de cima, uma linha de apoio com o motivo principal e uma faixa com três números e
 * o Menu. Com o carro em movimento nada se move além do pulso do alerta: o visor ao vivo e o diagnóstico somem.
 */
@Composable
fun TelaDirecao(
    ui: TelaUi,
    visor: QuadroVisor?,
    viagem: ViagemAtual?,
    fotos: EstadoFotos,
    acoes: AcoesMenu,
    escalaTexto: Float = 1f,
    menuNaEsquerda: Boolean = false,
) {
    val densidade = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(densidade.density, densidade.fontScale * escalaTexto)) {
        ConteudoDirecao(ui, visor, viagem, fotos, acoes, menuNaEsquerda)
    }
}

@Composable
private fun ConteudoDirecao(
    ui: TelaUi,
    visor: QuadroVisor?,
    viagem: ViagemAtual?,
    fotos: EstadoFotos,
    acoes: AcoesMenu,
    menuNaEsquerda: Boolean,
) {
    var menuAberto by rememberSaveable { mutableStateOf(false) }
    val estado = ui.estado ?: estadoInicial(ui)
    val parado = !ui.emMovimento
    // CRÍTICO: o selo cresce até a tela inteira e é ele que pulsa (regra 8). Decisão em aberto no ADR-012.
    val fundoNormal = MaterialTheme.colorScheme.background
    val fundo by animateColorAsState(
        if (estado.telaCheia) fundoDoSinal(estado.tom) else fundoNormal,
        tween(TRANSICAO_MS),
        label = "fundo",
    )
    val escurecimento = pulsoCritico(estado.telaCheia)
    val corTexto = if (estado.telaCheia) textoDoSinal(estado.tom) else MaterialTheme.colorScheme.onBackground
    // Texto de apoio: cor do tema (testada no CoresTest), nunca o texto principal com transparência.
    val corApoio = if (estado.telaCheia) corTexto else MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(fundo)
            // Véu preto: escurece o vermelho como o corDoPulso do CoresTest; o texto branco só ganha contraste.
            .background(Color.Black.copy(alpha = escurecimento))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Espaco.gg, vertical = Espaco.m),
    ) {
        val terco = maxHeight / 3
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espaco.m)) {
                Chips(ui, parado)
                // Até um terço da altura; com texto grande em tela baixa, encolhe antes de empurrar a faixa de baixo.
                Selo(estado, Modifier.weight(1f, fill = false).heightIn(max = terco).fillMaxHeight().fillMaxWidth())
                // Segunda informação depois do selo (regra 7): cor do texto principal, acima dos números.
                Text(
                    estado.apoio,
                    style = Tipos.principal,
                    color = corTexto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FaixaInferior(
                ui = ui,
                estado = estado,
                visor = visor.takeIf { parado },
                corTexto = corTexto,
                corApoio = corApoio,
                menuNaEsquerda = menuNaEsquerda,
                abrirMenu = { menuAberto = true },
            )
        }
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
                    manobra = { menuAberto = false; acoes.manobra() },
                ),
                aoFechar = { menuAberto = false },
                modifier = Modifier.align(Alignment.CenterEnd).windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
    }
}

/** Câmera e calibração, discretos, numa faixa própria acima do selo; o diagnóstico só com o carro parado. */
@Composable
private fun Chips(ui: TelaUi, parado: Boolean) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espaco.p)) {
        Chip(textoConexao(ui))
        ui.progressoCalibracao?.let { Chip("Calibrando ${(it * 100).toInt()}%: olhe para a estrada") }
        ui.manobraRestanteMs?.let { Chip("Manobra: ${(it + UM_SEGUNDO_MS - 1) / UM_SEGUNDO_MS} s") }
        if (parado) {
            ui.diagnostico?.let { d ->
                val acelerador = if (ui.aceleracaoGpu) "GPU" else "CPU"
                val descartados = "${d.descartados} descartados"
                Chip("${d.fps} qps na $acelerador · ${d.latenciaMs} ms · $descartados · ${d.memoriaMb} MB")
            }
        }
    }
}

@Composable
private fun Chip(texto: String) {
    Text(
        texto,
        style = Tipos.secundario,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = Espaco.m, vertical = Espaco.p),
    )
}

/** Selo de estado (regra 5): ícone, palavra e cor de fundo do estado. A palavra ocupa a largura que sobra. */
@Composable
private fun Selo(estado: EstadoTela, modifier: Modifier) {
    val fundo by animateColorAsState(fundoDoSinal(estado.tom), tween(TRANSICAO_MS), label = "selo")
    val texto = textoDoSinal(estado.tom)
    Row(
        modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(fundo)
            .padding(horizontal = Espaco.g, vertical = Espaco.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espaco.g),
    ) {
        Icon(
            painterResource(desenhoDoIcone(estado.icone)),
            contentDescription = null, // a palavra ao lado já diz o estado ao leitor de tela
            tint = texto,
            modifier = Modifier.fillMaxHeight(0.7f).aspectRatio(1f).testTag("icone_estado"),
        )
        // Mínimo de 32 sp (regra 4): com o maior tamanho de Ajustes (x1,5), "DESATENTO" ainda cabe no celular.
        BasicText(
            estado.titulo,
            style = Tipos.estado.copy(color = texto),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = Tipos.principal.fontSize, maxFontSize = 220.sp),
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Assertive },
        )
    }
}

/** Três números grandes (regra 7) e o Menu de 76 dp no canto da mão do motorista (regras 3 e 7, Fitts). */
@Composable
private fun FaixaInferior(
    ui: TelaUi,
    estado: EstadoTela,
    visor: QuadroVisor?,
    corTexto: Color,
    corApoio: Color,
    menuNaEsquerda: Boolean,
    abrirMenu: () -> Unit,
) {
    var ampliado by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(top = Espaco.m),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Espaco.gg),
    ) {
        if (menuNaEsquerda) BotaoMenu(abrirMenu)
        Indicador("${estado.perclosPercentual}%", "PERCLOS 60 s", corTexto, corApoio)
        Indicador(estado.piscadasPorMinuto?.toString() ?: "–", "Piscadas/min", corTexto, corApoio)
        Indicador(ui.duracaoViagemMs?.let(::duracaoTexto) ?: "–", "Viagem", corTexto, corApoio)
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(visor != null, enter = fadeIn(tween(TRANSICAO_MS)), exit = fadeOut(tween(TRANSICAO_MS))) {
            visor?.let { Miniatura(it) { ampliado = true } }
        }
        if (!menuNaEsquerda) BotaoMenu(abrirMenu)
    }
    // Visor nulo = carro em movimento (regra 2): fecha a ampliação, que só volta com um novo toque.
    LaunchedEffect(visor == null) { if (visor == null) ampliado = false }
    if (ampliado && visor != null) VisorAmpliado(visor) { ampliado = false }
}

@Composable
private fun Indicador(valor: String, rotulo: String, corTexto: Color, corApoio: Color) {
    Column {
        Text(valor, style = Tipos.numero, color = corTexto, maxLines = 1)
        Text(rotulo, style = Tipos.secundario, color = corApoio, maxLines = 1)
    }
}

/** Visor ao vivo só com o carro parado (regra 2): miniatura de 4:3 com 84 dp de altura, que amplia ao toque. */
@Composable
private fun Miniatura(quadro: QuadroVisor, aoTocar: () -> Unit) {
    Image(
        quadro.imagem.asImageBitmap(),
        contentDescription = "câmera ao vivo; toque para ampliar",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(width = 112.dp, height = 84.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = aoTocar),
    )
}

@Composable
private fun VisorAmpliado(quadro: QuadroVisor, aoFechar: () -> Unit) {
    Dialog(aoFechar, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Image(
            quadro.imagem.asImageBitmap(),
            contentDescription = "câmera ao vivo ampliada",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .clickable(onClick = aoFechar),
        )
    }
}

@Composable
private fun pulsoCritico(ativo: Boolean): Float {
    if (!ativo) return 0f
    val transicao = rememberInfiniteTransition(label = "critico")
    val alfa by transicao.animateFloat(
        initialValue = 0f,
        targetValue = ESCURECIMENTO_MAXIMO_PULSO.toFloat(),
        animationSpec = infiniteRepeatable(tween(TRANSICAO_MS), RepeatMode.Reverse),
        label = "pulso",
    )
    return alfa
}

@DrawableRes
private fun desenhoDoIcone(icone: Icone): Int = when (icone) {
    Icone.CONFIRMADO -> R.drawable.ic_check_circle
    Icone.ATENCAO -> R.drawable.ic_error
    Icone.SONO -> R.drawable.ic_bedtime
    Icone.ALERTA -> R.drawable.ic_warning
    Icone.PESSOA_AUSENTE -> R.drawable.ic_person_off
    Icone.OLHAR_FORA -> R.drawable.ic_visibility_off
    Icone.CAMERA_FRONTAL -> R.drawable.ic_photo_camera_front
    Icone.PAUSA -> R.drawable.ic_pause_circle
    Icone.PARADO -> R.drawable.ic_stop_circle
    Icone.SEM_CAMERA -> R.drawable.ic_videocam_off
    Icone.AGUARDE -> R.drawable.ic_hourglass_top
}

private fun textoConexao(ui: TelaUi): String = if (ui.pausada) "Câmera desligada na pausa" else when (ui.conexao) {
    Conexao.CONECTADA -> "Câmera ok"
    Conexao.CONECTANDO -> "Procurando a câmera"
    Conexao.DESCONECTADA -> "Câmera desconectada"
}

private fun estadoInicial(ui: TelaUi): EstadoTela {
    val (titulo, apoio, icone) = when {
        ui.pausada -> Triple("PAUSADO", "Viagem em pausa: Menu › Retomar", Icone.PAUSA)
        !ui.capturando -> Triple("PARADO", "Nenhuma viagem: Menu › Nova viagem", Icone.PARADO)
        ui.conexao == Conexao.DESCONECTADA -> Triple("SEM CÂMERA", "Verifique a câmera nos ajustes", Icone.SEM_CAMERA)
        else -> Triple("INICIANDO", "Procurando o rosto", Icone.AGUARDE)
    }
    return EstadoTela(
        titulo = titulo,
        detalhe = apoio,
        apoio = apoio,
        tom = Tom.NEUTRO,
        icone = icone,
        telaCheia = false,
        perclosPercentual = 0,
        piscadasPorMinuto = null,
    )
}
