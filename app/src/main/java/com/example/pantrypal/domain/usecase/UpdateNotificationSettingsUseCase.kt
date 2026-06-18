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

    suspend fun setFreshNotificationDays(days: Int) {
        settingsRepository.updateFreshNotificationDays(days)
        rescheduleIfEnabled()
    }

    suspend fun setLongLifeNotificationDays(days: Int) {
        settingsRepository.updateLongLifeNotificationDays(days)
        rescheduleIfEnabled()
    }

    private suspend fun rescheduleIfEnabled() {
        if (settingsRepository.getSettings().expirationNotificationsEnabled) {
            notificationScheduler.scheduleDailyExpiryCheck()
        }
    }
}
