package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.domain.model.PantryPalApiMode
import com.example.pantrypal.domain.model.RecipeSearchResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GetHomeSuggestedRecipesUseCase @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val recipeRepository: RecipeRepository
) {
    val apiMode: PantryPalApiMode
        get() = recipeRepository.apiMode

    operator fun invoke(allowNetwork: Boolean): Flow<RecipeSearchResult> =
        pantryRepository.observeActiveLotsWithCategories()
            .map { lots -> lots.map { it.categoryName }.distinct().sorted() }
            .distinctUntilChanged()
            .map { ingredients ->
                if (ingredients.isEmpty()) {
                    RecipeSearchResult.Empty
                } else {
                    recipeRepository.searchRecipesByIngredients(ingredients, allowNetwork = allowNetwork)
                }
            }
}
