package com.example.pantrypal.feature.pantry

import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation

data class FoodDetailUiState(
    val categoryId: Long = 1L,
    val name: String = "Uova Fresche",
    val totalQuantity: Int = 3,
    val updatedLabel: String = "aggiornato oggi",
    val storageLocation: StorageLocation = StorageLocation.FRIDGE,
    val perishability: PerishabilityType = PerishabilityType.FRESH,
    val lots: List<FoodLotUi> = sampleLots,
    val scannedProducts: List<ProductLinkUi> = sampleProducts,
    val recipeAliases: List<String> = sampleAliases,
    val isLoading: Boolean = false
)

data class FoodLotUi(
    val id: Long,
    val dateLabel: String,
    val expirationLabel: String,
    val quantity: Int,
    val isExpired: Boolean = false
)

data class ProductLinkUi(
    val barcode: String,
    val productName: String,
    val subtitle: String
)

sealed interface FoodDetailEvent {
    data object OnBackClick : FoodDetailEvent
    data object OnAddLotClick : FoodDetailEvent
    data object OnManageLinksClick : FoodDetailEvent
    data class OnLotMinusClick(val lotId: Long) : FoodDetailEvent
    data class OnLotPlusClick(val lotId: Long) : FoodDetailEvent
}

sealed interface FoodDetailEffect {
    data object NavigateBack : FoodDetailEffect
    data object NavigateToLinks : FoodDetailEffect
    data class ShowSnackbar(val message: String) : FoodDetailEffect
}

private val sampleLots = listOf(
    FoodLotUi(1, "15 Ottobre 2025", "Scaduta", 1, isExpired = true),
    FoodLotUi(2, "19 Ottobre 2025", "tra 4 giorni", 1),
    FoodLotUi(3, "02 Novembre 2025", "tra 3 settimane", 1)
)

private val sampleProducts = listOf(
    ProductLinkUi("800001", "Chicken Nuggets Findus", "Riconosciuto come Pollo fritto"),
    ProductLinkUi("800002", "Crispy Chicken Coop", "Riconosciuto come Pollo fritto")
)

private val sampleAliases = listOf("pollo fritto", "fried chicken", "breaded chicken", "chicken nuggets")
