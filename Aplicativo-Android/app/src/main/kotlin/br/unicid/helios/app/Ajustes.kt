package br.unicid.helios.app

import android.content.Context
import androidx.core.content.edit
import br.unicid.helios.app.adaptadores.esp32.ConfigEsp32
import br.unicid.helios.dominio.calibracao.Calibracao

enum class TipoFonte { CAMERA_DO_APARELHO, ESP32_CAM }

data class Ajustes(
    val fonte: TipoFonte = TipoFonte.CAMERA_DO_APARELHO,
    val esp32: ConfigEsp32 = ConfigEsp32(),
    val realceOpenCv: Boolean = false,
    val buzzerNaCamera: Boolean = false,
    val calibracao: Calibracao? = null,
)

/** Guarda os ajustes no próprio aparelho (SharedPreferences). Nada vai para fora do carro. */
class RepositorioAjustes(context: Context) {
    private val prefs = context.getSharedPreferences("helios", Context.MODE_PRIVATE)

    fun ler(): Ajustes {
        val padrao = Ajustes()
        val ear = prefs.getFloat(EAR, -1f)
        return Ajustes(
            fonte = TipoFonte.entries.getOrElse(prefs.getInt(FONTE, 0)) { padrao.fonte },
            esp32 = ConfigEsp32(
                ssid = prefs.getString(SSID, null) ?: padrao.esp32.ssid,
                senha = prefs.getString(SENHA, null) ?: padrao.esp32.senha,
                urlStream = prefs.getString(URL_STREAM, null) ?: padrao.esp32.urlStream,
                urlBuzzer = prefs.getString(URL_BUZZER, null) ?: padrao.esp32.urlBuzzer,
            ),
            realceOpenCv = prefs.getBoolean(OPENCV, false),
            buzzerNaCamera = prefs.getBoolean(BUZZER, false),
            calibracao = if (ear > 0f) Calibracao(ear.toDouble(), prefs.getFloat(PITCH, 0f).toDouble()) else null,
        )
    }

    fun salvar(a: Ajustes) = prefs.edit {
        putInt(FONTE, a.fonte.ordinal)
        putString(SSID, a.esp32.ssid)
        putString(SENHA, a.esp32.senha)
        putString(URL_STREAM, a.esp32.urlStream)
        putString(URL_BUZZER, a.esp32.urlBuzzer)
        putBoolean(OPENCV, a.realceOpenCv)
        putBoolean(BUZZER, a.buzzerNaCamera)
        putFloat(EAR, a.calibracao?.earReferencia?.toFloat() ?: -1f)
        putFloat(PITCH, a.calibracao?.pitchReferencia?.toFloat() ?: 0f)
    }

    private companion object {
        const val FONTE = "fonte"
        const val SSID = "ssid"
        const val SENHA = "senha"
        const val URL_STREAM = "url_stream"
        const val URL_BUZZER = "url_buzzer"
        const val OPENCV = "opencv"
        const val BUZZER = "buzzer"
        const val EAR = "ear_ref"
        const val PITCH = "pitch_ref"
    }
}
