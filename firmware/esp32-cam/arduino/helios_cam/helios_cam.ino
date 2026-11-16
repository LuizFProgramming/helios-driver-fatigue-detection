/*
 * HELIOS-CAM: firmware único da ESP32-CAM (AI-Thinker + OV2640).
 *
 * Esta pasta é ao mesmo tempo o sketch da Arduino IDE e o src_dir do PlatformIO,
 * então existe uma só cópia do código. Toda configuração fica em config.h.
 *
 * Endpoints (iguais aos que o APK usa):
 *   GET  http://192.168.4.1:81/stream        MJPEG, com X-Timestamp em cada quadro
 *   GET  http://192.168.4.1/status           JSON com fps, clientes, câmera, OLED e buzzer
 *   POST http://192.168.4.1/buzzer?padrao=N  0 desliga, 1 atenção/fadiga, 2 crítico
 *   GET  http://192.168.4.1/capture          um JPEG avulso
 *   GET  http://192.168.4.1/                 página de teste com o vídeo
 */
#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include "esp_camera.h"

#include "config.h"
#include "camera_pins.h"

#if defined(ESP_ARDUINO_VERSION_MAJOR) && ESP_ARDUINO_VERSION_MAJOR >= 3
#define HELIOS_CORE3 1
#else
#define HELIOS_CORE3 0
#endif

static const char STREAM_CONTENT_TYPE[] = "multipart/x-mixed-replace;boundary=frame";
static const char STREAM_BOUNDARY[] = "\r\n--frame\r\n";

static WebServer controlServer(HELIOS_CONTROL_PORT);
static WiFiServer streamServer(HELIOS_STREAM_PORT);
static WiFiClient streamClient;
static Adafruit_SSD1306 display(HELIOS_OLED_LARGURA, HELIOS_OLED_ALTURA, &Wire, -1);

static bool cameraAvailable = false;
static bool displayAvailable = false;
static bool modoEstacao = false;
static uint8_t padraoAtual = 0;
static bool buzzerSoando = false;
static uint32_t ultimaTrocaBuzzer = 0;
static uint32_t framesSent = 0;
static uint32_t streamStartedAt = 0;
static int ultimoNumeroClientes = -1;

// Ícones 16x16 do display (café e aviso).
static const unsigned char ICONE_CAFE[] PROGMEM = {
    0x00, 0x00, 0x14, 0x28, 0x08, 0x10, 0x14, 0x28, 0x7f, 0xf0, 0x40, 0x1c, 0x40, 0x06, 0x40, 0x06,
    0x40, 0x1c, 0x3f, 0xf0, 0x1f, 0xe0, 0x0f, 0xc0, 0x03, 0x80, 0x7f, 0xfc, 0x7f, 0xfc, 0x00, 0x00};
static const unsigned char ICONE_AVISO[] PROGMEM = {
    0x01, 0x80, 0x03, 0xc0, 0x03, 0xc0, 0x07, 0xe0, 0x06, 0x60, 0x0c, 0x30, 0x0c, 0x30, 0x18, 0x18,
    0x18, 0x18, 0x31, 0x8c, 0x31, 0x8c, 0x60, 0x06, 0x61, 0x86, 0x7f, 0xfe, 0x7f, 0xfe, 0x00, 0x00};

// ---------------------------------------------------------------------------
// Buzzer
// ---------------------------------------------------------------------------

#if !HELIOS_BUZZER_ATIVO
static uint32_t frequenciaDoPadrao(uint8_t padrao) {
  return padrao == 2 ? HELIOS_PADRAO2_FREQUENCIA_HZ : HELIOS_PADRAO1_FREQUENCIA_HZ;
}
#endif

static void saidaBuzzer(bool ligado) {
#if HELIOS_BUZZER_ATIVO
  const int desligado = HELIOS_BUZZER_NIVEL_LIGADO == HIGH ? LOW : HIGH;
  digitalWrite(HELIOS_BUZZER_GPIO, ligado ? HELIOS_BUZZER_NIVEL_LIGADO : desligado);
#elif HELIOS_CORE3
  if (ligado) ledcWriteTone(HELIOS_BUZZER_GPIO, frequenciaDoPadrao(padraoAtual));
  ledcWrite(HELIOS_BUZZER_GPIO, ligado ? HELIOS_BUZZER_DUTY : 0);
#else
  if (ligado) ledcWriteTone(HELIOS_BUZZER_CHANNEL, frequenciaDoPadrao(padraoAtual));
  ledcWrite(HELIOS_BUZZER_CHANNEL, ligado ? HELIOS_BUZZER_DUTY : 0);
#endif
}

static void configurarBuzzer() {
#if HELIOS_BUZZER_ATIVO
  pinMode(HELIOS_BUZZER_GPIO, OUTPUT);
#elif HELIOS_CORE3
  ledcAttach(HELIOS_BUZZER_GPIO, HELIOS_PADRAO2_FREQUENCIA_HZ, HELIOS_BUZZER_RESOLUTION);
#else
  ledcSetup(HELIOS_BUZZER_CHANNEL, HELIOS_PADRAO2_FREQUENCIA_HZ, HELIOS_BUZZER_RESOLUTION);
  ledcAttachPin(HELIOS_BUZZER_GPIO, HELIOS_BUZZER_CHANNEL);
#endif
  saidaBuzzer(false);
}

// Mantém o ritmo do padrão atual sem bloquear o loop (o stream continua enquanto apita).
static void atualizarBuzzer() {
  if (padraoAtual == 0) {
    if (buzzerSoando) saidaBuzzer(false);
    buzzerSoando = false;
    return;
  }
  const uint32_t ligadoMs = padraoAtual == 2 ? HELIOS_PADRAO2_LIGADO_MS : HELIOS_PADRAO1_LIGADO_MS;
  const uint32_t desligadoMs = padraoAtual == 2 ? HELIOS_PADRAO2_DESLIGADO_MS : HELIOS_PADRAO1_DESLIGADO_MS;
  const uint32_t agora = millis();
  if (agora - ultimaTrocaBuzzer >= (buzzerSoando ? ligadoMs : desligadoMs)) {
    ultimaTrocaBuzzer = agora;
    buzzerSoando = !buzzerSoando;
    saidaBuzzer(buzzerSoando);
  }
}

// ---------------------------------------------------------------------------
// Display OLED
// ---------------------------------------------------------------------------

static void desenharCabecalho() {
  display.setTextSize(1);
  display.setCursor(0, 0);
  if (modoEstacao) {
    display.print("HELIOS ");
    display.print(WiFi.localIP());
  } else {
    display.print("HELIOS | AP | ");
    display.print(WiFi.softAPgetStationNum());
    display.print(" cli");
  }
  display.drawFastHLine(0, 9, HELIOS_OLED_LARGURA, SSD1306_WHITE);
}

static void desenharTela() {
  if (!displayAvailable) return;
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);
  desenharCabecalho();
  display.setTextSize(1);
  if (!cameraAvailable) {
    display.drawBitmap(6, 22, ICONE_AVISO, 16, 16, SSD1306_WHITE);
    display.setCursor(30, 22);
    display.print("Falha na camera");
    display.setCursor(30, 36);
    display.print("Verifique o cabo");
  } else if (padraoAtual == 0) {
    display.setCursor(20, 25);
    display.print("MONITORANDO...");
    display.setCursor(15, 42);
    display.print("Motorista atento");
  } else if (padraoAtual == 1) {
    display.drawBitmap(10, 20, ICONE_CAFE, 16, 16, SSD1306_WHITE);
    display.setCursor(30, 20);
    display.print("ATENCAO");
    display.setCursor(30, 34);
    display.print("HORA DE PAUSA");
    display.setCursor(30, 48);
    display.print("TOME UM CAFE!");
  } else {
    display.drawBitmap(6, 16, ICONE_AVISO, 16, 16, SSD1306_WHITE);
    display.drawBitmap(106, 16, ICONE_AVISO, 16, 16, SSD1306_WHITE);
    display.setTextSize(2);
    display.setCursor(28, 18);
    display.print("PERIGO");
    display.setTextSize(1);
    display.setCursor(10, 42);
    display.print("SONO CRITICO!");
    display.setCursor(10, 53);
    display.print("PARE O VEICULO!");
  }
  display.display();
}

static void mostrarMensagem(const char* linha1, const char* linha2) {
  if (!displayAvailable) return;
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  display.setCursor(0, 20);
  display.println(linha1);
  display.setCursor(0, 36);
  display.println(linha2);
  display.display();
}

// Redesenha só quando muda o número de clientes, para não travar o stream com I2C à toa.
static void atualizarTelaSeClientesMudaram() {
  if (modoEstacao) return;
  const int clientes = WiFi.softAPgetStationNum();
  if (clientes == ultimoNumeroClientes) return;
  ultimoNumeroClientes = clientes;
  desenharTela();
}

static void definirPadrao(uint8_t padrao) {
  if (padrao == padraoAtual) return;
  padraoAtual = padrao;
  buzzerSoando = false;
  ultimaTrocaBuzzer = 0;
  saidaBuzzer(false);
  desenharTela();
}

// ---------------------------------------------------------------------------
// Câmera
// ---------------------------------------------------------------------------

static bool inicializarCamera() {
  camera_config_t config = {};
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sccb_sda = SIOD_GPIO_NUM;
  config.pin_sccb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size = HELIOS_FRAME_SIZE;
  config.jpeg_quality = HELIOS_JPEG_QUALITY;
  const bool psram = psramFound();
  config.fb_count = psram ? 2 : 1;
  config.grab_mode = psram ? CAMERA_GRAB_LATEST : CAMERA_GRAB_WHEN_EMPTY;
  config.fb_location = psram ? CAMERA_FB_IN_PSRAM : CAMERA_FB_IN_DRAM;

  const esp_err_t erro = esp_camera_init(&config);
  if (erro != ESP_OK) {
    Serial.printf("Falha ao inicializar a camera: 0x%x\n", erro);
    return false;
  }
  // Ajustes de imagem do commit "Improve ESP32-CAM image streaming".
  sensor_t* sensor = esp_camera_sensor_get();
  sensor->set_framesize(sensor, HELIOS_FRAME_SIZE);
  sensor->set_brightness(sensor, 1);
  sensor->set_contrast(sensor, 1);
  sensor->set_saturation(sensor, 0);
  sensor->set_whitebal(sensor, 1);
  return true;
}

// ---------------------------------------------------------------------------
// Rede
// ---------------------------------------------------------------------------

static void iniciarPontoDeAcesso() {
  IPAddress ip(HELIOS_IP);
  IPAddress gateway(HELIOS_GATEWAY);
  IPAddress netmask(HELIOS_NETMASK);
  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(ip, gateway, netmask);
  WiFi.softAP(HELIOS_WIFI_SSID, strlen(HELIOS_WIFI_PASSWORD) == 0 ? nullptr : HELIOS_WIFI_PASSWORD,
              HELIOS_WIFI_CHANNEL, false, HELIOS_WIFI_MAX_CLIENTS);
  modoEstacao = false;
  Serial.print("Rede HELIOS-CAM criada em http://");
  Serial.println(WiFi.softAPIP());
}

#ifdef HELIOS_WIFI_MODO_ESTACAO
// Só para bancada (secrets.h). Se não conectar em 20 s, cai para o ponto de acesso normal.
static bool conectarComoEstacao() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(HELIOS_ESTACAO_SSID, HELIOS_ESTACAO_SENHA);
  const uint32_t inicio = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - inicio < 20000UL) {
    delay(500);
    Serial.print(".");
  }
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("\nNao conectou na rede de bancada; criando a HELIOS-CAM.");
    WiFi.disconnect();
    return false;
  }
  modoEstacao = true;
  Serial.print("\nConectado na rede de bancada. IP: ");
  Serial.println(WiFi.localIP());
  return true;
}
#endif

static void iniciarRede() {
#ifdef HELIOS_WIFI_MODO_ESTACAO
  if (conectarComoEstacao()) return;
#endif
  iniciarPontoDeAcesso();
}

// ---------------------------------------------------------------------------
// Servidor de controle (porta 80)
// ---------------------------------------------------------------------------

static void handleRaiz() {
  static const char pagina[] =
      "<!doctype html><html lang='pt-BR'><head>"
      "<meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
      "<title>HELIOS-CAM</title>"
      "<style>body{font-family:Arial;background:#111;color:#fff;text-align:center;margin:20px}"
      "img{max-width:100%;height:auto;border:2px solid #0f0}</style>"
      "</head><body><h1>HELIOS-CAM</h1><p>ESP32-CAM online</p>"
      "<p><img id='camera' alt='Camera'></p>"
      "<script>document.getElementById('camera').src='http://'+location.hostname+':81/stream';</script>"
      "</body></html>";
  controlServer.send(200, "text/html; charset=utf-8", pagina);
}

static void handleStatus() {
  const uint32_t decorrido = millis() - streamStartedAt;
  const uint32_t fps = (streamClient && streamClient.connected() && decorrido > 0)
                           ? (framesSent * 1000UL) / decorrido
                           : 0;
  String corpo = "{\"sistema\":\"HELIOS-CAM\",\"status\":\"online\"";
  corpo += ",\"fps\":" + String(fps);
  corpo += ",\"clientes\":" + String(modoEstacao ? 0 : WiFi.softAPgetStationNum());
  corpo += ",\"buzzer\":" + String(padraoAtual);
  corpo += ",\"buzzer_ativo\":" + String(HELIOS_BUZZER_ATIVO ? "true" : "false");
  corpo += ",\"camera\":" + String(cameraAvailable ? "true" : "false");
  corpo += ",\"oled\":" + String(displayAvailable ? "true" : "false");
  corpo += ",\"modo\":\"" + String(modoEstacao ? "estacao" : "ap") + "\"";
  corpo += ",\"uptime_ms\":" + String(millis()) + "}";
  controlServer.sendHeader("Access-Control-Allow-Origin", "*");
  controlServer.send(200, "application/json", corpo);
}

static void handleBuzzer() {
  if (!controlServer.hasArg("padrao")) {
    controlServer.send(400, "text/plain", "parametro padrao obrigatorio");
    return;
  }
  const String valor = controlServer.arg("padrao");
  if (valor.length() != 1 || valor[0] < '0' || valor[0] > '2') {
    controlServer.send(400, "text/plain", "padrao deve ser 0, 1 ou 2");
    return;
  }
  definirPadrao(static_cast<uint8_t>(valor[0] - '0'));
  controlServer.send(204, "text/plain", "");
}

static void handleCaptura() {
  if (!cameraAvailable) {
    controlServer.send(503, "text/plain; charset=utf-8", "Camera indisponivel");
    return;
  }
  camera_fb_t* frame = esp_camera_fb_get();
  if (frame == nullptr) {
    controlServer.send(500, "text/plain; charset=utf-8", "Falha ao capturar imagem");
    return;
  }
  WiFiClient cliente = controlServer.client();
  cliente.print("HTTP/1.1 200 OK\r\nContent-Type: image/jpeg\r\nContent-Length: ");
  cliente.print(frame->len);
  cliente.print("\r\nAccess-Control-Allow-Origin: *\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n");
  cliente.write(frame->buf, frame->len);
  esp_camera_fb_return(frame);
}

static void iniciarServidores() {
  controlServer.on("/", HTTP_GET, handleRaiz);
  controlServer.on("/status", HTTP_GET, handleStatus);
  controlServer.on("/buzzer", HTTP_POST, handleBuzzer);
  controlServer.on("/capture", HTTP_GET, handleCaptura);
  controlServer.onNotFound([]() { controlServer.send(404, "text/plain", "nao encontrado"); });
  controlServer.begin();
  streamServer.begin();
}

// ---------------------------------------------------------------------------
// Stream MJPEG (porta 81, um cliente por vez)
// ---------------------------------------------------------------------------

static void aceitarClienteStream() {
  if (streamClient && streamClient.connected()) return;
  if (streamClient) streamClient.stop();
  streamClient = streamServer.available();
  if (!streamClient) return;
  streamClient.setNoDelay(true);
  streamClient.print("HTTP/1.1 200 OK\r\nContent-Type: ");
  streamClient.print(STREAM_CONTENT_TYPE);
  streamClient.print("\r\nCache-Control: no-cache\r\n"
                     "Access-Control-Allow-Origin: *\r\n"
                     "Connection: close\r\n\r\n");
  streamStartedAt = millis();
  framesSent = 0;
}

static void enviarQuadro() {
  if (!cameraAvailable || !streamClient || !streamClient.connected()) return;
  camera_fb_t* frame = esp_camera_fb_get();
  if (!frame) return;

  // X-Timestamp: instante da captura no relógio da ESP32 (s.us), para medir latência ponta a ponta no app.
  char cabecalho[160];
  const int tamanho = snprintf(cabecalho, sizeof(cabecalho),
                               "%sContent-Type: image/jpeg\r\nContent-Length: %u\r\nX-Timestamp: %ld.%06ld\r\n\r\n",
                               STREAM_BOUNDARY, static_cast<unsigned>(frame->len),
                               static_cast<long>(frame->timestamp.tv_sec),
                               static_cast<long>(frame->timestamp.tv_usec));
  const bool enviado =
      streamClient.write(reinterpret_cast<const uint8_t*>(cabecalho), tamanho) == static_cast<size_t>(tamanho) &&
      streamClient.write(frame->buf, frame->len) == frame->len;
  esp_camera_fb_return(frame);
  if (enviado) {
    ++framesSent;
  } else {
    streamClient.stop();
  }
}

// ---------------------------------------------------------------------------
// Demonstração sem o app
// ---------------------------------------------------------------------------

static void avancarDemo() {
#if HELIOS_MODO_DEMO
  static uint32_t ultimaTroca = 0;
  const uint32_t agora = millis();
  if (agora - ultimaTroca < HELIOS_DEMO_INTERVALO_MS) return;
  ultimaTroca = agora;
  definirPadrao(static_cast<uint8_t>((padraoAtual + 1) % 3));
#endif
}

// ---------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
  pinMode(LED_GPIO_NUM, OUTPUT);
  digitalWrite(LED_GPIO_NUM, LOW);  // flash desligado: luz forte no rosto atrapalha o motorista
  configurarBuzzer();

  Wire.begin(HELIOS_I2C_SDA, HELIOS_I2C_SCL);
  displayAvailable = display.begin(SSD1306_SWITCHCAPVCC, HELIOS_OLED_ENDERECO);
  if (displayAvailable) {
    Wire.setClock(400000);
    mostrarMensagem("Iniciando HELIOS...", "");
  } else {
    Serial.println("OLED nao encontrado; seguindo sem display.");
  }

  cameraAvailable = inicializarCamera();
  iniciarRede();
  iniciarServidores();
  desenharTela();
  Serial.println(cameraAvailable ? "Camera pronta. Stream na porta 81." : "Sem camera: so /status e /buzzer.");
}

void loop() {
  controlServer.handleClient();
  aceitarClienteStream();
  enviarQuadro();
  atualizarBuzzer();
  atualizarTelaSeClientesMudaram();
  avancarDemo();
  delay(1);
}
