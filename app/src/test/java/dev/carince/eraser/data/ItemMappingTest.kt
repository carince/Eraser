package dev.carince.eraser.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ItemMappingTest {
    @Test
    fun `round trips a timed item`() {
        val item = DueItem(
            id = "ics:event-1",
            title = "Problem Set 4",
            courseCode = "CS 3410",
            dueAt = Instant.parse("2026-11-10T23:59:00Z"),
            dueHasTime = true,
            rawDue = "2026-11-10T18:59:00",
        )

        val restored = item.toEntity(seenAt = 1_700_000_000_000).toDueItem()

        assertEquals(item, restored)
    }

    @Test
    fun `round trips an all-day item without a course`() {
        val item = DueItem(
            id = "ics:event-2",
            title = "Read chapter 7",
            courseCode = null,
            dueAt = Instant.parse("2026-11-12T04:00:00Z"),
            dueHasTime = false,
        )

        val entity = item.toEntity(seenAt = 42)
        val restored = entity.toDueItem()

        assertEquals(item, restored)
        assertEquals(42, entity.lastSeen)
        assertEquals(item.dueAt!!.toEpochMilli(), entity.dueAt)
    }

    @Test
    fun `keeps a null due date`() {
        val item = DueItem(
            id = "ics:event-3",
            title = "No date",
            courseCode = null,
            dueAt = null,
            dueHasTime = false,
        )

        assertNull(item.toEntity(seenAt = 1).toDueItem().dueAt)
    }
}
