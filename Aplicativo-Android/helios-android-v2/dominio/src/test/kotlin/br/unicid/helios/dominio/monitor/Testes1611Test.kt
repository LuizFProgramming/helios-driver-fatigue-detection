package br.unicid.helios.dominio.monitor

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.amostra
import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.classificacao.Classificador
import br.unicid.helios.dominio.classificacao.Indicadores
import br.unicid.helios.dominio.eventos.DetectorSustentado
import br.unicid.helios.dominio.eventos.Fase
import br.unicid.helios.dominio.instalacao.Zona
import br.unicid.helios.dominio.instalacao.ZonaOlhar
import br.unicid.helios.dominio.instalacao.zonaMaisProxima
import br.unicid.helios.dominio.instantes
import br.unicid.helios.dominio.tipos.AmostraFacial
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Evento
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.dominio.tipos.TipoEvento
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Casos tirados dos testes no carro de 16/11/2026 (sessões S6 e S7 do TCC). */
class Testes1611Test {

    private val aberto = amostra(ear = 0.30, pitch = 0.0)
    private val fechado = amostra(ear = 0.08, pitch = 0.0)
    private val estrada = Calibracao(0.30, pitchReferencia = 0.0, yawReferencia = 0.0)

    private fun MonitorMotorista.rodar(de: Long, ate: Long, fps: Int = 12, cena: (Long) -> AmostraFacial?) =
        instantes(de, ate, fps).map { processar(Quadro(it, cena(it))) }

    private fun List<Leitura>.eventos() = flatMap { it.eventos }
    private fun List<Leitura>.entre(de: Long, ate: Long) = filter { it.timestampMs in de until ate }

    @Test
    fun `interrupcao curta nao encerra a condicao e a duracao fica parada`() {
        val d = DetectorSustentado(toleranciaMs = 200)
        assertEquals(Fase.EmCurso(0, 0), d.atualizar(0, true))
        assertEquals(Fase.EmCurso(0, 1_080), d.atualizar(1_080, false))
        assertEquals(Fase.EmCurso(0, 1_160), d.atualizar(1_160, true))
        assertEquals(Fase.EmCurso(0, 2_000), d.atualizar(2_000, false))
        assertEquals(Fase.EmCurso(0, 2_000), d.atualizar(2_199, false))
        assertEquals(Fase.Encerrada(0, 2_000), d.atualizar(2_200, false))
        assertEquals(Fase.Inativa, d.atualizar(2_300, false))
    }

    @Test
    fun `reiniciar no meio da interrupcao esquece a condicao`() {
        val d = DetectorSustentado(toleranciaMs = 200)
        d.atualizar(0, true)
        d.atualizar(100, false)
        d.reiniciar()
        assertEquals(Fase.Inativa, d.atualizar(150, false))
    }

    /** S7, 19:42:38 a 19:42:47: o app registrou 5 microssonos e 1 piscada longa, e só 189 ms de CRÍTICO. */
    @Test
    fun `fechamento longo com aberturas de um quadro vira um SONO so e fica CRITICO`() {
        val fechamentos = listOf(
            0L to 1_687L, 2_265L to 3_441L, 3_533L to 4_198L, 4_366L to 5_369L, 5_446L to 7_150L, 7_385L to 8_389L,
        )
        val l = MonitorMotorista().rodar(0, 12_000) { t ->
            if (fechamentos.any { (a, b) -> t - 1_000 in a until b }) fechado else aberto
        }
        assertEquals(1, l.eventos().count { it.tipo == TipoEvento.SONO })
        val critico = l.filter { it.estado == EstadoMotorista.CRITICO }
        assertTrue(critico.isNotEmpty() && critico.all { Motivo.SONO in it.motivos })
        assertTrue(critico.last().timestampMs - critico.first().timestampMs >= 2_000)
    }

    @Test
    fun `olho fechado em 80 por cento dos ultimos 5 s e CRITICO mesmo sem 3 s seguidos`() {
        val cfg = ConfiguracaoDeteccao()
        val calmo = Indicadores(
            olhosFechadosMs = 0, cabecaCaida = false, perclosLongo = 0.0,
            bocejosRecentes = 0, piscadasLongasRecentes = 0,
        )
        val classificador = Classificador(cfg)
        fun estado(i: Indicadores) = classificador.classificar(i).estado
        assertEquals(4_000, cfg.perclosCriticoMs)
        val fechando = calmo.copy(olhosFechadosMs = 100)
        assertEquals(EstadoMotorista.ATENTO, estado(fechando.copy(olhosFechadosNaJanelaMs = 3_999)))
        val sono = classificador.classificar(fechando.copy(olhosFechadosNaJanelaMs = 4_000))
        assertEquals(setOf(Motivo.SONO), sono.motivos)
        // Com o olho aberto agora, o PERCLOS alto não segura o alarme.
        assertEquals(EstadoMotorista.ATENTO, estado(calmo.copy(olhosFechadosNaJanelaMs = 4_500)))

        // 1,3 s fechado e 0,3 s aberto, repetidos: nenhum fechamento chega a 1,5 s e a pausa passa da tolerância.
        val l = MonitorMotorista().rodar(0, 9_000) { t ->
            if (t >= 1_000 && (t - 1_000) % 1_600 < 1_300) fechado else aberto
        }
        assertTrue(l.eventos().none { it.tipo == TipoEvento.SONO })
        assertTrue(l.any { it.estado == EstadoMotorista.CRITICO })
    }

    /** S6, 19:29:56: microssono de 1,45 s e, 0,6 s depois, 13,4 s sem rosto contados como DESATENTO. */
    @Test
    fun `rosto que some logo depois de um fechamento longo e cochilo - CRITICO e cabeca caida`() {
        val l = MonitorMotorista().rodar(0, 8_000) { t ->
            when {
                t in 1_000 until 2_200 -> fechado
                t in 2_750 until 6_000 -> null
                else -> aberto
            }
        }
        val inicio = l.first { it.metricas == null }.timestampMs
        assertTrue(l.entre(inicio, inicio + 1_000).all { it.estado == EstadoMotorista.SEM_ROSTO })
        val depois = l.entre(inicio + 1_000, 6_000)
        assertTrue(depois.all { it.estado == EstadoMotorista.CRITICO && it.motivos == setOf(Motivo.CABECA_CAIDA) })
        val evento = l.eventos().single { it.tipo == TipoEvento.CABECA_CAIDA }
        assertEquals(inicio, evento.inicioMs)
        assertTrue(l.eventos().none { it.tipo == TipoEvento.ROSTO_AUSENTE })
        assertEquals(EstadoMotorista.FADIGA, l.last().estado) // pós-sono
    }

    @Test
    fun `fechamento longo antigo nao faz o rosto sumido virar cochilo`() {
        val l = MonitorMotorista().rodar(0, 9_000) { t ->
            when {
                t in 1_000 until 2_200 -> fechado
                t in 5_000 until 8_000 -> null
                else -> aberto
            }
        }
        assertEquals(EstadoMotorista.DESATENTO, l.last { it.metricas == null }.estado)
        assertEquals(TipoEvento.ROSTO_AUSENTE, l.eventos().last().tipo)
    }

    @Test
    fun `sinal de sono vale ate exatamente 1,5 s antes de o rosto sumir`() {
        fun estadoComRostoSumindoEm(sumiu: Long): EstadoMotorista {
            val m = MonitorMotorista()
            instantes(0, 2_100, 10).forEach { m.processar(Quadro(it, fechado)) } // último sinal de sono em 2.000
            m.processar(Quadro(sumiu, null))
            return m.processar(Quadro(sumiu + 1_000, null)).estado
        }
        assertEquals(EstadoMotorista.CRITICO, estadoComRostoSumindoEm(3_500))
        assertEquals(EstadoMotorista.SEM_ROSTO, estadoComRostoSumindoEm(3_501))
    }

    @Test
    fun `olhada para a zona com o rosto sumido distrai em exatamente 5 s`() {
        val espelho = ZonaOlhar(Zona.ESPELHO_ESQUERDO, 31.3, -1.2, null, null, 6.1, 5.0, 0.5)
        val m = MonitorMotorista(calibracao = estrada, zonas = listOf(espelho))
        m.processar(Quadro(0, amostra(pitch = 0.0, yaw = 32.0)))
        m.processar(Quadro(100, null))
        assertEquals(EstadoMotorista.SEM_ROSTO, m.processar(Quadro(5_099, null)).estado)
        assertEquals(EstadoMotorista.DISTRAIDO, m.processar(Quadro(5_100, null)).estado)
    }

    @Test
    fun `cochilo com rosto perdido nao e suspenso pela manobra`() {
        val monitor = MonitorMotorista()
        monitor.iniciarManobra(0)
        val l = monitor.rodar(0, 5_000) { t -> if (t < 1_500) fechado else null }
        assertEquals(EstadoMotorista.CRITICO, l.last().estado)
    }

    @Test
    fun `rosto que some com a cabeca virada e DISTRAIDO, de frente continua DESATENTO`() {
        val virado = amostra(pitch = 0.0, yaw = 30.0)
        val l = MonitorMotorista(calibracao = estrada).rodar(0, 4_000) { t -> if (t < 1_000) virado else null }
        assertEquals(EstadoMotorista.DISTRAIDO, l.last().estado)
        assertEquals(setOf(Motivo.CABECA_VIRADA), l.last().motivos)
        assertTrue(l.entre(1_000, 2_900).all { it.estado == EstadoMotorista.SEM_ROSTO })

        val frente = MonitorMotorista(calibracao = estrada).rodar(0, 4_000) { t -> if (t < 1_000) aberto else null }
        assertEquals(EstadoMotorista.DESATENTO, frente.last().estado)
    }

    /** S6, 19:29:27: giro de 32 graus até o espelho esquerdo (31,3), rosto perdido por 2,7 s e DESATENTO. */
    @Test
    fun `rosto que some olhando para o espelho segue a tolerancia da zona`() {
        val espelho = ZonaOlhar(Zona.ESPELHO_ESQUERDO, 31.3, -1.2, null, null, 6.1, 5.0, 0.5)
        val noEspelho = amostra(pitch = 0.0, yaw = 32.0)
        fun cena(volta: Long) = { t: Long -> if (t < 500 || t >= volta) (if (t < 500) noEspelho else aberto) else null }

        val curta = MonitorMotorista(calibracao = estrada, zonas = listOf(espelho)).rodar(0, 4_000, cena = cena(3_200))
        assertTrue(curta.none { it.estado == EstadoMotorista.DESATENTO || it.estado == EstadoMotorista.DISTRAIDO })
        assertTrue(curta.eventos().isEmpty())

        val longa = MonitorMotorista(calibracao = estrada, zonas = listOf(espelho)).rodar(0, 8_000, cena = cena(7_000))
        val distraido = longa.first { it.estado == EstadoMotorista.DISTRAIDO }
        assertEquals(setOf(Motivo.OLHAR_ZONA), distraido.motivos)
        assertTrue(distraido.timestampMs - 500 in 5_000..5_100)
        assertEquals(TipoEvento.ROSTO_AUSENTE, longa.eventos().single().tipo)
    }

    /** S7: três giros a menos de 5 graus do painel não casaram com a zona gravada com espalhamento 5 e 0,08. */
    @Test
    fun `espalhamento minimo vale tambem para perfil gravado com minimo menor`() {
        val painel = ZonaOlhar(Zona.PAINEL, 30.87, 0.97, 0.021, 0.047, 5.0, 5.0, 0.08)
        val inst = ConfiguracaoDeteccao().instalacao
        val (graus, olhar) = inst.espalhamentoMinGraus to inst.espalhamentoMinOlhar
        assertNull(zonaMaisProxima(25.0, 5.0, 0.12, 0.10, listOf(painel)))
        assertEquals(
            Zona.PAINEL,
            zonaMaisProxima(25.0, 5.0, 0.12, 0.10, listOf(painel), graus, olhar),
        )
        assertNull(zonaMaisProxima(10.0, 5.0, null, null, listOf(painel), graus, olhar))
    }

    @Test
    fun `evento de rosto ausente sem giro nao leva angulo`() {
        val l = MonitorMotorista().rodar(0, 4_000) { t -> if (t in 500 until 3_000) null else aberto }
        assertEquals(listOf<Double?>(null), l.eventos().map(Evento::yawMaximoGraus))
    }
}
