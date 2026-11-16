package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.calibracao.Calibrador
import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.tipos.PoseCabeca

/**
 * Recalibração curta do início de cada viagem (ADR-013): espera o rosto estável por 1 s com o carro parado e
 * junta 5 s de amostras olhando a estrada. Até ela terminar vale a calibração do perfil. Se o carro andar no meio,
 * cancela; se faltarem amostras (rosto sumiu), recomeça. Calibra uma vez só por viagem. Não é thread-safe.
 */
class RecalibracaoViagem(private val cfg: ConfiguracaoInstalacao) {
    private class Pose(val t: Long, val yaw: Double, val pitch: Double)

    private val janela = ArrayDeque<Pose>()
    private var calibrador: Calibrador? = null

    var concluida = false
        private set

    fun progresso(t: Long): Double? = calibrador?.progresso(t)

    /** Devolve a calibração nova uma única vez, no quadro em que ela termina. */
    fun processar(t: Long, metricas: Metricas?, emMovimento: Boolean): Calibracao? {
        val pose = metricas?.pose
        if (concluida || emMovimento || pose == null) {
            janela.clear()
            if (emMovimento) calibrador = null
            return null
        }
        return coletar(t, metricas, pose)
    }

    private fun coletar(t: Long, m: Metricas, pose: PoseCabeca): Calibracao? {
        val c = calibrador ?: if (estavel(t, pose)) Calibrador(t, cfg.recalibracaoMs, cfg.minimoAmostras) else null
        calibrador = c
        c?.adicionar(
            m.earBruto, pose.pitchGraus, pose.yawGraus, m.earDireito, m.earEsquerdo,
            m.olhar.horizontal, m.olhar.vertical,
        )
        // Acabou o tempo: com amostras suficientes conclui; sem elas (rosto sumiu), espera de novo e recomeça.
        val resultado = c?.takeIf { it.progresso(t) >= 1.0 }?.let { fim ->
            calibrador = null
            fim.resultado(t)
        }
        concluida = resultado != null
        return resultado
    }

    /** Pose variando no máximo [ConfiguracaoInstalacao.rostoEstavelGraus] durante o último 1 s inteiro. */
    private fun estavel(t: Long, pose: PoseCabeca): Boolean {
        janela.addLast(Pose(t, pose.yawGraus, pose.pitchGraus))
        while (janela.size > 1 && t - janela[1].t >= cfg.rostoEstavelMs) janela.removeFirst()
        if (t - janela.first().t < cfg.rostoEstavelMs) return false
        val yaws = janela.map { it.yaw }
        val pitches = janela.map { it.pitch }
        val limite = cfg.rostoEstavelGraus
        return yaws.max() - yaws.min() <= limite && pitches.max() - pitches.min() <= limite
    }
}
