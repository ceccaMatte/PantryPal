package com.example.pantrypal.core.database.seed

import com.example.pantrypal.domain.model.IngredientRelationType
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import kotlinx.serialization.Serializable

@Serializable
data class FoodCategorySeed(
    val name: String,
    val normalizedName: String,
    val defaultStorageLocation: StorageLocation,
    val defaultPerishability: PerishabilityType
)

@Serializable
data class RecipeIngredientLinkSeed(
    val aliasOriginal: String,
    val normalizedAlias: String,
    val categoryNormalizedName: String,
    val language: String? = null,
    val externalIngredientId: String? = null,
    val relationType: IngredientRelationType
)
