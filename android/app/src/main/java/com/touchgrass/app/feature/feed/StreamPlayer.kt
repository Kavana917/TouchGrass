package com.touchgrass.app.feature.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.touchgrass.app.core.feed.Stream
import com.touchgrass.app.core.feed.StreamResolver
import com.touchgrass.app.core.feed.StreamSource

/**
 * Plays a stream, choosing the renderer by source.
 *
 * TWO PATHS, AND THE SPLIT IS LEGAL AS WELL AS TECHNICAL
 * (tech_stack.md §5.2):
 *
 *  - HLS and SKYLINE go through Media3, which we control completely.
 *    SKYLINE resolves its manifest first, because the URL is tokenised and
 *    regenerated per request — see StreamResolver.
 *  - YouTube goes through the official IFrame player. Extracting YouTube's
 *    underlying HLS would be simpler and would violate their terms.
 */
@Composable
fun StreamPlayer(
    stream: Stream,
    audioOn: Boolean,
    paused: Boolean,
    resolver: StreamResolver,
    onError: (String) -> Unit,
    onReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (stream.source) {
        StreamSource.HLS, StreamSource.SKYLINE -> {
            var manifestUrl by remember(stream.id) { mutableStateOf<String?>(null) }

            LaunchedEffect(stream.id) {
                when (val result = resolver.resolve(stream)) {
                    is StreamResolver.Result.Ok -> manifestUrl = result.manifestUrl
                    is StreamResolver.Result.Error -> onError(result.message)
                }
            }

            manifestUrl?.let { url ->
                HlsPlayerView(
                    manifestUrl = url,
                    audioOn = audioOn && stream.hasAudio,
                    paused = paused,
                    onError = onError,
                    onReady = onReady,
                    modifier = modifier
                )
            }
        }

        StreamSource.YOUTUBE,
        StreamSource.YOUTUBE_CHANNEL ->
            YouTubePlayer(stream, audioOn, paused, onError, onReady, modifier)
    }
}

/**
 * Plays a YouTube stream through android-youtube-player.
 *
 * ⚠️ WHY A LIBRARY AND NOT A WEBVIEW WE CONTROL:
 *
 * This used to be a hand-rolled WebView holding an `<iframe>`, and it spent
 * three commits losing a fight it could not win: error 153 (no origin), then
 * error 152 (refused embed) even after supplying a youtube.com base URL and
 * masquerading as mobile Chrome. Every fix addressed a symptom.
 *
 * The root cause is that YouTube's embed is not an iframe you point at a
 * URL — it is the IFrame Player API, a JavaScript handshake. The library
 * does that handshake: it serves a local page, loads `iframe_api`,
 * constructs a real `YT.Player`, and bridges its events back to Kotlin.
 * tech_stack.md §5.2 has said "Required" about this library since before the
 * WebView was written, and the dependency has been declared and unused the
 * whole time.
 *
 * The other win is diagnostic. A WebView reports failure as pixels — an
 * error painted inside a page we can't read. The listener reports it as a
 * typed enum, which is the difference between "a black rectangle" and
 * "VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER". Same argument as HlsPlayerView.
 *
 * ⚠️ CONTROLS STAY ON. YouTube's terms for embedded players forbid hiding or
 * obscuring the controls, so `controls(1)` is not a preference. Clear Mode
 * (app_plan.md §3.4) may hide OUR chrome over a YouTube stream, never theirs.
 */
@Composable
private fun YouTubePlayer(
    stream: Stream,
    audioOn: Boolean,
    paused: Boolean,
    onError: (String) -> Unit,
    onReady: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keyed on the stream so switching streams builds a fresh player rather
    // than reusing one that is mid-handshake with the previous video.
    val playerView = remember(stream.id) {
        YouTubePlayerView(context).apply {
            // We initialize by hand below, to pass IFramePlayerOptions.
            enableAutomaticInitialization = false
            // The view is 16:9 by default; the feed is full-bleed.
            matchParent()
        }
    }

    var player by remember(stream.id) { mutableStateOf<YouTubePlayer?>(null) }

    DisposableEffect(stream.id) {
        val listener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                player = youTubePlayer
                youTubePlayer.loadVideo(stream.streamRef, 0f)
                onReady()
            }

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: PlayerConstants.PlayerError
            ) {
                onError(error.friendlyMessage())
            }
        }

        playerView.initialize(
            listener,
            IFramePlayerOptions.Builder()
                .controls(1)        // required by YouTube's embed terms
                .autoplay(1)
                .mute(if (audioOn && stream.hasAudio) 1 else 0)
                .rel(0)             // no "up next" grid when a stream ends
                .modestBranding(1)
                .build()
        )

        // The view is a LifecycleEventObserver: this is what pauses playback
        // when the app goes to the background, instead of a river quietly
        // streaming data all night in someone's pocket.
        lifecycleOwner.lifecycle.addObserver(playerView)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    // Mute rather than stop, so toggling audio doesn't restart the stream.
    LaunchedEffect(player, audioOn, paused) {
        val current = player ?: return@LaunchedEffect
        if (audioOn && stream.hasAudio) current.unMute() else current.mute()
        if (paused) current.pause() else current.play()
    }

    AndroidView(modifier = modifier, factory = { playerView })
}

/** Turns the player's error enum into something a person can act on. */
private fun PlayerConstants.PlayerError.friendlyMessage(): String = when (this) {
    PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
        "This channel doesn't allow its stream to play outside YouTube."

    PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
        "That stream no longer exists."

    PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST ->
        "That stream's ID looks wrong."

    PlayerConstants.PlayerError.HTML_5_PLAYER ->
        "The video player failed to start on this device."

    PlayerConstants.PlayerError.UNKNOWN ->
        "Playback failed for an unknown reason."
}
