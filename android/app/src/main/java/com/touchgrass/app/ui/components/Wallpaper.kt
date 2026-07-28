package com.touchgrass.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.touchgrass.app.ui.theme.RetroTheme
import kotlin.math.roundToInt

/**
 * The desktop backdrop — the XP "Bliss" wallpaper redrawn in pixels
 * (`2.webp`): banded sky, blocky clouds, two green hills, hard horizon.
 *
 * TWO RULES FROM §7, both easy to violate by accident:
 *
 *  1. The sky is BANDED, not a gradient. Any smooth ramp becomes 3–5 hard
 *     steps. A real gradient here would be the single most obvious break in
 *     the whole aesthetic.
 *  2. Every edge is a stair-step. No curves, no anti-aliasing.
 *
 * Day/night switches these colours and nothing else — windows and chrome
 * never recolour (§4.2, decision 4).
 */
@Composable
fun Wallpaper(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val colors = RetroTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Quantise to a chunky grid so everything lands on whole "pixels".
            val cell = (w / 64f).coerceAtLeast(4f)
            fun snap(v: Float) = (v / cell).roundToInt() * cell

            // ---- Sky: 5 hard bands, no gradient ----
            val bands = 5
            val horizon = snap(h * 0.62f)
            val bandHeight = horizon / bands
            repeat(bands) { i ->
                val t = i / (bands - 1f)
                val color = androidx.compose.ui.graphics.lerp(
                    colors.skyTop,
                    colors.skyBottom,
                    t
                )
                drawRect(
                    color = color,
                    topLeft = Offset(0f, i * bandHeight),
                    size = Size(w, bandHeight + 1f)
                )
            }

            // ---- Clouds: blocky, 4px minimum feature size ----
            fun cloud(cx: Float, cy: Float, scale: Float) {
                val u = cell * scale
                // A cloud is just a few overlapping rectangles.
                drawRect(colors.cloud, Offset(snap(cx), snap(cy)), Size(u * 6, u * 2))
                drawRect(colors.cloud, Offset(snap(cx + u), snap(cy - u)), Size(u * 4, u * 2))
                drawRect(colors.cloud, Offset(snap(cx + u * 2), snap(cy - u * 2)), Size(u * 2, u * 2))
            }
            cloud(w * 0.12f, h * 0.16f, 1.2f)
            cloud(w * 0.58f, h * 0.10f, 0.9f)
            cloud(w * 0.74f, h * 0.30f, 1.0f)

            // ---- Hills: stair-stepped, never curved ----
            // Far hill
            val farSteps = 8
            val farStepW = w / farSteps
            repeat(farSteps) { i ->
                val t = i / (farSteps - 1f)
                // A shallow arch, quantised into steps.
                val rise = (1f - (t - 0.5f) * (t - 0.5f) * 4f) * (h * 0.10f)
                val top = snap(horizon - rise)
                drawRect(
                    color = colors.hillFar,
                    topLeft = Offset(i * farStepW, top),
                    size = Size(farStepW + 1f, h - top)
                )
            }

            // Near hill
            val nearSteps = 6
            val nearStepW = w / nearSteps
            val nearBase = snap(horizon + h * 0.06f)
            repeat(nearSteps) { i ->
                val t = i / (nearSteps - 1f)
                val rise = (1f - (t - 0.35f) * (t - 0.35f) * 3f) * (h * 0.08f)
                val top = snap(nearBase - rise)
                drawRect(
                    color = colors.hillNear,
                    topLeft = Offset(i * nearStepW, top),
                    size = Size(nearStepW + 1f, h - top)
                )
            }
        }

        content()
    }
}
