package br.unicid.helios.dominio.monitor

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.amostra
import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.instantes
import br.unicid.helios.dominio.seisComRazao
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
    fun `olho fechado - aviso com 1,5 s e critico com 3 s, em qualquer fps (corrige P2)`() {
        for (fps in listOf(10, 15, 30)) {
            val leituras = MonitorMotorista().rodar(0, 5_000, fps) { t -> if (t >= 1_000) fechado else aberto }
            val aviso = leituras.first { it.estado == EstadoMotorista.ATENCAO }
            val critico = leituras.first { it.estado == EstadoMotorista.CRITICO }
            assertTrue(aviso.timestampMs in 2_500..2_600, "fps=$fps aviso=${aviso.timestampMs}")
            assertTrue(critico.timestampMs in 4_000..4_100, "fps=$fps critico=${critico.timestampMs}")
            assertTrue(Motivo.SONO in critico.motivos)
        }
    }

    @Test
    fun `fechar os olhos por 1 s e abrir e piscada lenta, sem alarme`() {
        val leituras = MonitorMotorista().rodar(0, 4_000, 30) { t -> if (t in 1_000 until 2_000) fechado else aberto }
        assertTrue(leituras.all { it.estado == EstadoMotorista.ATENTO })
        assertEquals(TipoEvento.MICROSSONO, leituras.eventos().single().tipo)
    }

    @Test
    fun `ao abrir os olhos depois do critico o alarme cai na hora e vem a fadiga pos-sono por 2 min`() {
        val cfg = ConfiguracaoDeteccao()
        val leituras = MonitorMotorista(cfg).rodar(0, 130_000, 10) { t -> if (t in 1_000 until 5_000) fechado else aberto }
        val depois = leituras.first { it.timestampMs == 5_000L }
        assertEquals(EstadoMotorista.FADIGA, depois.estado)
        assertEquals(setOf(Motivo.POS_SONO), depois.motivos)
        assertEquals(TipoEvento.SONO, leituras.eventos().single().tipo)
        assertEquals(EstadoMotorista.FADIGA, leituras.first { it.timestampMs == 124_900L }.estado)
        assertEquals(EstadoMotorista.ATENTO, leituras.first { it.timestampMs == 125_000L }.estado)
    }

    @Test
    fun `dois microssonos em cinco minutos indicam fadiga`() {
        val fechamentos = listOf(1_000L, 10_000L)
        val leituras = MonitorMotorista().rodar(0, 12_000, 10) { t ->
            if (fechamentos.any { t in it until it + 1_200 }) fechado else aberto
        }
        assertEquals(EstadoMotorista.FADIGA, leituras.last().estado)
        assertTrue(Motivo.MICROSSONOS_FREQUENTES in leituras.last().motivos)
    }

    @Test
    fun `piscar um olho so nao fecha os olhos`() {
        val piscandoUm = amostra(ear = 0.30).let { it.copy(olhoDireito = amostra(ear = 0.05).olhoDireito, piscadaDireita = 0.9) }
        val leituras = MonitorMotorista().rodar(0, 3_000, 10) { piscandoUm }
        assertTrue(leituras.all { it.metricas?.olhoFechado == false })
        val m = assertNotNull(leituras.last().metricas)
        assertTrue(m.olhoDireitoFechado)
        assertFalse(m.olhoEsquerdoFechado)
        assertEquals(OlhoAvaliado.AMBOS, m.olhoAvaliado)
    }

    @Test
    fun `com a cabeca virada vale so o olho mais visivel`() {
        // Olho esquerdo encurtado (de lado): largura 40 contra 100. Só o direito decide.
        val deLado = amostra(ear = 0.08).copy(olhoEsquerdo = seisComRazao(0.30, escala = 40.0, origemX = 200.0))
        val m = assertNotNull(MonitorMotorista().processar(Quadro(0, deLado)).metricas)
        assertEquals(OlhoAvaliado.DIREITO, m.olhoAvaliado)
        assertTrue(m.olhoFechado)
        val outroLado = amostra(ear = 0.30).copy(olhoDireito = seisComRazao(0.08, escala = 40.0))
        val m2 = assertNotNull(MonitorMotorista().processar(Quadro(0, outroLado)).metricas)
        assertEquals(OlhoAvaliado.ESQUERDO, m2.olhoAvaliado)
        assertFalse(m2.olhoFechado)
    }

    @Test
    fun `calibracao por olho usa o limiar de cada um`() {
        val monitor = MonitorMotorista(calibracao = Calibracao(0.30, 0.0, earDireito = 0.40, earEsquerdo = 0.20))
        // Limiares: direito 0,26 e esquerdo 0,13. EAR 0,20 nos dois: só o direito conta como fechado.
        val m = assertNotNull(monitor.processar(Quadro(0, amostra(ear = 0.20))).metricas)
        assertTrue(m.olhoDireitoFechado)
        assertFalse(m.olhoEsquerdoFechado)
        assertEquals(0.20, m.earDireito, 1e-9)
    }

    @Test
    fun `cronometro do estado conta a partir do primeiro frame critico (corrige P4)`() {
        val leituras = MonitorMotorista().rodar(0, 6_000, 10) { t -> if (t >= 500) fechado else aberto }
        val criticos = leituras.filter { it.estado == EstadoMotorista.CRITICO }
        assertEquals(0.0, criticos.first().segundosNoEstado)
        assertEquals((criticos.last().timestampMs - criticos.first().timestampMs) / 1000.0, criticos.last().segundosNoEstado)
    }

    @Test
    fun `microssono encerrado vira evento e conta no total`() {
        val leituras = MonitorMotorista().rodar(0, 8_000, 10) { t -> if (t in 500 until 2_000) fechado else aberto }
        assertEquals(1, leituras.last().totais.microssonos)
        assertEquals(Evento(TipoEvento.MICROSSONO, 500, 1_500), leituras.eventos().single())
        assertEquals(EstadoMotorista.ATENTO, leituras.last().estado)
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
    fun `fechamento mais curto que uma piscada e ruido e nao conta`() {
        val leituras = MonitorMotorista().rodar(0, 1_000, 30) { t -> if (t in 300 until 330) fechado else aberto }
        assertTrue(leituras.eventos().isEmpty())
        assertEquals(Totais(0, 0, 0, 0), leituras.last().totais)
    }

    @Test
    fun `rosto sumido por 2 s vira DESATENTO e evento ao voltar`() {
        val leituras = MonitorMotorista().rodar(0, 4_000, 10) { t -> if (t in 500 until 3_000) null else aberto }
        val semRosto = leituras.filter { it.timestampMs in 500 until 3_000 }
        assertTrue(semRosto.filter { it.timestampMs < 2_500 }.all { it.estado == EstadoMotorista.SEM_ROSTO })
        assertTrue(semRosto.filter { it.timestampMs >= 2_500 }.all { it.estado == EstadoMotorista.DESATENTO })
        assertEquals(setOf(Motivo.ROSTO_AUSENTE), semRosto.last().motivos)
        assertEquals(Evento(TipoEvento.ROSTO_AUSENTE, 500, 2_500), leituras.eventos().single())
        assertEquals(EstadoMotorista.ATENTO, leituras.last().estado)
    }

    @Test
    fun `olho fechado antes de perder o rosto nao vira sono depois da lacuna`() {
        val leituras = MonitorMotorista().rodar(0, 6_000, 10) { t ->
            when {
                t in 1_000 until 1_200 -> fechado
                t in 1_200 until 4_500 -> null
                else -> aberto
            }
        }
        assertTrue(leituras.eventos().none { it.tipo == TipoEvento.SONO || it.tipo == TipoEvento.MICROSSONO })
        assertTrue(leituras.none { it.estado == EstadoMotorista.CRITICO })
    }

    @Test
    fun `rosto sumido por pouco tempo nao gera evento`() {
        val leituras = MonitorMotorista().rodar(0, 2_000, 10) { t -> if (t in 500 until 1_000) null else aberto }
        assertTrue(leituras.eventos().isEmpty())
        assertTrue(leituras.none { it.estado == EstadoMotorista.DESATENTO })
    }

    @Test
    fun `cabeca virada por 2 s e DISTRAIDO, relativo a calibracao, e vira evento`() {
        val monitor = MonitorMotorista(calibracao = Calibracao(0.30, pitchReferencia = 0.0, yawReferencia = 10.0))
        val leituras = monitor.rodar(0, 5_000, 10) { t ->
            amostra(pitch = 0.0, yaw = if (t in 500 until 3_500) 35.0 else 10.0)
        }
        val primeiro = leituras.first { it.estado == EstadoMotorista.DISTRAIDO }
        assertEquals(2_500, primeiro.timestampMs)
        assertTrue(Motivo.CABECA_VIRADA in primeiro.motivos)
        assertEquals(Evento(TipoEvento.CABECA_VIRADA, 500, 3_000), leituras.eventos().single())
        assertEquals(EstadoMotorista.ATENTO, leituras.last().estado)
    }

    @Test
    fun `giro curto ou no limite nao distrai`() {
        val cfg = ConfiguracaoDeteccao()
        val curto = MonitorMotorista(cfg).rodar(0, 3_000, 10) { t -> amostra(pitch = 0.0, yaw = if (t in 500 until 1_500) -40.0 else 0.0) }
        assertTrue(curto.eventos().isEmpty())
        val limite = MonitorMotorista(cfg).rodar(0, 3_000, 10) { amostra(pitch = 0.0, yaw = cfg.giroCabecaGraus) }
        assertTrue(limite.none { it.estado == EstadoMotorista.DISTRAIDO })
    }

    @Test
    fun `segundos no estado reiniciam quando o estado muda`() {
        val leituras = MonitorMotorista().rodar(0, 1_000, 10) { aberto }
        assertEquals(0.9, leituras.last().segundosNoEstado, 1e-9)
        val monitor = MonitorMotorista()
        monitor.processar(Quadro(0, aberto))
        assertEquals(0.0, monitor.processar(Quadro(100, null)).segundosNoEstado)
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
