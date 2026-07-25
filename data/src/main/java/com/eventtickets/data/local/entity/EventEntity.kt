package com.eventtickets.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eventtickets.domain.model.Event
import java.time.LocalDateTime

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val venue: String,
    val dateTimeEpochSeconds: Long,
    val imageUrl: String?,
    val priceCents: Long,
    val availableTickets: Int,
    val currency: String
)

fun EventEntity.toDomain(): Event = Event(
    id = id,
    title = title,
    description = description,
    venue = venue,
    dateTime = LocalDateTime.ofEpochSecond(
        dateTimeEpochSeconds, 0, java.time.ZoneOffset.UTC
    ),
    imageUrl = imageUrl,
    priceCents = priceCents,
    availableTickets = availableTickets,
    currency = currency
)

fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
    title = title,
    description = description,
    venue = venue,
    dateTimeEpochSeconds = dateTime.toEpochSecond(java.time.ZoneOffset.UTC),
    imageUrl = imageUrl,
    priceCents = priceCents,
    availableTickets = availableTickets,
    currency = currency
)
