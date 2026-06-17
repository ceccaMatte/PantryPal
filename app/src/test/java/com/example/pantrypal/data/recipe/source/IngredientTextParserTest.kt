package com.example.pantrypal.data.recipe.source

import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientTextParserTest {
    @Test
    fun usesNameCleanBeforeParsingOriginal() {
        val cleanName = resolveIngredientCleanName(
            nameClean = "queso fresco",
            name = "cheese",
            original = "8 tbsp queso fresco"
        )

        assertEquals("queso fresco", cleanName)
    }

    @Test
    fun parsesCleanNameAndDisplayAmountFromOriginalWhenNeeded() {
        val original = "8 tbsp queso fresco"

        assertEquals("queso fresco", resolveIngredientCleanName(null, null, original))
        assertEquals("8 tbsp", ingredientDisplayAmount(null, null, original))
    }

    @Test
    fun formatsDisplayAmountFromAmountAndUnit() {
        assertEquals("8 tbsp", ingredientDisplayAmount(8.0, "tbsp", "8 tbsp queso fresco"))
        assertEquals("0.5 container", ingredientDisplayAmount(0.5, "container", "1/2 container white cheese"))
    }
}
