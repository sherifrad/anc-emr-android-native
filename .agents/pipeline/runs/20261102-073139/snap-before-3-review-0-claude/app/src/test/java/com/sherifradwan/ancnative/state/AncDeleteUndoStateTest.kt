package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AncDeleteUndoStateTest {
    private fun med(id: String) = DemoMedicationRecord(
        id = id, patientId = "p1", name = "Iron $id", dosage = "100 mg", route = "Oral",
        frequency = "Once daily", startDate = "2026-01-01", endDate = null, status = "Active",
        notes = null, isProphylaxis = false,
    )

    private fun patient(status: String = "active", revision: Long = 5) = DemoPatient(
        id = "p1", caseId = "P1", displayName = "Patient One", dateOfBirth = "1995-01-01",
        phone = "01000000000", visits = emptyList(), status = status,
        medications = listOf(med("m1"), med("m2")), revision = revision,
    )

    private fun base(patient: DemoPatient = patient()) =
        AncState(patients = listOf(patient), vaultState = VaultState.Unlocked(1L))

    private fun offer(token: String, label: String = "Iron m1", phase: UndoPhase = UndoPhase.READY) = UndoOffer(
        token = token, patientId = "p1", kind = UndoKind.DELETE_MED, label = label, phase = phase,
    )

    private fun reduce(state: AncState, event: AncEvent) = AncReducer.reduce(state, event)

    private fun requestDeleteM1(state: AncState) = reduce(state, AncEvent.RequestDeleteMedication("p1", "m1"))

    private fun withOffer(state: AncState, token: String, phase: UndoPhase = UndoPhase.READY) =
        state.copy(undoOffer = offer(token, phase = phase))

    @Test
    fun requestDeleteOnCleanStateAsksForConfirmation() {
        val next = requestDeleteM1(base())
        val pending = next.deleteConfirmation
        assertNotNull(pending)
        assertEquals("p1", pending!!.patientId)
        assertEquals("m1", pending.recordId)
        assertEquals(base().patients, next.patients)
    }

    @Test
    fun requestDeleteIgnoredForArchivedPatient() {
        val state = base(patient(status = "archived"))
        assertEquals(state, requestDeleteM1(state))
        assertEquals(state, reduce(state, AncEvent.RequestDeleteLab("p1", "lab-1")))
    }

    @Test
    fun requestDeleteIgnoredWhileDirtyDraftOrSaving() {
        val editing = reduce(base(), AncEvent.StartMedicationEditor("p1", "m2", "m2", "session-1"))
        val dirty = reduce(editing, AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, "Changed"))
        assertTrue(dirty.hasDirtyDraft)
        assertNull(requestDeleteM1(dirty).deleteConfirmation)

        for (phase in listOf(SavePhase.PENDING, SavePhase.RECONCILING, SavePhase.UNRESOLVED)) {
            val saving = base().copy(savePhase = phase)
            assertNull(phase.name, requestDeleteM1(saving).deleteConfirmation)
        }
    }

    @Test
    fun cancelDeleteDropsTheConfirmation() {
        val asked = requestDeleteM1(base())
        assertNull(reduce(asked, AncEvent.CancelDelete).deleteConfirmation)
    }

    @Test
    fun confirmDeleteClosesCleanEditorOnSameRecord() {
        val editing = reduce(base(), AncEvent.StartMedicationEditor("p1", "m1", "m1", "session-1"))
        assertTrue(editing.route is AncRoute.MedicationEditor)
        val asked = requestDeleteM1(editing)
        assertNotNull(asked.deleteConfirmation)

        val confirmed = reduce(asked, AncEvent.ConfirmDelete)
        assertNull(confirmed.medicationDraft)
        assertNull(confirmed.deleteConfirmation)
        assertTrue(confirmed.route !is AncRoute.MedicationEditor)
    }

    @Test
    fun confirmDeleteKeepsCleanEditorOnAnotherRecordOpen() {
        val editing = reduce(base(), AncEvent.StartMedicationEditor("p1", "m2", "m2", "session-1"))
        val confirmed = reduce(requestDeleteM1(editing), AncEvent.ConfirmDelete)
        assertEquals("m2", confirmed.medicationDraft?.medicationId)
        assertTrue(confirmed.route is AncRoute.MedicationEditor)
    }

    @Test
    fun undoExpiredWithStaleTokenIsNoop() {
        val state = withOffer(base(), "t1")
        assertEquals(state, reduce(state, AncEvent.UndoExpired("stale")))
        assertEquals(state, reduce(base(), AncEvent.UndoExpired("t1")))
        assertNull(reduce(state, AncEvent.UndoExpired("t1")).undoOffer)
    }

    @Test
    fun undoRequestedWithStaleTokenIsNoopAndMatchingTokenRuns() {
        val state = withOffer(base(), "t1")
        assertEquals(state, reduce(state, AncEvent.UndoRequested("stale")))
        assertEquals(UndoPhase.RUNNING, reduce(state, AncEvent.UndoRequested("t1")).undoOffer?.phase)
    }

    @Test
    fun undoCommittedRebasesBlankChainedMedicationDraft() {
        val blank = reduce(base(), AncEvent.StartMedicationEditor("p1", null, "new-med", "session-2"))
        assertEquals(5L, blank.medicationDraft?.basePatientRevision)
        val running = reduce(withOffer(blank, "t1"), AncEvent.UndoRequested("t1"))

        val committed = reduce(running, AncEvent.UndoCommitted(listOf(patient(revision = 7))))
        assertEquals(7L, committed.medicationDraft?.basePatientRevision)
        assertEquals(blank.medicationDraft?.values, committed.medicationDraft?.values)
        assertEquals("new-med", committed.medicationDraft?.medicationId)
        assertNull(committed.undoOffer)
    }

    @Test
    fun submitMedicationIgnoredWhileUndoRunning() {
        val blank = reduce(base(), AncEvent.StartMedicationEditor("p1", null, "new-med", "session-2"))
        val running = reduce(withOffer(blank, "t1"), AncEvent.UndoRequested("t1"))
        assertEquals(UndoPhase.RUNNING, running.undoOffer?.phase)
        assertEquals(running, reduce(running, AncEvent.SubmitMedication))
        assertEquals(running, reduce(running, AncEvent.SubmitMedicationAndAddAnother))
        assertEquals(SavePhase.IDLE, running.savePhase)
    }

    @Test
    fun lockResetClearsUndoOffer() {
        val state = withOffer(requestDeleteM1(base()), "t1")
        assertNotNull(state.undoOffer)
        assertNotNull(state.deleteConfirmation)
        val locked = AncState(patients = emptyList(), vaultState = VaultState.Locked)
        assertNull(locked.undoOffer)
        assertNull(locked.deleteConfirmation)
    }

    @Test
    fun newOfferReplacesOld() {
        val first = reduce(base(), AncEvent.DeleteCommitted(listOf(patient(revision = 6)), offer("t1")))
        assertEquals("t1", first.undoOffer?.token)
        val second = reduce(first, AncEvent.DeleteCommitted(listOf(patient(revision = 7)), offer("t2", label = "Iron m2")))
        assertEquals("t2", second.undoOffer?.token)
        assertEquals("Iron m2", second.undoOffer?.label)
        assertEquals(second, reduce(second, AncEvent.UndoExpired("t1")))
    }
}
