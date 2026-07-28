package com.touchgrass.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Supporting forms from §6.3: checkbox, radio, segmented progress, scrollbar.
 *
 * Every one of them keeps a ≥48dp hit area even though the visual control
 * is 24dp or less.
 */

/** Sunken square with a pixel ✓. */
@Composable
fun RetroCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.CheckboxSize)
                .bevel(if (enabled) BevelStyle.SUNKEN else BevelStyle.FLAT),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                PixelText(
                    text = "✓",
                    color = if (enabled) RetroTheme.colors.bodyText
                    else RetroTheme.colors.disabledText,
                    maxLines = 1
                )
            }
        }
        Box(Modifier.width(Dimens.ItemSpacing))
        PixelText(
            text = label,
            color = if (enabled) RetroTheme.colors.bodyText else RetroTheme.colors.disabledText
        )
    }
}

/** Sunken circle with a filled dot. */
@Composable
fun RetroRadio(
    selected: Boolean,
    onSelect: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(enabled = enabled, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.CheckboxSize)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .bevel(if (enabled) BevelStyle.SUNKEN else BevelStyle.FLAT),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (enabled) RetroTheme.colors.bodyText
                            else RetroTheme.colors.disabledText
                        )
                )
            }
        }
        Box(Modifier.width(Dimens.ItemSpacing))
        PixelText(
            text = label,
            color = if (enabled) RetroTheme.colors.bodyText else RetroTheme.colors.disabledText
        )
    }
}

/**
 * §6.3 — progress as discrete blocks, never a smooth bar.
 * Motion in this app is stepped, and a smoothly-filling bar would be the
 * most obvious violation of that.
 */
@Composable
fun SegmentedProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    blocks: Int = 12
) {
    val filled = (progress.coerceIn(0f, 1f) * blocks).toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ProgressHeight)
            .bevel(BevelStyle.SUNKEN, face = RetroTheme.colors.surface)
            .padding(Dimens.BevelTotal),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(blocks) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (index < filled) RetroTheme.colors.titleActive else Color.Transparent
                    )
            )
        }
    }
}

/**
 * §6.3 — a visible, chunky scrollbar with arrow buttons.
 *
 * "Do not hide it; a visible scrollbar showing how little is left reinforces
 * 'this surface has a bottom'." That's a product requirement wearing a
 * decoration's clothes.
 */
@Composable
fun RetroScrollbar(
    progress: Float,
    modifier: Modifier = Modifier,
    thumbFraction: Float = 0.3f,
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .width(Dimens.ScrollbarWidth)
            .background(RetroTheme.colors.surfaceLight)
    ) {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxHeight()) {
            RetroIconButton(glyph = "▲", onClick = onScrollUp, contentDescription = "Scroll up")

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val clamped = progress.coerceIn(0f, 1f)
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxHeight()) {
                    val trackHeight = maxHeight
                    val thumbHeight = trackHeight * thumbFraction
                    val offset = (trackHeight - thumbHeight) * clamped
                    Box(
                        Modifier
                            .padding(top = offset)
                            .fillMaxWidth()
                            .height(thumbHeight)
                            .bevel(BevelStyle.RAISED)
                    )
                }
            }

            RetroIconButton(glyph = "▼", onClick = onScrollDown, contentDescription = "Scroll down")
        }
    }
}
