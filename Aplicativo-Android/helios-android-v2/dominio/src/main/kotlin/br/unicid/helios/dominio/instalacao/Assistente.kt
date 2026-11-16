package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.calibracao.Calibrador
import br.unicid.helios.dominio.calibracao.Desvios
import br.unicid.helios.dominio.monitor.Metricas

/** Instalação gravada: onde a câmera está, a referência da estrada e as zonas do carro (ADR-013). */
data class PerfilInstalacao(
    val id: String,
    val nome: String,
    val posicao: PosicaoCamera,
    val lado: LadoMotorista,
    val criadoEpochMs: Long,
    val calibracao: Calibracao,
    val desvios: Desvios,
    val zonas: List<ZonaOlhar>,
    val fracaoVerificacoesOk: Double,
    val nota: Int,
)

enum class Etapa { POSICAO, ENQUADRAMENTO, CALIBRACAO, ZONAS, RESUMO }

/** O que a tela do assistente desenha. [progresso]: da calibração ou da zona sendo gravada. */
data class EstadoAssistente(
    val etapa: Etapa,
    val verificacoes: List<ResultadoVerificacao>,
    val podeCalibrar: Boolean,
    val calibracaoFalhou: Boolean,
    val progresso: Double?,
    val zona: Zona?,
    val coletandoZona: Boolean,
    val zonaFalhou: Boolean,
    val nota: NotaInstalacao?,
)

/**
 * Assistente de instalação (ADR-013), só com o carro parado: posição → enquadramento ao vivo → calibração
 * de 10 s olhando a estrada → zonas do carro (cada uma pode ser pulada) → resumo com a
 * nota. Instalação com nota para
 * refazer não vira perfil (decisão do grupo: bloquear). Não é thread-safe: chamado sempre pela thread do domínio.
 */
class Assistente(private val cfg: ConfiguracaoInstalacao) {
    private var etapa = Etapa.POSICAO
    private var posicao = PosicaoCamera.COLUNA_VOLANTE
    private var lado = LadoMotorista.ESQUERDO
    private var verificacoes = emptyList<ResultadoVerificacao>()
    private var okDesdeMs: Long? = null
    // Valores iniciais só para não serem nulos: a etapa garante que só são lidos depois de preenchidos.
    private var calibrador = Calibrador(0, cfg.calibracaoMs, cfg.minimoAmostras)
    private var calibracao = Calibracao(0.0, 0.0)
    private var calibracaoFalhou = false
    private var quadrosCalibracao = 0
    private var quadrosOk = 0
    private var desvios = Desvios(0.0, 0.0, null, null)
    private var indiceZona = 0
    private var coletor: ColetorZona? = null
    private var zonaFalhou = false
    private val zonas = mutableListOf<ZonaOlhar>()

    fun escolherPosicao(posicao: PosicaoCamera, lado: LadoMotorista) {
        this.posicao = posicao
        this.lado = lado
        etapa = Etapa.ENQUADRAMENTO
        okDesdeMs = null
    }

    fun processar(t: Long, amostra: AmostraInstalacao, metricas: Metricas?): EstadoAssistente {
        when (etapa) {
            Etapa.ENQUADRAMENTO -> enquadrar(t, amostra)
            Etapa.CALIBRACAO -> calibrar(t, enquadrar(t, amostra), metricas)
            Etapa.ZONAS -> coletor?.let { gravarAmostraDeZona(it, t, metricas) }
            Etapa.POSICAO, Etapa.RESUMO -> Unit
        }
        return estado(t)
    }

    /** Começa a calibração se o enquadramento está certo há 1 s; senão não faz nada e devolve falso. */
    fun comecarCalibracao(t: Long): Boolean {
        val pode = podeCalibrar(t)
        if (pode) {
            etapa = Etapa.CALIBRACAO
            calibrador = Calibrador(t, cfg.calibracaoMs, cfg.minimoAmostras)
            calibracaoFalhou = false
            quadrosCalibracao = 0
            quadrosOk = 0
        }
        return pode
    }

    fun gravarZona(t: Long) {
        if (etapa != Etapa.ZONAS) return
        coletor = ColetorZona(Zona.entries[indiceZona], t, calibracao, cfg)
        zonaFalhou = false
    }

    fun pularZona() {
        if (etapa != Etapa.ZONAS) return
        coletor = null
        zonaFalhou = false
        proximaZona()
    }

    /** Recomeça do zero, pela posição da câmera. */
    fun repetir() {
        etapa = Etapa.POSICAO
        okDesdeMs = null
        coletor = null
        zonas.clear()
        indiceZona = 0
    }

    /** O perfil desta instalação; nulo antes do resumo ou com nota para refazer. */
    fun perfil(id: String, nome: String, criadoEpochMs: Long): PerfilInstalacao? {
        val nota = nota()?.takeIf { it.qualidade != Qualidade.REFAZER } ?: return null
        return PerfilInstalacao(
            id, nome, posicao, lado, criadoEpochMs, calibracao, desvios, zonas.toList(), fracaoOk(), nota.pontos,
        )
    }

    /** Verifica o quadro e devolve se as cinco verificações passaram. */
    private fun enquadrar(t: Long, amostra: AmostraInstalacao): Boolean {
        verificacoes = verificar(amostra, posicao, cfg)
        val tudoOk = verificacoes.filterNot { it.ok }.isEmpty()
        okDesdeMs = if (tudoOk) okDesdeMs ?: t else null
        return tudoOk
    }

    private fun calibrar(t: Long, tudoOk: Boolean, metricas: Metricas?) {
        quadrosCalibracao++
        if (tudoOk) quadrosOk++
        metricas?.let { m ->
            calibrador.adicionar(
                m.earBruto, m.pose?.pitchGraus, m.pose?.yawGraus, m.earDireito, m.earEsquerdo,
                m.olhar.horizontal, m.olhar.vertical,
            )
        }
        if (calibrador.progresso(t) >= 1.0) terminarCalibracao(t)
    }

    private fun terminarCalibracao(t: Long) {
        val resultado = calibrador.resultado(t)
        calibracaoFalhou = resultado == null
        if (resultado == null) {
            etapa = Etapa.ENQUADRAMENTO
            okDesdeMs = null
        } else {
            calibracao = resultado
            desvios = calibrador.desvios()
            etapa = Etapa.ZONAS
        }
    }

    private fun gravarAmostraDeZona(c: ColetorZona, t: Long, metricas: Metricas?) {
        val olhar = metricas?.olhar
        c.adicionar(t, metricas?.pose, olhar?.horizontal, olhar?.vertical)
        if (c.progresso(t) < 1.0) return
        coletor = null
        val zona = c.resultado(t)
        zonaFalhou = zona == null
        if (zona != null) {
            zonas += zona
            proximaZona()
        }
    }

    private fun proximaZona() {
        indiceZona++
        if (indiceZona == Zona.entries.size) etapa = Etapa.RESUMO
    }

    private fun podeCalibrar(t: Long): Boolean =
        etapa == Etapa.ENQUADRAMENTO && okDesdeMs?.let { t - it >= cfg.enquadramentoOkMs } == true

    // Só chega aqui no resumo, depois de uma calibração com quadros: não há divisão por zero.
    private fun fracaoOk(): Double = quadrosOk.toDouble() / quadrosCalibracao

    private fun nota(): NotaInstalacao? =
        if (etapa == Etapa.RESUMO) notaInstalacao(fracaoOk(), desvios, zonas.size, cfg) else null

    /** O estado para a tela no instante [t]; o app chama depois de cada toque, sem esperar o próximo quadro. */
    fun estado(t: Long) = EstadoAssistente(
        etapa = etapa,
        verificacoes = verificacoes,
        podeCalibrar = podeCalibrar(t),
        calibracaoFalhou = calibracaoFalhou,
        progresso = when (etapa) {
            Etapa.CALIBRACAO -> calibrador.progresso(t)
            Etapa.ZONAS -> coletor?.progresso(t)
            Etapa.POSICAO, Etapa.ENQUADRAMENTO, Etapa.RESUMO -> null
        },
        zona = if (etapa == Etapa.ZONAS) Zona.entries[indiceZona] else null,
        coletandoZona = coletor != null,
        zonaFalhou = zonaFalhou,
        nota = nota(),
    )
}
