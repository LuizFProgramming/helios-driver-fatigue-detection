package br.unicid.helios.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
 * Assistente de instalação da câmera (ADR-013): posição, enquadramento ao vivo, calibração de 10 s, zonas do
 * carro e resumo com a nota. Usado só com o carro parado: alvos de 48 dp e a escala do Material 3
 * (ADR-012, regras 3 e 4).
 */
@Composable
fun TelaInstalacao(estado: EstadoInstalacao, visor: QuadroVisor?, emMovimento: Boolean, acoes: AcoesInstalacao) {
    val a = estado.assistente
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Espaco.gg, vertical = Espaco.m),
        verticalArrangement = Arrangement.spacedBy(Espaco.m),
    ) {
        Cabecalho(a.etapa, acoes.cancelar)
        if (emMovimento) {
            Text("Pare o carro para instalar a câmera.", style = MaterialTheme.typography.headlineSmall)
        } else {
            when (a.etapa) {
                Etapa.POSICAO -> EtapaPosicao(acoes.escolherPosicao)
                Etapa.ENQUADRAMENTO, Etapa.CALIBRACAO -> ComImagem(estado, visor) { EtapaCalibracao(a, acoes) }
                Etapa.ZONAS -> ComImagem(estado, visor) { EtapaZonas(a, acoes) }
                Etapa.RESUMO -> EtapaResumo(a, acoes)
            }
        }
    }
}

@Composable
private fun Cabecalho(etapa: Etapa, cancelar: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Instalação da câmera", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Passo ${etapa.ordinal + 1} de ${Etapa.entries.size}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        cancelar?.let { TextButton(onClick = it, Modifier.heightIn(min = Alvo.parado)) { Text("Cancelar") } }
    }
}

@Composable
private fun EtapaPosicao(escolher: (PosicaoCamera, LadoMotorista) -> Unit) {
    var posicao by rememberSaveable { mutableStateOf(PosicaoCamera.COLUNA_VOLANTE) }
    var lado by rememberSaveable { mutableStateOf(LadoMotorista.ESQUERDO) }
    Row(horizontalArrangement = Arrangement.spacedBy(Espaco.g)) {
        Column(Modifier.weight(2f).verticalScroll(rememberScrollState())) {
            Text("Onde a câmera está presa?", style = MaterialTheme.typography.titleLarge)
            PosicaoCamera.entries.forEach { p ->
                val explicacao = TextosInstalacao.explicacaoPosicao(p)
                Escolha(TextosInstalacao.posicao(p), explicacao, p == posicao) { posicao = p }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espaco.p)) {
            Text("Lado do motorista", style = MaterialTheme.typography.titleLarge)
            Escolha("Esquerdo", "Como no Brasil", lado == LadoMotorista.ESQUERDO) { lado = LadoMotorista.ESQUERDO }
            Escolha("Direito", null, lado == LadoMotorista.DIREITO) { lado = LadoMotorista.DIREITO }
            Button(onClick = { escolher(posicao, lado) }, Modifier.fillMaxWidth().heightIn(min = Alvo.parado)) {
                Text("Continuar")
            }
        }
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

/** Imagem ao vivo à esquerda, com o guia oval e a seta para centralizar o rosto; a etapa à direita. */
@Composable
private fun ComImagem(estado: EstadoInstalacao, visor: QuadroVisor?, etapa: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Espaco.g)) {
        Box(
            Modifier
                .weight(1f)
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
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Espaco.p),
        ) {
            etapa()
        }
    }
}

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
private fun EtapaCalibracao(a: EstadoAssistente, acoes: AcoesInstalacao) {
    Text("Olhe para a estrada como se estivesse dirigindo.", style = MaterialTheme.typography.titleMedium)
    if (a.etapa == Etapa.CALIBRACAO) {
        Text("Calibrando: fique assim por 10 segundos.", style = MaterialTheme.typography.bodyLarge)
        LinearProgressIndicator(progress = { (a.progresso ?: 0.0).toFloat() }, modifier = Modifier.fillMaxWidth())
    } else {
        // O botão vem antes da lista, para ficar à vista no celular em paisagem.
        Button(acoes.comecarCalibracao, Modifier.fillMaxWidth().heightIn(min = Alvo.parado), enabled = a.podeCalibrar) {
            Text("Começar a calibração")
        }
    }
    if (a.calibracaoFalhou) {
        Text("O rosto sumiu durante a calibração. Tente de novo.", color = MaterialTheme.colorScheme.error)
    }
    a.verificacoes.forEach { ItemVerificacao(it) }
}

/** Verificação com ícone, nome e, quando falha, a dica de correção (regra 5: nunca só a cor). */
@Composable
private fun ItemVerificacao(r: ResultadoVerificacao) {
    val cor = if (r.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(if (r.ok) R.drawable.ic_check_circle else R.drawable.ic_error),
            contentDescription = if (r.ok) "certo" else "corrigir",
            tint = cor,
            modifier = Modifier.size(28.dp).testTag(if (r.ok) "ok_${r.verificacao}" else "falha_${r.verificacao}"),
        )
        Column(Modifier.padding(start = Espaco.m)) {
            Text(TextosInstalacao.verificacao(r.verificacao), style = MaterialTheme.typography.titleMedium)
            r.dica?.let { Text(TextosInstalacao.dica(it), style = MaterialTheme.typography.bodyMedium, color = cor) }
        }
    }
}

@Composable
private fun EtapaZonas(a: EstadoAssistente, acoes: AcoesInstalacao) {
    val zona = a.zona ?: return
    Text("Lugar ${zona.ordinal + 1} de ${Zona.entries.size}", style = MaterialTheme.typography.bodyLarge)
    Text(TextosInstalacao.instrucaoZona(zona), style = MaterialTheme.typography.headlineSmall)
    Text(
        "Toque em Gravar e olhe para lá até a barra encher. Pule os que o carro não tem.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (a.coletandoZona) {
        LinearProgressIndicator(progress = { (a.progresso ?: 0.0).toFloat() }, modifier = Modifier.fillMaxWidth())
    }
    if (a.zonaFalhou) Text("O rosto sumiu. Grave de novo.", color = MaterialTheme.colorScheme.error)
    Row(horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
        Button(acoes.gravarZona, Modifier.heightIn(min = Alvo.parado), enabled = !a.coletandoZona) { Text("Gravar") }
        OutlinedButton(acoes.pularZona, Modifier.heightIn(min = Alvo.parado), enabled = !a.coletandoZona) {
            Text("Pular")
        }
    }
}

@Composable
private fun EtapaResumo(a: EstadoAssistente, acoes: AcoesInstalacao) {
    val nota = a.nota ?: return
    var nome by rememberSaveable { mutableStateOf("Meu carro") }
    val refazer = nota.qualidade == Qualidade.REFAZER
    Row(horizontalArrangement = Arrangement.spacedBy(Espaco.gg), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("${nota.pontos}", style = MaterialTheme.typography.displayLarge)
            Text(
                TextosInstalacao.qualidade(nota.qualidade),
                style = MaterialTheme.typography.headlineSmall,
                color = if (refazer) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espaco.p)) {
            TextosInstalacao.partesDaNota(nota.partes).forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
            if (refazer) {
                Text(
                    "A nota ficou abaixo de 60. Ajuste a câmera e refaça a instalação.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                OutlinedTextField(nome, { nome = it }, label = { Text("Nome desta instalação") }, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
                Button({ acoes.usar(nome) }, Modifier.heightIn(min = Alvo.parado), enabled = !refazer) {
                    Text("Usar esta instalação")
                }
                OutlinedButton(acoes.repetir, Modifier.heightIn(min = Alvo.parado)) { Text("Refazer instalação") }
            }
        }
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
