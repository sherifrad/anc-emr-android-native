package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GestationalDisplayTest {

    private fun gaResult(
        weeks: Int,
        days: Int,
        totalDays: Long = (weeks * 7 + days).toLong(),
        formatted: String = "${weeks}w ${days}d",
        trimester: Int = if (weeks < 14) 1 else if (weeks < 28) 2 else 3,
        isPostTerm: Boolean = weeks >= 42,
    ): GestationalAgeResult {
        return GestationalAgeResult(
            weeks = weeks,
            days = days,
            totalDays = totalDays,
            formatted = formatted,
            trimester = trimester,
            isPostTerm = isPostTerm,
        )
    }

    @Test
    fun testIsLateTermAndPostTerm() {
        // 40w 6d: neither late-term nor post-term
        val r40w6d = gaResult(40, 6)
        assertFalse(r40w6d.isLateTerm)
        assertFalse(r40w6d.isPostTerm)

        // 41w 0d: late-term true, post-term false
        val r41w0d = gaResult(41, 0)
        assertTrue(r41w0d.isLateTerm)
        assertFalse(r41w0d.isPostTerm)

        // 41w 6d: late-term true, post-term false
        val r41w6d = gaResult(41, 6)
        assertTrue(r41w6d.isLateTerm)
        assertFalse(r41w6d.isPostTerm)

        // 42w 0d: late-term false, post-term true
        val r42w0d = gaResult(42, 0)
        assertFalse(r42w0d.isLateTerm)
        assertTrue(r42w0d.isPostTerm)

        // Sweep weeks 0..45 to assert isLateTerm is true only at week 41 and never together with isPostTerm
        for (w in 0..45) {
            for (d in 0..6) {
                val res = gaResult(w, d)
                if (w == 41) {
                    assertTrue("Expected isLateTerm at ${w}w ${d}d", res.isLateTerm)
                    assertFalse("Expected not isPostTerm at ${w}w ${d}d", res.isPostTerm)
                } else {
                    assertFalse("Expected not isLateTerm at ${w}w ${d}d", res.isLateTerm)
                    if (w >= 42) {
                        assertTrue("Expected isPostTerm at ${w}w ${d}d", res.isPostTerm)
                    } else {
                        assertFalse("Expected not isPostTerm at ${w}w ${d}d", res.isPostTerm)
                    }
                }
                assertFalse(
                    "isLateTerm and isPostTerm must never both be true at ${w}w ${d}d",
                    res.isLateTerm && res.isPostTerm,
                )
            }
        }
    }

    @Test
    fun testProgressFraction() {
        val r0 = gaResult(0, 0, totalDays = 0L)
        assertEquals(0f, r0.progressFraction, 0.0001f)

        val r140 = gaResult(20, 0, totalDays = 140L)
        assertEquals(0.5f, r140.progressFraction, 0.0001f)

        val r280 = gaResult(40, 0, totalDays = 280L)
        assertEquals(1f, r280.progressFraction, 0.0001f)

        val r320 = gaResult(45, 5, totalDays = 320L)
        assertEquals(1f, r320.progressFraction, 0.0001f)

        // Clamped to 0f on negative totalDays
        val rNeg = gaResult(0, 0, totalDays = -5L)
        assertEquals(0f, rNeg.progressFraction, 0.0001f)
    }

    @Test
    fun testShortWeeksText() {
        val r21w5d = gaResult(21, 5)
        assertEquals("21+5 wks", r21w5d.shortWeeksText)

        val r0w0d = gaResult(0, 0)
        assertEquals("0+0 wks", r0w0d.shortWeeksText)

        val r40w0d = gaResult(40, 0)
        assertEquals("40+0 wks", r40w0d.shortWeeksText)

        val r41w6d = gaResult(41, 6)
        assertEquals("41+6 wks", r41w6d.shortWeeksText)
    }

    @Test
    fun testDaysToEdd() {
        // Positive: asOf is before EDD (10 days remaining)
        val eddFuture = LocalDate.of(2026, 5, 20)
        val asOfBefore = LocalDate.of(2026, 5, 10)
        assertEquals(10L, daysToEdd(eddFuture, asOfBefore))

        // Zero: asOf is the EDD itself (due today)
        val eddToday = LocalDate.of(2026, 6, 15)
        val asOfToday = LocalDate.of(2026, 6, 15)
        assertEquals(0L, daysToEdd(eddToday, asOfToday))

        // Negative: asOf is after EDD (5 days overdue)
        val eddPast = LocalDate.of(2026, 7, 10)
        val asOfAfter = LocalDate.of(2026, 7, 15)
        assertEquals(-5L, daysToEdd(eddPast, asOfAfter))

        // Across a leap day: 2028 is a leap year (2028-02-29)
        // 2028-02-27 to 2028-03-02 includes Feb 27, 28, 29, Mar 1, Mar 2 -> 4 days
        val eddAfterLeap = LocalDate.of(2028, 3, 2)
        val asOfBeforeLeap = LocalDate.of(2028, 2, 27)
        assertEquals(4L, daysToEdd(eddAfterLeap, asOfBeforeLeap))

        // Reverse across leap day
        assertEquals(-4L, daysToEdd(asOfBeforeLeap, eddAfterLeap))

        // Non-leap year 2026 has 3 days between Feb 27 and Mar 2
        assertEquals(3L, daysToEdd(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 2, 27)))
    }
}
