package com.payvoice.app.domain.parser

import com.payvoice.app.utils.PaymentSourceType
import java.util.regex.Pattern

object PaymentParser {

    // Known bank sender codes (case-insensitive substring match)
    private val KNOWN_BANK_KEYWORDS = listOf(
        "SBI", "HDFC", "ICICI", "AXIS", "KOTAK", "PNB", "BOB", "CANBNK", "CANARA",
        "UBI", "UNIONB", "YESBNK", "YESB", "INDUS", "IDBI", "FEDBNK", "FEDERAL",
        "IOB", "CENTBK", "CITI", "SCB", "STANDARD", "PAYTM", "AIRTEL", "RBL",
        "BANDHAN", "AUBANK", "AUBL", "IDFC", "IOBCHN", "BOM", "MAHABK", "SYNDBK",
        "CORPBK", "ALLAHB", "ANDHRAB", "VIJAYAB", "DENABK", "SVC", "SARASWAT",
        "COSMOS", "TJSB", "BHARAT", "EQUITA", "UJJIVAN", "FINCARE", "ESAF", "SURYA",
        "PAYTMB", "JIO", "AIRTELB", "IPPB"
    )

    // Known UPI app package names (both merchant/business and consumer apps)
    private val KNOWN_UPI_PACKAGES = mapOf(
        // Merchant & Business UPI apps
        "com.phonepe.app.business" to "PhonePe Business",
        "com.paytm.business" to "Paytm for Business",
        "com.bharatpe.app" to "BharatPe",
        "com.google.android.apps.nbu.paisa.merchant" to "Google Pay for Business",
        // Consumer UPI apps
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "com.dreamplug.androidapp" to "CRED",
        "in.amazon.mShop.android.shopping" to "Amazon Pay"
    )

    // Rejection keywords
    private val OTP_WORDS = listOf(
        "otp", "one time password", "verification code", "secret code",
        "do not share", "never share", "security code", "is your code",
        "valid for", "login code"
    )

    private val DEBIT_PATTERNS = listOf(
        Regex("(?i)\\bdebited\\b"),
        Regex("(?i)\\bdebited for\\b"),
        Regex("(?i)\\bwithdrawn\\b"),
        Regex("(?i)\\bspent\\b"),
        Regex("(?i)\\bpaid to\\b"),
        Regex("(?i)\\bsent to\\b"),
        Regex("(?i)\\btransfer to\\b"),
        Regex("(?i)\\bpurchase at\\b"),
        Regex("(?i)\\bdr\\b"),
        Regex("(?i)\\bdr\\.\\b"),
        Regex("(?i)\\bdeducted\\b")
    )

    private val PROMO_PATTERNS = listOf(
        Regex("(?i)\\bbill due\\b"),
        Regex("(?i)\\bminimum due\\b"),
        Regex("(?i)\\bemi due\\b"),
        Regex("(?i)\\bpre-approved\\b"),
        Regex("(?i)\\bloan offer\\b"),
        Regex("(?i)\\bapply now\\b"),
        Regex("(?i)\\bclaim now\\b"),
        Regex("(?i)\\bclick here\\b"),
        Regex("(?i)\\brecharge\\b")
    )

    // Credit patterns
    private val CREDIT_INDICATORS = listOf(
        "credited", "credit", "received", "deposited", "paid you", "cr", "cr."
    )

    /**
     * Checks if an SMS sender address matches a known Indian bank.
     * Matches senders with operator prefixes and suffixes (e.g. VM-HDFCBK, AD-SBIINB-S, JD-ICICIB-T)
     * by containing the bank code, case-insensitive.
     */
    fun isKnownBankSender(senderAddress: String): Boolean {
        if (senderAddress.isBlank()) return false
        val upperSender = senderAddress.trim().uppercase()
        return KNOWN_BANK_KEYWORDS.any { keyword ->
            upperSender.contains(keyword.uppercase())
        }
    }

    /**
     * Checks if a notification package is a supported UPI app.
     */
    fun isKnownUpiPackage(packageName: String): Boolean {
        return KNOWN_UPI_PACKAGES.containsKey(packageName)
    }

    fun getUpiAppName(packageName: String): String {
        return KNOWN_UPI_PACKAGES[packageName] ?: "UPI App"
    }

    /**
     * Main parse method for incoming bank SMS.
     */
    fun parseBankSms(sender: String, messageBody: String): ParsedPayment? {
        if (!isKnownBankSender(sender)) {
            return null
        }
        return parseMessageInternal(messageBody, source = "SMS ($sender)", sourceType = PaymentSourceType.SMS)
    }

    /**
     * Main parse method for UPI notifications.
     */
    fun parseUpiNotification(packageName: String, title: String, messageText: String): ParsedPayment? {
        if (!isKnownUpiPackage(packageName)) {
            return null
        }
        val appName = getUpiAppName(packageName)
        val combined = "$title $messageText"
        return parseMessageInternal(combined, source = "UPI:$appName", sourceType = PaymentSourceType.UPI)
    }

    /**
     * Internal parsing logic for both SMS and UPI text.
     */
    fun parseMessageInternal(text: String, source: String, sourceType: PaymentSourceType): ParsedPayment? {
        if (text.isBlank()) return null
        val lowerText = text.lowercase()

        // 1. Check OTP rejection
        for (otpWord in OTP_WORDS) {
            if (lowerText.contains(otpWord)) {
                return null
            }
        }

        // 2. Check Promo rejection
        for (pattern in PROMO_PATTERNS) {
            if (pattern.containsMatchIn(text)) {
                return null
            }
        }

        // 3. Check Debit rejection
        val hasDebit = DEBIT_PATTERNS.any { it.containsMatchIn(text) }
        val hasCredit = CREDIT_INDICATORS.any {
            Regex("(?i)\\b${Regex.escape(it)}\\b").containsMatchIn(text)
        }

        if (hasDebit && !lowerText.contains("credited")) {
            return null
        }
        if (!hasCredit) {
            return null
        }

        // 4. Extract Amount
        val amount = extractCreditAmount(text) ?: return null
        if (amount <= 0.0) return null

        // 5. Extract Account Last 4 digits (if present)
        val accountLast4 = extractAccountLast4(text)

        // 6. Extract Sender Name
        val senderName = extractSenderName(text, source)

        return ParsedPayment(
            amount = amount,
            senderName = senderName,
            accountLast4 = accountLast4,
            source = source,
            sourceType = sourceType,
            rawText = text,
            isCredit = true
        )
    }

    /**
     * Robust extraction of credited amounts in Indian currency notation.
     * Prevents confusion between 400 and 4,000, and avoids extracting "Avl Bal" instead of credited amount.
     */
    fun extractCreditAmount(text: String): Double? {
        val creditPatterns = listOf(
            Regex("(?i)(?:credited|received|deposited|paid you)\\s*(?:with|by|of)?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)"),
            Regex("(?i)(?:rs\\.?|inr|₹)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)\\s*(?:has been|is)?\\s*(?:credited|received|deposited)"),
            Regex("(?i)(?:paid you|sent you)\\s*(?:rs\\.?|inr|₹)?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)"),
            Regex("(?i)received\\s*(?:rs\\.?|inr|₹)?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)"),
            Regex("(?i)([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)\\s*(?:credited|deposited)")
        )

        for (pattern in creditPatterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val cleaned = match.groupValues[1].replace(",", "").trim()
                val parsed = cleaned.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }

        // Fallback: look for currency symbol before any "balance/bal" section
        val textBeforeBalance = text.split(Regex("(?i)\\b(?:avl\\s*bal|available\\s*bal|bal\\b|balance\\b)")).first()
        val currencyPattern = Regex("(?i)(?:rs\\.?|inr|₹)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)")
        val match = currencyPattern.find(textBeforeBalance)
        if (match != null && match.groupValues.size > 1) {
            val cleaned = match.groupValues[1].replace(",", "").trim()
            val parsed = cleaned.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) {
                return parsed
            }
        }

        return null
    }

    /**
     * Extracts last 4 digits of bank account.
     */
    fun extractAccountLast4(text: String): String {
        val patterns = listOf(
            Regex("(?i)(?:a/c|acct|account|acc)\\s*(?:no\\.?|ending with|ending)?\\s*[:.*X\\-]*([0-9]{4})\\b"),
            Regex("(?i)(?:card)\\s*(?:ending with|ending)?\\s*[:.*X\\-]*([0-9]{4})\\b"),
            Regex("(?i)ending\\s+(?:in|with)?\\s*([0-9]{4})\\b")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return ""
    }

    /**
     * Extracts payer / sender name from payment text.
     */
    fun extractSenderName(text: String, defaultSource: String): String {
        val fromPattern = Regex("(?i)(?:from|by transfer from|transfer from|vpa|sender)\\s+([A-Za-z\\s]{2,30}?)(?:\\s+(?:on|via|ref|upi|avl|bal|\\.|,))")
        val match = fromPattern.find(text)
        if (match != null && match.groupValues.size > 1) {
            val name = match.groupValues[1].trim()
            if (name.length in 2..28 && !name.equals("bank", ignoreCase = true)) {
                return name
            }
        }

        val paidYouPattern = Regex("(?i)^([A-Za-z\\s]{2,25}?)\\s+(?:paid you|sent you)")
        val matchPaidYou = paidYouPattern.find(text.trim())
        if (matchPaidYou != null && matchPaidYou.groupValues.size > 1) {
            val name = matchPaidYou.groupValues[1].trim()
            if (name.isNotEmpty()) return name
        }

        return if (defaultSource.startsWith("UPI:")) {
            defaultSource.substringAfter("UPI:")
        } else if (defaultSource.startsWith("SMS (")) {
            defaultSource.removePrefix("SMS (").removeSuffix(")")
        } else {
            "Customer"
        }
    }
}
