package dev.carince.eraser.feed

import dev.carince.eraser.ics.IcsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class FeedRepositoryTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun `returns parsed items from the feed`() {
        val repository = FeedRepository(
            source = IcsFeedSource { feed },
            parser = IcsParser(),
        )

        val result = repository.load("https://school.example.edu/feed.ics", zone)

        assertTrue(result is FeedResult.Success)
        val items = (result as FeedResult.Success).items
        assertEquals(1, items.size)
        assertEquals("event-1", items.single().id)
        assertEquals("Problem Set 4", items.single().title)
    }

    @Test
    fun `surfaces a source error as a failure`() {
        val repository = FeedRepository(
            source = IcsFeedSource { throw FeedException("Blackboard returned HTTP 403.") },
            parser = IcsParser(),
        )

        val result = repository.load("https://school.example.edu/feed.ics", zone)

        assertEquals(FeedResult.Failure("Blackboard returned HTTP 403."), result)
    }

    @Test
    fun `maps rejected tokens to an actionable message`() {
        assertEquals(
            "The calendar link was rejected. Copy a fresh link from Blackboard.",
            feedErrorMessage(403),
        )
        assertEquals(
            "The calendar link was rejected. Copy a fresh link from Blackboard.",
            feedErrorMessage(401),
        )
    }

    @Test
    fun `maps other status codes to the status`() {
        assertEquals("Blackboard returned HTTP 500.", feedErrorMessage(500))
        assertEquals("That calendar link no longer exists.", feedErrorMessage(404))
    }

    private val feed = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Eraser//Test//EN
        BEGIN:VEVENT
        UID:event-1
        SUMMARY:Problem Set 4
        DTSTART:20261110T235900Z
        CATEGORIES:CS 3410
        END:VEVENT
        END:VCALENDAR
    """.trimIndent()
}
