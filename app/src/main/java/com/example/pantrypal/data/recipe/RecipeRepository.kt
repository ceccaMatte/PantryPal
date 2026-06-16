package com.example.pantrypal.data.recipe

import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetail
import com.example.pantrypal.domain.model.RecipeIngredientLink
import com.example.pantrypal.domain.model.RecipeSearchQuery
import com.example.pantrypal.domain.model.RecipeSearchResult
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    suspend fun searchRecipes(query: RecipeSearchQuery): RecipeSearchResult
    suspend fun searchRecipesByIngredients(ingredients: List<String>): RecipeSearchResult
    suspend fun getRecipeDetail(externalId: String): RecipeDetail?
    fun observeFavoriteRecipes(): Flow<List<RecipeCard>>
    suspend fun getFavoriteRecipeDetail(externalId: String): RecipeDetail?
    suspend fun saveFavoriteRecipe(recipe: RecipeDetail)
    suspend fun removeFavoriteRecipe(externalId: String)
    suspend fun findIngredientLinks(normalizedAlias: String, externalIngredientId: String?): List<RecipeIngredientLink>
    suspend fun getIngredientLinksForCategory(categoryId: Long): List<RecipeIngredientLink>
}
