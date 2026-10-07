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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.unicid.helios.app.ui.TelaAjustes
import br.unicid.helios.app.ui.TelaDirecao
import br.unicid.helios.app.ui.TemaHelios

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

@Composable
private fun HeliosApp(vm: HeliosViewModel = viewModel()) {
    val ui by vm.tela.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    var emAjustes by rememberSaveable { mutableStateOf(false) }
    PedirCamera(aoConceder = vm::iniciarCaptura)

    if (emAjustes && !ui.emMovimento) {
        TelaAjustes(
            inicial = ajustes,
            aoSalvar = vm::salvarAjustes,
            aoCalibrar = vm::calibrar,
            aoVoltar = { emAjustes = false },
        )
    } else {
        TelaDirecao(ui, aoAbrirAjustes = { emAjustes = true })
    }
}

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
