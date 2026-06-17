package com.example.pantrypal.data.recipe.source

import com.example.pantrypal.BuildConfig
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.recipe.remote.SpoonacularApi
import com.example.pantrypal.data.recipe.remote.dto.SpoonacularRecipeInformationDto
import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetail
import com.example.pantrypal.domain.model.RecipeDetailResult
import com.example.pantrypal.domain.model.RecipeIngredientData
import com.example.pantrypal.domain.model.RecipeSearchResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

@Singleton
class SpoonacularRecipeRemoteDataSource @Inject constructor(
    private val api: SpoonacularApi,
    private val textNormalizer: TextNormalizer
) : RecipeRemoteDataSource {
    override suspend fun searchRecipes(query: String): RecipeSearchResult {
        if (BuildConfig.SPOONACULAR_API_KEY.isBlank()) return RecipeSearchResult.ConfigMissing
        return try {
            val recipes = api.searchRecipes(query).results.map {
                RecipeCard(
                    externalId = it.id.toString(),
                    title = it.title,
                    imageUrl = it.image,
                    preparationTimeMinutes = null
                )
            }
            if (recipes.isEmpty()) RecipeSearchResult.Empty else RecipeSearchResult.Success(recipes)
        } catch (error: Exception) {
            error.toSearchResult()
        }
    }

    override suspend fun findRecipesByIngredients(ingredients: List<String>): RecipeSearchResult {
        if (ingredients.isEmpty()) return RecipeSearchResult.Empty
        if (BuildConfig.SPOONACULAR_API_KEY.isBlank()) return RecipeSearchResult.ConfigMissing
        return try {
            val recipes = api.findRecipesByIngredients(ingredients.joinToString(",")).map {
                RecipeCard(
                    externalId = it.id.toString(),
                    title = it.title,
                    imageUrl = it.image,
                    preparationTimeMinutes = null
                )
            }
            if (recipes.isEmpty()) RecipeSearchResult.Empty else RecipeSearchResult.Success(recipes)
        } catch (error: Exception) {
            error.toSearchResult()
        }
    }

    override suspend fun getRecipeDetail(externalId: String): RecipeDetailResult {
        if (BuildConfig.SPOONACULAR_API_KEY.isBlank()) return RecipeDetailResult.ConfigMissing
        return try {
            RecipeDetailResult.Success(api.getRecipeInformation(externalId).toDomain())
        } catch (error: Exception) {
            error.toDetailResult()
        }
    }

    private fun SpoonacularRecipeInformationDto.toDomain(): RecipeDetail =
        RecipeDetail(
            externalId = id.toString(),
            title = title,
            description = summary?.replace("<[^>]+>".toRegex(), ""),
            preparationTimeMinutes = readyInMinutes,
            servings = servings,
            imageUrl = image,
            sourceUrl = sourceUrl,
            ingredients = extendedIngredients.mapNotNull { ingredient ->
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

    private fun Exception.toSearchResult(): RecipeSearchResult =
        when (this) {
            is IOException -> RecipeSearchResult.NetworkError
            is SecurityException -> RecipeSearchResult.NetworkError
            is SerializationException -> RecipeSearchResult.InvalidResponse
            is HttpException -> when (code()) {
                401, 403 -> RecipeSearchResult.ConfigMissing
                402 -> RecipeSearchResult.QuotaExceeded
                429 -> RecipeSearchResult.RateLimited
                in 500..599 -> RecipeSearchResult.NetworkError
                else -> RecipeSearchResult.GenericError
            }
            else -> RecipeSearchResult.GenericError
        }

    private fun Exception.toDetailResult(): RecipeDetailResult =
        when (this) {
            is IOException -> RecipeDetailResult.NetworkError
            is SecurityException -> RecipeDetailResult.NetworkError
            is SerializationException -> RecipeDetailResult.InvalidResponse
            is HttpException -> when (code()) {
                401, 403 -> RecipeDetailResult.ConfigMissing
                402 -> RecipeDetailResult.QuotaExceeded
                429 -> RecipeDetailResult.RateLimited
                404 -> RecipeDetailResult.Empty
                in 500..599 -> RecipeDetailResult.NetworkError
                else -> RecipeDetailResult.GenericError
            }
            else -> RecipeDetailResult.GenericError
        }
}
