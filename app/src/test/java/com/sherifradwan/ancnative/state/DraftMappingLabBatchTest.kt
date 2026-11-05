package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.DraftScreenKind
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The batch survives a lock: its list is encoded into one draft field and restored without loss. */
class DraftMappingLabBatchTest {
    private fun item(id: String, result: String = "") = LabBatchItem(
        labId = id,
        testName = "Test $id",
        category = "Hematology",
        unit = "g/dL",
        result = result,
        referenceRange = "11 - 15",
        isAbnormal = true,
        notes = "note $id",
    )

    private fun draft(items: List<LabBatchItem> = listOf(item("a", "10.8"), item("b"))) = LabBatchDraft(
        patientId = "p1",
        editorSessionId = "s1",
        basePatientRevision = 4,
        date = "2026-11-05",
        labName = "Al Borg",
        items = items,
        cursor = 1,
        tab = LabBatchTab.ENTER,
    )

    private fun state(withBatch: LabBatchDraft?) = AncState(
        patients = emptyList(),
        vaultState = VaultState.Unlocked(1L),
        labBatchDraft = withBatch,
    )

    @Test
    fun theItemListRoundTripsThroughOneField() {
        val items = listOf(item("a", "10.8"), item("b"))
        assertEquals(items, decodeLabBatchItems(encodeLabBatchItems(items)))
    }

    @Test
    fun separatorsInsideFreeTextCannotCorruptTheList() {
        val tricky = item("a").copy(notes = "first\nsecond\u001Fthird")
        val decoded = decodeLabBatchItems(encodeLabBatchItems(listOf(tricky, item("b"))))
        assertEquals(2, decoded.size)
        assertEquals("first second third", decoded[0].notes)
        assertEquals("b", decoded[1].labId)
    }

    @Test
    fun anEmptyBatchIsNotPersisted() {
        assertNull(draftRecordFor(state(draft(emptyList())), nowMillis = 1L))
    }

    @Test
    fun aBatchWithTestsBecomesALabBatchDraftRecord() {
        val record = draftRecordFor(state(draft()), nowMillis = 1L)!!
        assertEquals(DraftScreenKind.LAB_BATCH, record.screenKind)
        assertEquals("p1", record.patientId)
        assertEquals(4L, record.baseRevision)
        assertEquals("2026-11-05", record.fields["date"])
        assertEquals("Al Borg", record.fields["labName"])
        assertEquals("ENTER", record.fields["tab"])
        assertEquals("1", record.fields["cursor"])
        assertEquals(2, decodeLabBatchItems(record.fields["items"]!!).size)
    }

    @Test
    fun restoringTheRecordRebuildsTheBatch() {
        val record = DraftRecord(
            patientId = "p1",
            screenKind = DraftScreenKind.LAB_BATCH,
            editorKind = com.sherifradwan.ancnative.storage.DraftEditorKind.CREATE,
            recordId = null,
            baseRevision = 4,
            fields = mapOf(
                "date" to "2026-11-06",
                "labName" to "Al Mokhtabar",
                "cursor" to "0",
                "tab" to "REVIEW",
                "items" to encodeLabBatchItems(listOf(item("a", "9.9"))),
            ),
            updatedAtMillis = 1L,
        )
        val stub = state(draft(emptyList()).copy(basePatientRevision = 4))
        val restored = applyDraftRecord(stub, record)
        val batch = restored.labBatchDraft!!
        assertEquals("2026-11-06", batch.date)
        assertEquals("Al Mokhtabar", batch.labName)
        assertEquals(LabBatchTab.REVIEW, batch.tab)
        assertEquals(0, batch.cursor)
        assertEquals(1, batch.items.size)
        assertEquals("9.9", batch.items.single().result)
        assertTrue("a restored draft gets a fresh session id", batch.editorSessionId != "s1")
    }
}
