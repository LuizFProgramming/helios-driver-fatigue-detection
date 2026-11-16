package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.RecortesOlhos
import br.unicid.helios.dominio.validacao.IntervaloRoteiro
import br.unicid.helios.dominio.validacao.PassoRoteiro
import br.unicid.helios.dominio.validacao.Tarefa
import java.time.ZoneId
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** ADR-015: roteiro.csv, colunas de tarefa e óculos no quadros.csv, recortes.csv e os textos da tela. */
class RoteiroCsvTest {

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val instante = 1_792_030_205_123L // 2026-10-14T23:10:05.123-03:00

    @Test
    fun `roteiro csv tem uma linha por tarefa com o esperado e os oculos`() {
        val linhas = Csv.roteiro(
            7,
            listOf(
                IntervaloRoteiro(Tarefa.FECHAR_LONGO, instante, instante + 4_000),
                IntervaloRoteiro(Tarefa.PISCAR, instante + 7_000, instante + 15_000),
            ),
            oculos = true,
            zona,
        ).trim().lines()
        assertEquals("sessao,inicio,fim,tarefa,olho_esperado,alerta_esperado,oculos", linhas[0])
        assertEquals(
            "7,2026-10-14T23:10:05.123-03:00,2026-10-14T23:10:09.123-03:00,FECHAR_LONGO,FECHADO,SONO,1",
            linhas[1],
        )
        assertEquals(
            "7,2026-10-14T23:10:12.123-03:00,2026-10-14T23:10:20.123-03:00,PISCAR,,NENHUM,1",
            linhas[2],
        )
        val semOculos = Csv.roteiro(7, listOf(IntervaloRoteiro(Tarefa.ESTRADA, instante, instante)), false, zona)
        assertTrue(semOculos.trim().lines()[1].endsWith(",ESTRADA,ABERTO,NENHUM,0"))
    }

    @Test
    fun `quadro leva a tarefa do roteiro e os oculos`() {
        val colunas = RegistroQuadros.CABECALHO.split(",")
        val m = Metricas(0.3, 0.3, 0.1, 0.0, 0.0, 0.0, false, false, null)
        val base = LinhaQuadro(
            instante, EstadoMotorista.ATENTO, emptySet(), m, 40, "GPU", 0.0, null, "PADRAO", emptyMap(),
        )
        val linha = RegistroQuadros.linha(base.copy(oculos = true, tarefa = "PAINEL"), zona)
        val com = colunas.zip(linha.split(",")).toMap()
        assertEquals("1", com["oculos"])
        assertEquals("PAINEL", com["tarefa"])
        val sem = colunas.zip(RegistroQuadros.linha(base, zona).split(",")).toMap()
        assertEquals("0", sem["oculos"])
        assertEquals("", sem["tarefa"])
        assertEquals(colunas.size, RegistroQuadros.linha(base, zona).split(",").size)
    }

    @Test
    fun `recortes saem em duas linhas, olho direito e esquerdo, com os pixels em base64`() {
        val direito = ByteArray(64 * 64) { (it % 256).toByte() }
        val esquerdo = ByteArray(64 * 64) { 7 }
        val m = Metricas(
            0.1, 0.1, 0.1, 0.0, 0.0, 0.0, true, false, null,
            earDireito = 0.12, earEsquerdo = 0.08, cnnFechadoDireito = 0.97,
        )
        val recortes = RecortesOlhos(direito, esquerdo)
        val texto = RegistroRecortes.linhas(instante, Tarefa.FECHAR_CURTO, true, m, recortes, zona)
        val linhas = texto.trimEnd().lines()
        assertEquals(2, linhas.size)
        assertTrue(texto.endsWith("\n"))
        val colunas = RegistroRecortes.CABECALHO.split(",")
        val dir = colunas.zip(linhas[0].split(",")).toMap()
        val esq = colunas.zip(linhas[1].split(",")).toMap()
        assertEquals("2026-10-14T23:10:05.123-03:00", dir["inicio"])
        val esperado = listOf("FECHAR_CURTO", "FECHADO", "1", "dir", "0.1200", "0.9700")
        assertEquals(esperado, colunas.slice(1..6).map { dir[it] })
        assertEquals(listOf("esq", "0.0800", ""), listOf(esq["olho"], esq["ear"], esq["cnn"]))
        assertEquals(direito.toList(), Base64.getDecoder().decode(dir["pixels"]).toList())
        assertEquals(esquerdo.toList(), Base64.getDecoder().decode(esq["pixels"]).toList())
        val semMetricas = RegistroRecortes.linhas(instante, Tarefa.ESTRADA, false, null, recortes, zona)
        val vazio = colunas.zip(semMetricas.lines()[0].split(",")).toMap()
        val campos = listOf("olho_esperado", "oculos", "ear", "cnn")
        assertEquals(listOf("ABERTO", "0", "", ""), campos.map { vazio[it] })
        val piscar = RegistroRecortes.linhas(instante, Tarefa.PISCAR, false, m, recortes, zona)
        assertEquals("", colunas.zip(piscar.lines()[0].split(",")).toMap()["olho_esperado"])
    }

    private fun passo(i: Int, t: Tarefa, preparando: Boolean) = PassoRoteiro(i, t, preparando, 1_000)

    @Test
    fun `voz anuncia no preparo, bipe marca o inicio e o fim do olho fechado`() {
        val estradaPreparo = passo(0, Tarefa.ESTRADA, true)
        val estrada = passo(0, Tarefa.ESTRADA, false)
        val fecharPreparo = passo(1, Tarefa.FECHAR_CURTO, true)
        val fechar = passo(1, Tarefa.FECHAR_CURTO, false)
        val depoisPreparo = passo(2, Tarefa.ESTRADA, true)
        val primeiro = avisosDoRoteiro(null, estradaPreparo)
        assertEquals(listOf<AvisoRoteiro>(AvisoRoteiro.Falar("A seguir: olhe para a estrada")), primeiro)
        assertEquals("A seguir: olhe para a estrada", (primeiro.single() as AvisoRoteiro.Falar).texto)
        assertEquals(emptyList(), avisosDoRoteiro(estradaPreparo, estradaPreparo))
        assertEquals(listOf<AvisoRoteiro>(AvisoRoteiro.Bipe), avisosDoRoteiro(estradaPreparo, estrada))
        assertEquals(emptyList(), avisosDoRoteiro(estrada, estrada))
        val fala = AvisoRoteiro.Falar(TextosRoteiro.preparo(Tarefa.FECHAR_CURTO))
        assertEquals(listOf<AvisoRoteiro>(fala), avisosDoRoteiro(estrada, fecharPreparo))
        assertEquals(
            listOf(AvisoRoteiro.Bipe, AvisoRoteiro.Falar("A seguir: olhe para a estrada")),
            avisosDoRoteiro(fechar, depoisPreparo),
        )
        assertEquals(listOf(AvisoRoteiro.Bipe, AvisoRoteiro.Falar("Roteiro concluído")), avisosDoRoteiro(fechar, null))
        assertEquals(listOf<AvisoRoteiro>(AvisoRoteiro.Falar("Roteiro concluído")), avisosDoRoteiro(estrada, null))
        assertEquals(listOf<AvisoRoteiro>(AvisoRoteiro.Bipe), avisosDoRoteiro(null, estrada))
        assertEquals(emptyList(), avisosDoRoteiro(null, null))
    }

    @Test
    fun `cada tarefa tem instrucao propria e o preparo avisa a proxima`() {
        val instrucoes = Tarefa.entries.map(TextosRoteiro::instrucao)
        assertEquals(instrucoes.size, instrucoes.toSet().size)
        assertEquals("Feche os olhos e abra no bipe", TextosRoteiro.instrucao(Tarefa.FECHAR_CURTO))
        assertEquals("A seguir: olhe para o espelho esquerdo", TextosRoteiro.preparo(Tarefa.ESPELHO_ESQUERDO))
    }
}
