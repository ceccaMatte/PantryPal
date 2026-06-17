package com.example.pantrypal.domain.usecase

import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.UserSettings
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun isLotInExpirationThreshold(
    lot: LotWithCategory,
    settings: UserSettings,
    today: LocalDate = LocalDate.now()
): Boolean {
    val daysUntilExpiration = ChronoUnit.DAYS.between(today, lot.expirationDate)
    val threshold = when (lot.perishability) {
        PerishabilityType.FRESH -> settings.freshNotificationDays
        PerishabilityType.LONG_LIFE -> settings.longLifeNotificationDays
    }
    return daysUntilExpiration <= threshold
}
