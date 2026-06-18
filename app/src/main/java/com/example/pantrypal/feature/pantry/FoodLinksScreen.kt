package com.example.pantrypal.feature.pantry

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.pantrypal.core.designsystem.FoodChip
import com.example.pantrypal.core.designsystem.PantryCard
import com.example.pantrypal.core.designsystem.PantryColors
import com.example.pantrypal.core.designsystem.PantrySpacing
import com.example.pantrypal.core.designsystem.PantryTypography

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
                    buildAnnotatedString {
                        append("Questi prodotti e nomi aiutano PantryPal a riconoscere automaticamente ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(state.name) }
                        append(" da barcode e ricette.")
                    },
                    color = PantryColors.InkSoft
                )
            }
        }

        SectionLabel("PRODOTTI SCANSIONATI")
        if (state.scannedProducts.isEmpty()) {
            PantryCard {
                Text("Nessun prodotto collegato", style = PantryTypography.titleMedium)
                Text("I prodotti da barcode appariranno qui.", color = PantryColors.Muted)
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PantrySpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PantrySpacing.sm)
            ) {
                state.scannedProducts.forEach { product ->
                    FoodChip(
                        label = product.productName,
                        icon = Icons.Default.Close,
                        selected = false,
                        onClick = { onEvent(FoodDetailEvent.OnRemoveBarcodeClick(product.barcode)) }
                    )
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
