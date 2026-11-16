package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.dominio.tipos.Ponto2D
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapeadorFaceMeshTest {

    private val lidos = mutableSetOf<Int>()

    private fun resultado(
        n: Int = IndicesFaceMesh.TOTAL_PONTOS,
        blend: Map<String, Double> = emptyMap(),
        matriz: DoubleArray? = null,
    ) = ResultadoFaceMesh(n, { i -> lidos += i; Ponto2D(i / 1000.0, i / 2000.0) }, blend, matriz)

    @Test
    fun `menos de 478 pontos nao vira amostra`() {
        assertNull(MapeadorFaceMesh(640, 480).mapear(resultado(10)))
    }

    @Test
    fun `le so os 20 pontos usados, nao a malha inteira`() {
        MapeadorFaceMesh(640, 480).mapear(resultado())
        assertEquals(IndicesFaceMesh.USADOS.toSet(), lidos)
        assertEquals(20, lidos.size)
        assertEquals(
            setOf(
                "eyeBlinkLeft", "eyeBlinkRight", "jawOpen", "eyeSquintLeft", "eyeSquintRight",
                "eyeLookUpLeft", "eyeLookUpRight", "eyeLookDownLeft", "eyeLookDownRight",
            ),
            Blendshapes.USADOS,
        )
    }

    @Test
    fun `iris, olhar vertical e semicerrar vem da malha e dos blendshapes`() {
        val blend = mapOf(
            Blendshapes.OLHAR_CIMA_DIREITO to 0.1,
            Blendshapes.OLHAR_BAIXO_DIREITO to 0.7,
            Blendshapes.OLHAR_BAIXO_ESQUERDO to 0.5,
            Blendshapes.SEMICERRAR_DIREITO to 0.3,
            Blendshapes.SEMICERRAR_ESQUERDO to 0.4,
        )
        val a = assertNotNull(MapeadorFaceMesh(1000, 2000).mapear(resultado(blend = blend)))
        assertEquals(Ponto2D(468.0, 468.0), a.irisDireita)
        assertEquals(Ponto2D(473.0, 473.0), a.irisEsquerda)
        assertEquals(0.05 - 0.6, a.olharVertical!!, 1e-9)
        assertEquals(0.3, a.semicerrarDireito)
        assertEquals(0.4, a.semicerrarEsquerdo)
        val semOlhar = assertNotNull(MapeadorFaceMesh(1, 1).mapear(resultado()))
        assertNull(semOlhar.olharVertical)
        assertEquals(0.0, semOlhar.semicerrarDireito)
        val soCima = assertNotNull(MapeadorFaceMesh(1, 1).mapear(resultado(blend = mapOf(Blendshapes.OLHAR_CIMA_ESQUERDO to 0.4))))
        assertEquals(0.2, soCima.olharVertical!!, 1e-9)
    }

    @Test
    fun `pega os indices certos e converte para pixels`() {
        val amostra = MapeadorFaceMesh(640, 480).mapear(resultado())
        assertNotNull(amostra)
        assertEquals(Ponto2D(33 / 1000.0 * 640, 33 / 2000.0 * 480), amostra.olhoDireito.p1)
        assertEquals(Ponto2D(380 / 1000.0 * 640, 380 / 2000.0 * 480), amostra.olhoEsquerdo.p6)
        assertEquals(Ponto2D(78 / 1000.0 * 640, 78 / 2000.0 * 480), amostra.boca.p1)
        assertEquals(0.0, amostra.piscadaDireita)
        assertNull(amostra.pose)
    }

    @Test
    fun `blendshapes e matriz sao repassados`() {
        val blend = mapOf(Blendshapes.PISCADA_DIREITA to 0.1, Blendshapes.PISCADA_ESQUERDA to 0.2, Blendshapes.MANDIBULA to 0.3)
        val identidade = DoubleArray(16).also { for (i in 0..3) it[i * 5] = 1.0 }
        val amostra = MapeadorFaceMesh(1, 1).mapear(resultado(blend = blend, matriz = identidade))
        assertNotNull(amostra)
        assertEquals(0.1, amostra.piscadaDireita)
        assertEquals(0.2, amostra.piscadaEsquerda)
        assertEquals(0.3, amostra.aberturaMandibula)
        assertEquals(0.0, assertNotNull(amostra.pose).pitchGraus, 1e-9)
    }
}

class AlarmeTest {

    @Test
    fun `desatento e distraido tocam o mesmo padrao do prototipo Python`() {
        val desatento = assertNotNull(padraoPara(EstadoMotorista.DESATENTO))
        assertEquals(PadraoSonoro(1_200, 150, 850, 0.8, vibrar = true, codigoBuzzer = 1), desatento)
        assertEquals(desatento, padraoPara(EstadoMotorista.DISTRAIDO))
    }

    @Test
    fun `so fadiga e critico tocam, critico mais agudo e mais rapido`() {
        assertNull(padraoPara(EstadoMotorista.ATENTO))
        assertNull(padraoPara(EstadoMotorista.SEM_ROSTO))
        val fadiga = assertNotNull(padraoPara(EstadoMotorista.FADIGA))
        val critico = assertNotNull(padraoPara(EstadoMotorista.CRITICO))
        assertTrue(critico.frequenciaHz > fadiga.frequenciaHz)
        assertTrue(critico.desligadoMs < fadiga.desligadoMs)
        assertEquals(1, fadiga.codigoBuzzer)
        assertEquals(2, critico.codigoBuzzer)
    }

    @Test
    fun `taxa padrao do ciclo e 22050 Hz`() {
        val padrao = PadraoSonoro(1_000, ligadoMs = 100, desligadoMs = 100, volume = 0.5, vibrar = false, codigoBuzzer = 0)
        assertEquals(4_410, gerarCicloPcm(padrao).size)
    }

    @Test
    fun `ciclo pcm tem tom e depois silencio`() {
        val padrao = PadraoSonoro(1_000, ligadoMs = 100, desligadoMs = 50, volume = 1.0, vibrar = false, codigoBuzzer = 0)
        val pcm = gerarCicloPcm(padrao, taxaHz = 8_000)
        assertEquals(1_200, pcm.size)
        assertEquals(0, pcm[0].toInt())
        assertEquals(Short.MAX_VALUE.toInt(), pcm[2].toInt()) // pico em 1/4 do período (8 amostras)
        assertTrue(pcm.copyOfRange(800, 1_200).all { it.toInt() == 0 })
    }
}

class ModeloDeTelaTest {

    private fun leitura(
        estado: EstadoMotorista,
        motivos: Set<Motivo> = emptySet(),
        metricas: Metricas? = null,
        segundos: Double = 0.0,
    ) = Leitura(0, estado, motivos, metricas, Totais(0, 0, 0, 0), segundos, false, emptyList())

    @Test
    fun `cada estado tem titulo, icone e cor proprios`() {
        val telas = EstadoMotorista.entries.map { modeloDeTela(leitura(it)) }
        assertEquals(
            listOf("ATENTO", "FADIGA", "ACORDE", "DESATENTO", "DISTRAÍDO", "SEM ROSTO", "ATENÇÃO"),
            telas.map { it.titulo },
        )
        assertEquals(6, telas.map { it.tom }.toSet().size)
        // Regra 5 do ADR-012: palavra, ícone e cor; DESATENTO e DISTRAÍDO dividem o tom, mas não o ícone.
        assertEquals(EstadoMotorista.entries.size, telas.map { it.icone }.toSet().size)
        assertEquals(listOf(false, false, true, false, false, false, false), telas.map { it.telaCheia })
    }

    @Test
    fun `detalhe lista os motivos ou um texto padrao`() {
        assertEquals("boa viagem", modeloDeTela(leitura(EstadoMotorista.ATENTO)).detalhe)
        assertEquals("ajuste a câmera", modeloDeTela(leitura(EstadoMotorista.SEM_ROSTO)).detalhe)
        val todos = modeloDeTela(leitura(EstadoMotorista.CRITICO, Motivo.entries.toSet())).detalhe
        assertEquals(Motivo.entries.size, todos.split(", ").size)
    }

    @Test
    fun `perclos e piscadas vem da leitura`() {
        val m = Metricas(0.1, 0.2, 0.1, 0.5, 0.5, 0.234, true, false, null, piscadasNoMinuto = 17)
        val tela = modeloDeTela(leitura(EstadoMotorista.CRITICO, metricas = m))
        assertEquals(23, tela.perclosPercentual)
        assertEquals(17, tela.piscadasPorMinuto)
        val vazia = modeloDeTela(leitura(EstadoMotorista.ATENTO))
        assertEquals(0, vazia.perclosPercentual)
        assertNull(vazia.piscadasPorMinuto)
    }

    private fun apoio(estado: EstadoMotorista, motivos: Set<Motivo> = emptySet(), segundos: Double = 0.0) =
        modeloDeTela(leitura(estado, motivos, segundos = segundos)).apoio

    @Test
    fun `linha de apoio mostra so o motivo principal, com a primeira letra maiuscula`() {
        assertEquals(
            "Olhar fora da via",
            apoio(EstadoMotorista.ATENCAO, setOf(Motivo.DISTRACAO_ACUMULADA, Motivo.OLHAR_DESVIADO)),
        )
        assertEquals("Olhos fechados", apoio(EstadoMotorista.FADIGA, Motivo.entries.toSet()))
        assertEquals("Boa viagem", apoio(EstadoMotorista.ATENTO))
        assertEquals("Ajuste a câmera", apoio(EstadoMotorista.SEM_ROSTO))
    }

    @Test
    fun `alerta urgente diz ha quanto tempo dura`() {
        val olhar = setOf(Motivo.OLHAR_DESVIADO)
        assertEquals("Olhar fora da via há 3 s", apoio(EstadoMotorista.DISTRAIDO, olhar, 3.9))
        assertEquals("Rosto fora da câmera há 1 s", apoio(EstadoMotorista.DESATENTO, setOf(Motivo.ROSTO_AUSENTE), 1.0))
        assertEquals("Olhos fechados há 2 s", apoio(EstadoMotorista.CRITICO, setOf(Motivo.SONO), 2.4))
        assertEquals("Olhar fora da via há 59 s", apoio(EstadoMotorista.DISTRAIDO, olhar, 59.9))
        assertEquals("Olhar fora da via há 1 min", apoio(EstadoMotorista.DISTRAIDO, olhar, 60.0))
    }

    @Test
    fun `sem tempo abaixo de 1 s e fora dos alertas urgentes`() {
        assertEquals("Olhos fechados", apoio(EstadoMotorista.CRITICO, setOf(Motivo.SONO), 0.9))
        assertEquals("Olhos pesados", apoio(EstadoMotorista.FADIGA, setOf(Motivo.PERCLOS_FADIGA), 30.0))
    }

    @Test
    fun `nenhuma linha de apoio passa de 120 caracteres`() {
        val linhas = EstadoMotorista.entries.flatMap { e -> Motivo.entries.map { apoio(e, setOf(it), 3_599.0) } }
        val maior = linhas.maxBy { it.length }
        assertTrue(maior.length <= 120, maior)
    }
}
