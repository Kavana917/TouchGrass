package com.touchgrass.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * §6.3 — the persistent bottom strip (`2.webp`, `3.webp`).
 *
 * A start-style button on the left opening the nav menu, and a sunken clock
 * well on the right showing TIME LEFT TODAY.
 *
 * That clock is the neatest coincidence in the whole design: a taskbar clock
 * is authentically 1997 chrome, and "minutes remaining" is exactly the
 * persistent state Home needs to display anyway (§10). The theme and the
 * product want the same widget in the same corner.
 *
 * NOTE (§12): the button says "menu", never "start" styled as Microsoft's —
 * we keep the language of 90s UI and avoid the trademarks.
 */
@Composable
fun Taskbar(
    timeRemaining: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.TaskbarHeight)
            .bevel(BevelStyle.RAISED)
            .padding(Dimens.BevelTotal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = Dimens.MinTouchTarget)
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RetroButton(
                text = "menu",
                onClick = onMenuClick
            )
        }

        Box(Modifier.weight(1f))

        // The clock well — sunken, pixel numerals, states a fact.
        Box(
            modifier = Modifier
                .heightIn(min = 28.dp)
                .bevel(BevelStyle.SUNKEN, face = RetroTheme.colors.surface)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            PixelText(
                text = timeRemaining,
                style = RetroTheme.typography.numeralSmall,
                maxLines = 1
            )
        }

        Box(Modifier.width(2.dp))
    }
}
