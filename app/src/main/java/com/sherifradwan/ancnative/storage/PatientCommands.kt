package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.PatientClinicalInput
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.PatientVocabulary
import com.sherifradwan.ancnative.model.PreviousPregnancyClinicalInput
import com.sherifradwan.ancnative.model.validationErrors
import com.sherifradwan.ancnative.state.AncState
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

sealed interface PatientCommand {
    val operationUuid: String
    val editorSessionId: String
    val patientId: String
    val expectedPatientRevision: Long?
    val recordKind: String
    val recordId: String
    val fingerprint: String
}

data class SavePatientProfileCommand(
    override val operationUuid: String,
    override val editorSessionId: String,
    override val patientId: String,
    override val expectedPatientRevision: Long?,
    val createdAt: String?,
    val caseId: String,
    val displayName: String,
    val dateOfBirth: String?,
    val phone: String?,
    val riskLevel: String?,
    val ageText: String?,
    val addressText: String?,
    val bloodGroupText: String?,
    val gravida: Long?,
    val parity: Long?,
    val abortion: Long?,
    val obstetricNotes: String?,
    val clinicalAttention: String? = null,
    override val fingerprint: String,
) : PatientCommand {
    override val recordKind: String = if (expectedPatientRevision == null) {
        PatientRecordKind.PATIENT_CREATE
    } else {
        PatientRecordKind.PATIENT_UPDATE
    }
    override val recordId: String = patientId

    companion object {
        fun prepare(state: AncState, operationUuid: String, submittedAt: String): PatientCommandPreparation {
            val draft = state.patientDraft
                ?: return PatientCommandPreparation.Rejected(setOf(PatientValidationError.SAVE_FAILED))
            val gpa = parseGpa(draft.values.gravida, draft.values.parity, draft.values.abortion)
            if (gpa.errors.isNotEmpty()) return PatientCommandPreparation.Rejected(gpa.errors)
            val input = PatientClinicalInput(
                caseId = draft.values.caseId.trim(),
                displayName = draft.values.displayName.trim(),
                dateOfBirth = draft.values.dateOfBirth.normalized(),
                phone = draft.values.phone.normalized(),
                ageText = draft.values.ageText.normalized(),
                addressText = draft.values.addressText.normalized(),
                bloodGroupText = draft.values.bloodGroupText.normalized(),
                riskLevel = draft.values.riskLevel.normalized(),
                gravida = gpa.gravida,
                parity = gpa.parity,
                abortion = gpa.abortion,
                obstetricNotes = draft.values.obstetricNotes.normalized(),
            )
            val errors = input.validationErrors()
            if (errors.isNotEmpty()) return PatientCommandPreparation.Rejected(errors)
            val command = SavePatientProfileCommand(
                operationUuid = operationUuid,
                editorSessionId = draft.editorSessionId,
                patientId = draft.patientId,
                expectedPatientRevision = draft.baseRevision,
                createdAt = if (draft.baseRevision == null) submittedAt else draft.createdAt,
                caseId = input.caseId,
                displayName = input.displayName,
                dateOfBirth = input.dateOfBirth,
                phone = input.phone,
                riskLevel = input.riskLevel,
                ageText = input.ageText,
                addressText = input.addressText,
                bloodGroupText = input.bloodGroupText,
                gravida = input.gravida,
                parity = input.parity,
                abortion = input.abortion,
                obstetricNotes = input.obstetricNotes,
                clinicalAttention = draft.values.clinicalAttention.normalized(),
                fingerprint = "",
            )
            return PatientCommandPreparation.Ready(command.withCanonicalFingerprint())
        }
    }
}

data class SaveOtherHistoriesCommand(
    override val operationUuid: String,
    override val editorSessionId: String,
    override val patientId: String,
    override val expectedPatientRevision: Long,
    val medicalHistory: String?,
    val surgicalHistory: String?,
    val familyHistory: String?,
    val allergyHistory: String?,
    override val fingerprint: String,
) : PatientCommand {
    override val recordKind: String = PatientRecordKind.OTHER_HISTORIES
    override val recordId: String = patientId

    companion object {
        fun prepare(state: AncState, operationUuid: String): PatientCommandPreparation {
            val draft = state.historiesDraft
                ?: return PatientCommandPreparation.Rejected(setOf(PatientValidationError.SAVE_FAILED))
            val command = SaveOtherHistoriesCommand(
                operationUuid = operationUuid,
                editorSessionId = draft.editorSessionId,
                patientId = draft.patientId,
                expectedPatientRevision = draft.baseRevision,
                medicalHistory = draft.values.medicalHistory.normalized(),
                surgicalHistory = draft.values.surgicalHistory.normalized(),
                familyHistory = draft.values.familyHistory.normalized(),
                allergyHistory = draft.values.allergyHistory.normalized(),
                fingerprint = "",
            )
            return PatientCommandPreparation.Ready(command.withCanonicalFingerprint())
        }
    }
}

data class ChangePatientStatusCommand(
    override val operationUuid: String,
    override val editorSessionId: String,
    override val patientId: String,
    override val expectedPatientRevision: Long,
    val targetStatus: String,
    val reason: String,
    val changedAt: String,
    override val fingerprint: String,
) : PatientCommand {
    override val recordKind: String = if (targetStatus == "archived") {
        PatientRecordKind.ARCHIVE
    } else {
        PatientRecordKind.UNARCHIVE
    }
    override val recordId: String = patientId

    companion object {
        fun prepare(state: AncState, operationUuid: String, submittedAt: String): PatientCommandPreparation {
            val draft = state.statusDraft
                ?: return PatientCommandPreparation.Rejected(setOf(PatientValidationError.SAVE_FAILED))
            val reason = draft.reason.trim()
            val errors = buildSet {
                if (draft.targetStatus !in PatientVocabulary.statuses) add(PatientValidationError.INVALID_STATUS)
                if (reason.isEmpty()) add(PatientValidationError.ARCHIVE_REASON_REQUIRED)
            }
            if (errors.isNotEmpty()) return PatientCommandPreparation.Rejected(errors)
            val command = ChangePatientStatusCommand(
                operationUuid = operationUuid,
                editorSessionId = draft.editorSessionId,
                patientId = draft.patientId,
                expectedPatientRevision = draft.baseRevision,
                targetStatus = draft.targetStatus,
                reason = reason,
                changedAt = submittedAt,
                fingerprint = "",
            )
            return PatientCommandPreparation.Ready(command.withCanonicalFingerprint())
        }
    }
}

data class SavePreviousPregnancyCommand(
    override val operationUuid: String,
    override val editorSessionId: String,
    override val patientId: String,
    override val expectedPatientRevision: Long,
    val pregnancyId: String,
    val expectedPregnancyRevision: Long?,
    val modelVersion: Int,
    val dateText: String?,
    val outcomeText: String?,
    val pregnancyType: String?,
    val otherDetails: String?,
    val placeAndDoctor: String?,
    val deliveryBy: String?,
    val outcome: String?,
    val birthMethod: String?,
    val lossManagement: String?,
    val terminationMethod: String?,
    val provenanceKind: String,
    val clinicianId: String,
    val recordedAt: String,
    val updatedAt: String,
    override val fingerprint: String,
) : PatientCommand {
    override val recordKind: String = if (expectedPregnancyRevision == null) {
        PatientRecordKind.PREVIOUS_PREGNANCY_CREATE
    } else {
        PatientRecordKind.PREVIOUS_PREGNANCY_UPDATE
    }
    override val recordId: String = pregnancyId

    companion object {
        fun prepare(state: AncState, operationUuid: String, submittedAt: String): PatientCommandPreparation {
            val draft = state.pregnancyDraft
                ?: return PatientCommandPreparation.Rejected(setOf(PatientValidationError.SAVE_FAILED))
            val input = PreviousPregnancyClinicalInput(
                pregnancyType = draft.values.pregnancyType.normalized(),
                deliveryBy = draft.values.deliveryBy.normalized(),
                outcome = draft.values.outcome.normalized(),
                birthMethod = draft.values.birthMethod.normalized(),
                lossManagement = draft.values.lossManagement.normalized(),
                terminationMethod = draft.values.terminationMethod.normalized(),
                provenanceKind = PatientVocabulary.CLINICIAN_ENTRY,
                clinicianId = draft.values.clinicianId.trim(),
            )
            val errors = input.validationErrors()
            if (errors.isNotEmpty()) return PatientCommandPreparation.Rejected(errors)
            val command = SavePreviousPregnancyCommand(
                operationUuid = operationUuid,
                editorSessionId = draft.editorSessionId,
                patientId = draft.patientId,
                expectedPatientRevision = draft.basePatientRevision,
                pregnancyId = draft.pregnancyId,
                expectedPregnancyRevision = draft.basePregnancyRevision,
                modelVersion = 3,
                dateText = draft.values.dateText.normalized(),
                outcomeText = draft.values.outcomeText.normalized(),
                pregnancyType = input.pregnancyType,
                otherDetails = draft.values.otherDetails.normalized(),
                placeAndDoctor = draft.values.placeAndDoctor.normalized(),
                deliveryBy = input.deliveryBy,
                outcome = input.outcome,
                birthMethod = input.birthMethod,
                lossManagement = input.lossManagement,
                terminationMethod = input.terminationMethod,
                provenanceKind = input.provenanceKind,
                clinicianId = input.clinicianId,
                recordedAt = draft.recordedAt ?: submittedAt,
                updatedAt = submittedAt,
                fingerprint = "",
            )
            return PatientCommandPreparation.Ready(command.withCanonicalFingerprint())
        }
    }
}

object PatientRecordKind {
    const val PATIENT_CREATE = "PATIENT_CREATE"
    const val PATIENT_UPDATE = "PATIENT_UPDATE"
    const val OTHER_HISTORIES = "OTHER_HISTORIES"
    const val ARCHIVE = "ARCHIVE"
    const val UNARCHIVE = "UNARCHIVE"
    const val PREVIOUS_PREGNANCY_CREATE = "PREVIOUS_PREGNANCY_CREATE"
    const val PREVIOUS_PREGNANCY_UPDATE = "PREVIOUS_PREGNANCY_UPDATE"
}

sealed interface PatientCommandPreparation {
    data class Ready(val command: PatientCommand) : PatientCommandPreparation
    data class Rejected(val errors: Set<PatientValidationError>) : PatientCommandPreparation
}

data class PatientCommandReceipt(
    val operationUuid: String,
    val patientId: String,
    val recordKind: String,
    val recordId: String,
    val committedRevision: Long,
    val committedPatientRevision: Long,
    val fingerprint: String,
)

enum class PatientCommandFailure {
    INVALID_COMMAND,
    PATIENT_NOT_FOUND,
    RECORD_CONFLICT,
    CASE_ID_CONFLICT,
    STALE_PATIENT_REVISION,
    STALE_RECORD_REVISION,
    CROSS_PATIENT_WRITE,
    PATIENT_ARCHIVED,
    DUPLICATE_OPERATION_MISMATCH,
    REVISION_EXHAUSTED,
    CONFIRMED_NOT_SAVED,
}

sealed interface PatientCommandOutcome {
    data class Committed(
        val snapshot: RecordSnapshot,
        val receipt: PatientCommandReceipt,
    ) : PatientCommandOutcome
    data class Rejected(val failure: PatientCommandFailure) : PatientCommandOutcome
    data object Ambiguous : PatientCommandOutcome
    data object Obsolete : PatientCommandOutcome
}

internal fun PatientCommand.isStructurallyValid(): Boolean {
    if (!operationUuid.isCanonicalUuid() || !patientId.isCanonicalUuid() || !recordId.isCanonicalUuid()) return false
    if (editorSessionId.isBlank() || expectedPatientRevision?.let { it < 1 } == true) return false
    if (!fingerprint.matches(Regex("[0-9a-f]{64}")) || fingerprint != canonicalFingerprint()) return false
    return when (this) {
        is SavePatientProfileCommand -> {
            val input = PatientClinicalInput(
                caseId,
                displayName,
                dateOfBirth,
                phone,
                ageText,
                addressText,
                bloodGroupText,
                riskLevel,
                gravida,
                parity,
                abortion,
                obstetricNotes,
            )
            input.validationErrors().isEmpty() &&
                listOf(dateOfBirth, phone, ageText, addressText, obstetricNotes).all { it == it.normalized() } &&
                (createdAt == null || createdAt.isInstant()) &&
                (expectedPatientRevision != null || createdAt != null)
        }
        is SaveOtherHistoriesCommand ->
            listOf(medicalHistory, surgicalHistory, familyHistory, allergyHistory).all { it == it.normalized() }
        is ChangePatientStatusCommand ->
            targetStatus in PatientVocabulary.statuses && reason.isNotBlank() && reason == reason.trim() && changedAt.isInstant()
        is SavePreviousPregnancyCommand -> {
            val input = PreviousPregnancyClinicalInput(
                pregnancyType,
                deliveryBy,
                outcome,
                birthMethod,
                lossManagement,
                terminationMethod,
                provenanceKind,
                clinicianId,
            )
            modelVersion == 3 &&
                expectedPregnancyRevision?.let { it >= 1 } != false &&
                input.validationErrors().isEmpty() &&
                listOf(dateText, outcomeText, otherDetails, placeAndDoctor).all { it == it.normalized() } &&
                clinicianId == clinicianId.trim() &&
                recordedAt.isInstant() && updatedAt.isInstant()
        }
    }
}

internal fun PatientCommand.canonicalFingerprint(): String = when (this) {
    is SavePatientProfileCommand -> canonicalFingerprint(
        listOf(
            recordKind, operationUuid, patientId, expectedPatientRevision?.toString(), createdAt,
            caseId, displayName, dateOfBirth, phone, riskLevel, ageText, addressText, bloodGroupText,
            gravida?.toString(), parity?.toString(), abortion?.toString(), obstetricNotes, clinicalAttention,
        ),
    )
    is SaveOtherHistoriesCommand -> canonicalFingerprint(
        listOf(
            recordKind, operationUuid, patientId, expectedPatientRevision.toString(),
            medicalHistory, surgicalHistory, familyHistory, allergyHistory,
        ),
    )
    is ChangePatientStatusCommand -> canonicalFingerprint(
        listOf(
            recordKind, operationUuid, patientId, expectedPatientRevision.toString(),
            targetStatus, reason, changedAt,
        ),
    )
    is SavePreviousPregnancyCommand -> canonicalFingerprint(
        listOf(
            recordKind, operationUuid, patientId, expectedPatientRevision.toString(), pregnancyId,
            expectedPregnancyRevision?.toString(), modelVersion.toString(), dateText, outcomeText,
            pregnancyType, otherDetails, placeAndDoctor, deliveryBy, outcome, birthMethod,
            lossManagement, terminationMethod, provenanceKind, clinicianId, recordedAt, updatedAt,
        ),
    )
}

internal fun PatientCommand.withCanonicalFingerprint(): PatientCommand = when (this) {
    is SavePatientProfileCommand -> copy(fingerprint = canonicalFingerprint())
    is SaveOtherHistoriesCommand -> copy(fingerprint = canonicalFingerprint())
    is ChangePatientStatusCommand -> copy(fingerprint = canonicalFingerprint())
    is SavePreviousPregnancyCommand -> copy(fingerprint = canonicalFingerprint())
}

internal fun String.isCanonicalUuid(): Boolean = try {
    UUID.fromString(this).toString() == this
} catch (_: IllegalArgumentException) {
    false
}

private fun String.isInstant(): Boolean = try {
    Instant.parse(this).toString() == this
} catch (_: DateTimeParseException) {
    false
}

private data class ParsedGpa(
    val gravida: Long?,
    val parity: Long?,
    val abortion: Long?,
    val errors: Set<PatientValidationError>,
)

private fun parseGpa(gravida: String, parity: String, abortion: String): ParsedGpa {
    val raw = listOf(gravida.trim(), parity.trim(), abortion.trim())
    if (raw.all(String::isEmpty)) return ParsedGpa(null, null, null, emptySet())
    if (raw.any(String::isEmpty)) {
        return ParsedGpa(null, null, null, setOf(PatientValidationError.GPA_ALL_OR_NONE))
    }
    val parsed = raw.map { it.toLongOrNull() }
    if (parsed.any { it == null } || parsed.filterNotNull().any { it < 0 || it > PatientVocabulary.MAX_SAFE_GPA }) {
        return ParsedGpa(null, null, null, setOf(PatientValidationError.INVALID_GPA))
    }
    return ParsedGpa(parsed[0], parsed[1], parsed[2], emptySet())
}
