package com.example.pantrypal.core.database.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class SeedDataLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    fun loadFoodCategories(): List<FoodCategorySeed> =
        context.assets.open("seed/food_categories_seed.json").use { input ->
            json.decodeFromString(input.bufferedReader().readText())
        }

    fun loadRecipeIngredientLinks(): List<RecipeIngredientLinkSeed> =
        context.assets.open("seed/recipe_ingredient_links_seed.json").use { input ->
            json.decodeFromString(input.bufferedReader().readText())
        }
}
