package com.touchgrass.app.core.feed

import android.content.Context
import com.touchgrass.app.core.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the curated stream registry.
 *
 * Right now this reads the copy bundled in the APK. Phase 7 adds fetching a
 * remote `/v1/streams.json` and caching it, so a dead stream can be swapped
 * without shipping an app update — but the bundled copy always stays as the
 * fallback, because the Live Feed is the alternative offered to someone who
 * has just been told they can't use Instagram. It failing at that moment
 * would be the worst possible time.
 */
@Singleton
class StreamRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {

    private val _streams = MutableStateFlow<List<Stream>>(emptyList())
    val streams = _streams.asStateFlow()

    val favourites: Flow<Set<String>> = settings.favouriteStreams

    suspend fun load() {
        if (_streams.value.isNotEmpty()) return
        _streams.value = withContext(Dispatchers.IO) { readBundled() }
    }

    suspend fun toggleFavourite(streamId: String) {
        settings.toggleFavouriteStream(streamId)
    }

    private fun readBundled(): List<Stream> = runCatching {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        parse(raw)
    }.getOrDefault(emptyList())

    private fun parse(raw: String): List<Stream> {
        val root = JSONObject(raw)
        val array = root.optJSONArray("streams") ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            runCatching {
                Stream(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    place = obj.optString("place"),
                    lat = obj.optDouble("lat", 0.0),
                    lng = obj.optDouble("lng", 0.0),
                    category = StreamCategory.fromName(obj.optString("category")),
                    moods = obj.optJSONArray("moods")?.let { moods ->
                        (0 until moods.length()).mapNotNull {
                            StreamMood.fromName(moods.optString(it))
                        }
                    } ?: emptyList(),
                    source = StreamSource.fromName(obj.optString("source")),
                    streamRef = obj.getString("streamRef"),
                    hasAudio = obj.optBoolean("hasAudio", true),
                    timezone = obj.optString("timezone").ifBlank { null },
                    attribution = obj.optString("attribution").ifBlank { null },
                    lastVerified = obj.optString("lastVerified").ifBlank { null }
                )
                // One malformed entry must not take the whole registry down.
            }.getOrNull()
        }
    }

    private companion object {
        const val ASSET_NAME = "streams.json"
    }
}
