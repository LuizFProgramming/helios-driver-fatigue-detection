@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package br.unicid.helios.app

import android.graphics.Bitmap
import br.unicid.helios.app.portas.FotoSalva
import br.unicid.helios.app.portas.GravadorFotos
import br.unicid.helios.app.portas.RepositorioViagens
import br.unicid.helios.app.portas.SessaoGravada
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.Totais
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.protocolos.LinhaEvento
import br.unicid.helios.protocolos.LinhaJanela
import br.unicid.helios.protocolos.Retencao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GravacaoViagemTest {

    @Test
    fun `abre sessão na primeira leitura`() = runTest {
        val banco = FakeBanco()
        val gravacao = gravacao(banco = banco)

        gravacao.registrar(leitura(1_000L))
        advanceUntilIdle()

        assertNotNull(gravacao.atual(), "sessão deve estar aberta após a primeira leitura")
        assertTrue(banco.sessoesSalvas.isNotEmpty(), "salvarSessao deve ter sido chamado")
    }

    @Test
    fun `não registra com viagem encerrada`() = runTest {
        val banco = FakeBanco()
        val gravacao = gravacao(banco = banco, viagemEncerrada = { true })

        gravacao.registrar(leitura(1_000L))
        advanceUntilIdle()

        assertNull(gravacao.atual(), "nenhuma sessão deve ser aberta com viagem encerrada")
        assertTrue(banco.sessoesSalvas.isEmpty(), "salvarSessao não deve ter sido chamado")
    }

    @Test
    fun `finalizar devolve o id e grava a janela parcial`() = runTest {
        val banco = FakeBanco()
        val gravacao = gravacao(banco = banco)

        gravacao.registrar(leitura(1_000L))
        gravacao.registrar(leitura(5_000L))  // < 30 s: janela incompleta

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

        gravacao.registrar(leitura(1_000L))
        gravacao.finalizar()
        advanceUntilIdle()

        val idxArquivo = operacoes.indexOf("arquivo")
        val idxBanco = operacoes.indexOf("banco")
        assertTrue(idxArquivo >= 0, "gravador.excluir deve ter sido chamado")
        assertTrue(idxArquivo < idxBanco, "arquivo deve ser apagado antes do registro no banco")
    }

    // ── Infraestrutura ────────────────────────────────────────────────────────

    private fun leitura(timestampMs: Long) = Leitura(
        timestampMs = timestampMs,
        estado = EstadoMotorista.ATENTO,
        motivos = emptySet(),
        metricas = null,
        totais = Totais(0, 0, 0, 0),
        segundosNoEstado = 0.0,
        emMovimento = false,
        eventos = emptyList(),
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
    val fotosSalvas = mutableListOf<FotoSalva>()
    private var proximoId = 1L

    override fun salvarSessao(s: SessaoGravada) { sessoesSalvas += s }
    override fun gravarEventos(linhas: List<LinhaEvento>) {}
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
