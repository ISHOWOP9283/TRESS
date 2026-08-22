package com.example.treemap.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MangroveTealLight,
    onPrimary = Color.White,
    primaryContainer = MangroveDeepTeal,
    onPrimaryContainer = MangroveTealSurface,
    secondary = StatusFair,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3E3015),
    onSecondaryContainer = StatusFairLight,
    tertiary = StatusAtRisk,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = MangroveTealPrimary,
    onPrimary = Color.White,
    primaryContainer = MangroveDeepTeal,
    onPrimaryContainer = Color.White,
    secondary = StatusFair,
    onSecondary = Color.White,
    secondaryContainer = StatusFairLight,
    onSecondaryContainer = Color(0xFF4A3408),
    tertiary = StatusAtRisk,
    onTertiary = Color.White,
    background = AppBackground,
    onBackground = TextPrimaryDark,
    surface = SurfaceCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = MangroveTealSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderLight
)

@Composable
fun TreeMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

