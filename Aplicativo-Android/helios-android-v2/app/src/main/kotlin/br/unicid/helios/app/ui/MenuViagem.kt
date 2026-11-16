package br.unicid.helios.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import br.unicid.helios.app.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.unicid.helios.app.EstadoFotos
import br.unicid.helios.app.TelaUi
import br.unicid.helios.app.ViagemAtual
import br.unicid.helios.protocolos.SINAIS
import br.unicid.helios.protocolos.Tom
import br.unicid.helios.protocolos.misturar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** O que o menu pode fazer. Cada ação é um caso de uso do ViewModel. */
class AcoesMenu(
    val pausar: () -> Unit,
    val retomar: () -> Unit,
    val encerrar: () -> Unit,
    val novaViagem: () -> Unit,
    val historico: () -> Unit,
    val ajustes: () -> Unit,
    val atualizarNumeros: () -> Unit,
    /** "Manobra" (ADR-014): sem alerta de distração por um tempo (30 s no padrão), para estacionar ou dar ré. */
    val manobra: () -> Unit = {},
)

private const val SEGURAR_PARA_ENCERRAR_MS = 2_000
private const val ATUALIZAR_NUMEROS_MS = 2_000L

/**
 * Botão do menu: sempre no mesmo canto, grande e visível também com o carro andando,
 * para o motorista nunca ter que procurar como encerrar ou pausar a viagem.
 */
@Composable
fun BotaoMenu(aoTocar: () -> Unit, modifier: Modifier = Modifier, compacto: Boolean = false) {
    // 76 dp: alvo mínimo do Android for Cars para o que se usa com o carro ligado (ADR-012, regra 3).
    Button(
        onClick = aoTocar,
        modifier = modifier.heightIn(min = Alvo.direcao).widthIn(min = Alvo.direcao),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = if (compacto) Espaco.p else Espaco.g),
    ) {
        // Compacto (celular estreito com a miniatura): só o ícone, com o nome para o leitor de tela.
        val descricao = if (compacto) "Menu" else null
        Icon(painterResource(R.drawable.ic_menu), contentDescription = descricao, modifier = Modifier.size(32.dp))
        if (!compacto) {
            Spacer(Modifier.width(Espaco.p))
            val estilo = Tipos.secundario.copy(fontWeight = FontWeight.SemiBold)
            Text("Menu", style = estilo, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * Painel lateral do menu. Com o celular se mexendo, um aviso recomenda abrir histórico e ajustes só com o carro
 * parado (RF09), sem bloquear: o sensor acusa movimento na mão e talvez com o motor ligado (ADR-012, revisão).
 * Encerrar pede para segurar 2 s, para um toque sem querer não acabar com a viagem.
 */
@Composable
fun PainelMenu(
    ui: TelaUi,
    viagem: ViagemAtual?,
    fotos: EstadoFotos,
    acoes: AcoesMenu,
    aoFechar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val atualizar by rememberUpdatedState(acoes.atualizarNumeros)
    LaunchedEffect(Unit) {
        while (true) {
            atualizar()
            delay(ATUALIZAR_NUMEROS_MS)
        }
    }
    val emViagem = ui.capturando || ui.pausada
    Column(
        modifier
            .width(440.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val titulo = when {
                ui.pausada -> "Viagem pausada"
                ui.capturando -> "Viagem em andamento"
                else -> "Nenhuma viagem"
            }
            Text(titulo, style = Tipos.detalhe, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = aoFechar, modifier = Modifier.size(56.dp), contentPadding = ButtonDefaults.TextButtonContentPadding) {
                Text("✕", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        when {
            ui.pausada -> BotaoGrande("Retomar a viagem", acoes.retomar)
            ui.capturando -> BotaoGrande("Pausar (parada para descanso)", acoes.pausar)
            else -> BotaoGrande("Nova viagem", acoes.novaViagem)
        }
        if (emViagem) {
            BotaoSegurar("Segure para encerrar a viagem", acoes.encerrar)
            BotaoGrande("Manobra: ${ui.manobraSegundos} s sem alerta de distração", acoes.manobra)
        }

        // O sensor só recomenda (revisão do ADR-012 na 0.4.13): na mão ou com o motor ligado ele acusa movimento.
        if (ui.emMovimento && !ui.pausada) {
            Text(
                "O celular está se mexendo. Abra o histórico e os ajustes só com o carro parado.",
                style = Tipos.rodape,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = acoes.historico, modifier = Modifier.weight(1f).height(56.dp)) {
                Text("Histórico", fontSize = 18.sp)
            }
            OutlinedButton(onClick = acoes.ajustes, modifier = Modifier.weight(1f).height(56.dp)) {
                Text("Ajustes", fontSize = 18.sp)
            }
        }
        // Ações primeiro: em tela baixa os números da viagem não podem empurrar Pausar e Encerrar para fora.
        if (emViagem && viagem != null) NumerosDaViagem(viagem)
        val corFotos = with(MaterialTheme.colorScheme) { if (fotos.ligadas) onSurfaceVariant else error }
        Text(textoEstadoFotos(fotos), style = Tipos.rodape, color = corFotos)
    }
}

@Composable
private fun NumerosDaViagem(v: ViagemAtual) {
    val s = v.sessao
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Numero("Tempo monitorado", duracaoTexto(s.duracaoMs))
        Numero("Tempo em alerta", duracaoTexto(s.tempoEmAlertaMs))
        Numero("Cochilos (microssono e sono)", s.microssonos.toString())
        Numero("Bocejos", s.bocejos.toString())
        Numero("Distrações (rosto fora, cabeça ou olhar)", (s.desatencoes + s.distracoes).toString())
        Numero("PERCLOS médio", pctTexto(s.perclosMedio))
        Numero("Piscadas no último minuto", v.piscadasPorMinuto.toString())
    }
}

@Composable
private fun Numero(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        val corRotulo = MaterialTheme.colorScheme.onSurfaceVariant
        Text(rotulo, style = Tipos.rodape, color = corRotulo, modifier = Modifier.weight(1f))
        Text(valor, style = Tipos.rodape.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun BotaoGrande(texto: String, aoTocar: () -> Unit) {
    // 76 dp: usado com o carro ligado (ADR-012, regra 3).
    val tamanho = Modifier.fillMaxWidth().heightIn(min = Alvo.direcao)
    Button(onClick = aoTocar, modifier = tamanho, shape = RoundedCornerShape(12.dp)) {
        Text(texto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Botão de segurar: a barra enche em [SEGURAR_PARA_ENCERRAR_MS] enquanto o dedo fica em cima e
 * esvazia se soltar antes. Com leitor de tela (TalkBack), o toque duplo aciona direto.
 */
@Composable
fun BotaoSegurar(texto: String, aoConcluir: () -> Unit) {
    val concluir by rememberUpdatedState(aoConcluir)
    val progresso = remember { Animatable(0f) }
    val escopo = rememberCoroutineScope()
    // Vermelho do selo CRÍTICO com texto branco; o trilho é o mesmo vermelho escurecido,
    // então o texto passa 4,5:1 sobre os dois.
    val sinal = SINAIS.getValue(Tom.VERMELHO)
    val vermelho = cor(sinal.fundo)
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cor(misturar(sinal.fundo, 0xFF000000, 0.6)))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val enchendo = escopo.launch {
                            val restante = ((1f - progresso.value) * SEGURAR_PARA_ENCERRAR_MS).toInt()
                            progresso.animateTo(1f, tween(restante, easing = LinearEasing))
                            concluir()
                            progresso.snapTo(0f)
                        }
                        tryAwaitRelease()
                        if (enchendo.isActive) {
                            enchendo.cancel()
                            escopo.launch { progresso.animateTo(0f, tween(200)) }
                        }
                    },
                )
            }
            .semantics {
                role = Role.Button
                contentDescription = texto
                onClick(label = texto) {
                    concluir()
                    true
                }
            },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progresso.value).background(vermelho))
        Text(
            texto,
            color = cor(sinal.texto),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
