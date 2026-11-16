package br.unicid.helios.dominio.instalacao

import br.unicid.helios.dominio.tipos.CaixaRosto
import br.unicid.helios.dominio.tipos.PoseCabeca
import kotlin.math.abs

/** Onde a câmera fica presa no carro (ADR-013). */
enum class PosicaoCamera { CENTRO_PAINEL, COLUNA_VOLANTE, PILAR_A, RETROVISOR }

/** Lado do motorista no carro; esquerdo no Brasil. Nomeia os espelhos nas zonas de olhar (Parte C). */
enum class LadoMotorista { ESQUERDO, DIREITO }

/** O que o assistente mede num quadro: rosto, brilho do rosto (0 a 255), pose e RMS da aceleração (m/s²). */
data class AmostraInstalacao(
    val caixa: CaixaRosto?,
    val brilho: Double?,
    val pose: PoseCabeca?,
    val rmsAceleracao: Double,
)

/** As cinco verificações do enquadramento, na ordem em que aparecem na tela. */
enum class Verificacao { ROSTO_INTEIRO, TAMANHO, LUZ, ANGULO, CAMERA_PARADA }

/** Como corrigir uma verificação que falhou. A tela transforma em texto curto. */
enum class Dica { CENTRALIZAR, APROXIMAR, AFASTAR, MAIS_LUZ, MENOS_LUZ, SUBIR, ABAIXAR, MAIS_A_FRENTE, FIXAR }

data class ResultadoVerificacao(val verificacao: Verificacao, val dica: Dica?) {
    val ok: Boolean get() = dica == null
}

/**
 * Faixa aceitável da pose com o motorista olhando a estrada. [yawMax] vale para os dois lados, porque o sinal do
 * yaw depende de a imagem vir espelhada. Pitch negativo é câmera abaixo do rosto (teste de 16/11: -21 graus).
 */
data class FaixaAngulo(val yawMax: Double, val pitchMin: Double, val pitchMax: Double)

/**
 * Limiares da instalação e da calibração (ADR-013). Todos são hipóteses da equipe, a validar no carro:
 * rosto de 20% a 60% da largura da imagem, brilho do rosto de 60 a 220, câmera parada com o mesmo RMS da
 * histerese de movimento (0,25 m/s²) e as faixas de ângulo por posição.
 */
data class ConfiguracaoInstalacao(
    val margemQuadro: Double = 0.02,
    val larguraRostoMin: Double = 0.20,
    val larguraRostoMax: Double = 0.60,
    val brilhoMin: Double = 60.0,
    val brilhoMax: Double = 220.0,
    val rmsParado: Double = 0.25,
    val faixas: Map<PosicaoCamera, FaixaAngulo> = mapOf(
        PosicaoCamera.COLUNA_VOLANTE to FaixaAngulo(yawMax = 15.0, pitchMin = -35.0, pitchMax = 0.0),
        PosicaoCamera.CENTRO_PAINEL to FaixaAngulo(yawMax = 45.0, pitchMin = -30.0, pitchMax = 5.0),
        PosicaoCamera.PILAR_A to FaixaAngulo(yawMax = 45.0, pitchMin = -15.0, pitchMax = 20.0),
        PosicaoCamera.RETROVISOR to FaixaAngulo(yawMax = 35.0, pitchMin = -5.0, pitchMax = 35.0),
    ),
    /** Tempo com as cinco verificações certas antes de liberar a calibração. */
    val enquadramentoOkMs: Long = 1_000,
    val calibracaoMs: Long = 10_000,
    val recalibracaoMs: Long = 5_000,
    val minimoAmostras: Int = 15,
    /** Espera para o motorista levar os olhos até a zona, e o tempo de coleta dela. */
    val esperaZonaMs: Long = 1_000,
    val zonaMs: Long = 2_000,
    val minimoAmostrasZona: Int = 10,
    /** Espalhamento da zona: 2 desvios padrão, com mínimo de 5 graus e 0,05 de olhar. */
    val fatorEspalhamento: Double = 2.0,
    val espalhamentoMinGraus: Double = 5.0,
    val espalhamentoMinOlhar: Double = 0.05,
    /** Estabilidade na nota: desvio de até 3 graus vale tudo; de 8 ou mais, nada. */
    val desvioBomGraus: Double = 3.0,
    val desvioRuimGraus: Double = 8.0,
    val notaBoa: Int = 80,
    val notaMinima: Int = 60,
    /** Rosto estável para a recalibração da viagem: pose variando até 3 graus por 1 s. */
    val rostoEstavelMs: Long = 1_000,
    val rostoEstavelGraus: Double = 3.0,
)

/** As cinco verificações do quadro (ADR-013). Sem rosto, todas as que dependem dele pedem para centralizar. */
fun verificar(a: AmostraInstalacao, posicao: PosicaoCamera, cfg: ConfiguracaoInstalacao): List<ResultadoVerificacao> {
    val caixa = a.caixa
    val pose = a.pose
    val brilho = a.brilho
    return listOf(
        ResultadoVerificacao(
            Verificacao.ROSTO_INTEIRO,
            if (caixa == null || !dentro(caixa, cfg)) Dica.CENTRALIZAR else null,
        ),
        ResultadoVerificacao(Verificacao.TAMANHO, if (caixa == null) Dica.CENTRALIZAR else tamanho(caixa, cfg)),
        ResultadoVerificacao(Verificacao.LUZ, if (brilho == null) Dica.CENTRALIZAR else luz(brilho, cfg)),
        ResultadoVerificacao(
            Verificacao.ANGULO,
            if (pose == null) Dica.CENTRALIZAR else angulo(pose, cfg.faixas.getValue(posicao)),
        ),
        ResultadoVerificacao(Verificacao.CAMERA_PARADA, if (a.rmsAceleracao > cfg.rmsParado) Dica.FIXAR else null),
    )
}

private fun dentro(c: CaixaRosto, cfg: ConfiguracaoInstalacao): Boolean {
    val m = cfg.margemQuadro
    return c.x0 >= m && c.y0 >= m && c.x1 <= 1 - m && c.y1 <= 1 - m
}

private fun tamanho(c: CaixaRosto, cfg: ConfiguracaoInstalacao): Dica? = when {
    c.largura < cfg.larguraRostoMin -> Dica.APROXIMAR
    c.largura > cfg.larguraRostoMax -> Dica.AFASTAR
    else -> null
}

private fun luz(brilho: Double, cfg: ConfiguracaoInstalacao): Dica? = when {
    brilho < cfg.brilhoMin -> Dica.MAIS_LUZ
    brilho > cfg.brilhoMax -> Dica.MENOS_LUZ
    else -> null
}

/** Giro grande: a câmera está muito de lado. Pitch baixo: câmera baixa demais (sobe); alto: alta demais (desce). */
private fun angulo(p: PoseCabeca, faixa: FaixaAngulo): Dica? = when {
    abs(p.yawGraus) > faixa.yawMax -> Dica.MAIS_A_FRENTE
    p.pitchGraus < faixa.pitchMin -> Dica.SUBIR
    p.pitchGraus > faixa.pitchMax -> Dica.ABAIXAR
    else -> null
}
