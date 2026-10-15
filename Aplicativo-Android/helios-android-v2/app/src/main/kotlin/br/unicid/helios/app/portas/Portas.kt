package br.unicid.helios.app.portas

import android.graphics.Bitmap
import br.unicid.helios.dominio.tipos.Quadro
import br.unicid.helios.protocolos.LinhaEvento
import br.unicid.helios.protocolos.LinhaJanela
import br.unicid.helios.protocolos.PadraoSonoro
import kotlinx.coroutines.flow.Flow

/**
 * Portas do lado Android. Cada etapa do pipeline do TCC é uma porta, e cada porta tem
 * um adaptador trocável:
 *
 *  Captura de imagem        → [FonteDeImagens]   (CameraX ou ESP32-CAM)
 *  (pré-processamento)      → [PreProcessador]   (nenhum ou realce OpenCV)
 *  Extração de características → [DetectorFacial] (MediaPipe Face Landmarker)
 *  Análise e classificação  → MonitorMotorista   (módulo :dominio, sem Android)
 *  Alerta                   → [SaidaAlarme], [SaidaVoz] (som, vibração, voz, buzzer da ESP32)
 *  Sensores inerciais       → [FonteInercial]    (acelerômetro linear)
 *  Histórico e evidências   → [RepositorioViagens], [GravadorFotos]
 */

/**
 * Imagem já na orientação correta, com o instante da captura (SystemClock.elapsedRealtime).
 * Quando o bitmap vem de um pool, quem terminar de usá-lo por último chama [liberar].
 */
class Imagem(val bitmap: Bitmap, val timestampMs: Long, val liberar: () -> Unit = {})

enum class Conexao { CONECTANDO, CONECTADA, DESCONECTADA }

interface FonteDeImagens {
    val conexao: Flow<Conexao>

    /** O fluxo já descarta imagens quando o consumidor está ocupado: o app sempre analisa a mais nova. */
    fun imagens(): Flow<Imagem>

    /** Frames jogados fora porque o anterior ainda estava sendo analisado (diagnóstico). */
    val descartados: Long get() = 0
}

fun interface PreProcessador {
    /** Pode devolver o mesmo bitmap (alterado no lugar) se ele for mutável. */
    fun processar(bitmap: Bitmap): Bitmap
}

interface DetectorFacial : AutoCloseable {
    /** Envia uma imagem; o resultado chega depois em [quadros] (modo LIVE_STREAM). */
    fun enviar(imagem: Imagem)
    val quadros: Flow<Quadro>

    /** Quem quer a imagem analisada junto com a malha (visor e foto anotada). Nulo = ninguém. */
    var visual: ConsumidorVisual?
}

/**
 * Recebe a imagem exatamente como o MediaPipe a analisou, com os 478 pontos dela.
 * Chamado na thread do MediaPipe, antes de o bitmap voltar ao pool: copie o que precisar e saia rápido.
 */
interface ConsumidorVisual {
    fun quer(): Boolean

    /** [pontos] = x0, y0, x1, y1... normalizados de 0 a 1; vazio quando não há rosto. */
    fun receber(imagem: Bitmap, pontos: FloatArray, matriz: DoubleArray?)
}

interface SaidaAlarme : AutoCloseable {
    fun tocar(padrao: PadraoSonoro)
    fun parar()

    /** Som curto de "voltou", tocado uma vez quando o motorista sai de um alerta urgente. */
    fun tocarRetorno()
}

interface SaidaVoz : AutoCloseable {
    fun falar(texto: String)
    fun calar()
}

fun interface FonteInercial {
    /** Magnitude da aceleração linear (m/s², sem gravidade) com o instante em ms. */
    fun amostras(): Flow<Pair<Long, Double>>
}

/** Foto de alerta já gravada (na pasta HELIOS ou na escolhida pelo usuário). */
data class FotoSalva(
    val id: Long,
    val sessao: Long,
    val capturaEpochMs: Long,
    val usoNaCapturaMs: Long,
    val nome: String,
    val uri: String,
)

/** Linha da lista de viagens. */
data class SessaoGravada(
    val id: Long,
    val inicioEpochMs: Long,
    val duracaoMs: Long,
    val tempoEmAlertaMs: Long,
    val piscadas: Int,
    val microssonos: Int,
    val bocejos: Int,
    val desatencoes: Int,
    val distracoes: Int,
    val perclosMedio: Double,
    val perclosMaximo: Double,
    val kss: Int?,
)

/** Histórico local (RF08). Só números e nomes de arquivo; nunca a imagem. */
interface RepositorioViagens {
    fun salvarSessao(sessao: SessaoGravada)
    fun gravarEventos(linhas: List<LinhaEvento>)
    fun gravarJanela(linha: LinhaJanela)
    fun gravarFoto(foto: FotoSalva): Long
    fun sessoes(): List<SessaoGravada>
    fun eventos(sessao: Long?): List<LinhaEvento>
    fun janelas(sessao: Long?): List<LinhaJanela>
    fun fotos(): List<FotoSalva>
    fun excluirFotos(ids: Collection<Long>)
    fun definirKss(sessao: Long, kss: Int?)
    fun limparTudo()
}

/**
 * Grava e apaga as fotos e os CSV. [pasta] nula = pasta fixa do app (Pictures/HELIOS e Download/HELIOS);
 * senão, o endereço da pasta escolhida no seletor do Android.
 */
interface GravadorFotos {
    /** Devolve o endereço do arquivo criado, ou nulo se a pasta não estiver acessível. */
    fun salvarJpeg(pasta: String?, nome: String, bitmap: Bitmap): String?
    fun salvarTexto(pasta: String?, nome: String, conteudo: String): Boolean
    fun excluir(uri: String): Boolean
}
