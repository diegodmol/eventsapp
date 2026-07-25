package com.eventtickets.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.usecase.GetEventsUseCase
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
 * Segue a diretriz oficial de arquitetura Android: ViewModel expõe estado
 * imutável via StateFlow, sobrevive a mudanças de configuração, e toda
 * lógica de negócio fica delegada ao use case (nenhuma regra de negócio
 * aqui, apenas orquestração de estado de UI).
 */
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        observeEvents()
        refresh()
    }

    private fun observeEvents() {
        getEventsUseCase()
            .onEach { events ->
                _uiState.value = _uiState.value.copy(events = events, isLoading = false)
            }
            .catch { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Não foi possível carregar os eventos localmente: ${throwable.message}"
                )
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            when (val result = getEventsUseCase.refresh()) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isLoading = false,
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
