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
    val presentCount: Int?,
    val missingCount: Int?,
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
    val isSummaryExpanded: Boolean = false,
    val expandedIngredientKey: String? = null,
    val linkSheetIngredientKey: String? = null,
    val linkSheetQuery: String = "",
    val linkSheetCategories: List<RecipeLinkCategoryUi> = emptyList(),
    val linkSheetSelectedCategoryIds: Set<Long> = emptySet(),
    val canCreateLinkSheetCategory: Boolean = false,
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
    val replaceLinkId: Long? = null,
    val linkedCategoryNames: List<String> = emptyList(),
    val linkedCategories: List<RecipeLinkedCategoryUi> = emptyList(),
    val availableCategories: List<RecipeLinkedCategoryUi> = emptyList(),
    val selectedCategoryId: Long? = null
)

data class RecipeLinkedCategoryUi(
    val categoryId: Long,
    val label: String,
    val selected: Boolean = false
)

data class RecipeLinkCategoryUi(
    val categoryId: Long,
    val label: String,
    val selected: Boolean
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
    data object OnSummaryToggleClick : RecipeDetailEvent
    data class OnIngredientClick(val ingredientKey: String) : RecipeDetailEvent
    data object OnDismissIngredientSheet : RecipeDetailEvent
    data class OnAvailableCategoryClick(val ingredientKey: String, val categoryId: Long) : RecipeDetailEvent
    data class OnManageIngredientLinksClick(val ingredientKey: String) : RecipeDetailEvent
    data class OnLinkSheetQueryChange(val value: String) : RecipeDetailEvent
    data class OnLinkSheetCategoryToggle(val categoryId: Long) : RecipeDetailEvent
    data object OnLinkSheetCreateCategoryClick : RecipeDetailEvent
    data object OnLinkSheetSaveClick : RecipeDetailEvent
    data object OnMoveSelectedToBuyClick : RecipeDetailEvent
    data object OnMarkSelectedInPantryClick : RecipeDetailEvent
}

sealed interface RecipeDetailEffect {
    data object NavigateBack : RecipeDetailEffect
    data class ShowSnackbar(val message: String) : RecipeDetailEffect
    data class ShareShoppingList(val text: String) : RecipeDetailEffect
}
