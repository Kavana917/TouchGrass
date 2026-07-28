package com.touchgrass.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens from design_theme.md §4.
 *
 * Two families, and the distinction is load-bearing:
 *
 *  - CHROME (§4.1) — the window system. These NEVER change. Not for dark mode,
 *    not for anything. One set of grays, all day, all night.
 *  - DESKTOP (§4.2) — the wallpaper behind the windows. This is the only thing
 *    that shifts between day and night. That's what makes dark mode cheap.
 *
 * Don't invent new grays. If a surface needs to read differently, change its
 * BEVEL (raised / sunken / flat), not its colour — see §4.3.
 */

// ---- Chrome: the window system (§4.1) ----

/** Window face, buttons, menu bars, status bars, dialog bodies. */
val Surface = Color(0xFFC0C0C0)

/** Inner top/left bevel. */
val SurfaceLight = Color(0xFFDFDFDF)

/** Outer top/left bevel; sunken field backgrounds. */
val SurfaceWhite = Color(0xFFFFFFFF)

/** Inner bottom/right bevel; disabled glyphs. */
val SurfaceShadow = Color(0xFF808080)

/** Outer bottom/right bevel; 1px icon outlines. */
val SurfaceBlack = Color(0xFF0A0A0A)

/** Active title bar, menu highlight, selection. */
val TitleActive = Color(0xFF000080)

/** Inactive / background window title bar. */
val TitleInactive = Color(0xFF808080)

/** Text on navy. */
val TitleText = Color(0xFFFFFFFF)

/** Text on gray and on white fields. */
val BodyText = Color(0xFF0A0A0A)

/**
 * Disabled labels.
 *
 * WARNING: #808080 on #C0C0C0 fails contrast. This must NEVER be the only
 * disabled signal — always flatten the bevel too. See §9.
 */
val DisabledText = Color(0xFF808080)

// ---- Desktop: the wallpaper (§4.2) ----
// The only tokens that differ between day and night.

val SkyTopDay = Color(0xFF4A90D9)
val SkyBottomDay = Color(0xFF7FB2E5)
val CloudDay = Color(0xFFFFFFFF)
val HillFarDay = Color(0xFF5C9E3C)
val HillNearDay = Color(0xFF3E7B28)

val SkyTopNight = Color(0xFF141C3A)
val SkyBottomNight = Color(0xFF2B3566)
val CloudNight = Color(0xFF3D4675)
val HillFarNight = Color(0xFF1E3A22)
val HillNearNight = Color(0xFF152B18)

// ---- Accents: used sparingly (§4.3) ----

/** Balloon/notice dialogs, essay paper, drawing pages. */
val PaperCream = Color(0xFFFDF3D3)

/** The pixel heart; expiry states. NEVER for scolding. */
val AccentRed = Color(0xFFD32F2F)

/** Rare secondary highlight; classic desktop teal. */
val AccentTeal = Color(0xFF008080)

/** Selected drawing tool, active map pin. */
val HighlightYellow = Color(0xFFFFD54F)

/**
 * The full set of colours available to composables, resolved for day or night.
 * Only the wallpaper fields differ between the two.
 */
@Suppress("LongParameterList")
data class RetroColors(
    // Chrome — identical in both schemes
    val surface: Color = Surface,
    val surfaceLight: Color = SurfaceLight,
    val surfaceWhite: Color = SurfaceWhite,
    val surfaceShadow: Color = SurfaceShadow,
    val surfaceBlack: Color = SurfaceBlack,
    val titleActive: Color = TitleActive,
    val titleInactive: Color = TitleInactive,
    val titleText: Color = TitleText,
    val bodyText: Color = BodyText,
    val disabledText: Color = DisabledText,
    // Accents — identical in both schemes
    val paperCream: Color = PaperCream,
    val accentRed: Color = AccentRed,
    val accentTeal: Color = AccentTeal,
    val highlightYellow: Color = HighlightYellow,
    // Wallpaper — the only day/night difference
    val skyTop: Color,
    val skyBottom: Color,
    val cloud: Color,
    val hillFar: Color,
    val hillNear: Color
)

val DayColors = RetroColors(
    skyTop = SkyTopDay,
    skyBottom = SkyBottomDay,
    cloud = CloudDay,
    hillFar = HillFarDay,
    hillNear = HillNearDay
)

val NightColors = RetroColors(
    skyTop = SkyTopNight,
    skyBottom = SkyBottomNight,
    cloud = CloudNight,
    hillFar = HillFarNight,
    hillNear = HillNearNight
)
