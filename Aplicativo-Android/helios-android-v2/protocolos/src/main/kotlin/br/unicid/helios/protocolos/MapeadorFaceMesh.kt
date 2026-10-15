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

    /** Centro de cada íris (os pontos 468 a 477 são as íris; 468 e 473 são os centros). */
    const val IRIS_DIREITA = 468
    const val IRIS_ESQUERDA = 473

    /** Os 20 pontos que o HELIOS lê de fato. */
    val USADOS = OLHO_DIREITO_IMAGEM + OLHO_ESQUERDO_IMAGEM + BOCA_LABIO_INTERNO + IRIS_DIREITA + IRIS_ESQUERDA
}

/** Nomes dos blendshapes (convenção ARKit) usados pelo HELIOS. */
object Blendshapes {
    const val PISCADA_ESQUERDA = "eyeBlinkLeft"
    const val PISCADA_DIREITA = "eyeBlinkRight"
    const val MANDIBULA = "jawOpen"
    const val OLHAR_CIMA_ESQUERDO = "eyeLookUpLeft"
    const val OLHAR_CIMA_DIREITO = "eyeLookUpRight"
    const val OLHAR_BAIXO_ESQUERDO = "eyeLookDownLeft"
    const val OLHAR_BAIXO_DIREITO = "eyeLookDownRight"
    const val SEMICERRAR_ESQUERDO = "eyeSquintLeft"
    const val SEMICERRAR_DIREITO = "eyeSquintRight"
    val OLHAR = setOf(OLHAR_CIMA_ESQUERDO, OLHAR_CIMA_DIREITO, OLHAR_BAIXO_ESQUERDO, OLHAR_BAIXO_DIREITO)
    val USADOS = setOf(PISCADA_ESQUERDA, PISCADA_DIREITA, MANDIBULA, SEMICERRAR_ESQUERDO, SEMICERRAR_DIREITO) + OLHAR
}

/**
 * Resultado do MediaPipe visto por tipos simples. Os pontos são lidos sob demanda por [ponto]
 * (normalizados de 0 a 1, como o MediaPipe entrega): assim só os 18 usados viram objeto,
 * em vez de 478 a cada frame. [blendshapes] só precisa ter os nomes de [Blendshapes.USADOS].
 */
class ResultadoFaceMesh(
    val totalPontos: Int,
    val ponto: (Int) -> Ponto2D,
    val blendshapes: Map<String, Double>,
    val matrizTransformacao: DoubleArray?,
)

/**
 * Converte a saída do MediaPipe em [AmostraFacial] do domínio. É aqui que o
 * adaptador "fino" delega toda decisão (padrão Humble Object), para testar sem Android.
 */
class MapeadorFaceMesh(private val larguraPx: Int, private val alturaPx: Int) {

    fun mapear(resultado: ResultadoFaceMesh): AmostraFacial? {
        if (resultado.totalPontos < IndicesFaceMesh.TOTAL_PONTOS) return null
        val pontos = resultado.ponto
        return AmostraFacial(
            olhoDireito = seis(pontos, IndicesFaceMesh.OLHO_DIREITO_IMAGEM),
            olhoEsquerdo = seis(pontos, IndicesFaceMesh.OLHO_ESQUERDO_IMAGEM),
            boca = seis(pontos, IndicesFaceMesh.BOCA_LABIO_INTERNO),
            piscadaDireita = resultado.blendshapes[Blendshapes.PISCADA_DIREITA] ?: 0.0,
            piscadaEsquerda = resultado.blendshapes[Blendshapes.PISCADA_ESQUERDA] ?: 0.0,
            aberturaMandibula = resultado.blendshapes[Blendshapes.MANDIBULA] ?: 0.0,
            pose = resultado.matrizTransformacao?.let(::poseDaMatriz),
            irisDireita = pixel(pontos(IndicesFaceMesh.IRIS_DIREITA)),
            irisEsquerda = pixel(pontos(IndicesFaceMesh.IRIS_ESQUERDA)),
            olharVertical = olharVertical(resultado.blendshapes),
            semicerrarDireito = resultado.blendshapes[Blendshapes.SEMICERRAR_DIREITO] ?: 0.0,
            semicerrarEsquerdo = resultado.blendshapes[Blendshapes.SEMICERRAR_ESQUERDO] ?: 0.0,
        )
    }

    /** Média dos dois olhos de "olhar para cima" menos "olhar para baixo". Nulo se o modelo não deu esses blendshapes. */
    private fun olharVertical(b: Map<String, Double>): Double? {
        if (Blendshapes.OLHAR.none { it in b }) return null
        fun v(nome: String) = b[nome] ?: 0.0
        val cima = (v(Blendshapes.OLHAR_CIMA_DIREITO) + v(Blendshapes.OLHAR_CIMA_ESQUERDO)) / 2.0
        val baixo = (v(Blendshapes.OLHAR_BAIXO_DIREITO) + v(Blendshapes.OLHAR_BAIXO_ESQUERDO)) / 2.0
        return cima - baixo
    }

    private fun seis(pontos: (Int) -> Ponto2D, indices: List<Int>): SeisPontos {
        val p = indices.map { pixel(pontos(it)) }
        return SeisPontos(p[0], p[1], p[2], p[3], p[4], p[5])
    }

    /** EAR depende de distâncias reais: sem escalar x e y, um quadro 4:3 distorce a razão. */
    private fun pixel(n: Ponto2D) = Ponto2D(n.x * larguraPx, n.y * alturaPx)
}
