package br.unicid.helios.dominio.classificacao

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo

/** Tudo que a etapa "Análise dos sinais" produz para um frame. */
data class Indicadores(
    val microssonoEmCurso: Boolean,
    val cabecaCaida: Boolean,
    val perclosCurto: Double,
    val perclosLongo: Double,
    val bocejosRecentes: Int,
    val piscadasLongasRecentes: Int,
    val eventoBruscoRecente: Boolean,
)

data class Classificacao(val estado: EstadoMotorista, val motivos: Set<Motivo>)

/**
 * Regras da etapa "Classificação do estado".
 *
 * CRÍTICO: perigo imediato (microssono, cabeça caída, PERCLOS de 5 s alto) ou
 *          sinais de fadiga somados a um movimento brusco do carro.
 * FADIGA:  sinais acumulados ao longo de minutos (PERCLOS de 60 s, bocejos, piscadas longas).
 * ATENTO:  nenhum dos anteriores.
 *
 * Quando o classificador treinado da Fase 6 existir, ele substitui só [motivosDeFadiga].
 */
class Classificador(private val cfg: ConfiguracaoDeteccao) {

    fun classificar(ind: Indicadores): Classificacao {
        val criticos = motivosCriticos(ind)
        val fadiga = motivosDeFadiga(ind)
        val agravante = if (fadiga.isNotEmpty() && ind.eventoBruscoRecente) {
            setOf(Motivo.EVENTO_BRUSCO_COM_FADIGA)
        } else {
            emptySet()
        }
        val estado = when {
            criticos.isNotEmpty() || agravante.isNotEmpty() -> EstadoMotorista.CRITICO
            fadiga.isNotEmpty() -> EstadoMotorista.FADIGA
            else -> EstadoMotorista.ATENTO
        }
        return Classificacao(estado, criticos + agravante + fadiga)
    }

    private fun motivosCriticos(ind: Indicadores): Set<Motivo> = buildSet {
        if (ind.microssonoEmCurso) add(Motivo.MICROSSONO)
        if (ind.cabecaCaida) add(Motivo.CABECA_CAIDA)
        if (ind.perclosCurto >= cfg.limiarPerclosCritico) add(Motivo.PERCLOS_CRITICO)
    }

    private fun motivosDeFadiga(ind: Indicadores): Set<Motivo> = buildSet {
        if (ind.perclosLongo >= cfg.limiarPerclosFadiga) add(Motivo.PERCLOS_FADIGA)
        if (ind.bocejosRecentes >= cfg.bocejosParaFadiga) add(Motivo.BOCEJOS_FREQUENTES)
        if (ind.piscadasLongasRecentes >= cfg.piscadasLongasParaFadiga) {
            add(Motivo.PISCADAS_LONGAS_FREQUENTES)
        }
    }
}
