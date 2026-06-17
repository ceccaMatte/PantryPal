package com.example.pantrypal.data.recipe

import com.example.pantrypal.domain.model.RecipeCard
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRecipeCacheTest {
    @Test
    fun mergeAddsNewRecipesWithoutDroppingPreviousOnes() {
        val cache = SessionRecipeCache()

        cache.merge(listOf(recipe("1", "Pasta")))
        cache.merge(listOf(recipe("2", "Pollo")))

        assertEquals(listOf("1", "2"), cache.recipes.value.map { it.externalId })
    }

    @Test
    fun mergeDeduplicatesByExternalIdAndUpdatesExistingRecipe() {
        val cache = SessionRecipeCache()

        cache.merge(listOf(recipe("1", "Pasta")))
        cache.merge(listOf(recipe("1", "Pasta al pomodoro")))

        assertEquals(1, cache.recipes.value.size)
        assertEquals("Pasta al pomodoro", cache.recipes.value.single().title)
    }

    private fun recipe(id: String, title: String): RecipeCard =
        RecipeCard(
            externalId = id,
            title = title,
            imageUrl = null,
            preparationTimeMinutes = null,
            isFavorite = false
        )
}
