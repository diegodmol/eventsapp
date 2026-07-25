package com.eventtickets.domain.usecase

import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.Ticket
import com.eventtickets.domain.model.TicketOrder
import com.eventtickets.domain.repository.TicketRepository
import javax.inject.Inject

/**
 * Garante que os ingressos de um pedido APROVADO existam, gerando-os apenas
 * uma vez (checa [TicketRepository.hasTicketsForOrder] antes de gerar), para
 * que reabrir a tela de comprovante várias vezes não gere QR Codes
 * duplicados/diferentes para o mesmo pedido.
 */
class EnsureTicketsIssuedUseCase @Inject constructor(
    private val generateTicketsUseCase: GenerateTicketsUseCase,
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(order: TicketOrder): AppResult<List<Ticket>> {
        if (ticketRepository.hasTicketsForOrder(order.orderId)) {
            // Já existem; o chamador deve observar via
            // TicketRepository.observeTicketsForOrder para obtê-los.
            return AppResult.Success(emptyList())
        }

        return when (val generated = generateTicketsUseCase(order)) {
            is AppResult.Success -> {
                ticketRepository.saveTickets(generated.data)
                generated
            }
            is AppResult.Failure -> generated
        }
    }
}
