package br.unicid.helios.app

import br.unicid.helios.dominio.validacao.IntervaloRoteiro
import br.unicid.helios.dominio.validacao.PassoRoteiro
import br.unicid.helios.dominio.validacao.Roteiro
import br.unicid.helios.protocolos.AvisoRoteiro
import br.unicid.helios.protocolos.avisosDoRoteiro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Roteiro para a tela. [passo] nulo e não [concluido]: esperando o primeiro quadro analisado. */
data class EstadoRoteiro(val passo: PassoRoteiro?, val total: Int, val concluido: Boolean = false)

/**
 * Conduz o roteiro de teste (ADR-015) na thread do domínio: começa no primeiro quadro depois de [pedir], faz os
 * avisos de [avisosDoRoteiro] e, no fim ou ao cancelar, grava as tarefas já feitas. Fora do ViewModel.
 */
class ConducaoRoteiro(
    private val roteiro: Roteiro = Roteiro(),
    private val gravarRoteiro: (List<IntervaloRoteiro>, oculos: Boolean) -> Unit,
    private val falar: (String) -> Unit,
    private val bipe: () -> Unit,
    private val guardarRecortes: (Boolean) -> Unit,
) {
    private var pedido = false
    private var inicioMs: Long? = null
    private var anterior: PassoRoteiro? = null
    private val _estado = MutableStateFlow<EstadoRoteiro?>(null)
    val estado: StateFlow<EstadoRoteiro?> = _estado

    fun pedir(comRecortes: Boolean) {
        pedido = true
        inicioMs = null
        anterior = null
        guardarRecortes(comRecortes)
        _estado.value = EstadoRoteiro(null, roteiro.tarefas.size)
    }

    /** A cada leitura: devolve o passo para o registro do quadro, ou nulo fora do roteiro. */
    fun processar(t: Long, oculos: Boolean): PassoRoteiro? {
        if (!pedido) return null
        val inicio = inicioMs ?: t.also { inicioMs = it }
        val passo = roteiro.passoEm(t - inicio)
        avisosDoRoteiro(anterior, passo).forEach { if (it is AvisoRoteiro.Falar) falar(it.texto) else bipe() }
        anterior = passo
        if (passo == null) {
            terminar(inicio, t, oculos)
            _estado.value = EstadoRoteiro(null, roteiro.tarefas.size, concluido = true)
        } else {
            _estado.value = EstadoRoteiro(passo, roteiro.tarefas.size)
        }
        return passo
    }

    /** [agoraMs]: tempo da última leitura; nulo se nenhuma chegou (então não há o que gravar). */
    fun cancelar(agoraMs: Long?, oculos: Boolean) {
        val inicio = inicioMs
        if (pedido && inicio != null && agoraMs != null) terminar(inicio, agoraMs, oculos)
        pedido = false
        guardarRecortes(false)
        _estado.value = null
    }

    /** Volta da tela de concluído. */
    fun fechar() {
        _estado.value = null
    }

    private fun terminar(inicio: Long, agoraMs: Long, oculos: Boolean) {
        pedido = false
        guardarRecortes(false)
        gravarRoteiro(roteiro.intervalos(inicio).filter { it.fimMs <= agoraMs }, oculos)
    }
}
