package br.unicid.helios.dominio.geometria

import br.unicid.helios.dominio.tipos.Ponto2D
import br.unicid.helios.dominio.tipos.PoseCabeca
import br.unicid.helios.dominio.tipos.SeisPontos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.hypot

private const val EPSILON = 1e-8
private const val TAMANHO_MATRIZ = 16

fun distancia(a: Ponto2D, b: Ponto2D): Double = hypot(a.x - b.x, a.y - b.y)

/**
 * AR = (|p2 - p6| + |p3 - p5|) / (2 |p1 - p4|), Soukupová e Čech (2016).
 * Mesma fórmula para EAR (olho) e MAR (boca).
 */
fun razaoAspecto(p: SeisPontos): Double {
    val horizontal = 2.0 * distancia(p.p1, p.p4)
    if (horizontal < EPSILON) return 0.0
    return (distancia(p.p2, p.p6) + distancia(p.p3, p.p5)) / horizontal
}

fun earMedio(direito: SeisPontos, esquerdo: SeisPontos): Double =
    (razaoAspecto(direito) + razaoAspecto(esquerdo)) / 2.0

/**
 * Converte a matriz 4x4 de transformação facial do MediaPipe (16 valores, coluna maior)
 * em ângulos de Euler (ordem X-Y-Z). Os sinais dependem da convenção da câmera; por isso
 * a queda da cabeça é sempre medida em relação à pose calibrada, não ao zero absoluto.
 */
fun poseDaMatriz(m: DoubleArray): PoseCabeca {
    require(m.size == TAMANHO_MATRIZ) { "A matriz precisa ter 16 valores" }
    val r20 = m[2]
    val r21 = m[6]
    val r22 = m[10]
    val r10 = m[1]
    val r00 = m[0]
    val pitch = atan2(r21, r22)
    val yaw = asin((-r20).coerceIn(-1.0, 1.0))
    val roll = atan2(r10, r00)
    return PoseCabeca(Math.toDegrees(pitch), Math.toDegrees(yaw), Math.toDegrees(roll))
}
