package br.unicid.helios.app

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.unicid.helios.app.adaptadores.alarme.AlarmeAndroid
import br.unicid.helios.app.adaptadores.camera.FonteCameraX
import br.unicid.helios.app.adaptadores.esp32.BuzzerEsp32
import br.unicid.helios.app.adaptadores.esp32.FonteEsp32
import br.unicid.helios.app.adaptadores.esp32.RedeHeliosCam
import br.unicid.helios.app.adaptadores.inercial.AcelerometroAndroid
import br.unicid.helios.app.adaptadores.mediapipe.DetectorMediapipe
import br.unicid.helios.app.adaptadores.opencv.RealceOpenCv
import br.unicid.helios.app.portas.Conexao
import br.unicid.helios.app.portas.FonteDeImagens
import br.unicid.helios.app.portas.Imagem
import br.unicid.helios.app.portas.PreProcessador
import br.unicid.helios.dominio.calibracao.Calibrador
import br.unicid.helios.dominio.monitor.Leitura
import br.unicid.helios.dominio.monitor.MonitorMotorista
import br.unicid.helios.dominio.sinais.ContadorEmJanela
import br.unicid.helios.protocolos.EstadoTela
import br.unicid.helios.protocolos.modeloDeTela
import br.unicid.helios.protocolos.padraoPara
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TelaUi(
    val estado: EstadoTela? = null,
    val conexao: Conexao = Conexao.DESCONECTADA,
    val fps: Int = 0,
    val emMovimento: Boolean = false,
    val progressoCalibracao: Double? = null,
    val aceleracaoGpu: Boolean = false,
)

/**
 * Monta o pipeline do TCC:
 *   Captura → (realce OpenCV) → MediaPipe → MonitorMotorista → Alarme e tela
 *                                              ↑ acelerômetro
 *
 * Threads:
 *  - imagens: a fonte entrega; o pré-processamento e o envio ao MediaPipe rodam em [preparo].
 *    `conflate` descarta imagens enquanto a anterior ainda está sendo preparada.
 *  - domínio: TODA chamada ao [monitor] acontece em [dominio] (uma thread só), então o
 *    domínio não precisa de trava.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HeliosViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = RepositorioAjustes(app)
    private val _ajustes = MutableStateFlow(repositorio.ler())
    val ajustes: StateFlow<Ajustes> = _ajustes

    private val _tela = MutableStateFlow(TelaUi())
    val tela: StateFlow<TelaUi> = _tela

    private val dominio = Dispatchers.Default.limitedParallelism(1)
    private val preparo = Dispatchers.Default.limitedParallelism(1)

    private val monitor = MonitorMotorista(calibracao = _ajustes.value.calibracao)
    private val detector = DetectorMediapipe(app)
    private val alarme = AlarmeAndroid(app)
    private val fps = ContadorEmJanela(UM_SEGUNDO_MS)
    private var calibrador: Calibrador? = null
    private var buzzerAtual = 0

    private var rede: RedeHeliosCam? = null
    private var buzzer: BuzzerEsp32? = null
    private var captura: Job? = null

    init {
        _tela.update { it.copy(aceleracaoGpu = detector.delegate.name == "GPU") }
        viewModelScope.launch(dominio) { detector.quadros.collect { processar(monitor.processar(it)) } }
        viewModelScope.launch(dominio) {
            AcelerometroAndroid(app).amostras().collect { (t, m) -> monitor.registrarAceleracao(t, m) }
        }
    }

    /** Chamado depois que a permissão de câmera foi concedida (ou ao trocar a fonte). */
    fun iniciarCaptura() {
        captura?.cancel()
        val a = _ajustes.value
        captura = viewModelScope.launch {
            val fonte = criarFonte(a)
            launch { fonte.conexao.collect { c -> _tela.update { it.copy(conexao = c) } } }
            val realce = if (a.realceOpenCv) RealceOpenCv() else null
            try {
                fonte.imagens().conflate().collect { imagem -> withContext(preparo) { enviar(imagem, realce) } }
            } finally {
                withContext(preparo) { realce?.close() }
            }
        }
    }

    fun calibrar() {
        viewModelScope.launch(dominio) { calibrador = Calibrador(SystemClock.elapsedRealtime()) }
    }

    fun salvarAjustes(novos: Ajustes) {
        val anterior = _ajustes.value
        _ajustes.value = novos
        repositorio.salvar(novos)
        val mudouCaptura = novos.fonte != anterior.fonte || novos.esp32 != anterior.esp32 ||
            novos.realceOpenCv != anterior.realceOpenCv
        if (mudouCaptura) iniciarCaptura()
    }

    private fun criarFonte(a: Ajustes): FonteDeImagens = when (a.fonte) {
        TipoFonte.CAMERA_DO_APARELHO -> FonteCameraX(getApplication<Application>())
        TipoFonte.ESP32_CAM -> {
            rede?.liberar()
            val novaRede = RedeHeliosCam(getApplication<Application>(), a.esp32)
            rede = novaRede
            buzzer = BuzzerEsp32(novaRede, a.esp32, viewModelScope)
            FonteEsp32(novaRede, a.esp32)
        }
    }

    private fun enviar(imagem: Imagem, realce: PreProcessador?) {
        val bitmap = realce?.processar(imagem.bitmap) ?: imagem.bitmap
        detector.enviar(Imagem(bitmap, imagem.timestampMs))
    }

    /** Roda na thread [dominio]. */
    private fun processar(leitura: Leitura) {
        fps.registrar(leitura.timestampMs)
        avancarCalibracao(leitura)
        acionarAlarme(leitura)
        // Fase 5: gravar leitura.eventos no RepositorioSessoes (Room) aqui.
        _tela.update {
            it.copy(
                estado = modeloDeTela(leitura),
                fps = fps.contar(leitura.timestampMs),
                emMovimento = leitura.emMovimento,
                progressoCalibracao = calibrador?.progresso(leitura.timestampMs),
            )
        }
    }

    private fun avancarCalibracao(leitura: Leitura) {
        val c = calibrador ?: return
        leitura.metricas?.let { c.adicionar(it.earBruto, it.pose?.pitchGraus) }
        val resultado = c.resultado(leitura.timestampMs) ?: return
        monitor.calibrar(resultado)
        calibrador = null
        val novos = _ajustes.value.copy(calibracao = resultado)
        _ajustes.value = novos
        repositorio.salvar(novos)
    }

    private fun acionarAlarme(leitura: Leitura) {
        val padrao = padraoPara(leitura.estado)
        if (padrao == null) alarme.parar() else alarme.tocar(padrao)
        val codigo = padrao?.codigoBuzzer ?: 0
        if (_ajustes.value.buzzerNaCamera && codigo != buzzerAtual) buzzer?.tocar(codigo)
        buzzerAtual = codigo
    }

    override fun onCleared() {
        captura?.cancel()
        rede?.liberar()
        alarme.close()
        detector.close()
    }

    private companion object {
        const val UM_SEGUNDO_MS = 1_000L
    }
}
