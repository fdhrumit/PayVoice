package com.payvoice.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.payvoice.app.PayVoiceApplication
import com.payvoice.app.data.local.entity.PaymentEntity
import com.payvoice.app.domain.parser.PaymentParser
import com.payvoice.app.utils.Deduplicator
import com.payvoice.app.utils.PaymentSourceType
import com.payvoice.app.widget.PayVoiceWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    private val tag = "PayVoiceNotifListener"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (!PaymentParser.isKnownUpiPackage(packageName)) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val fullContent = if (bigText.isNotEmpty()) "$title $bigText" else "$title $text"

        Log.d(tag, "Received notification from $packageName: $fullContent")

        val parsed = PaymentParser.parseUpiNotification(packageName, title, fullContent)
        if (parsed != null && parsed.isCredit) {
            handleParsedPayment(parsed)
        }
    }

    private fun handleParsedPayment(parsed: com.payvoice.app.domain.parser.ParsedPayment) {
        scope.launch {
            val app = PayVoiceApplication.get()
            val db = app.database
            val paymentDao = db.paymentDao()
            val bankAccountDao = db.bankAccountDao()

            // 1. Account matching check (if user configured accounts and last4 was found)
            val enabledAccounts = bankAccountDao.getEnabledAccountsSync()
            if (enabledAccounts.isNotEmpty() && parsed.accountLast4.isNotEmpty()) {
                val matches = enabledAccounts.any { it.last4Digits == parsed.accountLast4 }
                if (!matches) {
                    Log.d(tag, "Account last4 ${parsed.accountLast4} does not match any enabled user account")
                    return@launch
                }
            }

            // 2. Cross-channel de-duplication check:
            // Checks if a matching SMS arrived for this exact amount & account within 5 mins.
            // Two separate UPI notifications are NOT treated as duplicates.
            if (Deduplicator.isDuplicate(parsed.amount, parsed.accountLast4, PaymentSourceType.UPI, paymentDao)) {
                Log.d(tag, "Duplicate cross-channel payment detected for ₹${parsed.amount}, skipping")
                return@launch
            }

            // Record in deduplication cache
            Deduplicator.recordPayment(parsed.amount, parsed.accountLast4, PaymentSourceType.UPI)

            val settings = app.settingsDataStore.getSettingsSnapshot()

            // 3. Save to Room database
            val entity = PaymentEntity(
                amount = parsed.amount,
                sender = parsed.senderName,
                accountLast4 = parsed.accountLast4,
                timestamp = System.currentTimeMillis(),
                source = parsed.source,
                rawMessage = parsed.rawText,
                isSpoken = settings.isSoundboxActive,
                speechLanguage = settings.language
            )
            paymentDao.insertPayment(entity)

            // 4. Update widget
            PayVoiceWidgetProvider.sendUpdateBroadcast(this@NotificationListener)

            // 5. Trigger Soundbox Service announcement
            if (settings.isSoundboxActive) {
                SoundboxService.announce(
                    context = this@NotificationListener,
                    amount = parsed.amount,
                    sender = parsed.senderName
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
