package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftError
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class SaveVisitCommand(
    val operationUuid: String,
    val editorSessionId: String,
    val patientId: String,
    val visitId: String,
    val expectedRevision: Int?,
    val date: String,
    val bloodPressure: String?,
    val weight: String?,
    val findings: String?,
    val notes: String?,
    val linkedUltrasoundId: String? = null,
    val fingerprint: String,
) {
    companion object {
        fun prepare(
            state: AncState,
            operationUuid: String,
            newVisitId: String,
        ): SavePreparation {
            val draft = state.draft ?: return SavePreparation.Rejected(setOf(DraftError.SAVE_FAILED))
            val date = draft.values.date.trim()
            val dateErrors = when {
                date.isEmpty() -> setOf(DraftError.DATE_REQUIRED)
                !date.isIsoDate() -> setOf(DraftError.INVALID_DATE)
                else -> emptySet()
            }
            if (dateErrors.isNotEmpty()) return SavePreparation.Rejected(dateErrors)

            val patient = state.patient(draft.patientId)
                ?: return SavePreparation.Rejected(setOf(DraftError.SAVE_FAILED))
            val existing = draft.visitId?.let { id -> patient.visits.firstOrNull { it.id == id } }
            if (draft.visitId != null && (existing == null || existing.revision != draft.baseRevision)) {
                return SavePreparation.Rejected(setOf(DraftError.STALE_REVISION))
            }

            val visitId = existing?.id ?: newVisitId
            val command = SaveVisitCommand(
                operationUuid = operationUuid,
                editorSessionId = draft.editorSessionId,
                patientId = draft.patientId,
                visitId = visitId,
                expectedRevision = draft.baseRevision,
                date = date,
                bloodPressure = draft.values.bloodPressure.normalized(),
                weight = draft.values.weight.normalized(),
                findings = draft.values.findings.normalized(),
                notes = draft.values.notes.normalized(),
                linkedUltrasoundId = draft.values.linkedUltrasoundId.normalized(),
                fingerprint = "",
            )
            return SavePreparation.Ready(
                command.copy(fingerprint = command.canonicalFingerprint()),
            )
        }

        private fun String.isIsoDate(): Boolean = try {
            LocalDate.parse(this).toString() == this
        } catch (_: DateTimeParseException) {
            false
        }
    }
}

internal fun SaveVisitCommand.canonicalFingerprint(): String = canonicalFingerprint(
    listOf(
        operationUuid,
        patientId,
        visitId,
        expectedRevision?.toString(),
        date.trim(),
        bloodPressure.normalized(),
        weight.normalized(),
        findings.normalized(),
        notes.normalized(),
        linkedUltrasoundId.normalized(),
    ),
)

internal fun canonicalFingerprint(fields: List<String?>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    fields.forEach { field ->
        if (field == null) {
            digest.update(ByteBuffer.allocate(4).putInt(-1).array())
        } else {
            val bytes = field.toByteArray(StandardCharsets.UTF_8)
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

internal fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)

sealed interface SavePreparation {
    data class Ready(val command: SaveVisitCommand) : SavePreparation
    data class Rejected(val errors: Set<DraftError>) : SavePreparation
}

data class RecordSnapshot(val patients: List<DemoPatient>)

data class SaveReceipt(
    val operationUuid: String,
    val patientId: String,
    val visitId: String,
    val committedRevision: Int,
    val fingerprint: String,
)

enum class SaveFailure {
    INVALID_COMMAND,
    PATIENT_NOT_FOUND,
    VISIT_CONFLICT,
    STALE_REVISION,
    CROSS_PATIENT_WRITE,
    DUPLICATE_OPERATION_MISMATCH,
    REVISION_EXHAUSTED,
    CONFIRMED_NOT_SAVED,
    STORAGE_LOCKED,
    PATIENT_ARCHIVED,
}

sealed interface SaveOutcome {
    data class Committed(val snapshot: RecordSnapshot, val receipt: SaveReceipt) : SaveOutcome
    data class Rejected(val failure: SaveFailure) : SaveOutcome
    data object Ambiguous : SaveOutcome
    data object Obsolete : SaveOutcome
}

internal interface SaveGateway {
    suspend fun save(command: SaveVisitCommand): SaveOutcome
    suspend fun reconcile(command: SaveVisitCommand): SaveOutcome
}

internal class SaveCoordinator(
    private val gateway: SaveGateway,
    private val epochIsCurrent: (Long) -> Boolean,
) {
    suspend fun execute(
        command: SaveVisitCommand,
        sessionEpoch: Long,
        onReconciliation: suspend () -> Unit = {},
    ): SaveOutcome {
        if (!epochIsCurrent(sessionEpoch)) return SaveOutcome.Obsolete
        val first = gateway.save(command)
        if (!epochIsCurrent(sessionEpoch)) return SaveOutcome.Obsolete
        if (first !is SaveOutcome.Ambiguous) return first
        onReconciliation()
        if (!epochIsCurrent(sessionEpoch)) return SaveOutcome.Obsolete
        val reconciled = gateway.reconcile(command)
        return if (epochIsCurrent(sessionEpoch)) reconciled else SaveOutcome.Obsolete
    }
}

internal fun DemoPatient.withCommittedVisit(command: SaveVisitCommand, revision: Int): DemoPatient {
    val visit = DemoVisit(
        id = command.visitId,
        date = command.date,
        bloodPressure = command.bloodPressure,
        weight = command.weight,
        findings = command.findings,
        notes = command.notes,
        linkedUltrasoundId = command.linkedUltrasoundId,
        revision = revision,
        isSyntheticEdit = true,
    )
    return copy(
        visits = visits.filterNot { it.id == visit.id }.plus(visit).sortedByDescending { it.date },
    )
}
