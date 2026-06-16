package com.example.pantrypal.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.pantrypal.domain.model.CategoryOrigin
import com.example.pantrypal.domain.model.IngredientRelationType
import com.example.pantrypal.domain.model.LinkOrigin
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "food_categories",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class FoodCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val defaultStorageLocation: StorageLocation,
    val defaultPerishability: PerishabilityType,
    val imageUri: String?,
    val origin: CategoryOrigin,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastUsedAt: Instant?
)

@Entity(
    tableName = "expiry_lots",
    foreignKeys = [
        ForeignKey(
            entity = FoodCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["expirationDate"]),
        Index(value = ["categoryId", "expirationDate"], unique = true)
    ]
)
data class ExpiryLotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val expirationDate: LocalDate,
    val quantity: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(
    tableName = "barcode_product_links",
    foreignKeys = [
        ForeignKey(
            entity = FoodCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class BarcodeProductLinkEntity(
    @PrimaryKey
    val barcode: String,
    val categoryId: Long,
    val productName: String,
    val genericName: String?,
    val brand: String?,
    val quantityLabel: String?,
    val imageUrl: String?,
    val rawCategoryTags: String?,
    val rawFoodGroupTags: String?,
    val origin: LinkOrigin,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastUsedAt: Instant?
)

@Entity(
    tableName = "recipe_ingredient_links",
    foreignKeys = [
        ForeignKey(
            entity = FoodCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["normalizedAlias"]),
        Index(value = ["externalIngredientId"]),
        Index(value = ["normalizedAlias", "categoryId"], unique = true)
    ]
)
data class RecipeIngredientLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val aliasOriginal: String,
    val normalizedAlias: String,
    val language: String?,
    val externalIngredientId: String?,
    val relationType: IngredientRelationType,
    val origin: LinkOrigin,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Entity(
    tableName = "favorite_recipes",
    indices = [Index(value = ["externalId"], unique = true)]
)
data class FavoriteRecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val externalId: String,
    val title: String,
    val description: String?,
    val preparationTimeMinutes: Int?,
    val servings: Int?,
    val imageUrl: String?,
    val sourceUrl: String?,
    val savedAt: Instant,
    val updatedAt: Instant
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = FavoriteRecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recipeId"]),
        Index(value = ["normalizedName"]),
        Index(value = ["externalIngredientId"])
    ]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: Long,
    val originalName: String,
    val normalizedName: String,
    val externalIngredientId: String?,
    val amount: Double?,
    val unit: String?
)
