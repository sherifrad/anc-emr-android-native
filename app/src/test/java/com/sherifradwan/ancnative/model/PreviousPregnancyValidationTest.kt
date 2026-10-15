package com.sherifradwan.ancnative.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviousPregnancyValidationTest {
    @Test
    fun explicitlyUnknownClinicalFieldsAreAcceptedForClinicianEntry() {
        assertTrue(validInput().validationErrors().isEmpty())
    }

    @Test
    fun clinicianEntryRequiresExplicitClinicianId() {
        assertFalse(validInput(clinicianId = "").validationErrors().isEmpty())
    }

    @Test
    fun outcomeOnlyAcceptsItsCompatibleConditionalMethod() {
        assertTrue(validInput(outcome = "LiveBirth", birthMethod = "Vaginal").validationErrors().isEmpty())
        assertTrue(validInput(outcome = "Stillbirth", birthMethod = "Cesarean").validationErrors().isEmpty())
        assertTrue(
            validInput(
                outcome = "EarlyPregnancyLoss",
                lossManagement = "Medical",
            ).validationErrors().isEmpty(),
        )
        assertTrue(
            validInput(
                outcome = "TerminationOfPregnancy",
                terminationMethod = "Hysterotomy",
            ).validationErrors().isEmpty(),
        )
        assertFalse(
            validInput(
                outcome = "EarlyPregnancyLoss",
                birthMethod = "Vaginal",
                lossManagement = "Medical",
            ).validationErrors().isEmpty(),
        )
        assertFalse(
            validInput(
                outcome = "LiveBirth",
                birthMethod = "Vaginal",
                terminationMethod = "Medical",
            ).validationErrors().isEmpty(),
        )
    }

    @Test
    fun controlledVocabulariesRejectUnapprovedValues() {
        assertTrue(validInput(pregnancyType = "Twin", deliveryBy = "CS").validationErrors().isEmpty())
        assertFalse(validInput(pregnancyType = "Twins").validationErrors().isEmpty())
        assertFalse(validInput(deliveryBy = "Cesarean").validationErrors().isEmpty())
        assertFalse(validInput(outcome = "livebirth").validationErrors().isEmpty())
    }

    private fun validInput(
        pregnancyType: String? = null,
        deliveryBy: String? = null,
        outcome: String? = null,
        birthMethod: String? = null,
        lossManagement: String? = null,
        terminationMethod: String? = null,
        clinicianId: String = "clinician-1",
    ) = PreviousPregnancyClinicalInput(
        pregnancyType = pregnancyType,
        deliveryBy = deliveryBy,
        outcome = outcome,
        birthMethod = birthMethod,
        lossManagement = lossManagement,
        terminationMethod = terminationMethod,
        provenanceKind = PatientVocabulary.CLINICIAN_ENTRY,
        clinicianId = clinicianId,
    )
}
