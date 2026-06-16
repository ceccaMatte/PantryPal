package com.example.pantrypal.data.recipe

import androidx.room.withTransaction
import com.example.pantrypal.BuildConfig
import com.example.pantrypal.core.database.PantryPalDatabase
import com.example.pantrypal.core.database.dao.RecipeDao
import com.example.pantrypal.core.database.dao.RecipeIngredientLinkDao
import com.example.pantrypal.core.database.entity.FavoriteRecipeEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientEntity
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.pantry.toDomain
import com.example.pantrypal.data.recipe.remote.SpoonacularApi
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
        if (BuildConfig.SPOONACULAR_API_KEY.isBlank()) return sampleRecipeResult()

        return try {
            val response = spoonacularApi.searchRecipes(query.query)
            val recipes = response.results.map {
                RecipeCard(
                    externalId = it.id.toString(),
                    title = it.title,
                    imageUrl = it.image,
                    preparationTimeMinutes = null
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
        if (BuildConfig.SPOONACULAR_API_KEY.isBlank()) return sampleRecipeResult()

        return try {
            val recipes = spoonacularApi.findRecipesByIngredients(ingredients.joinToString(","))
                .map {
                    RecipeCard(
                        externalId = it.id.toString(),
                        title = it.title,
                        imageUrl = it.image,
                        preparationTimeMinutes = null
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
        if (BuildConfig.SPOONACULAR_API_KEY.isBlank()) return sampleRecipeDetail(externalId)

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
                        RecipeIngredientData(
                            originalName = original,
                            normalizedName = textNormalizer.normalizeFoodText(original),
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

    private fun sampleRecipeResult(): RecipeSearchResult =
        RecipeSearchResult.Success(
            listOf(
                RecipeCard("sample-pasta", "Pasta al Pomodoro e Basilico", null, 20, false),
                RecipeCard("sample-quinoa", "Bowl di Quinoa e Avocado", null, 25, false),
                RecipeCard("sample-salmone", "Salmone al Forno", null, 30, false)
            )
        )

    private fun sampleRecipeDetail(externalId: String): RecipeDetail =
        RecipeDetail(
            externalId = externalId,
            title = when (externalId) {
                "sample-quinoa" -> "Bowl di Quinoa e Avocado"
                "sample-salmone" -> "Salmone al Forno"
                else -> "Pasta al Pomodoro e Basilico"
            },
            description = "Un classico pronto in pochi minuti con ingredienti freschi e profumati.",
            preparationTimeMinutes = 20,
            servings = 2,
            imageUrl = null,
            sourceUrl = null,
            ingredients = listOf(
                RecipeIngredientData("Pasta (Spaghetti)", "pasta spaghetti", null, 200.0, "g"),
                RecipeIngredientData("Olio d'Oliva", "olio d oliva", null, 2.0, "cucchiai"),
                RecipeIngredientData("Sale", "sale", null, null, "q.b."),
                RecipeIngredientData("Pomodorini Ciliegino", "pomodorini ciliegino", null, 250.0, "g"),
                RecipeIngredientData("Basilico fresco", "basilico fresco", null, 1.0, "mazzetto")
            )
        )
}
