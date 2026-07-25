# ADR 0001 — Estratégia de idempotência para evitar duplicidade de cobrança

## Status
Aceito

## Contexto
O requisito não-funcional exige que "o fluxo de compra deve evitar duplicidade
de cobrança em reenvio de ação". Reenvio de ação pode acontecer por:
1. Duplo toque do usuário no botão "Pagar" (dedo lento a soltar, tela travada).
2. Reentrada de tela após rotação/mudança de configuração enquanto a
   corrotina de pagamento ainda está em andamento.
3. Timeout de UI seguido de nova tentativa manual, enquanto a transação
   original ainda pode estar em processamento no terminal Cielo Smart.
4. Processo do app morto pelo sistema e recriado, com o usuário tentando pagar
   de novo sem saber que já havia uma tentativa anterior.

## Decisão
Adotamos idempotência em **três camadas independentes**, cada uma suficiente
por si só, para tolerar falhas parciais nas demais:

1. **Chave de idempotência estável por pedido** (`TicketOrder.idempotencyKey`):
   gerada uma única vez em `OrderRepositoryImpl.createOrder`, nunca
   regenerada. Toda tentativa de pagamento para o mesmo pedido reutiliza essa
   chave, e ela é repassada ao gateway de pagamento como referência externa
   (para que a própria Cielo possa deduplicar do seu lado, se suportado).

2. **Lock otimista no banco local** (`StartPaymentUseCase` + `OrderDao`):
   antes de chamar o gateway, o pedido é marcado como `PROCESSING`. Qualquer
   nova chamada ao use case, enquanto o status já é `PROCESSING`,
   `APPROVED`, `DENIED` ou `CANCELED`, é barrada com
   `AppError.DuplicatePaymentAttempt` — sem sequer tocar no gateway.
   A consulta `OrderDao.countActiveOrCompleted` usa a `idempotencyKey`
   (não o `orderId`) para também cobrir cenários futuros de múltiplos
   registros de pedido apontando para a mesma transação.

3. **Proteção na camada de apresentação** (`PaymentViewModel.pay()`):
   um flag local `isProcessingPayment` barra chamadas repetidas antes mesmo
   de a corrotina de negócio ser disparada — cobre o caso mais comum na
   prática (multi-tap) sem latência de I/O.

## Consequências
- Uma tentativa de pagamento que falhe por erro de integração (não
  financeiro) deixa o pedido em `ERROR`, e uma nova tentativa É permitida
  (não é tratada como duplicidade), pois nenhuma cobrança foi de fato
  concluída.
- Um pedido `APPROVED`, `DENIED` ou `CANCELED` nunca mais aceita nova
  tentativa de pagamento pelo mesmo `orderId`; qualquer necessidade de
  comprar novamente exige um novo pedido (`CreateOrderUseCase`).
- Testável sem UI real: toda a lógica de bloqueio está no `domain`
  (`StartPaymentUseCaseTest`), validável com JUnit puro.
