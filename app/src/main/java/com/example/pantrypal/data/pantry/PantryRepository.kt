package com.example.pantrypal.data.pantry

import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodDetailData
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.StorageLocationFilter
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface PantryRepository {
    fun observePantryRows(filter: StorageLocationFilter): Flow<List<PantryRow>>
    fun observeFoodDetail(categoryId: Long): Flow<FoodDetailData?>
    suspend fun getActiveLotsWithCategories(): List<LotWithCategory>
    suspend fun searchFoodCategories(query: String, limit: Int = 8): List<FoodCategory>
    suspend fun getFoodCategoryMatchSources(query: String, limit: Int = 32): List<FoodCategoryMatchSource>
    suspend fun findCategoryByNormalizedName(normalizedName: String): FoodCategory?
    suspend fun createFoodCategory(input: CreateFoodCategoryInput): Long
    suspend fun upsertExpiryLot(categoryId: Long, expirationDate: LocalDate, quantityDelta: Int)
    suspend fun decrementSingleLotCategory(categoryId: Long): Boolean
}
