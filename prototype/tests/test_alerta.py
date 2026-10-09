"""
Testes do alerta sonoro: padrão de cada categoria, geração do som e quando toca/para.

Rodam sem alto-falante, sem câmera e sem Windows: o winsound é trocado por um falso.

Para executar, na raiz do projeto:
  .venv\\Scripts\\python.exe -m pytest tests -v
"""

import io
import sys
import wave
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "prototipo_desktop_python"))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "esp32cam"))

from adapters.audio_alert import (  # noqa: E402
    AlertaSilencioso, AlertaSonoroWindows, criar_saida_de_alerta,
)
from config import PADROES_SONOROS, TAXA_AMOSTRAGEM_ALERTA_HZ  # noqa: E402
from domain.alert_controller import ControladorDeAlerta  # noqa: E402
from domain.alert_pattern import (  # noqa: E402
    AMPLITUDE_MAXIMA, PadraoSonoro, gerar_ciclo_pcm, gerar_wav, padrao_para_categoria,
)
from ports.alert_output_port import PortaSaidaDeAlerta  # noqa: E402

TAXA = TAXA_AMOSTRAGEM_ALERTA_HZ


# ── Padrão de cada categoria ──────────────────────────────────────────────────

@pytest.mark.parametrize("categoria", ["ATENTO", "PISCANDO", "SEM_ROSTO", "categoria_inventada"])
def test_categorias_normais_ficam_em_silencio(categoria):
    assert padrao_para_categoria(categoria) is None


@pytest.mark.parametrize("categoria", ["DESATENTO", "SONOLENCIA", "DORMINDO"])
def test_categorias_de_alerta_tem_padrao(categoria):
    assert isinstance(padrao_para_categoria(categoria), PadraoSonoro)


def test_dormindo_e_o_mais_urgente():
    dormindo = padrao_para_categoria("DORMINDO")
    for outra in ("SONOLENCIA", "DESATENTO"):
        padrao = padrao_para_categoria(outra)
        assert dormindo.frequencia_hz > padrao.frequencia_hz   # mais agudo
        assert dormindo.volume >= padrao.volume                # no mínimo igual de alto
        assert dormindo.duracao_do_ciclo_ms < padrao.duracao_do_ciclo_ms   # repete mais rápido


def test_cada_alerta_soa_diferente():
    padroes = {padrao_para_categoria(c) for c in PADROES_SONOROS}
    assert len(padroes) == len(PADROES_SONOROS)


def test_categorias_do_config_existem_no_script_da_camera():
    """Se alguém renomear uma categoria no webcam_api.py, o som não pode ficar mudo sem aviso."""
    import webcam_api
    assert set(PADROES_SONOROS) == webcam_api.CATEGORIAS_DE_ALERTA


@pytest.mark.parametrize("categoria", list(PADROES_SONOROS))
def test_valores_do_config_sao_validos(categoria):
    padrao = padrao_para_categoria(categoria)
    assert 200 <= padrao.frequencia_hz <= 8000      # audível e abaixo de metade da taxa
    assert padrao.ligado_ms >= 50                   # bipe curto demais não é percebido
    assert padrao.desligado_ms > 0
    assert 0.0 < padrao.volume <= 1.0


# ── Geração do som ────────────────────────────────────────────────────────────

PADRAO_TESTE = PadraoSonoro(frequencia_hz=1000, ligado_ms=100, desligado_ms=50, volume=1.0)


def test_ciclo_tem_o_tamanho_certo():
    ciclo = gerar_ciclo_pcm(PADRAO_TESTE, TAXA)
    assert len(ciclo) == TAXA * 150 // 1000


def test_silencio_vem_depois_do_bipe():
    ciclo = gerar_ciclo_pcm(PADRAO_TESTE, TAXA)
    fim_do_bipe = TAXA * 100 // 1000
    assert any(a != 0 for a in ciclo[:fim_do_bipe])
    assert all(a == 0 for a in ciclo[fim_do_bipe:])


def test_volume_define_a_amplitude():
    alto = gerar_ciclo_pcm(PADRAO_TESTE, TAXA)
    baixo = gerar_ciclo_pcm(PadraoSonoro(1000, 100, 50, volume=0.5), TAXA)
    assert max(alto) == pytest.approx(AMPLITUDE_MAXIMA, abs=50)
    assert max(baixo) == pytest.approx(AMPLITUDE_MAXIMA * 0.5, abs=50)


def test_a_frequencia_do_tom_esta_certa():
    # Trecho do meio do bipe (fora da rampa de subida e descida): de 10 ms a 90 ms,
    # ou seja, 80 ms de 1000 Hz = 80 ondas; cada onda cruza o zero 2 vezes.
    ciclo = gerar_ciclo_pcm(PADRAO_TESTE, TAXA)
    meio = [a for a in ciclo[TAXA * 10 // 1000: TAXA * 90 // 1000] if a != 0]
    cruzamentos = sum(1 for a, b in zip(meio, meio[1:]) if (a < 0) != (b < 0))
    assert cruzamentos == pytest.approx(160, abs=3)


def test_bipe_comeca_e_termina_suave_sem_clique():
    ciclo = gerar_ciclo_pcm(PADRAO_TESTE, TAXA)
    fim_do_bipe = TAXA * 100 // 1000
    assert abs(ciclo[0]) < AMPLITUDE_MAXIMA * 0.05
    assert abs(ciclo[fim_do_bipe - 1]) < AMPLITUDE_MAXIMA * 0.05


def test_volume_fora_do_limite_nao_estoura():
    ciclo = gerar_ciclo_pcm(PadraoSonoro(1000, 100, 50, volume=5.0), TAXA)
    assert max(ciclo) <= AMPLITUDE_MAXIMA and min(ciclo) >= -AMPLITUDE_MAXIMA


def test_wav_e_valido():
    with wave.open(io.BytesIO(gerar_wav(PADRAO_TESTE, repeticoes=3, taxa_hz=TAXA)), "rb") as w:
        assert w.getnchannels() == 1
        assert w.getsampwidth() == 2
        assert w.getframerate() == TAXA
        assert w.getnframes() == 3 * (TAXA * 150 // 1000)


# ── Quando o som toca e para ──────────────────────────────────────────────────

class SaidaFalsa(PortaSaidaDeAlerta):
    """Guarda o que o controlador mandou fazer."""

    def __init__(self):
        self.chamadas = []

    def tocar(self, padrao):
        self.chamadas.append(("tocar", padrao))

    def parar(self):
        self.chamadas.append(("parar",))

    def liberar(self):
        self.chamadas.append(("liberar",))

    def acoes(self):
        return [c[0] for c in self.chamadas]


def _controlador(**opcoes):
    saida = SaidaFalsa()
    return saida, ControladorDeAlerta(saida, ativo=True, segundos_para_silenciar=1.0, **opcoes)


def test_sem_alerta_nada_toca():
    saida, ctrl = _controlador()
    for t in range(10):
        ctrl.atualizar("ATENTO", t * 0.1)
    assert saida.chamadas == []


def test_toca_assim_que_entra_em_alerta():
    saida, ctrl = _controlador()
    ctrl.atualizar("ATENTO", 0.0)
    ctrl.atualizar("DORMINDO", 0.1)
    assert saida.chamadas == [("tocar", padrao_para_categoria("DORMINDO"))]
    assert ctrl.tocando == padrao_para_categoria("DORMINDO")


def test_nao_reinicia_o_som_a_cada_frame():
    saida, ctrl = _controlador()
    for i in range(90):                       # 3 s a 30 fps
        ctrl.atualizar("DORMINDO", i / 30)
    assert saida.acoes() == ["tocar"]


def test_continua_tocando_durante_o_tempo_de_espera():
    saida, ctrl = _controlador()
    ctrl.atualizar("DORMINDO", 0.0)
    ctrl.atualizar("ATENTO", 0.5)            # 0,5 s depois: ainda dentro de 1 s
    ctrl.atualizar("PISCANDO", 0.9)
    assert saida.acoes() == ["tocar"]
    assert ctrl.tocando is not None


def test_para_depois_do_tempo_de_espera():
    saida, ctrl = _controlador()
    ctrl.atualizar("DORMINDO", 0.0)
    ctrl.atualizar("ATENTO", 1.0)
    assert saida.acoes() == ["tocar", "parar"]
    assert ctrl.tocando is None


def test_olho_que_pisca_um_frame_nao_corta_o_som():
    saida, ctrl = _controlador()
    ctrl.atualizar("DORMINDO", 0.00)
    ctrl.atualizar("ATENTO", 0.03)           # 1 frame normal
    ctrl.atualizar("DORMINDO", 0.07)
    assert saida.acoes() == ["tocar"]


def test_a_espera_conta_a_partir_do_ultimo_alerta():
    saida, ctrl = _controlador()
    ctrl.atualizar("DORMINDO", 0.0)
    ctrl.atualizar("DORMINDO", 5.0)          # alerta continuou até 5 s
    ctrl.atualizar("ATENTO", 5.5)            # só 0,5 s depois do último alerta
    assert saida.acoes() == ["tocar"]
    ctrl.atualizar("ATENTO", 6.0)
    assert saida.acoes() == ["tocar", "parar"]


def test_troca_de_padrao_acontece_na_hora():
    saida, ctrl = _controlador()
    ctrl.atualizar("SONOLENCIA", 0.0)
    ctrl.atualizar("DORMINDO", 0.1)
    assert saida.chamadas == [
        ("tocar", padrao_para_categoria("SONOLENCIA")),
        ("tocar", padrao_para_categoria("DORMINDO")),
    ]


def test_toca_de_novo_no_proximo_alerta():
    saida, ctrl = _controlador()
    ctrl.atualizar("DORMINDO", 0.0)
    ctrl.atualizar("ATENTO", 2.0)
    ctrl.atualizar("DORMINDO", 3.0)
    assert saida.acoes() == ["tocar", "parar", "tocar"]


def test_mudo_corta_o_som_e_nao_toca_mais():
    saida, ctrl = _controlador()
    ctrl.atualizar("DORMINDO", 0.0)
    assert ctrl.alternar_mudo() is True
    ctrl.atualizar("DORMINDO", 0.1)
    assert saida.acoes() == ["tocar", "parar"]
    assert ctrl.mudo is True


def test_desligar_o_mudo_volta_a_tocar_se_ainda_ha_alerta():
    saida, ctrl = _controlador()
    ctrl.alternar_mudo()
    ctrl.atualizar("DORMINDO", 0.0)
    assert saida.chamadas == [("parar",)]    # só o "parar" do mudo; nada tocou
    assert ctrl.alternar_mudo() is False
    ctrl.atualizar("DORMINDO", 0.1)
    assert saida.acoes() == ["parar", "tocar"]


def test_alerta_desativado_no_config_comeca_mudo():
    saida = SaidaFalsa()
    ctrl = ControladorDeAlerta(saida, ativo=False)
    ctrl.atualizar("DORMINDO", 0.0)
    assert ctrl.mudo is True
    assert saida.chamadas == []


def test_liberar_para_e_libera_a_saida():
    saida, ctrl = _controlador()
    ctrl.atualizar("DORMINDO", 0.0)
    ctrl.liberar()
    assert saida.acoes() == ["tocar", "parar", "liberar"]


# ── Adaptador do Windows (com winsound falso) ─────────────────────────────────

class WinsoundFalso:
    SND_FILENAME = 0x20000
    SND_ASYNC = 0x1
    SND_LOOP = 0x8
    SND_PURGE = 0x40

    def __init__(self):
        self.chamadas = []

    def PlaySound(self, som, flags):
        self.chamadas.append((som, flags))


def test_windows_toca_o_wav_em_repeticao_e_em_segundo_plano():
    ws = WinsoundFalso()
    saida = AlertaSonoroWindows(winsound_modulo=ws)
    try:
        saida.tocar(PADRAO_TESTE)
        arquivo, flags = ws.chamadas[0]
        assert flags == ws.SND_FILENAME | ws.SND_ASYNC | ws.SND_LOOP
        with wave.open(arquivo, "rb") as w:               # o arquivo existe e é um WAV válido
            assert w.getnframes() == TAXA * 150 // 1000
    finally:
        saida.liberar()


def test_windows_parar_usa_purge():
    ws = WinsoundFalso()
    saida = AlertaSonoroWindows(winsound_modulo=ws)
    saida.parar()
    assert ws.chamadas == [(None, ws.SND_PURGE)]
    saida.liberar()


def test_windows_reaproveita_o_arquivo_do_mesmo_padrao():
    ws = WinsoundFalso()
    saida = AlertaSonoroWindows(winsound_modulo=ws)
    try:
        saida.tocar(PADRAO_TESTE)
        saida.tocar(PADRAO_TESTE)
        assert ws.chamadas[0][0] == ws.chamadas[1][0]
        outro = PadraoSonoro(500, 100, 50, 1.0)
        saida.tocar(outro)
        assert ws.chamadas[2][0] != ws.chamadas[0][0]
    finally:
        saida.liberar()


def test_windows_liberar_apaga_os_arquivos_temporarios():
    ws = WinsoundFalso()
    saida = AlertaSonoroWindows(winsound_modulo=ws)
    saida.tocar(PADRAO_TESTE)
    arquivo = Path(ws.chamadas[0][0])
    assert arquivo.exists()
    saida.liberar()
    assert not arquivo.exists()


def test_fora_do_windows_o_alerta_fica_silencioso(capsys):
    saida = criar_saida_de_alerta(sistema="linux")
    assert isinstance(saida, AlertaSilencioso)
    assert "indisponível" in capsys.readouterr().out
    saida.tocar(PADRAO_TESTE)      # não pode dar erro
    saida.parar()
    saida.liberar()


# ── Ligação com o script da câmera ────────────────────────────────────────────

def test_analisador_aciona_o_alerta_com_a_categoria_do_frame(monkeypatch):
    """O Analisador real do webcam_api.py manda cada categoria para o controlador."""
    import threading
    import numpy as np
    import webcam_api
    from domain.types import CAUSA_OLHOS_FECHADOS, EstadoMotorista, LeituraMonitoramento

    monkeypatch.setattr(webcam_api, "enviar_para_api", lambda *a, **k: None)   # sem rede

    class LeitorFalso:
        def __init__(self):
            self.parar = threading.Event()

        def proximo_frame(self, ultimo, espera=1.0):
            if ultimo >= 4:
                self.parar.set()
                return ultimo, None
            return ultimo + 1, np.zeros((48, 64, 3), dtype=np.uint8)

    class DetectorFalso:
        def detectar(self, frame):
            return None

    class MonitorFalso:
        def processar(self, amostra):
            return LeituraMonitoramento(
                estado=EstadoMotorista.SONOLENTO, ear_medio=0.1, mar=0.1, ecf_medio=0.9,
                perclos=0.1, total_piscadas=0, total_bocejos=0, segundos_em_alerta=1.5,
                esta_bocejando=False, causa_alerta=CAUSA_OLHOS_FECHADOS,
            )

    saida = SaidaFalsa()
    alerta = ControladorDeAlerta(saida, ativo=True)
    analisador = webcam_api.Analisador(LeitorFalso(), DetectorFalso(), MonitorFalso(), alerta)
    analisador.start()
    analisador.join(timeout=5)

    assert not analisador.is_alive()
    assert saida.chamadas == [("tocar", padrao_para_categoria("DORMINDO"))]


def test_analisador_continua_funcionando_sem_alerta(monkeypatch):
    """O alerta é opcional: sem controlador, a análise segue como antes."""
    import threading
    import webcam_api

    monkeypatch.setattr(webcam_api, "enviar_para_api", lambda *a, **k: None)

    class LeitorVazio:
        parar = threading.Event()

        def proximo_frame(self, ultimo, espera=1.0):
            self.parar.set()
            return ultimo, None

    analisador = webcam_api.Analisador(LeitorVazio(), None, None)
    analisador.start()
    analisador.join(timeout=5)
    assert not analisador.is_alive()
