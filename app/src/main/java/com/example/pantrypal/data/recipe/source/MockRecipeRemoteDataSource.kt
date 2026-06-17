package com.example.pantrypal.data.recipe.source

import android.content.Context
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.recipe.cache.CachedRecipeByIngredientsPayload
import com.example.pantrypal.data.recipe.cache.CachedRecipeDetailPayload
import com.example.pantrypal.data.recipe.cache.CachedRecipeSearchPayload
import com.example.pantrypal.data.recipe.cache.toDomain
import com.example.pantrypal.domain.model.RecipeDetailResult
import com.example.pantrypal.domain.model.RecipeSearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Singleton
class MockRecipeRemoteDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val textNormalizer: TextNormalizer
) : RecipeRemoteDataSource {
    override suspend fun searchRecipes(query: String): RecipeSearchResult =
        try {
            val normalizedQuery = textNormalizer.normalizeFoodText(query)
            val recipes = readSearchPayload("mock/recipes/search_results.json")
                .recipes
                .map { it.toDomain() }
                .filter { recipe ->
                    normalizedQuery.isBlank() ||
                        textNormalizer.normalizeFoodText(recipe.title).contains(normalizedQuery)
                }
                .ifEmpty { readSearchPayload("mock/recipes/search_results.json").recipes.map { it.toDomain() } }
            RecipeSearchResult.Success(recipes)
        } catch (_: Exception) {
            RecipeSearchResult.InvalidResponse
        }

    override suspend fun findRecipesByIngredients(ingredients: List<String>): RecipeSearchResult =
        try {
            RecipeSearchResult.Success(
                readSuggestedPayload("mock/recipes/suggested_by_ingredients.json").recipes.map { it.toDomain() }
            )
        } catch (_: Exception) {
            RecipeSearchResult.InvalidResponse
        }

    override suspend fun getRecipeDetail(externalId: String): RecipeDetailResult =
        try {
            val detail = listOf("mock/recipes/detail_1.json", "mock/recipes/detail_2.json", "mock/recipes/detail_3.json")
                .map(::readDetailPayload)
                .firstOrNull { it.externalId == externalId }
                ?: return RecipeDetailResult.Empty
            RecipeDetailResult.Success(detail.toDomain())
        } catch (_: SerializationException) {
            RecipeDetailResult.InvalidResponse
        } catch (_: Exception) {
            RecipeDetailResult.GenericError
        }

    private fun readSearchPayload(path: String): CachedRecipeSearchPayload =
        json.decodeFromString(CachedRecipeSearchPayload.serializer(), readAsset(path))

    private fun readSuggestedPayload(path: String): CachedRecipeByIngredientsPayload =
        json.decodeFromString(CachedRecipeByIngredientsPayload.serializer(), readAsset(path))

    private fun readDetailPayload(path: String): CachedRecipeDetailPayload =
        json.decodeFromString(CachedRecipeDetailPayload.serializer(), readAsset(path))

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}
