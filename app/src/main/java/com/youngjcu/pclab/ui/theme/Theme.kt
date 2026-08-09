package com.youngjcu.pclab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF005AC1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFF40637D),
    tertiary = Color(0xFF765573),
    error = Color(0xFFB3261E)
)

private val ColourBlindLightScheme = lightColorScheme(
    primary = Color(0xFF0072B2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F0FF),
    secondary = Color(0xFFE69F00),
    tertiary = Color(0xFF009E73),
    error = Color(0xFFD55E00)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFADC6FF),
    secondary = Color(0xFFA9CCE8),
    tertiary = Color(0xFFE6B8E0)
)

private val HighContrastLightScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color.Black,
    secondary = Color.Black,
    onSecondary = Color.White,
    tertiary = Color.Black,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = Color(0xFF9B0000),
    onError = Color.White,
    outline = Color.Black
)

private val HighContrastDarkScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF303030),
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    tertiary = Color.White,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    error = Color(0xFFFFB4AB),
    onError = Color.Black,
    outline = Color.White
)

@Composable
fun PcLabTheme(darkTheme: Boolean, colourBlindMode: Boolean, highContrastMode: Boolean, content: @Composable () -> Unit) {
    val colours = when {
        highContrastMode && darkTheme -> HighContrastDarkScheme
        highContrastMode -> HighContrastLightScheme
        darkTheme -> DarkScheme
        colourBlindMode -> ColourBlindLightScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = colours, content = content)
}
