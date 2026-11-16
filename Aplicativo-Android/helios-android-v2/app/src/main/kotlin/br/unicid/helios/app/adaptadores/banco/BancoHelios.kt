package br.unicid.helios.app.adaptadores.banco

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import br.unicid.helios.app.portas.FotoSalva
import br.unicid.helios.app.portas.RepositorioViagens
import br.unicid.helios.app.portas.SessaoGravada
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.TipoEvento
import br.unicid.helios.protocolos.Csv
import br.unicid.helios.protocolos.LinhaEvento
import br.unicid.helios.protocolos.LinhaJanela

/**
 * Histórico local em SQLite, usando a API que já vem no Android (sem Room, sem dependência nova).
 * Tudo fica no armazenamento privado do app; nenhuma imagem entra no banco, só o nome da foto.
 * Não é thread-safe por chamada: o ViewModel usa uma única thread de E/S para gravar.
 */
class BancoHelios(context: Context) :
    SQLiteOpenHelper(context, "helios.db", null, EsquemaBanco.VERSAO),
    RepositorioViagens {

    override fun onCreate(db: SQLiteDatabase) = EsquemaBanco.CRIAR.forEach(db::execSQL)

    override fun onUpgrade(db: SQLiteDatabase, antiga: Int, nova: Int) =
        EsquemaBanco.migracoes(antiga).forEach(db::execSQL)

    override fun salvarSessao(sessao: SessaoGravada) {
        val kssAtual = sessoes(sessao.id).firstOrNull()?.kss
        val valores = ContentValues().apply {
            put("id", sessao.id)
            put("inicio", sessao.inicioEpochMs)
            put("duracao_ms", sessao.duracaoMs)
            put("alerta_ms", sessao.tempoEmAlertaMs)
            put("piscadas", sessao.piscadas)
            put("microssonos", sessao.microssonos)
            put("bocejos", sessao.bocejos)
            put("desatencoes", sessao.desatencoes)
            put("distracoes", sessao.distracoes)
            put("perclos_medio", sessao.perclosMedio)
            put("perclos_max", sessao.perclosMaximo)
            (sessao.kss ?: kssAtual)?.let { put("kss", it) }
        }
        writableDatabase.insertWithOnConflict("sessao", null, valores, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun gravarEventos(linhas: List<LinhaEvento>) {
        if (linhas.isEmpty()) return
        transacao { db ->
            linhas.forEach { l ->
                db.insert(
                    "evento",
                    null,
                    ContentValues().apply {
                        put("sessao", l.sessao)
                        put("inicio", l.inicioEpochMs)
                        put("duracao_ms", l.duracaoMs)
                        put("tipo", l.tipo.name)
                        put("estado", l.estado.name)
                        put("perclos_5s", l.perclosCurto)
                        put("perclos_60s", l.perclosLongo)
                        put("estado_inicio", l.estadoInicio?.name)
                        put("yaw_max", l.yawMaximoGraus)
                    },
                )
            }
        }
    }

    override fun gravarJanela(linha: LinhaJanela) {
        writableDatabase.insert(
            "janela",
            null,
            ContentValues().apply {
                put("sessao", linha.sessao)
                put("inicio", linha.inicioEpochMs)
                put("perclos_60s", linha.perclosLongo)
                put("piscadas_min", linha.piscadasPorMinuto)
                put("duracao_media_ms", linha.duracaoMediaPiscadaMs)
                put("bocejos_min", linha.bocejosPorMinuto)
                put("desvio_pitch", linha.desvioPitchGraus)
                put("olhar_fora", linha.fracaoOlharFora)
                put("semicerrado", linha.fracaoSemicerrado)
                put("fps_medio", linha.fpsMedio)
                put("latencia_media_ms", linha.latenciaMediaMs)
                put("latencia_p95_ms", linha.latenciaP95Ms)
                Csv.COLUNAS_TEMPO.forEach { (estado, coluna) -> put(coluna, linha.tempoPorEstadoMs[estado] ?: 0L) }
                put("cnn_fechado_media", linha.cnnFechadoMedia)
                put("cnn_discordancia", linha.cnnDiscordancia)
                put("zona_espelho_ms", linha.zonaEspelhoMs)
                put("zona_painel_ms", linha.zonaPainelMs)
                put("zona_outra_ms", linha.zonaOutraMs)
                put("manobra_ms", linha.manobraMs)
                put("perfil", linha.perfil)
                put("sensibilidade", linha.sensibilidade)
            },
        )
    }

    override fun gravarFoto(foto: FotoSalva): Long = writableDatabase.insert(
        "foto",
        null,
        ContentValues().apply {
            put("sessao", foto.sessao)
            put("captura", foto.capturaEpochMs)
            put("uso_ms", foto.usoNaCapturaMs)
            put("nome", foto.nome)
            put("uri", foto.uri)
        },
    )

    override fun sessoes(): List<SessaoGravada> = sessoes(null)

    private fun sessoes(id: Long?): List<SessaoGravada> = consultar(
        "SELECT id, inicio, duracao_ms, alerta_ms, piscadas, microssonos, bocejos, desatencoes, distracoes, " +
            "perclos_medio, perclos_max, kss FROM sessao" +
            (if (id != null) " WHERE id = ?" else "") + " ORDER BY id DESC",
        id,
    ) { c ->
        SessaoGravada(
            id = c.getLong(0),
            inicioEpochMs = c.getLong(1),
            duracaoMs = c.getLong(2),
            tempoEmAlertaMs = c.getLong(3),
            piscadas = c.getInt(4),
            microssonos = c.getInt(5),
            bocejos = c.getInt(6),
            desatencoes = c.getInt(7),
            distracoes = c.getInt(8),
            perclosMedio = c.getDouble(9),
            perclosMaximo = c.getDouble(10),
            kss = if (c.isNull(11)) null else c.getInt(11),
        )
    }

    override fun eventos(sessao: Long?): List<LinhaEvento> = consultar(
        "SELECT sessao, inicio, duracao_ms, tipo, estado, perclos_5s, perclos_60s, estado_inicio, yaw_max FROM evento" +
            filtroSessao(sessao) + " ORDER BY sessao, inicio",
        sessao,
    ) { c ->
        LinhaEvento(
            sessao = c.getLong(0),
            inicioEpochMs = c.getLong(1),
            duracaoMs = c.getLong(2),
            tipo = TipoEvento.valueOf(c.getString(3)),
            estado = EstadoMotorista.valueOf(c.getString(4)),
            perclosCurto = c.getDouble(5),
            perclosLongo = c.getDouble(6),
            foto = null,
            estadoInicio = if (c.isNull(7)) null else EstadoMotorista.valueOf(c.getString(7)),
            yawMaximoGraus = if (c.isNull(8)) null else c.getDouble(8),
        )
    }

    override fun janelas(sessao: Long?): List<LinhaJanela> = consultar(
        "SELECT j.sessao, j.inicio, j.perclos_60s, j.piscadas_min, j.duracao_media_ms, j.bocejos_min, " +
            "j.desvio_pitch, s.kss, j.olhar_fora, j.semicerrado, " +
            "j.fps_medio, j.latencia_media_ms, j.latencia_p95_ms, " +
            Csv.COLUNAS_TEMPO.values.joinToString(", ") { "j.$it" } + ", j.cnn_fechado_media, j.cnn_discordancia, " +
            "j.zona_espelho_ms, j.zona_painel_ms, j.zona_outra_ms, j.manobra_ms, j.perfil, j.sensibilidade " +
            "FROM janela j LEFT JOIN sessao s ON s.id = j.sessao" +
            (if (sessao != null) " WHERE j.sessao = ?" else "") + " ORDER BY j.sessao, j.inicio",
        sessao,
    ) { c ->
        LinhaJanela(
            sessao = c.getLong(0),
            inicioEpochMs = c.getLong(1),
            perclosLongo = c.getDouble(2),
            piscadasPorMinuto = c.getDouble(3),
            duracaoMediaPiscadaMs = c.getDouble(4),
            bocejosPorMinuto = c.getDouble(5),
            desvioPitchGraus = c.getDouble(6),
            kss = if (c.isNull(7)) null else c.getInt(7),
            fracaoOlharFora = c.getDouble(8),
            fracaoSemicerrado = c.getDouble(9),
            fpsMedio = c.getDouble(10),
            latenciaMediaMs = c.getDouble(11),
            latenciaP95Ms = c.getLong(12),
            tempoPorEstadoMs = Csv.COLUNAS_TEMPO.keys.withIndex()
                .associate { (i, estado) -> estado to c.getLong(13 + i) },
            cnnFechadoMedia = (13 + Csv.COLUNAS_TEMPO.size).let { if (c.isNull(it)) null else c.getDouble(it) },
            cnnDiscordancia = (14 + Csv.COLUNAS_TEMPO.size).let { if (c.isNull(it)) null else c.getDouble(it) },
            zonaEspelhoMs = c.getLong(15 + Csv.COLUNAS_TEMPO.size),
            zonaPainelMs = c.getLong(16 + Csv.COLUNAS_TEMPO.size),
            zonaOutraMs = c.getLong(17 + Csv.COLUNAS_TEMPO.size),
            manobraMs = c.getLong(18 + Csv.COLUNAS_TEMPO.size),
            perfil = (19 + Csv.COLUNAS_TEMPO.size).let { if (c.isNull(it)) null else c.getString(it) },
            sensibilidade = (20 + Csv.COLUNAS_TEMPO.size).let { if (c.isNull(it)) null else c.getString(it) },
        )
    }

    override fun fotos(): List<FotoSalva> = consultar(
        "SELECT id, sessao, captura, uso_ms, nome, uri FROM foto ORDER BY captura",
        null,
    ) { c -> FotoSalva(c.getLong(0), c.getLong(1), c.getLong(2), c.getLong(3), c.getString(4), c.getString(5)) }

    override fun excluirFotos(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        transacao { db -> ids.forEach { db.delete("foto", "id = ?", arrayOf(it.toString())) } }
    }

    override fun definirKss(sessao: Long, kss: Int?) {
        writableDatabase.update(
            "sessao",
            ContentValues().apply { if (kss == null) putNull("kss") else put("kss", kss) },
            "id = ?",
            arrayOf(sessao.toString()),
        )
    }

    override fun limparTudo() {
        transacao { db -> listOf("sessao", "evento", "janela", "foto").forEach { db.delete(it, null, null) } }
    }

    private fun filtroSessao(sessao: Long?) = if (sessao != null) " WHERE sessao = ?" else ""

    private fun <T> consultar(sql: String, parametro: Long?, ler: (Cursor) -> T): List<T> =
        readableDatabase.rawQuery(sql, parametro?.let { arrayOf(it.toString()) }).use { c ->
            buildList { while (c.moveToNext()) add(ler(c)) }
        }

    private fun transacao(bloco: (SQLiteDatabase) -> Unit) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            bloco(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

/**
 * O SQL do banco, fora do [BancoHelios] para os testes rodarem num SQLite de verdade sem Android.
 * Colunas novas entram no fim do CREATE e numa migração com DEFAULT, na mesma ordem: nada do histórico se perde.
 */
internal object EsquemaBanco {
    const val VERSAO = 6

    /** Versão 3 (v0.4.3): fps, latência e tempo em cada estado por janela. Janelas antigas ficam com zero. */
    private val COLUNAS_V3_JANELA = listOf(
        "fps_medio REAL NOT NULL DEFAULT 0",
        "latencia_media_ms REAL NOT NULL DEFAULT 0",
        "latencia_p95_ms INTEGER NOT NULL DEFAULT 0",
    ) + Csv.COLUNAS_TEMPO.values.map { "$it INTEGER NOT NULL DEFAULT 0" }

    /** Versão 3 (v0.4.3): estado no início do evento. Eventos antigos ficam com NULL (coluna vazia no CSV). */
    private const val COLUNA_V3_EVENTO = "estado_inicio TEXT DEFAULT NULL"

    /** Versão 5 (v0.4.8): maior giro da cabeça no evento (ADR-014). NULL nos outros eventos e nos antigos. */
    private const val COLUNA_V5_EVENTO = "yaw_max REAL DEFAULT NULL"

    /** Versão 4 (v0.4.4): CNN do olho em modo sombra. NULL com ela desligada e nas janelas antigas. */
    private val COLUNAS_V4_JANELA = listOf("cnn_fechado_media REAL DEFAULT NULL", "cnn_discordancia REAL DEFAULT NULL")

    /** Versão 6 (v0.4.9): tempo por zona, manobra, perfil e sensibilidade (Parte D). Antigas: zero e NULL. */
    private val COLUNAS_V6_JANELA = listOf(
        "zona_espelho_ms INTEGER NOT NULL DEFAULT 0",
        "zona_painel_ms INTEGER NOT NULL DEFAULT 0",
        "zona_outra_ms INTEGER NOT NULL DEFAULT 0",
        "manobra_ms INTEGER NOT NULL DEFAULT 0",
        "perfil TEXT DEFAULT NULL",
        "sensibilidade TEXT DEFAULT NULL",
    )

    val CRIAR = listOf(
        """CREATE TABLE sessao (
            id INTEGER PRIMARY KEY, inicio INTEGER NOT NULL, duracao_ms INTEGER NOT NULL,
            alerta_ms INTEGER NOT NULL, piscadas INTEGER NOT NULL, microssonos INTEGER NOT NULL,
            bocejos INTEGER NOT NULL, desatencoes INTEGER NOT NULL, distracoes INTEGER NOT NULL,
            perclos_medio REAL NOT NULL, perclos_max REAL NOT NULL, kss INTEGER)""",
        """CREATE TABLE evento (
            sessao INTEGER NOT NULL, inicio INTEGER NOT NULL, duracao_ms INTEGER NOT NULL,
            tipo TEXT NOT NULL, estado TEXT NOT NULL, perclos_5s REAL NOT NULL, perclos_60s REAL NOT NULL,
            $COLUNA_V3_EVENTO, $COLUNA_V5_EVENTO)""",
        """CREATE TABLE janela (
            sessao INTEGER NOT NULL, inicio INTEGER NOT NULL, perclos_60s REAL NOT NULL,
            piscadas_min REAL NOT NULL, duracao_media_ms REAL NOT NULL, bocejos_min REAL NOT NULL,
            desvio_pitch REAL NOT NULL, olhar_fora REAL NOT NULL DEFAULT 0, semicerrado REAL NOT NULL DEFAULT 0,
            ${(COLUNAS_V3_JANELA + COLUNAS_V4_JANELA + COLUNAS_V6_JANELA).joinToString(", ")})""",
        """CREATE TABLE foto (
            id INTEGER PRIMARY KEY AUTOINCREMENT, sessao INTEGER NOT NULL, captura INTEGER NOT NULL,
            uso_ms INTEGER NOT NULL, nome TEXT NOT NULL, uri TEXT NOT NULL)""",
        "CREATE INDEX evento_sessao ON evento(sessao)",
        "CREATE INDEX janela_sessao ON janela(sessao)",
    )

    /** SQL que leva um banco da versão [antiga] até [VERSAO]. Versão 2 (v0.4.0): olhar fora e olhos semicerrados. */
    fun migracoes(antiga: Int): List<String> = buildList {
        if (antiga < 2) {
            add("ALTER TABLE janela ADD COLUMN olhar_fora REAL NOT NULL DEFAULT 0")
            add("ALTER TABLE janela ADD COLUMN semicerrado REAL NOT NULL DEFAULT 0")
        }
        if (antiga < 3) {
            add("ALTER TABLE evento ADD COLUMN $COLUNA_V3_EVENTO")
            COLUNAS_V3_JANELA.forEach { add("ALTER TABLE janela ADD COLUMN $it") }
        }
        if (antiga < 4) COLUNAS_V4_JANELA.forEach { add("ALTER TABLE janela ADD COLUMN $it") }
        if (antiga < 5) add("ALTER TABLE evento ADD COLUMN $COLUNA_V5_EVENTO")
        if (antiga < 6) COLUNAS_V6_JANELA.forEach { add("ALTER TABLE janela ADD COLUMN $it") }
    }
}
