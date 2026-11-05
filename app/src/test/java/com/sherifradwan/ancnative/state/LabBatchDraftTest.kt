package com.sherifradwan.ancnative.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The in-memory batch: adding, editing, cursor and the mapping to save entries. */
class LabBatchDraftTest {
    private fun item(id: String, name: String = "Test $id") = LabBatchItem(
        labId = id, testName = name, category = "Hematology", unit = "g/dL",
    )

    private fun draft(items: List<LabBatchItem> = emptyList()) = LabBatchDraft(
        patientId = "p1", editorSessionId = "s1", basePatientRevision = 3,
        date = "2026-11-05", labName = "Al Borg", items = items,
    )

    @Test
    fun aNewBatchStartsEmptyOnTheAddTab() {
        val d = draft()
        assertEquals(0, d.items.size)
        assertEquals(LabBatchTab.ADD, d.tab)
        assertEquals(0, d.filledCount)
        assertNull(d.currentItem)
    }

    @Test
    fun addingTheFirstTestsSwitchesToEnter() {
        val d = draft().add(listOf(item("a"), item("b"), item("c")))
        assertEquals(3, d.items.size)
        assertEquals(LabBatchTab.ENTER, d.tab)
        assertEquals(0, d.cursor)
        assertEquals("Test a", d.currentItem?.testName)
    }

    @Test
    fun addingMoreWhileAlreadyEnteringKeepsTheTab() {
        val d = draft().add(listOf(item("a"))).copy(tab = LabBatchTab.REVIEW).add(listOf(item("b")))
        assertEquals(LabBatchTab.REVIEW, d.tab)
        assertEquals(2, d.items.size)
    }

    @Test
    fun addingTheSameIdTwiceIsIgnoredEvenInsideOneCall() {
        assertEquals(1, draft().add(listOf(item("a"))).add(listOf(item("a"))).items.size)
        assertEquals(1, draft().add(listOf(item("a"), item("a"))).items.size)
    }

    @Test
    fun aResultFillsTheRowAndTheReducerClearsAStaleFlag() {
        var state = AncState(patients = emptyList(), vaultState = com.sherifradwan.ancnative.storage.VaultState.Unlocked(1L))
        state = AncReducer.reduce(state, AncEvent.StartLabBatch("p1", "s1", 3L, "2026-11-05", "Al Borg"))
        state = AncReducer.reduce(state, AncEvent.LabBatchItemsAdded(listOf(item("a"))))
        state = AncReducer.reduce(state, AncEvent.LabBatchFlagChanged("a", true))
        assertEquals(0, state.labBatchDraft!!.filledCount)

        state = AncReducer.reduce(state, AncEvent.LabBatchResultChanged("a", "10.8"))
        val row = state.labBatchDraft!!.item("a")!!
        assertEquals(1, state.labBatchDraft!!.filledCount)
        assertTrue(row.isFilled)
        assertTrue("a new value resets the manual flag", !row.isAbnormal)
    }

    @Test
    fun removingKeepsTheCursorInsideTheList() {
        val d = draft().add(listOf(item("a"), item("b"), item("c"))).moveCursor(2)
        val after = d.remove("c")
        assertEquals(2, after.items.size)
        assertEquals(1, after.cursor)
        assertEquals("removing an unknown id changes nothing", 1, after.remove("zzz").cursor)
        val empty = after.remove("a").remove("b")
        assertEquals(0, empty.items.size)
        assertEquals(0, empty.cursor)
    }

    @Test
    fun theCursorCannotLeaveTheList() {
        val d = draft().add(listOf(item("a"), item("b")))
        assertEquals(1, d.moveCursor(99).cursor)
        assertEquals(0, d.moveCursor(-5).cursor)
        assertEquals(0, draft().moveCursor(4).cursor)
    }

    @Test
    fun entriesAreBuiltFromTheDraftWithBlankOptionalFieldsRemoved() {
        val d = draft()
            .add(listOf(item("a").copy(result = " 10.8 ", referenceRange = "  ", notes = "")))
            .add(listOf(item("b").copy(result = "8", unit = "", referenceRange = "11 - 15")))
        val entries = d.toEntries()
        assertEquals(listOf("a", "b"), entries.map { it.labId })
        assertEquals("10.8", entries[0].result)
        assertNull(entries[0].referenceRange)
        assertNull(entries[0].notes)
        assertNull(entries[1].unit)
        assertEquals("11 - 15", entries[1].referenceRange)
    }

    @Test
    fun theReducerStartsAClearsTheDraft() {
        val state = AncState(patients = emptyList(), vaultState = com.sherifradwan.ancnative.storage.VaultState.Unlocked(1L))
        val started = AncReducer.reduce(state, AncEvent.StartLabBatch("p1", "s1", 3, "2026-11-05", "Al Borg"))
        assertEquals("p1", started.labBatchDraft?.patientId)
        assertEquals(3L, started.labBatchDraft?.basePatientRevision)

        val added = AncReducer.reduce(started, AncEvent.LabBatchItemsAdded(listOf(item("a"))))
        assertEquals(1, added.labBatchDraft?.items?.size)

        val cancelled = AncReducer.reduce(added, AncEvent.CancelLabBatch)
        assertNull(cancelled.labBatchDraft)
        assertEquals(AncRoute.Workspace("p1", WorkspaceSection.LABORATORY), cancelled.route)
    }

    @Test
    fun cancellingTheBatchKeepsAnUnrelatedDraft() {
        val base = AncState(patients = emptyList(), vaultState = com.sherifradwan.ancnative.storage.VaultState.Unlocked(1L))
        val withLabDraft = base.copy(
            labDraft = LabDraft(editorSessionId = "s1", patientId = "p1", labId = "l1", basePatientRevision = 1L, baseLabRevision = null, original = LabDraftValues(), values = LabDraftValues()),
        )
        val started = AncReducer.reduce(withLabDraft, AncEvent.StartLabBatch("p1", "s1", 1L, "2026-11-05", null))
        val cancelled = AncReducer.reduce(started, AncEvent.CancelLabBatch)
        assertNull(cancelled.labBatchDraft)
        assertEquals("an unrelated draft must survive", withLabDraft.labDraft, cancelled.labDraft)
    }

    @Test
    fun aBatchValidationFailureLandsOnTheBatchDraft() {
        val state = AncState(patients = emptyList(), vaultState = com.sherifradwan.ancnative.storage.VaultState.Unlocked(1L))
        val started = AncReducer.reduce(state, AncEvent.StartLabBatch("p1", "s1", 1L, "2026-11-05", null))
        val errors = setOf(ClinicalValidationError.REQUIRED_RESULT)
        val failed = AncReducer.reduce(started, AncEvent.ClinicalValidationFailed(com.sherifradwan.ancnative.storage.ClinicalRecordKind.LAB_BATCH_SAVE, errors))
        assertEquals(errors, failed.labBatchDraft?.errors)
        assertEquals(SavePhase.IDLE, failed.savePhase)
    }

    @Test
    fun batchEventsDoNothingWithoutADraft() {
        val state = AncState(patients = emptyList(), vaultState = com.sherifradwan.ancnative.storage.VaultState.Unlocked(1L))
        val after = AncReducer.reduce(state, AncEvent.LabBatchItemsAdded(listOf(item("a"))))
        assertEquals(state, after)
    }
    @Test
    fun aBatchNeedsADateAndAResultForEveryRow() {
        val empty = draft()
        assertEquals(setOf(ClinicalValidationError.REQUIRED_DATE, ClinicalValidationError.REQUIRED_RESULT), empty.copy(date = "").validationErrors())
        val noResult = draft().add(listOf(item("a")))
        assertEquals(setOf(ClinicalValidationError.REQUIRED_RESULT), noResult.validationErrors())
        val ready = noResult.withItem("a") { it.copy(result = "10.8") }
        assertTrue("a dated batch with every result is valid", ready.validationErrors().isEmpty())
        assertEquals(setOf(ClinicalValidationError.INVALID_DATE), ready.copy(date = "05/11/2026").validationErrors())
        assertEquals(setOf(ClinicalValidationError.REQUIRED_NAME), ready.withItem("a") { it.copy(testName = " ") }.validationErrors())
    }

    @Test
    fun theSaveCommandCarriesTheDraftAndTrimsTheLabName() {
        val d = draft().copy(labName = "  Al Borg  ")
            .add(listOf(item("a").copy(result = "10.8")))
        val command = d.toSaveCommand("op-1", expectedPatientRevision = 7)
        assertEquals("op-1", command.operationUuid)
        assertEquals("p1", command.patientId)
        assertEquals(7L, command.expectedPatientRevision)
        assertEquals("2026-11-05", command.date)
        assertEquals("Al Borg", command.labName)
        assertEquals(1, command.entries.size)
        assertEquals("10.8", command.entries.single().result)
        assertEquals("an empty lab name becomes null", null, d.copy(labName = "   ").toSaveCommand("op-2", 1).labName)
    }

    @Test
    fun aBatchWithTestsCountsAsUnsavedWork() {
        val base = AncState(patients = emptyList(), vaultState = com.sherifradwan.ancnative.storage.VaultState.Unlocked(1L))
        assertTrue("an empty batch is not unsaved work yet", !base.copy(labBatchDraft = draft()).hasDirtyDraft)
        assertTrue(base.copy(labBatchDraft = draft().add(listOf(item("a")))).hasDirtyDraft)
    }

}
