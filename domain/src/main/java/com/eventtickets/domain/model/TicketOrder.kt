package com.eventtickets.domain.model

import java.time.Instant

/**
 * Representa um pedido de ingressos, do carrinho até o resultado final do pagamento.
 *
 * O [idempotencyKey] é gerado uma única vez quando o pedido é criado (ver
 * CreateOrderUseCase) e reutilizado em qualquer retentativa de pagamento para o
 * MESMO pedido, garantindo que reenvios de ação (ex.: usuário clicando duas vezes,
 * ou reconexão após timeout) não gerem cobrança duplicada na Cielo.
 * Ver docs/adr/0001-idempotencia-pagamento.md.
 */
data class TicketOrder(
    val orderId: String,
    val idempotencyKey: String,
    val eventId: String,
    val eventTitle: String,
    val quantity: Int,
    val unitPriceCents: Long,
    val totalPriceCents: Long,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val payment: PaymentResult? = null
)

enum class OrderStatus {
    CREATED,        // Pedido criado, aguardando início do pagamento
    PROCESSING,     // Pagamento em andamento na Cielo
    APPROVED,       // Pagamento aprovado
    DENIED,         // Pagamento negado
    CANCELED,       // Pagamento cancelado pelo usuário ou pelo terminal
    ERROR           // Erro de integração/comunicação (não é resultado financeiro definitivo)
}

/**
 * Resultado retornado pela integração com a Cielo Smart para uma transação.
 */
data class PaymentResult(
    val transactionId: String?,
    val nsu: String?,
    val authorizationCode: String?,
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod?,
    val brand: String?,
    val amountCents: Long,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val respondedAt: Instant
)

enum class PaymentStatus {
    APPROVED,
    DENIED,
    CANCELED,
    ERROR,
    TIMEOUT
}

enum class PaymentMethod {
    CREDIT,
    DEBIT,
    PIX,
    VOUCHER,
    UNKNOWN
}
