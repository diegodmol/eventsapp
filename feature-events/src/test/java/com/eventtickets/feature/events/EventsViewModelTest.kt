package com.eventtickets.feature.events

import app.cash.turbine.test
import com.eventtickets.core.common.AppError
import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.Event
import com.eventtickets.domain.usecase.GetEventsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var getEventsUseCase: GetEventsUseCase

    private val sampleEvent = Event(
        id = "evt-1",
        title = "Show Teste",
        description = "desc",
        venue = "Arena",
        dateTime = LocalDateTime.now().plusDays(5),
        imageUrl = null,
        priceCents = 5000,
        availableTickets = 10
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        getEventsUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial expoe lista de eventos apos carregamento`() = runTest {
        val eventsFlow = MutableStateFlow(listOf(sampleEvent))
        every { getEventsUseCase() } returns eventsFlow
        coEvery { getEventsUseCase.refresh() } returns AppResult.Success(Unit)

        val viewModel = EventsViewModel(getEventsUseCase)

        viewModel.uiState.test {
            val initial = awaitItem()
            // pode já vir com os dados por causa do StateFlow "hot" no fake
            dispatcher.scheduler.advanceUntilIdle()
            val loaded = expectMostRecentItem()
            assertThat(loaded.events).contains(sampleEvent)
            assertThat(loaded.isLoading).isFalse()
        }
    }

    @Test
    fun `falha no refresh expoe mensagem de erro`() = runTest {
        val eventsFlow = MutableStateFlow(emptyList<Event>())
        every { getEventsUseCase() } returns eventsFlow
        coEvery { getEventsUseCase.refresh() } returns AppResult.Failure(
            AppError.Network("sem conexão")
        )

        val viewModel = EventsViewModel(getEventsUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.errorMessage).isNotNull()
        }
    }
}
