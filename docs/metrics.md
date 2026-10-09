# Métricas

## Indicadores de fadiga

Valores em uso no protótipo Python (`prototype/prototipo_desktop_python/config.py`), em 08/10/2026. Mudar um deles muda o comportamento do sistema e os números citados no texto do TCC: avisar a equipe antes.

| Indicador | O que mede | Referência | Limiar adotado |
| --- | --- | --- | --- |
| EAR | Abertura palpebral | Soukupová e Čech (2016) | Olho fechado com EAR < 0,19 (`EAR_LIMIAR_OLHO_FECHADO`) |
| ECF | Fechamento ocular medido pelo modelo do MediaPipe (blend shapes) | MediaPipe Face Landmarker | Olho fechado com ECF > 0,40; vale EAR **ou** ECF |
| PERCLOS | % de tempo com olhos fechados | Dinges e Grace (1998) | Risco elevado acima de 40% numa janela de 150 frames (≈ 5 s a 30 fps) |
| MAR | Abertura da boca (bocejo) | Abtahi et al. (2012) | Bocejo com MAR > 0,50 por 15 frames (≈ 0,5 s) |
| Microssono | Fechamento ocular prolongado | Wierwille (1994) | 30 frames seguidos de olho fechado (≈ 1 s) |
| Rosto fora da câmera | Distração (categoria DESATENTO) | Proposta da equipe (06/10/2026) | 2 s sem rosto; **ainda não validado** |
| Direção do rosto | Giro e inclinação da cabeça pela matriz de pose | MediaPipe | Fora da "frente" acima de 20° (`GRAUS_PARA_SAIR_DA_FRENTE`); sinais ainda não conferidos com rosto real |
| Inclinação via sensor inercial (IMU) | Cabeceio | — | Só no app Android (acelerômetro do aparelho); no protótipo Python não há |

> O limiar de EAR 0,19 e os números de acurácia que aparecem como comentário em `config.py` vêm do artigo de referência (Medina, 2025), **não** de medições do HELIOS. As contagens em frames pressupõem 30 fps (problema P2 em `docs/analises/03_Problemas_Encontrados.md`).

## Categorias de alerta e resposta

| Categoria | Regra | Alerta sonoro |
| --- | --- | --- |
| DORMINDO | Olhos fechados sem parar (microssono) | 2000 Hz, ciclo de 0,4 s |
| SONOLENCIA | PERCLOS acima de 40% ou bocejo | 880 Hz, ciclo de 2,7 s |
| DESATENTO | Sem rosto por 2 s ou mais | 1200 Hz, ciclo de 1 s |
| ATENTO, PISCANDO, SEM_ROSTO | — | Silêncio |

## Métricas de validação do sistema

- Acurácia
- Precisão
- Sensibilidade (recall)
- Especificidade
- Taxa de falsos positivos
- Tempo de resposta (latência do alerta)

**Latência do alerta sonoro:** ainda não medida. O som começa no mesmo frame em que a categoria muda; o tempo total é o da regra (ex.: cerca de 1 s de olho fechado para DORMINDO) mais o atraso do `winsound`.

## Protocolo de teste

> 🚧 Descrever: cenários (iluminação normal/baixa), duração, número de participantes, forma de rotulação.

Roteiro mínimo já usado para conferir o alerta (08/10/2026), sem rotulação formal: olhos abertos (silêncio); olhos fechados por mais de 1 s (DORMINDO); abrir os olhos (som para em cerca de 1 s); sair da câmera por 2 s (DESATENTO); bocejo amplo (SONOLENCIA); tecla `m`.
