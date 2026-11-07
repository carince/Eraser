package dev.carince.eraser.ics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CourseCodeTest {
    @Test
    fun `finds a code glued to digits`() {
        assertEquals("MATH181", courseCodeFromSummary("MATH181 CO1 ASSIGNMENT #1"))
        assertEquals("MATH170", courseCodeFromSummary("MATH170 CO1 Final Exam"))
    }

    @Test
    fun `finds a code separated by a space or hyphen`() {
        assertEquals("CS 3410", courseCodeFromSummary("CS 3410 Problem Set 4"))
        assertEquals("MATH-165", courseCodeFromSummary("MATH-165 Final Exam"))
    }

    @Test
    fun `ignores one and two digit suffixes`() {
        assertNull(courseCodeFromSummary("CO1 Exam"))
        assertNull(courseCodeFromSummary("SA1: Long Quiz 1"))
        assertNull(courseCodeFromSummary("M1 ASSESSMENT 1"))
        assertNull(courseCodeFromSummary("AT10"))
        assertNull(courseCodeFromSummary("METHOD 25"))
        assertNull(courseCodeFromSummary("JULY 01"))
    }

    @Test
    fun `ignores lowercase words`() {
        assertNull(courseCodeFromSummary("final exam review"))
    }
}
