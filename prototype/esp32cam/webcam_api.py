import cv2
import requests
import time

API_URL = "http://127.0.0.1:8000/frames"
DEVICE_ID = "WEBCAM_NOTEBOOK"
INTERVALO = 2
CAMERA_INDEX = 0

camera = cv2.VideoCapture(CAMERA_INDEX)
if not camera.isOpened():
    print("❌ Não foi possível acessar a câmera.")
    exit()

print("📷 Câmera conectada e transmitindo para o HELIOS!")

frame_id = 1

while True:
    sucesso, frame = camera.read()
    if not sucesso:
        break

    # Converter frame para JPG
    sucesso, imagem_jpg = cv2.imencode(".jpg", frame)
    if not sucesso:
        continue

    imagem_bytes = imagem_jpg.tobytes()

    # Prepara o arquivo de imagem
    arquivos = {
        "file": (f"webcam_frame_{frame_id}.jpg", imagem_bytes, "image/jpeg")
    }

    # Valores simulados (Substituir pelas variáveis calculadas pelo MediaPipe Face Mesh)
    ear_calculado = 0.28
    mar_calculado = 0.12
    perclos_calculado = 15.0  # Em percentual (%)

    # Regra de status simples baseada nos limiares
    status_atual = "Sonolência Detectada" if ear_calculado < 0.21 or perclos_calculado > 40 else "Atento"

    # Envia as métricas convertidas para string/float no payload form-data
    dados = {
        "frame_id": str(frame_id),
        "device_id": DEVICE_ID,
        "status_motorista": status_atual,
        "ear": str(ear_calculado),
        "mar": str(mar_calculado),
        "perclos": str(perclos_calculado),
    }

    try:
        resposta = requests.post(API_URL, files=arquivos, data=dados)
        if resposta.status_code == 200:
            print(f"✅ Frame {frame_id} enviado | Status: {status_atual} | EAR: {ear_calculado:.2f} | PERCLOS: {perclos_calculado}%")
        else:
            print(f"❌ Erro ao enviar: {resposta.status_code} - {resposta.text}")
    except Exception as erro:
        print(f"❌ Erro de conexão: {erro}")

    frame_id += 1
    time.sleep(INTERVALO)

    if cv2.waitKey(1) == 27:  # Pressione ESC para sair
        break

camera.release()
cv2.destroyAllWindows()