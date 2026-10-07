package br.unicid.helios.dominio.sinais

import br.unicid.helios.dominio.instantes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SinaisTest {

    @Test
    fun `ema comeca no primeiro valor e depois pondera`() {
        val ema = SuavizadorEma(0.25)
        assertEquals(1.0, ema.suavizar(1.0))
        assertEquals(0.75, ema.suavizar(0.0), 1e-12)
        assertEquals(0.8125, ema.suavizar(1.0), 1e-12)
    }

    @Test
    fun `perclos fica zero ate cobrir metade da janela`() {
        val p = Perclos(janelaMs = 1_000, coberturaMinima = 0.5, lacunaMaximaMs = 1_000)
        assertEquals(0.0, p.registrar(0, true))
        assertEquals(0.0, p.registrar(400, true))
        assertEquals(1.0, p.registrar(500, false), 1e-12)
    }

    @Test
    fun `perclos e o mesmo a 10, 15, 30 fps e com fps irregular`() {
        fun medir(ts: List<Long>): Double {
            val p = Perclos(janelaMs = 5_000, coberturaMinima = 0.5, lacunaMaximaMs = 1_000)
            var ultimo = 0.0
            // olhos fechados de 2000 a 3000 ms (20% de 5 s)
            ts.forEach { t -> ultimo = p.registrar(t, t in 2_000 until 3_000) }
            return ultimo
        }
        val irregular = instantes(0, 5_001, 30).filterIndexed { i, _ -> i % 3 != 1 }
        for (ts in listOf(instantes(0, 5_001, 10), instantes(0, 5_001, 15), instantes(0, 5_001, 30), irregular)) {
            assertEquals(0.20, medir(ts + 5_000), 0.02)
        }
    }

    @Test
    fun `perclos descarta o que saiu da janela e apara o segmento da borda`() {
        val p = Perclos(janelaMs = 1_000, coberturaMinima = 0.5, lacunaMaximaMs = 1_000)
        p.registrar(0, true)
        p.registrar(1_000, false) // segmento 0..1000 fechado
        assertEquals(0.5, p.registrar(1_500, false), 1e-12) // janela 500..1500: 500 fechado, 500 aberto
        assertEquals(0.0, p.registrar(2_000, false), 1e-12) // janela 1000..2000: tudo aberto
    }

    @Test
    fun `lacuna maior que o limite nao entra no perclos`() {
        val p = Perclos(janelaMs = 10_000, coberturaMinima = 0.0, lacunaMaximaMs = 1_000)
        p.registrar(0, true)
        p.registrar(500, false) // 500 ms fechado
        p.registrar(5_000, false) // lacuna de 4,5 s ignorada
        assertEquals(1.0, p.registrar(5_000, false), 1e-12) // timestamp repetido também é ignorado
        assertEquals(0.5, p.registrar(5_500, true), 1e-12)
    }

    @Test
    fun `contador conta so os eventos dentro da janela`() {
        val c = ContadorEmJanela(janelaMs = 1_000)
        c.registrar(0)
        c.registrar(500)
        assertEquals(2, c.contar(999))
        assertEquals(1, c.contar(1_000))
        assertEquals(0, c.contar(1_500))
    }
}
