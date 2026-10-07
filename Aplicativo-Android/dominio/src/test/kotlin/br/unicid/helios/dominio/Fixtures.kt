package br.unicid.helios.dominio

import br.unicid.helios.dominio.tipos.AmostraFacial
import br.unicid.helios.dominio.tipos.Ponto2D
import br.unicid.helios.dominio.tipos.PoseCabeca
import br.unicid.helios.dominio.tipos.SeisPontos

/**
 * Olho/boca sintético com largura 1 e as duas alturas iguais a [razao].
 * Assim razaoAspecto(seisComRazao(r)) == r exatamente.
 */
fun seisComRazao(razao: Double, escala: Double = 100.0, origemX: Double = 0.0): SeisPontos {
    val h = razao * escala
    return SeisPontos(
        p1 = Ponto2D(origemX, 0.0),
        p2 = Ponto2D(origemX + escala / 3, -h / 2),
        p3 = Ponto2D(origemX + 2 * escala / 3, -h / 2),
        p4 = Ponto2D(origemX + escala, 0.0),
        p5 = Ponto2D(origemX + 2 * escala / 3, h / 2),
        p6 = Ponto2D(origemX + escala / 3, h / 2),
    )
}

fun amostra(
    ear: Double = 0.30,
    mar: Double = 0.10,
    ecf: Double = 0.0,
    mandibula: Double = 0.0,
    pitch: Double? = null,
) = AmostraFacial(
    olhoDireito = seisComRazao(ear),
    olhoEsquerdo = seisComRazao(ear, origemX = 200.0),
    boca = seisComRazao(mar),
    piscadaDireita = ecf,
    piscadaEsquerda = ecf,
    aberturaMandibula = mandibula,
    pose = pitch?.let { PoseCabeca(it, 0.0, 0.0) },
)

/** Instantes de [inicioMs] até [fimMs] (exclusivo) espaçados para o [fps] dado. */
fun instantes(inicioMs: Long, fimMs: Long, fps: Int): List<Long> {
    val passo = 1000.0 / fps
    return generateSequence(0) { it + 1 }
        .map { inicioMs + (it * passo).toLong() }
        .takeWhile { it < fimMs }
        .toList()
}
