package br.unicid.helios.app.adaptadores.voz

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import br.unicid.helios.app.portas.SaidaVoz
import java.util.Locale

/**
 * Alerta falado em português com o TextToSpeech do próprio Android (sem biblioteca extra).
 * Sai pelo canal de ALARME, como o bipe, para tocar mesmo com o volume de mídia baixo.
 *
 * Se o aparelho não tiver voz pt-BR instalada, o app segue só com o bipe e a vibração.
 * Para funcionar sem internet, baixe a voz em Configurações > Sistema > Idioma > Saída de texto para voz.
 */
class VozAndroid(context: Context) : SaidaVoz {

    @Volatile
    private var pronta = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) configurar()
        }
    }

    private fun configurar() {
        val resultado = tts.setLanguage(Locale.forLanguageTag("pt-BR"))
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        pronta = resultado != TextToSpeech.LANG_MISSING_DATA && resultado != TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun falar(texto: String) {
        if (pronta) tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, ID_FALA)
    }

    override fun calar() {
        if (pronta) tts.stop()
    }

    override fun close() {
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val ID_FALA = "helios-alerta"
    }
}
