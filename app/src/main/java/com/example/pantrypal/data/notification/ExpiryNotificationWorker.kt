package com.example.pantrypal.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pantrypal.domain.usecase.CheckExpiryNotificationsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class ExpiryNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ExpiryNotificationWorkerEntryPoint::class.java
        )
        entryPoint.checkExpiryNotificationsUseCase().invoke()
        return Result.success()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExpiryNotificationWorkerEntryPoint {
    fun checkExpiryNotificationsUseCase(): CheckExpiryNotificationsUseCase
}
