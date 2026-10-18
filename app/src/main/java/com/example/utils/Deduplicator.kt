package com.example.utils

import com.example.data.local.dao.PaymentDao
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object Deduplicator {
    private const val DEDUPLICATION_WINDOW_MS = 5 * 60 * 1000L // 5 minutes

    // In-memory cache for ultra-fast check: amount to timestamp
    private val recentPayments = ConcurrentHashMap<Double, Long>()

    /**
     * Checks if this payment is a duplicate of a recent payment within the 5-minute window.
     * Checks both memory cache and Room DB.
     */
    fun isDuplicate(amount: Double, paymentDao: PaymentDao? = null): Boolean {
        val now = System.currentTimeMillis()
        val minTime = now - DEDUPLICATION_WINDOW_MS

        // Clean stale memory entries
        recentPayments.entries.removeIf { it.value < minTime }

        // Check in-memory
        for ((cachedAmount, timestamp) in recentPayments) {
            if (abs(cachedAmount - amount) < 0.01 && timestamp >= minTime) {
                return true
            }
        }

        // Check Room DB if available
        if (paymentDao != null) {
            val dbMatch = paymentDao.findRecentDuplicate(amount, minTime)
            if (dbMatch != null) {
                return true
            }
        }

        return false
    }

    /**
     * Records a newly announced payment into the cache.
     */
    fun recordPayment(amount: Double) {
        val now = System.currentTimeMillis()
        recentPayments[amount] = now
    }

    fun clearCache() {
        recentPayments.clear()
    }
}
