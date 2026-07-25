package com.eventtickets.data.repository

import com.eventtickets.core.common.AppError
import com.eventtickets.core.common.AppResult
import com.eventtickets.data.local.dao.EventDao
import com.eventtickets.data.local.entity.toDomain
import com.eventtickets.data.local.entity.toEntity
import com.eventtickets.data.remote.EventRemoteDataSource
import com.eventtickets.domain.model.Event
import com.eventtickets.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val remoteDataSource: EventRemoteDataSource
) : EventRepository {

    override fun observeEvents(): Flow<List<Event>> =
        eventDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshEvents(): AppResult<Unit> {
        return when (val result = remoteDataSource.fetchEvents()) {
            is AppResult.Success -> {
                eventDao.upsertAll(result.data.map { it.toEntity() })
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> result
        }
    }

    override suspend fun getEventById(eventId: String): AppResult<Event> {
        val entity = eventDao.getById(eventId)
            ?: return AppResult.Failure(AppError.EventNotFound(eventId))
        return AppResult.Success(entity.toDomain())
    }
}
