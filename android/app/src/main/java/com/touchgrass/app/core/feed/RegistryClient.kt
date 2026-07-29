package com.touchgrass.app.core.feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the published stream registry.
 *
 * ⚠️ WHY THE REGISTRY IS REMOTE:
 *
 * A registry baked into the APK can only be corrected by shipping a new APK.
 * 24/7 streams restart and are reissued a new video ID constantly — that is
 * how the first registry died within days (commit edb57f6), and no amount of
 * careful curation fixes it, because the rot happens after the build.
 *
 * A scheduled job now rebuilds the registry every few hours
 * (.github/workflows/refresh-streams.yml) and every phone reads the result.
 * A stream that dies at noon is gone from everyone's app by the evening,
 * with no release, no review, and no user action.
 *
 * This is also the piece that makes the app free at any scale: this file is
 * the ONLY thing we serve. The video itself streams from YouTube's CDN
 * straight to the device and never touches us (tech_stack.md §5.1).
 */
@Singleton
class RegistryClient @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * The published registry as raw JSON, or null if it couldn't be fetched.
     *
     * Null is not an error worth surfacing: the caller keeps using its cached
     * copy, and failing to reach the network is the normal state of a phone
     * on a train.
     */
    suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(REGISTRY_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull()
    }

    private companion object {
        /**
         * Served straight from the repo.
         *
         * Fine for now, and a one-line change when it stops being fine: at a
         * few thousand daily users, move this to GitHub Pages or
         * `cdn.jsdelivr.net/gh/Kavana917/TouchGrass@main/server/streams.json`,
         * both free and both CDN-backed. Nothing else in the app changes.
         */
        const val REGISTRY_URL =
            "https://raw.githubusercontent.com/Kavana917/TouchGrass/main/server/streams.json"
    }
}
