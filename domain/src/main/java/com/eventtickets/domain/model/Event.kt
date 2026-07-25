package com.eventtickets.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Representa um evento disponível para venda de ingressos.
 *
 * [priceCents] é mantido em centavos (Long) para evitar erros de arredondamento
 * de ponto flutuante em cálculos monetários — decisão registrada em docs/adr/0002.
 */
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val venue: String,
    val dateTime: LocalDateTime,
    val imageUrl: String?,
    val priceCents: Long,
    val availableTickets: Int,
    val currency: String = "BRL"
) {
    val priceAsDecimal: BigDecimal
        get() = BigDecimal(priceCents).movePointLeft(2)

    val isSoldOut: Boolean
        get() = availableTickets <= 0
}
