package br.unicid.helios.dominio.classificacao

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo

/** Tudo que a etapa "Análise dos sinais" produz para um frame. */
data class Indicadores(
    /** Há quanto tempo os olhos estão fechados sem parar (0 se abertos). */
    val olhosFechadosMs: Long,
    val cabecaCaida: Boolean,
    val perclosLongo: Double,
    val bocejosRecentes: Int,
    val piscadasLongasRecentes: Int,
    val microssonosRecentes: Int = 0,
    val eventoBruscoRecente: Boolean = false,
    val cabecaVirada: Boolean = false,
    /** O motorista saiu de um CRÍTICO há menos de [ConfiguracaoDeteccao.recuperacaoMs]. */
    val posSono: Boolean = false,
    /** Há quanto tempo o olhar está fora da via sem parar (íris ou olhar vertical). */
    val olharDesviadoMs: Long = 0,
    /** Tempo somado com olhar ou cabeça fora da via nos últimos 30 s. */
    val foraDaViaMs: Long = 0,
    val piscadasNoMinuto: Int = 0,
    val duracaoMediaPiscadaMs: Double = 0.0,
    /** Fração do último minuto com os olhos semicerrados. */
    val fracaoSemicerrado: Double = 0.0,
)

data class Classificacao(val estado: EstadoMotorista, val motivos: Set<Motivo>)

/**
 * Regras da etapa "Classificação do estado", em ordem de prioridade.
 *
 * CRÍTICO:   olhos fechados por 3 s ou mais (sono, na Euro NCAP), cabeça caída, ou
 *            sinais de fadiga somados a um movimento brusco do carro.
 * ATENÇÃO:   olhos fechados entre 1,5 s e 3 s: aviso curto antes do alarme forte. Abaixo de 1,5 s
 *            é piscada lenta e só entra na conta da fadiga.
 * DISTRAÍDO: cabeça virada para o lado por tempo demais, olhar fora da via por 3 s seguidos
 *            ou 10 s somados em 30 s (Euro NCAP).
 * FADIGA:    sinais acumulados ao longo de minutos (PERCLOS de 60 s, bocejos, piscadas longas,
 *            microssonos, piscadas lentas em média, olhos semicerrados) ou os 2 minutos seguintes a um CRÍTICO.
 * ATENTO:    nenhum dos anteriores.
 *
 * DESATENTO (rosto fora da câmera) não passa por aqui: sem rosto não há indicadores,
 * então o [br.unicid.helios.dominio.monitor.MonitorMotorista] decide esse caso.
 */
class Classificador(private val cfg: ConfiguracaoDeteccao) {

    fun classificar(ind: Indicadores): Classificacao {
        val criticos = motivosCriticos(ind)
        val fadiga = motivosDeFadiga(ind)
        // O pós-sono sozinho não soma com um solavanco: o motorista já está acordado e um buraco não é cochilo.
        val agravante = if ((fadiga - Motivo.POS_SONO).isNotEmpty() && ind.eventoBruscoRecente) {
            setOf(Motivo.EVENTO_BRUSCO_COM_FADIGA)
        } else {
            emptySet()
        }
        val aviso = if (ind.olhosFechadosMs in cfg.avisoOlhoFechadoMs until cfg.sonoMinMs) {
            setOf(Motivo.OLHOS_FECHANDO)
        } else {
            emptySet()
        }
        val distracao = motivosDeDistracao(ind)
        val estado = when {
            criticos.isNotEmpty() || agravante.isNotEmpty() -> EstadoMotorista.CRITICO
            aviso.isNotEmpty() -> EstadoMotorista.ATENCAO
            distracao.isNotEmpty() -> EstadoMotorista.DISTRAIDO
            fadiga.isNotEmpty() -> EstadoMotorista.FADIGA
            else -> EstadoMotorista.ATENTO
        }
        return Classificacao(estado, criticos + agravante + aviso + distracao + fadiga)
    }

    private fun motivosCriticos(ind: Indicadores): Set<Motivo> = buildSet {
        if (ind.olhosFechadosMs >= cfg.sonoMinMs) add(Motivo.SONO)
        if (ind.cabecaCaida) add(Motivo.CABECA_CAIDA)
    }

    private fun motivosDeDistracao(ind: Indicadores): Set<Motivo> = buildSet {
        if (ind.cabecaVirada) add(Motivo.CABECA_VIRADA)
        if (ind.olharDesviadoMs >= cfg.olharDesviadoMinMs) add(Motivo.OLHAR_DESVIADO)
        if (ind.foraDaViaMs >= cfg.distracaoAcumuladaMs) add(Motivo.DISTRACAO_ACUMULADA)
    }

    private fun motivosDeFadiga(ind: Indicadores): Set<Motivo> = buildSet {
        if (ind.perclosLongo >= cfg.limiarPerclosFadiga) add(Motivo.PERCLOS_FADIGA)
        if (ind.bocejosRecentes >= cfg.bocejosParaFadiga) add(Motivo.BOCEJOS_FREQUENTES)
        if (ind.piscadasLongasRecentes >= cfg.piscadasLongasParaFadiga) add(Motivo.PISCADAS_LONGAS_FREQUENTES)
        if (ind.microssonosRecentes >= cfg.microssonosParaFadiga) add(Motivo.MICROSSONOS_FREQUENTES)
        if (ind.posSono) add(Motivo.POS_SONO)
        if (piscadasLentas(ind)) add(Motivo.PISCADAS_LENTAS)
        if (ind.fracaoSemicerrado >= cfg.fracaoSemicerradoFadiga) add(Motivo.OLHOS_SEMICERRADOS)
    }

    private fun piscadasLentas(ind: Indicadores) =
        ind.piscadasNoMinuto >= cfg.minimoPiscadasParaMedia && ind.duracaoMediaPiscadaMs >= cfg.duracaoMediaPiscadaFadigaMs
}
