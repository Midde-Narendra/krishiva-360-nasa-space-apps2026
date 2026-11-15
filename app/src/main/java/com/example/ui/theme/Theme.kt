package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AgriGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = AgriGreenContainer,
    onPrimaryContainer = AgriOnGreenContainer,
    secondary = AgriGreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = AgriGreenContainer,
    onSecondaryContainer = AgriOnGreenContainer,
    tertiary = AgriEarthAmber,
    onTertiary = Color.White,
    tertiaryContainer = AgriAmberContainer,
    onTertiaryContainer = AgriOnAmberContainer,
    background = AgriBackground,
    onBackground = AgriTextPrimary,
    surface = AgriSurface,
    onSurface = AgriTextPrimary,
    surfaceVariant = AgriSurfaceVariant,
    onSurfaceVariant = AgriTextSecondary,
    outline = AgriCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = AgriGreenLight,
    onPrimary = Color(0xFF052E16),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF052E16),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = Color(0xFF0B140E),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF132018),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1F2F25),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF2E4535)
)

@Composable
fun KrishivaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    KrishivaTheme(darkTheme = darkTheme, content = content)
}
