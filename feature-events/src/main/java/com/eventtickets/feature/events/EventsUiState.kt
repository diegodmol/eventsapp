package com.eventtickets.feature.events

import com.eventtickets.domain.model.Event

/**
 * Estado de UI da tela de listagem de eventos (Fluxo 1: Visualizar eventos
 * disponíveis para compra).
 */
data class EventsUiState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)
