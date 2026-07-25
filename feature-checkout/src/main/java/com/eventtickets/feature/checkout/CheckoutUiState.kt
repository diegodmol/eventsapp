package com.eventtickets.feature.checkout

import com.eventtickets.domain.model.Event

/**
 * Estado de UI da tela de checkout (Fluxo 2: Selecionar a quantidade de
 * ingressos). Ao confirmar, o pedido (TicketOrder) é criado e seu orderId
 * é exposto via [createdOrderId] para navegação até a tela de pagamento.
 */
data class CheckoutUiState(
    val event: Event? = null,
    val quantity: Int = 1,
    val isCreatingOrder: Boolean = false,
    val errorMessage: String? = null,
    val createdOrderId: String? = null
) {
    val totalPriceCents: Long
        get() = (event?.priceCents ?: 0L) * quantity

    val canIncrement: Boolean
        get() = event != null && quantity < minOf(event.availableTickets, MAX_QUANTITY_PER_ORDER)

    val canDecrement: Boolean
        get() = quantity > MIN_QUANTITY

    companion object {
        const val MIN_QUANTITY = 1
        const val MAX_QUANTITY_PER_ORDER = 10
    }
}
