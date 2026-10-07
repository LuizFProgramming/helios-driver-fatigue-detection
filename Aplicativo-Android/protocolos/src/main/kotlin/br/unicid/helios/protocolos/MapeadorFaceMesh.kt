package br.unicid.helios.protocolos

import br.unicid.helios.dominio.geometria.poseDaMatriz
import br.unicid.helios.dominio.tipos.AmostraFacial
import br.unicid.helios.dominio.tipos.Ponto2D
import br.unicid.helios.dominio.tipos.SeisPontos

/**
 * Índices da malha de 478 pontos do MediaPipe Face Landmarker, iguais aos do config.py do protótipo.
 * "Direito" e "esquerdo" são da imagem (câmera), não do motorista.
 */
object IndicesFaceMesh {
    val OLHO_DIREITO_IMAGEM = listOf(33, 160, 158, 133, 153, 144)
    val OLHO_ESQUERDO_IMAGEM = listOf(362, 385, 387, 263, 373, 380)

    /** Lábio interno: mede a abertura real da boca e não sofre com barba. */
    val BOCA_LABIO_INTERNO = listOf(78, 81, 13, 308, 312, 14)
    const val TOTAL_PONTOS = 478
}

/** Nomes dos blendshapes (convenção ARKit) usados pelo HELIOS. */
object Blendshapes {
    const val PISCADA_ESQUERDA = "eyeBlinkLeft"
    const val PISCADA_DIREITA = "eyeBlinkRight"
    const val MANDIBULA = "jawOpen"
}

/**
 * Resultado do MediaPipe já convertido para tipos simples pelo adaptador Android.
 * Os pontos vêm normalizados (0 a 1), como o MediaPipe entrega.
 */
class ResultadoFaceMesh(
    val pontosNormalizados: List<Ponto2D>,
    val blendshapes: Map<String, Double>,
    val matrizTransformacao: DoubleArray?,
)

/**
 * Converte a saída do MediaPipe em [AmostraFacial] do domínio. É aqui que o
 * adaptador "fino" delega toda decisão (padrão Humble Object), para testar sem Android.
 */
class MapeadorFaceMesh(private val larguraPx: Int, private val alturaPx: Int) {

    fun mapear(resultado: ResultadoFaceMesh): AmostraFacial? {
        if (resultado.pontosNormalizados.size < IndicesFaceMesh.TOTAL_PONTOS) return null
        val pontos = resultado.pontosNormalizados
        return AmostraFacial(
            olhoDireito = seis(pontos, IndicesFaceMesh.OLHO_DIREITO_IMAGEM),
            olhoEsquerdo = seis(pontos, IndicesFaceMesh.OLHO_ESQUERDO_IMAGEM),
            boca = seis(pontos, IndicesFaceMesh.BOCA_LABIO_INTERNO),
            piscadaDireita = resultado.blendshapes[Blendshapes.PISCADA_DIREITA] ?: 0.0,
            piscadaEsquerda = resultado.blendshapes[Blendshapes.PISCADA_ESQUERDA] ?: 0.0,
            aberturaMandibula = resultado.blendshapes[Blendshapes.MANDIBULA] ?: 0.0,
            pose = resultado.matrizTransformacao?.let(::poseDaMatriz),
        )
    }

    private fun seis(pontos: List<Ponto2D>, indices: List<Int>): SeisPontos {
        val p = indices.map { pixel(pontos[it]) }
        return SeisPontos(p[0], p[1], p[2], p[3], p[4], p[5])
    }

    /** EAR depende de distâncias reais: sem escalar x e y, um quadro 4:3 distorce a razão. */
    private fun pixel(n: Ponto2D) = Ponto2D(n.x * larguraPx, n.y * alturaPx)
}
