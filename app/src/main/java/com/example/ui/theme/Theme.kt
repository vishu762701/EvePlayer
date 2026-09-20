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
import com.example.data.preferences.ThemeMode

private val AmoledColorScheme = darkColorScheme(
    primary = EveRedPrimary,
    onPrimary = Color.White,
    primaryContainer = EveRedContainerDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = EveRedLight,
    onSecondary = Color.White,
    background = AmoledBackground,
    onBackground = Color(0xFFF5F5F7),
    surface = AmoledSurface,
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = EveRedPrimary,
    onPrimary = Color.White,
    primaryContainer = EveRedContainerDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = EveRedLight,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = EveRedPrimary,
    onPrimary = Color.White,
    primaryContainer = EveRedContainerLight,
    onPrimaryContainer = EveRedContainerDark,
    secondary = EveRedVariant,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant
)

@Composable
fun EveTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
    }

    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.SYSTEM -> if (systemDark) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
