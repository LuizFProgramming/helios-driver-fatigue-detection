#pragma once

#define HELIOS_WIFI_SSID "HELIOS-CAM"
#define HELIOS_WIFI_PASSWORD ""
#define HELIOS_WIFI_CHANNEL 6
#define HELIOS_WIFI_MAX_CLIENTS 1

#define HELIOS_IP 192, 168, 4, 1
#define HELIOS_GATEWAY 192, 168, 4, 1
#define HELIOS_NETMASK 255, 255, 255, 0

// GPIO livre para buzzer externo; GPIO 4 fica reservado para o flash.
#define HELIOS_BUZZER_GPIO 13
#define HELIOS_BUZZER_CHANNEL 2
#define HELIOS_BUZZER_RESOLUTION 8
#define HELIOS_BUZZER_FREQUENCY_HZ 2000

#define HELIOS_STREAM_PORT 81
#define HELIOS_CONTROL_PORT 80
// VGA melhora bastante a visualização sem o custo de memória de UXGA.
#define HELIOS_FRAME_SIZE FRAMESIZE_VGA
// Valores menores preservam mais detalhes; 10 é um bom equilíbrio para o stream.
#define HELIOS_JPEG_QUALITY 10
