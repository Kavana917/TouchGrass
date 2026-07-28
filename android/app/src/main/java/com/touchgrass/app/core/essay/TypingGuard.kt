package com.touchgrass.app.core.essay

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Watches *how* text arrives, not what it says.
 *
 * THE HONEST FRAMING (app_plan.md §2.5): none of this stops someone who
 * genuinely wants to defeat it. They can retype an essay from another
 * screen, revoke usage access, or uninstall the app in ten seconds.
 *
 * The target isn't the adversary — it's the *reflex*. Every workaround
 * requires a deliberate, conscious decision, and a deliberate decision to
 * use Instagram is exactly what this app is trying to produce. So the guard
 * is built to make the lazy path impossible, not the determined one.
 *
 * Design consequence: never make the app feel like an opponent. No "NICE
 * TRY". Messages state what happened and let the user continue.
 */
class TypingGuard {

    private val intervals = mutableListOf<Long>()
    private var lastKeystrokeAt: Long = 0L
    private var startedAt: Long = 0L

    /** Set when a change was rejected, so the UI can explain why. */
    var lastRejection: String? = null
        private set

    val durationSeconds: Int
        get() = if (startedAt == 0L) 0
        else ((System.currentTimeMillis() - startedAt) / 1000).toInt()

    /**
     * Decides whether a text change is allowed.
     *
     * Returns true to accept, false to discard the change and keep the
     * previous value.
     *
     * The single most effective rule here is the bulk-insert check. It is a
     * backstop for paste routes we didn't anticipate — a keyboard's own
     * clipboard, autofill, an accessibility tool, a scripted input — rather
     * than a duplicate of the disabled paste menu.
     */
    fun accept(previous: String, next: String): Boolean {
        val now = System.currentTimeMillis()
        if (startedAt == 0L && next.isNotEmpty()) startedAt = now

        val delta = next.length - previous.length

        // Deleting is always fine, however much.
        if (delta <= 0) {
            lastRejection = null
            recordInterval(now)
            return true
        }

        if (delta > MAX_INSERT_CHARS) {
            lastRejection = "That looked like pasted text, so it wasn't counted. Keep going."
            return false
        }

        lastRejection = null
        recordInterval(now)
        return true
    }

    private fun recordInterval(now: Long) {
        if (lastKeystrokeAt != 0L) {
            val gap = now - lastKeystrokeAt
            // Ignore long thinking pauses — they say nothing about whether a
            // human is typing, and they'd swamp the variance calculation.
            if (gap in 1..MAX_TRACKED_GAP_MS) intervals.add(gap)
        }
        lastKeystrokeAt = now
    }

    /**
     * True if the typing rhythm looks machine-generated.
     *
     * Humans are erratic: they pause, burst, hesitate over spelling. A script
     * types with near-identical gaps. We measure the coefficient of variation
     * (spread relative to average) and flag anything implausibly regular.
     *
     * ⚠️ THIS IS A FLAG, NOT A BLOCK — deliberately.
     *
     * A false positive here would mean someone hand-typed 150 words and then
     * got told they cheated. That is a far worse outcome than letting an
     * automated essay through, and the app's whole posture is that it must
     * never feel like an opponent. So the signal is recorded on the essay and
     * nothing else happens to it.
     */
    fun looksAutomated(): Boolean {
        if (intervals.size < MIN_SAMPLES) return false

        val mean = intervals.average()
        if (mean <= 0) return false

        val variance = intervals.sumOf { val d = it - mean; d * d } / intervals.size
        val coefficientOfVariation = sqrt(variance) / mean

        return coefficientOfVariation < MIN_HUMAN_VARIATION
    }

    fun reset() {
        intervals.clear()
        lastKeystrokeAt = 0L
        startedAt = 0L
        lastRejection = null
    }

    private companion object {
        /**
         * Chars allowed in a single change event.
         *
         * Generous enough for swipe/glide typing, which commits a whole word
         * at once, and for a fast typist whose keystrokes coalesce. Tight
         * enough that a pasted sentence never gets through.
         */
        const val MAX_INSERT_CHARS = 15

        const val MIN_SAMPLES = 40
        const val MAX_TRACKED_GAP_MS = 3_000L

        /** Below this, the rhythm is too regular to be a person. */
        const val MIN_HUMAN_VARIATION = 0.08
    }
}
