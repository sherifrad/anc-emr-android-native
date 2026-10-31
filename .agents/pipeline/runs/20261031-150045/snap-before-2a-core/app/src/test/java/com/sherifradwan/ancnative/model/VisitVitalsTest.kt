package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Oracle Q84 (SPEC_FIX_R3.md, Round 3m): one parser for visit vitals, used by the
// save check, the editor hint and both visit displays. Runs against the production
// functions; until model/VisitVitals.kt exists this class cannot compile (planned).
class VisitVitalsTest {

    // parseBloodPressure ------------------------------------------------------

    @Test
    fun parseBloodPressureAcceptsTheDocumentedShapes() {
        assertEquals(BloodPressure(120, 80), parseBloodPressure("120/80"))
        assertEquals(BloodPressure(120, 80), parseBloodPressure("120 / 80"))
        assertEquals(BloodPressure(120, 80), parseBloodPressure("  120/80 "))
        assertEquals(BloodPressure(120, 80), parseBloodPressure("120 / 80 mmHg"))
        assertEquals(BloodPressure(120, 80), parseBloodPressure("120/80mmHg"))
        assertEquals(BloodPressure(120, 80), parseBloodPressure("120/80 MMHG"))
    }

    @Test
    fun parseBloodPressureRejectsBlankGarbageAndExtraTokens() {
        assertNull(parseBloodPressure(null))
        assertNull(parseBloodPressure(""))
        assertNull(parseBloodPressure("   "))
        assertNull(parseBloodPressure("abc"))
        assertNull(parseBloodPressure("12080"))
        assertNull(parseBloodPressure("120/80/90"))
        assertNull(parseBloodPressure("120/"))
        assertNull(parseBloodPressure("/80"))
        assertNull(parseBloodPressure("120.5/80"))
        assertNull(parseBloodPressure("120/80.5"))
        assertNull(parseBloodPressure("twelve/80"))
        assertNull(parseBloodPressure("120/80 mmHg today"))
    }

    @Test
    fun parseWeightAcceptsKgCommaAndSuffixedForms() {
        assertEquals(68.0, parseWeightKg("68")!!, 1e-9)
        assertEquals(68.5, parseWeightKg("68.5")!!, 1e-9)
        assertEquals(68.5, parseWeightKg("68,5")!!, 1e-9)
        assertEquals(68.5, parseWeightKg("  68,5 ")!!, 1e-9)
        assertEquals(70.0, parseWeightKg("70kg")!!, 1e-9)
        assertEquals(70.0, parseWeightKg("70 KG")!!, 1e-9)
        assertEquals(75.0, parseWeightKg("75.0 kg")!!, 1e-9)
        assertEquals(70.5, parseWeightKg("70.5Kg")!!, 1e-9)
        assertEquals(68.0, parseWeightKg("68 kilogram")!!, 1e-9)
        assertEquals(68.0, parseWeightKg("68 kilograms")!!, 1e-9)
        assertEquals(68.0, parseWeightKg("68 KILOGRAMS")!!, 1e-9)
    }

    @Test
    fun parseWeightRejectsTwoDecimalsBlankAndGarbage() {
        assertNull(parseWeightKg(null))
        assertNull(parseWeightKg(""))
        assertNull(parseWeightKg("   "))
        assertNull(parseWeightKg("68.55"))
        assertNull(parseWeightKg("68,55"))
        assertEquals(68.5, parseWeightKg("68,5 kg")!!, 1e-9)
        assertNull(parseWeightKg("abc"))
        assertNull(parseWeightKg("68 kg 5"))
        assertNull(parseWeightKg("68.5."))
    }

    // plausibility ------------------------------------------------------------

    @Test
    fun bloodPressurePlausibilityEdges60To260And30To160() {
        assertTrue(isPlausibleBloodPressure(BloodPressure(60, 50)))
        assertFalse(isPlausibleBloodPressure(BloodPressure(59, 50)))
        assertTrue(isPlausibleBloodPressure(BloodPressure(260, 100)))
        assertFalse(isPlausibleBloodPressure(BloodPressure(261, 100)))
        assertTrue(isPlausibleBloodPressure(BloodPressure(100, 30)))
        assertFalse(isPlausibleBloodPressure(BloodPressure(100, 29)))
        assertTrue(isPlausibleBloodPressure(BloodPressure(200, 160)))
        assertFalse(isPlausibleBloodPressure(BloodPressure(200, 161)))
        assertFalse(isPlausibleBloodPressure(BloodPressure(120, 120))) // systolic == diastolic
        assertFalse(isPlausibleBloodPressure(BloodPressure(80, 120))) // inverted
        assertTrue(isPlausibleBloodPressure(BloodPressure(120, 80)))
    }

    @Test
    fun weightPlausibilityEdges30To250() {
        assertTrue(isPlausibleWeightKg(30.0))
        assertFalse(isPlausibleWeightKg(29.9))
        assertTrue(isPlausibleWeightKg(250.0))
        assertFalse(isPlausibleWeightKg(250.1))
        assertTrue(isPlausibleWeightKg(68.5))
        assertFalse(isPlausibleWeightKg(5.0))
    }

    // bpLevel -----------------------------------------------------------------

    @Test
    fun bpLevelFollowsTheAcogTwoLevelRule() {
        assertEquals(BpLevel.NORMAL, bpLevel("139/89"))
        assertEquals(BpLevel.RAISED, bpLevel("140/80"))
        assertEquals(BpLevel.RAISED, bpLevel("120/90"))
        assertEquals(BpLevel.RAISED, bpLevel("159/109"))
        assertEquals(BpLevel.SEVERE, bpLevel("160/80"))
        assertEquals(BpLevel.SEVERE, bpLevel("120/110"))
        assertEquals(BpLevel.NORMAL, bpLevel("120/80"))
    }

    @Test
    fun bpLevelFallsBackToNormalForUnparseableOrBlankInput() {
        assertEquals(BpLevel.NORMAL, bpLevel(null))
        assertEquals(BpLevel.NORMAL, bpLevel(""))
        assertEquals(BpLevel.NORMAL, bpLevel("   "))
        assertEquals(BpLevel.NORMAL, bpLevel("abc"))
        assertEquals(BpLevel.NORMAL, bpLevel("12080"))
    }
}
