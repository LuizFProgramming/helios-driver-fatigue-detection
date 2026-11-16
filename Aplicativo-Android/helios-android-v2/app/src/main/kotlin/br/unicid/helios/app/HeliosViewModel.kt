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
import br.unicid.helios.app.adaptadores.cnn.ClassificadorOlhoLiteRt
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
import br.unicid.helios.app.portas.ClassificadorOlho
import br.unicid.helios.app.portas.ConsumidorVisual
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.app.portas.FonteDeImagens
import br.unicid.helios.app.portas.Imagem
import br.unicid.helios.app.portas.OlhosCnn
import br.unicid.helios.app.portas.PreProcessador
import br.unicid.helios.app.portas.SessaoGravada
import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.instalacao.AmostraInstalacao
import br.unicid.helios.dominio.instalacao.Assistente
import br.unicid.helios.dominio.instalacao.EstadoAssistente
import br.unicid.helios.dominio.instalacao.LadoMotorista
import br.unicid.helios.dominio.instalacao.PerfilInstalacao
import br.unicid.helios.dominio.instalacao.PosicaoCamera
import br.unicid.helios.dominio.instalacao.RecalibracaoViagem
import br.unicid.helios.dominio.tipos.CaixaRosto
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.protocolos.ArquivoPerfis
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.MonitorMotorista
import br.unicid.helios.dominio.sinais.ContadorEmJanela
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.TipoEvento
import br.unicid.helios.protocolos.ControladorFala
import br.unicid.helios.protocolos.Csv
import br.unicid.helios.protocolos.EstadoTela
import br.unicid.helios.protocolos.FotoRegistrada
import br.unicid.helios.protocolos.LinhaEvento
import br.unicid.helios.protocolos.LinhaJanela
import br.unicid.helios.protocolos.fotoDoEvento
import br.unicid.helios.protocolos.fotosDoEvento
import br.unicid.helios.protocolos.miniatura
import br.unicid.helios.protocolos.linhasDeMetricas
import br.unicid.helios.protocolos.modeloDeTela
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
    /** Tempo desde a primeira leitura da viagem, para a faixa de baixo da tela de direção. */
    val duracaoViagemMs: Long? = null,
    /** Quanto falta da manobra (ADR-014); nulo sem manobra. */
    val manobraRestanteMs: Long? = null,
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

/** O que a tela do assistente de instalação desenha (ADR-013); [caixa] é o rosto no último quadro. */
data class EstadoInstalacao(val assistente: EstadoAssistente, val caixa: CaixaRosto?)

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

    private val dominio = Dispatchers.Default.limitedParallelism(1)
    private val preparo = Dispatchers.Default.limitedParallelism(1)
    private val disco = Dispatchers.IO.limitedParallelism(1)

    /**
     * Escopo próprio da fila de disco: não é cancelado junto com o ViewModel, para o resumo final
     * de uma viagem ser gravado mesmo quando o app fecha, sempre DEPOIS das gravações anteriores.
     */
    private val escopoDisco = CoroutineScope(SupervisorJob() + disco)

    /** Trocado a cada viagem nova, para cronômetros e eventos não passarem de uma viagem para outra. */
    private var monitor = novoMonitor(_ajustes.value.perfil)
    private val cfgInstalacao = ConfiguracaoDeteccao().instalacao

    /** Recalibração curta do início de cada viagem, com o perfil ativo (ADR-013). Só na thread do domínio. */
    private var recalibracao = RecalibracaoViagem(cfgInstalacao)

    /** Assistente de instalação aberto; só na thread do domínio. Aberto, nada vai para alarme ou gravação. */
    private var assistente: Assistente? = null
    private var ultimoQuadroMs = 0L

    @Volatile
    private var instalando = false
    private val _instalacao = MutableStateFlow<EstadoInstalacao?>(null)
    val instalacao: StateFlow<EstadoInstalacao?> = _instalacao
    private val detector = DetectorMediapipe(app)
    private val alarme = AlarmeAndroid(app)
    private val voz = VozAndroid(app)
    private val banco = BancoHelios(app)
    private val gravador = GravadorArquivos(app.contentResolver)
    private val gravacao = GravacaoViagem(
        banco, gravador, repositorio, _ajustes, _mensagem, escopoDisco,
        viagemEncerrada = { viagemEncerrada },
        aoMudarHistorico = ::recarregarHistorico,
    )
    val estadoFotos: StateFlow<EstadoFotos> = gravacao.estadoFotos
    private val miniaturas = LruCache<String, Bitmap>(MINIATURAS_EM_CACHE)
    private val fps = ContadorEmJanela(UM_SEGUNDO_MS)
    private val fala = ControladorFala()
    private var buzzerAtual = 0

    private var rede: RedeHeliosCam? = null
    private var buzzer: BuzzerEsp32? = null
    private var captura: Job? = null

    @Volatile
    private var fonteAtual: FonteDeImagens? = null

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
    private val buffersVisor = BuffersVisor()
    private val desenhistaVisor = DesenhistaMalha()
    private val desenhistaFoto = DesenhistaMalha()
    private var versaoVisor = 0L

    /** Criada na primeira vez que a CNN é ligada e fechada só no fim: a thread do MediaPipe pode estar usando. */
    private var classificadorOlho: ClassificadorOlho? = null

    init {
        _tela.update { it.copy(aceleracaoGpu = detector.delegate.name == "GPU") }
        detector.visual = object : ConsumidorVisual {
            override fun quer() = visorLigado() || gravacao.querAnotacao()

            override fun receber(imagem: Bitmap, pontos: FloatArray, matriz: DoubleArray?, olhos: OlhosCnn?) =
                aoReceberVisual(imagem, pontos, matriz, olhos)
        }
        aplicarCnn(_ajustes.value.cnnOlho)
        viewModelScope.launch(dominio) {
            detector.quadros.collect { q ->
                val leitura = monitor.processar(q)
                if (assistente == null) processar(leitura) else instalar(q, leitura)
            }
        }
        // Sem instalação, a viagem não começa: o app abre direto no assistente (ADR-013).
        if (_ajustes.value.perfil == null) entrarNoAssistente()
        viewModelScope.launch(dominio) {
            AcelerometroAndroid(app).amostras().collect { (t, m) ->
                monitor.registrarAceleracao(t, m)
                // Atualiza o menu também com a câmera parada (viagem pausada ou encerrada).
                val andando = monitor.emMovimento()
                if (_tela.value.emMovimento != andando) _tela.update { it.copy(emMovimento = andando) }
            }
        }
        carregarHistorico()
        escopoDisco.launch { gravacao.atualizarEstadoFotos() }
    }

    // ── Captura ──────────────────────────────────────────────────────────────

    /** Com a permissão de câmera concedida. Não faz nada se já estiver capturando ou com a viagem encerrada. */
    fun iniciarCapturaSePreciso() {
        // O assistente liga a câmera mesmo com a viagem pausada ou encerrada.
        val semViagem = (viagemEncerrada || pausada) && !instalando
        if (captura?.isActive == true || semViagem) return
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
        recomecarMonitorECaptura()
    }

    /** Atualiza os números do painel do menu (chamado enquanto o painel está aberto). */
    fun atualizarViagemAtual() {
        viewModelScope.launch(dominio) { atualizarViagemAtualAgora() }
    }

    private fun atualizarViagemAtualAgora() {
        val atual = gravacao.atual()
        _viagemAtual.value = atual?.let {
            ViagemAtual(it, ultimaLeitura?.metricas?.piscadasNoMinuto ?: 0, pausada)
        }
    }

    /** "Nova viagem": monitor zerado (cronômetros e janelas), câmera de volta. Sem instalação, abre o assistente.
     */
    fun novaViagem() {
        if (_ajustes.value.perfil == null) {
            abrirAssistente()
            return
        }
        viagemEncerrada = false
        pausada = false
        _tela.update { it.copy(pausada = false) }
        recomecarMonitorECaptura()
    }

    /** Monitor zerado na thread do domínio e câmera de volta (usado por "Retomar" e "Nova viagem"). */
    private fun recomecarMonitorECaptura() {
        viewModelScope.launch(dominio) {
            monitor = novoMonitor(_ajustes.value.perfil)
            recalibracao = RecalibracaoViagem(cfgInstalacao)
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

    private fun pararCaptura() {
        captura?.cancel()
        captura = null
        _visor.value = null
        _tela.update { it.copy(capturando = false, estado = null, conexao = Conexao.DESCONECTADA) }
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

    // ── Instalação da câmera (ADR-013) ──

    /** Ajustes › Refazer instalação, ou Nova viagem sem perfil: abre o assistente e liga a câmera. */
    fun abrirAssistente() {
        entrarNoAssistente()
        iniciarCapturaSePreciso()
    }

    /** Sem ligar a câmera: na abertura do app ela só liga depois da permissão. */
    private fun entrarNoAssistente() {
        instalando = true
        detector.enquadramento = true
        viewModelScope.launch(dominio) {
            silenciarAlertas()
            val a = Assistente(cfgInstalacao)
            assistente = a
            _instalacao.value = EstadoInstalacao(a.estado(ultimoQuadroMs), null)
        }
    }

    fun escolherPosicao(posicao: PosicaoCamera, lado: LadoMotorista) =
        noAssistente { it.escolherPosicao(posicao, lado) }

    fun comecarCalibracao() = noAssistente { it.comecarCalibracao(ultimoQuadroMs) }

    fun gravarZona() = noAssistente { it.gravarZona(ultimoQuadroMs) }

    fun pularZona() = noAssistente(Assistente::pularZona)

    fun repetirInstalacao() = noAssistente(Assistente::repetir)

    private fun noAssistente(acao: (Assistente) -> Unit) {
        viewModelScope.launch(dominio) {
            val a = assistente ?: return@launch
            acao(a)
            _instalacao.update { it?.copy(assistente = a.estado(ultimoQuadroMs)) }
        }
    }

    /** "Usar esta instalação": grava o perfil, deixa ativo e recomeça o monitor com a calibração dele. */
    fun usarInstalacao(nome: String) {
        viewModelScope.launch(dominio) {
            val id = java.util.UUID.randomUUID().toString()
            val perfil = assistente?.perfil(id, nome.trim().ifEmpty { "Carro" }, System.currentTimeMillis())
                ?: return@launch
            alterarAjustes { it.copy(perfis = it.perfis + perfil, perfilAtivo = perfil.id) }
            monitor = novoMonitor(perfil)
            recalibracao = RecalibracaoViagem(cfgInstalacao)
            sairDoAssistente()
        }
    }

    /** Fecha o assistente sem gravar; só existe com um perfil para voltar. */
    fun cancelarInstalacao() {
        if (_ajustes.value.perfil == null) return
        viewModelScope.launch(dominio) { sairDoAssistente() }
    }

    /** Roda em [dominio]. Com a viagem pausada ou encerrada, a câmera que o assistente ligou volta a desligar. */
    private suspend fun sairDoAssistente() {
        assistente = null
        instalando = false
        detector.enquadramento = false
        _instalacao.value = null
        if (viagemEncerrada || pausada) withContext(Dispatchers.Main) { pararCaptura() }
    }

    /** Roda em [dominio]. Com o assistente aberto o quadro não vai para alarme, tela de direção nem gravação. */
    private fun instalar(q: Quadro, leitura: Leitura) {
        val a = assistente ?: return
        ultimoQuadroMs = leitura.timestampMs
        ultimaLeituraMs = SystemClock.elapsedRealtime()
        ultimaLeitura = leitura
        val amostra = AmostraInstalacao(
            q.amostra?.caixaRosto, q.amostra?.brilhoRosto, leitura.metricas?.pose, monitor.rmsAceleracao(),
        )
        _instalacao.value = EstadoInstalacao(a.processar(leitura.timestampMs, amostra, leitura.metricas), amostra.caixa)
    }

    fun escolherPerfil(id: String) {
        alterarAjustes { it.copy(perfilAtivo = id) }
        val perfil = _ajustes.value.perfil ?: return
        viewModelScope.launch(dominio) {
            usarPerfil(perfil)
            recalibracao = RecalibracaoViagem(cfgInstalacao)
        }
    }

    /** Roda em [dominio]: a referência da estrada e as zonas do perfil passam a valer no monitor. */
    private fun usarPerfil(perfil: PerfilInstalacao) {
        monitor.calibrar(perfil.calibracao)
        monitor.definirZonas(perfil.zonas)
    }

    private fun novoMonitor(perfil: PerfilInstalacao?) =
        MonitorMotorista(calibracao = perfil?.calibracao, zonas = perfil?.zonas.orEmpty())

    /** Botão "Manobra" (ADR-014): 30 s sem alerta de distração; o de sono continua. */
    fun iniciarManobra() {
        viewModelScope.launch(dominio) { ultimaLeitura?.let { monitor.iniciarManobra(it.timestampMs) } }
    }

    fun renomearPerfil(id: String, nome: String) {
        val limpo = nome.trim().takeIf { it.isNotEmpty() } ?: return
        alterarAjustes { a -> a.copy(perfis = a.perfis.map { if (it.id == id) it.copy(nome = limpo) else it }) }
    }

    /** Apagar o perfil ativo passa para o primeiro que sobrar; sem nenhum, abre o assistente. */
    fun apagarPerfil(id: String) {
        alterarAjustes { a ->
            val resto = a.perfis.filterNot { it.id == id }
            a.copy(perfis = resto, perfilAtivo = if (a.perfilAtivo == id) resto.firstOrNull()?.id else a.perfilAtivo)
        }
        val perfil = _ajustes.value.perfil
        if (perfil == null) {
            abrirAssistente()
        } else {
            viewModelScope.launch(dominio) { usarPerfil(perfil) }
        }
    }

    /** Backup dos perfis: o app não usa o backup do Android (allowBackup="false"), então o arquivo é a cópia. */
    fun exportarPerfis(pasta: String?) {
        val perfis = _ajustes.value.perfis
        escopoDisco.launch {
            val agora = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now())
            val nome = "helios-perfis-$agora.txt"
            val ok = gravador.salvarTexto(pasta, nome, ArquivoPerfis.escrever(perfis), "text/plain")
            val onde = if (pasta == null) GravadorArquivos.PASTA_CSV else "pasta escolhida"
            _mensagem.value = if (ok) "${perfis.size} perfis salvos em $onde: $nome" else "Falha ao gravar em $onde."
        }
    }

    /** O arquivo vem de fora do app: só as primeiras linhas são lidas e o [ArquivoPerfis] confere cada perfil. */
    fun importarPerfis(uri: String) {
        escopoDisco.launch {
            val texto = runCatching {
                val resolvedor = getApplication<Application>().contentResolver
                resolvedor.openInputStream(android.net.Uri.parse(uri))?.bufferedReader()?.use { r ->
                    generateSequence { r.readLine() }.take(MAX_LINHAS_BACKUP).joinToString("\n")
                }
            }.getOrNull().orEmpty()
            val lidos = ArquivoPerfis.ler(texto)
            alterarAjustes { a ->
                val ids = lidos.map { it.id }.toSet()
                a.copy(
                    perfis = a.perfis.filterNot { it.id in ids } + lidos,
                    perfilAtivo = a.perfilAtivo ?: lidos.firstOrNull()?.id,
                )
            }
            _mensagem.value = if (lidos.isEmpty()) "Nenhum perfil válido no arquivo." else "${lidos.size} importados."
        }
    }

    /** Ajustes das fotos valem na hora, sem esperar o "Salvar": a autorização não pode se perder ao sair da tela. */
    fun salvarFotos(fotos: AjustesFotos) {
        alterarAjustes { it.copy(fotos = fotos) }
        escopoDisco.launch { gravacao.atualizarEstadoFotos() }
    }

    /**
     * Resposta "Permitir" da pergunta da abertura: liga as fotos na pasta fixa HELIOS e grava uma foto de teste,
     * que cria a pasta e prova que a gravação funciona.
     */
    fun autorizarFotos() {
        alterarAjustes { it.copy(perguntouFotos = true, fotos = it.fotos.copy(consentimento = true, pasta = null)) }
        escopoDisco.launch { gravacao.atualizarEstadoFotos() }
        fotoDeTeste()
    }

    /** Resposta "Agora não": não pergunta de novo; dá para ligar depois em Ajustes. */
    fun recusarFotos() {
        alterarAjustes { it.copy(perguntouFotos = true) }
    }

    /** Grava a próxima imagem da câmera como foto de teste (crua e anotada), em qualquer estado do motorista. */
    fun fotoDeTeste() {
        when {
            !_ajustes.value.fotos.consentimento ->
                _mensagem.value = "Ligue \"Autorizo guardar fotos dos alertas\" em Ajustes para gravar a foto de teste."
            !_tela.value.capturando ->
                _mensagem.value = "A câmera precisa estar ligada para a foto de teste: Menu › Nova viagem ou Retomar."
            else -> {
                gravacao.pedirFotoDeTeste()
                _mensagem.value = "Gravando a foto de teste…"
            }
        }
    }

    private fun alterarAjustes(mudanca: (Ajustes) -> Ajustes) {
        _ajustes.update(mudanca)
        repositorio.salvar(_ajustes.value)
    }

    /** Os perfis mudam na hora pelos botões próprios; o "Salvar" da tela de ajustes não mexe neles. */
    fun salvarAjustes(formulario: Ajustes) {
        val anterior = _ajustes.value
        val novos = formulario.copy(perfis = anterior.perfis, perfilAtivo = anterior.perfilAtivo)
        _ajustes.value = novos
        repositorio.salvar(novos)
        val mudouCaptura = novos.fonte != anterior.fonte || novos.esp32 != anterior.esp32 ||
            novos.realceOpenCv != anterior.realceOpenCv || novos.reaproveitarBitmaps != anterior.reaproveitarBitmaps
        if (mudouCaptura && captura != null) iniciarCaptura()
        aplicarCnn(novos.cnnOlho)
    }

    /** Modo sombra: a CNN só registra e aparece no visor; o MonitorMotorista não usa o valor para decidir. */
    private fun aplicarCnn(ligada: Boolean) {
        detector.classificadorOlho = if (ligada) {
            classificadorOlho ?: ClassificadorOlhoLiteRt(getApplication()).also { classificadorOlho = it }
        } else {
            null
        }
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
            gravacao.aoReceberQuadroCru(imagem.bitmap)
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
    private fun aoReceberVisual(imagem: Bitmap, pontos: FloatArray, matriz: DoubleArray?, olhos: OlhosCnn?) {
        val leitura = ultimaLeitura
        val m = leitura?.metricas
        val malha = Malha(pontos, matriz, m?.olhoDireitoFechado, m?.olhoEsquerdoFechado, m?.olhar?.fora == true, olhos)
        if (visorLigado()) {
            val quadro = buffersVisor.obter(imagem.width, imagem.height)
            Canvas(quadro).drawBitmap(imagem, 0f, 0f, null)
            desenhistaVisor.desenhar(quadro, malha)
            _visor.value = QuadroVisor(quadro, ++versaoVisor)
        }
        gravacao.gravarAnotada {
            imagem.copy(Bitmap.Config.ARGB_8888, true)
                ?.also { desenhistaFoto.desenhar(it, malha, leitura?.let(::linhasDeMetricas).orEmpty()) }
        }
    }

    private fun visorLigado() = (_ajustes.value.mostrarVisor || instalando) && _tela.value.capturando

    /** Roda na thread [dominio]. Resultados que chegam depois de encerrar a viagem são ignorados. */
    private fun processar(leitura: Leitura) {
        if (!_tela.value.capturando) return
        val agora = SystemClock.elapsedRealtime() // local: o vigia de sinal pode zerar ultimaLeituraMs
        ultimaLeituraMs = agora
        ultimaLeitura = leitura
        fps.registrar(leitura.timestampMs)
        if (!recalibracao.concluida) {
            recalibracao.processar(leitura.timestampMs, leitura.metricas, leitura.emMovimento)?.let(monitor::calibrar)
        }
        acionarAlarme(leitura)
        // Latência da imagem até aqui. ESP32-CAM: conta a partir da chegada do JPEG, sem a captura e o Wi-Fi.
        gravacao.registrar(leitura, latenciaMs = agora - leitura.timestampMs)
        val a = _ajustes.value
        _tela.update {
            if (!it.capturando) return@update it // a viagem foi encerrada enquanto esta leitura era processada
            it.copy(
                estado = modeloDeTela(leitura),
                fps = fps.contar(leitura.timestampMs),
                emMovimento = leitura.emMovimento,
                progressoCalibracao = recalibracao.progresso(leitura.timestampMs),
                diagnostico = if (a.mostrarDiagnostico) diagnostico(a) else null,
                metricas = if (a.mostrarVisor) linhasDeMetricas(leitura) else emptyList(),
                duracaoViagemMs = gravacao.duracaoMs(),
                manobraRestanteMs = leitura.manobraRestanteMs,
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

    /** Fecha a sessão aberta (ver [GravacaoViagem.finalizar]) e apaga o visor. Devolve o id. */
    private fun finalizarSessao(): Long? {
        _visor.value = null
        return gravacao.finalizar()
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
            gravacao.apagarTudo()
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
        CoroutineScope(preparo).launch {
            detector.close()
            classificadorOlho?.close()
        }
        // O banco fecha no fim da fila de disco, depois do resumo final da viagem.
        escopoDisco.launch { banco.close() }
    }

    private companion object {
        /** Limite do arquivo de backup dos perfis: ele vem de fora do app. */
        const val MAX_LINHAS_BACKUP = 5_000
        const val TAG = "Helios"
        const val UM_SEGUNDO_MS = 1_000L
        const val SEM_SINAL_MS = 3_000L
        const val BYTES_POR_MB = 1_048_576L
        const val MINIATURAS_EM_CACHE = 40
        const val LARGURA_MINIATURA = 320
        const val ALTURA_MINIATURA = 240
        const val LADO_FOTO_CHEIA = 1_600
        const val MAX_EVENTOS_DETALHE = 300

        /** Piscadas comuns e solavancos enchem a lista e não têm foto: ficam só no CSV. */
        val EVENTOS_SEM_DETALHE = setOf(TipoEvento.PISCADA, TipoEvento.PISCADA_LONGA, TipoEvento.EVENTO_BRUSCO)
    }
}
