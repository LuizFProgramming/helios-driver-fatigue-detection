package br.unicid.helios.app

import android.graphics.Bitmap
import android.os.SystemClock
import br.unicid.helios.app.adaptadores.fotos.GravadorArquivos
import br.unicid.helios.app.portas.FotoSalva
import br.unicid.helios.app.portas.GravadorFotos
import br.unicid.helios.app.portas.RepositorioViagens
import br.unicid.helios.app.portas.SessaoGravada
import br.unicid.helios.dominio.Sensibilidade
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.tipos.RecortesOlhos
import br.unicid.helios.dominio.validacao.IntervaloRoteiro
import br.unicid.helios.dominio.validacao.PassoRoteiro
import br.unicid.helios.dominio.sessao.AgregadorSessao
import br.unicid.helios.dominio.sessao.JanelaResumo
import br.unicid.helios.dominio.sessao.PoliticaEvidencia
import br.unicid.helios.dominio.sessao.ResumoSessao
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.TipoEvento
import br.unicid.helios.protocolos.ArquivoPerfis
import br.unicid.helios.protocolos.Csv
import br.unicid.helios.protocolos.FotoGuardada
import br.unicid.helios.protocolos.FotoRegistrada
import br.unicid.helios.protocolos.LinhaEvento
import br.unicid.helios.protocolos.LinhaJanela
import br.unicid.helios.protocolos.LinhaQuadro
import br.unicid.helios.protocolos.PoliticaRetencao
import br.unicid.helios.protocolos.RegistroQuadros
import br.unicid.helios.protocolos.RegistroRecortes
import br.unicid.helios.protocolos.fotoDoEvento
import br.unicid.helios.protocolos.nomeFoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

/** O que acompanha cada leitura e só o app sabe: vai para o `janelas.csv` e o `quadros.csv` (Parte D). */
class ContextoLeitura(
    val perfil: String? = null,
    val sensibilidade: Sensibilidade = Sensibilidade.PADRAO,
    val delegate: String = "",
    val rmsAceleracao: Double = 0.0,
    val blendshapes: Map<String, Double>? = null,
    /** Roteiro de teste (ADR-015): óculos declarados, passo atual e recortes dos olhos do quadro. */
    val oculos: Boolean = false,
    val passo: PassoRoteiro? = null,
    val recortes: RecortesOlhos? = null,
)

/** CSV que cresce durante a viagem: linhas acumuladas e, só na fila de disco, o endereço do arquivo. */
private class ArquivoCrescente(val nome: String, val cabecalho: String, inicioMs: Long) {
    val linhas = StringBuilder()
    var enviadoMs = inicioMs
    var ultimoMs = inicioMs - INTERVALO_RECORTES_MS
    var uri: String? = null
    var falhou = false
}

/** Recortes dos olhos: 5 por segundo bastam para o treino e deixam o arquivo em uns 6 MB por roteiro. */
private const val INTERVALO_RECORTES_MS = 200L

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

        var contexto = ContextoLeitura()

        val quadros = ArquivoCrescente("helios-quadros-viagem-$id.csv", RegistroQuadros.CABECALHO, inicioMs)
        val recortes = ArquivoCrescente("helios-recortes-viagem-$id.csv", RegistroRecortes.CABECALHO, inicioMs)

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

    /** Tempo de viagem para a tela de direção: da primeira à última leitura, pausas incluídas. Nulo sem viagem. */
    fun duracaoMs(): Long? = synchronized(trava) { sessao?.let { it.ultimoMs - it.inicioMs } }

    /** [latenciaMs]: da imagem até esta leitura; ver a ressalva da ESP32-CAM em [JanelaResumo.latenciaMediaMs]. */
    fun registrar(
        leitura: Leitura,
        latenciaMs: Long,
        contexto: ContextoLeitura = ContextoLeitura(),
    ) = synchronized(trava) {
        if (viagemEncerrada()) return@synchronized
        val t = leitura.timestampMs
        val s = sessao ?: abrir(t)
        s.ultimoMs = t
        s.contexto = contexto
        val a = ajustes.value
        val noRoteiro = contexto.passo != null && a.consentimentoRoteiro
        if (a.quadrosAtivos || noRoteiro) acumularQuadro(s, leitura, latenciaMs)
        if (noRoteiro) acumularRecortes(s, leitura)
        s.agregador.registrar(leitura, latenciaMs)?.let { janela -> aoFecharJanela(s, janela) }
        if (leitura.eventos.isNotEmpty()) {
            val m = leitura.metricas
            val linhas = leitura.eventos.map { e ->
                LinhaEvento(
                    s.id, s.epoch(e.inicioMs), e.duracaoMs, e.tipo, leitura.estado,
                    m?.perclosCurto ?: 0.0, m?.perclosLongo ?: 0.0, null, s.agregador.estadoEm(e.inicioMs),
                    e.yawMaximoGraus,
                )
            }
            disco.launch { banco.gravarEventos(linhas) }
        }
        val capturar = s.evidencia.avaliar(leitura)
        if (capturar && ajustes.value.fotos.ativas) {
            fotoPendente.set(PedidoFoto(leitura.estado, s.id, s.epoch(t), s.usoAgora(t)))
        }
    }

    private fun acumularQuadro(s: SessaoAtiva, leitura: Leitura, latenciaMs: Long) {
        val c = s.contexto
        val linha = LinhaQuadro(
            s.epoch(leitura.timestampMs), leitura.estado, leitura.motivos, leitura.metricas, latenciaMs, c.delegate,
            c.rmsAceleracao, c.perfil, c.sensibilidade.name, c.blendshapes.orEmpty(), c.oculos,
            c.passo?.let { if (it.preparando) "PREPARO" else it.tarefa.name },
        )
        s.quadros.linhas.append(RegistroQuadros.linha(linha, ZoneId.systemDefault())).append('\n')
        if (leitura.timestampMs - s.quadros.enviadoMs >= ENVIO_QUADROS_MS) enviar(s.quadros, s.ultimoMs)
    }

    /** Recortes dos olhos só nas tarefas com rótulo, 5 por segundo (ADR-015). */
    private fun acumularRecortes(s: SessaoAtiva, leitura: Leitura) {
        val c = s.contexto
        val passo = c.passo
        val recortes = c.recortes
        val t = leitura.timestampMs
        val comRotulo = passo != null && !passo.preparando && passo.tarefa.olho != null
        if (!comRotulo || recortes == null || t - s.recortes.ultimoMs < INTERVALO_RECORTES_MS) return
        s.recortes.ultimoMs = t
        val zona = ZoneId.systemDefault()
        val linhas = RegistroRecortes.linhas(s.epoch(t), passo.tarefa, c.oculos, leitura.metricas, recortes, zona)
        s.recortes.linhas.append(linhas)
        if (t - s.recortes.enviadoMs >= ENVIO_QUADROS_MS) enviar(s.recortes, t)
    }

    /** Leva as linhas acumuladas para a fila de disco: a primeira vez cria o arquivo com o cabeçalho. */
    private fun enviar(arquivo: ArquivoCrescente, agoraMs: Long) {
        if (arquivo.linhas.isEmpty()) return
        val texto = arquivo.linhas.toString()
        arquivo.linhas.setLength(0)
        arquivo.enviadoMs = agoraMs
        val pasta = ajustes.value.fotos.pasta
        disco.launch {
            if (arquivo.falhou) return@launch
            val uri = arquivo.uri
            val ok = if (uri == null) {
                arquivo.uri = gravador.criarTexto(pasta, arquivo.nome, arquivo.cabecalho + "\n" + texto)
                arquivo.uri != null
            } else {
                gravador.acrescentar(uri, texto)
            }
            if (!ok) {
                arquivo.falhou = true
                mensagem.value = "Não foi possível gravar ${arquivo.nome}. Veja se a memória está cheia."
            }
        }
    }

    /** `roteiro.csv` (ADR-015): as tarefas feitas, com os horários da viagem. Antes, esvazia quadros e recortes. */
    fun gravarRoteiro(intervalos: List<IntervaloRoteiro>, oculos: Boolean) = synchronized(trava) {
        val s = sessao ?: return@synchronized
        enviar(s.quadros, s.ultimoMs)
        enviar(s.recortes, s.ultimoMs)
        val comEpoch = intervalos.map { it.copy(inicioMs = s.epoch(it.inicioMs), fimMs = s.epoch(it.fimMs)) }
        val pasta = ajustes.value.fotos.pasta
        disco.launch {
            val nome = "helios-roteiro-viagem-${s.id}.csv"
            val ok = gravador.salvarTexto(pasta, nome, Csv.roteiro(s.id, comEpoch, oculos, ZoneId.systemDefault()))
            mensagem.value = if (ok) "Roteiro salvo em ${onde(pasta)}: $nome" else "Falha ao gravar o roteiro."
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
            enviar(s.quadros, s.ultimoMs)
            enviar(s.recortes, s.ultimoMs)
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
        j.tempoPorEstadoMs, j.fpsMedio, j.latenciaMediaMs, j.latenciaP95Ms, j.cnnFechadoMedia, j.cnnDiscordancia,
        j.zonaEspelhoMs, j.zonaPainelMs, j.zonaOutraMs, j.manobraMs, s.contexto.perfil, s.contexto.sensibilidade.name,
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

    // ── Exportação ───────────────────────────────────────────────────────────

    /**
     * Exporta `eventos.csv`, `janelas.csv` e `instalacao.csv` de uma viagem (ou de todas, com [sessao] nulo)
     * para [pasta]; nula = Download/HELIOS.
     */
    fun exportarCsv(sessao: Long?, pasta: String?) {
        disco.launch {
            val zona = ZoneId.systemDefault()
            val fotos = banco.fotos().map { FotoRegistrada(it.capturaEpochMs, it.nome) to it.sessao }
            val eventos = banco.eventos(sessao).map { e ->
                val daViagem = fotos.filter { it.second == e.sessao }.map { it.first }
                e.copy(foto = fotoDoEvento(e.tipo, e.inicioEpochMs, e.duracaoMs, daViagem))
            }
            val sufixo = (sessao?.let { "viagem-$it" } ?: "todas") + "-" + agora()
            val perfis = ajustes.value.perfis
            val ok = gravador.salvarTexto(pasta, "helios-eventos-$sufixo.csv", Csv.eventos(eventos, zona)) &&
                gravador.salvarTexto(pasta, "helios-janelas-$sufixo.csv", Csv.janelas(banco.janelas(sessao), zona)) &&
                gravador.salvarTexto(pasta, "helios-instalacao-$sufixo.csv", Csv.instalacao(perfis, zona))
            mensagem.value = if (ok) {
                "CSV salvo em ${onde(pasta)}: eventos, janelas e instalação (helios-*-$sufixo.csv)"
            } else {
                "Não foi possível gravar o CSV em ${onde(pasta)}."
            }
        }
    }

    /** Backup dos perfis: o app não usa o backup do Android (allowBackup="false"), então o arquivo é a cópia. */
    fun exportarPerfis(pasta: String?) {
        val perfis = ajustes.value.perfis
        disco.launch {
            val agora = agora()
            val nome = "helios-perfis-$agora.txt"
            val instalacao = Csv.instalacao(perfis, ZoneId.systemDefault())
            val ok = gravador.salvarTexto(pasta, nome, ArquivoPerfis.escrever(perfis), "text/plain") &&
                gravador.salvarTexto(pasta, "helios-instalacao-$agora.csv", instalacao)
            mensagem.value = if (ok) "${perfis.size} perfis salvos em ${onde(pasta)}: $nome" else "Falha ao gravar."
        }
    }

    private fun agora() = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())

    private fun onde(pasta: String?) = if (pasta == null) GravadorArquivos.PASTA_CSV else "pasta escolhida"

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
        const val ENVIO_QUADROS_MS = 1_000L
    }
}
