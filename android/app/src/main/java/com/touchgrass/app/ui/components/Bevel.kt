package com.touchgrass.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * THE ONE PRIMITIVE (design_theme.md §6.1).
 *
 * Every raised or sunken surface in the app is this recipe. Build it once,
 * use it everywhere — a window frame, a button, a text field and a scrollbar
 * thumb are all the same four lines drawn in a different order.
 *
 * No border radius. No shadows except the bevel itself. No blur, no glow,
 * no translucency. Hard lines only.
 */
enum class BevelStyle {
    /** Buttons, window frames, menu bars — the default "pressable" look. */
    RAISED,

    /** A button being held. Both bevel pairs invert; the label shifts 1px down-right. */
    PRESSED,

    /** Text fields, list wells, canvases — content sits *in* the surface. */
    SUNKEN,

    /**
     * Disabled. Flat — no bevel at all.
     *
     * This exists because #808080 text on #C0C0C0 fails contrast (§9), so
     * greying the label can never be the only disabled signal. Removing the
     * bevel is the signal that actually carries.
     */
    FLAT
}

/**
 * Draws the double-bevel behind this composable and fills its face.
 *
 * @param style which bevel treatment to draw
 * @param face overrides the face colour (e.g. [RetroColors.paperCream] for a
 *   notice dialog). Defaults to the style's natural face — gray for raised,
 *   white for sunken.
 */
fun Modifier.bevel(
    style: BevelStyle = BevelStyle.RAISED,
    face: Color? = null
): Modifier = composed {
    val colors = RetroTheme.colors
    val line = Dimens.BevelLine

    // Outer and inner bevel pairs. RAISED reads as lit from the top-left;
    // SUNKEN and PRESSED invert that so the surface reads as pushed in.
    val outerTopLeft: Color
    val outerBottomRight: Color
    val innerTopLeft: Color
    val innerBottomRight: Color

    when (style) {
        BevelStyle.RAISED -> {
            outerTopLeft = colors.surfaceWhite
            outerBottomRight = colors.surfaceBlack
            innerTopLeft = colors.surfaceLight
            innerBottomRight = colors.surfaceShadow
        }
        BevelStyle.PRESSED, BevelStyle.SUNKEN -> {
            outerTopLeft = colors.surfaceBlack
            outerBottomRight = colors.surfaceWhite
            innerTopLeft = colors.surfaceShadow
            innerBottomRight = colors.surfaceLight
        }
        BevelStyle.FLAT -> {
            outerTopLeft = Color.Transparent
            outerBottomRight = Color.Transparent
            innerTopLeft = Color.Transparent
            innerBottomRight = Color.Transparent
        }
    }

    val faceColor = face ?: when (style) {
        BevelStyle.SUNKEN -> colors.surfaceWhite
        else -> colors.surface
    }

    drawBehind {
        val w = size.width
        val h = size.height
        val px = line.toPx()

        // Face first, then bevel lines on top of its edges.
        drawRect(color = faceColor, topLeft = Offset.Zero, size = Size(w, h))

        if (style == BevelStyle.FLAT) return@drawBehind

        // --- Outer ring ---
        drawRect(outerTopLeft, Offset(0f, 0f), Size(w, px))            // top
        drawRect(outerTopLeft, Offset(0f, 0f), Size(px, h))            // left
        drawRect(outerBottomRight, Offset(0f, h - px), Size(w, px))    // bottom
        drawRect(outerBottomRight, Offset(w - px, 0f), Size(px, h))    // right

        // --- Inner ring, inset by one line ---
        drawRect(innerTopLeft, Offset(px, px), Size(w - px * 2, px))
        drawRect(innerTopLeft, Offset(px, px), Size(px, h - px * 2))
        drawRect(innerBottomRight, Offset(px, h - px * 2), Size(w - px * 2, px))
        drawRect(innerBottomRight, Offset(w - px * 2, px), Size(px, h - px * 2))
    }
}

/**
 * Padding that keeps content clear of the bevel lines.
 * Use on anything placed inside a bevelled surface.
 */
fun Modifier.bevelContentPadding(extra: Dp = 0.dp): Modifier =
    this.padding(all = Dimens.BevelTotal + extra)
