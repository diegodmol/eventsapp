package com.eventtickets.feature.payment

import androidx.lifecycle.SavedStateHandle
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.usecase.ObserveOrderUseCase
import com.eventtickets.domain.usecase.StartPaymentUseCase
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
class PaymentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var observeOrderUseCase: ObserveOrderUseCase
    private lateinit var startPaymentUseCase: StartPaymentUseCase

    private val baseOrder = TicketOrder(
        orderId = "order-1",
        idempotencyKey = "idem-1",
        eventId = "evt-1",
        eventTitle = "Show Teste",
        quantity = 1,
        unitPriceCents = 5000,
        totalPriceCents = 5000,
        status = OrderStatus.CREATED,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        observeOrderUseCase = mockk()
        startPaymentUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): PaymentViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("orderId" to "order-1"))
        every { observeOrderUseCase("order-1") } returns MutableStateFlow(baseOrder)
        return PaymentViewModel(savedStateHandle, observeOrderUseCase, startPaymentUseCase)
    }

    @Test
    fun `pay chamado duas vezes rapidamente so aciona o use case uma vez`() = runTest {
        coEvery { startPaymentUseCase("order-1") } coAnswers {
            kotlinx.coroutines.delay(200)
            AppResult.Success(baseOrder.copy(status = OrderStatus.APPROVED))
        }

        val vm = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.pay()
        vm.pay() // reenvio imediato — deve ser ignorado pela UI

        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { startPaymentUseCase("order-1") }
    }

    @Test
    fun `pay nao eh chamado se pedido ja esta APPROVED`() = runTest {
        val approvedOrder = baseOrder.copy(status = OrderStatus.APPROVED)
        val savedStateHandle = SavedStateHandle(mapOf("orderId" to "order-1"))
        every { observeOrderUseCase("order-1") } returns MutableStateFlow(approvedOrder)
        val vm = PaymentViewModel(savedStateHandle, observeOrderUseCase, startPaymentUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        vm.pay()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { startPaymentUseCase(any()) }
    }

    @Test
    fun `erro no pagamento expoe mensagem e permite nova tentativa`() = runTest {
        coEvery { startPaymentUseCase("order-1") } returns AppResult.Failure(
            com.eventtickets.core.common.AppError.CieloSdkNotAvailable("emulador não encontrado")
        )

        val vm = buildViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.pay()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.uiState.value.errorMessage).isNotNull()
        assertThat(vm.uiState.value.isProcessingPayment).isFalse()
    }
}
