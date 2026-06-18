package com.example.pantrypal.feature.home

import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter

data class HomeUiState(
    val isOverviewLoading: Boolean = false,
    val username: String? = null,
    val totalPackages: Int = 0,
    val fridgePackages: Int = 0,
    val freezerPackages: Int = 0,
    val pantryPackages: Int = 0,
    val expiringFoods: List<HomeExpiringFoodUi> = emptyList(),
    val suggestedRecipes: List<HomeRecipeUi> = emptyList(),
    val suggestedRecipesMessage: String = "Aggiungi alimenti per ricevere suggerimenti",
    val canGenerateRecipes: Boolean = false,
    val isGeneratingRecipes: Boolean = false,
    val errorMessage: String? = null
)

data class HomeExpiringFoodUi(
    val categoryId: Long,
    val name: String,
    val expiringQuantity: Int,
    val storageLocation: StorageLocation
)

data class HomeRecipeUi(
    val externalId: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val localImageUri: String? = null,
    val timeLabel: String,
    val isFavorite: Boolean
)

sealed interface HomeEvent {
    data class OnExpiringFoodClick(val categoryId: Long) : HomeEvent
    data object OnExpiringCardClick : HomeEvent
    data object OnPantrySummaryClick : HomeEvent
    data class OnStorageStatClick(val filter: StorageLocationFilter) : HomeEvent
    data class OnRecipeClick(val recipeId: String) : HomeEvent
    data class OnSuggestedRecipeFavoriteClick(val recipeId: String) : HomeEvent
    data object OnGenerateRecipesClick : HomeEvent
    data object OnFabClick : HomeEvent
}

sealed interface HomeEffect {
    data class NavigateToPantry(val filter: StorageLocationFilter, val focusedCategoryId: Long? = null) : HomeEffect
    data class NavigateToRecipeDetail(val recipeId: String) : HomeEffect
    data object OpenAddChoiceSheet : HomeEffect
}
