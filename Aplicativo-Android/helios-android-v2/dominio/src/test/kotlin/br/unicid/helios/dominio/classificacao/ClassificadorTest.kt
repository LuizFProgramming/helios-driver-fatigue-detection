package br.unicid.helios.dominio.classificacao

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClassificadorTest {
    private val cfg = ConfiguracaoDeteccao()
    private val classificador = Classificador(cfg)
    private val calmo = Indicadores(
        olhosFechadosMs = 0,
        cabecaCaida = false,
        perclosLongo = 0.0,
        bocejosRecentes = 0,
        piscadasLongasRecentes = 0,
    )

    @Test
    fun `sem sinais o motorista esta atento`() {
        assertEquals(Classificacao(EstadoMotorista.ATENTO, emptySet()), classificador.classificar(calmo))
    }

    @Test
    fun `evento brusco sozinho nao muda o estado`() {
        assertEquals(EstadoMotorista.ATENTO, classificador.classificar(calmo.copy(eventoBruscoRecente = true)).estado)
    }

    @Test
    fun `escada do olho fechado segue a Euro NCAP - piscada lenta, aviso, sono`() {
        assertEquals(EstadoMotorista.ATENTO, classificador.classificar(calmo.copy(olhosFechadosMs = 1_499)).estado)
        assertEquals(
            Classificacao(EstadoMotorista.ATENCAO, setOf(Motivo.OLHOS_FECHANDO)),
            classificador.classificar(calmo.copy(olhosFechadosMs = cfg.avisoOlhoFechadoMs)),
        )
        assertEquals(EstadoMotorista.ATENCAO, classificador.classificar(calmo.copy(olhosFechadosMs = 2_999)).estado)
        assertEquals(
            Classificacao(EstadoMotorista.CRITICO, setOf(Motivo.SONO)),
            classificador.classificar(calmo.copy(olhosFechadosMs = cfg.sonoMinMs)),
        )
    }

    @Test
    fun `cabeca caida e critica`() {
        assertEquals(
            Classificacao(EstadoMotorista.CRITICO, setOf(Motivo.CABECA_CAIDA)),
            classificador.classificar(calmo.copy(cabecaCaida = true)),
        )
    }

    @Test
    fun `cada sinal de fadiga leva a fadiga e o limiar e inclusivo`() {
        val casos = mapOf(
            calmo.copy(perclosLongo = cfg.limiarPerclosFadiga) to Motivo.PERCLOS_FADIGA,
            calmo.copy(bocejosRecentes = cfg.bocejosParaFadiga) to Motivo.BOCEJOS_FREQUENTES,
            calmo.copy(piscadasLongasRecentes = cfg.piscadasLongasParaFadiga) to Motivo.PISCADAS_LONGAS_FREQUENTES,
            calmo.copy(microssonosRecentes = cfg.microssonosParaFadiga) to Motivo.MICROSSONOS_FREQUENTES,
            calmo.copy(posSono = true) to Motivo.POS_SONO,
        )
        casos.forEach { (ind, motivo) ->
            assertEquals(Classificacao(EstadoMotorista.FADIGA, setOf(motivo)), classificador.classificar(ind))
        }
    }

    @Test
    fun `logo abaixo dos limiares continua atento`() {
        val ind = calmo.copy(
            olhosFechadosMs = cfg.avisoOlhoFechadoMs - 1,
            perclosLongo = cfg.limiarPerclosFadiga - 0.001,
            bocejosRecentes = cfg.bocejosParaFadiga - 1,
            piscadasLongasRecentes = cfg.piscadasLongasParaFadiga - 1,
            microssonosRecentes = cfg.microssonosParaFadiga - 1,
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

    @Test
    fun `solavanco no pos-sono nao vira critico`() {
        val ind = calmo.copy(posSono = true, eventoBruscoRecente = true)
        assertEquals(Classificacao(EstadoMotorista.FADIGA, setOf(Motivo.POS_SONO)), classificador.classificar(ind))
    }

    @Test
    fun `prioridade - critico, atencao, distraido, fadiga`() {
        val virada = calmo.copy(cabecaVirada = true)
        assertEquals(Classificacao(EstadoMotorista.DISTRAIDO, setOf(Motivo.CABECA_VIRADA)), classificador.classificar(virada))
        assertEquals(EstadoMotorista.DISTRAIDO, classificador.classificar(virada.copy(posSono = true)).estado)
        assertEquals(EstadoMotorista.ATENCAO, classificador.classificar(virada.copy(olhosFechadosMs = 2_000)).estado)
        assertEquals(EstadoMotorista.CRITICO, classificador.classificar(virada.copy(cabecaCaida = true)).estado)
    }

    @Test
    fun `olhar fora por 3 s seguidos ou 10 s somados distrai, com limiar inclusivo`() {
        val olhar = classificador.classificar(calmo.copy(olharDesviadoMs = cfg.olharDesviadoMinMs))
        assertEquals(Classificacao(EstadoMotorista.DISTRAIDO, setOf(Motivo.OLHAR_DESVIADO)), olhar)
        val soma = classificador.classificar(calmo.copy(foraDaViaMs = cfg.distracaoAcumuladaMs))
        assertEquals(Classificacao(EstadoMotorista.DISTRAIDO, setOf(Motivo.DISTRACAO_ACUMULADA)), soma)
        val abaixo = calmo.copy(olharDesviadoMs = cfg.olharDesviadoMinMs - 1, foraDaViaMs = cfg.distracaoAcumuladaMs - 1)
        assertEquals(EstadoMotorista.ATENTO, classificador.classificar(abaixo).estado)
    }

    @Test
    fun `piscadas lentas precisam de piscadas suficientes e media alta`() {
        val lentas = calmo.copy(piscadasNoMinuto = cfg.minimoPiscadasParaMedia, duracaoMediaPiscadaMs = cfg.duracaoMediaPiscadaFadigaMs)
        assertEquals(Classificacao(EstadoMotorista.FADIGA, setOf(Motivo.PISCADAS_LENTAS)), classificador.classificar(lentas))
        val poucas = lentas.copy(piscadasNoMinuto = cfg.minimoPiscadasParaMedia - 1)
        assertEquals(EstadoMotorista.ATENTO, classificador.classificar(poucas).estado)
        val rapidas = lentas.copy(duracaoMediaPiscadaMs = cfg.duracaoMediaPiscadaFadigaMs - 1)
        assertEquals(EstadoMotorista.ATENTO, classificador.classificar(rapidas).estado)
    }

    @Test
    fun `olhos semicerrados em metade do minuto sao fadiga`() {
        val pesados = calmo.copy(fracaoSemicerrado = cfg.fracaoSemicerradoFadiga)
        assertEquals(Classificacao(EstadoMotorista.FADIGA, setOf(Motivo.OLHOS_SEMICERRADOS)), classificador.classificar(pesados))
        val menos = calmo.copy(fracaoSemicerrado = cfg.fracaoSemicerradoFadiga - 0.01)
        assertEquals(EstadoMotorista.ATENTO, classificador.classificar(menos).estado)
    }
}
