# ADR-008: escada do olho fechado, olhos independentes e volta do motorista

Data: 15/10/2026. Situação: aceita pela equipe em 15/10/2026, depois do primeiro teste no celular.

## Contexto

No primeiro teste da versão 0.2.0, fechar os olhos por pouco tempo já disparava o alarme forte, e o alarme continuava alguns segundos depois de abrir os olhos. As causas estavam no código:

- o CRÍTICO começava com 1 s de olho fechado;
- o PERCLOS de 5 s acima de 40% também levava ao CRÍTICO e o segurava enquanto a janela não limpava;
- os dois olhos entravam como uma média só.

## Decisão

1. **Escada do olho fechado.** Até 1,5 s não há alarme. De 1,5 s a 3 s vem o aviso ATENÇÃO, baixo. A partir de 3 s, o CRÍTICO. Aos 6 s de olho fechado, o alarme chega ao máximo. As faixas seguem a Euro NCAP, citada por Optalert: fechamento de 1 a 2 s é microssono, de 3 s ou mais é sono e de 6 s ou mais é motorista que não responde.
2. **Fim do PERCLOS de 5 s como gatilho de CRÍTICO.** Ele continua calculado e exportado.
3. **Olhos independentes.** Cada olho tem EAR, piscada e limiar calibrado próprios. Os olhos só contam como fechados quando os dois estão fechados. Com a cabeça de lado (um olho com menos de 60% da largura do outro), vale o mais largo.
4. **Volta do motorista.** Ao sair de um alerta urgente toca um som de retorno. Depois de um CRÍTICO, o app fica 2 min em FADIGA, com lembrete suave. Dois microssonos em 5 min também levam à FADIGA.

## Consequências

- Os números citados no texto do TCC mudam:
  - CRÍTICO por olho fechado: de 1 s para 3 s;
  - novo aviso aos 1,5 s;
  - o PERCLOS de 5 s deixa de ser gatilho.
- O valor de 60% para o olho único e os 2 min de pós-sono são propostas da equipe e ainda precisam de validação com vídeos rotulados.

## Fonte

OPTALERT. Euro NCAP's Fatigue and Unresponsive Driver Evaluation. Disponível em: https://www.optalert.com/?p=1757. Acesso em: 15 out. 2026. Confirmar os valores no protocolo original da Euro NCAP (Safe Driving, Driver Engagement) antes de citar no TCC.
