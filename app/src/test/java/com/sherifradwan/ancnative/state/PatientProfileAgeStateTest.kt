package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.storage.PatientCommandPreparation
import com.sherifradwan.ancnative.storage.SavePatientProfileCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientProfileAgeStateTest {
    private val legacy = DemoFixtures.patients[0].copy(ageText = null)
    private val withAge = DemoFixtures.patients[1].copy(ageText = "29")

    @Test
    fun legacyPatientWithoutAgeStillSavesAfterEditingAnotherField() {
        var state = AncState(patients = listOf(legacy))
        state = AncReducer.reduce(state, AncEvent.StartPatientEditor(legacy.id, legacy.id, "session-1"))
        state = AncReducer.reduce(state, AncEvent.PatientDraftChanged(PatientDraftField.CLINICAL_ATTENTION, "Review"))
        assertTrue(prepare(state) is PatientCommandPreparation.Ready)
    }

    @Test
    fun restoredCreateDraftWithBlankAgeIsRejected() {
        val state = AncState(patients = emptyList(), patientDraft = draft(null, "", PatientDraftValues()))
        assertEquals(setOf(PatientValidationError.AGE_REQUIRED), rejected(state))
    }

    @Test
    fun restoredEditDraftUsesStoredOriginalNotRestoredValues() {
        val legacyRestored = draft(2L, "", PatientDraftValues(caseId = "C-1", displayName = "P", ageText = ""))
        assertTrue(prepare(AncState(patients = listOf(legacy), patientDraft = legacyRestored)) is PatientCommandPreparation.Ready)
        val storedRestored = draft(2L, "", PatientDraftValues(caseId = "C-1", displayName = "P", ageText = "29"))
            .let { it.copy(original = it.original.copy(ageText = "29")) }
        val blanked = storedRestored.copy(values = storedRestored.values.copy(ageText = ""))
        assertEquals(
            setOf(PatientValidationError.AGE_REQUIRED),
            rejected(AncState(patients = listOf(withAge), patientDraft = blanked)),
        )
    }

    @Test
    fun ageZeroIsAcceptedBecauseItIsNotBlank() {
        val values = PatientDraftValues(caseId = "C-1", displayName = "P", ageText = "0")
        val state = AncState(patients = emptyList(), patientDraft = draft(null, "", values))
        assertTrue(prepare(state) is PatientCommandPreparation.Ready)
    }

    @Test
    fun legacyPatientWithoutAgeIsRejectedInTheIdentityStepAndStillSavesOutsideTheFlow() {
        val outside = AncReducer.reduce(
            AncReducer.reduce(
                AncState(patients = listOf(legacy)),
                AncEvent.StartPatientEditor(legacy.id, legacy.id, "session-1"),
            ),
            AncEvent.PatientDraftChanged(PatientDraftField.CLINICAL_ATTENTION, "Review"),
        )
        assertEquals(null, outside.registrationFlow)
        assertFalse(outside.forcesAgeRequired())
        assertTrue(prepare(outside, force = outside.forcesAgeRequired()) is PatientCommandPreparation.Ready)

        val inIdentity = AncReducer.reduce(
            AncReducer.reduce(
                AncState(patients = listOf(legacy)),
                AncEvent.Flow(FlowEvent.Begin(legacy.id, RegistrationStep.IDENTITY), "session-1", "pregnancy-1"),
            ),
            AncEvent.PatientDraftChanged(PatientDraftField.CLINICAL_ATTENTION, "Review"),
        )
        assertEquals(RegistrationStep.IDENTITY, inIdentity.registrationFlow?.current)
        assertTrue(inIdentity.forcesAgeRequired())
        assertEquals(
            setOf(PatientValidationError.AGE_REQUIRED),
            rejected(inIdentity, force = inIdentity.forcesAgeRequired()),
        )

        val inObstetric = inIdentity.copy(
            registrationFlow = inIdentity.registrationFlow!!.copy(current = RegistrationStep.OBSTETRIC),
        )
        assertFalse(inObstetric.forcesAgeRequired())
        assertTrue(prepare(inObstetric, force = inObstetric.forcesAgeRequired()) is PatientCommandPreparation.Ready)
    }

    @Test
    fun forcedAgeIsAcceptedWhenTheDraftHasOne() {
        val values = PatientDraftValues(caseId = "C-1", displayName = "P", ageText = "31")
        val state = AncState(patients = listOf(legacy), patientDraft = draft(2L, "", values))
        assertTrue(prepare(state, force = true) is PatientCommandPreparation.Ready)
    }

    private fun draft(baseRevision: Long?, originalAge: String, values: PatientDraftValues) = PatientDraft(
        editorSessionId = "session-1",
        patientId = "00000000-0000-4000-8000-000000000501",
        baseRevision = baseRevision,
        createdAt = "2026-10-11T09:00:00Z",
        original = PatientDraftValues(caseId = "C-1", displayName = "P", ageText = originalAge),
        values = if (values.caseId.isEmpty()) values.copy(caseId = "C-1", displayName = "P") else values,
    )

    private fun prepare(state: AncState, force: Boolean = false) = SavePatientProfileCommand.prepare(
        state,
        "00000000-0000-4000-8000-000000000502",
        "2026-10-11T10:00:00Z",
        forceAgeRequired = force,
    )

    private fun rejected(state: AncState, force: Boolean = false): Set<PatientValidationError> =
        (prepare(state, force) as PatientCommandPreparation.Rejected).errors
}
