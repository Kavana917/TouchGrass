package com.touchgrass.app.core.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.components.PixelIcons
import com.touchgrass.app.ui.components.RetroDialog
import com.touchgrass.app.ui.theme.TouchGrassTheme

/**
 * What the wall actually looks like.
 *
 * A modal dialog box on a dimmed screen (design_theme.md §10). The retro
 * costume is doing real work here: an old OS states a fact and offers you
 * buttons — it has no opinion about you. That's precisely the tone this
 * moment needs, because the one thing the app must never do is scold
 * (§11, and app_plan.md §1's anti-goals).
 *
 * "The pass has expired." Not "You've been on Instagram for 30 minutes
 * again." The dialog is incapable of disappointment, and that is the point.
 */
@Composable
fun WallContent(
    onWriteEssay: () -> Unit,
    onBackOff: () -> Unit,
    modifier: Modifier = Modifier
) {
    TouchGrassTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                // Dim rather than hide. Seeing the app you were just in,
                // greyed out behind a system dialog, is the whole metaphor.
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            RetroDialog(
                title = "TouchGrass",
                icon = PixelIcons.Clock,
                message = "The pass has expired.",
                primaryLabel = "Write an essay",
                onPrimary = onWriteEssay,
                secondaryLabel = "Not now",
                onSecondary = onBackOff
            )
        }
    }
}
