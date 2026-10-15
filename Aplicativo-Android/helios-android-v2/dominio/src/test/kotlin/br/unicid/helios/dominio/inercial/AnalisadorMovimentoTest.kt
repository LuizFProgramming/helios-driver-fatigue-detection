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
    fun `vibracao acima do limiar liga o movimento e so desliga depois de 8 s calmo`() {
        val a = AnalisadorMovimento(cfg)
        for (t in 0L..1_900L step 100) a.registrar(t, 0.8)
        assertTrue(a.emMovimento())
        // A janela de 2 s esvazia da vibração por volta de 4 s; o calmo começa ali e precisa durar 8 s.
        for (t in 2_000L..11_000L step 100) a.registrar(t, 0.05)
        assertTrue(a.emMovimento(), "ainda dentro dos 8 s de calma")
        for (t in 11_100L..13_000L step 100) a.registrar(t, 0.05)
        assertFalse(a.emMovimento())
    }

    @Test
    fun `vibracao intermediaria mantem o estado em que estava`() {
        val parado = AnalisadorMovimento(cfg)
        for (t in 0L..20_000L step 100) parado.registrar(t, 0.4)
        assertFalse(parado.emMovimento(), "entre 0,25 e 0,5 não liga")

        val andando = AnalisadorMovimento(cfg)
        for (t in 0L..1_900L step 100) andando.registrar(t, 0.8)
        for (t in 2_000L..30_000L step 100) andando.registrar(t, 0.4)
        assertTrue(andando.emMovimento(), "entre 0,25 e 0,5 não conta como calma")
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
