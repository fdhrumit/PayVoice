package com.example.data.preferences

data class AppSettings(
    val isSoundboxActive: Boolean = true,
    val language: String = "en", // "en", "hi", "gu"
    val speechVolume: Float = 1.0f, // 0.1 to 1.0
    val repeatAnnouncementTwice: Boolean = false,
    val minAmountToAnnounce: Double = 0.0,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartMinutes: Int = 22 * 60, // 22:00 (10 PM)
    val quietHoursEndMinutes: Int = 7 * 60,   // 07:00 (7 AM)
    val announceSenderName: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val isOnboardingCompleted: Boolean = false
) {
    fun isQuietHourNow(currentTimeMinutes: Int): Boolean {
        if (!quietHoursEnabled) return false
        return if (quietHoursStartMinutes <= quietHoursEndMinutes) {
            currentTimeMinutes in quietHoursStartMinutes..quietHoursEndMinutes
        } else {
            // Wraps past midnight (e.g., 22:00 to 07:00)
            currentTimeMinutes >= quietHoursStartMinutes || currentTimeMinutes < quietHoursEndMinutes
        }
    }
}
