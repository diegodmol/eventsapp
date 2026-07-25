package com.eventtickets.domain.usecase

import com.eventtickets.core.common.AppError
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.Ticket
import com.eventtickets.domain.model.TicketOrder
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Gera os ingressos (com payload de QR Code) para um pedido.
 *
 * Regra de negócio central: só gera ingressos para pedidos com status
 * APPROVED. Isso garante — por construção — que "o ingresso deve estar
 * vinculado à compra concluída", conforme exigido no enunciado.
 */
class GenerateTicketsUseCase @Inject constructor() {

    operator fun invoke(order: TicketOrder): AppResult<List<Ticket>> {
        if (order.status != OrderStatus.APPROVED) {
            return AppResult.Failure(
                AppError.Unknown("Não é possível gerar ingressos para pedido com status ${order.status}")
            )
        }

        val issuedAt = Instant.now()
        val tickets = (1..order.quantity).map { sequence ->
            val ticketId = UUID.randomUUID().toString()
            Ticket(
                ticketId = ticketId,
                orderId = order.orderId,
                eventId = order.eventId,
                eventTitle = order.eventTitle,
                sequence = sequence,
                totalInOrder = order.quantity,
                qrPayload = buildQrPayload(order, ticketId, sequence),
                issuedAt = issuedAt
            )
        }
        return AppResult.Success(tickets)
    }

    /**
     * Payload compacto e determinístico (não é um JWT assinado nesta versão —
     * ver docs/adr/0003 para discussão de assinatura/validação offline em uma
     * evolução futura com backend).
     */
    private fun buildQrPayload(order: TicketOrder, ticketId: String, sequence: Int): String {
        val txId = order.payment?.transactionId ?: "N/A"
        return "EVT|order=${order.orderId}|ticket=$ticketId|seq=$sequence/${order.quantity}|tx=$txId"
    }
}
