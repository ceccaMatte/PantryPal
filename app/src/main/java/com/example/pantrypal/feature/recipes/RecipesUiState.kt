package com.example.pantrypal.feature.recipes

data class RecipesUiState(
    val query: String = "",
    val selectedTab: RecipeTab = RecipeTab.RESULTS,
    val recipes: List<RecipeCardUi> = sampleRecipes,
    val favorites: List<RecipeCardUi> = sampleRecipes.take(1),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
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
    val externalId: String = "sample-pasta",
    val title: String = "Pasta al Pomodoro e Basilico",
    val description: String = "Un classico della cucina italiana, pronto in pochi minuti con ingredienti freschi e profumati.",
    val readyInMinutes: Int = 20,
    val difficultyLabel: String = "Facile",
    val servingsLabel: String = "2 persone",
    val isFavorite: Boolean = true,
    val presentIngredients: List<RecipeIngredientUi> = samplePresentIngredients,
    val missingIngredients: List<RecipeIngredientUi> = sampleMissingIngredients,
    val isLoading: Boolean = false
)

data class RecipeIngredientUi(
    val name: String,
    val amountLabel: String,
    val pantryMatchLabel: String? = null,
    val isPresent: Boolean
)

sealed interface RecipesEvent {
    data class OnQueryChange(val value: String) : RecipesEvent
    data class OnTabSelected(val tab: RecipeTab) : RecipesEvent
    data class OnRecipeClick(val externalId: String) : RecipesEvent
    data class OnFavoriteClick(val externalId: String) : RecipesEvent
}

sealed interface RecipesEffect {
    data class NavigateToRecipeDetail(val externalId: String) : RecipesEffect
}

sealed interface RecipeDetailEvent {
    data object OnBackClick : RecipeDetailEvent
    data object OnFavoriteClick : RecipeDetailEvent
    data class OnIngredientClick(val ingredientName: String) : RecipeDetailEvent
}

sealed interface RecipeDetailEffect {
    data object NavigateBack : RecipeDetailEffect
    data class ShowSnackbar(val message: String) : RecipeDetailEffect
}

private val sampleRecipes = listOf(
    RecipeCardUi(
        externalId = "sample-pasta",
        title = "Pasta al Pomodoro e Basilico",
        description = "Un classico intramontabile con pomodorini freschi e basilico.",
        readyInMinutes = 20,
        presentCount = 3,
        missingCount = 2,
        isFavorite = true
    ),
    RecipeCardUi(
        externalId = "sample-quinoa",
        title = "Bowl di Quinoa e Avocado",
        description = "Nutriente e colorata, ideale per un pranzo leggero.",
        readyInMinutes = 15,
        presentCount = 5,
        missingCount = 0,
        isFavorite = false
    ),
    RecipeCardUi(
        externalId = "sample-salmone",
        title = "Salmone al Forno",
        description = "Cena semplice con contorno fresco dalla dispensa.",
        readyInMinutes = 25,
        presentCount = 2,
        missingCount = 1,
        isFavorite = false
    )
)

private val samplePresentIngredients = listOf(
    RecipeIngredientUi("Pasta (Spaghetti)", "200g", "In dispensa", true),
    RecipeIngredientUi("Olio d'Oliva", "2 cucchiai", "Olio extravergine", true),
    RecipeIngredientUi("Sale", "q.b.", "In dispensa", true)
)

private val sampleMissingIngredients = listOf(
    RecipeIngredientUi("Pomodorini Ciliegino", "250g", null, false),
    RecipeIngredientUi("Basilico fresco", "1 mazzetto", null, false),
    RecipeIngredientUi("Parmigiano", "30g", null, false)
)
