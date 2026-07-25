package com.eventtickets.feature.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.usecase.ObserveOrderUseCase
import com.eventtickets.domain.usecase.StartPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ## Proteção contra reenvio de cobrança nesta camada
 * Mesmo já havendo a checagem de idempotência no [StartPaymentUseCase], a
 * ViewModel adiciona uma segunda barreira na UI: enquanto
 * [PaymentUiState.isProcessingPayment] é true, `pay()` retorna imediatamente
 * sem chamar o use case de novo. Isso cobre o caso de "reenvio de ação" mais
 * comum na prática (usuário tocando o botão várias vezes rapidamente) sem
 * sequer depender de uma viagem ao banco local para ser barrado.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeOrderUseCase: ObserveOrderUseCase,
    private val startPaymentUseCase: StartPaymentUseCase
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"]) {
        "PaymentViewModel requer o argumento de navegação 'orderId'"
    }

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        observeOrderUseCase(orderId)
            .onEach { order ->
                _uiState.value = _uiState.value.copy(
                    order = order,
                    paymentFinished = order?.status in setOf(
                        OrderStatus.APPROVED, OrderStatus.DENIED, OrderStatus.CANCELED
                    )
                )
            }
            .catch { throwable ->
                _uiState.value = _uiState.value.copy(errorMessage = throwable.message)
            }
            .launchIn(viewModelScope)
    }

    fun pay() {
        if (_uiState.value.isProcessingPayment) return
        val currentOrder = _uiState.value.order ?: return
        if (currentOrder.status != OrderStatus.CREATED && currentOrder.status != OrderStatus.ERROR) {
            // Já processado ou em processamento — não reenvia.
            return
        }

        _uiState.value = _uiState.value.copy(isProcessingPayment = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = startPaymentUseCase(orderId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isProcessingPayment = false)
                    // uiState.order já será atualizado pelo observeOrderUseCase (Flow do Room).
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessingPayment = false,
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
