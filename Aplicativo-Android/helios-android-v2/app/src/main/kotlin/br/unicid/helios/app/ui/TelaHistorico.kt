package br.unicid.helios.app.ui

import androidx.compose.foundation.Canvas
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextButton
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.unicid.helios.app.EventoDetalhe
import br.unicid.helios.app.adaptadores.fotos.GravadorArquivos
import br.unicid.helios.dominio.tipos.TipoEvento
import br.unicid.helios.protocolos.FotoRegistrada
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.unicid.helios.app.DetalheViagem
import br.unicid.helios.app.portas.SessaoGravada
import br.unicid.helios.protocolos.Paleta
import kotlin.math.roundToInt

private const val ESCALA_KSS = 9
private const val PERCLOS_FADIGA = 0.15f

/**
 * Histórico de viagens (RF08, RF09): só abre com o carro parado. Lista as viagens; ao tocar numa,
 * mostra o resumo, o PERCLOS ao longo da viagem, a nota KSS e a exportação CSV.
 */
@Composable
fun TelaHistorico(
    viagens: List<SessaoGravada>,
    detalhe: DetalheViagem?,
    pastaEscolhida: String?,
    aoAbrir: (Long?) -> Unit,
    aoDefinirKss: (Long, Int?) -> Unit,
    aoExportar: (Long?, String?) -> Unit,
    carregarMiniatura: suspend (String) -> Bitmap?,
    carregarFoto: suspend (String) -> Bitmap?,
    aoVoltar: () -> Unit,
) {
    var pedido by remember { mutableStateOf<Long?>(null) }
    val escolher = rememberSeletorDePasta { pasta -> aoExportar(pedido, pasta) }
    val exportar = { sessao: Long? -> aoExportar(sessao, pastaEscolhida) }
    val exportarOutra = { sessao: Long? ->
        pedido = sessao
        escolher()
    }
    var fotoAberta by remember { mutableStateOf<FotoRegistrada?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (detalhe == null) "Viagens" else "Resumo da viagem", style = MaterialTheme.typography.headlineLarge)
        }
        if (detalhe == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = aoVoltar) { Text("Voltar") }
                OutlinedButton(onClick = { exportar(null) }, enabled = viagens.isNotEmpty()) {
                    Text("Exportar todas (CSV)")
                }
                TextButton(onClick = { exportarOutra(null) }, enabled = viagens.isNotEmpty()) { Text("Exportar para outra pasta") }
            }
            Text(ondeVaiOCsv(pastaEscolhida), color = MaterialTheme.colorScheme.onSurfaceVariant)
            ListaViagens(viagens, aoAbrir)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { aoAbrir(null) }) { Text("Todas as viagens") }
                OutlinedButton(onClick = { exportar(detalhe.sessao.id) }) { Text("Exportar esta viagem (CSV)") }
                TextButton(onClick = { exportarOutra(detalhe.sessao.id) }) { Text("Outra pasta") }
                OutlinedButton(onClick = aoVoltar) { Text("Voltar à direção") }
            }
            Detalhe(detalhe, aoDefinirKss, carregarMiniatura, aoAbrirFoto = { fotoAberta = it })
        }
    }
    fotoAberta?.let { foto -> FotoCheia(foto, carregarFoto, aoFechar = { fotoAberta = null }) }
}

private fun ondeVaiOCsv(pasta: String?) =
    if (pasta == null) "Os CSV vão para a memória interna, na pasta ${GravadorArquivos.PASTA_CSV}." else "Os CSV vão para a pasta escolhida nos ajustes."

private val NOME_EVENTO = mapOf(
    TipoEvento.MICROSSONO to "Microssono",
    TipoEvento.SONO to "Sono (olhos fechados 3 s ou mais)",
    TipoEvento.BOCEJO to "Bocejo",
    TipoEvento.CABECA_CAIDA to "Cabeça caída",
    TipoEvento.ROSTO_AUSENTE to "Rosto fora da câmera",
    TipoEvento.CABECA_VIRADA to "Cabeça virada",
    TipoEvento.OLHAR_DESVIADO to "Olhar fora da via",
)

private const val PROPORCAO_MINIATURA = 4f / 3f

@Composable
private fun ListaViagens(viagens: List<SessaoGravada>, aoAbrir: (Long?) -> Unit) {
    if (viagens.isEmpty()) {
        Text("Nenhuma viagem gravada ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(viagens, key = { it.id }) { v ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { aoAbrir(v.id) }
                    .padding(16.dp),
            ) {
                Text("${data(v.inicioEpochMs)} · ${duracao(v.duracaoMs)}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${v.microssonos} microssonos · ${v.bocejos} bocejos · " +
                        "${v.desatencoes + v.distracoes} distrações · " +
                        "${duracao(v.tempoEmAlertaMs)} em alerta · PERCLOS médio ${pct(v.perclosMedio)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Detalhe(
    d: DetalheViagem,
    aoDefinirKss: (Long, Int?) -> Unit,
    carregarMiniatura: suspend (String) -> Bitmap?,
    aoAbrirFoto: (FotoRegistrada) -> Unit,
) {
    val v = d.sessao
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val titulo = "${data(v.inicioEpochMs)} · ${duracao(v.duracaoMs)} de monitoramento"
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        val fracaoAlerta = v.tempoEmAlertaMs.toDouble() / v.duracaoMs.coerceAtLeast(1)
        Linha("Tempo em alerta", "${duracao(v.tempoEmAlertaMs)} (${pct(fracaoAlerta)})")
        Linha("Microssonos", v.microssonos.toString())
        Linha("Bocejos", v.bocejos.toString())
        Linha("Piscadas", v.piscadas.toString())
        Linha("Rosto fora da câmera / cabeça virada", "${v.desatencoes} / ${v.distracoes}")
        Linha("PERCLOS médio / máximo (janelas de 30 s)", "${pct(v.perclosMedio)} / ${pct(v.perclosMaximo)}")
        Linha("Fotos de alerta guardadas", d.fotos.toString())
        if (d.janelas.isNotEmpty()) {
            Linha("Olhar fora da via (média das janelas)", pct(d.janelas.map { it.fracaoOlharFora }.average()))
            Linha("Olhos semicerrados (média das janelas)", pct(d.janelas.map { it.fracaoSemicerrado }.average()))
            Linha("Piscadas por minuto (média)", d.janelas.map { it.piscadasPorMinuto }.average().roundToInt().toString())
        }

        Text("PERCLOS ao longo da viagem", style = MaterialTheme.typography.titleMedium)
        GraficoPerclos(d.janelas.map { it.perclosLongo.toFloat() })

        Text(
            "Como o motorista se sentia (KSS: 1 muito alerta, 9 lutando contra o sono)",
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (nota in 1..ESCALA_KSS) {
                FilterChip(
                    selected = v.kss == nota,
                    onClick = { aoDefinirKss(v.id, if (v.kss == nota) null else nota) },
                    label = { Text(nota.toString()) },
                )
            }
        }

        Text("Eventos da viagem", style = MaterialTheme.typography.titleMedium)
        if (d.eventos.isEmpty()) {
            Text("Nenhum evento além de piscadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        d.eventos.forEach { e -> LinhaEvento(e, carregarMiniatura, aoAbrirFoto) }
        if (d.fotosSemEvento.isNotEmpty()) {
            Text("Outras fotos (fadiga acumulada e fotos de teste)", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                d.fotosSemEvento.forEach { f ->
                    Column {
                        Miniatura(f, carregarMiniatura, aoAbrirFoto)
                        Text(horaTexto(f.capturaEpochMs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaEvento(e: EventoDetalhe, carregarMiniatura: suspend (String) -> Bitmap?, aoAbrirFoto: (FotoRegistrada) -> Unit) {
    val ev = e.evento
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (e.foto != null) Miniatura(e.foto, carregarMiniatura, aoAbrirFoto) else Spacer(Modifier.width(LARGURA_MINIATURA))
        Column(Modifier.weight(1f)) {
            Text(NOME_EVENTO[ev.tipo] ?: ev.tipo.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${horaTexto(ev.inicioEpochMs)} · durou ${"%.1f".format(ev.duracaoMs / 1_000.0)} s · estado ${ev.estado.name.lowercase()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val LARGURA_MINIATURA = 160.dp

/** Miniatura carregada fora da thread de tela. Se o arquivo sumiu, mostra um aviso no lugar. */
@Composable
private fun Miniatura(foto: FotoRegistrada, carregar: suspend (String) -> Bitmap?, aoAbrir: (FotoRegistrada) -> Unit) {
    val imagem by produceState<Bitmap?>(null, foto.uri) { value = carregar(foto.uri) }
    Box(
        Modifier
            .width(LARGURA_MINIATURA)
            .aspectRatio(PROPORCAO_MINIATURA)
            .background(MaterialTheme.colorScheme.background)
            .clickable { aoAbrir(foto) },
        contentAlignment = Alignment.Center,
    ) {
        val bmp = imagem
        if (bmp != null) {
            Image(bmp.asImageBitmap(), contentDescription = "foto do alerta", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text("foto", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Foto em tela cheia (a anotada mostra a malha e os números daquele instante). Toque para fechar. */
@Composable
private fun FotoCheia(foto: FotoRegistrada, carregar: suspend (String) -> Bitmap?, aoFechar: () -> Unit) {
    val imagem by produceState<Bitmap?>(null, foto.uri) { value = carregar(foto.uri) }
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Asfalto).clickable(onClick = aoFechar), contentAlignment = Alignment.Center) {
            val bmp = imagem
            if (bmp != null) {
                Image(bmp.asImageBitmap(), contentDescription = foto.nome, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            } else {
                Text("Abrindo a foto… (se não abrir, o arquivo foi apagado ou movido)", color = Farol)
            }
            Text(foto.nome, color = Farol, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(rotulo, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}

/** Linha do PERCLOS por janela de 30 s, com o limiar de fadiga (15%) tracejado. Desenhado à mão, sem biblioteca. */
@Composable
private fun GraficoPerclos(valores: List<Float>) {
    if (valores.size < 2) {
        Text(
            "Viagem curta demais para o gráfico (menos de duas janelas de 30 s).",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val maximo = (valores.max().coerceAtLeast(PERCLOS_FADIGA) * 1.1f)
    val linha = cor(Paleta.AMBAR)
    val grade = cor(Paleta.NEVOA)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surface)
            .semantics { contentDescription = "PERCLOS por janela, máximo de ${pct(valores.max().toDouble())}" },
    ) {
        val y = { v: Float -> size.height * (1f - v / maximo) }
        val x = { i: Int -> size.width * i / (valores.size - 1) }
        drawLine(
            grade,
            Offset(0f, y(PERCLOS_FADIGA)),
            Offset(size.width, y(PERCLOS_FADIGA)),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
        )
        val caminho = Path().apply {
            moveTo(x(0), y(valores[0]))
            valores.forEachIndexed { i, v -> if (i > 0) lineTo(x(i), y(v)) }
        }
        drawPath(caminho, linha, style = Stroke(width = 5f))
    }
    Text("tracejado: limiar de fadiga (15%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun data(epochMs: Long) = dataTexto(epochMs)

private fun duracao(ms: Long) = duracaoTexto(ms)

private fun pct(fracao: Double) = pctTexto(fracao)
