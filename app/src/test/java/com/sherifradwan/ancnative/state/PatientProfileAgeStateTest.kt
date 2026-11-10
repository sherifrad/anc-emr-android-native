package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.storage.PatientCommandPreparation
import com.sherifradwan.ancnative.storage.SavePatientProfileCommand
import org.junit.Assert.assertEquals
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

    private fun draft(baseRevision: Long?, originalAge: String, values: PatientDraftValues) = PatientDraft(
        editorSessionId = "session-1",
        patientId = "00000000-0000-4000-8000-000000000501",
        baseRevision = baseRevision,
        createdAt = "2026-10-11T09:00:00Z",
        original = PatientDraftValues(caseId = "C-1", displayName = "P", ageText = originalAge),
        values = if (values.caseId.isEmpty()) values.copy(caseId = "C-1", displayName = "P") else values,
    )

    private fun prepare(state: AncState) =
        SavePatientProfileCommand.prepare(state, "00000000-0000-4000-8000-000000000502", "2026-10-11T10:00:00Z")

    private fun rejected(state: AncState): Set<PatientValidationError> =
        (prepare(state) as PatientCommandPreparation.Rejected).errors
}
