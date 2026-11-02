package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
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
            isEditorRoute = { it.route is AncRoute.MedicationEditor },
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
            // Frozen behaviour: today only the medication create draft follows the patient revision after an undo.
            rebasedPatientRevision = 5L,
            label = "Hb r1",
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

    // Medication and lab deliberately share the record ids "r1" and "r2".
    private fun patient(status: String = "active", revision: Long = 5) = DemoPatient(
        id = "p1", caseId = "P1", displayName = "Patient One", dateOfBirth = "1995-01-01",
        phone = "01000000000", visits = emptyList(), status = status,
        medications = listOf(med("r1"), med("r2")),
        labInvestigations = listOf(lab("r1"), lab("r2")),
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
}
