package com.example.pantrypal.feature.pantry

import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter

data class PantryUiState(
    val isLoading: Boolean = false,
    val selectedFilter: StorageLocationFilter = StorageLocationFilter.ALL,
    val expiringFoods: List<ExpiringFoodCardUi> = emptyList(),
    val pantryRows: List<PantryRowUi> = emptyList(),
    val focusedExpiringCategoryId: Long? = null,
    val errorMessage: String? = null
)

data class PantryRowUi(
    val categoryId: Long,
    val name: String,
    val storageLocation: StorageLocation,
    val perishability: PerishabilityType,
    val totalQuantity: Int,
    val expirationLabel: String,
    val lotCount: Int
)

data class ExpiringFoodCardUi(
    val categoryId: Long,
    val name: String,
    val expiringQuantity: Int,
    val expirationLabel: String,
    val storageLocation: StorageLocation,
    val isFocused: Boolean = false
)

sealed interface PantryEvent {
    data class OnFilterSelected(val filter: StorageLocationFilter) : PantryEvent
    data class OnFoodClick(val categoryId: Long) : PantryEvent
    data class OnExpiringFoodClick(val categoryId: Long) : PantryEvent
    data class OnMinusClick(val row: PantryRowUi) : PantryEvent
    data class OnPlusClick(val categoryId: Long) : PantryEvent
    data object OnFabClick : PantryEvent
}

sealed interface PantryEffect {
    data class NavigateToFoodDetail(val categoryId: Long) : PantryEffect
    data object OpenAddChoiceSheet : PantryEffect
    data class ShowSnackbar(val message: String) : PantryEffect
}
