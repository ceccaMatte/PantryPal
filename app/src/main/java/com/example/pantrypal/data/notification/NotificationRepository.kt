package com.example.pantrypal.data.notification

import com.example.pantrypal.domain.model.ExpirationNotificationContent

interface NotificationRepository {
    suspend fun areNotificationsAllowed(): Boolean
    fun createNotificationChannel()
    fun scheduleDailyExpirationWorker()
    fun cancelDailyExpirationWorker()
    fun showExpirationSummaryNotification(input: ExpirationNotificationContent)
}
