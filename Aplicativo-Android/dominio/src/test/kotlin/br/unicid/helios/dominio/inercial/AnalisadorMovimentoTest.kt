package br.unicid.helios.dominio.inercial

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalisadorMovimentoTest {
    private val cfg = ConfiguracaoDeteccao()

    @Test
    fun `sem amostras o carro esta parado`() {
        assertFalse(AnalisadorMovimento(cfg).emMovimento())
    }

    @Test
    fun `vibracao continua acima do limiar indica movimento e some quando sai da janela`() {
        val a = AnalisadorMovimento(cfg)
        for (t in 0L..1_900L step 100) a.registrar(t, 0.6)
        assertTrue(a.emMovimento())
        for (t in 2_000L..4_100L step 100) a.registrar(t, 0.05)
        assertFalse(a.emMovimento())
    }

    @Test
    fun `rms exatamente no limiar ainda e parado`() {
        val a = AnalisadorMovimento(cfg)
        a.registrar(0, cfg.limiarMovimentoRms)
        assertFalse(a.emMovimento())
    }

    @Test
    fun `evento brusco e novo so uma vez dentro da janela`() {
        val a = AnalisadorMovimento(cfg)
        assertFalse(a.eventoBruscoRecente(0))
        assertFalse(a.registrar(0, cfg.limiarEventoBrusco - 0.01))
        assertTrue(a.registrar(100, cfg.limiarEventoBrusco))
        assertFalse(a.registrar(200, 9.0))
        assertTrue(a.eventoBruscoRecente(200 + cfg.janelaEventoBruscoMs - 1))
        assertFalse(a.eventoBruscoRecente(200 + cfg.janelaEventoBruscoMs))
        assertTrue(a.registrar(200 + cfg.janelaEventoBruscoMs, 9.0))
    }
}
