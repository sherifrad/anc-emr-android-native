package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.buildVisitFindings
import com.sherifradwan.ancnative.model.isPlausibleBloodPressure
import com.sherifradwan.ancnative.model.isPlausibleWeightKg
import com.sherifradwan.ancnative.model.parseBloodPressure
import com.sherifradwan.ancnative.model.parseCanonicalIsoDateOrNull
import com.sherifradwan.ancnative.model.parseVisitFindings
import com.sherifradwan.ancnative.model.parseWeightKg
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
            today: LocalDate = LocalDate.now(),
        ): SavePreparation {
            val draft = state.draft ?: return SavePreparation.Rejected(setOf(DraftError.SAVE_FAILED))
            val date = draft.values.date.trim()
            val parsedDate = parseCanonicalIsoDateOrNull(date)

            val sysTrimmed = draft.values.systolic.trim()
            val diaTrimmed = draft.values.diastolic.trim()
            val isPartialBp = (sysTrimmed.isNotEmpty() && diaTrimmed.isEmpty()) ||
                (sysTrimmed.isEmpty() && diaTrimmed.isNotEmpty())

            val bloodPressureRaw = when {
                isPartialBp -> "INVALID_PARTIAL_BP"
                sysTrimmed.isNotEmpty() && diaTrimmed.isNotEmpty() -> "$sysTrimmed/$diaTrimmed"
                else -> draft.values.bloodPressure.trim()
            }

            val hasStructuredFindings = draft.values.fundalHeight.isNotBlank() ||
                draft.values.oedema.isNotBlank() ||
                draft.values.fetalMovements.isNotBlank() ||
                draft.values.urineProtein.isNotBlank()

            val findingsRaw = if (hasStructuredFindings) {
                val parsed = parseVisitFindings(draft.values.findings)
                buildVisitFindings(
                    fundalHeight = draft.values.fundalHeight,
                    oedema = draft.values.oedema,
                    fetalMovements = draft.values.fetalMovements,
                    urineProtein = draft.values.urineProtein,
                    additionalFindings = parsed.additionalFindings,
                )
            } else {
                draft.values.findings
            }

            val errors = buildSet {
                when {
                    date.isEmpty() -> add(DraftError.DATE_REQUIRED)
                    parsedDate == null -> add(DraftError.INVALID_DATE)
                    parsedDate.isAfter(today) -> add(DraftError.DATE_IN_FUTURE)
                }
                val bloodPressure = bloodPressureRaw.normalized()
                if (bloodPressure == "INVALID_PARTIAL_BP" ||
                    (bloodPressure != null && parseBloodPressure(bloodPressure)?.let(::isPlausibleBloodPressure) != true)
                ) {
                    add(DraftError.BLOOD_PRESSURE_INVALID)
                }
                val weight = draft.values.weight.normalized()
                if (weight != null && parseWeightKg(weight)?.let(::isPlausibleWeightKg) != true) {
                    add(DraftError.WEIGHT_INVALID)
                }
            }
            if (errors.isNotEmpty()) return SavePreparation.Rejected(errors)

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
                bloodPressure = bloodPressureRaw.normalized(),
                weight = draft.values.weight.normalized(),
                findings = findingsRaw.normalized(),
                notes = draft.values.notes.normalized(),
                linkedUltrasoundId = draft.values.linkedUltrasoundId.normalized(),
                fingerprint = "",
            )
            return SavePreparation.Ready(
                command.copy(fingerprint = command.canonicalFingerprint()),
            )
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
    return digest.digest().toHexLower()
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
        isSyntheticEdit = false,
    )
    return copy(
        visits = visits.filterNot { it.id == visit.id }.plus(visit).sortedByDescending { it.date },
    )
}
