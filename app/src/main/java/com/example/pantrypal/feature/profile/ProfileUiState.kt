package com.example.pantrypal.feature.profile

import com.example.pantrypal.domain.model.AppTheme

data class ProfileUiState(
    val username: String = "",
    val language: String = "it",
    val theme: AppTheme = AppTheme.SYSTEM,
    val expirationNotificationsEnabled: Boolean = false,
    val freshNotificationDays: Int = 2,
    val longLifeNotificationDays: Int = 7
)

sealed interface ProfileEvent {
    data class OnUsernameChange(val value: String) : ProfileEvent
    data class OnThemeSelected(val theme: AppTheme) : ProfileEvent
    data class OnNotificationsChanged(val enabled: Boolean) : ProfileEvent
    data object OnFreshDaysMinus : ProfileEvent
    data object OnFreshDaysPlus : ProfileEvent
    data object OnLongLifeDaysMinus : ProfileEvent
    data object OnLongLifeDaysPlus : ProfileEvent
}
