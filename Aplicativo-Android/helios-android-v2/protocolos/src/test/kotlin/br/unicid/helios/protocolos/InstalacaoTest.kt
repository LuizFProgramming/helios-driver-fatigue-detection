package br.unicid.helios.protocolos

import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.calibracao.Desvios
import br.unicid.helios.dominio.instalacao.Dica
import br.unicid.helios.dominio.instalacao.LadoMotorista
import br.unicid.helios.dominio.instalacao.PartesNota
import br.unicid.helios.dominio.instalacao.PerfilInstalacao
import br.unicid.helios.dominio.instalacao.PosicaoCamera
import br.unicid.helios.dominio.instalacao.Qualidade
import br.unicid.helios.dominio.instalacao.Verificacao
import br.unicid.helios.dominio.instalacao.Zona
import br.unicid.helios.dominio.instalacao.ZonaOlhar
import br.unicid.helios.dominio.tipos.CaixaRosto
import br.unicid.helios.dominio.tipos.Ponto2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArquivoPerfisTest {

    private val completo = PerfilInstalacao(
        id = "a1", nome = "Gol do Alexandre", posicao = PosicaoCamera.COLUNA_VOLANTE, lado = LadoMotorista.ESQUERDO,
        criadoEpochMs = 1_794_800_000_000,
        calibracao = Calibracao(
            0.31, -15.5, 2.25, earDireito = 0.3, earEsquerdo = 0.32, olharHorizontal = 0.48, olharVertical = 0.05,
        ),
        desvios = Desvios(1.5, 2.0, 0.01, null),
        zonas = listOf(
            ZonaOlhar(Zona.RETROVISOR, -30.0, 10.0, 0.1, null, 6.0, 5.0, 0.05),
            ZonaOlhar(Zona.PAINEL, 0.0, -25.0, null, -0.3, 5.0, 7.5, 0.08),
        ),
        fracaoVerificacoesOk = 0.95, nota = 92,
    )
    private val minimo = PerfilInstalacao(
        "b2", "Carro da mãe", PosicaoCamera.PILAR_A, LadoMotorista.DIREITO, 1L, Calibracao(0.28, 3.0),
        Desvios(0.0, 0.0, null, null), emptyList(), 1.0, 85,
    )

    @Test
    fun `perfis vao e voltam sem perder nada, inclusive os valores ausentes`() {
        val texto = ArquivoPerfis.escrever(listOf(completo, minimo))
        assertTrue(texto.startsWith(ArquivoPerfis.CABECALHO))
        assertEquals(listOf(completo, minimo), ArquivoPerfis.ler(texto))
    }

    @Test
    fun `nome com quebra de linha vira espaco e nome com igual continua inteiro`() {
        val p = minimo.copy(nome = "Carro\nnovo = azul")
        assertEquals("Carro novo = azul", ArquivoPerfis.ler(ArquivoPerfis.escrever(listOf(p))).single().nome)
    }

    @Test
    fun `perfil quebrado fica de fora sem derrubar os outros`() {
        val bom = ArquivoPerfis.escrever(listOf(minimo))
        val ruins = listOf(
            "posicao=TETO", "nota=abc", "nota=101", "nota=-1", "lado=", "id=", "zona=RETROVISOR;1;2", "ear=NaN",
            "pitch=Infinity", "yaw=-Infinity",
        )
        ruins.forEach { troca ->
            val chave = troca.substringBefore("=")
            val quebrado = ArquivoPerfis.escrever(listOf(completo)).lines().joinToString("\n") { linha ->
                if (linha.startsWith("$chave=")) troca else linha
            } + "\n" + bom.lines().drop(1).joinToString("\n")
            assertEquals(listOf(minimo), ArquivoPerfis.ler(quebrado), "com $troca")
        }
    }

    @Test
    fun `notas 0 e 100 valem, chave opcional pode faltar e fim sem perfil e ignorado`() {
        val texto = ArquivoPerfis.escrever(listOf(minimo.copy(nota = 0), minimo.copy(id = "c3", nota = 100)))
            .lines().filterNot { it.startsWith("ear_dir=") }.joinToString("\n")
            .replace(ArquivoPerfis.CABECALHO, ArquivoPerfis.CABECALHO + "\nfim\nlixo")
        assertEquals(listOf(0, 100), ArquivoPerfis.ler(texto).map { it.nota })
    }

    @Test
    fun `arquivo de outro formato nao vira perfil`() {
        assertEquals(emptyList(), ArquivoPerfis.ler(""))
        assertEquals(emptyList(), ArquivoPerfis.ler("helios-perfis 99\nperfil\nid=x\nfim"))
        assertEquals(emptyList(), ArquivoPerfis.ler("id,nome\n1,2"))
    }
}

class EnquadramentoTest {

    private val lidos = mutableSetOf<Int>()

    private fun resultado() = ResultadoFaceMesh(
        IndicesFaceMesh.TOTAL_PONTOS,
        { i ->
            lidos += i
            when (i) {
                234 -> Ponto2D(0.2, 0.5)
                454 -> Ponto2D(0.7, 0.5)
                else -> Ponto2D(0.4, i / 1000.0)
            }
        },
        emptyMap(),
        null,
    )

    @Test
    fun `no enquadramento a caixa do rosto sai do contorno, fora dele nem e lida`() {
        assertNull(MapeadorFaceMesh(640, 480).mapear(resultado())?.caixaRosto)
        assertEquals(20, lidos.size)
        val caixa = assertNotNull(MapeadorFaceMesh(640, 480, comEnquadramento = true).mapear(resultado())?.caixaRosto)
        assertEquals(CaixaRosto(0.2, 0.01, 0.7, 0.5), caixa) // 234 e 454 têm y = 0,5
        assertEquals(IndicesFaceMesh.USADOS.toSet() + IndicesFaceMesh.CONTORNO_ROSTO, lidos)
        assertEquals(36, IndicesFaceMesh.CONTORNO_ROSTO.size)
    }

    @Test
    fun `todos os blendshapes so vao para a amostra com o registro por quadro ligado`() {
        val todos = mapOf("jawOpen" to 0.4, "cheekPuff" to 0.1)
        val r = ResultadoFaceMesh(IndicesFaceMesh.TOTAL_PONTOS, { Ponto2D(0.5, 0.5) }, todos, null)
        assertNull(MapeadorFaceMesh(640, 480).mapear(r)?.blendshapes)
        assertEquals(todos, MapeadorFaceMesh(640, 480, comTodosBlendshapes = true).mapear(r)?.blendshapes)
    }

    @Test
    fun `brilho do rosto e a luminancia media dentro da caixa, cortada na imagem`() {
        // Metade esquerda preta e metade direita branca; a caixa passa da borda de cima.
        // Nas linhas 0 e 1 a metade esquerda é preta; o resto é branco. A caixa passa da borda de cima.
        val preto = 0xFF000000.toInt()
        val brilho = brilhoDoRosto(CaixaRosto(0.25, -0.5, 0.75, 0.5), 8, 8) { x, y ->
            if (x < 4 && y < 2) preto else -1
        }
        assertEquals(191.25, assertNotNull(brilho), 1e-9) // 12 brancos e 4 pretos
        val cinza = brilhoDoRosto(CaixaRosto(0.0, 0.0, 1.0, 1.0), 4, 4) { _, _ -> 0xFF1D1D1D.toInt() }
        assertEquals(29.0, assertNotNull(cinza), 1e-9)
    }

    /** Branco onde a coordenada [c] passa de 64 e é múltipla de 3; preto no resto. */
    private fun listras(c: Int) = if (c >= 64 && c % 3 == 0) -1 else 0xFF000000.toInt()

    @Test
    fun `rosto grande e amostrado numa grade de no maximo 32 por lado`() {
        // Lado menor de 64 px: passo 2. Das 32 colunas (ou linhas) pares de 64 a 126, 11 são múltiplas de 3.
        val esperado = 255.0 * 11 / 32
        val colunas = brilhoDoRosto(CaixaRosto(0.5, 0.25, 1.0, 1.0), 128, 128) { x, _ -> listras(x) }
        assertEquals(esperado, assertNotNull(colunas), 1e-9)
        val linhas = brilhoDoRosto(CaixaRosto(0.25, 0.5, 1.0, 1.0), 128, 128) { _, y -> listras(y) }
        assertEquals(esperado, assertNotNull(linhas), 1e-9)
    }

    @Test
    fun `caixa toda fora da imagem nao tem brilho`() {
        assertNull(brilhoDoRosto(CaixaRosto(1.2, 0.0, 1.5, 0.5), 8, 8) { _, _ -> -1 })
    }
}

class TextosInstalacaoTest {

    @Test
    fun `toda dica, verificacao, zona, posicao e qualidade tem texto curto`() {
        val textos = Dica.entries.map(TextosInstalacao::dica) +
            Verificacao.entries.map(TextosInstalacao::verificacao) +
            Zona.entries.map(TextosInstalacao::instrucaoZona) +
            PosicaoCamera.entries.map(TextosInstalacao::posicao) +
            PosicaoCamera.entries.map(TextosInstalacao::explicacaoPosicao) +
            Qualidade.entries.map(TextosInstalacao::qualidade)
        assertTrue(textos.all { it.isNotBlank() && it.length <= 120 && it[0].isUpperCase() }, textos.toString())
        assertEquals(textos.size, textos.toSet().size)
    }

    @Test
    fun `resumo diz quantos pontos cada parte fez`() {
        assertEquals(
            listOf(
                "Enquadramento certo: 30 de 60",
                "Câmera e rosto parados: 0 de 25",
                "Lugares do carro gravados: 4 de 15",
            ),
            TextosInstalacao.partesDaNota(PartesNota(30, 0, 4)),
        )
    }

    @Test
    fun `dicas dizem o que fazer com a camera`() {
        assertEquals("Suba a câmera", TextosInstalacao.dica(Dica.SUBIR))
        assertEquals("Olhe para o espelho esquerdo", TextosInstalacao.instrucaoZona(Zona.ESPELHO_ESQUERDO))
    }
}
