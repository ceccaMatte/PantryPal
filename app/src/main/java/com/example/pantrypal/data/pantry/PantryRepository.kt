package com.example.pantrypal.data.pantry

import com.example.pantrypal.domain.model.CreateFoodCategoryInput
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.AddFoodLotDraft
import com.example.pantrypal.domain.model.BarcodeProductDraft
import com.example.pantrypal.domain.model.BarcodeProductLink
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.FoodCategoryMatchSource
import com.example.pantrypal.domain.model.FoodDetailDraft
import com.example.pantrypal.domain.model.FoodDetailData
import com.example.pantrypal.domain.model.LotWithCategory
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.StorageLocationFilter
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface PantryRepository {
    fun observePantryRows(filter: StorageLocationFilter): Flow<List<PantryRow>>
    fun observeFoodDetail(categoryId: Long): Flow<FoodDetailData?>
    fun observeActiveLotsWithCategories(): Flow<List<LotWithCategory>>
    suspend fun getActiveLotsWithCategories(): List<LotWithCategory>
    suspend fun getActiveLotsForCategories(categoryIds: List<Long>): List<LotWithCategory>
    suspend fun getFoodCategory(categoryId: Long): FoodCategory?
    suspend fun searchFoodCategories(query: String, limit: Int = 8): List<FoodCategory>
    suspend fun getFoodCategoryMatchSources(query: String, limit: Int = 32): List<FoodCategoryMatchSource>
    suspend fun findCategoryByNormalizedName(normalizedName: String): FoodCategory?
    suspend fun findActiveBarcodeLink(barcode: String): BarcodeProductLink?
    suspend fun createFoodCategory(input: CreateFoodCategoryInput): Long
    suspend fun saveAddedFood(
        categorySelection: AddFoodCategorySelection,
        lots: List<AddFoodLotDraft>,
        barcodeProductDraft: BarcodeProductDraft? = null
    ): Long
    suspend fun saveFoodDetailChanges(draft: FoodDetailDraft)
    suspend fun upsertExpiryLot(categoryId: Long, expirationDate: LocalDate, quantityDelta: Int)
    suspend fun decrementSingleLotCategory(categoryId: Long): Boolean
    suspend fun addRecipeIngredientAlias(categoryId: Long, aliasOriginal: String, language: String?): Long
    suspend fun removeRecipeIngredientAlias(linkId: Long)
    suspend fun deactivateBarcodeLink(barcode: String)
}
