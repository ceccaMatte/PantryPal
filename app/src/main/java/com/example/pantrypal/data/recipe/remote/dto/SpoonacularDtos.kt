package com.example.pantrypal.data.recipe.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpoonacularByIngredientsRecipeDto(
    val id: Long,
    val title: String,
    val image: String? = null,
    val imageType: String? = null,
    val usedIngredientCount: Int? = null,
    val missedIngredientCount: Int? = null
)

@Serializable
data class SpoonacularComplexSearchResponseDto(
    val results: List<SpoonacularRecipeSearchItemDto> = emptyList(),
    val offset: Int? = null,
    val number: Int? = null,
    val totalResults: Int? = null
)

@Serializable
data class SpoonacularRecipeSearchItemDto(
    val id: Long,
    val title: String,
    val image: String? = null,
    val imageType: String? = null
)

@Serializable
data class SpoonacularRecipeInformationDto(
    val id: Long,
    val title: String,
    val image: String? = null,
    val readyInMinutes: Int? = null,
    val servings: Int? = null,
    val sourceUrl: String? = null,
    val summary: String? = null,
    val extendedIngredients: List<SpoonacularIngredientDto> = emptyList()
)

@Serializable
data class SpoonacularIngredientDto(
    val id: Long? = null,
    val nameClean: String? = null,
    val name: String? = null,
    val original: String? = null,
    val amount: Double? = null,
    val unit: String? = null
)
