package br.unicid.helios.app

import android.graphics.Bitmap
import android.os.SystemClock
import br.unicid.helios.app.adaptadores.fotos.GravadorArquivos
import br.unicid.helios.app.portas.FotoSalva
import br.unicid.helios.app.portas.GravadorFotos
import br.unicid.helios.app.portas.RepositorioViagens
import br.unicid.helios.app.portas.SessaoGravada
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.sessao.AgregadorSessao
import br.unicid.helios.dominio.sessao.JanelaResumo
import br.unicid.helios.dominio.sessao.PoliticaEvidencia
import br.unicid.helios.dominio.sessao.ResumoSessao
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.TipoEvento
import br.unicid.helios.protocolos.FotoGuardada
import br.unicid.helios.protocolos.LinhaEvento
import br.unicid.helios.protocolos.LinhaJanela
import br.unicid.helios.protocolos.PoliticaRetencao
import br.unicid.helios.protocolos.nomeFoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

/**
 * Sessão da viagem em andamento e fotos dos alertas: abre e fecha a sessão, grava eventos, janelas,
 * resumos e fotos, e aplica a retenção. Saiu do [HeliosViewModel] para ele cuidar só de captura e alerta.
 *
 * Threads: [registrar] e [finalizar] vêm da thread do domínio (ou da principal, no [finalizar] do onCleared),
 * por isso a sessão fica sob [trava]. Toda escrita em disco vai para [disco], uma fila só.
 */
class GravacaoViagem(
    private val banco: RepositorioViagens,
    private val gravador: GravadorFotos,
    private val repositorio: UsoMonitoramento,
    private val ajustes: StateFlow<Ajustes>,
    private val mensagem: MutableStateFlow<String?>,
    private val disco: CoroutineScope,
    private val viagemEncerrada: () -> Boolean,
    /** Chamado na fila de disco quando uma viagem nova entra no histórico. */
    private val aoMudarHistorico: () -> Unit,
    /** Diferença entre epoch (System.currentTimeMillis) e elapsedRealtime, injetável em testes. */
    private val epochOffsetMs: () -> Long = { System.currentTimeMillis() - SystemClock.elapsedRealtime() },
) {
    private val _estadoFotos = MutableStateFlow(EstadoFotos(false, GravadorArquivos.PASTA_FOTOS, 0, null))
    val estadoFotos: StateFlow<EstadoFotos> = _estadoFotos

    @Volatile
    private var ultimaFoto: String? = null

    private val trava = Any()
    private var sessao: SessaoAtiva? = null
    private val fotoPendente = AtomicReference<PedidoFoto?>(null)
    private val anotacaoPendente = AtomicReference<PedidoFoto?>(null)

    private class SessaoAtiva(val id: Long, val offsetEpochMs: Long, val inicioMs: Long, val usoBaseMs: Long) {
        val agregador = AgregadorSessao()
        val evidencia = PoliticaEvidencia()
        var ultimoMs = inicioMs

        fun epoch(t: Long) = t + offsetEpochMs
        fun usoAgora(t: Long) = usoBaseMs + (t - inicioMs)
    }

    private class PedidoFoto(
        val estado: EstadoMotorista,
        val sessao: Long,
        val epochMs: Long,
        val usoMs: Long,
        val teste: Boolean = false,
    )

    // ── Sessão ───────────────────────────────────────────────────────────────

    /** Retrato da viagem aberta para o painel do menu, ou nulo sem viagem. */
    fun atual(): SessaoGravada? = synchronized(trava) { sessao?.let { s -> gravada(s, s.agregador.resumo()) } }

    fun registrar(leitura: Leitura) = synchronized(trava) {
        if (viagemEncerrada()) return@synchronized
        val t = leitura.timestampMs
        val s = sessao ?: abrir(t)
        s.ultimoMs = t
        s.agregador.registrar(leitura)?.let { janela -> aoFecharJanela(s, janela) }
        if (leitura.eventos.isNotEmpty()) {
            val m = leitura.metricas
            val linhas = leitura.eventos.map { e ->
                LinhaEvento(
                    s.id, s.epoch(e.inicioMs), e.duracaoMs, e.tipo, leitura.estado,
                    m?.perclosCurto ?: 0.0, m?.perclosLongo ?: 0.0, null,
                )
            }
            disco.launch { banco.gravarEventos(linhas) }
        }
        val capturar = s.evidencia.avaliar(leitura)
        if (capturar && ajustes.value.fotos.ativas) {
            fotoPendente.set(PedidoFoto(leitura.estado, s.id, s.epoch(t), s.usoAgora(t)))
        }
    }

    private fun abrir(t: Long): SessaoAtiva {
        val offset = epochOffsetMs()
        val nova = SessaoAtiva(t + offset, offsetEpochMs = offset, inicioMs = t, usoBaseMs = repositorio.usoTotalMs())
        sessao = nova
        val inicial = gravada(nova, nova.agregador.resumo())
        disco.launch {
            banco.salvarSessao(inicial)
            aoMudarHistorico()
        }
        return nova
    }

    private fun aoFecharJanela(s: SessaoAtiva, j: JanelaResumo) {
        val linha = linhaJanela(s, j)
        val parcial = gravada(s, s.agregador.resumo())
        repositorio.salvarUsoTotal(s.usoAgora(s.ultimoMs))
        disco.launch {
            banco.gravarJanela(linha)
            banco.salvarSessao(parcial)
        }
    }

    /**
     * Fecha a sessão aberta. O retrato é tirado sob a trava; a gravação vai para o fim da fila de disco,
     * depois de tudo que a viagem já pediu (fotos, janelas, resumos parciais). Devolve o id, ou nulo sem sessão.
     */
    fun finalizar(): Long? {
        fotoPendente.set(null)
        anotacaoPendente.set(null)
        val s: SessaoAtiva
        val resumo: ResumoSessao
        synchronized(trava) {
            s = sessao ?: return null
            sessao = null
            resumo = s.agregador.resumo()
        }
        repositorio.salvarUsoTotal(s.usoAgora(s.ultimoMs))
        val parcial = resumo.janelas.lastOrNull()?.takeIf { it.fimMs - it.inicioMs < JANELA_COMPLETA_MS }
        val final = gravada(s, resumo)
        disco.launch {
            parcial?.let { banco.gravarJanela(linhaJanela(s, it)) }
            banco.salvarSessao(final)
            excluirFotos(politicaRetencao().aExcluirAoEncerrar(fotosGuardadas()))
            atualizarEstadoFotos()
        }
        return s.id
    }

    private fun linhaJanela(s: SessaoAtiva, j: JanelaResumo) = LinhaJanela(
        s.id, s.epoch(j.inicioMs), j.perclosLongo, j.piscadasPorMinuto, j.duracaoMediaPiscadaMs,
        j.bocejosPorMinuto, j.desvioPitchGraus, null, j.fracaoOlharFora, j.fracaoSemicerrado,
    )

    private fun gravada(s: SessaoAtiva, r: ResumoSessao): SessaoGravada {
        fun n(vararg tipos: TipoEvento) = tipos.sumOf { r.eventosPorTipo[it] ?: 0 }
        return SessaoGravada(
            id = s.id,
            inicioEpochMs = s.epoch(s.inicioMs),
            duracaoMs = r.duracaoMs,
            tempoEmAlertaMs = r.tempoEmAlertaMs,
            piscadas = n(TipoEvento.PISCADA, TipoEvento.PISCADA_LONGA),
            microssonos = n(TipoEvento.MICROSSONO, TipoEvento.SONO),
            bocejos = n(TipoEvento.BOCEJO),
            desatencoes = n(TipoEvento.ROSTO_AUSENTE),
            distracoes = n(TipoEvento.CABECA_VIRADA, TipoEvento.OLHAR_DESVIADO),
            perclosMedio = r.perclosMedio,
            perclosMaximo = r.perclosMaximo,
            kss = null,
        )
    }

    // ── Fotos ────────────────────────────────────────────────────────────────

    /** Pede a próxima imagem da câmera como foto de teste, crua e anotada. */
    fun pedirFotoDeTeste() {
        val id = synchronized(trava) { sessao?.id } ?: 0L
        fotoPendente.set(
            PedidoFoto(EstadoMotorista.ATENTO, id, System.currentTimeMillis(), repositorio.usoTotalMs(), teste = true),
        )
    }

    /**
     * Roda na fila de preparo, com o quadro cru (antes do realce, como no protótipo Python).
     * Se havia foto pedida, grava a cópia e deixa a versão anotada para o próximo resultado do MediaPipe.
     */
    fun aoReceberQuadroCru(bitmap: Bitmap) {
        val pedido = fotoPendente.getAndSet(null) ?: return
        val copia = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        if (copia != null) disco.launch { gravarFoto(pedido, copia, anotada = false) }
        anotacaoPendente.set(pedido)
    }

    fun querAnotacao() = anotacaoPendente.get() != null

    /** [desenhar] devolve a cópia da imagem com a análise desenhada; só é chamado se houver anotação pedida. */
    fun gravarAnotada(desenhar: () -> Bitmap?) {
        val pedido = anotacaoPendente.getAndSet(null) ?: return
        val copia = desenhar() ?: return
        disco.launch { gravarFoto(pedido, copia, anotada = true) }
    }

    /** Roda em [disco]. A foto crua e a [anotada] viram dois arquivos e dois registros. */
    private fun gravarFoto(pedido: PedidoFoto, bitmap: Bitmap, anotada: Boolean) {
        val pasta = ajustes.value.fotos.pasta
        val nome = nomeFoto(pedido.estado, pedido.epochMs, ZoneId.systemDefault(), anotada, pedido.teste)
        val uri = gravador.salvarJpeg(pasta, nome, bitmap)
        bitmap.recycle()
        if (uri == null) {
            mensagem.value = if (pasta == null) {
                "Não foi possível gravar a foto em ${GravadorArquivos.PASTA_FOTOS}. Veja se a memória está cheia."
            } else {
                "Não foi possível gravar a foto na pasta escolhida. Escolha de novo ou volte para a pasta HELIOS."
            }
            return
        }
        banco.gravarFoto(FotoSalva(0, pedido.sessao, pedido.epochMs, pedido.usoMs, nome, uri))
        ultimaFoto = nome
        if (pedido.teste && !anotada) {
            val onde = if (pasta == null) "${GravadorArquivos.PASTA_FOTOS} (álbum HELIOS na galeria)" else "na pasta escolhida"
            mensagem.value = "Foto de teste gravada em $onde: $nome"
        }
        excluirFotos(politicaRetencao().aExcluir(fotosGuardadas(), pedido.usoMs))
        atualizarEstadoFotos()
    }

    /** Roda em [disco]. */
    fun atualizarEstadoFotos() {
        val f = ajustes.value.fotos
        val onde = if (f.pasta == null) GravadorArquivos.PASTA_FOTOS else "pasta escolhida"
        _estadoFotos.value = EstadoFotos(f.consentimento, onde, banco.fotos().size, ultimaFoto)
    }

    /** Roda em [disco]. Botão "Apagar fotos e histórico". */
    fun apagarTudo() {
        banco.fotos().forEach { gravador.excluir(it.uri) }
        banco.limparTudo()
        ultimaFoto = null
        atualizarEstadoFotos()
    }

    private fun fotosGuardadas() = banco.fotos().map { FotoGuardada(it.id, it.usoNaCapturaMs) }

    private fun politicaRetencao() = ajustes.value.fotos.let { PoliticaRetencao(it.maxFotos, it.retencao) }

    /** Apaga o arquivo e depois o registro. Se o arquivo já tiver sido apagado à mão, o registro sai igual. */
    private fun excluirFotos(ids: List<Long>) {
        if (ids.isEmpty()) return
        val alvo = ids.toSet()
        banco.fotos().filter { it.id in alvo }.forEach { gravador.excluir(it.uri) }
        banco.excluirFotos(alvo)
    }

    private companion object {
        const val JANELA_COMPLETA_MS = 30_000L
    }
}
