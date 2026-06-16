package com.example.pantrypal.feature.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.domain.model.RecipeDetail
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
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    private val externalId: String = savedStateHandle["recipeId"] ?: "sample-pasta"

    private val _uiState = MutableStateFlow(RecipeDetailUiState(externalId = externalId))
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecipeDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            recipeRepository.getRecipeDetail(externalId)?.let { detail ->
                _uiState.value = detail.toUi()
            }
        }
    }

    fun onEvent(event: RecipeDetailEvent) {
        viewModelScope.launch {
            when (event) {
                RecipeDetailEvent.OnBackClick -> _effects.send(RecipeDetailEffect.NavigateBack)
                RecipeDetailEvent.OnFavoriteClick -> _uiState.update { it.copy(isFavorite = !it.isFavorite) }
                is RecipeDetailEvent.OnIngredientClick -> _effects.send(
                    RecipeDetailEffect.ShowSnackbar("Collegamento ingrediente pronto per lo step successivo")
                )
            }
        }
    }
}

private fun RecipeDetail.toUi(): RecipeDetailUiState {
    val ingredientUi = ingredients.mapIndexed { index, ingredient ->
        RecipeIngredientUi(
            name = ingredient.originalName,
            amountLabel = listOfNotNull(
                ingredient.amount?.let { amount ->
                    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
                },
                ingredient.unit
            ).joinToString(" ").ifBlank { "q.b." },
            pantryMatchLabel = if (index < 3) "In dispensa" else null,
            isPresent = index < 3
        )
    }
    return RecipeDetailUiState(
        externalId = externalId,
        title = title,
        description = description.orEmpty().ifBlank { "Ricetta pronta in pochi minuti con ingredienti freschi." },
        readyInMinutes = preparationTimeMinutes ?: 20,
        servingsLabel = "${servings ?: 2} persone",
        isFavorite = true,
        presentIngredients = ingredientUi.filter { it.isPresent },
        missingIngredients = ingredientUi.filterNot { it.isPresent }
    )
}
