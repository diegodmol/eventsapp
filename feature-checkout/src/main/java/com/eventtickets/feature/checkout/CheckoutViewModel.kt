package com.eventtickets.feature.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.repository.EventRepository
import com.eventtickets.domain.usecase.CreateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val createOrderUseCase: CreateOrderUseCase
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"]) {
        "CheckoutViewModel requer o argumento de navegação 'eventId'"
    }

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        loadEvent()
    }

    private fun loadEvent() {
        viewModelScope.launch {
            when (val result = eventRepository.getEventById(eventId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(event = result.data)
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
                }
            }
        }
    }

    fun incrementQuantity() {
        if (_uiState.value.canIncrement) {
            _uiState.value = _uiState.value.copy(quantity = _uiState.value.quantity + 1)
        }
    }

    fun decrementQuantity() {
        if (_uiState.value.canDecrement) {
            _uiState.value = _uiState.value.copy(quantity = _uiState.value.quantity - 1)
        }
    }

    /**
     * Confirma a seleção e cria o pedido local. Protegido contra duplo-tap:
     * enquanto [CheckoutUiState.isCreatingOrder] é true, chamadas repetidas
     * são ignoradas (ver checagem no início da função).
     */
    fun confirmSelection() {
        if (_uiState.value.isCreatingOrder || _uiState.value.createdOrderId != null) return

        val event = _uiState.value.event ?: return
        _uiState.value = _uiState.value.copy(isCreatingOrder = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = createOrderUseCase(event.id, _uiState.value.quantity)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingOrder = false,
                        createdOrderId = result.data.orderId
                    )
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingOrder = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
