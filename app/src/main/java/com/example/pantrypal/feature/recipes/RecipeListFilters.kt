package com.example.pantrypal.feature.recipes

import com.example.pantrypal.core.util.TextNormalizer

internal fun List<RecipeCardUi>.filterByRecipeTitle(
    query: String,
    textNormalizer: TextNormalizer
): List<RecipeCardUi> {
    val normalized = textNormalizer.normalizeFoodText(query)
    if (normalized.isBlank()) return this
    return filter { recipe ->
        textNormalizer.normalizeFoodText(recipe.title).contains(normalized)
    }
}
