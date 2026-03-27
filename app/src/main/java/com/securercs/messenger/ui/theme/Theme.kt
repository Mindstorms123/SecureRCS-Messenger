package com.securercs.messenger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors: ColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = Color(0xFF041014),
    secondary = AquaSecondary,
    onSecondary = Color(0xFF041014),
    tertiary = AccentBlue,
    background = NightBackground,
    onBackground = Color(0xFFE5E7EB),
    surface = NightSurface,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF334155),
)

@Composable
fun SecureRCSMessengerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
