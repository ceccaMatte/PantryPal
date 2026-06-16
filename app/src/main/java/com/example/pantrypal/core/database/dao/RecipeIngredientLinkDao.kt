package com.example.pantrypal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pantrypal.core.database.entity.RecipeIngredientLinkEntity
import com.example.pantrypal.core.database.projection.RecipeIngredientLinkProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeIngredientLinkDao {
    @Query(
        """
        SELECT
            ril.id AS id,
            ril.categoryId AS categoryId,
            fc.name AS categoryName,
            ril.aliasOriginal AS aliasOriginal,
            ril.normalizedAlias AS normalizedAlias,
            ril.language AS language,
            ril.externalIngredientId AS externalIngredientId,
            ril.relationType AS relationType,
            ril.origin AS origin,
            ril.isActive AS isActive
        FROM recipe_ingredient_links ril
        INNER JOIN food_categories fc ON fc.id = ril.categoryId
        WHERE ril.normalizedAlias = :normalizedAlias
          AND ril.isActive = 1
        ORDER BY
            CASE ril.origin
                WHEN 'USER' THEN 0
                WHEN 'SEED' THEN 1
                WHEN 'AUTO' THEN 2
                ELSE 3
            END,
            CASE ril.relationType
                WHEN 'EXACT' THEN 0
                WHEN 'COMPATIBLE' THEN 1
                WHEN 'BROADER' THEN 2
                ELSE 3
            END,
            fc.name ASC
        """
    )
    suspend fun findActiveLinksByAlias(normalizedAlias: String): List<RecipeIngredientLinkProjection>

    @Query(
        """
        SELECT
            ril.id AS id,
            ril.categoryId AS categoryId,
            fc.name AS categoryName,
            ril.aliasOriginal AS aliasOriginal,
            ril.normalizedAlias AS normalizedAlias,
            ril.language AS language,
            ril.externalIngredientId AS externalIngredientId,
            ril.relationType AS relationType,
            ril.origin AS origin,
            ril.isActive AS isActive
        FROM recipe_ingredient_links ril
        INNER JOIN food_categories fc ON fc.id = ril.categoryId
        WHERE ril.externalIngredientId = :externalIngredientId
          AND ril.isActive = 1
        ORDER BY fc.name ASC
        """
    )
    suspend fun findActiveLinksByExternalIngredientId(externalIngredientId: String): List<RecipeIngredientLinkProjection>

    @Query(
        """
        SELECT
            ril.id AS id,
            ril.categoryId AS categoryId,
            fc.name AS categoryName,
            ril.aliasOriginal AS aliasOriginal,
            ril.normalizedAlias AS normalizedAlias,
            ril.language AS language,
            ril.externalIngredientId AS externalIngredientId,
            ril.relationType AS relationType,
            ril.origin AS origin,
            ril.isActive AS isActive
        FROM recipe_ingredient_links ril
        INNER JOIN food_categories fc ON fc.id = ril.categoryId
        WHERE ril.categoryId = :categoryId
          AND ril.isActive = 1
        ORDER BY ril.aliasOriginal ASC
        """
    )
    fun observeActiveLinksForCategory(categoryId: Long): Flow<List<RecipeIngredientLinkProjection>>

    @Query(
        """
        SELECT *
        FROM recipe_ingredient_links
        WHERE normalizedAlias = :normalizedAlias
          AND categoryId = :categoryId
        LIMIT 1
        """
    )
    suspend fun getLinkByAliasAndCategory(
        normalizedAlias: String,
        categoryId: Long
    ): RecipeIngredientLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: RecipeIngredientLinkEntity): Long

    @Query("DELETE FROM recipe_ingredient_links WHERE id = :linkId")
    suspend fun deleteById(linkId: Long)

    @Query(
        """
        DELETE FROM recipe_ingredient_links
        WHERE normalizedAlias = :normalizedAlias
          AND categoryId = :categoryId
        """
    )
    suspend fun deleteByAliasAndCategory(normalizedAlias: String, categoryId: Long)

    @Query(
        """
        SELECT *
        FROM recipe_ingredient_links
        WHERE normalizedAlias LIKE '%' || :query || '%'
          AND isActive = 1
        ORDER BY normalizedAlias ASC
        LIMIT :limit
        """
    )
    suspend fun searchAliases(query: String, limit: Int = 8): List<RecipeIngredientLinkEntity>
}
