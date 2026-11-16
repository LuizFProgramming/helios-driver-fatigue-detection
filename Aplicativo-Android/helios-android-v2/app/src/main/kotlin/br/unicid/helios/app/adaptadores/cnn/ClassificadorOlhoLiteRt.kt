package br.unicid.helios.app.adaptadores.cnn

import android.content.Context
import br.unicid.helios.app.portas.ClassificadorOlho
import br.unicid.helios.protocolos.RecorteOlho
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * CNN do olho no LiteRT (Interpreter), só na CPU e sem Google Play Services. O modelo vai dentro do APK
 * (assets/olho_cnn.tflite, guardado sem compressão para ser mapeado direto da memória).
 *
 * Contrato do experimento (scripts/experimento_olhos): entrada 1x64x64x1 uint8 com escala 1 e ponto zero 0,
 * então os bytes do recorte entram como estão; saída 1x1 float32 = probabilidade de olho fechado.
 * Não é thread-safe: o DetectorMediapipe chama sempre da thread de resultados do MediaPipe.
 */
class ClassificadorOlhoLiteRt(context: Context) : ClassificadorOlho {

    private val interprete: Interpreter
    private val entrada = ByteBuffer.allocateDirect(RecorteOlho.LADO * RecorteOlho.LADO).order(ByteOrder.nativeOrder())
    private val saida = Array(1) { FloatArray(1) }

    init {
        val modelo = context.assets.openFd(MODELO).use { fd ->
            FileInputStream(fd.fileDescriptor).use {
                it.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
        // Uma thread: a rede é pequena e o MediaPipe já disputa a CPU quando cai do GPU.
        interprete = Interpreter(modelo, Interpreter.Options().setNumThreads(1))
    }

    override fun probFechado(recorte: ByteArray): Float {
        entrada.clear()
        entrada.put(recorte)
        entrada.rewind()
        interprete.run(entrada, saida)
        return saida[0][0]
    }

    override fun close() = interprete.close()

    companion object {
        const val MODELO = "olho_cnn.tflite"
    }
}
