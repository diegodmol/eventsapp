package com.eventtickets.feature.receipt

import com.eventtickets.domain.model.Ticket
import com.eventtickets.domain.model.TicketOrder

/**
 * Estado de UI da tela de comprovante/resumo da compra (Fluxo 5), incluindo
 * os ingressos com QR Code (opcional) quando o pedido foi aprovado.
 */
data class ReceiptUiState(
    val order: TicketOrder? = null,
    val tickets: List<Ticket> = emptyList(),
    val isGeneratingTickets: Boolean = false,
    val errorMessage: String? = null
)
