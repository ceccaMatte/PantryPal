package com.example.pantrypal.data.pantry

import androidx.room.withTransaction
import com.example.pantrypal.core.database.PantryPalDatabase
import com.example.pantrypal.core.database.dao.BarcodeProductLinkDao
import com.example.pantrypal.core.database.dao.ExpiryLotDao
import com.example.pantrypal.core.database.dao.FoodCategoryDao
import com.example.pantrypal.core.database.dao.RecipeIngredientLinkDao
import com.example.pantrypal.core.database.entity.ExpiryLotEntity
import com.example.pantrypal.core.database.entity.FoodCategoryEntity
import com.example.pantrypal.domain.model.CategoryOrigin
import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodDetailData
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
    private val recipeIngredientLinkDao: RecipeIngredientLinkDao
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
        val categoryFlow = flow { emit(foodCategoryDao.getById(categoryId)?.toDomain()) }
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

    override suspend fun searchFoodCategories(query: String, limit: Int): List<FoodCategory> {
        val normalizedQuery = query.trim().lowercase()
        return if (normalizedQuery.isBlank()) {
            foodCategoryDao.getAllCategories(limit)
        } else {
            foodCategoryDao.searchCategories(normalizedQuery, limit)
        }.map { it.toDomain() }
    }

    override suspend fun getFoodCategoryMatchSources(query: String, limit: Int): List<FoodCategoryMatchSource> {
        val categories = searchFoodCategories(query, limit)
        return categories.map { category ->
            FoodCategoryMatchSource(category = category)
        }
    }

    override suspend fun findCategoryByNormalizedName(normalizedName: String): FoodCategory? =
        foodCategoryDao.getByNormalizedName(normalizedName)?.toDomain()

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

    override suspend fun upsertExpiryLot(
        categoryId: Long,
        expirationDate: LocalDate,
        quantityDelta: Int
    ) {
        if (quantityDelta <= 0) return
        val now = Instant.now()
        database.withTransaction {
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

    private fun StorageLocationFilter.toStorageLocationOrNull(): StorageLocation? =
        when (this) {
            StorageLocationFilter.ALL -> null
            StorageLocationFilter.FRIDGE -> StorageLocation.FRIDGE
            StorageLocationFilter.FREEZER -> StorageLocation.FREEZER
            StorageLocationFilter.PANTRY -> StorageLocation.PANTRY
        }
}
