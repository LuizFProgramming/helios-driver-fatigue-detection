package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.calibracao.Desvios
import br.unicid.helios.dominio.tipos.PoseCabeca
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NotaEZonaTest {

    private val cfg = ConfiguracaoInstalacao()
    private val parado = Desvios(pitch = 1.0, yaw = 2.0, olharHorizontal = 0.01, olharVertical = 0.02)

    @Test
    fun `instalacao perfeita vale 100 e e boa`() {
        val esperada = NotaInstalacao(100, Qualidade.BOA, PartesNota(60, 25, 15))
        assertEquals(esperada, notaInstalacao(1.0, parado, zonasGravadas = 4, cfg))
    }

    @Test
    fun `verificacoes valem 60, estabilidade 25 e zonas 15`() {
        assertEquals(85, notaInstalacao(1.0, parado, zonasGravadas = 0, cfg).pontos)
        assertEquals(70, notaInstalacao(0.5, parado, zonasGravadas = 4, cfg).pontos)
        // desvio de 8 graus ou mais zera a estabilidade; de 5,5 (meio caminho entre 3 e 8) vale a metade
        assertEquals(75, notaInstalacao(1.0, parado.copy(yaw = 8.0), zonasGravadas = 4, cfg).pontos)
        assertEquals(88, notaInstalacao(1.0, parado.copy(pitch = 5.5), zonasGravadas = 4, cfg).pontos)
        assertEquals(51, notaInstalacao(0.6, parado.copy(yaw = 9.0), zonasGravadas = 4, cfg).pontos)
    }

    @Test
    fun `as partes mostram o que puxou a nota para baixo`() {
        val nota = notaInstalacao(0.5, parado.copy(yaw = 8.0), zonasGravadas = 1, cfg)
        assertEquals(listOf(30, 0, 4), listOf(nota.partes.verificacoes, nota.partes.estabilidade, nota.partes.zonas))
        assertEquals(25, notaInstalacao(1.0, parado, zonasGravadas = 0, cfg).partes.estabilidade)
        assertEquals(Qualidade.REFAZER, nota.qualidade)
    }

    @Test
    fun `qualidade boa a partir de 80, aceitavel a partir de 60, abaixo disso refazer`() {
        assertEquals(Qualidade.BOA, qualidade(80, cfg))
        assertEquals(Qualidade.ACEITAVEL, qualidade(79, cfg))
        assertEquals(Qualidade.ACEITAVEL, qualidade(60, cfg))
        assertEquals(Qualidade.REFAZER, qualidade(59, cfg))
    }

    private val estrada =
        Calibracao(0.3, pitchReferencia = -10.0, yawReferencia = 5.0, olharHorizontal = 0.5, olharVertical = 0.1)

    @Test
    fun `zona guarda centro e espalhamento relativos a estrada, depois da espera`() {
        val coletor = ColetorZona(Zona.ESPELHO_ESQUERDO, inicioMs = 0, estrada, cfg)
        coletor.adicionar(500, PoseCabeca(0.0, 90.0, 0.0), 0.9, 0.9) // ainda movendo os olhos: ignorada
        listOf(30.0, 32.0, 34.0, 36.0, 38.0, 40.0, 42.0, 44.0, 46.0, 48.0).forEachIndexed { i, yaw ->
            coletor.adicionar(1_000L + i * 200, PoseCabeca(-12.0, yaw, 0.0), 0.3, 0.1)
        }
        assertNull(coletor.resultado(2_999))
        val z = assertNotNull(coletor.resultado(3_000))
        assertEquals(Zona.ESPELHO_ESQUERDO, z.zona)
        assertEquals(34.0, z.yaw, 1e-9) // mediana 39 menos a referência 5
        assertEquals(-2.0, z.pitch, 1e-9)
        assertEquals(-0.2, assertNotNull(z.olharHorizontal), 1e-9)
        assertEquals(0.0, assertNotNull(z.olharVertical), 1e-9)
        assertEquals(2 * 5.744562646538029, z.espalhamentoYaw, 1e-9) // 2 desvios padrão
        assertEquals(5.0, z.espalhamentoPitch, 1e-9) // mínimo
        assertEquals(0.05, z.espalhamentoOlhar, 1e-9) // mínimo
        assertEquals(1.0, coletor.progresso(3_500))
    }

    @Test
    fun `zona sem amostras suficientes nao vale e sem olhar fica so com a cabeca`() {
        val poucas = ColetorZona(Zona.PAINEL, inicioMs = 0, estrada, cfg)
        repeat(3) { poucas.adicionar(1_100L + it * 100, PoseCabeca(-30.0, 0.0, 0.0), null, null) }
        assertNull(poucas.resultado(3_000))
        val semOlhar = ColetorZona(Zona.PAINEL, inicioMs = 0, estrada, cfg)
        repeat(12) { semOlhar.adicionar(1_000L + it * 150, PoseCabeca(-30.0, 0.0, 0.0), null, null) }
        val z = assertNotNull(semOlhar.resultado(3_000))
        assertNull(z.olharHorizontal)
        assertNull(z.olharVertical)
        assertEquals(0.0, semOlhar.progresso(500))
    }

    @Test
    fun `estrada calibrada sem olhar usa o centro do olho e o zero vertical`() {
        val coletor = ColetorZona(Zona.RETROVISOR, inicioMs = 0, Calibracao(0.3, pitchReferencia = 0.0), cfg)
        repeat(12) { coletor.adicionar(1_000L + it * 150, PoseCabeca(5.0, -20.0, 0.0), 0.7, -0.2) }
        val z = assertNotNull(coletor.resultado(3_000))
        assertEquals(0.2, assertNotNull(z.olharHorizontal), 1e-9)
        assertEquals(-0.2, assertNotNull(z.olharVertical), 1e-9)
    }
}
