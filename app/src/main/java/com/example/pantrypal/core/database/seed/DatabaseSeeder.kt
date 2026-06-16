package com.example.pantrypal.core.database.seed

import androidx.room.withTransaction
import com.example.pantrypal.core.database.PantryPalDatabase
import com.example.pantrypal.core.database.dao.FoodCategoryDao
import com.example.pantrypal.core.database.dao.RecipeIngredientLinkDao
import com.example.pantrypal.core.database.entity.FoodCategoryEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientLinkEntity
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.CategoryOrigin
import com.example.pantrypal.domain.model.LinkOrigin
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: PantryPalDatabase,
    private val foodCategoryDao: FoodCategoryDao,
    private val recipeIngredientLinkDao: RecipeIngredientLinkDao,
    private val settingsRepository: SettingsRepository,
    private val seedDataLoader: SeedDataLoader,
    private val textNormalizer: TextNormalizer
) {
    suspend fun seedIfNeeded() {
        if (settingsRepository.getSeedDataVersion() >= CURRENT_SEED_VERSION) return

        database.withTransaction {
            seedFoodCategories()
            seedRecipeIngredientLinks()
        }

        settingsRepository.setSeedDataVersion(CURRENT_SEED_VERSION)
    }

    private suspend fun seedFoodCategories() {
        val now = Instant.now()
        seedDataLoader.loadFoodCategories().forEach { seed ->
            val normalized = textNormalizer.normalizeFoodText(seed.name)
            val existing = foodCategoryDao.getByNormalizedName(normalized)
            if (existing == null) {
                foodCategoryDao.insert(
                    FoodCategoryEntity(
                        name = seed.name,
                        normalizedName = normalized,
                        defaultStorageLocation = seed.defaultStorageLocation,
                        defaultPerishability = seed.defaultPerishability,
                        imageUri = null,
                        origin = CategoryOrigin.SEED,
                        createdAt = now,
                        updatedAt = now,
                        lastUsedAt = null
                    )
                )
            }
        }
    }

    private suspend fun seedRecipeIngredientLinks() {
        val now = Instant.now()
        seedDataLoader.loadRecipeIngredientLinks().forEach { seed ->
            val categoryNormalized = textNormalizer.normalizeFoodText(seed.categoryNormalizedName)
            val category = foodCategoryDao.getByNormalizedName(categoryNormalized) ?: return@forEach
            val normalizedAlias = textNormalizer.normalizeFoodText(seed.aliasOriginal)
            val existing = recipeIngredientLinkDao.getLinkByAliasAndCategory(
                normalizedAlias = normalizedAlias,
                categoryId = category.id
            )
            if (existing == null) {
                recipeIngredientLinkDao.upsert(
                    RecipeIngredientLinkEntity(
                        categoryId = category.id,
                        aliasOriginal = seed.aliasOriginal,
                        normalizedAlias = normalizedAlias,
                        language = seed.language,
                        externalIngredientId = seed.externalIngredientId,
                        relationType = seed.relationType,
                        origin = LinkOrigin.SEED,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }

    companion object {
        const val CURRENT_SEED_VERSION = 1
    }
}
