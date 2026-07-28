package com.touchgrass.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Form 4 (§6.2) — the sunken white well.
 *
 * Two uses: a read-only container for content, and an editable text field.
 *
 * NOTE for Phase 3: the essay editor uses the editable variant, and §8 makes
 * it a chrome-free exception — the window frame stays retro but the writing
 * area is plain 16sp sans with generous line height. Someone is hand-typing
 * 150 words under mild frustration; nothing in that rectangle may add
 * friction. That's why [text] defaults to the sans body style, not pixel.
 */
@Composable
fun SunkenField(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .bevel(BevelStyle.SUNKEN)
            .padding(Dimens.FieldPadding)
    ) {
        content()
    }
}

@Composable
fun SunkenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = Dimens.MinTouchTarget,
    textStyle: TextStyle = RetroTheme.typography.body,
    placeholder: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .bevel(BevelStyle.SUNKEN)
            .padding(Dimens.FieldPadding)
    ) {
        if (value.isEmpty() && placeholder != null) {
            BodyText(
                text = placeholder,
                style = textStyle,
                color = RetroTheme.colors.surfaceShadow
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = textStyle.copy(color = RetroTheme.colors.bodyText),
            cursorBrush = SolidColor(RetroTheme.colors.bodyText),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
