package br.unicid.helios.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import br.unicid.helios.app.EstadoInstalacao
import br.unicid.helios.app.QuadroVisor
import br.unicid.helios.app.R
import br.unicid.helios.dominio.instalacao.EstadoAssistente
import br.unicid.helios.dominio.instalacao.Etapa
import br.unicid.helios.dominio.instalacao.LadoMotorista
import br.unicid.helios.dominio.instalacao.PerfilInstalacao
import br.unicid.helios.dominio.instalacao.PosicaoCamera
import br.unicid.helios.dominio.instalacao.Qualidade
import br.unicid.helios.dominio.instalacao.ResultadoVerificacao
import br.unicid.helios.dominio.instalacao.Verificacao
import br.unicid.helios.dominio.instalacao.Zona
import br.unicid.helios.dominio.tipos.CaixaRosto
import br.unicid.helios.protocolos.TextosInstalacao

/** Toques do assistente. [cancelar] nulo: não há instalação para voltar, então não há como sair dele. */
class AcoesInstalacao(
    val escolherPosicao: (PosicaoCamera, LadoMotorista) -> Unit,
    val comecarCalibracao: () -> Unit,
    val gravarZona: () -> Unit,
    val pularZona: () -> Unit,
    val repetir: () -> Unit,
    val usar: (nome: String) -> Unit,
    val cancelar: (() -> Unit)?,
)

/**
 * Assistente de instalação da câmera (ADR-013): à esquerda os cinco passos, à direita o passo atual, com a ação
 * principal sempre à vista. Usado com o carro parado: alvos de 48 dp e a escala do Material 3 (ADR-012, regras
 * 3 e 4). Movimento só gera aviso: quem instala segura o celular, e a calibração já espera a câmera parada.
 */
@Composable
fun TelaInstalacao(estado: EstadoInstalacao, visor: QuadroVisor?, emMovimento: Boolean, acoes: AcoesInstalacao) {
    val a = estado.assistente
    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Passos(a.etapa, acoes.cancelar)
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical))
                .padding(horizontal = Espaco.g, vertical = Espaco.m),
            verticalArrangement = Arrangement.spacedBy(Espaco.m),
        ) {
            if (emMovimento) AvisoMovimento()
            when (a.etapa) {
                Etapa.POSICAO -> EtapaPosicao(acoes.escolherPosicao)
                Etapa.ENQUADRAMENTO, Etapa.CALIBRACAO ->
                    ComImagem(estado, visor, acao = { AcaoCalibracao(a, acoes) }) { EtapaCalibracao(a) }
                Etapa.ZONAS -> ComImagem(estado, visor, acao = { AcaoZonas(a, acoes) }) { EtapaZonas(a) }
                Etapa.RESUMO -> EtapaResumo(a, acoes)
            }
        }
    }
}

private val NOME_DA_ETAPA = mapOf(
    Etapa.POSICAO to "Posição",
    Etapa.ENQUADRAMENTO to "Enquadrar",
    Etapa.CALIBRACAO to "Calibração",
    Etapa.ZONAS to "Lugares",
    Etapa.RESUMO to "Nota",
)

private val LARGURA_PASSOS = 208.dp
private val MARCA_PASSO = 28.dp

/** Trilho com os cinco passos: os feitos com visto, o atual em destaque, os próximos em contorno. */
@Composable
private fun Passos(atual: Etapa, cancelar: (() -> Unit)?) {
    Column(
        Modifier
            .width(LARGURA_PASSOS)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .padding(Espaco.m),
    ) {
        Text("Instalação da câmera", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Passo ${atual.ordinal + 1} de ${Etapa.entries.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Espaco.m))
        Column(verticalArrangement = Arrangement.spacedBy(Espaco.p)) { Etapa.entries.forEach { Passo(it, atual) } }
        Spacer(Modifier.weight(1f))
        cancelar?.let { TextButton(onClick = it, Modifier.heightIn(min = Alvo.parado)) { Text("Cancelar") } }
    }
}

@Composable
private fun Passo(etapa: Etapa, atual: Etapa) {
    val esquema = MaterialTheme.colorScheme
    val agora = etapa == atual
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
        if (etapa < atual) {
            val marca = painterResource(R.drawable.ic_check_circle)
            Icon(marca, "feito", Modifier.size(MARCA_PASSO), tint = esquema.primary)
        } else {
            NumeroDoPasso(etapa, agora)
        }
        Text(
            NOME_DA_ETAPA.getValue(etapa),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (agora) FontWeight.Bold else FontWeight.Medium,
            color = if (agora) esquema.onSurface else esquema.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Número do passo atual (cheio) ou de um próximo (só o contorno). */
@Composable
private fun NumeroDoPasso(etapa: Etapa, agora: Boolean) {
    val esquema = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(MARCA_PASSO)
            .clip(CircleShape)
            .background(if (agora) esquema.primary else Color.Transparent)
            .border(2.dp, if (agora) esquema.primary else esquema.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${etapa.ordinal + 1}",
            style = MaterialTheme.typography.labelLarge,
            color = if (agora) esquema.onPrimary else esquema.onSurfaceVariant,
        )
    }
}

/** Celular se mexendo (na mão ou com o carro andando): avisa, mas não esconde os passos. */
@Composable
private fun AvisoMovimento() {
    val cor = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = Espaco.m, vertical = Espaco.p),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espaco.m),
    ) {
        Icon(painterResource(R.drawable.ic_warning), contentDescription = null, Modifier.size(24.dp), tint = cor)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("O celular está se mexendo. ") }
                append("Se o carro estiver andando, pare antes de instalar; a calibração espera a câmera parada.")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = cor,
        )
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EtapaPosicao(escolher: (PosicaoCamera, LadoMotorista) -> Unit) {
    var posicao by rememberSaveable { mutableStateOf(PosicaoCamera.COLUNA_VOLANTE) }
    var lado by rememberSaveable { mutableStateOf(LadoMotorista.ESQUERDO) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Espaco.g)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Espaco.p)) {
            Titulo("Onde a câmera está presa?")
            DesenhoDoCarro(lado, Modifier.fillMaxWidth().weight(1f), camera = posicao)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espaco.p),
            ) {
                Text("Motorista", style = MaterialTheme.typography.titleSmall)
                LadoMotorista.entries.forEach { l ->
                    val nome = if (l == LadoMotorista.ESQUERDO) "Esquerda" else "Direita"
                    Alternativa(nome, l == lado, Modifier.weight(1f)) { lado = l }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Espaco.p)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Espaco.p),
            ) {
                PosicaoCamera.entries.forEach { p ->
                    val explicacao = TextosInstalacao.explicacaoPosicao(p).takeIf { p == posicao }
                    val nome = TextosInstalacao.posicao(p)
                    Cartao(nome, p == posicao, Modifier.fillMaxWidth(), explicacao) { posicao = p }
                }
            }
            Button(onClick = { escolher(posicao, lado) }, Modifier.fillMaxWidth().heightIn(min = Alvo.parado)) {
                Text("Continuar")
            }
        }
    }
}

/** Opção em cartão: borda e fundo em destaque quando escolhida (e o botão de opção, para não ser só a cor). */
@Composable
private fun Cartao(
    texto: String,
    selecionado: Boolean,
    modifier: Modifier,
    detalhe: String? = null,
    aoEscolher: () -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    Row(
        modifier
            .heightIn(min = Alvo.parado)
            .clip(MaterialTheme.shapes.small)
            .background(if (selecionado) esquema.primaryContainer else esquema.surfaceContainer)
            .border(
                if (selecionado) 2.dp else 1.dp,
                if (selecionado) esquema.primary else esquema.outlineVariant,
                MaterialTheme.shapes.small,
            )
            .selectable(selecionado, onClick = aoEscolher, role = Role.RadioButton)
            .padding(horizontal = Espaco.p),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selecionado, onClick = null)
        val cor = if (selecionado) esquema.onPrimaryContainer else esquema.onSurface
        Column(Modifier.padding(start = Espaco.p, top = Espaco.p, bottom = Espaco.p)) {
            // Uma linha só: em tela estreita a palavra não pode quebrar letra por letra.
            Text(texto, style = MaterialTheme.typography.titleSmall, color = cor, maxLines = 1, softWrap = false)
            detalhe?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = cor) }
        }
    }
}

/** Opção curta, lado a lado (esquerda ou direita): sem a bolinha; a escolhida tem borda grossa e negrito. */
@Composable
private fun Alternativa(texto: String, selecionada: Boolean, modifier: Modifier, aoEscolher: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Box(
        modifier
            .heightIn(min = Alvo.parado)
            .clip(MaterialTheme.shapes.small)
            .background(if (selecionada) esquema.primaryContainer else esquema.surfaceContainer)
            .border(
                if (selecionada) 2.dp else 1.dp,
                if (selecionada) esquema.primary else esquema.outlineVariant,
                MaterialTheme.shapes.small,
            )
            .selectable(selecionada, onClick = aoEscolher, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selecionada) FontWeight.Bold else FontWeight.Normal,
            color = if (selecionada) esquema.onPrimaryContainer else esquema.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun Escolha(titulo: String, explicacao: String?, selecionada: Boolean, aoEscolher: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selecionada, onClick = aoEscolher, role = Role.RadioButton)
            .padding(vertical = Espaco.p),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espaco.m),
    ) {
        RadioButton(selected = selecionada, onClick = null)
        Column {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            explicacao?.let {
                val cor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(it, style = MaterialTheme.typography.bodyMedium, color = cor)
            }
        }
    }
}

/**
 * Imagem ao vivo à esquerda, com o guia oval e a seta para centralizar o rosto. À direita, o passo, que rola,
 * e a [acao] presa embaixo, para o botão nunca sair da tela no celular em paisagem.
 */
@Composable
private fun ComImagem(
    estado: EstadoInstalacao,
    visor: QuadroVisor?,
    acao: @Composable () -> Unit,
    conteudo: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Espaco.g)) {
        Box(
            Modifier
                .weight(PESO_IMAGEM)
                .aspectRatio(4f / 3f)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            visor?.let {
                val imagem = it.imagem.asImageBitmap()
                Image(imagem, "imagem da câmera", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            val desalinhado = estado.assistente.verificacoes
                .any { it.verificacao == Verificacao.ROSTO_INTEIRO && !it.ok }
            GuiaDoRosto(estado.caixa.takeIf { desalinhado })
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Espaco.m)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Espaco.p),
            ) {
                conteudo()
            }
            acao()
        }
    }
}

private const val PESO_IMAGEM = 1.2f

/** Oval onde o rosto deve ficar e, com o rosto fora do lugar, uma seta do rosto até o centro da imagem. */
@Composable
private fun GuiaDoRosto(rostoForaDoLugar: CaixaRosto?) {
    val guia = MaterialTheme.colorScheme.outline
    val seta = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        val largura = size.width * 0.4f
        val altura = size.height * 0.75f
        val canto = Offset((size.width - largura) / 2, (size.height - altura) / 2)
        drawOval(guia, canto, Size(largura, altura), style = Stroke(4f))
        rostoForaDoLugar?.let { c ->
            val de = Offset(((c.x0 + c.x1) / 2).toFloat() * size.width, ((c.y0 + c.y1) / 2).toFloat() * size.height)
            drawLine(seta, de, center, strokeWidth = 8f)
            drawCircle(seta, 14f, center)
        }
    }
}

@Composable
private fun EtapaCalibracao(a: EstadoAssistente) {
    Text(
        "Olhe para a estrada, como ao dirigir",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    if (a.calibracaoFalhou) {
        Text("O rosto sumiu durante a calibração. Tente de novo.", color = MaterialTheme.colorScheme.error)
    }
    // O que falta corrigir vem primeiro: em tela baixa a lista rola e a falha não pode ficar escondida.
    a.verificacoes.sortedBy { it.ok }.forEach { ItemVerificacao(it) }
}

@Composable
private fun AcaoCalibracao(a: EstadoAssistente, acoes: AcoesInstalacao) {
    if (a.etapa == Etapa.CALIBRACAO) {
        Progresso("Fique assim por 10 segundos", a.progresso)
    } else {
        val falta = a.verificacoes.firstOrNull { !it.ok }?.dica
        if (!a.podeCalibrar && falta != null) {
            Text(
                "Para começar: ${TextosInstalacao.dica(falta).replaceFirstChar { it.lowercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(acoes.comecarCalibracao, Modifier.fillMaxWidth().heightIn(min = Alvo.parado), enabled = a.podeCalibrar) {
            Text("Começar a calibração")
        }
    }
}

@Composable
private fun Progresso(texto: String, progresso: Double?) {
    val fracao = (progresso ?: 0.0).toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(Espaco.p)) {
        Text("$texto: ${(fracao * CEM).toInt()}%", style = MaterialTheme.typography.titleSmall)
        LinearProgressIndicator(
            progress = { fracao },
            modifier = Modifier.fillMaxWidth().height(Espaco.p).clip(CircleShape),
        )
    }
}

private const val CEM = 100

/** Verificação com ícone, nome e, quando falha, a dica de correção (regra 5: nunca só a cor). */
@Composable
private fun ItemVerificacao(r: ResultadoVerificacao) {
    val cor = if (r.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(if (r.ok) R.drawable.ic_check_circle else R.drawable.ic_error),
            contentDescription = if (r.ok) "certo" else "corrigir",
            tint = cor,
            modifier = Modifier.size(24.dp).testTag(if (r.ok) "ok_${r.verificacao}" else "falha_${r.verificacao}"),
        )
        Column(Modifier.padding(start = Espaco.m)) {
            Text(TextosInstalacao.verificacao(r.verificacao), style = MaterialTheme.typography.bodyLarge)
            r.dica?.let { Text(TextosInstalacao.dica(it), style = MaterialTheme.typography.bodyMedium, color = cor) }
        }
    }
}

@Composable
private fun EtapaZonas(a: EstadoAssistente) {
    val zona = a.zona ?: return
    Text(
        "Lugar ${zona.ordinal + 1} de ${Zona.entries.size}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Titulo(TextosInstalacao.instrucaoZona(zona))
    // shortcut: o assistente não guarda o lado do motorista; o desenho usa o do Brasil.
    DesenhoDoCarro(LadoMotorista.ESQUERDO, Modifier.fillMaxWidth().height(ALTURA_DESENHO_ZONA), zona = zona)
    Text(
        "Toque em Gravar e olhe para lá. Pule o que o carro não tem.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (a.zonaFalhou) Text("O rosto sumiu. Grave de novo.", color = MaterialTheme.colorScheme.error)
}

private val ALTURA_DESENHO_ZONA = 112.dp

@Composable
private fun AcaoZonas(a: EstadoAssistente, acoes: AcoesInstalacao) {
    if (a.coletandoZona) {
        Progresso("Continue olhando", a.progresso)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
        Button(acoes.gravarZona, Modifier.weight(1f).heightIn(min = Alvo.parado)) { Text("Gravar") }
        OutlinedButton(acoes.pularZona, Modifier.weight(1f).heightIn(min = Alvo.parado)) { Text("Pular") }
    }
}

/** Máximo de cada parte da nota, na ordem de [TextosInstalacao.partesDaNota] (ver `Nota.kt`). */
private val MAXIMO_DAS_PARTES = listOf(60, 25, 15)

@Composable
private fun EtapaResumo(a: EstadoAssistente, acoes: AcoesInstalacao) {
    val nota = a.nota ?: return
    var nome by rememberSaveable { mutableStateOf("Meu carro") }
    val refazer = nota.qualidade == Qualidade.REFAZER
    val cor = if (refazer) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Espaco.gg)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espaco.p),
        ) {
            AnelDaNota(nota.pontos, cor)
            val qualidade = TextosInstalacao.qualidade(nota.qualidade)
            Text(qualidade, style = MaterialTheme.typography.headlineSmall, color = cor)
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Espaco.m)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Espaco.m),
            ) {
                Titulo(if (refazer) "Instalação para refazer" else "Instalação pronta")
                if (refazer) {
                    Text(
                        "A nota ficou abaixo de 60. Ajuste a câmera e refaça a instalação.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    OutlinedTextField(
                        nome, { nome = it }, Modifier.fillMaxWidth(),
                        label = { Text("Nome desta instalação") }, singleLine = true,
                    )
                }
                val partes = listOf(nota.partes.verificacoes, nota.partes.estabilidade, nota.partes.zonas)
                TextosInstalacao.partesDaNota(nota.partes).zip(partes.zip(MAXIMO_DAS_PARTES)).forEach { (texto, p) ->
                    ParteDaNota(texto, p.first.toFloat() / p.second)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
                Button({ acoes.usar(nome) }, Modifier.weight(1f).heightIn(min = Alvo.parado), enabled = !refazer) {
                    Text("Usar esta instalação")
                }
                OutlinedButton(acoes.repetir, Modifier.weight(1f).heightIn(min = Alvo.parado)) {
                    Text("Refazer instalação")
                }
            }
        }
    }
}

@Composable
private fun ParteDaNota(texto: String, fracao: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(Espaco.p)) {
        Text(texto, style = MaterialTheme.typography.bodyLarge)
        LinearProgressIndicator(
            progress = { fracao },
            modifier = Modifier.fillMaxWidth().height(Espaco.p).clip(CircleShape),
        )
    }
}

private val TAMANHO_ANEL = 168.dp

/** Nota de 0 a 100 num anel: o arco cheio é 100. */
@Composable
private fun AnelDaNota(pontos: Int, cor: Color) {
    val trilho = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(Modifier.size(TAMANHO_ANEL), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val traco = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val margem = traco.width / 2
            val area = Size(size.width - traco.width, size.height - traco.width)
            drawArc(trilho, INICIO_ARCO, VOLTA, false, Offset(margem, margem), area, style = traco)
            drawArc(cor, INICIO_ARCO, VOLTA * pontos / CEM, false, Offset(margem, margem), area, style = traco)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$pontos", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text("de 100", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

private const val INICIO_ARCO = -90f
private const val VOLTA = 360f

// ── Desenho do carro ─────────────────────────────────────────────────────────

/**
 * O painel visto do banco do motorista: para-brisa, retrovisor, espelhos, volante, painel de instrumentos e
 * central. Acende onde fica a [camera] (passo 1) ou o lugar para onde olhar (passo 4, [zona]).
 */
@Composable
private fun DesenhoDoCarro(lado: LadoMotorista, modifier: Modifier, camera: PosicaoCamera? = null, zona: Zona? = null) {
    val traco = MaterialTheme.colorScheme.outline
    val destaque = MaterialTheme.colorScheme.primary
    val descricao = "Painel visto do banco do motorista" +
        (camera?.let { ", câmera: " + TextosInstalacao.posicao(it) } ?: "") +
        (zona?.let { ", " + TextosInstalacao.instrucaoZona(it).lowercase() } ?: "")
    Canvas(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .semantics { contentDescription = descricao },
    ) {
        Carro(this, lado == LadoMotorista.ESQUERDO, traco, destaque).desenhar(camera, zona)
    }
}

private const val PROPORCAO_CARRO = 1.8f

/** Coordenadas em fração de uma área de proporção fixa, centrada: o carro não estica com a tela. */
private class Carro(val d: DrawScope, val esquerda: Boolean, val traco: Color, val destaque: Color) {
    private val w = minOf(d.size.width, d.size.height * PROPORCAO_CARRO)
    private val h = w / PROPORCAO_CARRO
    private val x0 = (d.size.width - w) / 2
    private val y0 = (d.size.height - h) / 2
    private val fino = Stroke(width = with(d) { 2.dp.toPx() }, cap = StrokeCap.Round)
    private val grosso = Stroke(width = with(d) { 6.dp.toPx() }, cap = StrokeCap.Round)
    private val volanteX = if (esquerda) 0.3f else 0.7f
    private fun p(x: Float, y: Float) = Offset(x0 + x * w, y0 + y * h)
    private fun lado(x: Float) = if (esquerda) x else 1f - x

    fun desenhar(camera: PosicaoCamera?, zona: Zona?) {
        val vidro = Path().apply {
            listOf(p(0.18f, 0.1f), p(0.82f, 0.1f), p(0.9f, 0.52f), p(0.1f, 0.52f)).forEachIndexed { i, o ->
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
            close()
        }
        d.drawPath(vidro, traco, style = fino)
        if (camera == PosicaoCamera.PILAR_A) {
            d.drawLine(destaque, p(lado(0.18f), 0.1f), p(lado(0.1f), 0.52f), grosso.width)
        }
        peca(0.42f, 0.14f, 0.16f, 0.08f, zona == Zona.RETROVISOR)
        peca(0.02f, 0.3f, 0.06f, 0.16f, zona == Zona.ESPELHO_ESQUERDO)
        peca(0.92f, 0.3f, 0.06f, 0.16f, zona == Zona.ESPELHO_DIREITO)
        d.drawLine(traco, p(0.02f, 0.6f), p(0.98f, 0.6f), fino.width)
        peca(volanteX - 0.1f, 0.62f, 0.2f, 0.12f, zona == Zona.PAINEL)
        peca(0.44f, 0.66f, 0.12f, 0.2f, false)
        d.drawCircle(traco, 0.24f * h, p(volanteX, 0.98f), style = Stroke(with(d) { 5.dp.toPx() }))
        camera?.let { marcarCamera(it) }
    }

    /** Peça do painel (retrovisor, espelho, instrumentos, central): acesa quando é o lugar do passo. */
    private fun peca(x: Float, y: Float, largura: Float, altura: Float, acesa: Boolean) {
        val canto = CornerRadius(with(d) { 6.dp.toPx() })
        val inicio = p(x, y)
        val tamanho = Size(largura * w, altura * h)
        if (acesa) d.drawRoundRect(destaque.copy(alpha = 0.35f), inicio, tamanho, canto, style = Fill)
        d.drawRoundRect(if (acesa) destaque else traco, inicio, tamanho, canto, style = if (acesa) grosso else fino)
    }

    private fun marcarCamera(c: PosicaoCamera) {
        val centro = when (c) {
            PosicaoCamera.COLUNA_VOLANTE -> p(volanteX, 0.8f)
            PosicaoCamera.CENTRO_PAINEL -> p(0.5f, 0.76f)
            PosicaoCamera.PILAR_A -> p(lado(0.14f), 0.31f)
            PosicaoCamera.RETROVISOR -> p(0.5f, 0.3f)
        }
        val raio = with(d) { 10.dp.toPx() }
        d.drawCircle(destaque.copy(alpha = 0.3f), raio * 2, centro)
        d.drawCircle(destaque, raio, centro)
    }
}

/** Perfis em Ajustes: escolher, renomear, apagar, nova instalação e backup em arquivo (ADR-013). */
class AcoesPerfis(
    val escolher: (String) -> Unit,
    val renomear: (id: String, nome: String) -> Unit,
    val apagar: (String) -> Unit,
    val novaInstalacao: () -> Unit,
    val exportar: () -> Unit,
    val importar: (uri: String) -> Unit,
)

@Composable
fun SecaoInstalacao(perfis: List<PerfilInstalacao>, ativo: String?, acoes: AcoesPerfis) {
    var renomeando by remember { mutableStateOf<PerfilInstalacao?>(null) }
    var apagando by remember { mutableStateOf<PerfilInstalacao?>(null) }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { acoes.importar(it.toString()) }
    }
    Text(
        "Cada instalação guarda onde a câmera está e para onde o motorista olha no carro.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (perfis.isEmpty()) Text("Nenhuma instalação gravada.")
    perfis.forEach { p ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                val detalhe = "${TextosInstalacao.posicao(p.posicao)} · nota ${p.nota} · ${dataTexto(p.criadoEpochMs)}"
                Escolha(p.nome, detalhe, p.id == ativo) {
                    acoes.escolher(p.id)
                }
            }
            TextButton({ renomeando = p }, Modifier.heightIn(min = Alvo.parado)) { Text("Renomear") }
            TextButton({ apagando = p }, Modifier.heightIn(min = Alvo.parado)) { Text("Apagar") }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
        OutlinedButton(acoes.novaInstalacao, Modifier.heightIn(min = Alvo.parado)) { Text("Nova instalação") }
        OutlinedButton(acoes.exportar, Modifier.heightIn(min = Alvo.parado)) { Text("Exportar perfis") }
        OutlinedButton({ importar.launch(arrayOf("text/plain")) }, Modifier.heightIn(min = Alvo.parado)) {
            Text("Importar perfis")
        }
    }
    renomeando?.let { p -> DialogoRenomear(p, { renomeando = null }) { nome -> acoes.renomear(p.id, nome) } }
    apagando?.let { p ->
        AlertDialog(
            onDismissRequest = { apagando = null },
            title = { Text("Apagar \"${p.nome}\"?") },
            text = { Text("A instalação some deste aparelho. Uma cópia exportada continua no arquivo.") },
            confirmButton = { TextButton({ apagando = null; acoes.apagar(p.id) }) { Text("Apagar") } },
            dismissButton = { TextButton({ apagando = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DialogoRenomear(p: PerfilInstalacao, fechar: () -> Unit, renomear: (String) -> Unit) {
    var nome by remember(p.id) { mutableStateOf(p.nome) }
    AlertDialog(
        onDismissRequest = fechar,
        title = { Text("Renomear instalação") },
        text = { OutlinedTextField(nome, { nome = it }, singleLine = true) },
        confirmButton = { TextButton({ fechar(); renomear(nome) }) { Text("Salvar") } },
        dismissButton = { TextButton(fechar) { Text("Cancelar") } },
    )
}
