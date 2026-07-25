package com.eventtickets.feature.receipt

import androidx.lifecycle.SavedStateHandle
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.PaymentMethod
import com.eventtickets.domain.model.PaymentResult
import com.eventtickets.domain.model.PaymentStatus
import com.eventtickets.domain.model.Ticket
import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.repository.TicketRepository
import com.eventtickets.domain.usecase.EnsureTicketsIssuedUseCase
import com.eventtickets.domain.usecase.ObserveOrderUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var observeOrderUseCase: ObserveOrderUseCase
    private lateinit var ensureTicketsIssuedUseCase: EnsureTicketsIssuedUseCase
    private lateinit var ticketRepository: TicketRepository

    private val payment = PaymentResult(
        transactionId = "tx-1",
        nsu = "111",
        authorizationCode = "AUTH",
        status = PaymentStatus.APPROVED,
        paymentMethod = PaymentMethod.CREDIT,
        brand = "VISA",
        amountCents = 5000,
        respondedAt = Instant.now()
    )

    private val approvedOrder = TicketOrder(
        orderId = "order-1",
        idempotencyKey = "idem-1",
        eventId = "evt-1",
        eventTitle = "Show Teste",
        quantity = 1,
        unitPriceCents = 5000,
        totalPriceCents = 5000,
        status = OrderStatus.APPROVED,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        payment = payment
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        observeOrderUseCase = mockk()
        ensureTicketsIssuedUseCase = mockk()
        ticketRepository = mockk()
        every { ticketRepository.observeTicketsForOrder("order-1") } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pedido aprovado dispara emissao de ingressos`() = runTest {
        every { observeOrderUseCase("order-1") } returns MutableStateFlow(approvedOrder)
        coEvery { ensureTicketsIssuedUseCase(approvedOrder) } returns AppResult.Success(emptyList())

        val savedStateHandle = SavedStateHandle(mapOf("orderId" to "order-1"))
        ReceiptViewModel(savedStateHandle, observeOrderUseCase, ensureTicketsIssuedUseCase, ticketRepository)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { ensureTicketsIssuedUseCase(approvedOrder) }
    }

    @Test
    fun `pedido negado NAO dispara emissao de ingressos`() = runTest {
        val deniedOrder = approvedOrder.copy(status = OrderStatus.DENIED)
        every { observeOrderUseCase("order-1") } returns MutableStateFlow(deniedOrder)

        val savedStateHandle = SavedStateHandle(mapOf("orderId" to "order-1"))
        ReceiptViewModel(savedStateHandle, observeOrderUseCase, ensureTicketsIssuedUseCase, ticketRepository)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { ensureTicketsIssuedUseCase(any()) }
    }
}
