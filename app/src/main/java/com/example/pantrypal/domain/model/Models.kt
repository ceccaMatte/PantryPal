package com.example.pantrypal.domain.model

import java.time.Instant
import java.time.LocalDate

enum class StorageLocation {
    FRIDGE,
    FREEZER,
    PANTRY
}

enum class StorageLocationFilter {
    ALL,
    FRIDGE,
    FREEZER,
    PANTRY
}

enum class PerishabilityType {
    FRESH,
    LONG_LIFE
}

enum class CategoryOrigin {
    SEED,
    USER
}

enum class LinkOrigin {
    SEED,
    AUTO,
    USER
}

enum class IngredientRelationType {
    EXACT,
    COMPATIBLE,
    BROADER,
    FALLBACK
}

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

data class UserSettings(
    val username: String = "",
    val language: String = "it",
    val theme: AppTheme = AppTheme.SYSTEM,
    val expirationNotificationsEnabled: Boolean = false,
    val freshNotificationDays: Int = 2,
    val longLifeNotificationDays: Int = 7,
    val pantryStorageFilter: StorageLocationFilter = StorageLocationFilter.ALL,
    val seedDataVersion: Int = 0
)

data class FoodCategory(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val defaultStorageLocation: StorageLocation,
    val defaultPerishability: PerishabilityType,
    val imageUri: String?,
    val origin: CategoryOrigin,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastUsedAt: Instant?
)

data class PantryRow(
    val categoryId: Long,
    val name: String,
    val imageUri: String?,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val totalQuantity: Int,
    val nearestExpirationDate: LocalDate?,
    val lotCount: Int
)

data class ExpiringFoodRow(
    val categoryId: Long,
    val name: String,
    val imageUri: String?,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val expiringQuantity: Int,
    val nearestExpirationDate: LocalDate
)

data class LotWithCategory(
    val lotId: Long,
    val categoryId: Long,
    val categoryName: String,
    val normalizedName: String,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val expirationDate: LocalDate,
    val quantity: Int
)

data class ExpiryLot(
    val id: Long,
    val categoryId: Long,
    val expirationDate: LocalDate,
    val quantity: Int
)

data class BarcodeProductLink(
    val barcode: String,
    val categoryId: Long,
    val productName: String,
    val genericName: String?,
    val brand: String?,
    val quantityLabel: String?,
    val imageUrl: String?,
    val rawCategoryTags: String?,
    val rawFoodGroupTags: String?,
    val origin: LinkOrigin,
    val isActive: Boolean
)

data class BarcodeProductDraft(
    val barcode: String,
    val productName: String,
    val genericName: String?,
    val brand: String?,
    val quantityLabel: String?,
    val imageUrl: String?,
    val rawCategoryTags: List<String>,
    val rawFoodGroupTags: List<String>
)

data class RecipeIngredientLink(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val aliasOriginal: String,
    val normalizedAlias: String,
    val language: String?,
    val externalIngredientId: String?,
    val relationType: IngredientRelationType,
    val origin: LinkOrigin,
    val isActive: Boolean
)

data class FoodDetailData(
    val category: FoodCategory,
    val lots: List<ExpiryLot>,
    val barcodeLinks: List<BarcodeProductLink>,
    val recipeIngredientLinks: List<RecipeIngredientLink>
)

data class CreateFoodCategoryInput(
    val name: String,
    val normalizedName: String,
    val defaultStorageLocation: StorageLocation,
    val defaultPerishability: PerishabilityType,
    val imageUri: String? = null
)

data class FoodCategoryMatchSource(
    val category: FoodCategory,
    val aliases: List<RecipeIngredientLink> = emptyList()
)

data class FoodCategorySuggestion(
    val categoryId: Long,
    val name: String,
    val score: Int,
    val reason: String
)

data class RecognizedBarcodeProduct(
    val barcode: String,
    val productName: String?,
    val genericName: String?,
    val brand: String?,
    val quantityLabel: String?,
    val imageUrl: String?,
    val rawCategoryTags: List<String>,
    val rawFoodGroupTags: List<String>
)

fun RecognizedBarcodeProduct.toBarcodeProductDraft(): BarcodeProductDraft =
    BarcodeProductDraft(
        barcode = barcode,
        productName = productName?.takeIf { it.isNotBlank() }
            ?: genericName?.takeIf { it.isNotBlank() }
            ?: barcode,
        genericName = genericName,
        brand = brand,
        quantityLabel = quantityLabel,
        imageUrl = imageUrl,
        rawCategoryTags = rawCategoryTags,
        rawFoodGroupTags = rawFoodGroupTags
    )

sealed interface ExternalProductResult {
    data class Found(val product: RecognizedBarcodeProduct) : ExternalProductResult
    data object NotFound : ExternalProductResult
    data object NetworkError : ExternalProductResult
    data object InvalidResponse : ExternalProductResult
    data object RateLimited : ExternalProductResult
}

sealed interface BarcodeResolution {
    data class KnownLocal(
        val link: BarcodeProductLink,
        val category: FoodCategory
    ) : BarcodeResolution

    data class FoundRemote(
        val product: RecognizedBarcodeProduct,
        val suggestions: List<FoodCategorySuggestion>,
        val preselectedCategoryId: Long?
    ) : BarcodeResolution

    data object NotFound : BarcodeResolution
    data object NetworkError : BarcodeResolution
    data object InvalidResponse : BarcodeResolution
    data object RateLimited : BarcodeResolution
}

data class RecipeCard(
    val externalId: String,
    val title: String,
    val imageUrl: String?,
    val preparationTimeMinutes: Int?,
    val isFavorite: Boolean = false
)

data class RecipeSearchQuery(val query: String)

sealed interface RecipeSearchResult {
    data class Success(val recipes: List<RecipeCard>) : RecipeSearchResult
    data object Empty : RecipeSearchResult
    data object ConfigMissing : RecipeSearchResult
    data object Offline : RecipeSearchResult
    data object Error : RecipeSearchResult
}

data class RecipeDetail(
    val externalId: String,
    val title: String,
    val description: String?,
    val preparationTimeMinutes: Int?,
    val servings: Int?,
    val imageUrl: String?,
    val sourceUrl: String?,
    val ingredients: List<RecipeIngredientData>
)

data class RecipeIngredientData(
    val originalName: String,
    val normalizedName: String,
    val externalIngredientId: String?,
    val amount: Double?,
    val unit: String?
)

enum class RecipeAvailabilityStatus {
    IN_PANTRY,
    TO_BUY
}

data class RecipeIngredientAvailabilityItem(
    val ingredient: RecipeIngredientData,
    val status: RecipeAvailabilityStatus,
    val linkedCategories: List<FoodCategory>,
    val matchingLinks: List<RecipeIngredientLink>,
    val totalAvailableQuantity: Int
)

data class RecipeAvailability(
    val items: List<RecipeIngredientAvailabilityItem>
)

data class ExpirationNotificationContent(
    val title: String,
    val body: String
)
