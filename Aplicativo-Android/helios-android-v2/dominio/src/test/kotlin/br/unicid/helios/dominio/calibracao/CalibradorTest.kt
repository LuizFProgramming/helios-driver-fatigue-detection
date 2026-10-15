package br.unicid.helios.dominio.calibracao

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CalibradorTest {

    @Test
    fun `progresso vai de 0 a 1 e satura`() {
        val c = Calibrador(inicioMs = 1_000, duracaoMs = 5_000)
        assertEquals(0.0, c.progresso(500))
        assertEquals(0.5, c.progresso(3_500))
        assertEquals(1.0, c.progresso(9_000))
    }

    @Test
    fun `sem tempo ou sem amostras nao ha resultado`() {
        val c = Calibrador(inicioMs = 0, duracaoMs = 1_000, minimoAmostras = 3)
        repeat(3) { c.adicionar(0.3, 0.0) }
        assertNull(c.resultado(999))
        val vazio = Calibrador(inicioMs = 0, duracaoMs = 1_000, minimoAmostras = 3)
        repeat(2) { vazio.adicionar(0.3, 0.0) }
        assertNull(vazio.resultado(1_000))
    }

    @Test
    fun `mediana ignora piscadas no meio da calibracao`() {
        val c = Calibrador(inicioMs = 0, duracaoMs = 1_000, minimoAmostras = 5)
        listOf(0.31, 0.30, 0.05, 0.32, 0.29).forEach { c.adicionar(it, -5.0) }
        assertEquals(Calibracao(0.30, -5.0), c.resultado(1_000))
    }

    @Test
    fun `sem pose a referencia de pitch e zero`() {
        val c = Calibrador(inicioMs = 0, duracaoMs = 10, minimoAmostras = 1)
        c.adicionar(0.3, null)
        assertEquals(Calibracao(0.3, 0.0), c.resultado(10))
    }

    @Test
    fun `mediana de quantidade par e a media dos dois do meio`() {
        assertEquals(2.5, mediana(listOf(4.0, 1.0, 3.0, 2.0)))
        assertEquals(3.0, mediana(listOf(5.0, 3.0, 1.0)))
    }

    @Test
    fun `cada olho ganha a sua referencia quando os dois sao informados`() {
        val c = Calibrador(inicioMs = 0, duracaoMs = 10, minimoAmostras = 3)
        listOf(0.30 to 0.26, 0.32 to 0.24, 0.31 to 0.25).forEach { (d, e) -> c.adicionar((d + e) / 2, 0.0, 0.0, d, e) }
        val r = assertNotNull(c.resultado(10))
        assertEquals(0.31, assertNotNull(r.earDireito), 1e-9)
        assertEquals(0.25, assertNotNull(r.earEsquerdo), 1e-9)
        val semOlhos = Calibrador(inicioMs = 0, duracaoMs = 10, minimoAmostras = 1).apply { adicionar(0.3, 0.0) }
        assertNull(assertNotNull(semOlhos.resultado(10)).earDireito)
    }

    @Test
    fun `yaw entra na calibracao pela mediana`() {
        val c = Calibrador(inicioMs = 0, duracaoMs = 10, minimoAmostras = 3)
        listOf(8.0, 12.0, 10.0).forEach { c.adicionar(0.3, 0.0, it) }
        assertEquals(Calibracao(0.3, 0.0, 10.0), c.resultado(10))
    }

    @Test
    fun `olhar entra na calibracao pela mediana e fica nulo sem amostras`() {
        val c = Calibrador(inicioMs = 0, duracaoMs = 10, minimoAmostras = 3)
        c.adicionar(0.3, 0.0, olharHorizontal = 0.60, olharVertical = -0.10)
        c.adicionar(0.3, 0.0, olharHorizontal = 0.70, olharVertical = -0.20)
        c.adicionar(0.3, 0.0, olharHorizontal = 0.65)
        val r = assertNotNull(c.resultado(10))
        assertEquals(0.65, r.olharHorizontal)
        assertEquals(-0.15, r.olharVertical!!, 1e-9)
        val sem = Calibrador(inicioMs = 0, duracaoMs = 10, minimoAmostras = 1).apply { adicionar(0.3, 0.0) }
        assertNull(assertNotNull(sem.resultado(10)).olharHorizontal)
    }
}
