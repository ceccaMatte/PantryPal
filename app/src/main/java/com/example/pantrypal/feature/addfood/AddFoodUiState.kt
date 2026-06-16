package com.example.pantrypal.feature.addfood

import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.SaveAddedFoodValidationError
import com.example.pantrypal.domain.model.StorageLocation
import java.time.LocalDate

data class ScanUiState(
    val isReading: Boolean = true,
    val torchEnabled: Boolean = false,
    val statusLabel: String = "Lettura in corso..."
)

data class ManualAddUiState(
    val query: String = "",
    val selectedSuggestion: FoodSuggestionUi? = null,
    val suggestions: List<FoodSuggestionUi> = emptyList(),
    val perishability: PerishabilityType = PerishabilityType.FRESH,
    val storageLocation: StorageLocation = StorageLocation.FRIDGE,
    val expirationDate: LocalDate? = null,
    val quantity: Int = 1,
    val validationErrors: Set<SaveAddedFoodValidationError> = emptySet(),
    val isSaving: Boolean = false
)

data class FoodSuggestionUi(
    val id: Long?,
    val label: String,
    val storageLocation: StorageLocation?,
    val perishability: PerishabilityType?,
    val isCreateNew: Boolean = false
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
    data class OnExpirationDateSelected(val date: LocalDate) : ManualAddEvent
    data object OnQuantityMinus : ManualAddEvent
    data object OnQuantityPlus : ManualAddEvent
    data object OnSaveClick : ManualAddEvent
}

sealed interface AddFoodEffect {
    data object FinishAddFlow : AddFoodEffect
    data object NavigateToManualAdd : AddFoodEffect
    data class ShowSnackbar(val message: String) : AddFoodEffect
}
