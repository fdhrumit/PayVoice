package com.example.domain.tts

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import com.example.data.preferences.AppSettings
import com.example.utils.NumberToWordsConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class SoundboxTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val tag = "SoundboxTtsManager"
    private var tts: TextToSpeech? = null
    private val isInitialized = AtomicBoolean(false)
    private val pendingQueue = ConcurrentLinkedQueue<SpeechRequest>()
    private var wakeLock: PowerManager.WakeLock? = null

    private var activeUtterances = 0

    data class SpeechRequest(
        val text: String,
        val language: String,
        val volume: Float,
        val repeatTwice: Boolean,
        val utteranceId: String = "payvoice_${System.currentTimeMillis()}_${(100..999).random()}"
    )

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized.set(true)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    acquireWakeLock()
                }

                override fun onDone(utteranceId: String?) {
                    releaseWakeLockIfIdle()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    releaseWakeLockIfIdle()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    releaseWakeLockIfIdle()
                }
            })
            // Process any queued items
            processQueue()
        } else {
            Log.e(tag, "TTS initialization failed with status $status")
        }
    }

    private fun acquireWakeLock() {
        synchronized(this) {
            activeUtterances++
            if (wakeLock == null) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "PayVoice:SpeechWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                // Keep wake lock for max 30 seconds as safety timeout
                wakeLock?.acquire(30000L)
            }
        }
    }

    private fun releaseWakeLockIfIdle() {
        synchronized(this) {
            activeUtterances = (activeUtterances - 1).coerceAtLeast(0)
            if (activeUtterances == 0 && wakeLock?.isHeld == true) {
                try {
                    wakeLock?.release()
                } catch (e: Exception) {
                    Log.e(tag, "Error releasing wakeLock", e)
                }
            }
        }
    }

    /**
     * Checks settings and announces payment out loud.
     * Returns true if speech was scheduled, false if skipped (due to quiet hours, min amount, etc.)
     */
    fun announcePayment(
        amount: Double,
        senderName: String,
        settings: AppSettings
    ): Boolean {
        if (!settings.isSoundboxActive) {
            Log.d(tag, "Soundbox is inactive, skipping speech")
            return false
        }

        // Check minimum amount
        if (amount < settings.minAmountToAnnounce) {
            Log.d(tag, "Amount $amount is less than min threshold ${settings.minAmountToAnnounce}")
            return false
        }

        // Check quiet hours
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (settings.isQuietHourNow(currentMinutes)) {
            Log.d(tag, "Current time is within quiet hours, skipping speech")
            return false
        }

        // Convert amount to natural spoken words in chosen language
        val speechText = NumberToWordsConverter.convertAmountToSpeech(
            amount = amount,
            language = settings.language,
            senderName = senderName,
            announceSender = settings.announceSenderName
        )

        speak(
            text = speechText,
            language = settings.language,
            volume = settings.speechVolume,
            repeatTwice = settings.repeatAnnouncementTwice
        )
        return true
    }

    /**
     * Speaks the given text using TextToSpeech.
     */
    fun speak(
        text: String,
        language: String = "en",
        volume: Float = 1.0f,
        repeatTwice: Boolean = false
    ) {
        val request = SpeechRequest(
            text = text,
            language = language,
            volume = volume,
            repeatTwice = repeatTwice
        )

        if (!isInitialized.get() || tts == null) {
            pendingQueue.add(request)
            initTts()
            return
        }

        enqueueSpeech(request)
    }

    private fun processQueue() {
        while (pendingQueue.isNotEmpty()) {
            val req = pendingQueue.poll() ?: break
            enqueueSpeech(req)
        }
    }

    private fun enqueueSpeech(request: SpeechRequest) {
        val targetLocale = when (request.language.lowercase()) {
            "hi" -> Locale("hi", "IN")
            "gu" -> Locale("gu", "IN")
            else -> Locale.ENGLISH
        }

        val checkLang = tts?.isLanguageAvailable(targetLocale)
        val selectedLocale = if (checkLang == TextToSpeech.LANG_AVAILABLE ||
            checkLang == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            checkLang == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        ) {
            targetLocale
        } else {
            // Fall back to English if voice is missing and inform user
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "${targetLocale.displayLanguage} voice missing. Please install voice data in Google TTS settings. Speaking in English.",
                    Toast.LENGTH_LONG
                ).show()
            }
            Locale.ENGLISH
        }

        tts?.language = selectedLocale

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, request.volume.coerceIn(0.1f, 1.0f))
        }

        // Utterance 1
        tts?.speak(request.text, TextToSpeech.QUEUE_ADD, params, request.utteranceId)

        // Repeat if enabled
        if (request.repeatTwice) {
            tts?.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, null)
            tts?.speak(request.text, TextToSpeech.QUEUE_ADD, params, "${request.utteranceId}_repeat")
        }
    }

    fun stop() {
        try {
            tts?.stop()
            releaseWakeLockIfIdle()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized.set(false)
            releaseWakeLockIfIdle()
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
    }
}
