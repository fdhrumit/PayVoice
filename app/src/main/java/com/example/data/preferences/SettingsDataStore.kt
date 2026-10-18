package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "payvoice_settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val SOUNDBOX_ACTIVE = booleanPreferencesKey("soundbox_active")
        val LANGUAGE = stringPreferencesKey("language")
        val SPEECH_VOLUME = floatPreferencesKey("speech_volume")
        val REPEAT_TWICE = booleanPreferencesKey("repeat_twice")
        val MIN_AMOUNT = doublePreferencesKey("min_amount")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        val ANNOUNCE_SENDER = booleanPreferencesKey("announce_sender")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            isSoundboxActive = preferences[PreferencesKeys.SOUNDBOX_ACTIVE] ?: true,
            language = preferences[PreferencesKeys.LANGUAGE] ?: "en",
            speechVolume = preferences[PreferencesKeys.SPEECH_VOLUME] ?: 1.0f,
            repeatAnnouncementTwice = preferences[PreferencesKeys.REPEAT_TWICE] ?: false,
            minAmountToAnnounce = preferences[PreferencesKeys.MIN_AMOUNT] ?: 0.0,
            quietHoursEnabled = preferences[PreferencesKeys.QUIET_HOURS_ENABLED] ?: false,
            quietHoursStartMinutes = preferences[PreferencesKeys.QUIET_HOURS_START] ?: (22 * 60),
            quietHoursEndMinutes = preferences[PreferencesKeys.QUIET_HOURS_END] ?: (7 * 60),
            announceSenderName = preferences[PreferencesKeys.ANNOUNCE_SENDER] ?: true,
            appLockEnabled = preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false,
            appLockPin = preferences[PreferencesKeys.APP_LOCK_PIN] ?: "",
            isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun getSettingsSnapshot(): AppSettings {
        return settingsFlow.first()
    }

    suspend fun setSoundboxActive(active: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SOUNDBOX_ACTIVE] = active }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[PreferencesKeys.LANGUAGE] = lang }
    }

    suspend fun setSpeechVolume(volume: Float) {
        context.dataStore.edit { it[PreferencesKeys.SPEECH_VOLUME] = volume.coerceIn(0.1f, 1.0f) }
    }

    suspend fun setRepeatTwice(repeat: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.REPEAT_TWICE] = repeat }
    }

    suspend fun setMinAmount(minAmount: Double) {
        context.dataStore.edit { it[PreferencesKeys.MIN_AMOUNT] = minAmount.coerceAtLeast(0.0) }
    }

    suspend fun setQuietHours(enabled: Boolean, startMinutes: Int, endMinutes: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.QUIET_HOURS_ENABLED] = enabled
            it[PreferencesKeys.QUIET_HOURS_START] = startMinutes
            it[PreferencesKeys.QUIET_HOURS_END] = endMinutes
        }
    }

    suspend fun setAnnounceSenderName(announce: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ANNOUNCE_SENDER] = announce }
    }

    suspend fun setAppLock(enabled: Boolean, pin: String) {
        context.dataStore.edit {
            it[PreferencesKeys.APP_LOCK_ENABLED] = enabled
            it[PreferencesKeys.APP_LOCK_PIN] = pin
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETED] = completed }
    }
}
