package com.example.pantrypal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pantrypal.core.database.entity.BarcodeProductLinkEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeProductLinkDao {
    @Query(
        """
        SELECT *
        FROM barcode_product_links
        WHERE barcode = :barcode
          AND isActive = 1
        LIMIT 1
        """
    )
    suspend fun findActiveByBarcode(barcode: String): BarcodeProductLinkEntity?

    @Query(
        """
        SELECT *
        FROM barcode_product_links
        WHERE categoryId = :categoryId
          AND isActive = 1
        ORDER BY updatedAt DESC
        """
    )
    fun observeActiveLinksForCategory(categoryId: Long): Flow<List<BarcodeProductLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: BarcodeProductLinkEntity)

    @Query(
        """
        UPDATE barcode_product_links
        SET isActive = 0,
            updatedAt = :updatedAt
        WHERE barcode = :barcode
        """
    )
    suspend fun deactivateByBarcode(barcode: String, updatedAt: Instant)
}
