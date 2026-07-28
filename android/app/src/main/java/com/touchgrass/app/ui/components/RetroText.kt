package com.touchgrass.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Text helpers so components don't each re-derive colour and style.
 *
 * The §5 dividing line, made into two functions:
 *   [PixelText] — you SCAN it. Labels, buttons, numbers, titles.
 *   [BodyText]  — you READ it. Essays, prose, descriptions.
 */

@Composable
fun PixelText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = RetroTheme.typography.chrome,
    color: Color = RetroTheme.colors.bodyText,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines
    )
}

@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = RetroTheme.typography.body,
    color: Color = RetroTheme.colors.bodyText
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color
    )
}
