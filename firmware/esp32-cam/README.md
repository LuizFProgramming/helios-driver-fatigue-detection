# Firmware ESP32-CAM

Firmware único da ESP32-CAM AI-Thinker com OV2640, compatível com o leitor de MJPEG e o buzzer do APK Android.

Até a versão anterior havia dois programas diferentes: `src/main.cpp` (rede HELIOS-CAM e `/buzzer`, que o APK usa) e
`helios_cam.ino` (rede de casa, display OLED e ciclo de demonstração). Agora existe uma cópia só, na pasta do
sketch, usada tanto pela Arduino IDE quanto pelo PlatformIO.

```
firmware/esp32-cam/
├── platformio.ini                 src_dir aponta para a pasta do sketch
└── arduino/helios_cam/
    ├── helios_cam.ino             todo o código
    ├── config.h                   única coisa que se edita
    ├── camera_pins.h              pinos da câmera AI-Thinker
    └── secrets.example.h          modelo para teste na rede de casa (o secrets.h não vai para o git)
```

## Endpoints

| Método e caminho | Uso |
| --- | --- |
| `GET http://192.168.4.1:81/stream` | MJPEG. Cada quadro leva `X-Timestamp` (instante da captura no relógio da ESP32), para medir latência |
| `GET http://192.168.4.1/status` | JSON: `fps`, `clientes`, `buzzer`, `buzzer_ativo`, `camera`, `oled`, `modo`, `uptime_ms` |
| `POST http://192.168.4.1/buzzer?padrao=0\|1\|2` | 0 desliga, 1 atenção/fadiga, 2 crítico. Também troca a tela do OLED |
| `GET http://192.168.4.1/capture` | Um JPEG avulso |
| `GET http://192.168.4.1/` | Página de teste com o vídeo |

Os códigos 1 e 2 são os que o APK manda (`protocolos/Alarme.kt`). O ritmo de cada um está em `config.h`.

## Ligações

| Módulo | Pino do módulo | ESP32-CAM |
| --- | --- | --- |
| OLED SSD1306 (I²C, 0x3C) | SDA / SCL | GPIO 15 / GPIO 14 |
| OLED | VCC / GND | 3,3 V / GND |
| Buzzer | Sinal (I/O) | **GPIO 13** |
| Buzzer | VCC / GND | 3,3 V ou 5 V (conforme o módulo) / GND |

GND comum para tudo. O microSD não pode ser usado junto, porque divide os GPIO 14, 15 e 13.

**O buzzer saiu do GPIO 12.** O GPIO 12 define a tensão da flash na partida: se o módulo do buzzer o puxar para nível
alto durante o boot, a placa não inicia ou reinicia sem motivo aparente. Se o buzzer já estiver soldado no 12, mude o
fio para o 13 (ou, se não houver como, troque `HELIOS_BUZZER_GPIO` em `config.h` sabendo do risco).

**Ativo ou passivo.** `HELIOS_BUZZER_ATIVO 1` serve para o buzzer ativo (tom fixo, por volta de 2,5 kHz, o firmware
só liga e desliga). Para o passivo, use `0` e o firmware gera o tom por PWM: 880 Hz no padrão 1 e 2000 Hz no padrão 2, os mesmos do app. Para
descobrir qual é: com o pino de sinal em nível alto fixo, o ativo apita contínuo e o passivo fica mudo. Se o seu
apitar parado e calar no alerta, ele dispara em nível baixo: troque `HELIOS_BUZZER_NIVEL_LIGADO` para `LOW`.

## Arduino IDE

1. Instale `esp32 by Espressif Systems` no Boards Manager (testado com a 2.0.17).
2. Em Library Manager, instale **Adafruit SSD1306** (ele pede o Adafruit GFX e o BusIO; aceite).
3. Abra a pasta `firmware/esp32-cam/arduino/helios_cam` como sketch.
4. Placa `AI Thinker ESP32-CAM`, porta serial do conversor da base ESP32-CAM-MB.
5. Faça o upload (na base MB, segure IO0 se ela não entrar sozinha em modo de gravação).
6. Serial Monitor em 115200.

## PlatformIO

Abra `firmware/esp32-cam` no VS Code com PlatformIO e rode `Upload`. As bibliotecas do OLED estão em `lib_deps`.

## Teste na rede de casa (bancada)

Copie `secrets.example.h` para `secrets.h`, na mesma pasta, e preencha nome e senha da rede. Com isso a placa entra na
rede de casa e mostra o IP no Serial Monitor e no OLED. Se não conectar em 20 s, ela cria a HELIOS-CAM normalmente.
O `secrets.h` está no `.gitignore`. Para usar com o APK no carro, apague o `secrets.h`.

## Demonstração sem o app

Com `HELIOS_MODO_DEMO 1`, a placa troca sozinha de estado a cada 6 s (monitorando, atenção, crítico), com tela e
buzzer. Serve para fotografar a case e testar a montagem. Volte para `0` antes de usar com o APK.

## Teste com o computador ou o celular

Conecte na rede `HELIOS-CAM` e rode:

```powershell
curl.exe http://192.168.4.1/status
curl.exe -X POST "http://192.168.4.1/buzzer?padrao=1"
curl.exe -X POST "http://192.168.4.1/buzzer?padrao=2"
curl.exe -X POST "http://192.168.4.1/buzzer?padrao=0"
```

Abra `http://192.168.4.1/` para ver o vídeo. Depois selecione `ESP32-CAM pelo Wi-Fi` nos Ajustes do APK e ligue
"Tocar também o buzzer da câmera".

## Verificação

Compilado com arduino-cli e o core esp32 2.0.17 (`esp32:esp32:esp32cam`, `--warnings all`) nas combinações buzzer
ativo e passivo, modo demonstração e modo estação: sem erros nem avisos, 869 KB de programa (27%) e 50 KB de RAM
(15%). Ainda não foi testado na placa.
