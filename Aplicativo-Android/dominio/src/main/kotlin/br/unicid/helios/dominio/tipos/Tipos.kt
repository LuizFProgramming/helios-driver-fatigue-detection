package br.unicid.helios.dominio.tipos

/** Ponto na imagem, em pixels. */
data class Ponto2D(val x: Double, val y: Double)

/**
 * Seis pontos na ordem de Soukupová e Čech (2016): p1 e p4 são os cantos,
 * (p2, p6) e (p3, p5) são os pares verticais opostos. Serve para olho e boca.
 */
data class SeisPontos(
    val p1: Ponto2D,
    val p2: Ponto2D,
    val p3: Ponto2D,
    val p4: Ponto2D,
    val p5: Ponto2D,
    val p6: Ponto2D,
)

/** Rotação da cabeça em graus. Pitch negativo = queixo descendo. */
data class PoseCabeca(val pitchGraus: Double, val yawGraus: Double, val rollGraus: Double)

/**
 * O que o detector facial entrega para um frame com rosto.
 * Os valores de piscada e mandíbula são os blendshapes do MediaPipe (0 = aberto, 1 = fechado).
 */
data class AmostraFacial(
    val olhoDireito: SeisPontos,
    val olhoEsquerdo: SeisPontos,
    val boca: SeisPontos,
    val piscadaDireita: Double,
    val piscadaEsquerda: Double,
    val aberturaMandibula: Double,
    val pose: PoseCabeca?,
)

/** Um frame já processado pelo detector. [amostra] nulo = nenhum rosto encontrado. */
data class Quadro(val timestampMs: Long, val amostra: AmostraFacial?)

/**
 * Estados da etapa "Classificação do estado" do pipeline.
 * O pipeline do TCC chama o primeiro de "alerta"; aqui ele é ATENTO para não
 * confundir com o alarme sonoro, que é a etapa seguinte.
 */
enum class EstadoMotorista { ATENTO, FADIGA, CRITICO, SEM_ROSTO }

/** Por que o estado foi escolhido. Vai para a tela e para o histórico. */
enum class Motivo {
    MICROSSONO,
    CABECA_CAIDA,
    PERCLOS_CRITICO,
    PERCLOS_FADIGA,
    BOCEJOS_FREQUENTES,
    PISCADAS_LONGAS_FREQUENTES,
    EVENTO_BRUSCO_COM_FADIGA,
}

enum class TipoEvento { PISCADA, PISCADA_LONGA, MICROSSONO, BOCEJO, CABECA_CAIDA, EVENTO_BRUSCO }

/** Evento encerrado, pronto para o histórico (RF08). Nenhuma imagem, só números. */
data class Evento(val tipo: TipoEvento, val inicioMs: Long, val duracaoMs: Long)
