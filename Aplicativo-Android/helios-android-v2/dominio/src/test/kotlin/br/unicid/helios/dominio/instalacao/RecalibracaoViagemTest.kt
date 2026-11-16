package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.MetricasOlhar
import br.unicid.helios.dominio.tipos.PoseCabeca
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecalibracaoViagemTest {

    private val cfg = ConfiguracaoInstalacao()

    private fun metricas(yaw: Double, pitch: Double = -12.0) = Metricas(
        earBruto = 0.28, earExibicao = 0.28, marExibicao = 0.1, ecf = 0.1, perclosCurto = 0.0, perclosLongo = 0.0,
        olhoFechado = false, bocaAberta = false, pose = PoseCabeca(pitch, yaw, 0.0),
        olhar = MetricasOlhar(horizontal = 0.45, vertical = 0.05),
    )

    /** Quadros a cada 50 ms; devolve a calibração, se alguma saiu. */
    private fun RecalibracaoViagem.quadros(
        de: Long,
        ate: Long,
        emMovimento: Boolean = false,
        yaw: (Long) -> Double = { 3.0 },
    ) = (de until ate step 50).mapNotNull { t -> processar(t, metricas(yaw(t)), emMovimento) }.singleOrNull()

    @Test
    fun `com o rosto estavel por 1 s e o carro parado, calibra em 5 s e so uma vez`() {
        val r = RecalibracaoViagem(cfg)
        assertNull(r.quadros(0, 1_000))
        assertNull(r.progresso(999))
        val c = assertNotNull(r.quadros(1_000, 6_100))
        assertEquals(3.0, c.yawReferencia, 1e-9)
        assertEquals(-12.0, c.pitchReferencia, 1e-9)
        assertEquals(0.45, assertNotNull(c.olharHorizontal), 1e-9)
        assertTrue(r.concluida)
        assertNull(r.quadros(6_100, 13_000))
    }

    @Test
    fun `carro andando ou cabeca mexendo nao comeca`() {
        val r = RecalibracaoViagem(cfg)
        assertNull(r.quadros(0, 8_000, emMovimento = true))
        assertNull(r.quadros(8_000, 16_000) { t -> if ((t / 50) % 2 == 0L) 0.0 else 8.0 })
        assertFalse(r.concluida)
    }

    @Test
    fun `carro que anda no meio da coleta cancela e recomeca depois`() {
        val r = RecalibracaoViagem(cfg)
        r.quadros(0, 3_000)
        assertNotNull(r.progresso(3_000))
        assertNull(r.quadros(3_000, 3_100, emMovimento = true))
        assertNull(r.progresso(3_100))
        assertNotNull(r.quadros(3_100, 9_200))
    }

    @Test
    fun `variacao de exatamente 3 graus ainda e estavel`() {
        val r = RecalibracaoViagem(cfg)
        r.quadros(0, 1_050) { t -> if ((t / 50) % 2 == 0L) 0.0 else 3.0 }
        assertNotNull(r.progresso(1_050))
    }

    @Test
    fun `o quadro mais velho que 1 s sai da janela`() {
        val r = RecalibracaoViagem(cfg)
        r.quadros(0, 1_100) { t -> if (t == 0L) 10.0 else 3.0 } // o giro de t = 0 sai da janela em 1.050
        assertNotNull(r.progresso(1_100))
    }

    @Test
    fun `rosto que some e volta precisa de mais 1 s estavel`() {
        val r = RecalibracaoViagem(cfg)
        r.quadros(0, 900)
        (900L until 1_100 step 50).forEach { r.processar(it, null, false) }
        r.quadros(1_100, 1_200)
        assertNull(r.progresso(1_200))
    }

    @Test
    fun `sem amostras suficientes no fim dos 5 s recomeca`() {
        val r = RecalibracaoViagem(cfg)
        r.quadros(0, 1_050) // começa em 1.000
        (1_050L until 6_000 step 50).forEach { r.processar(it, null, false) }
        assertNull(r.quadros(6_000, 6_050)) // 2 amostras: não conclui e zera
        assertFalse(r.concluida)
        assertNull(r.progresso(6_050))
        assertNotNull(r.quadros(6_050, 12_200))
    }

    @Test
    fun `pitch variando mais de 3 graus nao e estavel e exatamente 3 e`() {
        fun alternando(a: Double, b: Double) = RecalibracaoViagem(cfg).also { r ->
            (0L until 1_050 step 50).forEach { t ->
                r.processar(t, metricas(3.0, if ((t / 50) % 2 == 0L) a else b), false)
            }
        }
        assertNull(alternando(-12.0, -8.0).progresso(1_000))
        assertNotNull(alternando(10.0, 13.0).progresso(1_000))
    }

    @Test
    fun `sem rosto nao conta como estavel`() {
        val r = RecalibracaoViagem(cfg)
        (0L until 2_000 step 50).forEach { r.processar(it, null, false) }
        assertNull(r.progresso(2_000))
    }
}
