package com.example.pantrypal.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.pantrypal.R
import com.example.pantrypal.domain.model.ExpirationNotificationContent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) : NotificationRepository {
    override suspend fun areNotificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    override fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Scadenze dispensa",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Promemoria per alimenti in scadenza"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun scheduleDailyExpirationWorker() {
        val request = PeriodicWorkRequestBuilder<ExpirationPlaceholderWorker>(
            Duration.ofDays(1)
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancelDailyExpirationWorker() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    override fun showExpirationSummaryNotification(input: ExpirationNotificationContent) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(input.title)
            .setContentText(input.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(1001, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "expiration_summary"
        private const val WORK_NAME = "expiration_daily_check"
    }
}

class ExpirationPlaceholderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result = Result.success()
}
