package com.payvoice.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import com.payvoice.app.PayVoiceApplication
import com.payvoice.app.data.local.entity.PaymentEntity
import com.payvoice.app.domain.parser.PaymentParser
import com.payvoice.app.service.SoundboxService
import com.payvoice.app.utils.Deduplicator
import com.payvoice.app.utils.PaymentSourceType
import com.payvoice.app.widget.PayVoiceWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val tag = "PayVoiceSmsReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PayVoice:SmsProcessingWakeLock"
        )
        wakeLock?.acquire(10000L) // 10 seconds timeout

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                processSmsIntent(context, intent)
            } catch (e: Exception) {
                Log.e(tag, "Error processing incoming SMS", e)
            } finally {
                if (wakeLock?.isHeld == true) {
                    try {
                        wakeLock.release()
                    } catch (e: Exception) {
                        Log.e(tag, "Error releasing wakeLock", e)
                    }
                }
                pendingResult.finish()
            }
        }
    }

    private suspend fun processSmsIntent(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val fullBody = StringBuilder()
        for (sms in messages) {
            fullBody.append(sms.messageBody ?: "")
        }
        val messageText = fullBody.toString()

        Log.d(tag, "Received SMS from $sender: $messageText")

        val parsed = PaymentParser.parseBankSms(sender, messageText) ?: return
        if (!parsed.isCredit) return

        val app = PayVoiceApplication.get()
        val db = app.database
        val paymentDao = db.paymentDao()
        val bankAccountDao = db.bankAccountDao()

        // 1. Account matching check (if user configured accounts and last4 was found)
        val enabledAccounts = bankAccountDao.getEnabledAccountsSync()
        if (enabledAccounts.isNotEmpty() && parsed.accountLast4.isNotEmpty()) {
            val matches = enabledAccounts.any { it.last4Digits == parsed.accountLast4 }
            if (!matches) {
                Log.d(tag, "Account ${parsed.accountLast4} does not match any enabled tracked accounts")
                return
            }
        }

        // 2. Cross-channel de-duplication check:
        // Checks if a matching UPI notification arrived for this exact amount & account within 5 mins.
        // Two separate SMS messages with the same amount are NOT treated as duplicates.
        if (Deduplicator.isDuplicate(parsed.amount, parsed.accountLast4, PaymentSourceType.SMS, paymentDao)) {
            Log.d(tag, "Duplicate cross-channel payment detected for ₹${parsed.amount}, skipping")
            return
        }

        // Record in deduplication cache
        Deduplicator.recordPayment(parsed.amount, parsed.accountLast4, PaymentSourceType.SMS)

        val settings = app.settingsDataStore.getSettingsSnapshot()

        // 3. Save to Room DB
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

        // 4. Update Widget
        PayVoiceWidgetProvider.sendUpdateBroadcast(context)

        // 5. Trigger Soundbox Service announcement
        if (settings.isSoundboxActive) {
            SoundboxService.announce(
                context = context,
                amount = parsed.amount,
                sender = parsed.senderName
            )
        }
    }
}
