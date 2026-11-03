package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ClinicalConstantsTest {

    @Test
    fun activeStatusConstantKeepsStoredValue() {
        assertEquals("Active", MEDICATION_STATUS_ACTIVE)
    }

    @Test
    fun medicationStatusesAreTheFourStatusesInOrder() {
        assertEquals(
            listOf("Active", "Completed", "Discontinued", "Patient non-compliant"),
            ClinicalVocabulary.medicationStatuses,
        )
        assertEquals(MEDICATION_STATUS_ACTIVE, ClinicalVocabulary.medicationStatuses.first())
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, ClinicalVocabulary.medicationStatuses.last())
    }

    @Test
    fun labNamePresetsAreTheFourNamesInOrder() {
        assertEquals(
            listOf("Al Borg Labs", "El Mokhtabar", "Hospital Lab", "Alfa Labs"),
            ClinicalVocabulary.labNamePresets,
        )
    }
}
