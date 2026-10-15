package br.unicid.helios.protocolos

/**
 * Separa as imagens JPEG de um fluxo MJPEG (multipart/x-mixed-replace) da ESP32-CAM.
 *
 * Em vez de depender do boundary do multipart, procura os marcadores do próprio JPEG:
 * início FF D8 e fim FF D9. Dentro dos dados comprimidos todo FF é seguido de 00
 * (byte stuffing), então FF D9 só aparece no fim da imagem. Isso funciona com o
 * CameraWebServer da Espressif e com qualquer boundary.
 *
 * Função pura, sem rede: o adaptador Android só entrega os pedaços lidos do socket.
 */
class ExtratorJpeg(private val tamanhoMaximoBytes: Int = 512 * 1024) {
    private var buffer = ByteArray(64 * 1024)
    private var usados = 0

    fun alimentar(pedaco: ByteArray, tamanho: Int = pedaco.size): List<ByteArray> {
        garantirEspaco(tamanho)
        pedaco.copyInto(buffer, usados, 0, tamanho)
        usados += tamanho
        return extrairCompletos()
    }

    private fun extrairCompletos(): List<ByteArray> {
        val imagens = mutableListOf<ByteArray>()
        var cursor = 0
        while (true) {
            val inicio = procurar(MARCADOR_INICIO, cursor)
            if (inicio < 0) {
                cursor = manterUltimoByte()
                break
            }
            val fim = procurar(MARCADOR_FIM, inicio + 2)
            if (fim < 0) {
                cursor = inicio
                break
            }
            imagens += buffer.copyOfRange(inicio, fim + 2)
            cursor = fim + 2
        }
        descartarAte(cursor)
        return imagens
    }

    /** Sem início de JPEG: só o último byte pode ser metade de um FF D8. */
    private fun manterUltimoByte(): Int = (usados - 1).coerceAtLeast(0)

    private fun procurar(marcador: Int, desde: Int): Int {
        for (i in desde until usados - 1) {
            if (buffer[i] == FF && buffer[i + 1] == marcador.toByte()) return i
        }
        return -1
    }

    private fun descartarAte(posicao: Int) {
        buffer.copyInto(buffer, 0, posicao, usados)
        usados -= posicao
    }

    /** Lixo sem fim de JPEG por muito tempo é descartado para não crescer sem limite. */
    private fun garantirEspaco(chegando: Int) {
        if (usados + chegando > tamanhoMaximoBytes) usados = 0
        if (usados + chegando > buffer.size) {
            buffer = buffer.copyOf(maxOf(buffer.size * 2, usados + chegando))
        }
    }

    private companion object {
        const val FF: Byte = 0xFF.toByte()
        const val MARCADOR_INICIO = 0xD8
        const val MARCADOR_FIM = 0xD9
    }
}
