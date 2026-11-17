package br.unicid.helios.protocolos

import br.unicid.helios.dominio.tipos.CaixaRosto
import br.unicid.helios.dominio.tipos.Ponto2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RealceAutomaticoTest {

    @Test
    fun `liga depois de 2 s escuro e desliga depois de 2 s claro, com histerese`() {
        val r = RealceAutomatico()
        assertFalse(r.atualizar(0, 40.0))
        assertFalse(r.atualizar(1_999, 40.0))
        assertTrue(r.atualizar(2_000, 40.0))
        assertTrue(r.atualizar(3_000, 75.0)) // entre os limiares: continua como está
        assertTrue(r.atualizar(4_000, 120.0))
        assertTrue(r.atualizar(5_000, 70.0)) // voltou a escurecer: a contagem recomeça
        assertTrue(r.atualizar(6_000, 120.0))
        assertTrue(r.atualizar(7_999, 120.0))
        assertFalse(r.atualizar(8_000, 120.0))
        assertFalse(r.ligado)
    }

    @Test
    fun `brilho igual ao limiar nao muda e entre os limiares fica como esta`() {
        val r = RealceAutomatico()
        assertFalse(r.atualizar(0, 75.0))
        r.atualizar(100, 60.0)
        assertFalse(r.atualizar(3_000, 60.0))
        r.atualizar(4_000, 59.0)
        assertTrue(r.atualizar(6_000, 59.0))
        assertTrue(r.ligado)
        r.atualizar(7_000, 90.0)
        assertTrue(r.atualizar(10_000, 90.0))
    }

    @Test
    fun `um quadro claro no meio do escuro reinicia a espera`() {
        val r = RealceAutomatico()
        r.atualizar(0, 40.0)
        r.atualizar(1_500, 100.0)
        assertFalse(r.atualizar(3_000, 40.0))
        assertTrue(r.atualizar(5_000, 40.0))
    }
}

class RastreadorRostoTest {

    private val motorista = CaixaRosto(0.30, 0.20, 0.50, 0.50)
    private val passageiro = CaixaRosto(0.80, 0.30, 0.95, 0.50)

    @Test
    fun `sem posicao anterior fica o maior rosto, depois o que se sobrepoe ao motorista`() {
        val r = RastreadorRosto()
        assertEquals(1, r.escolher(0, listOf(passageiro, motorista)))
        val motoristaAndou = CaixaRosto(0.35, 0.20, 0.55, 0.50)
        val passageiroPerto = CaixaRosto(0.60, 0.00, 1.00, 1.00)
        assertEquals(0, r.escolher(100, listOf(motoristaAndou, passageiroPerto)))
    }

    @Test
    fun `sem sobreposicao nenhuma volta a ficar o maior`() {
        val r = RastreadorRosto()
        r.escolher(0, listOf(motorista))
        val longe = CaixaRosto(0.0, 0.0, 0.1, 0.1)
        val outro = CaixaRosto(0.7, 0.7, 0.95, 0.95)
        val abaixo = CaixaRosto(0.3, 0.6, 0.5, 0.65) // mesma coluna, sem sobreposição na altura
        assertEquals(1, r.escolher(100, listOf(longe, outro, abaixo)))
    }

    @Test
    fun `rosto perdido alterna recorte em volta da ultima posicao e imagem inteira por 3 s`() {
        val r = RastreadorRosto()
        assertNull(r.recorteParaEnviar(0)) // nunca viu rosto
        r.escolher(0, listOf(motorista))
        assertNull(r.recorteParaEnviar(50)) // rosto presente
        assertNull(r.escolher(100, emptyList()))
        assertNull(r.escolher(150, emptyList())) // o início da perda não muda
        val recorte = r.recorteParaEnviar(200)
        assertEquals(CaixaRosto(0.1, 0.05, 0.7, 0.65), recorte?.arredondada())
        assertNull(r.recorteParaEnviar(300))
        assertEquals(recorte, r.recorteParaEnviar(400))
        assertNull(r.recorteParaEnviar(3_101))
        r.escolher(3_200, listOf(motorista))
        assertNull(r.recorteParaEnviar(3_300))
    }

    @Test
    fun `busca vale ate exatamente 3 s depois da perda`() {
        val r = RastreadorRosto()
        r.escolher(0, listOf(motorista))
        r.escolher(1_000, emptyList())
        assertTrue(r.recorteParaEnviar(4_000) != null)
        assertNull(r.recorteParaEnviar(4_001))
    }

    @Test
    fun `o maior rosto conta largura e altura`() {
        val largoBaixo = CaixaRosto(0.0, 0.8, 0.5, 0.9)
        val estreitoAlto = CaixaRosto(0.6, 0.0, 0.8, 0.4)
        assertEquals(1, RastreadorRosto().escolher(0, listOf(largoBaixo, estreitoAlto)))
    }

    @Test
    fun `entre dois rostos sobrepostos fica o mais parecido com a ultima posicao`() {
        val r = RastreadorRosto()
        r.escolher(0, listOf(motorista))
        val quaseIgual = CaixaRosto(0.31, 0.21, 0.49, 0.49)
        val telaToda = CaixaRosto(0.0, 0.0, 1.0, 1.0)
        assertEquals(0, r.escolher(100, listOf(quaseIgual, telaToda)))
    }

    @Test
    fun `recorte fica dentro da imagem e nao passa do tamanho dela`() {
        val r = RastreadorRosto()
        r.escolher(0, listOf(CaixaRosto(0.85, 0.80, 1.0, 1.0)))
        r.escolher(100, emptyList())
        assertEquals(CaixaRosto(0.6, 0.6, 1.0, 1.0), r.recorteParaEnviar(200)?.arredondada())
        val grande = RastreadorRosto()
        grande.escolher(0, listOf(CaixaRosto(0.1, 0.1, 0.9, 0.9)))
        grande.escolher(100, emptyList())
        assertEquals(CaixaRosto(0.0, 0.0, 1.0, 1.0), grande.recorteParaEnviar(200))
    }

    @Test
    fun `pontos do recorte voltam para a imagem inteira e a caixa envolve os pontos`() {
        val recorte = CaixaRosto(0.2, 0.1, 0.6, 0.5)
        assertEquals(Ponto2D(0.4, 0.3), paraImagemInteira(Ponto2D(0.5, 0.5), recorte).arredondado())
        assertEquals(
            CaixaRosto(0.1, 0.2, 0.4, 0.6),
            caixaDosPontos(listOf(Ponto2D(0.1, 0.6), Ponto2D(0.4, 0.2), Ponto2D(0.3, 0.3))),
        )
        assertFailsWith<NoSuchElementException> { caixaDosPontos(emptyList()) }
    }

    private fun Double.r() = Math.round(this * 1e6) / 1e6
    private fun CaixaRosto.arredondada() = CaixaRosto(x0.r(), y0.r(), x1.r(), y1.r())
    private fun Ponto2D.arredondado() = Ponto2D(x.r(), y.r())
}
