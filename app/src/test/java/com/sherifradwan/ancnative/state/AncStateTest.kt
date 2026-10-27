package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.storage.SavePreparation
import com.sherifradwan.ancnative.storage.SaveVisitCommand
import com.sherifradwan.ancnative.storage.withCommittedVisit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AncStateTest {
    private val firstPatient = DemoFixtures.patients[0]
    private val secondPatient = DemoFixtures.patients[1]

    @Test
    fun saveAddsVisitToOnlyItsPatientAndProfileReadsSameState() {
        val initial = initialState()
        val editing = initial
            .reduce(AncEvent.OpenWorkspace(firstPatient.id))
            .reduce(AncEvent.StartVisit(firstPatient.id, null, "editor-1"))
            .reduce(AncEvent.DraftChanged(DraftField.DATE, "2026-10-08"))
            .reduce(AncEvent.DraftChanged(DraftField.NOTES, "Synthetic unit-test note"))

        val newVisitId = "00000000-0000-4000-8000-000000000100"
        val saved = editing.commit(newVisitId, "00000000-0000-4000-8000-000000000101")

        assertEquals(AncRoute.Workspace(firstPatient.id, WorkspaceSection.VISITS), saved.route)
        assertNull(saved.draft)
        assertTrue(saved.savedNotice)
        assertEquals(firstPatient.visits.size + 1, saved.patient(firstPatient.id)?.visits?.size)
        assertEquals(secondPatient.visits, saved.patient(secondPatient.id)?.visits)
        val profileState = saved.reduce(AncEvent.OpenProfile(firstPatient.id))
        assertEquals("Synthetic unit-test note", profileState.patient(firstPatient.id)?.visits?.find { it.id == newVisitId }?.notes)
    }

    @Test
    fun cancelDirtyDraftRequiresConfirmationAndNeverPartiallySaves() {
        val originalVisits = firstPatient.visits
        val editing = initialState()
            .reduce(AncEvent.StartVisit(firstPatient.id, null, "editor-2"))
            .reduce(AncEvent.DraftChanged(DraftField.DATE, "2026-10-08"))

        val requested = editing.reduce(AncEvent.CancelEditor)
        assertTrue(requested.showDiscardDialog)
        assertNotNull(requested.draft)
        assertEquals(originalVisits, requested.patient(firstPatient.id)?.visits)

        val discarded = requested.reduce(AncEvent.ConfirmDiscard)
        assertNull(discarded.draft)
        assertEquals(originalVisits, discarded.patient(firstPatient.id)?.visits)
    }

    @Test
    fun switchingPatientCannotLeakOrApplyDraft() {
        val editing = initialState()
            .reduce(AncEvent.StartVisit(firstPatient.id, null, "editor-3"))
            .reduce(AncEvent.DraftChanged(DraftField.DATE, "2026-10-08"))

        val switchRequested = editing.reduce(AncEvent.OpenWorkspace(secondPatient.id))
        assertTrue(switchRequested.showDiscardDialog)
        assertEquals(firstPatient.id, switchRequested.draft?.patientId)
        assertEquals(secondPatient.visits, switchRequested.patient(secondPatient.id)?.visits)

        val switched = switchRequested.reduce(AncEvent.ConfirmDiscard)
        assertEquals(AncRoute.Workspace(secondPatient.id), switched.route)
        assertNull(switched.draft)
        assertEquals(firstPatient.visits, switched.patient(firstPatient.id)?.visits)
        assertEquals(secondPatient.visits, switched.patient(secondPatient.id)?.visits)
    }

    @Test
    fun samePatientProfileAndWorkspaceNavigationWaitForExplicitDiscard() {
        val originalVisits = firstPatient.visits
        val editing = initialState()
            .reduce(AncEvent.StartVisit(firstPatient.id, null, "editor-same-profile"))
            .reduce(AncEvent.DraftChanged(DraftField.DATE, "2026-10-08"))

        val profileRequested = editing.reduce(AncEvent.OpenProfile(firstPatient.id))
        assertTrue(profileRequested.showDiscardDialog)
        assertEquals(AncRoute.VisitEditor(firstPatient.id), profileRequested.route)
        assertEquals("2026-10-08", profileRequested.draft?.values?.date)
        assertEquals(originalVisits, profileRequested.patient(firstPatient.id)?.visits)

        val profile = profileRequested.reduce(AncEvent.ConfirmDiscard)
        assertEquals(AncRoute.Profile(firstPatient.id), profile.route)
        assertNull(profile.draft)
        assertEquals(originalVisits, profile.patient(firstPatient.id)?.visits)

        val editingAgain = profile
            .reduce(AncEvent.StartVisit(firstPatient.id, null, "editor-same-workspace"))
            .reduce(AncEvent.DraftChanged(DraftField.NOTES, "Retain until Workspace discard"))
        val workspaceRequested = editingAgain.reduce(AncEvent.OpenWorkspace(firstPatient.id))
        assertTrue(workspaceRequested.showDiscardDialog)
        assertEquals(AncRoute.VisitEditor(firstPatient.id), workspaceRequested.route)
        assertEquals("Retain until Workspace discard", workspaceRequested.draft?.values?.notes)
        assertEquals(originalVisits, workspaceRequested.patient(firstPatient.id)?.visits)

        val workspace = workspaceRequested.reduce(AncEvent.ConfirmDiscard)
        assertEquals(AncRoute.Workspace(firstPatient.id), workspace.route)
        assertNull(workspace.draft)
        assertEquals(originalVisits, workspace.patient(firstPatient.id)?.visits)
    }

    @Test
    fun startVisitCannotReplaceDirtyEditorUntilExplicitDiscard() {
        val firstOriginalVisits = firstPatient.visits
        val secondOriginalVisits = secondPatient.visits
        val active = initialState()
            .reduce(AncEvent.StartVisit(firstPatient.id, null, "editor-active"))
            .reduce(AncEvent.DraftChanged(DraftField.DATE, "2026-10-08"))

        val samePatientRequested = active.reduce(
            AncEvent.StartVisit(firstPatient.id, firstPatient.visits.first().id, "editor-same-replacement"),
        )
        assertTrue(samePatientRequested.showDiscardDialog)
        assertEquals(AncRoute.VisitEditor(firstPatient.id), samePatientRequested.route)
        assertSame(active.draft, samePatientRequested.draft)
        assertEquals(firstOriginalVisits, samePatientRequested.patient(firstPatient.id)?.visits)

        val kept = samePatientRequested.reduce(AncEvent.KeepEditing)
        assertFalse(kept.showDiscardDialog)
        assertEquals("editor-active", kept.draft?.editorSessionId)
        assertEquals("2026-10-08", kept.draft?.values?.date)

        val otherPatientRequested = kept.reduce(
            AncEvent.StartVisit(secondPatient.id, null, "editor-other-patient"),
        )
        assertTrue(otherPatientRequested.showDiscardDialog)
        assertEquals(AncRoute.VisitEditor(firstPatient.id), otherPatientRequested.route)
        assertEquals(firstPatient.id, otherPatientRequested.draft?.patientId)
        assertEquals(firstOriginalVisits, otherPatientRequested.patient(firstPatient.id)?.visits)
        assertEquals(secondOriginalVisits, otherPatientRequested.patient(secondPatient.id)?.visits)

        val replaced = otherPatientRequested.reduce(AncEvent.ConfirmDiscard)
        assertEquals(AncRoute.VisitEditor(secondPatient.id), replaced.route)
        assertFalse(replaced.showDiscardDialog)
        assertEquals(secondPatient.id, replaced.draft?.patientId)
        assertEquals("editor-other-patient", replaced.draft?.editorSessionId)
        assertEquals(DraftValues(), replaced.draft?.values)
        assertEquals(firstOriginalVisits, replaced.patient(firstPatient.id)?.visits)
        assertEquals(secondOriginalVisits, replaced.patient(secondPatient.id)?.visits)
    }

    @Test
    fun invalidCalendarDateIsRejectedAndDraftIsRetained() {
        val editing = initialState()
            .reduce(AncEvent.StartVisit(firstPatient.id, null, "editor-4"))
            .reduce(AncEvent.DraftChanged(DraftField.DATE, "2026-02-30"))

        val preparation = SaveVisitCommand.prepare(
            editing,
            "00000000-0000-4000-8000-000000000102",
            "00000000-0000-4000-8000-000000000110",
        ) as SavePreparation.Rejected
        val rejected = editing.reduce(AncEvent.SaveValidationFailed(preparation.errors))

        assertTrue(DraftError.INVALID_DATE in requireNotNull(rejected.draft).errors)
        assertEquals(firstPatient.visits, rejected.patient(firstPatient.id)?.visits)
        assertTrue(rejected.route is AncRoute.VisitEditor)
    }

    @Test
    fun staleRevisionRejectsSaveAndRetainsDraft() {
        val visit = firstPatient.visits.first()
        val editing = initialState()
            .reduce(AncEvent.StartVisit(firstPatient.id, visit.id, "editor-5"))
            .reduce(AncEvent.DraftChanged(DraftField.NOTES, "My retained draft"))
        val externallyChangedPatients = editing.patients.map { patient ->
            if (patient.id != firstPatient.id) patient else patient.copy(
                visits = patient.visits.map { current ->
                    if (current.id == visit.id) current.copy(revision = current.revision + 1) else current
                },
            )
        }

        val rejectedState = editing.copy(patients = externallyChangedPatients)
        val preparation = SaveVisitCommand.prepare(
            rejectedState,
            "00000000-0000-4000-8000-000000000103",
            "00000000-0000-4000-8000-000000000111",
        ) as SavePreparation.Rejected
        val rejected = rejectedState.reduce(AncEvent.SaveValidationFailed(preparation.errors))

        assertTrue(DraftError.STALE_REVISION in requireNotNull(rejected.draft).errors)
        assertEquals("My retained draft", rejected.draft?.values?.notes)
        assertNotEquals("My retained draft", rejected.patient(firstPatient.id)?.visits?.first()?.notes)
    }

    @Test
    fun secondSaveCannotDuplicateVisit() {
        val editing = initialState()
            .reduce(AncEvent.StartVisit(firstPatient.id, null, "editor-6"))
            .reduce(AncEvent.DraftChanged(DraftField.DATE, "2026-10-08"))
        val firstId = "00000000-0000-4000-8000-000000000112"
        val secondId = "00000000-0000-4000-8000-000000000113"
        val once = editing.commit(firstId, "00000000-0000-4000-8000-000000000104")
        val twice = once.commit(secondId, "00000000-0000-4000-8000-000000000105")

        assertSame(once, twice)
        assertEquals(1, twice.patient(firstPatient.id)?.visits?.count { it.id == firstId })
        assertEquals(0, twice.patient(firstPatient.id)?.visits?.count { it.id == secondId })
    }

    @Test
    fun backOffersKeepEditingOrDiscardWithoutChangingSavedRecords() {
        val editing = initialState()
            .reduce(AncEvent.StartVisit(firstPatient.id, null, "editor-7"))
            .reduce(AncEvent.DraftChanged(DraftField.DATE, "2026-10-08"))
        val requested = editing.reduce(AncEvent.Back)
        assertTrue(requested.showDiscardDialog)

        val kept = requested.reduce(AncEvent.KeepEditing)
        assertFalse(kept.showDiscardDialog)
        assertTrue(kept.route is AncRoute.VisitEditor)
        assertNotNull(kept.draft)

        val discarded = kept.reduce(AncEvent.Back).reduce(AncEvent.ConfirmDiscard)
        assertEquals(AncRoute.Workspace(firstPatient.id, WorkspaceSection.VISITS), discarded.route)
        assertNull(discarded.draft)
        assertEquals(firstPatient.visits, discarded.patient(firstPatient.id)?.visits)
    }

    @Test
    fun datingAndUltrasoundReducerFlow() {
        val patient = firstPatient
        var state = AncState(
            patients = listOf(patient),
            vaultState = com.sherifradwan.ancnative.storage.VaultState.Unlocked(sessionEpoch = 401L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.DATING),
        )

        state = state.reduce(AncEvent.StartDatingEditor(patient.id, "dating-session-1"))
        assertEquals(AncRoute.DatingEditor(patient.id), state.route)
        assertNotNull(state.datingDraft)

        state = state.reduce(AncEvent.DatingDraftChanged(DatingDraftField.NOTES, "New dating draft note"))
        assertTrue(state.datingDraft?.isDirty == true)

        val discardDating = state.reduce(AncEvent.Back)
        assertTrue(discardDating.showDiscardDialog)

        state = discardDating.reduce(AncEvent.ConfirmDiscard)
        assertFalse(state.showDiscardDialog)
        assertFalse(state.hasDirtyDraft)
        assertEquals(AncRoute.Workspace(patient.id, WorkspaceSection.DATING), state.route)

        state = state.reduce(
            AncEvent.StartUltrasoundEditor(
                patientId = patient.id,
                scanId = null,
                newScanId = "us-scan-1",
                editorSessionId = "us-session-1",
            ),
        )
        assertEquals(AncRoute.UltrasoundEditor(patient.id, "us-scan-1", isNew = true), state.route)
        assertNotNull(state.ultrasoundDraft)
    }

    @Test
    fun labAndMedicationReducerFlow() {
        val patient = firstPatient
        var state = AncState(
            patients = listOf(patient),
            vaultState = com.sherifradwan.ancnative.storage.VaultState.Unlocked(sessionEpoch = 501L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.LABORATORY),
        )

        state = state.reduce(
            AncEvent.StartLabEditor(
                patientId = patient.id,
                labId = null,
                newLabId = "lab-test-1",
                editorSessionId = "lab-session-1",
            ),
        )
        assertEquals(AncRoute.LabEditor(patient.id, "lab-test-1", isNew = true), state.route)
        assertNotNull(state.labDraft)
        assertFalse(state.hasDirtyDraft)

        state = state.reduce(AncEvent.LabDraftChanged(LabDraftField.NAME, "Hemoglobin"))
        state = state.reduce(AncEvent.LabDraftChanged(LabDraftField.RESULT, "11.2"))
        state = state.reduce(AncEvent.LabAbnormalChanged(true))
        assertTrue(state.labDraft?.isDirty == true)
        assertTrue(state.labDraft?.values?.isAbnormal == true)
        assertEquals("Hemoglobin", state.labDraft?.values?.testName)
        assertEquals("11.2", state.labDraft?.values?.result)

        val discardLab = state.reduce(AncEvent.Back)
        assertTrue(discardLab.showDiscardDialog)

        state = discardLab.reduce(AncEvent.ConfirmDiscard)
        assertFalse(state.showDiscardDialog)
        assertFalse(state.hasDirtyDraft)
        assertNull(state.labDraft)
        assertEquals(AncRoute.Workspace(patient.id, WorkspaceSection.LABORATORY), state.route)

        state = state.reduce(
            AncEvent.StartMedicationEditor(
                patientId = patient.id,
                medicationId = null,
                newMedicationId = "med-test-1",
                editorSessionId = "med-session-1",
            ),
        )
        assertEquals(AncRoute.MedicationEditor(patient.id, "med-test-1", isNew = true), state.route)
        assertNotNull(state.medicationDraft)
        assertFalse(state.hasDirtyDraft)

        state = state.reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, "Folic Acid"))
        state = state.reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.DOSAGE, "400 mcg"))
        state = state.reduce(AncEvent.MedicationProphylaxisChanged(true))
        assertTrue(state.medicationDraft?.isDirty == true)
        assertTrue(state.medicationDraft?.values?.isProphylaxis == true)
        assertEquals("Folic Acid", state.medicationDraft?.values?.name)

        val discardMed = state.reduce(AncEvent.Back)
        assertTrue(discardMed.showDiscardDialog)

        state = discardMed.reduce(AncEvent.ConfirmDiscard)
        assertFalse(state.showDiscardDialog)
        assertFalse(state.hasDirtyDraft)
        assertNull(state.medicationDraft)
        assertEquals(AncRoute.Workspace(patient.id, WorkspaceSection.MEDICATIONS), state.route)
    }

    @Test
    fun visitAndUltrasoundLinkingDraftsTrackDirtyStateAndDiscardSafely() {
        val patient = firstPatient
        var state = initialState()
            .reduce(AncEvent.OpenWorkspace(patient.id))
            .reduce(AncEvent.StartVisit(patient.id, null, "editor-visit-link"))
            .reduce(AncEvent.DraftChanged(DraftField.DATE, "2026-10-15"))

        // Link ultrasound
        state = state.reduce(AncEvent.DraftChanged(DraftField.LINKED_ULTRASOUND, "scan-123"))
        assertEquals("scan-123", state.draft?.values?.linkedUltrasoundId)
        assertTrue(state.draft!!.isDirty)

        // Cancel prompts discard
        val cancelPrompt = state.reduce(AncEvent.CancelEditor)
        assertTrue(cancelPrompt.showDiscardDialog)

        // Discard clears draft
        state = cancelPrompt.reduce(AncEvent.ConfirmDiscard)
        assertNull(state.draft)
        assertFalse(state.hasDirtyDraft)

        // Ultrasound editor linking & photo attachments
        state = state.reduce(
            AncEvent.StartUltrasoundEditor(
                patientId = patient.id,
                scanId = null,
                newScanId = "new-scan-1",
                editorSessionId = "session-us",
            ),
        )
        assertNotNull(state.ultrasoundDraft)
        assertFalse(state.ultrasoundDraft!!.isDirty)

        // Link visit
        state = state.reduce(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, "visit-456"))
        assertEquals("visit-456", state.ultrasoundDraft?.values?.linkedVisitId)
        assertTrue(state.ultrasoundDraft!!.isDirty)

        // Add photos
        state = state.reduce(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMAGE_URIS, "photo1.jpg, photo2.jpg"))
        assertEquals(listOf("photo1.jpg", "photo2.jpg"), state.ultrasoundDraft?.values?.imageUris)

        // Unlink visit
        state = state.reduce(AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.LINKED_VISIT, ""))
        assertEquals("", state.ultrasoundDraft?.values?.linkedVisitId)

        // Discard ultrasound draft
        val discardUs = state.reduce(AncEvent.CancelEditor).reduce(AncEvent.ConfirmDiscard)
        assertNull(discardUs.ultrasoundDraft)
        assertFalse(discardUs.hasDirtyDraft)
    }

    @Test
    fun labDraftPrefillsDateAndLabNameWhenAddingAnother() {
        val patient = firstPatient
        val state = initialState()
            .reduce(AncEvent.OpenWorkspace(patient.id))
            .reduce(
                AncEvent.StartLabEditor(
                    patientId = patient.id,
                    labId = null,
                    newLabId = "lab-session-1",
                    editorSessionId = "session-1",
                    defaultDate = "2026-10-11",
                    defaultLabName = "Al Borg Labs",
                ),
            )

        assertNotNull(state.labDraft)
        assertEquals("2026-10-11", state.labDraft?.values?.date)
        assertEquals("Al Borg Labs", state.labDraft?.values?.labName)
        assertFalse(state.labDraft!!.isDirty)

        // Change values
        val updated = state
            .reduce(AncEvent.LabDraftChanged(LabDraftField.NAME, "Complete Blood Count"))
            .reduce(AncEvent.LabDraftChanged(LabDraftField.CATEGORY, "Hematology"))
            .reduce(AncEvent.LabDraftChanged(LabDraftField.RESULT, "12.5"))
            .reduce(AncEvent.LabAbnormalChanged(false))

        assertTrue(updated.labDraft!!.isDirty)
        assertEquals("Complete Blood Count", updated.labDraft?.values?.testName)
        assertEquals("12.5", updated.labDraft?.values?.result)
        assertFalse(updated.labDraft!!.values.isAbnormal)
    }

    @Test
    fun medicationDraftDefaultsAndComplianceTracking() {
        val patient = firstPatient
        val state = initialState()
            .reduce(AncEvent.OpenWorkspace(patient.id))
            .reduce(
                AncEvent.StartMedicationEditor(
                    patientId = patient.id,
                    medicationId = null,
                    newMedicationId = "med-session-1",
                    editorSessionId = "session-med-1",
                    defaultStartDate = "2026-10-11",
                ),
            )

        assertNotNull(state.medicationDraft)
        assertEquals("Oral", state.medicationDraft?.values?.route)
        assertEquals("Active", state.medicationDraft?.values?.status)
        assertEquals("2026-10-11", state.medicationDraft?.values?.startDate)
        assertFalse(state.medicationDraft!!.isDirty)
        assertFalse(state.medicationDraft!!.values.isNonCompliant)

        // Modify fields, toggle non-compliant, add adverse notes
        val updated = state
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, "Ferrous Sulfate"))
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.DOSAGE, "200 mg"))
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.FREQUENCY, "Once daily"))
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.IS_NON_COMPLIANT, "true"))
            .reduce(AncEvent.MedicationDraftChanged(MedicationDraftField.ADVERSE_NOTES, "Causes nausea"))

        assertTrue(updated.medicationDraft!!.isDirty)
        assertEquals("Ferrous Sulfate", updated.medicationDraft?.values?.name)
        assertTrue(updated.medicationDraft!!.values.isNonCompliant)
        assertEquals("Causes nausea", updated.medicationDraft?.values?.adverseNotes)
    }

    @Test
    fun otherHistoriesExplicitNegativeDraftTracking() {
        val patient = firstPatient
        val state = initialState()
            .reduce(AncEvent.OpenWorkspace(patient.id))
            .reduce(AncEvent.StartHistoriesEditor(patient.id, "hist-session-1"))

        assertNotNull(state.historiesDraft)
        assertFalse(state.historiesDraft!!.isDirty)

        val updated = state
            .reduce(AncEvent.HistoriesDraftChanged(HistoriesDraftField.MEDICAL, "No significant medical history"))
            .reduce(AncEvent.HistoriesDraftChanged(HistoriesDraftField.SURGICAL, "No prior surgical history"))
            .reduce(AncEvent.HistoriesDraftChanged(HistoriesDraftField.FAMILY, "No significant family history"))
            .reduce(AncEvent.HistoriesDraftChanged(HistoriesDraftField.ALLERGY, "No known drug allergies"))

        assertTrue(updated.historiesDraft!!.isDirty)
        assertEquals("No significant medical history", updated.historiesDraft?.values?.medicalHistory)
        assertEquals("No prior surgical history", updated.historiesDraft?.values?.surgicalHistory)
        assertEquals("No significant family history", updated.historiesDraft?.values?.familyHistory)
        assertEquals("No known drug allergies", updated.historiesDraft?.values?.allergyHistory)
    }

    @Test
    fun clinicalAttentionLevelPrefixParsing() {
        val critical = com.sherifradwan.ancnative.ui.ClinicalAttentionLevel.parse("[CRITICAL] Severe pre-eclampsia")
        assertEquals(com.sherifradwan.ancnative.ui.ClinicalAttentionLevel.CRITICAL, critical.first)
        assertEquals("Severe pre-eclampsia", critical.second)

        val alert = com.sherifradwan.ancnative.ui.ClinicalAttentionLevel.parse("ALERT: Low PAPP-A on first trimester screen")
        assertEquals(com.sherifradwan.ancnative.ui.ClinicalAttentionLevel.ALERT, alert.first)
        assertEquals("Low PAPP-A on first trimester screen", alert.second)

        // Substring guard: narrative containing "critical" without prefix MUST NOT be promoted to CRITICAL
        val negativeNarrative = com.sherifradwan.ancnative.ui.ClinicalAttentionLevel.parse("No critical alerts or allergies noted")
        assertEquals(com.sherifradwan.ancnative.ui.ClinicalAttentionLevel.NOTE, negativeNarrative.first)
        assertEquals("No critical alerts or allergies noted", negativeNarrative.second)
    }

    @Test
    fun chainedMedicationLifecycleSafetyOnCancelAndDiscard() {
        val patient = firstPatient
        val state = initialState()
            .reduce(AncEvent.OpenWorkspace(patient.id))
            .reduce(
                AncEvent.StartMedicationEditor(
                    patientId = patient.id,
                    medicationId = null,
                    newMedicationId = "med-chain-1",
                    editorSessionId = "session-chain-1",
                    defaultStartDate = "2026-10-11",
                ),
            )

        assertNotNull(state.medicationDraft)
        assertFalse(state.hasDirtyDraft)

        // Canceling clean draft returns to workspace without discard dialog
        val canceled = state.reduce(AncEvent.CancelEditor)
        assertNull(canceled.medicationDraft)
        assertEquals(AncRoute.Workspace(patient.id, WorkspaceSection.MEDICATIONS), canceled.route)
    }

    private fun initialState() = AncState(patients = DemoFixtures.patients)

    private fun AncState.commit(newVisitId: String, operationUuid: String): AncState {
        val prepared = SaveVisitCommand.prepare(this, operationUuid, newVisitId)
        if (prepared !is SavePreparation.Ready) return this
        val command = prepared.command
        val token = PendingSaveToken(command.operationUuid, command.editorSessionId)
        val pending = reduce(AncEvent.SaveStarted(token))
        assertEquals(patients, pending.patients)
        val revision = (command.expectedRevision ?: 0) + 1
        val committedPatients = patients.map { patient ->
            if (patient.id == command.patientId) patient.withCommittedVisit(command, revision) else patient
        }
        return pending.reduce(
            AncEvent.SaveCommitted(token, committedPatients, command.patientId),
        )
    }

    private fun AncState.reduce(event: AncEvent): AncState = AncReducer.reduce(this, event)
}
