package br.unicid.helios.app.adaptadores.banco

import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Roda o SQL do banco num SQLite de verdade (sqlite-jdbc), sem Android. O Android usa o mesmo motor,
 * então o que vale aqui vale no aparelho: CREATE, ALTER TABLE e DEFAULT.
 */
class EsquemaBancoTest {

    /** O esquema da versão 2 (v0.4.0 a 0.4.2), congelado: é o banco que já está nos celulares. */
    private val esquemaV2 = listOf(
        """CREATE TABLE sessao (
            id INTEGER PRIMARY KEY, inicio INTEGER NOT NULL, duracao_ms INTEGER NOT NULL,
            alerta_ms INTEGER NOT NULL, piscadas INTEGER NOT NULL, microssonos INTEGER NOT NULL,
            bocejos INTEGER NOT NULL, desatencoes INTEGER NOT NULL, distracoes INTEGER NOT NULL,
            perclos_medio REAL NOT NULL, perclos_max REAL NOT NULL, kss INTEGER)""",
        """CREATE TABLE evento (
            sessao INTEGER NOT NULL, inicio INTEGER NOT NULL, duracao_ms INTEGER NOT NULL,
            tipo TEXT NOT NULL, estado TEXT NOT NULL, perclos_5s REAL NOT NULL, perclos_60s REAL NOT NULL)""",
        """CREATE TABLE janela (
            sessao INTEGER NOT NULL, inicio INTEGER NOT NULL, perclos_60s REAL NOT NULL,
            piscadas_min REAL NOT NULL, duracao_media_ms REAL NOT NULL, bocejos_min REAL NOT NULL,
            desvio_pitch REAL NOT NULL, olhar_fora REAL NOT NULL DEFAULT 0, semicerrado REAL NOT NULL DEFAULT 0)""",
        """CREATE TABLE foto (
            id INTEGER PRIMARY KEY AUTOINCREMENT, sessao INTEGER NOT NULL, captura INTEGER NOT NULL,
            uso_ms INTEGER NOT NULL, nome TEXT NOT NULL, uri TEXT NOT NULL)""",
        "CREATE INDEX evento_sessao ON evento(sessao)",
        "CREATE INDEX janela_sessao ON janela(sessao)",
    )

    @Test
    fun `banco antigo sem as colunas novas migra sem perder linhas, com zeros e estado_inicio vazio`() = memoria { c ->
        executar(c, esquemaV2)
        executar(
            c,
            listOf(
                "INSERT INTO evento VALUES (1, 1000, 2600, 'MICROSSONO', 'CRITICO', 0.5, 0.1)",
                "INSERT INTO janela VALUES (1, 1000, 0.1, 12, 200, 1, 2, 0.25, 0.1)",
            ),
        )
        executar(c, EsquemaBanco.migracoes(2))

        val evento = consultar(c, "SELECT estado, estado_inicio FROM evento").single()
        assertEquals("CRITICO", evento[0])
        assertNull(evento[1])
        val janela = consultar(
            c,
            "SELECT olhar_fora, fps_medio, latencia_media_ms, latencia_p95_ms, tempo_atento_ms, tempo_sem_rosto_ms " +
                "FROM janela",
        ).single()
        assertEquals(listOf(0.25, 0.0, 0.0, 0.0, 0.0, 0.0), janela.map { (it as Number).toDouble() })
    }

    @Test
    fun `banco migrado da versao 1 ou 2 fica com as mesmas colunas de um banco novo`() {
        val novo = memoria { c -> executar(c, EsquemaBanco.CRIAR).let { colunas(c) } }
        val daV2 = memoria { c ->
            executar(c, esquemaV2)
            executar(c, EsquemaBanco.migracoes(2))
            colunas(c)
        }
        val daV1 = memoria { c ->
            val colunasV2 = ", olhar_fora REAL NOT NULL DEFAULT 0, semicerrado REAL NOT NULL DEFAULT 0"
            executar(c, esquemaV2.map { it.replace(colunasV2, "") })
            executar(c, EsquemaBanco.migracoes(1))
            colunas(c)
        }
        assertEquals(novo, daV2)
        assertEquals(novo, daV1)
        assertEquals(3, EsquemaBanco.VERSAO)
        assertEquals(emptyList(), EsquemaBanco.migracoes(EsquemaBanco.VERSAO))
    }

    private fun <T> memoria(bloco: (Connection) -> T): T =
        DriverManager.getConnection("jdbc:sqlite::memory:").use(bloco)

    private fun executar(c: Connection, sql: List<String>) =
        c.createStatement().use { s -> sql.forEach { s.execute(it) } }

    private fun consultar(c: Connection, sql: String): List<List<Any?>> = c.createStatement().use { s ->
        s.executeQuery(sql).use { r ->
            buildList { while (r.next()) add((1..r.metaData.columnCount).map { r.getObject(it) }) }
        }
    }

    /** Tabela → colunas com tipo, NOT NULL e DEFAULT, na ordem. */
    private fun colunas(c: Connection): Map<String, List<String>> =
        listOf("sessao", "evento", "janela", "foto").associateWith { tabela ->
            consultar(c, "PRAGMA table_info($tabela)").map { "${it[1]} ${it[2]} notnull=${it[3]} default=${it[4]}" }
        }
}
