package com.example.pantrypal.feature.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.pantrypal.domain.model.AppTheme

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
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
        Text("Profilo", style = PantryTypography.displaySmall)

        SectionLabel("PROFILO UTENTE")
        PantryCard {
            SectionLabel("NOME UTENTE")
            OutlinedTextField(
                value = state.username,
                onValueChange = { onEvent(ProfileEvent.OnUsernameChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PantryColors.Green700) },
                trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = PantryColors.Muted) },
                textStyle = PantryTypography.titleLarge,
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PantryColors.Line,
                    unfocusedBorderColor = PantryColors.Line,
                    focusedContainerColor = PantryColors.Card,
                    unfocusedContainerColor = PantryColors.Card
                )
            )
        }

        SectionLabel("IMPOSTAZIONI")
        PantryCard {
            SettingRow(Icons.Default.Public, "Lingua", "Italiano")
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PantryColors.Line)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg)) {
                IconBadge(Icons.Default.DarkMode)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tema", style = PantryTypography.titleLarge)
                    Spacer(Modifier.height(PantrySpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(PantrySpacing.sm), modifier = Modifier.fillMaxWidth()) {
                        ThemeChip("Chiaro", AppTheme.LIGHT, state.theme, Icons.Default.WbSunny, onEvent, Modifier.weight(1f))
                        ThemeChip("Scuro", AppTheme.DARK, state.theme, Icons.Default.DarkMode, onEvent, Modifier.weight(1f))
                        ThemeChip("Auto", AppTheme.SYSTEM, state.theme, Icons.Default.Language, onEvent, Modifier.weight(1f))
                    }
                }
            }
        }

        SectionLabel("NOTIFICHE")
        PantryCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg)) {
                IconBadge(Icons.Default.NotificationsNone, PantryColors.ErrorBg, PantryColors.Error)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Avvisi di pre-scadenza", style = PantryTypography.titleLarge)
                    Text("Promemoria per il cibo in scadenza", color = PantryColors.Muted)
                }
                Switch(
                    checked = state.expirationNotificationsEnabled,
                    onCheckedChange = { onEvent(ProfileEvent.OnNotificationsChanged(it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PantryColors.Green700)
                )
            }
            Spacer(Modifier.height(PantrySpacing.lg))
            NotificationDaysRow(
                icon = Icons.Default.Inventory2,
                title = "Alimenti freschi",
                days = state.freshNotificationDays,
                onMinus = { onEvent(ProfileEvent.OnFreshDaysMinus) },
                onPlus = { onEvent(ProfileEvent.OnFreshDaysPlus) }
            )
            Spacer(Modifier.height(PantrySpacing.lg))
            NotificationDaysRow(
                icon = Icons.Default.Inventory2,
                title = "Lunga conservazione",
                days = state.longLifeNotificationDays,
                iconBackground = Color(0xFFF4E8DB),
                iconColor = PantryColors.Pantry,
                onMinus = { onEvent(ProfileEvent.OnLongLifeDaysMinus) },
                onPlus = { onEvent(ProfileEvent.OnLongLifeDaysPlus) }
            )
        }
        Spacer(Modifier.height(104.dp))
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = PantrySpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg)
    ) {
        IconBadge(icon)
        Text(title, style = PantryTypography.titleLarge, modifier = Modifier.weight(1f))
        Text(value, color = PantryColors.Muted, style = PantryTypography.titleMedium)
    }
}

@Composable
private fun ThemeChip(
    label: String,
    theme: AppTheme,
    selectedTheme: AppTheme,
    icon: ImageVector,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    FoodChip(
        label = label,
        selected = theme == selectedTheme,
        icon = icon,
        modifier = modifier,
        onClick = { onEvent(ProfileEvent.OnThemeSelected(theme)) }
    )
}

@Composable
private fun NotificationDaysRow(
    icon: ImageVector,
    title: String,
    days: Int,
    iconBackground: Color = PantryColors.Green50,
    iconColor: Color = PantryColors.Green700,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantrySpacing.lg)) {
        IconBadge(icon, iconBackground, iconColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = PantryTypography.titleMedium)
            Text("Avvisa N giorni prima", color = PantryColors.Muted)
        }
        Stepper(value = days, onMinus = onMinus, onPlus = onPlus)
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    background: Color = PantryColors.Green50,
    color: Color = PantryColors.Green700
) {
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier
            .size(56.dp)
            .background(background, RoundedCornerShape(16.dp))
            .padding(14.dp),
        tint = color
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = PantryTypography.labelLarge, color = PantryColors.Muted)
}
