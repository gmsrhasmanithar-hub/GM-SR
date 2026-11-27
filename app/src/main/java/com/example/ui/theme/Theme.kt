package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = SlateTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = SlateTealContainer,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = SurfaceIvory,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantSoft,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSoft,
    error = StatusUnpaidRed,
    onError = Color.White,
    errorContainer = StatusUnpaidBg,
    onErrorContainer = StatusUnpaidRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = OnEmeraldContainer,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldContainer,
    secondary = SlateTealContainer,
    onSecondary = OnEmeraldContainer,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = EmeraldContainer,
    tertiary = DarkGoldAccent,
    onTertiary = OnGoldContainer,
    tertiaryContainer = OnGoldContainer,
    onTertiaryContainer = GoldContainer,
    background = DarkSurface,
    onBackground = Color(0xFFE6F0EC),
    surface = DarkSurfaceCard,
    onSurface = Color(0xFFE6F0EC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7BDB3),
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CommitteeManagerTheme(
    isUrdu: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildAppTypography(isUrdu = isUrdu),
        shapes = AppShapes,
        content = content
    )
}
