package com.touchgrass.app.core.feed

import android.content.Context
import com.touchgrass.app.core.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The stream registry: what ships with the app, plus whatever the user adds.
 *
 * ⚠️ WHY USER-ADDED STREAMS EXIST AT ALL:
 *
 * The registry is hand-curated by design (app_plan.md §3.6), and curation
 * means *verifying a stream actually plays* — which cannot be done from
 * inside the build. The first shipped registry pinned YouTube video IDs and
 * every one of them was dead on arrival, because 24/7 streams get restarted
 * and issued a new ID.
 *
 * Two fixes, both here: bundled entries now reference CHANNELS rather than
 * videos, and the person holding the phone can add a stream by pasting a
 * link and immediately seeing whether it works.
 */
@Singleton
class StreamRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {

    private val bundled = MutableStateFlow<List<Stream>>(emptyList())

    /** Bundled first, then the user's own. */
    val streams: Flow<List<Stream>> =
        combine(bundled, settings.customStreamsJson) { built, customJson ->
            built + parseArray(runCatching { JSONArray(customJson) }.getOrNull())
        }

    val favourites: Flow<Set<String>> = settings.favouriteStreams

    val customStreams: Flow<List<Stream>> =
        settings.customStreamsJson.map { json ->
            parseArray(runCatching { JSONArray(json) }.getOrNull())
        }

    suspend fun load() {
        if (bundled.value.isNotEmpty()) return
        bundled.value = withContext(Dispatchers.IO) { readBundled() }
    }

    suspend fun toggleFavourite(streamId: String) {
        settings.toggleFavouriteStream(streamId)
    }

    /**
     * Adds a stream from a pasted link.
     * @return an error message, or null on success.
     */
    suspend fun addStream(title: String, place: String, link: String): String? {
        val cleanTitle = title.trim().ifBlank { return "Give it a name." }

        return when (val parsed = StreamLinkParser.parse(link)) {
            is StreamLinkParser.Result.Error -> parsed.message
            is StreamLinkParser.Result.Ok -> {
                val stream = Stream(
                    id = "custom-${System.currentTimeMillis()}",
                    title = cleanTitle,
                    place = place.trim(),
                    lat = 0.0,
                    lng = 0.0,
                    category = StreamCategory.CITY,
                    source = parsed.source,
                    streamRef = parsed.ref,
                    attribution = "Added by you"
                )
                val current = customStreams.first()
                settings.setCustomStreamsJson(toJsonArray(current + stream))
                null
            }
        }
    }

    suspend fun removeStream(streamId: String) {
        val remaining = customStreams.first().filterNot { it.id == streamId }
        settings.setCustomStreamsJson(toJsonArray(remaining))
    }

    private fun readBundled(): List<Stream> = runCatching {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        parseArray(JSONObject(raw).optJSONArray("streams"))
    }.getOrDefault(emptyList())

    private fun parseArray(array: JSONArray?): List<Stream> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            // One malformed entry must not take the whole registry down.
            runCatching { obj.toStream() }.getOrNull()
        }
    }

    private fun JSONObject.toStream() = Stream(
        id = getString("id"),
        title = getString("title"),
        place = optString("place"),
        lat = optDouble("lat", 0.0),
        lng = optDouble("lng", 0.0),
        category = StreamCategory.fromName(optString("category")),
        moods = optJSONArray("moods")?.let { moods ->
            (0 until moods.length()).mapNotNull { StreamMood.fromName(moods.optString(it)) }
        } ?: emptyList(),
        source = StreamSource.fromName(optString("source")),
        streamRef = getString("streamRef"),
        hasAudio = optBoolean("hasAudio", true),
        timezone = optString("timezone").ifBlank { null },
        attribution = optString("attribution").ifBlank { null },
        lastVerified = optString("lastVerified").ifBlank { null }
    )

    private fun toJsonArray(streams: List<Stream>): String {
        val array = JSONArray()
        streams.forEach { stream ->
            array.put(
                JSONObject().apply {
                    put("id", stream.id)
                    put("title", stream.title)
                    put("place", stream.place)
                    put("category", stream.category.name)
                    put("source", stream.source.name)
                    put("streamRef", stream.streamRef)
                    put("hasAudio", stream.hasAudio)
                    stream.attribution?.let { put("attribution", it) }
                }
            )
        }
        return array.toString()
    }

    private companion object {
        const val ASSET_NAME = "streams.json"
    }
}
