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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.ExpiryLotsBlock
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantryExpiryLotUi
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.core.designsystem.PlaceholderImageBox
import com.example.pantrypal.domain.model.PerishabilityType
import com.example.pantrypal.domain.model.StorageLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val AddLotTarget = Long.MIN_VALUE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    state: FoodDetailUiState,
    onEvent: (FoodDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var datePickerTarget by remember { mutableStateOf<Long?>(null) }
    val targetLot = state.lots.firstOrNull { it.id == datePickerTarget }

    datePickerTarget?.let { target ->
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetLot?.expirationDate
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.toLocalDate()?.let { date ->
                            if (target == AddLotTarget) {
                                onEvent(FoodDetailEvent.OnAddLotWithDate(date))
                            } else {
                                onEvent(FoodDetailEvent.OnLotDateSelected(target, date))
                            }
                        }
                        datePickerTarget = null
                    }
                ) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) { Text("Annulla") }
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

        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(FoodDetailEvent.OnNameChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nome alimento") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PantryColors.Green700,
                unfocusedBorderColor = PantryColors.Line,
                focusedContainerColor = PantryColors.Card,
                unfocusedContainerColor = PantryColors.Card
            )
        )
        state.errorMessage?.let {
            Text(it, color = PantryColors.Error, style = PantryTypography.labelLarge)
        }

        SectionLabel("CONSERVAZIONE")
        PantryCard {
            SectionLabel("DEPERIBILITA")
            Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md), modifier = Modifier.fillMaxWidth()) {
                FoodChip(
                    label = "Lunga Conservazione",
                    selected = state.perishability == PerishabilityType.LONG_LIFE,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(FoodDetailEvent.OnPerishabilitySelected(PerishabilityType.LONG_LIFE)) }
                )
                FoodChip(
                    label = "Fresco",
                    selected = state.perishability == PerishabilityType.FRESH,
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f),
                    onClick = { onEvent(FoodDetailEvent.OnPerishabilitySelected(PerishabilityType.FRESH)) }
                )
            }
            Spacer(Modifier.height(PantrySpacing.lg))
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(PantryColors.Line))
            Spacer(Modifier.height(PantrySpacing.lg))
            SectionLabel("LUOGO")
            Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md), modifier = Modifier.fillMaxWidth()) {
                FoodChip("Frigo", selected = state.storageLocation == StorageLocation.FRIDGE, icon = Icons.Default.Kitchen, modifier = Modifier.weight(1f), onClick = { onEvent(FoodDetailEvent.OnStorageLocationSelected(StorageLocation.FRIDGE)) })
                FoodChip("Freezer", selected = state.storageLocation == StorageLocation.FREEZER, icon = Icons.Default.Inventory2, modifier = Modifier.weight(1f), onClick = { onEvent(FoodDetailEvent.OnStorageLocationSelected(StorageLocation.FREEZER)) })
                FoodChip("Dispensa", selected = state.storageLocation == StorageLocation.PANTRY, icon = Icons.Default.Inventory2, modifier = Modifier.weight(1f), onClick = { onEvent(FoodDetailEvent.OnStorageLocationSelected(StorageLocation.PANTRY)) })
            }
        }

        ExpiryLotsBlock(
            title = "SCADENZE - ${state.totalQuantity} CONFEZIONI",
            lots = state.lots.map { it.toPantryExpiryLotUi() },
            emptyTitle = "Nessuna scadenza",
            emptyMessage = "Aggiungi una scadenza per far comparire l'alimento in dispensa.",
            onAddClick = { datePickerTarget = AddLotTarget },
            onDateClick = { datePickerTarget = it },
            onMinusClick = { onEvent(FoodDetailEvent.OnLotMinusClick(it)) },
            onPlusClick = { onEvent(FoodDetailEvent.OnLotPlusClick(it)) }
        )

        Button(
            onClick = { onEvent(FoodDetailEvent.OnSaveClick) },
            enabled = state.isDirty && !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PantryColors.Green700, contentColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Text(if (state.isSaving) " Salvataggio..." else " Salva modifiche", style = PantryTypography.titleMedium)
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
            .height(210.dp)
            .background(PantryColors.WarningBg.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
    ) {
        PlaceholderImageBox(modifier = Modifier.align(Alignment.Center).size(58.dp), background = Color.Transparent)
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
        FoodChip(label = "x${state.totalQuantity}", modifier = Modifier.align(Alignment.TopEnd).padding(PantrySpacing.lg))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(PantrySpacing.xl)) {
            Text(state.name.ifBlank { "Alimento" }, style = PantryTypography.headlineMedium, color = Color.White)
            Text("${state.totalQuantity} confezioni - ${state.updatedLabel}", color = Color.White.copy(alpha = 0.86f))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = PantryTypography.labelLarge, color = PantryColors.Muted)
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun FoodLotUi.toPantryExpiryLotUi(): PantryExpiryLotUi =
    PantryExpiryLotUi(
        id = id,
        dateLabel = dateLabel,
        expirationLabel = expirationLabel,
        expirationDate = expirationDate,
        quantity = quantity,
        isExpired = isExpired
    )
