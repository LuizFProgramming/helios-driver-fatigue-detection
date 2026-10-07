package br.unicid.helios.protocolos

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtratorJpegTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    private val jpegA = bytes(0xFF, 0xD8, 0x01, 0xFF, 0x00, 0x02, 0xFF, 0xD9)
    private val jpegB = bytes(0xFF, 0xD8, 0x03, 0xFF, 0xD9)
    private val cabecalho = "--frame\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray()

    private fun fluxo(vararg partes: ByteArray) = partes.reduce { a, b -> a + b }

    @Test
    fun `extrai duas imagens de um pedaco so, ignorando cabecalhos do multipart`() {
        val imagens = ExtratorJpeg().alimentar(fluxo(cabecalho, jpegA, cabecalho, jpegB, cabecalho))
        assertEquals(2, imagens.size)
        assertContentEquals(jpegA, imagens[0])
        assertContentEquals(jpegB, imagens[1])
    }

    @Test
    fun `imagem cortada em qualquer ponto e montada no pedaco seguinte`() {
        val completo = fluxo(cabecalho, jpegA, cabecalho)
        for (corte in 1 until completo.size) {
            val extrator = ExtratorJpeg()
            val primeiro = extrator.alimentar(completo.copyOfRange(0, corte))
            val segundo = extrator.alimentar(completo.copyOfRange(corte, completo.size))
            val todas = primeiro + segundo
            assertEquals(1, todas.size, "corte=$corte")
            assertContentEquals(jpegA, todas[0])
        }
    }

    @Test
    fun `usa so os primeiros bytes quando o tamanho e informado`() {
        val buffer = jpegB + ByteArray(100)
        assertContentEquals(jpegB, ExtratorJpeg().alimentar(buffer, jpegB.size).single())
    }

    @Test
    fun `lixo sem marcador nao gera imagem e nao fica guardado`() {
        val extrator = ExtratorJpeg()
        repeat(50) { assertTrue(extrator.alimentar(ByteArray(300) { 0x41 }).isEmpty()) }
        assertContentEquals(jpegA, extrator.alimentar(jpegA).single())
    }

    @Test
    fun `inicio de jpeg sem fim e descartado quando passa do limite`() {
        val extrator = ExtratorJpeg(tamanhoMaximoBytes = 1_024)
        assertTrue(extrator.alimentar(bytes(0xFF, 0xD8) + ByteArray(900)).isEmpty())
        assertTrue(extrator.alimentar(ByteArray(300)).isEmpty())
        assertContentEquals(jpegA, extrator.alimentar(jpegA).single())
    }

    @Test
    fun `pedaco maior que o buffer inicial faz o buffer crescer`() {
        val grande = bytes(0xFF, 0xD8) + ByteArray(200 * 1024) + bytes(0xFF, 0xD9)
        assertEquals(grande.size, ExtratorJpeg().alimentar(grande).single().size)
    }

    @Test
    fun `pedaco vazio nao quebra`() {
        assertTrue(ExtratorJpeg().alimentar(ByteArray(0)).isEmpty())
    }
}
