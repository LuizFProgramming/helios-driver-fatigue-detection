package br.unicid.helios.app.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import br.unicid.helios.app.Ajustes
import br.unicid.helios.app.TipoFonte

/** Ajustes, abertos só com o carro parado. Alvos de toque grandes para a central multimídia. */
@Composable
fun TelaAjustes(inicial: Ajustes, aoSalvar: (Ajustes) -> Unit, aoCalibrar: () -> Unit, aoVoltar: () -> Unit) {
    var a by remember { mutableStateOf(inicial) }
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
}

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
private fun Campo(rotulo: String, valor: String, senha: Boolean = false, aoMudar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        visualTransformation = if (senha) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (senha) KeyboardType.Password else KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
}
