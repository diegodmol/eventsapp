package com.eventtickets.domain.repository

import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.Ticket
import kotlinx.coroutines.flow.Flow

interface TicketRepository {
    suspend fun saveTickets(tickets: List<Ticket>): AppResult<Unit>
    fun observeTicketsForOrder(orderId: String): Flow<List<Ticket>>
    suspend fun hasTicketsForOrder(orderId: String): Boolean
}
