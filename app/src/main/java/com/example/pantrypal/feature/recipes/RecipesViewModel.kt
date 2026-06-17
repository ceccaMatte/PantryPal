package com.example.pantrypal.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.core.util.TextNormalizer
import com.example.pantrypal.data.recipe.RecipeRepository
import com.example.pantrypal.domain.model.PantryPalApiMode
import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetailResult
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
    private val toggleFavoriteRecipeUseCase: ToggleFavoriteRecipeUseCase,
    private val textNormalizer: TextNormalizer
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecipesUiState(apiMode = recipeRepository.apiMode))
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecipesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var favoriteIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            recipeRepository.observeFavoriteRecipes().collect { favorites ->
                favoriteIds = favorites.map { it.externalId }.toSet()
                _uiState.update { state ->
                    state.copy(
                        favorites = favorites.map { it.toUi(isFavorite = true) },
                        recipes = state.recipes.markFavorites(favoriteIds)
                    )
                }
            }
        }
    }

    fun onEvent(event: RecipesEvent) {
        viewModelScope.launch {
            when (event) {
                is RecipesEvent.OnSearchQueryChanged -> onSearchQueryChanged(event.value)
                RecipesEvent.OnSearchClick -> searchSubmittedQuery()
                is RecipesEvent.OnTabSelected -> _uiState.update { it.copy(selectedTab = event.tab) }
                is RecipesEvent.OnRecipeClick -> _effects.send(RecipesEffect.NavigateToRecipeDetail(event.externalId))
                is RecipesEvent.OnRecipeFavoriteClick -> toggleFavorite(event.externalId)
            }
        }
    }

    private fun onSearchQueryChanged(value: String) {
        val normalizedQuery = textNormalizer.normalizeFoodText(value)
        _uiState.update {
            it.copy(
                searchQuery = value,
                isSearchButtonEnabled = normalizedQuery.length >= 3,
                message = null
            )
        }
    }

    private suspend fun searchSubmittedQuery() {
        val state = _uiState.value
        val normalizedQuery = textNormalizer.normalizeFoodText(state.searchQuery)
        if (normalizedQuery.length < 3) return
        if (state.lastSubmittedQuery == normalizedQuery && state.recipes.isNotEmpty()) return

        _uiState.update {
            it.copy(isLoading = true, message = null, selectedTab = RecipeTab.RESULTS)
        }
        when (val result = recipeRepository.searchRecipes(RecipeSearchQuery(state.searchQuery))) {
            is RecipeSearchResult.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    lastSubmittedQuery = normalizedQuery,
                    recipes = result.recipes.map { recipe -> recipe.toUi() }.markFavorites(favoriteIds),
                    message = null
                )
            }
            else -> _uiState.update {
                it.copy(
                    isLoading = false,
                    lastSubmittedQuery = normalizedQuery,
                    recipes = emptyList(),
                    message = result.toUiMessage(recipeRepository.apiMode)
                )
            }
        }
    }

    private suspend fun toggleFavorite(externalId: String) {
        if (externalId in favoriteIds) {
            recipeRepository.removeFavoriteRecipe(externalId)
            _uiState.update { state ->
                state.copy(
                    favorites = state.favorites.filterNot { it.externalId == externalId },
                    recipes = state.recipes.map {
                        if (it.externalId == externalId) it.copy(isFavorite = false) else it
                    }
                )
            }
            return
        }

        when (val detailResult = recipeRepository.getRecipeDetailResult(externalId)) {
            is RecipeDetailResult.Success -> {
                val isFavorite = toggleFavoriteRecipeUseCase(detailResult.recipe)
                _uiState.update { state ->
                    state.copy(
                        recipes = state.recipes.map {
                            if (it.externalId == externalId) it.copy(isFavorite = isFavorite) else it
                        },
                        message = null
                    )
                }
            }
            RecipeDetailResult.Empty -> _uiState.update {
                it.copy(message = if (recipeRepository.apiMode == PantryPalApiMode.CACHE_ONLY) {
                    "Dettaglio ricetta non disponibile in cache"
                } else {
                    "Dettaglio ricetta non disponibile"
                })
            }
            else -> _uiState.update { it.copy(message = detailResult.toUiMessage(recipeRepository.apiMode)) }
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

private fun List<RecipeCardUi>.markFavorites(favoriteIds: Set<String>): List<RecipeCardUi> =
    map { it.copy(isFavorite = it.externalId in favoriteIds) }

private fun RecipeSearchResult.toUiMessage(mode: PantryPalApiMode): String =
    when (this) {
        RecipeSearchResult.Empty -> if (mode == PantryPalApiMode.CACHE_ONLY) "Nessuna ricetta disponibile in cache" else "Nessuna ricetta trovata"
        RecipeSearchResult.ConfigMissing -> "Spoonacular non configurato"
        RecipeSearchResult.QuotaExceeded -> "Quota Spoonacular esaurita"
        RecipeSearchResult.RateLimited -> "Troppe richieste a Spoonacular"
        RecipeSearchResult.NetworkError, RecipeSearchResult.Offline -> "Connessione non disponibile"
        RecipeSearchResult.InvalidResponse -> "Risposta ricette non valida"
        RecipeSearchResult.GenericError, RecipeSearchResult.Error -> "Errore durante la ricerca"
        is RecipeSearchResult.Success -> ""
    }

private fun RecipeDetailResult.toUiMessage(mode: PantryPalApiMode): String =
    when (this) {
        RecipeDetailResult.Empty -> if (mode == PantryPalApiMode.CACHE_ONLY) "Dettaglio ricetta non disponibile in cache" else "Dettaglio ricetta non disponibile"
        RecipeDetailResult.ConfigMissing -> "Spoonacular non configurato"
        RecipeDetailResult.QuotaExceeded -> "Quota Spoonacular esaurita"
        RecipeDetailResult.RateLimited -> "Troppe richieste a Spoonacular"
        RecipeDetailResult.NetworkError -> "Connessione non disponibile"
        RecipeDetailResult.InvalidResponse -> "Risposta ricetta non valida"
        RecipeDetailResult.GenericError -> "Errore durante il dettaglio ricetta"
        is RecipeDetailResult.Success -> ""
    }
