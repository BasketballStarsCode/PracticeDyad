package com.practicedyad.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = TealPrimary,
    onPrimary        = Color.Black,
    primaryContainer = TealDark,
    secondary        = TealLight,
    onSecondary      = Color.Black,
    background       = DarkBackground,
    onBackground     = DarkOnSurface,
    surface          = DarkSurface,
    onSurface        = DarkOnSurface,
    surfaceVariant   = DarkSurface2,
    onSurfaceVariant = DarkOnSurface2,
    outline          = DarkBorder,
    error            = ErrorRed,
    onError          = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary          = TealDark,
    onPrimary        = Color.White,
    primaryContainer = TealUltraLight,
    secondary        = TealPrimary,
    onSecondary      = Color.White,
    background       = LightBackground,
    onBackground     = LightOnSurface,
    surface          = LightSurface,
    onSurface        = LightOnSurface,
    surfaceVariant   = LightSurface2,
    onSurfaceVariant = LightOnSurface2,
    outline          = LightBorder,
    error            = ErrorRed,
    onError          = Color.White
)

@Composable
fun PracticeDyadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = PracticeDyadTypography,
        content     = content
    )
}
