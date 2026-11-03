package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ClinicalPresetsTest {
    @Test
    fun commonMedicationFrequenciesAreTheFiveInOrder() {
        assertEquals(
            listOf("Once daily", "Twice daily", "Three times daily", "At bedtime", "As needed (PRN)"),
            COMMON_MEDICATION_FREQUENCIES,
        )
    }
}
