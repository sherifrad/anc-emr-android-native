package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.WorkspaceSection
import java.security.MessageDigest
import java.util.UUID

enum class ClinicalRecordKind {
    DATING,
    ULTRASOUND_CREATE,
    ULTRASOUND_UPDATE,
    LAB_CREATE,
    LAB_UPDATE,
    MEDICATION_CREATE,
    MEDICATION_UPDATE,
}

enum class ClinicalCommandFailure {
    INVALID_COMMAND,
    PATIENT_NOT_FOUND,
    PATIENT_ARCHIVED,
    RECORD_NOT_FOUND,
    STALE_REVISION,
    STALE_PATIENT_REVISION,
    RECORD_CONFLICT,
    CROSS_PATIENT_WRITE,
    DUPLICATE_OPERATION_MISMATCH,
    REVISION_EXHAUSTED,
    CONFIRMED_NOT_SAVED,
}

sealed interface ClinicalCommandOutcome {
    data class Committed(val snapshot: RecordSnapshot, val receipt: ClinicalCommandReceipt) : ClinicalCommandOutcome
    data class Rejected(val failure: ClinicalCommandFailure) : ClinicalCommandOutcome
    data object Ambiguous : ClinicalCommandOutcome
    data object Obsolete : ClinicalCommandOutcome
}

sealed interface ClinicalCommandPreparation {
    data class Ready(val command: ClinicalCommand) : ClinicalCommandPreparation
    data class Rejected(val errors: Set<ClinicalValidationError>) : ClinicalCommandPreparation
}

data class ClinicalCommandReceipt(
    val operationUuid: String,
    val patientId: String,
    val recordKind: String,
    val recordId: String,
    val committedRevision: Long,
    val committedPatientRevision: Long,
    val fingerprint: String,
)

sealed interface ClinicalCommand {
    val operationUuid: String
    val editorSessionId: String
    val patientId: String
    val expectedPatientRevision: Long
    val recordKind: ClinicalRecordKind
    val recordId: String

    fun destination(): AncRoute
    fun savedNotice(): SavedNotice
    fun isStructurallyValid(): Boolean
    fun computeFingerprint(): String
}

data class SaveDatingCommand(
    override val editorSessionId: String = "",
    override val operationUuid: String,
    override val patientId: String,
    override val expectedPatientRevision: Long,
    val lmpDate: String? = null,
    val ultrasoundEdd: String? = null,
    val agreedEdd: String? = null,
    val datingMethod: String? = null,
    val notes: String? = null,
    val transferDate: String? = null,
    val embryoAgeDays: Int? = null,
) : ClinicalCommand {
    override val recordKind = ClinicalRecordKind.DATING
    override val recordId = patientId

    override fun destination(): AncRoute = AncRoute.Workspace(patientId, WorkspaceSection.DATING)
    override fun savedNotice(): SavedNotice = SavedNotice.DATING

    override fun isStructurallyValid(): Boolean {
        if (!isValidUuid(operationUuid) || patientId.isBlank() || expectedPatientRevision < 1) return false
        if (!ClinicalVocabulary.isValidIsoDate(lmpDate)) return false
        if (!ClinicalVocabulary.isValidIsoDate(ultrasoundEdd)) return false
        if (!ClinicalVocabulary.isValidIsoDate(agreedEdd)) return false
        if (!ClinicalVocabulary.isValidIsoDate(transferDate)) return false
        if (datingMethod != null && datingMethod !in ClinicalVocabulary.datingMethods) return false
        if (embryoAgeDays != null && embryoAgeDays != 3 && embryoAgeDays != 5) return false
        if (datingMethod == "IVF_TRANSFER" && (transferDate == null || (embryoAgeDays != 3 && embryoAgeDays != 5))) return false
        return true
    }

    override fun computeFingerprint(): String = sha256(
        "DATING|$operationUuid|$patientId|$expectedPatientRevision|$lmpDate|$ultrasoundEdd|$agreedEdd|$datingMethod|$notes|$transferDate|$embryoAgeDays",
    )

    companion object {
        fun prepare(state: AncState, operationUuid: String): ClinicalCommandPreparation {
            val draft = state.datingDraft
                ?: return ClinicalCommandPreparation.Rejected(setOf(ClinicalValidationError.SAVE_FAILED))
            val errors = mutableSetOf<ClinicalValidationError>()
            val lmp = draft.values.lmpDate.trim().ifEmpty { null }
            val usEdd = draft.values.ultrasoundEdd.trim().ifEmpty { null }
            val agreedEdd = draft.values.agreedEdd.trim().ifEmpty { null }
            val method = draft.values.datingMethod.trim().ifEmpty { null }
            val transfer = draft.values.transferDate.trim().ifEmpty { null }
            val embryoAge = draft.values.embryoAgeDays.trim().toIntOrNull()
            val notes = draft.values.notes.trim().ifEmpty { null }

            if (lmp != null && !ClinicalVocabulary.isValidIsoDate(lmp)) errors.add(ClinicalValidationError.INVALID_DATE)
            if (usEdd != null && !ClinicalVocabulary.isValidIsoDate(usEdd)) errors.add(ClinicalValidationError.INVALID_DATE)
            if (agreedEdd != null && !ClinicalVocabulary.isValidIsoDate(agreedEdd)) errors.add(ClinicalValidationError.INVALID_DATE)
            if (transfer != null && !ClinicalVocabulary.isValidIsoDate(transfer)) errors.add(ClinicalValidationError.INVALID_DATE)
            if (method != null && method !in ClinicalVocabulary.datingMethods) errors.add(ClinicalValidationError.INVALID_METHOD)
            if (embryoAge != null && embryoAge != 3 && embryoAge != 5) errors.add(ClinicalValidationError.INVALID_VOCABULARY)
            if (method == "IVF_TRANSFER") {
                if (transfer == null) errors.add(ClinicalValidationError.REQUIRED_DATE)
                if (embryoAge == null || (embryoAge != 3 && embryoAge != 5)) errors.add(ClinicalValidationError.INVALID_VOCABULARY)
            }

            if (errors.isNotEmpty()) return ClinicalCommandPreparation.Rejected(errors)

            val command = SaveDatingCommand(
                editorSessionId = draft.editorSessionId,
                operationUuid = operationUuid,
                patientId = draft.patientId,
                expectedPatientRevision = draft.basePatientRevision,
                lmpDate = lmp,
                ultrasoundEdd = usEdd,
                agreedEdd = agreedEdd,
                datingMethod = method,
                transferDate = transfer,
                embryoAgeDays = embryoAge,
                notes = notes,
            )
            return ClinicalCommandPreparation.Ready(command)
        }
    }
}

data class SaveUltrasoundScanCommand(
    override val editorSessionId: String = "",
    override val operationUuid: String,
    override val patientId: String,
    override val expectedPatientRevision: Long,
    val scanId: String,
    val expectedScanRevision: Long?, // null for create
    val date: String,
    val gaWeeks: Int?,
    val gaDays: Int?,
    val fetalNumber: String?,
    val presentation: String?,
    val placentalSite: String?,
    val amnioticFluid: String?,
    val fetalHeartRate: String?,
    val biometryBpd: String?,
    val biometryHc: String?,
    val biometryAc: String?,
    val biometryFl: String?,
    val estimatedFetalWeight: String?,
    val impression: String?,
    val linkedVisitId: String? = null,
    val imageUris: List<String> = emptyList(),
) : ClinicalCommand {
    override val recordKind = if (expectedScanRevision == null) ClinicalRecordKind.ULTRASOUND_CREATE else ClinicalRecordKind.ULTRASOUND_UPDATE
    override val recordId = scanId

    override fun destination(): AncRoute = AncRoute.Workspace(patientId, WorkspaceSection.ULTRASOUND)
    override fun savedNotice(): SavedNotice = SavedNotice.ULTRASOUND

    override fun isStructurallyValid(): Boolean {
        if (!isValidUuid(operationUuid) || patientId.isBlank() || scanId.isBlank() || expectedPatientRevision < 1) return false
        if (expectedScanRevision != null && expectedScanRevision < 1) return false
        if (!ClinicalVocabulary.isValidIsoDate(date) || date.isBlank()) return false
        if (gaWeeks != null && gaWeeks !in 0..45) return false
        if (gaDays != null && gaDays !in 0..6) return false
        if (fetalNumber != null && fetalNumber !in ClinicalVocabulary.fetalNumbers) return false
        if (presentation != null && presentation !in ClinicalVocabulary.fetalPresentations) return false
        if (placentalSite != null && placentalSite !in ClinicalVocabulary.placentalSites) return false
        if (amnioticFluid != null && amnioticFluid !in ClinicalVocabulary.amnioticFluids) return false
        return true
    }

    override fun computeFingerprint(): String = sha256(
        "US|$operationUuid|$patientId|$expectedPatientRevision|$scanId|$expectedScanRevision|$date|$gaWeeks|$gaDays|$fetalNumber|$presentation|$placentalSite|$amnioticFluid|$fetalHeartRate|$biometryBpd|$biometryHc|$biometryAc|$biometryFl|$estimatedFetalWeight|$impression|$linkedVisitId|${imageUris.joinToString(",")}",
    )

    companion object {
        fun prepare(state: AncState, operationUuid: String, scanId: String): ClinicalCommandPreparation {
            val draft = state.ultrasoundDraft
                ?: return ClinicalCommandPreparation.Rejected(setOf(ClinicalValidationError.SAVE_FAILED))
            val errors = mutableSetOf<ClinicalValidationError>()
            val date = draft.values.date.trim()
            if (date.isEmpty()) {
                errors.add(ClinicalValidationError.REQUIRED_DATE)
            } else if (!ClinicalVocabulary.isValidIsoDate(date)) {
                errors.add(ClinicalValidationError.INVALID_DATE)
            }

            val gaWeeks = if (draft.values.gaWeeks.isBlank()) null else {
                draft.values.gaWeeks.trim().toIntOrNull()?.also {
                    if (it !in 0..45) errors.add(ClinicalValidationError.INVALID_GA_WEEKS)
                } ?: run {
                    errors.add(ClinicalValidationError.INVALID_GA_WEEKS)
                    null
                }
            }

            val gaDays = if (draft.values.gaDays.isBlank()) null else {
                draft.values.gaDays.trim().toIntOrNull()?.also {
                    if (it !in 0..6) errors.add(ClinicalValidationError.INVALID_GA_DAYS)
                } ?: run {
                    errors.add(ClinicalValidationError.INVALID_GA_DAYS)
                    null
                }
            }

            val fetalNumber = draft.values.fetalNumber.trim().ifEmpty { null }
            if (fetalNumber != null && fetalNumber !in ClinicalVocabulary.fetalNumbers) {
                errors.add(ClinicalValidationError.INVALID_VOCABULARY)
            }

            val presentation = draft.values.presentation.trim().ifEmpty { null }
            if (presentation != null && presentation !in ClinicalVocabulary.fetalPresentations) {
                errors.add(ClinicalValidationError.INVALID_VOCABULARY)
            }

            val placentalSite = draft.values.placentalSite.trim().ifEmpty { null }
            if (placentalSite != null && placentalSite !in ClinicalVocabulary.placentalSites) {
                errors.add(ClinicalValidationError.INVALID_VOCABULARY)
            }

            val amnioticFluid = draft.values.amnioticFluid.trim().ifEmpty { null }
            if (amnioticFluid != null && amnioticFluid !in ClinicalVocabulary.amnioticFluids) {
                errors.add(ClinicalValidationError.INVALID_VOCABULARY)
            }

            if (errors.isNotEmpty()) return ClinicalCommandPreparation.Rejected(errors)

            val command = SaveUltrasoundScanCommand(
                editorSessionId = draft.editorSessionId,
                operationUuid = operationUuid,
                patientId = draft.patientId,
                expectedPatientRevision = draft.basePatientRevision,
                scanId = scanId,
                expectedScanRevision = draft.baseScanRevision,
                date = date,
                gaWeeks = gaWeeks,
                gaDays = gaDays,
                fetalNumber = fetalNumber,
                presentation = presentation,
                placentalSite = placentalSite,
                amnioticFluid = amnioticFluid,
                fetalHeartRate = draft.values.fetalHeartRate.trim().ifEmpty { null },
                biometryBpd = draft.values.biometryBpd.trim().ifEmpty { null },
                biometryHc = draft.values.biometryHc.trim().ifEmpty { null },
                biometryAc = draft.values.biometryAc.trim().ifEmpty { null },
                biometryFl = draft.values.biometryFl.trim().ifEmpty { null },
                estimatedFetalWeight = draft.values.estimatedFetalWeight.trim().ifEmpty { null },
                impression = draft.values.impression.trim().ifEmpty { null },
                linkedVisitId = draft.values.linkedVisitId.trim().ifEmpty { null },
                imageUris = draft.values.imageUris,
            )
            return ClinicalCommandPreparation.Ready(command)
        }
    }
}

data class SaveLabInvestigationCommand(
    override val editorSessionId: String = "",
    override val operationUuid: String,
    override val patientId: String,
    override val expectedPatientRevision: Long,
    val labId: String,
    val expectedLabRevision: Long?, // null for create
    val date: String,
    val testCategory: String,
    val testName: String,
    val result: String,
    val unit: String?,
    val referenceRange: String?,
    val isAbnormal: Boolean,
    val notes: String?,
    val labName: String? = null,
) : ClinicalCommand {
    override val recordKind = if (expectedLabRevision == null) ClinicalRecordKind.LAB_CREATE else ClinicalRecordKind.LAB_UPDATE
    override val recordId = labId

    override fun destination(): AncRoute = AncRoute.Workspace(patientId, WorkspaceSection.LABORATORY)
    override fun savedNotice(): SavedNotice = SavedNotice.LABORATORY

    override fun isStructurallyValid(): Boolean {
        if (!isValidUuid(operationUuid) || patientId.isBlank() || labId.isBlank() || expectedPatientRevision < 1) return false
        if (expectedLabRevision != null && expectedLabRevision < 1) return false
        if (!ClinicalVocabulary.isValidIsoDate(date) || date.isBlank()) return false
        if (testCategory !in ClinicalVocabulary.testCategories || testName.isBlank() || result.isBlank()) return false
        return true
    }

    override fun computeFingerprint(): String = sha256(
        "LAB|$operationUuid|$patientId|$expectedPatientRevision|$labId|$expectedLabRevision|$date|$testCategory|$testName|$result|$unit|$referenceRange|$isAbnormal|$notes|$labName",
    )

    companion object {
        fun prepare(state: AncState, operationUuid: String, labId: String): ClinicalCommandPreparation {
            val draft = state.labDraft
                ?: return ClinicalCommandPreparation.Rejected(setOf(ClinicalValidationError.SAVE_FAILED))
            val errors = mutableSetOf<ClinicalValidationError>()
            val date = draft.values.date.trim()
            if (date.isEmpty()) {
                errors.add(ClinicalValidationError.REQUIRED_DATE)
            } else if (!ClinicalVocabulary.isValidIsoDate(date)) {
                errors.add(ClinicalValidationError.INVALID_DATE)
            }

            val category = draft.values.testCategory.trim()
            if (category.isEmpty()) {
                errors.add(ClinicalValidationError.REQUIRED_CATEGORY)
            } else if (category !in ClinicalVocabulary.testCategories) {
                errors.add(ClinicalValidationError.INVALID_CATEGORY)
            }

            val name = draft.values.testName.trim()
            if (name.isEmpty()) {
                errors.add(ClinicalValidationError.REQUIRED_NAME)
            }

            val result = draft.values.result.trim()
            if (result.isEmpty()) {
                errors.add(ClinicalValidationError.REQUIRED_RESULT)
            }

            if (errors.isNotEmpty()) return ClinicalCommandPreparation.Rejected(errors)

            val command = SaveLabInvestigationCommand(
                editorSessionId = draft.editorSessionId,
                operationUuid = operationUuid,
                patientId = draft.patientId,
                expectedPatientRevision = draft.basePatientRevision,
                labId = labId,
                expectedLabRevision = draft.baseLabRevision,
                date = date,
                testCategory = category,
                testName = name,
                result = result,
                unit = draft.values.unit.trim().ifEmpty { null },
                referenceRange = draft.values.referenceRange.trim().ifEmpty { null },
                isAbnormal = draft.values.isAbnormal,
                notes = draft.values.notes.trim().ifEmpty { null },
                labName = draft.values.labName.trim().ifEmpty { null },
            )
            return ClinicalCommandPreparation.Ready(command)
        }
    }
}

data class SaveMedicationCommand(
    override val editorSessionId: String = "",
    override val operationUuid: String,
    override val patientId: String,
    override val expectedPatientRevision: Long,
    val medicationId: String,
    val expectedMedicationRevision: Long?, // null for create
    val name: String,
    val dosage: String,
    val route: String,
    val frequency: String,
    val startDate: String,
    val endDate: String?,
    val status: String,
    val notes: String?,
    val isProphylaxis: Boolean,
    val isNonCompliant: Boolean = false,
    val adverseNotes: String? = null,
) : ClinicalCommand {
    override val recordKind = if (expectedMedicationRevision == null) ClinicalRecordKind.MEDICATION_CREATE else ClinicalRecordKind.MEDICATION_UPDATE
    override val recordId = medicationId

    override fun destination(): AncRoute = AncRoute.Workspace(patientId, WorkspaceSection.MEDICATIONS)
    override fun savedNotice(): SavedNotice = SavedNotice.MEDICATIONS

    override fun isStructurallyValid(): Boolean {
        if (!isValidUuid(operationUuid) || patientId.isBlank() || medicationId.isBlank() || expectedPatientRevision < 1) return false
        if (expectedMedicationRevision != null && expectedMedicationRevision < 1) return false
        if (name.isBlank() || dosage.isBlank() || frequency.isBlank()) return false
        if (route !in ClinicalVocabulary.medicationRoutes) return false
        if (status !in ClinicalVocabulary.medicationStatuses) return false
        if (!ClinicalVocabulary.isValidIsoDate(startDate) || startDate.isBlank()) return false
        if (!ClinicalVocabulary.isValidIsoDate(endDate)) return false
        return true
    }

    override fun computeFingerprint(): String = sha256(
        "MED|$operationUuid|$patientId|$expectedPatientRevision|$medicationId|$expectedMedicationRevision|$name|$dosage|$route|$frequency|$startDate|$endDate|$status|$notes|$isProphylaxis|$isNonCompliant|$adverseNotes",
    )

    companion object {
        fun prepare(state: AncState, operationUuid: String, medicationId: String): ClinicalCommandPreparation {
            val draft = state.medicationDraft
                ?: return ClinicalCommandPreparation.Rejected(setOf(ClinicalValidationError.SAVE_FAILED))
            val errors = mutableSetOf<ClinicalValidationError>()

            val name = draft.values.name.trim()
            if (name.isEmpty()) {
                errors.add(ClinicalValidationError.REQUIRED_NAME)
            }

            val dosage = draft.values.dosage.trim()
            if (dosage.isEmpty()) {
                errors.add(ClinicalValidationError.REQUIRED_DOSAGE)
            }

            val route = draft.values.route.trim().ifEmpty { "Oral" }
            if (route !in ClinicalVocabulary.medicationRoutes) {
                errors.add(ClinicalValidationError.INVALID_ROUTE)
            }

            val frequency = draft.values.frequency.trim()
            if (frequency.isEmpty()) {
                errors.add(ClinicalValidationError.REQUIRED_FREQUENCY)
            }

            val startDate = draft.values.startDate.trim()
            if (startDate.isEmpty()) {
                errors.add(ClinicalValidationError.REQUIRED_DATE)
            } else if (!ClinicalVocabulary.isValidIsoDate(startDate)) {
                errors.add(ClinicalValidationError.INVALID_DATE)
            }

            val endDate = draft.values.endDate.trim().ifEmpty { null }
            if (endDate != null && !ClinicalVocabulary.isValidIsoDate(endDate)) {
                errors.add(ClinicalValidationError.INVALID_DATE)
            }

            val status = draft.values.status.trim().ifEmpty { "Active" }
            if (status !in ClinicalVocabulary.medicationStatuses) {
                errors.add(ClinicalValidationError.INVALID_STATUS)
            }

            if (errors.isNotEmpty()) return ClinicalCommandPreparation.Rejected(errors)

            val command = SaveMedicationCommand(
                editorSessionId = draft.editorSessionId,
                operationUuid = operationUuid,
                patientId = draft.patientId,
                expectedPatientRevision = draft.basePatientRevision,
                medicationId = medicationId,
                expectedMedicationRevision = draft.baseMedicationRevision,
                name = name,
                dosage = dosage,
                route = route,
                frequency = frequency,
                startDate = startDate,
                endDate = endDate,
                status = status,
                notes = draft.values.notes.trim().ifEmpty { null },
                isProphylaxis = draft.values.isProphylaxis,
                isNonCompliant = draft.values.isNonCompliant,
                adverseNotes = draft.values.adverseNotes.trim().ifEmpty { null },
            )
            return ClinicalCommandPreparation.Ready(command)
        }
    }
}

private fun isValidUuid(raw: String): Boolean = try {
    UUID.fromString(raw)
    true
} catch (_: Exception) {
    false
}

private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(text.toByteArray())
    .joinToString("") { "%02x".format(it) }
