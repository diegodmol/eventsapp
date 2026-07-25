package com.eventtickets.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eventtickets.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(order: OrderEntity)

    @Update
    suspend fun update(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    fun observeById(orderId: String): Flow<OrderEntity?>

    /**
     * Verifica duplicidade: existe pedido com essa idempotencyKey cujo status
     * já é PROCESSING, APPROVED, DENIED ou CANCELED (ou seja, o pagamento já
     * foi iniciado ou concluído por qualquer via).
     */
    @Query(
        """
        SELECT COUNT(*) FROM orders 
        WHERE idempotencyKey = :idempotencyKey 
        AND status IN ('PROCESSING', 'APPROVED', 'DENIED', 'CANCELED')
        """
    )
    suspend fun countActiveOrCompleted(idempotencyKey: String): Int

    @Query("SELECT * FROM orders WHERE eventId = :eventId AND status = 'CREATED' ORDER BY createdAtEpochMillis DESC LIMIT 1")
    suspend fun findPendingOrderForEvent(eventId: String): OrderEntity?
}
