package com.example.pantrypal.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pantrypal.core.database.entity.ExpiryLotEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpiryLotDao {
    @Query(
        """
        SELECT *
        FROM expiry_lots
        WHERE categoryId = :categoryId
          AND quantity > 0
        ORDER BY expirationDate ASC
        """
    )
    fun observeActiveLotsForCategory(categoryId: Long): Flow<List<ExpiryLotEntity>>

    @Query(
        """
        SELECT *
        FROM expiry_lots
        WHERE categoryId = :categoryId
          AND quantity > 0
        ORDER BY expirationDate ASC
        """
    )
    suspend fun getActiveLotsForCategory(categoryId: Long): List<ExpiryLotEntity>

    @Query(
        """
        SELECT *
        FROM expiry_lots
        WHERE categoryId = :categoryId
          AND expirationDate = :expirationDate
        LIMIT 1
        """
    )
    suspend fun getLotByCategoryAndDate(categoryId: Long, expirationDate: LocalDate): ExpiryLotEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(lot: ExpiryLotEntity): Long

    @Update
    suspend fun update(lot: ExpiryLotEntity)

    @Delete
    suspend fun delete(lot: ExpiryLotEntity)

    @Query("DELETE FROM expiry_lots WHERE id = :lotId")
    suspend fun deleteById(lotId: Long)

    @Query("DELETE FROM expiry_lots WHERE categoryId = :categoryId")
    suspend fun deleteAllLotsForCategory(categoryId: Long)

    @Query(
        """
        UPDATE expiry_lots
        SET quantity = quantity + :delta,
            updatedAt = :updatedAt
        WHERE id = :lotId
        """
    )
    suspend fun incrementLotQuantity(lotId: Long, delta: Int, updatedAt: Instant)

    @Query(
        """
        UPDATE expiry_lots
        SET quantity = quantity - 1,
            updatedAt = :updatedAt
        WHERE id = :lotId
          AND quantity > 0
        """
    )
    suspend fun decrementLotByOne(lotId: Long, updatedAt: Instant)

    @Query("DELETE FROM expiry_lots WHERE quantity <= 0")
    suspend fun deleteZeroQuantityLots()
}
