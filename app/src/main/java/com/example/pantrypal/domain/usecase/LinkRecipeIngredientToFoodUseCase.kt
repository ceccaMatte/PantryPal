package com.example.pantrypal.domain.usecase

import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.LinkRecipeIngredientToFoodCommand
import com.example.pantrypal.domain.model.RecipeIngredientLink
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class LinkRecipeIngredientToFoodUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(command: LinkRecipeIngredientToFoodCommand): RecipeIngredientLink? {
        if (command.normalizedAlias.isBlank() || command.categoryId <= 0) return null
        val language = settingsRepository.observeSettings()
            .first()
            .language
            .takeIf { it.isNotBlank() }

        return recipeRepository.linkIngredientToFood(
            aliasOriginal = command.aliasOriginal,
            normalizedAlias = command.normalizedAlias,
            externalIngredientId = command.externalIngredientId,
            categoryId = command.categoryId,
            language = language,
            replaceLinkId = command.replaceLinkId
        )
    }
}
