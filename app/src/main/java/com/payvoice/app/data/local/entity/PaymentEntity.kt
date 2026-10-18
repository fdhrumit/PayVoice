package com.payvoice.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val sender: String,
    val accountLast4: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String, // "SMS" or "UPI:PhonePe Business", "UPI:Google Pay", etc.
    val rawMessage: String,
    val isSpoken: Boolean = false,
    val speechLanguage: String = "en"
)
