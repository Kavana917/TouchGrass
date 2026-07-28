package com.touchgrass.app.core.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamLinkParserTest {

    private fun ok(input: String): StreamLinkParser.Result.Ok =
        StreamLinkParser.parse(input) as StreamLinkParser.Result.Ok

    @Test
    fun `channel url gives a channel source`() {
        val result = ok("https://www.youtube.com/channel/UC-2KSeUU5SMCX6XLRD-AEvw")
        assertEquals(StreamSource.YOUTUBE_CHANNEL, result.source)
        assertEquals("UC-2KSeUU5SMCX6XLRD-AEvw", result.ref)
    }

    @Test
    fun `channel url with live suffix still works`() {
        val result = ok("https://www.youtube.com/channel/UCLA_DiR1FfKNvjuUpBHmylQ/live")
        assertEquals(StreamSource.YOUTUBE_CHANNEL, result.source)
        assertEquals("UCLA_DiR1FfKNvjuUpBHmylQ", result.ref)
    }

    @Test
    fun `bare channel id is accepted`() {
        val result = ok("UC-2KSeUU5SMCX6XLRD-AEvw")
        assertEquals(StreamSource.YOUTUBE_CHANNEL, result.source)
    }

    @Test
    fun `watch url gives a video source`() {
        val result = ok("https://www.youtube.com/watch?v=vytmBNhc9ig")
        assertEquals(StreamSource.YOUTUBE, result.source)
        assertEquals("vytmBNhc9ig", result.ref)
    }

    @Test
    fun `watch url with extra params still works`() {
        val result = ok("https://www.youtube.com/watch?feature=share&v=vytmBNhc9ig&t=30")
        assertEquals("vytmBNhc9ig", result.ref)
    }

    @Test
    fun `short link works`() {
        assertEquals("vytmBNhc9ig", ok("https://youtu.be/vytmBNhc9ig").ref)
    }

    @Test
    fun `live url works`() {
        assertEquals("uwXgcTc8oY8", ok("https://www.youtube.com/live/uwXgcTc8oY8").ref)
    }

    @Test
    fun `hls manifest is accepted directly`() {
        val url = "https://example.com/stream/playlist.m3u8"
        val result = ok(url)
        assertEquals(StreamSource.HLS, result.source)
        assertEquals(url, result.ref)
    }

    @Test
    fun `handle urls explain what to do instead`() {
        val result = StreamLinkParser.parse("https://www.youtube.com/@ExploreLiveNatureCams")
        result as StreamLinkParser.Result.Error
        assertTrue(result.message.contains("/channel/"))
    }

    @Test
    fun `nonsense is rejected`() {
        assertTrue(StreamLinkParser.parse("hello there") is StreamLinkParser.Result.Error)
    }

    @Test
    fun `empty input is rejected`() {
        assertTrue(StreamLinkParser.parse("   ") is StreamLinkParser.Result.Error)
    }
}
