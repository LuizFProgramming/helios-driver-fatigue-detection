"""
main.py — API do HELIOS

Recebe do script da câmera (esp32cam/webcam_api.py) uma foto e o resultado
da análise do rosto, guarda tudo e entrega para o app e para o painel web.

Pastas criadas dentro de backend/uploads/:
  frames/                  todas as fotos recebidas
  eventos/sonolencia/      fotos dos alertas de SONOLÊNCIA  (+ um .json com os dados)
  eventos/desatento/       fotos dos alertas de DESATENTO   (+ .json)
  eventos/dormindo/        fotos dos alertas de DORMINDO    (+ .json)

Nome das fotos de alerta: direção_rosto-TIPO-data_e_hora.jpg
  ex.: baixo_esquerda-DORMINDO-2026-10-06-22-13-33.jpg

As pastas de eventos ficam no disco: continuam lá depois de reiniciar a API.

Para executar, dentro da pasta backend:
  uvicorn main:app --reload --host 0.0.0.0 --port 8000

Páginas úteis:
  http://localhost:8000/painel   painel visual do monitoramento
  http://localhost:8000/docs     documentação interativa da API
"""

import json
import mimetypes
import re
from urllib.parse import quote
from collections import deque
from datetime import datetime
from pathlib import Path
from typing import Literal, Optional

from fastapi import FastAPI, File, Form, HTTPException, Query, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel

# ── Configuração ──────────────────────────────────────────────────────────────

PASTA_BACKEND = Path(__file__).resolve().parent
PASTA_UPLOADS = PASTA_BACKEND / "uploads"
PASTA_FRAMES = PASTA_UPLOADS / "frames"
PASTA_EVENTOS = PASTA_UPLOADS / "eventos"
PAGINA_PAINEL = PASTA_BACKEND / "painel.html"

# Quantas leituras ficam na memória. As mais antigas saem da lista,
# mas as fotos de eventos continuam salvas no disco.
MAXIMO_DE_FRAMES_EM_MEMORIA = 1000

# Os mesmos nomes estão em esp32cam/webcam_api.py.
Categoria = Literal["ATENTO", "PISCANDO", "SEM_ROSTO", "DESATENTO", "SONOLENCIA", "DORMINDO"]
CATEGORIAS = set(Categoria.__args__)

# Só estas categorias têm a foto guardada à parte.
PASTA_POR_CATEGORIA_DE_ALERTA = {
    "SONOLENCIA": "sonolencia",
    "DESATENTO": "desatento",
    "DORMINDO": "dormindo",
}
# Como o tipo aparece no nome do arquivo.
TIPO_NO_NOME_DO_ARQUIVO = {
    "SONOLENCIA": "SONOLÊNCIA",
    "DESATENTO": "DESATENTO",
    "DORMINDO": "DORMINDO",
}
FORMATO_DATA_NO_NOME = "%Y-%m-%d-%H-%M-%S"

# device_id vira parte do nome do arquivo: só letras, números, _ e -.
# Isso impede nomes como "../../arquivo" que gravariam fora da pasta uploads.
FORMATO_DEVICE_ID = re.compile(r"^[A-Za-z0-9_-]{1,32}$")
# Direção do rosto: frente, esquerda, baixo_direita, sem_rosto...
FORMATO_DIRECAO = re.compile(r"^[a-z_]{1,30}$")
# \w aceita letras com acento (SONOLÊNCIA), mas não barras nem pontos extras.
FORMATO_ARQUIVO_DE_EVENTO = re.compile(r"^[\w-]+\.(jpg|png)$")

EXTENSAO_POR_TIPO = {"image/jpeg": ".jpg", "image/jpg": ".jpg", "image/png": ".png"}

for pasta in [PASTA_FRAMES, *(PASTA_EVENTOS / p for p in PASTA_POR_CATEGORIA_DE_ALERTA.values())]:
    pasta.mkdir(parents=True, exist_ok=True)

# ── Modelos (aparecem na documentação em /docs) ───────────────────────────────


class Frame(BaseModel):
    id: int                         # número único dado pela API
    frame_id: int                   # número enviado pela câmera (recomeça a cada execução)
    device_id: str
    timestamp: str                  # quando a API recebeu, com fuso horário
    arquivo: str
    categoria: Categoria
    alerta: bool                    # True em DESATENTO, SONOLENCIA e DORMINDO
    status_motorista: str           # texto para mostrar na tela
    direcao_rosto: str              # frente, esquerda, baixo_direita, sem_rosto...
    capturado_em: str               # quando a câmera tirou a foto
    ear: Optional[float] = None
    ecf: Optional[float] = None
    mar: Optional[float] = None
    perclos: Optional[float] = None             # em %
    segundos_em_alerta: Optional[float] = None
    total_piscadas: Optional[int] = None
    total_bocejos: Optional[int] = None
    evento: Optional[str] = None                # caminho da foto salva à parte, se houver


class Leitura(BaseModel):
    """Formato combinado com o app Kotlin (GET /leituras/latest)."""
    device_id: str
    timestamp: str
    idade_segundos: float           # há quanto tempo chegou; acima de 5 s, mostrar "Sem sinal"
    categoria: Categoria
    alerta: bool
    status_motorista: str
    direcao_rosto: str
    ear: Optional[float]
    ecf: Optional[float]
    mar: Optional[float]
    perclos: Optional[float]
    segundos_em_alerta: Optional[float]
    total_piscadas: Optional[int]
    total_bocejos: Optional[int]
    imagem_url: str


class Evento(BaseModel):
    categoria: Categoria
    device_id: str
    timestamp: str
    capturado_em: str
    direcao_rosto: str
    arquivo_evento: str
    imagem_url: str
    ear: Optional[float] = None
    ecf: Optional[float] = None
    mar: Optional[float] = None
    perclos: Optional[float] = None
    segundos_em_alerta: Optional[float] = None


# ── Aplicação ─────────────────────────────────────────────────────────────────

app = FastAPI(
    title="HELIOS API",
    version="2.0",
    description=(
        "Detecção de sonolência ao volante (TCC UNICID 2026). "
        "Recebe as leituras da câmera, guarda as fotos dos alertas em pastas "
        "separadas e entrega os dados para o app e para o painel."
    ),
)

# Sem isto, o app rodando no navegador (Expo web, outra porta) não consegue ler a API.
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

frames: deque = deque(maxlen=MAXIMO_DE_FRAMES_EM_MEMORIA)
proximo_id = 1
inicio_da_api = datetime.now().astimezone()


def agora_iso() -> str:
    return datetime.now().astimezone().isoformat(timespec="milliseconds")


def categoria_pelo_texto(status_motorista: str) -> str:
    """Para câmeras antigas que não mandam 'categoria' (ex.: simulador.py)."""
    return "SONOLENCIA" if status_motorista == "Sonolência Detectada" else "ATENTO"


def ultimo_frame(device_id: str) -> dict:
    for frame in reversed(frames):
        if frame["device_id"] == device_id:
            return frame
    raise HTTPException(status_code=404, detail="Nenhum frame encontrado para este dispositivo.")


def interpretar_horario(texto: Optional[str]) -> datetime:
    """Horário enviado pela câmera; se faltar ou vier errado, usa a hora de chegada."""
    try:
        return datetime.fromisoformat(texto).astimezone() if texto else datetime.now().astimezone()
    except ValueError:
        return datetime.now().astimezone()


def nome_do_evento(direcao: str, categoria: str, capturado_em: datetime, pasta: Path) -> str:
    """
    direção_rosto-TIPO-data_e_hora, ex.: frente-DORMINDO-2026-10-06-22-13-33.
    Se já existir um arquivo com esse nome (duas fotos no mesmo segundo), acrescenta -2, -3...
    """
    base = f"{direcao}-{TIPO_NO_NOME_DO_ARQUIVO[categoria]}-{capturado_em.strftime(FORMATO_DATA_NO_NOME)}"
    nome, repeticao = base, 2
    while any((pasta / f"{nome}{ext}").exists() for ext in (".jpg", ".png", ".json")):
        nome, repeticao = f"{base}-{repeticao}", repeticao + 1
    return nome


def salvar_evento(frame: dict, conteudo: bytes, extensao: str) -> str:
    """Copia a foto para eventos/<categoria>/ junto com um .json dos dados."""
    pasta = PASTA_POR_CATEGORIA_DE_ALERTA[frame["categoria"]]
    caminho_pasta = PASTA_EVENTOS / pasta
    nome = nome_do_evento(
        frame["direcao_rosto"], frame["categoria"],
        datetime.fromisoformat(frame["capturado_em"]), caminho_pasta,
    )
    (caminho_pasta / f"{nome}{extensao}").write_bytes(conteudo)
    dados = {**frame, "arquivo_evento": f"{nome}{extensao}"}
    (caminho_pasta / f"{nome}.json").write_text(
        json.dumps(dados, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    return f"{pasta}/{nome}{extensao}"


# ── Sistema ───────────────────────────────────────────────────────────────────


@app.get("/", tags=["Sistema"], summary="A API está no ar?")
def inicio():
    return {
        "sistema": "HELIOS",
        "status": "online",
        "versao": app.version,
        "no_ar_desde": inicio_da_api.isoformat(timespec="seconds"),
        "frames_em_memoria": len(frames),
        "painel": "/painel",
        "documentacao": "/docs",
    }


@app.get("/painel", tags=["Sistema"], summary="Painel visual do monitoramento")
def painel():
    return FileResponse(PAGINA_PAINEL, media_type="text/html")


# ── Câmera ────────────────────────────────────────────────────────────────────


@app.post("/frames", tags=["Câmera"], summary="Recebe uma foto e o resultado da análise")
async def receber_frame(
    file: UploadFile = File(...),
    frame_id: int = Form(...),
    device_id: str = Form(...),
    status_motorista: str = Form("Atento"),
    categoria: Optional[str] = Form(None),
    direcao_rosto: Optional[str] = Form(None),
    capturado_em: Optional[str] = Form(None),
    ear: Optional[float] = Form(None),
    ecf: Optional[float] = Form(None),
    mar: Optional[float] = Form(None),
    perclos: Optional[float] = Form(None),
    segundos_em_alerta: Optional[float] = Form(None),
    total_piscadas: Optional[int] = Form(None),
    total_bocejos: Optional[int] = Form(None),
):
    global proximo_id

    if not FORMATO_DEVICE_ID.match(device_id):
        raise HTTPException(status_code=400, detail="device_id inválido: use só letras, números, _ e - (até 32).")
    extensao = EXTENSAO_POR_TIPO.get(file.content_type or "")
    if extensao is None:
        raise HTTPException(status_code=400, detail="O arquivo enviado precisa ser uma imagem JPEG ou PNG.")
    categoria = categoria or categoria_pelo_texto(status_motorista)
    if categoria not in CATEGORIAS:
        raise HTTPException(status_code=400, detail=f"categoria inválida. Use uma de: {sorted(CATEGORIAS)}.")
    if direcao_rosto is not None and not FORMATO_DIRECAO.match(direcao_rosto):
        raise HTTPException(status_code=400, detail="direcao_rosto inválida: use letras minúsculas e _ (ex.: baixo_esquerda).")

    conteudo = await file.read()
    numero = proximo_id
    proximo_id += 1
    nome_arquivo = f"{device_id}_{numero:06d}{extensao}"
    (PASTA_FRAMES / nome_arquivo).write_bytes(conteudo)

    frame = {
        "id": numero,
        "frame_id": frame_id,
        "device_id": device_id,
        "timestamp": agora_iso(),
        "arquivo": nome_arquivo,
        "categoria": categoria,
        "alerta": categoria in PASTA_POR_CATEGORIA_DE_ALERTA,
        "status_motorista": status_motorista,
        "direcao_rosto": direcao_rosto or "desconhecida",
        "capturado_em": interpretar_horario(capturado_em).isoformat(timespec="seconds"),
        "ear": ear,
        "ecf": ecf,
        "mar": mar,
        "perclos": perclos,
        "segundos_em_alerta": segundos_em_alerta,
        "total_piscadas": total_piscadas,
        "total_bocejos": total_bocejos,
        "evento": None,
    }
    if frame["alerta"]:
        frame["evento"] = salvar_evento(frame, conteudo, extensao)

    frames.append(frame)
    return {"status": "recebido", "frame": Frame(**frame)}


# ── App ───────────────────────────────────────────────────────────────────────


@app.get("/frames", tags=["App"], summary="Últimos frames recebidos (mais novo primeiro)")
def listar_frames(device_id: Optional[str] = None, limite: int = Query(50, ge=1, le=MAXIMO_DE_FRAMES_EM_MEMORIA)):
    lista = [f for f in reversed(frames) if device_id is None or f["device_id"] == device_id]
    return {"total": len(lista), "frames": lista[:limite]}


@app.get("/frames/latest", tags=["App"], summary="Último frame de um dispositivo (usado pelo app Expo)")
def obter_ultimo_frame(device_id: str):
    return {"frame": ultimo_frame(device_id)}


@app.get("/frames/{frame_id}", tags=["App"], summary="Foto de um frame")
def obter_frame(frame_id: int, device_id: Optional[str] = None):
    # Do mais novo para o mais antigo: o frame_id recomeça em 1 a cada execução da câmera.
    for frame in reversed(frames):
        if frame["frame_id"] == frame_id and (device_id is None or frame["device_id"] == device_id):
            caminho = PASTA_FRAMES / frame["arquivo"]
            if not caminho.exists():
                raise HTTPException(status_code=404, detail="Arquivo do frame não encontrado.")
            return FileResponse(caminho, media_type=mimetypes.guess_type(caminho.name)[0])
    raise HTTPException(status_code=404, detail="Frame não encontrado.")


@app.get("/leituras/latest", tags=["App"], response_model=Leitura,
         summary="Última leitura, só dados (formato do app Kotlin)")
def obter_ultima_leitura(device_id: str):
    frame = ultimo_frame(device_id)
    idade = (datetime.now().astimezone() - datetime.fromisoformat(frame["timestamp"])).total_seconds()
    return Leitura(
        **{k: frame[k] for k in Leitura.model_fields if k in frame},
        idade_segundos=round(idade, 1),
        imagem_url=f"/frames/{frame['frame_id']}?device_id={device_id}",
    )


# ── Eventos (fotos de alerta salvas à parte) ──────────────────────────────────


def ler_eventos(categoria: Optional[str], device_id: Optional[str]) -> list[dict]:
    pastas = [PASTA_POR_CATEGORIA_DE_ALERTA[categoria]] if categoria else PASTA_POR_CATEGORIA_DE_ALERTA.values()
    eventos = []
    for pasta in pastas:
        for arquivo_json in (PASTA_EVENTOS / pasta).glob("*.json"):
            dados = json.loads(arquivo_json.read_text(encoding="utf-8"))
            if device_id is None or dados["device_id"] == device_id:
                dados["imagem_url"] = f"/eventos/{pasta}/{quote(dados['arquivo_evento'])}"
                eventos.append(dados)
    return sorted(eventos, key=lambda e: e["timestamp"], reverse=True)


@app.get("/eventos", tags=["Eventos"], response_model=list[Evento],
         summary="Alertas salvos (mais novo primeiro)")
def listar_eventos(
    categoria: Optional[Literal["SONOLENCIA", "DESATENTO", "DORMINDO"]] = None,
    device_id: Optional[str] = None,
    limite: int = Query(60, ge=1, le=1000),
):
    return ler_eventos(categoria, device_id)[:limite]


@app.get("/eventos/resumo", tags=["Eventos"], summary="Quantos alertas de cada categoria foram salvos")
def resumo_dos_eventos(device_id: Optional[str] = None):
    eventos = ler_eventos(None, device_id)
    contagem = {categoria: 0 for categoria in PASTA_POR_CATEGORIA_DE_ALERTA}
    for evento in eventos:
        contagem[evento["categoria"]] += 1
    return {"total": len(eventos), "por_categoria": contagem}


@app.get("/eventos/{pasta}/{arquivo}", tags=["Eventos"], summary="Foto de um alerta")
def obter_foto_do_evento(pasta: str, arquivo: str):
    if pasta not in PASTA_POR_CATEGORIA_DE_ALERTA.values() or not FORMATO_ARQUIVO_DE_EVENTO.match(arquivo):
        raise HTTPException(status_code=404, detail="Evento não encontrado.")
    caminho = PASTA_EVENTOS / pasta / arquivo
    if not caminho.exists():
        raise HTTPException(status_code=404, detail="Evento não encontrado.")
    return FileResponse(caminho, media_type=mimetypes.guess_type(arquivo)[0])
