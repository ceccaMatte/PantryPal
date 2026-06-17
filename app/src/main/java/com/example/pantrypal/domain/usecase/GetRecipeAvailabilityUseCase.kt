package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.RecipeAvailability
import com.example.pantrypal.domain.model.RecipeAvailabilityStatus
import com.example.pantrypal.domain.model.RecipeDetail
import com.example.pantrypal.domain.model.RecipeIngredientAvailabilityItem
import javax.inject.Inject

class GetRecipeAvailabilityUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val pantryRepository: PantryRepository
) {
    suspend operator fun invoke(recipe: RecipeDetail): RecipeAvailability {
        val items = recipe.ingredients.map { ingredient ->
            val links = recipeRepository.findIngredientLinks(
                normalizedAlias = ingredient.normalizedName,
                externalIngredientId = ingredient.externalIngredientId
            )
            val categoryIds = links.map { it.categoryId }.distinct()
            val categories = categoryIds.mapNotNull { pantryRepository.getFoodCategory(it) }
            val activeLots = pantryRepository.getActiveLotsForCategories(categoryIds)
            val activeCategoryIds = activeLots.map { it.categoryId }.toSet()
            val availableCategories = categories.filter { it.id in activeCategoryIds }
            val totalAvailable = activeLots.sumOf { it.quantity }

            RecipeIngredientAvailabilityItem(
                ingredient = ingredient,
                status = if (links.isNotEmpty() && totalAvailable > 0) {
                    RecipeAvailabilityStatus.IN_PANTRY
                } else {
                    RecipeAvailabilityStatus.TO_BUY
                },
                linkedCategories = categories.sortedBy(FoodCategory::name),
                availableCategories = availableCategories.sortedBy(FoodCategory::name),
                matchingLinks = links,
                totalAvailableQuantity = totalAvailable
            )
        }
        return RecipeAvailability(items)
    }
}
