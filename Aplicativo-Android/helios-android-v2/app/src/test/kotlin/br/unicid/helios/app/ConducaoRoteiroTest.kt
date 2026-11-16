package br.unicid.helios.app

import br.unicid.helios.dominio.validacao.IntervaloRoteiro
import br.unicid.helios.dominio.validacao.Roteiro
import br.unicid.helios.dominio.validacao.Tarefa
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConducaoRoteiroTest {

    private val falas = mutableListOf<String>()
    private var bipes = 0
    private var recortes = false
    private val gravados = mutableListOf<Pair<List<IntervaloRoteiro>, Boolean>>()
    private val roteiro = Roteiro(listOf(Tarefa.ESTRADA, Tarefa.FECHAR_CURTO), preparoMs = 1_000)
    private val conducao = ConducaoRoteiro(
        roteiro,
        gravarRoteiro = { i, o -> gravados += i to o },
        falar = { falas += it },
        bipe = { bipes++ },
        guardarRecortes = { recortes = it },
    )

    @Test
    fun `fora do roteiro nao faz nada`() {
        assertNull(conducao.processar(5_000, oculos = false))
        assertNull(conducao.estado.value)
        assertTrue(falas.isEmpty())
    }

    @Test
    fun `comeca no primeiro quadro, fala, bipa e grava o roteiro no fim`() {
        conducao.pedir(comRecortes = true)
        assertTrue(recortes)
        val esperando = assertNotNull(conducao.estado.value)
        assertNull(esperando.passo)
        assertFalse(esperando.concluido)

        assertTrue(assertNotNull(conducao.processar(10_000, oculos = true)).preparando)
        assertEquals(listOf("A seguir: olhe para a estrada"), falas)
        conducao.processar(11_000, oculos = true)
        assertEquals(1, bipes)
        assertEquals(Tarefa.ESTRADA, conducao.estado.value?.passo?.tarefa)

        val fim = 10_000 + roteiro.duracaoMs
        assertNull(conducao.processar(fim, oculos = true))
        val (intervalos, oculos) = gravados.single()
        assertEquals(roteiro.intervalos(10_000), intervalos)
        assertTrue(oculos)
        assertTrue(assertNotNull(conducao.estado.value).concluido)
        assertFalse(recortes)
        assertNull(conducao.processar(fim + 100, oculos = true)) // depois do fim não volta a gravar
        assertEquals(1, gravados.size)

        conducao.fechar()
        assertNull(conducao.estado.value)
    }

    @Test
    fun `cancelar grava so as tarefas ja terminadas e volta ao inicio`() {
        conducao.pedir(comRecortes = false)
        conducao.processar(0, oculos = false)
        val depoisDaEstrada = 1_000 + Tarefa.ESTRADA.duracaoMs + 500
        conducao.processar(depoisDaEstrada, oculos = false)
        conducao.cancelar(depoisDaEstrada, oculos = false)
        assertEquals(listOf(Tarefa.ESTRADA), gravados.single().first.map { it.tarefa })
        assertNull(conducao.estado.value)
        assertNull(conducao.processar(depoisDaEstrada + 100, oculos = false))
    }

    @Test
    fun `cancelar antes do primeiro quadro nao grava nada`() {
        conducao.pedir(comRecortes = true)
        conducao.cancelar(null, oculos = false)
        assertTrue(gravados.isEmpty())
        assertFalse(recortes)
        assertNull(conducao.estado.value)
    }
}
