package com.example.pantrypal.feature.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.PantryRow
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocationFilter
import com.example.pantrypal.domain.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PantryViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PantryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .flatMapLatest { settings ->
                    val filter = settings.pantryStorageFilter
                    _uiState.update { it.copy(selectedFilter = filter, isLoading = true) }
                    observeRows(filter).map { rows -> settings to rows }
                }
                .collect { (settings, rows) ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            pantryRows = rows.map(PantryRow::toUi),
                            expiringFoods = rows
                                .filter { it.isInExpirationThreshold(settings) }
                                .sortedBy { it.nearestExpirationDate }
                                .take(6)
                                .map { it.toExpiringUi() }
                        )
                    }
                }
            }
    }

    fun onEvent(event: PantryEvent) {
        viewModelScope.launch {
            when (event) {
                is PantryEvent.OnFilterSelected -> {
                    _uiState.update { it.copy(selectedFilter = event.filter) }
                    settingsRepository.updatePantryStorageFilter(event.filter)
                }
                is PantryEvent.OnFoodClick -> _effects.send(PantryEffect.NavigateToFoodDetail(event.categoryId))
                is PantryEvent.OnExpiringFoodClick -> _effects.send(PantryEffect.NavigateToFoodDetail(event.categoryId))
                is PantryEvent.OnMinusClick -> {
                    val changed = pantryRepository.decrementSingleLotCategory(event.row.categoryId)
                    if (!changed) {
                        _effects.send(PantryEffect.NavigateToFoodDetail(event.row.categoryId))
                    }
                }
                is PantryEvent.OnPlusClick -> _effects.send(PantryEffect.NavigateToFoodDetail(event.categoryId))
                PantryEvent.OnFabClick -> _effects.send(PantryEffect.OpenAddChoiceSheet)
            }
        }
    }

    private fun observeRows(filter: StorageLocationFilter): Flow<List<PantryRow>> =
        pantryRepository.observePantryRows(filter)
}

private fun PantryRow.isInExpirationThreshold(settings: UserSettings): Boolean {
    val date = nearestExpirationDate ?: return false
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    val threshold = when (perishability) {
        PerishabilityType.FRESH -> settings.freshNotificationDays
        PerishabilityType.LONG_LIFE -> settings.longLifeNotificationDays
    }
    return days <= threshold
}

private fun PantryRow.toUi(): PantryRowUi =
    PantryRowUi(
        categoryId = categoryId,
        name = name,
        storageLocation = storageLocation,
        perishability = perishability,
        totalQuantity = totalQuantity,
        expirationLabel = nearestExpirationDate?.toExpirationLabel().orEmpty(),
        lotCount = lotCount
    )

private fun PantryRow.toExpiringUi(): ExpiringFoodCardUi =
    ExpiringFoodCardUi(
        categoryId = categoryId,
        name = name,
        expiringQuantity = totalQuantity,
        expirationLabel = nearestExpirationDate?.toExpirationLabel().orEmpty(),
        storageLocation = storageLocation
    )

private fun LocalDate.toExpirationLabel(): String {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), this).toInt()
    return when {
        days < 0 -> "Scaduto"
        days == 0 -> "Scade oggi"
        days == 1 -> "tra 1 giorno"
        days < 30 -> "tra $days giorni"
        days < 60 -> "tra 1 mese"
        else -> "tra ${days / 30} mesi"
    }
}
