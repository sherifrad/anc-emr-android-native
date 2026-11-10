package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
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

    @Test
    fun pf4_errorsAreSpecific() {
        assertEquals(
            setOf(PatientValidationError.OUTCOME_METHOD_MISMATCH),
            validInput(outcome = "EarlyPregnancyLoss", birthMethod = "Vaginal").validationErrors(),
        )
        assertEquals(setOf(PatientValidationError.INVALID_PREGNANCY_TYPE), validInput(pregnancyType = "Twins").validationErrors())
        assertEquals(setOf(PatientValidationError.INVALID_DELIVERY_MODE), validInput(deliveryBy = "Cesarean").validationErrors())
        assertEquals(setOf(PatientValidationError.INVALID_PREGNANCY_OUTCOME), validInput(outcome = "livebirth").validationErrors())
        assertEquals(setOf(PatientValidationError.INVALID_BIRTH_METHOD), validInput(outcome = "LiveBirth", birthMethod = "CS").validationErrors())
        assertEquals(setOf(PatientValidationError.INVALID_LOSS_MANAGEMENT), validInput(outcome = "EarlyPregnancyLoss", lossManagement = "x").validationErrors())
        assertEquals(setOf(PatientValidationError.INVALID_TERMINATION_METHOD), validInput(outcome = "TerminationOfPregnancy", terminationMethod = "x").validationErrors())
    }

    @Test
    fun pf2_placeAndDoctorRuleIsOneFunctionUsedByValidation() {
        assertTrue(PreviousPregnancyFormRules.placeAndDoctorRequired(isNew = true, originalPlaceAndDoctor = null))
        assertFalse(PreviousPregnancyFormRules.placeAndDoctorRequired(isNew = false, originalPlaceAndDoctor = null))
        assertFalse(PreviousPregnancyFormRules.placeAndDoctorRequired(isNew = false, originalPlaceAndDoctor = "  "))
        assertTrue(PreviousPregnancyFormRules.placeAndDoctorRequired(isNew = false, originalPlaceAndDoctor = "Clinic"))
        val required = validInput().copy(placeAndDoctorRequired = true)
        assertEquals(setOf(PatientValidationError.PLACE_AND_DOCTOR_REQUIRED), required.validationErrors())
        assertEquals(setOf(PatientValidationError.PLACE_AND_DOCTOR_REQUIRED), required.copy(placeAndDoctor = "   ").validationErrors())
        assertTrue(required.copy(placeAndDoctor = "Clinic A").validationErrors().isEmpty())
        assertTrue(validInput().validationErrors().isEmpty())
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
