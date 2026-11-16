package br.unicid.helios.protocolos

import br.unicid.helios.dominio.calibracao.Calibracao
import br.unicid.helios.dominio.calibracao.Desvios
import br.unicid.helios.dominio.instalacao.Dica
import br.unicid.helios.dominio.instalacao.LadoMotorista
import br.unicid.helios.dominio.instalacao.PartesNota
import br.unicid.helios.dominio.instalacao.PerfilInstalacao
import br.unicid.helios.dominio.instalacao.PosicaoCamera
import br.unicid.helios.dominio.instalacao.Qualidade
import br.unicid.helios.dominio.instalacao.Verificacao
import br.unicid.helios.dominio.instalacao.Zona
import br.unicid.helios.dominio.instalacao.ZonaOlhar
import br.unicid.helios.dominio.tipos.CaixaRosto
import kotlin.math.ceil
import kotlin.math.floor

private const val NOTA_MAXIMA = 100
private const val PASSOS_NO_LADO = 32
private const val CAMPOS_ZONA = 8

/**
 * Perfis de instalação em texto, um "chave=valor" por linha (ADR-013): guardados nas preferências do app e no
 * arquivo de backup. O backup vem de fora do app, então a leitura confere cada campo e descarta o perfil que
 * tiver algum valor inválido, sem perder os outros. Chaves desconhecidas são ignoradas.
 */
object ArquivoPerfis {
    const val CABECALHO = "helios-perfis 1"

    fun escrever(perfis: List<PerfilInstalacao>): String = buildString {
        appendLine(CABECALHO)
        perfis.forEach { p ->
            appendLine("perfil")
            campos(p).forEach { (chave, valor) -> appendLine("$chave=$valor") }
            p.zonas.forEach { appendLine("zona=${zona(it)}") }
            appendLine("fim")
        }
    }

    fun ler(texto: String): List<PerfilInstalacao> {
        val linhas = texto.lines().map { it.trim() }
        return if (linhas.firstOrNull() == CABECALHO) blocos(linhas.drop(1)).mapNotNull(::perfil) else emptyList()
    }

    /** As linhas entre cada "perfil" e o "fim" seguinte; um "fim" sem "perfil" antes é ignorado. */
    private fun blocos(linhas: List<String>): List<List<String>> {
        val blocos = mutableListOf<List<String>>()
        var bloco: MutableList<String>? = null
        for (linha in linhas) {
            when (linha) {
                "perfil" -> bloco = mutableListOf()
                "fim" -> {
                    bloco?.let(blocos::add)
                    bloco = null
                }
                else -> bloco?.add(linha)
            }
        }
        return blocos
    }

    private fun campos(p: PerfilInstalacao): List<Pair<String, Any?>> {
        val c = p.calibracao
        return listOf(
            "id" to p.id, "nome" to p.nome.replace(Regex("[\\r\\n]+"), " "), "posicao" to p.posicao, "lado" to p.lado,
            "criado" to p.criadoEpochMs, "nota" to p.nota, "verificacoes_ok" to p.fracaoVerificacoesOk,
            "ear" to c.earReferencia, "pitch" to c.pitchReferencia, "yaw" to c.yawReferencia,
            "ear_dir" to c.earDireito, "ear_esq" to c.earEsquerdo,
            "olhar_h" to c.olharHorizontal, "olhar_v" to c.olharVertical,
            "desvio_pitch" to p.desvios.pitch, "desvio_yaw" to p.desvios.yaw,
            "desvio_olhar_h" to p.desvios.olharHorizontal, "desvio_olhar_v" to p.desvios.olharVertical,
        ).map { (chave, valor) -> chave to (valor ?: "") }
    }

    private fun zona(z: ZonaOlhar) = listOf(
        z.zona, z.yaw, z.pitch, z.olharHorizontal ?: "", z.olharVertical ?: "",
        z.espalhamentoYaw, z.espalhamentoPitch, z.espalhamentoOlhar,
    ).joinToString(";")

    /** Nulo se algum campo obrigatório faltar ou for inválido. */
    private fun perfil(linhas: List<String>): PerfilInstalacao? = runCatching {
        val c = linhas.filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }
        val zonas = linhas.filter { it.startsWith("zona=") }.map { zonaDe(it.substringAfter('=')) }
        val nota = c.getValue("nota").toInt()
        require(c.getValue("id").isNotBlank() && nota in 0..NOTA_MAXIMA)
        PerfilInstalacao(
            id = c.getValue("id"),
            nome = c.getValue("nome"),
            posicao = PosicaoCamera.valueOf(c.getValue("posicao")),
            lado = LadoMotorista.valueOf(c.getValue("lado")),
            criadoEpochMs = c.getValue("criado").toLong(),
            calibracao = Calibracao(
                numero(c.getValue("ear")), numero(c.getValue("pitch")), numero(c.getValue("yaw")),
                opcional(c["ear_dir"]), opcional(c["ear_esq"]), opcional(c["olhar_h"]), opcional(c["olhar_v"]),
            ),
            desvios = Desvios(
                numero(c.getValue("desvio_pitch")), numero(c.getValue("desvio_yaw")),
                opcional(c["desvio_olhar_h"]), opcional(c["desvio_olhar_v"]),
            ),
            zonas = zonas,
            fracaoVerificacoesOk = numero(c.getValue("verificacoes_ok")),
            nota = nota,
        )
    }.getOrNull()

    private fun zonaDe(texto: String): ZonaOlhar {
        val campos = ArrayDeque(texto.split(";"))
        require(campos.size == CAMPOS_ZONA)
        fun proximo() = campos.removeFirst()
        return ZonaOlhar(
            Zona.valueOf(proximo()), numero(proximo()), numero(proximo()), opcional(proximo()), opcional(proximo()),
            numero(proximo()), numero(proximo()), numero(proximo()),
        )
    }

    private fun numero(texto: String): Double = texto.toDouble().also { require(!it.isNaN() && !it.isInfinite()) }

    private fun opcional(texto: String?): Double? = texto?.takeIf { it.isNotEmpty() }?.let(::numero)
}

/**
 * Brilho médio (luminância de 0 a 255) dentro da caixa do rosto, cortada na imagem, numa grade de no máximo
 * 32 x 32 amostras. Nulo se a caixa cair toda fora da imagem.
 */
fun brilhoDoRosto(caixa: CaixaRosto, largura: Int, altura: Int, argb: (x: Int, y: Int) -> Int): Double? {
    val x0 = floor(caixa.x0 * largura).toInt().coerceAtLeast(0)
    val x1 = ceil(caixa.x1 * largura).toInt().coerceAtMost(largura)
    val y0 = floor(caixa.y0 * altura).toInt().coerceAtLeast(0)
    val y1 = ceil(caixa.y1 * altura).toInt().coerceAtMost(altura)
    val passo = maxOf(1, minOf(x1 - x0, y1 - y0) / PASSOS_NO_LADO)
    val amostras = (y0 until y1 step passo).flatMap { y ->
        (x0 until x1 step passo).map { x -> RecorteOlho.luminancia(argb(x, y)) }
    }
    return amostras.takeIf { it.isNotEmpty() }?.average()
}

/** Textos do assistente de instalação, em linguagem simples (ADR-012, regra 4: até 120 caracteres). */
object TextosInstalacao {
    fun dica(d: Dica): String = when (d) {
        Dica.CENTRALIZAR -> "Centralize o rosto na imagem"
        Dica.APROXIMAR -> "Aproxime a câmera do rosto"
        Dica.AFASTAR -> "Afaste a câmera do rosto"
        Dica.MAIS_LUZ -> "Falta luz no rosto"
        Dica.MENOS_LUZ -> "Luz forte demais no rosto"
        Dica.SUBIR -> "Suba a câmera"
        Dica.ABAIXAR -> "Abaixe a câmera"
        Dica.MAIS_A_FRENTE -> "Leve a câmera mais para a frente do rosto"
        Dica.FIXAR -> "Fixe a câmera no suporte"
    }

    fun verificacao(v: Verificacao): String = when (v) {
        Verificacao.ROSTO_INTEIRO -> "Rosto inteiro na imagem"
        Verificacao.TAMANHO -> "Tamanho do rosto"
        Verificacao.LUZ -> "Luz no rosto"
        Verificacao.ANGULO -> "Ângulo da câmera"
        Verificacao.CAMERA_PARADA -> "Câmera parada"
    }

    fun instrucaoZona(z: Zona): String = when (z) {
        Zona.RETROVISOR -> "Olhe para o retrovisor interno"
        Zona.ESPELHO_ESQUERDO -> "Olhe para o espelho esquerdo"
        Zona.ESPELHO_DIREITO -> "Olhe para o espelho direito"
        Zona.PAINEL -> "Olhe para o painel de instrumentos"
    }

    fun posicao(p: PosicaoCamera): String = when (p) {
        PosicaoCamera.COLUNA_VOLANTE -> "Atrás do volante"
        PosicaoCamera.CENTRO_PAINEL -> "Centro do painel"
        PosicaoCamera.PILAR_A -> "Coluna ao lado do para-brisa"
        PosicaoCamera.RETROVISOR -> "Perto do retrovisor interno"
    }

    fun explicacaoPosicao(p: PosicaoCamera): String = when (p) {
        PosicaoCamera.COLUNA_VOLANTE -> "Na coluna de direção, de frente para o rosto e um pouco abaixo dele"
        PosicaoCamera.CENTRO_PAINEL -> "No suporte do meio do painel ou da central multimídia, de lado para o rosto"
        PosicaoCamera.PILAR_A -> "No pilar entre o para-brisa e a porta, do lado do motorista"
        PosicaoCamera.RETROVISOR -> "Presa no para-brisa junto ao retrovisor, acima do rosto"
    }

    /** Uma linha por parte da nota, para o resumo dizer o que falhou. */
    fun partesDaNota(p: PartesNota): List<String> = listOf(
        "Enquadramento certo: ${p.verificacoes} de 60",
        "Câmera e rosto parados: ${p.estabilidade} de 25",
        "Lugares do carro gravados: ${p.zonas} de 15",
    )

    fun qualidade(q: Qualidade): String = when (q) {
        Qualidade.BOA -> "Boa"
        Qualidade.ACEITAVEL -> "Aceitável"
        Qualidade.REFAZER -> "Refazer"
    }
}
