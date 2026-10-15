package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.MetricasOlhar
import br.unicid.helios.dominio.monitor.OlhoAvaliado
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.dominio.tipos.Ponto2D
import br.unicid.helios.dominio.tipos.PoseCabeca
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VisorTest {

    @Test
    fun `eixos da cabeca projetam as colunas da rotacao com y para baixo`() {
        val identidade = DoubleArray(16).also { for (i in 0..3) it[i * 5] = 1.0 }
        val e = eixosDaCabeca(identidade)
        assertEquals(Ponto2D(1.0, -0.0), e.x)
        assertEquals(Ponto2D(0.0, -1.0), e.y)
        assertEquals(Ponto2D(0.0, -0.0), e.z)
    }

    @Test
    fun `indices do nariz e pontos usados existem na malha`() {
        assertTrue((MalhaVisor.IRIS_DIREITA + MalhaVisor.IRIS_ESQUERDA).all { it in 468 until IndicesFaceMesh.TOTAL_PONTOS })
        assertTrue((MalhaVisor.NARIZ + MalhaVisor.NARINAS + MalhaVisor.PONTA_DO_NARIZ).all { it in 0 until IndicesFaceMesh.TOTAL_PONTOS })
        assertEquals(6, MalhaVisor.olhoDireito().size)
        assertEquals(6, MalhaVisor.olhoEsquerdo().size)
        assertEquals(6, MalhaVisor.boca().size)
    }

    @Test
    fun `painel de metricas mostra cada olho, boca, cabeca e PERCLOS`() {
        val m = Metricas(
            earBruto = 0.2, earExibicao = 0.2, marExibicao = 0.12, ecf = 0.4, perclosCurto = 0.3, perclosLongo = 0.123,
            olhoFechado = true, bocaAberta = false, pose = PoseCabeca(-3.4, 12.0, 1.0),
            earDireito = 0.18, earEsquerdo = 0.22, ecfDireito = 0.5, ecfEsquerdo = 0.3,
            olhoDireitoFechado = true, olhoEsquerdoFechado = false, olhoAvaliado = OlhoAvaliado.AMBOS,
            olhosFechadosMs = 1_800, aberturaMandibula = 0.05,
        )
        val leitura = Leitura(0, EstadoMotorista.ATENCAO, setOf(Motivo.OLHOS_FECHANDO), m, Totais(0, 0, 0, 0), 0.3, false, emptyList())
        assertEquals(
            listOf(
                "ATENÇÃO: olhos fechando",
                "Olho direito: EAR 0,18 · piscada 0,50 · fechado",
                "Olho esquerdo: EAR 0,22 · piscada 0,30 · aberto",
                "Avaliado: os dois olhos · fechados há 1,8 s",
                "Boca: MAR 0,12 · mandíbula 0,05",
                "Cabeça: pitch -3° · yaw 12° · roll 1°",
                "PERCLOS: 30% em 5 s · 12% em 60 s",
                "Olhar: não medido",
                "Fora da via: 0,0 s nos últimos 30 s",
                "Piscadas: 0 no último minuto · média 0 ms",
                "Abertura: 100% da calibrada · apertar 0,00 · normais (0% do minuto)",
            ),
            linhasDeMetricas(leitura),
        )
    }

    @Test
    fun `painel mostra olhar, piscadas e olhos semicerrados`() {
        val base = Metricas(0.2, 0.2, 0.1, 0.0, 0.0, 0.0, false, false, null)
        val m = base.copy(
            olhar = MetricasOlhar(0.62, -0.4, 0.12, -0.4, fora = true, foraMs = 1_250, foraDaViaMs = 4_000),
            piscadasNoMinuto = 14,
            duracaoMediaPiscadaMs = 182.6,
            aberturaRelativa = 0.72,
            semicerrar = 0.1,
            olhosSemicerrados = true,
            fracaoSemicerrado = 0.35,
        )
        val leitura = Leitura(0, EstadoMotorista.ATENTO, emptySet(), m, Totais(0, 0, 0, 0), 0.0, false, emptyList())
        val linhas = linhasDeMetricas(leitura).takeLast(4)
        assertEquals(
            listOf(
                "Olhar: íris 0,62 (+0,12) · vertical -0,40 (-0,40) · fora há 1,3 s",
                "Fora da via: 4,0 s nos últimos 30 s",
                "Piscadas: 14 no último minuto · média 183 ms",
                "Abertura: 72% da calibrada · apertar 0,10 · semicerrados (35% do minuto)",
            ),
            linhas,
        )
        val soVertical = base.copy(olhar = MetricasOlhar(vertical = 0.1))
        val texto = linhasDeMetricas(leitura.copy(metricas = soVertical))
        assertTrue("Olhar: vertical 0,10 (+0,00) · na via" in texto)
        val soIris = linhasDeMetricas(leitura.copy(metricas = base.copy(olhar = MetricasOlhar(horizontal = 0.5))))
        assertTrue("Olhar: íris 0,50 (+0,00) · na via" in soIris)
    }

    @Test
    fun `sem rosto e sem pose o painel avisa`() {
        val semRosto = Leitura(0, EstadoMotorista.SEM_ROSTO, emptySet(), null, Totais(0, 0, 0, 0), 0.0, false, emptyList())
        assertEquals(listOf("SEM ROSTO: ajuste a câmera", "Rosto não encontrado"), linhasDeMetricas(semRosto))
        val m = Metricas(0.3, 0.3, 0.1, 0.0, 0.0, 0.0, false, false, null, olhoAvaliado = OlhoAvaliado.DIREITO)
        val leitura = Leitura(0, EstadoMotorista.ATENTO, emptySet(), m, Totais(0, 0, 0, 0), 0.0, false, emptyList())
        val linhas = linhasDeMetricas(leitura)
        assertTrue("Avaliado: só o olho direito (cabeça de lado)" in linhas)
        assertTrue("Cabeça: sem pose" in linhas)
        val esq = linhasDeMetricas(leitura.copy(metricas = m.copy(olhoAvaliado = OlhoAvaliado.ESQUERDO)))
        assertTrue("Avaliado: só o olho esquerdo (cabeça de lado)" in esq)
    }
}
