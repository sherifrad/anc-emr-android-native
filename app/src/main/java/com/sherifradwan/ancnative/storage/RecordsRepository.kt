package com.sherifradwan.ancnative.storage

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.PatientVocabulary
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlinx.coroutines.CancellationException

internal class RecordsRepository(
    private val database: RecordsDatabase,
    internal val vaultUuid: String,
) : SaveGateway {
    private val dao = database.recordsDao()

    suspend fun seedInitialSyntheticFixtures() {
        database.withTransaction {
            check(dao.patientCount() == 0)
            check(dao.visitCount() == 0)
            check(dao.metadataCount() == 0)
            check(dao.receiptCount() == 0)
            check(dao.previousPregnancyCount() == 0)
            check(dao.patientCommandReceiptCount() == 0)
            check(dao.patientDatingCount() == 0)
            check(dao.ultrasoundScanCount() == 0)
            check(dao.labInvestigationCount() == 0)
            check(dao.medicationCount() == 0)
            check(dao.clinicalCommandReceiptCount() == 0)
            check(dao.attachmentCount() == 0)
            dao.insertMetadata(
                VaultMetadataEntity(
                    vaultUuid = vaultUuid,
                    schemaVersion = SCHEMA_VERSION,
                    isSynthetic = true,
                    initialized = true,
                ),
            )
            dao.insertPatients(DemoFixtures.patients.map { patient -> patient.toEntity() })
            dao.insertVisits(
                DemoFixtures.patients.flatMap { patient ->
                    patient.visits.map { visit -> visit.toEntity(patient.id) }
                },
            )
            DemoFixtures.patients.forEach { patient ->
                patient.dating?.let { dating ->
                    dao.insertPatientDating(dating.toEntity(patient.id))
                }
                patient.previousPregnancies.forEach { p ->
                    dao.insertPreviousPregnancy(p.toEntity(patient.id))
                }
                patient.ultrasoundScans.forEach { scan ->
                    dao.insertUltrasoundScan(scan.toEntity(patient.id))
                }
                patient.labInvestigations.forEach { lab ->
                    dao.insertLabInvestigation(lab.toEntity(patient.id))
                }
                patient.medications.forEach { med ->
                    dao.insertMedication(med.toEntity(patient.id))
                }
            }
        }
    }

    suspend fun validateAndLoad(): RepositoryOpenResult {
        val supportDatabase = database.openHelper.writableDatabase
        val userVersion = supportDatabase.query("PRAGMA user_version").use { cursor ->
            if (!cursor.moveToFirst()) -1 else cursor.getInt(0)
        }
        if (userVersion != SCHEMA_VERSION) return RepositoryOpenResult.Failure(VaultBlockReason.UNKNOWN_SCHEMA)

        val integrity = supportDatabase.query("PRAGMA cipher_integrity_check").use { cursor ->
            val rows = mutableListOf<String>()
            while (cursor.moveToNext()) rows += cursor.getString(0)
            rows
        }
        if (integrity.isNotEmpty() && integrity.any { !it.equals("ok", ignoreCase = true) }) {
            return RepositoryOpenResult.Failure(VaultBlockReason.DATABASE_INTEGRITY_FAILED)
        }

        val metadata = dao.metadataRows()
        if (metadata.isEmpty()) return RepositoryOpenResult.Failure(VaultBlockReason.VAULT_METADATA_MISSING)
        val row = metadata.singleOrNull()
            ?: return RepositoryOpenResult.Failure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        if (
            row.vaultUuid != vaultUuid ||
            row.schemaVersion != SCHEMA_VERSION ||
            !row.initialized
        ) {
            return RepositoryOpenResult.Failure(VaultBlockReason.VAULT_METADATA_MISMATCH)
        }
        val snapshot = loadSnapshot()
        if (!snapshot.isInternallyValid()) {
            return RepositoryOpenResult.Failure(VaultBlockReason.DATABASE_INTEGRITY_FAILED)
        }
        return RepositoryOpenResult.Success(snapshot)
    }

    suspend fun loadSnapshot(): RecordSnapshot = RecordSnapshot(
        patients = dao.loadPatientsWithVisits().map { relation ->
            relation.patient.toDomain(
                visits = relation.visits.map { it.toDomain() }.sortedByDescending { it.date },
                pregnancies = relation.previousPregnancies
                    .map { it.toDomain() }
                    .sortedByDescending { it.updatedAt },
                dating = relation.dating?.toDomain(),
                ultrasoundScans = relation.ultrasoundScans.map { it.toDomain() }.sortedByDescending { it.date },
                labInvestigations = relation.labInvestigations.map { it.toDomain() }.sortedByDescending { it.date },
                medications = relation.medications.map { it.toDomain() }.sortedByDescending { it.startDate },
                attachments = relation.attachments.map { it.toDomain() }.sortedByDescending { it.capturedAt },
            )
        },
    )

    suspend fun restoreCanonicalBackup(payload: CanonicalBackupPayload): RecordSnapshot {
        return database.withTransaction {
            val db = database.openHelper.writableDatabase
            db.execSQL("DELETE FROM patient_attachments")
            db.execSQL("DELETE FROM clinical_command_receipts")
            db.execSQL("DELETE FROM medications")
            db.execSQL("DELETE FROM lab_investigations")
            db.execSQL("DELETE FROM ultrasound_scans")
            db.execSQL("DELETE FROM patient_dating")
            db.execSQL("DELETE FROM previous_pregnancies")
            db.execSQL("DELETE FROM visits")
            db.execSQL("DELETE FROM patients")
            db.execSQL("DELETE FROM save_receipts")
            db.execSQL("DELETE FROM patient_command_receipts")

            for (p in payload.patients) {
                dao.insertPatient(p.patient)
                if (p.visits.isNotEmpty()) {
                    dao.insertVisits(p.visits)
                }
                p.dating?.let { dao.insertPatientDating(it) }
                for (prev in p.previousPregnancies) {
                    dao.insertPreviousPregnancy(prev)
                }
                for (us in p.ultrasoundScans) {
                    dao.insertUltrasoundScan(us)
                }
                for (lab in p.labInvestigations) {
                    dao.insertLabInvestigation(lab)
                }
                for (med in p.medications) {
                    dao.insertMedication(med)
                }
                if (p.attachments.isNotEmpty()) {
                    dao.insertAttachments(p.attachments)
                }
            }
            loadSnapshot()
        }
    }

    override suspend fun save(command: SaveVisitCommand): SaveOutcome {
        if (!command.isStructurallyValid()) return SaveOutcome.Rejected(SaveFailure.INVALID_COMMAND)
        return try {
            database.withTransaction {
                if (dao.patientCommandReceipt(command.operationUuid) != null || dao.clinicalCommandReceipt(command.operationUuid) != null) {
                    return@withTransaction SaveOutcome.Rejected(SaveFailure.DUPLICATE_OPERATION_MISMATCH)
                }
                val priorReceipt = dao.receipt(command.operationUuid)
                if (priorReceipt != null) return@withTransaction priorReceipt.replay(command)

                val patient = dao.patient(command.patientId)
                    ?: return@withTransaction SaveOutcome.Rejected(SaveFailure.PATIENT_NOT_FOUND)
                if (patient.status == "archived") {
                    return@withTransaction SaveOutcome.Rejected(SaveFailure.PATIENT_ARCHIVED)
                }

                if (command.linkedUltrasoundId != null) {
                    val scan = dao.ultrasoundScan(command.linkedUltrasoundId)
                    if (scan == null || scan.patientId != command.patientId) {
                        return@withTransaction SaveOutcome.Rejected(SaveFailure.CROSS_PATIENT_WRITE)
                    }
                }

                val revision = if (command.expectedRevision == null) {
                    val existing = dao.visit(command.visitId)
                    if (existing != null) {
                        val failure = if (existing.patientId == command.patientId) {
                            SaveFailure.VISIT_CONFLICT
                        } else {
                            SaveFailure.CROSS_PATIENT_WRITE
                        }
                        return@withTransaction SaveOutcome.Rejected(failure)
                    }
                    dao.insertVisit(command.toEntity(revision = 1))
                    1
                } else {
                    if (command.expectedRevision == Int.MAX_VALUE) {
                        return@withTransaction SaveOutcome.Rejected(SaveFailure.REVISION_EXHAUSTED)
                    }
                    val existing = dao.visit(command.visitId)
                        ?: return@withTransaction SaveOutcome.Rejected(SaveFailure.STALE_REVISION)
                    if (existing.patientId != command.patientId) {
                        return@withTransaction SaveOutcome.Rejected(SaveFailure.CROSS_PATIENT_WRITE)
                    }
                    val affected = dao.updateVisit(
                        visitId = command.visitId,
                        patientId = command.patientId,
                        expectedRevision = command.expectedRevision,
                        date = command.date,
                        bloodPressure = command.bloodPressure,
                        weight = command.weight,
                        findings = command.findings,
                        notes = command.notes,
                        linkedUltrasoundId = command.linkedUltrasoundId,
                    )
                    if (affected != 1) return@withTransaction SaveOutcome.Rejected(SaveFailure.STALE_REVISION)
                    command.expectedRevision + 1
                }

                if (command.linkedUltrasoundId != null) {
                    dao.linkUltrasoundToVisit(command.linkedUltrasoundId, command.visitId, command.patientId)
                    dao.unlinkUltrasoundFromVisit(command.visitId, exceptScanId = command.linkedUltrasoundId)
                } else {
                    dao.unlinkUltrasoundFromVisit(command.visitId)
                }

                val receipt = SaveReceiptEntity(
                    operationUuid = command.operationUuid,
                    patientId = command.patientId,
                    visitId = command.visitId,
                    committedRevision = revision,
                    fingerprint = command.fingerprint,
                )
                dao.insertReceipt(receipt)
                SaveOutcome.Committed(loadSnapshot(), receipt.toDomain())
            }
        } catch (_: SQLiteConstraintException) {
            SaveOutcome.Rejected(SaveFailure.VISIT_CONFLICT)
        } catch (_: SQLiteException) {
            SaveOutcome.Ambiguous
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            SaveOutcome.Ambiguous
        }
    }

    override suspend fun reconcile(command: SaveVisitCommand): SaveOutcome {
        if (!command.isStructurallyValid()) return SaveOutcome.Rejected(SaveFailure.INVALID_COMMAND)
        return try {
            if (dao.patientCommandReceipt(command.operationUuid) != null || dao.clinicalCommandReceipt(command.operationUuid) != null) {
                return SaveOutcome.Rejected(SaveFailure.DUPLICATE_OPERATION_MISMATCH)
            }
            val receipt = dao.receipt(command.operationUuid)
                ?: return SaveOutcome.Rejected(SaveFailure.CONFIRMED_NOT_SAVED)
            receipt.replay(command)
        } catch (_: SQLiteException) {
            SaveOutcome.Ambiguous
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            SaveOutcome.Ambiguous
        }
    }

    suspend fun execute(command: PatientCommand): PatientCommandOutcome {
        if (!command.isStructurallyValid()) {
            return PatientCommandOutcome.Rejected(PatientCommandFailure.INVALID_COMMAND)
        }
        return try {
            database.withTransaction {
                if (dao.receipt(command.operationUuid) != null || dao.clinicalCommandReceipt(command.operationUuid) != null) {
                    return@withTransaction PatientCommandOutcome.Rejected(
                        PatientCommandFailure.DUPLICATE_OPERATION_MISMATCH,
                    )
                }
                val priorReceipt = dao.patientCommandReceipt(command.operationUuid)
                if (priorReceipt != null) return@withTransaction priorReceipt.replay(command)
                when (command) {
                    is SavePatientProfileCommand -> savePatientProfile(command)
                    is SaveOtherHistoriesCommand -> saveOtherHistories(command)
                    is ChangePatientStatusCommand -> changePatientStatus(command)
                    is SavePreviousPregnancyCommand -> savePreviousPregnancy(command)
                }
            }
        } catch (abort: PatientCommandAbort) {
            PatientCommandOutcome.Rejected(abort.failure)
        } catch (_: SQLiteConstraintException) {
            when (command) {
                is SavePatientProfileCommand ->
                    PatientCommandOutcome.Rejected(PatientCommandFailure.CASE_ID_CONFLICT)
                else -> PatientCommandOutcome.Rejected(PatientCommandFailure.RECORD_CONFLICT)
            }
        } catch (_: SQLiteException) {
            PatientCommandOutcome.Ambiguous
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            PatientCommandOutcome.Ambiguous
        }
    }

    suspend fun reconcile(command: PatientCommand): PatientCommandOutcome {
        if (!command.isStructurallyValid()) {
            return PatientCommandOutcome.Rejected(PatientCommandFailure.INVALID_COMMAND)
        }
        return try {
            if (dao.receipt(command.operationUuid) != null || dao.clinicalCommandReceipt(command.operationUuid) != null) {
                return PatientCommandOutcome.Rejected(PatientCommandFailure.DUPLICATE_OPERATION_MISMATCH)
            }
            val receipt = dao.patientCommandReceipt(command.operationUuid)
                ?: return PatientCommandOutcome.Rejected(PatientCommandFailure.CONFIRMED_NOT_SAVED)
            receipt.replay(command)
        } catch (_: SQLiteException) {
            PatientCommandOutcome.Ambiguous
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            PatientCommandOutcome.Ambiguous
        }
    }

    suspend fun execute(command: ClinicalCommand): ClinicalCommandOutcome {
        if (!command.isStructurallyValid()) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.INVALID_COMMAND)
        }
        return try {
            database.withTransaction {
                if (dao.receipt(command.operationUuid) != null || dao.patientCommandReceipt(command.operationUuid) != null) {
                    return@withTransaction ClinicalCommandOutcome.Rejected(
                        ClinicalCommandFailure.DUPLICATE_OPERATION_MISMATCH,
                    )
                }
                val priorReceipt = dao.clinicalCommandReceipt(command.operationUuid)
                if (priorReceipt != null) return@withTransaction priorReceipt.replay(command)
                when (command) {
                    is SaveDatingCommand -> saveDating(command)
                    is SaveUltrasoundScanCommand -> saveUltrasoundScan(command)
                    is SaveLabInvestigationCommand -> saveLabInvestigation(command)
                    is SaveMedicationCommand -> saveMedication(command)
                }
            }
        } catch (abort: ClinicalCommandAbort) {
            ClinicalCommandOutcome.Rejected(abort.failure)
        } catch (_: SQLiteConstraintException) {
            ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_CONFLICT)
        } catch (_: SQLiteException) {
            ClinicalCommandOutcome.Ambiguous
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            ClinicalCommandOutcome.Ambiguous
        }
    }

    suspend fun reconcile(command: ClinicalCommand): ClinicalCommandOutcome {
        if (!command.isStructurallyValid()) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.INVALID_COMMAND)
        }
        return try {
            if (dao.receipt(command.operationUuid) != null || dao.patientCommandReceipt(command.operationUuid) != null) {
                return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.DUPLICATE_OPERATION_MISMATCH)
            }
            val receipt = dao.clinicalCommandReceipt(command.operationUuid)
                ?: return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.CONFIRMED_NOT_SAVED)
            receipt.replay(command)
        } catch (_: SQLiteException) {
            ClinicalCommandOutcome.Ambiguous
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            ClinicalCommandOutcome.Ambiguous
        }
    }

    private suspend fun savePatientProfile(command: SavePatientProfileCommand): PatientCommandOutcome {
        val excluding = command.patientId.takeIf { command.expectedPatientRevision != null }
        if (dao.caseIdCount(command.caseId, excluding) != 0) {
            return PatientCommandOutcome.Rejected(PatientCommandFailure.CASE_ID_CONFLICT)
        }
        val committedRevision = if (command.expectedPatientRevision == null) {
            if (dao.patient(command.patientId) != null) {
                return PatientCommandOutcome.Rejected(PatientCommandFailure.RECORD_CONFLICT)
            }
            dao.insertPatient(command.toEntity())
            1L
        } else {
            val patient = dao.patient(command.patientId)
                ?: return PatientCommandOutcome.Rejected(PatientCommandFailure.PATIENT_NOT_FOUND)
            patient.writeGuard(command.expectedPatientRevision)?.let {
                return PatientCommandOutcome.Rejected(it)
            }
            val affected = dao.updatePatientProfile(
                patientId = command.patientId,
                expectedRevision = command.expectedPatientRevision,
                caseId = command.caseId,
                displayName = command.displayName,
                dateOfBirth = command.dateOfBirth,
                phone = command.phone,
                riskLevel = command.riskLevel,
                ageText = command.ageText,
                addressText = command.addressText,
                bloodGroupText = command.bloodGroupText,
                gravida = command.gravida,
                parity = command.parity,
                abortion = command.abortion,
                obstetricNotes = command.obstetricNotes,
                clinicalAttention = command.clinicalAttention,
            )
            if (affected != 1) {
                return PatientCommandOutcome.Rejected(PatientCommandFailure.STALE_PATIENT_REVISION)
            }
            command.expectedPatientRevision + 1
        }
        return commitReceipt(command, committedRevision, committedRevision)
    }

    private suspend fun saveOtherHistories(command: SaveOtherHistoriesCommand): PatientCommandOutcome {
        val patient = dao.patient(command.patientId)
            ?: return PatientCommandOutcome.Rejected(PatientCommandFailure.PATIENT_NOT_FOUND)
        patient.writeGuard(command.expectedPatientRevision)?.let {
            return PatientCommandOutcome.Rejected(it)
        }
        val affected = dao.updateOtherHistories(
            patientId = command.patientId,
            expectedRevision = command.expectedPatientRevision,
            medicalHistory = command.medicalHistory,
            surgicalHistory = command.surgicalHistory,
            familyHistory = command.familyHistory,
            allergyHistory = command.allergyHistory,
        )
        if (affected != 1) {
            return PatientCommandOutcome.Rejected(PatientCommandFailure.STALE_PATIENT_REVISION)
        }
        val revision = command.expectedPatientRevision + 1
        return commitReceipt(command, revision, revision)
    }

    private suspend fun changePatientStatus(command: ChangePatientStatusCommand): PatientCommandOutcome {
        val patient = dao.patient(command.patientId)
            ?: return PatientCommandOutcome.Rejected(PatientCommandFailure.PATIENT_NOT_FOUND)
        if (patient.revision != command.expectedPatientRevision) {
            return PatientCommandOutcome.Rejected(PatientCommandFailure.STALE_PATIENT_REVISION)
        }
        if (patient.revision == Long.MAX_VALUE) {
            return PatientCommandOutcome.Rejected(PatientCommandFailure.REVISION_EXHAUSTED)
        }
        val affected = if (command.targetStatus == "archived") {
            dao.archivePatient(command.patientId, command.expectedPatientRevision, command.changedAt, command.reason)
        } else {
            dao.unarchivePatient(command.patientId, command.expectedPatientRevision)
        }
        if (affected != 1) {
            val failure = if (patient.status == "archived" && command.targetStatus == "archived") {
                PatientCommandFailure.PATIENT_ARCHIVED
            } else {
                PatientCommandFailure.RECORD_CONFLICT
            }
            return PatientCommandOutcome.Rejected(failure)
        }
        val revision = command.expectedPatientRevision + 1
        return commitReceipt(command, revision, revision)
    }

    private suspend fun savePreviousPregnancy(command: SavePreviousPregnancyCommand): PatientCommandOutcome {
        val patient = dao.patient(command.patientId)
            ?: return PatientCommandOutcome.Rejected(PatientCommandFailure.PATIENT_NOT_FOUND)
        patient.writeGuard(command.expectedPatientRevision)?.let {
            return PatientCommandOutcome.Rejected(it)
        }
        val committedPregnancyRevision = if (command.expectedPregnancyRevision == null) {
            val existing = dao.previousPregnancy(command.pregnancyId)
            if (existing != null) {
                val failure = if (existing.patientId == command.patientId) {
                    PatientCommandFailure.RECORD_CONFLICT
                } else {
                    PatientCommandFailure.CROSS_PATIENT_WRITE
                }
                return PatientCommandOutcome.Rejected(failure)
            }
            dao.insertPreviousPregnancy(command.toEntity(revision = 1))
            1L
        } else {
            val pregnancy = dao.previousPregnancy(command.pregnancyId)
                ?: return PatientCommandOutcome.Rejected(PatientCommandFailure.STALE_RECORD_REVISION)
            if (pregnancy.patientId != command.patientId) {
                return PatientCommandOutcome.Rejected(PatientCommandFailure.CROSS_PATIENT_WRITE)
            }
            if (pregnancy.revision != command.expectedPregnancyRevision) {
                return PatientCommandOutcome.Rejected(PatientCommandFailure.STALE_RECORD_REVISION)
            }
            if (pregnancy.revision == Long.MAX_VALUE) {
                return PatientCommandOutcome.Rejected(PatientCommandFailure.REVISION_EXHAUSTED)
            }
            val affected = dao.updatePreviousPregnancy(
                pregnancyId = command.pregnancyId,
                patientId = command.patientId,
                expectedRevision = command.expectedPregnancyRevision,
                dateText = command.dateText,
                outcomeText = command.outcomeText,
                pregnancyType = command.pregnancyType,
                otherDetails = command.otherDetails,
                placeAndDoctor = command.placeAndDoctor,
                deliveryBy = command.deliveryBy,
                outcome = command.outcome,
                birthMethod = command.birthMethod,
                lossManagement = command.lossManagement,
                terminationMethod = command.terminationMethod,
                provenanceKind = command.provenanceKind,
                clinicianId = command.clinicianId,
                updatedAt = command.updatedAt,
            )
            if (affected != 1) {
                return PatientCommandOutcome.Rejected(PatientCommandFailure.STALE_RECORD_REVISION)
            }
            command.expectedPregnancyRevision + 1
        }
        val parentAffected = dao.incrementPatientRevision(command.patientId, command.expectedPatientRevision)
        if (parentAffected != 1) {
            throw PatientCommandAbort(PatientCommandFailure.STALE_PATIENT_REVISION)
        }
        return commitReceipt(
            command,
            committedPregnancyRevision,
            command.expectedPatientRevision + 1,
        )
    }

    private suspend fun commitReceipt(
        command: PatientCommand,
        committedRevision: Long,
        committedPatientRevision: Long,
    ): PatientCommandOutcome {
        val receipt = PatientCommandReceiptEntity(
            operationUuid = command.operationUuid,
            patientId = command.patientId,
            recordKind = command.recordKind,
            recordId = command.recordId,
            committedRevision = committedRevision,
            committedPatientRevision = committedPatientRevision,
            fingerprint = command.fingerprint,
        )
        dao.insertPatientCommandReceipt(receipt)
        return PatientCommandOutcome.Committed(loadSnapshot(), receipt.toDomain())
    }

    private fun PatientEntity.writeGuard(expectedRevision: Long): PatientCommandFailure? = when {
        status == "archived" -> PatientCommandFailure.PATIENT_ARCHIVED
        revision != expectedRevision -> PatientCommandFailure.STALE_PATIENT_REVISION
        revision == Long.MAX_VALUE -> PatientCommandFailure.REVISION_EXHAUSTED
        else -> null
    }

    private suspend fun SaveReceiptEntity.replay(command: SaveVisitCommand): SaveOutcome {
        if (
            fingerprint != command.fingerprint ||
            patientId != command.patientId ||
            visitId != command.visitId
        ) {
            return SaveOutcome.Rejected(SaveFailure.DUPLICATE_OPERATION_MISMATCH)
        }
        val visit = dao.visit(visitId)
            ?: return SaveOutcome.Rejected(SaveFailure.CONFIRMED_NOT_SAVED)
        if (visit.patientId != patientId || visit.revision < committedRevision) {
            return SaveOutcome.Rejected(SaveFailure.DUPLICATE_OPERATION_MISMATCH)
        }
        return SaveOutcome.Committed(loadSnapshot(), toDomain())
    }

    private suspend fun PatientCommandReceiptEntity.replay(command: PatientCommand): PatientCommandOutcome {
        if (
            fingerprint != command.fingerprint ||
            patientId != command.patientId ||
            recordKind != command.recordKind ||
            recordId != command.recordId
        ) {
            return PatientCommandOutcome.Rejected(PatientCommandFailure.DUPLICATE_OPERATION_MISMATCH)
        }
        val patient = dao.patient(patientId)
            ?: return PatientCommandOutcome.Rejected(PatientCommandFailure.CONFIRMED_NOT_SAVED)
        if (patient.revision < committedPatientRevision) {
            return PatientCommandOutcome.Rejected(PatientCommandFailure.DUPLICATE_OPERATION_MISMATCH)
        }
        if (recordKind in PREGNANCY_KINDS) {
            val pregnancy = dao.previousPregnancy(recordId)
                ?: return PatientCommandOutcome.Rejected(PatientCommandFailure.CONFIRMED_NOT_SAVED)
            if (pregnancy.patientId != patientId || pregnancy.revision < committedRevision) {
                return PatientCommandOutcome.Rejected(PatientCommandFailure.DUPLICATE_OPERATION_MISMATCH)
            }
        } else if (recordId != patientId || patient.revision < committedRevision) {
            return PatientCommandOutcome.Rejected(PatientCommandFailure.DUPLICATE_OPERATION_MISMATCH)
        }
        return PatientCommandOutcome.Committed(loadSnapshot(), toDomain())
    }

    private fun SaveVisitCommand.isStructurallyValid(): Boolean {
        if (!operationUuid.isCanonicalUuid() || !patientId.isCanonicalUuid() || !visitId.isCanonicalUuid()) return false
        if (!fingerprint.matches(Regex("[0-9a-f]{64}"))) return false
        if (fingerprint != canonicalFingerprint()) return false
        if (editorSessionId.isBlank()) return false
        if (expectedRevision != null && expectedRevision < 1) return false
        if (date != date.trim()) return false
        if (bloodPressure != bloodPressure.normalized()) return false
        if (weight != weight.normalized()) return false
        if (findings != findings.normalized()) return false
        if (notes != notes.normalized()) return false
        return try {
            LocalDate.parse(date).toString() == date
        } catch (_: DateTimeParseException) {
            false
        }
    }

    private fun String.isCanonicalUuid(): Boolean = try {
        UUID.fromString(this).toString() == this
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun RecordSnapshot.isInternallyValid(): Boolean = patients.all { patient ->
        val gpa = listOf(patient.gravida, patient.parity, patient.abortion)
        patient.revision >= 1 &&
            patient.status?.let { it in PatientVocabulary.statuses } != false &&
            patient.riskLevel?.let { it in PatientVocabulary.riskLevels } != false &&
            patient.bloodGroupText?.let { it in PatientVocabulary.bloodGroups } != false &&
            !(gpa.any { it == null } && gpa.any { it != null }) &&
            gpa.filterNotNull().all { it in 0..PatientVocabulary.MAX_SAFE_GPA } &&
            patient.visits.all { it.revision >= 1 } &&
            patient.previousPregnancies.all { pregnancy ->
                pregnancy.patientId == patient.id &&
                    pregnancy.modelVersion == 3 &&
                    pregnancy.revision >= 1 &&
                    pregnancy.provenanceKind == PatientVocabulary.CLINICIAN_ENTRY &&
                    pregnancy.clinicianId.isNotBlank()
            }
    }

    private fun SavePatientProfileCommand.toEntity() = PatientEntity(
        id = patientId,
        caseId = caseId,
        displayName = displayName,
        dateOfBirth = dateOfBirth,
        phone = phone,
        createdAt = createdAt,
        riskLevel = riskLevel,
        ageText = ageText,
        addressText = addressText,
        bloodGroupText = bloodGroupText,
        gravida = gravida,
        parity = parity,
        abortion = abortion,
        obstetricNotes = obstetricNotes,
        clinicalAttention = clinicalAttention,
        revision = 1,
    )

    private fun SavePreviousPregnancyCommand.toEntity(revision: Long) = PreviousPregnancyEntity(
        id = pregnancyId,
        patientId = patientId,
        modelVersion = modelVersion,
        dateText = dateText,
        outcomeText = outcomeText,
        pregnancyType = pregnancyType,
        otherDetails = otherDetails,
        placeAndDoctor = placeAndDoctor,
        deliveryBy = deliveryBy,
        outcome = outcome,
        birthMethod = birthMethod,
        lossManagement = lossManagement,
        terminationMethod = terminationMethod,
        provenanceKind = provenanceKind,
        clinicianId = clinicianId,
        recordedAt = recordedAt,
        updatedAt = updatedAt,
        revision = revision,
    )

    private fun SaveVisitCommand.toEntity(revision: Int) = VisitEntity(
        id = visitId,
        patientId = patientId,
        date = date,
        bloodPressure = bloodPressure,
        weight = weight,
        findings = findings,
        notes = notes,
        linkedUltrasoundId = linkedUltrasoundId,
        revision = revision,
        isSyntheticEdit = true,
    )

    internal fun DemoPatient.toEntity() = PatientEntity(
        id = id,
        caseId = caseId,
        displayName = displayName,
        dateOfBirth = dateOfBirth,
        phone = phone,
        riskLevel = riskLevel,
        ageText = ageText,
        addressText = addressText,
        bloodGroupText = bloodGroupText,
        medicalHistory = medicalHistory,
        surgicalHistory = surgicalHistory,
        familyHistory = familyHistory,
        allergyHistory = allergyHistory,
        gravida = gravida,
        parity = parity,
        abortion = abortion,
        obstetricNotes = obstetricNotes,
        clinicalAttention = clinicalAttention,
        revision = revision,
    )

    internal fun DemoVisit.toEntity(patientId: String) = VisitEntity(
        id = id,
        patientId = patientId,
        date = date,
        bloodPressure = bloodPressure,
        weight = weight,
        findings = findings,
        notes = notes,
        linkedUltrasoundId = linkedUltrasoundId,
        revision = revision,
        isSyntheticEdit = isSyntheticEdit,
    )

    internal fun DemoPatientDating.toEntity(patientId: String) = PatientDatingEntity(
        patientId = patientId,
        lmpDate = lmpDate,
        ultrasoundEdd = ultrasoundEdd,
        agreedEdd = agreedEdd,
        datingMethod = datingMethod,
        transferDate = transferDate,
        embryoAgeDays = embryoAgeDays,
        notes = notes,
        revision = revision,
    )

    internal fun DemoPreviousPregnancy.toEntity(patientId: String) = PreviousPregnancyEntity(
        id = id,
        patientId = patientId,
        modelVersion = modelVersion,
        dateText = dateText,
        outcomeText = outcomeText,
        pregnancyType = pregnancyType,
        otherDetails = otherDetails,
        placeAndDoctor = placeAndDoctor,
        deliveryBy = deliveryBy,
        outcome = outcome,
        birthMethod = birthMethod,
        lossManagement = lossManagement,
        terminationMethod = terminationMethod,
        provenanceKind = provenanceKind,
        clinicianId = clinicianId,
        recordedAt = recordedAt,
        updatedAt = updatedAt,
        revision = revision,
    )

    internal fun DemoUltrasoundScan.toEntity(patientId: String) = UltrasoundScanEntity(
        id = id,
        patientId = patientId,
        date = date,
        gaWeeks = gestationalAgeWeeks,
        gaDays = gestationalAgeDays,
        fetalNumber = fetalNumber,
        presentation = presentation,
        placentalSite = placentalSite,
        amnioticFluid = amnioticFluid,
        fetalHeartRate = fetalHeartRate,
        biometryBpd = biometryBpd,
        biometryHc = biometryHc,
        biometryAc = biometryAc,
        biometryFl = biometryFl,
        estimatedFetalWeight = estimatedFetalWeight,
        impression = impression,
        linkedVisitId = linkedVisitId,
        imageUris = ImageUriCodec.serialize(imageUris),
        revision = revision,
    )

    internal fun DemoLabInvestigation.toEntity(patientId: String) = LabInvestigationEntity(
        id = id,
        patientId = patientId,
        date = date,
        testCategory = testCategory,
        testName = testName,
        result = result,
        unit = unit,
        referenceRange = referenceRange,
        isAbnormal = isAbnormal,
        notes = notes,
        labName = labName,
        revision = revision,
    )

    internal fun DemoMedicationRecord.toEntity(patientId: String) = MedicationEntity(
        id = id,
        patientId = patientId,
        name = name,
        dosage = dosage,
        route = route,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        status = status,
        notes = notes,
        isProphylaxis = isProphylaxis,
        isNonCompliant = isNonCompliant,
        adverseEffects = adverseNotes,
        revision = revision,
    )

    internal fun PatientEntity.toDomain(
        visits: List<DemoVisit>,
        pregnancies: List<DemoPreviousPregnancy>,
        dating: DemoPatientDating?,
        ultrasoundScans: List<DemoUltrasoundScan>,
        labInvestigations: List<DemoLabInvestigation>,
        medications: List<DemoMedicationRecord>,
        attachments: List<DemoAttachment> = emptyList(),
    ) = DemoPatient(
        id = id,
        caseId = caseId,
        displayName = displayName,
        dateOfBirth = dateOfBirth,
        phone = phone,
        visits = visits,
        createdAt = createdAt,
        status = status,
        archivedAt = archivedAt,
        archivedReason = archivedReason,
        riskLevel = riskLevel,
        ageText = ageText,
        addressText = addressText,
        bloodGroupText = bloodGroupText,
        clinicalAttention = clinicalAttention,
        medicalHistory = medicalHistory,
        surgicalHistory = surgicalHistory,
        familyHistory = familyHistory,
        allergyHistory = allergyHistory,
        gravida = gravida,
        parity = parity,
        abortion = abortion,
        obstetricNotes = obstetricNotes,
        revision = revision,
        previousPregnancies = pregnancies,
        dating = dating,
        ultrasoundScans = ultrasoundScans,
        labInvestigations = labInvestigations,
        medications = medications,
        attachments = attachments,
    )

    internal fun PatientAttachmentEntity.toDomain(): DemoAttachment = DemoAttachment(
        id = id,
        patientId = patientId,
        fileName = fileName,
        filePath = filePath,
        category = category,
        mimeType = mimeType,
        fileSizeBytes = fileSizeBytes,
        sha256Hash = sha256Hash,
        title = title,
        capturedAt = capturedAt,
        linkedRecordId = linkedRecordId,
        syncStatus = syncStatus,
        cloudUrl = cloudUrl,
        cloudSyncedAt = cloudSyncedAt,
        revision = revision,
    )

    internal fun DemoAttachment.toEntity(patientId: String): PatientAttachmentEntity = PatientAttachmentEntity(
        id = id,
        patientId = patientId,
        fileName = fileName,
        filePath = filePath,
        category = category,
        mimeType = mimeType,
        fileSizeBytes = fileSizeBytes,
        sha256Hash = sha256Hash,
        title = title,
        capturedAt = capturedAt,
        linkedRecordId = linkedRecordId,
        syncStatus = syncStatus,
        cloudUrl = cloudUrl,
        cloudSyncedAt = cloudSyncedAt,
        revision = revision,
    )

    suspend fun saveAttachment(attachment: DemoAttachment): Boolean {
        return database.withTransaction {
            val patient = dao.patient(attachment.patientId) ?: return@withTransaction false
            if (patient.status == "archived") return@withTransaction false
            dao.insertAttachment(attachment.toEntity(attachment.patientId))
            true
        }
    }

    suspend fun deleteAttachment(attachmentId: String, patientId: String): Boolean {
        return database.withTransaction {
            val affected = dao.deleteAttachment(attachmentId, patientId)
            affected > 0
        }
    }

    internal fun VisitEntity.toDomain() = DemoVisit(
        id = id,
        date = date,
        bloodPressure = bloodPressure,
        weight = weight,
        findings = findings,
        notes = notes,
        linkedUltrasoundId = linkedUltrasoundId,
        revision = revision,
        isSyntheticEdit = isSyntheticEdit,
    )

    internal fun PreviousPregnancyEntity.toDomain() = DemoPreviousPregnancy(
        id = id,
        patientId = patientId,
        modelVersion = modelVersion,
        dateText = dateText,
        outcomeText = outcomeText,
        pregnancyType = pregnancyType,
        otherDetails = otherDetails,
        placeAndDoctor = placeAndDoctor,
        deliveryBy = deliveryBy,
        outcome = outcome,
        birthMethod = birthMethod,
        lossManagement = lossManagement,
        terminationMethod = terminationMethod,
        provenanceKind = provenanceKind,
        clinicianId = clinicianId,
        recordedAt = recordedAt,
        updatedAt = updatedAt,
        revision = revision,
    )

    private fun SaveReceiptEntity.toDomain() = SaveReceipt(
        operationUuid = operationUuid,
        patientId = patientId,
        visitId = visitId,
        committedRevision = committedRevision,
        fingerprint = fingerprint,
    )

    private fun PatientCommandReceiptEntity.toDomain() = PatientCommandReceipt(
        operationUuid = operationUuid,
        patientId = patientId,
        recordKind = recordKind,
        recordId = recordId,
        committedRevision = committedRevision,
        committedPatientRevision = committedPatientRevision,
        fingerprint = fingerprint,
    )

    internal fun PatientDatingEntity.toDomain() = DemoPatientDating(
        patientId = patientId,
        lmpDate = lmpDate,
        ultrasoundEdd = ultrasoundEdd,
        agreedEdd = agreedEdd,
        datingMethod = datingMethod,
        transferDate = transferDate,
        embryoAgeDays = embryoAgeDays,
        notes = notes,
        revision = revision,
    )

    internal fun UltrasoundScanEntity.toDomain() = DemoUltrasoundScan(
        id = id,
        patientId = patientId,
        date = date,
        gestationalAgeWeeks = gaWeeks,
        gestationalAgeDays = gaDays,
        fetalNumber = fetalNumber,
        presentation = presentation,
        placentalSite = placentalSite,
        amnioticFluid = amnioticFluid,
        fetalHeartRate = fetalHeartRate,
        biometryBpd = biometryBpd,
        biometryHc = biometryHc,
        biometryAc = biometryAc,
        biometryFl = biometryFl,
        estimatedFetalWeight = estimatedFetalWeight,
        impression = impression,
        linkedVisitId = linkedVisitId,
        imageUris = ImageUriCodec.deserialize(imageUris),
        revision = revision,
    )

    internal fun LabInvestigationEntity.toDomain() = DemoLabInvestigation(
        id = id,
        patientId = patientId,
        date = date,
        testCategory = testCategory,
        testName = testName,
        result = result,
        unit = unit,
        referenceRange = referenceRange,
        isAbnormal = isAbnormal,
        notes = notes,
        labName = labName,
        revision = revision,
    )

    internal fun MedicationEntity.toDomain() = DemoMedicationRecord(
        id = id,
        patientId = patientId,
        name = name,
        dosage = dosage,
        route = route,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        status = status,
        notes = notes,
        isProphylaxis = isProphylaxis,
        isNonCompliant = isNonCompliant,
        adverseNotes = adverseEffects,
        revision = revision,
    )

    private fun ClinicalCommandReceiptEntity.toDomain() = ClinicalCommandReceipt(
        operationUuid = operationUuid,
        patientId = patientId,
        recordKind = recordKind,
        recordId = recordId,
        committedRevision = committedRevision,
        committedPatientRevision = committedPatientRevision,
        fingerprint = fingerprint,
    )

    private suspend fun ClinicalCommandReceiptEntity.replay(
        command: ClinicalCommand,
    ): ClinicalCommandOutcome {
        if (
            patientId != command.patientId ||
            recordKind != command.recordKind.name ||
            recordId != command.recordId ||
            fingerprint != command.computeFingerprint()
        ) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.DUPLICATE_OPERATION_MISMATCH)
        }
        val snapshot = loadSnapshot()
        if (snapshot.patients.none { it.id == command.patientId }) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_NOT_FOUND)
        }
        return ClinicalCommandOutcome.Committed(snapshot, toDomain())
    }

    private suspend fun saveDating(command: SaveDatingCommand): ClinicalCommandOutcome {
        val patient = dao.patient(command.patientId)
            ?: return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_NOT_FOUND)
        if (patient.status == "archived") {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_ARCHIVED)
        }
        if (patient.revision != command.expectedPatientRevision) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)
        }
        if (patient.revision == Long.MAX_VALUE) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.REVISION_EXHAUSTED)
        }

        val existing = dao.patientDating(command.patientId)
        val committedRevision = if (existing == null) {
            dao.insertPatientDating(
                PatientDatingEntity(
                    patientId = command.patientId,
                    lmpDate = command.lmpDate,
                    ultrasoundEdd = command.ultrasoundEdd,
                    agreedEdd = command.agreedEdd,
                    datingMethod = command.datingMethod,
                    transferDate = command.transferDate,
                    embryoAgeDays = command.embryoAgeDays,
                    notes = command.notes,
                    revision = 1,
                ),
            )
            1L
        } else {
            val updated = dao.updatePatientDating(
                patientId = command.patientId,
                expectedRevision = existing.revision,
                lmpDate = command.lmpDate,
                ultrasoundEdd = command.ultrasoundEdd,
                agreedEdd = command.agreedEdd,
                datingMethod = command.datingMethod,
                transferDate = command.transferDate,
                embryoAgeDays = command.embryoAgeDays,
                notes = command.notes,
            )
            if (updated != 1) return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_REVISION)
            existing.revision + 1
        }

        val patientUpdated = dao.incrementPatientRevision(command.patientId, command.expectedPatientRevision)
        if (patientUpdated != 1) return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)

        val receipt = ClinicalCommandReceiptEntity(
            operationUuid = command.operationUuid,
            patientId = command.patientId,
            recordKind = command.recordKind.name,
            recordId = command.recordId,
            committedRevision = committedRevision,
            committedPatientRevision = command.expectedPatientRevision + 1,
            fingerprint = command.computeFingerprint(),
        )
        dao.insertClinicalCommandReceipt(receipt)
        val snapshot = loadSnapshot()
        return ClinicalCommandOutcome.Committed(snapshot, receipt.toDomain())
    }

    private suspend fun saveUltrasoundScan(command: SaveUltrasoundScanCommand): ClinicalCommandOutcome {
        val patient = dao.patient(command.patientId)
            ?: return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_NOT_FOUND)
        if (patient.status == "archived") {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_ARCHIVED)
        }
        if (patient.revision != command.expectedPatientRevision) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)
        }
        if (patient.revision == Long.MAX_VALUE) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.REVISION_EXHAUSTED)
        }
        if (command.linkedVisitId != null) {
            val v = dao.visit(command.linkedVisitId)
            if (v == null || v.patientId != command.patientId) {
                return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.CROSS_PATIENT_WRITE)
            }
        }

        val committedRevision = if (command.expectedScanRevision == null) {
            val existing = dao.ultrasoundScan(command.scanId)
            if (existing != null) {
                val failure = if (existing.patientId == command.patientId) {
                    ClinicalCommandFailure.RECORD_CONFLICT
                } else {
                    ClinicalCommandFailure.CROSS_PATIENT_WRITE
                }
                return ClinicalCommandOutcome.Rejected(failure)
            }
            dao.insertUltrasoundScan(
                UltrasoundScanEntity(
                    id = command.scanId,
                    patientId = command.patientId,
                    date = command.date,
                    gaWeeks = command.gaWeeks,
                    gaDays = command.gaDays,
                    fetalNumber = command.fetalNumber,
                    presentation = command.presentation,
                    placentalSite = command.placentalSite,
                    amnioticFluid = command.amnioticFluid,
                    fetalHeartRate = command.fetalHeartRate,
                    biometryBpd = command.biometryBpd,
                    biometryHc = command.biometryHc,
                    biometryAc = command.biometryAc,
                    biometryFl = command.biometryFl,
                    estimatedFetalWeight = command.estimatedFetalWeight,
                    impression = command.impression,
                    linkedVisitId = command.linkedVisitId,
                    imageUris = ImageUriCodec.serialize(command.imageUris),
                    revision = 1,
                ),
            )
            1L
        } else {
            val existing = dao.ultrasoundScan(command.scanId)
                ?: return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_NOT_FOUND)
            if (existing.patientId != command.patientId) {
                return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.CROSS_PATIENT_WRITE)
            }
            val updated = dao.updateUltrasoundScan(
                scanId = command.scanId,
                patientId = command.patientId,
                expectedRevision = command.expectedScanRevision,
                date = command.date,
                gaWeeks = command.gaWeeks,
                gaDays = command.gaDays,
                fetalNumber = command.fetalNumber,
                presentation = command.presentation,
                placentalSite = command.placentalSite,
                amnioticFluid = command.amnioticFluid,
                fetalHeartRate = command.fetalHeartRate,
                biometryBpd = command.biometryBpd,
                biometryHc = command.biometryHc,
                biometryAc = command.biometryAc,
                biometryFl = command.biometryFl,
                estimatedFetalWeight = command.estimatedFetalWeight,
                impression = command.impression,
                linkedVisitId = command.linkedVisitId,
                imageUris = ImageUriCodec.serialize(command.imageUris),
            )
            if (updated != 1) return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_REVISION)
            command.expectedScanRevision + 1
        }

        if (command.linkedVisitId != null) {
            dao.linkVisitToUltrasound(command.linkedVisitId, command.scanId, command.patientId)
            dao.unlinkVisitFromUltrasound(command.scanId, exceptVisitId = command.linkedVisitId)
        } else {
            dao.unlinkVisitFromUltrasound(command.scanId)
        }

        val patientUpdated = dao.incrementPatientRevision(command.patientId, command.expectedPatientRevision)
        if (patientUpdated != 1) return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)

        val receipt = ClinicalCommandReceiptEntity(
            operationUuid = command.operationUuid,
            patientId = command.patientId,
            recordKind = command.recordKind.name,
            recordId = command.recordId,
            committedRevision = committedRevision,
            committedPatientRevision = command.expectedPatientRevision + 1,
            fingerprint = command.computeFingerprint(),
        )
        dao.insertClinicalCommandReceipt(receipt)
        val snapshot = loadSnapshot()
        return ClinicalCommandOutcome.Committed(snapshot, receipt.toDomain())
    }

    private suspend fun saveLabInvestigation(command: SaveLabInvestigationCommand): ClinicalCommandOutcome {
        val patient = dao.patient(command.patientId)
            ?: return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_NOT_FOUND)
        if (patient.status == "archived") {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_ARCHIVED)
        }
        if (patient.revision != command.expectedPatientRevision) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)
        }
        if (patient.revision == Long.MAX_VALUE) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.REVISION_EXHAUSTED)
        }

        val committedRevision = if (command.expectedLabRevision == null) {
            val existing = dao.labInvestigation(command.labId)
            if (existing != null) {
                val failure = if (existing.patientId == command.patientId) {
                    ClinicalCommandFailure.RECORD_CONFLICT
                } else {
                    ClinicalCommandFailure.CROSS_PATIENT_WRITE
                }
                return ClinicalCommandOutcome.Rejected(failure)
            }
            dao.insertLabInvestigation(
                LabInvestigationEntity(
                    id = command.labId,
                    patientId = command.patientId,
                    date = command.date,
                    testCategory = command.testCategory,
                    testName = command.testName,
                    result = command.result,
                    unit = command.unit,
                    referenceRange = command.referenceRange,
                    isAbnormal = command.isAbnormal,
                    notes = command.notes,
                    labName = command.labName,
                    revision = 1,
                ),
            )
            1L
        } else {
            val existing = dao.labInvestigation(command.labId)
                ?: return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_NOT_FOUND)
            if (existing.patientId != command.patientId) {
                return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.CROSS_PATIENT_WRITE)
            }
            val updated = dao.updateLabInvestigation(
                labId = command.labId,
                patientId = command.patientId,
                expectedRevision = command.expectedLabRevision,
                date = command.date,
                testCategory = command.testCategory,
                testName = command.testName,
                result = command.result,
                unit = command.unit,
                referenceRange = command.referenceRange,
                isAbnormal = command.isAbnormal,
                notes = command.notes,
                labName = command.labName,
            )
            if (updated != 1) return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_REVISION)
            command.expectedLabRevision + 1
        }

        val patientUpdated = dao.incrementPatientRevision(command.patientId, command.expectedPatientRevision)
        if (patientUpdated != 1) return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)

        val receipt = ClinicalCommandReceiptEntity(
            operationUuid = command.operationUuid,
            patientId = command.patientId,
            recordKind = command.recordKind.name,
            recordId = command.recordId,
            committedRevision = committedRevision,
            committedPatientRevision = command.expectedPatientRevision + 1,
            fingerprint = command.computeFingerprint(),
        )
        dao.insertClinicalCommandReceipt(receipt)
        val snapshot = loadSnapshot()
        return ClinicalCommandOutcome.Committed(snapshot, receipt.toDomain())
    }

    private suspend fun saveMedication(command: SaveMedicationCommand): ClinicalCommandOutcome {
        val patient = dao.patient(command.patientId)
            ?: return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_NOT_FOUND)
        if (patient.status == "archived") {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.PATIENT_ARCHIVED)
        }
        if (patient.revision != command.expectedPatientRevision) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)
        }
        if (patient.revision == Long.MAX_VALUE) {
            return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.REVISION_EXHAUSTED)
        }

        val committedRevision = if (command.expectedMedicationRevision == null) {
            val existing = dao.medication(command.medicationId)
            if (existing != null) {
                val failure = if (existing.patientId == command.patientId) {
                    ClinicalCommandFailure.RECORD_CONFLICT
                } else {
                    ClinicalCommandFailure.CROSS_PATIENT_WRITE
                }
                return ClinicalCommandOutcome.Rejected(failure)
            }
            dao.insertMedication(
                MedicationEntity(
                    id = command.medicationId,
                    patientId = command.patientId,
                    name = command.name,
                    dosage = command.dosage,
                    route = command.route,
                    frequency = command.frequency,
                    startDate = command.startDate,
                    endDate = command.endDate,
                    status = command.status,
                    notes = command.notes,
                    isProphylaxis = command.isProphylaxis,
                    isNonCompliant = command.isNonCompliant,
                    adverseEffects = command.adverseNotes,
                    revision = 1,
                ),
            )
            1L
        } else {
            val existing = dao.medication(command.medicationId)
                ?: return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_NOT_FOUND)
            if (existing.patientId != command.patientId) {
                return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.CROSS_PATIENT_WRITE)
            }
            val updated = dao.updateMedication(
                medicationId = command.medicationId,
                patientId = command.patientId,
                expectedRevision = command.expectedMedicationRevision,
                name = command.name,
                dosage = command.dosage,
                route = command.route,
                frequency = command.frequency,
                startDate = command.startDate,
                endDate = command.endDate,
                status = command.status,
                notes = command.notes,
                isProphylaxis = command.isProphylaxis,
                isNonCompliant = command.isNonCompliant,
                adverseEffects = command.adverseNotes,
            )
            if (updated != 1) return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_REVISION)
            command.expectedMedicationRevision + 1
        }

        val patientUpdated = dao.incrementPatientRevision(command.patientId, command.expectedPatientRevision)
        if (patientUpdated != 1) return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION)

        val receipt = ClinicalCommandReceiptEntity(
            operationUuid = command.operationUuid,
            patientId = command.patientId,
            recordKind = command.recordKind.name,
            recordId = command.recordId,
            committedRevision = committedRevision,
            committedPatientRevision = command.expectedPatientRevision + 1,
            fingerprint = command.computeFingerprint(),
        )
        dao.insertClinicalCommandReceipt(receipt)
        val snapshot = loadSnapshot()
        return ClinicalCommandOutcome.Committed(snapshot, receipt.toDomain())
    }

    private companion object {
        const val SCHEMA_VERSION = 5
        val PREGNANCY_KINDS = setOf(
            PatientRecordKind.PREVIOUS_PREGNANCY_CREATE,
            PatientRecordKind.PREVIOUS_PREGNANCY_UPDATE,
        )
    }

    private class PatientCommandAbort(val failure: PatientCommandFailure) : RuntimeException()
    private class ClinicalCommandAbort(val failure: ClinicalCommandFailure) : RuntimeException()
}

internal sealed interface RepositoryOpenResult {
    data class Success(val snapshot: RecordSnapshot) : RepositoryOpenResult
    data class Failure(val reason: VaultBlockReason) : RepositoryOpenResult
}
