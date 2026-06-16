package com.example.pantrypal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pantrypal.core.database.entity.FavoriteRecipeEntity
import com.example.pantrypal.core.database.entity.RecipeIngredientEntity
import com.example.pantrypal.core.database.projection.FavoriteRecipeCardProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM favorite_recipes WHERE externalId = :externalId LIMIT 1")
    suspend fun getFavoriteByExternalId(externalId: String): FavoriteRecipeEntity?

    @Query(
        """
        SELECT
            id,
            externalId,
            title,
            imageUrl,
            preparationTimeMinutes,
            servings
        FROM favorite_recipes
        ORDER BY savedAt DESC
        """
    )
    fun observeFavoriteRecipeCards(): Flow<List<FavoriteRecipeCardProjection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavoriteRecipe(recipe: FavoriteRecipeEntity): Long

    @Query("DELETE FROM favorite_recipes WHERE externalId = :externalId")
    suspend fun deleteFavoriteByExternalId(externalId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: Long)

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY id ASC")
    suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity>

    @Query("SELECT COUNT(*) FROM favorite_recipes WHERE externalId = :externalId")
    suspend fun isFavoriteCount(externalId: String): Int
}
