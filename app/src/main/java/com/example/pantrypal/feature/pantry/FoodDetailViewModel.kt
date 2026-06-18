package com.example.pantrypal.feature.pantry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.FoodDetailDraft
import com.example.pantrypal.domain.model.FoodDetailData
import com.example.pantrypal.domain.model.FoodLotDraft
import com.example.pantrypal.domain.model.SaveFoodDetailChangesResult
import com.example.pantrypal.domain.usecase.SaveFoodDetailChangesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pantryRepository: PantryRepository,
    private val settingsRepository: SettingsRepository,
    private val saveFoodDetailChangesUseCase: SaveFoodDetailChangesUseCase
) : ViewModel() {
    private val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: 1L
    private var nextDraftLotId = -1L

    private val _uiState = MutableStateFlow(FoodDetailUiState(categoryId = categoryId))
    val uiState: StateFlow<FoodDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FoodDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            pantryRepository.observeFoodDetail(categoryId).collect { detail ->
                if (detail != null && !_uiState.value.isDirty) {
                    _uiState.value = detail.toUi()
                }
            }
        }
    }

    fun onEvent(event: FoodDetailEvent) {
        viewModelScope.launch {
            when (event) {
                FoodDetailEvent.OnBackClick -> _effects.send(FoodDetailEffect.NavigateBack)
                FoodDetailEvent.OnManageLinksClick -> _effects.send(FoodDetailEffect.NavigateToLinks)
                is FoodDetailEvent.OnNameChange -> _uiState.update { it.copy(name = event.value, isDirty = true) }
                is FoodDetailEvent.OnStorageLocationSelected -> _uiState.update { it.copy(storageLocation = event.storageLocation, isDirty = true) }
                is FoodDetailEvent.OnPerishabilitySelected -> _uiState.update { it.copy(perishability = event.perishability, isDirty = true) }
                is FoodDetailEvent.OnAddLotWithDate -> addLot(event.date)
                is FoodDetailEvent.OnLotDateSelected -> updateLotDate(event.lotId, event.date)
                FoodDetailEvent.OnSaveClick -> saveChanges()
                is FoodDetailEvent.OnLotMinusClick -> updateLotQuantity(event.lotId, -1)
                is FoodDetailEvent.OnLotPlusClick -> updateLotQuantity(event.lotId, 1)
                is FoodDetailEvent.OnAliasDraftChange -> _uiState.update { it.copy(aliasDraft = event.value) }
                FoodDetailEvent.OnAddAliasClick -> addAlias()
                is FoodDetailEvent.OnRemoveAliasClick -> pantryRepository.removeRecipeIngredientAlias(event.aliasId)
                is FoodDetailEvent.OnRemoveBarcodeClick -> pantryRepository.deactivateBarcodeLink(event.barcode)
            }
        }
    }

    private fun addLot(date: LocalDate) {
        _uiState.update { state ->
            val existing = state.lots.firstOrNull { it.expirationDate == date }
            val lots = if (existing != null) {
                state.lots.map {
                    if (it.id == existing.id) it.withQuantity(it.quantity + 1) else it
                }
            } else {
                state.lots + FoodLotUi(
                    id = nextDraftLotId--,
                    expirationDate = date,
                    dateLabel = date.format(DateFormatter),
                    expirationLabel = date.toExpirationLabel(),
                    quantity = 1,
                    isExpired = date.isBefore(LocalDate.now())
                )
            }
            state.copyWithLots(lots.sortedBy { it.expirationDate })
        }
    }

    private fun updateLotDate(lotId: Long, date: LocalDate) {
        _uiState.update { state ->
            state.copyWithLots(
                state.lots.map {
                    if (it.id == lotId) {
                        it.copy(
                            expirationDate = date,
                            dateLabel = date.format(DateFormatter),
                            expirationLabel = date.toExpirationLabel(),
                            isExpired = date.isBefore(LocalDate.now())
                        )
                    } else {
                        it
                    }
                }.sortedBy { it.expirationDate }
            )
        }
    }

    private fun updateLotQuantity(lotId: Long, delta: Int) {
        _uiState.update { state ->
            val lots = state.lots.mapNotNull {
                if (it.id == lotId) {
                    val nextQuantity = it.quantity + delta
                    if (nextQuantity <= 0) null else it.withQuantity(nextQuantity)
                } else {
                    it
                }
            }
            state.copyWithLots(lots)
        }
    }

    private suspend fun saveChanges() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        val result = saveFoodDetailChangesUseCase(
            FoodDetailDraft(
                categoryId = state.categoryId,
                name = state.name,
                storageLocation = state.storageLocation,
                perishability = state.perishability,
                lots = state.lots.map {
                    FoodLotDraft(
                        id = it.id.takeIf { id -> id > 0 },
                        expirationDate = it.expirationDate,
                        quantity = it.quantity
                    )
                }
            )
        )
        when (result) {
            SaveFoodDetailChangesResult.Success -> {
                _uiState.update { it.copy(isDirty = false, isSaving = false) }
                _effects.send(FoodDetailEffect.ShowSnackbar("Modifiche salvate"))
            }
            is SaveFoodDetailChangesResult.ValidationError -> _uiState.update {
                it.copy(isSaving = false, errorMessage = "Nome alimento obbligatorio")
            }
            SaveFoodDetailChangesResult.StorageError -> _uiState.update {
                it.copy(isSaving = false, errorMessage = "Errore durante il salvataggio")
            }
        }
    }

    private suspend fun addAlias() {
        val state = _uiState.value
        if (state.aliasDraft.isBlank()) return
        val language = settingsRepository.getSettings().language.takeIf { it.isNotBlank() }
        pantryRepository.addRecipeIngredientAlias(
            categoryId = state.categoryId,
            aliasOriginal = state.aliasDraft,
            language = language
        )
        _uiState.update { it.copy(aliasDraft = "") }
    }
}

private fun FoodDetailData.toUi(): FoodDetailUiState =
    FoodDetailUiState(
        categoryId = category.id,
        name = category.name,
        imageUri = category.imageUri,
        totalQuantity = lots.sumOf { it.quantity },
        storageLocation = category.defaultStorageLocation,
        perishability = category.defaultPerishability,
        lots = lots.map {
            FoodLotUi(
                id = it.id,
                expirationDate = it.expirationDate,
                dateLabel = it.expirationDate.format(DateFormatter),
                expirationLabel = it.expirationDate.toExpirationLabel(),
                quantity = it.quantity,
                isExpired = it.expirationDate.isBefore(LocalDate.now())
            )
        },
        scannedProducts = barcodeLinks.map {
            ProductLinkUi(
                barcode = it.barcode,
                productName = it.productName,
                subtitle = "Riconosciuto come ${category.name}"
            )
        },
        recipeAliases = recipeIngredientLinks.map {
            RecipeAliasUi(id = it.id, alias = it.aliasOriginal, language = it.language)
        }
    )

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ITALIAN)

private fun LocalDate.toExpirationLabel(): String {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), this).toInt()
    return when {
        days < 0 -> "Scaduta"
        days == 0 -> "Scade oggi"
        days == 1 -> "tra 1 giorno"
        days < 30 -> "tra $days giorni"
        days < 60 -> "tra 1 mese"
        else -> "tra ${days / 30} mesi"
    }
}

private fun FoodLotUi.withQuantity(quantity: Int): FoodLotUi =
    copy(quantity = quantity)

private fun FoodDetailUiState.copyWithLots(lots: List<FoodLotUi>): FoodDetailUiState =
    copy(lots = lots, totalQuantity = lots.sumOf { it.quantity }, isDirty = true)
