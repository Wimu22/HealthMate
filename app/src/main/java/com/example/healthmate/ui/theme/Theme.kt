package com.example.healthmate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Maps the spec's palette onto Material 3 roles, so standard components
 * (buttons, chips, text fields, dialogs) pick up the right colours automatically.
 */
private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlue,

    secondary = SecondaryYellow,
    onSecondary = TextPrimary,
    secondaryContainer = SecondaryYellowLight,
    onSecondaryContainer = TextPrimary,

    background = White,
    onBackground = TextPrimary,
    surface = White,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGrey,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = SurfaceGrey,
    surfaceContainerHigh = SurfaceGrey,
    surfaceContainerHighest = SurfaceGrey,
    surfaceTint = Color.Transparent, // no blue tint on raised surfaces: keeps cards clean white

    outline = BorderGrey,
    outlineVariant = BorderGrey,

    error = ErrorRed,
    onError = White,
    errorContainer = ErrorRedLight,
    onErrorContainer = ErrorRed
)

/**
 * Colours Material 3 has no role for: the three stat colours and success green.
 * Read them in composables with `HealthMateTheme.extraColors.steps` etc.
 */
@Immutable
data class ExtraColors(
    val steps: Color = StepsGreen,
    val calories: Color = CaloriesRed,
    val water: Color = WaterBlue,
    val success: Color = SuccessGreen
)

private val LocalExtraColors = staticCompositionLocalOf { ExtraColors() }

/**
 * The app theme. Wrap every screen in this. Light only, and dynamic (wallpaper) colours are
 * deliberately not used, so the app always shows the HealthMate brand colours.
 */
@Composable
fun HealthMateTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtraColors provides ExtraColors()) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = HealthMateTypography,
            shapes = HealthMateShapes,
            content = content
        )
    }
}

object HealthMateTheme {
    val extraColors: ExtraColors
        @Composable get() = LocalExtraColors.current
}
