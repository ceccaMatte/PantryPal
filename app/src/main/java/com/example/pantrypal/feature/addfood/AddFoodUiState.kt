package com.example.pantrypal.feature.addfood

import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.SaveAddedFoodValidationError
import com.example.pantrypal.domain.model.StorageLocation
import java.time.LocalDate

data class ScanUiState(
    val isReading: Boolean = false,
    val torchEnabled: Boolean = false,
    val statusLabel: String = "Inquadra il codice a barre",
    val barcodeInput: String = "",
    val isLookingUp: Boolean = false,
    val recognizedProduct: ProductRecognizedUi? = null,
    val hasCameraPermission: Boolean = false,
    val isRequestingPermission: Boolean = false,
    val isCameraReady: Boolean = false,
    val isProcessingBarcode: Boolean = false,
    val detectedBarcode: String? = null,
    val analyzerResetKey: Int = 0,
    val showRetryButton: Boolean = false,
)

data class ProductRecognizedUi(
    val barcode: String,
    val title: String,
    val subtitle: String,
    val quantityLabel: String?,
    val imageUrl: String?,
    val suggestedCategoryLabels: List<String>,
    val preselectedCategoryId: Long?
)

data class ManualAddUiState(
    val query: String = "",
    val selectedSuggestion: FoodSuggestionUi? = null,
    val suggestions: List<FoodSuggestionUi> = emptyList(),
    val recognizedProductLabel: String? = null,
    val perishability: PerishabilityType = PerishabilityType.FRESH,
    val storageLocation: StorageLocation = StorageLocation.FRIDGE,
    val lots: List<ManualAddLotUi> = listOf(ManualAddLotUi(id = 1)),
    val validationErrors: Set<SaveAddedFoodValidationError> = emptySet(),
    val isSaving: Boolean = false
)

data class ManualAddLotUi(
    val id: Long,
    val expirationDate: LocalDate? = null,
    val quantity: Int = 1
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
    data class OnBarcodeChange(val value: String) : ScanEvent
    data object OnSearchBarcodeClick : ScanEvent
    data object OnUseRecognizedProductClick : ScanEvent
    data object OnDismissRecognizedProduct : ScanEvent
    data object OnRequestCameraPermissionClick : ScanEvent
    data class OnCameraPermissionResult(val granted: Boolean) : ScanEvent
    data class OnBarcodeDetected(val value: String) : ScanEvent
    data object OnRetryScanClick : ScanEvent
}

sealed interface ManualAddEvent {
    data object OnBackClick : ManualAddEvent
    data class OnQueryChange(val value: String) : ManualAddEvent
    data class OnSuggestionSelected(val suggestion: FoodSuggestionUi) : ManualAddEvent
    data class OnPerishabilitySelected(val perishability: PerishabilityType) : ManualAddEvent
    data class OnStorageLocationSelected(val storageLocation: StorageLocation) : ManualAddEvent
    data object OnAddLotClick : ManualAddEvent
    data class OnRemoveLotClick(val lotId: Long) : ManualAddEvent
    data class OnExpirationDateSelected(val lotId: Long, val date: LocalDate) : ManualAddEvent
    data class OnQuantityMinus(val lotId: Long) : ManualAddEvent
    data class OnQuantityPlus(val lotId: Long) : ManualAddEvent
    data object OnSaveClick : ManualAddEvent
}

sealed interface AddFoodEffect {
    data object FinishAddFlow : AddFoodEffect
    data object NavigateToManualAdd : AddFoodEffect
    data class ShowSnackbar(val message: String) : AddFoodEffect
    data object RequestCameraPermission : AddFoodEffect
}
