package com.example.pantrypal.data.settings

import com.example.pantrypal.domain.model.AppTheme
import com.example.pantrypal.domain.model.StorageLocationFilter
import com.example.pantrypal.domain.model.UserSettings
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun getSettings(): UserSettings
    suspend fun updateUsername(username: String?)
    suspend fun updateLanguage(language: String)
    suspend fun updateTheme(theme: AppTheme)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun updateFreshNotificationDays(days: Int)
    suspend fun updateLongLifeNotificationDays(days: Int)
    suspend fun updatePantryStorageFilter(filter: StorageLocationFilter)
    suspend fun setLastExpiryNotificationDate(date: LocalDate?) = Unit
    suspend fun getSeedDataVersion(): Int
    suspend fun setSeedDataVersion(version: Int)
}
