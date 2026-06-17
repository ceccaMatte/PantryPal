package com.example.pantrypal.feature.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.EmptyState
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.LoadingState
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.core.designsystem.PlaceholderImageBox

@Composable
fun RecipesScreen(
    state: RecipesUiState,
    onEvent: (RecipesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleRecipes = if (state.selectedTab == RecipeTab.RESULTS) state.recipes else state.favorites
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PantryColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(PantrySpacing.md)
    ) {
        Text("Ricette", style = PantryTypography.headlineMedium, color = PantryColors.Green700)
        Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onEvent(RecipesEvent.OnSearchQueryChanged(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Cerca ingredienti o piatti...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (state.isSearchButtonEnabled) onEvent(RecipesEvent.OnSearchClick)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PantryColors.Line,
                    unfocusedBorderColor = PantryColors.Line,
                    focusedContainerColor = PantryColors.Card,
                    unfocusedContainerColor = PantryColors.Card
                )
            )
            Button(
                onClick = { onEvent(RecipesEvent.OnSearchClick) },
                enabled = state.isSearchButtonEnabled && !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green700, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cerca")
            }
        }
        state.message?.let {
            Text(it, color = PantryColors.Muted)
        }
        Text("Sulla base di cio che hai in dispensa", color = PantryColors.Muted, style = PantryTypography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.xl), verticalAlignment = Alignment.CenterVertically) {
            RecipeTabLabel("Risultati", RecipeTab.RESULTS, state.selectedTab, onEvent)
            RecipeTabLabel("Preferiti", RecipeTab.FAVORITES, state.selectedTab, onEvent)
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PantryColors.Line)
        )

        if (state.isLoading) {
            LoadingState("Ricerca ricette...")
        } else if (state.message != null && state.selectedTab == RecipeTab.RESULTS && visibleRecipes.isEmpty()) {
            EmptyState("Ricette", state.message)
        } else if (visibleRecipes.isEmpty()) {
            EmptyState(
                if (state.selectedTab == RecipeTab.FAVORITES && state.searchQuery.isNotBlank()) {
                    "Nessuna ricetta preferita trovata"
                } else {
                    "Nessuna ricetta"
                },
                if (state.selectedTab == RecipeTab.FAVORITES) {
                    if (state.searchQuery.isBlank()) "I preferiti salvati compariranno qui." else "Prova con un altro titolo."
                } else {
                    "Cerca una ricetta per vedere risultati reali."
                }
            )
        } else {
            visibleRecipes.forEach { recipe ->
                RecipeCard(recipe, onEvent)
            }
        }
        Spacer(Modifier.height(104.dp))
    }
}

@Composable
private fun RecipeTabLabel(
    label: String,
    tab: RecipeTab,
    selectedTab: RecipeTab,
    onEvent: (RecipesEvent) -> Unit
) {
    Text(
        text = label,
        modifier = Modifier.clickable { onEvent(RecipesEvent.OnTabSelected(tab)) },
        style = PantryTypography.titleMedium,
        color = if (tab == selectedTab) PantryColors.Green700 else PantryColors.Muted,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RecipeCard(recipe: RecipeCardUi, onEvent: (RecipesEvent) -> Unit) {
    PantryCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .background(recipeTint(recipe.externalId), RoundedCornerShape(16.dp))
        ) {
            PlaceholderImageBox(modifier = Modifier.align(Alignment.Center).size(50.dp), background = Color.Transparent)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(PantrySpacing.sm),
                shape = CircleShape,
                color = PantryColors.Card,
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = { onEvent(RecipesEvent.OnRecipeFavoriteClick(recipe.externalId)) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Preferito",
                        tint = PantryColors.Green700
                    )
                }
            }
        }
        Spacer(Modifier.height(PantrySpacing.md))
        Text(recipe.title, style = PantryTypography.titleMedium, color = PantryColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(recipe.description, color = PantryColors.Muted, style = PantryTypography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(PantrySpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.sm), modifier = Modifier.fillMaxWidth()) {
            if (recipe.presentCount != null && recipe.missingCount != null) {
                FoodChip(label = "${recipe.presentCount} presenti", icon = Icons.Default.CheckCircleOutline, modifier = Modifier.widthIn(max = 150.dp))
                FoodChip(label = "${recipe.missingCount} mancanti", icon = Icons.Default.Info, modifier = Modifier.widthIn(max = 150.dp))
            } else {
                FoodChip(label = "Da collegare", icon = Icons.Default.Info)
            }
        }
        Spacer(Modifier.height(PantrySpacing.sm))
        Button(
            onClick = { onEvent(RecipesEvent.OnRecipeClick(recipe.externalId)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green50, contentColor = PantryColors.Green700),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Vedi Ricetta", style = PantryTypography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

private fun recipeTint(id: String): Color =
    when {
        id.contains("quinoa") -> PantryColors.Green50
        id.contains("salmone") -> PantryColors.Green100
        else -> PantryColors.WarningBg.copy(alpha = 0.55f)
    }
