package br.unicid.helios.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.unicid.helios.app.ui.AcoesMenu
import br.unicid.helios.app.ui.TelaAjustes
import br.unicid.helios.app.ui.TelaDirecao
import br.unicid.helios.app.ui.TelaHistorico
import br.unicid.helios.app.ui.TemaHelios
import kotlinx.coroutines.delay

/**
 * Tela única, sempre ligada e em paisagem. Pensada para ficar fixa no painel
 * (celular no suporte ou central multimídia Android), abrindo direto no monitoramento.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { TemaHelios { HeliosApp() } }
    }
}

private enum class Tela { DIRECAO, AJUSTES, HISTORICO }

@Composable
private fun HeliosApp(vm: HeliosViewModel = viewModel()) {
    val ui by vm.tela.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val viagens by vm.historico.collectAsStateWithLifecycle()
    val detalhe by vm.detalhe.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    val visor by vm.visor.collectAsStateWithLifecycle()
    val viagem by vm.viagemAtual.collectAsStateWithLifecycle()
    val fotos by vm.estadoFotos.collectAsStateWithLifecycle()
    var tela by rememberSaveable { mutableStateOf(Tela.DIRECAO) }
    PedirCamera(aoConceder = vm::iniciarCapturaSePreciso)

    // O sensor de movimento só bloqueia ENTRAR em histórico e ajustes (botões do menu desabilitados);
    // nunca tira o motorista de uma tela que ele já abriu.
    val voltarParaDirecao = {
        tela = Tela.DIRECAO
        vm.abrirDetalhe(null)
    }
    Box {
        when (tela) {
            Tela.AJUSTES -> TelaAjustes(
                inicial = ajustes,
                estadoFotos = fotos,
                aoSalvar = vm::salvarAjustes,
                aoMudarFotos = vm::salvarFotos,
                aoFotoDeTeste = vm::fotoDeTeste,
                aoCalibrar = vm::calibrar,
                aoLimparTudo = vm::limparTudo,
                aoVoltar = { tela = Tela.DIRECAO },
            )
            Tela.HISTORICO -> TelaHistorico(
                viagens = viagens,
                detalhe = detalhe,
                pastaEscolhida = ajustes.fotos.pasta,
                aoAbrir = vm::abrirDetalhe,
                aoDefinirKss = vm::definirKss,
                aoExportar = vm::exportarCsv,
                carregarMiniatura = vm::carregarMiniatura,
                carregarFoto = vm::carregarFoto,
                aoVoltar = voltarParaDirecao,
            )
            Tela.DIRECAO -> TelaDirecao(
                ui,
                visor = visor.takeIf { ajustes.mostrarVisor && ui.capturando },
                viagem = viagem,
                fotos = fotos,
                escalaTexto = ajustes.escalaTexto,
                acoes = AcoesMenu(
                    pausar = vm::pausar,
                    retomar = vm::retomar,
                    encerrar = {
                        vm.encerrarViagem()
                        tela = Tela.HISTORICO
                    },
                    novaViagem = vm::novaViagem,
                    historico = {
                        vm.carregarHistorico()
                        tela = Tela.HISTORICO
                    },
                    ajustes = { tela = Tela.AJUSTES },
                    atualizarNumeros = vm::atualizarViagemAtual,
                ),
            )
        }
        if (!ajustes.perguntouFotos) PerguntaFotos(aoPermitir = vm::autorizarFotos, aoRecusar = vm::recusarFotos)
        mensagem?.let { Aviso(it, vm::mensagemVista, Modifier.align(Alignment.BottomCenter)) }
    }
}

/**
 * Pergunta da primeira abertura (LGPD): o app só guarda fotos do motorista com o consentimento dele.
 * Ao permitir, grava uma foto de teste, que cria a pasta Pictures/HELIOS e mostra que a gravação funciona.
 * No Android 10 ou superior gravar na própria pasta do app não exige permissão de armazenamento do sistema.
 */
@Composable
private fun PerguntaFotos(aoPermitir: () -> Unit, aoRecusar: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Guardar fotos dos alertas?") },
        text = {
            Text(
                "Quando detectar sono ou distração, o HELIOS grava uma foto do motorista e outra com a análise " +
                    "desenhada na memória interna do celular, na pasta Pictures/HELIOS (álbum HELIOS na galeria). " +
                    "As fotos não saem do celular pelo app, mas a galeria e o backup do celular conseguem vê-las.\n\n" +
                    "Ao permitir, o app grava agora uma foto de teste para criar a pasta. Dá para mudar isso depois em Ajustes.",
            )
        },
        confirmButton = { TextButton(onClick = aoPermitir) { Text("Permitir e gravar foto de teste") } },
        dismissButton = { TextButton(onClick = aoRecusar) { Text("Agora não") } },
    )
}

@Composable
private fun Aviso(texto: String, aoFechar: () -> Unit, modifier: Modifier) {
    LaunchedEffect(texto) {
        delay(DURACAO_AVISO_MS)
        aoFechar()
    }
    Text(
        texto,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.padding(24.dp).background(MaterialTheme.colorScheme.primary).padding(16.dp),
    )
}

private const val DURACAO_AVISO_MS = 4_000L

/** A câmera do aparelho precisa de permissão; a ESP32 não, mas pedir no início simplifica o fluxo. */
@Composable
private fun PedirCamera(aoConceder: () -> Unit) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val lancador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) aoConceder()
    }
    LaunchedEffect(Unit) {
        val jaTem = ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (jaTem) aoConceder() else lancador.launch(Manifest.permission.CAMERA)
    }
}
