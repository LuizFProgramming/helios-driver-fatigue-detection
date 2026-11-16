#pragma once

// Único arquivo de configuração do firmware. O código em helios_cam.ino não precisa ser editado.

// ---------------------------------------------------------------------------
// Rede
// ---------------------------------------------------------------------------
// Padrão (uso no carro e com o APK): a ESP32 cria a própria rede HELIOS-CAM em 192.168.4.1.
// O APK está configurado para rede aberta (senha vazia).
#define HELIOS_WIFI_SSID "HELIOS-CAM"
#define HELIOS_WIFI_PASSWORD ""
#define HELIOS_WIFI_CHANNEL 6
#define HELIOS_WIFI_MAX_CLIENTS 1

#define HELIOS_IP 192, 168, 4, 1
#define HELIOS_GATEWAY 192, 168, 4, 1
#define HELIOS_NETMASK 255, 255, 255, 0

// Teste de bancada na rede de casa (modo estação). Para usar, copie secrets.example.h para
// secrets.h (mesma pasta) e preencha. O secrets.h está no .gitignore: senha de rede nunca vai para o git.
// Neste modo o IP vem do roteador (aparece no Serial Monitor e no OLED) e o APK precisa apontar para ele.
#if __has_include("secrets.h")
#include "secrets.h"
#endif

// ---------------------------------------------------------------------------
// Buzzer
// ---------------------------------------------------------------------------
// GPIO 13 não interfere no boot. Evite o GPIO 12: se o módulo o puxar para nível alto na partida,
// a ESP32 escolhe a tensão errada da flash e não inicia. O GPIO 4 é o flash da câmera.
#define HELIOS_BUZZER_GPIO 13
// 1 = buzzer ativo (tom fixo interno, basta ligar e desligar o pino).
// 0 = buzzer passivo (o firmware gera o tom por PWM, na frequência de cada padrão).
// Teste rápido: com o pino em nível alto fixo, o ativo apita contínuo e o passivo fica mudo.
#define HELIOS_BUZZER_ATIVO 1
// Alguns módulos ativos disparam em nível baixo. Se o seu apitar ao ligar e calar no alerta, troque para 0.
#define HELIOS_BUZZER_NIVEL_LIGADO HIGH
#define HELIOS_BUZZER_CHANNEL 2
#define HELIOS_BUZZER_RESOLUTION 8
#define HELIOS_BUZZER_DUTY 128

// Cada padrão recebido em POST /buzzer?padrao=N: tom (só no passivo) e ritmo (ms ligado / ms desligado).
// 1 = atenção, fadiga, desatento ou distraído; 2 = crítico. Mesmos códigos e tons do APK (protocolos/Alarme.kt):
// 880 Hz na fadiga e 2000 Hz com 300/100 ms no crítico. O buzzer ativo toca sempre o tom interno dele.
#define HELIOS_PADRAO1_FREQUENCIA_HZ 880
#define HELIOS_PADRAO1_LIGADO_MS 200
#define HELIOS_PADRAO1_DESLIGADO_MS 1000
#define HELIOS_PADRAO2_FREQUENCIA_HZ 2000
#define HELIOS_PADRAO2_LIGADO_MS 300
#define HELIOS_PADRAO2_DESLIGADO_MS 100

// ---------------------------------------------------------------------------
// Display OLED SSD1306 (opcional: se não for encontrado, o firmware segue sem ele)
// ---------------------------------------------------------------------------
#define HELIOS_I2C_SDA 15
#define HELIOS_I2C_SCL 14
#define HELIOS_OLED_ENDERECO 0x3C
#define HELIOS_OLED_LARGURA 128
#define HELIOS_OLED_ALTURA 64

// ---------------------------------------------------------------------------
// Câmera e servidores
// ---------------------------------------------------------------------------
#define HELIOS_STREAM_PORT 81
#define HELIOS_CONTROL_PORT 80
// VGA melhora a visualização sem o custo de memória de UXGA. Para mais fps, use FRAMESIZE_QVGA.
#define HELIOS_FRAME_SIZE FRAMESIZE_VGA
// Valores menores preservam mais detalhes; 10 é um bom equilíbrio para o stream.
#define HELIOS_JPEG_QUALITY 10

// ---------------------------------------------------------------------------
// Demonstração
// ---------------------------------------------------------------------------
// 1 = sem o app, o firmware percorre sozinho os estados (normal, atenção, crítico) a cada 6 s,
// com tela e buzzer. Serve para fotografar a case e testar a montagem. Deixe 0 para uso com o APK.
#define HELIOS_MODO_DEMO 0
#define HELIOS_DEMO_INTERVALO_MS 6000
