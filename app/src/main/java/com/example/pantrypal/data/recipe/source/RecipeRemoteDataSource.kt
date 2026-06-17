package com.example.pantrypal.data.recipe.source

import com.example.pantrypal.domain.model.RecipeDetailResult
import com.example.pantrypal.domain.model.RecipeSearchResult

interface RecipeRemoteDataSource {
    suspend fun searchRecipes(query: String): RecipeSearchResult
    suspend fun findRecipesByIngredients(ingredients: List<String>): RecipeSearchResult
    suspend fun getRecipeDetail(externalId: String): RecipeDetailResult
}
