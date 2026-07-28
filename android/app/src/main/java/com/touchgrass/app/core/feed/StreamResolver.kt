package com.touchgrass.app.core.feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a stream reference into a playable manifest URL.
 *
 * ⚠️ WHY THIS EXISTS — the whole Live Feed sourcing problem in one place:
 *
 * Free live webcam feeds are, in practice, one of three things:
 *   1. Dead. Published URL lists rot within months.
 *   2. YouTube, which refuses embedding on many devices (error 152).
 *   3. Token-protected, where the manifest URL is generated per request
 *      and expires — so nothing can be hardcoded.
 *
 * SkylineWebcams is category 3, and it turns out that's the *good* case:
 * the token is issued by their own public page, so resolving it at play
 * time gives a URL that always works, instead of a URL that worked once
 * when the registry was written. Verified end to end — page fetch yields a
 * fresh token, and the manifest returns live HTTPS segments.
 *
 * This is the same request their own web player makes. Worth a check with
 * them before any public release, though.
 */
@Singleton
class StreamResolver @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    sealed interface Result {
        data class Ok(val manifestUrl: String) : Result
        data class Error(val message: String) : Result
    }

    suspend fun resolve(stream: Stream): Result = withContext(Dispatchers.IO) {
        when (stream.source) {
            StreamSource.SKYLINE -> resolveSkyline(stream.streamRef)
            StreamSource.HLS -> Result.Ok(stream.streamRef)
            else -> Result.Error("This stream type isn't resolvable to a manifest.")
        }
    }

    private fun resolveSkyline(pageUrl: String): Result {
        val html = runCatching {
            val request = Request.Builder()
                .url(pageUrl)
                // Their page serves different markup to unknown agents.
                .header("User-Agent", BROWSER_UA)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.Error("Page returned ${response.code}.")
                response.body?.string().orEmpty()
            }
        }.getOrElse { return Result.Error("Couldn't reach the webcam page.") }

        val token = TOKEN.find(html)?.groupValues?.getOrNull(1)
            ?: return Result.Error("That page isn't streaming right now.")

        return Result.Ok("https://hd-auth.skylinewebcams.com/live.m3u8?a=$token")
    }

    private companion object {
        val TOKEN = Regex("""livee\.m3u8\?a=([a-z0-9]+)""")
        const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
