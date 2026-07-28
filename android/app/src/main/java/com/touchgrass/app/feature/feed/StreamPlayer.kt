package com.touchgrass.app.feature.feed

import android.view.ViewGroup
import android.webkit.CookieManager
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
        StreamSource.YOUTUBE,
        StreamSource.YOUTUBE_CHANNEL -> YouTubeWebPlayer(stream, audioOn, paused, modifier)
    }
}

/**
 * Plays a YouTube stream inside the official IFrame embed.
 *
 * ⚠️ TWO THINGS LEARNED THE HARD WAY, BOTH ENCODED HERE:
 *
 * 1. THE HTML WRAPPER IS NOT OPTIONAL. Pointing a WebView straight at the
 *    embed URL gives "Error 153 — Video player configuration error".
 *    YouTube rejects embeds arriving with no origin, and a bare loadUrl()
 *    has none — there's no embedding page, so no Referer to check. Loading
 *    a real HTML document through loadDataWithBaseURL() with a youtube.com
 *    base supplies a legitimate origin and the player configures normally.
 *
 * 2. `embed/live_stream?channel=…` DOES NOT WORK ANY MORE. It's the
 *    obvious way to avoid pinning video IDs, and it loads far enough to
 *    render YouTube's own "Video unavailable" — which is how we know the
 *    wrapper above is correct and the endpoint is the dead part. Registry
 *    entries use video IDs; StreamSource.YOUTUBE_CHANNEL is kept only so
 *    old saved entries don't break.
 *
 * Freeze uses the IFrame JS API rather than tearing the player down, so
 * resuming doesn't re-buffer the whole stream.
 */
@Composable
private fun YouTubeWebPlayer(
    stream: Stream,
    audioOn: Boolean,
    paused: Boolean,
    modifier: Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Without this the embed waits for a tap the user already
                // gave by opening the screen.
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(android.graphics.Color.BLACK)

                // Android WebView advertises itself with "; wv" in the user
                // agent, and YouTube refuses embeds from it — the symptom is
                // "This video is unavailable, error 152" on videos that play
                // perfectly in a browser. Presenting as ordinary mobile
                // Chrome is what the official player library does too.
                settings.userAgentString = CHROME_USER_AGENT

                // The embed sets cookies during player setup; without them
                // configuration can fail for the same reason.
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                // Required for HTML5 video to play in a WebView at all —
                // without a WebChromeClient you get a black rectangle.
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                loadEmbed(stream, audioOn && stream.hasAudio)
                tag = "${stream.streamRef}:${audioOn && stream.hasAudio}"
            }
        },
        update = { webView ->
            // Mute is baked into the embed URL, so a change needs a reload.
            val wanted = "${stream.streamRef}:${audioOn && stream.hasAudio}"
            if (webView.tag != wanted) {
                webView.tag = wanted
                webView.loadEmbed(stream, audioOn && stream.hasAudio)
            } else {
                // Freeze/resume goes through the JS API — tearing the player
                // down and rebuilding it would re-buffer the whole stream.
                webView.command(if (paused) "pauseVideo" else "playVideo")
            }
        },
        onRelease = { webView ->
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    )
}

private fun WebView.loadEmbed(stream: Stream, audioOn: Boolean) {
    loadDataWithBaseURL(
        YOUTUBE_ORIGIN,
        embedHtml(stream, audioOn),
        "text/html",
        "utf-8",
        null
    )
}

/** Sends an IFrame API command to the embedded player. */
private fun WebView.command(func: String) {
    evaluateJavascript(
        """
        (function() {
          var f = document.getElementById('player');
          if (f && f.contentWindow) {
            f.contentWindow.postMessage(
              JSON.stringify({event:'command', func:'$func', args:[]}), '*'
            );
          }
        })();
        """.trimIndent(),
        null
    )
}

private fun embedHtml(stream: Stream, audioOn: Boolean): String {
    val base = when (stream.source) {
        StreamSource.YOUTUBE_CHANNEL ->
            "https://www.youtube.com/embed/live_stream?channel=${stream.streamRef}"
        else ->
            "https://www.youtube.com/embed/${stream.streamRef}"
    }

    val src = base +
        (if (base.contains('?')) "&" else "?") +
        "autoplay=1" +
        "&mute=${if (audioOn) 0 else 1}" +
        "&playsinline=1" +
        "&rel=0" +
        "&modestbranding=1" +
        "&enablejsapi=1" +
        "&origin=$YOUTUBE_ORIGIN"

    return """
        <!DOCTYPE html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              html, body { margin:0; padding:0; height:100%; background:#000; overflow:hidden; }
              iframe { width:100%; height:100%; border:0; display:block; }
            </style>
          </head>
          <body>
            <iframe
              id="player"
              src="$src"
              frameborder="0"
              allow="autoplay; encrypted-media; picture-in-picture"
              allowfullscreen>
            </iframe>
          </body>
        </html>
    """.trimIndent()
}

private const val YOUTUBE_ORIGIN = "https://www.youtube.com"

/**
 * Ordinary mobile Chrome — deliberately without the "; wv" token that
 * Android WebView normally adds and that YouTube uses to reject embeds.
 */
private const val CHROME_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Mobile Safari/537.36"

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
