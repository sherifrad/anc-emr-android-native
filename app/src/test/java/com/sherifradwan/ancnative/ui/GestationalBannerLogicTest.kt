package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class GestationalBannerLogicTest {

    @Test
    fun testEarlyPregnancyAtZeroWeeks() {
        // LMP: 2026-10-01, asOfDate: 2026-10-01
        val lmp = "2026-10-01"
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = lmp,
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )
        val asOf = LocalDate.of(2026, 10, 1)
        val result = dating.calculateGestationalAgeResult(asOf)
        val edd = dating.calculateEstimatedDueDate()

        assertNotNull(result)
        assertEquals(0, result!!.weeks)
        assertEquals(0, result.days)
        assertEquals(1, result.trimester)
        assertFalse(result.isPostTerm)

        val progress = (result.totalDays / 280f).coerceIn(0f, 1f)
        assertEquals(0f, progress, 0.001f)

        assertNotNull(edd)
        val daysDiff = ChronoUnit.DAYS.between(asOf, edd)
        assertEquals(280L, daysDiff)
        assertTrue(daysDiff > 0)
    }

    @Test
    fun testMidPregnancyAt28Weeks3Days() {
        // LMP: 2026-01-01, asOf: 2026-07-19 (199 days)
        val dating = DemoPatientDating(
            patientId = "P2",
            lmpDate = "2026-01-01",
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )
        val asOf = LocalDate.of(2026, 7, 19)
        val result = dating.calculateGestationalAgeResult(asOf)
        val edd = dating.calculateEstimatedDueDate()

        assertNotNull(result)
        assertEquals(28, result!!.weeks)
        assertEquals(3, result.days)
        assertEquals(3, result.trimester)
        assertFalse(result.isPostTerm)

        val progress = (result.totalDays / 280f).coerceIn(0f, 1f)
        assertEquals(199f / 280f, progress, 0.005f)

        assertNotNull(edd)
        val daysDiff = ChronoUnit.DAYS.between(asOf, edd)
        assertEquals(81L, daysDiff)
    }

    @Test
    fun testPostTermAt41Weeks() {
        // LMP: 2026-01-01, asOf: 2026-10-16 (288 days = 41w 1d)
        val dating = DemoPatientDating(
            patientId = "P3",
            lmpDate = "2026-01-01",
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )
        val asOf = LocalDate.of(2026, 10, 16)
        val result = dating.calculateGestationalAgeResult(asOf)
        val edd = dating.calculateEstimatedDueDate()

        assertNotNull(result)
        assertEquals(41, result!!.weeks)
        val isLateTerm = !result.isPostTerm && result.weeks == 41
        assertTrue(isLateTerm)

        val progress = (result.totalDays / 280f).coerceIn(0f, 1f)
        // Progress should clamp to 1.0f max
        assertEquals(1f, progress, 0.001f)

        assertNotNull(edd)
        val daysDiff = ChronoUnit.DAYS.between(asOf, edd)
        assertEquals(-8L, daysDiff)
        assertTrue(daysDiff < 0)

        // At 42+ weeks, engine flags isPostTerm
        val postTerm42 = dating.calculateGestationalAgeResult(LocalDate.of(2026, 10, 23))
        assertNotNull(postTerm42)
        assertTrue(postTerm42!!.isPostTerm)
        val isPostTerm = postTerm42.isPostTerm || postTerm42.weeks >= 42
        assertTrue(isPostTerm)
    }

    @Test
    fun testEddToday() {
        // EDD: 2026-10-08, asOf: 2026-10-08
        val dating = DemoPatientDating(
            patientId = "P4",
            lmpDate = null,
            ultrasoundEdd = "2026-10-08",
            agreedEdd = null,
            datingMethod = "ULTRASOUND",
            notes = null,
        )
        val asOf = LocalDate.of(2026, 10, 8)
        val result = dating.calculateGestationalAgeResult(asOf)
        val edd = dating.calculateEstimatedDueDate()

        assertNotNull(result)
        assertEquals(40, result!!.weeks)
        assertEquals(0, result.days)

        assertNotNull(edd)
        val daysDiff = ChronoUnit.DAYS.between(asOf, edd)
        assertEquals(0L, daysDiff)
    }

    @Test
    fun testMissingAndInvalidDating() {
        val nullDating: DemoPatientDating? = null
        assertNull(nullDating?.calculateGestationalAgeResult())
        assertNull(nullDating?.calculateEstimatedDueDate())

        val invalidDating = DemoPatientDating(
            patientId = "P5",
            lmpDate = "invalid-date",
            ultrasoundEdd = "garbage",
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )
        assertNull(invalidDating.calculateGestationalAgeResult())
        assertNull(invalidDating.calculateEstimatedDueDate())
    }

    @Test
    fun testPreLmpDateReturnsNull() {
        // Pre-LMP date: asOf is before LMP -> clinically invalid date
        val dating = DemoPatientDating(
            patientId = "P6",
            lmpDate = "2026-10-10",
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )
        val asOf = LocalDate.of(2026, 10, 5)
        val result = dating.calculateGestationalAgeResult(asOf)
        assertNull(result)

        // Math clamping verification
        val clampedProgress = (-5L / 280f).coerceIn(0f, 1f)
        assertEquals(0f, clampedProgress, 0.001f)
    }
}
