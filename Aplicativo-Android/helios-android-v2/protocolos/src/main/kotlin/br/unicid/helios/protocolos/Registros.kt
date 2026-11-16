package br.unicid.helios.protocolos

import br.unicid.helios.dominio.instalacao.PerfilInstalacao
import br.unicid.helios.dominio.instalacao.Zona
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.TipoEvento
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Uma linha do `eventos.csv`. Horários em milissegundos desde 1970 (relógio de parede). */
data class LinhaEvento(
    val sessao: Long,
    val inicioEpochMs: Long,
    val duracaoMs: Long,
    val tipo: TipoEvento,
    /** Estado da leitura em que o evento terminou (coluna `estado_fim`). */
    val estado: EstadoMotorista,
    val perclosCurto: Double,
    val perclosLongo: Double,
    val foto: String?,
    /** Estado da leitura em que o evento começou. Nulo nos eventos gravados antes da versão 0.4.3. */
    val estadoInicio: EstadoMotorista? = null,
    /** Giro e rosto ausente: o maior giro da cabeça no evento (ADR-014). Nulo nos outros e nos antigos. */
    val yawMaximoGraus: Double? = null,
)

/** Uma linha do `janelas.csv` (uma por janela de 30 s). [kss] é a nota de sonolência de Karolinska, 1 a 9. */
data class LinhaJanela(
    val sessao: Long,
    val inicioEpochMs: Long,
    val perclosLongo: Double,
    val piscadasPorMinuto: Double,
    val duracaoMediaPiscadaMs: Double,
    val bocejosPorMinuto: Double,
    val desvioPitchGraus: Double,
    val kss: Int?,
    val fracaoOlharFora: Double = 0.0,
    val fracaoSemicerrado: Double = 0.0,
    /** Tempo em cada estado na janela; estado ausente vale zero. Janelas gravadas antes da 0.4.3 vêm vazias. */
    val tempoPorEstadoMs: Map<EstadoMotorista, Long> = emptyMap(),
    val fpsMedio: Double = 0.0,
    val latenciaMediaMs: Double = 0.0,
    val latenciaP95Ms: Long = 0,
    /** CNN do olho em modo sombra; nulos com ela desligada (colunas vazias no CSV). */
    val cnnFechadoMedia: Double? = null,
    val cnnDiscordancia: Double? = null,
    /** Parte D: tempo com a olhada em espelhos, painel e outra direção, em manobra; perfil e sensibilidade em uso. */
    val zonaEspelhoMs: Long = 0,
    val zonaPainelMs: Long = 0,
    val zonaOutraMs: Long = 0,
    val manobraMs: Long = 0,
    val perfil: String? = null,
    val sensibilidade: String? = null,
)

/**
 * Exportação CSV combinada com a equipe: vírgula como separador e ponto decimal, para ler direto
 * no Python (`pandas.read_csv`). Horário em ISO 8601 com fuso, legível e sem ambiguidade.
 */
object Csv {
    /** Coluna de tempo de cada estado no `janelas.csv`, na ordem do arquivo. Os mesmos nomes são colunas do banco. */
    val COLUNAS_TEMPO = mapOf(
        EstadoMotorista.ATENTO to "tempo_atento_ms",
        EstadoMotorista.ATENCAO to "tempo_atencao_ms",
        EstadoMotorista.FADIGA to "tempo_fadiga_ms",
        EstadoMotorista.CRITICO to "tempo_critico_ms",
        EstadoMotorista.DISTRAIDO to "tempo_distraido_ms",
        EstadoMotorista.DESATENTO to "tempo_desatento_ms",
        EstadoMotorista.SEM_ROSTO to "tempo_sem_rosto_ms",
    )

    const val CABECALHO_EVENTOS =
        "sessao,inicio,duracao_ms,tipo,estado_inicio,estado_fim,perclos_5s,perclos_60s,foto,yaw_max_graus"
    const val CABECALHO_JANELAS =
        "sessao,inicio,perclos_60s,piscadas_min,duracao_media_piscada_ms,bocejos_min,desvio_pitch," +
            "olhar_fora,semicerrado,kss," +
            "tempo_atento_ms,tempo_atencao_ms,tempo_fadiga_ms,tempo_critico_ms,tempo_distraido_ms," +
            "tempo_desatento_ms,tempo_sem_rosto_ms,fps_medio,latencia_media_ms,latencia_p95_ms," +
            "cnn_fechado_media,cnn_discordancia," +
            "zona_espelho_ms,zona_painel_ms,zona_outra_ms,manobra_ms,perfil,sensibilidade"

    private val FORMATO_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private const val CASAS_PERCLOS = 4
    private const val CASAS_TAXA = 2
    private const val CASAS_MS = 1

    fun evento(l: LinhaEvento, zona: ZoneId): String = listOf(
        l.sessao.toString(),
        hora(l.inicioEpochMs, zona),
        l.duracaoMs.toString(),
        l.tipo.name,
        l.estadoInicio?.name.orEmpty(),
        l.estado.name,
        decimal(l.perclosCurto, CASAS_PERCLOS),
        decimal(l.perclosLongo, CASAS_PERCLOS),
        campo(l.foto.orEmpty()),
        l.yawMaximoGraus?.let { decimal(it, CASAS_GRAUS) }.orEmpty(),
    ).joinToString(",")

    fun janela(l: LinhaJanela, zona: ZoneId): String = listOf(
        l.sessao.toString(),
        hora(l.inicioEpochMs, zona),
        decimal(l.perclosLongo, CASAS_PERCLOS),
        decimal(l.piscadasPorMinuto, CASAS_TAXA),
        decimal(l.duracaoMediaPiscadaMs, CASAS_MS),
        decimal(l.bocejosPorMinuto, CASAS_TAXA),
        decimal(l.desvioPitchGraus, CASAS_TAXA),
        decimal(l.fracaoOlharFora, CASAS_PERCLOS),
        decimal(l.fracaoSemicerrado, CASAS_PERCLOS),
        l.kss?.toString().orEmpty(),
    ).plus(COLUNAS_TEMPO.keys.map { (l.tempoPorEstadoMs[it] ?: 0L).toString() })
        .plus(listOf(decimal(l.fpsMedio, CASAS_TAXA), decimal(l.latenciaMediaMs, CASAS_MS), l.latenciaP95Ms.toString()))
        .plus(listOf(l.cnnFechadoMedia, l.cnnDiscordancia).map { v -> v?.let { decimal(it, CASAS_PERCLOS) }.orEmpty() })
        .plus(listOf(l.zonaEspelhoMs, l.zonaPainelMs, l.zonaOutraMs, l.manobraMs).map(Long::toString))
        .plus(listOf(campo(l.perfil.orEmpty()), l.sensibilidade.orEmpty()))
        .joinToString(",")

    fun eventos(linhas: List<LinhaEvento>, zona: ZoneId): String =
        arquivo(CABECALHO_EVENTOS, linhas.map { evento(it, zona) })

    /** `instalacao.csv` (Parte D): uma linha por perfil, com a calibração, os desvios e as quatro zonas. */
    fun instalacao(perfis: List<PerfilInstalacao>, zona: ZoneId): String =
        arquivo(CABECALHO_INSTALACAO, perfis.map { p -> perfil(p, zona) })

    private fun perfil(p: PerfilInstalacao, zona: ZoneId): String {
        val c = p.calibracao
        val numeros = listOf(
            c.earReferencia, c.pitchReferencia, c.yawReferencia, c.earDireito, c.earEsquerdo,
            c.olharHorizontal, c.olharVertical, p.desvios.pitch, p.desvios.yaw, p.desvios.olharHorizontal,
            p.desvios.olharVertical,
        ).map { it?.toString().orEmpty() }
        val zonas = Zona.entries.flatMap { z ->
            val zo = p.zonas.firstOrNull { it.zona == z }
            listOf(
                zo?.yaw, zo?.pitch, zo?.olharHorizontal, zo?.olharVertical,
                zo?.espalhamentoYaw, zo?.espalhamentoPitch, zo?.espalhamentoOlhar,
            ).map { it?.toString().orEmpty() }
        }
        return (
            listOf(
                campo(p.id), campo(p.nome), p.posicao.name, p.lado.name, hora(p.criadoEpochMs, zona),
                p.nota.toString(), p.fracaoVerificacoesOk.toString(),
            ) + numeros + zonas
            ).joinToString(",")
    }

    fun janelas(linhas: List<LinhaJanela>, zona: ZoneId): String =
        arquivo(CABECALHO_JANELAS, linhas.map { janela(it, zona) })

    /** Regra do RFC 4180: aspas em volta se houver vírgula, aspas ou quebra de linha. */
    fun campo(valor: String): String =
        if (valor.any { it == ',' || it == '"' || it == '\n' }) "\"" + valor.replace("\"", "\"\"") + "\"" else valor

    private fun arquivo(cabecalho: String, linhas: List<String>) =
        (listOf(cabecalho) + linhas).joinToString("\n", postfix = "\n")

    internal fun hora(epochMs: Long, zona: ZoneId) = FORMATO_HORA.format(Instant.ofEpochMilli(epochMs).atZone(zona))

    internal fun decimal(valor: Double, casas: Int) = "%.${casas}f".format(Locale.ROOT, valor)
}

// ── Fotos dos alertas ────────────────────────────────────────────────────────

private val FORMATO_NOME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

/**
 * Nome do arquivo, no estilo do protótipo Python: estado e horário local. Ex.: helios-critico-20261014-231005.jpg.
 * A versão [anotada] (malha e métricas desenhadas) ganha o sufixo "-analise". A foto de [teste] (pedida pelo
 * usuário para conferir a pasta) leva "teste" no lugar do estado.
 */
fun nomeFoto(estado: EstadoMotorista, epochMs: Long, zona: ZoneId, anotada: Boolean = false, teste: Boolean = false): String {
    val horario = FORMATO_NOME.format(Instant.ofEpochMilli(epochMs).atZone(zona))
    val sufixo = if (anotada) "-analise" else ""
    val rotulo = if (teste) "teste" else estado.name.lowercase(Locale.ROOT)
    return "helios-$rotulo-$horario$sufixo.jpg"
}

/** Foto já gravada. [uri] é o endereço do arquivo, para abrir a miniatura no histórico. */
data class FotoRegistrada(val capturaEpochMs: Long, val nome: String, val uri: String = "")

private const val FOLGA_FOTO_MS = 2_000L
private const val CASAS_GRAUS = 1

private val COLUNAS_PERFIL = listOf(
    "id", "nome", "posicao", "lado", "criado", "nota", "verificacoes_ok", "ear", "pitch", "yaw", "ear_dir", "ear_esq",
    "olhar_h", "olhar_v", "desvio_pitch", "desvio_yaw", "desvio_olhar_h", "desvio_olhar_v",
)
private val COLUNAS_ZONA = listOf("yaw", "pitch", "olhar_h", "olhar_v", "esp_yaw", "esp_pitch", "esp_olhar")
private val CABECALHO_INSTALACAO =
    (COLUNAS_PERFIL + Zona.entries.flatMap { z -> COLUNAS_ZONA.map { "${z.name.lowercase()}_$it" } }).joinToString(",")

/** Eventos que levam a um alerta. Piscadas e solavancos nunca recebem foto. */
private val EVENTOS_COM_FOTO = setOf(
    TipoEvento.MICROSSONO,
    TipoEvento.SONO,
    TipoEvento.CABECA_CAIDA,
    TipoEvento.BOCEJO,
    TipoEvento.ROSTO_AUSENTE,
    TipoEvento.CABECA_VIRADA,
    TipoEvento.OLHAR_DESVIADO,
)

/**
 * A foto é tirada ao entrar no alerta, que acontece no meio do evento (o microssono começa
 * 1 s antes do CRÍTICO; a ausência do rosto, 2 s antes do DESATENTO). Vale a primeira foto entre
 * o início do evento e o fim dele mais uma folga, e só para eventos que levam a alerta.
 */
fun fotoDoEvento(tipo: TipoEvento, inicioEpochMs: Long, duracaoMs: Long, fotos: List<FotoRegistrada>): String? =
    fotosDoEvento(tipo, inicioEpochMs, duracaoMs, fotos).firstOrNull()?.nome

/**
 * Todas as fotos da mesma captura do evento: a crua e a "-analise" saem juntas, com o mesmo horário.
 * O histórico mostra a anotada como miniatura; o CSV cita a primeira.
 */
fun fotosDoEvento(tipo: TipoEvento, inicioEpochMs: Long, duracaoMs: Long, fotos: List<FotoRegistrada>): List<FotoRegistrada> {
    if (tipo !in EVENTOS_COM_FOTO) return emptyList()
    val janela = inicioEpochMs..(inicioEpochMs + duracaoMs + FOLGA_FOTO_MS)
    val primeira = fotos.firstOrNull { it.capturaEpochMs in janela } ?: return emptyList()
    return fotos.filter { it.capturaEpochMs == primeira.capturaEpochMs }
}

/** Entre as fotos de uma captura, a que tem a análise desenhada; senão a crua. */
fun miniatura(fotos: List<FotoRegistrada>): FotoRegistrada? =
    fotos.firstOrNull { it.nome.endsWith(SUFIXO_ANALISE) } ?: fotos.firstOrNull()

private const val SUFIXO_ANALISE = "-analise.jpg"

/** Quanto tempo de USO do app uma foto pode ficar guardada. Nulo = não apaga por idade. */
private const val UMA_HORA_MS = 3_600_000L

enum class Retencao(val usoMaximoMs: Long?) {
    DUAS_HORAS(UMA_HORA_MS * 2),
    TRES_HORAS(UMA_HORA_MS * 3),
    AO_ENCERRAR(null),
    SO_LIMITE(null),
}

data class FotoGuardada(val id: Long, val usoNaCapturaMs: Long)

/**
 * Limite físico das fotos: no máximo [maxFotos] guardadas e, conforme [retencao], apagadas depois
 * de um tempo de uso do app (não de relógio: o celular parado na gaveta não conta).
 */
class PoliticaRetencao(private val maxFotos: Int, private val retencao: Retencao) {

    /** Ids a apagar agora, mais antigas primeiro. */
    fun aExcluir(fotos: List<FotoGuardada>, usoAtualMs: Long): List<Long> {
        val ordenadas = fotos.sortedBy { it.usoNaCapturaMs }
        val limite = retencao.usoMaximoMs
        val velhas = if (limite == null) emptyList() else ordenadas.filter { usoAtualMs - it.usoNaCapturaMs > limite }
        val restantes = ordenadas - velhas.toSet()
        val excesso = restantes.take((restantes.size - maxFotos).coerceAtLeast(0))
        return (velhas + excesso).map { it.id }
    }

    /** Ao encerrar a viagem: com [Retencao.AO_ENCERRAR], todas. */
    fun aExcluirAoEncerrar(fotos: List<FotoGuardada>): List<Long> =
        if (retencao == Retencao.AO_ENCERRAR) fotos.map { it.id } else emptyList()
}
