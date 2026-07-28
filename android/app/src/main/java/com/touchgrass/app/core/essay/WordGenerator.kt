package com.touchgrass.app.core.essay

import com.touchgrass.app.core.data.db.EssayDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Picks the essay prompt.
 *
 * Avoids anything used recently, so you can't get "lighthouse" twice in a
 * week and start reusing a paragraph. If every word has somehow been used
 * recently it falls back to the full list rather than failing — a repeated
 * word is a much smaller problem than a screen that won't load.
 */
@Singleton
class WordGenerator @Inject constructor(
    private val essayDao: EssayDao
) {
    suspend fun next(): String {
        val recent = runCatching { essayDao.recentWords(RECENT_WINDOW) }
            .getOrDefault(emptyList())
            .toSet()

        val candidates = WordList.WORDS.filterNot { it in recent }
        val pool = candidates.ifEmpty { WordList.WORDS }
        return pool[Random.nextInt(pool.size)]
    }

    private companion object {
        /**
         * How far back to look for repeats. Comfortably larger than the
         * number of essays anyone writes in a day, small enough that the
         * pool never empties.
         */
        const val RECENT_WINDOW = 40
    }
}
