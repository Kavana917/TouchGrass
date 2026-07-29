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
 * The stream registry: whatever is live right now, plus whatever the user adds.
 *
 * ⚠️ THREE SOURCES, IN PRIORITY ORDER, AND THE ORDER IS THE DESIGN:
 *
 *  1. The PUBLISHED registry, rebuilt every few hours by a scheduled job and
 *     cached here after the first successful fetch. This is the real one.
 *  2. The BUNDLED asset, used only until that first fetch lands, or when the
 *     phone has no network. It is a floor, not a source of truth.
 *  3. The user's own streams, always appended.
 *
 * The first shipped registry pinned YouTube video IDs and was dead within
 * days (commit edb57f6): 24/7 streams restart constantly and are reissued a
 * new ID each time. Curation cannot fix that, because the rot happens after
 * the build — which is why the live half of the registry now comes down the
 * wire and only the *channel* list is curated (server/channels.json).
 *
 * User-added streams stay, and matter for the same reason they always did:
 * the person holding the phone is the only one who can verify a stream
 * actually plays on it.
 */
@Singleton
class StreamRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val registryClient: RegistryClient
) {

    private val bundled = MutableStateFlow<List<Stream>>(emptyList())

    /** Published registry if we have one, otherwise bundled; then the user's own. */
    val streams: Flow<List<Stream>> =
        combine(
            bundled,
            settings.remoteStreamsJson,
            settings.customStreamsJson
        ) { built, remoteJson, customJson ->
            val published = parseRegistry(remoteJson)
            val base = published.ifEmpty { built }
            base + parseArray(runCatching { JSONArray(customJson) }.getOrNull())
        }

    val favourites: Flow<Set<String>> = settings.favouriteStreams

    val customStreams: Flow<List<Stream>> =
        settings.customStreamsJson.map { json ->
            parseArray(runCatching { JSONArray(json) }.getOrNull())
        }

    suspend fun load() {
        if (bundled.value.isEmpty()) {
            bundled.value = withContext(Dispatchers.IO) { readBundled() }
        }
        refreshIfStale()
    }

    /**
     * Pulls a fresh registry if the cached one has aged out.
     *
     * Six hours matches the workflow's schedule — checking more often only
     * costs battery to re-download something that hasn't changed. A failure
     * is silent by design: the cached registry is still perfectly good, and
     * "couldn't refresh the list" is not news the user needs while they are
     * trying to look at a river.
     */
    suspend fun refreshIfStale(force: Boolean = false) {
        val fetchedAt = settings.remoteStreamsFetchedAt.first()
        val age = System.currentTimeMillis() - fetchedAt
        if (!force && fetchedAt > 0L && age < REFRESH_INTERVAL_MS) return

        val json = registryClient.fetch() ?: return

        // Only replace the cache if the download actually parses into
        // streams. A truncated response or an HTML error page must never
        // blank out a registry that was working a moment ago.
        if (parseRegistry(json).isNotEmpty()) {
            settings.setRemoteStreamsJson(json)
        }
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
        parseRegistry(raw)
    }.getOrDefault(emptyList())

    /** Parses a `{ "streams": [...] }` document. Empty on anything unexpected. */
    private fun parseRegistry(json: String): List<Stream> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            parseArray(JSONObject(json).optJSONArray("streams"))
        }.getOrDefault(emptyList())
    }

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

        /** Matches the refresh cadence in .github/workflows/refresh-streams.yml. */
        const val REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}
