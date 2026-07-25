package com.eventtickets.data.remote

import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.Event
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fonte de dados de eventos.
 *
 * O enunciado explicita que a construção de um backend de apoio NÃO será
 * avaliada. Por isso, esta fonte simula uma chamada remota (delay + payload
 * fixo) mas mantém a interface exatamente como um EventApi via Retrofit se
 * comportaria — trocar esta classe por uma implementação Retrofit real é uma
 * mudança isolada nesta única classe (Single Responsibility / Dependency
 * Inversion), sem qualquer impacto em domain, use cases ou UI.
 */
@Singleton
class EventRemoteDataSource @Inject constructor() {

    suspend fun fetchEvents(): AppResult<List<Event>> {
        return try {
            delay(400) // simula latência de rede real
            AppResult.Success(seedEvents())
        } catch (e: Exception) {
            AppResult.Failure(
                com.eventtickets.core.common.AppError.Network(
                    reason = e.message ?: "erro desconhecido ao buscar eventos",
                    throwable = e
                )
            )
        }
    }

    private fun seedEvents(): List<Event> = listOf(
        Event(
            id = "evt-rock-fest",
            title = "Rock Fest São Paulo",
            description = "O maior festival de rock do ano, com bandas nacionais e internacionais.",
            venue = "Allianz Parque - São Paulo/SP",
            dateTime = LocalDateTime.now().plusDays(30).withHour(19).withMinute(0),
            imageUrl = null,
            priceCents = 25000,
            availableTickets = 150
        ),
        Event(
            id = "evt-stand-up",
            title = "Noite de Stand-up Comedy",
            description = "Uma noite de comédia com os melhores humoristas da cidade.",
            venue = "Teatro Bradesco - São Paulo/SP",
            dateTime = LocalDateTime.now().plusDays(10).withHour(21).withMinute(0),
            imageUrl = null,
            priceCents = 8000,
            availableTickets = 40
        ),
        Event(
            id = "evt-tech-conf",
            title = "TechConf Android 2026",
            description = "Conferência sobre desenvolvimento Android moderno com Kotlin e Compose.",
            venue = "Centro de Convenções - São Paulo/SP",
            dateTime = LocalDateTime.now().plusDays(45).withHour(9).withMinute(0),
            imageUrl = null,
            priceCents = 12000,
            availableTickets = 5
        ),
        Event(
            id = "evt-esgotado",
            title = "Show Especial de Fim de Ano",
            description = "Edição especial com ingressos já esgotados — usado para testar o estado 'sold out'.",
            venue = "Espaço das Américas - São Paulo/SP",
            dateTime = LocalDateTime.now().plusDays(60).withHour(20).withMinute(0),
            imageUrl = null,
            priceCents = 15000,
            availableTickets = 0
        )
    )
}
