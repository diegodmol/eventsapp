# Especificação Funcional — EventTickets

## Visão geral
App Android nativo (Kotlin) para descoberta de eventos e compra de ingressos,
com pagamento processado através do ecossistema Cielo Smart (terminais LIO).

## Fluxos obrigatórios e critérios de aceite

### Fluxo 1 — Visualizar eventos disponíveis
- **Dado** que existem eventos cadastrados (locais/simulados),
  **quando** o usuário abre o app,
  **então** ele vê uma lista com título, local, data/hora, preço e
  quantidade de ingressos disponíveis de cada evento.
- Eventos com `availableTickets == 0` são exibidos como "Esgotado" e não são
  clicáveis.
- Falha ao carregar exibe estado de erro com opção de "Tentar novamente".

### Fluxo 2 — Selecionar quantidade de ingressos
- **Dado** um evento selecionado,
  **quando** o usuário ajusta a quantidade (1 a `min(10, disponível)`),
  **então** o total é recalculado em tempo real.
- Confirmar cria um `TicketOrder` com status `CREATED` e navega para a tela
  de pagamento.
- Múltiplos toques rápidos em "Confirmar" não criam múltiplos pedidos.

### Fluxo 3 — Iniciar e concluir pagamento via Cielo
- **Dado** um pedido `CREATED`,
  **quando** o usuário toca em "Pagar",
  **então** o app dispara a integração com a Cielo Smart e exibe o pedido
  como `PROCESSING` até a resposta chegar.
- Reenviar a mesma ação de pagamento (duplo tap, retry após timeout) não gera
  uma segunda cobrança — ver ADR 0001.

### Fluxo 4 — Registrar resultado da compra
- O resultado retornado pela Cielo (aprovado, negado, cancelado, erro) é
  persistido de forma atômica junto ao pedido, nunca deixando o pedido em um
  estado ambíguo.
- Um resultado `DENIED`/`CANCELED` é um desfecho de negócio válido — não é
  tratado como falha de sistema, e a tela reflete isso claramente.
- Um erro de integração (SDK ausente, timeout, terminal indisponível) é
  distinto de um resultado financeiro definitivo e permite nova tentativa.

### Fluxo 5 — Exibir comprovante/resumo da compra
- Mostra: evento, quantidade, valor total, status, e dados da transação
  (ID, NSU, código de autorização, bandeira) quando disponíveis.
- Se o pedido foi `APPROVED`, os ingressos (com QR Code) são gerados e
  exibidos; a geração é idempotente (reabrir a tela não duplica ingressos).

## Requisitos não-funcionais e como foram atendidos

| Requisito | Onde/Como |
|---|---|
| Tratamento explícito de erros de integração/pagamento | `AppResult`/`AppError` (core-common); nenhuma função de borda lança exception não tratada |
| Evitar duplicidade de cobrança em reenvio de ação | ADR 0001 — 3 camadas de proteção (UI, use case, banco) |
| Código organizado e de fácil manutenção | Multi-módulo por camada/feature, Clean Architecture, injeção de dependência via Hilt |
| Testes automatizados para cenários críticos | JUnit + MockK + Turbine + Truth em domain/data/feature; cenários de duplicidade, validação de quantidade, vínculo QR↔compra cobertos explicitamente |
| Uso de IA como suporte, com "como" documentado | `docs/harness/README.md` |
| Backend não avaliado | `EventRemoteDataSource` simula a fonte remota localmente, documentado como ponto de troca futuro |

## Fora de escopo (explicitamente, por definição do enunciado)
- Backend de apoio (API própria, autenticação de usuário, catálogo remoto
  real).
- Emissão de nota fiscal/comprovante fiscal.
- Validação de ingresso na portaria (leitor de QR do lado do organizador).
