package com.strefagentelmena.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.strefagentelmena.R

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val myCustomFont = FontFamily(
    Font(R.font.proxima_nova_regular),
)


val TypographyCustom = Typography(
    displayLarge = TextStyle(
        fontFamily = myCustomFont,
        fontWeight = FontWeight.W700, // Grubszy font dla dużych nagłówków
        fontSize = 32.sp // Przykładowy rozmiar czcionki
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
        fontWeight = FontWeight.W600, // Średni font dla nagłówków
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
        fontWeight = FontWeight.W500, // Normalny font dla tytułów
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
        fontWeight = FontWeight.Normal, // Domyślna waga dla tekstu
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
        fontWeight = FontWeight.W400, // Lekki font dla etykiet
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


@Composable
fun StrefaGentelmenaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TypographyCustom,
        content = content
    )
}
