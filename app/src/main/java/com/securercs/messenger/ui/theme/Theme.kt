package com.securercs.messenger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    secondary = GreenSecondary,
    onSecondary = Color.White,
    tertiary = AccentBlue,
    background = Color.White,
    surface = GraySurface,
)

@Composable
fun SecureRCSMessengerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
