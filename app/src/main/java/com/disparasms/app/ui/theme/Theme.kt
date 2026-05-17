package com.disparasms.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue800,
    secondary = Gray700,
    onSecondary = Color.White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray800,
    tertiary = Blue400,
    onTertiary = Color.White,
    tertiaryContainer = Blue50,
    onTertiaryContainer = Blue700,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    scrim = LightScrim,
    error = Red500,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Red600
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Color.White,
    primaryContainer = Blue800,
    onPrimaryContainer = Blue100,
    secondary = Gray300,
    onSecondary = Gray900,
    secondaryContainer = Gray800,
    onSecondaryContainer = Gray100,
    tertiary = Blue300,
    onTertiary = Gray900,
    tertiaryContainer = Blue900,
    onTertiaryContainer = Blue100,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    scrim = DarkScrim,
    error = Red500,
    onError = Color.White,
    errorContainer = Color(0xFF3A0D0D),
    onErrorContainer = Red100
)

@Composable
fun DisparaSMSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
