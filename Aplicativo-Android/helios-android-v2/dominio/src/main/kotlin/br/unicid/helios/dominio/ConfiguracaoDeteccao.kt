package br.unicid.helios.dominio

/**
 * Todos os limiares do sistema, injetados no construtor do monitor (corrige o P13).
 * Tudo que é tempo está em milissegundos, nunca em frames (corrige o P2).
 *
 * Origem dos valores:
 *  - limiarEar 0,19: Medina (2025), índice de Youden sobre o Driver Inattention Detection Dataset.
 *  - limiarMar 0,50 e bocejo 500 ms: valores reais do config.py do protótipo (corrige o P1).
 *  - Escada do olho fechado (Euro NCAP, protocolo de monitoramento do motorista): fechamento de 1 a 2 s é
 *    microssono, de 3 s ou mais é sono e de 6 s ou mais é motorista que não responde. O aviso de 1,5 s fica
 *    no meio do microssono; até lá é piscada lenta e só conta para a fadiga.
 *  - PERCLOS: Dinges et al. (1998).
 *  - Olhar fora da via (Euro NCAP, distração): um olhar único de 3 s ou mais é distração longa; somar 10 s
 *    fora em 30 s é distração curta (VATS). O app não distingue espelho de celular, então conta os dois.
 *  - Piscadas: duram em geral de 100 a 500 ms (Kleifges et al., 2017) e ficam mais longas e frequentes
 *    com a sonolência (McIntire et al., 2014). Os 250 ms de média são hipótese da equipe, sem validação.
 *  - Olhos semicerrados: abertura abaixo de 80% da calibrada em metade do último minuto. Hipótese da equipe.
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
    val avisoOlhoFechadoMs: Long = 1_500,
    val sonoMinMs: Long = 3_000,
    // Olho de lado: se um olho tiver menos de 60% da largura do outro, vale só o mais largo (mais visível).
    val razaoOlhoUnico: Double = 0.6,
    // Boca
    val limiarMar: Double = 0.50,
    val limiarMandibula: Double = 0.55,
    val bocejoMinMs: Long = 500,
    // Cabeça
    val quedaCabecaGraus: Double = 20.0,
    val cabecaCaidaMinMs: Long = 1_000,
    // Distração: cabeça virada para o lado (yaw) e rosto fora da câmera. Valores do protótipo Python
    // (GRAUS_PARA_SAIR_DA_FRENTE e SEGUNDOS_SEM_ROSTO_PARA_DESATENTO), ainda não validados com motoristas.
    val giroCabecaGraus: Double = 20.0,
    val giroCabecaMinMs: Long = 2_000,
    val semRostoParaDesatentoMs: Long = 2_000,
    // Olhar (íris e blendshapes), sempre em relação ao calibrado. Horizontal: deslocamento da íris em fração
    // da largura do olho. Vertical: diferença do blendshape de olhar para cima menos para baixo.
    val limiarOlharHorizontal: Double = 0.12,
    val limiarOlharVertical: Double = 0.35,
    val olharDesviadoMinMs: Long = 3_000,
    val janelaDistracaoMs: Long = 30_000,
    val distracaoAcumuladaMs: Long = 10_000,
    // Piscadas no último minuto: frequência e duração média
    val janelaPiscadasMs: Long = 60_000,
    val minimoPiscadasParaMedia: Int = 5,
    val duracaoMediaPiscadaFadigaMs: Double = 250.0,
    // Olhos semicerrados: abertura relativa baixa sem o músculo de apertar o olho (sol) ativo
    val fatorSemicerrado: Double = 0.80,
    val limiarMusculoSemicerrar: Double = 0.5,
    val janelaSemicerradoMs: Long = 60_000,
    val fracaoSemicerradoFadiga: Double = 0.5,
    // PERCLOS
    val janelaPerclosCurtoMs: Long = 5_000,
    val janelaPerclosLongoMs: Long = 60_000,
    val limiarPerclosFadiga: Double = 0.15,
    val coberturaMinimaPerclos: Double = 0.5,
    val lacunaMaximaMs: Long = 1_000,
    // Frequência de eventos
    val janelaBocejosMs: Long = 300_000,
    val bocejosParaFadiga: Int = 3,
    val janelaPiscadasLongasMs: Long = 60_000,
    val piscadasLongasParaFadiga: Int = 3,
    val janelaMicrossonosMs: Long = 300_000,
    val microssonosParaFadiga: Int = 2,
    // Depois de um CRÍTICO, o motorista fica em FADIGA por este tempo: quem cochilou continua em risco.
    val recuperacaoMs: Long = 120_000,
    // Sensores inerciais (aceleração linear, sem gravidade, em m/s²)
    // Histerese: liga "em movimento" acima de 0,5 e só desliga depois de 8 s seguidos abaixo de 0,25.
    // Assim celular na mão ou motor ligado parado não ficam ligando e desligando o menu.
    val janelaMovimentoMs: Long = 2_000,
    val limiarMovimentoRms: Double = 0.5,
    val limiarParadoRms: Double = 0.25,
    val paradoMinMs: Long = 8_000,
    val limiarEventoBrusco: Double = 4.0,
    val janelaEventoBruscoMs: Long = 10_000,
    // Exibição
    val alphaEma: Double = 0.25,
)
