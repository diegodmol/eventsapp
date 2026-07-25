package com.eventtickets.feature.payment

import com.eventtickets.domain.model.TicketOrder

/**
 * Estado de UI da tela de pagamento (Fluxo 3: iniciar/concluir pagamento via
 * Cielo; Fluxo 4: registrar resultado da compra).
 */
data class PaymentUiState(
    val order: TicketOrder? = null,
    val isProcessingPayment: Boolean = false,
    val errorMessage: String? = null,
    val paymentFinished: Boolean = false
)
