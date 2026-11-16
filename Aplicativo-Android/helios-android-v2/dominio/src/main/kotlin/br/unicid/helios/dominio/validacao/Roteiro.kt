package br.unicid.helios.dominio.validacao

/** O que se espera do olho durante a tarefa; nulo quando ele abre e fecha (piscar) ou varia (bocejo). */
enum class OlhoEsperado { ABERTO, FECHADO }

/** Alerta que a tarefa deve disparar no app, pelos tempos da `ConfiguracaoDeteccao` padrão. */
enum class AlertaEsperado { NENHUM, MICROSSONO, SONO, DISTRACAO, BOCEJO }

/**
 * Tarefa do roteiro de teste (ADR-015): o que o motorista faz, por quanto tempo, e o que o app deve ver.
 * Fechar 2 s passa do microssono (1 s) sem chegar ao sono (3 s); fechar 4 s passa do sono; olhar de lado 5 s
 * passa dos 3 s de olhar desviado; espelhos e painel por 2 s ficam abaixo da tolerância das zonas (3 s).
 */
enum class Tarefa(val duracaoMs: Long, val olho: OlhoEsperado?, val alerta: AlertaEsperado) {
    ESTRADA(ESTRADA_MS, OlhoEsperado.ABERTO, AlertaEsperado.NENHUM),
    PISCAR(PISCAR_MS, null, AlertaEsperado.NENHUM),
    FECHAR_CURTO(OLHADA_MS, OlhoEsperado.FECHADO, AlertaEsperado.MICROSSONO),
    FECHAR_LONGO(FECHAR_LONGO_MS, OlhoEsperado.FECHADO, AlertaEsperado.SONO),
    ESPELHO_ESQUERDO(OLHADA_MS, OlhoEsperado.ABERTO, AlertaEsperado.NENHUM),
    ESPELHO_DIREITO(OLHADA_MS, OlhoEsperado.ABERTO, AlertaEsperado.NENHUM),
    PAINEL(OLHADA_MS, OlhoEsperado.ABERTO, AlertaEsperado.NENHUM),
    OLHAR_DE_LADO(OLHAR_DE_LADO_MS, OlhoEsperado.ABERTO, AlertaEsperado.DISTRACAO),
    BOCEJAR(ESTRADA_MS, null, AlertaEsperado.BOCEJO),
}

private const val ESTRADA_MS = 6_000L
private const val PISCAR_MS = 8_000L
private const val OLHADA_MS = 2_000L
private const val FECHAR_LONGO_MS = 4_000L
private const val OLHAR_DE_LADO_MS = 5_000L

/** Depois de cada tarefa com alerta, volta para a estrada: o alerta acaba antes da próxima tarefa. */
val ROTEIRO_PADRAO = listOf(
    Tarefa.ESTRADA, Tarefa.PISCAR, Tarefa.ESTRADA, Tarefa.FECHAR_CURTO, Tarefa.ESTRADA, Tarefa.FECHAR_LONGO,
    Tarefa.ESTRADA, Tarefa.ESPELHO_ESQUERDO, Tarefa.ESPELHO_DIREITO, Tarefa.PAINEL, Tarefa.ESTRADA,
    Tarefa.OLHAR_DE_LADO, Tarefa.ESTRADA, Tarefa.BOCEJAR, Tarefa.ESTRADA,
)

/** Onde o roteiro está: a tarefa [indice], no preparo (contagem antes dela) ou já valendo, e quanto falta. */
data class PassoRoteiro(val indice: Int, val tarefa: Tarefa, val preparando: Boolean, val restanteMs: Long)

/** Intervalo em que a tarefa valeu, sem o preparo: vira uma linha do `roteiro.csv`. */
data class IntervaloRoteiro(val tarefa: Tarefa, val inicioMs: Long, val fimMs: Long)

/**
 * Roteiro de teste guiado: [preparoMs] de aviso antes de cada tarefa, para o rótulo não pegar o tempo de reação.
 * Puro: tudo sai do tempo decorrido desde o início, então não guarda estado.
 */
class Roteiro(val tarefas: List<Tarefa> = ROTEIRO_PADRAO, private val preparoMs: Long = PREPARO_PADRAO_MS) {

    val duracaoMs: Long = tarefas.sumOf { preparoMs + it.duracaoMs }

    /** Passo em [decorridoMs] desde o início; nulo depois da última tarefa. */
    fun passoEm(decorridoMs: Long): PassoRoteiro? {
        var fimAnterior = 0L
        for (i in tarefas.indices) {
            val comeca = fimAnterior + preparoMs
            val fim = comeca + tarefas[i].duracaoMs
            if (decorridoMs < fim) {
                val preparando = decorridoMs < comeca
                return PassoRoteiro(i, tarefas[i], preparando, (if (preparando) comeca else fim) - decorridoMs)
            }
            fimAnterior = fim
        }
        return null
    }

    /** Tarefa valendo em [decorridoMs]; nula no preparo e depois do fim. */
    fun tarefaEm(decorridoMs: Long): Tarefa? {
        val passo = passoEm(decorridoMs)
        return if (passo == null || passo.preparando) null else passo.tarefa
    }

    fun intervalos(inicioMs: Long): List<IntervaloRoteiro> {
        var fimAnterior = inicioMs
        return tarefas.map { t ->
            val comeca = fimAnterior + preparoMs
            fimAnterior = comeca + t.duracaoMs
            IntervaloRoteiro(t, comeca, fimAnterior)
        }
    }

    private companion object {
        const val PREPARO_PADRAO_MS = 3_000L
    }
}
