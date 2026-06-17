package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.HomeExpiringFood
import com.example.pantrypal.domain.model.HomeOverview
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.StorageLocation
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetHomeOverviewUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<HomeOverview> =
        combine(
            pantryRepository.observeActiveLotsWithCategories(),
            settingsRepository.observeSettings()
        ) { lots, settings ->
            val today = LocalDate.now()
            val expiring = lots
                .filter { lot -> isLotInExpirationThreshold(lot, settings, today) }
                .groupBy { it.categoryId }
                .values
                .map { categoryLots ->
                    val first = categoryLots.minBy { it.expirationDate }
                    HomeExpiringFood(
                        categoryId = first.categoryId,
                        name = first.categoryName,
                        expiringQuantity = categoryLots.sumOf { it.quantity },
                        storageLocation = first.storageLocation
                    )
                }
                .sortedWith(compareBy<HomeExpiringFood> { food ->
                    lots.filter { it.categoryId == food.categoryId }.minOf { it.expirationDate }
                }.thenBy { it.name })

            HomeOverview(
                username = settings.username.takeIf { it.isNotBlank() },
                totalPackages = lots.sumOf { it.quantity },
                fridgePackages = lots.sumForLocation(StorageLocation.FRIDGE),
                freezerPackages = lots.sumForLocation(StorageLocation.FREEZER),
                pantryPackages = lots.sumForLocation(StorageLocation.PANTRY),
                expiringFoods = expiring
            )
        }
}

private fun List<LotWithCategory>.sumForLocation(location: StorageLocation): Int =
    filter { it.storageLocation == location }.sumOf { it.quantity }
