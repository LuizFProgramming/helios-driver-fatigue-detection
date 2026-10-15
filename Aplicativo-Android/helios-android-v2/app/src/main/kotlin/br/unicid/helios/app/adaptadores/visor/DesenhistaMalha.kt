package br.unicid.helios.app.adaptadores.visor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import br.unicid.helios.protocolos.IndicesFaceMesh
import br.unicid.helios.protocolos.MalhaVisor
import br.unicid.helios.protocolos.eixosDaCabeca
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker

/** O que desenhar sobre a imagem: os pontos do MediaPipe, a pose e o estado de cada olho. */
class Malha(
    val pontos: FloatArray,
    val matriz: DoubleArray?,
    val olhoDireitoFechado: Boolean?,
    val olhoEsquerdoFechado: Boolean?,
    val olharFora: Boolean = false,
)

/**
 * Desenha a análise sobre a imagem com o Canvas do Android: contorno do rosto, sobrancelhas, olhos
 * (verde aberto, vermelho fechado, cada um pelo seu estado), íris (âmbar com o olhar fora da via), nariz, boca, os 20 pontos usados
 * no cálculo e os eixos da cabeça (x vermelho, y verde, z azul, a partir da ponta do nariz).
 * O mesmo desenho serve para o visor ao vivo e para a foto anotada.
 * Não é thread-safe: um desenhista por thread.
 */
class DesenhistaMalha {

    private fun traco(cor: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cor
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val contorno = traco(Color.argb(170, 237, 233, 225))
    private val nariz = traco(Color.rgb(120, 190, 255))
    private val boca = traco(Color.rgb(242, 169, 0))
    private val olhoAberto = traco(Color.rgb(63, 178, 127))
    private val olhoFechado = traco(Color.rgb(229, 50, 45))
    private val irisFora = traco(Color.rgb(242, 169, 0))
    private val ponto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val eixoX = traco(Color.rgb(229, 50, 45))
    private val eixoY = traco(Color.rgb(63, 178, 127))
    private val eixoZ = traco(Color.rgb(80, 140, 255))
    private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val fundoTexto = Paint().apply { color = Color.argb(170, 27, 32, 38) }

    fun desenhar(alvo: Bitmap, malha: Malha, linhas: List<String> = emptyList()) {
        val canvas = Canvas(alvo)
        val escala = alvo.width.coerceAtMost(alvo.height) / REFERENCIA_PX
        listOf(contorno, nariz, boca, olhoAberto, olhoFechado, irisFora).forEach { it.strokeWidth = TRACO * escala }
        listOf(eixoX, eixoY, eixoZ).forEach { it.strokeWidth = TRACO_EIXO * escala }
        if (malha.pontos.size >= IndicesFaceMesh.TOTAL_PONTOS * 2) {
            val p = Pontos(malha.pontos, alvo.width.toFloat(), alvo.height.toFloat())
            desenharRosto(canvas, p, malha)
            desenharPontosUsados(canvas, p, RAIO_PONTO * escala)
            malha.matriz?.let { desenharEixos(canvas, p, it, alvo.width * TAMANHO_EIXO) }
        }
        if (linhas.isNotEmpty()) desenharTexto(canvas, linhas, escala)
    }

    private class Pontos(private val n: FloatArray, private val w: Float, private val h: Float) {
        fun x(i: Int) = n[2 * i] * w
        fun y(i: Int) = n[2 * i + 1] * h
    }

    private fun desenharRosto(canvas: Canvas, p: Pontos, malha: Malha) {
        linhas(canvas, p, OVAL_E_SOBRANCELHAS, contorno)
        linhas(canvas, p, IRIS, if (malha.olharFora) irisFora else contorno)
        linhas(canvas, p, LABIOS, boca)
        linhas(canvas, p, OLHO_DIREITO, if (malha.olhoDireitoFechado == true) olhoFechado else olhoAberto)
        linhas(canvas, p, OLHO_ESQUERDO, if (malha.olhoEsquerdoFechado == true) olhoFechado else olhoAberto)
        polilinha(canvas, p, MalhaVisor.NARIZ, nariz)
        polilinha(canvas, p, MalhaVisor.NARINAS, nariz)
    }

    private fun desenharPontosUsados(canvas: Canvas, p: Pontos, raio: Float) {
        IndicesFaceMesh.USADOS.forEach { canvas.drawCircle(p.x(it), p.y(it), raio, ponto) }
    }

    private fun desenharEixos(canvas: Canvas, p: Pontos, matriz: DoubleArray, tamanho: Float) {
        val ox = p.x(MalhaVisor.PONTA_DO_NARIZ)
        val oy = p.y(MalhaVisor.PONTA_DO_NARIZ)
        val e = eixosDaCabeca(matriz)
        listOf(e.x to eixoX, e.y to eixoY, e.z to eixoZ).forEach { (v, tinta) ->
            canvas.drawLine(ox, oy, ox + v.x.toFloat() * tamanho, oy + v.y.toFloat() * tamanho, tinta)
        }
    }

    private fun desenharTexto(canvas: Canvas, linhas: List<String>, escala: Float) {
        texto.textSize = TEXTO * escala
        val altura = texto.textSize * ENTRELINHA
        val largura = linhas.maxOf { texto.measureText(it) }
        val margem = MARGEM * escala
        canvas.drawRect(0f, 0f, largura + 2 * margem, altura * linhas.size + 2 * margem, fundoTexto)
        linhas.forEachIndexed { i, linha ->
            canvas.drawText(linha, margem, margem + altura * (i + 1) - texto.descent(), texto)
        }
    }

    private fun linhas(canvas: Canvas, p: Pontos, pares: IntArray, tinta: Paint) {
        for (k in pares.indices step 2) {
            canvas.drawLine(p.x(pares[k]), p.y(pares[k]), p.x(pares[k + 1]), p.y(pares[k + 1]), tinta)
        }
    }

    private fun polilinha(canvas: Canvas, p: Pontos, indices: List<Int>, tinta: Paint) {
        indices.zipWithNext { a, b -> canvas.drawLine(p.x(a), p.y(a), p.x(b), p.y(b), tinta) }
    }

    private companion object {
        const val REFERENCIA_PX = 480f
        const val TRACO = 1.6f
        const val TRACO_EIXO = 3f
        const val RAIO_PONTO = 2.4f
        const val TAMANHO_EIXO = 0.12f
        const val TEXTO = 15f
        const val ENTRELINHA = 1.3f
        const val MARGEM = 8f

        fun pares(vararg conjuntos: Set<com.google.mediapipe.tasks.components.containers.Connection>): IntArray =
            conjuntos.flatMap { c -> c.flatMap { listOf(it.start(), it.end()) } }.toIntArray()

        val OVAL_E_SOBRANCELHAS = pares(
            FaceLandmarker.FACE_LANDMARKS_FACE_OVAL,
            FaceLandmarker.FACE_LANDMARKS_LEFT_EYE_BROW,
            FaceLandmarker.FACE_LANDMARKS_RIGHT_EYE_BROW,
        )
        val IRIS = pares(FaceLandmarker.FACE_LANDMARKS_LEFT_IRIS, FaceLandmarker.FACE_LANDMARKS_RIGHT_IRIS)
        val LABIOS = pares(FaceLandmarker.FACE_LANDMARKS_LIPS)

        // No MediaPipe, LEFT_EYE é o olho esquerdo da pessoa (o "olhoEsquerdo" do domínio).
        val OLHO_ESQUERDO = pares(FaceLandmarker.FACE_LANDMARKS_LEFT_EYE)
        val OLHO_DIREITO = pares(FaceLandmarker.FACE_LANDMARKS_RIGHT_EYE)
    }
}

/**
 * Três bitmaps em rodízio para o visor: um sendo desenhado, um na tela e um de folga,
 * para a tela nunca mostrar um quadro pela metade.
 */
class BuffersVisor {
    private val buffers = arrayOfNulls<Bitmap>(QUANTIDADE)
    private var proximo = 0

    fun obter(largura: Int, altura: Int): Bitmap {
        val atual = buffers[proximo]?.takeIf { it.width == largura && it.height == altura }
            ?: Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888).also { buffers[proximo] = it }
        proximo = (proximo + 1) % QUANTIDADE
        return atual
    }

    private companion object {
        const val QUANTIDADE = 3
    }
}
