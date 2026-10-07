package br.unicid.helios.app.portas

import android.graphics.Bitmap
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.protocolos.PadraoSonoro
import kotlinx.coroutines.flow.Flow

/**
 * Portas do lado Android. Cada etapa do pipeline do TCC é uma porta, e cada porta tem
 * um adaptador trocável:
 *
 *  Captura de imagem        → [FonteDeImagens]   (CameraX ou ESP32-CAM)
 *  (pré-processamento)      → [PreProcessador]   (nenhum ou realce OpenCV)
 *  Extração de características → [DetectorFacial] (MediaPipe Face Landmarker)
 *  Análise e classificação  → MonitorMotorista   (módulo :dominio, sem Android)
 *  Alerta                   → [SaidaAlarme]      (som + vibração, buzzer da ESP32)
 *  Sensores inerciais       → [FonteInercial]    (acelerômetro linear)
 */

/** Imagem já na orientação correta, com o instante da captura (SystemClock.elapsedRealtime). */
class Imagem(val bitmap: Bitmap, val timestampMs: Long)

enum class Conexao { CONECTANDO, CONECTADA, DESCONECTADA }

interface FonteDeImagens {
    val conexao: Flow<Conexao>
    fun imagens(): Flow<Imagem>
}

fun interface PreProcessador {
    fun processar(bitmap: Bitmap): Bitmap
}

interface DetectorFacial : AutoCloseable {
    /** Envia uma imagem; o resultado chega depois em [quadros] (modo LIVE_STREAM). */
    fun enviar(imagem: Imagem)
    val quadros: Flow<Quadro>
}

interface SaidaAlarme : AutoCloseable {
    fun tocar(padrao: PadraoSonoro)
    fun parar()
}

fun interface FonteInercial {
    /** Magnitude da aceleração linear (m/s², sem gravidade) com o instante em ms. */
    fun amostras(): Flow<Pair<Long, Double>>
}
