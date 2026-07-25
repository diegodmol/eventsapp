package com.eventtickets.domain.usecase

import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.PaymentMethod
import com.eventtickets.domain.model.PaymentResult
import com.eventtickets.domain.model.PaymentStatus
import com.eventtickets.domain.model.TicketOrder
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Instant

class GenerateTicketsUseCaseTest {

    private lateinit var useCase: GenerateTicketsUseCase

    private val approvedPayment = PaymentResult(
        transactionId = "tx-999",
        nsu = "000999",
        authorizationCode = "AUTH9",
        status = PaymentStatus.APPROVED,
        paymentMethod = PaymentMethod.CREDIT,
        brand = "VISA",
        amountCents = 15000,
        respondedAt = Instant.now()
    )

    private val baseOrder = TicketOrder(
        orderId = "order-77",
        idempotencyKey = "idem-77",
        eventId = "event-77",
        eventTitle = "Festival X",
        quantity = 3,
        unitPriceCents = 5000,
        totalPriceCents = 15000,
        status = OrderStatus.APPROVED,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        payment = approvedPayment
    )

    @Before
    fun setUp() {
        useCase = GenerateTicketsUseCase()
    }

    @Test
    fun `pedido aprovado gera um ticket por unidade comprada`() {
        val result = useCase(baseOrder)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val tickets = (result as AppResult.Success).data
        assertThat(tickets).hasSize(3)
        tickets.forEachIndexed { index, ticket ->
            assertThat(ticket.orderId).isEqualTo("order-77")
            assertThat(ticket.sequence).isEqualTo(index + 1)
            assertThat(ticket.qrPayload).contains("order=order-77")
            assertThat(ticket.qrPayload).contains("tx=tx-999")
        }
    }

    @Test
    fun `cada ticket gerado tem qrPayload unico`() {
        val result = useCase(baseOrder) as AppResult.Success
        val payloads = result.data.map { it.qrPayload }
        assertThat(payloads.toSet()).hasSize(payloads.size)
    }

    @Test
    fun `pedido NAO aprovado nao gera tickets (vinculo com compra concluida)`() {
        val deniedOrder = baseOrder.copy(status = OrderStatus.DENIED)

        val result = useCase(deniedOrder)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }

    @Test
    fun `pedido PROCESSING nao gera tickets`() {
        val processingOrder = baseOrder.copy(status = OrderStatus.PROCESSING)

        val result = useCase(processingOrder)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }

    @Test
    fun `pedido CANCELED nao gera tickets`() {
        val canceledOrder = baseOrder.copy(status = OrderStatus.CANCELED)

        val result = useCase(canceledOrder)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }
}
