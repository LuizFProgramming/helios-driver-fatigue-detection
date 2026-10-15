package br.unicid.helios.app.adaptadores.camera

import android.graphics.Bitmap

/**
 * Pequeno conjunto de bitmaps reaproveitados entre frames. Sem ele, cada frame de 640x480
 * cria 1,2 MB novos (36 MB/s a 30 fps) para o coletor de lixo limpar, o que custa CPU e bateria.
 *
 * Um bitmap só volta ao pool quando o último usuário (o MediaPipe) chama [devolver]. Se todos
 * estiverem em uso, [obter] devolve nulo e o frame é descartado: o app sempre analisa o mais novo.
 */
class PoolBitmaps(private val capacidade: Int = 3) {
    private val livres = ArrayDeque<Bitmap>()
    private var largura = 0
    private var altura = 0
    private var criados = 0

    @Synchronized
    fun obter(w: Int, h: Int): Bitmap? {
        if (w != largura || h != altura) {
            livres.clear()
            criados = 0
            largura = w
            altura = h
        }
        livres.removeFirstOrNull()?.let { return it }
        if (criados >= capacidade) return null
        criados++
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Bitmaps de um tamanho antigo (a resolução mudou) são simplesmente esquecidos.
     * Devolver duas vezes o mesmo bitmap não o duplica (senão dois frames dividiriam os mesmos pixels).
     */
    @Synchronized
    fun devolver(bitmap: Bitmap) {
        val serve = bitmap.width == largura && bitmap.height == altura && livres.size < capacidade
        if (serve && livres.none { it === bitmap }) livres.addLast(bitmap)
    }
}
