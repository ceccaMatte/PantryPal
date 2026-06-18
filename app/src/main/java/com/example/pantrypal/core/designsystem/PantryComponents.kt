package com.example.pantrypal.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import java.time.LocalDate

@Composable
fun PantryCard(
    modifier: Modifier = Modifier,
    containerColor: Color = PantryColors.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = { Column(Modifier.padding(PantrySpacing.card), content = content) }
    )
}

@Composable
fun FoodChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
    badge: String? = null,
    dashed: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val background = if (selected) PantryColors.Green700 else PantryColors.Card
    val contentColor = if (selected) Color.White else PantryColors.InkSoft
    val shape = RoundedCornerShape(28.dp)
    val border = if (selected) null else BorderStroke(1.dp, PantryColors.Line)
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = background,
        contentColor = contentColor,
        border = if (dashed) BorderStroke(1.dp, PantryColors.Green700) else border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(15.dp)) }
            Text(label, style = PantryTypography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            badge?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .background(PantryColors.ErrorBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = PantryColors.Error,
                    style = PantryTypography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun Stepper(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    valueLabel: String = "x$value"
) {
    Row(
        modifier = modifier
            .border(1.dp, PantryColors.Line, RoundedCornerShape(28.dp))
            .background(PantryColors.Card, RoundedCornerShape(28.dp))
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMinus, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Rimuovi", tint = PantryColors.Green700)
        }
        Text(valueLabel, style = PantryTypography.labelLarge, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center, maxLines = 1)
        IconButton(onClick = onPlus, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi", tint = PantryColors.Green700)
        }
    }
}

@Composable
fun PantryFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(58.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        containerColor = PantryColors.Green700,
        contentColor = Color.White
    ) {
        Icon(Icons.Default.Add, contentDescription = "Aggiungi", modifier = Modifier.size(28.dp))
    }
}

data class PantryBottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun PantryBottomBar(
    items: List<PantryBottomBarItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(68.dp),
            shape = RoundedCornerShape(22.dp),
            color = PantryColors.Card,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.take(2).forEach { item ->
                    NavigationBarItemContent(item, currentRoute, onItemClick)
                }
                Spacer(Modifier.width(64.dp))
                items.drop(2).forEach { item ->
                    NavigationBarItemContent(item, currentRoute, onItemClick)
                }
            }
        }
        PantryFab(onFabClick, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun RowScope.NavigationBarItemContent(
    item: PantryBottomBarItem,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    val selected = currentRoute == item.route || currentRoute?.startsWith(item.route) == true
    val color = if (selected) PantryColors.Green700 else PantryColors.Muted
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onItemClick(item.route) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(20.dp))
        Text(item.label, style = PantryTypography.labelLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    PantryCard(modifier = modifier) {
        Text(title, style = PantryTypography.titleMedium)
        Spacer(Modifier.height(PantrySpacing.sm))
        Text(message, color = PantryColors.Muted)
    }
}

@Composable
fun LoadingState(message: String = "Caricamento...", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PantrySpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = PantryColors.Green700)
        Spacer(Modifier.height(PantrySpacing.md))
        Text(message, color = PantryColors.Muted)
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PantryColors.ErrorBg, RoundedCornerShape(18.dp))
            .padding(PantrySpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = PantryColors.Error)
        Spacer(Modifier.width(PantrySpacing.sm))
        Text(message, color = PantryColors.Error, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlaceholderImageBox(
    modifier: Modifier = Modifier,
    background: Color = PantryColors.Green50
) {
    Box(
        modifier = modifier.background(background, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .border(2.dp, PantryColors.Line, CircleShape)
        )
    }
}

@Composable
fun PantryImage(
    model: Any?,
    modifier: Modifier = Modifier,
    background: Color = PantryColors.Green50,
    contentDescription: String? = null
) {
    if (model == null || model.toString().isBlank()) {
        PlaceholderImageBox(modifier = modifier, background = background)
        return
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { PlaceholderImageBox(modifier = Modifier.fillMaxSize(), background = Color.Transparent) },
            error = { PlaceholderImageBox(modifier = Modifier.fillMaxSize(), background = Color.Transparent) }
        )
    }
}

@Composable
fun FoodCategoryImage(
    imageUri: String?,
    modifier: Modifier = Modifier,
    background: Color = PantryColors.Green50
) {
    PantryImage(model = imageUri, modifier = modifier, background = background, contentDescription = "Alimento")
}

@Composable
fun RecipeImage(
    imageUrl: String?,
    localImageUri: String? = null,
    modifier: Modifier = Modifier,
    background: Color = PantryColors.WarningBg.copy(alpha = 0.55f)
) {
    PantryImage(model = localImageUri ?: imageUrl, modifier = modifier, background = background, contentDescription = "Ricetta")
}

@Composable
fun ProductImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    background: Color = PantryColors.WarningBg.copy(alpha = 0.55f)
) {
    PantryImage(model = imageUrl, modifier = modifier, background = background, contentDescription = "Prodotto")
}

data class PantryExpiryLotUi(
    val id: Long,
    val dateLabel: String,
    val expirationLabel: String,
    val expirationDate: LocalDate?,
    val quantity: Int,
    val isExpired: Boolean = false
)

@Composable
fun ExpiryLotsBlock(
    title: String,
    lots: List<PantryExpiryLotUi>,
    emptyTitle: String,
    emptyMessage: String,
    onAddClick: () -> Unit,
    onDateClick: (Long) -> Unit,
    onRemoveClick: ((Long) -> Unit)? = null,
    onMinusClick: (Long) -> Unit,
    onPlusClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showAsSingleCard: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PantrySpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = PantryTypography.labelLarge, color = PantryColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onAddClick)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PantryColors.Green700, modifier = Modifier.size(20.dp))
                Text(" Aggiungi", color = PantryColors.Green700, style = PantryTypography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (lots.isEmpty()) {
            EmptyState(emptyTitle, emptyMessage)
        } else if (showAsSingleCard) {
            PantryCard {
                lots.forEachIndexed { index, lot ->
                    ExpiryLotRow(
                        lot = lot,
                        onDateClick = onDateClick,
                        onRemoveClick = onRemoveClick,
                        onMinusClick = onMinusClick,
                        onPlusClick = onPlusClick
                    )
                    if (index < lots.lastIndex) {
                        Spacer(Modifier.fillMaxWidth().height(1.dp).background(PantryColors.Line))
                    }
                }
            }
        } else {
            lots.forEach { lot ->
                PantryCard {
                    ExpiryLotRow(
                        lot = lot,
                        onDateClick = onDateClick,
                        onRemoveClick = onRemoveClick,
                        onMinusClick = onMinusClick,
                        onPlusClick = onPlusClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpiryLotRow(
    lot: PantryExpiryLotUi,
    onDateClick: (Long) -> Unit,
    onRemoveClick: ((Long) -> Unit)?,
    onMinusClick: (Long) -> Unit,
    onPlusClick: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PantrySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrySpacing.md)
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .background(if (lot.isExpired) PantryColors.ErrorBg else PantryColors.Green50, RoundedCornerShape(15.dp))
                .clickable { onDateClick(lot.id) }
                .padding(13.dp),
            tint = if (lot.isExpired) PantryColors.Error else PantryColors.Green700
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onDateClick(lot.id) }
        ) {
            Text(lot.dateLabel, style = PantryTypography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (lot.expirationLabel.isNotBlank()) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (lot.isExpired) PantryColors.Error else PantryColors.WarningText,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        " ${lot.expirationLabel}",
                        color = if (lot.isExpired) PantryColors.Error else PantryColors.WarningText,
                        style = PantryTypography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        onRemoveClick?.let {
            IconButton(onClick = { it(lot.id) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Rimuovi scadenza", tint = PantryColors.Error)
            }
        }
        Stepper(
            value = lot.quantity,
            onMinus = { onMinusClick(lot.id) },
            onPlus = { onPlusClick(lot.id) }
        )
    }
}
