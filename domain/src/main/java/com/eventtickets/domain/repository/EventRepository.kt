package com.eventtickets.domain.repository

import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    /** Fluxo reativo de eventos, alimentado pelo cache local (Room) atualizado por sync. */
    fun observeEvents(): Flow<List<Event>>

    /** Força uma sincronização com a fonte remota (ou mock/local, se não houver backend). */
    suspend fun refreshEvents(): AppResult<Unit>

    suspend fun getEventById(eventId: String): AppResult<Event>
}
