package com.example.pantrypal.data.notification

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSchedulerImpl @Inject constructor(
    private val workManager: WorkManager
) : NotificationScheduler {
    override fun scheduleDailyExpiryCheck() {
        val request = PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(
            Duration.ofDays(1)
        )
            .addTag(NotificationScheduler.WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            NotificationScheduler.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancelDailyExpiryCheck() {
        workManager.cancelUniqueWork(NotificationScheduler.UNIQUE_WORK_NAME)
    }
}
