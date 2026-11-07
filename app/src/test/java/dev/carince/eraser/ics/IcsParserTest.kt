package dev.carince.eraser.ics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class IcsParserTest {
    private val parser = IcsParser()
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun `parses a timed UTC event`() {
        val items = parser.parse(
            feed(
                listOf(
                    "UID:event-1",
                    "SUMMARY:Problem Set 4",
                    "DTSTART:20261110T235900Z",
                    "CATEGORIES:CS 3410",
                ),
            ),
            zone,
        )

        assertEquals(1, items.size)
        val item = items.single()
        assertEquals("event-1", item.id)
        assertEquals("Problem Set 4", item.title)
        assertEquals("CS 3410", item.courseCode)
        assertEquals(Instant.parse("2026-11-10T23:59:00Z"), item.dueAt)
        assertTrue(item.dueHasTime)
    }

    @Test
    fun `honors the TZID of a timed event`() {
        val items = parser.parse(
            feed(
                listOf(
                    "UID:event-2",
                    "SUMMARY:Midterm paper draft",
                    "DTSTART;TZID=America/New_York:20261110T235900",
                ),
            ),
            zone,
        )

        assertEquals(Instant.parse("2026-11-11T04:59:00Z"), items.single().dueAt)
    }

    @Test
    fun `marks an all-day event as having no time`() {
        val items = parser.parse(
            feed(
                listOf(
                    "UID:event-3",
                    "SUMMARY:Read chapter 7",
                    "DTSTART;VALUE=DATE:20261112",
                ),
            ),
            zone,
        )

        val item = items.single()
        assertFalse(item.dueHasTime)
        assertEquals(LocalDate.of(2026, 11, 12).atStartOfDay(zone).toInstant(), item.dueAt)
    }

    @Test
    fun `unfolds long lines and unescapes text`() {
        val items = parser.parse(
            feed(
                listOf(
                    "UID:event-4",
                    "SUMMARY:Problem Set 4\\, part 2 and a long title that is folded across",
                    "  lines in the feed",
                    "DTSTART:20261110T235900Z",
                ),
            ),
            zone,
        )

        assertEquals(
            "Problem Set 4, part 2 and a long title that is folded across lines in the feed",
            items.single().title,
        )
    }

    @Test
    fun `skips events without a UID`() {
        val items = parser.parse(
            feed(
                listOf(
                    "SUMMARY:No stable identity",
                    "DTSTART:20261110T235900Z",
                ),
            ),
            zone,
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `applies the correct offset on both sides of a DST change`() {
        val items = parser.parse(
            feed(
                listOf(
                    "UID:before-dst",
                    "SUMMARY:Before",
                    "DTSTART;TZID=America/New_York:20260308T003000",
                ),
                listOf(
                    "UID:after-dst",
                    "SUMMARY:After",
                    "DTSTART;TZID=America/New_York:20260308T033000",
                ),
            ),
            zone,
        )

        assertEquals(Instant.parse("2026-03-08T05:30:00Z"), items[0].dueAt)
        assertEquals(Instant.parse("2026-03-08T07:30:00Z"), items[1].dueAt)
    }

    private fun feed(vararg events: List<String>): String = buildString {
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//Eraser//Test//EN")
        events.forEach { event ->
            appendLine("BEGIN:VEVENT")
            event.forEach(::appendLine)
            appendLine("END:VEVENT")
        }
        appendLine("END:VCALENDAR")
    }
}
