package com.example.pantrypal.data.recipe.remote

import com.example.pantrypal.BuildConfig
import com.example.pantrypal.data.recipe.remote.dto.SpoonacularByIngredientsRecipeDto
import com.example.pantrypal.data.recipe.remote.dto.SpoonacularComplexSearchResponseDto
import com.example.pantrypal.data.recipe.remote.dto.SpoonacularRecipeInformationDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpoonacularApi {
    @GET("recipes/findByIngredients")
    suspend fun findRecipesByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 5,
        @Query("ranking") ranking: Int = 1,
        @Query("ignorePantry") ignorePantry: Boolean = false,
        @Query("apiKey") apiKey: String = BuildConfig.SPOONACULAR_API_KEY
    ): List<SpoonacularByIngredientsRecipeDto>

    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("number") number: Int = 10,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = false,
        @Query("apiKey") apiKey: String = BuildConfig.SPOONACULAR_API_KEY
    ): SpoonacularComplexSearchResponseDto

    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: String,
        @Query("includeNutrition") includeNutrition: Boolean = false,
        @Query("apiKey") apiKey: String = BuildConfig.SPOONACULAR_API_KEY
    ): SpoonacularRecipeInformationDto
}
