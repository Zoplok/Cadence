package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PeriwinklePrimaryDark,
    onPrimary = OnPeriwinkleDark,
    primaryContainer = PeriwinkleContainerDark,
    onPrimaryContainer = OnPeriwinkleContainerDark,
    secondary = CoralSecondaryDark,
    onSecondary = OnCoralDark,
    secondaryContainer = CoralContainerDark,
    onSecondaryContainer = OnCoralContainerDark,
    tertiary = MintTertiaryDark,
    onTertiary = OnMintDark,
    tertiaryContainer = MintContainerDark,
    onTertiaryContainer = OnMintContainerDark,
    background = ObsidianBackgroundDark,
    onBackground = IvoryOnSurfaceDark,
    surface = MidnightSurfaceDark,
    onSurface = IvoryOnSurfaceDark,
    surfaceVariant = CharcoalSurfaceVariantDark,
    onSurfaceVariant = MutedSlateOnVariantDark,
    outline = SlateOutlineDark,
    outlineVariant = SlateOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimaryLight,
    onPrimary = OnCobaltLight,
    primaryContainer = CobaltContainerLight,
    onPrimaryContainer = OnCobaltContainerLight,
    secondary = TerracottaSecondaryLight,
    onSecondary = OnTerracottaLight,
    secondaryContainer = TerracottaContainerLight,
    onSecondaryContainer = OnTerracottaContainerLight,
    tertiary = SageTertiaryLight,
    onTertiary = OnSageLight,
    tertiaryContainer = SageContainerLight,
    onTertiaryContainer = OnSageContainerLight,
    background = PaperBackgroundLight,
    onBackground = InkOnSurfaceLight,
    surface = CrispSurfaceLight,
    onSurface = InkOnSurfaceLight,
    surfaceVariant = StoneSurfaceVariantLight,
    onSurfaceVariant = SlateOnSurfaceVariantLight,
    outline = WarmOutlineLight,
    outlineVariant = WarmOutlineVariantLight
)

val CadenceShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CadenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CadenceShapes,
        content = content
    )
}
