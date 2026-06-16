package com.example.pantrypal.feature.pantry

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.core.designsystem.PlaceholderImageBox
import com.example.pantrypal.core.designsystem.Stepper
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation

@Composable
fun FoodDetailScreen(
    state: FoodDetailUiState,
    onEvent: (FoodDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PantryColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(PantrySpacing.lg)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onEvent(FoodDetailEvent.OnBackClick) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
            }
            Text(
                "Dettaglio Alimento",
                modifier = Modifier.weight(1f),
                style = PantryTypography.headlineMedium,
                color = PantryColors.Green700
            )
        }

        FoodHero(state)
        SectionLabel("CONSERVAZIONE")
        PantryCard {
            SectionLabel("DEPERIBILITA")
            Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md), modifier = Modifier.fillMaxWidth()) {
                FoodChip(
                    label = "Lunga Conservazione",
                    selected = state.perishability == PerishabilityType.LONG_LIFE,
                    modifier = Modifier.weight(1f)
                )
                FoodChip(
                    label = "Fresco",
                    selected = state.perishability == PerishabilityType.FRESH,
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(PantrySpacing.lg))
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PantryColors.Line)
            )
            Spacer(Modifier.height(PantrySpacing.lg))
            SectionLabel("LUOGO")
            Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md), modifier = Modifier.fillMaxWidth()) {
                FoodChip("Frigo", selected = state.storageLocation == StorageLocation.FRIDGE, icon = Icons.Default.Kitchen, modifier = Modifier.weight(1f))
                FoodChip("Freezer", selected = state.storageLocation == StorageLocation.FREEZER, icon = Icons.Default.Inventory2, modifier = Modifier.weight(1f))
                FoodChip("Dispensa", selected = state.storageLocation == StorageLocation.PANTRY, icon = Icons.Default.Inventory2, modifier = Modifier.weight(1f))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("SCADENZE - ${state.totalQuantity} CONFEZIONI")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onEvent(FoodDetailEvent.OnAddLotClick) }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PantryColors.Green700)
                Text(" Aggiungi", color = PantryColors.Green700, style = PantryTypography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        PantryCard {
            state.lots.forEachIndexed { index, lot ->
                LotRow(lot, onEvent)
                if (index < state.lots.lastIndex) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(PantryColors.Line)
                    )
                }
            }
        }

        PantryCard(
            containerColor = PantryColors.Green50,
            onClick = { onEvent(FoodDetailEvent.OnManageLinksClick) }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)) {
                Icon(Icons.Default.Link, contentDescription = null, tint = PantryColors.Green700)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Collegamenti alimento", style = PantryTypography.titleMedium, color = PantryColors.Green700)
                    Text("Aiuta PantryPal a riconoscere prodotti e nomi ricetta.", color = PantryColors.Muted)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PantryColors.Green700)
            }
        }
        Spacer(Modifier.height(104.dp))
    }
}

@Composable
private fun FoodHero(state: FoodDetailUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color(0xFFF4E8DB), RoundedCornerShape(24.dp))
    ) {
        PlaceholderImageBox(
            modifier = Modifier
                .align(Alignment.Center)
                .size(58.dp),
            background = Color.Transparent
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.55f)
                    ),
                    RoundedCornerShape(24.dp)
                )
        )
        FoodChip(
            label = "x${state.totalQuantity}",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(PantrySpacing.lg)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(PantrySpacing.xl)
        ) {
            Text(state.name, style = PantryTypography.headlineMedium, color = Color.White)
            Text("${state.totalQuantity} confezioni - ${state.updatedLabel}", color = Color.White.copy(alpha = 0.86f))
        }
    }
}

@Composable
private fun LotRow(lot: FoodLotUi, onEvent: (FoodDetailEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PantrySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg)
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .background(if (lot.isExpired) PantryColors.ErrorBg else PantryColors.Green50, RoundedCornerShape(16.dp))
                .padding(14.dp),
            tint = if (lot.isExpired) PantryColors.Error else PantryColors.Green700
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(lot.dateLabel, style = PantryTypography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (lot.isExpired) PantryColors.Error else PantryColors.WarningText,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    " ${lot.expirationLabel}",
                    color = if (lot.isExpired) PantryColors.Error else PantryColors.WarningText,
                    style = PantryTypography.titleMedium
                )
            }
        }
        Stepper(
            value = lot.quantity,
            onMinus = { onEvent(FoodDetailEvent.OnLotMinusClick(lot.id)) },
            onPlus = { onEvent(FoodDetailEvent.OnLotPlusClick(lot.id)) }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = PantryTypography.labelLarge, color = PantryColors.Muted)
}
