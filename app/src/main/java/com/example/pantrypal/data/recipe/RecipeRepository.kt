package com.example.pantrypal.data.recipe

import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetail
import com.example.pantrypal.domain.model.RecipeDetailResult
import com.example.pantrypal.domain.model.RecipeIngredientLink
import com.example.pantrypal.domain.model.ReplaceRecipeIngredientLinksCommand
import com.example.pantrypal.domain.model.RecipeSearchQuery
import com.example.pantrypal.domain.model.RecipeSearchResult
import com.example.pantrypal.domain.model.PantryPalApiMode
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    val apiMode: PantryPalApiMode
    suspend fun searchRecipes(query: RecipeSearchQuery, allowNetwork: Boolean = true): RecipeSearchResult
    suspend fun searchRecipesByIngredients(ingredients: List<String>, allowNetwork: Boolean = true): RecipeSearchResult
    suspend fun getRecipeDetailResult(externalId: String, allowNetwork: Boolean = true): RecipeDetailResult
    suspend fun getRecipeDetail(externalId: String): RecipeDetail?
    fun observeFavoriteRecipes(): Flow<List<RecipeCard>>
    suspend fun getFavoriteRecipeDetail(externalId: String): RecipeDetail?
    suspend fun saveFavoriteRecipe(recipe: RecipeDetail)
    suspend fun removeFavoriteRecipe(externalId: String)
    suspend fun isFavorite(externalId: String): Boolean
    suspend fun findIngredientLinks(normalizedAlias: String, externalIngredientId: String?): List<RecipeIngredientLink>
    suspend fun getIngredientLinksForCategory(categoryId: Long): List<RecipeIngredientLink>
    suspend fun linkIngredientToFood(
        aliasOriginal: String,
        normalizedAlias: String,
        externalIngredientId: String?,
        categoryId: Long,
        language: String?,
        replaceLinkId: Long? = null
    ): RecipeIngredientLink?
    suspend fun replaceIngredientFoodLinks(command: ReplaceRecipeIngredientLinksCommand): List<RecipeIngredientLink>
}
