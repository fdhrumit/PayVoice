package com.payvoice.app.utils

import com.payvoice.app.data.local.dao.PaymentDao
import kotlin.math.abs

enum class PaymentSourceType {
    SMS,
    UPI
}

data class DeduplicationRecord(
    val amount: Double,
    val accountLast4: String,
    val sourceType: PaymentSourceType,
    val timestamp: Long
)

object Deduplicator {
    private const val DEDUPLICATION_WINDOW_MS = 5 * 60 * 1000L // 5 minutes

    // In-memory list of recent payments
    private val recentRecords = mutableListOf<DeduplicationRecord>()

    /**
     * Checks if this payment is a cross-channel duplicate:
     * ONLY if one message is from SMS and the other is from a UPI notification,
     * with the same amount and matching account within 5 minutes.
     * Two SMS or two UPI notifications with the same amount are treated as separate payments
     * and are NOT duplicates (both are announced).
     */
    @Synchronized
    fun isDuplicate(
        amount: Double,
        accountLast4: String,
        sourceType: PaymentSourceType,
        paymentDao: PaymentDao? = null
    ): Boolean {
        val now = System.currentTimeMillis()
        val minTime = now - DEDUPLICATION_WINDOW_MS

        // Clean stale memory entries
        recentRecords.removeAll { it.timestamp < minTime }

        // Check against in-memory records:
        // Must be DIFFERENT source types (one SMS, one UPI)
        for (record in recentRecords) {
            if (record.sourceType != sourceType && abs(record.amount - amount) < 0.01 && record.timestamp >= minTime) {
                // If accounts are specified, check if they match (or if either is blank)
                if (record.accountLast4.isEmpty() || accountLast4.isEmpty() || record.accountLast4 == accountLast4) {
                    return true
                }
            }
        }

        // Check against Room database if available
        if (paymentDao != null) {
            val excludePrefix = if (sourceType == PaymentSourceType.SMS) "SMS" else "UPI"
            val dbMatch = paymentDao.findCrossSourceDuplicate(
                amount = amount,
                minTimestamp = minTime,
                excludeSourcePrefix = excludePrefix
            )
            if (dbMatch != null) {
                if (dbMatch.accountLast4.isEmpty() || accountLast4.isEmpty() || dbMatch.accountLast4 == accountLast4) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Records a newly announced payment into the cache with its source type.
     */
    @Synchronized
    fun recordPayment(amount: Double, accountLast4: String, sourceType: PaymentSourceType) {
        val now = System.currentTimeMillis()
        recentRecords.add(DeduplicationRecord(amount, accountLast4, sourceType, now))
    }

    @Synchronized
    fun clearCache() {
        recentRecords.clear()
    }
}
