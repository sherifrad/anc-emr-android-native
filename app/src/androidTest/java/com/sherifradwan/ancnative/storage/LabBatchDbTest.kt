package com.sherifradwan.ancnative.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The batch save and its one-action undo on a real encrypted Room database (demo data only). */
@RunWith(AndroidJUnit4::class)
class LabBatchDbTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private class Env(val repo: RecordsRepository, val dao: RecordsDao) {
        val patientA = DemoFixtures.patients[0].id
        val patientB = DemoFixtures.patients[1].id
    }

    private fun withEnv(block: suspend Env.() -> Unit) {
        val namespace = UUID.randomUUID().toString()
        val files = VaultFiles.isolatedTest(context, namespace)
        val vaultUuid = UUID.randomUUID().toString()
        files.prepareDescriptor(VaultDescriptor(vaultUuid, UUID.randomUUID().toString()))
        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        try {
            RecordsDatabase.open(context, files.databaseFile(vaultUuid).absolutePath, passphrase).use { opened ->
                runBlocking {
                    val repo = RecordsRepository(opened.database, vaultUuid)
                    repo.seedInitialSyntheticFixtures(includeDemoPatients = true)
                    Env(repo, opened.database.recordsDao()).block()
                }
            }
        } finally {
            passphrase.fill(0)
            files.cleanupRegisteredTestNamespace()
        }
    }

    private fun entries(count: Int): List<LabBatchEntry> = (1..count).map { i ->
        LabBatchEntry(
            labId = "batch-lab-$i-${UUID.randomUUID()}",
            testName = "Haemoglobin $i",
            testCategory = "Hematology",
            result = "1$i.0",
            unit = "g/dL",
            referenceRange = null,
            isAbnormal = false,
            notes = null,
        )
    }

    private fun domainOf(row: LabInvestigationEntity): DemoLabInvestigation = DemoLabInvestigation(
        id = row.id, patientId = row.patientId, date = row.date, testCategory = row.testCategory,
        testName = row.testName, result = row.result, unit = row.unit, referenceRange = row.referenceRange,
        isAbnormal = row.isAbnormal, notes = row.notes, labName = row.labName, revision = row.revision,
    )

    private suspend fun Env.saveCmd(entries: List<LabBatchEntry>, patientId: String = patientA): SaveLabBatchCommand =
        SaveLabBatchCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = patientId,
            expectedPatientRevision = dao.patient(patientId)!!.revision,
            date = "2026-11-05",
            labName = "Al Borg",
            entries = entries,
        )

    private suspend fun Env.refsOf(entries: List<LabBatchEntry>): List<LabBatchRecordRef> = entries.map { entry ->
        val row = dao.labInvestigation(entry.labId) ?: error("missing row ${entry.labId}")
        LabBatchRecordRef(entry.labId, row.revision, labContentHash(domainOf(row)))
    }

    private fun committed(outcome: ClinicalCommandOutcome): ClinicalCommandOutcome.Committed {
        assertTrue("expected Committed but was $outcome", outcome is ClinicalCommandOutcome.Committed)
        return outcome as ClinicalCommandOutcome.Committed
    }

    @Test
    fun saveWritesEveryRowAndExactlyOneReceipt() = withEnv {
        val entries = entries(3)
        val beforeRevision = dao.patient(patientA)!!.revision
        val beforeReceipts = dao.clinicalCommandReceiptCount()
        val cmd = saveCmd(entries)

        committed(repo.execute(cmd))

        entries.forEach { entry ->
            val row = dao.labInvestigation(entry.labId)
            assertNotNull("row for ${entry.testName} missing", row)
            assertEquals(1L, row!!.revision)
            assertEquals("Al Borg", row.labName)
            assertEquals(entry.result, row.result)
        }
        assertEquals(beforeRevision + 1, dao.patient(patientA)!!.revision)
        assertEquals(beforeReceipts + 1, dao.clinicalCommandReceiptCount())
        val receipt = dao.clinicalCommandReceipt(cmd.operationUuid)
        assertEquals(ClinicalRecordKind.LAB_BATCH_SAVE.name, receipt!!.recordKind)
        assertEquals(cmd.operationUuid, receipt.recordId)
    }

    @Test
    fun oneStaleReferenceAbortsTheWholeDelete() = withEnv {
        val entries = entries(2)
        committed(repo.execute(saveCmd(entries)))
        val refs = refsOf(entries).toMutableList()
        refs[1] = refs[1].copy(contentHash = "deadbeef")

        val beforeRevision = dao.patient(patientA)!!.revision
        val beforeReceipts = dao.clinicalCommandReceiptCount()
        val delete = DeleteLabBatchCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = patientA,
            expectedPatientRevision = beforeRevision,
            batchId = UUID.randomUUID().toString(),
            refs = refs,
        )

        assertEquals(ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_REVISION), repo.execute(delete))
        entries.forEach { assertNotNull(dao.labInvestigation(it.labId)) }
        assertEquals(beforeRevision, dao.patient(patientA)!!.revision)
        assertEquals(beforeReceipts, dao.clinicalCommandReceiptCount())
    }

    @Test
    fun theWholeBatchDeletesAndComesBackAtTombstonePlusOne() = withEnv {
        // Eleven rows is the size the approved mock saves in one action.
        val entries = entries(11)
        committed(repo.execute(saveCmd(entries)))
        val refs = refsOf(entries)
        val batchId = UUID.randomUUID().toString()
        val date = "2026-11-05"
        val deleteId = UUID.randomUUID().toString()
        val restoreId = UUID.randomUUID().toString()

        committed(
            repo.execute(
                DeleteLabBatchCommand(
                    operationUuid = deleteId,
                    patientId = patientA,
                    expectedPatientRevision = dao.patient(patientA)!!.revision,
                    batchId = batchId,
                    refs = refs,
                ),
            ),
        )
        entries.forEach { assertNull(dao.labInvestigation(it.labId)) }
        assertEquals(ClinicalRecordKind.LAB_BATCH_DELETE.name, dao.clinicalCommandReceipt(deleteId)!!.recordKind)

        committed(
            repo.execute(
                RestoreLabBatchCommand(
                    operationUuid = restoreId,
                    patientId = patientA,
                    expectedPatientRevision = dao.patient(patientA)!!.revision,
                    batchId = batchId,
                    date = date,
                    labName = "Al Borg",
                    entries = entries,
                    restoredRevision = 3,
                ),
            ),
        )
        entries.forEach { assertEquals(3L, dao.labInvestigation(it.labId)!!.revision) }
        assertEquals(ClinicalRecordKind.LAB_BATCH_RESTORE.name, dao.clinicalCommandReceipt(restoreId)!!.recordKind)
    }

    @Test
    fun anArchivedPatientCannotHaveABatchDeleted() = withEnv {
        val entries = entries(2)
        committed(repo.execute(saveCmd(entries)))
        val refs = refsOf(entries)
        dao.archivePatient(patientA, dao.patient(patientA)!!.revision, "2026-11-05T00:00:00Z", "test")
        val delete = DeleteLabBatchCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = patientA,
            expectedPatientRevision = dao.patient(patientA)!!.revision,
            batchId = UUID.randomUUID().toString(),
            refs = refs,
        )
        assertEquals(ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_ARCHIVED), repo.execute(delete))
        entries.forEach { assertNotNull(dao.labInvestigation(it.labId)) }
    }

    @Test
    fun aBatchOfAnotherPatientIsRejected() = withEnv {
        val entries = entries(2)
        committed(repo.execute(saveCmd(entries)))
        val refs = refsOf(entries)
        val delete = DeleteLabBatchCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = patientB,
            expectedPatientRevision = dao.patient(patientB)!!.revision,
            batchId = UUID.randomUUID().toString(),
            refs = refs,
        )
        assertEquals(ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.CROSS_PATIENT_WRITE), repo.execute(delete))
        entries.forEach { assertNotNull(dao.labInvestigation(it.labId)) }
    }
}
