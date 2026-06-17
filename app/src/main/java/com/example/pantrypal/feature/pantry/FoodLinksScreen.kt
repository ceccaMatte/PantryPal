package com.example.pantrypal.feature.pantry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography
import com.example.pantrypal.core.designsystem.PlaceholderImageBox

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodLinksScreen(
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
        verticalArrangement = Arrangement.spacedBy(PantrySpacing.xl)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onEvent(FoodDetailEvent.OnBackClick) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Collegamenti alimento", style = PantryTypography.headlineMedium, color = PantryColors.Green700)
                Text(state.name, style = PantryTypography.titleMedium, color = PantryColors.Green700)
            }
        }

        PantryCard(containerColor = PantryColors.Green50) {
            Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PantryColors.Green700)
                Text(
                    "Questi prodotti e nomi aiutano PantryPal a riconoscere automaticamente ${state.name} da barcode e ricette.",
                    color = PantryColors.InkSoft
                )
            }
        }

        SectionLabel("PRODOTTI SCANSIONATI")
        if (state.scannedProducts.isEmpty()) {
            PantryCard {
                Text("Nessun prodotto collegato", style = PantryTypography.titleMedium)
                Text("I prodotti da barcode appariranno qui nei prossimi step.", color = PantryColors.Muted)
            }
        } else {
            state.scannedProducts.forEach { product ->
                PantryCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg)) {
                        PlaceholderImageBox(modifier = Modifier.size(76.dp), background = Color(0xFFF2E7D8))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.productName, style = PantryTypography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(product.subtitle, color = PantryColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onEvent(FoodDetailEvent.OnRemoveBarcodeClick(product.barcode)) }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = PantryColors.Error)
                            Text(" Rimuovi", color = PantryColors.Error, fontWeight = FontWeight.Bold)
                        }
                    }
                }   
            }   
        }

        SectionLabel("NOMI NELLE RICETTE")
        if (state.recipeAliases.isEmpty()) {
            PantryCard {
                Text("Nessun alias ricetta", style = PantryTypography.titleMedium)
                Text("Aggiungi nomi come milk, latte parzialmente scremato o simili.", color = PantryColors.Muted)
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.sm), verticalArrangement = Arrangement.spacedBy(PantrySpacing.sm)) {
                state.recipeAliases.forEach { alias ->
                    FoodChip(
                        label = alias.alias,
                        icon = Icons.Default.Close,
                        selected = false,
                        onClick = { onEvent(FoodDetailEvent.OnRemoveAliasClick(alias.id)) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.aliasDraft,
            onValueChange = { onEvent(FoodDetailEvent.OnAliasDraftChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Aggiungi un nome...") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = PantryColors.Green700) },
            trailingIcon = {
                TextButton(
                    onClick = { onEvent(FoodDetailEvent.OnAddAliasClick) },
                    enabled = state.aliasDraft.isNotBlank()
                ) {
                    Text("Aggiungi", maxLines = 1)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PantryColors.Line,
                unfocusedBorderColor = PantryColors.Line,
                focusedContainerColor = PantryColors.Card,
                unfocusedContainerColor = PantryColors.Card
            )
        )
        Spacer(Modifier.height(104.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = PantryTypography.labelLarge, color = PantryColors.Muted)
}
