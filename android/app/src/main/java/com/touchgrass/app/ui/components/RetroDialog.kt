package com.touchgrass.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Form 9 (§6.2) — the dialog / balloon, as in `2.webp`.
 *
 * This is the form the Instagram overlay takes in Phase 4 (§10), so two
 * things matter beyond looks:
 *
 *  - VOICE (§11): the message is a machine statement of fact
 *    (`The pass has expired.`), never a judgement. A 1997 dialog box is
 *    incapable of disappointment, which is exactly why the theme suits an
 *    app that must never scold.
 *  - Cream face by default — the `2.webp` notice-balloon look.
 */
@Composable
fun RetroDialog(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: PixelArt? = null,
    face: Color? = null,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .width(300.dp)
            .bevel(BevelStyle.RAISED, face = face ?: RetroTheme.colors.paperCream)
            .padding(Dimens.BevelTotal)
    ) {
        if (title != null) {
            TitleBar(title = title, icon = icon, active = true)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.ContentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null && title == null) {
                PixelIcon(
                    art = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconBase)
                )
                Box(Modifier.width(Dimens.ContentPadding))
            }
            BodyText(text = message, modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ContentPadding)
                .padding(bottom = Dimens.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing, Alignment.End)
        ) {
            if (secondaryLabel != null && onSecondary != null) {
                RetroButton(text = secondaryLabel, onClick = onSecondary)
            }
            if (primaryLabel != null && onPrimary != null) {
                RetroButton(text = primaryLabel, onClick = onPrimary, primary = true)
            }
        }
    }
}
