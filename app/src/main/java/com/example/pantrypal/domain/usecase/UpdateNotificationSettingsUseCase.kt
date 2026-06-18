package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.notification.NotificationScheduler
import com.example.pantrypal.data.settings.SettingsRepository
import javax.inject.Inject

class UpdateNotificationSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationScheduler: NotificationScheduler
) {
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        settingsRepository.setNotificationsEnabled(enabled)
        if (enabled) {
            notificationScheduler.scheduleDailyExpiryCheck()
        } else {
            notificationScheduler.cancelDailyExpiryCheck()
        }
    }

    suspend fun setExpiryThresholdDays(days: Int) {
        val threshold = days.takeIf { it in AllowedThresholds } ?: DefaultThreshold
        settingsRepository.updateFreshNotificationDays(threshold)
        settingsRepository.updateLongLifeNotificationDays(threshold)
        if (settingsRepository.getSettings().expirationNotificationsEnabled) {
            notificationScheduler.scheduleDailyExpiryCheck()
        }
    }

    private companion object {
        val AllowedThresholds = setOf(1, 3, 7)
        const val DefaultThreshold = 3
    }
}
