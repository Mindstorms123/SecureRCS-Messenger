package com.securercs.messenger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors: ColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color(0xFF000000),
    secondary = PrimaryGreenDark,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = PrimaryGreen,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2A3940),
    onSurfaceVariant = Color(0xFFB0B7BE),
    outline = Color(0xFF697C86),
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
