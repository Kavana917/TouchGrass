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
    YOUTUBE,
    HLS;

    companion object {
        fun fromName(value: String?): StreamSource =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: YOUTUBE
    }
}
