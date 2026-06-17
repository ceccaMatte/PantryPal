package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.BarcodeProductDraft
import com.example.pantrypal.domain.model.BarcodeProductLink
import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodDetailDraft
import com.example.pantrypal.domain.model.FoodDetailData
import com.example.pantrypal.domain.model.FoodLotDraft
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.SaveFoodDetailChangesResult
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveFoodDetailChangesUseCaseTest {
    @Test
    fun mergeLotsByDateSumsQuantities() {
        val useCase = SaveFoodDetailChangesUseCase(FakePantryRepositoryForFoodDetail())
        val date = LocalDate.of(2026, 7, 20)

        val result = useCase.mergeLotsByDate(
            listOf(
                FoodLotDraft(1, date, 2),
                FoodLotDraft(2, date, 1)
            )
        )

        assertEquals(listOf(FoodLotDraft(1, date, 3)), result)
    }

    @Test
    fun mergeLotsByDateDropsZeroQuantityLots() {
        val useCase = SaveFoodDetailChangesUseCase(FakePantryRepositoryForFoodDetail())

        val result = useCase.mergeLotsByDate(
            listOf(
                FoodLotDraft(1, LocalDate.of(2026, 7, 20), 0),
                FoodLotDraft(2, LocalDate.of(2026, 7, 21), 2)
            )
        )

        assertEquals(1, result.size)
        assertEquals(2, result.first().quantity)
    }

    @Test
    fun allZeroLotsKeepsCategoryButSavesEmptySnapshot() = runBlocking {
        val repository = FakePantryRepositoryForFoodDetail()
        val useCase = SaveFoodDetailChangesUseCase(repository)

        val result = useCase(
            FoodDetailDraft(
                categoryId = 9,
                name = "Latte",
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH,
                lots = listOf(FoodLotDraft(1, LocalDate.of(2026, 7, 20), 0))
            )
        )

        assertEquals(SaveFoodDetailChangesResult.Success, result)
        assertEquals(9L, repository.savedDraft?.categoryId)
        assertTrue(repository.savedDraft?.lots.orEmpty().isEmpty())
    }
}

private class FakePantryRepositoryForFoodDetail : PantryRepository {
    var savedDraft: FoodDetailDraft? = null

    override fun observePantryRows(filter: StorageLocationFilter): Flow<List<PantryRow>> = emptyFlow()
    override fun observeFoodDetail(categoryId: Long): Flow<FoodDetailData?> = emptyFlow()
    override fun observeActiveLotsWithCategories(): Flow<List<LotWithCategory>> = emptyFlow()
    override suspend fun getActiveLotsWithCategories(): List<LotWithCategory> = emptyList()
    override suspend fun getActiveLotsForCategories(categoryIds: List<Long>): List<LotWithCategory> = emptyList()
    override suspend fun getFoodCategory(categoryId: Long): FoodCategory? = null
    override suspend fun searchFoodCategories(query: String, limit: Int): List<FoodCategory> = emptyList()
    override suspend fun getFoodCategoryMatchSources(query: String, limit: Int): List<FoodCategoryMatchSource> = emptyList()
    override suspend fun findCategoryByNormalizedName(normalizedName: String): FoodCategory? = null
    override suspend fun findActiveBarcodeLink(barcode: String): BarcodeProductLink? = null
    override suspend fun createFoodCategory(input: CreateFoodCategoryInput): Long = 0
    override suspend fun saveAddedFood(
        categorySelection: AddFoodCategorySelection,
        expirationDate: LocalDate,
        quantity: Int,
        barcodeProductDraft: BarcodeProductDraft?
    ): Long = 0
    override suspend fun saveFoodDetailChanges(draft: FoodDetailDraft) {
        savedDraft = draft
    }
    override suspend fun upsertExpiryLot(categoryId: Long, expirationDate: LocalDate, quantityDelta: Int) = Unit
    override suspend fun decrementSingleLotCategory(categoryId: Long): Boolean = false
    override suspend fun addRecipeIngredientAlias(categoryId: Long, aliasOriginal: String, language: String?): Long = 0
    override suspend fun removeRecipeIngredientAlias(linkId: Long) = Unit
    override suspend fun deactivateBarcodeLink(barcode: String) = Unit
}
