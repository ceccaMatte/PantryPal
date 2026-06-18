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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.ExpiryLotsBlock
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantryExpiryLotUi
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.SaveAddedFoodValidationError
import com.example.pantrypal.domain.model.StorageLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddScreen(
    state: ManualAddUiState,
    onEvent: (ManualAddEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedLotId by remember { mutableStateOf<Long?>(null) }

    if (showDatePicker) {
        val selectedLot = state.lots.firstOrNull { it.id == selectedLotId }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedLot?.expirationDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            selectedLotId?.let { lotId ->
                                onEvent(ManualAddEvent.OnExpirationDateSelected(lotId, millis.toLocalDate()))
                            }
                        }
                        showDatePicker = false
                        selectedLotId = null
                    }
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    selectedLotId = null
                }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

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
        if (SaveAddedFoodValidationError.CATEGORY_REQUIRED in state.validationErrors) {
            Text("Seleziona un alimento", color = PantryColors.Error, style = PantryTypography.labelLarge)
        }

        state.recognizedProductLabel?.let { label ->
            PantryCard(containerColor = PantryColors.Green50) {
                Text("Prodotto riconosciuto", style = PantryTypography.labelLarge, color = PantryColors.Green700)
                Text(label, style = PantryTypography.titleMedium)
                Text("Il barcode verra collegato all'alimento selezionato quando salvi.", color = PantryColors.Muted)
            }
        }

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

        if (SaveAddedFoodValidationError.LOTS_REQUIRED in state.validationErrors) {
            Text("Aggiungi almeno una scadenza valida", color = PantryColors.Error, style = PantryTypography.labelLarge)
        }
        if (SaveAddedFoodValidationError.DATE_REQUIRED in state.validationErrors) {
            Text("Seleziona una data per ogni scadenza", color = PantryColors.Error, style = PantryTypography.labelLarge)
        }
        if (SaveAddedFoodValidationError.QUANTITY_INVALID in state.validationErrors) {
            Text("Quantita non valida", color = PantryColors.Error, style = PantryTypography.labelLarge)
        }

        ExpiryLotsBlock(
            title = "SCADENZE - ${state.lots.sumOf { it.quantity.coerceAtLeast(0) }} AGGIUNTE",
            lots = state.lots.map { it.toPantryExpiryLotUi() },
            emptyTitle = "Nessuna scadenza",
            emptyMessage = "Aggiungi almeno una data di scadenza.",
            onAddClick = { onEvent(ManualAddEvent.OnAddLotClick) },
            onDateClick = {
                selectedLotId = it
                showDatePicker = true
            },
            onRemoveClick = { onEvent(ManualAddEvent.OnRemoveLotClick(it)) },
            onMinusClick = { onEvent(ManualAddEvent.OnQuantityMinus(it)) },
            onPlusClick = { onEvent(ManualAddEvent.OnQuantityPlus(it)) },
            showAsSingleCard = true
        )

        Button(
            onClick = { onEvent(ManualAddEvent.OnSaveClick) },
            enabled = !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green700, contentColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text(if (state.isSaving) " Salvataggio..." else " Salva alimento", style = PantryTypography.titleMedium)
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

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ITALIAN)

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun LocalDate.toRelativeLabel(): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), this).toInt()
    return when {
        days < 0 -> "scaduta"
        days == 0 -> "oggi"
        days == 1 -> "tra 1 giorno"
        days < 30 -> "tra $days giorni"
        days < 60 -> "tra 1 mese"
        else -> "tra ${days / 30} mesi"
    }
}

private fun ManualAddLotUi.toPantryExpiryLotUi(): PantryExpiryLotUi =
    PantryExpiryLotUi(
        id = id,
        dateLabel = expirationDate?.format(DateFormatter) ?: "Scegli data",
        expirationLabel = expirationDate?.toRelativeLabel().orEmpty(),
        expirationDate = expirationDate,
        quantity = quantity,
        isExpired = expirationDate?.isBefore(LocalDate.now()) == true
    )
