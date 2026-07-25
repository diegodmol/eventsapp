package com.eventtickets.domain.usecase

import com.eventtickets.domain.model.Event
import com.eventtickets.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Fluxo 1: Visualizar eventos disponíveis para compra.
 */
class GetEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> = eventRepository.observeEvents()

    suspend fun refresh() = eventRepository.refreshEvents()
}
