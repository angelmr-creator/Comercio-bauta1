package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CommerceColors(
    val collected: Color,
    val collectedContainer: Color,
    val pending: Color,
    val pendingContainer: Color,
    val partial: Color,
    val partialContainer: Color
)

val LocalCommerceColors = staticCompositionLocalOf {
    CommerceColors(
        collected = IncomeGreenLight,
        collectedContainer = IncomeContainerLight,
        pending = PendingRedLight,
        pendingContainer = PendingContainerLight,
        partial = PartialAmberLight,
        partialContainer = PartialContainerLight
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = CobaltTertiary,
    onTertiary = Color.White,
    tertiaryContainer = CobaltContainer,
    onTertiaryContainer = OnCobaltContainer,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = OnCrimsonContainer,
    background = PaperBackground,
    onBackground = PaperOnBackground,
    surface = PaperSurface,
    onSurface = PaperOnBackground,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = PaperOnSurfaceVariant,
    outline = PaperOutline,
    outlineVariant = PaperOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = OnEmeraldDarkPrimary,
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = EmeraldContainer,
    secondary = GoldDarkSecondary,
    onSecondary = OnGoldContainer,
    secondaryContainer = GoldDarkContainer,
    onSecondaryContainer = GoldContainer,
    tertiary = CobaltDarkTertiary,
    onTertiary = OnCobaltDarkTertiary,
    tertiaryContainer = CobaltDarkContainer,
    onTertiaryContainer = CobaltContainer,
    error = CrimsonDarkError,
    onError = OnCrimsonDarkError,
    errorContainer = CrimsonDarkContainer,
    onErrorContainer = CrimsonContainer,
    background = SlateDarkBackground,
    onBackground = SlateDarkOnBackground,
    surface = SlateDarkSurface,
    onSurface = SlateDarkOnBackground,
    surfaceVariant = SlateDarkSurfaceVariant,
    onSurfaceVariant = SlateDarkOnSurfaceVariant,
    outline = SlateDarkOutline,
    outlineVariant = SlateDarkOutlineVariant
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ComercioBautaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val commerceColors = if (darkTheme) {
        CommerceColors(
            collected = IncomeGreenDark,
            collectedContainer = IncomeContainerDark,
            pending = PendingRedDark,
            pendingContainer = PendingContainerDark,
            partial = PartialAmberDark,
            partialContainer = PartialContainerDark
        )
    } else {
        CommerceColors(
            collected = IncomeGreenLight,
            collectedContainer = IncomeContainerLight,
            pending = PendingRedLight,
            pendingContainer = PendingContainerLight,
            partial = PartialAmberLight,
            partialContainer = PartialContainerLight
        )
    }

    CompositionLocalProvider(LocalCommerceColors provides commerceColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
