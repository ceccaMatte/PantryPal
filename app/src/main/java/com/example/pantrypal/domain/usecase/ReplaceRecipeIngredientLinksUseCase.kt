package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.domain.model.ReplaceRecipeIngredientLinksCommand
import javax.inject.Inject

class ReplaceRecipeIngredientLinksUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository
) {
    suspend operator fun invoke(command: ReplaceRecipeIngredientLinksCommand) =
        recipeRepository.replaceIngredientFoodLinks(command)
}
