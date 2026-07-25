package com.eventtickets.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.PaymentMethod
import com.eventtickets.domain.model.PaymentResult
import com.eventtickets.domain.model.PaymentStatus
import com.eventtickets.domain.model.TicketOrder
import java.time.Instant

/**
 * Entidade única (order + último resultado de pagamento embutido) para manter
 * a atualização atômica em uma única transação de UPDATE — evita janelas de
 * inconsistência entre "status do pedido" e "resultado do pagamento" que
 * poderiam ser exploradas por um reenvio de ação concorrente.
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val orderId: String,
    val idempotencyKey: String,
    val eventId: String,
    val eventTitle: String,
    val quantity: Int,
    val unitPriceCents: Long,
    val totalPriceCents: Long,
    val status: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,

    val paymentTransactionId: String?,
    val paymentNsu: String?,
    val paymentAuthorizationCode: String?,
    val paymentStatus: String?,
    val paymentMethod: String?,
    val paymentBrand: String?,
    val paymentAmountCents: Long?,
    val paymentErrorCode: String?,
    val paymentErrorMessage: String?,
    val paymentRespondedAtEpochMillis: Long?
)

fun OrderEntity.toDomain(): TicketOrder {
    val payment = paymentStatus?.let {
        PaymentResult(
            transactionId = paymentTransactionId,
            nsu = paymentNsu,
            authorizationCode = paymentAuthorizationCode,
            status = PaymentStatus.valueOf(it),
            paymentMethod = paymentMethod?.let { m -> PaymentMethod.valueOf(m) },
            brand = paymentBrand,
            amountCents = paymentAmountCents ?: totalPriceCents,
            errorCode = paymentErrorCode,
            errorMessage = paymentErrorMessage,
            respondedAt = Instant.ofEpochMilli(paymentRespondedAtEpochMillis ?: updatedAtEpochMillis)
        )
    }
    return TicketOrder(
        orderId = orderId,
        idempotencyKey = idempotencyKey,
        eventId = eventId,
        eventTitle = eventTitle,
        quantity = quantity,
        unitPriceCents = unitPriceCents,
        totalPriceCents = totalPriceCents,
        status = OrderStatus.valueOf(status),
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
        payment = payment
    )
}

fun TicketOrder.toEntity(): OrderEntity = OrderEntity(
    orderId = orderId,
    idempotencyKey = idempotencyKey,
    eventId = eventId,
    eventTitle = eventTitle,
    quantity = quantity,
    unitPriceCents = unitPriceCents,
    totalPriceCents = totalPriceCents,
    status = status.name,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    paymentTransactionId = payment?.transactionId,
    paymentNsu = payment?.nsu,
    paymentAuthorizationCode = payment?.authorizationCode,
    paymentStatus = payment?.status?.name,
    paymentMethod = payment?.paymentMethod?.name,
    paymentBrand = payment?.brand,
    paymentAmountCents = payment?.amountCents,
    paymentErrorCode = payment?.errorCode,
    paymentErrorMessage = payment?.errorMessage,
    paymentRespondedAtEpochMillis = payment?.respondedAt?.toEpochMilli()
)