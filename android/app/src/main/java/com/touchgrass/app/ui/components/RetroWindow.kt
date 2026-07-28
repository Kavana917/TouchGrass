package com.touchgrass.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.touchgrass.app.ui.theme.Dimens

/**
 * Form 1 (§6.2) — the container for every screen in the app.
 *
 * A raised frame with an optional title bar, menu bar and status bar, and a
 * 4dp inset for content. This is the "window" of the desktop metaphor and it
 * does real product work: a window has edges and a bottom, which is exactly
 * the anti-infinite-scroll property the app is built around (§1).
 */
@Composable
fun RetroWindow(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleIcon: PixelArt? = null,
    active: Boolean = true,
    menuItems: List<String> = emptyList(),
    onMenuItemClick: (String) -> Unit = {},
    statusText: String? = null,
    statusSecondary: String? = null,
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bevel(BevelStyle.RAISED)
            .padding(Dimens.BevelTotal)
    ) {
        if (title != null) {
            TitleBar(
                title = title,
                icon = titleIcon,
                active = active,
                onClose = onClose
            )
        }

        if (menuItems.isNotEmpty()) {
            MenuBar(items = menuItems, onItemClick = onMenuItemClick)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(Dimens.WindowPadding),
            content = content
        )

        if (statusText != null) {
            StatusBar(text = statusText, secondary = statusSecondary)
        }
    }
}
