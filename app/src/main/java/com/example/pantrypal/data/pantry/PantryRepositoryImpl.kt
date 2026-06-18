package com.example.pantrypal.data.pantry

import androidx.room.withTransaction
import com.example.pantrypal.core.database.PantryPalDatabase
import com.example.pantrypal.core.database.dao.BarcodeProductLinkDao
import com.example.pantrypal.core.database.dao.ExpiryLotDao
import com.example.pantrypal.core.database.dao.FoodCategoryDao
import com.example.pantrypal.core.database.dao.RecipeIngredientLinkDao
import com.example.pantrypal.core.database.entity.BarcodeProductLinkEntity
import com.example.pantrypal.core.database.entity.ExpiryLotEntity
import com.example.pantrypal.core.database.entity.FoodCategoryEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientLinkEntity
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.AddFoodLotDraft
import com.example.pantrypal.domain.model.BarcodeProductDraft
import com.example.pantrypal.domain.model.BarcodeProductLink
import com.example.pantrypal.domain.model.CategoryOrigin
import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodDetailDraft
import com.example.pantrypal.domain.model.FoodDetailData
import com.example.pantrypal.domain.model.IngredientRelationType
import com.example.pantrypal.domain.model.LinkOrigin
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Singleton
class PantryRepositoryImpl @Inject constructor(
    private val database: PantryPalDatabase,
    private val foodCategoryDao: FoodCategoryDao,
    private val expiryLotDao: ExpiryLotDao,
    private val barcodeProductLinkDao: BarcodeProductLinkDao,
    private val recipeIngredientLinkDao: RecipeIngredientLinkDao,
    private val textNormalizer: TextNormalizer
) : PantryRepository {
    override fun observePantryRows(filter: StorageLocationFilter): Flow<List<PantryRow>> {
        val location = filter.toStorageLocationOrNull()
        return if (location == null) {
            foodCategoryDao.observePantryRowsAll()
        } else {
            foodCategoryDao.observePantryRowsByLocation(location)
        }.map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeFoodDetail(categoryId: Long): Flow<FoodDetailData?> {
        val categoryFlow = foodCategoryDao.observeById(categoryId).map { it?.toDomain() }
        val lotsFlow = expiryLotDao.observeActiveLotsForCategory(categoryId)
            .map { lots -> lots.map { it.toDomain() } }
        val barcodeLinksFlow = barcodeProductLinkDao.observeActiveLinksForCategory(categoryId)
            .map { links -> links.map { it.toDomain() } }
        val recipeLinksFlow = recipeIngredientLinkDao.observeActiveLinksForCategory(categoryId)
            .map { links -> links.map { it.toDomain() } }

        return combine(categoryFlow, lotsFlow, barcodeLinksFlow, recipeLinksFlow) { category, lots, barcodeLinks, recipeLinks ->
            category?.let {
                FoodDetailData(
                    category = it,
                    lots = lots,
                    barcodeLinks = barcodeLinks,
                    recipeIngredientLinks = recipeLinks
                )
            }
        }
    }

    override suspend fun getActiveLotsWithCategories(): List<LotWithCategory> =
        foodCategoryDao.getActiveLotsWithCategories().map { it.toDomain() }

    override suspend fun getActiveLotsForCategories(categoryIds: List<Long>): List<LotWithCategory> =
        if (categoryIds.isEmpty()) {
            emptyList()
        } else {
            foodCategoryDao.getActiveLotsForCategories(categoryIds).map { it.toDomain() }
        }

    override suspend fun getFoodCategory(categoryId: Long): FoodCategory? =
        foodCategoryDao.getById(categoryId)?.toDomain()

    override fun observeActiveLotsWithCategories(): Flow<List<LotWithCategory>> =
        foodCategoryDao.observeActiveLotsWithCategories().map { lots -> lots.map { it.toDomain() } }

    override suspend fun searchFoodCategories(query: String, limit: Int): List<FoodCategory> {
        val normalizedQuery = textNormalizer.normalizeFoodText(query)
        return if (normalizedQuery.isBlank()) {
            foodCategoryDao.getAllCategories(limit)
        } else {
            foodCategoryDao.searchCategories(normalizedQuery, limit)
        }.map { it.toDomain() }
    }

    override suspend fun getFoodCategoryMatchSources(query: String, limit: Int): List<FoodCategoryMatchSource> {
        val normalizedQuery = textNormalizer.normalizeFoodText(query)
        val categoryMatches = searchFoodCategories(query, limit)
        val aliasMatches = if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            recipeIngredientLinkDao.searchActiveLinkProjections(normalizedQuery, limit)
        }
        val categoryIds = (categoryMatches.map { it.id } + aliasMatches.map { it.categoryId }).distinct()
        if (categoryIds.isEmpty()) return emptyList()

        val categoriesById = (categoryMatches + foodCategoryDao.getByIds(categoryIds).map { it.toDomain() })
            .distinctBy { it.id }
            .associateBy { it.id }
        val aliasesByCategory = recipeIngredientLinkDao.getActiveLinksForCategories(categoryIds)
            .map { it.toDomain() }
            .groupBy { it.categoryId }

        return categoryIds.mapNotNull { categoryId ->
            categoriesById[categoryId]?.let { category ->
                FoodCategoryMatchSource(category = category, aliases = aliasesByCategory[categoryId].orEmpty())
            }
        }
    }

    override suspend fun findCategoryByNormalizedName(normalizedName: String): FoodCategory? =
        foodCategoryDao.getByNormalizedName(normalizedName)?.toDomain()

    override suspend fun findActiveBarcodeLink(barcode: String): BarcodeProductLink? =
        barcodeProductLinkDao.findActiveByBarcode(barcode)?.toDomain()

    override suspend fun createFoodCategory(input: CreateFoodCategoryInput): Long {
        val now = Instant.now()
        return database.withTransaction {
            val existing = foodCategoryDao.getByNormalizedName(input.normalizedName)
            existing?.id ?: foodCategoryDao.insert(
                FoodCategoryEntity(
                    name = input.name,
                    normalizedName = input.normalizedName,
                    defaultStorageLocation = input.defaultStorageLocation,
                    defaultPerishability = input.defaultPerishability,
                    imageUri = input.imageUri,
                    origin = CategoryOrigin.USER,
                    createdAt = now,
                    updatedAt = now,
                    lastUsedAt = null
                )
            )
        }
    }

    override suspend fun saveAddedFood(
        categorySelection: AddFoodCategorySelection,
        lots: List<AddFoodLotDraft>,
        barcodeProductDraft: BarcodeProductDraft?
    ): Long {
        val now = Instant.now()
        return database.withTransaction {
            val categoryId = when (categorySelection) {
                is AddFoodCategorySelection.Existing -> categorySelection.categoryId
                is AddFoodCategorySelection.New -> {
                    val normalizedName = textNormalizer.normalizeFoodText(categorySelection.name)
                    val existing = foodCategoryDao.getByNormalizedName(normalizedName)
                    existing?.id ?: foodCategoryDao.insert(
                        FoodCategoryEntity(
                            name = categorySelection.name.trim(),
                            normalizedName = normalizedName,
                            defaultStorageLocation = categorySelection.storageLocation,
                            defaultPerishability = categorySelection.perishability,
                            imageUri = null,
                            origin = CategoryOrigin.USER,
                            createdAt = now,
                            updatedAt = now,
                            lastUsedAt = null
                        )
                    )
                }
            }
            lots
                .filter { it.quantity > 0 && it.expirationDate != null }
                .groupBy { requireNotNull(it.expirationDate) }
                .mapValues { (_, dateLots) -> dateLots.sumOf { it.quantity } }
                .forEach { (expirationDate, quantity) ->
                    upsertExpiryLotInTransaction(categoryId, expirationDate, quantity, now)
                }
            barcodeProductDraft?.let { draft ->
                barcodeProductLinkDao.upsert(
                    BarcodeProductLinkEntity(
                        barcode = draft.barcode,
                        categoryId = categoryId,
                        productName = draft.productName,
                        genericName = draft.genericName,
                        brand = draft.brand,
                        quantityLabel = draft.quantityLabel,
                        imageUrl = draft.imageUrl,
                        rawCategoryTags = draft.rawCategoryTags.joinToString("|").ifBlank { null },
                        rawFoodGroupTags = draft.rawFoodGroupTags.joinToString("|").ifBlank { null },
                        origin = LinkOrigin.USER,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now,
                        lastUsedAt = now
                    )
                )
            }
            categoryId
        }
    }

    override suspend fun updateFoodCategoryImageIfEmpty(categoryId: Long, imageUri: String): Boolean =
        foodCategoryDao.updateImageUriIfEmpty(categoryId, imageUri, Instant.now()) > 0

    override suspend fun saveFoodDetailChanges(draft: FoodDetailDraft) {
        val now = Instant.now()
        database.withTransaction {
            val currentCategory = foodCategoryDao.getById(draft.categoryId) ?: return@withTransaction
            val normalizedName = textNormalizer.normalizeFoodText(draft.name)
            foodCategoryDao.updateCategoryDetails(
                categoryId = draft.categoryId,
                name = draft.name.trim(),
                normalizedName = normalizedName,
                storageLocation = draft.storageLocation,
                perishability = draft.perishability,
                imageUri = currentCategory.imageUri,
                updatedAt = now
            )

            val finalLotsByDate = draft.lots
                .filter { it.quantity > 0 }
                .groupBy { it.expirationDate }
                .mapValues { (_, lots) -> lots.sumOf { it.quantity } }

            expiryLotDao.deleteAllLotsForCategory(draft.categoryId)
            finalLotsByDate.forEach { (date, quantity) ->
                expiryLotDao.insert(
                    ExpiryLotEntity(
                        categoryId = draft.categoryId,
                        expirationDate = date,
                        quantity = quantity,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            foodCategoryDao.markUsed(draft.categoryId, now)
        }
    }

    override suspend fun upsertExpiryLot(
        categoryId: Long,
        expirationDate: LocalDate,
        quantityDelta: Int
    ) {
        if (quantityDelta <= 0) return
        val now = Instant.now()
        database.withTransaction {
            upsertExpiryLotInTransaction(categoryId, expirationDate, quantityDelta, now)
        }
    }

    override suspend fun decrementSingleLotCategory(categoryId: Long): Boolean {
        val now = Instant.now()
        return database.withTransaction {
            val lots = expiryLotDao.getActiveLotsForCategory(categoryId)
            if (lots.size != 1) return@withTransaction false
            val lot = lots.first()
            if (lot.quantity <= 1) {
                expiryLotDao.deleteById(lot.id)
            } else {
                expiryLotDao.decrementLotByOne(lot.id, now)
            }
            expiryLotDao.deleteZeroQuantityLots()
            true
        }
    }

    override suspend fun addRecipeIngredientAlias(categoryId: Long, aliasOriginal: String, language: String?): Long {
        val normalizedAlias = textNormalizer.normalizeFoodText(aliasOriginal)
        if (normalizedAlias.isBlank()) return 0L
        val now = Instant.now()
        return database.withTransaction {
            val existing = recipeIngredientLinkDao.getLinkByAliasAndCategory(normalizedAlias, categoryId)
            if (existing != null) {
                recipeIngredientLinkDao.upsert(
                    existing.copy(
                        aliasOriginal = aliasOriginal.trim(),
                        language = language?.takeIf { it.isNotBlank() },
                        relationType = IngredientRelationType.EXACT,
                        origin = LinkOrigin.USER,
                        isActive = true,
                        updatedAt = now
                    )
                )
            } else {
                recipeIngredientLinkDao.upsert(
                    RecipeIngredientLinkEntity(
                        categoryId = categoryId,
                        aliasOriginal = aliasOriginal.trim(),
                        normalizedAlias = normalizedAlias,
                        language = language?.takeIf { it.isNotBlank() },
                        externalIngredientId = null,
                        relationType = IngredientRelationType.EXACT,
                        origin = LinkOrigin.USER,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }

    override suspend fun removeRecipeIngredientAlias(linkId: Long) {
        recipeIngredientLinkDao.deleteById(linkId)
    }

    override suspend fun deactivateBarcodeLink(barcode: String) {
        barcodeProductLinkDao.deactivateByBarcode(barcode, Instant.now())
    }

    private suspend fun upsertExpiryLotInTransaction(
        categoryId: Long,
        expirationDate: LocalDate,
        quantityDelta: Int,
        now: Instant
    ) {
        val existing = expiryLotDao.getLotByCategoryAndDate(categoryId, expirationDate)
        if (existing == null) {
            expiryLotDao.insert(
                ExpiryLotEntity(
                    categoryId = categoryId,
                    expirationDate = expirationDate,
                    quantity = quantityDelta,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            expiryLotDao.update(existing.copy(quantity = existing.quantity + quantityDelta, updatedAt = now))
        }
        foodCategoryDao.markUsed(categoryId, now)
    }

    private fun StorageLocationFilter.toStorageLocationOrNull(): StorageLocation? =
        when (this) {
            StorageLocationFilter.ALL -> null
            StorageLocationFilter.FRIDGE -> StorageLocation.FRIDGE
            StorageLocationFilter.FREEZER -> StorageLocation.FREEZER
            StorageLocationFilter.PANTRY -> StorageLocation.PANTRY
        }
}
