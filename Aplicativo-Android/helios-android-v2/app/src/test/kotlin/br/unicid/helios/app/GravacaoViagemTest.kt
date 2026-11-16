@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package br.unicid.helios.app

import android.graphics.Bitmap
import br.unicid.helios.app.portas.FotoSalva
import br.unicid.helios.app.portas.GravadorFotos
import br.unicid.helios.app.portas.RepositorioViagens
import br.unicid.helios.app.portas.SessaoGravada
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Evento
import br.unicid.helios.dominio.tipos.TipoEvento
import br.unicid.helios.protocolos.LinhaEvento
import br.unicid.helios.protocolos.LinhaJanela
import br.unicid.helios.protocolos.Retencao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GravacaoViagemTest {

    @Test
    fun `abre sessão na primeira leitura`() = runTest {
        val banco = FakeBanco()
        val gravacao = gravacao(banco = banco)

        gravacao.registrar(leitura(1_000L), latenciaMs = 0)
        advanceUntilIdle()

        assertNotNull(gravacao.atual(), "sessão deve estar aberta após a primeira leitura")
        assertTrue(banco.sessoesSalvas.isNotEmpty(), "salvarSessao deve ter sido chamado")
    }

    @Test
    fun `não registra com viagem encerrada`() = runTest {
        val banco = FakeBanco()
        val gravacao = gravacao(banco = banco, viagemEncerrada = { true })

        gravacao.registrar(leitura(1_000L), latenciaMs = 0)
        advanceUntilIdle()

        assertNull(gravacao.atual(), "nenhuma sessão deve ser aberta com viagem encerrada")
        assertTrue(banco.sessoesSalvas.isEmpty(), "salvarSessao não deve ter sido chamado")
    }

    @Test
    fun `finalizar devolve o id e grava a janela parcial`() = runTest {
        val banco = FakeBanco()
        val gravacao = gravacao(banco = banco)

        gravacao.registrar(leitura(1_000L), latenciaMs = 0)
        gravacao.registrar(leitura(5_000L), latenciaMs = 0)  // < 30 s: janela incompleta

        val id = gravacao.finalizar()
        advanceUntilIdle()

        assertNotNull(id, "finalizar deve devolver o id da sessão")
        assertTrue(banco.janelasSalvas.isNotEmpty(), "janela parcial deve ser gravada ao finalizar")
        assertTrue(banco.sessoesSalvas.any { it.id == id }, "sessão final deve ser gravada com o mesmo id")
    }

    @Test
    fun `retenção apaga arquivo antes do registro`() = runTest {
        val operacoes = mutableListOf<String>()

        val banco = object : FakeBanco() {
            override fun excluirFotos(ids: Collection<Long>) {
                operacoes += "banco"
                super.excluirFotos(ids)
            }
        }
        banco.fotosSalvas += FotoSalva(1L, 0L, 0L, 0L, "alerta.jpg", "uri:alerta.jpg")

        val gravador = object : FakeGravador() {
            override fun excluir(uri: String): Boolean {
                operacoes += "arquivo"
                return super.excluir(uri)
            }
        }

        val ajustesFlow = MutableStateFlow(
            Ajustes(fotos = AjustesFotos(consentimento = true, retencao = Retencao.AO_ENCERRAR)),
        )
        val gravacao = gravacao(banco = banco, gravador = gravador, ajustes = ajustesFlow)

        gravacao.registrar(leitura(1_000L), latenciaMs = 0)
        gravacao.finalizar()
        advanceUntilIdle()

        val idxArquivo = operacoes.indexOf("arquivo")
        val idxBanco = operacoes.indexOf("banco")
        assertTrue(idxArquivo >= 0, "gravador.excluir deve ter sido chamado")
        assertTrue(idxArquivo < idxBanco, "arquivo deve ser apagado antes do registro no banco")
    }

    @Test
    fun `janela e evento gravados levam tempo por estado, fps, latencia e estado no inicio`() = runTest {
        val banco = FakeBanco()
        val gravacao = gravacao(banco = banco)
        val microssono = Evento(TipoEvento.MICROSSONO, 18_000, 2_500)

        (0L..30_000L step 100).forEach { t ->
            val estado = if (t >= 20_000) EstadoMotorista.ATENCAO else EstadoMotorista.ATENTO
            val eventos = if (t == 20_500L) listOf(microssono) else emptyList()
            gravacao.registrar(leitura(t, estado, eventos), latenciaMs = 40)
        }
        advanceUntilIdle()

        val j = banco.janelasSalvas.single()
        assertEquals(mapOf(EstadoMotorista.ATENTO to 20_000L, EstadoMotorista.ATENCAO to 10_000L), j.tempoPorEstadoMs)
        assertEquals(10.0, j.fpsMedio, 1e-9)
        assertEquals(40.0, j.latenciaMediaMs)
        assertEquals(40L, j.latenciaP95Ms)
        val e = banco.eventosSalvos.single()
        assertEquals(EstadoMotorista.ATENTO, e.estadoInicio)
        assertEquals(EstadoMotorista.ATENCAO, e.estado)
    }

    @Test
    fun `janela gravada leva a media e a discordancia da CNN`() = runTest {
        val banco = FakeBanco()
        val gravacao = gravacao(banco = banco)
        val metricas = Metricas(0.3, 0.3, 0.1, 0.0, 0.0, 0.0, false, false, null)
        listOf(0L, 100L, 200L).forEach { t ->
            val m = if (t == 0L) metricas else metricas.copy(cnnFechadoDireito = 0.9, cnnFechadoEsquerdo = 0.7)
            gravacao.registrar(leitura(t).copy(metricas = m), latenciaMs = 0)
        }
        gravacao.finalizar()
        advanceUntilIdle()

        val j = banco.janelasSalvas.single()
        assertEquals(0.8, assertNotNull(j.cnnFechadoMedia), 1e-9)
        assertEquals(1.0, assertNotNull(j.cnnDiscordancia), 1e-9) // CNN diz fechado, o app diz aberto
    }

    // ── Infraestrutura ────────────────────────────────────────────────────────

    private fun leitura(
        timestampMs: Long,
        estado: EstadoMotorista = EstadoMotorista.ATENTO,
        eventos: List<Evento> = emptyList(),
    ) = Leitura(
        timestampMs = timestampMs,
        estado = estado,
        motivos = emptySet(),
        metricas = null,
        totais = Totais(0, 0, 0, 0),
        segundosNoEstado = 0.0,
        emMovimento = false,
        eventos = eventos,
    )

    private fun kotlinx.coroutines.test.TestScope.gravacao(
        banco: FakeBanco = FakeBanco(),
        gravador: FakeGravador = FakeGravador(),
        uso: FakeUso = FakeUso(),
        ajustes: MutableStateFlow<Ajustes> = MutableStateFlow(Ajustes()),
        mensagem: MutableStateFlow<String?> = MutableStateFlow(null),
        disco: CoroutineScope = this,
        viagemEncerrada: () -> Boolean = { false },
        aoMudarHistorico: () -> Unit = {},
    ) = GravacaoViagem(
        banco = banco,
        gravador = gravador,
        repositorio = uso,
        ajustes = ajustes,
        mensagem = mensagem,
        disco = disco,
        viagemEncerrada = viagemEncerrada,
        aoMudarHistorico = aoMudarHistorico,
        epochOffsetMs = { 0L },
    )
}

// ── Dublês ────────────────────────────────────────────────────────────────────

private open class FakeBanco : RepositorioViagens {
    val sessoesSalvas = mutableListOf<SessaoGravada>()
    val janelasSalvas = mutableListOf<LinhaJanela>()
    val eventosSalvos = mutableListOf<LinhaEvento>()
    val fotosSalvas = mutableListOf<FotoSalva>()
    private var proximoId = 1L

    override fun salvarSessao(s: SessaoGravada) { sessoesSalvas += s }
    override fun gravarEventos(linhas: List<LinhaEvento>) { eventosSalvos += linhas }
    override fun gravarJanela(linha: LinhaJanela) { janelasSalvas += linha }
    override fun gravarFoto(foto: FotoSalva): Long = proximoId++.also { fotosSalvas += foto.copy(id = it) }
    override fun sessoes() = sessoesSalvas.toList()
    override fun eventos(sessao: Long?) = emptyList<LinhaEvento>()
    override fun janelas(sessao: Long?) = janelasSalvas.toList()
    override fun fotos() = fotosSalvas.toList()
    override fun excluirFotos(ids: Collection<Long>) { fotosSalvas.removeAll { it.id in ids } }
    override fun definirKss(sessao: Long, kss: Int?) {}
    override fun limparTudo() { sessoesSalvas.clear(); janelasSalvas.clear(); fotosSalvas.clear() }
}

private open class FakeGravador : GravadorFotos {
    override fun salvarJpeg(pasta: String?, nome: String, bitmap: Bitmap): String? = "uri:$nome"
    override fun salvarTexto(pasta: String?, nome: String, conteudo: String) = true
    override fun excluir(uri: String): Boolean = true
}

private class FakeUso : UsoMonitoramento {
    private var totalMs = 0L
    override fun usoTotalMs() = totalMs
    override fun salvarUsoTotal(ms: Long) { totalMs = ms }
}
