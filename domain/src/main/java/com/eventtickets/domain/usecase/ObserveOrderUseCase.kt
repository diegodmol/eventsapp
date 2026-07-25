package com.eventtickets.domain.usecase

import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Fluxo 5: Exibir comprovante/resumo da compra (também usado durante o
 * processamento para refletir mudanças de status em tempo real na UI).
 */
class ObserveOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    operator fun invoke(orderId: String): Flow<TicketOrder?> = orderRepository.observeOrder(orderId)
}
