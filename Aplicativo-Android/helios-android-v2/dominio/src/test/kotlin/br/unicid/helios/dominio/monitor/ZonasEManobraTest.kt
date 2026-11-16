package br.unicid.helios.dominio.monitor

import br.unicid.helios.dominio.amostra
import br.unicid.helios.dominio.calibracao.Calibracao
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** ADR-014: olhadas para zonas ligadas à direção (espelhos e painel) e a manobra. 10 quadros por segundo. */
class ZonasEManobraTest {

    private val estrada = Calibracao(0.30, pitchReferencia = 0.0, yawReferencia = 0.0)
    private val espelhoEsquerdo = ZonaOlhar(Zona.ESPELHO_ESQUERDO, -40.0, 0.0, null, null, 8.0, 8.0, 0.05)
    private val painel = ZonaOlhar(Zona.PAINEL, 0.0, -10.0, null, -0.5, 8.0, 8.0, 0.1)

    private fun monitor(zonas: List<ZonaOlhar> = listOf(espelhoEsquerdo, painel)) =
        MonitorMotorista(calibracao = estrada, zonas = zonas)

    private fun MonitorMotorista.rodar(de: Long, ate: Long, amostraEm: (Long) -> AmostraFacial?) =
        instantes(de, ate, 10).map { t -> processar(Quadro(t, amostraEm(t))) }

    private companion object {
        const val ESPELHO = -40.0
    }

    private fun List<Leitura>.estadoEm(t: Long) = first { it.timestampMs == t }.estado

    @Test
    fun `zona mais proxima ganha e olhar fora de todas nao e zona`() {
        val zonas = listOf(espelhoEsquerdo, ZonaOlhar(Zona.RETROVISOR, -20.0, 10.0, null, null, 15.0, 15.0, 0.05))
        assertEquals(Zona.ESPELHO_ESQUERDO, zonaMaisProxima(-35.0, 2.0, null, null, zonas))
        assertEquals(Zona.RETROVISOR, zonaMaisProxima(-22.0, 9.0, null, null, zonas))
        assertNull(zonaMaisProxima(40.0, 0.0, null, null, zonas))
        assertNull(zonaMaisProxima(-40.0, 0.0, null, null, emptyList()))
        assertEquals(Zona.ESPELHO_ESQUERDO, zonaMaisProxima(-32.0, 0.0, null, null, zonas)) // distância 1: ainda dentro
        assertNull(zonaMaisProxima(-34.34, 6.2, null, null, listOf(espelhoEsquerdo))) // 0,5 + 0,6: fora
    }

    @Test
    fun `zonas valem relativas a referencia da estrada e podem ser trocadas`() {
        val calibrado = MonitorMotorista(calibracao = Calibracao(0.30, pitchReferencia = 5.0, yawReferencia = 10.0))
        calibrado.definirZonas(listOf(espelhoEsquerdo))
        val l = calibrado.rodar(0, 3_000) { t -> amostra(pitch = 5.0, yaw = if (t >= 500) -30.0 else 10.0) }
        assertEquals(Zona.ESPELHO_ESQUERDO, l.last().metricas?.olhar?.zona)
        assertTrue(l.none { it.estado == EstadoMotorista.DISTRAIDO })
    }

    @Test
    fun `olhar medido longe do olhar da zona tira a olhada da zona`() {
        val comOlhar = espelhoEsquerdo.copy(olharHorizontal = -0.2, olharVertical = 0.0)
        assertEquals(Zona.ESPELHO_ESQUERDO, zonaMaisProxima(-40.0, 0.0, -0.21, 0.0, listOf(comOlhar)))
        assertNull(zonaMaisProxima(-40.0, 0.0, 0.2, 0.0, listOf(comOlhar)))
        assertNull(zonaMaisProxima(-40.0, 0.0, -0.2, 0.3, listOf(comOlhar)))
        // Olhar não medido (olho baixo): vale só a cabeça.
        assertEquals(Zona.ESPELHO_ESQUERDO, zonaMaisProxima(-40.0, 0.0, null, null, listOf(comOlhar)))
    }

    @Test
    fun `cabeca virada para o espelho so distrai depois de 3 s, com o motivo e a zona`() {
        val curta = monitor().rodar(0, 4_000) { t ->
            amostra(pitch = 0.0, yaw = if (t in 500 until 3_200) ESPELHO else 0.0)
        }
        assertTrue(curta.none { it.estado == EstadoMotorista.DISTRAIDO })
        assertEquals(Zona.ESPELHO_ESQUERDO, curta.first { it.timestampMs == 1_000L }.metricas?.olhar?.zona)
        assertNull(curta.first { it.timestampMs == 3_500L }.metricas?.olhar?.zona)
        assertTrue(curta.flatMap { it.eventos }.none { it.tipo == TipoEvento.CABECA_VIRADA })
        val longa = monitor().rodar(0, 5_000) { t ->
            amostra(pitch = 0.0, yaw = if (t in 500 until 4_500) ESPELHO else 0.0)
        }
        assertEquals(EstadoMotorista.ATENTO, longa.estadoEm(3_400))
        val distraido = longa.first { it.estado == EstadoMotorista.DISTRAIDO }
        assertEquals(3_500, distraido.timestampMs)
        assertEquals(setOf(Motivo.OLHAR_ZONA), distraido.motivos)
    }

    @Test
    fun `sem zonas no perfil a mesma cabeca virada distrai em 2 s, como antes`() {
        val l = monitor(zonas = emptyList()).rodar(0, 4_000) { t ->
            amostra(pitch = 0.0, yaw = if (t in 500 until 3_200) ESPELHO else 0.0)
        }
        assertEquals(2_500, l.first { it.estado == EstadoMotorista.DISTRAIDO }.timestampMs)
        assertTrue(Motivo.CABECA_VIRADA in l.first { it.estado == EstadoMotorista.DISTRAIDO }.motivos)
    }

    @Test
    fun `olhadas curtas para o espelho nao somam no tempo fora da via, para outra direcao somam`() {
        // 1,5 s fora e 0,5 s na estrada, por 30 s: 22 s fora, nenhuma olhada passa de 2 s.
        fun padrao(yaw: Double) = { t: Long -> amostra(pitch = 0.0, yaw = if (t % 2_000 < 1_500) yaw else 0.0) }
        val espelho = monitor().rodar(0, 30_000, padrao(-40.0))
        assertTrue(espelho.none { it.estado == EstadoMotorista.DISTRAIDO })
        val outra = monitor().rodar(0, 30_000, padrao(40.0))
        assertTrue(outra.any { Motivo.DISTRACAO_ACUMULADA in it.motivos })
    }

    @Test
    fun `olhadas para o painel tambem ficam fora da soma, mas cabeca caida continua sendo sono`() {
        // Olhos para baixo (olhar vertical -0,5) por 1,5 s a cada 2 s, por 30 s.
        val l = monitor().rodar(0, 30_000) { t ->
            if (t % 2_000 < 1_500) amostra(pitch = -10.0, olharVertical = -0.5) else amostra(pitch = 0.0)
        }
        assertTrue(l.none { it.estado == EstadoMotorista.DISTRAIDO })
        assertEquals(Zona.PAINEL, l.first { it.timestampMs == 1_000L }.metricas?.olhar?.zona)
        val caindo = monitor(listOf(painel.copy(pitch = -30.0)))
            .rodar(0, 3_000) { amostra(pitch = -30.0, olharVertical = -0.5) }
        assertEquals(EstadoMotorista.CRITICO, caindo.last().estado)
    }

    @Test
    fun `evento de giro e de rosto ausente guardam o maior giro da cabeca`() {
        val giro = monitor(zonas = emptyList()).rodar(0, 5_000) { t ->
            amostra(pitch = 0.0, yaw = if (t in 500 until 3_500) 30.0 + (t - 500) / 100.0 else 0.0)
        }
        val evento = giro.flatMap { it.eventos }.single()
        assertEquals(Evento(TipoEvento.CABECA_VIRADA, 500, 3_000, yawMaximoGraus = 59.0), evento)
        val virouSumiu = monitor(zonas = emptyList()).rodar(0, 6_000) { t ->
            when {
                t < 500 -> amostra(pitch = 0.0, yaw = 0.0)
                t < 1_500 -> amostra(pitch = 0.0, yaw = 50.0 + (t - 500) / 100.0)
                t < 4_000 -> null
                else -> amostra(pitch = 0.0, yaw = 0.0)
            }
        }
        val ausente = virouSumiu.flatMap { it.eventos }.single { it.tipo == TipoEvento.ROSTO_AUSENTE }
        assertEquals(59.0, ausente.yawMaximoGraus)
        val semGiro = monitor(zonas = emptyList()).rodar(0, 4_000) { t ->
            if (t in 500 until 3_000) null else amostra(pitch = 0.0)
        }
        assertNull(semGiro.flatMap { it.eventos }.single().yawMaximoGraus)
    }

    @Test
    fun `manobra suspende distracao e desatencao por 30 s, mas nunca o sono`() {
        val m = monitor(zonas = emptyList())
        m.processar(Quadro(0, amostra(pitch = 0.0)))
        m.iniciarManobra(1_000)
        val virado = m.rodar(1_100, 10_000) { amostra(pitch = 0.0, yaw = 40.0) }
        assertTrue(virado.none { it.estado == EstadoMotorista.DISTRAIDO })
        assertEquals(29_900, virado.first().manobraRestanteMs)
        val semRosto = m.rodar(10_000, 15_000) { null }
        assertTrue(semRosto.none { it.estado == EstadoMotorista.DESATENTO })
        val dormindo = m.rodar(15_000, 19_000) { amostra(ear = 0.05, ecf = 0.9, pitch = 0.0) }
        assertEquals(EstadoMotorista.CRITICO, dormindo.last().estado)
        val depois = m.rodar(19_000, 35_000) { t -> amostra(pitch = 0.0, yaw = if (t >= 31_000) 40.0 else 0.0) }
        val fim = depois.first { it.timestampMs == 31_000L }.eventos.single { it.tipo == TipoEvento.MANOBRA }
        assertEquals(Evento(TipoEvento.MANOBRA, 1_000, 30_000), fim)
        assertNull(depois.last().manobraRestanteMs)
        assertEquals(EstadoMotorista.DISTRAIDO, depois.last().estado) // virou de novo depois da manobra
    }

    @Test
    fun `tocar de novo recomeca os 30 s e a manobra vira um evento so`() {
        val m = monitor()
        m.iniciarManobra(0)
        m.rodar(0, 20_000) { amostra(pitch = 0.0) }
        m.iniciarManobra(20_000)
        val l = m.rodar(20_000, 51_000) { amostra(pitch = 0.0) }
        val manobras = l.flatMap { it.eventos }.filter { it.tipo == TipoEvento.MANOBRA }
        assertEquals(listOf(Evento(TipoEvento.MANOBRA, 0, 50_000)), manobras)
        assertFalse(l.last().manobraRestanteMs != null)
    }
}
