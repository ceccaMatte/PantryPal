package com.example.pantrypal.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.BuildConfig
import com.example.pantrypal.data.notification.NotificationRepository
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.CheckExpiryNotificationsResult
import com.example.pantrypal.domain.model.AppTheme
import com.example.pantrypal.domain.model.UserSettings
import com.example.pantrypal.domain.usecase.CheckExpiryNotificationsUseCase
import com.example.pantrypal.domain.usecase.UpdateNotificationSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository,
    private val updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase,
    private val checkExpiryNotificationsUseCase: CheckExpiryNotificationsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

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
                is ProfileEvent.OnNotificationsChanged -> onNotificationsChanged(event.enabled)
                is ProfileEvent.OnNotificationPermissionResult -> onNotificationPermissionResult(event.granted)
                ProfileEvent.OnDebugNotificationClick -> runDebugNotificationCheck()
                ProfileEvent.OnFreshDaysMinus -> updateNotificationSettingsUseCase.setFreshNotificationDays(current.freshNotificationDays - 1)
                ProfileEvent.OnFreshDaysPlus -> updateNotificationSettingsUseCase.setFreshNotificationDays(current.freshNotificationDays + 1)
                ProfileEvent.OnLongLifeDaysMinus -> updateNotificationSettingsUseCase.setLongLifeNotificationDays(current.longLifeNotificationDays - 1)
                ProfileEvent.OnLongLifeDaysPlus -> updateNotificationSettingsUseCase.setLongLifeNotificationDays(current.longLifeNotificationDays + 1)
            }
        }
    }

    private suspend fun onNotificationsChanged(enabled: Boolean) {
        if (!enabled) {
            updateNotificationSettingsUseCase.setNotificationsEnabled(false)
            return
        }
        if (notificationRepository.areNotificationsAllowed()) {
            updateNotificationSettingsUseCase.setNotificationsEnabled(true)
        } else {
            _effects.send(ProfileEffect.RequestNotificationPermission)
        }
    }

    private suspend fun onNotificationPermissionResult(granted: Boolean) {
        if (granted) {
            updateNotificationSettingsUseCase.setNotificationsEnabled(true)
            _effects.send(ProfileEffect.ShowSnackbar("Notifiche attivate"))
        } else {
            updateNotificationSettingsUseCase.setNotificationsEnabled(false)
            _effects.send(ProfileEffect.ShowSnackbar("Permesso notifiche non concesso"))
        }
    }

    private suspend fun runDebugNotificationCheck() {
        val message = when (
            checkExpiryNotificationsUseCase(
                ignoreAlreadySentToday = true,
                updateLastNotificationDate = false,
                debugNotification = true
            )
        ) {
            CheckExpiryNotificationsResult.NotificationShown -> "Notifica di scadenza inviata"
            CheckExpiryNotificationsResult.Disabled -> "Attiva le notifiche per testarle"
            CheckExpiryNotificationsResult.PermissionDenied -> "Permesso notifiche non concesso"
            CheckExpiryNotificationsResult.AlreadySentToday -> "Notifica gia inviata oggi"
            CheckExpiryNotificationsResult.NothingToNotify -> "Nessun alimento in scadenza"
            CheckExpiryNotificationsResult.NotificationFailed -> "Impossibile mostrare la notifica"
        }
        _effects.send(ProfileEffect.ShowSnackbar(message))
    }
}

private fun UserSettings.toUi(): ProfileUiState =
    ProfileUiState(
        username = username,
        language = language,
        theme = theme,
        expirationNotificationsEnabled = expirationNotificationsEnabled,
        freshNotificationDays = freshNotificationDays,
        longLifeNotificationDays = longLifeNotificationDays,
        showDebugNotificationTrigger = BuildConfig.DEBUG
    )
