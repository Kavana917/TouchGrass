package com.touchgrass.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Form 8 (§6.2) — the status bar.
 *
 * "Always states a fact, never an opinion." That constraint is the whole
 * reason this component earns its place: `0 items. You're caught up.` is
 * simultaneously authentic 1997 chrome and precisely the message the FOMO
 * feature exists to deliver (§10).
 *
 * Sunken wells, pixel text, resize grip at the right.
 */
@Composable
fun StatusBar(
    text: String,
    modifier: Modifier = Modifier,
    secondary: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.StatusBarHeight)
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusWell(text = text, modifier = Modifier.weight(1f))

        if (secondary != null) {
            Box(Modifier.width(2.dp))
            StatusWell(text = secondary, modifier = Modifier.width(110.dp))
        }

        Box(Modifier.width(2.dp))
        ResizeGrip()
    }
}

@Composable
private fun StatusWell(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = Dimens.StatusBarHeight)
            .bevel(BevelStyle.SUNKEN, face = RetroTheme.colors.surface)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        PixelText(text = text, maxLines = 1)
    }
}

/**
 * The little diagonal hatch at the bottom-right of every Win95 window.
 * Purely decorative here — there's nothing to resize on a phone — but its
 * absence is immediately noticeable to anyone who used the era.
 */
@Composable
private fun ResizeGrip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(Dimens.StatusBarHeight)
            .heightIn(min = Dimens.StatusBarHeight),
        contentAlignment = Alignment.Center
    ) {
        PixelText(
            text = "◢",
            color = RetroTheme.colors.surfaceShadow,
            maxLines = 1
        )
    }
}
