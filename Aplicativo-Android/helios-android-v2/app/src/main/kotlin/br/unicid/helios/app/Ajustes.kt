package br.unicid.helios.app

import android.content.Context
import androidx.core.content.edit
import br.unicid.helios.app.adaptadores.esp32.ConfigEsp32
import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.protocolos.Retencao

enum class TipoFonte { CAMERA_DO_APARELHO, ESP32_CAM }

private const val MAX_FOTOS_PADRAO = 100

/**
 * Fotos dos alertas: só são gravadas com [consentimento]. [pasta] nula = pasta fixa do app na memória
 * interna (Pictures/HELIOS); senão, a pasta escolhida no seletor do Android. Os CSV vão para o mesmo lugar
 * (Download/HELIOS no padrão). [maxFotos] e [retencao] formam o limite físico do que fica guardado.
 */
data class AjustesFotos(
    val consentimento: Boolean = false,
    val pasta: String? = null,
    val maxFotos: Int = MAX_FOTOS_PADRAO,
    val retencao: Retencao = Retencao.TRES_HORAS,
) {
    val ativas: Boolean get() = consentimento
}

data class Ajustes(
    val fonte: TipoFonte = TipoFonte.CAMERA_DO_APARELHO,
    val esp32: ConfigEsp32 = ConfigEsp32(),
    val realceOpenCv: Boolean = false,
    val buzzerNaCamera: Boolean = false,
    val calibracao: Calibracao? = null,
    val alertaPorVoz: Boolean = true,
    val fotos: AjustesFotos = AjustesFotos(),
    val reaproveitarBitmaps: Boolean = true,
    val mostrarDiagnostico: Boolean = false,
    val mostrarVisor: Boolean = true,
    /** Se o app já perguntou na abertura se pode guardar fotos (a pergunta aparece uma vez só). */
    val perguntouFotos: Boolean = false,
    /** Tamanho do texto da tela de direção (1 = padrão). Centrais mais longe do motorista pedem letra maior. */
    val escalaTexto: Float = 1f,
)

/** Tempo de uso acumulado do monitoramento, base da política de retenção das fotos. */
interface UsoMonitoramento {
    fun usoTotalMs(): Long
    fun salvarUsoTotal(ms: Long)
}

/** Guarda os ajustes no próprio aparelho (SharedPreferences). Nada vai para fora do carro. */
class RepositorioAjustes(context: Context) : UsoMonitoramento {
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
            calibracao = if (ear > 0f) {
                Calibracao(
                    ear.toDouble(),
                    prefs.getFloat(PITCH, 0f).toDouble(),
                    prefs.getFloat(YAW, 0f).toDouble(),
                    prefs.getFloat(EAR_DIREITO, -1f).takeIf { it > 0f }?.toDouble(),
                    prefs.getFloat(EAR_ESQUERDO, -1f).takeIf { it > 0f }?.toDouble(),
                    lerOpcional(OLHAR_H),
                    lerOpcional(OLHAR_V),
                )
            } else {
                null
            },
            alertaPorVoz = prefs.getBoolean(VOZ, padrao.alertaPorVoz),
            fotos = AjustesFotos(
                consentimento = prefs.getBoolean(FOTOS_CONSENTIMENTO, false),
                pasta = prefs.getString(FOTOS_PASTA, null),
                maxFotos = prefs.getInt(FOTOS_MAX, padrao.fotos.maxFotos),
                retencao = Retencao.entries.getOrElse(prefs.getInt(FOTOS_RETENCAO, padrao.fotos.retencao.ordinal)) {
                    padrao.fotos.retencao
                },
            ),
            reaproveitarBitmaps = prefs.getBoolean(POOL, padrao.reaproveitarBitmaps),
            mostrarDiagnostico = prefs.getBoolean(DIAGNOSTICO, false),
            mostrarVisor = prefs.getBoolean(VISOR, padrao.mostrarVisor),
            perguntouFotos = prefs.getBoolean(PERGUNTOU_FOTOS, false),
            escalaTexto = prefs.getFloat(ESCALA_TEXTO, padrao.escalaTexto),
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
        putFloat(YAW, a.calibracao?.yawReferencia?.toFloat() ?: 0f)
        putFloat(EAR_DIREITO, a.calibracao?.earDireito?.toFloat() ?: -1f)
        putFloat(EAR_ESQUERDO, a.calibracao?.earEsquerdo?.toFloat() ?: -1f)
        gravarOpcional(OLHAR_H, a.calibracao?.olharHorizontal)
        gravarOpcional(OLHAR_V, a.calibracao?.olharVertical)
        putBoolean(VOZ, a.alertaPorVoz)
        putBoolean(FOTOS_CONSENTIMENTO, a.fotos.consentimento)
        putString(FOTOS_PASTA, a.fotos.pasta)
        putInt(FOTOS_MAX, a.fotos.maxFotos)
        putInt(FOTOS_RETENCAO, a.fotos.retencao.ordinal)
        putBoolean(POOL, a.reaproveitarBitmaps)
        putBoolean(DIAGNOSTICO, a.mostrarDiagnostico)
        putBoolean(VISOR, a.mostrarVisor)
        putBoolean(PERGUNTOU_FOTOS, a.perguntouFotos)
        putFloat(ESCALA_TEXTO, a.escalaTexto)
    }

    /** O olhar vertical pode ser negativo: "ausente" é a chave não existir, não um valor mágico. */
    private fun lerOpcional(chave: String): Double? = if (prefs.contains(chave)) prefs.getFloat(chave, 0f).toDouble() else null

    private fun android.content.SharedPreferences.Editor.gravarOpcional(chave: String, valor: Double?) {
        if (valor == null) remove(chave) else putFloat(chave, valor.toFloat())
    }

    /** Tempo total de uso do monitoramento, base da retenção das fotos. */
    override fun usoTotalMs(): Long = prefs.getLong(USO_TOTAL, 0L)

    override fun salvarUsoTotal(ms: Long) = prefs.edit { putLong(USO_TOTAL, ms) }

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
        const val YAW = "yaw_ref"
        const val EAR_DIREITO = "ear_ref_direito"
        const val EAR_ESQUERDO = "ear_ref_esquerdo"
        const val OLHAR_H = "olhar_ref_horizontal"
        const val OLHAR_V = "olhar_ref_vertical"
        const val VISOR = "visor"
        const val PERGUNTOU_FOTOS = "perguntou_fotos"
        const val VOZ = "voz"
        const val FOTOS_CONSENTIMENTO = "fotos_consentimento"
        const val FOTOS_PASTA = "fotos_pasta"
        const val FOTOS_MAX = "fotos_max"
        const val FOTOS_RETENCAO = "fotos_retencao"
        const val POOL = "pool_bitmaps"
        const val DIAGNOSTICO = "diagnostico"
        const val USO_TOTAL = "uso_total_ms"
        const val ESCALA_TEXTO = "escala_texto"
    }
}
