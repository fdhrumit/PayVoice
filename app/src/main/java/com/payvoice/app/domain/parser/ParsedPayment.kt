package com.payvoice.app.domain.parser

import com.payvoice.app.utils.PaymentSourceType

data class ParsedPayment(
    val amount: Double,
    val senderName: String,
    val accountLast4: String,
    val source: String,
    val sourceType: PaymentSourceType,
    val rawText: String,
    val isCredit: Boolean
)
