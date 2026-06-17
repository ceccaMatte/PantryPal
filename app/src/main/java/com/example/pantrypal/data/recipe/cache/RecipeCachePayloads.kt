package com.example.pantrypal.data.recipe.cache

import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetail
import com.example.pantrypal.domain.model.RecipeIngredientData
import kotlinx.serialization.Serializable

@Serializable
data class CachedRecipeSearchPayload(
    val recipes: List<CachedRecipeCardPayload>
)

@Serializable
data class CachedRecipeByIngredientsPayload(
    val recipes: List<CachedRecipeCardPayload>
)

@Serializable
data class CachedRecipeDetailPayload(
    val externalId: String,
    val title: String,
    val description: String? = null,
    val preparationTimeMinutes: Int? = null,
    val servings: Int? = null,
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val ingredients: List<CachedRecipeIngredientPayload> = emptyList()
)

@Serializable
data class CachedRecipeCardPayload(
    val externalId: String,
    val title: String,
    val imageUrl: String? = null,
    val preparationTimeMinutes: Int? = null
)

@Serializable
data class CachedRecipeIngredientPayload(
    val originalName: String,
    val normalizedName: String,
    val externalIngredientId: String? = null,
    val amount: Double? = null,
    val unit: String? = null
)

fun RecipeCard.toCachePayload(): CachedRecipeCardPayload =
    CachedRecipeCardPayload(
        externalId = externalId,
        title = title,
        imageUrl = imageUrl,
        preparationTimeMinutes = preparationTimeMinutes
    )

fun CachedRecipeCardPayload.toDomain(isFavorite: Boolean = false): RecipeCard =
    RecipeCard(
        externalId = externalId,
        title = title,
        imageUrl = imageUrl,
        preparationTimeMinutes = preparationTimeMinutes,
        isFavorite = isFavorite
    )

fun RecipeDetail.toCachePayload(): CachedRecipeDetailPayload =
    CachedRecipeDetailPayload(
        externalId = externalId,
        title = title,
        description = description,
        preparationTimeMinutes = preparationTimeMinutes,
        servings = servings,
        imageUrl = imageUrl,
        sourceUrl = sourceUrl,
        ingredients = ingredients.map {
            CachedRecipeIngredientPayload(
                originalName = it.originalName,
                normalizedName = it.normalizedName,
                externalIngredientId = it.externalIngredientId,
                amount = it.amount,
                unit = it.unit
            )
        }
    )

fun CachedRecipeDetailPayload.toDomain(): RecipeDetail =
    RecipeDetail(
        externalId = externalId,
        title = title,
        description = description,
        preparationTimeMinutes = preparationTimeMinutes,
        servings = servings,
        imageUrl = imageUrl,
        sourceUrl = sourceUrl,
        ingredients = ingredients.map {
            RecipeIngredientData(
                originalName = it.originalName,
                normalizedName = it.normalizedName,
                externalIngredientId = it.externalIngredientId,
                amount = it.amount,
                unit = it.unit
            )
        }
    )
