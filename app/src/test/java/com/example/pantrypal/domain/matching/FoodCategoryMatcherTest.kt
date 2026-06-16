package com.example.pantrypal.domain.matching

import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.domain.model.CategoryOrigin
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.IngredientRelationType
import com.example.pantrypal.domain.model.LinkOrigin
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.RecipeIngredientLink
import com.example.pantrypal.domain.model.StorageLocation
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCategoryMatcherTest {
    private val matcher = FoodCategoryMatcher(TextNormalizer())

    @Test
    fun match_putsExactCategoryNameFirst() {
        val matches = matcher.match(
            query = "latte",
            sources = listOf(
                source(category(1, "Latte", "latte")),
                source(category(2, "Latte di cocco", "latte di cocco"))
            )
        )

        assertEquals(1L, matches.first().categoryId)
        assertEquals("exact", matches.first().reason)
    }

    @Test
    fun match_usesAliasWhenCategoryNameDoesNotMatchExactly() {
        val matches = matcher.match(
            query = "chicken breast",
            sources = listOf(
                source(
                    category(3, "Petto di pollo", "petto di pollo"),
                    alias(30, 3, "chicken breast")
                ),
                source(category(4, "Pollo", "pollo"))
            )
        )

        assertEquals(3L, matches.first().categoryId)
        assertEquals("alias", matches.first().reason)
    }

    @Test
    fun shouldShowCreateNew_dependsOnExactNormalizedName() {
        val sources = listOf(source(category(1, "Latte", "latte")))

        assertFalse(matcher.shouldShowCreateNew("Latte", sources))
        assertTrue(matcher.shouldShowCreateNew("Kefir", sources))
    }

    private fun source(category: FoodCategory, vararg aliases: RecipeIngredientLink): FoodCategoryMatchSource =
        FoodCategoryMatchSource(category = category, aliases = aliases.toList())

    private fun category(id: Long, name: String, normalizedName: String): FoodCategory =
        FoodCategory(
            id = id,
            name = name,
            normalizedName = normalizedName,
            defaultStorageLocation = StorageLocation.FRIDGE,
            defaultPerishability = PerishabilityType.FRESH,
            imageUri = null,
            origin = CategoryOrigin.SEED,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            lastUsedAt = null
        )

    private fun alias(id: Long, categoryId: Long, normalizedAlias: String): RecipeIngredientLink =
        RecipeIngredientLink(
            id = id,
            categoryId = categoryId,
            categoryName = "Petto di pollo",
            aliasOriginal = normalizedAlias,
            normalizedAlias = normalizedAlias,
            language = "en",
            externalIngredientId = null,
            relationType = IngredientRelationType.EXACT,
            origin = LinkOrigin.SEED,
            isActive = true
        )
}
