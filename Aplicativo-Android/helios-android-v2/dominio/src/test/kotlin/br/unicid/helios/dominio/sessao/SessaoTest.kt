package br.unicid.helios.dominio.sessao

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.MetricasOlhar
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Evento
import br.unicid.helios.dominio.tipos.PoseCabeca
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
