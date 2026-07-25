package com.eventtickets.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eventtickets.domain.model.Ticket
import java.time.Instant

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val ticketId: String,
    val orderId: String,
    val eventId: String,
    val eventTitle: String,
    val sequence: Int,
    val totalInOrder: Int,
    val qrPayload: String,
    val issuedAtEpochMillis: Long
)

fun TicketEntity.toDomain(): Ticket = Ticket(
    ticketId = ticketId,
    orderId = orderId,
    eventId = eventId,
    eventTitle = eventTitle,
    sequence = sequence,
    totalInOrder = totalInOrder,
    qrPayload = qrPayload,
    issuedAt = Instant.ofEpochMilli(issuedAtEpochMillis)
)

fun Ticket.toEntity(): TicketEntity = TicketEntity(
    ticketId = ticketId,
    orderId = orderId,
    eventId = eventId,
    eventTitle = eventTitle,
    sequence = sequence,
    totalInOrder = totalInOrder,
    qrPayload = qrPayload,
    issuedAtEpochMillis = issuedAt.toEpochMilli()
)
