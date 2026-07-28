package com.touchgrass.app.feature.feed

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.theme.Dimens

/**
 * A single stream, full screen.
 *
 * CLEAR MODE is the signature interaction (app_plan.md §3.4): everything
 * disappears — status bar, navigation, every control — and only the view
 * remains. One tap brings the controls back for a few seconds.
 *
 * design_theme.md §8 makes this one of three surfaces where the retro theme
 * steps back entirely: "a live river is the point; a title bar over it is
 * noise." So there is no window frame here, no bevels, no chrome. The theme
 * owns the frame; the user owns the content.
 */
@Composable
fun StreamScreen(
    streamId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stream = remember(streamId, state.streams) { viewModel.streamById(streamId) }

    var controlsVisible by remember { mutableStateOf(true) }
    var frozen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity

    // Keep the screen awake — you're watching a river, not touching the
    // phone, and a 30-second timeout would make the feature unusable.
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Immersive: system bars away while watching, restored on exit.
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // Controls fade out on their own. Clear Mode is the resting state, not
    // a mode you have to keep choosing.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            kotlinx.coroutines.delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }

    BackHandler { onBack() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        if (stream == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelText("Stream not found.", color = Color.White)
            }
        } else {
            StreamPlayer(
                stream = stream,
                audioOn = state.audioOn,
                paused = frozen,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (controlsVisible && stream != null) {
            // Minimal, and over a scrim rather than a window frame — chrome
            // here would defeat the purpose of the screen.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(Dimens.ContentPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
            ) {
                PixelText(text = stream.title, color = Color.White, maxLines = 1)
                PixelText(text = stream.place, color = Color.White.copy(alpha = 0.7f), maxLines = 1)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RetroButton(text = "Back", onClick = onBack)
                    if (stream.hasAudio) {
                        RetroButton(
                            text = if (state.audioOn) "Sound on" else "Sound off",
                            onClick = { viewModel.toggleAudio() }
                        )
                    }
                    // Freeze frame lands properly in Phase 9, where it lets
                    // you draw something that won't hold still. Useful here
                    // on its own.
                    RetroButton(
                        text = if (frozen) "Resume" else "Freeze",
                        onClick = { frozen = !frozen }
                    )
                }

                PixelText(
                    text = "tap anywhere to hide",
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private const val CONTROLS_TIMEOUT_MS = 3_000L
