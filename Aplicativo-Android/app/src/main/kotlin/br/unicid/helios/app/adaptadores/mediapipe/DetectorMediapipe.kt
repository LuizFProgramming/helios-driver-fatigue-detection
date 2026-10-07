package br.unicid.helios.app.adaptadores.mediapipe

import android.content.Context
import br.unicid.helios.app.portas.DetectorFacial
import br.unicid.helios.app.portas.Imagem
import br.unicid.helios.dominio.tipos.Ponto2D
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.protocolos.MapeadorFaceMesh
import br.unicid.helios.protocolos.ResultadoFaceMesh
import com.google.mediapipe.framework.MediaPipeException
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * MediaPipe Face Landmarker nativo (mesmo modelo do protótipo Python), em modo LIVE_STREAM.
 *
 * Por que é mais rápido que a versão Web planejada antes: roda direto na GPU do aparelho
 * (delegate GPU) e não passa por WebView, canvas nem WASM. Se a GPU não estiver disponível
 * (algumas centrais multimídia), cai para CPU automaticamente.
 *
 * O modelo vai dentro do APK (assets/face_landmarker.task): nenhuma requisição externa.
 */
class DetectorMediapipe(context: Context) : DetectorFacial {

    private val saida = MutableSharedFlow<Quadro>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val quadros: Flow<Quadro> = saida

    /** Qual delegate ficou ativo, para mostrar nos ajustes e medir no capítulo de resultados. */
    val delegate: Delegate

    private val landmarker: FaceLandmarker
    private var ultimoTimestampMs = -1L

    init {
        val (marcador, usado) = criar(context, Delegate.GPU) ?: (criar(context, Delegate.CPU) ?: error("MediaPipe indisponível"))
        landmarker = marcador
        delegate = usado
    }

    override fun enviar(imagem: Imagem) {
        // O MediaPipe exige timestamps estritamente crescentes.
        val ts = maxOf(imagem.timestampMs, ultimoTimestampMs + 1)
        ultimoTimestampMs = ts
        landmarker.detectAsync(BitmapImageBuilder(imagem.bitmap).build(), ts)
    }

    override fun close() = landmarker.close()

    private fun criar(context: Context, delegate: Delegate): Pair<FaceLandmarker, Delegate>? = try {
        val base = BaseOptions.builder()
            .setModelAssetPath(MODELO)
            .setDelegate(delegate)
            .build()
        val opcoes = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setOutputFaceBlendshapes(true)
            .setOutputFacialTransformationMatrixes(true)
            .setResultListener(::aoReceber)
            .build()
        FaceLandmarker.createFromOptions(context, opcoes) to delegate
    } catch (_: MediaPipeException) {
        null // delegate indisponível neste aparelho: tenta o próximo
    } catch (_: IllegalStateException) {
        null
    }

    private fun aoReceber(resultado: FaceLandmarkerResult, entrada: MPImage) {
        saida.tryEmit(Quadro(resultado.timestampMs(), converter(resultado, entrada)))
    }

    /** Só copia dados para tipos simples; quem decide é o [MapeadorFaceMesh]. */
    private fun converter(resultado: FaceLandmarkerResult, entrada: MPImage) =
        resultado.faceLandmarks().firstOrNull()?.let { rosto ->
            val blend = resultado.faceBlendshapes().orElse(emptyList()).firstOrNull().orEmpty()
            val matriz = resultado.facialTransformationMatrixes().orElse(emptyList()).firstOrNull()
            MapeadorFaceMesh(entrada.width, entrada.height).mapear(
                ResultadoFaceMesh(
                    pontosNormalizados = rosto.map { Ponto2D(it.x().toDouble(), it.y().toDouble()) },
                    blendshapes = blend.associate { it.categoryName() to it.score().toDouble() },
                    matrizTransformacao = matriz?.let { m -> DoubleArray(m.size) { m[it].toDouble() } },
                ),
            )
        }

    private companion object {
        const val MODELO = "face_landmarker.task"
    }
}
