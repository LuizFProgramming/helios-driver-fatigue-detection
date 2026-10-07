package br.unicid.helios.dominio.geometria

import br.unicid.helios.dominio.seisComRazao
import br.unicid.helios.dominio.tipos.Ponto2D
import br.unicid.helios.dominio.tipos.SeisPontos
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeometriaTest {

    @Test
    fun `distancia euclidiana 3-4-5`() {
        assertEquals(5.0, distancia(Ponto2D(0.0, 0.0), Ponto2D(3.0, 4.0)), 1e-12)
    }

    @Test
    fun `razao de aspecto reproduz a altura sobre a largura`() {
        assertEquals(0.30, razaoAspecto(seisComRazao(0.30)), 1e-12)
        assertEquals(0.05, razaoAspecto(seisComRazao(0.05)), 1e-12)
    }

    @Test
    fun `razao de aspecto nao depende da escala nem da posicao`() {
        for (escala in listOf(1.0, 37.5, 640.0)) {
            assertEquals(0.27, razaoAspecto(seisComRazao(0.27, escala, origemX = 123.0)), 1e-9)
        }
    }

    @Test
    fun `cantos coincidentes devolvem zero em vez de dividir por zero`() {
        val p = Ponto2D(5.0, 5.0)
        assertEquals(0.0, razaoAspecto(SeisPontos(p, Ponto2D(5.0, 4.0), p, p, p, p)))
    }

    @Test
    fun `ear medio e a media dos dois olhos`() {
        assertEquals(0.25, earMedio(seisComRazao(0.20), seisComRazao(0.30)), 1e-12)
    }

    @Test
    fun `matriz identidade da pose zero`() {
        val pose = poseDaMatriz(identidade())
        assertEquals(0.0, pose.pitchGraus, 1e-9)
        assertEquals(0.0, pose.yawGraus, 1e-9)
        assertEquals(0.0, pose.rollGraus, 1e-9)
    }

    @Test
    fun `rotacao em X vira pitch`() {
        for (graus in listOf(-30.0, 15.0, 30.0)) {
            assertEquals(graus, poseDaMatriz(rotacaoX(graus)).pitchGraus, 1e-9)
        }
    }

    @Test
    fun `rotacao em Y vira yaw e em Z vira roll`() {
        assertEquals(20.0, poseDaMatriz(rotacaoY(20.0)).yawGraus, 1e-9)
        assertEquals(-25.0, poseDaMatriz(rotacaoZ(-25.0)).rollGraus, 1e-9)
    }

    @Test
    fun `yaw satura em 90 graus quando o arredondamento passa de 1`() {
        val m = identidade().also { it[2] = -1.0000001 }
        assertEquals(90.0, poseDaMatriz(m).yawGraus, 1e-9)
        val n = identidade().also { it[2] = 1.0000001 }
        assertEquals(-90.0, poseDaMatriz(n).yawGraus, 1e-9)
    }

    @Test
    fun `matriz com tamanho errado e rejeitada`() {
        assertFailsWith<IllegalArgumentException> { poseDaMatriz(DoubleArray(9)) }
    }

    // Matrizes em coluna maior, como o MediaPipe entrega.
    private fun identidade() = DoubleArray(16).also { for (i in 0..3) it[i * 5] = 1.0 }

    private fun rotacaoX(graus: Double): DoubleArray {
        val r = Math.toRadians(graus)
        return identidade().also { m -> m[5] = cos(r); m[6] = sin(r); m[9] = -sin(r); m[10] = cos(r) }
    }

    private fun rotacaoY(graus: Double): DoubleArray {
        val r = Math.toRadians(graus)
        return identidade().also { m -> m[0] = cos(r); m[2] = -sin(r); m[8] = sin(r); m[10] = cos(r) }
    }

    private fun rotacaoZ(graus: Double): DoubleArray {
        val r = Math.toRadians(graus)
        return identidade().also { m -> m[0] = cos(r); m[1] = sin(r); m[4] = -sin(r); m[5] = cos(r) }
    }
}
