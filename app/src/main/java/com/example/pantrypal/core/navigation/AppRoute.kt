package com.example.pantrypal.core.navigation

sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")
    data object Pantry : AppRoute("pantry")
    data object Recipes : AppRoute("recipes")
    data object Profile : AppRoute("profile")
    data object Scan : AppRoute("scan")
    data object ManualAdd : AppRoute("manual-add")
    data object FoodDetail : AppRoute("food/{categoryId}") {
        fun create(categoryId: Long) = "food/$categoryId"
    }
    data object FoodLinks : AppRoute("food/{categoryId}/links") {
        fun create(categoryId: Long) = "food/$categoryId/links"
    }
    data object RecipeDetail : AppRoute("recipe/{recipeId}") {
        fun create(recipeId: String) = "recipe/$recipeId"
    }
}

val MainTabRoutes = listOf(
    AppRoute.Home.route,
    AppRoute.Pantry.route,
    AppRoute.Recipes.route,
    AppRoute.Profile.route
)

fun String?.isAddFlowRoute(): Boolean =
    this == AppRoute.Scan.route || this == AppRoute.ManualAdd.route

fun String?.isMainTabRoute(): Boolean = this in MainTabRoutes
