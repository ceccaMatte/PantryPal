package com.example.pantrypal.feature.pantry

import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import java.time.LocalDate

data class FoodDetailUiState(
    val categoryId: Long = 1L,
    val name: String = "",
    val imageUri: String? = null,
    val totalQuantity: Int = 0,
    val updatedLabel: String = "aggiornato oggi",
    val storageLocation: StorageLocation = StorageLocation.FRIDGE,
    val perishability: PerishabilityType = PerishabilityType.FRESH,
    val lots: List<FoodLotUi> = emptyList(),
    val scannedProducts: List<ProductLinkUi> = emptyList(),
    val recipeAliases: List<RecipeAliasUi> = emptyList(),
    val aliasDraft: String = "",
    val isLoading: Boolean = false,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

data class FoodLotUi(
    val id: Long,
    val expirationDate: LocalDate,
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

data class RecipeAliasUi(
    val id: Long,
    val alias: String,
    val language: String?
)

sealed interface FoodDetailEvent {
    data object OnBackClick : FoodDetailEvent
    data class OnNameChange(val value: String) : FoodDetailEvent
    data class OnStorageLocationSelected(val storageLocation: StorageLocation) : FoodDetailEvent
    data class OnPerishabilitySelected(val perishability: PerishabilityType) : FoodDetailEvent
    data class OnAddLotWithDate(val date: LocalDate) : FoodDetailEvent
    data class OnLotDateSelected(val lotId: Long, val date: LocalDate) : FoodDetailEvent
    data object OnSaveClick : FoodDetailEvent
    data object OnManageLinksClick : FoodDetailEvent
    data class OnLotMinusClick(val lotId: Long) : FoodDetailEvent
    data class OnLotPlusClick(val lotId: Long) : FoodDetailEvent
    data class OnAliasDraftChange(val value: String) : FoodDetailEvent
    data object OnAddAliasClick : FoodDetailEvent
    data class OnRemoveAliasClick(val aliasId: Long) : FoodDetailEvent
    data class OnRemoveBarcodeClick(val barcode: String) : FoodDetailEvent
}

sealed interface FoodDetailEffect {
    data object NavigateBack : FoodDetailEffect
    data object NavigateToLinks : FoodDetailEffect
    data class ShowSnackbar(val message: String) : FoodDetailEffect
}
