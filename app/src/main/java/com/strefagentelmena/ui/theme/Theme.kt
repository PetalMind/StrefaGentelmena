package com.strefagentelmena.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.strefagentelmena.R

private val myCustomFont = FontFamily(
    Font(R.font.proxima_nova_regular),
)

val TypographyCustom = Typography(
    displayLarge = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W700,
        fontSize = 32.sp
    ),
    displayMedium = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp
    ),
    displaySmall = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W700,
        fontSize = 20.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp
    ),
    titleLarge = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W500,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodySmall = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp
    ),
)

private val SalonColorScheme = darkColorScheme(
    primary = SalonGold,
    onPrimary = SalonBg,
    primaryContainer = SalonGoldDim,
    onPrimaryContainer = SalonGold,
    inversePrimary = SalonText,
    secondary = SalonMuted2,
    onSecondary = SalonBg,
    secondaryContainer = SalonBg3,
    onSecondaryContainer = SalonText,
    tertiary = SalonMuted,
    onTertiary = SalonText,
    tertiaryContainer = SalonBg2,
    onTertiaryContainer = SalonMuted2,
    background = SalonBg,
    onBackground = SalonText,
    surface = SalonBg2,
    onSurface = SalonText,
    surfaceVariant = SalonBg3,
    onSurfaceVariant = SalonMuted2,
    surfaceTint = Color.Unspecified,
    inverseSurface = SalonText,
    inverseOnSurface = SalonBg,
    error = Color(0xFFE57373),
    onError = SalonBg,
    errorContainer = Color(0xFF5C2B2B),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = SalonBorder,
    outlineVariant = SalonBorderElevated,
    scrim = Color(0xFF000000),
)

private val SalonLightColorScheme = lightColorScheme(
    primary = SalonGold,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFF4D6),
    onPrimaryContainer = Color(0xFF4A3A10),
    inversePrimary = SalonGold,
    secondary = Color(0xFF6A6252),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE1C0),
    onSecondaryContainer = Color(0xFF252017),
    tertiary = Color(0xFF71624A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF7E6C4),
    onTertiaryContainer = Color(0xFF251A05),
    background = Color(0xFFFFFBF2),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFF8E9),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF2E8D6),
    onSurfaceVariant = Color(0xFF514737),
    surfaceTint = Color.Unspecified,
    inverseSurface = Color(0xFF2A2A2A),
    inverseOnSurface = Color(0xFFF8F1E0),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF8A816E),
    outlineVariant = Color(0xFFD6C9AF),
    scrim = Color(0xFF000000),
)

@Composable
fun StrefaGentelmenaTheme(
    darkTheme: Boolean = true,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SalonColorScheme else SalonLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TypographyCustom,
        content = content
    )
}
