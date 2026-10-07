package br.unicid.helios.protocolos

import br.unicid.helios.dominio.tipos.EstadoMotorista
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

fun padraoPara(estado: EstadoMotorista): PadraoSonoro? = when (estado) {
    EstadoMotorista.CRITICO -> PadraoSonoro(2_000, 300, 100, 1.0, vibrar = true, codigoBuzzer = 2)
    EstadoMotorista.FADIGA -> PadraoSonoro(880, 200, 1_800, 0.6, vibrar = true, codigoBuzzer = 1)
    EstadoMotorista.ATENTO, EstadoMotorista.SEM_ROSTO -> null
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
