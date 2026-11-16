# Retreino para a câmera real

Treino: 3332 olhos da CEW e 24000 da MRL; 25 épocas; melhor AUC na validação (limpa e aumentada): 0.992. Modelo: olho_cnn_real.tflite, 310192 bytes.

| modelo | condicao | f1 | auc | falso_fechado |
|---|---|---|---|---|
| atual (só CEW) | CEW limpo | 0.947 | 0.985 | 0.044 |
| atual (só CEW) | CEW com os aumentos | 0.773 | 0.843 | 0.495 |
| atual (só CEW) | CEW só com óculos | 0.726 | 0.758 | 0.582 |
| atual (só CEW) | fotos reais do app (36 olhos abertos) | nan | nan | 0.917 |
| novo (aumentos + MRL) | CEW limpo | 0.950 | 0.991 | 0.008 |
| novo (aumentos + MRL) | CEW com os aumentos | 0.930 | 0.986 | 0.036 |
| novo (aumentos + MRL) | CEW só com óculos | 0.918 | 0.983 | 0.033 |
| novo (aumentos + MRL) | fotos reais do app (36 olhos abertos) | nan | nan | 0.250 |
