package com.example.pantrypal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pantrypal.core.database.entity.FoodCategoryEntity
import com.example.pantrypal.core.database.projection.LotWithCategoryProjection
import com.example.pantrypal.core.database.projection.PantryRowProjection
import com.example.pantrypal.domain.model.StorageLocation
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodCategoryDao {
    @Query("SELECT * FROM food_categories WHERE id = :id")
    suspend fun getById(id: Long): FoodCategoryEntity?

    @Query("SELECT * FROM food_categories WHERE id = :id")
    fun observeById(id: Long): Flow<FoodCategoryEntity?>

    @Query("SELECT * FROM food_categories WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<FoodCategoryEntity>

    @Query("SELECT * FROM food_categories WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getByNormalizedName(normalizedName: String): FoodCategoryEntity?

    @Query(
        """
        SELECT *
        FROM food_categories
        WHERE normalizedName LIKE '%' || :query || '%'
        ORDER BY
            CASE
                WHEN normalizedName = :query THEN 0
                WHEN normalizedName LIKE :query || '%' THEN 1
                ELSE 2
            END,
            lastUsedAt DESC,
            name ASC
        LIMIT :limit
        """
    )
    suspend fun searchCategories(query: String, limit: Int = 8): List<FoodCategoryEntity>

    @Query("SELECT * FROM food_categories ORDER BY name ASC LIMIT :limit")
    suspend fun getAllCategories(limit: Int = 100): List<FoodCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: FoodCategoryEntity): Long

    @Update
    suspend fun update(category: FoodCategoryEntity)

    @Query(
        """
        UPDATE food_categories
        SET lastUsedAt = :usedAt,
            updatedAt = :usedAt
        WHERE id = :categoryId
        """
    )
    suspend fun markUsed(categoryId: Long, usedAt: Instant)

    @Query(
        """
        UPDATE food_categories
        SET name = :name,
            normalizedName = :normalizedName,
            defaultStorageLocation = :storageLocation,
            defaultPerishability = :perishability,
            imageUri = :imageUri,
            updatedAt = :updatedAt
        WHERE id = :categoryId
        """
    )
    suspend fun updateCategoryDetails(
        categoryId: Long,
        name: String,
        normalizedName: String,
        storageLocation: StorageLocation,
        perishability: com.example.pantrypal.domain.model.PerishabilityType,
        imageUri: String?,
        updatedAt: Instant
    )

    @Query(
        """
        UPDATE food_categories
        SET imageUri = :imageUri,
            updatedAt = :updatedAt
        WHERE id = :categoryId
          AND (imageUri IS NULL OR imageUri = '')
        """
    )
    suspend fun updateImageUriIfEmpty(
        categoryId: Long,
        imageUri: String,
        updatedAt: Instant
    ): Int

    @Query(
        """
        SELECT
            fc.id AS categoryId,
            fc.name AS name,
            fc.imageUri AS imageUri,
            fc.defaultStorageLocation AS storageLocation,
            fc.defaultPerishability AS perishability,
            SUM(el.quantity) AS totalQuantity,
            MIN(el.expirationDate) AS nearestExpirationDate,
            COUNT(el.id) AS lotCount
        FROM food_categories fc
        INNER JOIN expiry_lots el ON el.categoryId = fc.id
        WHERE el.quantity > 0
        GROUP BY fc.id
        ORDER BY nearestExpirationDate ASC, fc.name ASC
        """
    )
    fun observePantryRowsAll(): Flow<List<PantryRowProjection>>

    @Query(
        """
        SELECT
            fc.id AS categoryId,
            fc.name AS name,
            fc.imageUri AS imageUri,
            fc.defaultStorageLocation AS storageLocation,
            fc.defaultPerishability AS perishability,
            SUM(el.quantity) AS totalQuantity,
            MIN(el.expirationDate) AS nearestExpirationDate,
            COUNT(el.id) AS lotCount
        FROM food_categories fc
        INNER JOIN expiry_lots el ON el.categoryId = fc.id
        WHERE el.quantity > 0
          AND fc.defaultStorageLocation = :location
        GROUP BY fc.id
        ORDER BY nearestExpirationDate ASC, fc.name ASC
        """
    )
    fun observePantryRowsByLocation(location: StorageLocation): Flow<List<PantryRowProjection>>

    @Query(
        """
        SELECT
            el.id AS lotId,
            fc.id AS categoryId,
            fc.name AS categoryName,
            fc.normalizedName AS normalizedName,
            fc.defaultStorageLocation AS storageLocation,
            fc.defaultPerishability AS perishability,
            el.expirationDate AS expirationDate,
            el.quantity AS quantity
        FROM expiry_lots el
        INNER JOIN food_categories fc ON fc.id = el.categoryId
        WHERE el.quantity > 0
        ORDER BY el.expirationDate ASC, fc.name ASC
        """
    )
    suspend fun getActiveLotsWithCategories(): List<LotWithCategoryProjection>

    @Query(
        """
        SELECT
            el.id AS lotId,
            fc.id AS categoryId,
            fc.name AS categoryName,
            fc.normalizedName AS normalizedName,
            fc.defaultStorageLocation AS storageLocation,
            fc.defaultPerishability AS perishability,
            el.expirationDate AS expirationDate,
            el.quantity AS quantity
        FROM expiry_lots el
        INNER JOIN food_categories fc ON fc.id = el.categoryId
        WHERE el.quantity > 0
        ORDER BY el.expirationDate ASC, fc.name ASC
        """
    )
    fun observeActiveLotsWithCategories(): Flow<List<LotWithCategoryProjection>>

    @Query(
        """
        SELECT
            el.id AS lotId,
            fc.id AS categoryId,
            fc.name AS categoryName,
            fc.normalizedName AS normalizedName,
            fc.defaultStorageLocation AS storageLocation,
            fc.defaultPerishability AS perishability,
            el.expirationDate AS expirationDate,
            el.quantity AS quantity
        FROM expiry_lots el
        INNER JOIN food_categories fc ON fc.id = el.categoryId
        WHERE el.quantity > 0
          AND fc.id IN (:categoryIds)
        ORDER BY el.expirationDate ASC
        """
    )
    suspend fun getActiveLotsForCategories(categoryIds: List<Long>): List<LotWithCategoryProjection>
}
