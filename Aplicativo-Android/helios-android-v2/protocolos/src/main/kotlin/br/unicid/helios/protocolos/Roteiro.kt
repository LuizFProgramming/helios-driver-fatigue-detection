package br.unicid.helios.protocolos

import br.unicid.helios.dominio.monitor.Metricas
import br.unicid.helios.dominio.tipos.RecortesOlhos
import br.unicid.helios.dominio.validacao.OlhoEsperado
import br.unicid.helios.dominio.validacao.PassoRoteiro
import br.unicid.helios.dominio.validacao.Tarefa
import java.time.ZoneId
import java.util.Base64

/** Textos da tela e da voz do roteiro de teste (ADR-015). */
object TextosRoteiro {
    fun instrucao(t: Tarefa): String = when (t) {
        Tarefa.ESTRADA -> "Olhe para a estrada"
        Tarefa.PISCAR -> "Pisque normalmente, umas 5 vezes"
        Tarefa.FECHAR_CURTO -> "Feche os olhos e abra no bipe"
        Tarefa.FECHAR_LONGO -> "Feche os olhos por mais tempo, até o bipe"
        Tarefa.ESPELHO_ESQUERDO -> "Olhe para o espelho esquerdo"
        Tarefa.ESPELHO_DIREITO -> "Olhe para o espelho direito"
        Tarefa.PAINEL -> "Olhe para o painel de instrumentos"
        Tarefa.OLHAR_DE_LADO -> "Vire o rosto para a janela do lado e fique assim"
        Tarefa.BOCEJAR -> "Boceje ou abra bem a boca"
    }

    /** Dito e mostrado na contagem antes da tarefa. */
    fun preparo(t: Tarefa): String {
        val texto = instrucao(t)
        return buildString {
            append("A seguir: ")
            append(texto[0].lowercaseChar())
            append(texto.drop(1))
        }
    }
}

/** O que o app faz numa troca de passo do roteiro: falar a próxima tarefa ou tocar o bipe curto. */
sealed interface AvisoRoteiro {
    data class Falar(val texto: String) : AvisoRoteiro
    data object Bipe : AvisoRoteiro
}

/**
 * Avisos entre o passo [anterior] e o [atual] (nulo: fora do roteiro ou terminado). A voz anuncia a tarefa no
 * preparo, o bipe marca o começo de cada tarefa e o fim das de olho fechado, para a pessoa saber quando abrir.
 */
fun avisosDoRoteiro(anterior: PassoRoteiro?, atual: PassoRoteiro?): List<AvisoRoteiro> = buildList {
    val saiuDeOlhoFechado = anterior != null && !anterior.preparando && anterior.tarefa.olho == OlhoEsperado.FECHADO
    val trocouDeTarefa = anterior?.indice != atual?.indice
    if (trocouDeTarefa && saiuDeOlhoFechado) add(AvisoRoteiro.Bipe)
    when {
        atual == null -> if (anterior != null) add(AvisoRoteiro.Falar(CONCLUIDO))
        atual.preparando -> if (trocouDeTarefa) add(AvisoRoteiro.Falar(TextosRoteiro.preparo(atual.tarefa)))
        anterior == null || anterior.preparando -> add(AvisoRoteiro.Bipe)
    }
}

private const val CONCLUIDO = "Roteiro concluído"

private const val CASAS = 4

/**
 * `recortes.csv` (ADR-015): os recortes 64x64 em cinza que a CNN recebe, com o rótulo da tarefa do roteiro, para
 * treinar e testar a rede com os dados do grupo. Só com o consentimento do roteiro; nunca fora dele.
 */
object RegistroRecortes {
    const val CABECALHO = "inicio,tarefa,olho_esperado,oculos,olho,ear,cnn,pixels"

    /** Duas linhas do mesmo quadro: olho direito e esquerdo; pixels são os 4096 bytes do recorte em base64. */
    fun linhas(epochMs: Long, tarefa: Tarefa, oculos: Boolean, m: Metricas?, r: RecortesOlhos, zona: ZoneId): String {
        val olho = tarefa.olho
        val comum = listOf(
            Csv.hora(epochMs, zona), tarefa.name, if (olho == null) "" else olho.name, if (oculos) "1" else "0",
        ).joinToString(",")
        return linha(comum, "dir", m?.earDireito, m?.cnnFechadoDireito, r.direito) +
            linha(comum, "esq", m?.earEsquerdo, m?.cnnFechadoEsquerdo, r.esquerdo)
    }

    private fun linha(comum: String, olho: String, ear: Double?, cnn: Double?, pixels: ByteArray) = buildString {
        append(comum).append(',').append(olho).append(',').append(num(ear)).append(',').append(num(cnn)).append(',')
        append(Base64.getEncoder().encodeToString(pixels))
        append('\n')
    }

    private fun num(v: Double?) = if (v == null) "" else Csv.decimal(v, CASAS)
}
