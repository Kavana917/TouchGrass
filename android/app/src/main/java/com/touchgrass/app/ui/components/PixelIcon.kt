package com.touchgrass.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens

/**
 * Form 10 (§6.2) — pixel icons.
 *
 * We have no hand-drawn art yet, so icons are defined here as character maps:
 * one char per pixel, mapped to a colour. They're drawn as solid rectangles
 * on a Canvas, which means there is no bitmap and therefore no filtering —
 * edges are mathematically hard. That sidesteps the failure mode §7 warns
 * about ("bilinear filtering on pixel art is the single most common way this
 * theme dies") rather than merely configuring around it.
 *
 * When real 32×32 art arrives, swap the renderer for an ImageBitmap with
 * FilterQuality.None. The component's API doesn't change.
 *
 * Palette convention:
 *   '.' transparent   'X' outline (#0A0A0A)   'W' white   'S' shadow gray
 *   plus per-icon colours.
 */
data class PixelArt(
    val rows: List<String>,
    val palette: Map<Char, Color>
) {
    init {
        require(rows.isNotEmpty()) { "PixelArt needs at least one row" }
        val width = rows.first().length
        require(rows.all { it.length == width }) {
            "All PixelArt rows must be the same length; got ${rows.map { it.length }}"
        }
    }

    val height: Int get() = rows.size
    val width: Int get() = rows.first().length
}

@Composable
fun PixelIcon(
    art: PixelArt,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = Dimens.IconBase
) {
    Canvas(
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
    ) {
        // One "pixel" is the canvas divided by the art grid. Using floor and
        // drawing edge-to-edge avoids sub-pixel seams between cells.
        val cellW = this.size.width / art.width
        val cellH = this.size.height / art.height

        art.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = art.palette[ch] ?: Color.Transparent
                if (color != Color.Transparent) {
                    drawRect(
                        color = color,
                        topLeft = Offset(x * cellW, y * cellH),
                        // +1 covers rounding gaps between adjacent cells
                        size = Size(cellW + 1f, cellH + 1f)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Proof icons. Deliberately few — real art replaces these later.
// ---------------------------------------------------------------------------

private val Outline = Color(0xFF0A0A0A)

object PixelIcons {

    val Folder = PixelArt(
        rows = listOf(
            "................",
            "................",
            "..XXXX..........",
            ".XYYYYX.........",
            ".XYYYYXXXXXXXX..",
            ".XYYYYYYYYYYYX..",
            ".XYYYYYYYYYYYX..",
            ".XYYYYYYYYYYYX..",
            ".XYYYYYYYYYYYX..",
            ".XYYYYYYYYYYYX..",
            ".XYYYYYYYYYYYX..",
            ".XYYYYYYYYYYYX..",
            ".XXXXXXXXXXXXX..",
            "................",
            "................",
            "................"
        ),
        palette = mapOf(
            '.' to Color.Transparent,
            'X' to Outline,
            'Y' to Color(0xFFFFD54F)
        )
    )

    val Heart = PixelArt(
        rows = listOf(
            "................",
            "................",
            "..XXX....XXX....",
            ".XRRRX..XRRRX...",
            "XRRRRRXXRRRRRX..",
            "XRRRRRRRRRRRRX..",
            "XRRRRRRRRRRRRX..",
            ".XRRRRRRRRRRX...",
            "..XRRRRRRRRX....",
            "...XRRRRRRX.....",
            "....XRRRRX......",
            ".....XRRX.......",
            "......XX........",
            "................",
            "................",
            "................"
        ),
        palette = mapOf(
            '.' to Color.Transparent,
            'X' to Outline,
            'R' to Color(0xFFD32F2F)
        )
    )

    val Clock = PixelArt(
        rows = listOf(
            "................",
            "....XXXXXXXX....",
            "..XXWWWWWWWWXX..",
            ".XWWWWWWWWWWWWX.",
            ".XWWWWWKWWWWWWX.",
            "XWWWWWWKWWWWWWWX",
            "XWWWWWWKWWWWWWWX",
            "XWWWWWWKKKKWWWWX",
            "XWWWWWWWWWWWWWWX",
            "XWWWWWWWWWWWWWWX",
            ".XWWWWWWWWWWWWX.",
            ".XWWWWWWWWWWWWX.",
            "..XXWWWWWWWWXX..",
            "....XXXXXXXX....",
            "................",
            "................"
        ),
        palette = mapOf(
            '.' to Color.Transparent,
            'X' to Outline,
            'W' to Color(0xFFFFFFFF),
            'K' to Outline
        )
    )

    /** A blocky leaf — placeholder app icon. */
    val Grass = PixelArt(
        rows = listOf(
            "................",
            "................",
            ".......XX.......",
            "......XGGX......",
            "..X...XGGX...X..",
            ".XGX..XGGX..XGX.",
            ".XGX.XGGGGX.XGX.",
            ".XGGXXGGGGXXGGX.",
            ".XGGGGGGGGGGGGX.",
            "..XGGGGGGGGGGX..",
            "...XGGGGGGGGX...",
            "....XXXXXXXX....",
            "................",
            "................",
            "................",
            "................"
        ),
        palette = mapOf(
            '.' to Color.Transparent,
            'X' to Outline,
            'G' to Color(0xFF3E7B28)
        )
    )
}
