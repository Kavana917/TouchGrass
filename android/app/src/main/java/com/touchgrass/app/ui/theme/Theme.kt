package com.touchgrass.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The app's theme. Provides colours, typography and the night flag to every
 * composable beneath it.
 *
 * KEY PROPERTY (design_theme.md §4.2, decision 4): night mode changes the
 * WALLPAPER ONLY. Windows, buttons, bevels and text never recolour. That's
 * what makes dark mode both authentic — a 1997 desktop didn't have a dark
 * mode, it had a different wallpaper — and cheap to maintain, since there is
 * exactly one set of chrome tokens to reason about.
 */

val LocalRetroColors: ProvidableCompositionLocal<RetroColors> =
    staticCompositionLocalOf { DayColors }

val LocalRetroTypography: ProvidableCompositionLocal<RetroTypography> =
    staticCompositionLocalOf { RetroTypography() }

val LocalIsNight: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { false }

/**
 * Convenience accessors so components can write `RetroTheme.colors.surface`
 * instead of `LocalRetroColors.current.surface`.
 */
object RetroTheme {
    val colors: RetroColors
        @Composable @ReadOnlyComposable get() = LocalRetroColors.current

    val typography: RetroTypography
        @Composable @ReadOnlyComposable get() = LocalRetroTypography.current

    val isNight: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsNight.current
}

@Composable
fun TouchGrassTheme(
    isNight: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (isNight) NightColors else DayColors
    val typography = RetroTypography()

    CompositionLocalProvider(
        LocalRetroColors provides colors,
        LocalRetroTypography provides typography,
        LocalIsNight provides isNight
    ) {
        // MaterialTheme is still here because some Material 3 components
        // (Text, Surface, ripple defaults) read from it. Our own components
        // ignore it and use LocalRetroColors instead.
        MaterialTheme(
            typography = Typography(bodyLarge = typography.body),
            content = content
        )
    }
}
