package br.unicid.helios.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.unicid.helios.app.Ajustes
import br.unicid.helios.app.EstadoRoteiro
import br.unicid.helios.app.R
import br.unicid.helios.app.adaptadores.fotos.GravadorArquivos
import br.unicid.helios.dominio.validacao.PassoRoteiro
import br.unicid.helios.protocolos.TextosRoteiro

/**
 * Roteiro de teste guiado (ADR-015): uns 2 minutos de tarefas com rótulo, para medir a precisão do app e da CNN e
 * coletar dados com óculos. Antes, a pessoa diz se está de óculos e se autoriza guardar os dados do teste.
 */
@Composable
fun TelaRoteiro(estado: EstadoRoteiro?, ajustes: Ajustes, acoes: AcoesRoteiro) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Espaco.gg, vertical = Espaco.g)
            .widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(Espaco.m),
    ) {
        val passo = estado?.passo
        when {
            estado == null -> Inicio(ajustes, acoes)
            estado.concluido -> Concluido(ajustes, acoes.voltar)
            passo == null -> Esperando(acoes.cancelar)
            else -> EmAndamento(passo, estado.total, acoes.cancelar)
        }
    }
}

@Composable
private fun Inicio(a: Ajustes, acoes: AcoesRoteiro) {
    Text("Roteiro de teste", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Text(
        "Cerca de 2 minutos, com o celular no suporte e o carro parado. O app fala cada tarefa: olhar a estrada, " +
            "piscar, fechar os olhos, olhar os espelhos, virar o rosto e bocejar. Os alarmes tocam como numa viagem.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Opcao("Estou de óculos", a.oculos) { acoes.mudarAjustes(a.copy(oculos = it)) }
    Opcao("Autorizo guardar os números do rosto e os recortes dos olhos deste teste", a.consentimentoRoteiro) {
        acoes.mudarAjustes(a.copy(consentimentoRoteiro = it))
    }
    Text(
        "Os recortes são imagens pequenas dos olhos em cinza, usadas para treinar a rede neural com óculos. " +
            "Ficam só neste celular, em ${GravadorArquivos.PASTA_CSV}; o app não usa a internet. " +
            "Sem a autorização, o teste grava só o roteiro e os alertas.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
        Button(acoes.comecar, Modifier.heightIn(min = Alvo.parado)) { Text("Começar o roteiro") }
        OutlinedButton(acoes.voltar, Modifier.heightIn(min = Alvo.parado)) { Text("Voltar") }
    }
}

@Composable
private fun Opcao(texto: String, ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    // A linha inteira é o alvo do toque (regra 3), não só a chave.
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Alvo.parado)
            .toggleable(ligado, role = Role.Switch, onValueChange = aoMudar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = ligado, onCheckedChange = null)
    }
}

@Composable
private fun Esperando(cancelar: () -> Unit) {
    Text("Procurando o rosto…", style = MaterialTheme.typography.headlineMedium)
    Text(
        "O roteiro começa no primeiro quadro com a câmera ligada.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(cancelar, Modifier.heightIn(min = Alvo.parado)) { Text("Cancelar") }
}

private const val MS_POR_S = 1_000

@Composable
private fun EmAndamento(passo: PassoRoteiro, total: Int, cancelar: () -> Unit) {
    val segundos = (passo.restanteMs + MS_POR_S - 1) / MS_POR_S
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
        Text(
            "Tarefa ${passo.indice + 1} de $total",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { passo.indice.toFloat() / total },
            modifier = Modifier.weight(1f).height(Espaco.p).clip(CircleShape),
        )
    }
    Text(
        if (passo.preparando) "Prepare-se" else "Agora",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(TextosRoteiro.instrucao(passo.tarefa), style = Tipos.principal)
    Text("$segundos s", style = Tipos.numero.copy(fontSize = CONTAGEM_SP.sp, lineHeight = CONTAGEM_SP.sp))
    OutlinedButton(cancelar, Modifier.heightIn(min = Alvo.parado)) { Text("Cancelar o roteiro") }
}

private const val CONTAGEM_SP = 72

@Composable
private fun Concluido(a: Ajustes, voltar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaco.m)) {
        Icon(
            painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Text("Roteiro concluído", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    }
    val arquivos = if (a.consentimentoRoteiro) "roteiro, quadros e recortes" else "roteiro"
    Text(
        "Os arquivos de $arquivos estão em ${GravadorArquivos.PASTA_CSV}. Exporte também o histórico (eventos) " +
            "e passe tudo para o script scripts/analise_logs.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Button(voltar, Modifier.heightIn(min = Alvo.parado)) { Text("Voltar") }
}
