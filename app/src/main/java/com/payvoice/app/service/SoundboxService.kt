package com.payvoice.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.payvoice.app.MainActivity
import com.payvoice.app.PayVoiceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SoundboxService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_TEST_VOICE -> {
                testVoice()
            }
            ACTION_ANNOUNCE -> {
                val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: "Customer"
                announcePayment(amount, sender)
            }
        }

        startInForeground()
        return START_STICKY
    }

    private fun startInForeground() {
        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val testVoiceIntent = Intent(this, SoundboxService::class.java).apply {
            action = ACTION_TEST_VOICE
        }
        val testVoicePendingIntent = PendingIntent.getService(
            this,
            1,
            testVoiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SoundboxService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PayVoiceApplication.CHANNEL_ID_SOUNDBOX)
            .setContentTitle("PayVoice Soundbox active")
            .setContentText("Listening for incoming Bank SMS & UPI payments")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Test Voice", testVoicePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun testVoice() {
        serviceScope.launch {
            val app = PayVoiceApplication.get()
            val settings = app.settingsDataStore.getSettingsSnapshot()
            app.ttsManager.announcePayment(
                amount = 500.0,
                senderName = "Test Shop",
                settings = settings
            )
        }
    }

    private fun announcePayment(amount: Double, sender: String) {
        serviceScope.launch {
            val app = PayVoiceApplication.get()
            val settings = app.settingsDataStore.getSettingsSnapshot()
            app.ttsManager.announcePayment(
                amount = amount,
                senderName = sender,
                settings = settings
            )
        }
    }

    private fun stopForegroundService() {
        serviceScope.launch {
            PayVoiceApplication.get().settingsDataStore.setSoundboxActive(false)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.payvoice.app.service.ACTION_START"
        const val ACTION_STOP = "com.payvoice.app.service.ACTION_STOP"
        const val ACTION_ANNOUNCE = "com.payvoice.app.service.ACTION_ANNOUNCE"
        const val ACTION_TEST_VOICE = "com.payvoice.app.service.ACTION_TEST_VOICE"

        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_SENDER = "extra_sender"

        fun start(context: Context) {
            val intent = Intent(context, SoundboxService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SoundboxService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun announce(context: Context, amount: Double, sender: String) {
            val intent = Intent(context, SoundboxService::class.java).apply {
                action = ACTION_ANNOUNCE
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_SENDER, sender)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
