package com.example.pantrypal.data.notification

interface NotificationScheduler {
    fun scheduleDailyExpiryCheck()
    fun cancelDailyExpiryCheck()

    companion object {
        const val UNIQUE_WORK_NAME = "expiry_notifications_daily_work"
        const val WORK_TAG = "expiry_notifications"
    }
}
