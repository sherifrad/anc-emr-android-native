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

    @Test
    fun editorChoiceMapsStoredStatusesToOnOrStopped() {
        assertEquals(MedicationOnOff.ON, medicationEditorChoice("Active"))
        assertEquals(MedicationOnOff.ON, medicationEditorChoice(""))
        assertEquals(MedicationOnOff.ON, medicationEditorChoice(MEDICATION_STATUS_NON_COMPLIANT))
        assertEquals(MedicationOnOff.STOPPED, medicationEditorChoice("Completed"))
        assertEquals(MedicationOnOff.STOPPED, medicationEditorChoice("Discontinued"))
        assertEquals(null, medicationEditorChoice("something else"))
    }

    @Test
    fun tappingTheMatchingChoiceKeepsTheStoredStatus() {
        assertEquals("Completed", medicationStatusAfterChoice("Completed", MedicationOnOff.STOPPED))
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, medicationStatusAfterChoice(MEDICATION_STATUS_NON_COMPLIANT, MedicationOnOff.ON))
        assertEquals("Active", medicationStatusAfterChoice("Active", MedicationOnOff.ON))
        assertEquals("Discontinued", medicationStatusAfterChoice("Discontinued", MedicationOnOff.STOPPED))
    }

    @Test
    fun tappingTheOtherChoiceStoresActiveOrDiscontinued() {
        assertEquals("Active", medicationStatusAfterChoice("Completed", MedicationOnOff.ON))
        assertEquals("Active", medicationStatusAfterChoice("Discontinued", MedicationOnOff.ON))
        assertEquals("Discontinued", medicationStatusAfterChoice("Active", MedicationOnOff.STOPPED))
        assertEquals("Discontinued", medicationStatusAfterChoice(MEDICATION_STATUS_NON_COMPLIANT, MedicationOnOff.STOPPED))
        assertEquals("Active", medicationStatusAfterChoice("something else", MedicationOnOff.ON))
    }

    @Test
    fun uiShowsOnlyActiveOrStopped() {
        assertEquals("Active", medicationStatusLabel("Active"))
        assertEquals("Active", medicationStatusLabel(MEDICATION_STATUS_NON_COMPLIANT))
        assertEquals("Stopped", medicationStatusLabel("Completed"))
        assertEquals("Stopped", medicationStatusLabel("Discontinued"))
    }

    @Test
    fun instructionsJoinDoseRouteAndNoteAndLeaveOutMissingParts() {
        assertEquals("500 mg · Oral · 1 tab twice daily", medicationInstructions("500 mg", "Oral", "1 tab twice daily"))
        assertEquals("500 mg · Oral", medicationInstructions("500 mg", "Oral", null))
        assertEquals("Oral", medicationInstructions(" ", "Oral", ""))
        assertEquals("", medicationInstructions(null, null, "  "))
    }
}
