package com.example.pantrypal.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PantryColors {
    val Background = Color(0xFFF3F5F2)
    val Card = Color(0xFFFFFFFF)
    val Green50 = Color(0xFFEAF3ED)
    val Green100 = Color(0xFFDBEBE0)
    val Green600 = Color(0xFF207049)
    val Green700 = Color(0xFF1B5E40)
    val Green900 = Color(0xFF13402B)
    val Ink = Color(0xFF172019)
    val InkSoft = Color(0xFF3C4842)
    val Muted = Color(0xFF6E7B73)
    val Line = Color(0xFFE7EAE7)
    val WarningBg = Color(0xFFFBEFC9)
    val WarningText = Color(0xFF8A6D17)
    val ErrorBg = Color(0xFFFBEAE7)
    val Error = Color(0xFFC0392B)
    val Freezer = Color(0xFF2F76C9)
    val Pantry = Color(0xFFC8642B)
}

object PantrySpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

val PantryTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )
)

private val LightColors = lightColorScheme(
    primary = PantryColors.Green700,
    onPrimary = Color.White,
    primaryContainer = PantryColors.Green50,
    onPrimaryContainer = PantryColors.Green900,
    secondary = PantryColors.Pantry,
    onSecondary = Color.White,
    background = PantryColors.Background,
    onBackground = PantryColors.Ink,
    surface = PantryColors.Card,
    onSurface = PantryColors.Ink,
    error = PantryColors.Error
)

@Composable
fun PantryPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = PantryTypography,
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
