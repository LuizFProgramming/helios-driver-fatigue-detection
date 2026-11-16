package br.unicid.helios.protocolos

import br.unicid.helios.dominio.tipos.Ponto2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Paridade com o experimento: o [RecorteOlho] tem de dar o mesmo recorte que o `recortar_olho` do Python
 * (scripts/experimento_olhos/dourado_sintetico.py gera os arquivos em resources/dourado_olho). As imagens são
 * desenhadas, sem rosto de pessoa. Tolerância de 1 nível de cinza: o OpenCV arredonda em ponto fixo.
 */
class OlhoCnnDouradoTest {

    private class Caso(val nome: String, val contorno: List<Ponto2D>)

    private fun casos(): List<Caso> {
        val linhas = recurso("dourado.csv").bufferedReader().readLines()
        val cabecalho = linhas.first().split(",")
        return linhas.drop(1).map { linha ->
            val c = cabecalho.zip(linha.split(",")).toMap()
            val contorno = (0 until 16).map { k -> Ponto2D(c.getValue("x$k").toDouble(), c.getValue("y$k").toDouble()) }
            Caso(c.getValue("caso"), contorno)
        }
    }

    private fun recurso(nome: String) =
        requireNotNull(javaClass.getResourceAsStream("/dourado_olho/$nome")) { "falta resources/dourado_olho/$nome" }

    private fun imagem(nome: String): BufferedImage = recurso(nome).use { ImageIO.read(it) }

    @Test
    fun `o dourado tem os sete casos do experimento`() {
        assertEquals(
            listOf(
                "reducao_inteira", "reducao_quebrada", "borda_esquerda_topo", "borda_direita_baixo",
                "ampliacao", "colorido", "inclinado",
            ),
            casos().map { it.nome },
        )
    }

    @Test
    fun `recorte igual ao do experimento em todos os casos`() {
        val diferencas = casos().associate { caso ->
            val entrada = imagem("${caso.nome}.png")
            val esperado = imagem("${caso.nome}_esperado.png").raster
            // Cantos do EAR (p1 = 33 e p4 = 133) são o 1º e o 9º ponto do contorno.
            val caixa = RecorteOlho.caixa(caso.contorno, caso.contorno[0], caso.contorno[8])
            val obtido = RecorteOlho.entrada(caixa, entrada.width, entrada.height) { x, y -> entrada.getRGB(x, y) }
            caso.nome to (0 until RecorteOlho.LADO * RecorteOlho.LADO).maxOf { i ->
                abs((obtido[i].toInt() and 0xFF) - esperado.getSample(i % RecorteOlho.LADO, i / RecorteOlho.LADO, 0))
            }
        }
        assertTrue(diferencas.values.all { it <= 1 }, "maior diferença por caso, em níveis de cinza: $diferencas")
    }
}
