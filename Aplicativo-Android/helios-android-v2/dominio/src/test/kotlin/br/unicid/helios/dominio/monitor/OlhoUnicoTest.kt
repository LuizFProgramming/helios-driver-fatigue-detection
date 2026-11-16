package br.unicid.helios.dominio.monitor

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.amostra
import br.unicid.helios.dominio.instantes
import br.unicid.helios.dominio.seisComRazao
import br.unicid.helios.dominio.tipos.AmostraFacial
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Quadro
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Acessibilidade (ADR-016): motorista que usa um olho só escolhe qual olho o app avalia. */
class OlhoUnicoTest {

    private fun MonitorMotorista.rodar(de: Long, ate: Long, fps: Int, cena: () -> AmostraFacial) =
        instantes(de, ate, fps).map { processar(Quadro(it, cena())) }

    private val soDireito = ConfiguracaoDeteccao(olhoUnico = OlhoAvaliado.DIREITO)
    private val soEsquerdo = ConfiguracaoDeteccao(olhoUnico = OlhoAvaliado.ESQUERDO)

    /** Olho direito fechado, esquerdo aberto e parado (prótese, por exemplo). */
    private val direitoFechado = amostra(ear = 0.30).let {
        it.copy(olhoDireito = amostra(ear = 0.05).olhoDireito, piscadaDireita = 0.9)
    }

    /** Olho esquerdo sempre fechado (tapa-olho, pálpebra caída), direito aberto. */
    private val esquerdoFechado = amostra(ear = 0.30).let {
        it.copy(olhoEsquerdo = amostra(ear = 0.05).olhoEsquerdo, piscadaEsquerda = 0.9)
    }

    @Test
    fun `no padrao um olho fechado sozinho nao e sono, com os dois olhos em uso`() {
        val leituras = MonitorMotorista().rodar(0, 4_000, 10) { direitoFechado }
        assertTrue(leituras.all { it.metricas?.olhoFechado == false })
        assertEquals(EstadoMotorista.ATENTO, leituras.last().estado)
    }

    @Test
    fun `com so o olho direito em uso, fecha-lo e sono mesmo com o outro aberto`() {
        val leituras = MonitorMotorista(soDireito).rodar(0, 4_000, 10) { direitoFechado }
        val m = assertNotNull(leituras.last().metricas)
        assertTrue(m.olhoFechado)
        assertEquals(OlhoAvaliado.DIREITO, m.olhoAvaliado)
        assertEquals(EstadoMotorista.CRITICO, leituras.last().estado)
    }

    @Test
    fun `o olho que nao esta em uso pode ficar fechado sem alerta`() {
        val leituras = MonitorMotorista(soDireito).rodar(0, 8_000, 10) { esquerdoFechado }
        assertTrue(leituras.all { it.metricas?.olhoFechado == false })
        assertEquals(EstadoMotorista.ATENTO, leituras.last().estado)
        val outro = MonitorMotorista(soEsquerdo).rodar(0, 4_000, 10) { esquerdoFechado }
        assertEquals(EstadoMotorista.CRITICO, outro.last().estado)
        assertEquals(OlhoAvaliado.ESQUERDO, outro.last().metricas?.olhoAvaliado)
    }

    @Test
    fun `a escolha vale mesmo com a cabeca de lado, quando o app escolheria o outro olho`() {
        // Olho esquerdo encurtado (de lado): sozinho, o app avaliaria só o direito.
        val deLado = amostra(ear = 0.30).copy(olhoEsquerdo = seisComRazao(0.30, escala = 40.0, origemX = 200.0))
        val livre = assertNotNull(MonitorMotorista().processar(Quadro(0, deLado)).metricas)
        assertEquals(OlhoAvaliado.DIREITO, livre.olhoAvaliado)
        val escolhido = assertNotNull(MonitorMotorista(soEsquerdo).processar(Quadro(0, deLado)).metricas)
        assertEquals(OlhoAvaliado.ESQUERDO, escolhido.olhoAvaliado)
        assertFalse(escolhido.olhoFechado)
    }
}
