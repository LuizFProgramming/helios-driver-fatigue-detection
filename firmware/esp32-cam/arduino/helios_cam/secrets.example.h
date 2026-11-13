#pragma once

// Copie este arquivo para secrets.h, na mesma pasta (esse nome não vai para o git), e preencha.
// Com HELIOS_WIFI_MODO_ESTACAO definido, a ESP32 entra na rede abaixo em vez de criar a HELIOS-CAM.
// Use só em bancada. No carro e com o APK, apague o secrets.h ou comente a linha abaixo.

#define HELIOS_WIFI_MODO_ESTACAO
#define HELIOS_ESTACAO_SSID "nome-da-rede"
#define HELIOS_ESTACAO_SENHA "senha-da-rede"
