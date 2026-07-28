package com.touchgrass.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Form 6 (§6.2) — the right-click context menu, as seen in `3.webp`.
 *
 * Raised panel, 40dp rows, `▸` for submenus, groove separators between
 * groups, navy highlight on the pressed row.
 */
data class ContextMenuItem(
    val label: String,
    val hasSubmenu: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
)

@Composable
fun ContextMenu(
    groups: List<List<ContextMenuItem>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .bevel(BevelStyle.RAISED)
            .padding(Dimens.BevelTotal)
    ) {
        groups.forEachIndexed { index, group ->
            group.forEach { item -> ContextMenuRow(item) }
            if (index != groups.lastIndex) {
                GrooveSeparator(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ContextMenuRow(item: ContextMenuItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val highlighted = isPressed && item.enabled

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.ContextMenuRowHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = item.enabled,
                onClick = item.onClick
            )
            .background(
                if (highlighted) RetroTheme.colors.titleActive else RetroTheme.colors.surface
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelText(
            text = item.label,
            color = when {
                !item.enabled -> RetroTheme.colors.disabledText
                highlighted -> RetroTheme.colors.titleText
                else -> RetroTheme.colors.bodyText
            },
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (item.hasSubmenu) {
            PixelText(
                text = "▸",
                color = if (highlighted) RetroTheme.colors.titleText
                else RetroTheme.colors.bodyText,
                maxLines = 1
            )
        }
    }
}

/**
 * §6.3 — the only divider we use: 1px shadow over 1px white.
 * That two-tone stack is what makes it read as an engraved groove rather
 * than a flat line.
 */
@Composable
fun GrooveSeparator(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.BevelLine)
                .background(RetroTheme.colors.surfaceShadow)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.BevelLine)
                .background(RetroTheme.colors.surfaceWhite)
        )
    }
}
