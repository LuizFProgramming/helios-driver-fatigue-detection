package br.unicid.helios.dominio.sessao

import br.unicid.helios.dominio.amostra
import br.unicid.helios.dominio.instantes
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.MetricasOlhar
import br.unicid.helios.dominio.monitor.MonitorMotorista
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Evento
import br.unicid.helios.dominio.tipos.PoseCabeca
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.dominio.tipos.TipoEvento
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun metricas(perclosLongo: Double = 0.1, pitch: Double? = 0.0) =
    Metricas(0.3, 0.3, 0.1, 0.0, 0.0, perclosLongo, false, false, pitch?.let { PoseCabeca(it, 0.0, 0.0) })

private fun leitura(
    t: Long,
    estado: EstadoMotorista = EstadoMotorista.ATENTO,
    eventos: List<Evento> = emptyList(),
    metricas: Metricas? = metricas(),
) = Leitura(t, estado, emptySet(), metricas, Totais(0, 0, 0, 0), 0.0, false, eventos)

class AgregadorSessaoTest {

    @Test
    fun `tempo em cada estado soma os intervalos e ignora lacunas grandes`() {
        val a = AgregadorSessao(janelaMs = 60_000, lacunaMaximaMs = 1_000)
        a.registrar(leitura(0))
        a.registrar(leitura(500))
        a.registrar(leitura(1_000, EstadoMotorista.FADIGA))
        a.registrar(leitura(1_500, EstadoMotorista.FADIGA))
        a.registrar(leitura(9_000)) // lacuna de 7,5 s: não conta para ninguém
        val r = a.resumo()
        assertEquals(1_000L, r.tempoPorEstadoMs[EstadoMotorista.ATENTO])
        assertEquals(500L, r.tempoPorEstadoMs[EstadoMotorista.FADIGA])
        assertEquals(0L, r.inicioMs)
        assertEquals(9_000L, r.fimMs)
        assertEquals(500L, r.tempoEmAlertaMs)
    }

    @Test
    fun `eventos sao contados por tipo`() {
        val a = AgregadorSessao()
        a.registrar(leitura(0, eventos = listOf(Evento(TipoEvento.PISCADA, 0, 100), Evento(TipoEvento.BOCEJO, 0, 600))))
        a.registrar(leitura(100, eventos = listOf(Evento(TipoEvento.PISCADA, 50, 120))))
        val r = a.resumo()
        assertEquals(2, r.eventosPorTipo[TipoEvento.PISCADA])
        assertEquals(1, r.eventosPorTipo[TipoEvento.BOCEJO])
        assertNull(r.eventosPorTipo[TipoEvento.MICROSSONO])
    }

    @Test
    fun `janela fecha ao passar do limite com taxas por minuto e desvio do pitch`() {
        val a = AgregadorSessao(janelaMs = 30_000)
        assertNull(a.registrar(leitura(0, metricas = metricas(0.10, pitch = -2.0))))
        assertNull(
            a.registrar(
                leitura(
                    10_000,
                    eventos = listOf(Evento(TipoEvento.PISCADA, 9_800, 100), Evento(TipoEvento.PISCADA_LONGA, 9_000, 700)),
                    metricas = metricas(0.20, pitch = 2.0),
                ),
            ),
        )
        assertNull(a.registrar(leitura(20_000, eventos = listOf(Evento(TipoEvento.BOCEJO, 19_000, 800)), metricas = null)))
        val janela = assertNotNull(a.registrar(leitura(30_000, metricas = metricas(0.9))))
        assertEquals(0L, janela.inicioMs)
        assertEquals(30_000L, janela.fimMs)
        assertEquals(0.15, janela.perclosLongo, 1e-9)
        assertEquals(4.0, janela.piscadasPorMinuto, 1e-9)
        assertEquals(400.0, janela.duracaoMediaPiscadaMs, 1e-9)
        assertEquals(2.0, janela.bocejosPorMinuto, 1e-9)
        assertEquals(2.0, janela.desvioPitchGraus, 1e-9)
        // A leitura que fechou a janela já pertence à próxima.
        val final = a.resumo()
        assertEquals(2, final.janelas.size)
        assertEquals(0.9, final.janelas.last().perclosLongo, 1e-9)
        assertEquals(30_000L, final.janelas.last().inicioMs)
        assertEquals(30_000L, final.janelas.last().fimMs)
    }

    @Test
    fun `janela sem dados fica com zeros e lacuna longa pula janelas vazias`() {
        val a = AgregadorSessao(janelaMs = 30_000)
        a.registrar(leitura(0, metricas = null))
        val primeira = assertNotNull(a.registrar(leitura(95_000, metricas = null)))
        assertEquals(0.0, primeira.perclosLongo)
        assertEquals(0.0, primeira.duracaoMediaPiscadaMs)
        assertEquals(0.0, primeira.desvioPitchGraus)
        assertEquals(90_000L, a.resumo().janelas.last().inicioMs)
    }

    @Test
    fun `resumo de sessao vazia e PERCLOS medio e maximo das janelas`() {
        val vazio = AgregadorSessao().resumo()
        assertTrue(vazio.janelas.isEmpty())
        assertEquals(0.0, vazio.perclosMedio)
        assertEquals(0.0, vazio.perclosMaximo)
        assertEquals(0L, vazio.duracaoMs)

        val a = AgregadorSessao(janelaMs = 1_000)
        a.registrar(leitura(0, metricas = metricas(0.1)))
        a.registrar(leitura(1_000, metricas = metricas(0.3)))
        val r = a.resumo()
        assertEquals(0.2, r.perclosMedio, 1e-9)
        assertEquals(0.3, r.perclosMaximo, 1e-9)
        assertEquals(1_000L, r.duracaoMs)
    }

    @Test
    fun `tempo em alerta soma so os estados de alerta`() {
        val a = AgregadorSessao(lacunaMaximaMs = 10_000)
        listOf(
            EstadoMotorista.ATENTO, EstadoMotorista.FADIGA, EstadoMotorista.CRITICO, EstadoMotorista.DESATENTO,
            EstadoMotorista.DISTRAIDO, EstadoMotorista.SEM_ROSTO, EstadoMotorista.ATENTO,
        ).forEachIndexed { i, e -> a.registrar(leitura(i * 1_000L, e)) }
        assertEquals(4_000L, a.resumo().tempoEmAlertaMs)
        assertFalse(EstadoMotorista.SEM_ROSTO.ehAlerta())
        assertTrue(EstadoMotorista.DISTRAIDO.ehAlerta())
        assertTrue(EstadoMotorista.ATENCAO.ehAlerta())
    }
}

class AgregadorSessaoBordasTest {

    @Test
    fun `janela parcial calcula as taxas pela duracao real`() {
        val a = AgregadorSessao(janelaMs = 30_000)
        a.registrar(leitura(0))
        a.registrar(leitura(10_000, eventos = listOf(Evento(TipoEvento.PISCADA, 9_000, 100), Evento(TipoEvento.BOCEJO, 9_000, 600))))
        val parcial = a.resumo().janelas.single()
        assertEquals(6.0, parcial.piscadasPorMinuto, 1e-9) // 1 em 10 s, não 1 em 30 s
        assertEquals(6.0, parcial.bocejosPorMinuto, 1e-9)
    }

    @Test
    fun `timestamp repetido, metricas sem pose e eventos de outros tipos nao quebram a janela`() {
        val a = AgregadorSessao(janelaMs = 30_000)
        a.registrar(leitura(0, metricas = metricas(0.2, pitch = null)))
        a.registrar(leitura(0, EstadoMotorista.FADIGA, eventos = listOf(Evento(TipoEvento.MICROSSONO, 0, 1_500)), metricas = null))
        val r = a.resumo()
        assertNull(r.tempoPorEstadoMs[EstadoMotorista.ATENTO])
        val j = r.janelas.single()
        assertEquals(0.2, j.perclosLongo, 1e-9)
        assertEquals(0.0, j.desvioPitchGraus)
        assertEquals(0.0, j.piscadasPorMinuto)
        assertEquals(1, r.eventosPorTipo[TipoEvento.MICROSSONO])
        assertEquals(0.0, j.fracaoOlharFora)
    }

    @Test
    fun `janela guarda a fracao de frames com olhar fora e com olhos semicerrados`() {
        val a = AgregadorSessao(janelaMs = 30_000)
        val fora = metricas().copy(olhar = MetricasOlhar(fora = true))
        val pesado = metricas().copy(olhosSemicerrados = true)
        listOf(fora, fora, pesado, metricas()).forEachIndexed { i, m -> a.registrar(leitura(i * 100L, metricas = m)) }
        val j = a.resumo().janelas.single()
        assertEquals(0.5, j.fracaoOlharFora, 1e-9)
        assertEquals(0.25, j.fracaoSemicerrado, 1e-9)
    }
}

class TempoPorEstadoNaJanelaTest {

    @Test
    fun `soma do tempo por estado na janela e a duracao util, sem a lacuna`() {
        val a = AgregadorSessao(janelaMs = 30_000, lacunaMaximaMs = 1_000)
        (0L until 10_000L step 100).forEach { a.registrar(leitura(it)) }
        (10_000L until 15_000L step 100).forEach { a.registrar(leitura(it, EstadoMotorista.FADIGA)) }
        // de 14_900 a 20_000 a câmera caiu: 5,1 s que não contam
        (20_000L until 30_000L step 100).forEach { a.registrar(leitura(it)) }
        val j = assertNotNull(a.registrar(leitura(30_000)))
        assertEquals(mapOf(EstadoMotorista.ATENTO to 20_000L, EstadoMotorista.FADIGA to 4_900L), j.tempoPorEstadoMs)
        assertEquals(30_000L - 5_100L, j.tempoPorEstadoMs.values.sum())
    }

    @Test
    fun `intervalo que cruza a fronteira e repartido entre as duas janelas`() {
        val a = AgregadorSessao(janelaMs = 30_000, lacunaMaximaMs = 1_000)
        a.registrar(leitura(0))
        a.registrar(leitura(29_800, EstadoMotorista.FADIGA))
        val fechada = assertNotNull(a.registrar(leitura(30_300)))
        a.registrar(leitura(30_500))
        assertEquals(mapOf(EstadoMotorista.FADIGA to 200L), fechada.tempoPorEstadoMs)
        val r = a.resumo()
        val parcial = r.janelas.last().tempoPorEstadoMs
        assertEquals(mapOf(EstadoMotorista.FADIGA to 300L, EstadoMotorista.ATENTO to 200L), parcial)
        assertEquals(500L, r.tempoPorEstadoMs[EstadoMotorista.FADIGA])
    }

    @Test
    fun `intervalo igual a lacuna maxima conta e maior nao conta`() {
        val a = AgregadorSessao(janelaMs = 30_000, lacunaMaximaMs = 1_000)
        a.registrar(leitura(0))
        a.registrar(leitura(1_000, EstadoMotorista.FADIGA))
        a.registrar(leitura(2_001))
        a.registrar(leitura(2_001)) // timestamp repetido também não conta
        assertEquals(mapOf(EstadoMotorista.ATENTO to 1_000L), a.resumo().janelas.single().tempoPorEstadoMs)
    }
}

class EstadoNoInicioDoEventoTest {

    @Test
    fun `estado em um instante e o da ultima leitura ate ele`() {
        val a = AgregadorSessao()
        a.registrar(leitura(1_000))
        a.registrar(leitura(1_100))
        a.registrar(leitura(1_200, EstadoMotorista.ATENCAO))
        a.registrar(leitura(1_300, EstadoMotorista.CRITICO))
        assertEquals(EstadoMotorista.ATENTO, a.estadoEm(1_150))
        assertEquals(EstadoMotorista.ATENCAO, a.estadoEm(1_200))
        assertEquals(EstadoMotorista.CRITICO, a.estadoEm(9_000))
        assertEquals(EstadoMotorista.ATENTO, a.estadoEm(500)) // antes da primeira leitura: a primeira
    }

    @Test
    fun `microssono que vira ATENCAO e depois ATENTO tem estado no inicio diferente do estado no fim`() {
        val a = AgregadorSessao()
        val microssono = Evento(TipoEvento.MICROSSONO, 1_000, 2_600)
        a.registrar(leitura(0))
        a.registrar(leitura(1_000)) // os olhos fecham
        a.registrar(leitura(2_500, EstadoMotorista.ATENCAO))
        val fim = leitura(3_600, EstadoMotorista.ATENCAO, eventos = listOf(microssono))
        a.registrar(fim)
        a.registrar(leitura(3_700))
        assertEquals(EstadoMotorista.ATENTO, a.estadoEm(microssono.inicioMs))
        assertEquals(EstadoMotorista.ATENCAO, fim.estado)
    }

    @Test
    fun `no monitor real o microssono de 2,6 s passa por ATENCAO mas comeca e termina em ATENTO`() {
        val monitor = MonitorMotorista()
        val a = AgregadorSessao()
        val aberto = amostra(ear = 0.30)
        val fechado = amostra(ear = 0.08)
        val leituras = instantes(0, 6_000, 10).map { t ->
            monitor.processar(Quadro(t, if (t in 1_000 until 3_600) fechado else aberto)).also { a.registrar(it) }
        }
        val fim = leituras.single { l -> l.eventos.any { it.tipo == TipoEvento.MICROSSONO } }
        val microssono = fim.eventos.single { it.tipo == TipoEvento.MICROSSONO }
        assertTrue(leituras.any { it.estado == EstadoMotorista.ATENCAO })
        assertEquals(1_000L, microssono.inicioMs)
        assertEquals(EstadoMotorista.ATENTO, a.estadoEm(microssono.inicioMs))
        assertEquals(EstadoMotorista.ATENTO, fim.estado) // o alarme cai na leitura em que os olhos abrem
    }
}

class DesempenhoNaJanelaTest {

    @Test
    fun `fps medio e o numero de leituras sobre a duracao real a 4, 10 e 30 quadros por segundo`() {
        for (fps in listOf(4, 10, 30)) {
            val a = AgregadorSessao(janelaMs = 30_000)
            val fechadas = instantes(0, 30_001, fps).mapNotNull { a.registrar(leitura(it)) }
            val j = fechadas.single()
            assertEquals(fps.toDouble(), j.fpsMedio, 1e-9, "fps=$fps")
            assertEquals(30_000L, j.tempoPorEstadoMs.values.sum(), "fps=$fps")
        }
    }

    @Test
    fun `latencia media e p95 da janela`() {
        val a = AgregadorSessao(janelaMs = 30_000)
        (1L..20L).reversed().forEach { a.registrar(leitura(it * 100), latenciaMs = it * 10) }
        val j = a.resumo().janelas.single()
        assertEquals(105.0, j.latenciaMediaMs, 1e-9)
        assertEquals(190L, j.latenciaP95Ms)
    }

    @Test
    fun `janela de uma leitura so tem duracao zero e fps zero`() {
        val a = AgregadorSessao()
        a.registrar(leitura(5_000), latenciaMs = 40)
        val j = a.resumo().janelas.single()
        assertEquals(0.0, j.fpsMedio)
        assertEquals(40.0, j.latenciaMediaMs)
        assertEquals(40L, j.latenciaP95Ms)
    }

    @Test
    fun `p95 e o valor na posicao teto de 0,95 n da lista ordenada`() {
        assertEquals(7L, percentil95(listOf(7L)))
        assertEquals(19L, percentil95((1L..20L).shuffled(java.util.Random(1))))
        assertEquals(95L, percentil95((1L..100L).shuffled(java.util.Random(2))))
    }

    @Test
    fun `sem leitura a media e o p95 sao zero`() {
        assertEquals(0L, percentil95(emptyList()))
        assertEquals(0.0, media(emptyList()))
        assertEquals(2.5, media(listOf(2L, 3L)))
    }
}

class CnnNaJanelaTest {

    private fun comCnn(t: Long, dir: Double?, esq: Double?, olhoFechado: Boolean) = leitura(
        t,
        metricas = metricas().copy(olhoFechado = olhoFechado, cnnFechadoDireito = dir, cnnFechadoEsquerdo = esq),
    )

    @Test
    fun `janela guarda a media da CNN e a fracao em que ela discordou do olho fechado do app`() {
        val a = AgregadorSessao()
        a.registrar(comCnn(0, 0.9, 0.7, olhoFechado = false)) // média 0,8: fechado pela CNN, aberto pelo app
        a.registrar(comCnn(100, 0.2, 0.2, olhoFechado = true)) // 0,2: aberto pela CNN, fechado pelo app
        a.registrar(comCnn(200, 0.6, 0.4, olhoFechado = true)) // 0,5: fechado nos dois (corte inclusivo)
        a.registrar(comCnn(300, 0.3, 0.1, olhoFechado = false)) // 0,2: aberto nos dois
        a.registrar(comCnn(350, 0.9, 0.9, olhoFechado = false)) // 0,9: fechado pela CNN, aberto pelo app
        a.registrar(comCnn(400, null, null, olhoFechado = true)) // CNN desligada neste quadro: não entra
        a.registrar(leitura(500, metricas = null)) // sem rosto: não entra
        val j = a.resumo().janelas.single()
        assertEquals((0.8 + 0.2 + 0.5 + 0.2 + 0.9) / 5, assertNotNull(j.cnnFechadoMedia), 1e-9)
        assertEquals(3.0 / 5, assertNotNull(j.cnnDiscordancia), 1e-9)
    }

    @Test
    fun `janela sem a CNN fica com os dois campos vazios`() {
        val a = AgregadorSessao()
        a.registrar(leitura(0))
        a.registrar(comCnn(100, 0.9, null, olhoFechado = false)) // um olho só não conta
        val j = a.resumo().janelas.single()
        assertNull(j.cnnFechadoMedia)
        assertNull(j.cnnDiscordancia)
    }
}

class PoliticaEvidenciaTest {

    @Test
    fun `captura ao entrar num estado de alerta e respeita o intervalo minimo`() {
        val p = PoliticaEvidencia(intervaloMinimoMs = 30_000)
        assertFalse(p.avaliar(leitura(0)))
        assertTrue(p.avaliar(leitura(1_000, EstadoMotorista.CRITICO)))
        assertFalse(p.avaliar(leitura(1_100, EstadoMotorista.CRITICO))) // continua no mesmo estado
        assertFalse(p.avaliar(leitura(2_000, EstadoMotorista.DESATENTO))) // mudou, mas cedo demais
        assertFalse(p.avaliar(leitura(40_000, EstadoMotorista.DESATENTO))) // não é entrada nova
        assertTrue(p.avaliar(leitura(41_000, EstadoMotorista.DISTRAIDO)))
        assertFalse(p.avaliar(leitura(80_000, EstadoMotorista.ATENCAO))) // aviso curto não tira foto
    }

    @Test
    fun `estados fora da lista nunca capturam`() {
        val p = PoliticaEvidencia(estados = setOf(EstadoMotorista.CRITICO))
        assertFalse(p.avaliar(leitura(0, EstadoMotorista.FADIGA)))
        assertFalse(p.avaliar(leitura(1, EstadoMotorista.SEM_ROSTO)))
        assertTrue(p.avaliar(leitura(2, EstadoMotorista.CRITICO)))
    }
}
