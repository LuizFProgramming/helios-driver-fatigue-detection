package br.unicid.helios.protocolos

import br.unicid.helios.dominio.tipos.CaixaRosto
import br.unicid.helios.dominio.tipos.Ponto2D

/**
 * Liga o realce de imagem sozinho quando o quadro fica escuro, com histerese: liga depois de [esperaMs] abaixo de
 * [ligaAbaixo] e só desliga depois de [esperaMs] acima de [desligaAcima]. Brilho em luminância de 0 a 255, medido
 * pelo [brilhoDoRosto] no quadro inteiro. Limiares são hipótese da equipe (testes noturnos de 16/11).
 */
class RealceAutomatico(
    private val ligaAbaixo: Double = 60.0,
    private val desligaAcima: Double = 90.0,
    private val esperaMs: Long = 2_000,
) {
    var ligado = false
        private set
    private var desdeMs: Long? = null

    fun atualizar(timestampMs: Long, brilho: Double): Boolean {
        val querMudar = if (ligado) brilho > desligaAcima else brilho < ligaAbaixo
        if (!querMudar) {
            desdeMs = null
            return ligado
        }
        val desde = desdeMs ?: timestampMs.also { desdeMs = it }
        if (timestampMs - desde >= esperaMs) {
            ligado = !ligado
            desdeMs = null
        }
        return ligado
    }
}

/** Caixa que envolve os pontos (frações da imagem). */
fun caixaDosPontos(pontos: List<Ponto2D>): CaixaRosto {
    val p0 = pontos.first()
    return pontos.fold(CaixaRosto(p0.x, p0.y, p0.x, p0.y)) { c, p ->
        CaixaRosto(minOf(c.x0, p.x), minOf(c.y0, p.y), maxOf(c.x1, p.x), maxOf(c.y1, p.y))
    }
}

/** Ponto do recorte levado de volta para frações da imagem inteira. */
fun paraImagemInteira(p: Ponto2D, recorte: CaixaRosto): Ponto2D =
    Ponto2D(recorte.x0 + p.x * recorte.largura, recorte.y0 + p.y * (recorte.y1 - recorte.y0))

private fun area(c: CaixaRosto) = c.largura * (c.y1 - c.y0)

private fun sobreposicao(a: CaixaRosto, b: CaixaRosto): Double {
    val largura = minOf(a.x1, b.x1) - maxOf(a.x0, b.x0)
    val altura = minOf(a.y1, b.y1) - maxOf(a.y0, b.y0)
    if (largura <= 0 || altura <= 0) return 0.0
    val comum = largura * altura
    return comum / (area(a) + area(b) - comum)
}

/**
 * Mantém o rosto do motorista em toda a área da câmera.
 *
 * - **Escolha:** com mais de um rosto na imagem (passageiro ao fundo), fica o que mais se sobrepõe à última posição
 *   do motorista; sem posição anterior ou sem sobreposição, o maior (o mais perto da câmera).
 * - **Nova busca:** quando o rosto some, por até [buscaMaxMs] metade dos quadros vai para o detector recortada em
 *   volta da última posição, com [margem] de cada lado, e ampliada para o tamanho da imagem. Rosto pequeno, escuro
 *   ou de lado fica maior para o detector. A outra metade continua com a imagem inteira, para achar o rosto se ele
 *   reaparecer longe. O recorte tem a mesma proporção da imagem, então a ampliação não deforma o olho (EAR).
 *
 * Não é thread-safe: o adaptador chama sob o mesmo lock.
 */
class RastreadorRosto(private val margem: Double = 0.5, private val buscaMaxMs: Long = 3_000) {
    private var ultima: CaixaRosto? = null
    private var perdidoDesdeMs: Long? = null
    private var alternar = false

    /** Recorte a usar no quadro que vai ser enviado agora; nulo = imagem inteira. */
    fun recorteParaEnviar(timestampMs: Long): CaixaRosto? {
        val caixa = ultima
        val desde = perdidoDesdeMs
        if (caixa == null || desde == null || timestampMs - desde > buscaMaxMs) return null
        alternar = !alternar
        return recorteEmVolta(caixa).takeIf { alternar }
    }

    /** Recebe as caixas dos rostos achados (frações da imagem inteira) e diz qual é o motorista; nulo = nenhum. */
    fun escolher(timestampMs: Long, caixas: List<CaixaRosto>): Int? {
        if (caixas.isEmpty()) {
            if (perdidoDesdeMs == null) perdidoDesdeMs = timestampMs
            return null
        }
        val anterior = ultima
        val pelaPosicao = anterior?.let { a ->
            caixas.indices.maxBy { sobreposicao(caixas[it], a) }.takeIf { sobreposicao(caixas[it], a) > 0.0 }
        }
        val escolhido = pelaPosicao ?: caixas.indices.maxBy { area(caixas[it]) }
        ultima = caixas[escolhido]
        perdidoDesdeMs = null
        return escolhido
    }

    private fun recorteEmVolta(c: CaixaRosto): CaixaRosto {
        val lado = (maxOf(c.largura, c.y1 - c.y0) * (1 + 2 * margem)).coerceAtMost(1.0)
        val x0 = ((c.x0 + c.x1) / 2 - lado / 2).coerceIn(0.0, 1.0 - lado)
        val y0 = ((c.y0 + c.y1) / 2 - lado / 2).coerceIn(0.0, 1.0 - lado)
        return CaixaRosto(x0, y0, x0 + lado, y0 + lado)
    }
}
