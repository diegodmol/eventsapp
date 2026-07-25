package com.eventtickets.data.repository

import com.eventtickets.core.common.AppResult
import com.eventtickets.data.local.dao.TicketDao
import com.eventtickets.data.local.entity.toDomain
import com.eventtickets.data.local.entity.toEntity
import com.eventtickets.domain.model.Ticket
import com.eventtickets.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TicketRepositoryImpl @Inject constructor(
    private val ticketDao: TicketDao
) : TicketRepository {

    override suspend fun saveTickets(tickets: List<Ticket>): AppResult<Unit> {
        ticketDao.insertAll(tickets.map { it.toEntity() })
        return AppResult.Success(Unit)
    }

    override fun observeTicketsForOrder(orderId: String): Flow<List<Ticket>> =
        ticketDao.observeByOrderId(orderId).map { list -> list.map { it.toDomain() } }

    override suspend fun hasTicketsForOrder(orderId: String): Boolean =
        ticketDao.countForOrder(orderId) > 0
}
