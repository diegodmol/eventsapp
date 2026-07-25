package com.eventtickets.domain.repository

import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.PaymentResult
import com.eventtickets.domain.model.TicketOrder
import kotlinx.coroutines.flow.Flow

interface OrderRepository {

    /** Cria (ou recupera, se já existir um pedido idêntico pendente) um pedido local. */
    suspend fun createOrder(
        eventId: String,
        quantity: Int
    ): AppResult<TicketOrder>

    fun observeOrder(orderId: String): Flow<TicketOrder?>

    suspend fun getOrder(orderId: String): AppResult<TicketOrder>

    /**
     * Persiste o resultado do pagamento e atualiza o status do pedido de forma
     * atômica. Deve ser chamado exatamente uma vez por resposta real da Cielo.
     */
    suspend fun recordPaymentResult(
        orderId: String,
        status: OrderStatus,
        paymentResult: PaymentResult?
    ): AppResult<TicketOrder>

    /**
     * Verifica se já existe um pagamento em andamento ou concluído para este
     * pedido — usado para bloquear reenvio de cobrança (ver
     * StartPaymentUseCase e docs/adr/0001).
     */
    suspend fun hasActiveOrCompletedPayment(orderId: String): Boolean
}
