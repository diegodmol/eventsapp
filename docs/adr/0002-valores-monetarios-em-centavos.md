# ADR 0002 — Representação monetária em centavos (Long)

## Status
Aceito

## Contexto
Valores monetários (preço do evento, total do pedido, valor da transação)
precisam ser somados, multiplicados e comparados sem erro de arredondamento,
e persistidos de forma estável no Room (SQLite).

## Decisão
Todos os valores monetários no domínio e na persistência são representados
como `Long` em centavos (ex.: R$ 50,00 = `5000L`), nunca como `Double`/`Float`.
A conversão para `BigDecimal`/exibição formatada ("R$ 50,00") acontece apenas
na borda de apresentação (`Event.priceAsDecimal`, formatação nas telas
Compose).

## Consequências
- Elimina uma classe inteira de bugs de arredondamento de ponto flutuante em
  cálculos de total (`unitPriceCents * quantity`).
- Exige disciplina para nunca introduzir `Double` monetário em novas
  funcionalidades; recomenda-se um lint/code review manual neste ponto.
- Simplifica a serialização em Room/JSON, já que `Long` não sofre problemas
  de precisão como `Double`.
