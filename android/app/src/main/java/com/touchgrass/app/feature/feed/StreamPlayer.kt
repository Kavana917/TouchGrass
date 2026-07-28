package com.touchgrass.app.feature.feed

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.touchgrass.app.core.feed.Stream
import com.touchgrass.app.core.feed.StreamSource

/**
 * Plays a stream, choosing the renderer by source.
 *
 * TWO PLAYERS, AND THE SPLIT IS LEGAL AS WELL AS TECHNICAL
 * (tech_stack.md §5.2):
 *
 *  - Direct webcam HLS goes through Media3/ExoPlayer, which we control
 *    completely — quality, buffering, lifecycle.
 *  - YouTube goes through the official IFrame player. Extracting YouTube's
 *    underlying HLS URL would be simpler and would violate their terms,
 *    break whenever they change internals, and invite a takedown.
 */
@Composable
fun StreamPlayer(
    stream: Stream,
    audioOn: Boolean,
    paused: Boolean,
    modifier: Modifier = Modifier
) {
    when (stream.source) {
        StreamSource.HLS -> HlsPlayer(stream, audioOn, paused, modifier)
        StreamSource.YOUTUBE -> YouTubePlayer(stream, audioOn, paused, modifier)
        StreamSource.YOUTUBE_CHANNEL -> YouTubeChannelPlayer(stream, audioOn, modifier)
    }
}

/**
 * Plays whatever a channel is currently live-streaming.
 *
 * Uses YouTube's own `embed/live_stream?channel=…` endpoint in a WebView —
 * still the official IFrame embed, so still within YouTube's terms, but
 * resolved by YouTube at play time instead of pinned to a video ID that
 * expires when the broadcaster restarts the stream.
 */
@Composable
private fun YouTubeChannelPlayer(
    stream: Stream,
    audioOn: Boolean,
    modifier: Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Without this the embed waits for a tap that the user has
                // already given by opening the screen.
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(android.graphics.Color.BLACK)

                // A WebChromeClient is required for HTML5 video to play at
                // all in a WebView — without one you get a black rectangle.
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                loadUrl(channelEmbedUrl(stream.streamRef, audioOn && stream.hasAudio))
            }
        },
        update = { webView ->
            // Reload only when the mute state actually changes; the embed
            // has no JS bridge here to toggle it in place.
            val wanted = channelEmbedUrl(stream.streamRef, audioOn && stream.hasAudio)
            if (webView.tag != wanted) {
                webView.tag = wanted
                webView.loadUrl(wanted)
            }
        },
        onRelease = { webView ->
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    )
}

private fun channelEmbedUrl(channelId: String, audioOn: Boolean): String =
    "https://www.youtube.com/embed/live_stream" +
        "?channel=$channelId" +
        "&autoplay=1" +
        "&mute=${if (audioOn) 0 else 1}" +
        "&playsinline=1"

@OptIn(UnstableApi::class)
@Composable
private fun HlsPlayer(
    stream: Stream,
    audioOn: Boolean,
    paused: Boolean,
    modifier: Modifier
) {
    val context = LocalContext.current

    val player = remember(stream.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(stream.streamRef))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }

    // Mute rather than stop, so toggling audio doesn't restart the stream.
    player.volume = if (audioOn && stream.hasAudio) 1f else 0f
    player.playWhenReady = !paused

    DisposableEffect(stream.id) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                // Chrome-free: §8 makes the stream one of the three surfaces
                // where the theme steps back entirely.
                useController = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}

@Composable
private fun YouTubePlayer(
    stream: Stream,
    audioOn: Boolean,
    paused: Boolean,
    modifier: Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var player: YouTubePlayer? = remember { null }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                // We drive lifecycle ourselves below so the view can be
                // released properly when the screen goes away.
                enableAutomaticInitialization = false

                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player = youTubePlayer
                        youTubePlayer.loadVideo(stream.streamRef, 0f)
                        if (!audioOn || !stream.hasAudio) youTubePlayer.mute()
                    }
                })

                initialize(
                    object : AbstractYouTubePlayerListener() {},
                    // Handle network changes ourselves rather than letting
                    // the view reload aggressively.
                    true
                )

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            player?.let { p ->
                if (audioOn && stream.hasAudio) p.unMute() else p.mute()
                if (paused) p.pause() else p.play()
            }
        },
        onRelease = { view -> view.release() }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player?.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
