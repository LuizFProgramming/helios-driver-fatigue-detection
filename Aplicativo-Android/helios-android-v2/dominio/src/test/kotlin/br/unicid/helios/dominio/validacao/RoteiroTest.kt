package br.unicid.helios.dominio.validacao

import br.unicid.helios.dominio.amostra
import br.unicid.helios.dominio.tipos.RecortesOlhos
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Roteiro de teste guiado: cada tarefa com preparo antes, tempo próprio e o que se espera do app. */
class RoteiroTest {

    private val curto = Roteiro(listOf(Tarefa.ESTRADA, Tarefa.FECHAR_CURTO), preparoMs = 1_000)

    @Test
    fun `cada tarefa comeca com o preparo e depois conta o proprio tempo`() {
        val preparo = assertNotNull(curto.passoEm(0))
        assertEquals(Tarefa.ESTRADA, preparo.tarefa)
        assertTrue(preparo.preparando)
        assertEquals(1_000L, preparo.restanteMs)
        assertEquals(0, preparo.indice)

        val estrada = assertNotNull(curto.passoEm(1_000))
        assertFalse(estrada.preparando)
        assertEquals(Tarefa.ESTRADA.duracaoMs, estrada.restanteMs)

        val proximo = assertNotNull(curto.passoEm(1_000 + Tarefa.ESTRADA.duracaoMs + 400))
        assertEquals(Tarefa.FECHAR_CURTO, proximo.tarefa)
        assertTrue(proximo.preparando)
        assertEquals(600L, proximo.restanteMs)
        assertEquals(1, proximo.indice)
    }

    @Test
    fun `acaba depois da ultima tarefa e informa a duracao total`() {
        val total = 2_000 + Tarefa.ESTRADA.duracaoMs + Tarefa.FECHAR_CURTO.duracaoMs
        assertEquals(total, curto.duracaoMs)
        assertNotNull(curto.passoEm(total - 1))
        assertNull(curto.passoEm(total))
        assertNull(curto.tarefaEm(total))
    }

    @Test
    fun `tarefa so vale fora do preparo`() {
        assertNull(curto.tarefaEm(500))
        assertEquals(Tarefa.ESTRADA, curto.tarefaEm(1_000))
        assertEquals(Tarefa.FECHAR_CURTO, curto.tarefaEm(2_000 + Tarefa.ESTRADA.duracaoMs))
    }

    @Test
    fun `intervalos das tarefas sem o preparo, a partir do inicio do roteiro`() {
        val inicio = 50_000L
        val estradaFim = inicio + 1_000 + Tarefa.ESTRADA.duracaoMs
        val fecharInicio = estradaFim + 1_000
        val intervalos = curto.intervalos(inicio)
        assertEquals(
            listOf(
                IntervaloRoteiro(Tarefa.ESTRADA, inicio + 1_000, estradaFim),
                IntervaloRoteiro(Tarefa.FECHAR_CURTO, fecharInicio, fecharInicio + Tarefa.FECHAR_CURTO.duracaoMs),
            ),
            intervalos,
        )
        assertEquals(listOf(inicio + 1_000, fecharInicio), intervalos.map { it.inicioMs })
        assertEquals(listOf(estradaFim, fecharInicio + 2_000), intervalos.map { it.fimMs })
    }

    @Test
    fun `roteiro padrao cobre olho fechado curto e longo, distracao, zonas e bocejo em cerca de 2 minutos`() {
        val r = Roteiro()
        val tarefas = r.tarefas.toSet()
        assertTrue(tarefas.containsAll(Tarefa.entries))
        assertTrue(r.duracaoMs in 90_000L..150_000L, "duração de ${r.duracaoMs} ms")
        // Depois de cada tarefa com alerta, o motorista volta para a estrada antes da próxima.
        r.tarefas.zipWithNext().filter { it.first.alerta != AlertaEsperado.NENHUM }.forEach {
            assertEquals(Tarefa.ESTRADA, it.second, "depois de ${it.first}")
        }
    }

    @Test
    fun `amostra leva os recortes dos olhos do roteiro`() {
        val recortes = RecortesOlhos(ByteArray(4) { 1 }, ByteArray(4) { 2 })
        val a = amostra().copy(recortesOlhos = recortes)
        assertEquals(recortes, a.recortesOlhos)
        assertNull(amostra().recortesOlhos)
    }

    @Test
    fun `o esperado de cada tarefa segue os tempos do app`() {
        assertEquals(AlertaEsperado.MICROSSONO, Tarefa.FECHAR_CURTO.alerta) // 2 s: passa de 1 s, não chega a 3 s
        assertEquals(AlertaEsperado.SONO, Tarefa.FECHAR_LONGO.alerta) // 4 s: passa de 3 s
        assertEquals(AlertaEsperado.DISTRACAO, Tarefa.OLHAR_DE_LADO.alerta) // 5 s: passa de 3 s
        assertEquals(OlhoEsperado.FECHADO, Tarefa.FECHAR_LONGO.olho)
        assertEquals(OlhoEsperado.ABERTO, Tarefa.ESPELHO_ESQUERDO.olho)
        assertNull(Tarefa.PISCAR.olho)
        assertNull(Tarefa.BOCEJAR.olho)
    }
}
