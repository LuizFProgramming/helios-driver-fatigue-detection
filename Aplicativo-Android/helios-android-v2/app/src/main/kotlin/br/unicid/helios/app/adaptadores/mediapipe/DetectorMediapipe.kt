package br.unicid.helios.app.adaptadores.mediapipe

import android.content.Context
import android.os.SystemClock
import br.unicid.helios.app.portas.ConsumidorVisual
import br.unicid.helios.app.portas.DetectorFacial
import br.unicid.helios.app.portas.Imagem
import br.unicid.helios.dominio.tipos.Ponto2D
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.protocolos.Blendshapes
import br.unicid.helios.protocolos.MapeadorFaceMesh
import br.unicid.helios.protocolos.ResultadoFaceMesh
import com.google.mediapipe.framework.MediaPipeException
import com.google.mediapipe.framework.image.BitmapExtractor
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
 *
 * Cada imagem enviada fica "pendente" até sair um resultado com timestamp igual ou maior. Só então
 * o bitmap é devolvido ao pool, porque o MediaPipe trabalha de forma assíncrona. Frames que ele
 * descartar por estar ocupado são liberados junto com o próximo resultado.
 */
class DetectorMediapipe(context: Context) : DetectorFacial {

    private val saida = MutableSharedFlow<Quadro>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val quadros: Flow<Quadro> = saida

    /** Qual delegate ficou ativo, para mostrar nos ajustes e medir no capítulo de resultados. */
    val delegate: Delegate

    private val landmarker: FaceLandmarker
    private var ultimoTimestampMs = -1L
    private val pendentes = ArrayDeque<Pair<Long, () -> Unit>>()

    @Volatile
    override var visual: ConsumidorVisual? = null

    /** Tempo entre a captura e o resultado do último frame, em ms (diagnóstico). */
    @Volatile
    var latenciaMs: Long = 0
        private set

    init {
        val (marcador, usado) = criar(context, Delegate.GPU) ?: (criar(context, Delegate.CPU) ?: error("MediaPipe indisponível"))
        landmarker = marcador
        delegate = usado
    }

    override fun enviar(imagem: Imagem) {
        // O MediaPipe exige timestamps estritamente crescentes.
        val ts = maxOf(imagem.timestampMs, ultimoTimestampMs + 1)
        ultimoTimestampMs = ts
        synchronized(pendentes) { pendentes.addLast(ts to imagem.liberar) }
        try {
            landmarker.detectAsync(BitmapImageBuilder(imagem.bitmap).build(), ts)
        } catch (e: RuntimeException) {
            liberarAte(ts) // MediaPipeException ou detector já fechado: o bitmap não pode ficar preso
            throw e
        }
    }

    override fun close() {
        landmarker.close()
        liberarAte(Long.MAX_VALUE)
    }

    private fun liberarAte(timestampMs: Long) {
        val prontos = synchronized(pendentes) {
            buildList {
                while (pendentes.firstOrNull()?.let { it.first <= timestampMs } == true) add(pendentes.removeFirst())
            }
        }
        prontos.forEach { it.second() }
    }

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
            .setErrorListener { liberarAte(Long.MAX_VALUE) }
            .build()
        FaceLandmarker.createFromOptions(context, opcoes) to delegate
    } catch (_: MediaPipeException) {
        null // delegate indisponível neste aparelho: tenta o próximo
    } catch (_: IllegalStateException) {
        null
    }

    private fun aoReceber(resultado: FaceLandmarkerResult, entrada: MPImage) {
        val ts = resultado.timestampMs()
        latenciaMs = SystemClock.elapsedRealtime() - ts
        try {
            saida.tryEmit(Quadro(ts, converter(resultado, entrada)))
            try {
                entregarVisual(resultado, entrada)
            } catch (e: RuntimeException) {
                // O visor é só para análise: uma falha nele nunca pode derrubar o alarme.
                android.util.Log.w("HeliosVisor", "Falha ao desenhar o visor", e)
            }
        } finally {
            liberarAte(ts)
        }
    }

    /** Visor e foto anotada: a imagem exata que o MediaPipe analisou, com todos os pontos (só quando alguém quer). */
    private fun entregarVisual(resultado: FaceLandmarkerResult, entrada: MPImage) {
        val consumidor = visual?.takeIf { it.quer() } ?: return
        val rosto = resultado.faceLandmarks().firstOrNull().orEmpty()
        val pontos = FloatArray(rosto.size * 2)
        rosto.forEachIndexed { i, p ->
            pontos[2 * i] = p.x()
            pontos[2 * i + 1] = p.y()
        }
        val matriz = resultado.facialTransformationMatrixes().orElse(emptyList()).firstOrNull()
        val valores = matriz?.let { m -> DoubleArray(m.size) { m[it].toDouble() } }
        consumidor.receber(BitmapExtractor.extract(entrada), pontos, valores)
    }

    /**
     * Só repassa dados para tipos simples; quem decide é o [MapeadorFaceMesh]. Os pontos são lidos
     * sob demanda (20 de 478) e só os blendshapes usados entram no mapa.
     */
    private fun converter(resultado: FaceLandmarkerResult, entrada: MPImage) =
        resultado.faceLandmarks().firstOrNull()?.let { rosto ->
            val blend = resultado.faceBlendshapes().orElse(emptyList()).firstOrNull().orEmpty()
            val matriz = resultado.facialTransformationMatrixes().orElse(emptyList()).firstOrNull()
            MapeadorFaceMesh(entrada.width, entrada.height).mapear(
                ResultadoFaceMesh(
                    totalPontos = rosto.size,
                    ponto = { i -> rosto[i].let { Ponto2D(it.x().toDouble(), it.y().toDouble()) } },
                    blendshapes = blend.filter { it.categoryName() in Blendshapes.USADOS }
                        .associate { it.categoryName() to it.score().toDouble() },
                    matrizTransformacao = matriz?.let { m -> DoubleArray(m.size) { m[it].toDouble() } },
                ),
            )
        }

    private companion object {
        const val MODELO = "face_landmarker.task"
    }
}
