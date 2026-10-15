package br.unicid.helios.app.adaptadores.fotos

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import br.unicid.helios.app.portas.GravadorFotos
import java.io.IOException
import java.io.OutputStream

/**
 * Grava as fotos e os CSV do HELIOS.
 *
 * Sem pasta escolhida (o padrão), usa a pasta fixa do app na memória interna do celular, criada
 * pelo próprio Android (MediaStore): fotos em Pictures/HELIOS e CSV em Download/HELIOS. Não precisa
 * de permissão nem de seletor, e a pasta aparece na galeria, no app Arquivos e no PC pelo cabo USB.
 * Com uma pasta escolhida no seletor do Android (Storage Access Framework), grava nela.
 *
 * Atenção (LGPD): nessas pastas a galeria e outros apps enxergam as fotos, e o backup automático
 * do celular pode mandá-las para a nuvem.
 */
class GravadorArquivos(private val resolver: ContentResolver) : GravadorFotos {

    override fun salvarJpeg(pasta: String?, nome: String, bitmap: Bitmap): String? {
        val uri = criar(pasta, nome, "image/jpeg") ?: return null
        val ok = escrever(uri) { saida ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida)) throw IOException("JPEG não gerado")
        }
        return if (ok) uri.toString() else null.also { excluir(uri.toString()) } // não deixa arquivo vazio
    }

    override fun salvarTexto(pasta: String?, nome: String, conteudo: String): Boolean {
        val uri = criar(pasta, nome, "text/csv") ?: return false
        val ok = escrever(uri) { saida -> saida.write(conteudo.toByteArray(Charsets.UTF_8)) }
        if (!ok) excluir(uri.toString())
        return ok
    }

    override fun excluir(uri: String): Boolean = tentar {
        val u = Uri.parse(uri)
        if (u.authority == MediaStore.AUTHORITY) resolver.delete(u, null, null) > 0 else DocumentsContract.deleteDocument(resolver, u)
    } ?: false

    /** Miniatura para o histórico; nulo se o arquivo sumiu (apagado à mão, pasta trocada). */
    fun miniatura(uri: String, largura: Int, altura: Int): Bitmap? = tentar {
        resolver.loadThumbnail(Uri.parse(uri), Size(largura, altura), null)
    } ?: abrirReduzida(uri, maxOf(largura, altura))

    /** Foto inteira, reduzida para caber em [ladoMaximo] pixels (abrir em tela cheia sem estourar memória). */
    fun abrirReduzida(uri: String, ladoMaximo: Int): Bitmap? = tentar {
        val u = Uri.parse(uri)
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, limites) }
        var amostra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (amostra * 2) >= ladoMaximo) amostra *= 2
        resolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = amostra }) }
    }

    private fun criar(pasta: String?, nome: String, tipo: String): Uri? =
        if (pasta == null) criarNaPastaHelios(nome, tipo) else criarNaPastaEscolhida(pasta, nome, tipo)

    private fun criarNaPastaHelios(nome: String, tipo: String): Uri? = tentar {
        val imagem = tipo.startsWith("image/")
        val colecao = if (imagem) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val valores = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nome)
            put(MediaStore.MediaColumns.MIME_TYPE, tipo)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (imagem) PASTA_FOTOS else PASTA_CSV)
            put(MediaStore.MediaColumns.IS_PENDING, 1) // invisível para a galeria até terminar de gravar
        }
        resolver.insert(colecao, valores)
    }

    private fun criarNaPastaEscolhida(pasta: String, nome: String, tipo: String): Uri? = tentar {
        val arvore = Uri.parse(pasta)
        val pai = DocumentsContract.buildDocumentUriUsingTree(arvore, DocumentsContract.getTreeDocumentId(arvore))
        DocumentsContract.createDocument(resolver, pai, tipo, nome)
    }

    private fun escrever(uri: Uri, bloco: (OutputStream) -> Unit): Boolean {
        val ok = tentar { resolver.openOutputStream(uri)?.use(bloco) != null } ?: false
        if (ok && uri.authority == MediaStore.AUTHORITY) {
            tentar { resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) }
        }
        return ok
    }

    /** A pasta pode ter sido apagada ou o acesso revogado: o app segue sem o arquivo. */
    private fun <T> tentar(acao: () -> T): T? = try {
        acao()
    } catch (e: IOException) {
        Log.w(TAG, "Falha de E/S no arquivo", e)
        null
    } catch (e: SecurityException) {
        Log.w(TAG, "Sem acesso ao arquivo ou à pasta", e)
        null
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Endereço inválido", e)
        null
    } catch (e: IllegalStateException) {
        Log.w(TAG, "Armazenamento indisponível", e)
        null
    }

    companion object {
        private const val TAG = "HeliosArquivos"
        private const val QUALIDADE_JPEG = 85
        const val PASTA_FOTOS = "Pictures/HELIOS"
        const val PASTA_CSV = "Download/HELIOS"
    }
}
