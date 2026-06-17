package com.example.pantrypal.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.HomeOverview
import com.example.pantrypal.domain.model.PantryPalApiMode
import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeDetailResult
import com.example.pantrypal.domain.model.RecipeSearchResult
import com.example.pantrypal.domain.model.StorageLocationFilter
import com.example.pantrypal.domain.usecase.GetHomeSuggestedRecipesUseCase
import com.example.pantrypal.domain.usecase.GetHomeOverviewUseCase
import com.example.pantrypal.domain.usecase.ToggleFavoriteRecipeUseCase
import com.example.pantrypal.data.recipe.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    getHomeOverviewUseCase: GetHomeOverviewUseCase,
    private val getHomeSuggestedRecipesUseCase: GetHomeSuggestedRecipesUseCase,
    private val settingsRepository: SettingsRepository,
    private val recipeRepository: RecipeRepository,
    private val toggleFavoriteRecipeUseCase: ToggleFavoriteRecipeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            getHomeOverviewUseCase().collect { overview ->
                _uiState.value = overview.toUiState(_uiState.value, getHomeSuggestedRecipesUseCase.apiMode)
            }
        }
        viewModelScope.launch {
            getHomeSuggestedRecipesUseCase(allowNetwork = false).collect { result ->
                _uiState.update { it.withSuggestedRecipes(result, getHomeSuggestedRecipesUseCase.apiMode) }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        viewModelScope.launch {
            when (event) {
                is HomeEvent.OnExpiringFoodClick -> _effects.send(
                    HomeEffect.NavigateToPantry(StorageLocationFilter.ALL)
                )
                HomeEvent.OnExpiringCardClick -> navigateToAllPantry()
                HomeEvent.OnPantrySummaryClick -> navigateToAllPantry()
                is HomeEvent.OnStorageStatClick -> {
                    settingsRepository.updatePantryStorageFilter(event.filter)
                    _effects.send(HomeEffect.NavigateToPantry(event.filter))
                }
                is HomeEvent.OnRecipeClick -> _effects.send(HomeEffect.NavigateToRecipeDetail(event.recipeId))
                is HomeEvent.OnSuggestedRecipeFavoriteClick -> toggleSuggestedFavorite(event.recipeId)
                HomeEvent.OnGenerateRecipesClick -> generateRecipes()
                HomeEvent.OnFabClick -> _effects.send(HomeEffect.OpenAddChoiceSheet)
            }
        }
    }

    private suspend fun navigateToAllPantry() {
        settingsRepository.updatePantryStorageFilter(StorageLocationFilter.ALL)
        _effects.send(HomeEffect.NavigateToPantry(StorageLocationFilter.ALL))
    }

    private suspend fun generateRecipes() {
        _uiState.update { it.copy(isGeneratingRecipes = true, canGenerateRecipes = false) }
        val result = getHomeSuggestedRecipesUseCase(allowNetwork = true).first()
        _uiState.update { it.withSuggestedRecipes(result, getHomeSuggestedRecipesUseCase.apiMode).copy(isGeneratingRecipes = false) }
    }

    private suspend fun toggleSuggestedFavorite(recipeId: String) {
        if (recipeRepository.isFavorite(recipeId)) {
            recipeRepository.removeFavoriteRecipe(recipeId)
            _uiState.update { state ->
                state.copy(suggestedRecipes = state.suggestedRecipes.map {
                    if (it.externalId == recipeId) it.copy(isFavorite = false) else it
                })
            }
            return
        }
        when (val detail = recipeRepository.getRecipeDetailResult(recipeId)) {
            is RecipeDetailResult.Success -> {
                toggleFavoriteRecipeUseCase(detail.recipe)
                _uiState.update { state ->
                    state.copy(suggestedRecipes = state.suggestedRecipes.map {
                        if (it.externalId == recipeId) it.copy(isFavorite = true) else it
                    })
                }
            }
            else -> Unit
        }
    }
}

private fun HomeOverview.toUiState(previous: HomeUiState, apiMode: PantryPalApiMode): HomeUiState =
    previous.copy(
        username = username,
        totalPackages = totalPackages,
        fridgePackages = fridgePackages,
        freezerPackages = freezerPackages,
        pantryPackages = pantryPackages,
        expiringFoods = expiringFoods.map {
            HomeExpiringFoodUi(
                categoryId = it.categoryId,
                name = it.name,
                expiringQuantity = it.expiringQuantity,
                storageLocation = it.storageLocation
            )
        },
        canGenerateRecipes = totalPackages > 0 && (
            previous.canGenerateRecipes ||
                (apiMode == PantryPalApiMode.REAL && previous.suggestedRecipes.isEmpty())
            )
    )

private fun HomeUiState.withSuggestedRecipes(result: RecipeSearchResult, apiMode: PantryPalApiMode): HomeUiState =
    when (result) {
        is RecipeSearchResult.Success -> copy(
            suggestedRecipes = result.recipes.map { it.toHomeUi() },
            suggestedRecipesMessage = "Dai tuoi ingredienti",
            canGenerateRecipes = false
        )
        RecipeSearchResult.ConfigMissing -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Configura Spoonacular per vedere suggerimenti",
            canGenerateRecipes = false
        )
        RecipeSearchResult.Empty -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = if (apiMode == PantryPalApiMode.REAL && totalPackages > 0) {
                "Nessuna cache suggerimenti disponibile"
            } else {
                "Aggiungi alimenti per ricevere suggerimenti"
            },
            canGenerateRecipes = apiMode == PantryPalApiMode.REAL && totalPackages > 0
        )
        RecipeSearchResult.QuotaExceeded -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Quota Spoonacular esaurita",
            canGenerateRecipes = false
        )
        RecipeSearchResult.RateLimited -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Troppe richieste a Spoonacular",
            canGenerateRecipes = false
        )
        RecipeSearchResult.InvalidResponse -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Risposta suggerimenti non valida",
            canGenerateRecipes = false
        )
        RecipeSearchResult.GenericError, RecipeSearchResult.Error -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Ricette non disponibili al momento",
            canGenerateRecipes = apiMode == PantryPalApiMode.REAL && totalPackages > 0
        )
        RecipeSearchResult.NetworkError, RecipeSearchResult.Offline -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Connessione assente per i suggerimenti",
            canGenerateRecipes = apiMode == PantryPalApiMode.REAL && totalPackages > 0
        )
    }

private fun RecipeCard.toHomeUi(): HomeRecipeUi =
    HomeRecipeUi(
        externalId = externalId,
        title = title,
        subtitle = "Suggerita dai tuoi ingredienti",
        timeLabel = preparationTimeMinutes?.let { "$it min" } ?: "--",
        isFavorite = isFavorite
    )
