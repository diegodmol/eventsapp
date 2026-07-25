package com.eventtickets.data.repository

import com.eventtickets.core.common.AppError
import com.eventtickets.core.common.AppResult
import com.eventtickets.data.local.dao.EventDao
import com.eventtickets.data.local.dao.OrderDao
import com.eventtickets.data.local.entity.OrderEntity
import com.eventtickets.data.local.entity.toDomain
import com.eventtickets.data.local.entity.toEntity
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.PaymentResult
import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao,
    private val eventDao: EventDao
) : OrderRepository {

    override suspend fun createOrder(eventId: String, quantity: Int): AppResult<TicketOrder> {
        val event = eventDao.getById(eventId)
            ?: return AppResult.Failure(AppError.EventNotFound(eventId))

        // Reaproveita um pedido CREATED (ainda não pago) para o mesmo evento em
        // vez de criar infinitos pedidos órfãos a cada toque em "Selecionar
        // quantidade" — mitiga parte da duplicidade já na criação do pedido.
        val existingPending = orderDao.findPendingOrderForEvent(eventId)
        if (existingPending != null && existingPending.quantity == quantity) {
            return AppResult.Success(existingPending.toDomain())
        }

        val now = Instant.now()
        val order = TicketOrder(
            orderId = UUID.randomUUID().toString(),
            idempotencyKey = UUID.randomUUID().toString(),
            eventId = event.id,
            eventTitle = event.title,
            quantity = quantity,
            unitPriceCents = event.priceCents,
            totalPriceCents = event.priceCents * quantity,
            status = OrderStatus.CREATED,
            createdAt = now,
            updatedAt = now
        )
        orderDao.insert(order.toEntity())
        return AppResult.Success(order)
    }

    override fun observeOrder(orderId: String): Flow<TicketOrder?> =
        orderDao.observeById(orderId).map { it?.toDomain() }

    override suspend fun getOrder(orderId: String): AppResult<TicketOrder> {
        val entity = orderDao.getById(orderId)
            ?: return AppResult.Failure(AppError.OrderNotFound(orderId))
        return AppResult.Success(entity.toDomain())
    }

    override suspend fun recordPaymentResult(
        orderId: String,
        status: OrderStatus,
        paymentResult: PaymentResult?
    ): AppResult<TicketOrder> {
        val current = orderDao.getById(orderId)
            ?: return AppResult.Failure(AppError.OrderNotFound(orderId))
        val currentDomain = current.toDomain()

        val updatedDomain = currentDomain.copy(
            status = status,
            updatedAt = Instant.now(),
            payment = paymentResult ?: currentDomain.payment
        )

        val updatedEntity: OrderEntity = updatedDomain.toEntity()
        orderDao.update(updatedEntity)

        // Baixa de estoque só ocorre quando o pagamento é de fato APROVADO,
        // evitando reduzir disponibilidade para pagamentos negados/cancelados
        // ou para tentativas repetidas do mesmo pedido.
        if (status == OrderStatus.APPROVED) {
            eventDao.decrementAvailability(updatedDomain.eventId, updatedDomain.quantity)
        }

        return AppResult.Success(updatedEntity.toDomain())
    }

    override suspend fun hasActiveOrCompletedPayment(orderId: String): Boolean {
        val order = orderDao.getById(orderId) ?: return false
        return orderDao.countActiveOrCompleted(order.idempotencyKey) > 0
    }
}
