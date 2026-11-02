package com.sherifradwan.ancnative.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Real RecordsRepository on a real (encrypted, on-device) Room database, demo data only.
 * Covers D600-D613 for BOTH medications and lab investigations (parameterised: "medication", "lab").
 */
@RunWith(Parameterized::class)
class ClinicalDeleteRestoreDbTest(private val kindName: String) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun kinds(): List<String> = listOf("medication", "lab")
    }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ---- per-kind adapter (only builds commands / reads rows; no production rules) ----------------------------

    private interface Kind {
        fun save(patientId: String, patientRevision: Long, id: String, tag: String): ClinicalCommand
        suspend fun row(dao: RecordsDao, id: String): Any?
        suspend fun domain(dao: RecordsDao, id: String): Any?
        fun revisionOf(domain: Any): Long
        fun patientOf(domain: Any): String
        fun hash(domain: Any): String
        fun changed(domain: Any): Any
        fun withRevision(domain: Any, revision: Long): Any
        fun del(op: String, patientId: String, patientRevision: Long, id: String, revision: Long, hash: String): ClinicalCommand
        fun res(op: String, patientId: String, patientRevision: Long, domain: Any, source: ClinicalCommand): ClinicalCommand
        suspend fun insertRaw(dao: RecordsDao, domain: Any)
        val deleteKind: ClinicalRecordKind
        val restoreKind: ClinicalRecordKind
    }

    private object MedKind : Kind {
        override fun save(patientId: String, patientRevision: Long, id: String, tag: String) = SaveMedicationCommand(
            editorSessionId = "del-test",
            operationUuid = UUID.randomUUID().toString(),
            patientId = patientId,
            expectedPatientRevision = patientRevision,
            medicationId = id,
            expectedMedicationRevision = null,
            name = "Ferrous Sulfate $tag",
            dosage = "200 mg",
            route = "Oral",
            frequency = "Once daily",
            startDate = "2026-05-20",
            endDate = "2026-06-20",
            status = "Active",
            notes = "demo $tag",
            isProphylaxis = false,
        )

        override suspend fun row(dao: RecordsDao, id: String): Any? = dao.medication(id)
        override suspend fun domain(dao: RecordsDao, id: String): Any? = dao.medication(id)?.let {
            DemoMedicationRecord(
                id = it.id, patientId = it.patientId, name = it.name, dosage = it.dosage, route = it.route,
                frequency = it.frequency, startDate = it.startDate, endDate = it.endDate, status = it.status,
                notes = it.notes, isProphylaxis = it.isProphylaxis, isNonCompliant = it.isNonCompliant,
                adverseNotes = it.adverseEffects, revision = it.revision,
            )
        }

        override fun revisionOf(domain: Any) = (domain as DemoMedicationRecord).revision
        override fun patientOf(domain: Any) = (domain as DemoMedicationRecord).patientId
        override fun hash(domain: Any) = medicationContentHash(domain as DemoMedicationRecord)
        override fun changed(domain: Any) = (domain as DemoMedicationRecord).copy(notes = "tampered")
        override fun withRevision(domain: Any, revision: Long) = (domain as DemoMedicationRecord).copy(revision = revision)
        override fun del(op: String, patientId: String, patientRevision: Long, id: String, revision: Long, hash: String) =
            DeleteMedicationCommand(
                operationUuid = op, patientId = patientId, expectedPatientRevision = patientRevision,
                medicationId = id, expectedMedicationRevision = revision, deletedContentHash = hash,
            )

        override fun res(op: String, patientId: String, patientRevision: Long, domain: Any, source: ClinicalCommand) =
            RestoreMedicationCommand(
                operationUuid = op, patientId = patientId, expectedPatientRevision = patientRevision,
                record = domain as DemoMedicationRecord, sourceDelete = source as DeleteMedicationCommand,
            )

        override suspend fun insertRaw(dao: RecordsDao, domain: Any) {
            val r = domain as DemoMedicationRecord
            dao.insertMedication(
                MedicationEntity(
                    id = r.id, patientId = r.patientId, name = r.name, dosage = r.dosage, route = r.route,
                    frequency = r.frequency, startDate = r.startDate, endDate = r.endDate, status = r.status,
                    notes = r.notes, isProphylaxis = r.isProphylaxis, isNonCompliant = r.isNonCompliant,
                    adverseEffects = r.adverseNotes, revision = r.revision,
                ),
            )
        }

        override val deleteKind = ClinicalRecordKind.MEDICATION_DELETE
        override val restoreKind = ClinicalRecordKind.MEDICATION_RESTORE
    }

    private object LabKind : Kind {
        override fun save(patientId: String, patientRevision: Long, id: String, tag: String) = SaveLabInvestigationCommand(
            editorSessionId = "del-test",
            operationUuid = UUID.randomUUID().toString(),
            patientId = patientId,
            expectedPatientRevision = patientRevision,
            labId = id,
            expectedLabRevision = null,
            date = "2026-10-30",
            testCategory = "Hematology",
            testName = "Hemoglobin $tag",
            result = "10.5",
            unit = "g/dL",
            referenceRange = "11.0 - 15.0",
            isAbnormal = true,
            notes = "demo $tag",
        )

        override suspend fun row(dao: RecordsDao, id: String): Any? = dao.labInvestigation(id)
        override suspend fun domain(dao: RecordsDao, id: String): Any? = dao.labInvestigation(id)?.let {
            DemoLabInvestigation(
                id = it.id, patientId = it.patientId, date = it.date, testCategory = it.testCategory,
                testName = it.testName, result = it.result, unit = it.unit, referenceRange = it.referenceRange,
                isAbnormal = it.isAbnormal, notes = it.notes, labName = it.labName, revision = it.revision,
            )
        }

        override fun revisionOf(domain: Any) = (domain as DemoLabInvestigation).revision
        override fun patientOf(domain: Any) = (domain as DemoLabInvestigation).patientId
        override fun hash(domain: Any) = labContentHash(domain as DemoLabInvestigation)
        override fun changed(domain: Any) = (domain as DemoLabInvestigation).copy(notes = "tampered")
        override fun withRevision(domain: Any, revision: Long) = (domain as DemoLabInvestigation).copy(revision = revision)
        override fun del(op: String, patientId: String, patientRevision: Long, id: String, revision: Long, hash: String) =
            DeleteLabInvestigationCommand(
                operationUuid = op, patientId = patientId, expectedPatientRevision = patientRevision,
                labId = id, expectedLabRevision = revision, deletedContentHash = hash,
            )

        override fun res(op: String, patientId: String, patientRevision: Long, domain: Any, source: ClinicalCommand) =
            RestoreLabInvestigationCommand(
                operationUuid = op, patientId = patientId, expectedPatientRevision = patientRevision,
                record = domain as DemoLabInvestigation, sourceDelete = source as DeleteLabInvestigationCommand,
            )

        override suspend fun insertRaw(dao: RecordsDao, domain: Any) {
            val r = domain as DemoLabInvestigation
            dao.insertLabInvestigation(
                LabInvestigationEntity(
                    id = r.id, patientId = r.patientId, date = r.date, testCategory = r.testCategory,
                    testName = r.testName, result = r.result, unit = r.unit, referenceRange = r.referenceRange,
                    isAbnormal = r.isAbnormal, notes = r.notes, labName = r.labName, revision = r.revision,
                ),
            )
        }

        override val deleteKind = ClinicalRecordKind.LAB_DELETE
        override val restoreKind = ClinicalRecordKind.LAB_RESTORE
    }

    private val kind: Kind get() = if (kindName == "medication") MedKind else LabKind

    // ---- harness ---------------------------------------------------------------------------------------------

    private class Env(val repo: RecordsRepository, val dao: RecordsDao, val db: RecordsDatabase) {
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
                    Env(repo, opened.database.recordsDao(), opened.database).block()
                }
            }
        } finally {
            passphrase.fill(0)
            files.cleanupRegisteredTestNamespace()
        }
    }

    private suspend fun Env.patientRev(id: String = patientA): Long = dao.patient(id)!!.revision

    private suspend fun Env.create(patientId: String, tag: String): String {
        val id = UUID.randomUUID().toString()
        val outcome = repo.execute(kind.save(patientId, patientRev(patientId), id, tag))
        assertTrue("setup save must commit but was $outcome", outcome is ClinicalCommandOutcome.Committed)
        return id
    }

    private suspend fun Env.deleteCmd(id: String, patientId: String = patientA, op: String = UUID.randomUUID().toString()): ClinicalCommand {
        val d = kind.domain(dao, id)!!
        return kind.del(op, patientId, patientRev(patientId), id, kind.revisionOf(d), kind.hash(d))
    }

    private fun committed(o: ClinicalCommandOutcome): ClinicalCommandOutcome.Committed {
        assertTrue("expected Committed but was $o", o is ClinicalCommandOutcome.Committed)
        return o as ClinicalCommandOutcome.Committed
    }

    private fun assertRejected(expected: ClinicalCommandFailure, o: ClinicalCommandOutcome) =
        assertEquals(ClinicalCommandOutcome.Rejected(expected), o)

    private class State(val patients: List<PatientEntity?>, val rows: List<Any?>, val receipts: Int)

    private suspend fun Env.capture(vararg ids: String) =
        State(listOf(dao.patient(patientA), dao.patient(patientB)), ids.map { kind.row(dao, it) }, dao.clinicalCommandReceiptCount())

    private suspend fun Env.assertUnchanged(before: State, vararg ids: String, operationUuid: String? = null) {
        val after = capture(*ids)
        assertEquals("patients changed", before.patients, after.patients)
        assertEquals("rows changed", before.rows, after.rows)
        assertEquals("receipt count changed", before.receipts, after.receipts)
        if (operationUuid != null) assertNull("receipt leaked", dao.clinicalCommandReceipt(operationUuid))
    }

    // ---- 1. delete happy path --------------------------------------------------------------------------------

    @Test
    fun deleteHappyPathRemovesRowWritesTombstoneAndBumpsPatientRevision() = withEnv {
        val id = create(patientA, "one")
        val before = kind.domain(dao, id)!!
        val r = kind.revisionOf(before)
        val pRev = patientRev()
        val receiptsBefore = dao.clinicalCommandReceiptCount()
        val cmd = deleteCmd(id)

        val result = committed(repo.execute(cmd))

        assertNull(kind.row(dao, id))
        assertEquals(pRev + 1, patientRev())
        assertEquals(receiptsBefore + 1, dao.clinicalCommandReceiptCount())
        val receipt = dao.clinicalCommandReceipt(cmd.operationUuid)!!
        assertEquals(kind.deleteKind.name, receipt.recordKind)
        assertEquals(id, receipt.recordId)
        assertEquals(patientA, receipt.patientId)
        assertEquals(r + 1, receipt.committedRevision)
        assertEquals(pRev + 1, receipt.committedPatientRevision)
        assertEquals(cmd.computeFingerprint(), receipt.fingerprint)
        assertEquals(r + 1, result.receipt.committedRevision)
        assertEquals(pRev + 1, result.receipt.committedPatientRevision)
    }

    // ---- 2. restore after delete -----------------------------------------------------------------------------

    @Test
    fun restoreAfterDeleteReinsertsIdenticalContentAtTombstonePlusOne() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val r = kind.revisionOf(original)
        val del = deleteCmd(id)
        committed(repo.execute(del))
        val pRevAfterDelete = patientRev()
        val receiptsBefore = dao.clinicalCommandReceiptCount()
        assertNull(kind.row(dao, id))

        val restore = kind.res(UUID.randomUUID().toString(), patientA, pRevAfterDelete, original, del)
        val result = committed(repo.execute(restore))

        val restored = kind.domain(dao, id)!!
        assertEquals(r + 2, kind.revisionOf(restored))
        // identical content: only the revision differs from the original
        assertEquals(kind.withRevision(original, r + 2), restored)
        assertEquals(kind.hash(original), kind.hash(restored))
        assertEquals(pRevAfterDelete + 1, patientRev())
        assertEquals(receiptsBefore + 1, dao.clinicalCommandReceiptCount())
        val receipt = dao.clinicalCommandReceipt(restore.operationUuid)!!
        assertEquals(kind.restoreKind.name, receipt.recordKind)
        assertEquals(r + 2, receipt.committedRevision)
        assertEquals(pRevAfterDelete + 1, receipt.committedPatientRevision)
        assertEquals(r + 2, result.receipt.committedRevision)
    }

    // ---- 3. delete rejections change nothing -----------------------------------------------------------------

    @Test
    fun deleteWithStaleRecordRevisionIsStaleRevisionAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val d = kind.domain(dao, id)!!
        val before = capture(id)
        val cmd = kind.del(UUID.randomUUID().toString(), patientA, patientRev(), id, kind.revisionOf(d) + 1, kind.hash(d))
        assertRejected(ClinicalCommandFailure.STALE_REVISION, repo.execute(cmd))
        assertUnchanged(before, id, operationUuid = cmd.operationUuid)
    }

    @Test
    fun deleteWithWrongContentHashIsStaleRevisionAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val d = kind.domain(dao, id)!!
        val before = capture(id)
        val cmd = kind.del(UUID.randomUUID().toString(), patientA, patientRev(), id, kind.revisionOf(d), kind.hash(kind.changed(d)))
        assertRejected(ClinicalCommandFailure.STALE_REVISION, repo.execute(cmd))
        assertUnchanged(before, id, operationUuid = cmd.operationUuid)
    }

    @Test
    fun deleteOfAnotherPatientsRecordIsCrossPatientWriteAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val d = kind.domain(dao, id)!!
        val before = capture(id)
        val cmd = kind.del(UUID.randomUUID().toString(), patientB, patientRev(patientB), id, kind.revisionOf(d), kind.hash(d))
        assertRejected(ClinicalCommandFailure.CROSS_PATIENT_WRITE, repo.execute(cmd))
        assertUnchanged(before, id, operationUuid = cmd.operationUuid)
    }

    @Test
    fun deleteForArchivedPatientIsPatientArchivedAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val d = kind.domain(dao, id)!!
        assertEquals(1, dao.archivePatient(patientA, patientRev(), "2026-10-30", "demo archive"))
        val before = capture(id)
        val cmd = kind.del(UUID.randomUUID().toString(), patientA, patientRev(), id, kind.revisionOf(d), kind.hash(d))
        assertRejected(ClinicalCommandFailure.PATIENT_ARCHIVED, repo.execute(cmd))
        assertUnchanged(before, id, operationUuid = cmd.operationUuid)
    }

    @Test
    fun deleteWithStalePatientRevisionIsStalePatientRevisionAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val d = kind.domain(dao, id)!!
        val before = capture(id)
        val cmd = kind.del(UUID.randomUUID().toString(), patientA, patientRev() + 1, id, kind.revisionOf(d), kind.hash(d))
        assertRejected(ClinicalCommandFailure.STALE_PATIENT_REVISION, repo.execute(cmd))
        assertUnchanged(before, id, operationUuid = cmd.operationUuid)
    }

    @Test
    fun deleteOfUnknownRecordIsRecordNotFoundAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val d = kind.domain(dao, id)!!
        val before = capture(id)
        val cmd = kind.del(UUID.randomUUID().toString(), patientA, patientRev(), UUID.randomUUID().toString(), 1, kind.hash(d))
        assertRejected(ClinicalCommandFailure.RECORD_NOT_FOUND, repo.execute(cmd))
        assertUnchanged(before, id, operationUuid = cmd.operationUuid)
    }

    @Test
    fun deleteForUnknownPatientIsPatientNotFoundAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val d = kind.domain(dao, id)!!
        val before = capture(id)
        val cmd = kind.del(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 1, id, kind.revisionOf(d), kind.hash(d))
        assertRejected(ClinicalCommandFailure.PATIENT_NOT_FOUND, repo.execute(cmd))
        assertUnchanged(before, id, operationUuid = cmd.operationUuid)
    }

    // ---- 4. restore rejections change nothing ----------------------------------------------------------------

    @Test
    fun restoreWithoutAnySourceDeleteIsRecordConflictAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val neverRun = deleteCmd(id) // built but never executed
        val before = capture(id)
        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, neverRun)
        assertRejected(ClinicalCommandFailure.RECORD_CONFLICT, repo.execute(restore))
        assertUnchanged(before, id, operationUuid = restore.operationUuid)
    }

    @Test
    fun restoreWhileRowStillPresentIsRecordConflictAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val del = deleteCmd(id)
        committed(repo.execute(del))
        // somebody re-created the same row out of band; the delete receipt is still the latest one
        kind.insertRaw(dao, kind.withRevision(original, kind.revisionOf(original) + 1))
        val before = capture(id)
        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)
        assertRejected(ClinicalCommandFailure.RECORD_CONFLICT, repo.execute(restore))
        assertUnchanged(before, id, operationUuid = restore.operationUuid)
    }

    @Test
    fun restoreWhenNewerRecordWithSameIdExistsIsRecordConflictAndKeepsTheNewerRecord() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val del = deleteCmd(id)
        committed(repo.execute(del))
        val newer = kind.withRevision(kind.changed(original), kind.revisionOf(original) + 5)
        kind.insertRaw(dao, newer)
        val before = capture(id)
        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)
        assertRejected(ClinicalCommandFailure.RECORD_CONFLICT, repo.execute(restore))
        assertUnchanged(before, id, operationUuid = restore.operationUuid)
        assertEquals(newer, kind.domain(dao, id))
    }

    @Test
    fun restoreWithDifferentContentHashIsInvalidCommandAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val del = deleteCmd(id)
        committed(repo.execute(del))
        val before = capture(id)
        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), kind.changed(original), del)
        assertRejected(ClinicalCommandFailure.INVALID_COMMAND, repo.execute(restore))
        assertUnchanged(before, id, operationUuid = restore.operationUuid)
        assertNull(kind.row(dao, id))
    }

    @Test
    fun restoreOfAnOlderDeleteWhenNotTheLatestReceiptIsRecordConflictAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val firstDelete = deleteCmd(id)
        committed(repo.execute(firstDelete))
        // restore, then delete again: the first delete is no longer the latest receipt for this record
        committed(repo.execute(kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, firstDelete)))
        committed(repo.execute(deleteCmd(id)))
        assertNull(kind.row(dao, id))
        val before = capture(id)
        val staleRestore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, firstDelete)
        assertRejected(ClinicalCommandFailure.RECORD_CONFLICT, repo.execute(staleRestore))
        assertUnchanged(before, id, operationUuid = staleRestore.operationUuid)
    }

    @Test
    fun restoreForArchivedPatientIsPatientArchivedAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val del = deleteCmd(id)
        committed(repo.execute(del))
        assertEquals(1, dao.archivePatient(patientA, patientRev(), "2026-10-30", "demo archive"))
        val before = capture(id)
        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)
        assertRejected(ClinicalCommandFailure.PATIENT_ARCHIVED, repo.execute(restore))
        assertUnchanged(before, id, operationUuid = restore.operationUuid)
    }

    @Test
    fun restoreWithStalePatientRevisionIsStalePatientRevisionAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val del = deleteCmd(id)
        committed(repo.execute(del))
        val before = capture(id)
        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev() - 1, original, del)
        assertRejected(ClinicalCommandFailure.STALE_PATIENT_REVISION, repo.execute(restore))
        assertUnchanged(before, id, operationUuid = restore.operationUuid)
    }

    // ---- 5. replay -------------------------------------------------------------------------------------------

    @Test
    fun replayOfSameDeleteOperationReturnsCommittedAndWritesNoSecondTombstone() = withEnv {
        val id = create(patientA, "one")
        val cmd = deleteCmd(id)
        val first = committed(repo.execute(cmd))
        val pRev = patientRev()
        val receipts = dao.clinicalCommandReceiptCount()

        val second = committed(repo.execute(cmd))

        assertEquals(first.receipt, second.receipt)
        assertEquals(pRev, patientRev())
        assertEquals(receipts, dao.clinicalCommandReceiptCount())
        assertNull(kind.row(dao, id))
    }

    @Test
    fun replayOfSameRestoreOperationUuidIsIdempotentCommittedWithNoSecondInsert() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val del = deleteCmd(id)
        committed(repo.execute(del))
        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)
        val first = committed(repo.execute(restore))
        val rowAfterFirst = kind.row(dao, id)
        val pRev = patientRev()
        val receipts = dao.clinicalCommandReceiptCount()

        // Actual behaviour: execute() finds the stored receipt first and replays it, so the identical
        // command with the same operationUuid is Committed again (not a conflict) and changes nothing.
        val second = committed(repo.execute(restore))

        assertEquals(first.receipt, second.receipt)
        assertEquals(rowAfterFirst, kind.row(dao, id))
        assertEquals(pRev, patientRev())
        assertEquals(receipts, dao.clinicalCommandReceiptCount())
    }

    @Test
    fun secondRestoreWithFreshOperationUuidIsRecordConflictAndChangesNothing() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val del = deleteCmd(id)
        committed(repo.execute(del))
        committed(repo.execute(kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)))
        val before = capture(id)
        val again = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)
        assertRejected(ClinicalCommandFailure.RECORD_CONFLICT, repo.execute(again))
        assertUnchanged(before, id, operationUuid = again.operationUuid)
    }

    @Test
    fun deleteOperationUuidReusedWithDifferentRecordIsDuplicateOperationMismatch() = withEnv {
        val a = create(patientA, "one")
        val b = create(patientA, "two")
        val op = UUID.randomUUID().toString()
        committed(repo.execute(deleteCmd(a, op = op)))
        val before = capture(a, b)
        assertRejected(ClinicalCommandFailure.DUPLICATE_OPERATION_MISMATCH, repo.execute(deleteCmd(b, op = op)))
        assertUnchanged(before, a, b)
        assertNotNull(kind.row(dao, b))
    }

    // ---- 6. atomicity ----------------------------------------------------------------------------------------

    @Test
    fun failureWritingTheDeleteReceiptRollsBackRowRemovalAndPatientRevision() = withEnv {
        val id = create(patientA, "one")
        val before = capture(id)
        val cmd = deleteCmd(id)
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_delete_receipt BEFORE INSERT ON clinical_command_receipts " +
                "WHEN NEW.record_kind = '${kind.deleteKind.name}' BEGIN SELECT RAISE(ABORT, 'forced failure'); END",
        )
        val outcome = repo.execute(cmd)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_delete_receipt")

        assertFalse("must not report success: $outcome", outcome is ClinicalCommandOutcome.Committed)
        assertUnchanged(before, id, operationUuid = cmd.operationUuid)

        // and the system recovers: the same command now succeeds
        committed(repo.execute(cmd))
        assertNull(kind.row(dao, id))
    }

    @Test
    fun failureWritingTheRestoreReceiptRollsBackInsertAndPatientRevision() = withEnv {
        val id = create(patientA, "one")
        val original = kind.domain(dao, id)!!
        val del = deleteCmd(id)
        committed(repo.execute(del))
        val before = capture(id)
        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_restore_receipt BEFORE INSERT ON clinical_command_receipts " +
                "WHEN NEW.record_kind = '${kind.restoreKind.name}' BEGIN SELECT RAISE(ABORT, 'forced failure'); END",
        )
        val outcome = repo.execute(restore)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_restore_receipt")

        assertFalse("must not report success: $outcome", outcome is ClinicalCommandOutcome.Committed)
        assertUnchanged(before, id, operationUuid = restore.operationUuid)
        assertNull(kind.row(dao, id))

        committed(repo.execute(restore))
        assertNotNull(kind.row(dao, id))
    }

    // ---- 7. unrelated records untouched ----------------------------------------------------------------------

    @Test
    fun deleteAndRestoreLeaveOtherRecordsOfTheSamePatientAndOtherPatientsUntouched() = withEnv {
        val target = create(patientA, "target")
        val sibling1 = create(patientA, "sibling1")
        val sibling2 = create(patientA, "sibling2")
        val other = create(patientB, "other")
        val otherPatientBefore = dao.patient(patientB)
        val siblingsBefore = listOf(sibling1, sibling2, other).map { kind.row(dao, it) }
        val original = kind.domain(dao, target)!!

        val del = deleteCmd(target)
        committed(repo.execute(del))
        assertEquals(siblingsBefore, listOf(sibling1, sibling2, other).map { kind.row(dao, it) })
        assertEquals(otherPatientBefore, dao.patient(patientB))
        assertNull(kind.row(dao, target))

        committed(repo.execute(kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)))
        assertEquals(siblingsBefore, listOf(sibling1, sibling2, other).map { kind.row(dao, it) })
        assertEquals(otherPatientBefore, dao.patient(patientB))
        assertNotNull(kind.row(dao, target))
    }
}
