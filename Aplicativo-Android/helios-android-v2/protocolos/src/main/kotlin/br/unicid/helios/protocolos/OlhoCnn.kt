package br.unicid.helios.protocolos

import br.unicid.helios.dominio.sessao.CORTE_CNN
import br.unicid.helios.dominio.tipos.Ponto2D
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.round

private const val BYTE = 0xFF

/** Caixa do recorte em pixels da imagem, com fim exclusivo. Pode passar da borda: aí a borda se repete. */
data class CaixaRecorte(val x0: Int, val y0: Int, val x1: Int, val y1: Int)

/**
 * Recorte do olho que entra na CNN (models/olho_cnn.tflite), igual ao `recortar_olho` de
 * scripts/experimento_olhos/extrair.py: o app precisa mostrar à rede a mesma imagem que ela viu no treino.
 * "Direito" e "esquerdo" são da imagem, como em [IndicesFaceMesh].
 */
object RecorteOlho {
    /** Entrada da rede: 64x64 pixels de cinza, um byte cada (0 a 255). */
    const val LADO = 64
    private const val MARGEM = 1.6
    private const val MEIO_MINIMO_PX = 4.0
    private const val PESO_R = 0.299
    private const val PESO_G = 0.587
    private const val PESO_B = 0.114

    val CONTORNO_DIREITO_IMAGEM = listOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246)
    val CONTORNO_ESQUERDO_IMAGEM =
        listOf(362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398)

    /**
     * Quadrado centrado na média do [contorno], com lado de 1,6 x a distância entre os cantos (p1 e p4 do EAR),
     * no mínimo 8 px. O lado não muda com o olho inclinado. Arredonda meio pixel para o par, como o Python.
     */
    fun caixa(contorno: List<Ponto2D>, canto1: Ponto2D, canto4: Ponto2D): CaixaRecorte {
        val cx = contorno.sumOf { it.x } / contorno.size
        val cy = contorno.sumOf { it.y } / contorno.size
        val meio = maxOf(MEIO_MINIMO_PX, MARGEM * hypot(canto1.x - canto4.x, canto1.y - canto4.y) / 2)
        return CaixaRecorte(px(cx - meio), px(cy - meio), px(cx + meio), px(cy + meio))
    }

    /**
     * Os 4096 bytes de entrada: luminância de cada pixel da [caixa], borda replicada fora da imagem
     * ([argb] só recebe coordenadas dentro dela) e redução para 64x64 pela média da área coberta.
     */
    fun entrada(caixa: CaixaRecorte, largura: Int, altura: Int, argb: (x: Int, y: Int) -> Int): ByteArray {
        val w = caixa.x1 - caixa.x0
        val h = caixa.y1 - caixa.y0
        val cinza = IntArray(w * h) { i ->
            luminancia(argb((caixa.x0 + i % w).coerceIn(0, largura - 1), (caixa.y0 + i / w).coerceIn(0, altura - 1)))
        }
        val pesosX = pesosDaArea(w)
        val pesosY = pesosDaArea(h)
        return ByteArray(LADO * LADO) { k ->
            var soma = 0.0
            for ((j, py) in pesosY[k / LADO]) for ((i, px) in pesosX[k % LADO]) soma += py * px * cinza[j * w + i]
            round(soma).toInt().toByte()
        }
    }

    fun luminancia(argb: Int): Int {
        val r = (argb shr 16) and BYTE
        val g = (argb shr 8) and BYTE
        val b = argb and BYTE
        return round(PESO_R * r + PESO_G * g + PESO_B * b).toInt()
    }

    /** Para cada pixel de saída, os pixels de entrada que ele cobre e a fração da área de cada um (soma 1). */
    private fun pesosDaArea(n: Int): List<List<Pair<Int, Double>>> = (0 until LADO).map { u ->
        val inicio = u * n.toDouble() / LADO
        val fim = (u + 1) * n.toDouble() / LADO
        (floor(inicio).toInt() until ceil(fim).toInt()).map { s ->
            s to (minOf(fim, s + 1.0) - maxOf(inicio, s.toDouble())) / (fim - inicio)
        }
    }

    private fun px(v: Double) = round(v).toInt()
}

private const val ALFA_OPACO = 0xFF000000.toInt()

/** Repete o cinza nos canais R, G e B. */
private const val CINZA_EM_RGB = 0x010101

/** Recorte em cinza como pixels ARGB opacos, para desenhar no visor o que a rede viu. */
fun argbDeCinza(cinza: ByteArray): IntArray = IntArray(cinza.size) { i ->
    ALFA_OPACO or ((cinza[i].toInt() and BYTE) * CINZA_EM_RGB)
}

/** Decisão da CNN para o visor (borda verde ou vermelha). Modo sombra: não muda nenhum estado. */
fun cnnFechado(probabilidade: Double): Boolean = probabilidade >= CORTE_CNN
