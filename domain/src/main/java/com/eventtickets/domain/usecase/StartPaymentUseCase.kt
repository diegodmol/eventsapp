package com.eventtickets.domain.usecase

import com.eventtickets.core.common.AppError
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.PaymentResult
import com.eventtickets.domain.model.PaymentStatus
import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.repository.OrderRepository
import com.eventtickets.domain.repository.PaymentGateway
import javax.inject.Inject

/**
 * Fluxo 3: Iniciar e concluir o pagamento via integração com a Cielo.
 * Fluxo 4: Registrar o resultado da compra (aprovada, negada ou cancelada).
 *
 * ## Proteção contra duplicidade de cobrança (requisito não-funcional)
 * Antes de chamar o [PaymentGateway], este use case:
 *  1. Busca o pedido e confirma que ele existe e está em um estado que
 *     permite iniciar pagamento (CREATED ou ERROR — nunca APPROVED/DENIED/
 *     PROCESSING de novo).
 *  2. Consulta [OrderRepository.hasActiveOrCompletedPayment]; se já houver uma
 *     tentativa em andamento ou concluída para a MESMA idempotencyKey,
 *     retorna falha de duplicidade em vez de chamar a Cielo de novo.
 *  3. Marca o pedido como PROCESSING de forma atômica (repositório) ANTES de
 *     chamar a Cielo, então qualquer reentrada concorrente (ex.: duplo tap no
 *     botão "Pagar") encontra o pedido já em PROCESSING e é barrada no passo 2.
 *  4. Sempre grava o resultado (sucesso OU falha) via
 *     [OrderRepository.recordPaymentResult], garantindo que o histórico de
 *     compra nunca fique "no limbo".
 *
 * A idempotencyKey do pedido é repassada ao gateway; a implementação real do
 * gateway (Cielo Smart) deve, por sua vez, também evitar reenviar a mesma
 * transação ao terminal físico (ver docs/adr/0001).
 */
class StartPaymentUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val paymentGateway: PaymentGateway
) {
    suspend operator fun invoke(orderId: String): AppResult<TicketOrder> {
        val orderResult = orderRepository.getOrder(orderId)
        val order = when (orderResult) {
            is AppResult.Success -> orderResult.data
            is AppResult.Failure -> return orderResult
        }

        if (!canStartPayment(order.status)) {
            return AppResult.Failure(AppError.DuplicatePaymentAttempt(orderId))
        }

        if (orderRepository.hasActiveOrCompletedPayment(orderId)) {
            return AppResult.Failure(AppError.DuplicatePaymentAttempt(orderId))
        }

        // Marca como PROCESSING antes de chamar a Cielo (lock otimista).
        val lockResult = orderRepository.recordPaymentResult(
            orderId = orderId,
            status = OrderStatus.PROCESSING,
            paymentResult = null
        )
        if (lockResult is AppResult.Failure) return lockResult

        val paymentResult = paymentGateway.startPayment(
            orderId = order.orderId,
            idempotencyKey = order.idempotencyKey,
            amountCents = order.totalPriceCents
        )

        return when (paymentResult) {
            is AppResult.Success -> {
                val payment = paymentResult.data
                orderRepository.recordPaymentResult(
                    orderId = orderId,
                    status = payment.status.toOrderStatus(),
                    paymentResult = payment
                )
            }
            is AppResult.Failure -> {
                // Erro de integração: registra como ERROR (não é resultado
                // financeiro definitivo) para permitir nova tentativa
                // controlada pelo usuário, sem perder o rastro do ocorrido.
                orderRepository.recordPaymentResult(
                    orderId = orderId,
                    status = OrderStatus.ERROR,
                    paymentResult = errorAsPaymentResult(order.totalPriceCents, paymentResult.error)
                )
                paymentResult
            }
        }
    }

    private fun canStartPayment(status: OrderStatus): Boolean =
        status == OrderStatus.CREATED || status == OrderStatus.ERROR

    private fun PaymentStatus.toOrderStatus(): OrderStatus = when (this) {
        PaymentStatus.APPROVED -> OrderStatus.APPROVED
        PaymentStatus.DENIED -> OrderStatus.DENIED
        PaymentStatus.CANCELED -> OrderStatus.CANCELED
        PaymentStatus.ERROR, PaymentStatus.TIMEOUT -> OrderStatus.ERROR
    }

    private fun errorAsPaymentResult(amountCents: Long, error: AppError): PaymentResult =
        PaymentResult(
            transactionId = null,
            nsu = null,
            authorizationCode = null,
            status = PaymentStatus.ERROR,
            paymentMethod = null,
            brand = null,
            amountCents = amountCents,
            errorCode = error::class.simpleName,
            errorMessage = error.message,
            respondedAt = java.time.Instant.now()
        )
}
