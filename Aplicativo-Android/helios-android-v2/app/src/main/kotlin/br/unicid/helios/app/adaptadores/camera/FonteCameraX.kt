package br.unicid.helios.app.adaptadores.camera

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.app.portas.FonteDeImagens
import br.unicid.helios.app.portas.Imagem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Câmera do próprio aparelho (RF02). Usa ImageAnalysis do CameraX:
 * - KEEP_ONLY_LATEST: se o MediaPipe atrasar, frames velhos são descartados (latência baixa).
 * - RGBA_8888 + rotação feita pelo CameraX: os bytes já saem em pé, sem conversão manual de YUV.
 * - 640x480: o Face Landmarker redimensiona para 256x256 internamente; mais que isso só gasta bateria.
 *
 * Com [reaproveitarBitmaps], os pixels são copiados para bitmaps de um [PoolBitmaps] em vez de
 * `toBitmap()` criar um novo a cada frame. Dá para ligar e desligar nos ajustes e comparar fps e memória.
 *
 * O canal é de encontro (RENDEZVOUS): se o app ainda está processando o frame anterior, este é
 * descartado na hora e o bitmap volta ao pool. Assim nenhum bitmap fica esquecido numa fila.
 *
 * A câmera segue o ciclo de vida do APP inteiro (ProcessLifecycleOwner): para quando o app vai para
 * segundo plano, porque o Android bloqueia a câmera de apps em segundo plano (ERROR_CAMERA_DISABLED),
 * e volta sozinha quando o app reaparece. Deve ser coletada na thread principal.
 */
class FonteCameraX(
    private val context: Context,
    private val reaproveitarBitmaps: Boolean,
    private val lente: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
) : FonteDeImagens {

    private val estado = MutableStateFlow(Conexao.DESCONECTADA)
    override val conexao: Flow<Conexao> = estado
    private val contadorDescartados = AtomicLong()
    override val descartados: Long get() = contadorDescartados.get()

    private val pool = PoolBitmaps()
    private var compacto: ByteBuffer? = null

    override fun imagens(): Flow<Imagem> = callbackFlow {
        estado.value = Conexao.CONECTANDO
        val executor = Executors.newSingleThreadExecutor()
        val analise = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                    ).build(),
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setOutputImageRotationEnabled(true)
            .build()
        analise.setAnalyzer(executor) { proxy ->
            proxy.use { imagem -> converter(imagem)?.let { if (trySend(it).isFailure) descartar(it) } }
        }
        val futuro = ProcessCameraProvider.getInstance(context)
        futuro.addListener({
            futuro.get().bindToLifecycle(ProcessLifecycleOwner.get(), lente, analise)
            estado.value = Conexao.CONECTADA
        }, ContextCompat.getMainExecutor(context))

        awaitClose {
            if (futuro.isDone) futuro.get().unbind(analise)
            analise.clearAnalyzer()
            executor.shutdown()
            estado.value = Conexao.DESCONECTADA
        }
    }.buffer(Channel.RENDEZVOUS)

    private fun converter(proxy: ImageProxy): Imagem? {
        val agora = SystemClock.elapsedRealtime()
        if (!reaproveitarBitmaps) return Imagem(proxy.toBitmap(), agora)
        val bitmap = pool.obter(proxy.width, proxy.height)
        if (bitmap == null) {
            contadorDescartados.incrementAndGet()
            return null
        }
        copiarRgba(proxy, bitmap)
        return Imagem(bitmap, agora) { pool.devolver(bitmap) }
    }

    private fun descartar(imagem: Imagem) {
        contadorDescartados.incrementAndGet()
        imagem.liberar()
    }

    /** Copia o plano RGBA para o bitmap. Se a linha tiver enchimento (rowStride maior), copia linha a linha. */
    private fun copiarRgba(proxy: ImageProxy, destino: Bitmap) {
        val plano = proxy.planes[0]
        val origem = plano.buffer.apply { rewind() }
        val bytesPorLinha = proxy.width * BYTES_POR_PIXEL
        if (plano.rowStride == bytesPorLinha) {
            destino.copyPixelsFromBuffer(origem)
            return
        }
        val tamanho = bytesPorLinha * proxy.height
        val alvo = compacto?.takeIf { it.capacity() == tamanho }
            ?: ByteBuffer.allocateDirect(tamanho).also { compacto = it }
        alvo.clear()
        for (linha in 0 until proxy.height) {
            val inicio = linha * plano.rowStride
            origem.limit(inicio + bytesPorLinha).position(inicio)
            alvo.put(origem)
            origem.limit(origem.capacity())
        }
        alvo.rewind()
        destino.copyPixelsFromBuffer(alvo)
    }

    private companion object {
        const val BYTES_POR_PIXEL = 4
    }
}
