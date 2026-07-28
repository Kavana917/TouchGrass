package com.touchgrass.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Form 5 (§6.2) — the menu bar.
 *
 * 28dp strip, pixel labels, first letter underlined. The underline is
 * DECORATIVE ONLY — there's no keyboard on a phone, so per §5 we use it for
 * flavour and never imply a shortcut that doesn't exist.
 *
 * Highlight on press is the navy selection bar, same as a real menu.
 */
@Composable
fun MenuBar(
    items: List<String>,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MenuBarHeight)
            .background(RetroTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            MenuBarItem(label = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun MenuBarItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            // Menu strip is 28dp but the tap area still clears 48dp (§9).
            .heightIn(min = Dimens.MinTouchTarget)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(
                if (isPressed) RetroTheme.colors.titleActive else RetroTheme.colors.surface
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mnemonicLabel(label),
            style = RetroTheme.typography.chrome,
            color = if (isPressed) RetroTheme.colors.titleText else RetroTheme.colors.bodyText,
            maxLines = 1
        )
    }
}

/** Underlines the first character, e.g. F̲ile — decorative, see above. */
private fun mnemonicLabel(label: String): AnnotatedString = buildAnnotatedString {
    if (label.isEmpty()) return@buildAnnotatedString
    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
        append(label.first())
    }
    append(label.drop(1))
}
