package com.sherifradwan.ancnative.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/** Pure contract of the batch commands: what is valid, and what the fingerprint covers. */
class LabBatchCommandTest {
    private fun entry(id: String, result: String = "10.8") = LabBatchEntry(
        labId = id,
        testName = "Haemoglobin",
        testCategory = "Hematology",
        result = result,
        unit = "g/dL",
        referenceRange = null,
        isAbnormal = false,
        notes = null,
    )

    private fun save(entries: List<LabBatchEntry>) = SaveLabBatchCommand(
        operationUuid = UUID.randomUUID().toString(),
        patientId = "p1",
        expectedPatientRevision = 5,
        date = "2026-11-05",
        labName = "Al Borg",
        entries = entries,
    )

    @Test
    fun aWellFormedBatchIsValid() {
        assertTrue(save(listOf(entry("l1"), entry("l2"))).isStructurallyValid())
    }

    @Test
    fun duplicateIdsMakeTheBatchInvalid() {
        assertFalse(save(listOf(entry("l1"), entry("l1"))).isStructurallyValid())
    }

    @Test
    fun aBlankResultMakesTheBatchInvalid() {
        assertFalse(save(listOf(entry("l1"), entry("l2", result = " "))).isStructurallyValid())
    }

    @Test
    fun anEmptyBatchIsInvalid() {
        assertFalse(save(emptyList()).isStructurallyValid())
    }

    @Test
    fun aBadOrMissingDateMakesTheBatchInvalid() {
        fun withDate(value: String) = SaveLabBatchCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "p1",
            expectedPatientRevision = 5,
            date = value,
            labName = null,
            entries = listOf(entry("l1")),
        )
        assertFalse("a non-ISO date must be rejected", withDate("05/11/2026").isStructurallyValid())
        // isValidIsoDate accepts blank because it is written for optional dates: a batch date is required.
        assertFalse("a blank date must be rejected", withDate("").isStructurallyValid())
        assertFalse("a whitespace date must be rejected", withDate("   ").isStructurallyValid())
        assertTrue(withDate("2026-11-05").isStructurallyValid())
    }

    @Test
    fun theFingerprintCoversTheContentAndTheOrder() {
        val a = entry("l1")
        val b = entry("l2")
        val cmd = save(listOf(a, b))
        val base = cmd.computeFingerprint()
        assertEquals(base, cmd.computeFingerprint())
        assertNotEquals(base, cmd.copy(entries = listOf(b, a)).computeFingerprint())
        assertNotEquals(base, cmd.copy(entries = listOf(a, b.copy(result = "11.0"))).computeFingerprint())
    }

    @Test
    fun aDeleteNeedsAtLeastOneCompleteReference() {
        val ok = DeleteLabBatchCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "p1",
            expectedPatientRevision = 5,
            batchId = UUID.randomUUID().toString(),
            refs = listOf(LabBatchRecordRef("l1", 1, "hash")),
        )
        assertTrue(ok.isStructurallyValid())
        assertFalse(ok.copy(refs = emptyList()).isStructurallyValid())
        assertFalse(ok.copy(refs = listOf(LabBatchRecordRef("l1", 0, "hash"))).isStructurallyValid())
        assertFalse(ok.copy(refs = listOf(LabBatchRecordRef("l1", 1, " "))).isStructurallyValid())
    }

    @Test
    fun aRestoreNeedsTombstonePlusOne() {
        val restore = RestoreLabBatchCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "p1",
            expectedPatientRevision = 6,
            batchId = UUID.randomUUID().toString(),
            date = "2026-11-05",
            labName = null,
            entries = listOf(entry("l1")),
            restoredRevision = 3,
        )
        assertTrue(restore.isStructurallyValid())
        // Revision 2 is the tombstone itself; rows come back at tombstone + 1.
        assertFalse(restore.copy(restoredRevision = 2).isStructurallyValid())
        assertFalse(restore.copy(restoredRevision = 1).isStructurallyValid())
        assertFalse(restore.copy(date = "").isStructurallyValid())
    }

    @Test
    fun theEntryHashChangesWithAnyField() {
        val base = labBatchEntryHash(entry("l1"), "2026-11-05", "Al Borg")
        assertEquals(base, labBatchEntryHash(entry("l1"), "2026-11-05", "Al Borg"))
        assertNotEquals(base, labBatchEntryHash(entry("l1", result = "9.9"), "2026-11-05", "Al Borg"))
        assertNotEquals(base, labBatchEntryHash(entry("l1"), "2026-11-06", "Al Borg"))
    }
}
