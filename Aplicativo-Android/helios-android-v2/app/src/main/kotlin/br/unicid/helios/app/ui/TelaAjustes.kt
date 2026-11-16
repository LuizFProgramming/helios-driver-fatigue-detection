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
import br.unicid.helios.dominio.Sensibilidade
import br.unicid.helios.dominio.instalacao.PerfilInstalacao
import br.unicid.helios.protocolos.RegistroQuadros
import br.unicid.helios.protocolos.Retencao

/** Ajustes, abertos só com o carro parado. Alvos de toque grandes para a central multimídia. */
@Composable
fun TelaAjustes(
    inicial: Ajustes,
    estadoFotos: EstadoFotos,
    aoSalvar: (Ajustes) -> Unit,
    aoMudarFotos: (AjustesFotos) -> Unit,
    aoFotoDeTeste: () -> Unit,
    perfis: List<PerfilInstalacao>,
    perfilAtivo: String?,
    acoesPerfis: AcoesPerfis,
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
            Campo("Endereço do buzzer", a.esp32.urlBuzzer) { a = a.copy(esp32 = a.esp32.copy(urlBuzzer = it)) }
            Interruptor("Tocar também o buzzer da câmera", a.buzzerNaCamera) { a = a.copy(buzzerNaCamera = it) }
        }

        Secao("Imagem")
        Interruptor("Realçar imagem escura (OpenCV)", a.realceOpenCv) { a = a.copy(realceOpenCv = it) }

        SecaoAlertas(a) { a = it }

        Secao("Fotos dos alertas")
        SecaoFotos(a.fotos, estadoFotos, aoMudar = mudarFotos, aoEscolherPasta = escolherPasta, aoFotoDeTeste = aoFotoDeTeste)

        Secao("Tela de direção")
        ESCALAS_TEXTO.forEach { (nome, escala) ->
            Opcao("Texto $nome", a.escalaTexto == escala) { a = a.copy(escalaTexto = escala) }
        }
        Interruptor("Tema claro (para o dia)", a.temaClaro) { a = a.copy(temaClaro = it) }
        Interruptor("Botão Menu à esquerda", a.menuNaEsquerda) { a = a.copy(menuNaEsquerda = it) }

        Secao("Análise")
        Interruptor("Mostrar a câmera com a malha do rosto e as métricas", a.mostrarVisor) { a = a.copy(mostrarVisor = it) }
        Interruptor("Rede neural do olho (experimental)", a.cnnOlho) { a = a.copy(cnnOlho = it) }
        Text("Só registra e mostra; não muda o alarme", color = MaterialTheme.colorScheme.onSurfaceVariant)
        SecaoQuadros(a.registroQuadros, a.fotos.consentimento) { a = a.copy(registroQuadros = it) }

        Secao("Desempenho")
        Interruptor("Reaproveitar bitmaps da câmera (menos memória)", a.reaproveitarBitmaps) {
            a = a.copy(reaproveitarBitmaps = it)
        }
        Interruptor("Mostrar diagnóstico na tela (fps, memória, latência)", a.mostrarDiagnostico) {
            a = a.copy(mostrarDiagnostico = it)
        }

        Secao("Dados guardados")
        OutlinedButton(onClick = { confirmarLimpeza = true }) { Text("Apagar todas as fotos e o histórico") }

        Secao("Instalação da câmera")
        SecaoInstalacao(perfis, perfilAtivo, acoesPerfis)

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

/** Voz, volume mínimo, sensibilidade, tolerância do espelho e tempo da manobra (Parte D). */
@Composable
private fun SecaoAlertas(a: Ajustes, aoMudar: (Ajustes) -> Unit) {
    Secao("Alertas")
    Interruptor("Falar o alerta em voz alta, além do bipe", a.alertaPorVoz) { aoMudar(a.copy(alertaPorVoz = it)) }
    Text("Volume mínimo do bipe", style = MaterialTheme.typography.titleMedium)
    VOLUMES.forEach { (v, texto) -> Opcao(texto, a.volumeMinimo == v) { aoMudar(a.copy(volumeMinimo = v)) } }
    Text(
        "Se o volume de alarme do celular estiver no zero, o app avisa ao começar a viagem.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Secao("Sensibilidade")
    Text(
        "Muda o alerta de distração e de cansaço acumulado. O alerta de sono é sempre o mesmo. " +
            "Vale a partir da próxima viagem.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SENSIBILIDADES.forEach { (s, texto) -> Opcao(texto, a.sensibilidade == s) { aoMudar(a.copy(sensibilidade = s)) } }
    Text("Olhar um espelho ou o painel vira alerta depois de", style = MaterialTheme.typography.titleMedium)
    TOLERANCIAS_ESPELHO_MS.forEach { ms ->
        Opcao("${ms / MS_POR_S} s", a.toleranciaEspelhoMs == ms) { aoMudar(a.copy(toleranciaEspelhoMs = ms)) }
    }
    Text("Botão Manobra: tempo sem alerta de distração", style = MaterialTheme.typography.titleMedium)
    MANOBRAS_MS.forEach { ms -> Opcao("${ms / MS_POR_S} s", a.manobraMs == ms) { aoMudar(a.copy(manobraMs = ms)) } }
}

/** `quadros.csv`: uma linha por imagem analisada, sem imagem nem pontos do rosto (Parte D). */
@Composable
private fun SecaoQuadros(ligado: Boolean, consentimento: Boolean, aoMudar: (Boolean) -> Unit) {
    Interruptor("Registrar cada imagem analisada (quadros.csv)", ligado && consentimento) {
        if (consentimento) aoMudar(it)
    }
    val porHora = RegistroQuadros.megabytesPorHora(BYTES_LINHA_QUADRO, QUADROS_POR_SEGUNDO)
    Text(
        "Números de cada imagem (olhos, boca, cabeça e as 52 expressões do rosto), sem foto nem pontos do rosto. " +
            "Cerca de ${porHora.toInt()} MB por hora de viagem, no mesmo lugar dos CSV. " +
            "Fica só neste celular: o app não usa a internet.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!consentimento) {
        Text(
            "Precisa da autorização das fotos (expressões do rosto são dado pessoal).",
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

private val ESCALAS_TEXTO = listOf("normal" to 1f, "grande" to 1.25f, "muito grande" to 1.5f)

/** 50% é o bipe mais baixo que o app já tocava: abaixo disso o piso não mudaria nada. */
private val VOLUMES = listOf(0.5 to "50% (padrão)", 0.7 to "70%", 1.0 to "100%")
private const val MS_POR_S = 1_000
private val TOLERANCIAS_ESPELHO_MS = listOf(2_000L, 3_000L, 4_000L)
private val MANOBRAS_MS = listOf(15_000L, 30_000L, 45_000L, 60_000L)
private val SENSIBILIDADES = listOf(
    Sensibilidade.SUAVE to "Suave: menos alertas de distração",
    Sensibilidade.PADRAO to "Padrão",
    Sensibilidade.ALTA to "Alta: avisa mais cedo",
)

/** Linha típica do `quadros.csv`: 52 expressões de 7 bytes e cerca de 240 das outras 27 colunas, a 20 imagens/s. */
private const val BYTES_LINHA_QUADRO = 600
private const val QUADROS_POR_SEGUNDO = 20.0

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
