package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import java.time.ZoneId

/**
 * Um quadro do `quadros.csv` (Parte D): estado, motivos, métricas, os 52 blendshapes, latência, aceleração, zona,
 * CNN, perfil e sensibilidade. Nunca imagem nem coordenadas de pontos (LGPD). Sem rosto, as métricas ficam vazias.
 */
data class LinhaQuadro(
    val epochMs: Long,
    val estado: EstadoMotorista,
    val motivos: Set<Motivo>,
    val metricas: Metricas?,
    val latenciaMs: Long,
    val delegate: String,
    val rmsAceleracao: Double,
    val perfil: String?,
    val sensibilidade: String,
    val blendshapes: Map<String, Double>,
)

private const val CASAS = 4
private const val CASAS_GRAUS = 1
private const val SEGUNDOS_POR_HORA = 3_600
private const val BYTES_POR_MB = 1_000_000.0

/** Colunas de "ear" a "cnn_esq": vazias quando não há rosto. */
private const val COLUNAS_DE_METRICAS = 19

object RegistroQuadros {
    private val COLUNAS = listOf(
        "inicio", "estado", "motivos", "ear", "ear_dir", "ear_esq", "mar", "ecf", "perclos_5s", "perclos_60s",
        "olho_fechado", "pitch", "yaw", "roll", "olhar_h", "olhar_v", "desvio_h", "desvio_v", "zona", "fora_da_via",
        "cnn_dir", "cnn_esq", "latencia_ms", "delegate", "rms_aceleracao", "perfil", "sensibilidade",
    )

    val CABECALHO: String = (COLUNAS + Blendshapes.TODOS).joinToString(",")

    fun linha(l: LinhaQuadro, zona: ZoneId): String {
        val m = l.metricas
        return (
            listOf(Csv.hora(l.epochMs, zona), l.estado.name, l.motivos.map { it.name }.sorted().joinToString(";")) +
                metricas(m) +
                listOf(l.latenciaMs.toString(), l.delegate, num(l.rmsAceleracao)) +
                listOf(Csv.campo(l.perfil ?: ""), l.sensibilidade) +
                Blendshapes.TODOS.map { nome -> num(l.blendshapes[nome]) }
            ).joinToString(",")
    }

    private fun metricas(m: Metricas?): List<String> {
        if (m == null) return List(COLUNAS_DE_METRICAS) { "" }
        val pose = m.pose
        val o = m.olhar
        val zonaOlhar = o.zona
        return listOf(
            num(m.earBruto), num(m.earDireito), num(m.earEsquerdo), num(m.marExibicao), num(m.ecf),
            num(m.perclosCurto), num(m.perclosLongo), if (m.olhoFechado) "1" else "0",
            graus(pose?.pitchGraus), graus(pose?.yawGraus), graus(pose?.rollGraus),
            num(o.horizontal), num(o.vertical), num(o.desvioHorizontal), num(o.desvioVertical),
            if (zonaOlhar == null) "" else zonaOlhar.name, if (o.foraAgora) "1" else "0",
            num(m.cnnFechadoDireito), num(m.cnnFechadoEsquerdo),
        )
    }

    private fun num(v: Double?) = if (v == null) "" else Csv.decimal(v, CASAS)

    private fun graus(v: Double?) = if (v == null) "" else Csv.decimal(v, CASAS_GRAUS)

    /** Quanto uma hora de registro ocupa, para Ajustes mostrar antes de ligar. */
    fun megabytesPorHora(bytesPorLinha: Int, quadrosPorSegundo: Double): Double =
        bytesPorLinha * quadrosPorSegundo * SEGUNDOS_POR_HORA / BYTES_POR_MB
}
