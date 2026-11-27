package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ArunDarkColorScheme = darkColorScheme(
    primary = ArunCyan,
    onPrimary = Color(0xFF041421),
    primaryContainer = Color(0xFF0F2D46),
    onPrimaryContainer = TextPrimary,
    secondary = ArunIndigo,
    onSecondary = Color(0xFF0B0D26),
    secondaryContainer = Color(0xFF1B204A),
    onSecondaryContainer = TextPrimary,
    tertiary = ArunEmerald,
    onTertiary = Color(0xFF041F17),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderSubtle,
    outlineVariant = GlassBorderSpecular,
    error = ArunRose,
    onError = Color(0xFF2B060D)
)

val ArunShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun ArunMusicTheme(
    accentColor: Color = ArunCyan,
    content: @Composable () -> Unit
) {
    val dynamicScheme = ArunDarkColorScheme.copy(
        primary = accentColor
    )
    MaterialTheme(
        colorScheme = dynamicScheme,
        typography = ArunTypography,
        shapes = ArunShapes,
        content = content
    )
}
