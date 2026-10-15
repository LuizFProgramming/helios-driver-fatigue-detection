# HELIOS: app Android de detecção de sonolência (TCC UNICID 2026)

Leia `.claude/skills/helios-desenvolvimento/SKILL.md` antes de mexer no código.

- Pipeline: Captura (CameraX ou ESP32-CAM) → realce OpenCV opcional → MediaPipe Face Landmarker → `MonitorMotorista` (EAR, MAR, ECF, PERCLOS, pose, acelerômetro) → ATENTO / FADIGA / CRÍTICO → alarme sonoro, vibração, tela.
- Módulos: `dominio` (puro), `protocolos` (puro), `app` (Android, adaptadores finos).
- Documentação do TCC: `../02_Documentacao/`. Protótipo Python de referência: `../03_Codigo/prototipo_desktop_python/`.
- Textos em português. Na escrita de documentos, evitar travessões em excesso, frases redundantes e conectivos mecânicos.
- Não adicionar comportamentos defensivos ou de segurança não pedidos sem avisar antes.
- Antes de dizer que terminou: `./gradlew :dominio:test :protocolos:test` verde; para entregas maiores, `./gradlew qualidade`.
