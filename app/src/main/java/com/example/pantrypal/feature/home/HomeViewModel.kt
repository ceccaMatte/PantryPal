package com.example.pantrypal.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.HomeOverview
import com.example.pantrypal.domain.model.RecipeCard
import com.example.pantrypal.domain.model.RecipeSearchResult
import com.example.pantrypal.domain.model.StorageLocationFilter
import com.example.pantrypal.domain.usecase.GetHomeSuggestedRecipesUseCase
import com.example.pantrypal.domain.usecase.GetHomeOverviewUseCase
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
class HomeViewModel @Inject constructor(
    getHomeOverviewUseCase: GetHomeOverviewUseCase,
    getHomeSuggestedRecipesUseCase: GetHomeSuggestedRecipesUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            getHomeOverviewUseCase().collect { overview ->
                _uiState.value = overview.toUiState(_uiState.value)
            }
        }
        viewModelScope.launch {
            getHomeSuggestedRecipesUseCase().collect { result ->
                _uiState.update { it.withSuggestedRecipes(result) }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        viewModelScope.launch {
            when (event) {
                is HomeEvent.OnExpiringFoodClick -> _effects.send(
                    HomeEffect.NavigateToPantry(StorageLocationFilter.ALL, event.categoryId)
                )
                is HomeEvent.OnStorageStatClick -> {
                    settingsRepository.updatePantryStorageFilter(event.filter)
                    _effects.send(HomeEffect.NavigateToPantry(event.filter))
                }
                is HomeEvent.OnRecipeClick -> _effects.send(HomeEffect.NavigateToRecipeDetail(event.recipeId))
                HomeEvent.OnFabClick -> _effects.send(HomeEffect.OpenAddChoiceSheet)
            }
        }
    }
}

private fun HomeOverview.toUiState(previous: HomeUiState): HomeUiState =
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
        }
    )

private fun HomeUiState.withSuggestedRecipes(result: RecipeSearchResult): HomeUiState =
    when (result) {
        is RecipeSearchResult.Success -> copy(
            suggestedRecipes = result.recipes.map { it.toHomeUi() },
            suggestedRecipesMessage = "Dai tuoi ingredienti"
        )
        RecipeSearchResult.ConfigMissing -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Configura Spoonacular per vedere suggerimenti"
        )
        RecipeSearchResult.Empty -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Aggiungi alimenti per ricevere suggerimenti"
        )
        RecipeSearchResult.Error -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Ricette non disponibili al momento"
        )
        RecipeSearchResult.Offline -> copy(
            suggestedRecipes = emptyList(),
            suggestedRecipesMessage = "Connessione assente per i suggerimenti"
        )
    }

private fun RecipeCard.toHomeUi(): HomeRecipeUi =
    HomeRecipeUi(
        externalId = externalId,
        title = title,
        subtitle = "Suggerita dai tuoi ingredienti",
        timeLabel = preparationTimeMinutes?.let { "$it min" } ?: "--"
    )
