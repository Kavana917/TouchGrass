package com.touchgrass.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Form 2 (§6.2) — the navy title bar.
 *
 * 32dp tall, optional pixel icon at the left, bold white title, window
 * buttons at the right. An inactive window's bar goes gray (#808080), which
 * is how a desktop signalled focus before anyone had heard of a focus ring.
 *
 * The `_ □ ✕` buttons are optional — most screens in this app only need
 * close, and some need none at all.
 */
@Composable
fun TitleBar(
    title: String,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    icon: PixelArt? = null,
    onMinimize: (() -> Unit)? = null,
    onMaximize: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.TitleBarHeight)
            .background(
                if (active) RetroTheme.colors.titleActive
                else RetroTheme.colors.titleInactive
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            PixelIcon(
                art = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Box(Modifier.size(6.dp))
        }

        PixelText(
            text = title,
            style = RetroTheme.typography.titleBar,
            color = RetroTheme.colors.titleText,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            onMinimize?.let {
                RetroIconButton(glyph = "_", onClick = it, contentDescription = "Minimise")
            }
            onMaximize?.let {
                RetroIconButton(glyph = "□", onClick = it, contentDescription = "Maximise")
            }
            onClose?.let {
                RetroIconButton(glyph = "✕", onClick = it, contentDescription = "Close")
            }
        }
    }
}
