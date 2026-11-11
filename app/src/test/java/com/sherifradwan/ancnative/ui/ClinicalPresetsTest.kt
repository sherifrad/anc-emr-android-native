package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClinicalPresetsTest {
    @Test
    fun commonMedicationFrequenciesAreTheFiveInOrder() {
        assertEquals(
            listOf("Once daily", "Twice daily", "Three times daily", "At bedtime", "As needed (PRN)"),
            COMMON_MEDICATION_FREQUENCIES,
        )
    }

    @Test
    fun everyFixedFrequencyHasItsOwnLabelAndFreeTextHasNone() {
        assertEquals(
            listOf(
                R.string.medication_frequency_once_daily,
                R.string.medication_frequency_twice_daily,
                R.string.medication_frequency_three_times_daily,
                R.string.medication_frequency_at_bedtime,
                R.string.medication_frequency_as_needed,
            ),
            COMMON_MEDICATION_FREQUENCIES.map { medicationFrequencyLabelRes(it) },
        )
        assertNull(medicationFrequencyLabelRes("Every 12 hours"))
        assertNull(medicationFrequencyLabelRes(""))
        assertNull("the match is exact, not case-folded", medicationFrequencyLabelRes("once daily"))
    }
}
