package com.touchgrass.app.feature.feed

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * HLS playback through Media3.
 *
 * ⚠️ THE ERROR CALLBACK IS NOT OPTIONAL.
 *
 * Without it a dead stream renders as a black rectangle and nothing else —
 * indistinguishable from "still buffering", "wrong URL", "no network" and
 * "codec unsupported". That ambiguity cost several rounds of guessing on
 * the Live Feed, in exactly the way silently-swallowed failures cost
 * several rounds on the wall. Failures have to say what they are.
 */
@OptIn(UnstableApi::class)
@Composable
fun HlsPlayerView(
    manifestUrl: String,
    audioOn: Boolean,
    paused: Boolean,
    onError: (String) -> Unit,
    onReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val player = remember(manifestUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(manifestUrl))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(manifestUrl) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onError(error.friendlyMessage())
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) onReady()
                if (state == Player.STATE_ENDED) {
                    onError("The stream ended.")
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Mute rather than stop, so toggling audio doesn't restart the stream.
    player.volume = if (audioOn) 1f else 0f
    player.playWhenReady = !paused

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                // Chrome-free: design_theme.md §8 makes the stream one of
                // the three surfaces where the theme steps back entirely.
                useController = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}

/** Turns ExoPlayer's error codes into something a person can act on. */
private fun PlaybackException.friendlyMessage(): String = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Couldn't reach the stream. Check your connection."

    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "The stream is gone — the webcam host returned an error."

    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "That stream no longer exists."

    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
        "This stream uses plain HTTP, which is blocked for that host."

    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
        "The stream sent something unreadable — it's probably offline."

    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
        "This phone can't decode that stream's format."

    else -> "Playback failed (${errorCodeName})."
}
