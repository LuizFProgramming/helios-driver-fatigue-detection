Escolhido: media (treino cew), 303 KB, F1 na validação 91,9%. Regra: maior F1 na validação com .tflite até 1024 KB; o teste só foi olhado depois da escolha.

Métricas por olho (cada recorte é uma amostra), corte 0,5. Teste e validação da CEW, divididos por pessoa.

| Arquitetura | Treino | Parâmetros | .tflite (KB) | F1 validação | Acurácia | Sensibilidade | Especificidade | F1 | AUC | ms/recorte (PC) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| pequena | cew | 23.809 | 33 | 76,4% | 82,3% | 65,6% | 98,4% | 78,4% | 0,963 | 0,42 |
| media | cew | 288.353 | 303 | 91,9% | 94,8% | 94,0% | 95,6% | 94,7% | 0,985 | 2,92 |
| mobilenet | cew | 411.489 | 611 | 65,7% | 50,3% | 100,0% | 2,5% | 66,4% | 0,887 | 0,56 |
| mobilenet_100 | cew | 2.259.265 | 2651 | 69,8% | 60,7% | 100,0% | 23,0% | 71,4% | 0,931 | 1,52 |
| pequena | cew+mrl | 23.809 | 33 | 71,2% | 79,2% | 63,1% | 94,8% | 74,9% | 0,922 | 0,41 |
| media | cew+mrl | 288.353 | 303 | 91,2% | 90,9% | 91,5% | 90,4% | 90,8% | 0,977 | 2,93 |
| mobilenet | cew+mrl | 411.489 | 611 | 65,2% | 50,1% | 100,0% | 2,2% | 66,3% | 0,523 | 0,56 |

Não treinada: mobilenet_100 (treino cew+mrl). O treino parou antes (falta de memória); não entra na tabela.
