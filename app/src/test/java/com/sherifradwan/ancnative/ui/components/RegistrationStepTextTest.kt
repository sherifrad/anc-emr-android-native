package com.sherifradwan.ancnative.ui.components

import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.RegistrationStep
import com.sherifradwan.ancnative.state.StepStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RegistrationStepTextTest {
    @Test
    fun every_step_maps_to_its_exact_title_resource() {
        assertEquals(R.string.registration_step_identity, RegistrationStep.IDENTITY.titleRes())
        assertEquals(R.string.dating, RegistrationStep.DATING.titleRes())
        assertEquals(R.string.obstetric_risk_section_title, RegistrationStep.OBSTETRIC.titleRes())
        assertEquals(R.string.registration_step_histories, RegistrationStep.HISTORIES.titleRes())
        assertEquals(R.string.previous_pregnancies, RegistrationStep.PREGNANCIES.titleRes())
        assertEquals(RegistrationStep.entries.size, RegistrationStep.entries.map { it.titleRes() }.toSet().size)
    }

    @Test
    fun every_status_maps_to_its_exact_word_resource() {
        assertEquals(R.string.done, StepStatus.DONE.labelRes())
        assertEquals(R.string.registration_status_skipped, StepStatus.SKIPPED.labelRes())
        assertEquals(R.string.registration_status_pending, StepStatus.PENDING.labelRes())
    }

    @Test
    fun every_status_has_its_pinned_distinct_glyph() {
        assertEquals("\u2713", StepStatus.DONE.glyph())
        assertEquals("\u2212", StepStatus.SKIPPED.glyph())
        assertEquals("\u25CB", StepStatus.PENDING.glyph())
    }
}
