package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.AddFoodLotDraft
import com.example.pantrypal.domain.model.BarcodeProductDraft
import com.example.pantrypal.domain.model.BarcodeProductLink
import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodDetailDraft
import com.example.pantrypal.domain.model.FoodDetailData
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.SaveAddedFoodCommand
import com.example.pantrypal.domain.model.SaveAddedFoodResult
import com.example.pantrypal.domain.model.SaveAddedFoodValidationError
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveAddedFoodUseCaseTest {
    @Test
    fun returnsValidationErrorsForMissingRequiredFields() = runBlocking {
        val useCase = SaveAddedFoodUseCase(FakePantryRepositoryForAddedFood())

        val result = useCase(
            SaveAddedFoodCommand(
                categorySelection = null,
                lots = emptyList(),
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH
            )
        )

        val errors = (result as SaveAddedFoodResult.ValidationError).errors
        assertTrue(SaveAddedFoodValidationError.CATEGORY_REQUIRED in errors)
        assertTrue(SaveAddedFoodValidationError.LOTS_REQUIRED in errors)
    }

    @Test
    fun returnsDateErrorWhenAValidQuantityLotHasNoDate() = runBlocking {
        val useCase = SaveAddedFoodUseCase(FakePantryRepositoryForAddedFood())

        val result = useCase(
            SaveAddedFoodCommand(
                categorySelection = AddFoodCategorySelection.Existing(7),
                lots = listOf(AddFoodLotDraft(expirationDate = null, quantity = 1)),
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH
            )
        )

        val errors = (result as SaveAddedFoodResult.ValidationError).errors
        assertTrue(SaveAddedFoodValidationError.DATE_REQUIRED in errors)
    }

    @Test
    fun savesExistingCategoryWithLot() = runBlocking {
        val repository = FakePantryRepositoryForAddedFood()
        val useCase = SaveAddedFoodUseCase(repository)
        val date = LocalDate.of(2026, 6, 20)

        val result = useCase(
            SaveAddedFoodCommand(
                categorySelection = AddFoodCategorySelection.Existing(7),
                lots = listOf(AddFoodLotDraft(date, 2)),
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH
            )
        )

        assertEquals(SaveAddedFoodResult.Success(7), result)
        assertEquals(AddFoodCategorySelection.Existing(7), repository.savedSelection)
        assertEquals(listOf(AddFoodLotDraft(date, 2)), repository.savedLots)
    }

    @Test
    fun savesNewCategoryOnlyOnSave() = runBlocking {
        val repository = FakePantryRepositoryForAddedFood(returnCategoryId = 42)
        val useCase = SaveAddedFoodUseCase(repository)

        val result = useCase(
            SaveAddedFoodCommand(
                categorySelection = AddFoodCategorySelection.New(
                    name = "Kefir",
                    storageLocation = StorageLocation.FRIDGE,
                    perishability = PerishabilityType.FRESH
                ),
                lots = listOf(AddFoodLotDraft(LocalDate.of(2026, 7, 1), 1)),
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH
            )
        )

        assertEquals(SaveAddedFoodResult.Success(42), result)
        assertTrue(repository.savedSelection is AddFoodCategorySelection.New)
    }

    @Test
    fun filtersOutZeroQuantityLotsBeforeSaving() = runBlocking {
        val repository = FakePantryRepositoryForAddedFood()
        val useCase = SaveAddedFoodUseCase(repository)
        val validDate = LocalDate.of(2026, 7, 1)

        val result = useCase(
            SaveAddedFoodCommand(
                categorySelection = AddFoodCategorySelection.Existing(7),
                lots = listOf(
                    AddFoodLotDraft(validDate, 2),
                    AddFoodLotDraft(LocalDate.of(2026, 7, 2), 0)
                ),
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH
            )
        )

        assertEquals(SaveAddedFoodResult.Success(7), result)
        assertEquals(listOf(AddFoodLotDraft(validDate, 2)), repository.savedLots)
    }

    @Test
    fun returnsLotsRequiredWhenAllLotsAreRemovedOrZero() = runBlocking {
        val repository = FakePantryRepositoryForAddedFood()
        val useCase = SaveAddedFoodUseCase(repository)

        val result = useCase(
            SaveAddedFoodCommand(
                categorySelection = AddFoodCategorySelection.Existing(7),
                lots = listOf(AddFoodLotDraft(LocalDate.of(2026, 7, 1), 0)),
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH
            )
        )

        val errors = (result as SaveAddedFoodResult.ValidationError).errors
        assertTrue(SaveAddedFoodValidationError.LOTS_REQUIRED in errors)
        assertEquals(emptyList<AddFoodLotDraft>(), repository.savedLots)
    }

    @Test
    fun forwardsMultipleValidLotsToRepository() = runBlocking {
        val repository = FakePantryRepositoryForAddedFood()
        val useCase = SaveAddedFoodUseCase(repository)
        val lots = listOf(
            AddFoodLotDraft(LocalDate.of(2026, 7, 1), 1),
            AddFoodLotDraft(LocalDate.of(2026, 7, 8), 3)
        )

        val result = useCase(
            SaveAddedFoodCommand(
                categorySelection = AddFoodCategorySelection.Existing(7),
                lots = lots,
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH
            )
        )

        assertEquals(SaveAddedFoodResult.Success(7), result)
        assertEquals(lots, repository.savedLots)
    }

    @Test
    fun forwardsBarcodeProductDraftToRepositoryOnSave() = runBlocking {
        val repository = FakePantryRepositoryForAddedFood()
        val useCase = SaveAddedFoodUseCase(repository)
        val draft = BarcodeProductDraft(
            barcode = "1234567890123",
            productName = "Latte Parmalat",
            genericName = "Latte",
            brand = "Parmalat",
            quantityLabel = "1L",
            imageUrl = null,
            rawCategoryTags = emptyList(),
            rawFoodGroupTags = emptyList()
        )

        useCase(
            SaveAddedFoodCommand(
                categorySelection = AddFoodCategorySelection.Existing(7),
                lots = listOf(AddFoodLotDraft(LocalDate.of(2026, 7, 1), 1)),
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH,
                barcodeProductDraft = draft
            )
        )

        assertEquals(draft, repository.savedBarcodeProductDraft)
    }

    @Test
    fun doesNotForwardBarcodeProductDraftWhenAbsent() = runBlocking {
        val repository = FakePantryRepositoryForAddedFood()
        val useCase = SaveAddedFoodUseCase(repository)

        useCase(
            SaveAddedFoodCommand(
                categorySelection = AddFoodCategorySelection.Existing(7),
                lots = listOf(AddFoodLotDraft(LocalDate.of(2026, 7, 1), 1)),
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH,
                barcodeProductDraft = null
            )
        )

        assertEquals(null, repository.savedBarcodeProductDraft)
    }

    @Test
    fun doesNotSaveWhenCategoryNotSelectedEvenIfBarcodePresent() = runBlocking {
        val repository = FakePantryRepositoryForAddedFood()
        val useCase = SaveAddedFoodUseCase(repository)
        val draft = BarcodeProductDraft(
            barcode = "1234567890123",
            productName = "Test",
            genericName = null,
            brand = null,
            quantityLabel = null,
            imageUrl = null,
            rawCategoryTags = emptyList(),
            rawFoodGroupTags = emptyList()
        )

        val result = useCase(
            SaveAddedFoodCommand(
                categorySelection = null,
                lots = listOf(AddFoodLotDraft(LocalDate.of(2026, 7, 1), 1)),
                storageLocation = StorageLocation.FRIDGE,
                perishability = PerishabilityType.FRESH,
                barcodeProductDraft = draft
            )
        )

        assertTrue(result is SaveAddedFoodResult.ValidationError)
        assertEquals(null, repository.savedBarcodeProductDraft)
    }
}

private class FakePantryRepositoryForAddedFood(
    private val returnCategoryId: Long = 7
) : PantryRepository {
    var savedSelection: AddFoodCategorySelection? = null
    var savedLots: List<AddFoodLotDraft> = emptyList()
    var savedBarcodeProductDraft: BarcodeProductDraft? = null

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
    override suspend fun createFoodCategory(input: CreateFoodCategoryInput): Long = returnCategoryId
    override suspend fun saveAddedFood(
        categorySelection: AddFoodCategorySelection,
        lots: List<AddFoodLotDraft>,
        barcodeProductDraft: BarcodeProductDraft?
    ): Long {
        savedSelection = categorySelection
        savedLots = lots
        savedBarcodeProductDraft = barcodeProductDraft
        return returnCategoryId
    }
    override suspend fun saveFoodDetailChanges(draft: FoodDetailDraft) = Unit
    override suspend fun upsertExpiryLot(categoryId: Long, expirationDate: LocalDate, quantityDelta: Int) = Unit
    override suspend fun decrementSingleLotCategory(categoryId: Long): Boolean = false
    override suspend fun addRecipeIngredientAlias(categoryId: Long, aliasOriginal: String, language: String?): Long = 0
    override suspend fun removeRecipeIngredientAlias(linkId: Long) = Unit
    override suspend fun deactivateBarcodeLink(barcode: String) = Unit
}
