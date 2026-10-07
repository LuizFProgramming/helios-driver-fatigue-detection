package br.unicid.helios.dominio.eventos

import br.unicid.helios.dominio.ConfiguracaoDeteccao
import br.unicid.helios.dominio.tipos.TipoEvento
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventosTest {

    @Test
    fun `detector sustentado percorre inativa, em curso e encerrada`() {
        val d = DetectorSustentado()
        assertEquals(Fase.Inativa, d.atualizar(0, false))
        assertEquals(Fase.EmCurso(100, 0), d.atualizar(100, true))
        assertEquals(Fase.EmCurso(100, 250), d.atualizar(350, true))
        assertEquals(Fase.Encerrada(100, 300), d.atualizar(400, false))
        assertEquals(Fase.Inativa, d.atualizar(450, false))
        assertEquals(Fase.EmCurso(500, 0), d.atualizar(500, true))
    }

    @Test
    fun `duracao em curso so existe na fase em curso`() {
        assertEquals(250, Fase.EmCurso(0, 250).duracaoEmCurso())
        assertEquals(0, Fase.Encerrada(0, 250).duracaoEmCurso())
        assertEquals(0, Fase.Inativa.duracaoEmCurso())
    }

    @Test
    fun `fechamento classificado pelas bordas exatas`() {
        val cfg = ConfiguracaoDeteccao()
        assertNull(classificarFechamento(59, cfg))
        assertEquals(TipoEvento.PISCADA, classificarFechamento(60, cfg))
        assertEquals(TipoEvento.PISCADA, classificarFechamento(499, cfg))
        assertEquals(TipoEvento.PISCADA_LONGA, classificarFechamento(500, cfg))
        assertEquals(TipoEvento.PISCADA_LONGA, classificarFechamento(999, cfg))
        assertEquals(TipoEvento.MICROSSONO, classificarFechamento(1_000, cfg))
    }
}
