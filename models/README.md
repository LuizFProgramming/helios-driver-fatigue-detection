# Modelos

Modelos treinados **não são versionados**. Documente aqui onde baixar cada um, versão e métricas.

| Modelo | Versão | Link | Métricas |
| --- | --- | --- | --- |
| MediaPipe Face Landmarker (`face_landmarker.task`, ~3,7 MB) | float16 / 1 | [download oficial (Google)](https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task) | Pré-treinado pelo Google; 478 pontos do rosto + blendshapes. Nenhuma métrica medida no HELIOS ainda. |

## Onde colocar

- `face_landmarker.task` → `prototype/prototipo_desktop_python/face_landmarker.task` (caminho definido em `config.py`, `CAMINHO_MODELO_MEDIAPIPE`).

Windows (PowerShell, na raiz do repositório):

```powershell
Invoke-WebRequest -Uri "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task" -OutFile "prototype/prototipo_desktop_python/face_landmarker.task"
```
