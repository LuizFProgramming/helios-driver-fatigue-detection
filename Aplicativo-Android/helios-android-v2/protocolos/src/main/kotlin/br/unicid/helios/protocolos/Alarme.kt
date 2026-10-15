package br.unicid.helios.protocolos

import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import kotlin.math.PI
import kotlin.math.sin

/**
 * Padrão do alarme sonoro por estado. O alarme principal é sempre sonoro: tela vermelha
 * sozinha não acorda um motorista de olhos fechados.
 *
 * [codigoBuzzer] é o número enviado para `POST /buzzer?padrao=N` na ESP32-CAM.
 */
data class PadraoSonoro(
    val frequenciaHz: Int,
    val ligadoMs: Int,
    val desligadoMs: Int,
    val volume: Double,
    val vibrar: Boolean,
    val codigoBuzzer: Int,
)

/**
 * DESATENTO e DISTRAÍDO usam o padrão do protótipo Python (PADROES_SONOROS["DESATENTO"]),
 * ainda uma proposta da equipe. O buzzer da ESP32 só conhece os códigos 1 e 2, então eles usam o 1.
 */
fun padraoPara(estado: EstadoMotorista): PadraoSonoro? = when (estado) {
    EstadoMotorista.CRITICO -> PadraoSonoro(2_000, 300, 100, 1.0, vibrar = true, codigoBuzzer = 2)
    EstadoMotorista.FADIGA -> PadraoSonoro(880, 200, 1_800, 0.6, vibrar = true, codigoBuzzer = 1)
    EstadoMotorista.DESATENTO, EstadoMotorista.DISTRAIDO ->
        PadraoSonoro(1_200, 150, 850, 0.8, vibrar = true, codigoBuzzer = 1)
    // Aviso de olhos fechando (1,5 a 3 s): mais grave e mais baixo, para não assustar numa piscada lenta.
    EstadoMotorista.ATENCAO -> PadraoSonoro(660, 120, 380, 0.5, vibrar = true, codigoBuzzer = 1)
    EstadoMotorista.ATENTO, EstadoMotorista.SEM_ROSTO -> null
}

/**
 * O que o app faz num alerta: o som (já escalado), a frase falada e de quanto em quanto tempo repeti-la.
 * [nivel] 0 ao entrar no alerta, 1 depois de 5 s e 2 depois de 10 s sem o motorista reagir.
 */
data class PlanoAlerta(val padrao: PadraoSonoro, val nivel: Int, val fala: String, val intervaloFalaMs: Long)

/** Segundos no estado para subir aos níveis 1 e 2. O CRÍTICO começa com 3 s de olho fechado: o nível 2
 *  chega aos 6 s, quando a Euro NCAP considera que o motorista não responde. */
private val ESCALADA_SEGUNDOS = mapOf(EstadoMotorista.CRITICO to (1.5 to 3.0))
private val ESCALADA_PADRAO = 5.0 to 10.0
private const val AUMENTO_VOLUME_POR_NIVEL = 0.2
private val INTERVALO_FALA_POR_NIVEL_MS = listOf(12_000L, 8_000L, 4_000L)

private val FALAS = mapOf(
    EstadoMotorista.CRITICO to "Acorde! Pare o carro em um lugar seguro.",
    EstadoMotorista.FADIGA to "Sinais de cansaço. Procure um lugar para descansar.",
    EstadoMotorista.DESATENTO to "Olhe para a estrada.",
    EstadoMotorista.DISTRAIDO to "Atenção, olhe para a frente.",
    EstadoMotorista.ATENCAO to "Atenção, abra os olhos.",
)

/** Depois de um CRÍTICO: um bipe suave a cada 20 s e a voz pedindo pausa a cada minuto. */
private val LEMBRETE_POS_SONO = PlanoAlerta(
    padrao = PadraoSonoro(660, 200, 19_800, 0.5, vibrar = false, codigoBuzzer = 0),
    nivel = 0,
    fala = "Você cochilou há pouco. Faça uma pausa assim que puder.",
    intervaloFalaMs = 60_000L,
)

/**
 * Alarme que escala quando o motorista não reage (ideia do monitoramento do openpilot):
 * a cada nível o volume sobe, o silêncio entre bipes cai pela metade e a voz repete mais vezes.
 * O aviso de olhos fechando não escala (dura no máximo 1,5 s) e a fadiga só de pós-sono vira lembrete.
 */
fun planoDeAlerta(estado: EstadoMotorista, motivos: Set<Motivo>, segundosNoEstado: Double): PlanoAlerta? {
    if (estado == EstadoMotorista.FADIGA && motivos == setOf(Motivo.POS_SONO)) return LEMBRETE_POS_SONO
    val base = padraoPara(estado) ?: return null
    val (nivel1, nivel2) = ESCALADA_SEGUNDOS[estado] ?: ESCALADA_PADRAO
    val nivel = when {
        estado == EstadoMotorista.ATENCAO -> 0
        segundosNoEstado >= nivel2 -> 2
        segundosNoEstado >= nivel1 -> 1
        else -> 0
    }
    val padrao = base.copy(
        volume = (base.volume + AUMENTO_VOLUME_POR_NIVEL * nivel).coerceAtMost(1.0),
        desligadoMs = base.desligadoMs / (nivel + 1),
    )
    return PlanoAlerta(padrao, nivel, FALAS.getValue(estado), INTERVALO_FALA_POR_NIVEL_MS[nivel])
}

/** Decide quando falar: ao entrar num alerta (ou trocar de alerta) e depois a cada intervalo do nível. */
class ControladorFala {
    private var estado: EstadoMotorista? = null
    private var ultimaFalaMs: Long? = null

    fun avaliar(agoraMs: Long, estadoAtual: EstadoMotorista, plano: PlanoAlerta?): String? {
        if (plano == null) {
            estado = null
            ultimaFalaMs = null
            return null
        }
        val novo = estadoAtual != estado
        val vencido = ultimaFalaMs?.let { agoraMs - it >= plano.intervaloFalaMs } ?: true
        estado = estadoAtual
        if (!novo && !vencido) return null
        ultimaFalaMs = agoraMs
        return plano.fala
    }
}

/**
 * Um ciclo do alarme em PCM 16 bits mono: tom durante [PadraoSonoro.ligadoMs] e
 * silêncio em seguida. O adaptador Android só escreve isto num AudioTrack em loop.
 */
fun gerarCicloPcm(padrao: PadraoSonoro, taxaHz: Int = 22_050): ShortArray {
    val amostrasLigado = taxaHz * padrao.ligadoMs / 1000
    val total = amostrasLigado + taxaHz * padrao.desligadoMs / 1000
    val amplitude = Short.MAX_VALUE * padrao.volume
    return ShortArray(total) { i ->
        if (i < amostrasLigado) (amplitude * sin(2 * PI * padrao.frequenciaHz * i / taxaHz)).toInt().toShort() else 0
    }
}

/** Alertas em que o motorista estava em perigo agora; ao sair deles, toca o som de "voltou". */
private val URGENTES = setOf(
    EstadoMotorista.CRITICO,
    EstadoMotorista.ATENCAO,
    EstadoMotorista.DESATENTO,
    EstadoMotorista.DISTRAIDO,
)

/** Toca o som de retorno ao sair de um alerta urgente para ATENTO ou FADIGA (rosto visto e olhos abertos). */
fun sinalDeRetorno(anterior: EstadoMotorista?, atual: EstadoMotorista): Boolean =
    anterior in URGENTES && (atual == EstadoMotorista.ATENTO || atual == EstadoMotorista.FADIGA)

private const val RETORNO_GRAVE_HZ = 660
private const val RETORNO_AGUDO_HZ = 990
private const val RETORNO_GRAVE_MS = 120
private const val RETORNO_AGUDO_MS = 180
private const val RETORNO_VOLUME = 0.7

/** Som curto de "voltou": duas notas subindo (660 Hz e 990 Hz), tocado uma vez só. */
fun gerarRetornoPcm(taxaHz: Int = 22_050): ShortArray {
    val grave = gerarCicloPcm(PadraoSonoro(RETORNO_GRAVE_HZ, RETORNO_GRAVE_MS, 0, RETORNO_VOLUME, false, 0), taxaHz)
    val agudo = gerarCicloPcm(PadraoSonoro(RETORNO_AGUDO_HZ, RETORNO_AGUDO_MS, 0, RETORNO_VOLUME, false, 0), taxaHz)
    return grave + agudo
}
