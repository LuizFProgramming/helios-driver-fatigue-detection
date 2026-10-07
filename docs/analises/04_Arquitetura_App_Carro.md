# 04. Arquitetura do app: celular ou central multimídia + câmera no carro

## 1. O que precisa ser decidido

São três perguntas independentes:

1. **Por onde o vídeo chega ao app?** Wi-Fi ou Bluetooth.
2. **Onde a IA roda?** Na placa da câmera ou no celular/central.
3. **Em que tela o app roda?** Celular, Android Auto/CarPlay, Android Automotive ou central multimídia Android "genérica".

## 2. Wi-Fi ou Bluetooth

| Critério | Wi-Fi (MJPEG por HTTP) | Bluetooth (BLE) |
|----------|-------------------------|-----------------|
| Taxa de quadros na ESP32-CAM | QVGA 320×240: ~20 a 25 fps. VGA 640×480: ~10 a 15 fps (valores típicos de tutoriais; medir no seu hardware) | Na prática poucas dezenas de KB/s. Um JPEG QVGA tem ~8 a 15 KB, então o vídeo ficaria em poucos fps e instável |
| Serve para detectar piscada (dura ~100 a 400 ms)? | Sim, em QVGA | Não |
| Complexidade | Baixa: a ESP32-CAM já tem exemplo de servidor `/stream` | Alta: fragmentar JPEG em pacotes BLE |
| Uso recomendado | **Vídeo** | Configuração inicial (passar senha do Wi-Fi), comandos curtos, acionar buzzer |

**Conclusão: vídeo por Wi-Fi.** Bluetooth fica, no máximo, como canal auxiliar.

### Modos de Wi-Fi

| Modo | Como funciona | Prós | Contras |
|------|---------------|------|---------|
| ESP32 como ponto de acesso (SoftAP) | A câmera cria a rede "HELIOS-CAM" e o celular conecta nela | Não depende de nada; funciona em qualquer carro | O celular perde a internet enquanto está nessa rede (o app precisa "prender" o tráfego nessa rede no Android) |
| ESP32 como cliente (STA) do hotspot do celular ou da central | O celular/central liga o roteador e a câmera entra | O celular continua com 4G/5G | Precisa descobrir o IP da câmera (mDNS `helios.local` ou varredura); hotspot precisa estar ligado |

Sugestão: **STA no hotspot como padrão e SoftAP como plano B**, configurável pelo app.

## 3. Onde a IA roda

| Opção | Viável? | Motivo |
|-------|---------|--------|
| Na ESP32-CAM | Não para o HELIOS | 240 MHz, 520 KB de RAM + 4 MB PSRAM. Não roda o Face Landmarker de 478 pontos. A ESP32-S3 com ESP-DL faz detecção de rosto, mas não a malha facial com qualidade para EAR |
| **No celular/central** | **Sim** | O MediaPipe Face Landmarker tem versão oficial para Android, iOS e Web. É o mesmo modelo do protótipo Python |
| Num servidor (o backend FastAPI atual) | Não para tempo real | Exige internet no carro, aumenta a latência e envia o rosto do motorista para fora do veículo (problema de LGPD) |

**A câmera vira só sensor.** A IA roda no aparelho que mostra o alerta. Isso também é bom argumento de privacidade no TCC: nenhuma imagem sai do carro, só eventos (ex.: "sonolência às 14h32, durou 3 s").

## 4. Em que tela o app roda

| Plataforma | Dá para usar no HELIOS? | Por quê |
|------------|-------------------------|---------|
| Celular Android (suporte no painel) | **Sim** | Instala o APK, mantém tela ligada, alerta sonoro e vibração |
| Celular iOS | Possível depois | MediaPipe tem iOS, mas exige Mac para compilar |
| Android Auto / Apple CarPlay | Não | O app só pode mostrar telas em modelos prontos (mídia, navegação, mensagens). Não dá para desenhar vídeo nem HUD próprio |
| Android Automotive OS (Google embarcado de fábrica) | Não, durante a direção | Apps de terceiros nessa categoria só funcionam com o carro parado, e a documentação diz que os carros geralmente não dão acesso às câmeras |
| **Central multimídia Android aftermarket** (as "centrais Android" de 7 a 10 polegadas) | **Sim** | Roda Android comum e aceita APK. É o mesmo app do celular. Ponto de atenção: processadores fracos; é preciso medir o fps real do MediaPipe nelas |

Ou seja, o mesmo APK atende "celular" e "tablet do carro", desde que o tablet seja uma central Android comum.

## 5. Tecnologia do app

| Opção | Desempenho | Encaixe nas metas de qualidade | Observação |
|-------|-----------|--------------------------------|-----------|
| **A. TypeScript + Capacitor + `@mediapipe/tasks-vision` (Web/JS)** | Bom em celular; incerto em central fraca | Excelente: as metas (`any`/`unknown`, Stryker, complexidade cognitiva) são do ecossistema TS | Usa exatamente o link do Face Landmarker Web/JS. A ESP32 precisa responder com cabeçalho CORS para o frame poder ser lido no canvas |
| B. Kotlin nativo + MediaPipe Tasks Android | O melhor (GPU delegate) | Bom: detekt (complexidade), Kover (cobertura), Pitest (mutação). `any`/`unknown` não se aplicam diretamente | Mais código para escrever; nada em comum com o TS |
| C. React Native | Bom | Bom | As bibliotecas de câmera do RN leem a câmera do aparelho, não um stream MJPEG. Complica o caso ESP32 |
| D. Flutter | Bom | Ferramentas de mutação fracas | Não há plugin oficial do MediaPipe |

### Recomendação

**Opção A com o domínio isolado**, que é o mesmo desenho hexagonal do protótipo Python:

```
app-helios/
  src/domain/        ← TS puro: EAR, MAR, PERCLOS, máquina de estados, classificador
                        (100% testável, 0 mutantes, sem DOM, sem MediaPipe)
  src/ports/         ← FonteDeFrames, DetectorFacial, SaidaDeAlerta, RepositorioEventos
  src/adapters/
     esp32-mjpeg/    ← lê o stream http://<ip>/stream
     camera-local/   ← getUserMedia (câmera do próprio celular, plano B sem ESP32)
     mediapipe-web/  ← FaceLandmarker em modo VIDEO
     alerta/         ← som, vibração, tela vermelha, buzzer na ESP32
  android/           ← gerado pelo Capacitor
```

Se a central do carro não aguentar o MediaPipe Web, troca-se **só** o adaptador `mediapipe-web` por um plugin Kotlin nativo. Domínio e testes ficam iguais. Isso é exatamente o argumento da arquitetura hexagonal que o README já defende, e rende uma boa seção no TCC.

## 6. Arquitetura proposta

```
┌──────────── CARRO ─────────────────────────────────────────────┐
│                                                                │
│  ESP32-CAM (firmware)              Celular ou central Android  │
│  ┌─────────────────┐   Wi-Fi       ┌─────────────────────────┐ │
│  │ OV2640 QVGA     │──MJPEG────────▶│ Adaptador ESP32-MJPEG   │ │
│  │ GET /stream     │  20+ fps      │          │              │ │
│  │ GET /status     │               │ MediaPipe Face Landmarker│ │
│  │ POST /buzzer    │◀── alerta ────│          │ 478 pts + blend│ │
│  │ (LED IR p/ noite)│               │ DOMÍNIO HELIOS (TS)     │ │
│  └─────────────────┘               │  EAR·MAR·PERCLOS·cabeça │ │
│         ▲ BLE opcional:            │  máquina de estados / ML│ │
│         └ senha Wi-Fi, comandos    │          │              │ │
│                                    │ Alerta: som, vibração,  │ │
│                                    │ tela vermelha           │ │
│                                    │ Histórico local (eventos)│ │
│                                    └──────────┬──────────────┘ │
└───────────────────────────────────────────────┼────────────────┘
                                                │ opcional, só eventos, sem imagem
                                                ▼
                                   Backend (FastAPI atual ou Spring Boot do SafeDrive)
                                   relatórios por viagem
```

## 7. Pontos práticos para não esquecer

- **Noite**: a OV2640 comum tem filtro IR. Para dirigir à noite precisa de módulo sem filtro (NoIR) + LED infravermelho 850 nm. Sem isso o sistema só funciona de dia, e isso deve aparecer como limitação no TCC.
- **Posição da câmera**: o rosto precisa ficar de frente, sem o volante cobrindo. Testar no carro real cedo.
- **Tela sempre ligada** e app em primeiro plano. No Android, apps em segundo plano não podem usar câmera/processamento contínuo sem serviço em primeiro plano.
- **Alerta com o motorista de olhos fechados**: o alerta principal tem de ser **sonoro**. Tela vermelha sozinha não acorda ninguém.
- **Distração ao volante**: a interface durante a direção deve ser mínima (estado + alerta). Gráficos e histórico só com o carro parado.
- **Óculos escuros** quebram o EAR e o ECF. Registrar como limitação.
