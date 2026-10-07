package br.unicid.helios.app.adaptadores.opencv

import android.graphics.Bitmap
import br.unicid.helios.app.portas.PreProcessador
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.CLAHE
import org.opencv.imgproc.Imgproc

/**
 * Pré-processamento opcional com OpenCV para pouca luz e imagem infravermelha (LED 850 nm).
 *
 * Aplica CLAHE (equalização adaptativa de histograma com limite de contraste) só no canal L
 * do espaço Lab: clareia o rosto sem estourar as cores e sem mudar a geometria, então o
 * MediaPipe continua achando os mesmos 478 pontos. Desligado por padrão; ligue nos ajustes.
 *
 * Os Mat são reaproveitados entre frames para não alocar memória a cada imagem.
 * Não é thread-safe: chamar sempre da mesma thread.
 */
class RealceOpenCv(limiteContraste: Double = 2.0, grade: Int = 8) : PreProcessador, AutoCloseable {

    private val clahe: CLAHE
    private val rgba = Mat()
    private val rgb = Mat()
    private val lab = Mat()
    private val canais = ArrayList<Mat>(3)

    init {
        check(OpenCVLoader.initLocal()) { "OpenCV não carregou" }
        clahe = Imgproc.createCLAHE(limiteContraste, Size(grade.toDouble(), grade.toDouble()))
    }

    override fun processar(bitmap: Bitmap): Bitmap {
        Utils.bitmapToMat(bitmap, rgba)
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(rgb, lab, Imgproc.COLOR_RGB2Lab)
        Core.split(lab, canais)
        clahe.apply(canais[0], canais[0])
        Core.merge(canais, lab)
        Imgproc.cvtColor(lab, rgb, Imgproc.COLOR_Lab2RGB)
        Imgproc.cvtColor(rgb, rgba, Imgproc.COLOR_RGB2RGBA)
        val saida = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgba, saida)
        return saida
    }

    override fun close() {
        (listOf(rgba, rgb, lab) + canais).forEach(Mat::release)
    }
}
