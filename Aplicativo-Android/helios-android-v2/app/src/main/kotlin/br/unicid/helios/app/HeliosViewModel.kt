package br.unicid.helios.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.unicid.helios.app.adaptadores.alarme.AlarmeAndroid
import br.unicid.helios.app.adaptadores.banco.BancoHelios
import br.unicid.helios.app.adaptadores.camera.FonteCameraX
import br.unicid.helios.app.adaptadores.esp32.BuzzerEsp32
import br.unicid.helios.app.adaptadores.esp32.FonteEsp32
import br.unicid.helios.app.adaptadores.esp32.RedeHeliosCam
import br.unicid.helios.app.adaptadores.fotos.GravadorArquivos
import br.unicid.helios.app.adaptadores.inercial.AcelerometroAndroid
import br.unicid.helios.app.adaptadores.mediapipe.DetectorMediapipe
import br.unicid.helios.app.adaptadores.opencv.RealceOpenCv
import br.unicid.helios.app.adaptadores.visor.BuffersVisor
import br.unicid.helios.app.adaptadores.visor.DesenhistaMalha
import br.unicid.helios.app.adaptadores.visor.Malha
import br.unicid.helios.app.adaptadores.voz.VozAndroid
import br.unicid.helios.app.portas.ConsumidorVisual
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.app.portas.FonteDeImagens
import br.unicid.helios.app.portas.FotoSalva
import br.unicid.helios.app.portas.Imagem
import br.unicid.helios.app.portas.PreProcessador
import br.unicid.helios.app.portas.SessaoGravada
import br.unicid.helios.dominio.calibracao.Calibrador
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.MonitorMotorista
import br.unicid.helios.dominio.sessao.AgregadorSessao
import br.unicid.helios.dominio.sessao.JanelaResumo
import br.unicid.helios.dominio.sessao.PoliticaEvidencia
import br.unicid.helios.dominio.sessao.ResumoSessao
import br.unicid.helios.dominio.sinais.ContadorEmJanela
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.TipoEvento
import br.unicid.helios.protocolos.ControladorFala
import br.unicid.helios.protocolos.Csv
import br.unicid.helios.protocolos.EstadoTela
import br.unicid.helios.protocolos.FotoGuardada
import br.unicid.helios.protocolos.FotoRegistrada
import br.unicid.helios.protocolos.LinhaEvento
import br.unicid.helios.protocolos.LinhaJanela
import br.unicid.helios.protocolos.PoliticaRetencao
import br.unicid.helios.protocolos.fotoDoEvento
import br.unicid.helios.protocolos.fotosDoEvento
import br.unicid.helios.protocolos.miniatura
import br.unicid.helios.protocolos.linhasDeMetricas
import br.unicid.helios.protocolos.modeloDeTela
import br.unicid.helios.protocolos.nomeFoto
import br.unicid.helios.protocolos.planoDeAlerta
import br.unicid.helios.protocolos.sinalDeRetorno
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

/** Números para medir o desempenho no aparelho (ligar em Ajustes > Desempenho). */
data class Diagnostico(
    val fps: Int,
    val descartados: Long,
    val memoriaMb: Long,
    val latenciaMs: Long,
    val poolDeBitmaps: Boolean,
)

data class TelaUi(
    val estado: EstadoTela? = null,
    val conexao: Conexao = Conexao.DESCONECTADA,
    val fps: Int = 0,
    val emMovimento: Boolean = false,
    val progressoCalibracao: Double? = null,
    val aceleracaoGpu: Boolean = false,
    val capturando: Boolean = false,
    /** Viagem aberta mas com a câmera parada pelo botão "Pausar" (parada para descanso). */
    val pausada: Boolean = false,
    val diagnostico: Diagnostico? = null,
    /** Painel do visor: estado e todos os números usados na decisão (vazio com o visor desligado). */
    val metricas: List<String> = emptyList(),
)

/** Um quadro do visor: a imagem analisada com a malha desenhada. [versao] muda a cada quadro. */
class QuadroVisor(val imagem: Bitmap, val versao: Long)

/** Um evento da viagem no histórico, com a foto daquele momento (a anotada, se houver). */
data class EventoDetalhe(val evento: LinhaEvento, val foto: FotoRegistrada?)

/** Uma viagem já gravada, aberta na tela de histórico. [fotosSemEvento]: fotos de alertas sem evento ligado (fadiga acumulada). */
data class DetalheViagem(
    val sessao: SessaoGravada,
    val janelas: List<LinhaJanela>,
    val fotos: Int,
    val eventos: List<EventoDetalhe> = emptyList(),
    val fotosSemEvento: List<FotoRegistrada> = emptyList(),
)

/**
 * Situação das fotos, mostrada no menu e nos ajustes para o usuário saber se elas estão sendo gravadas e onde.
 * [ultima] é o nome do último arquivo gravado nesta abertura do app.
 */
data class EstadoFotos(val ligadas: Boolean, val onde: String, val gravadas: Int, val ultima: String?)

/** Números da viagem em andamento para o painel do menu. */
data class ViagemAtual(val sessao: SessaoGravada, val piscadasPorMinuto: Int, val pausada: Boolean)

/**
 * Monta o pipeline do TCC:
 *   Captura → (realce OpenCV) → MediaPipe → MonitorMotorista → Alarme, voz e tela
 *                                              ↑ acelerômetro      ↓
 *                                                      sessão: histórico, fotos, CSV
 * Threads:
 *  - imagens: a fonte entrega e já descarta as velhas; o pré-processamento e o envio ao MediaPipe
 *    rodam em [preparo].
 *  - domínio: TODA chamada ao [monitor] e ao agregador da sessão acontece em [dominio] (uma thread só),
 *    então o domínio não precisa de trava.
 *  - disco: banco e arquivos em [disco] (uma thread só), fora do caminho do alarme.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HeliosViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = RepositorioAjustes(app)
    private val _ajustes = MutableStateFlow(repositorio.ler())
    val ajustes: StateFlow<Ajustes> = _ajustes

    private val _tela = MutableStateFlow(TelaUi())
    val tela: StateFlow<TelaUi> = _tela

    private val _historico = MutableStateFlow<List<SessaoGravada>>(emptyList())
    val historico: StateFlow<List<SessaoGravada>> = _historico

    private val _detalhe = MutableStateFlow<DetalheViagem?>(null)
    val detalhe: StateFlow<DetalheViagem?> = _detalhe

    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem

    private val _visor = MutableStateFlow<QuadroVisor?>(null)
    val visor: StateFlow<QuadroVisor?> = _visor

    private val _viagemAtual = MutableStateFlow<ViagemAtual?>(null)
    val viagemAtual: StateFlow<ViagemAtual?> = _viagemAtual

    private val _estadoFotos = MutableStateFlow(EstadoFotos(false, GravadorArquivos.PASTA_FOTOS, 0, null))
    val estadoFotos: StateFlow<EstadoFotos> = _estadoFotos

    @Volatile
    private var ultimaFoto: String? = null

    private val dominio = Dispatchers.Default.limitedParallelism(1)
    private val preparo = Dispatchers.Default.limitedParallelism(1)
    private val disco = Dispatchers.IO.limitedParallelism(1)

    /**
     * Escopo próprio da fila de disco: não é cancelado junto com o ViewModel, para o resumo final
     * de uma viagem ser gravado mesmo quando o app fecha, sempre DEPOIS das gravações anteriores.
     */
    private val escopoDisco = CoroutineScope(SupervisorJob() + disco)

    /** Trocado a cada viagem nova, para cronômetros e eventos não passarem de uma viagem para outra. */
    private var monitor = MonitorMotorista(calibracao = _ajustes.value.calibracao)
    private val detector = DetectorMediapipe(app)
    private val alarme = AlarmeAndroid(app)
    private val voz = VozAndroid(app)
    private val banco = BancoHelios(app)
    private val gravador = GravadorArquivos(app.contentResolver)
    private val miniaturas = LruCache<String, Bitmap>(MINIATURAS_EM_CACHE)
    private val fps = ContadorEmJanela(UM_SEGUNDO_MS)
    private val fala = ControladorFala()
    private var calibrador: Calibrador? = null
    private var buzzerAtual = 0

    private var rede: RedeHeliosCam? = null
    private var buzzer: BuzzerEsp32? = null
    private var captura: Job? = null

    @Volatile
    private var fonteAtual: FonteDeImagens? = null

    private val travaSessao = Any()
    private var sessao: SessaoAtiva? = null
    private val fotoPendente = AtomicReference<PedidoFoto?>(null)

    /** Depois de "Encerrar viagem", a câmera só volta com "Nova viagem" (não sozinha ao recriar a tela). */
    @Volatile
    private var viagemEncerrada = false

    /** Com a viagem pausada a câmera também não volta sozinha: só com "Retomar". */
    @Volatile
    private var pausada = false

    @Volatile
    private var ultimaLeituraMs = 0L

    /** Última leitura do domínio, para o texto da foto anotada e as cores dos olhos no visor. */
    @Volatile
    private var ultimaLeitura: Leitura? = null

    private var estadoAnterior: EstadoMotorista? = null
    private val anotacaoPendente = AtomicReference<PedidoFoto?>(null)
    private val buffersVisor = BuffersVisor()
    private val desenhistaVisor = DesenhistaMalha()
    private val desenhistaFoto = DesenhistaMalha()
    private var versaoVisor = 0L

    init {
        _tela.update { it.copy(aceleracaoGpu = detector.delegate.name == "GPU") }
        detector.visual = object : ConsumidorVisual {
            override fun quer() = visorLigado() || anotacaoPendente.get() != null

            override fun receber(imagem: Bitmap, pontos: FloatArray, matriz: DoubleArray?) =
                aoReceberVisual(imagem, pontos, matriz)
        }
        viewModelScope.launch(dominio) { detector.quadros.collect { processar(monitor.processar(it)) } }
        viewModelScope.launch(dominio) {
            AcelerometroAndroid(app).amostras().collect { (t, m) ->
                monitor.registrarAceleracao(t, m)
                // Atualiza o menu também com a câmera parada (viagem pausada ou encerrada).
                val andando = monitor.emMovimento()
                if (_tela.value.emMovimento != andando) _tela.update { it.copy(emMovimento = andando) }
            }
        }
        carregarHistorico()
        escopoDisco.launch { atualizarEstadoFotos() }
    }

    // ── Captura ──────────────────────────────────────────────────────────────

    /** Com a permissão de câmera concedida. Não faz nada se já estiver capturando ou com a viagem encerrada. */
    fun iniciarCapturaSePreciso() {
        if (captura?.isActive == true || viagemEncerrada || pausada) return
        iniciarCaptura()
    }

    /**
     * "Pausar": para a câmera e o alarme, mas a viagem continua aberta (parada para descanso, abastecer).
     * O tempo parado não entra no tempo da viagem: o agregador ignora intervalos sem leitura.
     */
    fun pausar() {
        if (viagemEncerrada || pausada) return
        pausada = true
        captura?.cancel()
        captura = null
        _visor.value = null
        _tela.update { it.copy(capturando = false, pausada = true, estado = null, conexao = Conexao.DESCONECTADA) }
        viewModelScope.launch(dominio) {
            silenciarAlertas()
            atualizarViagemAtualAgora()
        }
    }

    /**
     * "Retomar": a câmera volta na mesma viagem. O monitor recomeça do zero, para um olho que estava
     * fechado antes da pausa não virar "olhos fechados há 10 minutos".
     */
    fun retomar() {
        if (!pausada) return
        pausada = false
        _tela.update { it.copy(pausada = false) }
        viewModelScope.launch(dominio) {
            monitor = MonitorMotorista(calibracao = _ajustes.value.calibracao)
            withContext(Dispatchers.Main) { iniciarCapturaSePreciso() }
        }
    }

    /** Atualiza os números do painel do menu (chamado enquanto o painel está aberto). */
    fun atualizarViagemAtual() {
        viewModelScope.launch(dominio) { atualizarViagemAtualAgora() }
    }

    private fun atualizarViagemAtualAgora() {
        val atual = synchronized(travaSessao) { sessao?.let { s -> gravada(s, s.agregador.resumo()) } }
        _viagemAtual.value = atual?.let {
            ViagemAtual(it, ultimaLeitura?.metricas?.piscadasNoMinuto ?: 0, pausada)
        }
    }

    /** "Nova viagem": monitor zerado (cronômetros e janelas), câmera de volta. */
    fun novaViagem() {
        viagemEncerrada = false
        pausada = false
        _tela.update { it.copy(pausada = false) }
        viewModelScope.launch(dominio) {
            monitor = MonitorMotorista(calibracao = _ajustes.value.calibracao)
            withContext(Dispatchers.Main) { iniciarCapturaSePreciso() }
        }
    }

    private fun iniciarCaptura() {
        captura?.cancel()
        val a = _ajustes.value
        _tela.update { it.copy(capturando = true) }
        captura = viewModelScope.launch {
            val fonte = criarFonte(a)
            fonteAtual = fonte
            launch { fonte.conexao.collect { c -> _tela.update { it.copy(conexao = c) } } }
            launch(dominio) { vigiarSinal() }
            val realce = if (a.realceOpenCv) RealceOpenCv() else null
            try {
                fonte.imagens().collect { imagem -> withContext(preparo) { enviar(imagem, realce) } }
            } finally {
                withContext(NonCancellable + preparo) { realce?.close() }
            }
        }
    }

    /**
     * Se nenhuma leitura chegar por [SEM_SINAL_MS] (câmera caiu, Wi-Fi da ESP32 perdido), o alarme
     * para e a tela volta a "procurando": um alarme preso tocando sem análise não ajuda o motorista.
     */
    private suspend fun vigiarSinal() {
        while (currentCoroutineContext().isActive) {
            delay(UM_SEGUNDO_MS)
            val ultima = ultimaLeituraMs
            if (ultima > 0 && SystemClock.elapsedRealtime() - ultima > SEM_SINAL_MS) {
                ultimaLeituraMs = 0
                silenciarAlertas()
                _tela.update { it.copy(estado = null) }
            }
        }
    }

    /** Roda em [dominio]. */
    private fun silenciarAlertas() {
        alarme.parar()
        estadoAnterior = null
        ultimaLeitura = null
        voz.calar()
        fala.avaliar(0, EstadoMotorista.ATENTO, null)
        if (buzzerAtual != 0) buzzer?.tocar(0)
        buzzerAtual = 0
    }

    /** Encerra a viagem: para a câmera e o alarme, fecha a sessão e mostra o resumo no histórico. */
    fun encerrarViagem() {
        viagemEncerrada = true
        pausada = false
        captura?.cancel()
        captura = null
        _viagemAtual.value = null
        _tela.update {
            it.copy(capturando = false, pausada = false, estado = null, conexao = Conexao.DESCONECTADA)
        }
        viewModelScope.launch(dominio) {
            silenciarAlertas() // de novo, na thread do domínio: cobre uma leitura que estava em andamento
            val id = finalizarSessao()
            escopoDisco.launch {
                recarregarHistorico()
                id?.let { abrirDetalheAgora(it) }
            }
        }
    }

    fun calibrar() {
        viewModelScope.launch(dominio) { calibrador = Calibrador(SystemClock.elapsedRealtime()) }
    }

    /** Ajustes das fotos valem na hora, sem esperar o "Salvar": a autorização não pode se perder ao sair da tela. */
    fun salvarFotos(fotos: AjustesFotos) {
        _ajustes.update { it.copy(fotos = fotos) }
        repositorio.salvar(_ajustes.value)
        escopoDisco.launch { atualizarEstadoFotos() }
    }

    /**
     * Resposta "Permitir" da pergunta da abertura: liga as fotos na pasta fixa HELIOS e grava uma foto de teste,
     * que cria a pasta e prova que a gravação funciona.
     */
    fun autorizarFotos() {
        _ajustes.update { it.copy(perguntouFotos = true, fotos = it.fotos.copy(consentimento = true, pasta = null)) }
        repositorio.salvar(_ajustes.value)
        escopoDisco.launch { atualizarEstadoFotos() }
        fotoDeTeste()
    }

    /** Resposta "Agora não": não pergunta de novo; dá para ligar depois em Ajustes. */
    fun recusarFotos() {
        _ajustes.update { it.copy(perguntouFotos = true) }
        repositorio.salvar(_ajustes.value)
    }

    /** Grava a próxima imagem da câmera como foto de teste (crua e anotada), em qualquer estado do motorista. */
    fun fotoDeTeste() {
        when {
            !_ajustes.value.fotos.consentimento ->
                _mensagem.value = "Ligue \"Autorizo guardar fotos dos alertas\" em Ajustes para gravar a foto de teste."
            !_tela.value.capturando ->
                _mensagem.value = "A câmera precisa estar ligada para a foto de teste: Menu › Nova viagem ou Retomar."
            else -> {
                val agora = System.currentTimeMillis()
                val id = synchronized(travaSessao) { sessao?.id } ?: 0L
                fotoPendente.set(PedidoFoto(EstadoMotorista.ATENTO, id, agora, repositorio.usoTotalMs(), teste = true))
                _mensagem.value = "Gravando a foto de teste…"
            }
        }
    }

    fun salvarAjustes(novos: Ajustes) {
        val anterior = _ajustes.value
        _ajustes.value = novos
        repositorio.salvar(novos)
        val mudouCaptura = novos.fonte != anterior.fonte || novos.esp32 != anterior.esp32 ||
            novos.realceOpenCv != anterior.realceOpenCv || novos.reaproveitarBitmaps != anterior.reaproveitarBitmaps
        if (mudouCaptura && captura != null) iniciarCaptura()
    }

    private fun criarFonte(a: Ajustes): FonteDeImagens = when (a.fonte) {
        TipoFonte.CAMERA_DO_APARELHO -> {
            rede?.liberar()
            rede = null
            buzzer = null
            FonteCameraX(getApplication<Application>(), a.reaproveitarBitmaps)
        }
        TipoFonte.ESP32_CAM -> {
            rede?.liberar()
            val novaRede = RedeHeliosCam(getApplication<Application>(), a.esp32)
            rede = novaRede
            buzzer = BuzzerEsp32(novaRede, a.esp32, viewModelScope)
            FonteEsp32(novaRede, a.esp32)
        }
    }

    /**
     * Roda em [preparo]. A foto (se pedida) sai do frame cru, antes do realce, como no protótipo Python.
     * Uma falha num frame (OpenCV, MediaPipe) descarta só aquele frame e devolve o bitmap ao pool.
     */
    private fun enviar(imagem: Imagem, realce: PreProcessador?) {
        try {
            fotoPendente.getAndSet(null)?.let { pedido ->
                val copia = imagem.bitmap.copy(Bitmap.Config.ARGB_8888, false)
                if (copia != null) escopoDisco.launch { gravarFoto(pedido, copia, anotada = false) }
                anotacaoPendente.set(pedido) // a versão com a análise sai no próximo resultado do MediaPipe (até ~100 ms)
            }
            val bitmap = realce?.processar(imagem.bitmap) ?: imagem.bitmap
            detector.enviar(Imagem(bitmap, imagem.timestampMs, imagem.liberar))
        } catch (e: RuntimeException) {
            Log.w(TAG, "Frame descartado", e)
            imagem.liberar()
        }
    }

    // ── Domínio ──────────────────────────────────────────────────────────────

    /**
     * Roda na thread do MediaPipe. Visor: copia a imagem analisada num buffer e desenha a malha por cima.
     * Foto anotada: a mesma imagem, com a malha e o painel de métricas, gravada ao lado da foto crua.
     */
    private fun aoReceberVisual(imagem: Bitmap, pontos: FloatArray, matriz: DoubleArray?) {
        val leitura = ultimaLeitura
        val m = leitura?.metricas
        val malha = Malha(pontos, matriz, m?.olhoDireitoFechado, m?.olhoEsquerdoFechado, m?.olhar?.fora == true)
        if (visorLigado()) {
            val quadro = buffersVisor.obter(imagem.width, imagem.height)
            Canvas(quadro).drawBitmap(imagem, 0f, 0f, null)
            desenhistaVisor.desenhar(quadro, malha)
            _visor.value = QuadroVisor(quadro, ++versaoVisor)
        }
        anotacaoPendente.getAndSet(null)?.let { pedido ->
            val copia = imagem.copy(Bitmap.Config.ARGB_8888, true) ?: return
            desenhistaFoto.desenhar(copia, malha, leitura?.let(::linhasDeMetricas).orEmpty())
            escopoDisco.launch { gravarFoto(pedido, copia, anotada = true) }
        }
    }

    private fun visorLigado() = _ajustes.value.mostrarVisor && _tela.value.capturando

    /** Roda na thread [dominio]. Resultados que chegam depois de encerrar a viagem são ignorados. */
    private fun processar(leitura: Leitura) {
        if (!_tela.value.capturando) return
        ultimaLeituraMs = SystemClock.elapsedRealtime()
        ultimaLeitura = leitura
        fps.registrar(leitura.timestampMs)
        avancarCalibracao(leitura)
        acionarAlarme(leitura)
        registrarNaSessao(leitura)
        val a = _ajustes.value
        _tela.update {
            if (!it.capturando) return@update it // a viagem foi encerrada enquanto esta leitura era processada
            it.copy(
                estado = modeloDeTela(leitura),
                fps = fps.contar(leitura.timestampMs),
                emMovimento = leitura.emMovimento,
                progressoCalibracao = calibrador?.progresso(leitura.timestampMs),
                diagnostico = if (a.mostrarDiagnostico) diagnostico(a) else null,
                metricas = if (a.mostrarVisor) linhasDeMetricas(leitura) else emptyList(),
            )
        }
    }

    private fun diagnostico(a: Ajustes): Diagnostico {
        val rt = Runtime.getRuntime()
        return Diagnostico(
            fps = _tela.value.fps,
            descartados = fonteAtual?.descartados ?: 0,
            memoriaMb = (rt.totalMemory() - rt.freeMemory()) / BYTES_POR_MB,
            latenciaMs = detector.latenciaMs,
            poolDeBitmaps = a.reaproveitarBitmaps && a.fonte == TipoFonte.CAMERA_DO_APARELHO,
        )
    }

    private fun avancarCalibracao(leitura: Leitura) {
        val c = calibrador ?: return
        leitura.metricas?.let { m ->
            c.adicionar(
                m.earBruto, m.pose?.pitchGraus, m.pose?.yawGraus, m.earDireito, m.earEsquerdo,
                m.olhar.horizontal, m.olhar.vertical,
            )
        }
        val resultado = c.resultado(leitura.timestampMs) ?: return
        monitor.calibrar(resultado)
        calibrador = null
        _ajustes.update { it.copy(calibracao = resultado) }
        repositorio.salvar(_ajustes.value)
    }

    private fun acionarAlarme(leitura: Leitura) {
        // Rosto perdido por instantes (cabeça caída no CRÍTICO, por exemplo): o alarme continua como estava
        // e o estado anterior fica guardado, para o som de retorno tocar quando o rosto voltar.
        if (leitura.estado == EstadoMotorista.SEM_ROSTO) return
        val plano = planoDeAlerta(leitura.estado, leitura.motivos, leitura.segundosNoEstado)
        if (plano == null) alarme.parar() else alarme.tocar(plano.padrao)
        if (sinalDeRetorno(estadoAnterior, leitura.estado)) alarme.tocarRetorno()
        estadoAnterior = leitura.estado
        val frase = fala.avaliar(leitura.timestampMs, leitura.estado, plano)
        if (frase != null && _ajustes.value.alertaPorVoz) voz.falar(frase)
        val codigo = plano?.padrao?.codigoBuzzer ?: 0
        if (_ajustes.value.buzzerNaCamera && codigo != buzzerAtual) buzzer?.tocar(codigo)
        buzzerAtual = codigo
    }

    // ── Sessão (histórico, fotos) ────────────────────────────────────────────

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

    private fun registrarNaSessao(leitura: Leitura) = synchronized(travaSessao) {
        if (viagemEncerrada) return@synchronized
        val t = leitura.timestampMs
        val s = sessao ?: abrirSessao(t)
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
            escopoDisco.launch { banco.gravarEventos(linhas) }
        }
        val capturar = s.evidencia.avaliar(leitura)
        if (capturar && _ajustes.value.fotos.ativas) {
            fotoPendente.set(PedidoFoto(leitura.estado, s.id, s.epoch(t), s.usoAgora(t)))
        }
    }

    private fun abrirSessao(t: Long): SessaoAtiva {
        val offset = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        val nova = SessaoAtiva(t + offset, offsetEpochMs = offset, inicioMs = t, usoBaseMs = repositorio.usoTotalMs())
        sessao = nova
        val inicial = gravada(nova, nova.agregador.resumo())
        escopoDisco.launch {
            banco.salvarSessao(inicial)
            recarregarHistorico()
        }
        return nova
    }

    private fun aoFecharJanela(s: SessaoAtiva, j: JanelaResumo) {
        val linha = linhaJanela(s, j)
        val parcial = gravada(s, s.agregador.resumo())
        repositorio.salvarUsoTotal(s.usoAgora(s.ultimoMs))
        escopoDisco.launch {
            banco.gravarJanela(linha)
            banco.salvarSessao(parcial)
        }
    }

    /**
     * Fecha a sessão aberta. O retrato da sessão é tirado aqui, sob a trava; a gravação vai para o fim
     * da fila de disco, depois de tudo que a viagem já pediu para gravar (fotos, janelas, resumos parciais).
     * Grava a janela parcial e o resumo final e aplica a retenção "ao encerrar". Devolve o id.
     */
    private fun finalizarSessao(): Long? {
        fotoPendente.set(null)
        anotacaoPendente.set(null)
        _visor.value = null
        val s: SessaoAtiva
        val resumo: ResumoSessao
        synchronized(travaSessao) {
            s = sessao ?: return null
            sessao = null
            resumo = s.agregador.resumo()
        }
        repositorio.salvarUsoTotal(s.usoAgora(s.ultimoMs))
        val parcial = resumo.janelas.lastOrNull()?.takeIf { it.fimMs - it.inicioMs < JANELA_COMPLETA_MS }
        val final = gravada(s, resumo)
        escopoDisco.launch {
            parcial?.let {
                banco.gravarJanela(linhaJanela(s, it))
            }
            banco.salvarSessao(final)
            val guardadas = banco.fotos().map { FotoGuardada(it.id, it.usoNaCapturaMs) }
            excluirFotos(politicaRetencao().aExcluirAoEncerrar(guardadas))
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

    /** Roda em [disco]. A foto crua e a [anotada] (com a análise desenhada) viram dois arquivos e dois registros. */
    private fun gravarFoto(pedido: PedidoFoto, bitmap: Bitmap, anotada: Boolean) {
        val pasta = _ajustes.value.fotos.pasta
        val nome = nomeFoto(pedido.estado, pedido.epochMs, ZoneId.systemDefault(), anotada, pedido.teste)
        val uri = gravador.salvarJpeg(pasta, nome, bitmap)
        bitmap.recycle()
        if (uri == null) {
            _mensagem.value = if (pasta == null) {
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
            _mensagem.value = "Foto de teste gravada em $onde: $nome"
        }
        val fotos = banco.fotos().map { FotoGuardada(it.id, it.usoNaCapturaMs) }
        excluirFotos(politicaRetencao().aExcluir(fotos, pedido.usoMs))
        atualizarEstadoFotos()
    }

    /** Roda em [disco]. */
    private fun atualizarEstadoFotos() {
        val f = _ajustes.value.fotos
        val onde = if (f.pasta == null) GravadorArquivos.PASTA_FOTOS else "pasta escolhida"
        _estadoFotos.value = EstadoFotos(f.consentimento, onde, banco.fotos().size, ultimaFoto)
    }

    private fun politicaRetencao() = _ajustes.value.fotos.let { PoliticaRetencao(it.maxFotos, it.retencao) }

    /** Apaga o arquivo e depois o registro. Se o arquivo já tiver sido apagado à mão, o registro sai igual. */
    private fun excluirFotos(ids: List<Long>) {
        if (ids.isEmpty()) return
        val alvo = ids.toSet()
        banco.fotos().filter { it.id in alvo }.forEach { gravador.excluir(it.uri) }
        banco.excluirFotos(alvo)
    }

    // ── Histórico ────────────────────────────────────────────────────────────

    fun carregarHistorico() {
        escopoDisco.launch { recarregarHistorico() }
    }

    fun abrirDetalhe(id: Long?) {
        if (id == null) {
            _detalhe.value = null
            return
        }
        escopoDisco.launch { abrirDetalheAgora(id) }
    }

    fun definirKss(id: Long, kss: Int?) {
        escopoDisco.launch {
            banco.definirKss(id, kss)
            recarregarHistorico()
            abrirDetalheAgora(id)
        }
    }

    /**
     * Exporta `eventos.csv` e `janelas.csv` de uma viagem (ou de todas, com [sessao] nulo) para [pasta];
     * nula = Download/HELIOS.
     */
    fun exportarCsv(sessao: Long?, pasta: String?) {
        escopoDisco.launch {
            val zona = ZoneId.systemDefault()
            val fotos = banco.fotos().map { FotoRegistrada(it.capturaEpochMs, it.nome) to it.sessao }
            val eventos = banco.eventos(sessao).map { e ->
                val daViagem = fotos.filter { it.second == e.sessao }.map { it.first }
                e.copy(foto = fotoDoEvento(e.tipo, e.inicioEpochMs, e.duracaoMs, daViagem))
            }
            val sufixo = (sessao?.let { "viagem-$it" } ?: "todas") + "-" +
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now())
            val ok = gravador.salvarTexto(pasta, "helios-eventos-$sufixo.csv", Csv.eventos(eventos, zona)) &&
                gravador.salvarTexto(pasta, "helios-janelas-$sufixo.csv", Csv.janelas(banco.janelas(sessao), zona))
            val onde = if (pasta == null) GravadorArquivos.PASTA_CSV else "pasta escolhida"
            _mensagem.value = if (ok) {
                "CSV salvo em $onde: helios-eventos-$sufixo.csv e helios-janelas-$sufixo.csv"
            } else {
                "Não foi possível gravar o CSV em $onde."
            }
        }
    }

    /** Botão "Apagar fotos e histórico": remove os arquivos das fotos e zera o banco. */
    fun limparTudo() {
        escopoDisco.launch {
            banco.fotos().forEach { gravador.excluir(it.uri) }
            banco.limparTudo()
            ultimaFoto = null
            atualizarEstadoFotos()
            _detalhe.value = null
            recarregarHistorico()
            _mensagem.value = "Fotos e histórico apagados."
        }
    }

    fun mensagemVista() {
        _mensagem.value = null
    }

    private fun recarregarHistorico() {
        _historico.value = banco.sessoes()
    }

    private fun abrirDetalheAgora(id: Long) {
        val s = banco.sessoes().firstOrNull { it.id == id } ?: return
        val fotos = banco.fotos().filter { it.sessao == id }.map { FotoRegistrada(it.capturaEpochMs, it.nome, it.uri) }
        val usadas = mutableSetOf<Long>()
        val eventos = banco.eventos(id).filter { it.tipo !in EVENTOS_SEM_DETALHE }.takeLast(MAX_EVENTOS_DETALHE).map { e ->
            val daCaptura = fotosDoEvento(e.tipo, e.inicioEpochMs, e.duracaoMs, fotos)
            daCaptura.forEach { usadas += it.capturaEpochMs }
            EventoDetalhe(e, miniatura(daCaptura))
        }
        val soltas = fotos.filter { it.capturaEpochMs !in usadas }.groupBy { it.capturaEpochMs }.values.mapNotNull(::miniatura)
        _detalhe.value = DetalheViagem(s, banco.janelas(id), fotos.size, eventos, soltas)
    }

    /** Miniatura para a lista do histórico. Lida fora da thread de tela e guardada num cache pequeno. */
    suspend fun carregarMiniatura(uri: String): Bitmap? = withContext(Dispatchers.IO) {
        miniaturas.get(uri) ?: gravador.miniatura(uri, LARGURA_MINIATURA, ALTURA_MINIATURA)?.also { miniaturas.put(uri, it) }
    }

    /** Foto em tela cheia, reduzida para não estourar a memória. */
    suspend fun carregarFoto(uri: String): Bitmap? = withContext(Dispatchers.IO) { gravador.abrirReduzida(uri, LADO_FOTO_CHEIA) }

    override fun onCleared() {
        viagemEncerrada = true
        _tela.update { it.copy(capturando = false) }
        captura?.cancel()
        finalizarSessao()
        rede?.liberar()
        // Alarme e voz fecham na thread do domínio, depois de qualquer leitura em andamento (evita alarme órfão tocando).
        CoroutineScope(dominio).launch {
            alarme.close()
            voz.close()
        }
        // O detector fecha na fila de preparo, depois do frame que estiver em andamento.
        CoroutineScope(preparo).launch { detector.close() }
        // O banco fecha no fim da fila de disco, depois do resumo final da viagem.
        escopoDisco.launch { banco.close() }
    }

    private companion object {
        const val TAG = "Helios"
        const val UM_SEGUNDO_MS = 1_000L
        const val SEM_SINAL_MS = 3_000L
        const val BYTES_POR_MB = 1_048_576L
        const val JANELA_COMPLETA_MS = 30_000L
        const val MINIATURAS_EM_CACHE = 40
        const val LARGURA_MINIATURA = 320
        const val ALTURA_MINIATURA = 240
        const val LADO_FOTO_CHEIA = 1_600
        const val MAX_EVENTOS_DETALHE = 300

        /** Piscadas comuns e solavancos enchem a lista e não têm foto: ficam só no CSV. */
        val EVENTOS_SEM_DETALHE = setOf(TipoEvento.PISCADA, TipoEvento.PISCADA_LONGA, TipoEvento.EVENTO_BRUSCO)
    }
}
