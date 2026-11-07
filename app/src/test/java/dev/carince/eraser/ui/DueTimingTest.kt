package dev.carince.eraser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class DueTimingTest {
    private val zone = ZoneId.of("America/New_York")
    private val now = Instant.parse("2026-11-07T17:00:00Z")
    private val locale = Locale.US

    @Test
    fun `formats a deadline later today in hours`() {
        val result = relativeDue(Instant.parse("2026-11-07T20:00:00Z"), true, now, zone, locale)

        assertEquals("3h", result.label)
        assertEquals(DueUrgency.SOON, result.urgency)
    }

    @Test
    fun `formats a deadline in minutes`() {
        val result = relativeDue(Instant.parse("2026-11-07T17:20:00Z"), true, now, zone, locale)

        assertEquals("20m", result.label)
        assertEquals(DueUrgency.SOON, result.urgency)
    }

    @Test
    fun `formats tomorrow`() {
        val result = relativeDue(Instant.parse("2026-11-08T19:00:00Z"), true, now, zone, locale)

        assertEquals("Tomorr.", result.label)
        assertEquals(DueUrgency.NORMAL, result.urgency)
    }

    @Test
    fun `formats a weekday later this week`() {
        val result = relativeDue(Instant.parse("2026-11-10T14:00:00Z"), true, now, zone, locale)

        assertEquals("Tue", result.label)
    }

    @Test
    fun `formats a date beyond this week`() {
        val result = relativeDue(Instant.parse("2026-11-17T14:00:00Z"), true, now, zone, locale)

        assertEquals("Nov 17", result.label)
    }

    @Test
    fun `formats an overdue deadline in days`() {
        val result = relativeDue(Instant.parse("2026-11-06T14:00:00Z"), true, now, zone, locale)

        assertEquals("1d late", result.label)
        assertEquals(DueUrgency.OVERDUE, result.urgency)
    }

    @Test
    fun `formats an overdue deadline in hours`() {
        val result = relativeDue(Instant.parse("2026-11-07T14:00:00Z"), true, now, zone, locale)

        assertEquals("3h late", result.label)
        assertEquals(DueUrgency.OVERDUE, result.urgency)
    }

    @Test
    fun `formats all-day deadlines by date`() {
        val today = startOfDay("2026-11-07")
        val tomorrow = startOfDay("2026-11-08")
        val later = startOfDay("2026-11-10")

        assertEquals("Today", relativeDue(today, false, now, zone, locale).label)
        assertEquals("Tomorr.", relativeDue(tomorrow, false, now, zone, locale).label)
        assertEquals("Tue", relativeDue(later, false, now, zone, locale).label)
    }

    @Test
    fun `groups timed deadlines`() {
        assertEquals(DueBucket.OVERDUE, bucketFor(Instant.parse("2026-11-07T16:00:00Z"), true, now, zone))
        assertEquals(DueBucket.TODAY, bucketFor(Instant.parse("2026-11-07T20:00:00Z"), true, now, zone))
        assertEquals(DueBucket.THIS_WEEK, bucketFor(Instant.parse("2026-11-10T14:00:00Z"), true, now, zone))
        assertEquals(DueBucket.LATER, bucketFor(Instant.parse("2026-11-17T14:00:00Z"), true, now, zone))
    }

    @Test
    fun `groups all-day deadlines by calendar date`() {
        assertEquals(DueBucket.OVERDUE, bucketFor(startOfDay("2026-11-06"), false, now, zone))
        assertEquals(DueBucket.TODAY, bucketFor(startOfDay("2026-11-07"), false, now, zone))
        assertEquals(DueBucket.THIS_WEEK, bucketFor(startOfDay("2026-11-08"), false, now, zone))
        assertEquals(DueBucket.LATER, bucketFor(startOfDay("2026-11-17"), false, now, zone))
    }

    @Test
    fun `keeps recently overdue items visible`() {
        assertTrue(isActionable(startOfDay("2026-11-06"), now, zone))
        assertTrue(isActionable(startOfDay("2026-10-24"), now, zone))
    }

    @Test
    fun `hides items older than the overdue window`() {
        assertFalse(isActionable(startOfDay("2026-10-23"), now, zone))
        assertFalse(isActionable(startOfDay("2025-11-13"), now, zone))
    }

    private fun startOfDay(date: String): Instant =
        java.time.LocalDate.parse(date).atStartOfDay(zone).toInstant()
}
