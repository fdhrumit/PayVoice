package com.example.domain.parser

data class ParsedPayment(
    val amount: Double,
    val senderName: String,
    val accountLast4: String,
    val source: String,
    val rawText: String,
    val isCredit: Boolean
)
