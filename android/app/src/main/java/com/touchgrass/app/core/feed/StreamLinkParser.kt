package com.touchgrass.app.core.feed

/**
 * Turns whatever a user pastes into something playable.
 *
 * Curation is a human job (app_plan.md §3.6) and I can't verify a stream is
 * alive from inside the app — so the answer is to let the person who CAN
 * verify it do the adding, and make pasting a link the whole interaction.
 */
object StreamLinkParser {

    sealed interface Result {
        data class Ok(val source: StreamSource, val ref: String) : Result
        data class Error(val message: String) : Result
    }

    fun parse(input: String): Result {
        val text = input.trim()
        if (text.isEmpty()) return Result.Error("Paste a link first.")

        // A SkylineWebcams page — resolved to a manifest at play time,
        // which is why pasting the ordinary page URL is enough.
        if (text.contains("skylinewebcams.com", ignoreCase = true)) {
            return Result.Ok(StreamSource.SKYLINE, text)
        }

        // Direct HLS.
        if (text.startsWith("http", ignoreCase = true) && text.contains(".m3u8")) {
            return Result.Ok(StreamSource.HLS, text)
        }

        channelIdFrom(text)?.let { return Result.Ok(StreamSource.YOUTUBE_CHANNEL, it) }
        videoIdFrom(text)?.let { return Result.Ok(StreamSource.YOUTUBE, it) }

        // @handle URLs can't be resolved to a channel ID without the
        // YouTube API, and we deliberately ship no API key in the app
        // (tech_stack.md §5.3). Tell the user how to get the real ID.
        if (text.contains("youtube.com/@")) {
            return Result.Error(
                "That's a channel handle. Open the channel, tap Share, and " +
                    "use the link with /channel/UC… in it."
            )
        }

        return Result.Error("Couldn't read that as a YouTube link or .m3u8 URL.")
    }

    /** Channel IDs always start with UC and are 24 characters. */
    private fun channelIdFrom(text: String): String? {
        CHANNEL_URL.find(text)?.groupValues?.get(1)?.let { return it }
        if (RAW_CHANNEL_ID.matches(text)) return text
        return null
    }

    private fun videoIdFrom(text: String): String? {
        VIDEO_URL_PATTERNS.forEach { pattern ->
            pattern.find(text)?.groupValues?.get(1)?.let { return it }
        }
        if (RAW_VIDEO_ID.matches(text)) return text
        return null
    }

    private val CHANNEL_URL = Regex("""youtube\.com/channel/(UC[\w-]{22})""")
    private val RAW_CHANNEL_ID = Regex("""^UC[\w-]{22}$""")

    private val VIDEO_URL_PATTERNS = listOf(
        Regex("""youtube\.com/watch\?(?:.*&)?v=([\w-]{11})"""),
        Regex("""youtu\.be/([\w-]{11})"""),
        Regex("""youtube\.com/live/([\w-]{11})"""),
        Regex("""youtube\.com/embed/([\w-]{11})""")
    )
    private val RAW_VIDEO_ID = Regex("""^[\w-]{11}$""")
}
