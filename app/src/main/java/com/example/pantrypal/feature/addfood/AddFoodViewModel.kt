package com.example.pantrypal.feature.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.domain.model.BarcodeProductDraft
import com.example.pantrypal.domain.model.BarcodeProductLink
import com.example.pantrypal.domain.model.BarcodeResolution
import com.example.pantrypal.domain.matching.FoodCategoryMatcher
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.AddFoodLotDraft
import com.example.pantrypal.domain.model.FoodCategory
import com.example.pantrypal.domain.model.SaveAddedFoodCommand
import com.example.pantrypal.domain.model.SaveAddedFoodResult
import com.example.pantrypal.domain.usecase.SaveAddedFoodUseCase
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.domain.model.toBarcodeProductDraft
import com.example.pantrypal.domain.usecase.ResolveBarcodeUseCase
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val foodCategoryMatcher: FoodCategoryMatcher,
    private val saveAddedFoodUseCase: SaveAddedFoodUseCase,
    private val resolveBarcodeUseCase: ResolveBarcodeUseCase,
    private val addFoodFlowStore: AddFoodFlowStore,
    private val textNormalizer: TextNormalizer
) : ViewModel() {
    private val _scanState = MutableStateFlow(ScanUiState())
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _manualState = MutableStateFlow(ManualAddUiState())
    val manualState: StateFlow<ManualAddUiState> = _manualState.asStateFlow()

    private val _effects = Channel<AddFoodEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var pendingRemoteResolution: BarcodeResolution.FoundRemote? = null
    private var manualBarcodeProductDraft: BarcodeProductDraft? = null
    private var nextManualLotId = 2L

    init {
        viewModelScope.launch { refreshSuggestions("") }
    }

    fun onManualRouteStarted() {
        viewModelScope.launch {
            val prefill = addFoodFlowStore.consumePrefill() ?: return@launch
            manualBarcodeProductDraft = prefill.barcodeProductDraft
            val selectedCategory = prefill.selectedCategoryId?.let { pantryRepository.getFoodCategory(it) }
            _manualState.update {
                it.copy(
                    query = prefill.query,
                    selectedSuggestion = selectedCategory?.toSuggestionUi(),
                    recognizedProductLabel = prefill.barcodeProductDraft?.productName,
                    storageLocation = selectedCategory?.defaultStorageLocation ?: it.storageLocation,
                    perishability = selectedCategory?.defaultPerishability ?: it.perishability,
                    validationErrors = emptySet()
                )
            }
            refreshSuggestions(prefill.query)
        }
    }

    fun onScanEvent(event: ScanEvent) {
        viewModelScope.launch {
            when (event) {
                ScanEvent.OnBackClick -> {
                    addFoodFlowStore.clear()
                    _effects.send(AddFoodEffect.FinishAddFlow)
                }
                ScanEvent.OnManualClick -> {
                    addFoodFlowStore.clear()
                    _effects.send(AddFoodEffect.NavigateToManualAdd)
                }
                ScanEvent.OnTorchClick -> _scanState.update { it.copy(torchEnabled = !it.torchEnabled) }
                is ScanEvent.OnBarcodeChange -> _scanState.update {
                    it.copy(barcodeInput = event.value, recognizedProduct = null)
                }
                ScanEvent.OnSearchBarcodeClick -> resolveBarcode(_scanState.value.barcodeInput.trim())
                ScanEvent.OnUseRecognizedProductClick -> useRecognizedProduct()
                ScanEvent.OnDismissRecognizedProduct -> {
                    pendingRemoteResolution = null
                    _scanState.update {
                        it.copy(
                            recognizedProduct = null,
                            isProcessingBarcode = false,
                            isLookingUp = false,
                            detectedBarcode = null,
                            showRetryButton = false,
                            analyzerResetKey = it.analyzerResetKey + 1
                        )
                    }
                }
                ScanEvent.OnRequestCameraPermissionClick -> {
                    _scanState.update { it.copy(isRequestingPermission = true) }
                    _effects.send(AddFoodEffect.RequestCameraPermission)
                }
                is ScanEvent.OnCameraPermissionResult -> {
                    _scanState.update {
                        it.copy(hasCameraPermission = event.granted, isRequestingPermission = false)
                    }
                }
                is ScanEvent.OnBarcodeDetected -> {
                    if (_scanState.value.isProcessingBarcode) {
                        Log.d(TAG, "Barcode ignored because already processing: ${event.value}")
                        return@launch
                    }
                    val barcode = event.value.trim()
                    if (!isSupportedProductBarcode(barcode)) {
                        Log.d(TAG, "Barcode ignored because invalid: $barcode")
                        return@launch
                    }
                    _scanState.update {
                        it.copy(isProcessingBarcode = true, detectedBarcode = barcode, showRetryButton = false)
                    }
                    resolveBarcode(barcode)
                }
                ScanEvent.OnRetryScanClick -> {
                    _scanState.update {
                        it.copy(
                            isProcessingBarcode = false,
                            isLookingUp = false,
                            detectedBarcode = null,
                            statusLabel = "Inquadra il codice a barre",
                            recognizedProduct = null,
                            showRetryButton = false,
                            analyzerResetKey = it.analyzerResetKey + 1
                        )
                    }
                }
            }
        }
    }

    fun onManualEvent(event: ManualAddEvent) {
        viewModelScope.launch {
            when (event) {
                ManualAddEvent.OnBackClick -> {
                    clearAddFlowState()
                    _effects.send(AddFoodEffect.FinishAddFlow)
                }
                is ManualAddEvent.OnQueryChange -> {
                    _manualState.update {
                        it.copy(
                            query = event.value,
                            selectedSuggestion = null,
                            validationErrors = emptySet()
                        )
                    }
                    refreshSuggestions(event.value)
                }
                is ManualAddEvent.OnSuggestionSelected -> _manualState.update {
                    it.copy(
                        selectedSuggestion = event.suggestion,
                        storageLocation = event.suggestion.storageLocation ?: it.storageLocation,
                        perishability = event.suggestion.perishability ?: it.perishability,
                        validationErrors = it.validationErrors - com.example.pantrypal.domain.model.SaveAddedFoodValidationError.CATEGORY_REQUIRED
                    )
                }
                is ManualAddEvent.OnPerishabilitySelected -> _manualState.update { it.copy(perishability = event.perishability) }
                is ManualAddEvent.OnStorageLocationSelected -> _manualState.update { it.copy(storageLocation = event.storageLocation) }
                ManualAddEvent.OnAddLotClick -> _manualState.update {
                    it.copy(
                        lots = it.lots + ManualAddLotUi(id = nextManualLotId++),
                        validationErrors = it.validationErrors - com.example.pantrypal.domain.model.SaveAddedFoodValidationError.LOTS_REQUIRED
                    )
                }
                is ManualAddEvent.OnRemoveLotClick -> _manualState.update {
                    it.copy(lots = it.lots.filterNot { lot -> lot.id == event.lotId })
                }
                is ManualAddEvent.OnExpirationDateSelected -> _manualState.update {
                    it.copy(
                        lots = it.lots.map { lot -> if (lot.id == event.lotId) lot.copy(expirationDate = event.date) else lot },
                        validationErrors = it.validationErrors - com.example.pantrypal.domain.model.SaveAddedFoodValidationError.DATE_REQUIRED
                    )
                }
                is ManualAddEvent.OnQuantityMinus -> _manualState.update {
                    it.copy(lots = it.lots.map { lot ->
                        if (lot.id == event.lotId) lot.copy(quantity = (lot.quantity - 1).coerceAtLeast(0)) else lot
                    })
                }
                is ManualAddEvent.OnQuantityPlus -> _manualState.update {
                    it.copy(lots = it.lots.map { lot ->
                        if (lot.id == event.lotId) lot.copy(quantity = lot.quantity + 1) else lot
                    })
                }
                ManualAddEvent.OnSaveClick -> saveManualFood()
            }
        }
    }

    private suspend fun refreshSuggestions(query: String) {
        val sources = pantryRepository.getFoodCategoryMatchSources(query)
        val sourceByCategoryId = sources.associateBy { it.category.id }
        val matches = foodCategoryMatcher.match(query, sources).map {
            val source = sourceByCategoryId[it.categoryId]
            FoodSuggestionUi(
                id = it.categoryId,
                label = it.name,
                storageLocation = source?.category?.defaultStorageLocation,
                perishability = source?.category?.defaultPerishability
            )
        }
        val normalizedQuery = textNormalizer.normalizeFoodText(query)
        val hasExactCategoryOrAlias = normalizedQuery.isNotBlank() && sources.any { source ->
            source.category.normalizedName == normalizedQuery ||
                source.aliases.any { it.normalizedAlias == normalizedQuery }
        }
        val createNew = FoodSuggestionUi(
            id = null,
            label = "Crea nuovo",
            storageLocation = null,
            perishability = null,
            isCreateNew = true
        )
        _manualState.update {
            it.copy(
                suggestions = if (!hasExactCategoryOrAlias && normalizedQuery.isNotBlank()) {
                    matches + createNew
                } else {
                    matches
                }
            )
        }
    }

    private suspend fun saveManualFood() {
        val state = _manualState.value
        _manualState.update { it.copy(isSaving = true, validationErrors = emptySet()) }

        val selection = when {
            state.selectedSuggestion?.isCreateNew == true -> AddFoodCategorySelection.New(
                name = state.query,
                storageLocation = state.storageLocation,
                perishability = state.perishability
            )
            state.selectedSuggestion?.id != null -> AddFoodCategorySelection.Existing(requireNotNull(state.selectedSuggestion.id))
            else -> null
        }

        when (val result = saveAddedFoodUseCase(
            SaveAddedFoodCommand(
                categorySelection = selection,
                lots = state.lots.map { AddFoodLotDraft(it.expirationDate, it.quantity) },
                storageLocation = state.storageLocation,
                perishability = state.perishability,
                barcodeProductDraft = manualBarcodeProductDraft
            )
        )) {
            is SaveAddedFoodResult.Success -> {
                clearAddFlowState()
                refreshSuggestions("")
                _effects.send(AddFoodEffect.FinishAddFlow)
            }
            is SaveAddedFoodResult.ValidationError -> _manualState.update {
                it.copy(isSaving = false, validationErrors = result.errors)
            }
            SaveAddedFoodResult.StorageError -> {
                _manualState.update { it.copy(isSaving = false) }
                _effects.send(AddFoodEffect.ShowSnackbar("Errore durante il salvataggio"))
            }
        }
    }

    private suspend fun resolveBarcode(barcode: String) {
        if (barcode.isBlank()) {
            _scanState.update { it.copy(isProcessingBarcode = false) }
            _effects.send(AddFoodEffect.ShowSnackbar("Inserisci un barcode"))
            return
        }
        // Capture whether this resolve was triggered by the camera before any state changes
        val isFromCamera = _scanState.value.detectedBarcode != null
        Log.d(TAG, "ResolveBarcode start: $barcode isFromCamera=$isFromCamera")
        _scanState.update {
            it.copy(isLookingUp = true, isReading = false, statusLabel = "Ricerca prodotto...", recognizedProduct = null)
        }
        when (val resolution = resolveBarcodeUseCase(barcode)) {
            is BarcodeResolution.KnownLocal -> {
                if (isFromCamera && _scanState.value.detectedBarcode != barcode) {
                    Log.d(TAG, "Ignoring stale barcode result: $barcode")
                    _scanState.update { it.copy(isLookingUp = false) }
                    return
                }
                Log.d(TAG, "ResolveBarcode result: $barcode -> KnownLocal")
                addFoodFlowStore.setPrefill(
                    AddFoodPrefill(
                        query = resolution.category.name,
                        selectedCategoryId = resolution.category.id,
                        barcodeProductDraft = resolution.link.toDraft()
                    )
                )
                _scanState.update {
                    it.copy(
                        isLookingUp = false, isProcessingBarcode = false,
                        detectedBarcode = null, statusLabel = "Prodotto già collegato"
                    )
                }
                _effects.send(AddFoodEffect.NavigateToManualAdd)
            }
            is BarcodeResolution.FoundRemote -> {
                if (isFromCamera && _scanState.value.detectedBarcode != barcode) {
                    Log.d(TAG, "Ignoring stale barcode result: $barcode")
                    _scanState.update { it.copy(isLookingUp = false) }
                    return
                }
                Log.d(TAG, "ResolveBarcode result: $barcode -> FoundRemote")
                pendingRemoteResolution = resolution
                _scanState.update {
                    it.copy(
                        isLookingUp = false,
                        isProcessingBarcode = false,
                        statusLabel = "Prodotto riconosciuto",
                        recognizedProduct = resolution.toUi()
                    )
                }
            }
            BarcodeResolution.NotFound -> {
                _scanState.update { it.copy(isProcessingBarcode = false, isLookingUp = false) }
                Log.d(TAG, "ResolveBarcode result: $barcode -> NotFound")
                if (isFromCamera) {
                    if (_scanState.value.detectedBarcode != barcode) {
                        Log.d(TAG, "Ignoring stale barcode result: $barcode")
                        return
                    }
                    _scanState.update {
                        it.copy(statusLabel = "Barcode non riconosciuto", showRetryButton = true, detectedBarcode = null)
                    }
                } else {
                    continueManuallyWithMessage("Barcode non riconosciuto. Inseriscilo manualmente.")
                }
            }
            BarcodeResolution.NetworkError -> {
                _scanState.update { it.copy(isProcessingBarcode = false, isLookingUp = false) }
                Log.d(TAG, "ResolveBarcode result: $barcode -> NetworkError")
                if (isFromCamera) {
                    if (_scanState.value.detectedBarcode != barcode) {
                        Log.d(TAG, "Ignoring stale barcode result: $barcode")
                        return
                    }
                    _scanState.update {
                        it.copy(
                            statusLabel = "Impossibile cercare il prodotto",
                            showRetryButton = true, detectedBarcode = null
                        )
                    }
                    _effects.send(AddFoodEffect.ShowSnackbar("Impossibile cercare il prodotto. Controlla la connessione."))
                } else {
                    continueManuallyWithMessage("Impossibile cercare il prodotto. Puoi inserirlo manualmente.")
                }
            }
            BarcodeResolution.InvalidResponse -> {
                _scanState.update { it.copy(isProcessingBarcode = false, isLookingUp = false) }
                Log.d(TAG, "ResolveBarcode result: $barcode -> InvalidResponse")
                if (isFromCamera) {
                    if (_scanState.value.detectedBarcode != barcode) {
                        Log.d(TAG, "Ignoring stale barcode result: $barcode")
                        return
                    }
                    _scanState.update {
                        it.copy(
                            statusLabel = "Risposta prodotto non valida",
                            showRetryButton = true, detectedBarcode = null
                        )
                    }
                } else {
                    continueManuallyWithMessage("Risposta prodotto non valida. Puoi inserirlo manualmente.")
                }
            }
            BarcodeResolution.RateLimited -> {
                _scanState.update { it.copy(isProcessingBarcode = false, isLookingUp = false) }
                Log.d(TAG, "ResolveBarcode result: $barcode -> RateLimited")
                if (isFromCamera) {
                    if (_scanState.value.detectedBarcode != barcode) {
                        Log.d(TAG, "Ignoring stale barcode result: $barcode")
                        return
                    }
                    _scanState.update {
                        it.copy(
                            statusLabel = "Servizio non disponibile, riprova",
                            showRetryButton = true, detectedBarcode = null
                        )
                    }
                    _effects.send(AddFoodEffect.ShowSnackbar("Servizio temporaneamente non disponibile."))
                } else {
                    continueManuallyWithMessage("Servizio temporaneamente non disponibile. Puoi inserirlo manualmente.")
                }
            }
        }
    }

    private fun isSupportedProductBarcode(value: String): Boolean {
        val v = value.trim()
        return v.all { it.isDigit() } && v.length in setOf(8, 12, 13)
    }

    private suspend fun useRecognizedProduct() {
        val resolution = pendingRemoteResolution ?: return
        val product = resolution.product
        val query = product.genericName?.takeIf { it.isNotBlank() }
            ?: product.productName?.takeIf { it.isNotBlank() }
            ?: product.barcode
        addFoodFlowStore.setPrefill(
            AddFoodPrefill(
                query = query,
                selectedCategoryId = resolution.preselectedCategoryId,
                barcodeProductDraft = product.toBarcodeProductDraft()
            )
        )
        pendingRemoteResolution = null
        _effects.send(AddFoodEffect.NavigateToManualAdd)
    }

    private suspend fun continueManuallyWithMessage(message: String) {
        pendingRemoteResolution = null
        addFoodFlowStore.clear()
        _scanState.update {
            it.copy(isLookingUp = false, statusLabel = message, recognizedProduct = null)
        }
        _effects.send(AddFoodEffect.ShowSnackbar(message))
        _effects.send(AddFoodEffect.NavigateToManualAdd)
    }

    private suspend fun clearAddFlowState() {
        addFoodFlowStore.clear()
        pendingRemoteResolution = null
        manualBarcodeProductDraft = null
        _manualState.value = ManualAddUiState()
        nextManualLotId = 2L
    }

    private fun FoodCategory.toSuggestionUi(): FoodSuggestionUi =
        FoodSuggestionUi(
            id = id,
            label = name,
            storageLocation = defaultStorageLocation,
            perishability = defaultPerishability
        )

    private fun BarcodeProductLink.toDraft(): BarcodeProductDraft =
        BarcodeProductDraft(
            barcode = barcode,
            productName = productName,
            genericName = genericName,
            brand = brand,
            quantityLabel = quantityLabel,
            imageUrl = imageUrl,
            rawCategoryTags = rawCategoryTags?.split("|").orEmpty().filter { it.isNotBlank() },
            rawFoodGroupTags = rawFoodGroupTags?.split("|").orEmpty().filter { it.isNotBlank() }
        )

    companion object {
        private const val TAG = "PantryPalScan"
    }

    private fun BarcodeResolution.FoundRemote.toUi(): ProductRecognizedUi =
        ProductRecognizedUi(
            barcode = product.barcode,
            title = product.productName?.takeIf { it.isNotBlank() }
                ?: product.genericName?.takeIf { it.isNotBlank() }
                ?: product.barcode,
            subtitle = listOfNotNull(product.brand, product.genericName)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
                .ifBlank { "Prodotto trovato su Open Food Facts" },
            quantityLabel = product.quantityLabel,
            suggestedCategoryLabels = suggestions.map { it.name },
            preselectedCategoryId = preselectedCategoryId
        )
}
