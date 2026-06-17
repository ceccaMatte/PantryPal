package com.example.pantrypal.data.recipe

import com.example.pantrypal.domain.model.RecipeCard
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@ActivityRetainedScoped
class SessionRecipeCache @Inject constructor() {
    private val recipesById = linkedMapOf<String, RecipeCard>()
    private val _recipes = MutableStateFlow<List<RecipeCard>>(emptyList())
    val recipes: StateFlow<List<RecipeCard>> = _recipes.asStateFlow()

    fun merge(recipes: List<RecipeCard>) {
        if (recipes.isEmpty()) return
        recipes.forEach { recipe ->
            recipesById[recipe.externalId] = recipe
        }
        _recipes.value = recipesById.values.toList()
    }
}
