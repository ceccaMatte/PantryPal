package com.example.pantrypal.feature.addfood

import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation

data class ScanUiState(
    val isReading: Boolean = true,
    val torchEnabled: Boolean = false,
    val statusLabel: String = "Lettura in corso..."
)

data class ManualAddUiState(
    val query: String = "Pollo fritto",
    val selectedSuggestion: FoodSuggestionUi? = sampleSuggestions.first(),
    val suggestions: List<FoodSuggestionUi> = sampleSuggestions,
    val perishability: PerishabilityType = PerishabilityType.FRESH,
    val storageLocation: StorageLocation = StorageLocation.FRIDGE,
    val lots: List<ManualLotUi> = listOf(ManualLotUi("20 Giu 2026", 1, "tra 7 giorni")),
    val canSave: Boolean = true
)

data class FoodSuggestionUi(
    val id: Long?,
    val label: String,
    val storageLocation: StorageLocation?,
    val isCreateNew: Boolean = false
)

data class ManualLotUi(
    val dateLabel: String,
    val quantity: Int,
    val expirationLabel: String
)

sealed interface ScanEvent {
    data object OnBackClick : ScanEvent
    data object OnTorchClick : ScanEvent
    data object OnManualClick : ScanEvent
}

sealed interface ManualAddEvent {
    data object OnBackClick : ManualAddEvent
    data class OnQueryChange(val value: String) : ManualAddEvent
    data class OnSuggestionSelected(val suggestion: FoodSuggestionUi) : ManualAddEvent
    data class OnPerishabilitySelected(val perishability: PerishabilityType) : ManualAddEvent
    data class OnStorageLocationSelected(val storageLocation: StorageLocation) : ManualAddEvent
    data object OnAddLotClick : ManualAddEvent
    data object OnSaveClick : ManualAddEvent
}

sealed interface AddFoodEffect {
    data object FinishAddFlow : AddFoodEffect
    data object NavigateToManualAdd : AddFoodEffect
    data class ShowSnackbar(val message: String) : AddFoodEffect
}

private val sampleSuggestions = listOf(
    FoodSuggestionUi(10, "Pollo fritto", StorageLocation.FRIDGE),
    FoodSuggestionUi(11, "Pollo", StorageLocation.FRIDGE),
    FoodSuggestionUi(12, "Cotoletta", StorageLocation.FREEZER),
    FoodSuggestionUi(null, "Crea nuovo", null, isCreateNew = true)
)
