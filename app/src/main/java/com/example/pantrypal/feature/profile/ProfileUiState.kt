package com.example.pantrypal.feature.profile

import com.example.pantrypal.domain.model.AppTheme

data class ProfileUiState(
    val username: String = "",
    val language: String = "it",
    val theme: AppTheme = AppTheme.SYSTEM,
    val expirationNotificationsEnabled: Boolean = false,
    val freshNotificationDays: Int = 2,
    val longLifeNotificationDays: Int = 7,
    val showDebugNotificationTrigger: Boolean = false
)

sealed interface ProfileEvent {
    data class OnUsernameChange(val value: String) : ProfileEvent
    data class OnThemeSelected(val theme: AppTheme) : ProfileEvent
    data class OnNotificationsChanged(val enabled: Boolean) : ProfileEvent
    data class OnNotificationPermissionResult(val granted: Boolean) : ProfileEvent
    data object OnDebugNotificationClick : ProfileEvent
    data object OnFreshDaysMinus : ProfileEvent
    data object OnFreshDaysPlus : ProfileEvent
    data object OnLongLifeDaysMinus : ProfileEvent
    data object OnLongLifeDaysPlus : ProfileEvent
}

sealed interface ProfileEffect {
    data object RequestNotificationPermission : ProfileEffect
    data class ShowSnackbar(val message: String) : ProfileEffect
}
