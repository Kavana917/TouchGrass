package com.touchgrass.app.core.essay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EssayValidatorTest {

    @Test
    fun `counts words separated by any whitespace`() {
        assertEquals(4, EssayValidator.countWords("one two\tthree\nfour"))
    }

    @Test
    fun `ignores leading and trailing whitespace`() {
        assertEquals(2, EssayValidator.countWords("   hello world   "))
    }

    @Test
    fun `empty text is zero words`() {
        assertEquals(0, EssayValidator.countWords("   "))
    }

    @Test
    fun `unique ratio ignores case and punctuation`() {
        // "the" three times, "cat" once -> 2 unique of 4
        val ratio = EssayValidator.uniqueRatio("The the, THE. cat")
        assertEquals(0.5, ratio, 0.001)
    }

    @Test
    fun `too short is rejected with a count of what is left`() {
        val result = EssayValidator.validate("one two three", requiredWords = 10)
        assertFalse(result.valid)
        assertTrue(result.message!!.contains("7"))
    }

    @Test
    fun `repeated single word is rejected even when long enough`() {
        val text = List(60) { "word" }.joinToString(" ")
        val result = EssayValidator.validate(text, requiredWords = 50)
        assertFalse(result.valid)
        assertTrue(result.message!!.contains("repeated"))
    }

    @Test
    fun `one long token with no spaces is rejected`() {
        val result = EssayValidator.validate("a".repeat(400), requiredWords = 1)
        assertFalse(result.valid)
    }

    @Test
    fun `varied prose of sufficient length is accepted`() {
        val text = (1..60).joinToString(" ") { "word$it" }
        val result = EssayValidator.validate(text, requiredWords = 50)
        assertTrue(result.message ?: "", result.valid)
        assertEquals(60, result.wordCount)
    }

    @Test
    fun `exactly the required count is enough`() {
        val text = (1..50).joinToString(" ") { "word$it" }
        assertTrue(EssayValidator.validate(text, requiredWords = 50).valid)
    }
}
