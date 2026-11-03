package com.sherifradwan.ancnative.storage

import android.database.Cursor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
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
private typealias Row = Map<String, String?>
private typealias Dump = Map<String, List<Row>>

@RunWith(Parameterized::class)
class ClinicalDeleteRestoreDbTest(private val kindName: String) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun kinds(): List<String> = listOf("medication", "lab", "ultrasound", "visit", "previous_pregnancy")
    }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ---- per-kind adapter (only builds commands / reads rows; no production rules) ----------------------------

    private interface Kind {
        fun save(patientId: String, patientRevision: Long, id: String, tag: String): ClinicalCommand
        suspend fun create(repo: RecordsRepository, patientId: String, patientRevision: Long, id: String, tag: String) {
            val outcome = repo.execute(save(patientId, patientRevision, id, tag))
            assertTrue("setup save must commit but was $outcome", outcome is ClinicalCommandOutcome.Committed)
        }
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
        val table: String
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
            DeleteRecordCommand(
                type = DeletableType.MEDICATION, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, recordId = id, expectedRecordRevision = revision,
                deletedContentHash = hash,
            )

        override fun res(op: String, patientId: String, patientRevision: Long, domain: Any, source: ClinicalCommand) =
            RestoreRecordCommand(
                type = DeletableType.MEDICATION, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, record = domain as DemoMedicationRecord,
                sourceDelete = source as DeleteRecordCommand,
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

        override val table = "medication_records"
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
            DeleteRecordCommand(
                type = DeletableType.LAB, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, recordId = id, expectedRecordRevision = revision,
                deletedContentHash = hash,
            )

        override fun res(op: String, patientId: String, patientRevision: Long, domain: Any, source: ClinicalCommand) =
            RestoreRecordCommand(
                type = DeletableType.LAB, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, record = domain as DemoLabInvestigation,
                sourceDelete = source as DeleteRecordCommand,
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

        override val table = "lab_investigations"
        override val deleteKind = ClinicalRecordKind.LAB_DELETE
        override val restoreKind = ClinicalRecordKind.LAB_RESTORE
    }

    private object UltrasoundKind : Kind {
        override fun save(patientId: String, patientRevision: Long, id: String, tag: String) = SaveUltrasoundScanCommand(
            editorSessionId = "del-test",
            operationUuid = UUID.randomUUID().toString(),
            patientId = patientId,
            expectedPatientRevision = patientRevision,
            scanId = id,
            expectedScanRevision = null,
            date = "2026-10-30",
            gaWeeks = 20,
            gaDays = 3,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Posterior",
            amnioticFluid = "Normal",
            fetalHeartRate = "142",
            biometryBpd = "48",
            biometryHc = "175",
            biometryAc = "150",
            biometryFl = "32",
            estimatedFetalWeight = "350g",
            impression = "Normal anatomy $tag",
        )

        override suspend fun row(dao: RecordsDao, id: String): Any? = dao.ultrasoundScan(id)
        override suspend fun domain(dao: RecordsDao, id: String): Any? = dao.ultrasoundScan(id)?.let {
            DemoUltrasoundScan(
                id = it.id, patientId = it.patientId, date = it.date, gestationalAgeWeeks = it.gaWeeks,
                gestationalAgeDays = it.gaDays, fetalNumber = it.fetalNumber, presentation = it.presentation,
                placentalSite = it.placentalSite, amnioticFluid = it.amnioticFluid, fetalHeartRate = it.fetalHeartRate,
                biometryBpd = it.biometryBpd, biometryHc = it.biometryHc, biometryAc = it.biometryAc,
                biometryFl = it.biometryFl, estimatedFetalWeight = it.estimatedFetalWeight, impression = it.impression,
                linkedVisitId = it.linkedVisitId, imageUris = ImageUriCodec.deserialize(it.imageUris), revision = it.revision,
            )
        }

        override fun revisionOf(domain: Any) = (domain as DemoUltrasoundScan).revision
        override fun patientOf(domain: Any) = (domain as DemoUltrasoundScan).patientId
        override fun hash(domain: Any) = ultrasoundContentHash(domain as DemoUltrasoundScan)
        override fun changed(domain: Any) = (domain as DemoUltrasoundScan).copy(impression = "tampered")
        override fun withRevision(domain: Any, revision: Long) = (domain as DemoUltrasoundScan).copy(revision = revision)
        override fun del(op: String, patientId: String, patientRevision: Long, id: String, revision: Long, hash: String) =
            DeleteRecordCommand(
                type = DeletableType.ULTRASOUND, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, recordId = id, expectedRecordRevision = revision,
                deletedContentHash = hash,
            )

        override fun res(op: String, patientId: String, patientRevision: Long, domain: Any, source: ClinicalCommand) =
            RestoreRecordCommand(
                type = DeletableType.ULTRASOUND, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, record = domain as DemoUltrasoundScan,
                sourceDelete = source as DeleteRecordCommand,
            )

        override suspend fun insertRaw(dao: RecordsDao, domain: Any) {
            val r = domain as DemoUltrasoundScan
            dao.insertUltrasoundScan(
                UltrasoundScanEntity(
                    id = r.id, patientId = r.patientId, date = r.date, gaWeeks = r.gestationalAgeWeeks,
                    gaDays = r.gestationalAgeDays, fetalNumber = r.fetalNumber, presentation = r.presentation,
                    placentalSite = r.placentalSite, amnioticFluid = r.amnioticFluid, fetalHeartRate = r.fetalHeartRate,
                    biometryBpd = r.biometryBpd, biometryHc = r.biometryHc, biometryAc = r.biometryAc,
                    biometryFl = r.biometryFl, estimatedFetalWeight = r.estimatedFetalWeight, impression = r.impression,
                    linkedVisitId = r.linkedVisitId, imageUris = ImageUriCodec.serialize(r.imageUris), revision = r.revision,
                ),
            )
        }

        override val table = "ultrasound_scans"
        override val deleteKind = ClinicalRecordKind.ULTRASOUND_DELETE
        override val restoreKind = ClinicalRecordKind.ULTRASOUND_RESTORE
    }

    private object VisitKind : Kind {
        override fun save(patientId: String, patientRevision: Long, id: String, tag: String): ClinicalCommand =
            error("Use create()")

        override suspend fun create(repo: RecordsRepository, patientId: String, patientRevision: Long, id: String, tag: String) {
            val cmd = SaveVisitCommand(
                operationUuid = UUID.randomUUID().toString(),
                editorSessionId = "del-test",
                patientId = patientId,
                visitId = id,
                expectedRevision = null,
                date = "2026-10-30",
                bloodPressure = "120/80",
                weight = "65 kg",
                findings = "Normal $tag",
                notes = "Visit notes $tag",
                fingerprint = "",
            )
            val outcome = repo.save(cmd.copy(fingerprint = cmd.canonicalFingerprint()))
            assertTrue("setup save must commit but was $outcome", outcome is SaveOutcome.Committed)
        }

        override suspend fun row(dao: RecordsDao, id: String): Any? = dao.visit(id)
        override suspend fun domain(dao: RecordsDao, id: String): Any? = dao.visit(id)?.let {
            DemoVisit(
                id = it.id, patientId = it.patientId, date = it.date, bloodPressure = it.bloodPressure,
                weight = it.weight, findings = it.findings, notes = it.notes, linkedUltrasoundId = it.linkedUltrasoundId,
                revision = it.revision.toLong(), isSyntheticEdit = it.isSyntheticEdit,
            )
        }

        override fun revisionOf(domain: Any) = (domain as DemoVisit).revision
        override fun patientOf(domain: Any) = (domain as DemoVisit).patientId
        override fun hash(domain: Any) = visitContentHash(domain as DemoVisit)
        override fun changed(domain: Any) = (domain as DemoVisit).copy(notes = "tampered")
        override fun withRevision(domain: Any, revision: Long) = (domain as DemoVisit).copy(revision = revision)
        override fun del(op: String, patientId: String, patientRevision: Long, id: String, revision: Long, hash: String) =
            DeleteRecordCommand(
                type = DeletableType.VISIT, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, recordId = id, expectedRecordRevision = revision,
                deletedContentHash = hash,
            )

        override fun res(op: String, patientId: String, patientRevision: Long, domain: Any, source: ClinicalCommand) =
            RestoreRecordCommand(
                type = DeletableType.VISIT, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, record = domain as DemoVisit,
                sourceDelete = source as DeleteRecordCommand,
            )

        override suspend fun insertRaw(dao: RecordsDao, domain: Any) {
            val r = domain as DemoVisit
            dao.insertVisit(
                VisitEntity(
                    id = r.id, patientId = r.patientId, date = r.date, bloodPressure = r.bloodPressure,
                    weight = r.weight, findings = r.findings, notes = r.notes, linkedUltrasoundId = r.linkedUltrasoundId,
                    revision = r.revision.toInt(), isSyntheticEdit = r.isSyntheticEdit,
                ),
            )
        }

        override val table = "visits"
        override val deleteKind = ClinicalRecordKind.VISIT_DELETE
        override val restoreKind = ClinicalRecordKind.VISIT_RESTORE
    }

    private object PreviousPregnancyKind : Kind {
        override fun save(patientId: String, patientRevision: Long, id: String, tag: String): ClinicalCommand =
            error("Use create()")

        override suspend fun create(repo: RecordsRepository, patientId: String, patientRevision: Long, id: String, tag: String) {
            val cmd = SavePreviousPregnancyCommand(
                operationUuid = UUID.randomUUID().toString(),
                editorSessionId = "del-test",
                patientId = patientId,
                expectedPatientRevision = patientRevision,
                pregnancyId = id,
                expectedPregnancyRevision = null,
                modelVersion = 3,
                dateText = "2024-05-12",
                outcomeText = "Live birth $tag",
                pregnancyType = "Singleton",
                otherDetails = "None $tag",
                placeAndDoctor = "City Hospital",
                deliveryBy = "Vaginal",
                outcome = "LiveBirth",
                birthMethod = "Vaginal",
                lossManagement = null,
                terminationMethod = null,
                provenanceKind = "CLINICIAN_ENTRY",
                clinicianId = "dr-smith",
                recordedAt = "2026-01-10T10:00:00Z",
                updatedAt = "2026-01-10T10:00:00Z",
                fingerprint = "",
            )
            val outcome = repo.execute(cmd.withCanonicalFingerprint())
            assertTrue("setup save must commit but was $outcome", outcome is PatientCommandOutcome.Committed)
        }

        override suspend fun row(dao: RecordsDao, id: String): Any? = dao.previousPregnancy(id)
        override suspend fun domain(dao: RecordsDao, id: String): Any? = dao.previousPregnancy(id)?.let {
            DemoPreviousPregnancy(
                id = it.id, patientId = it.patientId, modelVersion = it.modelVersion, dateText = it.dateText,
                outcomeText = it.outcomeText, pregnancyType = it.pregnancyType, otherDetails = it.otherDetails,
                placeAndDoctor = it.placeAndDoctor, deliveryBy = it.deliveryBy, outcome = it.outcome,
                birthMethod = it.birthMethod, lossManagement = it.lossManagement, terminationMethod = it.terminationMethod,
                provenanceKind = it.provenanceKind, clinicianId = it.clinicianId, recordedAt = it.recordedAt,
                updatedAt = it.updatedAt, revision = it.revision,
            )
        }

        override fun revisionOf(domain: Any) = (domain as DemoPreviousPregnancy).revision
        override fun patientOf(domain: Any) = (domain as DemoPreviousPregnancy).patientId
        override fun hash(domain: Any) = previousPregnancyContentHash(domain as DemoPreviousPregnancy)
        override fun changed(domain: Any) = (domain as DemoPreviousPregnancy).copy(outcomeText = "tampered")
        override fun withRevision(domain: Any, revision: Long) = (domain as DemoPreviousPregnancy).copy(revision = revision)
        override fun del(op: String, patientId: String, patientRevision: Long, id: String, revision: Long, hash: String) =
            DeleteRecordCommand(
                type = DeletableType.PREVIOUS_PREGNANCY, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, recordId = id, expectedRecordRevision = revision,
                deletedContentHash = hash,
            )

        override fun res(op: String, patientId: String, patientRevision: Long, domain: Any, source: ClinicalCommand) =
            RestoreRecordCommand(
                type = DeletableType.PREVIOUS_PREGNANCY, operationUuid = op, patientId = patientId,
                expectedPatientRevision = patientRevision, record = domain as DemoPreviousPregnancy,
                sourceDelete = source as DeleteRecordCommand,
            )

        override suspend fun insertRaw(dao: RecordsDao, domain: Any) {
            val r = domain as DemoPreviousPregnancy
            dao.insertPreviousPregnancy(
                PreviousPregnancyEntity(
                    id = r.id, patientId = r.patientId, modelVersion = r.modelVersion, dateText = r.dateText,
                    outcomeText = r.outcomeText, pregnancyType = r.pregnancyType, otherDetails = r.otherDetails,
                    placeAndDoctor = r.placeAndDoctor, deliveryBy = r.deliveryBy, outcome = r.outcome,
                    birthMethod = r.birthMethod, lossManagement = r.lossManagement, terminationMethod = r.terminationMethod,
                    provenanceKind = r.provenanceKind, clinicianId = r.clinicianId, recordedAt = r.recordedAt,
                    updatedAt = r.updatedAt, revision = r.revision,
                ),
            )
        }

        override val table = "previous_pregnancies"
        override val deleteKind = ClinicalRecordKind.PREVIOUS_PREGNANCY_DELETE
        override val restoreKind = ClinicalRecordKind.PREVIOUS_PREGNANCY_RESTORE
    }

    private val kind: Kind get() = when (kindName) {
        "medication" -> MedKind
        "lab" -> LabKind
        "ultrasound" -> UltrasoundKind
        "visit" -> VisitKind
        "previous_pregnancy" -> PreviousPregnancyKind
        else -> throw IllegalArgumentException("Unknown kind $kindName")
    }
    private val otherKind: Kind get() = when (kindName) {
        "medication" -> LabKind
        "lab" -> UltrasoundKind
        "ultrasound" -> VisitKind
        "visit" -> PreviousPregnancyKind
        "previous_pregnancy" -> MedKind
        else -> throw IllegalArgumentException("Unknown kind $kindName")
    }

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

    private suspend fun Env.create(patientId: String, tag: String): String = createOf(kind, patientId, UUID.randomUUID().toString(), tag)

    private suspend fun Env.createOf(of: Kind, patientId: String, id: String, tag: String): String {
        of.create(repo, patientId, patientRev(patientId), id, tag)
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

    // ---- 8. isolation and database integrity (D654): whole-database snapshot diffs --------------------------

    private class TableDiff(val removed: List<Row>, val added: List<Row>)

    private fun cellText(c: Cursor, i: Int): String? = when (c.getType(i)) {
        Cursor.FIELD_TYPE_NULL -> null
        Cursor.FIELD_TYPE_BLOB -> c.getBlob(i).joinToString("") { "%02x".format(it) }
        else -> c.getString(i)
    }

    /** Every table found in sqlite_master, so a table added later is covered without touching this test. */
    private fun Env.dump(): Dump {
        val sdb = db.openHelper.writableDatabase
        val tables = sdb.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        return tables.associateWith { table ->
            sdb.query("SELECT * FROM \"$table\"").use { c ->
                buildList<Row> {
                    while (c.moveToNext()) add((0 until c.columnCount).associate { c.getColumnName(it) to cellText(c, it) })
                }.sortedBy { it.toString() }
            }
        }
    }

    private fun diff(before: Dump, after: Dump): Map<String, TableDiff> {
        assertEquals("tables appeared or vanished", before.keys, after.keys)
        return before.keys.mapNotNull { table ->
            val removed = before.getValue(table).toMutableList()
            val added = after.getValue(table).toMutableList()
            before.getValue(table).forEach { added.remove(it) }
            after.getValue(table).forEach { removed.remove(it) }
            if (removed.isEmpty() && added.isEmpty()) null else table to TableDiff(removed, added)
        }.toMap()
    }

    private fun Env.assertIntegrity() {
        val sdb = db.openHelper.writableDatabase
        sdb.query("PRAGMA foreign_key_check").use { assertEquals("foreign_key_check must return no rows", 0, it.count) }
        sdb.query("PRAGMA integrity_check").use {
            assertEquals(1, it.count)
            assertTrue(it.moveToFirst())
            assertEquals("ok", it.getString(0))
        }
    }

    private fun Dump.row(table: String, id: String): Row = getValue(table).single { it["id"] == id }

    /** Rows that belong to one patient in any table (patient_id column, or the patients row itself). */
    private fun Dump.rowsOfPatient(patientId: String): Map<String, List<Row>> =
        mapValues { (table, rows) -> rows.filter { it["patient_id"] == patientId || (table == "patients" && it["id"] == patientId) } }

    private suspend fun Env.assertRejectedWithoutTrace(expected: ClinicalCommandFailure?, cmd: ClinicalCommand) {
        val before = dump()
        val outcome = repo.execute(cmd)
        if (expected != null) {
            assertRejected(expected, outcome)
        } else {
            assertTrue("expected Rejected but was $outcome", outcome is ClinicalCommandOutcome.Rejected)
        }
        assertEquals("a failed attempt must change no table", emptySet<String>(), diff(before, dump()).keys)
        assertIntegrity()
    }

    /** A committed delete or restore changes exactly: the target row, the patient revision (+1) and one new receipt. */
    private fun assertOnlyTargetChanged(
        before: Dump,
        after: Dump,
        cmd: ClinicalCommand,
        receiptKind: ClinicalRecordKind,
        committedRevision: Long,
        removedRow: Row?,
        addedRow: Row?,
    ) {
        val d = diff(before, after)
        val expectedKeys = if (kind.table == "visits") {
            if (receiptKind == ClinicalRecordKind.VISIT_DELETE && before.containsKey("save_receipts") && before.getValue("save_receipts").any { it["visit_id"] == cmd.recordId }) {
                setOf(kind.table, "patients", "clinical_command_receipts", "save_receipts")
            } else {
                setOf(kind.table, "patients", "clinical_command_receipts")
            }
        } else {
            setOf(kind.table, "patients", "clinical_command_receipts")
        }
        assertEquals(expectedKeys, d.keys)
        assertEquals(listOfNotNull(removedRow), d.getValue(kind.table).removed)
        assertEquals(listOfNotNull(addedRow), d.getValue(kind.table).added)

        val oldPatient = before.row("patients", cmd.patientId)
        val newRevision = oldPatient.getValue("revision")!!.toLong() + 1
        assertEquals(listOf(oldPatient), d.getValue("patients").removed)
        assertEquals(listOf(oldPatient + ("revision" to newRevision.toString())), d.getValue("patients").added)

        assertTrue(d.getValue("clinical_command_receipts").removed.isEmpty())
        val receipt = d.getValue("clinical_command_receipts").added.single()
        assertEquals(cmd.operationUuid, receipt["operation_uuid"])
        assertEquals(cmd.patientId, receipt["patient_id"])
        assertEquals(receiptKind.name, receipt["record_kind"])
        assertEquals(cmd.recordId, receipt["record_id"])
        assertEquals(committedRevision.toString(), receipt["committed_revision"])
        assertEquals(newRevision.toString(), receipt["committed_patient_revision"])
    }

    @Test
    fun snapshotDiffShowsOnlyTargetRowPatientRevisionAndReceipt() = withEnv {
        val id = create(patientA, "target")
        create(patientA, "sibling")
        createOf(otherKind, patientA, UUID.randomUUID().toString(), "other kind")
        val idB = create(patientB, "other patient")
        assertTrue(
            dump().keys.containsAll(
                listOf("patients", "medication_records", "lab_investigations", "patient_attachments", "clinical_command_receipts"),
            ),
        )
        assertIntegrity()
        val original = kind.domain(dao, id)!!
        val r = kind.revisionOf(original)

        // failed deletes change nothing at all
        assertRejectedWithoutTrace(ClinicalCommandFailure.STALE_REVISION, kind.del(UUID.randomUUID().toString(), patientA, patientRev(), id, r + 1, kind.hash(original)))
        assertRejectedWithoutTrace(ClinicalCommandFailure.STALE_PATIENT_REVISION, kind.del(UUID.randomUUID().toString(), patientA, patientRev() + 1, id, r, kind.hash(original)))
        assertRejectedWithoutTrace(ClinicalCommandFailure.STALE_REVISION, kind.del(UUID.randomUUID().toString(), patientA, patientRev(), id, r, kind.hash(kind.changed(original))))
        assertRejectedWithoutTrace(ClinicalCommandFailure.CROSS_PATIENT_WRITE, kind.del(UUID.randomUUID().toString(), patientB, patientRev(patientB), id, r, kind.hash(original)))

        // the delete
        val beforeDelete = dump()
        val del = deleteCmd(id)
        committed(repo.execute(del))
        val afterDelete = dump()
        assertOnlyTargetChanged(beforeDelete, afterDelete, del, kind.deleteKind, r + 1, removedRow = beforeDelete.row(kind.table, id), addedRow = null)
        assertIntegrity()

        // failed restores change nothing at all
        assertRejectedWithoutTrace(ClinicalCommandFailure.INVALID_COMMAND, kind.res(UUID.randomUUID().toString(), patientA, patientRev(), kind.changed(original), del))
        assertRejectedWithoutTrace(ClinicalCommandFailure.STALE_PATIENT_REVISION, kind.res(UUID.randomUUID().toString(), patientA, patientRev() - 1, original, del))
        assertRejectedWithoutTrace(ClinicalCommandFailure.INVALID_COMMAND, kind.res(UUID.randomUUID().toString(), patientB, patientRev(patientB), original, del))

        // the restore
        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)
        committed(repo.execute(restore))
        val afterRestore = dump()
        val restoredRow = beforeDelete.row(kind.table, id) + ("revision" to (r + 2).toString())
        assertOnlyTargetChanged(afterDelete, afterRestore, restore, kind.restoreKind, r + 2, removedRow = null, addedRow = restoredRow)
        assertIntegrity()

        // replaying the committed restore changes nothing
        committed(repo.execute(restore))
        assertEquals(emptySet<String>(), diff(afterRestore, dump()).keys)

        // delete again, then a newer row appears out of band: a fresh restore is refused and nothing changes
        val del2 = deleteCmd(id)
        committed(repo.execute(del2))
        val newer = kind.withRevision(kind.changed(original), r + 9)
        kind.insertRaw(dao, newer)
        assertRejectedWithoutTrace(ClinicalCommandFailure.RECORD_CONFLICT, kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del2))
        assertEquals(newer, kind.domain(dao, id))

        // archived patient
        assertEquals(1, dao.archivePatient(patientB, patientRev(patientB), "2026-10-30", "demo archive"))
        val domainB = kind.domain(dao, idB)!!
        assertRejectedWithoutTrace(
            ClinicalCommandFailure.PATIENT_ARCHIVED,
            kind.del(UUID.randomUUID().toString(), patientB, patientRev(patientB), idB, kind.revisionOf(domainB), kind.hash(domainB)),
        )
    }

    @Test
    fun crossKindSameIdCollisionLeavesTheOtherKindUntouched() = withEnv {
        val shared = UUID.randomUUID().toString()
        createOf(kind, patientA, shared, "target")
        createOf(otherKind, patientA, shared, "bystander")
        val original = kind.domain(dao, shared)!!
        val bystander = otherKind.domain(dao, shared)!!
        val bystanderRow = dump().row(otherKind.table, shared)
        val r = kind.revisionOf(original)
        assertIntegrity()

        // the other kind's content hash must not authorise a delete of this kind's record
        assertRejectedWithoutTrace(
            ClinicalCommandFailure.STALE_REVISION,
            kind.del(UUID.randomUUID().toString(), patientA, patientRev(), shared, r, otherKind.hash(bystander)),
        )

        val beforeDelete = dump()
        val del = deleteCmd(shared)
        committed(repo.execute(del))
        val afterDelete = dump()
        assertOnlyTargetChanged(beforeDelete, afterDelete, del, kind.deleteKind, r + 1, removedRow = beforeDelete.row(kind.table, shared), addedRow = null)
        assertEquals(bystanderRow, afterDelete.row(otherKind.table, shared))
        assertEquals(bystander, otherKind.domain(dao, shared))
        assertNull(kind.row(dao, shared))
        assertIntegrity()

        val restore = kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)
        committed(repo.execute(restore))
        val afterRestore = dump()
        assertOnlyTargetChanged(
            afterDelete, afterRestore, restore, kind.restoreKind, r + 2,
            removedRow = null, addedRow = beforeDelete.row(kind.table, shared) + ("revision" to (r + 2).toString()),
        )
        assertEquals(bystanderRow, afterRestore.row(otherKind.table, shared))
        assertEquals(bystander, otherKind.domain(dao, shared))
        assertIntegrity()
    }

    @Test
    fun twoPatientsWithSameLookingRecordsStayIsolated() = withEnv {
        val a = create(patientA, "same")
        val b = create(patientB, "same")
        val originalA = kind.domain(dao, a)!!
        val originalB = kind.domain(dao, b)!!
        val patientBBefore = dump().rowsOfPatient(patientB)
        assertIntegrity()

        val del = deleteCmd(a)
        committed(repo.execute(del))
        assertEquals("patient B changed by a delete for patient A", patientBBefore, dump().rowsOfPatient(patientB))
        assertIntegrity()

        committed(repo.execute(kind.res(UUID.randomUUID().toString(), patientA, patientRev(), originalA, del)))
        assertEquals("patient B changed by a restore for patient A", patientBBefore, dump().rowsOfPatient(patientB))
        assertEquals(originalB, kind.domain(dao, b))
        assertIntegrity()

        // A's record id with B's patient id: refused, nothing changes
        val restoredA = kind.domain(dao, a)!!
        assertRejectedWithoutTrace(
            ClinicalCommandFailure.CROSS_PATIENT_WRITE,
            kind.del(UUID.randomUUID().toString(), patientB, patientRev(patientB), a, kind.revisionOf(restoredA), kind.hash(restoredA)),
        )
        // B's own record with the hash of A's same-looking record: refused, nothing changes
        assertRejectedWithoutTrace(
            ClinicalCommandFailure.STALE_REVISION,
            kind.del(UUID.randomUUID().toString(), patientB, patientRev(patientB), b, kind.revisionOf(originalB), kind.hash(originalA)),
        )

        // a restore of A's deleted record carrying B's patient id: refused, nothing changes
        val del2 = deleteCmd(a)
        committed(repo.execute(del2))
        assertRejectedWithoutTrace(ClinicalCommandFailure.INVALID_COMMAND, kind.res(UUID.randomUUID().toString(), patientB, patientRev(patientB), originalA, del2))
        assertEquals(patientBBefore, dump().rowsOfPatient(patientB))
        assertNull(kind.row(dao, a))
    }

    @Test
    fun restoreReinsertsIntoTheSameTableForTheSamePatientOnly() = withEnv {
        val id = create(patientA, "target")
        createOf(otherKind, patientA, UUID.randomUUID().toString(), "other kind")
        create(patientB, "other patient")
        val original = kind.domain(dao, id)!!
        val del = deleteCmd(id)
        committed(repo.execute(del))
        assertTrue(dump().none { (_, rows) -> rows.any { it["id"] == id } })

        committed(repo.execute(kind.res(UUID.randomUUID().toString(), patientA, patientRev(), original, del)))

        val after = dump()
        assertEquals(setOf(kind.table), after.filterValues { rows -> rows.any { it["id"] == id } }.keys)
        assertEquals(patientA, after.row(kind.table, id)["patient_id"])
        assertEquals(1, after.getValue(kind.table).count { it["id"] == id })
        assertIntegrity()
    }

    @Test
    fun replaySaveOfDeletedVisitIsRejected() = withEnv {
        if (kind.table != "visits") return@withEnv
        val id = create(patientA, "target")
        val del = deleteCmd(id)
        committed(repo.execute(del))
        val replayCmd = SaveVisitCommand(
            operationUuid = UUID.randomUUID().toString(),
            editorSessionId = "replay-test",
            patientId = patientA,
            visitId = id,
            expectedRevision = null,
            date = "2026-10-30",
            bloodPressure = "120/80",
            weight = "65 kg",
            findings = "Replay",
            notes = "Replay notes",
            fingerprint = "",
        )
        val outcome = repo.save(replayCmd.copy(fingerprint = replayCmd.canonicalFingerprint()))
        assertTrue(
            "replaying save of deleted visit must reject with VISIT_CONFLICT but was $outcome",
            outcome is SaveOutcome.Rejected && outcome.failure == SaveFailure.VISIT_CONFLICT,
        )
    }
}
