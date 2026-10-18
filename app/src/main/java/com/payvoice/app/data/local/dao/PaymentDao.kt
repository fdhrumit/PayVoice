package com.payvoice.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.payvoice.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE timestamp >= :startOfDayMillis ORDER BY timestamp DESC")
    fun getTodayPaymentsFlow(startOfDayMillis: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getPaymentsBetweenFlow(startTime: Long, endTime: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE timestamp >= :startOfDayMillis ORDER BY timestamp DESC")
    fun getTodayPaymentsSync(startOfDayMillis: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments ORDER BY timestamp DESC LIMIT 1")
    fun getLatestPaymentFlow(): Flow<PaymentEntity?>

    @Query("SELECT * FROM payments ORDER BY timestamp DESC LIMIT 1")
    fun getLatestPaymentSync(): PaymentEntity?

    @Query("SELECT * FROM payments WHERE ABS(amount - :amount) < 0.01 AND timestamp >= :minTimestamp AND source NOT LIKE :excludeSourcePrefix || '%' ORDER BY timestamp DESC LIMIT 1")
    fun findCrossSourceDuplicate(amount: Double, minTimestamp: Long, excludeSourcePrefix: String): PaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM payments")
    suspend fun clearAllPayments()
}
