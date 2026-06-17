package com.example.pantrypal.domain.usecase

import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.UserSettings
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpirationThresholdPolicyTest {
    private val today = LocalDate.of(2026, 6, 17)

    @Test
    fun freshLotAfterThresholdIsNotExpiring() {
        val settings = UserSettings(
            expirationNotificationsEnabled = false,
            freshNotificationDays = 2,
            longLifeNotificationDays = 7
        )

        assertFalse(isLotInExpirationThreshold(lot(today.plusDays(3), PerishabilityType.FRESH), settings, today))
    }

    @Test
    fun freshLotInsideThresholdIsExpiringEvenWhenNotificationsOff() {
        val settings = UserSettings(
            expirationNotificationsEnabled = false,
            freshNotificationDays = 2,
            longLifeNotificationDays = 7
        )

        assertTrue(isLotInExpirationThreshold(lot(today.plusDays(2), PerishabilityType.FRESH), settings, today))
        assertTrue(isLotInExpirationThreshold(lot(today.minusDays(1), PerishabilityType.FRESH), settings, today))
    }

    @Test
    fun longLifeUsesLongLifeThreshold() {
        val settings = UserSettings(
            freshNotificationDays = 2,
            longLifeNotificationDays = 7
        )

        assertTrue(isLotInExpirationThreshold(lot(today.plusDays(7), PerishabilityType.LONG_LIFE), settings, today))
        assertFalse(isLotInExpirationThreshold(lot(today.plusDays(8), PerishabilityType.LONG_LIFE), settings, today))
    }
}

private fun lot(date: LocalDate, perishability: PerishabilityType): LotWithCategory =
    LotWithCategory(
        lotId = 1,
        categoryId = 1,
        categoryName = "Latte",
        normalizedName = "latte",
        storageLocation = StorageLocation.FRIDGE,
        perishability = perishability,
        expirationDate = date,
        quantity = 1
    )
