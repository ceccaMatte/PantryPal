package com.example.pantrypal.feature.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.EmptyState
import com.example.pantrypal.core.designsystem.ErrorState
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.LoadingState
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.core.designsystem.PlaceholderImageBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.linkSheetIngredientKey != null) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(RecipeDetailEvent.OnDismissIngredientSheet) },
            containerColor = PantryColors.Card
        ) {
            IngredientLinksSheet(state = state, onEvent = onEvent)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PantryColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(PantrySpacing.sm)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onEvent(RecipeDetailEvent.OnBackClick) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
            }
            Text(
                "Dettaglio Ricetta",
                modifier = Modifier.weight(1f),
                style = PantryTypography.titleMedium,
                color = PantryColors.Green700
            )
            IconButton(onClick = { onEvent(RecipeDetailEvent.OnFavoriteClick) }) {
                Icon(
                    if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Preferito",
                    tint = PantryColors.Green700
                )
            }
            IconButton(onClick = { onEvent(RecipeDetailEvent.OnShareClick) }) {
                Icon(Icons.Default.Share, contentDescription = "Condividi", tint = PantryColors.Green700)
            }
        }

        when {
            state.isLoading -> {
                LoadingState("Caricamento ricetta...")
                return@Column
            }
            state.errorMessage != null -> {
                ErrorState(state.errorMessage)
                return@Column
            }
            state.configMissing -> {
                EmptyState("Spoonacular non configurato", "Configura la chiave API o apri una ricetta preferita salvata.")
                return@Column
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(PantryColors.WarningBg.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
        ) {
            PlaceholderImageBox(modifier = Modifier.align(Alignment.Center).size(50.dp), background = Color.Transparent)
            FoodChip(
                label = "${state.readyInMinutes} min",
                icon = Icons.Default.AccessTime,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(PantrySpacing.md)
            )
        }

        Text(state.title, style = PantryTypography.headlineMedium, color = PantryColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md), verticalAlignment = Alignment.CenterVertically) {
            MetaLabel(Icons.Default.AccessTime, "${state.readyInMinutes} min")
            MetaLabel(Icons.Default.Info, state.difficultyLabel)
            MetaLabel(Icons.Default.People, state.servingsLabel)
        }
        Text(
            state.description,
            color = PantryColors.Muted,
            style = PantryTypography.bodyLarge,
            maxLines = if (state.isSummaryExpanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = { onEvent(RecipeDetailEvent.OnSummaryToggleClick) }) {
            Text(if (state.isSummaryExpanded) "Nascondi" else "Mostra altro")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Ingredienti", style = PantryTypography.titleMedium)
                Text("Tocca un ingrediente per collegarlo.", color = PantryColors.Muted, style = PantryTypography.labelLarge)
            }
            FoodChip(label = "${state.presentIngredients.size + state.missingIngredients.size} item")
        }

        IngredientSection(
            title = "In Dispensa",
            count = state.presentIngredients.size,
            containerColor = PantryColors.Green50,
            ingredients = state.presentIngredients,
            isPresent = true,
            state = state,
            onEvent = onEvent
        )
        IngredientSection(
            title = "Da Comprare",
            count = state.missingIngredients.size,
            containerColor = PantryColors.ErrorBg,
            ingredients = state.missingIngredients,
            isPresent = false,
            state = state,
            onEvent = onEvent
        )
        Spacer(Modifier.height(104.dp))
    }
}

@Composable
private fun MetaLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = PantryColors.Muted, modifier = Modifier.size(18.dp))
        Text(" $label", color = PantryColors.Muted, style = PantryTypography.labelLarge)
    }
}

@Composable
private fun IngredientSection(
    title: String,
    count: Int,
    containerColor: Color,
    ingredients: List<RecipeIngredientUi>,
    isPresent: Boolean,
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit
) {
    PantryCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(horizontal = PantrySpacing.md, vertical = PantrySpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)) {
                Icon(if (isPresent) Icons.Default.Restaurant else Icons.Default.WarningAmber, contentDescription = null, tint = if (isPresent) PantryColors.Green700 else PantryColors.Error, modifier = Modifier.size(22.dp))
                Text(title, style = PantryTypography.titleMedium, color = if (isPresent) PantryColors.Green700 else PantryColors.Error)
            }
            Text("$count", style = PantryTypography.labelLarge, color = if (isPresent) PantryColors.Green700 else PantryColors.Error)
        }
        ingredients.forEach { ingredient ->
            IngredientRow(
                ingredient = ingredient,
                isPresent = isPresent,
                isExpanded = state.expandedIngredientKey == ingredient.key,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: RecipeIngredientUi,
    isPresent: Boolean,
    isExpanded: Boolean,
    onEvent: (RecipeDetailEvent) -> Unit
) {
    Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PantrySpacing.xs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(RecipeDetailEvent.OnIngredientClick(ingredient.key)) }
                .padding(vertical = PantrySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)
        ) {
            Icon(
                if (isPresent) Icons.Default.CheckCircleOutline else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isPresent) PantryColors.Green700 else PantryColors.Muted,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(ingredient.name, style = PantryTypography.titleMedium, color = PantryColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                ingredient.pantryMatchLabel?.let {
                    Text(it, color = PantryColors.Green700, style = PantryTypography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(ingredient.amountLabel, color = PantryColors.Green700, style = PantryTypography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = PantryColors.Muted,
                modifier = Modifier.size(22.dp)
            )
        }

        if (isExpanded) {
            IngredientInlineEditor(
                ingredient = ingredient,
                isPresent = isPresent,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun IngredientInlineEditor(
    ingredient: RecipeIngredientUi,
    isPresent: Boolean,
    onEvent: (RecipeDetailEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPresent) PantryColors.Green50 else PantryColors.ErrorBg.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(PantrySpacing.md),
        verticalArrangement = Arrangement.spacedBy(PantrySpacing.sm)
    ) {
        if (isPresent) {
            val count = ingredient.availableCategories.size
            if (count > 0) {
                Text(
                    "$count CORRISPONDENZ${if (count == 1) "A" else "E"} IN DISPENSA",
                    style = PantryTypography.labelLarge,
                    color = PantryColors.Green700
                )
                ingredient.availableCategories.forEach { category ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(RecipeDetailEvent.OnAvailableCategoryClick(ingredient.key, category.categoryId)) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (category.selected) PantryColors.Card else PantryColors.Background,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (category.selected) PantryColors.Green700 else PantryColors.Line
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = PantrySpacing.md, vertical = PantrySpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = PantryColors.Green700,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                category.label,
                                style = PantryTypography.titleMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                if (category.selected) Icons.Default.CheckCircleOutline else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (category.selected) PantryColors.Green700 else PantryColors.Line,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            } else {
                Text("Segnato manualmente in questa ricetta", color = PantryColors.Green700, style = PantryTypography.labelLarge)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onEvent(RecipeDetailEvent.OnMoveSelectedToBuyClick) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("Sposta in \"Da comprare\"", color = PantryColors.Error, style = PantryTypography.labelLarge)
                }
                Button(
                    onClick = { onEvent(RecipeDetailEvent.OnManageIngredientLinksClick(ingredient.key)) },
                    colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green700, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Gestisci", maxLines = 1)
                }
            }
        } else {
            if (ingredient.linkedCategoryNames.isNotEmpty()) {
                Text(
                    "Collegato a: ${ingredient.linkedCategoryNames.joinToString()}",
                    color = PantryColors.Muted,
                    style = PantryTypography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "Non risulta in dispensa. Ce l'hai già? Segnalo come presente per toglierlo dalla lista.",
                color = PantryColors.Muted,
                style = PantryTypography.labelLarge
            )
            Button(
                onClick = { onEvent(RecipeDetailEvent.OnMarkSelectedInPantryClick) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green700, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Ce l'ho — segna in dispensa", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = { onEvent(RecipeDetailEvent.OnManageIngredientLinksClick(ingredient.key)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green50, contentColor = PantryColors.Green700),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Collega alimento", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun IngredientLinksSheet(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit
) {
    val ingredientName = (state.presentIngredients + state.missingIngredients)
        .firstOrNull { it.key == state.linkSheetIngredientKey }
        ?.name
        .orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(PantrySpacing.md)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 44.dp, height = 5.dp)
                .background(PantryColors.Line, RoundedCornerShape(12.dp))
        )
        Text("Collega a un alimento", style = PantryTypography.headlineMedium, color = PantryColors.Ink)
        Text(
            "A quale alimento della tua dispensa corrisponde \"$ingredientName\"?",
            color = PantryColors.Muted,
            style = PantryTypography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
            value = state.linkSheetQuery,
            onValueChange = { onEvent(RecipeDetailEvent.OnLinkSheetQueryChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cerca alimento...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PantryColors.Green700,
                unfocusedBorderColor = PantryColors.Line,
                focusedContainerColor = PantryColors.Card,
                unfocusedContainerColor = PantryColors.Card
            )
        )
        state.linkSheetCategories.take(8).forEach { category ->
            LinkCategoryRow(category = category, onEvent = onEvent)
        }
        if (state.canCreateLinkSheetCategory) {
            Button(
                onClick = { onEvent(RecipeDetailEvent.OnLinkSheetCreateCategoryClick) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green50, contentColor = PantryColors.Green700),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Aggiungi nuovo alimento", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Button(
            onClick = { onEvent(RecipeDetailEvent.OnLinkSheetSaveClick) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green700, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Salva collegamenti")
        }
        Spacer(Modifier.height(PantrySpacing.lg))
    }
}

@Composable
private fun LinkCategoryRow(category: RecipeLinkCategoryUi, onEvent: (RecipeDetailEvent) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEvent(RecipeDetailEvent.OnLinkSheetCategoryToggle(category.categoryId)) },
        shape = RoundedCornerShape(16.dp),
        color = if (category.selected) PantryColors.Green50 else PantryColors.Card,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (category.selected) PantryColors.Green700 else PantryColors.Line
        )
    ) {
        Row(
            modifier = Modifier.padding(PantrySpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)
        ) {
            Icon(
                imageVector = category.storageIcon(),
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .background(PantryColors.Green50, RoundedCornerShape(13.dp))
                    .padding(10.dp),
                tint = category.storageTint()
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(category.label, style = PantryTypography.titleMedium, color = PantryColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(category.subtitle, style = PantryTypography.labelLarge, color = PantryColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                if (category.selected) Icons.Default.CheckCircleOutline else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (category.selected) PantryColors.Green700 else PantryColors.Line,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

private fun RecipeLinkCategoryUi.storageIcon() =
    when {
        subtitle.contains("Freezer", ignoreCase = true) -> Icons.Default.AcUnit
        subtitle.contains("Dispensa", ignoreCase = true) -> Icons.Default.Inventory2
        subtitle.contains("Frigo", ignoreCase = true) -> Icons.Default.Kitchen
        else -> Icons.Default.Restaurant
    }

private fun RecipeLinkCategoryUi.storageTint(): Color =
    when {
        subtitle.contains("Freezer", ignoreCase = true) -> PantryColors.Freezer
        subtitle.contains("Dispensa", ignoreCase = true) -> PantryColors.Pantry
        else -> PantryColors.Green700
    }
