package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Ponto2D
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** O recorte e a entrada da CNN precisam ser os mesmos do experimento em Python (extrair.py). */
class OlhoCnnTest {

    /** Olho com os cantos a [largura] px de distância, inclinado [graus], contorno simétrico em volta do centro. */
    private fun olho(
        cx: Double,
        cy: Double,
        largura: Double,
        graus: Double = 0.0,
    ): Triple<List<Ponto2D>, Ponto2D, Ponto2D> {
        val a = Math.toRadians(graus)
        fun p(dx: Double, dy: Double) = Ponto2D(cx + dx * cos(a) - dy * sin(a), cy + dx * sin(a) + dy * cos(a))
        val meio = largura / 2
        val contorno = (0 until 16).map { k ->
            val t = 2 * Math.PI * k / 16
            p(meio * cos(t), meio / 3 * sin(t))
        }
        return Triple(contorno, p(-meio, 0.0), p(meio, 0.0))
    }

    private fun caixaDe(cx: Double, cy: Double, largura: Double, graus: Double = 0.0): CaixaRecorte {
        val (contorno, p1, p4) = olho(cx, cy, largura, graus)
        return RecorteOlho.caixa(contorno, p1, p4)
    }

    @Test
    fun `contornos sao os 16 pontos do experimento, com os cantos do EAR`() {
        assertEquals(
            listOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246),
            RecorteOlho.CONTORNO_DIREITO_IMAGEM,
        )
        assertEquals(
            listOf(362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398),
            RecorteOlho.CONTORNO_ESQUERDO_IMAGEM,
        )
        val direito = IndicesFaceMesh.OLHO_DIREITO_IMAGEM
        val esquerdo = IndicesFaceMesh.OLHO_ESQUERDO_IMAGEM
        assertTrue(RecorteOlho.CONTORNO_DIREITO_IMAGEM.containsAll(listOf(direito[0], direito[3])))
        assertTrue(RecorteOlho.CONTORNO_ESQUERDO_IMAGEM.containsAll(listOf(esquerdo[0], esquerdo[3])))
    }

    @Test
    fun `caixa e centrada no olho com lado de 1,6 vezes a distancia entre os cantos`() {
        assertEquals(CaixaRecorte(88, 68, 152, 132), caixaDe(120.0, 100.0, 40.0))
    }

    @Test
    fun `olho inclinado da a mesma caixa, porque o lado vem da distancia entre os cantos`() {
        assertEquals(caixaDe(120.0, 100.0, 40.0), caixaDe(120.0, 100.0, 40.0, graus = 30.0))
    }

    @Test
    fun `olho minusculo ainda da uma caixa de 8 px`() {
        assertEquals(CaixaRecorte(16, 16, 24, 24), caixaDe(20.0, 20.0, 1.0))
    }

    @Test
    fun `meio pixel arredonda para o par, como o round do Python`() {
        // centro 10,5 e meia caixa 4: 6,5 vira 6 e 14,5 vira 14
        val contorno = List(16) { Ponto2D(10.5, 10.5) }
        assertEquals(CaixaRecorte(6, 6, 14, 14), RecorteOlho.caixa(contorno, Ponto2D(10.0, 10.5), Ponto2D(11.0, 10.5)))
    }

    @Test
    fun `luminancia usa 0,299 R + 0,587 G + 0,114 B`() {
        assertEquals(76, RecorteOlho.luminancia(0xFFFF0000.toInt()))
        assertEquals(150, RecorteOlho.luminancia(0xFF00FF00.toInt()))
        assertEquals(29, RecorteOlho.luminancia(0xFF0000FF.toInt()))
        assertEquals(255, RecorteOlho.luminancia(0xFFFFFFFF.toInt()))
    }

    @Test
    fun `entrada tem 4096 bytes de cinza`() {
        val bytes = RecorteOlho.entrada(CaixaRecorte(0, 0, 128, 128), 200, 200) { _, _ -> 0xFFFF0000.toInt() }
        assertEquals(RecorteOlho.LADO * RecorteOlho.LADO, bytes.size)
        assertTrue(bytes.all { it.toInt() and 0xFF == 76 })
    }

    @Test
    fun `fora da imagem a borda se repete`() {
        // Imagem 100x100: colunas 0 a 49 pretas, 50 a 99 brancas. A caixa começa 64 px antes da borda esquerda.
        val bytes = RecorteOlho.entrada(CaixaRecorte(-64, -10, 64, 118), 100, 100) { x, y ->
            require(x in 0 until 100 && y in 0 until 100) { "pediu pixel fora da imagem: $x, $y" }
            if (x < 50) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        // Cada saída cobre 2 colunas: as 32 primeiras caem à esquerda da imagem e repetem a coluna 0 (preta).
        val linha = (0 until RecorteOlho.LADO).map { bytes[it].toInt() and 0xFF }
        assertEquals(List(57) { 0 } + List(7) { 255 }, linha)
    }

    @Test
    fun `linhas tambem repetem a borda e caem no lugar certo`() {
        // Imagem 100x80: linhas 0 a 39 pretas, 40 a 79 brancas. Caixa de 128x128 que começa 64 px acima da imagem.
        val bytes = RecorteOlho.entrada(CaixaRecorte(10, -64, 138, 64), 100, 80) { x, y ->
            require(x in 0 until 100 && y in 0 until 80) { "pediu pixel fora da imagem: $x, $y" }
            if (y < 40) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val coluna = (0 until RecorteOlho.LADO).map { bytes[it * RecorteOlho.LADO].toInt() and 0xFF }
        // Cada saída cobre 2 linhas: as 32 primeiras estão acima da imagem e repetem a linha 0 (preta).
        assertEquals(List(52) { 0 } + List(12) { 255 }, coluna)
        assertTrue((0 until RecorteOlho.LADO).all { bytes[51 * RecorteOlho.LADO + it].toInt() == 0 })
    }

    @Test
    fun `reducao e a media da area de cada pixel de saida`() {
        // Listras de 1 px: cada pixel de saída cobre 2x2 de entrada, metade preta e metade branca.
        val bytes = RecorteOlho.entrada(CaixaRecorte(0, 0, 128, 128), 128, 128) { x, _ ->
            if (x % 2 == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        assertTrue(bytes.all { it.toInt() and 0xFF == 128 }) // 127,5 arredonda para o par
    }

    @Test
    fun `reducao com fator quebrado pondera pela fracao da area coberta`() {
        // 96 px para 64: cada saída cobre 1,5 px. A coluna 1 (branca) entra com metade do peso nas saídas 0 e 1.
        val bytes = RecorteOlho.entrada(CaixaRecorte(0, 0, 96, 96), 96, 96) { x, _ ->
            if (x == 1) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        }
        assertEquals(85, bytes[0].toInt() and 0xFF) // 0,5 / 1,5 de 255
        assertEquals(85, bytes[1].toInt() and 0xFF)
        assertEquals(0, bytes[2].toInt() and 0xFF)
    }

    @Test
    fun `recorte em cinza vira ARGB opaco para desenhar no visor`() {
        assertContentEquals(
            intArrayOf(0xFF000000.toInt(), 0xFFC8C8C8.toInt(), 0xFFFFFFFF.toInt()),
            argbDeCinza(byteArrayOf(0, 200.toByte(), 255.toByte())),
        )
    }

    @Test
    fun `decisao da CNN no visor usa o corte de 0,5`() {
        assertTrue(cnnFechado(0.5))
        assertFalse(cnnFechado(0.4999))
        assertEquals("CNN: dir. 0,93 · esq. 0,91", textoCnn(0.934, 0.906))
    }

    @Test
    fun `painel de metricas ganha a linha da CNN so quando ela esta ligada`() {
        val m = Metricas(0.3, 0.3, 0.1, 0.0, 0.0, 0.0, false, false, null)
        fun leitura(metricas: Metricas) =
            Leitura(0, EstadoMotorista.ATENTO, emptySet(), metricas, Totais(0, 0, 0, 0), 0.0, false, emptyList())
        assertTrue(linhasDeMetricas(leitura(m)).none { it.startsWith("CNN") })
        assertTrue(linhasDeMetricas(leitura(m.copy(cnnFechadoDireito = 0.9))).none { it.startsWith("CNN") })
        val comCnn = m.copy(cnnFechadoDireito = 0.93, cnnFechadoEsquerdo = 0.91)
        assertEquals("CNN: dir. 0,93 · esq. 0,91", linhasDeMetricas(leitura(comCnn)).last())
    }
}
