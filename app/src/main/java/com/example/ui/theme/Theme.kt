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
    primary = VintedTeal,
    onPrimary = Color.White,
    primaryContainer = SoftTealContainer,
    onPrimaryContainer = OnSoftTeal,
    secondary = TerracottaGold,
    onSecondary = Color.White,
    secondaryContainer = WarmTerracottaContainer,
    onSecondaryContainer = OnWarmTerracotta,
    background = IvoryCanvas,
    onBackground = StudioCharcoal,
    surface = PureCardSurface,
    onSurface = StudioCharcoal,
    surfaceVariant = WarmLinen,
    onSurfaceVariant = MutedSlate,
    outline = CrispHairline,
    outlineVariant = CrispHairline,
    error = StudioErrorLight,
    errorContainer = StudioErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = LuminousTeal,
    onPrimary = OnSoftTeal,
    primaryContainer = DeepTealContainer,
    onPrimaryContainer = SoftTealContainer,
    secondary = WarmGold,
    onSecondary = OnWarmTerracotta,
    secondaryContainer = DeepTerracottaContainer,
    onSecondaryContainer = WarmTerracottaContainer,
    background = DeepObsidian,
    onBackground = WarmAlabaster,
    surface = CharcoalSurface,
    onSurface = WarmAlabaster,
    surfaceVariant = ElevatedCharcoal,
    onSurfaceVariant = SoftStone,
    outline = DarkHairline,
    outlineVariant = DarkHairline,
    error = StudioErrorDark,
    errorContainer = StudioErrorContainerDark
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VintedStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
