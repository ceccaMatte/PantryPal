package com.example.pantrypal.data.recipe

import androidx.room.withTransaction
import com.example.pantrypal.core.database.PantryPalDatabase
import com.example.pantrypal.core.database.dao.RecipeDao
import com.example.pantrypal.core.database.dao.RecipeIngredientLinkDao
import com.example.pantrypal.core.database.entity.FavoriteRecipeEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientLinkEntity
import com.example.pantrypal.data.pantry.toDomain
import com.example.pantrypal.data.recipe.cache.RecipeCacheStore
import com.example.pantrypal.data.recipe.source.MockRecipeRemoteDataSource
import com.example.pantrypal.data.recipe.source.SpoonacularRecipeRemoteDataSource
import com.example.pantrypal.domain.model.IngredientRelationType
import com.example.pantrypal.domain.model.LinkOrigin
import com.example.pantrypal.domain.model.PantryPalApiMode
import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetail
import com.example.pantrypal.domain.model.RecipeDetailResult
import com.example.pantrypal.domain.model.RecipeIngredientData
import com.example.pantrypal.domain.model.RecipeIngredientLink
import com.example.pantrypal.domain.model.RecipeSearchQuery
import com.example.pantrypal.domain.model.RecipeSearchResult
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val database: PantryPalDatabase,
    private val recipeDao: RecipeDao,
    private val recipeIngredientLinkDao: RecipeIngredientLinkDao,
    private val apiModeProvider: ApiModeProvider,
    private val recipeCacheStore: RecipeCacheStore,
    private val spoonacularDataSource: SpoonacularRecipeRemoteDataSource,
    private val mockDataSource: MockRecipeRemoteDataSource
) : RecipeRepository {
    override val apiMode: PantryPalApiMode
        get() = apiModeProvider.mode

    override suspend fun searchRecipes(
        query: RecipeSearchQuery,
        allowNetwork: Boolean
    ): RecipeSearchResult {
        if (query.query.isBlank()) return RecipeSearchResult.Empty
        return when (apiMode) {
            PantryPalApiMode.MOCK -> mockDataSource.searchRecipes(query.query).withFavoriteState()
            PantryPalApiMode.CACHE_ONLY -> recipeCacheStore.getSearch(query.query)
                ?.toSuccessWithFavorites()
                ?: RecipeSearchResult.Empty
            PantryPalApiMode.REAL -> {
                recipeCacheStore.getSearch(query.query)?.let { return it.toSuccessWithFavorites() }
                if (!allowNetwork) return RecipeSearchResult.Empty
                when (val result = spoonacularDataSource.searchRecipes(query.query)) {
                    is RecipeSearchResult.Success -> {
                        recipeCacheStore.putSearch(query.query, result.recipes)
                        result.withFavoriteState()
                    }
                    else -> result
                }
            }
        }
    }

    override suspend fun searchRecipesByIngredients(
        ingredients: List<String>,
        allowNetwork: Boolean
    ): RecipeSearchResult {
        if (ingredients.isEmpty()) return RecipeSearchResult.Empty
        return when (apiMode) {
            PantryPalApiMode.MOCK -> mockDataSource.findRecipesByIngredients(ingredients).withFavoriteState()
            PantryPalApiMode.CACHE_ONLY -> recipeCacheStore.getByIngredients(ingredients)
                ?.toSuccessWithFavorites()
                ?: RecipeSearchResult.Empty
            PantryPalApiMode.REAL -> {
                recipeCacheStore.getByIngredients(ingredients)?.let { return it.toSuccessWithFavorites() }
                if (!allowNetwork) return RecipeSearchResult.Empty
                when (val result = spoonacularDataSource.findRecipesByIngredients(ingredients)) {
                    is RecipeSearchResult.Success -> {
                        recipeCacheStore.putByIngredients(ingredients, result.recipes)
                        result.withFavoriteState()
                    }
                    else -> result
                }
            }
        }
    }

    override suspend fun getRecipeDetailResult(
        externalId: String,
        allowNetwork: Boolean
    ): RecipeDetailResult {
        getFavoriteRecipeDetail(externalId)?.let { return RecipeDetailResult.Success(it) }
        return when (apiMode) {
            PantryPalApiMode.MOCK -> mockDataSource.getRecipeDetail(externalId)
            PantryPalApiMode.CACHE_ONLY -> recipeCacheStore.getDetail(externalId)
                ?.let(RecipeDetailResult::Success)
                ?: RecipeDetailResult.Empty
            PantryPalApiMode.REAL -> {
                recipeCacheStore.getDetail(externalId)?.let { return RecipeDetailResult.Success(it) }
                if (!allowNetwork) return RecipeDetailResult.Empty
                when (val result = spoonacularDataSource.getRecipeDetail(externalId)) {
                    is RecipeDetailResult.Success -> {
                        recipeCacheStore.putDetail(result.recipe)
                        result
                    }
                    else -> result
                }
            }
        }
    }

    override suspend fun getRecipeDetail(externalId: String): RecipeDetail? =
        (getRecipeDetailResult(externalId) as? RecipeDetailResult.Success)?.recipe

    override fun observeFavoriteRecipes(): Flow<List<RecipeCard>> =
        recipeDao.observeFavoriteRecipeCards().map { cards ->
            cards.map {
                RecipeCard(
                    externalId = it.externalId,
                    title = it.title,
                    imageUrl = it.imageUrl,
                    preparationTimeMinutes = it.preparationTimeMinutes,
                    isFavorite = true
                )
            }
        }

    override suspend fun getFavoriteRecipeDetail(externalId: String): RecipeDetail? {
        val recipe = recipeDao.getFavoriteByExternalId(externalId) ?: return null
        val ingredients = recipeDao.getIngredientsForRecipe(recipe.id).map {
            RecipeIngredientData(
                originalName = it.originalName,
                normalizedName = it.normalizedName,
                externalIngredientId = it.externalIngredientId,
                amount = it.amount,
                unit = it.unit,
                cleanName = it.originalName,
                displayAmount = listOfNotNull(
                    it.amount?.let { amount -> if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString() },
                    it.unit
                ).joinToString(" ").ifBlank { null },
                originalText = it.originalName
            )
        }
        return RecipeDetail(
            externalId = recipe.externalId,
            title = recipe.title,
            description = recipe.description,
            preparationTimeMinutes = recipe.preparationTimeMinutes,
            servings = recipe.servings,
            imageUrl = recipe.imageUrl,
            sourceUrl = recipe.sourceUrl,
            ingredients = ingredients
        )
    }

    override suspend fun saveFavoriteRecipe(recipe: RecipeDetail) {
        val now = Instant.now()
        database.withTransaction {
            val recipeId = recipeDao.upsertFavoriteRecipe(
                FavoriteRecipeEntity(
                    externalId = recipe.externalId,
                    title = recipe.title,
                    description = recipe.description,
                    preparationTimeMinutes = recipe.preparationTimeMinutes,
                    servings = recipe.servings,
                    imageUrl = recipe.imageUrl,
                    sourceUrl = recipe.sourceUrl,
                    savedAt = now,
                    updatedAt = now
                )
            )
            recipeDao.deleteIngredientsForRecipe(recipeId)
            recipeDao.insertIngredients(
                recipe.ingredients.map {
                    RecipeIngredientEntity(
                        recipeId = recipeId,
                        originalName = it.cleanName,
                        normalizedName = it.normalizedName,
                        externalIngredientId = it.externalIngredientId,
                        amount = it.amount,
                        unit = it.unit
                    )
                }
            )
        }
    }

    override suspend fun removeFavoriteRecipe(externalId: String) {
        recipeDao.deleteFavoriteByExternalId(externalId)
    }

    override suspend fun isFavorite(externalId: String): Boolean =
        recipeDao.isFavoriteCount(externalId) > 0

    override suspend fun findIngredientLinks(
        normalizedAlias: String,
        externalIngredientId: String?
    ): List<RecipeIngredientLink> {
        val byAlias = recipeIngredientLinkDao.findActiveLinksByAlias(normalizedAlias)
        val byExternalId = externalIngredientId
            ?.let { recipeIngredientLinkDao.findActiveLinksByExternalIngredientId(it) }
            .orEmpty()
        return (byExternalId + byAlias).distinctBy { it.id }.map { it.toDomain() }
    }

    override suspend fun getIngredientLinksForCategory(categoryId: Long): List<RecipeIngredientLink> =
        recipeIngredientLinkDao.observeActiveLinksForCategory(categoryId)
            .map { links -> links.map { it.toDomain() } }
            .first()

    override suspend fun linkIngredientToFood(
        aliasOriginal: String,
        normalizedAlias: String,
        externalIngredientId: String?,
        categoryId: Long,
        language: String?,
        replaceLinkId: Long?
    ): RecipeIngredientLink? {
        if (normalizedAlias.isBlank()) return null
        val now = Instant.now()
        return database.withTransaction {
            replaceLinkId?.let { recipeIngredientLinkDao.deleteById(it) }
            val existing = recipeIngredientLinkDao.getLinkByAliasAndCategory(normalizedAlias, categoryId)
            if (existing == null) {
                recipeIngredientLinkDao.upsert(
                    RecipeIngredientLinkEntity(
                        categoryId = categoryId,
                        aliasOriginal = aliasOriginal.trim(),
                        normalizedAlias = normalizedAlias,
                        language = language?.takeIf { it.isNotBlank() },
                        externalIngredientId = externalIngredientId,
                        relationType = IngredientRelationType.EXACT,
                        origin = LinkOrigin.USER,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                recipeIngredientLinkDao.upsert(
                    existing.copy(
                        aliasOriginal = aliasOriginal.trim(),
                        language = language?.takeIf { it.isNotBlank() },
                        externalIngredientId = externalIngredientId ?: existing.externalIngredientId,
                        relationType = IngredientRelationType.EXACT,
                        origin = LinkOrigin.USER,
                        isActive = true,
                        updatedAt = now
                    )
                )
            }
            recipeIngredientLinkDao.findActiveLinksByAlias(normalizedAlias)
                .firstOrNull { it.categoryId == categoryId }
                ?.toDomain()
        }
    }

    private suspend fun RecipeSearchResult.withFavoriteState(): RecipeSearchResult =
        when (this) {
            is RecipeSearchResult.Success -> recipes.toSuccessWithFavorites()
            else -> this
        }

    private suspend fun List<RecipeCard>.toSuccessWithFavorites(): RecipeSearchResult.Success {
        val favoriteIds = recipeDao.observeFavoriteRecipeCards().first().map { it.externalId }.toSet()
        return RecipeSearchResult.Success(map { it.copy(isFavorite = it.externalId in favoriteIds) })
    }
}
