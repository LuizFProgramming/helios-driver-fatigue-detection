#include "esp_camera.h"
#include <WiFi.h>
#include <WebServer.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

const char* ssid = "Familiasouza";
const char* password = "Souza135";

#define I2C_SDA 15
#define I2C_SCL 14
#define BUZZER_PIN 12

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);
bool displayAvailable = false;
bool cameraAvailable = false;
bool controleRemoto = false;
WebServer webServer(80);
WiFiServer streamServer(81);
WiFiClient streamClient;

#define PWDN_GPIO_NUM 32
#define RESET_GPIO_NUM -1
#define XCLK_GPIO_NUM 0
#define SIOD_GPIO_NUM 26
#define SIOC_GPIO_NUM 27
#define Y9_GPIO_NUM 35
#define Y8_GPIO_NUM 34
#define Y7_GPIO_NUM 39
#define Y6_GPIO_NUM 36
#define Y5_GPIO_NUM 21
#define Y4_GPIO_NUM 19
#define Y3_GPIO_NUM 18
#define Y2_GPIO_NUM 5
#define VSYNC_GPIO_NUM 25
#define HREF_GPIO_NUM 23
#define PCLK_GPIO_NUM 22

const unsigned char epd_bitmap_coffee[] PROGMEM = {
  0x00, 0x00, 0x14, 0x28, 0x08, 0x10, 0x14, 0x28,
  0x7f, 0xf0, 0x40, 0x1c, 0x40, 0x06, 0x40, 0x06,
  0x40, 0x1c, 0x3f, 0xf0, 0x1f, 0xe0, 0x0f, 0xc0,
  0x03, 0x80, 0x7f, 0xfc, 0x7f, 0xfc, 0x00, 0x00
};

const unsigned char epd_bitmap_warning[] PROGMEM = {
  0x01, 0x80, 0x03, 0xc0, 0x03, 0xc0, 0x07, 0xe0,
  0x06, 0x60, 0x0c, 0x30, 0x0c, 0x30, 0x18, 0x18,
  0x18, 0x18, 0x31, 0x8c, 0x31, 0x8c, 0x60, 0x06,
  0x61, 0x86, 0x7f, 0xfe, 0x7f, 0xfe, 0x00, 0x00
};

enum NivelAlerta {
  NORMAL = 0,
  LEVE = 1,
  MODERADO = 2,
  CRITICO = 3
};

NivelAlerta estadoAtual = NORMAL;
unsigned long ultimoAlerta = 0;
unsigned long ultimoBipe = 0;
bool buzzerLigado = false;

void mostrarMensagem(const char* linha1, const char* linha2 = nullptr) {
  if (!displayAvailable) return;
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  display.setCursor(0, 20);
  display.println(linha1);
  if (linha2 != nullptr) {
    display.setCursor(0, 36);
    display.println(linha2);
  }
  display.display();
}

bool inicializarCamera() {
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

  if (psramFound()) {
    config.frame_size = FRAMESIZE_VGA;
    config.jpeg_quality = 8;
    config.fb_count = 2;
    config.grab_mode = CAMERA_GRAB_LATEST;
    config.fb_location = CAMERA_FB_IN_PSRAM;
  } else {
    config.frame_size = FRAMESIZE_VGA;
    config.jpeg_quality = 10;
    config.fb_count = 1;
    config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
    config.fb_location = CAMERA_FB_IN_DRAM;
  }

  const esp_err_t error = esp_camera_init(&config);
  if (error != ESP_OK) {
    Serial.printf("Falha na inicializacao da camera: 0x%x\n", error);
    return false;
  }

  sensor_t* sensor = esp_camera_sensor_get();
  sensor->set_brightness(sensor, 1);
  sensor->set_contrast(sensor, 1);
  sensor->set_saturation(sensor, 0);
  sensor->set_whitebal(sensor, 1);
  return true;
}

void desenharTela(NivelAlerta nivel) {
  if (!displayAvailable) return;
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  display.setCursor(0, 0);
  display.print(WiFi.status() == WL_CONNECTED ? "HELIOS | Wi-Fi OK" : "HELIOS | Wi-Fi OFF");
  display.drawFastHLine(0, 9, 128, SSD1306_WHITE);

  if (nivel == NORMAL) {
    display.setCursor(20, 25);
    display.print("MONITORANDO...");
    display.setCursor(15, 42);
    display.print("Motorista Atento");
  } else if (nivel == LEVE) {
    display.drawBitmap(10, 20, epd_bitmap_coffee, 16, 16, SSD1306_WHITE);
    display.setCursor(30, 20);
    display.print("FADIGA DETECTADA");
    display.setCursor(30, 34);
    display.print("HORA DE PAUSA");
    display.setCursor(30, 48);
    display.print("TOME UM CAFE!");
  } else if (nivel == MODERADO) {
    display.drawBitmap(10, 22, epd_bitmap_warning, 16, 16, SSD1306_WHITE);
    display.setCursor(35, 22);
    display.print("ATENCAO!");
    display.setCursor(35, 36);
    display.print("Olhos fechados");
    display.setCursor(35, 50);
    display.print("Reduza a vel.");
  } else {
    display.drawBitmap(6, 16, epd_bitmap_warning, 16, 16, SSD1306_WHITE);
    display.drawBitmap(106, 16, epd_bitmap_warning, 16, 16, SSD1306_WHITE);
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

void gerenciarBuzzer(NivelAlerta nivel) {
  const unsigned long agora = millis();
  if (nivel == NORMAL) {
    noTone(BUZZER_PIN);
    digitalWrite(BUZZER_PIN, LOW);
    buzzerLigado = false;
    return;
  }

  int intervalo = 1200;
  int frequencia = 880;
  if (nivel == MODERADO) intervalo = 400;
  if (nivel == MODERADO) frequencia = 1200;
  if (nivel == CRITICO) {
    intervalo = 100;
    frequencia = 2000;
  }
  if (agora - ultimoBipe >= static_cast<unsigned long>(intervalo)) {
    ultimoBipe = agora;
    buzzerLigado = !buzzerLigado;
    if (buzzerLigado) {
      tone(BUZZER_PIN, frequencia);
    } else {
      noTone(BUZZER_PIN);
    }
  }
}

NivelAlerta nivelPorCodigo(int codigo) {
  if (codigo == 1) return LEVE;
  if (codigo == 2) return CRITICO;
  return NORMAL;
}

void handleBuzzer() {
  if (!webServer.hasArg("padrao")) {
    webServer.send(400, "text/plain; charset=utf-8", "Parametro padrao obrigatorio");
    return;
  }

  const int codigo = webServer.arg("padrao").toInt();
  if (codigo < 0 || codigo > 2) {
    webServer.send(400, "text/plain; charset=utf-8", "Padrao deve ser 0, 1 ou 2");
    return;
  }

  estadoAtual = nivelPorCodigo(codigo);
  controleRemoto = true;
  ultimoBipe = millis();
  desenharTela(estadoAtual);
  gerenciarBuzzer(estadoAtual);
  webServer.send(204, "text/plain", "");
}

void handlePaginaInicial() {
  const char* pagina =
      "<!doctype html><html lang='pt-BR'><head>"
      "<meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
      "<title>HELIOS - Teste</title>"
      "<style>body{font-family:Arial;background:#111;color:#fff;text-align:center;margin:20px}"
      "img{max-width:100%;height:auto;border:2px solid #0f0}</style>"
      "</head><body><h1>HELIOS</h1><p>ESP32-CAM online</p>"
      "<p><img id='camera' alt='Camera'></p>"
      "<script>document.getElementById('camera').src='http://'+location.hostname+':81/stream';</script>"
      "</body></html>";
  webServer.send(200, "text/html; charset=utf-8", pagina);
}

void handleCaptura() {
  if (!cameraAvailable) {
    webServer.send(503, "text/plain; charset=utf-8", "Camera indisponivel");
    return;
  }

  camera_fb_t* frame = esp_camera_fb_get();
  if (frame == nullptr) {
    webServer.send(500, "text/plain; charset=utf-8", "Falha ao capturar imagem");
    return;
  }

  WiFiClient cliente = webServer.client();
  cliente.print("HTTP/1.1 200 OK\r\n");
  cliente.print("Content-Type: image/jpeg\r\n");
  cliente.print("Content-Length: ");
  cliente.print(frame->len);
  cliente.print("\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n");
  cliente.write(frame->buf, frame->len);
  esp_camera_fb_return(frame);
}

void iniciarServidorWeb() {
  webServer.on("/", HTTP_GET, handlePaginaInicial);
  webServer.on("/capture", HTTP_GET, handleCaptura);
  webServer.on("/buzzer", HTTP_POST, handleBuzzer);
  webServer.onNotFound([]() {
    webServer.send(404, "text/plain; charset=utf-8", "Pagina nao encontrada");
  });
  webServer.begin();
  streamServer.begin();
  Serial.println("Servidor web iniciado.");
  Serial.println("Stream: http://IP_DA_ESP32:81/stream");
}

void aceitarClienteStream() {
  if (streamClient && streamClient.connected()) return;
  if (streamClient) streamClient.stop();
  streamClient = streamServer.available();
  if (!streamClient) return;

  streamClient.setNoDelay(true);
  streamClient.print(
      "HTTP/1.1 200 OK\r\n"
      "Content-Type: multipart/x-mixed-replace; boundary=frame\r\n"
      "Cache-Control: no-cache\r\n"
      "Connection: close\r\n\r\n");
}

void enviarFrameStream() {
  if (!streamClient || !streamClient.connected() || !cameraAvailable) return;

  camera_fb_t* frame = esp_camera_fb_get();
  if (frame == nullptr) return;

  const size_t frameLength = frame->len;
  streamClient.printf(
      "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n",
      static_cast<unsigned>(frameLength));
  const size_t enviado = streamClient.write(frame->buf, frameLength);
  streamClient.print("\r\n");
  esp_camera_fb_return(frame);

  if (enviado != frameLength) {
    streamClient.stop();
  }
}

void conectarWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  const unsigned long inicio = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - inicio < 20000UL) {
    delay(500);
    Serial.print(".");
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWi-Fi conectado!");
    Serial.print("IP: ");
    Serial.println(WiFi.localIP());
    iniciarServidorWeb();
  } else {
    Serial.println("\nFalha ao conectar ao Wi-Fi.");
    WiFi.disconnect();
    mostrarMensagem("Falha no Wi-Fi", "Continuando teste");
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  Wire.begin(I2C_SDA, I2C_SCL);

  displayAvailable = display.begin(SSD1306_SWITCHCAPVCC, 0x3C);
  if (!displayAvailable) {
    Serial.println("Erro ao inicializar display OLED!");
  } else {
    mostrarMensagem("Iniciando HELIOS...");
  }

  conectarWiFi();
  cameraAvailable = inicializarCamera();
  if (!cameraAvailable) {
    mostrarMensagem("Falha na camera", "Verifique o modulo");
  } else {
    Serial.println("Camera inicializada.");
    desenharTela(NORMAL);
  }
}

void loop() {
  webServer.handleClient();
  aceitarClienteStream();
  enviarFrameStream();
  const unsigned long tempo = millis();
  if (!controleRemoto && tempo - ultimoAlerta > 6000UL) {
    ultimoAlerta = tempo;
    if (estadoAtual == NORMAL) estadoAtual = LEVE;
    else if (estadoAtual == LEVE) estadoAtual = MODERADO;
    else if (estadoAtual == MODERADO) estadoAtual = CRITICO;
    else estadoAtual = NORMAL;
    desenharTela(estadoAtual);
  }
  gerenciarBuzzer(estadoAtual);
}
