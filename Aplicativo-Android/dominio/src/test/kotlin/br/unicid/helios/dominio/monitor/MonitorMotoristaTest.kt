package br.unicid.helios.dominio.monitor

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.amostra
import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.instantes
import br.unicid.helios.dominio.tipos.AmostraFacial
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Evento
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.dominio.tipos.TipoEvento
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonitorMotoristaTest {

    private val aberto = amostra(ear = 0.30)
    private val fechado = amostra(ear = 0.08)

    /** Roda o monitor de [inicioMs] a [fimMs] usando [cena] para escolher a amostra de cada instante. */
    private fun MonitorMotorista.rodar(
        inicioMs: Long,
        fimMs: Long,
        fps: Int,
        cena: (Long) -> AmostraFacial?,
    ): List<Leitura> = instantes(inicioMs, fimMs, fps).map { processar(Quadro(it, cena(it))) }

    private fun List<Leitura>.eventos(): List<Evento> = flatMap { it.eventos }

    @Test
    fun `olhos abertos o tempo todo e atento`() {
        val leituras = MonitorMotorista().rodar(0, 3_000, 30) { aberto }
        assertTrue(leituras.all { it.estado == EstadoMotorista.ATENTO })
        assertEquals(Totais(0, 0, 0, 0), leituras.last().totais)
        assertNotNull(leituras.last().metricas)
    }

    @Test
    fun `piscada curta de 100 ms e contada a 30 fps mesmo com a EMA (corrige P3)`() {
        val leituras = MonitorMotorista().rodar(0, 1_000, 30) { t -> if (t in 300 until 400) fechado else aberto }
        assertEquals(1, leituras.last().totais.piscadas)
        val piscada = leituras.eventos().single()
        assertEquals(TipoEvento.PISCADA, piscada.tipo)
    }

    @Test
    fun `microssono dispara critico depois de 1 s em qualquer fps (corrige P2)`() {
        for (fps in listOf(10, 15, 30)) {
            val leituras = MonitorMotorista().rodar(0, 2_500, fps) { t -> if (t >= 1_000) fechado else aberto }
            val primeiroCritico = leituras.first { it.estado == EstadoMotorista.CRITICO }
            assertTrue(primeiroCritico.timestampMs in 2_000..2_100, "fps=$fps t=${primeiroCritico.timestampMs}")
            assertTrue(Motivo.MICROSSONO in primeiroCritico.motivos)
        }
    }

    @Test
    fun `cronometro do critico conta a partir do primeiro frame critico (corrige P4)`() {
        val leituras = MonitorMotorista().rodar(0, 3_000, 10) { t -> if (t >= 500) fechado else aberto }
        val criticos = leituras.filter { it.estado == EstadoMotorista.CRITICO }
        assertEquals(0.0, criticos.first().segundosEmCritico)
        assertEquals((criticos.last().timestampMs - criticos.first().timestampMs) / 1000.0, criticos.last().segundosEmCritico)
    }

    @Test
    fun `microssono encerrado vira evento e conta no total`() {
        // Depois do microssono o PERCLOS de 5 s segura o CRÍTICO até a janela limpar.
        val leituras = MonitorMotorista().rodar(0, 8_000, 10) { t -> if (t in 500 until 2_000) fechado else aberto }
        assertEquals(1, leituras.last().totais.microssonos)
        assertEquals(Evento(TipoEvento.MICROSSONO, 500, 1_500), leituras.eventos().single())
        assertEquals(0.0, leituras.last().segundosEmCritico)
    }

    @Test
    fun `piscada longa vira evento e tres em um minuto indicam fadiga`() {
        val longas = listOf(1_000L, 5_000L, 9_000L)
        val leituras = MonitorMotorista().rodar(0, 12_000, 10) { t ->
            if (longas.any { t in it until it + 600 }) fechado else aberto
        }
        assertEquals(3, leituras.last().totais.piscadasLongas)
        assertEquals(EstadoMotorista.FADIGA, leituras.last().estado)
        assertTrue(Motivo.PISCADAS_LONGAS_FREQUENTES in leituras.last().motivos)
    }

    @Test
    fun `ECF alto fecha o olho mesmo com EAR alto`() {
        val leitura = MonitorMotorista().processar(Quadro(0, amostra(ear = 0.30, ecf = 0.6)))
        assertEquals(true, leitura.metricas?.olhoFechado)
    }

    @Test
    fun `bocejo pelo MAR ou pela mandibula conta so com 500 ms`() {
        val monitor = MonitorMotorista()
        val leituras = monitor.rodar(0, 4_000, 10) { t ->
            when (t) {
                in 500 until 900 -> amostra(mar = 0.7) // 400 ms: não conta
                in 1_500 until 2_100 -> amostra(mar = 0.7) // 600 ms: conta
                in 3_000 until 3_600 -> amostra(mandibula = 0.8) // 600 ms pela mandíbula: conta
                else -> aberto
            }
        }
        assertEquals(2, leituras.last().totais.bocejos)
        assertEquals(listOf(TipoEvento.BOCEJO, TipoEvento.BOCEJO), leituras.eventos().map { it.tipo })
    }

    @Test
    fun `tres bocejos em cinco minutos indicam fadiga`() {
        val bocejos = listOf(1_000L, 3_000L, 5_000L)
        val leituras = MonitorMotorista().rodar(0, 6_000, 10) { t ->
            if (bocejos.any { t in it until it + 600 }) amostra(mar = 0.8) else aberto
        }
        assertEquals(EstadoMotorista.FADIGA, leituras.last().estado)
    }

    @Test
    fun `cabeca caindo por 1 s e critica e vira evento ao levantar`() {
        val monitor = MonitorMotorista(calibracao = Calibracao(0.30, pitchReferencia = -5.0))
        val leituras = monitor.rodar(0, 3_000, 10) { t ->
            amostra(pitch = if (t in 500 until 2_000) -30.0 else -5.0)
        }
        assertTrue(leituras.any { Motivo.CABECA_CAIDA in it.motivos })
        assertEquals(Evento(TipoEvento.CABECA_CAIDA, 500, 1_500), leituras.eventos().single())
    }

    @Test
    fun `cabeca baixa por pouco tempo nao gera evento e sem pose nunca cai`() {
        val leituras = MonitorMotorista().rodar(0, 2_000, 10) { t ->
            if (t in 500 until 900) amostra(pitch = -40.0) else amostra(pitch = null)
        }
        assertTrue(leituras.eventos().isEmpty())
        assertTrue(leituras.none { Motivo.CABECA_CAIDA in it.motivos })
    }

    @Test
    fun `queda exatamente no limite nao conta`() {
        val cfg = ConfiguracaoDeteccao()
        val leituras = MonitorMotorista(cfg).rodar(0, 2_000, 10) { amostra(pitch = -cfg.quedaCabecaGraus) }
        assertTrue(leituras.none { Motivo.CABECA_CAIDA in it.motivos })
    }

    @Test
    fun `calibracao muda o limiar do EAR`() {
        val monitor = MonitorMotorista()
        assertEquals(false, monitor.processar(Quadro(0, amostra(ear = 0.22))).metricas?.olhoFechado)
        monitor.calibrar(Calibracao(earReferencia = 0.40, pitchReferencia = 0.0)) // limiar vira 0,26
        assertEquals(true, monitor.processar(Quadro(100, amostra(ear = 0.22))).metricas?.olhoFechado)
    }

    @Test
    fun `sem rosto devolve SEM_ROSTO sem metricas`() {
        val leitura = MonitorMotorista().processar(Quadro(0, null))
        assertEquals(EstadoMotorista.SEM_ROSTO, leitura.estado)
        assertNull(leitura.metricas)
        assertTrue(leitura.motivos.isEmpty())
    }

    @Test
    fun `evento brusco aparece na proxima leitura, com ou sem rosto`() {
        val monitor = MonitorMotorista()
        monitor.registrarAceleracao(10, 9.0)
        monitor.registrarAceleracao(20, 0.1)
        val primeira = monitor.processar(Quadro(30, null))
        assertEquals(listOf(Evento(TipoEvento.EVENTO_BRUSCO, 10, 0)), primeira.eventos)
        assertTrue(monitor.processar(Quadro(60, aberto)).eventos.isEmpty())
    }

    @Test
    fun `fadiga com evento brusco recente vira critico`() {
        val monitor = MonitorMotorista()
        val bocejos = listOf(1_000L, 3_000L, 5_000L)
        monitor.rodar(0, 6_000, 10) { t -> if (bocejos.any { t in it until it + 600 }) amostra(mar = 0.8) else aberto }
        monitor.registrarAceleracao(6_000, 6.0)
        val leitura = monitor.processar(Quadro(6_100, aberto))
        assertEquals(EstadoMotorista.CRITICO, leitura.estado)
        assertTrue(Motivo.EVENTO_BRUSCO_COM_FADIGA in leitura.motivos)
    }

    @Test
    fun `movimento do carro aparece na leitura`() {
        val monitor = MonitorMotorista()
        assertFalse(monitor.processar(Quadro(0, aberto)).emMovimento)
        for (t in 0L..1_000L step 50) monitor.registrarAceleracao(t, 1.0)
        assertTrue(monitor.processar(Quadro(1_000, aberto)).emMovimento)
    }

    @Test
    fun `numeros de exibicao sao suavizados e o EAR bruto nao`() {
        val monitor = MonitorMotorista()
        monitor.processar(Quadro(0, amostra(ear = 0.30, mar = 0.10)))
        val m = monitor.processar(Quadro(33, amostra(ear = 0.10, mar = 0.30))).metricas
        assertNotNull(m)
        assertEquals(0.10, m.earBruto, 1e-9)
        assertEquals(0.25, m.earExibicao, 1e-9)
        assertEquals(0.15, m.marExibicao, 1e-9)
    }
}
