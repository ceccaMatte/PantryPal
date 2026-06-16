package com.example.pantrypal.feature.pantry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.pantry.PantryRepository
import com.example.pantrypal.domain.model.FoodDetailData
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
    private val pantryRepository: PantryRepository
) : ViewModel() {
    private val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: 1L

    private val _uiState = MutableStateFlow(FoodDetailUiState(categoryId = categoryId))
    val uiState: StateFlow<FoodDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FoodDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            pantryRepository.observeFoodDetail(categoryId).collect { detail ->
                if (detail != null) {
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
                FoodDetailEvent.OnAddLotClick -> _effects.send(FoodDetailEffect.ShowSnackbar("Nuova scadenza pronta per lo step successivo"))
                is FoodDetailEvent.OnLotMinusClick -> _effects.send(FoodDetailEffect.ShowSnackbar("Riduzione lotto pronta per lo step successivo"))
                is FoodDetailEvent.OnLotPlusClick -> _effects.send(FoodDetailEffect.ShowSnackbar("Incremento lotto pronto per lo step successivo"))
            }
        }
    }
}

private fun FoodDetailData.toUi(): FoodDetailUiState =
    FoodDetailUiState(
        categoryId = category.id,
        name = category.name,
        totalQuantity = lots.sumOf { it.quantity },
        storageLocation = category.defaultStorageLocation,
        perishability = category.defaultPerishability,
        lots = lots.map {
            FoodLotUi(
                id = it.id,
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
        recipeAliases = recipeIngredientLinks.map { it.aliasOriginal }.distinct().ifEmpty { listOf(category.name.lowercase()) }
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
