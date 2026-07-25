package com.eventtickets.feature.checkout

import androidx.lifecycle.SavedStateHandle
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.Event
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.repository.EventRepository
import com.eventtickets.domain.usecase.CreateOrderUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var eventRepository: EventRepository
    private lateinit var createOrderUseCase: CreateOrderUseCase

    private val event = Event(
        id = "evt-1",
        title = "Show Teste",
        description = "desc",
        venue = "Arena",
        dateTime = LocalDateTime.now().plusDays(5),
        imageUrl = null,
        priceCents = 5000,
        availableTickets = 10
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        eventRepository = mockk()
        createOrderUseCase = mockk()
        coEvery { eventRepository.getEventById("evt-1") } returns AppResult.Success(event)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): CheckoutViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("eventId" to "evt-1"))
        return CheckoutViewModel(savedStateHandle, eventRepository, createOrderUseCase)
    }

    @Test
    fun `incrementar alem do estoque disponivel eh ignorado`() = runTest {
        val vm = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        repeat(20) { vm.incrementQuantity() }

        assertThat(vm.uiState.value.quantity).isEqualTo(10) // availableTickets = 10
    }

    @Test
    fun `decrementar abaixo de 1 eh ignorado`() = runTest {
        val vm = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        repeat(5) { vm.decrementQuantity() }

        assertThat(vm.uiState.value.quantity).isEqualTo(1)
    }

    @Test
    fun `confirmSelection chamado duas vezes em sequencia so cria um pedido (protecao contra duplo tap)`() = runTest {
        val order = TicketOrder(
            orderId = "order-1",
            idempotencyKey = "idem-1",
            eventId = "evt-1",
            eventTitle = event.title,
            quantity = 1,
            unitPriceCents = 5000,
            totalPriceCents = 5000,
            status = OrderStatus.CREATED,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { createOrderUseCase("evt-1", 1) } coAnswers {
            kotlinx.coroutines.delay(100)
            AppResult.Success(order)
        }

        val vm = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.confirmSelection()
        vm.confirmSelection() // segundo tap enquanto o primeiro ainda está em andamento
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { createOrderUseCase("evt-1", 1) }
        assertThat(vm.uiState.value.createdOrderId).isEqualTo("order-1")
    }
}
