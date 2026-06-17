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
    val timeLabel: String
)

sealed interface HomeEvent {
    data class OnExpiringFoodClick(val categoryId: Long) : HomeEvent
    data class OnStorageStatClick(val filter: StorageLocationFilter) : HomeEvent
    data class OnRecipeClick(val recipeId: String) : HomeEvent
    data object OnFabClick : HomeEvent
}

sealed interface HomeEffect {
    data class NavigateToPantry(val filter: StorageLocationFilter, val focusedCategoryId: Long? = null) : HomeEffect
    data class NavigateToRecipeDetail(val recipeId: String) : HomeEffect
    data object OpenAddChoiceSheet : HomeEffect
}
