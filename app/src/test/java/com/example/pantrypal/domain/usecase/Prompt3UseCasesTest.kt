package com.example.pantrypal.domain.usecase

import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.product.FoodRecognitionRepository
import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.matching.FoodCategoryMatcher
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.AddFoodLotDraft
import com.example.pantrypal.domain.model.AppTheme
import com.example.pantrypal.domain.model.BarcodeProductDraft
import com.example.pantrypal.domain.model.BarcodeProductLink
import com.example.pantrypal.domain.model.BarcodeResolution
import com.example.pantrypal.domain.model.CategoryOrigin
import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.ExternalProductResult
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodDetailData
import com.example.pantrypal.domain.model.FoodDetailDraft
import com.example.pantrypal.domain.model.IngredientRelationType
import com.example.pantrypal.domain.model.LinkOrigin
import com.example.pantrypal.domain.model.LinkRecipeIngredientToFoodCommand
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.PantryPalApiMode
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetail
import com.example.pantrypal.domain.model.RecipeDetailResult
import com.example.pantrypal.domain.model.RecipeIngredientData
import com.example.pantrypal.domain.model.RecipeIngredientLink
import com.example.pantrypal.domain.model.RecipeSearchQuery
import com.example.pantrypal.domain.model.RecipeSearchResult
import com.example.pantrypal.domain.model.RecipeAvailabilityStatus
import com.example.pantrypal.domain.model.RecognizedBarcodeProduct
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter
import com.example.pantrypal.domain.model.UserSettings
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Prompt3UseCasesTest {
    private val normalizer = TextNormalizer()
    private val matcher = FoodCategoryMatcher(normalizer)

    @Test
    fun resolveBarcodeUsesLocalLinkBeforeRemote() = runBlocking {
        val pantry = FakePantryRepository().apply {
            categories[1] = category(1, "Latte", "latte")
            barcodeLinks["123"] = barcodeLink("123", 1)
        }
        val recognition = FakeFoodRecognitionRepository(ExternalProductResult.NotFound)
        val useCase = ResolveBarcodeUseCase(pantry, recognition, matcher, normalizer)

        val result = useCase("123")

        assertTrue(result is BarcodeResolution.KnownLocal)
        assertEquals(0, recognition.lookupCount)
    }

    @Test
    fun resolveBarcodeReturnsRemoteFoundWithoutSavingLink() = runBlocking {
        val pantry = FakePantryRepository().apply {
            categories[2] = category(2, "Latte", "latte")
            matchSources = listOf(FoodCategoryMatchSource(requireNotNull(categories[2])))
        }
        val recognition = FakeFoodRecognitionRepository(
            ExternalProductResult.Found(
                RecognizedBarcodeProduct(
                    barcode = "555",
                    productName = "Milk bottle",
                    genericName = "milk",
                    brand = "Brand",
                    quantityLabel = "1L",
                    imageUrl = null,
                    rawCategoryTags = emptyList(),
                    rawFoodGroupTags = emptyList()
                )
            )
        )
        val useCase = ResolveBarcodeUseCase(pantry, recognition, matcher, normalizer)

        val result = useCase("555")

        assertTrue(result is BarcodeResolution.FoundRemote)
        assertEquals(0, pantry.saveAddedFoodCalls)
        assertTrue(pantry.barcodeLinks.isEmpty())
    }

    @Test
    fun resolveBarcodeMapsNotFoundAndNetworkError() = runBlocking {
        val pantry = FakePantryRepository()

        val notFound = ResolveBarcodeUseCase(
            pantry,
            FakeFoodRecognitionRepository(ExternalProductResult.NotFound),
            matcher,
            normalizer
        )("404")
        val network = ResolveBarcodeUseCase(
            pantry,
            FakeFoodRecognitionRepository(ExternalProductResult.NetworkError),
            matcher,
            normalizer
        )("500")

        assertEquals(BarcodeResolution.NotFound, notFound)
        assertEquals(BarcodeResolution.NetworkError, network)
    }

    @Test
    fun toggleFavoriteSavesAndRemovesRecipeWithoutRemovingGlobalLinks() = runBlocking {
        val repository = FakeRecipeRepository()
        val useCase = ToggleFavoriteRecipeUseCase(repository)
        val recipe = recipe("10")
        repository.links += ingredientLink(id = 77, categoryId = 1, alias = "milk")

        assertTrue(useCase(recipe))
        assertTrue(repository.favorites.containsKey("10"))

        assertFalse(useCase(recipe))
        assertFalse(repository.favorites.containsKey("10"))
        assertTrue(repository.links.any { it.id == 77L })
    }

    @Test
    fun availabilityRequiresPersistentLinkAndActiveLot() = runBlocking {
        val pantry = FakePantryRepository().apply {
            categories[1] = category(1, "Latte", "latte")
            activeLots += lot(categoryId = 1, categoryName = "Latte", normalizedName = "latte", quantity = 1)
        }
        val recipes = FakeRecipeRepository().apply {
            links += ingredientLink(id = 1, categoryId = 1, alias = "milk")
        }
        val availability = GetRecipeAvailabilityUseCase(recipes, pantry)(
            recipe("20", ingredient = RecipeIngredientData("milk", "milk", null, null, null))
        )

        assertEquals(RecipeAvailabilityStatus.IN_PANTRY, availability.items.first().status)
    }

    @Test
    fun availabilityLinkedIngredientWithoutLotIsToBuy() = runBlocking {
        val pantry = FakePantryRepository().apply {
            categories[1] = category(1, "Latte", "latte")
        }
        val recipes = FakeRecipeRepository().apply {
            links += ingredientLink(id = 1, categoryId = 1, alias = "milk")
        }
        val availability = GetRecipeAvailabilityUseCase(recipes, pantry)(
            recipe("21", ingredient = RecipeIngredientData("milk", "milk", null, null, null))
        )

        assertEquals(RecipeAvailabilityStatus.TO_BUY, availability.items.first().status)
    }

    @Test
    fun availabilityWithoutLinkIsToBuyEvenIfCategoryTextMatches() = runBlocking {
        val pantry = FakePantryRepository().apply {
            categories[1] = category(1, "Milk", "milk")
            activeLots += lot(categoryId = 1, categoryName = "Milk", normalizedName = "milk", quantity = 1)
        }
        val availability = GetRecipeAvailabilityUseCase(FakeRecipeRepository(), pantry)(
            recipe("22", ingredient = RecipeIngredientData("milk", "milk", null, null, null))
        )

        assertEquals(RecipeAvailabilityStatus.TO_BUY, availability.items.first().status)
    }

    @Test
    fun availabilityManyToManyIsInPantryWhenOneLinkedCategoryHasLot() = runBlocking {
        val pantry = FakePantryRepository().apply {
            categories[1] = category(1, "Latte fresco", "latte fresco")
            categories[2] = category(2, "Latte UHT", "latte uht")
            activeLots += lot(categoryId = 2, categoryName = "Latte UHT", normalizedName = "latte uht", quantity = 2)
        }
        val recipes = FakeRecipeRepository().apply {
            links += ingredientLink(id = 1, categoryId = 1, alias = "milk")
            links += ingredientLink(id = 2, categoryId = 2, alias = "milk")
        }
        val availability = GetRecipeAvailabilityUseCase(recipes, pantry)(
            recipe("23", ingredient = RecipeIngredientData("milk", "milk", null, null, null))
        )

        assertEquals(RecipeAvailabilityStatus.IN_PANTRY, availability.items.first().status)
    }

    @Test
    fun linkIngredientCreatesUserExactAndAvoidsDuplicates() = runBlocking {
        val repository = FakeRecipeRepository()
        val useCase = LinkRecipeIngredientToFoodUseCase(repository, FakeSettingsRepository())
        val command = LinkRecipeIngredientToFoodCommand("milk", "milk", null, 1)

        useCase(command)
        useCase(command)

        assertEquals(1, repository.links.size)
        assertEquals(LinkOrigin.USER, repository.links.first().origin)
        assertEquals(IngredientRelationType.EXACT, repository.links.first().relationType)
    }

    @Test
    fun linkIngredientCorrectionDeletesWrongLinkAndCreatesNewOne() = runBlocking {
        val repository = FakeRecipeRepository().apply {
            links += ingredientLink(id = 9, categoryId = 1, alias = "milk")
        }
        val useCase = LinkRecipeIngredientToFoodUseCase(repository, FakeSettingsRepository())

        useCase(LinkRecipeIngredientToFoodCommand("milk", "milk", null, 2, replaceLinkId = 9))

        assertFalse(repository.links.any { it.id == 9L })
        assertTrue(repository.links.any { it.categoryId == 2L && it.normalizedAlias == "milk" })
    }
}

private class FakeFoodRecognitionRepository(
    private val result: ExternalProductResult
) : FoodRecognitionRepository {
    var lookupCount = 0

    override suspend fun lookupProductByBarcode(barcode: String): ExternalProductResult {
        lookupCount += 1
        return result
    }
}

private class FakePantryRepository : PantryRepository {
    val categories = mutableMapOf<Long, FoodCategory>()
    val barcodeLinks = mutableMapOf<String, BarcodeProductLink>()
    val activeLots = mutableListOf<LotWithCategory>()
    var matchSources: List<FoodCategoryMatchSource> = emptyList()
    var saveAddedFoodCalls = 0

    override fun observePantryRows(filter: StorageLocationFilter): Flow<List<PantryRow>> = emptyFlow()
    override fun observeFoodDetail(categoryId: Long): Flow<FoodDetailData?> = emptyFlow()
    override fun observeActiveLotsWithCategories(): Flow<List<LotWithCategory>> = flowOf(activeLots)
    override suspend fun getActiveLotsWithCategories(): List<LotWithCategory> = activeLots
    override suspend fun getActiveLotsForCategories(categoryIds: List<Long>): List<LotWithCategory> =
        activeLots.filter { it.categoryId in categoryIds }
    override suspend fun getFoodCategory(categoryId: Long): FoodCategory? = categories[categoryId]
    override suspend fun searchFoodCategories(query: String, limit: Int): List<FoodCategory> = categories.values.toList()
    override suspend fun getFoodCategoryMatchSources(query: String, limit: Int): List<FoodCategoryMatchSource> =
        matchSources.ifEmpty { categories.values.map { FoodCategoryMatchSource(it) } }
    override suspend fun findCategoryByNormalizedName(normalizedName: String): FoodCategory? =
        categories.values.firstOrNull { it.normalizedName == normalizedName }
    override suspend fun findActiveBarcodeLink(barcode: String): BarcodeProductLink? = barcodeLinks[barcode]
    override suspend fun createFoodCategory(input: CreateFoodCategoryInput): Long {
        val id = (categories.keys.maxOrNull() ?: 0L) + 1L
        categories[id] = category(id, input.name, input.normalizedName)
        return id
    }
    override suspend fun saveAddedFood(
        categorySelection: AddFoodCategorySelection,
        lots: List<AddFoodLotDraft>,
        barcodeProductDraft: BarcodeProductDraft?
    ): Long {
        saveAddedFoodCalls += 1
        return when (categorySelection) {
            is AddFoodCategorySelection.Existing -> categorySelection.categoryId
            is AddFoodCategorySelection.New -> createFoodCategory(
                CreateFoodCategoryInput(
                    categorySelection.name,
                    categorySelection.name.lowercase(),
                    categorySelection.storageLocation,
                    categorySelection.perishability
                )
            )
        }
    }
    override suspend fun saveFoodDetailChanges(draft: FoodDetailDraft) = Unit
    override suspend fun upsertExpiryLot(categoryId: Long, expirationDate: LocalDate, quantityDelta: Int) = Unit
    override suspend fun decrementSingleLotCategory(categoryId: Long): Boolean = false
    override suspend fun addRecipeIngredientAlias(categoryId: Long, aliasOriginal: String, language: String?): Long = 0
    override suspend fun removeRecipeIngredientAlias(linkId: Long) = Unit
    override suspend fun deactivateBarcodeLink(barcode: String) = Unit
}

private class FakeRecipeRepository : RecipeRepository {
    val favorites = mutableMapOf<String, RecipeDetail>()
    val links = mutableListOf<RecipeIngredientLink>()
    private var nextLinkId = 100L

    override val apiMode: PantryPalApiMode = PantryPalApiMode.MOCK
    override suspend fun searchRecipes(query: RecipeSearchQuery, allowNetwork: Boolean): RecipeSearchResult = RecipeSearchResult.Empty
    override suspend fun searchRecipesByIngredients(ingredients: List<String>, allowNetwork: Boolean): RecipeSearchResult = RecipeSearchResult.Empty
    override suspend fun getRecipeDetailResult(externalId: String, allowNetwork: Boolean): RecipeDetailResult =
        favorites[externalId]?.let(RecipeDetailResult::Success) ?: RecipeDetailResult.Empty
    override suspend fun getRecipeDetail(externalId: String): RecipeDetail? = favorites[externalId]
    override fun observeFavoriteRecipes(): Flow<List<RecipeCard>> = flowOf(favorites.values.map { it.toCard() })
    override suspend fun getFavoriteRecipeDetail(externalId: String): RecipeDetail? = favorites[externalId]
    override suspend fun saveFavoriteRecipe(recipe: RecipeDetail) {
        favorites[recipe.externalId] = recipe
    }
    override suspend fun removeFavoriteRecipe(externalId: String) {
        favorites.remove(externalId)
    }
    override suspend fun isFavorite(externalId: String): Boolean = favorites.containsKey(externalId)
    override suspend fun findIngredientLinks(normalizedAlias: String, externalIngredientId: String?): List<RecipeIngredientLink> =
        links.filter {
            it.isActive && (it.normalizedAlias == normalizedAlias || externalIngredientId != null && it.externalIngredientId == externalIngredientId)
        }
    override suspend fun getIngredientLinksForCategory(categoryId: Long): List<RecipeIngredientLink> =
        links.filter { it.categoryId == categoryId && it.isActive }
    override suspend fun linkIngredientToFood(
        aliasOriginal: String,
        normalizedAlias: String,
        externalIngredientId: String?,
        categoryId: Long,
        language: String?,
        replaceLinkId: Long?
    ): RecipeIngredientLink? {
        replaceLinkId?.let { id -> links.removeAll { it.id == id } }
        links.firstOrNull { it.normalizedAlias == normalizedAlias && it.categoryId == categoryId }?.let { return it }
        return ingredientLink(
            id = nextLinkId++,
            categoryId = categoryId,
            alias = normalizedAlias,
            externalIngredientId = externalIngredientId,
            language = language
        ).also { links += it }
    }
}

private class FakeSettingsRepository : SettingsRepository {
    private val settings = UserSettings(language = "it")
    override fun observeSettings(): Flow<UserSettings> = flowOf(settings)
    override suspend fun getSettings(): UserSettings = settings
    override suspend fun updateUsername(username: String?) = Unit
    override suspend fun updateLanguage(language: String) = Unit
    override suspend fun updateTheme(theme: AppTheme) = Unit
    override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
    override suspend fun updateFreshNotificationDays(days: Int) = Unit
    override suspend fun updateLongLifeNotificationDays(days: Int) = Unit
    override suspend fun updatePantryStorageFilter(filter: StorageLocationFilter) = Unit
    override suspend fun getSeedDataVersion(): Int = 1
    override suspend fun setSeedDataVersion(version: Int) = Unit
}

private fun category(id: Long, name: String, normalizedName: String): FoodCategory =
    FoodCategory(
        id = id,
        name = name,
        normalizedName = normalizedName,
        defaultStorageLocation = StorageLocation.FRIDGE,
        defaultPerishability = PerishabilityType.FRESH,
        imageUri = null,
        origin = CategoryOrigin.SEED,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        lastUsedAt = null
    )

private fun barcodeLink(barcode: String, categoryId: Long): BarcodeProductLink =
    BarcodeProductLink(
        barcode = barcode,
        categoryId = categoryId,
        productName = "Milk bottle",
        genericName = "milk",
        brand = "Brand",
        quantityLabel = "1L",
        imageUrl = null,
        rawCategoryTags = null,
        rawFoodGroupTags = null,
        origin = LinkOrigin.USER,
        isActive = true
    )

private fun ingredientLink(
    id: Long,
    categoryId: Long,
    alias: String,
    externalIngredientId: String? = null,
    language: String? = "it"
): RecipeIngredientLink =
    RecipeIngredientLink(
        id = id,
        categoryId = categoryId,
        categoryName = "Category $categoryId",
        aliasOriginal = alias,
        normalizedAlias = alias,
        language = language,
        externalIngredientId = externalIngredientId,
        relationType = IngredientRelationType.EXACT,
        origin = LinkOrigin.USER,
        isActive = true
    )

private fun lot(
    categoryId: Long,
    categoryName: String,
    normalizedName: String,
    quantity: Int
): LotWithCategory =
    LotWithCategory(
        lotId = categoryId,
        categoryId = categoryId,
        categoryName = categoryName,
        normalizedName = normalizedName,
        storageLocation = StorageLocation.FRIDGE,
        perishability = PerishabilityType.FRESH,
        expirationDate = LocalDate.of(2026, 7, 1),
        quantity = quantity
    )

private fun recipe(
    id: String,
    ingredient: RecipeIngredientData = RecipeIngredientData("milk", "milk", null, 1.0, "cup")
): RecipeDetail =
    RecipeDetail(
        externalId = id,
        title = "Recipe $id",
        description = null,
        preparationTimeMinutes = 10,
        servings = 2,
        imageUrl = null,
        sourceUrl = null,
        ingredients = listOf(ingredient)
    )

private fun RecipeDetail.toCard(): RecipeCard =
    RecipeCard(
        externalId = externalId,
        title = title,
        imageUrl = imageUrl,
        preparationTimeMinutes = preparationTimeMinutes,
        isFavorite = true
    )
