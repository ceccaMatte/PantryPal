package com.example.pantrypal.feature.addfood

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.core.designsystem.Stepper
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation

@Composable
fun ManualAddScreen(
    state: ManualAddUiState,
    onEvent: (ManualAddEvent) -> Unit,
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
            IconButton(onClick = { onEvent(ManualAddEvent.OnBackClick) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
            }
            Text(
                text = "Aggiungi Alimento",
                style = PantryTypography.headlineMedium,
                color = PantryColors.Green700,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = { onEvent(ManualAddEvent.OnQueryChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            textStyle = PantryTypography.titleLarge,
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PantryColors.Line,
                unfocusedBorderColor = PantryColors.Line,
                focusedContainerColor = PantryColors.Card,
                unfocusedContainerColor = PantryColors.Card
            )
        )

        Text("Scegli l'alimento corrispondente o creane uno nuovo", color = PantryColors.Muted)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PantrySpacing.sm)
        ) {
            state.suggestions.forEach { suggestion ->
                FoodChip(
                    label = suggestion.label,
                    selected = suggestion == state.selectedSuggestion,
                    dashed = suggestion.isCreateNew,
                    icon = suggestion.icon,
                    onClick = { onEvent(ManualAddEvent.OnSuggestionSelected(suggestion)) }
                )
            }
        }

        SectionLabel("CONSERVAZIONE")
        PantryCard {
            SectionLabel("DEPERIBILITA")
            Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md), modifier = Modifier.fillMaxWidth()) {
                ChoiceButton(
                    label = "Lunga Conservazione",
                    selected = state.perishability == PerishabilityType.LONG_LIFE,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(ManualAddEvent.OnPerishabilitySelected(PerishabilityType.LONG_LIFE)) }
                )
                ChoiceButton(
                    label = "Fresco",
                    selected = state.perishability == PerishabilityType.FRESH,
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(ManualAddEvent.OnPerishabilitySelected(PerishabilityType.FRESH)) }
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
                StorageChoice("Frigo", StorageLocation.FRIDGE, state.storageLocation, Icons.Default.Kitchen, onEvent, Modifier.weight(1f))
                StorageChoice("Freezer", StorageLocation.FREEZER, state.storageLocation, Icons.Default.AcUnit, onEvent, Modifier.weight(1f))
                StorageChoice("Dispensa", StorageLocation.PANTRY, state.storageLocation, Icons.Default.Inventory2, onEvent, Modifier.weight(1f))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("SCADENZE - ${state.lots.size} AGGIUNTA")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PantryColors.Green700)
                Text("Aggiungi", color = PantryColors.Green700, style = PantryTypography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        state.lots.forEach { lot ->
            PantryCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg)) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier
                            .size(58.dp)
                            .background(PantryColors.Green50, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        tint = PantryColors.Green700
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lot.dateLabel, style = PantryTypography.titleLarge)
                        Text(lot.expirationLabel, color = PantryColors.WarningText, fontWeight = FontWeight.Bold)
                    }
                    Stepper(value = lot.quantity, onMinus = { }, onPlus = { })
                }
            }
        }

        Button(
            onClick = { onEvent(ManualAddEvent.OnSaveClick) },
            enabled = state.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green700, contentColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(" Salva alimento", style = PantryTypography.titleMedium)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = PantryTypography.labelLarge, color = PantryColors.Muted)
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    FoodChip(label = label, selected = selected, icon = icon, modifier = modifier, onClick = onClick)
}

@Composable
private fun StorageChoice(
    label: String,
    location: StorageLocation,
    selectedLocation: StorageLocation,
    icon: ImageVector,
    onEvent: (ManualAddEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FoodChip(
        label = label,
        selected = location == selectedLocation,
        icon = icon,
        modifier = modifier,
        onClick = { onEvent(ManualAddEvent.OnStorageLocationSelected(location)) }
    )
}

private val FoodSuggestionUi.icon: ImageVector?
    get() = when {
        isCreateNew -> Icons.Default.Add
        storageLocation == StorageLocation.FRIDGE -> Icons.Default.Kitchen
        storageLocation == StorageLocation.FREEZER -> Icons.Default.AcUnit
        storageLocation == StorageLocation.PANTRY -> Icons.Default.Inventory2
        else -> Icons.Default.Restaurant
    }
