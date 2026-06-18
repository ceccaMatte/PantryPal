package com.example.pantrypal.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.pantrypal.MainActivity
import com.example.pantrypal.R
import com.example.pantrypal.domain.model.ExpirationNotificationContent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {
    private val debugNotificationIds = AtomicInteger(DEBUG_NOTIFICATION_ID_START)

    override suspend fun areNotificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    override fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Scadenze alimenti",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifiche per alimenti in scadenza"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun showExpirationSummaryNotification(
        input: ExpirationNotificationContent,
        debug: Boolean
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        createNotificationChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(input.title)
            .setContentText(input.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(input.body))
            .setContentIntent(contentIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationId = if (debug) debugNotificationIds.getAndIncrement() else NOTIFICATION_ID
        return runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }.isSuccess
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    companion object {
        const val CHANNEL_ID = "expiry_alerts"
        const val NOTIFICATION_ID = 1001
        private const val DEBUG_NOTIFICATION_ID_START = 2001
    }
}
