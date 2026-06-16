package com.example.pantrypal.domain.model

import java.time.LocalDate

sealed interface AddFoodCategorySelection {
    data class Existing(val categoryId: Long) : AddFoodCategorySelection
    data class New(
        val name: String,
        val storageLocation: StorageLocation,
        val perishability: PerishabilityType
    ) : AddFoodCategorySelection
}

data class SaveAddedFoodCommand(
    val categorySelection: AddFoodCategorySelection?,
    val expirationDate: LocalDate?,
    val quantity: Int,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType
)

enum class SaveAddedFoodValidationError {
    CATEGORY_REQUIRED,
    DATE_REQUIRED,
    QUANTITY_INVALID
}

sealed interface SaveAddedFoodResult {
    data class Success(val categoryId: Long) : SaveAddedFoodResult
    data class ValidationError(val errors: Set<SaveAddedFoodValidationError>) : SaveAddedFoodResult
    data object StorageError : SaveAddedFoodResult
}

data class FoodDetailDraft(
    val categoryId: Long,
    val name: String,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val lots: List<FoodLotDraft>
)

data class FoodLotDraft(
    val id: Long?,
    val expirationDate: LocalDate,
    val quantity: Int
)

enum class SaveFoodDetailValidationError {
    NAME_REQUIRED
}

sealed interface SaveFoodDetailChangesResult {
    data object Success : SaveFoodDetailChangesResult
    data class ValidationError(val errors: Set<SaveFoodDetailValidationError>) : SaveFoodDetailChangesResult
    data object StorageError : SaveFoodDetailChangesResult
}

data class HomeOverview(
    val username: String?,
    val totalPackages: Int,
    val fridgePackages: Int,
    val freezerPackages: Int,
    val pantryPackages: Int,
    val expiringFoods: List<HomeExpiringFood>
)

data class HomeExpiringFood(
    val categoryId: Long,
    val name: String,
    val expiringQuantity: Int,
    val storageLocation: StorageLocation
)
