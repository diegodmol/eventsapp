package com.eventtickets.domain.usecase

import com.eventtickets.core.common.AppError
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.repository.EventRepository
import com.eventtickets.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Fluxo 2: Selecionar a quantidade de ingressos e criar o pedido correspondente.
 *
 * Valida a quantidade contra o estoque disponível do evento ANTES de criar o
 * pedido, e delega ao [OrderRepository] a geração de uma idempotencyKey
 * estável para esse pedido (reutilizada em todas as tentativas de pagamento).
 */
class CreateOrderUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val orderRepository: OrderRepository
) {
    companion object {
        const val MIN_QUANTITY = 1
        const val MAX_QUANTITY_PER_ORDER = 10
    }

    suspend operator fun invoke(eventId: String, quantity: Int): AppResult<TicketOrder> {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY_PER_ORDER) {
            return AppResult.Failure(AppError.InvalidQuantity(quantity, MAX_QUANTITY_PER_ORDER))
        }

        val eventResult = eventRepository.getEventById(eventId)
        val event = when (eventResult) {
            is AppResult.Success -> eventResult.data
            is AppResult.Failure -> return eventResult
        }

        if (quantity > event.availableTickets) {
            return AppResult.Failure(AppError.InvalidQuantity(quantity, event.availableTickets))
        }

        return orderRepository.createOrder(eventId, quantity)
    }
}
