"""
config.py — Configuracao central do sistema HELIOS

Todos os valores numericos e parametros do sistema ficam aqui.
Para ajustar o comportamento da deteccao, edite apenas este arquivo.

Fontes dos valores:
  [RAZR]      Seminario Medina (2025) — "Sistema Autonomo de Deteccion de
              Somnolencia Basado en Relacion de Aspecto Ocular (EAR)"
  [SOUKUPOVA] Soukupova & Cech (2016) — "Real-Time Eye Blink Detection
              using Facial Landmarks"
  [HELIOS]    HELIOS_Secao8_ConfigAmb.docx v3.0, UNICID 2026
"""

from pathlib import Path

# =============================================================================
# MODELO MEDIAPIPE
# =============================================================================

# Arquivo do modelo Face Landmarker (float16, 478 landmarks).
# Deve estar na mesma pasta que este arquivo.
# Download: https://storage.googleapis.com/mediapipe-models/
#           face_landmarker/face_landmarker/float16/1/face_landmarker.task
CAMINHO_MODELO_MEDIAPIPE = Path(__file__).parent / "face_landmarker.task"

# Numero maximo de rostos rastreados por frame.
# Para monitoramento de motorista individual, fixado em 1.
NUMERO_MAXIMO_DE_ROSTOS = 1

# Ritmo da analise, em frames por segundo. As contagens em frames deste arquivo
# (30 frames = 1 s, 150 frames = 5 s) pressupoem 30 fps, entao a analise e
# limitada a este valor para que "30 frames" continue valendo 1 segundo.
FPS_ALVO = 30

# Frames maiores que isto sao reduzidos ANTES do MediaPipe (so para a analise;
# a tela e a foto salva continuam na resolucao original). O modelo trabalha
# internamente em 256x256, entao reduzir nao perde precisao e ganha tempo.
LARGURA_MAXIMA_PARA_DETECCAO = 640

# Habilita calculo de blend shapes pelo modelo (ECF direto da rede neural).
# Custo computacional marginal — recomendado manter True.
HABILITAR_BLEND_SHAPES = True

# =============================================================================
# SUAVIZACAO TEMPORAL — EMA (Exponential Moving Average)
# =============================================================================
# Fator de suavizacao aplicado sobre EAR e MAR frame a frame.
# Elimina jitter de medicao sem introduzir atraso perceptivel.
#
#   0.10 — muito suave (reage devagar a mudancas reais)
#   0.25 — equilibrio recomendado
#   1.00 — sem suavizacao (comportamento original)
EMA_ALPHA = 0.25

# =============================================================================
# ECF — Eye Closure Factor (blend shape direto do modelo)
# =============================================================================
# eyeBlinkLeft / eyeBlinkRight retornados pelo MediaPipe:
#   0.0 = olho completamente aberto
#   1.0 = olho completamente fechado
#
# Usado como sinal secundario ao EAR. Se ECF > limiar, confirma fechamento
# mesmo que o calculo geometrico do EAR esteja impreciso (oculos, barba, etc.)
ECF_LIMIAR_OLHO_FECHADO = 0.40

# =============================================================================
# IRIS — landmarks 468-477 (MediaPipe 478 pontos)
# =============================================================================
# Os ultimos 10 landmarks do modelo sao os pontos da iris:
#   468-472: iris do olho direito da camera (esq. do motorista)
#   473-477: iris do olho esquerdo da camera (dir. do motorista)
# Indice 0 de cada grupo e o centro da iris; 1-4 sao pontos do perimetro.
INDICES_IRIS_OLHO_DIREITO_CAMERA  = [468, 469, 470, 471, 472]
INDICES_IRIS_OLHO_ESQUERDO_CAMERA = [473, 474, 475, 476, 477]

# =============================================================================
# INDICES DOS LANDMARKS OCULARES — Modelo MediaPipe 478 pontos
# =============================================================================
# Cada olho e representado por 6 landmarks periorbitais na seguinte ordem:
#   [p1, p2, p3, p4, p5, p6]
#   p1 — canto lateral  (externo)
#   p2 — palpebra superior interna
#   p3 — palpebra superior externa
#   p4 — canto medial   (interno)
#   p5 — palpebra inferior externa
#   p6 — palpebra inferior interna
#
# Essa ordem e exigida pela formula EAR de Soukupova & Cech (2016).
# Fonte: [RAZR] Secao III-B; [SOUKUPOVA] Fig. 1
#
# Nota: "direito" e "esquerdo" sao do ponto de vista da IMAGEM (camera),
# nao da perspectiva do motorista.
LANDMARKS_OLHO_DIREITO_NA_IMAGEM  = [33,  160, 158, 133, 153, 144]
LANDMARKS_OLHO_ESQUERDO_NA_IMAGEM = [362, 385, 387, 263, 373, 380]

# =============================================================================
# LIMIAR EAR — Eye Aspect Ratio
# =============================================================================
# Valor abaixo do qual o olho e considerado FECHADO.
#
# Valor 0.19 otimizado pelo Indice de Youden (J = 0.8631) sobre o
# Driver Inattention Detection Dataset, atingindo:
#   Acuracia:    95.47%
#   Recall:      90.00%
#   Especificidade: 96.31%
#   F1-Score:    84.11%
#   AUC-ROC:     93.15%
#
# Fonte: [RAZR] Secao IV-A-2
EAR_LIMIAR_OLHO_FECHADO = 0.19

# =============================================================================
# DETECCAO DE PISCADAS E SONOLENCIA — contagem de frames consecutivos
# =============================================================================

# Numero MINIMO de frames com EAR abaixo do limiar para contar como piscada.
# Valores abaixo disso sao tratados como ruido de medicao e ignorados.
# A 30fps: 2 frames ≈ 67ms — tempo minimo de uma piscada reflexiva humana.
FRAMES_MINIMOS_PARA_CONTAR_PISCADA = 2

# Numero de frames consecutivos com olho fechado que caracteriza SONOLENCIA.
# A 30fps: 30 frames = 1 segundo — limiar classico para micro-sono.
# Fonte: [SOUKUPOVA] Secao 3; [RAZR] Secao III-D (parametro t_u)
FRAMES_CONSECUTIVOS_PARA_ALERTA_SONOLENCIA = 30

# =============================================================================
# PERCLOS — Percentage of Eye Closure
# =============================================================================
# Metrica clinicamente estabelecida para avaliacao de fadiga ao volante.
# Mede o percentual do tempo com olhos fechados dentro de uma janela temporal.

# Tamanho da janela deslizante de analise (em frames).
# A 30fps: 150 frames = 5 segundos — janela recomendada pela literatura.
PERCLOS_TAMANHO_DA_JANELA_EM_FRAMES = 150

# Percentual de fechamento que indica RISCO ELEVADO de acidente.
# PERCLOS > 40% esta associado a degradacao cognitiva critica.
# Fonte: Dinges et al. (1998) — padrao na literatura de seguranca vial.
PERCLOS_LIMIAR_RISCO_ELEVADO = 0.40

# =============================================================================
# MAR — Mouth Aspect Ratio (deteccao de bocejo)
# =============================================================================
# Indices dos 6 landmarks do LABIO INTERNO (inner lip) para calculo do MAR.
# Mesma ordem do EAR: [canto_esq, sup_esq, sup_dir, canto_dir, inf_dir, inf_esq]
#
# Por que labio interno e nao externo?
# O conjunto externo original [61, 39, 0, 291, 321, 405] incluia o landmark 0,
# que se posiciona na regiao do filtro nasal — acima do labio real. Isso gera
# MAR elevado mesmo com a boca fechada, especialmente em rostos com barba.
# Os landmarks do labio interno medem diretamente a abertura oral e sao
# robustos a variacao de pelos faciais.
#
# Inner lip no MediaPipe 478 pontos:
#   78  = canto esquerdo (inner)      308 = canto direito (inner)
#   81  = labio sup. esquerdo          312 = labio inf. direito
#   13  = labio superior centro         14 = labio inferior centro
LANDMARKS_BOCA_PARA_MAR = [78, 81, 13, 308, 312, 14]

# Valor acima do qual a boca e considerada ABERTA em bocejo.
# Com inner lip: boca fechada MAR ~ 0.05-0.20 | bocejo amplo MAR ~ 0.50-1.20
# Limiar de 0.50 da boa separacao para rostos com ou sem barba.
MAR_LIMIAR_BOCEJO = 0.50

# Frames consecutivos com MAR acima do limiar para confirmar bocejo.
# A 30fps: 15 frames = 500ms — exige abertura sustentada, evita falsos positivos
# de fala ou respiracao pela boca.
FRAMES_MINIMOS_PARA_CONFIRMAR_BOCEJO = 15

# =============================================================================
# CATEGORIAS DE ALERTA — usadas pela API para separar as imagens
# =============================================================================
# DORMINDO   = SONOLENTO por olhos fechados sem parar (regra dos 30 frames acima)
# SONOLENCIA = SONOLENTO por PERCLOS, ou bocejo confirmado
# DESATENTO  = rosto fora da camera por este tempo ou mais (motorista olhando
#              para baixo, para o lado ou para o celular)
#
# Valor proposto pela equipe (06/10/2026), ainda nao validado com videos.
SEGUNDOS_SEM_ROSTO_PARA_DESATENTO = 2.0

# =============================================================================
# DIRECAO DO ROSTO — pose da cabeca (matriz de transformacao do MediaPipe)
# =============================================================================
# yaw   = giro para os lados (esquerda / direita)
# pitch = inclinacao para cima / baixo
# Acima deste angulo, em graus, o rosto deixa de ser "frente" naquele eixo.
# Ex.: yaw 30 e pitch 5 -> "direita"; yaw 30 e pitch -25 -> "baixo_direita".
GRAUS_PARA_SAIR_DA_FRENTE = 20.0

# Os sinais dos angulos dependem da camera (algumas espelham a imagem).
# Teste: vire o rosto para a SUA esquerda. Se a janela mostrar "direita",
# troque INVERTER_ESQUERDA_DIREITA para True. O mesmo vale para cima/baixo.
INVERTER_ESQUERDA_DIREITA = False
INVERTER_CIMA_BAIXO       = False

# Nome usado quando o MediaPipe nao encontra rosto (de costas, fora do
# quadro ou escuro demais: a camera nao consegue diferenciar esses casos).
DIRECAO_SEM_ROSTO = "sem_rosto"

# =============================================================================
# VISUALIZACAO — HUD (Heads-Up Display)
# =============================================================================

# Valor maximo do EAR para a escala da barra visual.
# Olho completamente aberto raramente supera 0.45.
EAR_VALOR_MAXIMO_NA_BARRA_VISUAL = 0.45

# Cores em formato BGR (Blue, Green, Red) — padrao OpenCV

# Estados do motorista
COR_ESTADO_ATENTO    = (0,   220,   0)   # verde
COR_ESTADO_PISCANDO  = (0,   165, 255)   # laranja
COR_ESTADO_SONOLENTO = (0,     0, 255)   # vermelho
COR_ESTADO_SEM_FACE  = (160, 160, 160)   # cinza

# Painel de metricas
COR_VALOR_EAR               = (255, 220,   0)   # amarelo
COR_VALOR_MAR               = (100, 255, 150)   # verde claro
COR_METRICAS_SECUNDARIAS    = (160, 160, 160)   # cinza claro

# Contornos dos olhos
COR_CONTORNO_OLHO_ABERTO    = (220, 220,   0)   # ciano
COR_CONTORNO_OLHO_FECHADO   = (0,     0, 255)   # vermelho

# Contorno da boca
COR_CONTORNO_BOCA_NORMAL    = (100, 255, 100)   # verde claro
COR_CONTORNO_BOCA_BOCEJO    = (0,   200, 255)   # amarelo

# Paineis e fundos
COR_FUNDO_PAINEL_METRICAS   = (15,   15,  15)   # quase preto
COR_FUNDO_BARRA_EAR         = (40,   40,  40)   # cinza escuro
COR_FUNDO_BANNER_ALERTA     = (0,     0, 160)   # azul escuro
COR_FUNDO_BANNER_BOCEJO     = (0,   120, 180)   # azul medio

# =============================================================================
# VISUALIZACAO — Janela de Mapeamento
# =============================================================================

# Cores para cada regiao facial no mapa de landmarks
COR_MAPA_TODOS_LANDMARKS    = (55,   55,  55)   # cinza escuro (fundo)
COR_MAPA_CONTORNO_ROSTO     = (180,  60, 220)   # violeta
COR_MAPA_SOBRANCELHAS       = (0,   210, 255)   # amarelo
COR_MAPA_OLHOS              = (200, 200,  60)   # azul claro
COR_MAPA_NARIZ              = (50,  140, 255)   # laranja
COR_MAPA_BOCA               = (60,  200,  80)   # verde
COR_MAPA_EAR_PONTOS         = (255, 230,   0)   # ciano brilhante
COR_MAPA_EAR_LINHA_HORIZ    = (255,  80,  80)   # azul (distancia horizontal)
COR_MAPA_EAR_LINHA_VERT     = (80,  255,  80)   # verde (distancias verticais)
COR_MAPA_MAR_PONTOS         = (0,   220, 255)   # amarelo brilhante
COR_MAPA_MAR_LINHA_HORIZ    = (255,  80,  80)   # azul
COR_MAPA_MAR_LINHA_VERT     = (80,  255,  80)   # verde
