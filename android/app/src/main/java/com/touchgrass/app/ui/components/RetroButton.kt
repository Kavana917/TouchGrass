package com.touchgrass.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Form 3 (design_theme.md §6.2) — the chunky 3D button.
 *
 * Two accessibility rules are enforced here rather than left to callers:
 *
 *  1. The hit area is always ≥48dp even when the visual button is shorter.
 *     "Pad the hit area, not the pixels" (§9).
 *  2. Disabled FLATTENS the bevel. Grey text alone fails contrast on #C0C0C0,
 *     so the bevel change is what actually communicates the state.
 *
 * Pressing inverts the bevel and shifts the label 1px down-right, exactly as
 * a real Win95 button did (§6.1).
 */
@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    face: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val style = when {
        !enabled -> BevelStyle.FLAT
        isPressed -> BevelStyle.PRESSED
        else -> BevelStyle.RAISED
    }

    // The offset that sells the press. 2dp because that's one bevel line.
    val shift = if (isPressed && enabled) Dimens.BevelLine else 0.dp

    Box(
        modifier = modifier
            // Hit area first — this is the ≥48dp guarantee.
            .sizeIn(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // no ripple — motion is stepped, not eased (§7)
                enabled = enabled,
                onClick = onClick
            )
            .padding(2.dp) // visual inset so adjacent buttons don't fuse
            .bevel(style, face)
            .defaultMinSize(minWidth = 88.dp),
        contentAlignment = Alignment.Center
    ) {
        PixelText(
            text = if (primary) text.uppercase() else text,
            style = RetroTheme.typography.chrome,
            color = if (enabled) RetroTheme.colors.bodyText else RetroTheme.colors.disabledText,
            modifier = Modifier
                .offset(x = shift, y = shift)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}

/**
 * The small square buttons in a title bar (`_ □ ✕`) and scrollbar arrows.
 * Visually 20dp, but still carries a 48dp hit area.
 */
@Composable
fun RetroIconButton(
    glyph: String,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val style = when {
        !enabled -> BevelStyle.FLAT
        isPressed -> BevelStyle.PRESSED
        else -> BevelStyle.RAISED
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 22.dp, minHeight = 22.dp)
                .bevel(style),
            contentAlignment = Alignment.Center
        ) {
            PixelText(
                text = glyph,
                color = if (enabled) RetroTheme.colors.bodyText else RetroTheme.colors.disabledText,
                maxLines = 1
            )
        }
    }
}
