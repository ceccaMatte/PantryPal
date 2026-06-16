package com.example.pantrypal.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pantrypal.domain.model.AppTheme
import com.example.pantrypal.domain.model.StorageLocationFilter
import com.example.pantrypal.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    override fun observeSettings(): Flow<UserSettings> =
        context.settingsDataStore.data.map { preferences ->
            UserSettings(
                username = preferences[Keys.USERNAME].orEmpty(),
                language = preferences[Keys.LANGUAGE] ?: "it",
                theme = preferences[Keys.THEME]?.let(AppTheme::valueOf) ?: AppTheme.SYSTEM,
                expirationNotificationsEnabled = preferences[Keys.EXPIRATION_NOTIFICATIONS_ENABLED] ?: false,
                freshNotificationDays = preferences[Keys.FRESH_NOTIFICATION_DAYS] ?: 2,
                longLifeNotificationDays = preferences[Keys.LONG_LIFE_NOTIFICATION_DAYS] ?: 7,
                pantryStorageFilter = preferences[Keys.PANTRY_STORAGE_FILTER]?.let(StorageLocationFilter::valueOf)
                    ?: StorageLocationFilter.ALL,
                seedDataVersion = preferences[Keys.SEED_DATA_VERSION] ?: 0
            )
        }

    override suspend fun getSettings(): UserSettings = observeSettings().first()

    override suspend fun updateUsername(username: String?) {
        context.settingsDataStore.edit { it[Keys.USERNAME] = username.orEmpty() }
    }

    override suspend fun updateLanguage(language: String) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.ifBlank { "it" } }
    }

    override suspend fun updateTheme(theme: AppTheme) {
        context.settingsDataStore.edit { it[Keys.THEME] = theme.name }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.EXPIRATION_NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun updateFreshNotificationDays(days: Int) {
        context.settingsDataStore.edit { it[Keys.FRESH_NOTIFICATION_DAYS] = days.coerceIn(1, 7) }
    }

    override suspend fun updateLongLifeNotificationDays(days: Int) {
        context.settingsDataStore.edit { it[Keys.LONG_LIFE_NOTIFICATION_DAYS] = days.coerceIn(1, 30) }
    }

    override suspend fun updatePantryStorageFilter(filter: StorageLocationFilter) {
        context.settingsDataStore.edit { it[Keys.PANTRY_STORAGE_FILTER] = filter.name }
    }

    override suspend fun getSeedDataVersion(): Int = getSettings().seedDataVersion

    override suspend fun setSeedDataVersion(version: Int) {
        context.settingsDataStore.edit { it[Keys.SEED_DATA_VERSION] = version }
    }

    private object Keys {
        val USERNAME = stringPreferencesKey("username")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val EXPIRATION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("expiration_notifications_enabled")
        val FRESH_NOTIFICATION_DAYS = intPreferencesKey("fresh_notification_days")
        val LONG_LIFE_NOTIFICATION_DAYS = intPreferencesKey("long_life_notification_days")
        val PANTRY_STORAGE_FILTER = stringPreferencesKey("pantry_storage_filter")
        val SEED_DATA_VERSION = intPreferencesKey("seed_data_version")
    }
}
