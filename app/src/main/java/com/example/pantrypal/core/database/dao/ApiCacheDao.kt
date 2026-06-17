package com.example.pantrypal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pantrypal.core.database.entity.ApiCacheEntryEntity
import java.time.Instant

@Dao
interface ApiCacheDao {
    @Query("SELECT * FROM api_cache_entries WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): ApiCacheEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ApiCacheEntryEntity)

    @Query("DELETE FROM api_cache_entries WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Instant)
}
