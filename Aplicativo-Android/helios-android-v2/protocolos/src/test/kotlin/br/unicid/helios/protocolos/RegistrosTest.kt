package br.unicid.helios.protocolos

import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.TipoEvento
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CsvTest {
    private val zona = ZoneOffset.ofHours(-3)

    // 2026-10-15T02:10:05.123Z = 2026-10-14T23:10:05.123-03:00
    private val instante = 1_792_030_205_123L

    @Test
    fun `cabecalhos combinados com a equipe`() {
        assertEquals(
            "sessao,inicio,duracao_ms,tipo,estado_inicio,estado_fim,perclos_5s,perclos_60s,foto,yaw_max_graus",
            Csv.CABECALHO_EVENTOS,
        )
        assertEquals(
            "sessao,inicio,perclos_60s,piscadas_min,duracao_media_piscada_ms,bocejos_min,desvio_pitch," +
                "olhar_fora,semicerrado,kss," +
                "tempo_atento_ms,tempo_atencao_ms,tempo_fadiga_ms,tempo_critico_ms,tempo_distraido_ms," +
                "tempo_desatento_ms,tempo_sem_rosto_ms,fps_medio,latencia_media_ms,latencia_p95_ms," +
                "cnn_fechado_media,cnn_discordancia",
            Csv.CABECALHO_JANELAS,
        )
    }

    @Test
    fun `toda coluna de tempo tem um estado e todo estado tem coluna`() {
        assertEquals(EstadoMotorista.entries.toSet(), Csv.COLUNAS_TEMPO.keys)
        assertTrue(Csv.CABECALHO_JANELAS.contains(",kss," + Csv.COLUNAS_TEMPO.values.joinToString(",") + ",fps_medio"))
    }

    @Test
    fun `linha de evento usa virgula, ponto decimal e horario com fuso`() {
        val l = LinhaEvento(
            7, instante, 1_500, TipoEvento.MICROSSONO, EstadoMotorista.CRITICO, 0.41234, 0.1, "helios-critico.jpg",
            estadoInicio = EstadoMotorista.ATENTO,
        )
        assertEquals(
            "7,2026-10-14T23:10:05.123-03:00,1500,MICROSSONO,ATENTO,CRITICO,0.4123,0.1000,helios-critico.jpg,",
            Csv.evento(l, zona),
        )
        assertEquals(
            "7,2026-10-14T23:10:05.123-03:00,1500,MICROSSONO,ATENTO,CRITICO,0.4123,0.1000,,",
            Csv.evento(l.copy(foto = null), zona),
        )
    }

    @Test
    fun `evento gravado antes do estado_inicio sai com a coluna vazia`() {
        val l = LinhaEvento(7, instante, 1_500, TipoEvento.MICROSSONO, EstadoMotorista.CRITICO, 0.5, 0.1, null)
        assertEquals("7,2026-10-14T23:10:05.123-03:00,1500,MICROSSONO,,CRITICO,0.5000,0.1000,,", Csv.evento(l, zona))
    }

    @Test
    fun `giro e rosto ausente levam o maior giro da cabeca em graus`() {
        val l = LinhaEvento(7, instante, 2_500, TipoEvento.CABECA_VIRADA, EstadoMotorista.ATENTO, 0.0, 0.0, null)
        assertEquals(
            "7,2026-10-14T23:10:05.123-03:00,2500,CABECA_VIRADA,,ATENTO,0.0000,0.0000,,59.0",
            Csv.evento(l.copy(yawMaximoGraus = 59.04), zona),
        )
    }

    @Test
    fun `linha de janela com e sem KSS`() {
        val l = LinhaJanela(
            7, instante, 0.12345, 14.0, 182.5, 2.0, 3.456, 6, fracaoOlharFora = 0.25, fracaoSemicerrado = 0.1,
            tempoPorEstadoMs = mapOf(
                EstadoMotorista.ATENTO to 20_000L,
                EstadoMotorista.ATENCAO to 1_500L,
                EstadoMotorista.FADIGA to 4_000L,
                EstadoMotorista.CRITICO to 2_500L,
                EstadoMotorista.DISTRAIDO to 1_000L,
                EstadoMotorista.DESATENTO to 700L,
                EstadoMotorista.SEM_ROSTO to 300L,
            ),
            fpsMedio = 29.876, latenciaMediaMs = 41.25, latenciaP95Ms = 63,
            cnnFechadoMedia = 0.41234, cnnDiscordancia = 0.125,
        )
        val tempos = "20000,1500,4000,2500,1000,700,300,29.88,41.3,63,0.4123,0.1250"
        assertEquals(
            "7,2026-10-14T23:10:05.123-03:00,0.1235,14.00,182.5,2.00,3.46,0.2500,0.1000,6,$tempos",
            Csv.janela(l, zona),
        )
        assertEquals(
            "7,2026-10-14T23:10:05.123-03:00,0.1235,14.00,182.5,2.00,3.46,0.2500,0.1000,,$tempos",
            Csv.janela(l.copy(kss = null), zona),
        )
    }

    @Test
    fun `janela gravada antes dos tempos sai com zeros e sem a CNN sai com as colunas vazias`() {
        val l = LinhaJanela(7, instante, 0.0, 0.0, 0.0, 0.0, 0.0, null)
        assertEquals(
            "7,2026-10-14T23:10:05.123-03:00,0.0000,0.00,0.0,0.00,0.00,0.0000,0.0000,,0,0,0,0,0,0,0,0.00,0.0,0,,",
            Csv.janela(l, zona),
        )
    }

    @Test
    fun `arquivo inteiro tem cabecalho e uma linha por registro`() {
        val ev = LinhaEvento(1, instante, 100, TipoEvento.PISCADA, EstadoMotorista.ATENTO, 0.0, 0.0, null)
        val texto = Csv.eventos(listOf(ev, ev), zona)
        assertEquals(3, texto.trimEnd().lines().size)
        assertEquals(Csv.CABECALHO_EVENTOS, texto.lines().first())
        val jan = LinhaJanela(1, instante, 0.0, 0.0, 0.0, 0.0, 0.0, null)
        assertEquals(listOf(Csv.CABECALHO_JANELAS), Csv.janelas(emptyList(), zona).trimEnd().lines())
        assertEquals(2, Csv.janelas(listOf(jan), zona).trimEnd().lines().size)
    }

    @Test
    fun `campo com virgula ou aspas e protegido`() {
        assertEquals("simples", Csv.campo("simples"))
        assertEquals("\"a,b\"", Csv.campo("a,b"))
        assertEquals("\"diz \"\"oi\"\"\"", Csv.campo("diz \"oi\""))
        assertEquals("\"linha\nnova\"", Csv.campo("linha\nnova"))
    }
}

class FotosTest {
    private val zona = ZoneOffset.ofHours(-3)

    @Test
    fun `nome da foto tem estado e horario local`() {
        assertEquals("helios-desatento-20261014-231005.jpg", nomeFoto(EstadoMotorista.DESATENTO, 1_792_030_205_123L, zona))
        assertEquals(
            "helios-critico-20261014-231005-analise.jpg",
            nomeFoto(EstadoMotorista.CRITICO, 1_792_030_205_123L, zona, anotada = true),
        )
        assertEquals(
            "helios-teste-20261014-231005-analise.jpg",
            nomeFoto(EstadoMotorista.ATENTO, 1_792_030_205_123L, zona, anotada = true, teste = true),
        )
    }

    @Test
    fun `foto do evento e a capturada durante ele ou logo depois`() {
        val fotos = listOf(FotoRegistrada(10_000, "a.jpg"), FotoRegistrada(20_500, "b.jpg"))
        assertEquals("a.jpg", fotoDoEvento(TipoEvento.MICROSSONO, 9_000, 1_500, fotos))
        assertEquals("b.jpg", fotoDoEvento(TipoEvento.ROSTO_AUSENTE, 19_000, 1_000, fotos)) // 500 ms depois do fim: na folga
        assertNull(fotoDoEvento(TipoEvento.CABECA_VIRADA, 30_000, 1_000, fotos))
        assertNull(fotoDoEvento(TipoEvento.BOCEJO, 10_500, 100, fotos)) // foto antes do evento não conta
        assertNull(fotoDoEvento(TipoEvento.PISCADA, 9_900, 100, fotos)) // piscada nunca recebe foto
        assertEquals("a.jpg", fotoDoEvento(TipoEvento.SONO, 7_000, 3_500, fotos))
        assertEquals("a.jpg", fotoDoEvento(TipoEvento.OLHAR_DESVIADO, 9_000, 3_000, fotos))
    }

    @Test
    fun `fotos do evento trazem a crua e a anotada da mesma captura, e a miniatura e a anotada`() {
        val crua = FotoRegistrada(10_000, "helios-critico-x.jpg", "content://a")
        val anotada = FotoRegistrada(10_000, "helios-critico-x-analise.jpg", "content://b")
        val outra = FotoRegistrada(11_000, "helios-fadiga-y.jpg", "content://c")
        val doEvento = fotosDoEvento(TipoEvento.MICROSSONO, 9_000, 1_500, listOf(crua, anotada, outra))
        assertEquals(listOf(crua, anotada), doEvento)
        assertEquals(anotada, miniatura(doEvento))
        assertEquals(crua, miniatura(listOf(crua)))
        assertNull(miniatura(emptyList()))
        assertEquals(emptyList(), fotosDoEvento(TipoEvento.PISCADA, 9_000, 1_500, listOf(crua)))
        assertEquals(emptyList(), fotosDoEvento(TipoEvento.SONO, 50_000, 1_500, listOf(crua)))
    }

    @Test
    fun `retencao por tempo de uso apaga as velhas e depois o excesso`() {
        val p = PoliticaRetencao(maxFotos = 2, retencao = Retencao.DUAS_HORAS)
        val fotos = listOf(FotoGuardada(1, 0), FotoGuardada(2, 3_600_000), FotoGuardada(3, 7_000_000), FotoGuardada(4, 7_100_000))
        // Uso atual 2 h + 10 min: a 1 tem mais de 2 h de uso; das que sobram, a mais velha excede o limite de 2.
        assertEquals(listOf(1L, 2L), p.aExcluir(fotos, usoAtualMs = 7_800_000))
        assertEquals(emptyList(), p.aExcluir(fotos.take(2), usoAtualMs = 3_600_000))
    }

    @Test
    fun `so limite e ao encerrar nao apagam por idade`() {
        val fotos = listOf(FotoGuardada(1, 0), FotoGuardada(2, 1), FotoGuardada(3, 2))
        assertEquals(listOf(1L), PoliticaRetencao(2, Retencao.SO_LIMITE).aExcluir(fotos, Long.MAX_VALUE / 2))
        assertEquals(emptyList(), PoliticaRetencao(5, Retencao.AO_ENCERRAR).aExcluir(fotos, Long.MAX_VALUE / 2))
    }

    @Test
    fun `ao encerrar apaga tudo so na opcao AO_ENCERRAR`() {
        val fotos = listOf(FotoGuardada(1, 0), FotoGuardada(2, 1))
        assertEquals(listOf(1L, 2L), PoliticaRetencao(100, Retencao.AO_ENCERRAR).aExcluirAoEncerrar(fotos))
        assertEquals(emptyList(), PoliticaRetencao(100, Retencao.TRES_HORAS).aExcluirAoEncerrar(fotos))
        assertEquals(10_800_000L, Retencao.TRES_HORAS.usoMaximoMs)
    }
}
