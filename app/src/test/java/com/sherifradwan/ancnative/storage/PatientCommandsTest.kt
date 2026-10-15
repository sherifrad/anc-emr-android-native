package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PatientDraft
import com.sherifradwan.ancnative.state.PatientDraftValues
import com.sherifradwan.ancnative.state.PregnancyDraftField
import com.sherifradwan.ancnative.state.PreviousPregnancyDraft
import com.sherifradwan.ancnative.state.PreviousPregnancyDraftValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientCommandsTest {
    @Test
    fun profilePreparationTrimsRequiredIdentityAndPreservesUnknownFields() {
        val values = PatientDraftValues(
            caseId = "  CASE-9  ",
            displayName = "  Synthetic Person  ",
            dateOfBirth = "   ",
            phone = "",
            gravida = "",
            parity = "  ",
            abortion = "",
        )
        val state = AncState(
            patients = emptyList(),
            patientDraft = PatientDraft(
                editorSessionId = "editor-1",
                patientId = PATIENT_ID,
                baseRevision = null,
                createdAt = null,
                original = PatientDraftValues(),
                values = values,
            ),
        )

        val prepared = SavePatientProfileCommand.prepare(state, OPERATION_ID, SUBMITTED_AT)
        assertTrue(prepared is PatientCommandPreparation.Ready)
        val command = (prepared as PatientCommandPreparation.Ready).command as SavePatientProfileCommand
        assertEquals("CASE-9", command.caseId)
        assertEquals("Synthetic Person", command.displayName)
        assertNull(command.dateOfBirth)
        assertNull(command.phone)
        assertNull(command.gravida)
        assertNull(command.parity)
        assertNull(command.abortion)
        assertTrue(command.isStructurallyValid())
    }

    @Test
    fun profilePreparationRejectsPartialOrOutOfRangeGpa() {
        val partial = profileState(PatientDraftValues(caseId = "CASE-1", displayName = "Patient", gravida = "1"))
        assertTrue(
            SavePatientProfileCommand.prepare(partial, OPERATION_ID, SUBMITTED_AT) is
                PatientCommandPreparation.Rejected,
        )

        val outOfRange = profileState(
            PatientDraftValues(
                caseId = "CASE-1",
                displayName = "Patient",
                gravida = "9007199254740992",
                parity = "0",
                abortion = "0",
            ),
        )
        assertTrue(
            SavePatientProfileCommand.prepare(outOfRange, OPERATION_ID, SUBMITTED_AT) is
                PatientCommandPreparation.Rejected,
        )
    }

    @Test
    fun outcomeChangeRetainsOnlyCompatibleConditionalMethod() {
        val starting = pregnancyState(
            PreviousPregnancyDraftValues(
                outcome = "LiveBirth",
                birthMethod = "Cesarean",
                lossManagement = "Surgical",
                terminationMethod = "Medical",
                clinicianId = "clinician-1",
            ),
        )

        val loss = AncReducer.reduce(
            starting,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "EarlyPregnancyLoss"),
        ).pregnancyDraft!!.values
        assertEquals("", loss.birthMethod)
        assertEquals("Surgical", loss.lossManagement)
        assertEquals("", loss.terminationMethod)

        val termination = AncReducer.reduce(
            starting,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "TerminationOfPregnancy"),
        ).pregnancyDraft!!.values
        assertEquals("", termination.birthMethod)
        assertEquals("", termination.lossManagement)
        assertEquals("Medical", termination.terminationMethod)

        val stillbirth = AncReducer.reduce(
            starting,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "Stillbirth"),
        ).pregnancyDraft!!.values
        assertEquals("Cesarean", stillbirth.birthMethod)
        assertEquals("", stillbirth.lossManagement)
        assertEquals("", stillbirth.terminationMethod)
    }

    @Test
    fun canonicalFingerprintMakesUuidAndPayloadReplayImmutable() {
        val command = SavePatientProfileCommand(
            operationUuid = OPERATION_ID,
            editorSessionId = "editor-1",
            patientId = PATIENT_ID,
            expectedPatientRevision = null,
            createdAt = SUBMITTED_AT,
            caseId = "CASE-1",
            displayName = "Synthetic Patient",
            dateOfBirth = null,
            phone = null,
            riskLevel = null,
            ageText = null,
            addressText = null,
            bloodGroupText = null,
            gravida = null,
            parity = null,
            abortion = null,
            obstetricNotes = null,
            fingerprint = "",
        ).withCanonicalFingerprint() as SavePatientProfileCommand

        assertTrue(command.isStructurallyValid())
        assertFalse(command.copy(displayName = "Different").isStructurallyValid())
        assertFalse(command.copy(operationUuid = "not-a-uuid").withCanonicalFingerprint().isStructurallyValid())
    }

    private fun profileState(values: PatientDraftValues) = AncState(
        patients = emptyList(),
        patientDraft = PatientDraft(
            editorSessionId = "editor-1",
            patientId = PATIENT_ID,
            baseRevision = null,
            createdAt = null,
            original = PatientDraftValues(),
            values = values,
        ),
    )

    private fun pregnancyState(values: PreviousPregnancyDraftValues) = AncState(
        patients = emptyList(),
        pregnancyDraft = PreviousPregnancyDraft(
            editorSessionId = "editor-1",
            patientId = PATIENT_ID,
            pregnancyId = PREGNANCY_ID,
            basePatientRevision = 1,
            basePregnancyRevision = null,
            recordedAt = null,
            original = PreviousPregnancyDraftValues(),
            values = values,
        ),
    )

    private companion object {
        const val OPERATION_ID = "00000000-0000-4000-8000-000000000901"
        const val PATIENT_ID = "00000000-0000-4000-8000-000000000902"
        const val PREGNANCY_ID = "00000000-0000-4000-8000-000000000903"
        const val SUBMITTED_AT = "2026-10-11T08:00:00Z"
    }
}
