# ADR 0003 — QR Code do ingresso sem assinatura criptográfica (MVP)

## Status
Aceito, com limitação conhecida documentada

## Contexto
O enunciado trata QR Code como opcional, mas exige que, se implementado, "o
ingresso deve estar vinculado à compra concluída". Uma solução completa de
produção validaria o QR Code offline (sem round-trip a um backend) por meio
de assinatura criptográfica (ex.: HMAC ou par de chaves assimétricas), para
impedir falsificação de ingressos.

## Decisão
Para o escopo deste desafio — sem backend obrigatório — o payload do QR é um
texto estruturado e determinístico:

```
EVT|order=<orderId>|ticket=<ticketId>|seq=<n>/<total>|tx=<transactionId>
```

Ele é gerado exclusivamente por `GenerateTicketsUseCase`, cuja pré-condição é
`TicketOrder.status == OrderStatus.APPROVED` — portanto, por construção, é
impossível gerar um QR para um pedido que não teve pagamento aprovado pela
Cielo. A emissão é idempotente (`EnsureTicketsIssuedUseCase` verifica se já
existem tickets antes de gerar novamente).

## Consequências
- **Limitação conhecida**: qualquer pessoa com acesso ao texto do payload
  pode reproduzir um QR Code visualmente idêntico (não há assinatura), pois
  não há um verificador externo (backend/leitor de portaria) neste escopo.
- Em uma evolução com backend, a recomendação é: (1) assinar o payload com
  uma chave privada do servidor (ex.: JWT compacto ou HMAC-SHA256), (2)
  incluir a assinatura no próprio QR, e (3) validar a assinatura + consultar
  o status "não utilizado" do ingresso no momento da portaria — sem depender
  de o dispositivo do usuário estar online.
- Essa limitação foi um trade-off consciente para manter o escopo dentro do
  que o enunciado pede (backend não avaliado) sem comprometer a
  rastreabilidade local do vínculo QR ↔ compra aprovada.
