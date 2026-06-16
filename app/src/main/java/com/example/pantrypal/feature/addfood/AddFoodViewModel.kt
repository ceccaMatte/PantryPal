package com.example.pantrypal.feature.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.domain.matching.FoodCategoryMatcher
import com.example.pantrypal.domain.model.AddFoodCategorySelection
import com.example.pantrypal.domain.model.SaveAddedFoodCommand
import com.example.pantrypal.domain.model.SaveAddedFoodResult
import com.example.pantrypal.domain.usecase.SaveAddedFoodUseCase
import com.example.pantrypal.core.util.TextNormalizer
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
    private val textNormalizer: TextNormalizer
) : ViewModel() {
    private val _scanState = MutableStateFlow(ScanUiState())
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _manualState = MutableStateFlow(ManualAddUiState())
    val manualState: StateFlow<ManualAddUiState> = _manualState.asStateFlow()

    private val _effects = Channel<AddFoodEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch { refreshSuggestions("") }
    }

    fun onScanEvent(event: ScanEvent) {
        viewModelScope.launch {
            when (event) {
                ScanEvent.OnBackClick -> _effects.send(AddFoodEffect.FinishAddFlow)
                ScanEvent.OnManualClick -> _effects.send(AddFoodEffect.NavigateToManualAdd)
                ScanEvent.OnTorchClick -> _scanState.update { it.copy(torchEnabled = !it.torchEnabled) }
            }
        }
    }

    fun onManualEvent(event: ManualAddEvent) {
        viewModelScope.launch {
            when (event) {
                ManualAddEvent.OnBackClick -> _effects.send(AddFoodEffect.FinishAddFlow)
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
                is ManualAddEvent.OnExpirationDateSelected -> _manualState.update {
                    it.copy(
                        expirationDate = event.date,
                        validationErrors = it.validationErrors - com.example.pantrypal.domain.model.SaveAddedFoodValidationError.DATE_REQUIRED
                    )
                }
                ManualAddEvent.OnQuantityMinus -> _manualState.update {
                    it.copy(quantity = (it.quantity - 1).coerceAtLeast(0))
                }
                ManualAddEvent.OnQuantityPlus -> _manualState.update {
                    it.copy(quantity = it.quantity + 1)
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
                expirationDate = state.expirationDate,
                quantity = state.quantity,
                storageLocation = state.storageLocation,
                perishability = state.perishability
            )
        )) {
            is SaveAddedFoodResult.Success -> {
                _manualState.value = ManualAddUiState()
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
}
