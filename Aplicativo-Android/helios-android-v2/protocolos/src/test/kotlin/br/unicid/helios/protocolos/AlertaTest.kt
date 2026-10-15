package br.unicid.helios.protocolos

import br.unicid.helios.dominio.tipos.EstadoMotorista
import br.unicid.helios.dominio.tipos.Motivo
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanoDeAlertaTest {

    @Test
    fun `sem alarme nao ha plano`() {
        assertNull(planoDeAlerta(EstadoMotorista.ATENTO, emptySet(), 30.0))
        assertNull(planoDeAlerta(EstadoMotorista.SEM_ROSTO, emptySet(), 30.0))
    }

    @Test
    fun `critico escala mais rapido - maximo aos 3 s, que sao 6 s de olho fechado`() {
        val n1 = assertNotNull(planoDeAlerta(EstadoMotorista.CRITICO, setOf(Motivo.SONO), 1.5))
        val n2 = assertNotNull(planoDeAlerta(EstadoMotorista.CRITICO, setOf(Motivo.SONO), 3.0))
        assertEquals(1, n1.nivel)
        assertEquals(2, n2.nivel)
    }

    @Test
    fun `aviso de olhos fechando e curto, baixo e nao escala`() {
        val aviso = assertNotNull(planoDeAlerta(EstadoMotorista.ATENCAO, setOf(Motivo.OLHOS_FECHANDO), 20.0))
        assertEquals(0, aviso.nivel)
        assertTrue(aviso.padrao.volume <= 0.5)
        assertTrue(aviso.padrao.frequenciaHz < assertNotNull(padraoPara(EstadoMotorista.CRITICO)).frequenciaHz)
    }

    @Test
    fun `fadiga so de pos-sono vira lembrete suave a cada 20 s, sem escalar`() {
        val lembrete = assertNotNull(planoDeAlerta(EstadoMotorista.FADIGA, setOf(Motivo.POS_SONO), 90.0))
        assertEquals(0, lembrete.nivel)
        assertEquals(20_000, lembrete.padrao.ligadoMs + lembrete.padrao.desligadoMs)
        assertEquals(0, lembrete.padrao.codigoBuzzer)
        assertTrue(lembrete.fala.contains("pausa"))
        // Com outro sinal de fadiga junto, volta o padrão normal de fadiga.
        val normal = assertNotNull(planoDeAlerta(EstadoMotorista.FADIGA, setOf(Motivo.POS_SONO, Motivo.BOCEJOS_FREQUENTES), 0.0))
        assertEquals(padraoPara(EstadoMotorista.FADIGA), normal.padrao)
    }

    @Test
    fun `alarme escala aos 5 e aos 10 segundos sem reacao`() {
        val n0 = assertNotNull(planoDeAlerta(EstadoMotorista.FADIGA, emptySet(), 4.9))
        val n1 = assertNotNull(planoDeAlerta(EstadoMotorista.FADIGA, emptySet(), 5.0))
        val n2 = assertNotNull(planoDeAlerta(EstadoMotorista.FADIGA, emptySet(), 10.0))
        assertEquals(listOf(0, 1, 2), listOf(n0.nivel, n1.nivel, n2.nivel))
        assertEquals(padraoPara(EstadoMotorista.FADIGA), n0.padrao)
        assertEquals(0.8, n1.padrao.volume, 1e-9)
        assertEquals(1.0, n2.padrao.volume, 1e-9)
        assertEquals(900, n1.padrao.desligadoMs)
        assertEquals(600, n2.padrao.desligadoMs)
        assertTrue(n0.intervaloFalaMs > n1.intervaloFalaMs && n1.intervaloFalaMs > n2.intervaloFalaMs)
    }

    @Test
    fun `volume nunca passa de 100 por cento`() {
        assertEquals(1.0, assertNotNull(planoDeAlerta(EstadoMotorista.CRITICO, emptySet(), 60.0)).padrao.volume)
    }

    @Test
    fun `cada alerta tem a sua frase em portugues`() {
        val falas = listOf(
            EstadoMotorista.FADIGA, EstadoMotorista.CRITICO, EstadoMotorista.DESATENTO,
            EstadoMotorista.DISTRAIDO, EstadoMotorista.ATENCAO,
        ).map { assertNotNull(planoDeAlerta(it, emptySet(), 0.0)).fala }
        assertEquals(5, falas.toSet().size)
        assertTrue(falas.all { it.isNotBlank() })
    }
}

class ControladorFalaTest {

    @Test
    fun `fala ao entrar no alerta e repete no intervalo do nivel`() {
        val c = ControladorFala()
        val plano = assertNotNull(planoDeAlerta(EstadoMotorista.CRITICO, emptySet(), 0.0))
        assertEquals(plano.fala, c.avaliar(0, EstadoMotorista.CRITICO, plano))
        assertNull(c.avaliar(1_000, EstadoMotorista.CRITICO, plano))
        assertEquals(plano.fala, c.avaliar(plano.intervaloFalaMs, EstadoMotorista.CRITICO, plano))
    }

    @Test
    fun `troca de alerta fala na hora e sem alerta fica em silencio e reinicia`() {
        val c = ControladorFala()
        val critico = assertNotNull(planoDeAlerta(EstadoMotorista.CRITICO, emptySet(), 0.0))
        val desatento = assertNotNull(planoDeAlerta(EstadoMotorista.DESATENTO, emptySet(), 0.0))
        c.avaliar(0, EstadoMotorista.CRITICO, critico)
        assertEquals(desatento.fala, c.avaliar(500, EstadoMotorista.DESATENTO, desatento))
        assertNull(c.avaliar(600, EstadoMotorista.ATENTO, null))
        assertEquals(critico.fala, c.avaliar(700, EstadoMotorista.CRITICO, critico))
    }
}

class RetornoTest {

    @Test
    fun `som de retorno so ao sair de um alerta urgente para atento ou fadiga`() {
        assertTrue(sinalDeRetorno(EstadoMotorista.CRITICO, EstadoMotorista.FADIGA))
        assertTrue(sinalDeRetorno(EstadoMotorista.ATENCAO, EstadoMotorista.ATENTO))
        assertTrue(sinalDeRetorno(EstadoMotorista.DESATENTO, EstadoMotorista.ATENTO))
        assertTrue(sinalDeRetorno(EstadoMotorista.DISTRAIDO, EstadoMotorista.ATENTO))
        assertFalse(sinalDeRetorno(EstadoMotorista.CRITICO, EstadoMotorista.CRITICO))
        assertFalse(sinalDeRetorno(EstadoMotorista.FADIGA, EstadoMotorista.ATENTO))
        assertFalse(sinalDeRetorno(EstadoMotorista.CRITICO, EstadoMotorista.SEM_ROSTO))
        assertFalse(sinalDeRetorno(null, EstadoMotorista.ATENTO))
    }

    @Test
    fun `som de retorno sobe de tom e e curto`() {
        val pcm = gerarRetornoPcm(taxaHz = 8_000)
        assertEquals(2_400, pcm.size) // 120 ms + 180 ms
        assertTrue(pcm.any { it.toInt() != 0 })
        assertEquals(4_410 + 2_205, gerarRetornoPcm().size) // taxa padrão
    }
}
