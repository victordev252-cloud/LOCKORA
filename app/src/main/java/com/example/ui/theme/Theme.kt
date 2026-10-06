package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = TextPrimary,
    primaryContainer = CyberDarkCard,
    onPrimaryContainer = NeonVioletGlow,
    secondary = NeonCyan,
    onSecondary = TextPrimary,
    tertiary = NeonEmerald,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberDarkCard,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    error = NeonCoral,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = NeonViolet,
    onPrimary = TextPrimary,
    primaryContainer = CyberDarkCard,
    secondary = NeonCyan,
    background = CyberBlack, // Lockora is optimized with a dark, privacy-first cybersecurity aesthetic
    surface = CyberCardSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun LockoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // default dark security aesthetic
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = CyberBlack.toArgb()
            window.navigationBarColor = CyberBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
