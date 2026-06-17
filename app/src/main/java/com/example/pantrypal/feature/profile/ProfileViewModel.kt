package com.example.pantrypal.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.AppTheme
import com.example.pantrypal.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.value = settings.toUi()
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        viewModelScope.launch {
            val current = _uiState.value
            when (event) {
                is ProfileEvent.OnUsernameChange -> settingsRepository.updateUsername(event.value)
                is ProfileEvent.OnThemeSelected -> settingsRepository.updateTheme(event.theme)
                is ProfileEvent.OnNotificationsChanged -> settingsRepository.setNotificationsEnabled(event.enabled)
                ProfileEvent.OnFreshDaysMinus -> settingsRepository.updateFreshNotificationDays(current.freshNotificationDays - 1)
                ProfileEvent.OnFreshDaysPlus -> settingsRepository.updateFreshNotificationDays(current.freshNotificationDays + 1)
                ProfileEvent.OnLongLifeDaysMinus -> settingsRepository.updateLongLifeNotificationDays(current.longLifeNotificationDays - 1)
                ProfileEvent.OnLongLifeDaysPlus -> settingsRepository.updateLongLifeNotificationDays(current.longLifeNotificationDays + 1)
            }
        }
    }
}

private fun UserSettings.toUi(): ProfileUiState =
    ProfileUiState(
        username = username,
        language = language,
        theme = theme,
        expirationNotificationsEnabled = expirationNotificationsEnabled,
        freshNotificationDays = freshNotificationDays,
        longLifeNotificationDays = longLifeNotificationDays
    )
