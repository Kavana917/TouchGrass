package com.touchgrass.app.core.essay

/**
 * Decides whether an essay counts.
 *
 * WHAT IS DELIBERATELY NOT CHECKED (app_plan.md §6.6, risk 4): quality,
 * relevance to the prompt, grammar, spelling. The moment we grade quality we
 * have to define quality, and the app becomes a hostile teacher marking your
 * homework at midnight. The toll is *effort*, and effort is adequately
 * measured by "you typed 150 words by hand".
 */
object EssayValidator {

    /** Cheap guard against `word word word word...`. */
    const val MIN_UNIQUE_RATIO = 0.40

    data class Result(
        val valid: Boolean,
        val wordCount: Int,
        val uniqueRatio: Double,
        val message: String?
    )

    fun countWords(text: String): Int =
        text.trim().split(WHITESPACE).count { it.isNotBlank() }

    fun uniqueRatio(text: String): Double {
        val words = text.lowercase()
            .split(WHITESPACE)
            .map { it.trim(*PUNCTUATION) }
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return 0.0
        return words.toSet().size.toDouble() / words.size
    }

    fun validate(text: String, requiredWords: Int): Result {
        val wordCount = countWords(text)
        val ratio = uniqueRatio(text)

        val message = when {
            wordCount < requiredWords ->
                "${requiredWords - wordCount} more to go."

            ratio < MIN_UNIQUE_RATIO ->
                "That's a lot of repeated words. Try writing a bit more freely."

            // Catches one enormous token with no spaces — technically long
            // enough, obviously not sentences.
            !text.contains(' ') ->
                "That doesn't look like sentences yet."

            else -> null
        }

        return Result(
            valid = message == null,
            wordCount = wordCount,
            uniqueRatio = ratio,
            message = message
        )
    }

    private val WHITESPACE = Regex("\\s+")
    private val PUNCTUATION = charArrayOf(
        '.', ',', '!', '?', ';', ':', '"', '\'', '(', ')', '—', '-'
    )
}
