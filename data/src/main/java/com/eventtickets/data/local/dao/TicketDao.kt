package com.eventtickets.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eventtickets.data.local.entity.TicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickets: List<TicketEntity>)

    @Query("SELECT * FROM tickets WHERE orderId = :orderId ORDER BY sequence ASC")
    fun observeByOrderId(orderId: String): Flow<List<TicketEntity>>

    @Query("SELECT COUNT(*) FROM tickets WHERE orderId = :orderId")
    suspend fun countForOrder(orderId: String): Int
}
