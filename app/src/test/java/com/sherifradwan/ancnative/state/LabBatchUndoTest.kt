package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.ClinicalCommand
import com.sherifradwan.ancnative.storage.ClinicalCommandOutcome
import com.sherifradwan.ancnative.storage.ClinicalCommandReceipt
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.DeleteLabBatchCommand
import com.sherifradwan.ancnative.storage.LabBatchEntry
import com.sherifradwan.ancnative.storage.RecordSnapshot
import com.sherifradwan.ancnative.storage.VaultState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/** The batch offer: one Undo, one command, every row or none. */
class LabBatchUndoTest {
    private val batchId = UUID.randomUUID().toString()
    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
    private val executed = mutableListOf<ClinicalCommand>()
    private lateinit var state: AncState
    private lateinit var coordinator: UndoCoordinator

    @After
    fun tearDown() = scope.cancel()

    private fun lab(id: String) = DemoLabInvestigation(
        id = id, patientId = "p1", date = "2026-11-05", testCategory = "Hematology",
        testName = "Test $id", result = "10.0", unit = "g/dL", referenceRange = null,
        isAbnormal = false, notes = null, revision = 1,
    )

    private fun entry(id: String) = LabBatchEntry(
        labId = id, testName = "Test $id", testCategory = "Hematology",
        result = "10.0", unit = "g/dL", referenceRange = null, isAbnormal = false, notes = null,
    )

    private fun patient(ids: List<String>) = DemoPatient(
        id = "p1", caseId = "P1", displayName = "Patient One", dateOfBirth = "1995-01-01",
        phone = "01000000000", visits = emptyList(), revision = 2,
        labInvestigations = ids.map(::lab),
    )

    private fun receipt() = ClinicalCommandReceipt(
        operationUuid = UUID.randomUUID().toString(), patientId = "p1", recordKind = "LAB_BATCH_SAVE",
        recordId = batchId, committedRevision = 1, committedPatientRevision = 2, fingerprint = "fp",
    )

    private fun start(ids: List<String>) {
        state = AncState(patients = listOf(patient(ids)), vaultState = VaultState.Unlocked(1L))
        coordinator = UndoCoordinator(
            scope = scope,
            execute = { cmd, _ ->
                executed += cmd
                val current = state.patient("p1")!!
                val updated = current.copy(labInvestigations = emptyList(), revision = current.revision + 1)
                ClinicalCommandOutcome.Committed(RecordSnapshot(listOf(updated)), receipt())
            },
            reduce = { state = AncReducer.reduce(state, it) },
            currentState = { state },
            snapshot = { RecordSnapshot(state.patients) },
        )
    }

    @Test
    fun oneOfferCoversTheWholeBatchAndUndoExecutesOneCommand() {
        start(listOf("b1", "b2", "b3"))
        coordinator.offerLabBatchSaveUndo(
            patientId = "p1",
            batchId = batchId,
            entries = listOf(entry("b1"), entry("b2"), entry("b3")),
            receipt = receipt(),
            type = DeletableType.LAB,
            label = "3 lab results",
        )

        val offer = state.undoOffer
        assertNotNull("a batch save must offer one undo", offer)
        assertEquals("3 lab results", offer!!.label)
        assertEquals(UndoAction.SAVE, offer.action)
        assertEquals(DeletableType.LAB, offer.type)
        assertEquals(batchId, offer.recordId)

        coordinator.request(offer.token)

        assertEquals(1, executed.size)
        val command = executed.single()
        assertTrue("the pending action must be the batch delete", command is DeleteLabBatchCommand)
        command as DeleteLabBatchCommand
        assertEquals(3, command.refs.size)
        assertEquals(batchId, command.batchId)
        assertEquals(2L, command.expectedPatientRevision)
        assertTrue("every row goes in one command", state.patient("p1")!!.labInvestigations.isEmpty())
        assertNull(state.undoOffer)
    }

    @Test
    fun noOfferWhenTheSnapshotDoesNotHoldTheSavedRows() {
        start(listOf("b1"))
        coordinator.offerLabBatchSaveUndo(
            patientId = "p1",
            batchId = batchId,
            entries = listOf(entry("b1"), entry("missing")),
            receipt = receipt(),
            type = DeletableType.LAB,
            label = "2 lab results",
        )
        assertNull("a partial snapshot must not offer an undo", state.undoOffer)
    }
}
