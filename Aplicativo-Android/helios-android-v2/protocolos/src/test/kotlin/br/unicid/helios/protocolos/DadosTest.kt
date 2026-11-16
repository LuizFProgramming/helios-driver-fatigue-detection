package br.unicid.helios.protocolos

import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.calibracao.Desvios
import br.unicid.helios.dominio.instalacao.LadoMotorista
import br.unicid.helios.dominio.instalacao.PerfilInstalacao
import br.unicid.helios.dominio.instalacao.PosicaoCamera
import br.unicid.helios.dominio.instalacao.Zona
import br.unicid.helios.dominio.instalacao.ZonaOlhar
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.monitor.MetricasOlhar
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import br.unicid.helios.dominio.tipos.PoseCabeca
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Parte D: janelas com zonas e manobra, instalacao.csv, quadros.csv e o piso de volume do alarme. */
class DadosTest {

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val instante = 1_792_030_205_123L // 2026-10-14T23:10:05.123-03:00

    @Test
    fun `janela traz o tempo por zona, a manobra, o perfil e a sensibilidade`() {
        val j = LinhaJanela(
            1, instante, 0.1, 15.0, 200.0, 0.0, 2.0, null,
            zonaEspelhoMs = 4_000, zonaPainelMs = 1_500, zonaOutraMs = 700, manobraMs = 9_000,
            perfil = "Gol, azul", sensibilidade = "ALTA",
        )
        val novas = ",zona_espelho_ms,zona_painel_ms,zona_outra_ms,manobra_ms,perfil,sensibilidade"
        assertTrue(Csv.CABECALHO_JANELAS.endsWith(novas))
        assertTrue(Csv.janela(j, zona).endsWith(",4000,1500,700,9000,\"Gol, azul\",ALTA"))
        assertTrue(Csv.janela(j.copy(perfil = null, sensibilidade = null), zona).endsWith(",4000,1500,700,9000,,"))
    }

    @Test
    fun `instalacao csv tem uma linha por perfil com a calibracao e as quatro zonas`() {
        val p = PerfilInstalacao(
            "a1", "Gol", PosicaoCamera.COLUNA_VOLANTE, LadoMotorista.ESQUERDO, instante,
            Calibracao(0.31, -15.5, 2.0, olharHorizontal = 0.48), Desvios(1.5, 2.0, 0.01, null),
            listOf(ZonaOlhar(Zona.ESPELHO_ESQUERDO, -40.0, 0.0, null, null, 8.0, 8.0, 0.05)), 0.95, 89,
        )
        val linhas = Csv.instalacao(listOf(p), zona).trim().lines()
        val cabecalho = linhas.first().split(",")
        val valores = cabecalho.zip(linhas[1].split(",")).toMap()
        assertEquals(18 + 4 * 7, cabecalho.size)
        assertEquals("2026-10-14T23:10:05.123-03:00", valores["criado"])
        assertEquals("COLUNA_VOLANTE", valores["posicao"])
        assertEquals("89", valores["nota"])
        assertEquals("-15.5", valores["pitch"])
        assertEquals("", valores["ear_dir"])
        assertEquals("-40.0", valores["espelho_esquerdo_yaw"])
        assertEquals("", valores["retrovisor_yaw"])
    }

    private val colunas = RegistroQuadros.CABECALHO.split(",")

    private fun valores(l: LinhaQuadro): Map<String, String> {
        val campos = RegistroQuadros.linha(l, zona).split(",")
        assertEquals(colunas.size, campos.size)
        return colunas.zip(campos).toMap()
    }

    private val quadroCompleto = LinhaQuadro(
        instante, EstadoMotorista.DISTRAIDO, setOf(Motivo.OLHAR_ZONA, Motivo.CABECA_VIRADA),
        Metricas(
            0.28, 0.29, 0.12, 0.05, 0.02, 0.1, true, false, PoseCabeca(-12.0, 3.5, 1.0),
            earDireito = 0.27, earEsquerdo = 0.26,
            olhar = MetricasOlhar(0.45, 0.4, -0.05, 0.03, zona = Zona.PAINEL, foraAgora = true),
            cnnFechadoDireito = 0.7, cnnFechadoEsquerdo = 0.6,
        ),
        48, "GPU", 0.12, "a1", "PADRAO", mapOf("eyeBlinkLeft" to 0.25, "jawOpen" to 0.5),
    )

    @Test
    fun `quadro tem as metricas e os 52 blendshapes, sem imagem nem pontos`() {
        assertEquals(52, Blendshapes.TODOS.size)
        assertTrue(colunas.containsAll(Blendshapes.TODOS))
        assertTrue(colunas.none { it.startsWith("ponto") || it.startsWith("x") })
        val esperado = mapOf(
            "inicio" to "2026-10-14T23:10:05.123-03:00", "estado" to "DISTRAIDO",
            "motivos" to "CABECA_VIRADA;OLHAR_ZONA",
            "ear" to "0.2800", "ear_dir" to "0.2700", "ear_esq" to "0.2600", "mar" to "0.1200", "ecf" to "0.0500",
            "perclos_5s" to "0.0200", "perclos_60s" to "0.1000", "olho_fechado" to "1",
            "pitch" to "-12.0", "yaw" to "3.5", "roll" to "1.0",
            "olhar_h" to "0.4500", "olhar_v" to "0.4000", "desvio_h" to "-0.0500", "desvio_v" to "0.0300",
            "zona" to "PAINEL", "fora_da_via" to "1", "cnn_dir" to "0.7000", "cnn_esq" to "0.6000",
            "latencia_ms" to "48", "delegate" to "GPU", "rms_aceleracao" to "0.1200", "perfil" to "a1",
            "sensibilidade" to "PADRAO", "eyeBlinkLeft" to "0.2500", "jawOpen" to "0.5000", "eyeBlinkRight" to "",
        )
        val v = valores(quadroCompleto)
        assertEquals(esperado, esperado.keys.associateWith { v[it] })
    }

    @Test
    fun `quadro sem pose, sem olhar, sem CNN e sem perfil sai com as colunas vazias`() {
        val m = Metricas(0.3, 0.3, 0.1, 0.0, 0.0, 0.0, false, false, null)
        val v = valores(quadroCompleto.copy(metricas = m, perfil = null, blendshapes = emptyMap()))
        val vazias = listOf("pitch", "yaw", "roll", "olhar_h", "olhar_v", "zona", "cnn_dir", "perfil", "jawOpen")
        assertEquals(vazias.map { "" }, vazias.map { v[it] })
        assertEquals(listOf("0", "0"), listOf(v["olho_fechado"], v["fora_da_via"]))
        val semRosto = valores(quadroCompleto.copy(metricas = null))
        val metricas = colunas.subList(colunas.indexOf("ear"), colunas.indexOf("latencia_ms"))
        assertEquals(metricas.map { "" }, metricas.map { semRosto[it] })
        assertEquals("48", semRosto["latencia_ms"])
    }

    @Test
    fun `tamanho de uma hora sai do tamanho da linha e do fps`() {
        assertEquals(72.0, RegistroQuadros.megabytesPorHora(bytesPorLinha = 1_000, quadrosPorSegundo = 20.0), 1e-9)
        assertEquals(0.0, RegistroQuadros.megabytesPorHora(600, 0.0), 1e-9)
    }

    @Test
    fun `piso de volume sobe o bipe fraco e nao mexe no forte`() {
        val plano = assertNotNull(planoDeAlerta(EstadoMotorista.FADIGA, setOf(Motivo.PERCLOS_FADIGA), 0.0))
        assertEquals(0.7, plano.comVolumeMinimo(0.7).padrao.volume, 1e-9)
        val forte = assertNotNull(planoDeAlerta(EstadoMotorista.CRITICO, setOf(Motivo.SONO), 30.0))
        assertEquals(forte.padrao.volume, forte.comVolumeMinimo(0.3).padrao.volume, 1e-9)
        assertEquals(plano, plano.comVolumeMinimo(0.0))
    }
}
