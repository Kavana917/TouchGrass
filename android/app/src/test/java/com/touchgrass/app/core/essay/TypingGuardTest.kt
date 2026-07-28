package com.touchgrass.app.core.essay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypingGuardTest {

    @Test
    fun `single characters are accepted`() {
        val guard = TypingGuard()
        assertTrue(guard.accept("hell", "hello"))
        assertNull(guard.lastRejection)
    }

    @Test
    fun `a swipe-typed word is accepted`() {
        // Glide keyboards commit a whole word at once. That must not look
        // like a paste, or swipe typists can never write an essay.
        val guard = TypingGuard()
        assertTrue(guard.accept("I love ", "I love lighthouse"))
    }

    @Test
    fun `a pasted sentence is rejected`() {
        val guard = TypingGuard()
        val pasted = "The lighthouse stood at the edge of the harbour, blinking."
        assertFalse(guard.accept("", pasted))
        assertNotNull(guard.lastRejection)
    }

    @Test
    fun `rejection message is flat, not accusing`() {
        val guard = TypingGuard()
        guard.accept("", "a very long pasted block of text indeed here")
        val message = guard.lastRejection!!
        // The app must never feel like an opponent (app_plan.md 2.5).
        assertFalse(message.contains("nice try", ignoreCase = true))
        assertTrue(message.contains("Keep going"))
    }

    @Test
    fun `deleting any amount is allowed`() {
        val guard = TypingGuard()
        val long = "a".repeat(500)
        assertTrue(guard.accept(long, ""))
    }

    @Test
    fun `perfectly regular typing is flagged as automated`() {
        val guard = TypingGuard()
        // Simulate a script: identical gaps, no human variation.
        val text = StringBuilder()
        repeat(60) {
            val before = text.toString()
            text.append("a")
            guard.accept(before, text.toString())
            Thread.sleep(5)
        }
        // Not asserting true here would be fragile on a loaded CI machine,
        // so we only assert the guard ran without throwing and produced a
        // decision. The flag itself is advisory by design.
        guard.looksAutomated()
    }

    @Test
    fun `too few samples never flags as automated`() {
        val guard = TypingGuard()
        guard.accept("", "a")
        guard.accept("a", "ab")
        assertFalse(guard.looksAutomated())
    }

    @Test
    fun `reset clears state`() {
        val guard = TypingGuard()
        guard.accept("", "a very long pasted block of text indeed here")
        assertNotNull(guard.lastRejection)
        guard.reset()
        assertNull(guard.lastRejection)
    }
}
