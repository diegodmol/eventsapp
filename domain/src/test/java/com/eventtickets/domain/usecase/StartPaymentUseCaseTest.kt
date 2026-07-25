package com.eventtickets.domain.usecase

import com.eventtickets.core.common.AppError
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.PaymentMethod
import com.eventtickets.domain.model.PaymentResult
import com.eventtickets.domain.model.PaymentStatus
import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.repository.OrderRepository
import com.eventtickets.domain.repository.PaymentGateway
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class StartPaymentUseCaseTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var paymentGateway: PaymentGateway
    private lateinit var useCase: StartPaymentUseCase

    private val baseOrder = TicketOrder(
        orderId = "order-1",
        idempotencyKey = "idem-key-1",
        eventId = "event-1",
        eventTitle = "Show Teste",
        quantity = 2,
        unitPriceCents = 5000,
        totalPriceCents = 10000,
        status = OrderStatus.CREATED,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Before
    fun setUp() {
        orderRepository = mockk()
        paymentGateway = mockk()
        useCase = StartPaymentUseCase(orderRepository, paymentGateway)
    }

    @Test
    fun `quando pagamento eh aprovado, registra status APPROVED`() = runTest {
        coEvery { orderRepository.getOrder("order-1") } returns AppResult.Success(baseOrder)
        coEvery { orderRepository.hasActiveOrCompletedPayment("order-1") } returns false
        coEvery {
            orderRepository.recordPaymentResult("order-1", OrderStatus.PROCESSING, null)
        } returns AppResult.Success(baseOrder.copy(status = OrderStatus.PROCESSING))

        val approvedPayment = PaymentResult(
            transactionId = "tx-123",
            nsu = "000123",
            authorizationCode = "AUTH1",
            status = PaymentStatus.APPROVED,
            paymentMethod = PaymentMethod.CREDIT,
            brand = "VISA",
            amountCents = 10000,
            respondedAt = Instant.now()
        )
        coEvery {
            paymentGateway.startPayment("order-1", "idem-key-1", 10000)
        } returns AppResult.Success(approvedPayment)

        val finalOrder = baseOrder.copy(status = OrderStatus.APPROVED, payment = approvedPayment)
        coEvery {
            orderRepository.recordPaymentResult("order-1", OrderStatus.APPROVED, approvedPayment)
        } returns AppResult.Success(finalOrder)

        val result = useCase("order-1")

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data.status).isEqualTo(OrderStatus.APPROVED)
        coVerify(exactly = 1) { paymentGateway.startPayment(any(), any(), any()) }
    }

    @Test
    fun `quando ja existe pagamento ativo ou concluido, bloqueia nova chamada ao gateway (anti-duplicidade)`() = runTest {
        coEvery { orderRepository.getOrder("order-1") } returns AppResult.Success(baseOrder)
        coEvery { orderRepository.hasActiveOrCompletedPayment("order-1") } returns true

        val result = useCase("order-1")

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.DuplicatePaymentAttempt::class.java)
        coVerify(exactly = 0) { paymentGateway.startPayment(any(), any(), any()) }
    }

    @Test
    fun `quando pedido ja esta APPROVED, nao permite reenviar pagamento`() = runTest {
        val approvedOrder = baseOrder.copy(status = OrderStatus.APPROVED)
        coEvery { orderRepository.getOrder("order-1") } returns AppResult.Success(approvedOrder)

        val result = useCase("order-1")

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.DuplicatePaymentAttempt::class.java)
        coVerify(exactly = 0) { paymentGateway.startPayment(any(), any(), any()) }
        coVerify(exactly = 0) { orderRepository.hasActiveOrCompletedPayment(any()) }
    }

    @Test
    fun `quando pedido esta PROCESSING (reentrada concorrente), bloqueia`() = runTest {
        val processingOrder = baseOrder.copy(status = OrderStatus.PROCESSING)
        coEvery { orderRepository.getOrder("order-1") } returns AppResult.Success(processingOrder)

        val result = useCase("order-1")

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.DuplicatePaymentAttempt::class.java)
        coVerify(exactly = 0) { paymentGateway.startPayment(any(), any(), any()) }
    }

    @Test
    fun `quando pagamento eh negado, registra status DENIED e retorna sucesso com pedido atualizado`() = runTest {
        coEvery { orderRepository.getOrder("order-1") } returns AppResult.Success(baseOrder)
        coEvery { orderRepository.hasActiveOrCompletedPayment("order-1") } returns false
        coEvery {
            orderRepository.recordPaymentResult("order-1", OrderStatus.PROCESSING, null)
        } returns AppResult.Success(baseOrder.copy(status = OrderStatus.PROCESSING))

        val deniedPayment = PaymentResult(
            transactionId = "tx-124",
            nsu = "000124",
            authorizationCode = null,
            status = PaymentStatus.DENIED,
            paymentMethod = PaymentMethod.CREDIT,
            brand = "MASTERCARD",
            amountCents = 10000,
            errorCode = "51",
            errorMessage = "Saldo insuficiente",
            respondedAt = Instant.now()
        )
        coEvery {
            paymentGateway.startPayment("order-1", "idem-key-1", 10000)
        } returns AppResult.Success(deniedPayment)

        val finalOrder = baseOrder.copy(status = OrderStatus.DENIED, payment = deniedPayment)
        coEvery {
            orderRepository.recordPaymentResult("order-1", OrderStatus.DENIED, deniedPayment)
        } returns AppResult.Success(finalOrder)

        val result = useCase("order-1")

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data.status).isEqualTo(OrderStatus.DENIED)
    }

    @Test
    fun `quando gateway retorna erro de integracao, registra status ERROR sem perder rastro`() = runTest {
        coEvery { orderRepository.getOrder("order-1") } returns AppResult.Success(baseOrder)
        coEvery { orderRepository.hasActiveOrCompletedPayment("order-1") } returns false
        coEvery {
            orderRepository.recordPaymentResult("order-1", OrderStatus.PROCESSING, null)
        } returns AppResult.Success(baseOrder.copy(status = OrderStatus.PROCESSING))

        coEvery {
            paymentGateway.startPayment("order-1", "idem-key-1", 10000)
        } returns AppResult.Failure(AppError.CieloSdkNotAvailable("terminal desconectado"))

        coEvery {
            orderRepository.recordPaymentResult("order-1", OrderStatus.ERROR, any())
        } returns AppResult.Success(baseOrder.copy(status = OrderStatus.ERROR))

        val result = useCase("order-1")

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.CieloSdkNotAvailable::class.java)
        coVerify(exactly = 1) {
            orderRepository.recordPaymentResult("order-1", OrderStatus.ERROR, any())
        }
    }

    @Test
    fun `retentativa apos ERROR eh permitida (nao eh tratada como duplicidade)`() = runTest {
        val erroredOrder = baseOrder.copy(status = OrderStatus.ERROR)
        coEvery { orderRepository.getOrder("order-1") } returns AppResult.Success(erroredOrder)
        coEvery { orderRepository.hasActiveOrCompletedPayment("order-1") } returns false
        coEvery {
            orderRepository.recordPaymentResult("order-1", OrderStatus.PROCESSING, null)
        } returns AppResult.Success(erroredOrder.copy(status = OrderStatus.PROCESSING))

        val approvedPayment = PaymentResult(
            transactionId = "tx-125",
            nsu = "000125",
            authorizationCode = "AUTH2",
            status = PaymentStatus.APPROVED,
            paymentMethod = PaymentMethod.PIX,
            brand = null,
            amountCents = 10000,
            respondedAt = Instant.now()
        )
        coEvery {
            paymentGateway.startPayment("order-1", "idem-key-1", 10000)
        } returns AppResult.Success(approvedPayment)
        coEvery {
            orderRepository.recordPaymentResult("order-1", OrderStatus.APPROVED, approvedPayment)
        } returns AppResult.Success(erroredOrder.copy(status = OrderStatus.APPROVED, payment = approvedPayment))

        val result = useCase("order-1")

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        coVerify(exactly = 1) { paymentGateway.startPayment(any(), any(), any()) }
    }
}
