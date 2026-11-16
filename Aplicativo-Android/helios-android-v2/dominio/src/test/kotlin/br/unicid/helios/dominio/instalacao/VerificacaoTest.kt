package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.tipos.CaixaRosto
import br.unicid.helios.dominio.tipos.PoseCabeca
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerificacaoTest {

    private val cfg = ConfiguracaoInstalacao()
    private val rostoBom = CaixaRosto(0.35, 0.25, 0.65, 0.75) // 30% da largura, no meio
    private val boa = AmostraInstalacao(rostoBom, brilho = 120.0, pose = pose(-10.0, 5.0), rmsAceleracao = 0.1)

    private fun pose(pitch: Double, yaw: Double) = PoseCabeca(pitch, yaw, 0.0)

    private fun dica(a: AmostraInstalacao, v: Verificacao, posicao: PosicaoCamera = PosicaoCamera.COLUNA_VOLANTE) =
        verificar(a, posicao, cfg).single { it.verificacao == v }.dica

    private fun caixa(x0: Double, y0: Double, x1: Double, y1: Double) = boa.copy(caixa = CaixaRosto(x0, y0, x1, y1))

    private fun angulo(pitch: Double, yaw: Double, posicao: PosicaoCamera = PosicaoCamera.COLUNA_VOLANTE) =
        dica(boa.copy(pose = pose(pitch, yaw)), Verificacao.ANGULO, posicao)

    @Test
    fun `instalacao boa passa nas cinco verificacoes, na ordem da tela`() {
        val r = verificar(boa, PosicaoCamera.COLUNA_VOLANTE, cfg)
        assertEquals(Verificacao.entries, r.map { it.verificacao })
        assertTrue(r.all { it.ok })
    }

    @Test
    fun `rosto saindo do quadro ou ausente pede para centralizar`() {
        assertEquals(Dica.CENTRALIZAR, dica(caixa(0.01, 0.2, 0.4, 0.7), Verificacao.ROSTO_INTEIRO))
        assertEquals(Dica.CENTRALIZAR, dica(caixa(0.5, 0.2, 0.99, 0.7), Verificacao.ROSTO_INTEIRO))
        assertEquals(Dica.CENTRALIZAR, dica(caixa(0.3, 0.01, 0.6, 0.6), Verificacao.ROSTO_INTEIRO))
        assertEquals(Dica.CENTRALIZAR, dica(caixa(0.3, 0.4, 0.6, 0.99), Verificacao.ROSTO_INTEIRO))
        val semRosto = verificar(boa.copy(caixa = null, brilho = null, pose = null), PosicaoCamera.COLUNA_VOLANTE, cfg)
        assertEquals(List(4) { Dica.CENTRALIZAR }, semRosto.dropLast(1).map { it.dica })
    }

    @Test
    fun `rosto encostado na margem de 2 por cento ainda esta inteiro`() {
        assertEquals(null, dica(caixa(0.02, 0.02, 0.98, 0.98), Verificacao.ROSTO_INTEIRO))
    }

    @Test
    fun `rosto pequeno pede para aproximar e grande para afastar, com os limites valendo`() {
        assertEquals(Dica.APROXIMAR, dica(caixa(0.45, 0.4, 0.64, 0.6), Verificacao.TAMANHO))
        assertEquals(Dica.AFASTAR, dica(caixa(0.1, 0.1, 0.71, 0.9), Verificacao.TAMANHO))
        assertEquals(null, dica(caixa(0.3, 0.3, 0.5, 0.7), Verificacao.TAMANHO)) // 20%
        assertEquals(null, dica(caixa(0.1, 0.1, 0.7, 0.9), Verificacao.TAMANHO)) // 60%
    }

    @Test
    fun `luz fraca pede mais luz e luz forte pede menos, com os limites valendo`() {
        assertEquals(Dica.MAIS_LUZ, dica(boa.copy(brilho = 59.0), Verificacao.LUZ))
        assertEquals(Dica.MENOS_LUZ, dica(boa.copy(brilho = 221.0), Verificacao.LUZ))
        assertEquals(null, dica(boa.copy(brilho = 60.0), Verificacao.LUZ))
        assertEquals(null, dica(boa.copy(brilho = 220.0), Verificacao.LUZ))
    }

    @Test
    fun `angulo fora da faixa da posicao diz como mover a camera`() {
        assertEquals(Dica.MAIS_A_FRENTE, angulo(-10.0, 16.0))
        assertEquals(Dica.MAIS_A_FRENTE, angulo(-10.0, -16.0))
        assertEquals(Dica.SUBIR, angulo(-36.0, 0.0))
        assertEquals(Dica.ABAIXAR, angulo(1.0, 0.0))
        // No centro do painel a câmera fica de lado: 30 graus de giro ainda é normal.
        assertEquals(null, angulo(-10.0, 30.0, PosicaoCamera.CENTRO_PAINEL))
        // No retrovisor a câmera fica acima do rosto: pitch positivo é normal.
        assertEquals(null, angulo(30.0, 0.0, PosicaoCamera.RETROVISOR))
        assertEquals(Dica.SUBIR, angulo(-16.0, 0.0, PosicaoCamera.PILAR_A))
    }

    @Test
    fun `limites da faixa de angulo valem`() {
        assertEquals(null, angulo(-35.0, 15.0))
        assertEquals(null, angulo(0.0, -15.0))
    }

    @Test
    fun `cada posicao tem uma faixa de angulo`() {
        assertEquals(PosicaoCamera.entries.toSet(), cfg.faixas.keys)
    }

    @Test
    fun `celular balancando pede para fixar a camera`() {
        assertEquals(Dica.FIXAR, dica(boa.copy(rmsAceleracao = 0.26), Verificacao.CAMERA_PARADA))
        assertEquals(null, dica(boa.copy(rmsAceleracao = 0.25), Verificacao.CAMERA_PARADA))
    }
}
