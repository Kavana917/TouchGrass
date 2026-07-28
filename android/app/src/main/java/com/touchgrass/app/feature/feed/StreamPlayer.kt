package com.touchgrass.app.feature.feed

import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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
 *  - YouTube goes through the official IFrame embed. Extracting YouTube's
 *    underlying HLS would be simpler and would violate their terms.
 *
 * ⚠️ YouTube embeds are refused outright on some devices (error 152) even
 * with a correct origin and a Chrome user agent. The shipped registry no
 * longer depends on them; the path stays for user-added streams.
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
 *    has none. Loading a real HTML document through loadDataWithBaseURL()
 *    with a youtube.com base supplies the origin and the player configures.
 *
 * 2. `embed/live_stream?channel=…` NO LONGER RESOLVES. It's the obvious way
 *    to avoid pinning video IDs, and it loads far enough to render
 *    YouTube's own "Video unavailable" — which is how we know the wrapper
 *    is right and the endpoint is the dead part.
 *
 * Even with both fixed, some devices still refuse embeds with error 152.
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
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(android.graphics.Color.BLACK)

                // Android WebView advertises "; wv" in its user agent and
                // YouTube refuses embeds from it. Present as mobile Chrome.
                settings.userAgentString = CHROME_USER_AGENT

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                // Required for HTML5 video in a WebView at all.
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
            val wanted = "${stream.streamRef}:${audioOn && stream.hasAudio}"
            if (webView.tag != wanted) {
                webView.tag = wanted
                webView.loadEmbed(stream, audioOn && stream.hasAudio)
            } else {
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
    loadDataWithBaseURL(YOUTUBE_ORIGIN, embedHtml(stream, audioOn), "text/html", "utf-8", null)
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
        "autoplay=1&mute=${if (audioOn) 0 else 1}&playsinline=1" +
        "&rel=0&modestbranding=1&enablejsapi=1&origin=$YOUTUBE_ORIGIN"

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
            <iframe id="player" src="$src" frameborder="0"
              allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe>
          </body>
        </html>
    """.trimIndent()
}

private const val YOUTUBE_ORIGIN = "https://www.youtube.com"

private const val CHROME_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Mobile Safari/537.36"
