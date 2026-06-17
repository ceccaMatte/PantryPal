package com.example.pantrypal.feature.recipes

data class RecipesUiState(
    val searchQuery: String = "",
    val lastSubmittedQuery: String? = null,
    val selectedTab: RecipeTab = RecipeTab.RESULTS,
    val recipes: List<RecipeCardUi> = emptyList(),
    val favorites: List<RecipeCardUi> = emptyList(),
    val isLoading: Boolean = false,
    val isSearchButtonEnabled: Boolean = false,
    val apiMode: com.example.pantrypal.domain.model.PantryPalApiMode = com.example.pantrypal.domain.model.PantryPalApiMode.MOCK,
    val message: String? = null
)

enum class RecipeTab {
    RESULTS,
    FAVORITES
}

data class RecipeCardUi(
    val externalId: String,
    val title: String,
    val description: String,
    val readyInMinutes: Int,
    val presentCount: Int,
    val missingCount: Int,
    val isFavorite: Boolean
)

data class RecipeDetailUiState(
    val externalId: String = "",
    val title: String = "",
    val description: String = "",
    val sourceUrl: String? = null,
    val readyInMinutes: Int = 0,
    val difficultyLabel: String = "Facile",
    val servingsLabel: String = "",
    val isFavorite: Boolean = false,
    val presentIngredients: List<RecipeIngredientUi> = emptyList(),
    val missingIngredients: List<RecipeIngredientUi> = emptyList(),
    val selectedIngredient: RecipeIngredientUi? = null,
    val linkSuggestions: List<RecipeFoodSuggestionUi> = emptyList(),
    val linkQuery: String = "",
    val isLoading: Boolean = false,
    val configMissing: Boolean = false,
    val errorMessage: String? = null
)

data class RecipeIngredientUi(
    val key: String,
    val name: String,
    val amountLabel: String,
    val pantryMatchLabel: String? = null,
    val isPresent: Boolean,
    val externalIngredientId: String?,
    val normalizedName: String,
    val replaceLinkId: Long? = null
)

data class RecipeFoodSuggestionUi(
    val categoryId: Long?,
    val label: String,
    val isCreateNew: Boolean = false
)

sealed interface RecipesEvent {
    data class OnSearchQueryChanged(val value: String) : RecipesEvent
    data object OnSearchClick : RecipesEvent
    data class OnTabSelected(val tab: RecipeTab) : RecipesEvent
    data class OnRecipeClick(val externalId: String) : RecipesEvent
    data class OnRecipeFavoriteClick(val externalId: String) : RecipesEvent
}

sealed interface RecipesEffect {
    data class NavigateToRecipeDetail(val externalId: String) : RecipesEffect
}

sealed interface RecipeDetailEvent {
    data object OnBackClick : RecipeDetailEvent
    data object OnFavoriteClick : RecipeDetailEvent
    data object OnShareClick : RecipeDetailEvent
    data class OnIngredientClick(val ingredientKey: String) : RecipeDetailEvent
    data object OnDismissIngredientSheet : RecipeDetailEvent
    data class OnLinkQueryChange(val value: String) : RecipeDetailEvent
    data class OnFoodSuggestionClick(val suggestion: RecipeFoodSuggestionUi) : RecipeDetailEvent
    data object OnMoveSelectedToBuyClick : RecipeDetailEvent
    data object OnMarkSelectedInPantryClick : RecipeDetailEvent
}

sealed interface RecipeDetailEffect {
    data object NavigateBack : RecipeDetailEffect
    data class ShowSnackbar(val message: String) : RecipeDetailEffect
    data class ShareShoppingList(val text: String) : RecipeDetailEffect
}
