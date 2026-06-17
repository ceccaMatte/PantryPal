package com.example.pantrypal.feature.recipes

import com.example.pantrypal.core.util.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeListFiltersTest {
    private val normalizer = TextNormalizer()

    @Test
    fun filterByRecipeTitleUsesNormalizedTitle() {
        val recipes = listOf(
            card("1", "Pollo al Limone"),
            card("2", "Pasta al Pomodoro"),
            card("3", "Insalata ricca")
        )

        val filtered = recipes.filterByRecipeTitle("pomodoro", normalizer)

        assertEquals(listOf("2"), filtered.map { it.externalId })
    }

    @Test
    fun blankQueryKeepsCurrentList() {
        val recipes = listOf(card("1", "Pollo"), card("2", "Pasta"))

        val filtered = recipes.filterByRecipeTitle(" ", normalizer)

        assertEquals(listOf("1", "2"), filtered.map { it.externalId })
    }

    private fun card(id: String, title: String): RecipeCardUi =
        RecipeCardUi(
            externalId = id,
            title = title,
            description = "",
            readyInMinutes = 20,
            presentCount = null,
            missingCount = null,
            isFavorite = false
        )
}
