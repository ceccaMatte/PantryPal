package com.example.pantrypal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pantrypal.core.database.dao.BarcodeProductLinkDao
import com.example.pantrypal.core.database.dao.ExpiryLotDao
import com.example.pantrypal.core.database.dao.FoodCategoryDao
import com.example.pantrypal.core.database.dao.RecipeDao
import com.example.pantrypal.core.database.dao.RecipeIngredientLinkDao
import com.example.pantrypal.core.database.entity.BarcodeProductLinkEntity
import com.example.pantrypal.core.database.entity.ExpiryLotEntity
import com.example.pantrypal.core.database.entity.FavoriteRecipeEntity
import com.example.pantrypal.core.database.entity.FoodCategoryEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientLinkEntity

@Database(
    entities = [
        FoodCategoryEntity::class,
        ExpiryLotEntity::class,
        BarcodeProductLinkEntity::class,
        RecipeIngredientLinkEntity::class,
        FavoriteRecipeEntity::class,
        RecipeIngredientEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(PantryPalTypeConverters::class)
abstract class PantryPalDatabase : RoomDatabase() {
    abstract fun foodCategoryDao(): FoodCategoryDao
    abstract fun expiryLotDao(): ExpiryLotDao
    abstract fun barcodeProductLinkDao(): BarcodeProductLinkDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientLinkDao(): RecipeIngredientLinkDao
}
