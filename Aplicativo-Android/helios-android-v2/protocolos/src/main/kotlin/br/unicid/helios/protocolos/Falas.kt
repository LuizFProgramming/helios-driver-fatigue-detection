package br.unicid.helios.protocolos

import br.unicid.helios.dominio.tipos.EstadoMotorista

object Falas {
    val ATENCAO = listOf(
        "Atenção, abra os olhos.",
        "Abra os olhos!",
        "Olhos abertos, motorista.",
        "Atenção, olhos na estrada.",
        "Não feche os olhos.",
        "Fique de olhos abertos.",
        "Olhos abertos, por favor.",
    )

    val CRITICO = listOf(
        "Acorde! Pare o carro em um lugar seguro.",
        "Perigo! Pare o carro agora!",
        "Risco de acidente! Pare imediatamente!",
        "Alerta máximo! Encoste o veículo agora!",
        "Olhos fechando! Pare o carro agora!",
        "Pare agora, é uma emergência!",
        "Cuidado! Reduza e encoste com segurança!",
        "Sono extremo! Estacione imediatamente!",
    )

    val FADIGA = listOf(
        "Sinais de cansaço. Procure um lugar para descansar.",
        "Você parece cansado. Procure um local seguro para descansar.",
        "Seu cansaço está aumentando. Descansar agora é mais seguro.",
        "Cansaço detectado. Que tal parar e esticar as pernas?",
        "Seus olhos mostram fadiga. Pare assim que for seguro.",
        "Dirigir cansado é arriscado. Faça uma parada em breve.",
        "Fadiga à vista. Procure um lugar seguro para parar.",
        "Atenção redobrada: você está ficando cansado. Pare em breve.",
    )

    val DESATENTO = listOf(
        "Olhe para a estrada.",
        "Você desviou o olhar da pista.",
        "Volte os olhos para a frente.",
        "Atenção, você está desatento.",
        "Mantenha os olhos na via.",
        "Cabeça erguida, olhos na estrada.",
        "Foque na estrada à frente.",
    )

    val DISTRAIDO = listOf(
        "Atenção, olhe para a frente.",
        "Distração detectada. Foque na direção.",
        "Concentre-se na condução, por favor.",
        "Deixe distrações de lado agora.",
        "Você parece distraído. Atenção!",
        "Volte sua atenção para dirigir.",
        "Foco total na estrada, motorista.",
    )

    val POS_SONO = listOf(
        "Você cochilou há pouco. Faça uma pausa assim que puder.",
        "Você cochilou há pouco. Uma pausa agora pode evitar um acidente.",
        "Lembrete: faça uma pausa longa antes de seguir viagem.",
        "Sua atenção voltou, mas o cansaço não. Faça uma pausa.",
        "Que tal uma pausa para um café e uma caminhada?",
        "Depois do cochilo, uma pausa curta faz diferença.",
    )

    val POR_ESTADO = mapOf(
        EstadoMotorista.ATENCAO to ATENCAO,
        EstadoMotorista.CRITICO to CRITICO,
        EstadoMotorista.FADIGA to FADIGA,
        EstadoMotorista.DESATENTO to DESATENTO,
        EstadoMotorista.DISTRAIDO to DISTRAIDO,
    )

    val LIMITE_PALAVRAS = mapOf(
        EstadoMotorista.ATENCAO to 4,
        EstadoMotorista.CRITICO to 8,
        EstadoMotorista.FADIGA to 12,
        EstadoMotorista.DESATENTO to 7,
        EstadoMotorista.DISTRAIDO to 7,
    )

    const val LIMITE_PALAVRAS_POS_SONO = 14
}
