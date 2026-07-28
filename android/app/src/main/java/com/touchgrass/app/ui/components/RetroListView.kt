package com.touchgrass.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Form 7 (§6.2) — the file-listing list view.
 *
 * Column headers as small raised buttons, white rows, navy selection bar.
 * Used by the FOMO digest, essay history and the stream list.
 *
 * Deliberately NOT a LazyColumn by default: these lists are finite and short
 * by design (§1 — "nothing scrolls forever"). If a caller genuinely needs
 * virtualisation it can pass its own scrolling container.
 */
@Composable
fun RetroListView(
    headers: List<String>,
    modifier: Modifier = Modifier,
    weights: List<Float> = headers.map { 1f },
    content: @Composable ColumnScopeMarker.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bevel(BevelStyle.SUNKEN)
    ) {
        // Header strip
        Row(modifier = Modifier.fillMaxWidth()) {
            headers.forEachIndexed { index, header ->
                Box(
                    modifier = Modifier
                        .weight(weights.getOrElse(index) { 1f })
                        .bevel(BevelStyle.RAISED)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    PixelText(text = header, maxLines = 1)
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            ColumnScopeMarker(weights).content()
        }
    }
}

/** Restricts [RetroListRow] to only being used inside a [RetroListView]. */
class ColumnScopeMarker internal constructor(internal val weights: List<Float>)

@Composable
fun ColumnScopeMarker.RetroListRow(
    cells: List<String>,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val background =
        if (selected) RetroTheme.colors.titleActive else RetroTheme.colors.surfaceWhite
    val textColor =
        if (selected) RetroTheme.colors.titleText else RetroTheme.colors.bodyText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cells.forEachIndexed { index, cell ->
            Box(
                modifier = Modifier
                    .weight(weights.getOrElse(index) { 1f })
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                PixelText(text = cell, color = textColor, maxLines = 1)
            }
        }
    }
}
