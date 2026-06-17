package com.example.pantrypal.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import com.example.pantrypal.domain.model.AppTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PantryColors {
    var Background = Color(0xFFF3F5F2)
        private set
    var Card = Color(0xFFFFFFFF)
        private set
    var Green50 = Color(0xFFEAF3ED)
        private set
    var Green100 = Color(0xFFDBEBE0)
        private set
    var Green600 = Color(0xFF207049)
        private set
    var Green700 = Color(0xFF1B5E40)
        private set
    var Green900 = Color(0xFF13402B)
        private set
    var Ink = Color(0xFF172019)
        private set
    var InkSoft = Color(0xFF3C4842)
        private set
    var Muted = Color(0xFF6E7B73)
        private set
    var Line = Color(0xFFE7EAE7)
        private set
    var WarningBg = Color(0xFFFBEFC9)
        private set
    var WarningText = Color(0xFF8A6D17)
        private set
    var ErrorBg = Color(0xFFFBEAE7)
        private set
    var Error = Color(0xFFC0392B)
        private set
    var Freezer = Color(0xFF2F76C9)
        private set
    var Pantry = Color(0xFFC8642B)
        private set

    fun applyTheme(darkTheme: Boolean) {
        if (darkTheme) {
            Background = Color(0xFF101711)
            Card = Color(0xFF182119)
            Green50 = Color(0xFF203327)
            Green100 = Color(0xFF284634)
            Green600 = Color(0xFF61B883)
            Green700 = Color(0xFF75C996)
            Green900 = Color(0xFF0F2A1D)
            Ink = Color(0xFFEAF1EA)
            InkSoft = Color(0xFFC6D3CA)
            Muted = Color(0xFF98A79E)
            Line = Color(0xFF2C372F)
            WarningBg = Color(0xFF3E351B)
            WarningText = Color(0xFFE7C96B)
            ErrorBg = Color(0xFF3D2421)
            Error = Color(0xFFE07062)
            Freezer = Color(0xFF84B8F5)
            Pantry = Color(0xFFE29562)
        } else {
            Background = Color(0xFFF3F5F2)
            Card = Color(0xFFFFFFFF)
            Green50 = Color(0xFFEAF3ED)
            Green100 = Color(0xFFDBEBE0)
            Green600 = Color(0xFF207049)
            Green700 = Color(0xFF1B5E40)
            Green900 = Color(0xFF13402B)
            Ink = Color(0xFF172019)
            InkSoft = Color(0xFF3C4842)
            Muted = Color(0xFF6E7B73)
            Line = Color(0xFFE7EAE7)
            WarningBg = Color(0xFFFBEFC9)
            WarningText = Color(0xFF8A6D17)
            ErrorBg = Color(0xFFFBEAE7)
            Error = Color(0xFFC0392B)
            Freezer = Color(0xFF2F76C9)
            Pantry = Color(0xFFC8642B)
        }
    }
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
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
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

private val DarkColors = darkColorScheme(
    primary = Color(0xFF75C996),
    onPrimary = Color(0xFF092115),
    primaryContainer = Color(0xFF203327),
    onPrimaryContainer = Color(0xFFEAF1EA),
    secondary = Color(0xFFE29562),
    onSecondary = Color(0xFF2A1308),
    background = Color(0xFF101711),
    onBackground = Color(0xFFEAF1EA),
    surface = Color(0xFF182119),
    onSurface = Color(0xFFEAF1EA),
    error = Color(0xFFE07062)
)

fun resolveDarkTheme(appTheme: AppTheme, systemDark: Boolean): Boolean =
    when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> systemDark
    }

@Composable
fun PantryPalTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    darkTheme: Boolean = resolveDarkTheme(appTheme, isSystemInDarkTheme()),
    content: @Composable () -> Unit
) {
    PantryColors.applyTheme(darkTheme)
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
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
