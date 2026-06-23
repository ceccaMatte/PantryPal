package com.example.pantrypal.feature.pantry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.EmptyState
import com.example.pantrypal.core.designsystem.FoodCategoryImage
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.core.designsystem.Stepper
import com.example.pantrypal.domain.model.StorageLocation
import com.example.pantrypal.domain.model.StorageLocationFilter

@Composable
fun PantryScreen(
    state: PantryUiState,
    onEvent: (PantryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = state.pantryRows
        .filterBy(state.selectedFilter)
        .filterBySearch(state.normalizedSearchQuery)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PantryColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(PantrySpacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dispensa", style = PantryTypography.headlineMedium)
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onEvent(PantryEvent.OnSearchQueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cerca alimento...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PantryColors.Muted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PantryColors.Green700,
                unfocusedBorderColor = PantryColors.Line,
                focusedContainerColor = PantryColors.Card,
                unfocusedContainerColor = PantryColors.Card
            )
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PantrySpacing.sm)
        ) {
            PantryFilterChip("Tutti", StorageLocationFilter.ALL, state.selectedFilter, Icons.Default.CheckCircleOutline, onEvent)
            PantryFilterChip("Frigo", StorageLocationFilter.FRIDGE, state.selectedFilter, Icons.Default.Kitchen, onEvent)
            PantryFilterChip("Freezer", StorageLocationFilter.FREEZER, state.selectedFilter, Icons.Default.AcUnit, onEvent)
            PantryFilterChip("Dispensa", StorageLocationFilter.PANTRY, state.selectedFilter, Icons.Default.Inventory2, onEvent)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.sm)) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = PantryColors.Error)
            Text("In scadenza", style = PantryTypography.titleLarge)
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)) {
            items(state.expiringFoods) { food ->
                ExpiringFoodCard(food) { onEvent(PantryEvent.OnExpiringFoodClick(food.categoryId)) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TUTTI GLI ALIMENTI - ${rows.sumOf { it.totalQuantity }}", style = PantryTypography.labelLarge, color = PantryColors.Muted)
            FoodChip(label = "Presenti", icon = Icons.Default.CheckCircleOutline)
        }

        if (rows.isEmpty()) {
            EmptyState(
                title = if (state.searchQuery.isBlank()) "Nessun alimento" else "Nessun alimento trovato",
                message = if (state.searchQuery.isBlank()) {
                    "Aggiungi un alimento per iniziare a popolare questa sezione."
                } else {
                    "Prova con un altro nome o cambia filtro."
                }
            )
        } else {
            rows.groupBy { it.storageLocation }.forEach { (location, groupedRows) ->
                StorageSection(location, groupedRows, onEvent)
            }
        }
        Spacer(Modifier.height(104.dp))
    }
}

@Composable
private fun PantryFilterChip(
    label: String,
    filter: StorageLocationFilter,
    selectedFilter: StorageLocationFilter,
    icon: ImageVector,
    onEvent: (PantryEvent) -> Unit
) {
    FoodChip(
        label = label,
        selected = filter == selectedFilter,
        icon = icon,
        onClick = { onEvent(PantryEvent.OnFilterSelected(filter)) }
    )
}

@Composable
private fun ExpiringFoodCard(food: ExpiringFoodCardUi, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(locationTint(food.storageLocation))
            .clickable(onClick = onClick)
    ) {
        // Immagine che riempie tutta la card come sfondo
        FoodCategoryImage(
            imageUri = food.imageUri,
            modifier = Modifier.fillMaxSize(),
            background = locationTint(food.storageLocation)
        )
        // Gradiente scuro nella metà inferiore per rendere leggibile il testo
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.40f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.62f)
                    )
                )
        )
        // Badge quantità in scadenza
        FoodChip(
            label = "x${food.expiringQuantity}",
            badge = null,
            modifier = Modifier.padding(10.dp)
        )
        // Nome e luogo in basso
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(PantrySpacing.md)
        ) {
            Text(food.name, style = PantryTypography.titleMedium, color = Color.White)
            Text(food.storageLocation.label, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun StorageSection(
    location: StorageLocation,
    rows: List<PantryRowUi>,
    onEvent: (PantryEvent) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(location.icon, contentDescription = null, tint = location.color)
        Spacer(Modifier.width(PantrySpacing.md))
        Text(location.label.uppercase(), style = PantryTypography.labelLarge, color = PantryColors.Muted)
        Spacer(
            Modifier
                .padding(horizontal = PantrySpacing.md)
                .height(1.dp)
                .weight(1f)
                .background(PantryColors.Line)
        )
        Text("${rows.sumOf { it.totalQuantity }}", style = PantryTypography.labelLarge, color = PantryColors.Muted)
    }
    rows.forEach { row ->
        PantryFoodRow(row, onEvent)
    }
}

@Composable
private fun PantryFoodRow(row: PantryRowUi, onEvent: (PantryEvent) -> Unit) {
    PantryCard(onClick = { onEvent(PantryEvent.OnFoodClick(row.categoryId)) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg)) {
            FoodCategoryImage(imageUri = row.imageUri, modifier = Modifier.size(76.dp), background = locationTint(row.storageLocation))
            Column(modifier = Modifier.weight(1f)) {
                Text(row.name, style = PantryTypography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (row.expirationLabel.contains("Scade") || row.expirationLabel.contains("scaduta")) {
                            PantryColors.Error
                        } else {
                            PantryColors.WarningText
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        " ${row.expirationLabel}",
                        color = if (row.expirationLabel.contains("Scade") || row.expirationLabel.contains("scaduta")) {
                            PantryColors.Error
                        } else {
                            PantryColors.WarningText
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Stepper(
                value = row.totalQuantity,
                onMinus = { onEvent(PantryEvent.OnMinusClick(row)) },
                onPlus = { onEvent(PantryEvent.OnPlusClick(row.categoryId)) }
            )
        }
    }
}

private fun List<PantryRowUi>.filterBy(filter: StorageLocationFilter): List<PantryRowUi> =
    when (filter) {
        StorageLocationFilter.ALL -> this
        StorageLocationFilter.FRIDGE -> filter { it.storageLocation == StorageLocation.FRIDGE }
        StorageLocationFilter.FREEZER -> filter { it.storageLocation == StorageLocation.FREEZER }
        StorageLocationFilter.PANTRY -> filter { it.storageLocation == StorageLocation.PANTRY }
    }

private fun List<PantryRowUi>.filterBySearch(normalizedQuery: String): List<PantryRowUi> =
    if (normalizedQuery.isBlank()) {
        this
    } else {
        filter { row -> row.name.lowercase().contains(normalizedQuery) }
    }

private val StorageLocation.label: String
    get() = when (this) {
        StorageLocation.FRIDGE -> "Frigo"
        StorageLocation.FREEZER -> "Freezer"
        StorageLocation.PANTRY -> "Dispensa"
    }

private val StorageLocation.icon: ImageVector
    get() = when (this) {
        StorageLocation.FRIDGE -> Icons.Default.Kitchen
        StorageLocation.FREEZER -> Icons.Default.AcUnit
        StorageLocation.PANTRY -> Icons.Default.Inventory2
    }

private val StorageLocation.color: Color
    get() = when (this) {
        StorageLocation.FRIDGE -> PantryColors.Green600
        StorageLocation.FREEZER -> PantryColors.Freezer
        StorageLocation.PANTRY -> PantryColors.Pantry
    }

private fun locationTint(location: StorageLocation): Color =
    when (location) {
        StorageLocation.FRIDGE -> Color(0xFFEAF2F5)
        StorageLocation.FREEZER -> Color(0xFFE9F0FA)
        StorageLocation.PANTRY -> Color(0xFFF4E8DB)
    }
