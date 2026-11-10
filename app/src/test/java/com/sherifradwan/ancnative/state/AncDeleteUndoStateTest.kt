package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.storage.ClinicalRecordKind
import com.sherifradwan.ancnative.storage.DeletableRegistry
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AncDeleteUndoStateTest {
    /** One row per deletable type; every case below runs once per row through the real reducer and editor hooks. */
    private class Kind(
        val type: DeletableType,
        val startEditor: (state: AncState, recordId: String?, newId: String, session: String) -> AncState,
        val makeDirty: (AncState) -> AncState,
        val openRecordId: (AncState) -> String?,
        val isEditorRoute: (AncState) -> Boolean,
        val draftBasePatientRevision: (AncState) -> Long?,
        val draftValues: (AncState) -> Any?,
        val submit: AncEvent,
        val rebasedPatientRevision: Long,
        val label: String,
    )

    private fun reduce(state: AncState, event: AncEvent) = AncReducer.reduce(state, event)

    private val kinds = listOf(
        Kind(
            type = DeletableType.MEDICATION,
            startEditor = { s, id, newId, session -> reduce(s, AncEvent.StartMedicationEditor("p1", id, newId, session)) },
            makeDirty = { reduce(it, AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, "Changed")) },
            openRecordId = { it.medicationDraft?.medicationId },
            // The medication sheet shows while its draft exists; it has no route of its own.
            isEditorRoute = { it.medicationDraft != null },
            draftBasePatientRevision = { it.medicationDraft?.basePatientRevision },
            draftValues = { it.medicationDraft?.values },
            submit = AncEvent.SubmitMedication,
            rebasedPatientRevision = 7L,
            label = "Iron r1",
        ),
        Kind(
            type = DeletableType.LAB,
            startEditor = { s, id, newId, session -> reduce(s, AncEvent.StartLabEditor("p1", id, newId, session)) },
            makeDirty = { reduce(it, AncEvent.LabDraftChanged(LabDraftField.NAME, "Changed")) },
            openRecordId = { it.labDraft?.labId },
            isEditorRoute = { it.route is AncRoute.LabEditor },
            draftBasePatientRevision = { it.labDraft?.basePatientRevision },
            draftValues = { it.labDraft?.values },
            submit = AncEvent.SubmitLab,
            rebasedPatientRevision = 7L,
            label = "Hb r1",
        ),
        Kind(
            type = DeletableType.ULTRASOUND,
            startEditor = { s, id, newId, session -> reduce(s, AncEvent.StartUltrasoundEditor("p1", id, newId, session)) },
            makeDirty = { reduce(it, AncEvent.UltrasoundDraftChanged(UltrasoundDraftField.IMPRESSION, "Changed")) },
            openRecordId = { it.ultrasoundDraft?.scanId },
            isEditorRoute = { it.route is AncRoute.UltrasoundEditor },
            draftBasePatientRevision = { it.ultrasoundDraft?.basePatientRevision },
            draftValues = { it.ultrasoundDraft?.values },
            submit = AncEvent.SubmitUltrasound,
            rebasedPatientRevision = 7L,
            label = "2026-01-03",
        ),
    )

    private fun med(id: String) = DemoMedicationRecord(
        id = id, patientId = "p1", name = "Iron $id", dosage = "100 mg", route = "Oral",
        frequency = "Once daily", startDate = "2026-01-01", endDate = null, status = "Active",
        notes = null, isProphylaxis = false,
    )

    private fun lab(id: String) = DemoLabInvestigation(
        id = id, patientId = "p1", date = "2026-01-02", testCategory = "Hematology", testName = "Hb $id",
        result = "11", unit = "g/dL", referenceRange = null, isAbnormal = false, notes = null,
    )

    private fun us(id: String) = DemoUltrasoundScan(
        id = id, patientId = "p1", date = "2026-01-03", gestationalAgeWeeks = 20, gestationalAgeDays = 3,
        fetalNumber = "Single", presentation = "Cephalic", placentalSite = "Posterior", amnioticFluid = "Normal",
        fetalHeartRate = "142", biometryBpd = "48", biometryHc = "175", biometryAc = "150", biometryFl = "32",
        estimatedFetalWeight = "350g", impression = null,
    )

    // Medication, lab and ultrasound deliberately share the record ids "r1" and "r2".
    private fun patient(status: String = "active", revision: Long = 5) = DemoPatient(
        id = "p1", caseId = "P1", displayName = "Patient One", dateOfBirth = "1995-01-01",
        phone = "01000000000", visits = emptyList(), status = status,
        medications = listOf(med("r1"), med("r2")),
        labInvestigations = listOf(lab("r1"), lab("r2")),
        ultrasoundScans = listOf(us("r1"), us("r2")),
        revision = revision,
    )

    private fun base(patient: DemoPatient = patient()) =
        AncState(patients = listOf(patient), vaultState = VaultState.Unlocked(1L))

    private fun offer(kind: Kind, token: String, label: String = kind.label, phase: UndoPhase = UndoPhase.READY) = UndoOffer(
        token = token, patientId = "p1", type = kind.type, action = UndoAction.DELETE, label = label, phase = phase,
    )

    private fun requestDelete(kind: Kind, state: AncState, recordId: String = "r1") =
        reduce(state, AncEvent.RequestDelete("p1", kind.type, recordId))

    private fun withOffer(kind: Kind, state: AncState, token: String, phase: UndoPhase = UndoPhase.READY) =
        state.copy(undoOffer = offer(kind, token, phase = phase))

    @Test
    fun requestDeleteOnCleanStateAsksForConfirmation() {
        for (kind in kinds) {
            val next = requestDelete(kind, base())
            val pending = next.deleteConfirmation
            assertNotNull(kind.type.name, pending)
            assertEquals(PendingDeletion("p1", kind.type, "r1"), pending)
            assertEquals(kind.type.name, base().patients, next.patients)
        }
    }

    @Test
    fun requestDeleteIgnoredForArchivedPatient() {
        for (kind in kinds) {
            val state = base(patient(status = "archived"))
            assertEquals(kind.type.name, state, requestDelete(kind, state))
        }
    }

    @Test
    fun requestDeleteIgnoredWhileDirtyDraftOrSaving() {
        for (kind in kinds) {
            val editing = kind.startEditor(base(), "r2", "r2", "session-1")
            val dirty = kind.makeDirty(editing)
            assertTrue(kind.type.name, dirty.hasDirtyDraft)
            assertNull(kind.type.name, requestDelete(kind, dirty).deleteConfirmation)

            for (phase in listOf(SavePhase.PENDING, SavePhase.RECONCILING, SavePhase.UNRESOLVED)) {
                val saving = base().copy(savePhase = phase)
                assertNull("${kind.type} $phase", requestDelete(kind, saving).deleteConfirmation)
            }
        }
    }

    @Test
    fun cancelDeleteDropsTheConfirmation() {
        for (kind in kinds) {
            val asked = requestDelete(kind, base())
            assertNull(kind.type.name, reduce(asked, AncEvent.CancelDelete).deleteConfirmation)
        }
    }

    @Test
    fun confirmDeleteClosesCleanEditorOnSameRecord() {
        for (kind in kinds) {
            val editing = kind.startEditor(base(), "r1", "r1", "session-1")
            assertTrue(kind.type.name, kind.isEditorRoute(editing))
            val asked = requestDelete(kind, editing)
            assertNotNull(kind.type.name, asked.deleteConfirmation)

            val confirmed = reduce(asked, AncEvent.ConfirmDelete)
            assertNull(kind.type.name, kind.openRecordId(confirmed))
            assertNull(kind.type.name, confirmed.deleteConfirmation)
            assertTrue(kind.type.name, !kind.isEditorRoute(confirmed))
        }
    }

    @Test
    fun confirmDeleteKeepsCleanEditorOnAnotherRecordOpen() {
        for (kind in kinds) {
            val editing = kind.startEditor(base(), "r2", "r2", "session-1")
            val confirmed = reduce(requestDelete(kind, editing), AncEvent.ConfirmDelete)
            assertEquals(kind.type.name, "r2", kind.openRecordId(confirmed))
            assertTrue(kind.type.name, kind.isEditorRoute(confirmed))
        }
    }

    @Test
    fun confirmDeleteKeepsEditorOfTheOtherTypeWithTheSameRecordId() {
        for (kind in kinds) {
            val other = kinds.first { it !== kind }
            val editing = other.startEditor(base(), "r1", "r1", "session-1")
            val confirmed = reduce(requestDelete(kind, editing), AncEvent.ConfirmDelete)
            assertEquals(kind.type.name, "r1", other.openRecordId(confirmed))
            assertTrue(kind.type.name, other.isEditorRoute(confirmed))
        }
    }

    @Test
    fun undoExpiredWithStaleTokenIsNoop() {
        for (kind in kinds) {
            val state = withOffer(kind, base(), "t1")
            assertEquals(state, reduce(state, AncEvent.UndoExpired("stale")))
            assertEquals(base(), reduce(base(), AncEvent.UndoExpired("t1")))
            assertNull(reduce(state, AncEvent.UndoExpired("t1")).undoOffer)
        }
    }

    @Test
    fun undoRequestedWithStaleTokenIsNoopAndMatchingTokenRuns() {
        for (kind in kinds) {
            val state = withOffer(kind, base(), "t1")
            assertEquals(state, reduce(state, AncEvent.UndoRequested("stale")))
            assertEquals(UndoPhase.RUNNING, reduce(state, AncEvent.UndoRequested("t1")).undoOffer?.phase)
        }
    }

    @Test
    fun undoCommittedRebasesBlankChainedCreateDraft() {
        for (kind in kinds) {
            val blank = kind.startEditor(base(), null, "new-id", "session-2")
            assertEquals(kind.type.name, 5L, kind.draftBasePatientRevision(blank))
            val running = reduce(withOffer(kind, blank, "t1"), AncEvent.UndoRequested("t1"))

            val committed = reduce(running, AncEvent.UndoCommitted("t1", listOf(patient(revision = 7))))
            assertEquals(kind.type.name, kind.rebasedPatientRevision, kind.draftBasePatientRevision(committed))
            assertEquals(kind.type.name, kind.draftValues(blank), kind.draftValues(committed))
            assertEquals(kind.type.name, "new-id", kind.openRecordId(committed))
            assertNull(kind.type.name, committed.undoOffer)
        }
    }

    @Test
    fun undoCommittedRebasesDirtyCreateDraftKeepingTypedContent() {
        for (kind in kinds) {
            val blank = kind.startEditor(base(), null, "new-id", "session-2")
            val dirty = kind.makeDirty(blank)
            assertTrue(kind.type.name, dirty.hasDirtyDraft)
            val running = reduce(withOffer(kind, dirty, "t1"), AncEvent.UndoRequested("t1"))
            val committed = reduce(running, AncEvent.UndoCommitted("t1", listOf(patient(revision = 7))))
            assertEquals(kind.type.name, kind.rebasedPatientRevision, kind.draftBasePatientRevision(committed))
            assertEquals(kind.type.name, kind.draftValues(dirty), kind.draftValues(committed))
        }
    }

    @Test
    fun undoCommittedDoesNotRebaseDraftOfExistingRecord() {
        for (kind in kinds) {
            val editing = kind.startEditor(base(), "r2", "r2", "session-1")
            val running = reduce(withOffer(kind, editing, "t1"), AncEvent.UndoRequested("t1"))
            val committed = reduce(running, AncEvent.UndoCommitted("t1", listOf(patient(revision = 7))))
            assertEquals(kind.type.name, kind.draftBasePatientRevision(editing), kind.draftBasePatientRevision(committed))
            assertEquals(kind.type.name, kind.draftValues(editing), kind.draftValues(committed))
        }
    }

    @Test
    fun runningUndoIsDetectedAndIgnoresSecondRequest() {
        for (kind in kinds) {
            val ready = withOffer(kind, base(), "t1")
            assertTrue(ready.canRequestUndo("t1"))
            assertEquals(false, ready.canRequestUndo("other"))
            assertEquals(false, ready.undoRunning)
            val running = reduce(ready, AncEvent.UndoRequested("t1"))
            assertTrue(running.undoRunning)
            assertEquals(false, running.canRequestUndo("t1"))
            assertEquals(running, reduce(running, AncEvent.UndoRequested("t1")))
            assertEquals(false, base().undoRunning)
        }
    }

    @Test
    fun submitIgnoredWhileUndoRunning() {
        for (kind in kinds) {
            val editing = kind.makeDirty(kind.startEditor(base(), "r2", "r2", "session-1"))
            val running = reduce(withOffer(kind, editing, "t1"), AncEvent.UndoRequested("t1"))
            assertTrue(kind.type.name, running.undoRunning)
            assertEquals(kind.type.name, running, reduce(running, kind.submit))
        }
    }

    @Test
    fun deleteRequestAndConfirmIgnoredWhileUndoRunning() {
        for (kind in kinds) {
            val running = reduce(withOffer(kind, base(), "t1"), AncEvent.UndoRequested("t1"))
            assertTrue(kind.type.name, running.undoRunning)
            assertEquals(kind.type.name, running, requestDelete(kind, running, "r2"))
            val pending = running.copy(deleteConfirmation = PendingDeletion("p1", kind.type, "r2"))
            assertEquals(kind.type.name, pending, reduce(pending, AncEvent.ConfirmDelete))
        }
    }

    @Test
    fun saveAndAddAnotherKeepsSavedNoticeAndUndoOfferWhileOpeningBlankEditor() {
        val offer = offer(kinds[0], "t1", "Iron saved")
        var s = base().copy(savedNotice = true, notice = SavedNotice.VISIT)
        s = reduce(s, AncEvent.UndoOffered(offer))
        s = reduce(s, AncEvent.StartMedicationEditor("p1", null, "new-id", "session-2", keepSavedNotice = true))
        assertEquals(base().route, s.route)
        assertEquals("new-id", s.medicationDraft?.medicationId)
        assertTrue(s.savedNotice)
        assertEquals(SavedNotice.VISIT, s.notice)
        assertEquals(offer, s.undoOffer)
        val plain = reduce(base().copy(savedNotice = true, notice = SavedNotice.VISIT), AncEvent.StartMedicationEditor("p1", null, "n2", "s3"))
        assertEquals(false, plain.savedNotice)
        assertNull(plain.notice)
    }

    @Test
    fun lockClearsUndoOfferAndDeleteConfirmation() {
        for (kind in kinds) {
            val state = withOffer(kind, requestDelete(kind, base()), "t1")
            assertNotNull(state.undoOffer)
            assertNotNull(state.deleteConfirmation)
            val locked = state.clearedForLock()
            assertNull(kind.type.name, locked.undoOffer)
            assertNull(kind.type.name, locked.deleteConfirmation)
            assertEquals(kind.type.name, state.patients, locked.patients)
        }
    }

    @Test
    fun failedUndoMayRestartItsWindowOnlyOnce() {
        assertTrue(undoMayRestart(0))
        assertEquals(false, undoMayRestart(1))
        assertEquals(false, undoMayRestart(2))
    }

    @Test
    fun newOfferReplacesOld() {
        for (kind in kinds) {
            val first = reduce(base(), AncEvent.DeleteCommitted(listOf(patient(revision = 6)), offer(kind, "t1")))
            assertEquals("t1", first.undoOffer?.token)
            val second = reduce(first, AncEvent.DeleteCommitted(listOf(patient(revision = 7)), offer(kind, "t2", label = "Other")))
            assertEquals(kind.type.name, "t2", second.undoOffer?.token)
            assertEquals(kind.type.name, "Other", second.undoOffer?.label)
            assertEquals(kind.type.name, second, reduce(second, AncEvent.UndoExpired("t1")))
        }
    }

    @Test
    fun undoExpiredMatchingTokenClearsOfferAndStaleTokenLeavesStateUntouched() {
        for (kind in kinds) {
            val state = withOffer(kind, base(), "t1")
            assertNull(reduce(state, AncEvent.UndoExpired("t1")).undoOffer)
            assertEquals(state, reduce(state, AncEvent.UndoExpired("other")))
        }
    }

    @Test
    fun undoCommittedWithStaleTokenKeepsNewerOffer() {
        for (kind in kinds) {
            val state = withOffer(kind, base(), "newer")
            val next = reduce(state, AncEvent.UndoCommitted("older", listOf(patient(revision = 9))))
            assertEquals(kind.type.name, offer(kind, "newer"), next.undoOffer)
            assertEquals(kind.type.name, 9L, next.patient("p1")?.revision)
        }
    }

    @Test
    fun undoFailedWithStaleTokenIsNoopAndMatchingTokenReturnsToReady() {
        for (kind in kinds) {
            val running = withOffer(kind, base(), "t1", UndoPhase.RUNNING)
            assertEquals(running, reduce(running, AncEvent.UndoFailed("other", SaveUiError.STORAGE_ERROR)))
            val failed = reduce(running, AncEvent.UndoFailed("t1", SaveUiError.STORAGE_ERROR))
            assertEquals(kind.type.name, UndoPhase.READY, failed.undoOffer?.phase)
        }
    }

    @Test
    fun confirmDeleteIgnoredWhenDraftDirtyOrSaving() {
        for (kind in kinds) {
            val asked = requestDelete(kind, base())
            val saving = asked.copy(savePhase = SavePhase.PENDING)
            assertEquals(kind.type.name, saving, reduce(saving, AncEvent.ConfirmDelete))

            val editing = kind.startEditor(base(), "r2", "r2", "session-1")
            val askedClean = requestDelete(kind, editing)
            val dirty = kind.makeDirty(askedClean)
            assertTrue(kind.type.name, dirty.hasDirtyDraft)
            assertEquals(kind.type.name, dirty, reduce(dirty, AncEvent.ConfirmDelete))
        }
    }

    private fun saveOffer(kind: Kind, token: String, recordId: String) = UndoOffer(
        token = token, patientId = "p1", type = kind.type, action = UndoAction.SAVE, label = kind.label,
        phase = UndoPhase.READY, patientName = "Patient One", recordId = recordId,
    )

    @Test
    fun deleteFailedShowsAVisibleMessageForThatPatientAndKeepsTheRecord() {
        for (kind in kinds) {
            val running = reduce(requestDelete(kind, base()), AncEvent.ConfirmDelete).let { reduce(it, AncEvent.DeleteStarted) }
            assertTrue(running.deleteRunning)
            val failed = reduce(running, AncEvent.DeleteFailed(SaveUiError.STALE_OR_CONFLICT, "p1", kind.type, kind.label))
            val message = failed.deleteMessage
            assertNotNull(kind.type.name, message)
            assertEquals(DeleteMessageKind.DELETE_FAILED, message!!.kind)
            assertEquals("p1", message.patientId)
            assertEquals(kind.label, message.label)
            assertEquals(false, failed.deleteRunning)
            assertEquals(base().patients, failed.patients)
            assertNull(reduce(failed, AncEvent.DismissDeleteMessage).deleteMessage)
        }
    }

    @Test
    fun failedUndoSaysTheRecordIsStillDeletedAndStaysUntilDismissed() {
        for (kind in kinds) {
            val running = withOffer(kind, base(), "t1", UndoPhase.RUNNING)
            val failed = reduce(running, AncEvent.UndoFailed("t1", SaveUiError.STORAGE_ERROR))
            val message = failed.deleteMessage!!
            assertEquals(DeleteMessageKind.UNDO_FAILED, message.kind)
            assertEquals(UndoAction.DELETE, message.action)
            assertEquals(kind.label, message.label)
            // The offer's window ending does not hide the message.
            assertNotNull(reduce(failed, AncEvent.UndoExpired("t1")).deleteMessage)
            // A fresh Undo attempt clears it.
            assertNull(reduce(failed, AncEvent.UndoRequested("t1")).deleteMessage)
        }
    }

    @Test
    fun openingAnotherPatientDropsOfferAndNoticeOfTheFirstButKeepsItsFailureMessage() {
        for (kind in kinds) {
            val inWorkspace = base().copy(route = AncRoute.Workspace("p1", WorkspaceSection.MEDICATIONS))
            val state = reduce(withOffer(kind, inWorkspace, "t1"), AncEvent.UndoCommitted("zz", listOf(patient()), SavedNotice.MEDICATION_RESTORED, "p1"))
                .copy(deleteMessage = DeleteMessage(DeleteMessageKind.UNDO_FAILED, "p1", kind.type, kind.label, UndoAction.DELETE))
            assertNotNull(state.undoOffer)
            assertEquals("p1", state.noticePatientId)
            val sameLaterPatient = reduce(state, AncEvent.OpenWorkspace("p1"))
            assertNotNull(kind.type.name, sameLaterPatient.undoOffer)
            val other = reduce(state, AncEvent.OpenWorkspace("p2"))
            assertNull(kind.type.name, other.undoOffer)
            // A failure message names its patient and so stays; only the offer and the notice are patient-scoped.
            assertNotNull(kind.type.name, other.deleteMessage)
            assertEquals(false, other.savedNotice)
            assertNull(other.noticePatientId)
        }
    }

    @Test
    fun noticeOfALateCommitNeverShowsOnAnotherPatient() {
        for (kind in kinds) {
            val onOther = base().copy(route = AncRoute.Workspace("p2", WorkspaceSection.MEDICATIONS))
            val next = reduce(withOffer(kind, onOther, "t1", UndoPhase.RUNNING), AncEvent.UndoCommitted("t1", listOf(patient()), SavedNotice.MEDICATION_RESTORED, "p1"))
            assertEquals(false, next.savedNotice)
            assertNull(next.noticePatientId)
        }
    }

    @Test
    fun undoOfSaveClosesACleanEditorOnThatRecordFirst() {
        for (kind in kinds) {
            val editing = kind.startEditor(base(), "r1", "r1", "session-1")
            assertTrue(kind.isEditorRoute(editing))
            val running = reduce(editing.copy(undoOffer = saveOffer(kind, "t1", "r1")), AncEvent.UndoRequested("t1"))
            assertTrue(kind.type.name, running.canRunUndo("t1"))
            assertNull(kind.type.name, kind.openRecordId(running))
            assertEquals(false, kind.isEditorRoute(running))
        }
    }

    @Test
    fun undoOfSaveIsRefusedWithAMessageWhenThatRecordHasUnsavedEdits() {
        for (kind in kinds) {
            val dirty = kind.makeDirty(kind.startEditor(base(), "r1", "r1", "session-1"))
            val refused = reduce(dirty.copy(undoOffer = saveOffer(kind, "t1", "r1")), AncEvent.UndoRequested("t1"))
            assertEquals(false, refused.canRunUndo("t1"))
            assertEquals(UndoPhase.READY, refused.undoOffer?.phase)
            assertEquals(DeleteMessageKind.UNDO_BLOCKED, refused.deleteMessage?.kind)
            assertEquals("r1", kind.openRecordId(refused))
        }
    }

    @Test
    fun undoOfSaveLeavesAnEditorOnAnotherRecordAlone() {
        for (kind in kinds) {
            val editing = kind.makeDirty(kind.startEditor(base(), "r2", "r2", "session-1"))
            val running = reduce(editing.copy(undoOffer = saveOffer(kind, "t1", "r1")), AncEvent.UndoRequested("t1"))
            assertTrue(kind.type.name, running.canRunUndo("t1"))
            assertEquals("r2", kind.openRecordId(running))
        }
    }

    @Test
    fun deleteRequestAndConfirmIgnoredWhileADeleteRuns() {
        for (kind in kinds) {
            val asked = requestDelete(kind, base())
            val running = reduce(asked, AncEvent.DeleteStarted)
            assertEquals(kind.type.name, running, requestDelete(kind, running, "r2"))
            val pending = running.copy(deleteConfirmation = PendingDeletion("p1", kind.type, "r2"))
            assertEquals(kind.type.name, pending, reduce(pending, AncEvent.ConfirmDelete))
            val done = reduce(running, AncEvent.DeleteCommitted(listOf(patient(revision = 6)), offer(kind, "t1"), null, "p1"))
            assertEquals(false, done.deleteRunning)
            assertNotNull(requestDelete(kind, done, "r2").deleteConfirmation)
        }
    }

    @Test
    fun confirmationForARecordThatIsGoneIsClosed() {
        for (kind in kinds) {
            val asked = requestDelete(kind, base())
            val gone = patient().copy(
                medications = patient().medications.filterNot { it.id == "r1" },
                labInvestigations = patient().labInvestigations.filterNot { it.id == "r1" },
                ultrasoundScans = patient().ultrasoundScans.filterNot { it.id == "r1" },
                revision = 6,
            )
            val next = reduce(asked, AncEvent.UndoCommitted("none", listOf(gone)))
            assertNull(kind.type.name, next.deleteConfirmation)
        }
    }

    @Test
    fun lockClearsTheNewDeleteUiState() {
        val locked = base().copy(
            deleteRunning = true, noticePatientId = "p1",
            deleteMessage = DeleteMessage(DeleteMessageKind.DELETE_FAILED, "p1", DeletableType.LAB, "x"),
        ).clearedForLock()
        assertEquals(false, locked.deleteRunning)
        assertNull(locked.deleteMessage)
        assertNull(locked.noticePatientId)
    }

    @Test
    fun failureMessagesSurviveAPatientSwitchAndNameThePatient() {
        for (kind in kinds) {
            val inWorkspace = base().copy(route = AncRoute.Workspace("p1", WorkspaceSection.MEDICATIONS))
            val failed = reduce(
                reduce(inWorkspace, AncEvent.DeleteStarted),
                AncEvent.DeleteFailed(SaveUiError.STORAGE_ERROR, "p1", kind.type, kind.label, "Patient One"),
            )
            val other = reduce(failed, AncEvent.OpenWorkspace("p2"))
            assertEquals(kind.type.name, DeleteMessageKind.DELETE_FAILED, other.deleteMessage?.kind)
            assertEquals("Patient One", other.deleteMessage?.patientName)
            assertNull(reduce(other, AncEvent.DismissDeleteMessage).deleteMessage)
            // The refusal "finish your edits" is about the open screen only and does not follow the clinician.
            val blocked = failed.copy(deleteMessage = failed.deleteMessage!!.copy(kind = DeleteMessageKind.UNDO_BLOCKED))
            assertNull(reduce(blocked, AncEvent.OpenWorkspace("p2")).deleteMessage)
        }
    }

    @Test
    fun undoFailureStillShownWhenItsOfferWasAlreadyDropped() {
        for (kind in kinds) {
            val onOther = base().copy(route = AncRoute.Workspace("p2", WorkspaceSection.MEDICATIONS))
            val lost = offer(kind, "t1", phase = UndoPhase.RUNNING).copy(patientName = "Patient One")
            val failed = reduce(onOther, AncEvent.UndoFailed("t1", SaveUiError.STORAGE_ERROR, lost))
            val message = failed.deleteMessage!!
            assertEquals(DeleteMessageKind.UNDO_FAILED, message.kind)
            assertEquals("p1", message.patientId)
            assertEquals("Patient One", message.patientName)
            assertEquals(kind.label, message.label)
            assertNull(failed.undoOffer)
            // Without the offer details a stale token is still ignored.
            assertEquals(onOther, reduce(onOther, AncEvent.UndoFailed("t1", SaveUiError.STORAGE_ERROR)))
        }
    }

    private fun failureOfP1(kind: Kind) = DeleteMessage(
        DeleteMessageKind.DELETE_FAILED, "p1", kind.type, kind.label, UndoAction.DELETE, SaveUiError.STORAGE_ERROR, "Patient One",
    )

    @Test
    fun anotherPatientsFailureSurvivesUnrelatedSuccessesAndOnlyDismissOrAFailureClearsIt() {
        for (kind in kinds) {
            val onP2 = base().copy(route = AncRoute.Workspace("p2", WorkspaceSection.MEDICATIONS), deleteMessage = failureOfP1(kind))
            val p2offer = offer(kind, "t9").copy(patientId = "p2")
            assertEquals(kind.type.name, failureOfP1(kind), reduce(onP2, AncEvent.UndoOffered(p2offer)).deleteMessage)
            assertEquals(failureOfP1(kind), reduce(onP2.copy(patients = listOf(patient().copy(id = "p2"))), AncEvent.RequestDelete("p2", kind.type, "r1")).deleteMessage)
            assertEquals(failureOfP1(kind), reduce(onP2, AncEvent.DeleteCommitted(onP2.patients, p2offer, null, "p2")).deleteMessage)
            val withOffer = onP2.copy(undoOffer = p2offer)
            assertEquals(failureOfP1(kind), reduce(withOffer, AncEvent.UndoRequested("t9")).deleteMessage)
            // The message of the open patient, and a "finish your edits" refusal, are still cleared by a success.
            val own = onP2.copy(deleteMessage = failureOfP1(kind).copy(patientId = "p2"))
            assertNull(reduce(own, AncEvent.UndoOffered(p2offer)).deleteMessage)
            val blocked = onP2.copy(deleteMessage = failureOfP1(kind).copy(kind = DeleteMessageKind.UNDO_BLOCKED))
            assertNull(reduce(blocked, AncEvent.UndoOffered(p2offer)).deleteMessage)
            assertNull(reduce(onP2, AncEvent.DismissDeleteMessage).deleteMessage)
        }
    }

    @Test
    fun undoFailureIsIgnoredWhileTheVaultIsLocked() {
        for (kind in kinds) {
            val locked = base().copy(vaultState = VaultState.Locked)
            val lost = offer(kind, "t1", phase = UndoPhase.RUNNING)
            assertEquals(locked, reduce(locked, AncEvent.UndoFailed("t1", SaveUiError.STORAGE_ERROR, lost)))
            val withOffer = locked.copy(undoOffer = lost)
            assertEquals(withOffer, reduce(withOffer, AncEvent.UndoFailed("t1", SaveUiError.STORAGE_ERROR)))
        }
    }

    @Test
    fun droppedOfferUndoFailureDoesNotOverwriteTheOpenScreensSaveError() {
        for (kind in kinds) {
            val onOther = base().copy(route = AncRoute.Workspace("p2", WorkspaceSection.MEDICATIONS), saveError = SaveUiError.CASE_ID_CONFLICT)
            val lost = offer(kind, "t1", phase = UndoPhase.RUNNING)
            val failed = reduce(onOther, AncEvent.UndoFailed("t1", SaveUiError.STORAGE_ERROR, lost))
            assertEquals(SaveUiError.CASE_ID_CONFLICT, failed.saveError)
            assertEquals(DeleteMessageKind.UNDO_FAILED, failed.deleteMessage?.kind)
        }
    }

    @Test
    fun undoRequestRefusedWhileADeleteRuns() {
        for (kind in kinds) {
            val state = reduce(withOffer(kind, base(), "t1"), AncEvent.DeleteStarted)
            val next = reduce(state, AncEvent.UndoRequested("t1"))
            assertEquals(false, next.canRunUndo("t1"))
            assertEquals(state, next)
            val done = reduce(state, AncEvent.DeleteCommitted(listOf(patient(revision = 6)), offer(kind, "t2"), null, "p1"))
            assertTrue(reduce(done, AncEvent.UndoRequested("t2")).canRunUndo("t2"))
        }
    }

    @Test
    fun editorOfARecordIsNotOpenedWhileItsUndoRuns() {
        for (kind in kinds) {
            val running = base().copy(undoOffer = saveOffer(kind, "t1", "r1").copy(phase = UndoPhase.RUNNING))
            val tapped = kind.startEditor(running, "r1", "n1", "s1")
            assertEquals(running, tapped)
            // Another record of the same kind can still be edited.
            assertEquals("r2", kind.openRecordId(kind.startEditor(running, "r2", "n2", "s2")))
            // Once the Undo is no longer running the record opens as before.
            val ready = base().copy(undoOffer = saveOffer(kind, "t1", "r1"))
            assertEquals("r1", kind.openRecordId(kind.startEditor(ready, "r1", "n1", "s1")))
        }
    }

    @Test
    fun allClinicalRecordKindsAreExhaustivelyHandledInClinicalValidationFailedWithoutFallingThrough() {
        val testErrors = setOf(ClinicalValidationError.REQUIRED_NAME)
        val baseState = base().copy(
            datingDraft = DatingDraft(editorSessionId = "s1", patientId = "p1", basePatientRevision = 1L, original = DatingDraftValues(), values = DatingDraftValues()),
            ultrasoundDraft = UltrasoundDraft(editorSessionId = "s1", patientId = "p1", scanId = "u1", basePatientRevision = 1L, baseScanRevision = null, original = UltrasoundDraftValues(), values = UltrasoundDraftValues()),
            labDraft = LabDraft(editorSessionId = "s1", patientId = "p1", labId = "l1", basePatientRevision = 1L, baseLabRevision = null, original = LabDraftValues(), values = LabDraftValues()),
            medicationDraft = MedicationDraft(editorSessionId = "s1", patientId = "p1", medicationId = "m1", basePatientRevision = 1L, baseMedicationRevision = null, original = MedicationDraftValues(), values = MedicationDraftValues()),
        )
        for (kind in ClinicalRecordKind.values()) {
            val reduced = reduce(baseState, AncEvent.ClinicalValidationFailed(kind, testErrors))
            if (DeletableRegistry.isDeleteOrRestoreKind(kind)) {
                assertEquals(kind.name, baseState, reduced)
            } else {
                when (kind) {
                    ClinicalRecordKind.DATING -> assertEquals(testErrors, reduced.datingDraft?.errors)
                    ClinicalRecordKind.ULTRASOUND_CREATE, ClinicalRecordKind.ULTRASOUND_UPDATE -> assertEquals(testErrors, reduced.ultrasoundDraft?.errors)
                    ClinicalRecordKind.LAB_CREATE, ClinicalRecordKind.LAB_UPDATE -> assertEquals(testErrors, reduced.labDraft?.errors)
                    ClinicalRecordKind.MEDICATION_CREATE, ClinicalRecordKind.MEDICATION_UPDATE -> assertEquals(testErrors, reduced.medicationDraft?.errors)
                    // The batch draft has no field errors yet; the save must still end cleanly.
                    ClinicalRecordKind.LAB_BATCH_SAVE,
                    ClinicalRecordKind.LAB_BATCH_DELETE,
                    ClinicalRecordKind.LAB_BATCH_RESTORE,
                    -> Unit
                    else -> throw AssertionError("Unhandled ClinicalRecordKind: $kind")
                }
                assertEquals(SavePhase.IDLE, reduced.savePhase)
                assertNull(reduced.saveError)
            }
        }
    }

    @Test
    fun chainedMedicationEditorStartsTodayByDefault() {
        val state = base().copy(patients = listOf(patient()))
        val reduced = reduce(
            state,
            AncEvent.StartMedicationEditor(
                patientId = "p1",
                medicationId = null,
                newMedicationId = "new-m1",
                editorSessionId = "sess-1",
                keepSavedNotice = true,
            ),
        )
        assertEquals(java.time.LocalDate.now().toString(), reduced.medicationDraft?.values?.startDate)
    }
}
