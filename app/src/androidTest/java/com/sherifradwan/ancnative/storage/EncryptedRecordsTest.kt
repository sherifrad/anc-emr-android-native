package com.sherifradwan.ancnative.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.PatientVocabulary
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedRecordsTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun schemaThreeContainsOnlyApprovedTablesColumnsForeignKeysAndIndexes() = withDatabase { opened, _ ->
        val db = opened.database.openHelper.writableDatabase
        val tables = db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    if (!name.startsWith("sqlite_") && name !in setOf("android_metadata", "room_master_table")) add(name)
                }
            }
        }
        assertEquals(
            setOf(
                "patients",
                "visits",
                "vault_metadata",
                "save_receipts",
                "previous_pregnancies",
                "patient_command_receipts",
                "patient_dating",
                "ultrasound_scans",
                "lab_investigations",
                "medication_records",
                "clinical_command_receipts",
            ),
            tables,
        )
        assertEquals(4, db.query("PRAGMA user_version").use { it.moveToFirst(); it.getInt(0) })

        val patients = tableColumns(db, "patients")
        assertEquals(
            setOf(
                "id", "case_id", "display_name", "date_of_birth", "phone", "created_at",
                "patient_status", "archived_at", "archived_reason", "risk_level", "age_text",
                "address_text", "blood_group_text", "medical_history", "surgical_history",
                "family_history", "allergy_history", "gravida", "parity", "abortion",
                "obstetric_notes", "clinical_attention", "revision",
            ),
            patients.keys,
        )
        assertEquals(0, patients.getValue("date_of_birth").notNull)
        assertEquals(0, patients.getValue("phone").notNull)

        val visits = tableColumns(db, "visits")
        assertEquals(
            setOf(
                "id", "patient_id", "recorded_date", "blood_pressure", "weight",
                "findings", "notes", "linked_ultrasound_id", "revision", "is_synthetic_edit",
            ),
            visits.keys,
        )
        listOf("blood_pressure", "weight", "findings", "notes").forEach {
            assertEquals(0, visits.getValue(it).notNull)
        }
        assertTrue(
            listOf(
                "date_of_birth", "phone", "created_at", "patient_status", "archived_at",
                "archived_reason", "risk_level", "age_text", "address_text", "blood_group_text",
                "medical_history", "surgical_history", "family_history", "allergy_history",
                "gravida", "parity", "abortion", "obstetric_notes", "clinical_attention",
            ).all { patients.getValue(it).defaultValue == null },
        )
        val pregnancies = tableColumns(db, "previous_pregnancies")
        assertEquals(
            setOf(
                "id", "patient_id", "model_version", "date_text", "outcome_text",
                "pregnancy_type", "other_details", "place_and_doctor", "delivery_by", "outcome",
                "birth_method", "loss_management", "termination_method", "provenance_kind",
                "clinician_id", "recorded_at", "updated_at", "revision",
            ),
            pregnancies.keys,
        )
        assertEquals(
            setOf(
                "operation_uuid", "patient_id", "record_kind", "record_id", "committed_revision",
                "committed_patient_revision", "fingerprint",
            ),
            tableColumns(db, "patient_command_receipts").keys,
        )

        val dating = tableColumns(db, "patient_dating")
        assertEquals(
            setOf(
                "patient_id", "lmp_date", "ultrasound_edd", "agreed_edd", "dating_method",
                "transfer_date", "embryo_age_days", "notes", "revision",
            ),
            dating.keys,
        )

        val ultrasounds = tableColumns(db, "ultrasound_scans")
        assertEquals(
            setOf(
                "id", "patient_id", "scan_date", "ga_weeks", "ga_days", "fetal_number",
                "presentation", "placental_site", "amniotic_fluid", "fetal_heart_rate",
                "biometry_bpd", "biometry_hc", "biometry_ac", "biometry_fl",
                "estimated_fetal_weight", "impression", "linked_visit_id", "image_uris", "revision",
            ),
            ultrasounds.keys,
        )

        val labs = tableColumns(db, "lab_investigations")
        assertEquals(
            setOf(
                "id", "patient_id", "test_date", "test_category", "test_name", "result",
                "unit", "reference_range", "is_abnormal", "notes", "lab_name", "revision",
            ),
            labs.keys,
        )

        val meds = tableColumns(db, "medication_records")
        assertEquals(
            setOf(
                "id", "patient_id", "name", "dosage", "route", "frequency",
                "start_date", "end_date", "status", "notes", "is_prophylaxis",
                "is_non_compliant", "adverse_effects", "revision",
            ),
            meds.keys,
        )

        val clinicalReceipts = tableColumns(db, "clinical_command_receipts")
        assertEquals(
            setOf(
                "operation_uuid", "patient_id", "record_kind", "record_id", "committed_revision",
                "committed_patient_revision", "fingerprint",
            ),
            clinicalReceipts.keys,
        )

        assertEquals(1, countRows(db, "PRAGMA foreign_key_list(visits)"))
        assertEquals(2, countRows(db, "PRAGMA foreign_key_list(save_receipts)"))
        assertEquals(1, countRows(db, "PRAGMA foreign_key_list(previous_pregnancies)"))
        assertEquals(1, countRows(db, "PRAGMA foreign_key_list(patient_command_receipts)"))
        assertEquals(1, countRows(db, "PRAGMA foreign_key_list(patient_dating)"))
        assertEquals(1, countRows(db, "PRAGMA foreign_key_list(ultrasound_scans)"))
        assertEquals(1, countRows(db, "PRAGMA foreign_key_list(lab_investigations)"))
        assertEquals(1, countRows(db, "PRAGMA foreign_key_list(medication_records)"))
        assertEquals(1, countRows(db, "PRAGMA foreign_key_list(clinical_command_receipts)"))

        assertTrue(indexNames(db, "patients").any { it.contains("case_id") })
        assertTrue(indexNames(db, "visits").any { it.contains("patient_id") })
        assertTrue(indexNames(db, "save_receipts").any { it.contains("patient_id") })
        assertTrue(indexNames(db, "save_receipts").any { it.contains("visit_id") })
        assertTrue(indexNames(db, "previous_pregnancies").any { it.contains("patient_id") })
        assertTrue(indexNames(db, "patient_command_receipts").any { it.contains("patient_id") })
        assertTrue(indexNames(db, "patient_dating").any { it.contains("patient_id") })
        assertTrue(indexNames(db, "ultrasound_scans").any { it.contains("patient_id") })
        assertTrue(indexNames(db, "lab_investigations").any { it.contains("patient_id") })
        assertTrue(indexNames(db, "medication_records").any { it.contains("patient_id") })
        assertTrue(indexNames(db, "clinical_command_receipts").any { it.contains("patient_id") })
    }

    @Test
    fun encryptedSchemaOneMigratesLogicalRowsUnknownsMetadataAndRawVaultUuid() {
        val fixture = schemaOneFixture()
        try {
            createEncryptedSchemaOne(fixture)
            RecordsDatabase.open(
                context,
                fixture.files.databaseFile(fixture.vaultUuid).absolutePath,
                fixture.passphrase,
            ).use { opened ->
                val repository = RecordsRepository(opened.database, fixture.vaultUuid)
                runBlocking {
                    val result = repository.validateAndLoad() as RepositoryOpenResult.Success
                    val patient = result.snapshot.patients.single()
                    assertEquals(SCHEMA_ONE_PATIENT_ID, patient.id)
                    assertEquals("MIGRATION-CASE-1", patient.caseId)
                    assertEquals("Migration Synthetic", patient.displayName)
                    assertEquals("2001-02-03", patient.dateOfBirth)
                    assertNull(patient.phone)
                    assertNull(patient.createdAt)
                    assertNull(patient.status)
                    assertNull(patient.riskLevel)
                    assertNull(patient.bloodGroupText)
                    assertNull(patient.gravida)
                    assertNull(patient.parity)
                    assertNull(patient.abortion)
                    assertEquals(1L, patient.revision)
                    assertTrue(patient.previousPregnancies.isEmpty())
                    assertEquals(1, patient.visits.size)
                    assertEquals(SCHEMA_ONE_VISIT_ID, patient.visits.single().id)
                    assertEquals("schema-one note", patient.visits.single().notes)

                    val dao = opened.database.recordsDao()
                    val receipt = checkNotNull(dao.receipt(SCHEMA_ONE_RECEIPT_OPERATION))
                    assertEquals(SCHEMA_ONE_RECEIPT_OPERATION, receipt.operationUuid)
                    assertEquals(SCHEMA_ONE_PATIENT_ID, receipt.patientId)
                    assertEquals(SCHEMA_ONE_VISIT_ID, receipt.visitId)
                    assertEquals(1, receipt.committedRevision)
                    assertEquals("a".repeat(64), receipt.fingerprint)
                    val metadata = dao.metadataRows().single()
                    assertEquals(fixture.vaultUuid, metadata.vaultUuid)
                    assertEquals(4, metadata.schemaVersion)
                    assertTrue(metadata.isSynthetic)
                    assertTrue(metadata.initialized)
                }
                assertEquals(
                    4,
                    opened.database.openHelper.writableDatabase.query("PRAGMA user_version").use {
                        it.moveToFirst()
                        it.getInt(0)
                    },
                )
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun encryptedSchemaOneWithPrematureP3TableIsRejectedBeforeMigrationWrites() {
        val fixture = schemaOneFixture()
        try {
            createEncryptedSchemaOne(
                fixture,
                extraSchema = "CREATE TABLE previous_pregnancies (id TEXT NOT NULL PRIMARY KEY)",
            )
            val failure = try {
                RecordsDatabase.open(
                    context,
                    fixture.files.databaseFile(fixture.vaultUuid).absolutePath,
                    fixture.passphrase,
                ).use { fail("unexpected schema opened") }
                null
            } catch (caught: Throwable) {
                caught
            }
            assertEquals(VaultBlockReason.UNKNOWN_SCHEMA, RecordsMigration.failureReason(checkNotNull(failure)))

            withEncryptedSchemaOneDatabase(fixture) { db ->
                assertEquals(1, db.query("PRAGMA user_version").use { it.moveToFirst(); it.getInt(0) })
                assertTrue("created_at" !in tableColumns(db, "patients"))
                assertEquals(1, countRows(db, "SELECT id FROM previous_pregnancies"))
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun patientCommandsAreIdempotentRevisionedAndArchiveRejectsFurtherWrites() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            val create = patientCommand(
                operationUuid = "00000000-0000-4000-8000-000000000401",
                patientId = "00000000-0000-4000-8000-000000000402",
                caseId = "P3-CASE-SENSITIVE",
                displayName = "P3 Synthetic Patient",
            )
            val created = repository.execute(create) as PatientCommandOutcome.Committed
            assertEquals(1L, created.receipt.committedRevision)
            val createdPatient = created.snapshot.patients.single { it.id == create.patientId }
            assertNull(createdPatient.dateOfBirth)
            assertNull(createdPatient.phone)
            assertNull(createdPatient.riskLevel)
            assertNull(createdPatient.gravida)

            val caseVariant = patientCommand(
                operationUuid = "00000000-0000-4000-8000-000000000408",
                patientId = "00000000-0000-4000-8000-000000000409",
                caseId = "p3-case-sensitive",
                displayName = "Case-sensitive synthetic patient",
            )
            repository.execute(caseVariant) as PatientCommandOutcome.Committed
            val exactCaseConflict = patientCommand(
                operationUuid = "00000000-0000-4000-8000-00000000040a",
                patientId = "00000000-0000-4000-8000-00000000040b",
                caseId = create.caseId,
                displayName = "Conflicting synthetic patient",
            )
            assertEquals(
                PatientCommandOutcome.Rejected(PatientCommandFailure.CASE_ID_CONFLICT),
                repository.execute(exactCaseConflict),
            )

            val replay = repository.execute(create) as PatientCommandOutcome.Committed
            assertEquals(created.receipt, replay.receipt)
            assertEquals(1, replay.snapshot.patients.count { it.id == create.patientId })
            val changedReplay = create.copy(displayName = "Changed replay", fingerprint = "")
                .withCanonicalFingerprint()
            assertEquals(
                PatientCommandOutcome.Rejected(PatientCommandFailure.DUPLICATE_OPERATION_MISMATCH),
                repository.execute(changedReplay),
            )

            val update = create.copy(
                operationUuid = "00000000-0000-4000-8000-000000000403",
                expectedPatientRevision = 1,
                displayName = "P3 Synthetic Patient Corrected",
                fingerprint = "",
            ).withCanonicalFingerprint()
            val updated = repository.execute(update) as PatientCommandOutcome.Committed
            assertEquals(2L, updated.receipt.committedRevision)
            assertEquals(
                "P3 Synthetic Patient Corrected",
                updated.snapshot.patients.single { it.id == create.patientId }.displayName,
            )

            val archive = ChangePatientStatusCommand(
                operationUuid = "00000000-0000-4000-8000-000000000404",
                editorSessionId = "patient-editor",
                patientId = create.patientId,
                expectedPatientRevision = 2,
                targetStatus = "archived",
                reason = "Synthetic archive test",
                changedAt = "2026-10-11T09:02:00Z",
                fingerprint = "",
            ).withCanonicalFingerprint()
            val archived = repository.execute(archive) as PatientCommandOutcome.Committed
            assertEquals(3L, archived.receipt.committedRevision)
            assertEquals("archived", archived.snapshot.patients.single { it.id == create.patientId }.status)

            val blockedHistories = SaveOtherHistoriesCommand(
                operationUuid = "00000000-0000-4000-8000-000000000405",
                editorSessionId = "histories-editor",
                patientId = create.patientId,
                expectedPatientRevision = 3,
                medicalHistory = "must not persist",
                surgicalHistory = null,
                familyHistory = null,
                allergyHistory = null,
                fingerprint = "",
            ).withCanonicalFingerprint()
            assertEquals(
                PatientCommandOutcome.Rejected(PatientCommandFailure.PATIENT_ARCHIVED),
                repository.execute(blockedHistories),
            )
            assertNull(opened.database.recordsDao().patientCommandReceipt(blockedHistories.operationUuid))
            assertNull(opened.database.recordsDao().patient(create.patientId)?.medicalHistory)
            val blockedVisit = command(
                operation = "00000000-0000-4000-8000-000000000406",
                patientId = create.patientId,
                visitId = "00000000-0000-4000-8000-000000000407",
            )
            assertEquals(
                SaveOutcome.Rejected(SaveFailure.PATIENT_ARCHIVED),
                repository.save(blockedVisit),
            )
            assertNull(opened.database.recordsDao().receipt(blockedVisit.operationUuid))
            assertNull(opened.database.recordsDao().visit(blockedVisit.visitId))
        }
    }

    @Test
    fun previousPregnancyCreateCorrectAndReplayAdvanceBothRevisionsAtomically() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            val patient = DemoFixtures.patients.first()
            val create = pregnancyCommand(
                operationUuid = "00000000-0000-4000-8000-000000000411",
                patientId = patient.id,
                pregnancyId = "00000000-0000-4000-8000-000000000412",
                expectedPatientRevision = 1,
                expectedPregnancyRevision = null,
                outcome = "LiveBirth",
                birthMethod = "Vaginal",
            )
            val created = repository.execute(create) as PatientCommandOutcome.Committed
            assertEquals(1L, created.receipt.committedRevision)
            assertEquals(2L, created.receipt.committedPatientRevision)

            val correction = pregnancyCommand(
                operationUuid = "00000000-0000-4000-8000-000000000413",
                patientId = patient.id,
                pregnancyId = create.pregnancyId,
                expectedPatientRevision = 2,
                expectedPregnancyRevision = 1,
                outcome = "EarlyPregnancyLoss",
                lossManagement = "Medical",
            )
            val corrected = repository.execute(correction) as PatientCommandOutcome.Committed
            assertEquals(2L, corrected.receipt.committedRevision)
            assertEquals(3L, corrected.receipt.committedPatientRevision)
            val pregnancy = corrected.snapshot.patients.single { it.id == patient.id }.previousPregnancies.single()
            assertEquals("EarlyPregnancyLoss", pregnancy.outcome)
            assertNull(pregnancy.birthMethod)
            assertEquals("Medical", pregnancy.lossManagement)
            assertNull(pregnancy.terminationMethod)

            val replay = repository.execute(create) as PatientCommandOutcome.Committed
            assertEquals(created.receipt, replay.receipt)
            assertEquals(2L, replay.snapshot.patients.single { it.id == patient.id }.previousPregnancies.single().revision)
            assertEquals(
                PatientCommandOutcome.Rejected(PatientCommandFailure.STALE_PATIENT_REVISION),
                repository.execute(
                    pregnancyCommand(
                        operationUuid = "00000000-0000-4000-8000-000000000414",
                        patientId = patient.id,
                        pregnancyId = create.pregnancyId,
                        expectedPatientRevision = 2,
                        expectedPregnancyRevision = 1,
                        outcome = "LiveBirth",
                        birthMethod = "Cesarean",
                    ),
                ),
            )
        }
    }

    @Test
    fun operationUuidCollisionsAreRejectedAcrossBothReceiptTables() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            val patient = DemoFixtures.patients.first()
            val visitOperation = "00000000-0000-4000-8000-000000000421"
            repository.save(
                command(
                    operation = visitOperation,
                    patientId = patient.id,
                    visitId = "00000000-0000-4000-8000-000000000422",
                ),
            ) as SaveOutcome.Committed
            assertEquals(
                PatientCommandOutcome.Rejected(PatientCommandFailure.DUPLICATE_OPERATION_MISMATCH),
                repository.execute(historiesCommand(visitOperation, patient.id, 1)),
            )

            val patientOperation = "00000000-0000-4000-8000-000000000423"
            repository.execute(historiesCommand(patientOperation, patient.id, 1)) as PatientCommandOutcome.Committed
            assertEquals(
                SaveOutcome.Rejected(SaveFailure.DUPLICATE_OPERATION_MISMATCH),
                repository.save(
                    command(
                        operation = patientOperation,
                        patientId = patient.id,
                        visitId = "00000000-0000-4000-8000-000000000424",
                    ),
                ),
            )
        }
    }

    @Test
    fun seedSaveReceiptRetryStaleAndCrossPatientRulesAreTransactional() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            val initial = repository.validateAndLoad() as RepositoryOpenResult.Success
            assertEquals(3, initial.snapshot.patients.size)

            val patient = DemoFixtures.patients[0]
            val insert = command(
                operation = "00000000-0000-4000-8000-000000000301",
                patientId = patient.id,
                visitId = "00000000-0000-4000-8000-000000000302",
            )
            val first = repository.save(insert) as SaveOutcome.Committed
            assertEquals(1, first.receipt.committedRevision)
            val replay = repository.save(insert) as SaveOutcome.Committed
            assertEquals(first.receipt, replay.receipt)
            assertEquals(1, replay.snapshot.patients.first { it.id == patient.id }.visits.count { it.id == insert.visitId })

            val tamperedNotes = repository.save(insert.copy(notes = "Changed"))
            assertEquals(SaveOutcome.Rejected(SaveFailure.INVALID_COMMAND), tamperedNotes)
            val tamperedDate = repository.save(insert.copy(date = "2026-10-10"))
            assertEquals(SaveOutcome.Rejected(SaveFailure.INVALID_COMMAND), tamperedDate)
            val changedPayload = insert.copy(notes = "Changed").withCanonicalFingerprint()
            assertEquals(
                SaveOutcome.Rejected(SaveFailure.DUPLICATE_OPERATION_MISMATCH),
                repository.save(changedPayload),
            )
            val changedPatient = insert.copy(patientId = DemoFixtures.patients[1].id).withCanonicalFingerprint()
            assertEquals(
                SaveOutcome.Rejected(SaveFailure.DUPLICATE_OPERATION_MISMATCH),
                repository.save(changedPatient),
            )

            val stale = repository.save(
                command(
                    operation = "00000000-0000-4000-8000-000000000303",
                    patientId = patient.id,
                    visitId = patient.visits.first().id,
                    expectedRevision = 99,
                ),
            )
            assertEquals(SaveOutcome.Rejected(SaveFailure.STALE_REVISION), stale)

            val crossPatient = repository.save(
                command(
                    operation = "00000000-0000-4000-8000-000000000304",
                    patientId = DemoFixtures.patients[1].id,
                    visitId = patient.visits.first().id,
                    expectedRevision = 1,
                ),
            )
            assertEquals(SaveOutcome.Rejected(SaveFailure.CROSS_PATIENT_WRITE), crossPatient)
        }
    }

    @Test
    fun durableReceiptsReplayAfterLaterEditsWithLatestAuthoritativeSnapshot() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            val patient = DemoFixtures.patients[0]
            val visitId = "00000000-0000-4000-8000-000000000312"
            val create = command(
                operation = "00000000-0000-4000-8000-000000000311",
                patientId = patient.id,
                visitId = visitId,
                notes = "created",
            )
            val createReceipt = (repository.save(create) as SaveOutcome.Committed).receipt
            val firstUpdate = command(
                operation = "00000000-0000-4000-8000-000000000313",
                patientId = patient.id,
                visitId = visitId,
                expectedRevision = 1,
                notes = "first update",
            )
            val firstUpdateReceipt = (repository.save(firstUpdate) as SaveOutcome.Committed).receipt

            val replayCreate = repository.save(create) as SaveOutcome.Committed
            assertEquals(createReceipt, replayCreate.receipt)
            assertEquals(
                "first update",
                replayCreate.snapshot.patients.first { it.id == patient.id }.visits.first { it.id == visitId }.notes,
            )

            val laterUpdate = command(
                operation = "00000000-0000-4000-8000-000000000314",
                patientId = patient.id,
                visitId = visitId,
                expectedRevision = 2,
                notes = "later update",
            )
            repository.save(laterUpdate) as SaveOutcome.Committed
            val replayPriorUpdate = repository.save(firstUpdate) as SaveOutcome.Committed
            assertEquals(firstUpdateReceipt, replayPriorUpdate.receipt)
            assertEquals(
                "later update",
                replayPriorUpdate.snapshot.patients.first { it.id == patient.id }.visits.first { it.id == visitId }.notes,
            )
        }
    }

    @Test
    fun exhaustedRevisionIsRejectedBeforeRowOrReceiptMutation() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            val patient = DemoFixtures.patients[0]
            val visitId = patient.visits.first().id
            opened.database.openHelper.writableDatabase.execSQL(
                "UPDATE visits SET revision = ? WHERE id = ?",
                arrayOf<Any>(Int.MAX_VALUE, visitId),
            )
            val operation = "00000000-0000-4000-8000-000000000315"
            val exhausted = command(
                operation = operation,
                patientId = patient.id,
                visitId = visitId,
                expectedRevision = Int.MAX_VALUE,
                notes = "must not persist",
            )

            assertEquals(
                SaveOutcome.Rejected(SaveFailure.REVISION_EXHAUSTED),
                repository.save(exhausted),
            )
            assertEquals(Int.MAX_VALUE, opened.database.recordsDao().visit(visitId)?.revision)
            assertEquals(null, opened.database.recordsDao().receipt(operation))
        }
    }

    @Test
    fun emptyCommittedResultNeverSilentlyReseedsFixtures() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            val db = opened.database.openHelper.writableDatabase
            db.execSQL("DELETE FROM save_receipts")
            db.execSQL("DELETE FROM visits")
            db.execSQL("DELETE FROM patients")
            val result = repository.validateAndLoad() as RepositoryOpenResult.Success
            assertTrue(result.snapshot.patients.isEmpty())
            assertEquals(0, opened.database.recordsDao().patientCount())
        }
    }

    @Test
    fun absentMetadataFailsClosed() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            opened.database.openHelper.writableDatabase.execSQL("DELETE FROM vault_metadata")
            assertEquals(
                RepositoryOpenResult.Failure(VaultBlockReason.VAULT_METADATA_MISSING),
                repository.validateAndLoad(),
            )
        }
    }

    @Test
    fun wrongDatabasePassphraseCannotOpenCiphertext() {
        val namespace = UUID.randomUUID().toString()
        val files = VaultFiles.isolatedTest(context, namespace)
        val vaultUuid = UUID.randomUUID().toString()
        files.prepareDescriptor(VaultDescriptor(vaultUuid, UUID.randomUUID().toString()))
        val correct = ByteArray(32).also(SecureRandom()::nextBytes)
        val wrong = ByteArray(32).also(SecureRandom()::nextBytes)
        try {
            RecordsDatabase.open(context, files.databaseFile(vaultUuid).absolutePath, correct).use { opened ->
                runBlocking { RecordsRepository(opened.database, vaultUuid).seedInitialSyntheticFixtures() }
            }
            var wrongPassphraseOpened = false
            try {
                RecordsDatabase.open(context, files.databaseFile(vaultUuid).absolutePath, wrong).use {
                    wrongPassphraseOpened = true
                }
            } catch (_: Throwable) {
                // Expected: SQLCipher rejects the wrong key before any repository read.
            }
            if (wrongPassphraseOpened) fail("wrong SQLCipher passphrase opened the database")
        } finally {
            correct.fill(0)
            wrong.fill(0)
            files.cleanupRegisteredTestNamespace()
        }
    }

    @Test
    fun clinicalCommandsPersistDatingAndUltrasoundWithReceiptsAndAudit() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            val initial = repository.validateAndLoad() as RepositoryOpenResult.Success
            val patient = initial.snapshot.patients.first()

            val datingCmd = SaveDatingCommand(
                editorSessionId = "session-dating",
                operationUuid = UUID.randomUUID().toString(),
                patientId = patient.id,
                expectedPatientRevision = patient.revision,
                lmpDate = "2026-01-01",
                ultrasoundEdd = "2026-10-08",
                agreedEdd = "2026-10-08",
                datingMethod = "Ultrasound",
                notes = "Dating confirmed",
            )
            val datingOutcome = repository.execute(datingCmd) as ClinicalCommandOutcome.Committed
            assertEquals(1L, datingOutcome.receipt.committedRevision)
            val datingPatient = datingOutcome.snapshot.patients.first { it.id == patient.id }
            assertEquals("2026-01-01", datingPatient.dating?.lmpDate)
            assertEquals("2026-10-08", datingPatient.dating?.agreedEdd)

            val scanId = UUID.randomUUID().toString()
            val usCmd = SaveUltrasoundScanCommand(
                editorSessionId = "session-us",
                operationUuid = UUID.randomUUID().toString(),
                patientId = patient.id,
                expectedPatientRevision = datingOutcome.receipt.committedPatientRevision,
                scanId = scanId,
                expectedScanRevision = null,
                date = "2026-05-15",
                gaWeeks = 20,
                gaDays = 3,
                fetalNumber = "Singleton",
                presentation = "Cephalic",
                placentalSite = "Anterior",
                amnioticFluid = "Normal",
                fetalHeartRate = "142 bpm",
                biometryBpd = "48 mm",
                biometryHc = "175 mm",
                biometryAc = "150 mm",
                biometryFl = "32 mm",
                estimatedFetalWeight = "350 g",
                impression = "Normal morphology",
            )
            val usOutcome = repository.execute(usCmd) as ClinicalCommandOutcome.Committed
            assertEquals(1L, usOutcome.receipt.committedRevision)
            val usPatient = usOutcome.snapshot.patients.first { it.id == patient.id }
            assertEquals(1, usPatient.ultrasoundScans.size)
            assertEquals("2026-05-15", usPatient.ultrasoundScans.first().date)
            assertEquals(20, usPatient.ultrasoundScans.first().gestationalAgeWeeks)
            assertEquals("Cephalic", usPatient.ultrasoundScans.first().presentation)

            // Verify idempotency
            val usReplay = repository.execute(usCmd) as ClinicalCommandOutcome.Committed
            assertEquals(usOutcome.receipt, usReplay.receipt)
        }
    }

    @Test
    fun clinicalCommandsPersistLabAndMedicationWithReceiptsAndAudit() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures()
            val initial = repository.validateAndLoad() as RepositoryOpenResult.Success
            val patient = initial.snapshot.patients.first()

            val labId = UUID.randomUUID().toString()
            val labCmd = SaveLabInvestigationCommand(
                editorSessionId = "session-lab",
                operationUuid = UUID.randomUUID().toString(),
                patientId = patient.id,
                expectedPatientRevision = patient.revision,
                labId = labId,
                expectedLabRevision = null,
                date = "2026-05-20",
                testCategory = "Hematology",
                testName = "Hemoglobin",
                result = "10.5",
                unit = "g/dL",
                referenceRange = "11.0 - 15.0",
                isAbnormal = true,
                notes = "Mild anemia",
            )
            val labOutcome = repository.execute(labCmd) as ClinicalCommandOutcome.Committed
            assertEquals(1L, labOutcome.receipt.committedRevision)
            val labPatient = labOutcome.snapshot.patients.first { it.id == patient.id }
            assertEquals(1, labPatient.labInvestigations.size)
            assertEquals("Hemoglobin", labPatient.labInvestigations.first().testName)
            assertEquals("10.5", labPatient.labInvestigations.first().result)
            assertTrue(labPatient.labInvestigations.first().isAbnormal)

            // Verify lab replay idempotency
            val labReplay = repository.execute(labCmd) as ClinicalCommandOutcome.Committed
            assertEquals(labOutcome.receipt, labReplay.receipt)

            val medId = UUID.randomUUID().toString()
            val medCmd = SaveMedicationCommand(
                editorSessionId = "session-med",
                operationUuid = UUID.randomUUID().toString(),
                patientId = patient.id,
                expectedPatientRevision = labOutcome.receipt.committedPatientRevision,
                medicationId = medId,
                expectedMedicationRevision = null,
                name = "Ferrous Sulfate",
                dosage = "200 mg",
                route = "Oral",
                frequency = "Once daily",
                startDate = "2026-05-20",
                endDate = "2026-06-20",
                status = "Active",
                notes = "Iron supplementation",
                isProphylaxis = false,
            )
            val medOutcome = repository.execute(medCmd) as ClinicalCommandOutcome.Committed
            assertEquals(1L, medOutcome.receipt.committedRevision)
            val medPatient = medOutcome.snapshot.patients.first { it.id == patient.id }
            assertEquals(1, medPatient.medications.size)
            assertEquals("Ferrous Sulfate", medPatient.medications.first().name)
            assertEquals("200 mg", medPatient.medications.first().dosage)
            assertEquals("Oral", medPatient.medications.first().route)

            // Verify medication replay idempotency
            val medReplay = repository.execute(medCmd) as ClinicalCommandOutcome.Committed
            assertEquals(medOutcome.receipt, medReplay.receipt)
        }
    }

    private fun withDatabase(block: (OpenedRecordsDatabase, String) -> Unit) {
        val namespace = UUID.randomUUID().toString()
        val files = VaultFiles.isolatedTest(context, namespace)
        val vaultUuid = UUID.randomUUID().toString()
        files.prepareDescriptor(VaultDescriptor(vaultUuid, UUID.randomUUID().toString()))
        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        try {
            RecordsDatabase.open(context, files.databaseFile(vaultUuid).absolutePath, passphrase).use { opened ->
                block(opened, vaultUuid)
            }
        } finally {
            passphrase.fill(0)
            files.cleanupRegisteredTestNamespace()
        }
    }

    private fun command(
        operation: String,
        patientId: String,
        visitId: String,
        expectedRevision: Int? = null,
        notes: String = "Synthetic encrypted repository test",
    ): SaveVisitCommand = SaveVisitCommand(
            operationUuid = operation,
            editorSessionId = "instrumented-editor",
            patientId = patientId,
            visitId = visitId,
            expectedRevision = expectedRevision,
            date = "2026-10-09",
            bloodPressure = null,
            weight = null,
            findings = null,
            notes = notes,
            fingerprint = "",
        ).withCanonicalFingerprint()

    private fun SaveVisitCommand.withCanonicalFingerprint(): SaveVisitCommand =
        copy(fingerprint = canonicalFingerprint())

    private fun patientCommand(
        operationUuid: String,
        patientId: String,
        caseId: String,
        displayName: String,
    ): SavePatientProfileCommand = SavePatientProfileCommand(
        operationUuid = operationUuid,
        editorSessionId = "patient-editor",
        patientId = patientId,
        expectedPatientRevision = null,
        createdAt = "2026-10-11T09:00:00Z",
        caseId = caseId,
        displayName = displayName,
        dateOfBirth = null,
        phone = null,
        riskLevel = null,
        ageText = null,
        addressText = null,
        bloodGroupText = null,
        gravida = null,
        parity = null,
        abortion = null,
        obstetricNotes = null,
        fingerprint = "",
    ).withCanonicalFingerprint() as SavePatientProfileCommand

    private fun historiesCommand(
        operationUuid: String,
        patientId: String,
        expectedRevision: Long,
    ): PatientCommand = SaveOtherHistoriesCommand(
        operationUuid = operationUuid,
        editorSessionId = "histories-editor",
        patientId = patientId,
        expectedPatientRevision = expectedRevision,
        medicalHistory = "Synthetic history",
        surgicalHistory = null,
        familyHistory = null,
        allergyHistory = null,
        fingerprint = "",
    ).withCanonicalFingerprint()

    private fun pregnancyCommand(
        operationUuid: String,
        patientId: String,
        pregnancyId: String,
        expectedPatientRevision: Long,
        expectedPregnancyRevision: Long?,
        outcome: String,
        birthMethod: String? = null,
        lossManagement: String? = null,
    ): SavePreviousPregnancyCommand = SavePreviousPregnancyCommand(
        operationUuid = operationUuid,
        editorSessionId = "pregnancy-editor",
        patientId = patientId,
        expectedPatientRevision = expectedPatientRevision,
        pregnancyId = pregnancyId,
        expectedPregnancyRevision = expectedPregnancyRevision,
        modelVersion = 3,
        dateText = null,
        outcomeText = null,
        pregnancyType = null,
        otherDetails = null,
        placeAndDoctor = null,
        deliveryBy = null,
        outcome = outcome,
        birthMethod = birthMethod,
        lossManagement = lossManagement,
        terminationMethod = null,
        provenanceKind = PatientVocabulary.CLINICIAN_ENTRY,
        clinicianId = "synthetic-clinician",
        recordedAt = "2026-10-11T09:10:00Z",
        updatedAt = if (expectedPregnancyRevision == null) "2026-10-11T09:10:00Z" else "2026-10-11T09:11:00Z",
        fingerprint = "",
    ).withCanonicalFingerprint() as SavePreviousPregnancyCommand

    private fun schemaOneFixture(): SchemaOneFixture {
        val namespace = UUID.randomUUID().toString()
        val files = VaultFiles.isolatedTest(context, namespace)
        val vaultUuid = UUID.randomUUID().toString()
        files.prepareDescriptor(VaultDescriptor(vaultUuid, UUID.randomUUID().toString()))
        return SchemaOneFixture(files, vaultUuid, ByteArray(32).also(SecureRandom()::nextBytes))
    }

    private fun createEncryptedSchemaOne(fixture: SchemaOneFixture, extraSchema: String? = null) {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(fixture.files.databaseFile(fixture.vaultUuid).absolutePath)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE patients (" +
                            "id TEXT NOT NULL, case_id TEXT NOT NULL, display_name TEXT NOT NULL, " +
                            "date_of_birth TEXT, phone TEXT, PRIMARY KEY(id))",
                    )
                    db.execSQL("CREATE UNIQUE INDEX index_patients_case_id ON patients (case_id)")
                    db.execSQL(
                        "CREATE TABLE visits (" +
                            "id TEXT NOT NULL, patient_id TEXT NOT NULL, recorded_date TEXT NOT NULL, " +
                            "blood_pressure TEXT, weight TEXT, findings TEXT, notes TEXT, " +
                            "revision INTEGER NOT NULL, is_synthetic_edit INTEGER NOT NULL, PRIMARY KEY(id), " +
                            "FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_visits_patient_id ON visits (patient_id)")
                    db.execSQL(
                        "CREATE TABLE vault_metadata (" +
                            "vault_uuid TEXT NOT NULL, schema_version INTEGER NOT NULL, " +
                            "is_synthetic INTEGER NOT NULL, initialized INTEGER NOT NULL, PRIMARY KEY(vault_uuid))",
                    )
                    db.execSQL(
                        "CREATE TABLE save_receipts (" +
                            "operation_uuid TEXT NOT NULL, patient_id TEXT NOT NULL, visit_id TEXT NOT NULL, " +
                            "committed_revision INTEGER NOT NULL, fingerprint TEXT NOT NULL, " +
                            "PRIMARY KEY(operation_uuid), " +
                            "FOREIGN KEY(patient_id) REFERENCES patients(id) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
                            "FOREIGN KEY(visit_id) REFERENCES visits(id) ON UPDATE RESTRICT ON DELETE RESTRICT)",
                    )
                    db.execSQL("CREATE INDEX index_save_receipts_patient_id ON save_receipts (patient_id)")
                    db.execSQL("CREATE INDEX index_save_receipts_visit_id ON save_receipts (visit_id)")
                    db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                    db.execSQL(
                        "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                        arrayOf<Any>(SCHEMA_ONE_IDENTITY_HASH),
                    )
                    extraSchema?.let(db::execSQL)
                    db.execSQL(
                        "INSERT INTO patients(id, case_id, display_name, date_of_birth, phone) VALUES (?, ?, ?, ?, NULL)",
                        arrayOf<Any>(SCHEMA_ONE_PATIENT_ID, "MIGRATION-CASE-1", "Migration Synthetic", "2001-02-03"),
                    )
                    db.execSQL(
                        "INSERT INTO visits(id, patient_id, recorded_date, blood_pressure, weight, findings, notes, revision, is_synthetic_edit) " +
                            "VALUES (?, ?, ?, NULL, NULL, NULL, ?, 1, 1)",
                        arrayOf<Any>(SCHEMA_ONE_VISIT_ID, SCHEMA_ONE_PATIENT_ID, "2026-10-10", "schema-one note"),
                    )
                    db.execSQL(
                        "INSERT INTO vault_metadata(vault_uuid, schema_version, is_synthetic, initialized) VALUES (?, 1, 1, 1)",
                        arrayOf<Any>(fixture.vaultUuid),
                    )
                    db.execSQL(
                        "INSERT INTO save_receipts(operation_uuid, patient_id, visit_id, committed_revision, fingerprint) " +
                            "VALUES (?, ?, ?, 1, ?)",
                        arrayOf<Any>(SCHEMA_ONE_RECEIPT_OPERATION, SCHEMA_ONE_PATIENT_ID, SCHEMA_ONE_VISIT_ID, "a".repeat(64)),
                    )
                    if (extraSchema != null) {
                        db.execSQL("INSERT INTO previous_pregnancies(id) VALUES ('premature-row')")
                    }
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        SupportOpenHelperFactory(fixture.passphrase.copyOf(), null, true).create(configuration).use { helper ->
            helper.writableDatabase
        }
    }

    private fun withEncryptedSchemaOneDatabase(
        fixture: SchemaOneFixture,
        block: (SupportSQLiteDatabase) -> Unit,
    ) {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(fixture.files.databaseFile(fixture.vaultUuid).absolutePath)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        SupportOpenHelperFactory(fixture.passphrase.copyOf(), null, true).create(configuration).use { helper ->
            block(helper.writableDatabase)
        }
    }

    private fun tableColumns(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        table: String,
    ): Map<String, ColumnInfo> = db.query("PRAGMA table_info($table)").use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                put(
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    ColumnInfo(
                        notNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull")),
                        defaultValue = cursor.getString(cursor.getColumnIndexOrThrow("dflt_value")),
                    ),
                )
            }
        }
    }

    private fun countRows(db: androidx.sqlite.db.SupportSQLiteDatabase, query: String): Int =
        db.query(query).use { cursor ->
            var count = 0
            while (cursor.moveToNext()) count += 1
            count
        }

    private fun indexNames(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA index_list($table)").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
        }

    private data class ColumnInfo(val notNull: Int, val defaultValue: String?)

    private data class SchemaOneFixture(
        val files: VaultFiles,
        val vaultUuid: String,
        val passphrase: ByteArray,
    ) {
        fun close() {
            passphrase.fill(0)
            files.cleanupRegisteredTestNamespace()
        }
    }

    private companion object {
        const val SCHEMA_ONE_PATIENT_ID = "00000000-0000-4000-8000-000000000431"
        const val SCHEMA_ONE_VISIT_ID = "00000000-0000-4000-8000-000000000432"
        const val SCHEMA_ONE_RECEIPT_OPERATION = "00000000-0000-4000-8000-000000000433"
        const val SCHEMA_ONE_IDENTITY_HASH = "3f70ff532f730058d815244c633b2166"
    }
}
