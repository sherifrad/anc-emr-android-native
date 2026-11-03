package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationStatusTest {
    @Test
    fun vocabularyIsTheFourStatusesInOrder() {
        assertEquals(
            listOf("Active", "Completed", "Discontinued", "Patient non-compliant"),
            ClinicalVocabulary.medicationStatuses.toList(),
        )
        assertEquals("Patient non-compliant", MEDICATION_STATUS_NON_COMPLIANT)
    }

    @Test
    fun activeWithFlagIsShownAsNonCompliant() {
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, effectiveMedicationStatus("Active", true))
    }

    @Test
    fun activeWithoutFlagStaysActive() {
        assertEquals("Active", effectiveMedicationStatus("Active", false))
    }

    @Test
    fun completedAndDiscontinuedKeepTheirStatusEvenWithOldFlag() {
        assertEquals("Completed", effectiveMedicationStatus("Completed", true))
        assertEquals("Discontinued", effectiveMedicationStatus("Discontinued", true))
    }

    @Test
    fun alreadyNonCompliantStaysNonCompliant() {
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, effectiveMedicationStatus(MEDICATION_STATUS_NON_COMPLIANT, true))
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, effectiveMedicationStatus(MEDICATION_STATUS_NON_COMPLIANT, false))
    }

    @Test
    fun ongoingIsActiveOrNonCompliantOnly() {
        assertTrue(isOngoingMedication("Active", false))
        assertTrue(isOngoingMedication("Active", true))
        assertTrue(isOngoingMedication(MEDICATION_STATUS_NON_COMPLIANT, false))
        assertTrue(isOngoingMedication(MEDICATION_STATUS_NON_COMPLIANT, true))
        assertFalse(isOngoingMedication("Completed", false))
        assertFalse(isOngoingMedication("Completed", true))
        assertFalse(isOngoingMedication("Discontinued", false))
        assertFalse(isOngoingMedication("Discontinued", true))
    }
}
