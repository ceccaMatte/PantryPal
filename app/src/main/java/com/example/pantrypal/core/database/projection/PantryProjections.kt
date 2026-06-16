package com.example.pantrypal.core.database.projection

import com.example.pantrypal.domain.model.CategoryOrigin
import com.example.pantrypal.domain.model.IngredientRelationType
import com.example.pantrypal.domain.model.LinkOrigin
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import java.time.LocalDate

data class PantryRowProjection(
    val categoryId: Long,
    val name: String,
    val imageUri: String?,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val totalQuantity: Int,
    val nearestExpirationDate: LocalDate?,
    val lotCount: Int
)

data class LotWithCategoryProjection(
    val lotId: Long,
    val categoryId: Long,
    val categoryName: String,
    val normalizedName: String,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val expirationDate: LocalDate,
    val quantity: Int
)

data class FoodDetailProjection(
    val categoryId: Long,
    val name: String,
    val normalizedName: String,
    val imageUri: String?,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val origin: CategoryOrigin
)

data class FavoriteRecipeCardProjection(
    val id: Long,
    val externalId: String,
    val title: String,
    val imageUrl: String?,
    val preparationTimeMinutes: Int?,
    val servings: Int?
)

data class RecipeIngredientLinkProjection(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val aliasOriginal: String,
    val normalizedAlias: String,
    val language: String?,
    val externalIngredientId: String?,
    val relationType: IngredientRelationType,
    val origin: LinkOrigin,
    val isActive: Boolean
)
