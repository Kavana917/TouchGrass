package com.touchgrass.app.core.feed

/**
 * One curated live stream (app_plan.md §3.6).
 *
 * Streams are hand-picked, not discovered. ~50 excellent ones beat 500
 * unreliable ones, and the whole feature dies if a user taps three dead
 * links in a row.
 */
data class Stream(
    val id: String,
    val title: String,
    val place: String,
    val lat: Double,
    val lng: Double,
    val category: StreamCategory,
    val moods: List<StreamMood> = emptyList(),
    val source: StreamSource,
    /** YouTube video ID, or a direct HLS manifest URL. */
    val streamRef: String,
    val hasAudio: Boolean = true,
    /** IANA zone, so we can show the local time where the camera is. */
    val timezone: String? = null,
    val attribution: String? = null,
    /** Last time this was confirmed alive. Streams rot constantly. */
    val lastVerified: String? = null
)

enum class StreamCategory(val label: String) {
    RIVER("Rivers"),
    COAST("Coasts"),
    MOUNTAIN("Mountains"),
    CITY("Cities"),
    WILDLIFE("Wildlife"),
    SPACE("Space");

    companion object {
        fun fromName(value: String?): StreamCategory =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CITY
    }
}

enum class StreamMood(val label: String) {
    CALM("Calm"),
    ALIVE("Alive"),
    DARK_AND_QUIET("Dark & quiet");

    companion object {
        fun fromName(value: String?): StreamMood? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * How the stream is played.
 *
 * The distinction is legal, not just technical (tech_stack.md §5.2):
 * YouTube's terms require their content to play through the official IFrame
 * player. Extracting the underlying HLS URL violates those terms, breaks
 * whenever YouTube changes its internals, and is a straightforward route to
 * a takedown. Direct webcam feeds have no such constraint and play through
 * Media3.
 */
enum class StreamSource {
    /** A specific YouTube video ID. */
    YOUTUBE,

    /**
     * A YouTube CHANNEL id — plays whatever that channel is live-streaming
     * right now, via `youtube.com/embed/live_stream?channel=…`.
     *
     * ⚠️ THIS IS THE DURABLE ONE, AND IT MATTERS.
     *
     * The first registry hardcoded video IDs and every entry was dead within
     * days: 24/7 "live" streams are restarted regularly and get a NEW video
     * ID each time, so a pinned ID rots almost immediately. Channel IDs are
     * permanent. Prefer this for anything that runs continuously.
     */
    YOUTUBE_CHANNEL,

    /** Direct HLS manifest from a webcam. */
    HLS,

    /**
     * A SkylineWebcams page URL, resolved to a manifest at play time.
     *
     * Their manifest URLs carry a token that is regenerated on every page
     * load, so nothing can be pinned — which is exactly why this works
     * where hardcoded lists rot. See StreamResolver.
     */
    SKYLINE;

    companion object {
        fun fromName(value: String?): StreamSource =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: YOUTUBE
    }
}
