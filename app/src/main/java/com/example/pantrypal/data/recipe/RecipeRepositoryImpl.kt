package com.example.pantrypal.data.recipe

import androidx.room.withTransaction
import com.example.pantrypal.BuildConfig
import com.example.pantrypal.core.database.PantryPalDatabase
import com.example.pantrypal.core.database.dao.RecipeDao
import com.example.pantrypal.core.database.dao.RecipeIngredientLinkDao
import com.example.pantrypal.core.database.entity.FavoriteRecipeEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientLinkEntity
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.pantry.toDomain
import com.example.pantrypal.data.recipe.remote.SpoonacularApi
import com.example.pantrypal.domain.model.IngredientRelationType
import com.example.pantrypal.domain.model.LinkOrigin
import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetail
import com.example.pantrypal.domain.model.RecipeIngredientData
import com.example.pantrypal.domain.model.RecipeIngredientLink
import com.example.pantrypal.domain.model.RecipeSearchQuery
import com.example.pantrypal.domain.model.RecipeSearchResult
import java.io.IOException
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
    private val spoonacularApi: SpoonacularApi,
    private val textNormalizer: TextNormalizer
) : RecipeRepository {
    override suspend fun searchRecipes(query: RecipeSearchQuery): RecipeSearchResult {
        if (query.query.isBlank()) return RecipeSearchResult.Empty
        if (BuildConfig.SPOONACULAR_API_KEY.isBlank()) return RecipeSearchResult.ConfigMissing

        return try {
            val response = spoonacularApi.searchRecipes(query.query)
            val recipes = response.results.map {
                RecipeCard(
                    externalId = it.id.toString(),
                    title = it.title,
                    imageUrl = it.image,
                    preparationTimeMinutes = null,
                    isFavorite = isFavorite(it.id.toString())
                )
            }
            if (recipes.isEmpty()) RecipeSearchResult.Empty else RecipeSearchResult.Success(recipes)
        } catch (_: IOException) {
            RecipeSearchResult.Offline
        } catch (_: Exception) {
            RecipeSearchResult.Error
        }
    }

    override suspend fun searchRecipesByIngredients(ingredients: List<String>): RecipeSearchResult {
        if (ingredients.isEmpty()) return RecipeSearchResult.Empty
        if (BuildConfig.SPOONACULAR_API_KEY.isBlank()) return RecipeSearchResult.ConfigMissing

        return try {
            val recipes = spoonacularApi.findRecipesByIngredients(ingredients.joinToString(","))
                .map {
                    RecipeCard(
                        externalId = it.id.toString(),
                        title = it.title,
                        imageUrl = it.image,
                        preparationTimeMinutes = null,
                        isFavorite = isFavorite(it.id.toString())
                    )
                }
            if (recipes.isEmpty()) RecipeSearchResult.Empty else RecipeSearchResult.Success(recipes)
        } catch (_: IOException) {
            RecipeSearchResult.Offline
        } catch (_: Exception) {
            RecipeSearchResult.Error
        }
    }

    override suspend fun getRecipeDetail(externalId: String): RecipeDetail? {
        getFavoriteRecipeDetail(externalId)?.let { return it }
        if (BuildConfig.SPOONACULAR_API_KEY.isBlank()) return null

        return try {
            spoonacularApi.getRecipeInformation(externalId).let { dto ->
                RecipeDetail(
                    externalId = dto.id.toString(),
                    title = dto.title,
                    description = dto.summary?.replace("<[^>]+>".toRegex(), ""),
                    preparationTimeMinutes = dto.readyInMinutes,
                    servings = dto.servings,
                    imageUrl = dto.image,
                    sourceUrl = dto.sourceUrl,
                    ingredients = dto.extendedIngredients.mapNotNull { ingredient ->
                        val original = ingredient.original ?: ingredient.name ?: return@mapNotNull null
                        val matchName = ingredient.name?.takeIf { it.isNotBlank() } ?: original
                        RecipeIngredientData(
                            originalName = original,
                            normalizedName = textNormalizer.normalizeFoodText(matchName),
                            externalIngredientId = ingredient.id?.toString(),
                            amount = ingredient.amount,
                            unit = ingredient.unit
                        )
                    }
                )
            }
        } catch (_: Exception) {
            null
        }
    }

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
                unit = it.unit
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
                        originalName = it.originalName,
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

}
