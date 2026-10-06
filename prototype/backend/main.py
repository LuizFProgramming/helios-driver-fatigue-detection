from datetime import datetime
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.responses import FileResponse

app = FastAPI(title="HELIOS API")

# Pasta para salvar as imagens recebidas
UPLOAD_DIR = Path("uploads")
UPLOAD_DIR.mkdir(exist_ok=True)

# Banco de dados temporário em memória
frames = []


@app.get("/")
def inicio():
    return {"sistema": "HELIOS", "status": "online"}


@app.post("/frames")
async def receber_frame(
    file: UploadFile = File(...),
    frame_id: int = Form(...),
    device_id: str = Form(...),
    status_motorista: str = Form("Atento"),
    ear: Optional[float] = Form(None),
    mar: Optional[float] = Form(None),
    perclos: Optional[float] = Form(None),
):
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code=400, detail="O arquivo enviado não é uma imagem."
        )

    timestamp = datetime.now().isoformat()
    extensao = Path(file.filename).suffix.lower() or ".jpg"
    nome_arquivo = f"{device_id}_frame_{frame_id}{extensao}"
    caminho = UPLOAD_DIR / nome_arquivo

    conteudo = await file.read()
    with open(caminho, "wb") as arquivo:
        arquivo.write(conteudo)

    frame = {
        "frame_id": frame_id,
        "device_id": device_id,
        "timestamp": timestamp,
        "arquivo": nome_arquivo,
        "status_motorista": status_motorista,
        "ear": ear,
        "mar": mar,
        "perclos": perclos,
    }

    frames.append(frame)
    return {"status": "recebido", "frame": frame}


@app.get("/frames")
def listar_frames():
    return {"total": len(frames), "frames": frames}


@app.get("/frames/latest")
def obter_ultimo_frame(device_id: str):
    frames_dispositivos = [f for f in frames if f["device_id"] == device_id]
    if not frames_dispositivos:
        raise HTTPException(
            status_code=404,
            detail="Nenhum frame encontrado para este dispositivo.",
        )
    return {"frame": frames_dispositivos[-1]}


@app.get("/frames/{frame_id}")
def obter_frame(frame_id: int):
    for frame in frames:
        if frame["frame_id"] == frame_id:
            caminho = UPLOAD_DIR / frame["arquivo"]
            if not caminho.exists():
                raise HTTPException(
                    status_code=404, detail="Arquivo do frame não encontrado."
                )
            return FileResponse(caminho, media_type="image/jpeg")
    raise HTTPException(status_code=404, detail="Frame não encontrado.")