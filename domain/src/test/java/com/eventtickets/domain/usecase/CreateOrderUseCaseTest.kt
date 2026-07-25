package com.eventtickets.domain.usecase

import com.eventtickets.core.common.AppError
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.Event
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.repository.EventRepository
import com.eventtickets.domain.repository.OrderRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class CreateOrderUseCaseTest {

    private lateinit var eventRepository: EventRepository
    private lateinit var orderRepository: OrderRepository
    private lateinit var useCase: CreateOrderUseCase

    private val event = Event(
        id = "event-1",
        title = "Show Teste",
        description = "desc",
        venue = "Arena",
        dateTime = LocalDateTime.now().plusDays(10),
        imageUrl = null,
        priceCents = 5000,
        availableTickets = 3
    )

    @Before
    fun setUp() {
        eventRepository = mockk()
        orderRepository = mockk()
        useCase = CreateOrderUseCase(eventRepository, orderRepository)
    }

    @Test
    fun `quantidade abaixo do minimo eh rejeitada`() = runTest {
        val result = useCase("event-1", 0)
        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.InvalidQuantity::class.java)
    }

    @Test
    fun `quantidade acima do limite por pedido eh rejeitada`() = runTest {
        val result = useCase("event-1", 999)
        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.InvalidQuantity::class.java)
    }

    @Test
    fun `quantidade maior que estoque disponivel eh rejeitada`() = runTest {
        coEvery { eventRepository.getEventById("event-1") } returns AppResult.Success(event)

        val result = useCase("event-1", 5) // evento só tem 3 disponíveis

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.InvalidQuantity::class.java)
        coVerify(exactly = 0) { orderRepository.createOrder(any(), any()) }
    }

    @Test
    fun `quantidade valida cria o pedido`() = runTest {
        coEvery { eventRepository.getEventById("event-1") } returns AppResult.Success(event)
        val expectedOrder = TicketOrder(
            orderId = "order-1",
            idempotencyKey = "idem-1",
            eventId = "event-1",
            eventTitle = event.title,
            quantity = 2,
            unitPriceCents = 5000,
            totalPriceCents = 10000,
            status = OrderStatus.CREATED,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { orderRepository.createOrder("event-1", 2) } returns AppResult.Success(expectedOrder)

        val result = useCase("event-1", 2)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data.quantity).isEqualTo(2)
    }

    @Test
    fun `evento inexistente propaga falha`() = runTest {
        coEvery { eventRepository.getEventById("event-x") } returns
            AppResult.Failure(AppError.EventNotFound("event-x"))

        val result = useCase("event-x", 1)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.EventNotFound::class.java)
    }
}
