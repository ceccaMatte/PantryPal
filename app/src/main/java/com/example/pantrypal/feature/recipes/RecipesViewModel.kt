package com.example.pantrypal.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeSearchQuery
import com.example.pantrypal.domain.model.RecipeSearchResult
import com.example.pantrypal.domain.usecase.ToggleFavoriteRecipeUseCase
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
class RecipesViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val toggleFavoriteRecipeUseCase: ToggleFavoriteRecipeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecipesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            recipeRepository.observeFavoriteRecipes().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites.map { favorite -> favorite.toUi(isFavorite = true) }) }
            }
        }
    }

    fun onEvent(event: RecipesEvent) {
        viewModelScope.launch {
            when (event) {
                is RecipesEvent.OnQueryChange -> {
                    _uiState.update { it.copy(query = event.value, configMissing = false, errorMessage = null) }
                    if (event.value.isBlank()) {
                        _uiState.update { it.copy(recipes = emptyList(), isLoading = false) }
                    } else {
                        search(event.value)
                    }
                }
                is RecipesEvent.OnTabSelected -> _uiState.update { it.copy(selectedTab = event.tab) }
                is RecipesEvent.OnRecipeClick -> _effects.send(RecipesEffect.NavigateToRecipeDetail(event.externalId))
                is RecipesEvent.OnFavoriteClick -> toggleFavorite(event.externalId)
            }
        }
    }

    private suspend fun search(query: String) {
        if (query.isBlank()) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = recipeRepository.searchRecipes(RecipeSearchQuery(query))) {
            is RecipeSearchResult.Success -> _uiState.update {
                it.copy(isLoading = false, configMissing = false, recipes = result.recipes.map(RecipeCard::toUi))
            }
            RecipeSearchResult.Empty -> _uiState.update { it.copy(isLoading = false, recipes = emptyList()) }
            RecipeSearchResult.ConfigMissing -> _uiState.update {
                it.copy(isLoading = false, recipes = emptyList(), configMissing = true, errorMessage = null)
            }
            RecipeSearchResult.Offline -> _uiState.update { it.copy(isLoading = false, errorMessage = "Ricerca offline in questo step") }
            RecipeSearchResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = "Errore durante la ricerca") }
        }
    }

    private suspend fun toggleFavorite(externalId: String) {
        val detail = recipeRepository.getRecipeDetail(externalId)
        if (detail == null) {
            _uiState.update { it.copy(errorMessage = "Dettaglio ricetta non disponibile") }
            return
        }
        val isFavorite = toggleFavoriteRecipeUseCase(detail)
        _uiState.update { state ->
            state.copy(
                recipes = state.recipes.map {
                    if (it.externalId == externalId) it.copy(isFavorite = isFavorite) else it
                }
            )
        }
    }
}

private fun RecipeCard.toUi(isFavorite: Boolean = this.isFavorite): RecipeCardUi =
    RecipeCardUi(
        externalId = externalId,
        title = title,
        description = "Ricetta pronta da collegare agli ingredienti in dispensa.",
        readyInMinutes = preparationTimeMinutes ?: 20,
        presentCount = 0,
        missingCount = 0,
        isFavorite = isFavorite
    )
