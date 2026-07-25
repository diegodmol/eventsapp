package com.eventtickets.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.eventtickets.data.local.dao.EventDao
import com.eventtickets.data.local.dao.OrderDao
import com.eventtickets.data.local.dao.TicketDao
import com.eventtickets.data.local.entity.EventEntity
import com.eventtickets.data.local.entity.OrderEntity
import com.eventtickets.data.local.entity.TicketEntity

@Database(
    entities = [EventEntity::class, OrderEntity::class, TicketEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun orderDao(): OrderDao
    abstract fun ticketDao(): TicketDao

    companion object {
        const val DATABASE_NAME = "event_tickets.db"
    }
}
