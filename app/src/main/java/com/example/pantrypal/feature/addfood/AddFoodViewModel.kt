package com.example.pantrypal.feature.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.domain.matching.FoodCategoryMatcher
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
    private val foodCategoryMatcher: FoodCategoryMatcher
) : ViewModel() {
    private val _scanState = MutableStateFlow(ScanUiState())
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _manualState = MutableStateFlow(ManualAddUiState())
    val manualState: StateFlow<ManualAddUiState> = _manualState.asStateFlow()

    private val _effects = Channel<AddFoodEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

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
                    _manualState.update { it.copy(query = event.value) }
                    refreshSuggestions(event.value)
                }
                is ManualAddEvent.OnSuggestionSelected -> _manualState.update { it.copy(selectedSuggestion = event.suggestion) }
                is ManualAddEvent.OnPerishabilitySelected -> _manualState.update { it.copy(perishability = event.perishability) }
                is ManualAddEvent.OnStorageLocationSelected -> _manualState.update { it.copy(storageLocation = event.storageLocation) }
                ManualAddEvent.OnAddLotClick -> _effects.send(AddFoodEffect.ShowSnackbar("Nuova scadenza pronta per lo step successivo"))
                ManualAddEvent.OnSaveClick -> _effects.send(AddFoodEffect.FinishAddFlow)
            }
        }
    }

    private suspend fun refreshSuggestions(query: String) {
        if (query.isBlank()) {
            _manualState.update { it.copy(suggestions = emptyList(), selectedSuggestion = null) }
            return
        }
        val sources = pantryRepository.getFoodCategoryMatchSources(query)
        val matches = foodCategoryMatcher.match(query, sources).map {
            FoodSuggestionUi(
                id = it.categoryId,
                label = it.name,
                storageLocation = null
            )
        }
        val createNew = FoodSuggestionUi(null, "Crea nuovo", null, isCreateNew = true)
        _manualState.update {
            it.copy(
                suggestions = (matches + createNew).ifEmpty { listOf(createNew) },
                selectedSuggestion = matches.firstOrNull() ?: it.selectedSuggestion
            )
        }
    }
}
