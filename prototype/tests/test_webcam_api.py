"""
Testes do script da câmera (esp32cam/webcam_api.py).

Testam só as decisões do script (categoria, quando enviar e o que enviar),
sem abrir câmera, sem MediaPipe e sem API rodando.

Para executar, na raiz do projeto:
  .venv\\Scripts\\python.exe -m pytest tests -v
"""

import sys
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "esp32cam"))

import webcam_api  # noqa: E402
from webcam_api import (  # noqa: E402
    ATENTO, DESATENTO, DORMINDO, PISCANDO, SEM_ROSTO, SONOLENCIA,
    TEXTO_POR_CATEGORIA, ClassificadorDeAlerta, deve_enviar, montar_dados,
)
from domain.types import (  # noqa: E402
    CAUSA_OLHOS_FECHADOS, CAUSA_PERCLOS, EstadoMotorista, LeituraMonitoramento,
)

HORA = datetime(2026, 10, 6, 22, 13, 33, tzinfo=timezone.utc)


def _leitura(estado=EstadoMotorista.ATENTO, causa=None, bocejando=False,
             ear=0.31, perclos=0.153, yaw=0.0, pitch=0.0) -> LeituraMonitoramento:
    return LeituraMonitoramento(
        estado=estado, ear_medio=ear, mar=0.12, ecf_medio=0.05, perclos=perclos,
        total_piscadas=3, total_bocejos=1, segundos_em_alerta=0.0,
        esta_bocejando=bocejando, causa_alerta=causa, yaw_graus=yaw, pitch_graus=pitch,
    )


# ── Categoria ─────────────────────────────────────────────────────────────────

def test_atento_e_piscando():
    c = ClassificadorDeAlerta()
    assert c.classificar(_leitura(), 0.0) == ATENTO
    assert c.classificar(_leitura(EstadoMotorista.PISCANDO), 0.1) == PISCANDO


def test_olhos_fechados_sem_parar_e_dormindo():
    leitura = _leitura(EstadoMotorista.SONOLENTO, causa=CAUSA_OLHOS_FECHADOS)
    assert ClassificadorDeAlerta().classificar(leitura, 0.0) == DORMINDO


def test_perclos_alto_e_sonolencia():
    leitura = _leitura(EstadoMotorista.SONOLENTO, causa=CAUSA_PERCLOS)
    assert ClassificadorDeAlerta().classificar(leitura, 0.0) == SONOLENCIA


def test_bocejo_e_sonolencia():
    assert ClassificadorDeAlerta().classificar(_leitura(bocejando=True), 0.0) == SONOLENCIA


def test_rosto_some_por_pouco_tempo_e_sem_rosto():
    c = ClassificadorDeAlerta()
    sem_rosto = _leitura(EstadoMotorista.SEM_FACE)
    assert c.classificar(sem_rosto, 10.0) == SEM_ROSTO
    assert c.classificar(sem_rosto, 11.9) == SEM_ROSTO


def test_rosto_some_por_2_segundos_e_desatento():
    c = ClassificadorDeAlerta()
    sem_rosto = _leitura(EstadoMotorista.SEM_FACE)
    c.classificar(sem_rosto, 10.0)
    assert c.classificar(sem_rosto, 12.0) == DESATENTO


def test_rosto_volta_e_zera_a_contagem():
    c = ClassificadorDeAlerta()
    sem_rosto = _leitura(EstadoMotorista.SEM_FACE)
    c.classificar(sem_rosto, 10.0)
    c.classificar(_leitura(), 11.5)
    assert c.classificar(sem_rosto, 12.0) == SEM_ROSTO


# ── Quando enviar ─────────────────────────────────────────────────────────────

def test_envia_no_primeiro_frame():
    assert deve_enviar(agora=10.0, ultimo_envio=None, categoria=ATENTO, categoria_anterior=None)


def test_nao_envia_antes_de_2_segundos():
    assert not deve_enviar(agora=11.9, ultimo_envio=10.0, categoria=ATENTO, categoria_anterior=ATENTO)


def test_envia_quando_passam_2_segundos():
    assert deve_enviar(agora=12.0, ultimo_envio=10.0, categoria=ATENTO, categoria_anterior=ATENTO)


def test_envia_na_hora_ao_entrar_em_cada_alerta():
    for alerta in (DESATENTO, SONOLENCIA, DORMINDO):
        assert deve_enviar(agora=10.1, ultimo_envio=10.0, categoria=alerta, categoria_anterior=ATENTO)


def test_troca_de_alerta_tambem_envia_na_hora():
    assert deve_enviar(agora=10.1, ultimo_envio=10.0, categoria=DORMINDO, categoria_anterior=SONOLENCIA)


def test_nao_repete_envio_imediato_enquanto_continua_no_alerta():
    assert not deve_enviar(agora=10.5, ultimo_envio=10.1, categoria=DORMINDO, categoria_anterior=DORMINDO)


def test_piscar_nao_envia_na_hora():
    assert not deve_enviar(agora=10.1, ultimo_envio=10.0, categoria=PISCANDO, categoria_anterior=ATENTO)


def test_intervalo_vem_da_constante(monkeypatch):
    monkeypatch.setattr(webcam_api, "INTERVALO_DE_ENVIO_EM_SEGUNDOS", 5.0)
    assert not deve_enviar(agora=14.0, ultimo_envio=10.0, categoria=ATENTO, categoria_anterior=ATENTO)
    assert deve_enviar(agora=15.0, ultimo_envio=10.0, categoria=ATENTO, categoria_anterior=ATENTO)


# ── O que enviar ──────────────────────────────────────────────────────────────

def test_sonolencia_e_dormindo_usam_o_texto_que_faz_o_app_vibrar():
    # helios-mobile/src/app/index.tsx compara exatamente este texto.
    assert TEXTO_POR_CATEGORIA[SONOLENCIA] == "Sonolência Detectada"
    assert TEXTO_POR_CATEGORIA[DORMINDO] == "Sonolência Detectada"


def test_dados_do_formulario():
    dados = montar_dados(7, _leitura(ear=0.123456, yaw=30.0, pitch=-25.0), DORMINDO, HORA)
    assert dados == {
        "frame_id": "7",
        "device_id": "WEBCAM_NOTEBOOK",
        "categoria": "DORMINDO",
        "status_motorista": "Sonolência Detectada",
        "direcao_rosto": "baixo_esquerda",
        "capturado_em": "2026-10-06T22:13:33+00:00",
        "ear": "0.1235",
        "ecf": "0.0500",
        "mar": "0.1200",
        "perclos": "15.3",
        "segundos_em_alerta": "0.0",
        "total_piscadas": "3",
        "total_bocejos": "1",
    }


def test_sem_rosto_vai_como_sem_rosto_na_direcao():
    dados = montar_dados(1, _leitura(EstadoMotorista.SEM_FACE, yaw=None, pitch=None), DESATENTO, HORA)
    assert dados["direcao_rosto"] == "sem_rosto"
