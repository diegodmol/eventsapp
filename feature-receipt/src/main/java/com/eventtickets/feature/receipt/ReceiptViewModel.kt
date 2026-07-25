package com.eventtickets.feature.receipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.repository.TicketRepository
import com.eventtickets.domain.usecase.EnsureTicketsIssuedUseCase
import com.eventtickets.domain.usecase.ObserveOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeOrderUseCase: ObserveOrderUseCase,
    private val ensureTicketsIssuedUseCase: EnsureTicketsIssuedUseCase,
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"]) {
        "ReceiptViewModel requer o argumento de navegação 'orderId'"
    }

    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    init {
        observeOrderUseCase(orderId)
            .onEach { order ->
                _uiState.value = _uiState.value.copy(order = order)
                if (order?.status == OrderStatus.APPROVED) {
                    issueTicketsIfNeeded(order)
                }
            }
            .launchIn(viewModelScope)

        ticketRepository.observeTicketsForOrder(orderId)
            .onEach { tickets ->
                _uiState.value = _uiState.value.copy(tickets = tickets)
            }
            .launchIn(viewModelScope)
    }

    private fun issueTicketsIfNeeded(order: com.eventtickets.domain.model.TicketOrder) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingTickets = true)
            when (val result = ensureTicketsIssuedUseCase(order)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isGeneratingTickets = false)
                    // Os tickets chegam via o Flow do ticketRepository acima.
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingTickets = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }
}
