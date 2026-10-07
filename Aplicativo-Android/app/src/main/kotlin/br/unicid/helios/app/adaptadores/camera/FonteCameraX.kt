package br.unicid.helios.app.adaptadores.camera

import android.content.Context
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.app.portas.FonteDeImagens
import br.unicid.helios.app.portas.Imagem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executors

/**
 * Câmera do próprio aparelho (RF02). Usa ImageAnalysis do CameraX:
 * - KEEP_ONLY_LATEST: se o MediaPipe atrasar, frames velhos são descartados (latência baixa).
 * - RGBA_8888 + rotação feita pelo CameraX: o bitmap já sai em pé, sem conversão manual de YUV.
 * - 640x480: o Face Landmarker redimensiona para 256x256 internamente; mais que isso só gasta bateria.
 *
 * A câmera segue o ciclo de vida do APP inteiro (ProcessLifecycleOwner): para quando o app vai para
 * segundo plano, porque o Android bloqueia a câmera de apps em segundo plano (ERROR_CAMERA_DISABLED),
 * e volta sozinha quando o app reaparece. Sobrevive à rotação da tela porque vive no ViewModel.
 * Deve ser coletada na thread principal.
 */
class FonteCameraX(
    private val context: Context,
    private val lente: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
) : FonteDeImagens {

    private val estado = MutableStateFlow(Conexao.DESCONECTADA)
    override val conexao: Flow<Conexao> = estado

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
            proxy.use { trySend(Imagem(it.toBitmap(), SystemClock.elapsedRealtime())) }
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
    }
}
