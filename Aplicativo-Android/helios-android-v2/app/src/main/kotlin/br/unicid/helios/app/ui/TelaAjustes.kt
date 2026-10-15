package br.unicid.helios.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import br.unicid.helios.app.Ajustes
import br.unicid.helios.app.AjustesFotos
import br.unicid.helios.app.EstadoFotos
import br.unicid.helios.app.TipoFonte
import br.unicid.helios.app.adaptadores.fotos.GravadorArquivos
import br.unicid.helios.protocolos.Retencao

/** Ajustes, abertos só com o carro parado. Alvos de toque grandes para a central multimídia. */
@Composable
fun TelaAjustes(
    inicial: Ajustes,
    estadoFotos: EstadoFotos,
    aoSalvar: (Ajustes) -> Unit,
    aoMudarFotos: (AjustesFotos) -> Unit,
    aoFotoDeTeste: () -> Unit,
    aoCalibrar: () -> Unit,
    aoLimparTudo: () -> Unit,
    aoVoltar: () -> Unit,
) {
    var a by remember { mutableStateOf(inicial) }
    var confirmarLimpeza by remember { mutableStateOf(false) }
    // As fotos valem na hora (sem o "Salvar"): a autorização não se perde se o usuário sair pelo voltar.
    val mudarFotos = { f: AjustesFotos ->
        a = a.copy(fotos = f)
        aoMudarFotos(f)
    }
    val escolherPasta = rememberSeletorDePasta { uri -> mudarFotos(a.fotos.copy(pasta = uri)) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineLarge)

        Secao("De onde vem a imagem")
        Opcao("Câmera do aparelho", a.fonte == TipoFonte.CAMERA_DO_APARELHO) { a = a.copy(fonte = TipoFonte.CAMERA_DO_APARELHO) }
        Opcao("ESP32-CAM pelo Wi-Fi", a.fonte == TipoFonte.ESP32_CAM) { a = a.copy(fonte = TipoFonte.ESP32_CAM) }

        if (a.fonte == TipoFonte.ESP32_CAM) {
            Campo("Rede da câmera", a.esp32.ssid) { a = a.copy(esp32 = a.esp32.copy(ssid = it)) }
            Campo("Senha da rede", a.esp32.senha, senha = true) { a = a.copy(esp32 = a.esp32.copy(senha = it)) }
            Campo("Endereço do vídeo", a.esp32.urlStream) { a = a.copy(esp32 = a.esp32.copy(urlStream = it)) }
            Interruptor("Tocar também o buzzer da câmera", a.buzzerNaCamera) { a = a.copy(buzzerNaCamera = it) }
        }

        Secao("Imagem")
        Interruptor("Realçar imagem escura (OpenCV)", a.realceOpenCv) { a = a.copy(realceOpenCv = it) }

        Secao("Alertas")
        Interruptor("Falar o alerta em voz alta, além do bipe", a.alertaPorVoz) { a = a.copy(alertaPorVoz = it) }

        Secao("Fotos dos alertas")
        SecaoFotos(a.fotos, estadoFotos, aoMudar = mudarFotos, aoEscolherPasta = escolherPasta, aoFotoDeTeste = aoFotoDeTeste)

        Secao("Análise")
        Interruptor("Mostrar a câmera com a malha do rosto e as métricas", a.mostrarVisor) { a = a.copy(mostrarVisor = it) }

        Secao("Desempenho")
        Interruptor("Reaproveitar bitmaps da câmera (menos memória)", a.reaproveitarBitmaps) {
            a = a.copy(reaproveitarBitmaps = it)
        }
        Interruptor("Mostrar diagnóstico na tela (fps, memória, latência)", a.mostrarDiagnostico) {
            a = a.copy(mostrarDiagnostico = it)
        }

        Secao("Dados guardados")
        OutlinedButton(onClick = { confirmarLimpeza = true }) { Text("Apagar todas as fotos e o histórico") }

        Secao("Motorista")
        Text(
            a.calibracao?.let { "Calibrado: olho aberto em %.2f".format(it.earReferencia) }
                ?: "Ainda não calibrado. Usando o limiar padrão.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { aoSalvar(a); aoCalibrar(); aoVoltar() }) { Text("Calibrar agora (5 segundos)") }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { aoSalvar(a); aoVoltar() }) { Text("Salvar") }
            OutlinedButton(onClick = aoVoltar) { Text("Cancelar") }
        }
    }
    if (confirmarLimpeza) {
        AlertDialog(
            onDismissRequest = { confirmarLimpeza = false },
            title = { Text("Apagar tudo?") },
            text = {
                Text("Apaga as fotos dos alertas guardadas pelo app e todo o histórico de viagens. Não dá para desfazer.")
            },
            confirmButton = { TextButton(onClick = { confirmarLimpeza = false; aoLimparTudo() }) { Text("Apagar") } },
            dismissButton = { TextButton(onClick = { confirmarLimpeza = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SecaoFotos(
    f: AjustesFotos,
    estado: EstadoFotos,
    aoMudar: (AjustesFotos) -> Unit,
    aoEscolherPasta: () -> Unit,
    aoFotoDeTeste: () -> Unit,
) {
    Text(
        "Ao entrar num alerta, o app guarda duas fotos do motorista: a original e outra com a análise desenhada " +
            "(no máximo uma captura a cada 30 s). Nessas pastas a galeria e o backup do celular também enxergam as fotos.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Interruptor("Autorizo guardar fotos dos alertas", f.consentimento) { aoMudar(f.copy(consentimento = it)) }
    if (!f.consentimento) {
        Text("Fotos desligadas: nenhuma foto é gravada.", color = MaterialTheme.colorScheme.error)
        return
    }
    val destino = if (f.pasta == null) {
        "Fotos: memória interna › ${GravadorArquivos.PASTA_FOTOS.replace("/", " › ")}\n" +
            "CSV: memória interna › ${GravadorArquivos.PASTA_CSV.replace("/", " › ")}"
    } else {
        "Fotos e CSV: pasta escolhida (" + Uri.decode(f.pasta).substringAfterLast(':').ifEmpty { "raiz" } + ")"
    }
    Text(destino, style = MaterialTheme.typography.titleMedium)
    Text(textoEstadoFotos(estado), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button(onClick = aoFotoDeTeste) { Text("Gravar foto de teste agora") }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(onClick = aoEscolherPasta) { Text("Usar outra pasta") }
        if (f.pasta != null) {
            OutlinedButton(onClick = { aoMudar(f.copy(pasta = null)) }) { Text("Voltar para a pasta HELIOS") }
        }
    }
    var maximo by remember { mutableStateOf(f.maxFotos.toString()) }
    Campo("Máximo de fotos guardadas (1 a $MAXIMO_FOTOS)", maximo, numerico = true) { texto ->
        maximo = texto.filter(Char::isDigit).take(DIGITOS_MAXIMO)
        maximo.toIntOrNull()?.takeIf { it in 1..MAXIMO_FOTOS }?.let { aoMudar(f.copy(maxFotos = it)) }
    }
    Text("Apagar fotos antigas", style = MaterialTheme.typography.titleMedium)
    val opcoes = listOf(
        Retencao.DUAS_HORAS to "Depois de 2 horas de uso",
        Retencao.TRES_HORAS to "Depois de 3 horas de uso",
        Retencao.AO_ENCERRAR to "Ao encerrar cada viagem",
        Retencao.SO_LIMITE to "Só pelo limite de quantidade",
    )
    opcoes.forEach { (r, texto) -> Opcao(texto, f.retencao == r) { aoMudar(f.copy(retencao = r)) } }
    if (f.retencao == Retencao.AO_ENCERRAR) {
        Text(
            "Atenção: com esta opção as fotos somem ao encerrar a viagem e não ficam para analisar depois.",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/**
 * Abre o seletor de pastas do Android e guarda o acesso de forma permanente
 * (sobrevive a reinícios), sem pedir permissão de armazenamento.
 */
@Composable
fun rememberSeletorDePasta(aoEscolher: (String) -> Unit): () -> Unit {
    val contexto = LocalContext.current
    val lancador = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                contexto.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // Alguns provedores não dão acesso permanente: a pasta vale até o app fechar.
            }
            aoEscolher(uri.toString())
        }
    }
    return { lancador.launch(null) }
}

private const val MAXIMO_FOTOS = 10_000
private const val DIGITOS_MAXIMO = 5

@Composable
private fun Secao(titulo: String) {
    Text(titulo, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Opcao(texto: String, selecionada: Boolean, aoEscolher: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selecionada, onClick = aoEscolher, role = Role.RadioButton).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selecionada, onClick = null)
        Spacer(Modifier.padding(start = 12.dp))
        Text(texto, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Interruptor(texto: String, ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = ligado, onCheckedChange = aoMudar)
    }
}

@Composable
private fun Campo(
    rotulo: String,
    valor: String,
    senha: Boolean = false,
    numerico: Boolean = false,
    aoMudar: (String) -> Unit,
) {
    val teclado = when {
        senha -> KeyboardType.Password
        numerico -> KeyboardType.Number
        else -> KeyboardType.Uri
    }
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        visualTransformation = if (senha) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** "Fotos ligadas: 4 gravadas em Pictures/HELIOS, a última helios-critico-...jpg". */
internal fun textoEstadoFotos(e: EstadoFotos): String {
    if (!e.ligadas) return "Fotos desligadas"
    val ultima = e.ultima?.let { ", a última $it" }.orEmpty()
    return "Fotos ligadas: ${e.gravadas} guardadas em ${e.onde}$ultima"
}
