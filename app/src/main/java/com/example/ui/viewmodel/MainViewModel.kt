package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PayVoiceApplication
import com.example.data.local.entity.BankAccountEntity
import com.example.data.local.entity.PaymentEntity
import com.example.data.preferences.AppSettings
import com.example.service.SoundboxService
import com.example.utils.CsvExporter
import com.example.widget.PayVoiceWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HistoryFilter {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL
}

data class DailyTotal(
    val dayLabel: String,
    val totalAmount: Double,
    val count: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PayVoiceApplication
    private val paymentRepo = app.paymentRepository
    private val settingsStore = app.settingsDataStore
    private val ttsManager = app.ttsManager

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val todayPayments: StateFlow<List<PaymentEntity>> = paymentRepo.getTodayPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = paymentRepo.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bankAccounts: StateFlow<List<BankAccountEntity>> = paymentRepo.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFilter = MutableStateFlow(HistoryFilter.TODAY)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()

    // Filtered payments for History tab
    val filteredPayments: StateFlow<List<PaymentEntity>> = combine(allPayments, _selectedFilter) { list, filter ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        when (filter) {
            HistoryFilter.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                list.filter { it.timestamp >= start }
            }
            HistoryFilter.YESTERDAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val todayStart = cal.timeInMillis
                val yesterdayStart = todayStart - (24 * 60 * 60 * 1000L)
                list.filter { it.timestamp in yesterdayStart until todayStart }
            }
            HistoryFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfWeek = cal.timeInMillis
                list.filter { it.timestamp >= startOfWeek }
            }
            HistoryFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfMonth = cal.timeInMillis
                list.filter { it.timestamp >= startOfMonth }
            }
            HistoryFilter.ALL -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7-day daily totals for the bar chart
    val last7DaysTotals: StateFlow<List<DailyTotal>> = allPayments.mapTo7DaysTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
    }

    fun toggleSoundbox(active: Boolean) {
        viewModelScope.launch {
            settingsStore.setSoundboxActive(active)
            if (active) {
                SoundboxService.start(app)
            } else {
                SoundboxService.stop(app)
            }
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsStore.setLanguage(lang)
        }
    }

    fun setSpeechVolume(volume: Float) {
        viewModelScope.launch {
            settingsStore.setSpeechVolume(volume)
        }
    }

    fun setRepeatTwice(repeat: Boolean) {
        viewModelScope.launch {
            settingsStore.setRepeatTwice(repeat)
        }
    }

    fun setMinAmount(amount: Double) {
        viewModelScope.launch {
            settingsStore.setMinAmount(amount)
        }
    }

    fun setQuietHours(enabled: Boolean, startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch {
            settingsStore.setQuietHours(enabled, startMinutes, endMinutes)
        }
    }

    fun setAnnounceSender(announce: Boolean) {
        viewModelScope.launch {
            settingsStore.setAnnounceSenderName(announce)
        }
    }

    fun addBankAccount(bankName: String, last4: String) {
        viewModelScope.launch {
            paymentRepo.insertAccount(bankName, last4)
        }
    }

    fun toggleBankAccount(account: BankAccountEntity) {
        viewModelScope.launch {
            paymentRepo.updateAccount(account.copy(isEnabled = !account.isEnabled))
        }
    }

    fun deleteBankAccount(id: Long) {
        viewModelScope.launch {
            paymentRepo.deleteAccount(id)
        }
    }

    fun setAppLock(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            settingsStore.setAppLock(enabled, pin)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsStore.setOnboardingCompleted(true)
            SoundboxService.start(app)
        }
    }

    fun testVoice() {
        val currentSettings = settings.value
        ttsManager.announcePayment(
            amount = 500.0,
            senderName = "PayVoice Test",
            settings = currentSettings
        )
    }

    fun simulatePayment(amount: Double, sender: String, source: String = "UPI:PhonePe") {
        viewModelScope.launch {
            val entity = PaymentEntity(
                amount = amount,
                sender = sender,
                accountLast4 = "4921",
                timestamp = System.currentTimeMillis(),
                source = source,
                rawMessage = "Simulated: Received ₹$amount from $sender",
                isSpoken = settings.value.isSoundboxActive,
                speechLanguage = settings.value.language
            )
            paymentRepo.insertPayment(entity)
            PayVoiceWidgetProvider.sendUpdateBroadcast(app)
            if (settings.value.isSoundboxActive) {
                ttsManager.announcePayment(
                    amount = amount,
                    senderName = sender,
                    settings = settings.value
                )
            }
        }
    }

    fun exportFilteredCsv(context: Context) {
        val list = filteredPayments.value
        val intent = CsvExporter.exportPaymentsToCsv(context, list)
        if (intent != null) {
            context.startActivity(android.content.Intent.createChooser(intent, "Share Payment Report"))
        }
    }
}

private fun kotlinx.coroutines.flow.Flow<List<PaymentEntity>>.mapTo7DaysTotals(): kotlinx.coroutines.flow.Flow<List<DailyTotal>> {
    return this.map { payments ->
        val result = mutableListOf<DailyTotal>()
        val dayFormat = SimpleDateFormat("EEE", Locale.US)

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startDay = dayCal.timeInMillis
            val endDay = startDay + (24 * 60 * 60 * 1000L)
            val dayPayments = payments.filter { it.timestamp in startDay until endDay }
            val sum = dayPayments.sumOf { it.amount }
            val label = if (i == 0) "Today" else dayFormat.format(dayCal.time)
            result.add(DailyTotal(dayLabel = label, totalAmount = sum, count = dayPayments.size))
        }
        result
    }
}
