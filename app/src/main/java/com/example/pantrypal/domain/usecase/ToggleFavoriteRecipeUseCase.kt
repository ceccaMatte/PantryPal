package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.domain.model.RecipeDetail
import javax.inject.Inject

class ToggleFavoriteRecipeUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository
) {
    suspend operator fun invoke(recipe: RecipeDetail): Boolean {
        return if (recipeRepository.isFavorite(recipe.externalId)) {
            recipeRepository.removeFavoriteRecipe(recipe.externalId)
            false
        } else {
            recipeRepository.saveFavoriteRecipe(recipe)
            true
        }
    }
}
