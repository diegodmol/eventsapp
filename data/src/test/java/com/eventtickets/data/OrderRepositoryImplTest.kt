package com.eventtickets.data

import com.eventtickets.core.common.AppResult
import com.eventtickets.data.local.dao.EventDao
import com.eventtickets.data.local.dao.OrderDao
import com.eventtickets.data.local.entity.EventEntity
import com.eventtickets.data.local.entity.OrderEntity
import com.eventtickets.data.repository.OrderRepositoryImpl
import com.eventtickets.domain.model.OrderStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Testa OrderRepositoryImpl com DAOs mockados (unit test puro, rápido).
 * A cobertura de integração real do Room fica no androidTest (Room in-memory).
 */
class OrderRepositoryImplTest {

    private lateinit var orderDao: OrderDao
    private lateinit var eventDao: EventDao
    private lateinit var repository: OrderRepositoryImpl

    private val eventEntity = EventEntity(
        id = "evt-1",
        title = "Evento Teste",
        description = "desc",
        venue = "local",
        dateTimeEpochSeconds = System.currentTimeMillis() / 1000,
        imageUrl = null,
        priceCents = 5000,
        availableTickets = 10,
        currency = "BRL"
    )

    @Before
    fun setUp() {
        orderDao = mockk(relaxed = true)
        eventDao = mockk()
        repository = OrderRepositoryImpl(orderDao, eventDao)
    }

    @Test
    fun `hasActiveOrCompletedPayment retorna true quando ja existe transacao concluida para a idempotencyKey`() = runTest {
        val orderEntity = OrderEntity(
            orderId = "order-1",
            idempotencyKey = "idem-1",
            eventId = "evt-1",
            eventTitle = "Evento Teste",
            quantity = 1,
            unitPriceCents = 5000,
            totalPriceCents = 5000,
            status = OrderStatus.APPROVED.name,
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            paymentTransactionId = "tx-1",
            paymentNsu = "111",
            paymentAuthorizationCode = "AUTH",
            paymentStatus = "APPROVED",
            paymentMethod = "CREDIT",
            paymentBrand = "VISA",
            paymentAmountCents = 5000,
            paymentErrorCode = null,
            paymentErrorMessage = null,
            paymentRespondedAtEpochMillis = System.currentTimeMillis()
        )
        coEvery { orderDao.getById("order-1") } returns orderEntity
        coEvery { orderDao.countActiveOrCompleted("idem-1") } returns 1

        val result = repository.hasActiveOrCompletedPayment("order-1")

        assertThat(result).isTrue()
    }

    @Test
    fun `hasActiveOrCompletedPayment retorna false para pedido recem criado`() = runTest {
        val orderEntity = OrderEntity(
            orderId = "order-2",
            idempotencyKey = "idem-2",
            eventId = "evt-1",
            eventTitle = "Evento Teste",
            quantity = 1,
            unitPriceCents = 5000,
            totalPriceCents = 5000,
            status = OrderStatus.CREATED.name,
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            paymentTransactionId = null,
            paymentNsu = null,
            paymentAuthorizationCode = null,
            paymentStatus = null,
            paymentMethod = null,
            paymentBrand = null,
            paymentAmountCents = null,
            paymentErrorCode = null,
            paymentErrorMessage = null,
            paymentRespondedAtEpochMillis = null
        )
        coEvery { orderDao.getById("order-2") } returns orderEntity
        coEvery { orderDao.countActiveOrCompleted("idem-2") } returns 0

        val result = repository.hasActiveOrCompletedPayment("order-2")

        assertThat(result).isFalse()
    }

    @Test
    fun `createOrder reaproveita pedido pendente identico em vez de duplicar`() = runTest {
        coEvery { eventDao.getById("evt-1") } returns eventEntity
        val pending = OrderEntity(
            orderId = "order-existing",
            idempotencyKey = "idem-existing",
            eventId = "evt-1",
            eventTitle = "Evento Teste",
            quantity = 2,
            unitPriceCents = 5000,
            totalPriceCents = 10000,
            status = OrderStatus.CREATED.name,
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            paymentTransactionId = null,
            paymentNsu = null,
            paymentAuthorizationCode = null,
            paymentStatus = null,
            paymentMethod = null,
            paymentBrand = null,
            paymentAmountCents = null,
            paymentErrorCode = null,
            paymentErrorMessage = null,
            paymentRespondedAtEpochMillis = null
        )
        coEvery { orderDao.findPendingOrderForEvent("evt-1") } returns pending

        val result = repository.createOrder("evt-1", 2)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data.orderId).isEqualTo("order-existing")
    }
}
