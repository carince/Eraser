package dev.carince.eraser.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedUrlTest {
    @Test
    fun `trims whitespace`() {
        assertEquals(
            "https://school.example.edu/webcal/abc.ics",
            normalizeFeedUrl("  https://school.example.edu/webcal/abc.ics \n"),
        )
    }

    @Test
    fun `converts webcal links to https`() {
        assertEquals(
            "https://school.example.edu/webcal/abc.ics",
            normalizeFeedUrl("webcal://school.example.edu/webcal/abc.ics"),
        )
    }

    @Test
    fun `keeps https links untouched`() {
        val url = "https://school.example.edu/webcal/abc.ics?token=x"
        assertEquals(url, normalizeFeedUrl(url))
    }

    @Test
    fun `rejects blank input`() {
        assertNull(normalizeFeedUrl(""))
        assertNull(normalizeFeedUrl("   "))
    }

    @Test
    fun `rejects unsupported schemes`() {
        assertNull(normalizeFeedUrl("ftp://school.example.edu/feed.ics"))
        assertNull(normalizeFeedUrl("school.example.edu/feed.ics"))
    }

    @Test
    fun `rejects a link without a host`() {
        assertNull(normalizeFeedUrl("https:///feed.ics"))
    }
}
