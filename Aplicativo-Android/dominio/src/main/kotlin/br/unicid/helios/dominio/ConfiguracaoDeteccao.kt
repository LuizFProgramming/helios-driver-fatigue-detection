package br.unicid.helios.dominio

/**
 * Todos os limiares do sistema, injetados no construtor do monitor (corrige o P13).
 * Tudo que é tempo está em milissegundos, nunca em frames (corrige o P2).
 *
 * Origem dos valores:
 *  - limiarEar 0,19: Medina (2025), índice de Youden sobre o Driver Inattention Detection Dataset.
 *  - limiarMar 0,50 e bocejo 500 ms: valores reais do config.py do protótipo (corrige o P1).
 *  - microssono 1 s e PERCLOS: Soukupová e Čech (2016); Dinges et al. (1998).
 *  - Os demais são pontos de partida e devem ser ajustados com os dados da Fase 5.
 */
data class ConfiguracaoDeteccao(
    // Olhos
    val limiarEar: Double = 0.19,
    val fatorLimiarCalibrado: Double = 0.65,
    val limiarEcf: Double = 0.40,
    val piscadaMinMs: Long = 60,
    val piscadaLongaMinMs: Long = 500,
    val microssonoMinMs: Long = 1_000,
    // Boca
    val limiarMar: Double = 0.50,
    val limiarMandibula: Double = 0.55,
    val bocejoMinMs: Long = 500,
    // Cabeça
    val quedaCabecaGraus: Double = 20.0,
    val cabecaCaidaMinMs: Long = 1_000,
    // PERCLOS
    val janelaPerclosCurtoMs: Long = 5_000,
    val limiarPerclosCritico: Double = 0.40,
    val janelaPerclosLongoMs: Long = 60_000,
    val limiarPerclosFadiga: Double = 0.15,
    val coberturaMinimaPerclos: Double = 0.5,
    val lacunaMaximaMs: Long = 1_000,
    // Frequência de eventos
    val janelaBocejosMs: Long = 300_000,
    val bocejosParaFadiga: Int = 3,
    val janelaPiscadasLongasMs: Long = 60_000,
    val piscadasLongasParaFadiga: Int = 3,
    // Sensores inerciais (aceleração linear, sem gravidade, em m/s²)
    val janelaMovimentoMs: Long = 2_000,
    val limiarMovimentoRms: Double = 0.35,
    val limiarEventoBrusco: Double = 4.0,
    val janelaEventoBruscoMs: Long = 10_000,
    // Exibição
    val alphaEma: Double = 0.25,
)
