package br.unicid.helios.dominio.sessao

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.Sensibilidade
import br.unicid.helios.dominio.amostra
import br.unicid.helios.dominio.com
import br.unicid.helios.dominio.instalacao.Zona
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.MetricasOlhar
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Parte D: tempo por zona e em manobra em cada janela de 30 s, e a sensibilidade. */
class ZonasNaJanelaTest {

    private fun leitura(t: Long, zona: Zona? = null, fora: Boolean = false, manobra: Boolean = false) = Leitura(
        t, EstadoMotorista.ATENTO, emptySet(),
        Metricas(
            0.3, 0.3, 0.1, 0.1, 0.0, 0.0, false, false, null,
            olhar = MetricasOlhar(zona = zona, foraAgora = fora),
        ),
        Totais(0, 0, 0, 0), 0.0, false, emptyList(), manobraRestanteMs = if (manobra) 10_000 else null,
    )

    @Test
    fun `janela soma o tempo em espelho, painel, outra direcao e manobra`() {
        val a = AgregadorSessao()
        // 0-10 s estrada, 10-15 s espelho, 15-20 s painel, 20-25 s outra direção; manobra de 5 s a 15 s.
        (0L until 31_000L step 100).forEach { t ->
            val l = when {
                t < 10_000 -> leitura(t)
                t < 15_000 -> leitura(t, Zona.ESPELHO_ESQUERDO, fora = true)
                t < 20_000 -> leitura(t, Zona.PAINEL, fora = true)
                t < 25_000 -> leitura(t, fora = true)
                else -> leitura(t)
            }
            a.registrar(if (t in 5_000 until 15_000) l.copy(manobraRestanteMs = 1) else l)
        }
        val j = assertNotNull(a.resumo().janelas.first())
        val tempos = listOf(j.zonaEspelhoMs, j.zonaPainelMs, j.zonaOutraMs, j.manobraMs)
        assertEquals(listOf(5_000L, 5_000L, 5_000L, 10_000L), tempos)
    }

    @Test
    fun `retrovisor e espelho direito tambem sao espelho e sem metricas nada soma`() {
        val a = AgregadorSessao()
        a.registrar(leitura(0, Zona.RETROVISOR, fora = true))
        a.registrar(leitura(1_000, Zona.ESPELHO_DIREITO, fora = true))
        a.registrar(leitura(2_000).copy(metricas = null))
        a.registrar(leitura(3_000))
        val j = a.resumo().janelas.single()
        assertEquals(2_000L, j.zonaEspelhoMs)
        assertEquals(0L, j.zonaOutraMs)
    }

    @Test
    fun `sensibilidade muda so distracao e fadiga acumulada, nunca o sono`() {
        val base = ConfiguracaoDeteccao()
        val suave = base.com(Sensibilidade.SUAVE)
        val alta = base.com(Sensibilidade.ALTA)
        assertEquals(base, base.com(Sensibilidade.PADRAO))
        fun distracao(c: ConfiguracaoDeteccao) = listOf(c.olharDesviadoMinMs, c.giroCabecaMinMs, c.distracaoAcumuladaMs)
        assertEquals(listOf(4_000L, 3_000L, 12_000L), distracao(suave))
        assertEquals(0.20, suave.limiarPerclosFadiga, 1e-9)
        assertEquals(listOf(2_500L, 1_500L, 8_000L), distracao(alta))
        assertEquals(0.12, alta.limiarPerclosFadiga, 1e-9)
        listOf(suave, alta).forEach { c ->
            assertEquals(
                listOf(base.avisoOlhoFechadoMs, base.sonoMinMs, base.microssonoMinMs, base.cabecaCaidaMinMs),
                listOf(c.avisoOlhoFechadoMs, c.sonoMinMs, c.microssonoMinMs, c.cabecaCaidaMinMs),
            )
            assertEquals(base.limiarEar, c.limiarEar, 0.0)
            assertEquals(base.quedaCabecaGraus, c.quedaCabecaGraus, 0.0)
        }
    }

    @Test
    fun `amostra leva os blendshapes do registro por quadro`() {
        val todos = mapOf("cheekPuff" to 0.2)
        assertEquals(todos, amostra().copy(blendshapes = todos).blendshapes)
    }
}
