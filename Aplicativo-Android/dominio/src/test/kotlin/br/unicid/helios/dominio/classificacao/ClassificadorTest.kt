package br.unicid.helios.dominio.classificacao

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClassificadorTest {
    private val cfg = ConfiguracaoDeteccao()
    private val classificador = Classificador(cfg)
    private val calmo = Indicadores(false, false, 0.0, 0.0, 0, 0, false)

    @Test
    fun `sem sinais o motorista esta atento`() {
        assertEquals(Classificacao(EstadoMotorista.ATENTO, emptySet()), classificador.classificar(calmo))
    }

    @Test
    fun `evento brusco sozinho nao muda o estado`() {
        assertEquals(EstadoMotorista.ATENTO, classificador.classificar(calmo.copy(eventoBruscoRecente = true)).estado)
    }

    @Test
    fun `cada sinal critico leva a critico`() {
        val casos = mapOf(
            calmo.copy(microssonoEmCurso = true) to Motivo.MICROSSONO,
            calmo.copy(cabecaCaida = true) to Motivo.CABECA_CAIDA,
            calmo.copy(perclosCurto = cfg.limiarPerclosCritico) to Motivo.PERCLOS_CRITICO,
        )
        casos.forEach { (ind, motivo) ->
            assertEquals(Classificacao(EstadoMotorista.CRITICO, setOf(motivo)), classificador.classificar(ind))
        }
    }

    @Test
    fun `cada sinal de fadiga leva a fadiga e o limiar e inclusivo`() {
        val casos = mapOf(
            calmo.copy(perclosLongo = cfg.limiarPerclosFadiga) to Motivo.PERCLOS_FADIGA,
            calmo.copy(bocejosRecentes = cfg.bocejosParaFadiga) to Motivo.BOCEJOS_FREQUENTES,
            calmo.copy(piscadasLongasRecentes = cfg.piscadasLongasParaFadiga) to Motivo.PISCADAS_LONGAS_FREQUENTES,
        )
        casos.forEach { (ind, motivo) ->
            assertEquals(Classificacao(EstadoMotorista.FADIGA, setOf(motivo)), classificador.classificar(ind))
        }
    }

    @Test
    fun `logo abaixo dos limiares continua atento`() {
        val ind = calmo.copy(
            perclosCurto = cfg.limiarPerclosCritico - 0.001,
            perclosLongo = cfg.limiarPerclosFadiga - 0.001,
            bocejosRecentes = cfg.bocejosParaFadiga - 1,
            piscadasLongasRecentes = cfg.piscadasLongasParaFadiga - 1,
        )
        assertEquals(EstadoMotorista.ATENTO, classificador.classificar(ind).estado)
    }

    @Test
    fun `fadiga com evento brusco vira critico e guarda os dois motivos`() {
        val ind = calmo.copy(bocejosRecentes = 3, eventoBruscoRecente = true)
        assertEquals(
            Classificacao(EstadoMotorista.CRITICO, setOf(Motivo.EVENTO_BRUSCO_COM_FADIGA, Motivo.BOCEJOS_FREQUENTES)),
            classificador.classificar(ind),
        )
    }
}
