package br.unicid.helios.app.adaptadores.esp32

import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.SystemClock
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.app.portas.FonteDeImagens
import br.unicid.helios.app.portas.Imagem
import br.unicid.helios.protocolos.ExtratorJpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

data class ConfigEsp32(
    val ssid: String = "HELIOS-CAM",
    val senha: String = "",
    /** Porta 81 = CameraWebServer da Espressif. O firmware próprio (Fase 4) pode usar /stream na 80. */
    val urlStream: String = "http://192.168.4.1:81/stream",
    val urlBuzzer: String = "http://192.168.4.1/buzzer",
)

/**
 * Pede ao Android a rede Wi-Fi da câmera *só para este app* (WifiNetworkSpecifier).
 * O celular continua com os dados móveis para o resto do sistema, e o tráfego do HELIOS
 * vai pela rede sem internet da ESP32. Isso resolve o risco do spike 0.2 do roadmap.
 * Na primeira vez o Android mostra uma janela para o motorista aprovar a rede.
 *
 * RNF01: assim que a rede da câmera aparece, o processo inteiro do app fica preso a ela
 * (bindProcessToNetwork). Qualquer conexão de qualquer biblioteca só consegue ir para a
 * rede HELIOS-CAM, que não tem internet. Se a câmera cair, o app continua preso à rede
 * perdida (as conexões falham em vez de escapar pelos dados móveis) até [liberar].
 */
class RedeHeliosCam(context: Context, private val config: ConfigEsp32) {
    private val conectividade = context.getSystemService(ConnectivityManager::class.java)
    private val rede = MutableStateFlow<Network?>(null)
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun pedir() {
        if (callback != null) return
        val especificador = WifiNetworkSpecifier.Builder().setSsid(config.ssid)
            .apply { if (config.senha.isNotEmpty()) setWpa2Passphrase(config.senha) }
            .build()
        val pedido = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(especificador)
            .build()
        val novo = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                conectividade.bindProcessToNetwork(network)
                rede.value = network
            }

            override fun onLost(network: Network) { rede.value = null }
        }
        callback = novo
        conectividade.requestNetwork(pedido, novo)
    }

    suspend fun aguardar(): Network = rede.filterNotNull().first()

    fun atual(): Network? = rede.value

    /** Chamado ao trocar para a câmera do aparelho ou fechar o app: devolve a rede padrão ao processo. */
    fun liberar() {
        conectividade.bindProcessToNetwork(null)
        callback?.let(conectividade::unregisterNetworkCallback)
        callback = null
        rede.value = null
    }
}

/**
 * Lê o MJPEG da ESP32-CAM (RF01). Este arquivo só abre o socket e repassa bytes:
 * a separação das imagens fica no [ExtratorJpeg], testado sem Android.
 * Reconecta sozinho com espera crescente: 1, 2, 4, 8 s (RF11).
 */
class FonteEsp32(private val rede: RedeHeliosCam, private val config: ConfigEsp32) : FonteDeImagens {

    private val estado = MutableStateFlow(Conexao.DESCONECTADA)
    override val conexao: Flow<Conexao> = estado

    override fun imagens(): Flow<Imagem> = flow {
        rede.pedir()
        var espera = ESPERA_INICIAL_MS
        while (coroutineContext.isActive) {
            estado.value = Conexao.CONECTANDO
            try {
                ler(rede.aguardar()) { emit(it); espera = ESPERA_INICIAL_MS }
            } catch (_: IOException) {
                // Câmera desligada, fora de alcance ou reiniciando: tenta de novo.
            }
            estado.value = Conexao.DESCONECTADA
            delay(espera)
            espera = (espera * 2).coerceAtMost(ESPERA_MAXIMA_MS)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun ler(network: Network, aoReceber: suspend (Imagem) -> Unit) {
        val conexaoHttp = network.openConnection(URL(config.urlStream)) as HttpURLConnection
        conexaoHttp.connectTimeout = TEMPO_LIMITE_MS
        conexaoHttp.readTimeout = TEMPO_LIMITE_MS
        try {
            val extrator = ExtratorJpeg()
            val buffer = ByteArray(TAMANHO_LEITURA)
            conexaoHttp.inputStream.use { entrada ->
                estado.value = Conexao.CONECTADA
                while (coroutineContext.isActive) {
                    val lidos = entrada.read(buffer)
                    if (lidos < 0) break
                    for (jpeg in extrator.alimentar(buffer, lidos)) {
                        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.let {
                            aoReceber(Imagem(it, SystemClock.elapsedRealtime()))
                        }
                    }
                }
            }
        } finally {
            conexaoHttp.disconnect()
        }
    }

    private companion object {
        const val ESPERA_INICIAL_MS = 1_000L
        const val ESPERA_MAXIMA_MS = 8_000L
        const val TEMPO_LIMITE_MS = 3_000
        const val TAMANHO_LEITURA = 16 * 1024
    }
}

/** Toca o buzzer da câmera (RF07, opcional). Dispara e esquece; falha não atrapalha o alarme do aparelho. */
class BuzzerEsp32(private val rede: RedeHeliosCam, private val config: ConfigEsp32, private val escopo: CoroutineScope) {
    fun tocar(codigo: Int) {
        val network = rede.atual() ?: return
        escopo.launch(Dispatchers.IO) {
            try {
                val c = network.openConnection(URL("${config.urlBuzzer}?padrao=$codigo")) as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 1_000
                c.readTimeout = 1_000
                c.responseCode
                c.disconnect()
            } catch (_: IOException) {
                // Sem buzzer: o alarme do aparelho continua tocando.
            }
        }
    }
}
