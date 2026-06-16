package com.example.pantrypal.feature.pantry

import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter

data class PantryUiState(
    val isLoading: Boolean = false,
    val selectedFilter: StorageLocationFilter = StorageLocationFilter.ALL,
    val expiringFoods: List<ExpiringFoodCardUi> = sampleExpiringFoods,
    val pantryRows: List<PantryRowUi> = samplePantryRows,
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

private val sampleExpiringFoods = listOf(
    ExpiringFoodCardUi(1, "Latte", 3, "Scade oggi", StorageLocation.FRIDGE),
    ExpiringFoodCardUi(2, "Yogurt Greco", 2, "tra 2 gg", StorageLocation.FRIDGE),
    ExpiringFoodCardUi(3, "Avocado", 1, "tra 3 gg", StorageLocation.FRIDGE)
)

private val samplePantryRows = listOf(
    PantryRowUi(1, "Latte", StorageLocation.FRIDGE, PerishabilityType.FRESH, 1, "Scade oggi", 1),
    PantryRowUi(2, "Yogurt Greco", StorageLocation.FRIDGE, PerishabilityType.FRESH, 2, "tra 2 giorni", 1),
    PantryRowUi(4, "Petto di Pollo", StorageLocation.FREEZER, PerishabilityType.FRESH, 1, "tra 4 giorni", 1),
    PantryRowUi(5, "Piselli", StorageLocation.FREEZER, PerishabilityType.LONG_LIFE, 2, "tra 3 mesi", 1),
    PantryRowUi(6, "Biscotti", StorageLocation.PANTRY, PerishabilityType.LONG_LIFE, 3, "1 scaduta", 2),
    PantryRowUi(7, "Passata", StorageLocation.PANTRY, PerishabilityType.LONG_LIFE, 1, "tra 4 mesi", 1)
)
