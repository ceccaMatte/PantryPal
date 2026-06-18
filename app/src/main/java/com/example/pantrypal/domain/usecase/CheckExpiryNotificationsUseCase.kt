package com.example.pantrypal.domain.usecase

import com.example.pantrypal.core.util.DateProvider
import com.example.pantrypal.data.notification.NotificationRepository
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.CheckExpiryNotificationsResult
import com.example.pantrypal.domain.model.ExpirationNotificationContent
import com.example.pantrypal.domain.model.ExpiryNotificationSummary
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.UserSettings
import java.time.LocalDate
import javax.inject.Inject

class CheckExpiryNotificationsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pantryRepository: PantryRepository,
    private val notificationRepository: NotificationRepository,
    private val dateProvider: DateProvider
) {
    suspend operator fun invoke(): CheckExpiryNotificationsResult {
        val settings = settingsRepository.getSettings()
        if (!settings.expirationNotificationsEnabled) return CheckExpiryNotificationsResult.Disabled
        if (!notificationRepository.areNotificationsAllowed()) return CheckExpiryNotificationsResult.PermissionDenied

        val today = dateProvider.today()
        if (settings.lastExpiryNotificationDate == today) {
            return CheckExpiryNotificationsResult.AlreadySentToday
        }

        val summary = buildExpiryNotificationSummary(
            lots = pantryRepository.getActiveLotsWithCategories(),
            settings = settings,
            today = today
        )
        if (summary.totalCount == 0) return CheckExpiryNotificationsResult.NothingToNotify

        val shown = notificationRepository.showExpirationSummaryNotification(summary.toNotificationContent())
        return if (shown) {
            settingsRepository.setLastExpiryNotificationDate(today)
            CheckExpiryNotificationsResult.NotificationShown
        } else {
            CheckExpiryNotificationsResult.NotificationFailed
        }
    }
}

fun buildExpiryNotificationSummary(
    lots: List<LotWithCategory>,
    settings: UserSettings,
    today: LocalDate
): ExpiryNotificationSummary {
    val categorySummaries = lots
        .filter { it.quantity > 0 }
        .groupBy { it.categoryId }
        .mapNotNull { (_, categoryLots) ->
            val expiredLots = categoryLots.filter { it.expirationDate.isBefore(today) }
            if (expiredLots.isNotEmpty()) {
                val nearest = expiredLots.minBy { it.expirationDate }
                CategoryExpirySummary(
                    name = nearest.categoryName,
                    expirationDate = nearest.expirationDate,
                    status = ExpiryStatus.EXPIRED
                )
            } else {
                val expiringLots = categoryLots.filter { lot ->
                    !lot.expirationDate.isBefore(today) &&
                        !lot.expirationDate.isAfter(today.plusDays(thresholdFor(lot, settings).toLong()))
                }
                expiringLots.minByOrNull { it.expirationDate }?.let { nearest ->
                    CategoryExpirySummary(
                        name = nearest.categoryName,
                        expirationDate = nearest.expirationDate,
                        status = ExpiryStatus.EXPIRING
                    )
                }
            }
        }
        .sortedWith(compareBy<CategoryExpirySummary> { if (it.status == ExpiryStatus.EXPIRED) 0 else 1 }
            .thenBy { it.expirationDate }
            .thenBy { it.name.lowercase() })

    return ExpiryNotificationSummary(
        expiredCount = categorySummaries.count { it.status == ExpiryStatus.EXPIRED },
        expiringSoonCount = categorySummaries.count { it.status == ExpiryStatus.EXPIRING },
        itemNames = categorySummaries.take(3).map { it.name }
    )
}

fun ExpiryNotificationSummary.toNotificationContent(): ExpirationNotificationContent {
    val countText = when {
        expiredCount > 0 && expiringSoonCount > 0 ->
            "$expiredCount ${expiredCount.foodWord()} già ${expiredCount.expiredVerb()}, " +
                "$expiringSoonCount ${expiringSoonCount.foodWord()} scadono presto"
        expiredCount > 0 -> "$expiredCount ${expiredCount.foodWord()} già ${expiredCount.expiredVerb()}"
        else -> "$expiringSoonCount ${expiringSoonCount.foodWord()} scadono presto"
    }
    val names = formatNames(itemNames, totalCount)
    val body = if (names.isBlank()) {
        "Hai $totalCount ${totalCount.foodWord()} in scadenza: $countText."
    } else {
        "Hai $totalCount ${totalCount.foodWord()} in scadenza: $countText. Controlla $names."
    }
    return ExpirationNotificationContent(
        title = "PantryPal",
        body = body
    )
}

private fun thresholdFor(lot: LotWithCategory, settings: UserSettings): Int =
    when (lot.perishability) {
        PerishabilityType.FRESH -> settings.freshNotificationDays
        PerishabilityType.LONG_LIFE -> settings.longLifeNotificationDays
    }

private fun formatNames(names: List<String>, total: Int): String =
    when (names.size) {
        0 -> ""
        1 -> if (total > 1) "${names.first()} e altri ${total - 1}" else names.first()
        2 -> names.joinToString(" e ")
        else -> {
            val base = "${names[0]}, ${names[1]} e ${names[2]}"
            if (total > 3) "$base e altri ${total - 3}" else base
        }
    }

private fun Int.foodWord(): String = if (this == 1) "alimento" else "alimenti"

private fun Int.expiredVerb(): String = if (this == 1) "scaduto" else "scaduti"

private data class CategoryExpirySummary(
    val name: String,
    val expirationDate: LocalDate,
    val status: ExpiryStatus
)

private enum class ExpiryStatus {
    EXPIRED,
    EXPIRING
}
