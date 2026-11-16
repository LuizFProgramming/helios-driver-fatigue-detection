package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.tipos.CaixaRosto
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.MetricasOlhar
import br.unicid.helios.dominio.tipos.PoseCabeca
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Câmera atrás do volante, 20 quadros por segundo (50 ms). */
class AssistenteTest {

    private val cfg = ConfiguracaoInstalacao()
    private val caixaBoa = CaixaRosto(0.35, 0.25, 0.65, 0.75)

    private fun amostra(pose: PoseCabeca?, caixa: CaixaRosto? = caixaBoa) =
        AmostraInstalacao(caixa, brilho = 120.0, pose = pose, rmsAceleracao = 0.1)

    private fun metricas(pose: PoseCabeca, h: Double = 0.5, v: Double = 0.0) = Metricas(
        earBruto = 0.3, earExibicao = 0.3, marExibicao = 0.1, ecf = 0.1, perclosCurto = 0.0, perclosLongo = 0.0,
        olhoFechado = false, bocaAberta = false, pose = pose, olhar = MetricasOlhar(horizontal = h, vertical = v),
    )

    /** Manda quadros de [de] até [ate] (exclusivo) olhando na direção [pose]; devolve o último estado. */
    private fun Assistente.quadros(de: Long, ate: Long, pose: PoseCabeca, caixa: CaixaRosto? = caixaBoa) =
        (de until ate step 50).map { t ->
            processar(t, amostra(pose, caixa), if (caixa == null) null else metricas(pose))
        }.last()

    private val estrada = PoseCabeca(-15.0, 0.0, 0.0)

    private fun assistenteCalibrado(): Assistente {
        val a = Assistente(cfg)
        a.escolherPosicao(PosicaoCamera.COLUNA_VOLANTE, LadoMotorista.ESQUERDO)
        a.quadros(0, 1_050, estrada)
        assertTrue(a.comecarCalibracao(1_050))
        a.quadros(1_050, 11_100, estrada)
        return a
    }

    @Test
    fun `comeca pela posicao e so libera a calibracao depois de 1 s com tudo certo`() {
        val a = Assistente(cfg)
        assertEquals(Etapa.POSICAO, a.processar(0, amostra(estrada), metricas(estrada)).etapa)
        a.escolherPosicao(PosicaoCamera.COLUNA_VOLANTE, LadoMotorista.ESQUERDO)
        val antes = a.quadros(0, 950, estrada)
        assertFalse(antes.podeCalibrar)
        assertEquals(Verificacao.entries, antes.verificacoes.map { it.verificacao })
        assertFalse(a.comecarCalibracao(950))
        assertTrue(a.quadros(950, 1_050, estrada).podeCalibrar)
        // Uma falha zera a contagem.
        assertFalse(a.processar(1_100, amostra(PoseCabeca(-15.0, 40.0, 0.0)), null).podeCalibrar)
        assertFalse(a.quadros(1_150, 2_000, estrada).podeCalibrar)
        assertTrue(a.quadros(2_000, 2_200, estrada).podeCalibrar)
    }

    @Test
    fun `calibracao de 10 s leva as zonas, que podem ser gravadas ou puladas, e termina no resumo`() {
        val a = assistenteCalibrado()
        var e = a.quadros(11_100, 11_200, estrada)
        assertEquals(Etapa.ZONAS, e.etapa)
        assertEquals(Zona.RETROVISOR, e.zona)
        assertFalse(e.coletandoZona)
        assertNull(a.perfil("p1", "Meu carro", criadoEpochMs = 123)) // ainda nas zonas
        a.gravarZona(11_200)
        val meio = a.quadros(11_200, 13_250, PoseCabeca(-5.0, -30.0, 0.0))
        assertTrue(meio.coletandoZona)
        assertEquals(0.5, assertNotNull(meio.progresso), 1e-9)
        e = a.quadros(13_250, 14_250, PoseCabeca(-5.0, -30.0, 0.0))
        assertEquals(Zona.ESPELHO_ESQUERDO, e.zona)
        a.pularZona()
        a.pularZona()
        assertEquals(Zona.PAINEL, a.processar(14_300, amostra(estrada), metricas(estrada)).zona)
        a.pularZona()
        e = a.processar(14_350, amostra(estrada), metricas(estrada))
        assertEquals(Etapa.RESUMO, e.etapa)
        // Tudo certo, parado, 1 zona de 4: 60 + 25 + 3,75.
        assertEquals(NotaInstalacao(89, Qualidade.BOA, PartesNota(60, 25, 4)), e.nota)

        val p = assertNotNull(a.perfil("p1", "Meu carro", criadoEpochMs = 123))
        assertEquals(listOf("p1", "Meu carro"), listOf(p.id, p.nome))
        assertEquals(123, p.criadoEpochMs)
        assertEquals(PosicaoCamera.COLUNA_VOLANTE, p.posicao)
        assertEquals(LadoMotorista.ESQUERDO, p.lado)
        assertEquals(-15.0, p.calibracao.pitchReferencia, 1e-9)
        assertEquals(listOf(Zona.RETROVISOR), p.zonas.map { it.zona })
        assertEquals(-30.0, p.zonas.single().yaw, 1e-9)
        assertEquals(0.0, assertNotNull(p.zonas.single().olharHorizontal), 1e-9)
        assertEquals(89, p.nota)
        assertEquals(1.0, p.fracaoVerificacoesOk, 1e-9)
    }

    @Test
    fun `progresso da calibracao aparece na tela`() {
        val a = Assistente(cfg)
        a.escolherPosicao(PosicaoCamera.COLUNA_VOLANTE, LadoMotorista.ESQUERDO)
        a.quadros(0, 1_050, estrada)
        a.comecarCalibracao(1_050)
        val e = a.quadros(1_050, 6_100, estrada)
        assertEquals(Etapa.CALIBRACAO, e.etapa)
        assertEquals(0.5, assertNotNull(e.progresso), 1e-9)
        assertFalse(e.calibracaoFalhou)
    }

    @Test
    fun `quadros sem pose no meio da calibracao nao atrapalham`() {
        val a = Assistente(cfg)
        a.escolherPosicao(PosicaoCamera.COLUNA_VOLANTE, LadoMotorista.ESQUERDO)
        a.quadros(0, 1_050, estrada)
        a.comecarCalibracao(1_050)
        (1_050L until 11_100 step 50).forEachIndexed { i, t ->
            val m = if (i % 2 == 0) metricas(estrada) else metricas(estrada).copy(pose = null)
            a.processar(t, amostra(estrada), m)
        }
        repeat(4) { a.pularZona() }
        val p = assertNotNull(a.perfil("p1", "x", 0))
        assertEquals(-15.0, p.calibracao.pitchReferencia, 1e-9)
    }

    @Test
    fun `zonas fora da etapa de zonas sao ignoradas e repetir apaga as zonas gravadas`() {
        val a = Assistente(cfg)
        a.gravarZona(0)
        a.pularZona()
        assertEquals(Etapa.POSICAO, a.processar(0, amostra(estrada), null).etapa)
        val calibrado = assistenteCalibrado()
        calibrado.quadros(11_100, 11_200, estrada)
        calibrado.gravarZona(11_200)
        calibrado.quadros(11_200, 14_250, PoseCabeca(-5.0, -30.0, 0.0))
        calibrado.repetir()
        calibrado.escolherPosicao(PosicaoCamera.COLUNA_VOLANTE, LadoMotorista.DIREITO)
        calibrado.quadros(20_000, 21_050, estrada)
        calibrado.comecarCalibracao(21_050)
        calibrado.quadros(21_050, 31_100, estrada)
        repeat(4) { calibrado.pularZona() }
        val p = assertNotNull(calibrado.perfil("p2", "x", 0))
        assertEquals(emptyList(), p.zonas)
        assertEquals(LadoMotorista.DIREITO, p.lado)
    }

    @Test
    fun `instalacao para refazer nao vira perfil`() {
        val a = Assistente(cfg)
        a.escolherPosicao(PosicaoCamera.COLUNA_VOLANTE, LadoMotorista.ESQUERDO)
        a.quadros(0, 1_050, estrada)
        a.comecarCalibracao(1_050)
        // Cabeça balançando 20 graus e metade dos quadros com o rosto grande demais.
        (1_050L until 11_100 step 50).forEachIndexed { i, t ->
            val pose = PoseCabeca(-15.0, if (i % 2 == 0) 10.0 else -10.0, 0.0)
            val caixa = if (i % 2 == 0) caixaBoa else CaixaRosto(0.1, 0.1, 0.9, 0.9)
            a.processar(t, amostra(pose, caixa), metricas(pose))
        }
        repeat(4) { a.pularZona() }
        val e = a.processar(11_200, amostra(estrada), metricas(estrada))
        assertEquals(Qualidade.REFAZER, assertNotNull(e.nota).qualidade)
        assertNull(a.perfil("p1", "Meu carro", criadoEpochMs = 123))
    }

    @Test
    fun `calibracao sem rosto volta ao enquadramento avisando a falha, e repetir recomeca`() {
        val a = Assistente(cfg)
        a.escolherPosicao(PosicaoCamera.PILAR_A, LadoMotorista.ESQUERDO)
        a.quadros(0, 1_050, PoseCabeca(0.0, 20.0, 0.0))
        a.comecarCalibracao(1_050)
        val e = a.quadros(1_050, 11_100, estrada, caixa = null)
        assertEquals(Etapa.ENQUADRAMENTO, e.etapa)
        assertTrue(e.calibracaoFalhou)
        assertNull(a.perfil("p1", "x", 0))
        a.repetir()
        assertEquals(Etapa.POSICAO, a.processar(11_200, amostra(estrada), null).etapa)
    }

    @Test
    fun `zona que nao junta amostras pode ser gravada de novo`() {
        val a = assistenteCalibrado()
        a.gravarZona(11_100)
        val e = a.quadros(11_100, 14_200, estrada, caixa = null)
        assertEquals(Zona.RETROVISOR, e.zona)
        assertFalse(e.coletandoZona)
        assertTrue(e.zonaFalhou)
        a.gravarZona(14_200)
        assertFalse(a.processar(14_250, amostra(estrada), metricas(estrada)).zonaFalhou)
    }
}
