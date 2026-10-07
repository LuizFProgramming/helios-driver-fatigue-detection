package br.unicid.helios.app.adaptadores.alarme

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.VibratorManager
import br.unicid.helios.app.portas.SaidaAlarme
import br.unicid.helios.protocolos.PadraoSonoro
import br.unicid.helios.protocolos.gerarCicloPcm

/**
 * Alarme sonoro + vibração. O som sai pelo canal de ALARME (USAGE_ALARM), que no Android
 * ignora o modo silencioso de mídia. O ciclo de PCM vem pronto de [gerarCicloPcm];
 * aqui só se entrega o buffer ao AudioTrack em loop.
 */
class AlarmeAndroid(context: Context) : SaidaAlarme {

    private val vibrador = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private var faixa: AudioTrack? = null
    private var atual: PadraoSonoro? = null

    override fun tocar(padrao: PadraoSonoro) {
        if (padrao == atual) return
        parar()
        atual = padrao
        faixa = criarFaixa(gerarCicloPcm(padrao, TAXA_HZ)).also { it.play() }
        if (padrao.vibrar) {
            val forma = longArrayOf(0, padrao.ligadoMs.toLong(), padrao.desligadoMs.toLong())
            vibrador.vibrate(VibrationEffect.createWaveform(forma, 0))
        }
    }

    override fun parar() {
        faixa?.run { stop(); release() }
        faixa = null
        atual = null
        vibrador.cancel()
    }

    override fun close() = parar()

    private fun criarFaixa(pcm: ShortArray): AudioTrack {
        val faixa = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(TAXA_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        faixa.write(pcm, 0, pcm.size)
        faixa.setLoopPoints(0, pcm.size, -1)
        return faixa
    }

    private companion object {
        const val TAXA_HZ = 22_050
    }
}
