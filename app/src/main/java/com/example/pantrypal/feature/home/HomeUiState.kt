package com.example.pantrypal.feature.home

import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter

data class HomeUiState(
    val isOverviewLoading: Boolean = false,
    val username: String? = null,
    val totalPackages: Int = 84,
    val fridgePackages: Int = 24,
    val freezerPackages: Int = 18,
    val pantryPackages: Int = 42,
    val expiringFoods: List<HomeExpiringFoodUi> = sampleExpiringFoods,
    val suggestedRecipes: List<HomeRecipeUi> = sampleHomeRecipes,
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

private val sampleExpiringFoods = listOf(
    HomeExpiringFoodUi(1, "Latte", 2, StorageLocation.FRIDGE),
    HomeExpiringFoodUi(2, "Pollo", 1, StorageLocation.FRIDGE),
    HomeExpiringFoodUi(3, "Insalata", 3, StorageLocation.FRIDGE)
)

private val sampleHomeRecipes = listOf(
    HomeRecipeUi("sample-pollo", "Pollo al Limone", "Usa: Pollo - Limone - Burro", "25 min"),
    HomeRecipeUi("sample-insalata", "Insalata Ricca", "Usa: Insalata - Pomodori - Mais", "10 min")
)
