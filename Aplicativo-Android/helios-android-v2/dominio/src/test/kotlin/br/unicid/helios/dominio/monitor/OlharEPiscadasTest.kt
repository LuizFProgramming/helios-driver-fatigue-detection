package br.unicid.helios.dominio.monitor

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.amostra
import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.instantes
import br.unicid.helios.dominio.seisComRazao
import br.unicid.helios.dominio.tipos.AmostraFacial
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.dominio.tipos.Ponto2D
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.dominio.tipos.TipoEvento
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Análises da v0.4.0: direção do olhar (íris), piscadas no minuto e olhos semicerrados. */
class OlharEPiscadasTest {
    private val cfg = ConfiguracaoDeteccao()
    private val frente = amostra(iris = 0.5, olharVertical = 0.0)
    private val ladoDireito = amostra(iris = 0.7, olharVertical = 0.0)
    private val paraBaixo = amostra(iris = 0.5, olharVertical = -0.6)
    private val fechado = amostra(ear = 0.08, iris = 0.9, olharVertical = -0.9)

    private fun MonitorMotorista.rodar(inicioMs: Long, fimMs: Long, fps: Int, cena: (Long) -> AmostraFacial?) =
        instantes(inicioMs, fimMs, fps).map { processar(Quadro(it, cena(it))) }

    // ── Olhar ────────────────────────────────────────────────────────────────

    @Test
    fun `olhar de lado por 3 s seguidos vira DISTRAIDO e gera evento ao voltar`() {
        val leituras = MonitorMotorista(cfg).rodar(0, 6_000, 30) { t -> if (t in 1_000 until 4_500) ladoDireito else frente }
        val distraido = leituras.first { it.estado == EstadoMotorista.DISTRAIDO }
        assertTrue(distraido.timestampMs in 4_000..4_100, "entrou em ${distraido.timestampMs}")
        assertTrue(Motivo.OLHAR_DESVIADO in distraido.motivos)
        assertEquals(EstadoMotorista.ATENTO, leituras.last().estado)
        val evento = leituras.flatMap { it.eventos }.single { it.tipo == TipoEvento.OLHAR_DESVIADO }
        assertTrue(evento.duracaoMs in 3_400..3_600)
    }

    @Test
    fun `olhada rapida no espelho nao alerta nem gera evento`() {
        val leituras = MonitorMotorista(cfg).rodar(0, 4_000, 30) { t -> if (t in 1_000 until 2_500) ladoDireito else frente }
        assertTrue(leituras.all { it.estado == EstadoMotorista.ATENTO })
        assertTrue(leituras.flatMap { it.eventos }.none { it.tipo == TipoEvento.OLHAR_DESVIADO })
        assertTrue(leituras.first { it.timestampMs >= 2_000 }.metricas!!.olhar.fora)
    }

    @Test
    fun `olhar para baixo (celular no colo) tambem conta`() {
        val leituras = MonitorMotorista(cfg).rodar(0, 4_500, 30) { paraBaixo }
        assertEquals(EstadoMotorista.DISTRAIDO, leituras.last().estado)
        val olhar = leituras.last().metricas!!.olhar
        assertEquals(-0.6, olhar.desvioVertical!!, 1e-9)
        assertEquals(0.0, olhar.desvioHorizontal!!, 1e-9)
    }

    @Test
    fun `com os olhos fechados o olhar nao e medido, e sono nao vira distracao`() {
        val leituras = MonitorMotorista(cfg).rodar(0, 4_500, 30) { fechado }
        val ultima = leituras.last()
        assertEquals(EstadoMotorista.CRITICO, ultima.estado)
        assertFalse(Motivo.OLHAR_DESVIADO in ultima.motivos)
        assertNull(ultima.metricas!!.olhar.horizontal)
        assertFalse(ultima.metricas!!.olhar.fora)
    }

    @Test
    fun `com o olho baixo o olhar vertical e ignorado (palpebra puxa o blendshape para baixo)`() {
        val olhoBaixoOlhandoBaixo = amostra(ear = 0.20, iris = 0.5, olharVertical = -0.9)
        val leituras = MonitorMotorista(cfg).rodar(0, 5_000, 30) { olhoBaixoOlhandoBaixo }
        assertFalse(leituras.last().metricas!!.olhar.fora)
        assertTrue(leituras.none { it.estado == EstadoMotorista.DISTRAIDO })
    }

    @Test
    fun `olhar calibrado - celular de lado no painel, a estrada fica fora do centro da camera`() {
        val calibracao = Calibracao(0.30, 0.0, olharHorizontal = 0.7, olharVertical = -0.2)
        val estrada = amostra(iris = 0.7, olharVertical = -0.2)
        val camera = amostra(iris = 0.5, olharVertical = -0.2)
        val m = MonitorMotorista(cfg, calibracao)
        assertTrue(m.rodar(0, 5_000, 30) { estrada }.all { it.estado == EstadoMotorista.ATENTO })
        assertEquals(EstadoMotorista.DISTRAIDO, m.rodar(5_000, 9_000, 30) { camera }.last().estado)
    }

    @Test
    fun `sem iris nem blendshape de olhar nao ha medida nem distracao`() {
        val leituras = MonitorMotorista(cfg).rodar(0, 4_000, 30) { amostra() }
        val olhar = leituras.last().metricas!!.olhar
        assertNull(olhar.horizontal)
        assertNull(olhar.vertical)
        assertFalse(olhar.fora)
    }

    @Test
    fun `cabeca de lado - vale so a iris do olho visivel`() {
        val olhoEsquerdoEncurtado = AmostraFacial(
            olhoDireito = seisComRazao(0.30),
            olhoEsquerdo = seisComRazao(0.30, escala = 40.0, origemX = 200.0),
            boca = seisComRazao(0.10),
            piscadaDireita = 0.0,
            piscadaEsquerda = 0.0,
            aberturaMandibula = 0.0,
            pose = null,
            irisDireita = Ponto2D(50.0, 0.0),
            irisEsquerda = Ponto2D(200.0 + 40.0 * 0.95, 0.0),
        )
        val m = MonitorMotorista(cfg).processar(Quadro(0, olhoEsquerdoEncurtado)).metricas!!
        assertEquals(OlhoAvaliado.DIREITO, m.olhoAvaliado)
        assertEquals(0.5, m.olhar.horizontal!!, 1e-9)

        val espelhado = olhoEsquerdoEncurtado.copy(
            olhoDireito = seisComRazao(0.30, escala = 40.0),
            olhoEsquerdo = seisComRazao(0.30, origemX = 200.0),
            irisDireita = Ponto2D(40.0 * 0.95, 0.0),
            irisEsquerda = Ponto2D(250.0, 0.0),
        )
        val m2 = MonitorMotorista(cfg).processar(Quadro(0, espelhado)).metricas!!
        assertEquals(OlhoAvaliado.ESQUERDO, m2.olhoAvaliado)
        assertEquals(0.5, m2.olhar.horizontal!!, 1e-9)

        val soUmaIris = olhoEsquerdoEncurtado.copy(olhoEsquerdo = seisComRazao(0.30, origemX = 200.0), irisDireita = null)
        val m3 = MonitorMotorista(cfg).processar(Quadro(0, soUmaIris)).metricas!!
        assertEquals(OlhoAvaliado.AMBOS, m3.olhoAvaliado)
        assertEquals(0.38, m3.olhar.horizontal!!, 1e-9)

        val soADireita = soUmaIris.copy(irisDireita = Ponto2D(60.0, 0.0), irisEsquerda = null)
        assertEquals(0.6, MonitorMotorista(cfg).processar(Quadro(0, soADireita)).metricas!!.olhar.horizontal!!, 1e-9)
    }

    @Test
    fun `calibracao antiga, sem olhar, mede em relacao ao centro da camera`() {
        val antiga = Calibracao(0.30, 0.0)
        val m = MonitorMotorista(cfg, antiga).processar(Quadro(0, amostra(iris = 0.6, olharVertical = 0.1))).metricas!!
        assertEquals(0.1, m.olhar.desvioHorizontal!!, 1e-9)
        assertEquals(0.1, m.olhar.desvioVertical!!, 1e-9)
    }

    @Test
    fun `olhadas curtas que somam 10 s em 30 s viram DISTRACAO_ACUMULADA (VATS da Euro NCAP)`() {
        // 2 s fora, 1 s na estrada, repetindo: nenhuma olhada chega a 3 s, mas a soma passa de 10 s.
        val leituras = MonitorMotorista(cfg).rodar(0, 20_000, 30) { t -> if (t % 3_000 < 2_000) ladoDireito else frente }
        assertTrue(leituras.none { Motivo.OLHAR_DESVIADO in it.motivos })
        val acumulada = leituras.first { Motivo.DISTRACAO_ACUMULADA in it.motivos }
        assertEquals(EstadoMotorista.DISTRAIDO, acumulada.estado)
        assertTrue(acumulada.metricas!!.olhar.foraDaViaMs >= cfg.distracaoAcumuladaMs)
    }

    @Test
    fun `cabeca virada tambem soma no tempo fora da via`() {
        val calibracao = Calibracao(0.30, 0.0, yawReferencia = 0.0)
        val virada = amostra(pitch = 0.0, yaw = 30.0)
        val leituras = MonitorMotorista(cfg, calibracao).rodar(0, 1_500, 30) { virada }
        assertTrue(leituras.last().metricas!!.olhar.foraDaViaMs > 1_000)
        assertFalse(leituras.last().metricas!!.olhar.fora)
    }

    @Test
    fun `rosto some no meio da olhada - a contagem recomeca`() {
        val leituras = MonitorMotorista(cfg).rodar(0, 6_000, 30) { t ->
            when {
                t < 2_000 -> ladoDireito
                t < 3_500 -> null
                else -> ladoDireito
            }
        }
        val voltou = leituras.first { it.timestampMs >= 3_500 }
        assertTrue(voltou.metricas!!.olhar.foraMs < 100)
    }

    // ── Piscadas no minuto ───────────────────────────────────────────────────

    @Test
    fun `piscadas lentas em media no ultimo minuto viram fadiga`() {
        // Uma piscada de 300 ms a cada 4 s: média acima de 250 ms com bem mais de 5 piscadas.
        val leituras = MonitorMotorista(cfg).rodar(0, 30_000, 30) { t ->
            if (t % 4_000 in 1_000 until 1_300) amostra(ear = 0.08) else frente
        }
        val ultima = leituras.last()
        assertEquals(EstadoMotorista.FADIGA, ultima.estado)
        assertTrue(Motivo.PISCADAS_LENTAS in ultima.motivos)
        assertEquals(8, ultima.metricas!!.piscadasNoMinuto)
        assertTrue(ultima.metricas!!.duracaoMediaPiscadaMs in 280.0..320.0)
    }

    @Test
    fun `piscadas rapidas nao contam como fadiga e saem da janela depois de 1 minuto`() {
        val m = MonitorMotorista(cfg)
        val leituras = m.rodar(0, 30_000, 30) { t -> if (t % 4_000 in 1_000 until 1_100) amostra(ear = 0.08) else frente }
        assertTrue(leituras.none { Motivo.PISCADAS_LENTAS in it.motivos })
        assertEquals(8, leituras.last().metricas!!.piscadasNoMinuto)
        val depois = m.rodar(30_000, 95_000, 10) { frente }.last()
        assertEquals(0, depois.metricas!!.piscadasNoMinuto)
        assertEquals(0.0, depois.metricas!!.duracaoMediaPiscadaMs)
    }

    // ── Olhos semicerrados ───────────────────────────────────────────────────

    @Test
    fun `olho baixo por mais da metade do minuto vira fadiga por olhos semicerrados`() {
        val pesado = amostra(ear = 0.21, iris = 0.5, olharVertical = 0.0) // 72% do aberto padrão
        val leituras = MonitorMotorista(cfg).rodar(0, 40_000, 10) { pesado }
        val ultima = leituras.last()
        assertTrue(ultima.metricas!!.olhosSemicerrados)
        assertTrue(ultima.metricas!!.aberturaRelativa < cfg.fatorSemicerrado)
        assertEquals(EstadoMotorista.FADIGA, ultima.estado)
        assertTrue(Motivo.OLHOS_SEMICERRADOS in ultima.motivos)
    }

    @Test
    fun `apertar o olho contra o sol nao e sono`() {
        val sol = amostra(ear = 0.21, semicerrar = 0.8)
        val leituras = MonitorMotorista(cfg).rodar(0, 40_000, 10) { sol }
        assertFalse(leituras.last().metricas!!.olhosSemicerrados)
        assertEquals(0.8, leituras.last().metricas!!.semicerrar)
        assertEquals(EstadoMotorista.ATENTO, leituras.last().estado)
    }

    @Test
    fun `abertura relativa usa a calibracao de cada olho`() {
        val calibracao = Calibracao(0.30, 0.0, earDireito = 0.40, earEsquerdo = 0.20)
        val m = MonitorMotorista(cfg, calibracao).processar(Quadro(0, amostra(ear = 0.30))).metricas!!
        assertEquals((0.30 / 0.40 + 0.30 / 0.20) / 2, m.aberturaRelativa, 1e-9)
        val semOlho = MonitorMotorista(cfg, Calibracao(0.25, 0.0)).processar(Quadro(0, amostra(ear = 0.30))).metricas!!
        assertEquals(1.2, semOlho.aberturaRelativa, 1e-9)
    }

    @Test
    fun `olho fechado nao conta como semicerrado`() {
        val m = MonitorMotorista(cfg).processar(Quadro(0, amostra(ear = 0.05))).metricas
        assertNotNull(m)
        assertFalse(m.olhosSemicerrados)
    }

    @Test
    fun `movimento do carro pode ser lido sem frames da camera`() {
        val m = MonitorMotorista(cfg)
        assertFalse(m.emMovimento())
        for (t in 0L..1_900L step 100) m.registrarAceleracao(t, 0.8)
        assertTrue(m.emMovimento())
    }
}
