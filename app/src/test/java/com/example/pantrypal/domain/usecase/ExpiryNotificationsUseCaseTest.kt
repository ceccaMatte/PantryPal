package com.example.pantrypal.domain.usecase

import com.example.pantrypal.core.util.DateProvider
import com.example.pantrypal.data.notification.NotificationRepository
import com.example.pantrypal.data.notification.NotificationScheduler
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.AddFoodLotDraft
import com.example.pantrypal.domain.model.AppTheme
import com.example.pantrypal.domain.model.BarcodeProductDraft
import com.example.pantrypal.domain.model.BarcodeProductLink
import com.example.pantrypal.domain.model.CheckExpiryNotificationsResult
import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.ExpirationNotificationContent
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodDetailData
import com.example.pantrypal.domain.model.FoodDetailDraft
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter
import com.example.pantrypal.domain.model.UserSettings
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryNotificationsUseCaseTest {
    private val today = LocalDate.of(2026, 6, 18)

    @Test
    fun summaryCountsExpiredAndExpiringLots() {
        val summary = buildExpiryNotificationSummary(
            lots = listOf(
                lot(categoryId = 1, name = "Latte", date = today.minusDays(1)),
                lot(categoryId = 2, name = "Yogurt", date = today.plusDays(3)),
                lot(categoryId = 3, name = "Pasta", date = today.plusDays(8), perishability = PerishabilityType.LONG_LIFE)
            ),
            settings = settings(freshDays = 3, longLifeDays = 7),
            today = today
        )

        assertEquals(1, summary.expiredCount)
        assertEquals(1, summary.expiringSoonCount)
        assertEquals(listOf("Latte", "Yogurt"), summary.itemNames)
    }

    @Test
    fun categoryWithExpiredAndExpiringLotCountsOnlyAsExpired() {
        val summary = buildExpiryNotificationSummary(
            lots = listOf(
                lot(categoryId = 1, name = "Latte", date = today.minusDays(1)),
                lot(categoryId = 1, name = "Latte", date = today.plusDays(1))
            ),
            settings = settings(freshDays = 3),
            today = today
        )

        assertEquals(1, summary.expiredCount)
        assertEquals(0, summary.expiringSoonCount)
        assertEquals(1, summary.totalCount)
    }

    @Test
    fun freshAndLongLifeUseSeparateThresholds() {
        val summary = buildExpiryNotificationSummary(
            lots = listOf(
                lot(categoryId = 1, name = "Latte", date = today.plusDays(1), quantity = 0),
                lot(categoryId = 2, name = "Pollo", date = today.plusDays(4), perishability = PerishabilityType.FRESH),
                lot(categoryId = 3, name = "Riso", date = today.plusDays(7), perishability = PerishabilityType.LONG_LIFE)
            ),
            settings = settings(freshDays = 3, longLifeDays = 7),
            today = today
        )

        assertEquals(0, summary.expiredCount)
        assertEquals(1, summary.expiringSoonCount)
        assertEquals(listOf("Riso"), summary.itemNames)
    }

    @Test
    fun disabledNotificationsDoNotNotify() = runTest {
        val settingsRepository = ExpiryFakeSettingsRepository(settings(enabled = false))
        val notificationRepository = FakeNotificationRepository()
        val result = useCase(settingsRepository, notificationRepository = notificationRepository)()

        assertEquals(CheckExpiryNotificationsResult.Disabled, result)
        assertEquals(0, notificationRepository.showCount)
    }

    @Test
    fun missingPermissionDoesNotNotifyOrUpdateLastDate() = runTest {
        val settingsRepository = ExpiryFakeSettingsRepository(settings(enabled = true))
        val notificationRepository = FakeNotificationRepository(allowed = false)
        val result = useCase(settingsRepository, notificationRepository = notificationRepository)()

        assertEquals(CheckExpiryNotificationsResult.PermissionDenied, result)
        assertEquals(0, notificationRepository.showCount)
        assertNull(settingsRepository.current.lastExpiryNotificationDate)
    }

    @Test
    fun alreadySentTodayDoesNotNotifyAgain() = runTest {
        val settingsRepository = ExpiryFakeSettingsRepository(
            settings(enabled = true, lastDate = today)
        )
        val notificationRepository = FakeNotificationRepository()
        val result = useCase(settingsRepository, notificationRepository = notificationRepository)()

        assertEquals(CheckExpiryNotificationsResult.AlreadySentToday, result)
        assertEquals(0, notificationRepository.showCount)
    }

    @Test
    fun debugCheckIgnoresAlreadySentTodayAndDoesNotUpdateLastDate() = runTest {
        val yesterday = today.minusDays(1)
        val settingsRepository = ExpiryFakeSettingsRepository(
            settings(enabled = true, lastDate = today)
        )
        val notificationRepository = FakeNotificationRepository()

        settingsRepository.setLastExpiryNotificationDate(today)
        val result = useCase(
            settingsRepository = settingsRepository,
            notificationRepository = notificationRepository
        ).invoke(ignoreAlreadySentToday = true, updateLastNotificationDate = false)

        assertEquals(CheckExpiryNotificationsResult.NotificationShown, result)
        assertEquals(1, notificationRepository.showCount)
        assertEquals(today, settingsRepository.current.lastExpiryNotificationDate)

        settingsRepository.setLastExpiryNotificationDate(yesterday)
        useCase(
            settingsRepository = settingsRepository,
            notificationRepository = notificationRepository
        ).invoke(ignoreAlreadySentToday = true, updateLastNotificationDate = false)
        assertEquals(yesterday, settingsRepository.current.lastExpiryNotificationDate)
    }

    @Test
    fun relevantLotsShowNotificationAndPersistLastDate() = runTest {
        val settingsRepository = ExpiryFakeSettingsRepository(settings(enabled = true))
        val pantryRepository = ExpiryFakePantryRepository(listOf(lot(categoryId = 1, name = "Latte", date = today.plusDays(1))))
        val notificationRepository = FakeNotificationRepository()
        val result = useCase(
            settingsRepository = settingsRepository,
            pantryRepository = pantryRepository,
            notificationRepository = notificationRepository
        )()

        assertEquals(CheckExpiryNotificationsResult.NotificationShown, result)
        assertEquals(1, notificationRepository.showCount)
        assertEquals(today, settingsRepository.current.lastExpiryNotificationDate)
    }

    @Test
    fun updateSettingsSchedulesAndCancelsWork() = runTest {
        val settingsRepository = ExpiryFakeSettingsRepository(settings(enabled = false))
        val scheduler = FakeNotificationScheduler()
        val useCase = UpdateNotificationSettingsUseCase(settingsRepository, scheduler)

        useCase.setNotificationsEnabled(true)
        assertTrue(settingsRepository.current.expirationNotificationsEnabled)
        assertEquals(1, scheduler.scheduleCount)

        useCase.setFreshNotificationDays(7)
        assertEquals(7, settingsRepository.current.freshNotificationDays)
        assertEquals(3, settingsRepository.current.longLifeNotificationDays)
        assertEquals(2, scheduler.scheduleCount)

        useCase.setLongLifeNotificationDays(30)
        assertEquals(7, settingsRepository.current.freshNotificationDays)
        assertEquals(30, settingsRepository.current.longLifeNotificationDays)
        assertEquals(3, scheduler.scheduleCount)

        useCase.setNotificationsEnabled(false)
        assertFalse(settingsRepository.current.expirationNotificationsEnabled)
        assertEquals(1, scheduler.cancelCount)
    }

    private fun useCase(
        settingsRepository: ExpiryFakeSettingsRepository = ExpiryFakeSettingsRepository(settings(enabled = true)),
        pantryRepository: ExpiryFakePantryRepository = ExpiryFakePantryRepository(listOf(lot(1, "Latte", today.plusDays(1)))),
        notificationRepository: FakeNotificationRepository = FakeNotificationRepository()
    ): CheckExpiryNotificationsUseCase =
        CheckExpiryNotificationsUseCase(
            settingsRepository = settingsRepository,
            pantryRepository = pantryRepository,
            notificationRepository = notificationRepository,
            dateProvider = FixedDateProvider(today)
        )

    private fun settings(
        enabled: Boolean = true,
        freshDays: Int = 3,
        longLifeDays: Int = 3,
        lastDate: LocalDate? = null
    ): UserSettings =
        UserSettings(
            expirationNotificationsEnabled = enabled,
            freshNotificationDays = freshDays,
            longLifeNotificationDays = longLifeDays,
            lastExpiryNotificationDate = lastDate
        )
}

private class FixedDateProvider(private val today: LocalDate) : DateProvider {
    override fun today(): LocalDate = today
}

private class FakeNotificationRepository(
    private val allowed: Boolean = true,
    private val showSucceeds: Boolean = true
) : NotificationRepository {
    var showCount = 0
    var lastContent: ExpirationNotificationContent? = null
    override suspend fun areNotificationsAllowed(): Boolean = allowed
    override fun createNotificationChannel() = Unit
    override fun showExpirationSummaryNotification(input: ExpirationNotificationContent): Boolean {
        showCount++
        lastContent = input
        return showSucceeds
    }
}

private class FakeNotificationScheduler : NotificationScheduler {
    var scheduleCount = 0
    var cancelCount = 0
    override fun scheduleDailyExpiryCheck() {
        scheduleCount++
    }
    override fun cancelDailyExpiryCheck() {
        cancelCount++
    }
}

private class ExpiryFakeSettingsRepository(initial: UserSettings) : SettingsRepository {
    private val settingsFlow = MutableStateFlow(initial)
    var current: UserSettings
        get() = settingsFlow.value
        private set(value) {
            settingsFlow.value = value
        }

    override fun observeSettings(): Flow<UserSettings> = settingsFlow
    override suspend fun getSettings(): UserSettings = current
    override suspend fun updateUsername(username: String?) {
        current = current.copy(username = username.orEmpty())
    }
    override suspend fun updateLanguage(language: String) {
        current = current.copy(language = language)
    }
    override suspend fun updateTheme(theme: AppTheme) {
        current = current.copy(theme = theme)
    }
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        current = current.copy(expirationNotificationsEnabled = enabled)
    }
    override suspend fun updateFreshNotificationDays(days: Int) {
        current = current.copy(freshNotificationDays = days)
    }
    override suspend fun updateLongLifeNotificationDays(days: Int) {
        current = current.copy(longLifeNotificationDays = days)
    }
    override suspend fun updatePantryStorageFilter(filter: StorageLocationFilter) {
        current = current.copy(pantryStorageFilter = filter)
    }
    override suspend fun setLastExpiryNotificationDate(date: LocalDate?) {
        current = current.copy(lastExpiryNotificationDate = date)
    }
    override suspend fun getSeedDataVersion(): Int = current.seedDataVersion
    override suspend fun setSeedDataVersion(version: Int) {
        current = current.copy(seedDataVersion = version)
    }
}

private class ExpiryFakePantryRepository(
    private val lots: List<LotWithCategory>
) : PantryRepository {
    override fun observePantryRows(filter: StorageLocationFilter): Flow<List<PantryRow>> = TODO("unused")
    override fun observeFoodDetail(categoryId: Long): Flow<FoodDetailData?> = TODO("unused")
    override fun observeActiveLotsWithCategories(): Flow<List<LotWithCategory>> = TODO("unused")
    override suspend fun getActiveLotsWithCategories(): List<LotWithCategory> = lots
    override suspend fun getActiveLotsForCategories(categoryIds: List<Long>): List<LotWithCategory> = lots.filter { it.categoryId in categoryIds }
    override suspend fun getFoodCategory(categoryId: Long): FoodCategory? = TODO("unused")
    override suspend fun searchFoodCategories(query: String, limit: Int): List<FoodCategory> = TODO("unused")
    override suspend fun getFoodCategoryMatchSources(query: String, limit: Int): List<FoodCategoryMatchSource> = TODO("unused")
    override suspend fun findCategoryByNormalizedName(normalizedName: String): FoodCategory? = TODO("unused")
    override suspend fun findActiveBarcodeLink(barcode: String): BarcodeProductLink? = TODO("unused")
    override suspend fun createFoodCategory(input: CreateFoodCategoryInput): Long = TODO("unused")
    override suspend fun saveAddedFood(
        categorySelection: AddFoodCategorySelection,
        lots: List<AddFoodLotDraft>,
        barcodeProductDraft: BarcodeProductDraft?
    ): Long = TODO("unused")
    override suspend fun saveFoodDetailChanges(draft: FoodDetailDraft) = TODO("unused")
    override suspend fun upsertExpiryLot(categoryId: Long, expirationDate: LocalDate, quantityDelta: Int) = TODO("unused")
    override suspend fun decrementSingleLotCategory(categoryId: Long): Boolean = TODO("unused")
    override suspend fun addRecipeIngredientAlias(categoryId: Long, aliasOriginal: String, language: String?): Long = TODO("unused")
    override suspend fun removeRecipeIngredientAlias(linkId: Long) = TODO("unused")
    override suspend fun deactivateBarcodeLink(barcode: String) = TODO("unused")
}

private fun lot(
    categoryId: Long,
    name: String,
    date: LocalDate,
    perishability: PerishabilityType = PerishabilityType.FRESH,
    quantity: Int = 1
): LotWithCategory =
    LotWithCategory(
        lotId = categoryId,
        categoryId = categoryId,
        categoryName = name,
        normalizedName = name.lowercase(),
        storageLocation = StorageLocation.FRIDGE,
        perishability = perishability,
        expirationDate = date,
        quantity = quantity
    )
