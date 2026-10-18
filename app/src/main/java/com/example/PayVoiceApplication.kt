package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.PaymentRepository
import com.example.domain.tts.SoundboxTtsManager

class PayVoiceApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var paymentRepository: PaymentRepository
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var ttsManager: SoundboxTtsManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        paymentRepository = PaymentRepository(
            paymentDao = database.paymentDao(),
            bankAccountDao = database.bankAccountDao()
        )
        settingsDataStore = SettingsDataStore(this)
        ttsManager = SoundboxTtsManager(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SOUNDBOX,
                "PayVoice Soundbox Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent soundbox status for incoming payments"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Payment Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads up alerts when payment is received and announced"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            notificationManager?.createNotificationChannel(alertsChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_SOUNDBOX = "payvoice_soundbox_channel"
        const val CHANNEL_ID_ALERTS = "payvoice_alerts_channel"

        lateinit var instance: PayVoiceApplication
            private set

        fun get(): PayVoiceApplication = instance
    }
}
