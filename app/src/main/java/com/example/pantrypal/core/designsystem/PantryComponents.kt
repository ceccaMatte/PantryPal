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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        content = { Column(Modifier.padding(PantrySpacing.md), content = content) }
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp)) }
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
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Rimuovi", tint = PantryColors.Green700)
        }
        Text(valueLabel, style = PantryTypography.labelLarge, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center, maxLines = 1)
        IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi", tint = PantryColors.Green700)
        }
    }
}

@Composable
fun PantryFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(66.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        containerColor = PantryColors.Green700,
        contentColor = Color.White
    ) {
        Icon(Icons.Default.Add, contentDescription = "Aggiungi", modifier = Modifier.size(30.dp))
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .height(78.dp),
            shape = RoundedCornerShape(24.dp),
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
                Spacer(Modifier.width(72.dp))
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
        Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(22.dp))
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
        modifier = modifier.background(background, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .border(2.dp, PantryColors.Line, CircleShape)
        )
    }
}
