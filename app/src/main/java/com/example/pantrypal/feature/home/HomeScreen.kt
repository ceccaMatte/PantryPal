package com.example.pantrypal.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.core.designsystem.PlaceholderImageBox
import com.example.pantrypal.domain.model.StorageLocationFilter

@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(PantryColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(PantrySpacing.xl)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = PantryColors.Green600)
                Text(
                    text = " PantryPal",
                    style = PantryTypography.titleLarge,
                    color = PantryColors.Green600
                )
            }
            Spacer(Modifier.height(PantrySpacing.sm))
            Text(
                text = state.username?.let { "Ciao, $it!" } ?: "Ciao!",
                style = PantryTypography.displaySmall,
                color = PantryColors.Ink
            )
        }

        PantryCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FoodChip(
                    label = "${state.expiringFoods.sumOf { it.expiringQuantity }} articoli",
                    icon = Icons.Default.WarningAmber,
                    selected = false,
                    badge = null
                )
                Text(
                    text = "OGGI",
                    modifier = Modifier
                        .background(PantryColors.WarningBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = PantryColors.WarningText,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(PantrySpacing.md))
            Text("In scadenza a breve - controllali ora", style = PantryTypography.titleMedium, color = PantryColors.InkSoft)
            Spacer(Modifier.height(PantrySpacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.sm)) {
                state.expiringFoods.forEach {
                    FoodChip(
                        label = it.name,
                        badge = "x${it.expiringQuantity}",
                        onClick = { onEvent(HomeEvent.OnExpiringFoodClick(it.categoryId)) }
                    )
                }
            }
        }

        Text("Riepilogo Dispensa", style = PantryTypography.headlineMedium)
        PantryCard(containerColor = PantryColors.Green900) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(PantrySpacing.md))
                Text("${state.totalPackages} articoli", style = PantryTypography.displaySmall, color = Color.White)
                Text("TOTALE DISPENSA", style = PantryTypography.labelLarge, color = Color.White.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(PantrySpacing.xl))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(PantrySpacing.lg),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StorageStat("Frigo", state.fridgePackages, PantryColors.Green600) {
                    onEvent(HomeEvent.OnStorageStatClick(StorageLocationFilter.FRIDGE))
                }
                StorageStat("Freezer", state.freezerPackages, PantryColors.Freezer) {
                    onEvent(HomeEvent.OnStorageStatClick(StorageLocationFilter.FREEZER))
                }
                StorageStat("Dispensa", state.pantryPackages, PantryColors.Pantry) {
                    onEvent(HomeEvent.OnStorageStatClick(StorageLocationFilter.PANTRY))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ricette Suggerite", style = PantryTypography.headlineMedium)
            Text(
                "DAI TUOI INGREDIENTI",
                modifier = Modifier.background(PantryColors.Green50, RoundedCornerShape(12.dp)).padding(10.dp),
                color = PantryColors.Green700,
                style = PantryTypography.labelLarge
            )
        }
        state.suggestedRecipes.forEach { recipe ->
            RecipeSuggestionCard(recipe) { onEvent(HomeEvent.OnRecipeClick(recipe.externalId)) }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun StorageStat(label: String, value: Int, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Kitchen, contentDescription = label, tint = color)
        Text("$value", style = PantryTypography.headlineMedium, color = PantryColors.Ink)
        Text(label.uppercase(), style = PantryTypography.labelLarge, color = PantryColors.Muted)
    }
}

@Composable
private fun RecipeSuggestionCard(recipe: HomeRecipeUi, onClick: () -> Unit) {
    PantryCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg)) {
            PlaceholderImageBox(modifier = Modifier.size(90.dp), background = Color(0xFFF2E7D8))
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.title, style = PantryTypography.titleLarge)
                Text(recipe.subtitle, color = PantryColors.Muted)
                Spacer(Modifier.height(PantrySpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = PantryColors.Muted, modifier = Modifier.size(18.dp))
                    Text(" ${recipe.timeLabel}", color = PantryColors.Muted, style = PantryTypography.labelLarge)
                }
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green700),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Vedi")
            }
        }
    }
}
