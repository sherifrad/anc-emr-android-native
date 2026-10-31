package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GestationalAgeEngineTest {

    @Test
    fun testLmpDatingCalculation() {
        val lmp = LocalDate.of(2026, 1, 1)
        val asOf = LocalDate.of(2026, 3, 12) // 70 days later = exactly 10 weeks
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = lmp.toString(),
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )

        val ga = GestationalAgeEngine.calculate(dating, asOf)
        assertNotNull(ga)
        assertEquals(10, ga!!.weeks)
        assertEquals(0, ga.days)
        assertEquals("10w 0d", ga.formatted)
        assertEquals(1, ga.trimester)
        assertFalse(ga.isPostTerm)

        val edd = GestationalAgeEngine.calculateEstimatedDueDate(dating)
        assertEquals(LocalDate.of(2026, 10, 8), edd) // Jan 1 + 280 days
    }

    @Test
    fun testIvfDay3EmbryoTransfer() {
        // Day 3 transfer on 2026-02-01
        // Initial GA at transfer = 17 days (2w 3d)
        // 28 days later (4 weeks) -> total GA = 17 + 28 = 45 days = 6w 3d
        val transferDate = LocalDate.of(2026, 2, 1)
        val asOf = transferDate.plusDays(28)
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = null,
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "IVF_TRANSFER",
            transferDate = transferDate.toString(),
            embryoAgeDays = 3,
            notes = null,
        )

        val ga = GestationalAgeEngine.calculate(dating, asOf)
        assertNotNull(ga)
        assertEquals(6, ga!!.weeks)
        assertEquals(3, ga.days)
        assertEquals("6w 3d", ga.formatted)
        assertEquals(1, ga.trimester)

        val edd = GestationalAgeEngine.calculateEstimatedDueDate(dating)
        assertEquals(transferDate.plusDays(263), edd) // Day 3 adds 263 days
    }

    @Test
    fun testIvfDay5BlastocystTransfer() {
        // Day 5 blastocyst transfer on 2026-02-01
        // Initial GA at transfer = 19 days (2w 5d)
        // 35 days later (5 weeks) -> total GA = 19 + 35 = 54 days = 7w 5d
        val transferDate = LocalDate.of(2026, 2, 1)
        val asOf = transferDate.plusDays(35)
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = null,
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "IVF_TRANSFER",
            transferDate = transferDate.toString(),
            embryoAgeDays = 5,
            notes = null,
        )

        val ga = GestationalAgeEngine.calculate(dating, asOf)
        assertNotNull(ga)
        assertEquals(7, ga!!.weeks)
        assertEquals(5, ga.days)
        assertEquals("7w 5d", ga.formatted)
        assertEquals(1, ga.trimester)

        val edd = GestationalAgeEngine.calculateEstimatedDueDate(dating)
        assertEquals(transferDate.plusDays(261), edd) // Day 5 adds 261 days
    }

    @Test
    fun testInvalidEmbryoAgeIsRejected() {
        val transferDate = LocalDate.of(2026, 2, 1)
        val asOf = transferDate.plusDays(35)
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = null,
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "IVF_TRANSFER",
            transferDate = transferDate.toString(),
            embryoAgeDays = 4, // Invalid: must be 3 or 5
            notes = null,
        )

        val ga = GestationalAgeEngine.calculate(dating, asOf)
        assertNull(ga)
    }

    @Test
    fun testPostTermGestationalAgeCalculation() {
        // EDD was 2026-09-01
        // As of 2026-09-17 (16 days past EDD = 42w 2d)
        val edd = LocalDate.of(2026, 9, 1)
        val asOf = LocalDate.of(2026, 9, 17)
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = null,
            ultrasoundEdd = edd.toString(),
            agreedEdd = null,
            datingMethod = "Ultrasound",
            notes = null,
        )

        val ga = GestationalAgeEngine.calculate(dating, asOf)
        assertNotNull(ga)
        assertEquals(42, ga!!.weeks)
        assertEquals(2, ga.days)
        assertTrue(ga.isPostTerm)
        assertEquals("42w 2d (Post-term)", ga.formatted)
        assertEquals(3, ga.trimester)
    }

    @Test
    fun testAgreedEddOverridesMethod() {
        val lmp = LocalDate.of(2026, 1, 1)
        val agreed = LocalDate.of(2026, 10, 20)
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = lmp.toString(),
            ultrasoundEdd = null,
            agreedEdd = agreed.toString(),
            datingMethod = "LMP",
            notes = null,
        )

        val edd = GestationalAgeEngine.calculateEstimatedDueDate(dating)
        assertEquals(agreed, edd)
    }

    @Test
    fun testPreConceptionReturnsNull() {
        val lmp = LocalDate.of(2026, 6, 1)
        val asOf = LocalDate.of(2026, 5, 1) // Before LMP
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = lmp.toString(),
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )

        val ga = GestationalAgeEngine.calculate(dating, asOf)
        assertNull(ga)
    }

    @Test
    fun testCalculateGestationalAgeAtWithStringDate() {
        val lmp = LocalDate.of(2026, 1, 1)
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = lmp.toString(),
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )

        val ga = GestationalAgeEngine.calculateGestationalAgeAt(dating, "2026-03-12")
        assertNotNull(ga)
        assertEquals(10, ga!!.weeks)
        assertEquals(0, ga.days)
        assertEquals("10w 0d", ga.displayText)
        assertTrue(ga.isPostConception)

        // Invalid date string
        assertNull(GestationalAgeEngine.calculateGestationalAgeAt(dating, "invalid-date"))
        assertNull(GestationalAgeEngine.calculateGestationalAgeAt(dating, null))
        assertNull(GestationalAgeEngine.calculateGestationalAgeAt(null, "2026-03-12"))
    }
}
