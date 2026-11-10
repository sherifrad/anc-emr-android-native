package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_NON_COMPLIANT
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.PreviousPregnancyFormRules
import com.sherifradwan.ancnative.model.effectiveMedicationStatus
import com.sherifradwan.ancnative.model.parseBloodPressure
import com.sherifradwan.ancnative.model.parseVisitFindings
import com.sherifradwan.ancnative.storage.ClinicalRecordKind
import com.sherifradwan.ancnative.storage.DeletableRecord
import com.sherifradwan.ancnative.storage.DeletableRegistry
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.VaultState

enum class AttachmentError { SAVE_FAILED }

enum class WorkspaceSection {
    OBSTETRIC_HISTORY,
    DATING,
    OTHER_HISTORIES,
    EPISODE_OVERVIEW,
    VISITS,
    ULTRASOUND,
    LABORATORY,
    MEDICATIONS,
    DELIVERY,
    ATTACHMENTS,
}

sealed interface AncRoute {
    data object PatientList : AncRoute
    data class Workspace(
        val patientId: String,
        val section: WorkspaceSection = WorkspaceSection.VISITS,
    ) : AncRoute
    data class VisitEditor(val patientId: String) : AncRoute
    data class PatientEditor(val patientId: String, val isNew: Boolean) : AncRoute
    data class OtherHistoriesEditor(val patientId: String) : AncRoute
    data class PreviousPregnancyEditor(val patientId: String, val pregnancyId: String) : AncRoute
    data class StatusEditor(val patientId: String, val targetStatus: String) : AncRoute
    data class DatingEditor(val patientId: String) : AncRoute
    data class UltrasoundEditor(val patientId: String, val scanId: String, val isNew: Boolean) : AncRoute
    data class LabEditor(val patientId: String, val labId: String, val isNew: Boolean) : AncRoute
    data class LabBatchEditor(val patientId: String) : AncRoute
    data class MedicationEditor(val patientId: String, val medicationId: String, val isNew: Boolean) : AncRoute
}

sealed interface PendingAction {
    data class Navigate(val destination: AncRoute) : PendingAction
    data class StartVisit(
        val patientId: String,
        val visitId: String?,
        val editorSessionId: String,
    ) : PendingAction
}

enum class DraftField {
    DATE,
    BLOOD_PRESSURE,
    WEIGHT,
    FINDINGS,
    NOTES,
    LINKED_ULTRASOUND,
    SYSTOLIC,
    DIASTOLIC,
    FUNDAL_HEIGHT,
    FETAL_HEART_RATE,
    OEDEMA,
    FETAL_MOVEMENTS,
    URINE_PROTEIN,
}
enum class PatientDraftField {
    CASE_ID,
    DISPLAY_NAME,
    DATE_OF_BIRTH,
    PHONE,
    AGE_TEXT,
    ADDRESS_TEXT,
    BLOOD_GROUP,
    RISK_LEVEL,
    GRAVIDA,
    PARITY,
    ABORTION,
    OBSTETRIC_NOTES,
    CLINICAL_ATTENTION,
    LMP_DATE,
    DATING_METHOD,
    TRANSFER_DATE,
    EMBRYO_AGE_DAYS,
    AGREED_EDD,
    SCAN_DATE,
    SCAN_GA_WEEKS,
    SCAN_GA_DAYS,
}
enum class HistoriesDraftField { MEDICAL, SURGICAL, FAMILY, ALLERGY }
enum class PregnancyDraftField {
    DATE_TEXT,
    PREGNANCY_TYPE,
    OTHER_DETAILS,
    PLACE_AND_DOCTOR,
    DELIVERY_BY,
    OUTCOME,
    LOSS_MANAGEMENT,
    TERMINATION_METHOD,
}
enum class DatingDraftField {
    LMP_DATE,
    ULTRASOUND_EDD,
    AGREED_EDD,
    DATING_METHOD,
    TRANSFER_DATE,
    EMBRYO_AGE_DAYS,
    NOTES,
}
enum class UltrasoundDraftField {
    DATE,
    GA_WEEKS,
    GA_DAYS,
    FETAL_NUMBER,
    PRESENTATION,
    PLACENTAL_SITE,
    AMNIOTIC_FLUID,
    FETAL_HEART_RATE,
    BIOMETRY_BPD,
    BIOMETRY_HC,
    BIOMETRY_AC,
    BIOMETRY_FL,
    ESTIMATED_FETAL_WEIGHT,
    IMPRESSION,
    LINKED_VISIT,
    IMAGE_URIS,
}
enum class LabDraftField {
    DATE,
    CATEGORY,
    NAME,
    RESULT,
    UNIT,
    REFERENCE_RANGE,
    NOTES,
    LAB_NAME,
}
enum class MedicationDraftField {
    NAME,
    DOSAGE,
    ROUTE,
    FREQUENCY,
    START_DATE,
    END_DATE,
    STATUS,
    NOTES,
    ADVERSE_NOTES,
}

enum class DraftError {
    DATE_REQUIRED,
    INVALID_DATE,
    DATE_IN_FUTURE,
    BLOOD_PRESSURE_INVALID,
    WEIGHT_INVALID,
    STALE_REVISION,
    SAVE_FAILED,
}
enum class ClinicalValidationError {
    REQUIRED_DATE,
    INVALID_DATE,
    INVALID_METHOD,
    INVALID_GA_WEEKS,
    INVALID_GA_DAYS,
    INVALID_VOCABULARY,
    REQUIRED_NAME,
    REQUIRED_RESULT,
    REQUIRED_DOSAGE,
    REQUIRED_FREQUENCY,
    REQUIRED_CATEGORY,
    INVALID_CATEGORY,
    INVALID_ROUTE,
    INVALID_STATUS,
    SAVE_FAILED,
}
enum class SavePhase { IDLE, PENDING, RECONCILING, ERROR, UNRESOLVED }
fun SavePhase.blocksEditing(): Boolean =
    this == SavePhase.PENDING || this == SavePhase.RECONCILING || this == SavePhase.UNRESOLVED
enum class SaveUiError {
    CONFIRMED_NOT_SAVED,
    STALE_OR_CONFLICT,
    CROSS_PATIENT_REJECTED,
    DUPLICATE_OPERATION_MISMATCH,
    PATIENT_ARCHIVED,
    CASE_ID_CONFLICT,
    STORAGE_ERROR,
    OUTCOME_UNRESOLVED,
}

fun SaveUiError?.stringRes(): Int = when (this) {
    SaveUiError.CONFIRMED_NOT_SAVED -> R.string.save_confirmed_not_saved
    SaveUiError.STALE_OR_CONFLICT -> R.string.save_stale_conflict
    SaveUiError.CROSS_PATIENT_REJECTED -> R.string.save_cross_patient_rejected
    SaveUiError.DUPLICATE_OPERATION_MISMATCH -> R.string.save_operation_mismatch
    SaveUiError.PATIENT_ARCHIVED -> R.string.save_patient_archived
    SaveUiError.CASE_ID_CONFLICT -> R.string.save_case_id_conflict
    SaveUiError.OUTCOME_UNRESOLVED -> R.string.save_unresolved
    SaveUiError.STORAGE_ERROR, null -> R.string.save_storage_error
}
enum class SavedNotice {
    VISIT,
    PATIENT,
    PATIENT_DATING_NOT_UPDATED,
    HISTORIES,
    PREGNANCY,
    ARCHIVED,
    UNARCHIVED,
    DATING,
    ULTRASOUND,
    LABORATORY,
    MEDICATIONS,
    MEDICATION_DELETED,
    MEDICATION_RESTORED,
    MEDICATION_SAVE_UNDONE,
    LAB_DELETED,
    LAB_RESTORED,
    LAB_SAVE_UNDONE,
    ULTRASOUND_DELETED,
    ULTRASOUND_RESTORED,
    ULTRASOUND_SAVE_UNDONE,
    VISIT_DELETED,
    VISIT_RESTORED,
    VISIT_SAVE_UNDONE,
    PREVIOUS_PREGNANCY_DELETED,
    PREVIOUS_PREGNANCY_RESTORED,
    PREVIOUS_PREGNANCY_SAVE_UNDONE,
    ATTACHMENT_DELETED,
    ATTACHMENT_RESTORED,
}
fun SavedNotice?.stringRes(): Int = when (this) {
    SavedNotice.PATIENT -> R.string.patient_saved
    SavedNotice.PATIENT_DATING_NOT_UPDATED -> R.string.patient_saved_dating_not_updated
    SavedNotice.HISTORIES -> R.string.histories_saved
    SavedNotice.PREGNANCY -> R.string.pregnancy_saved
    SavedNotice.ARCHIVED -> R.string.patient_archived_notice
    SavedNotice.UNARCHIVED -> R.string.patient_unarchived_notice
    SavedNotice.DATING -> R.string.dating_saved
    SavedNotice.ULTRASOUND -> R.string.ultrasound_saved
    SavedNotice.LABORATORY -> R.string.lab_saved
    SavedNotice.MEDICATIONS -> R.string.medication_saved
    SavedNotice.MEDICATION_DELETED -> R.string.medication_deleted
    SavedNotice.MEDICATION_RESTORED -> R.string.medication_restored
    SavedNotice.MEDICATION_SAVE_UNDONE -> R.string.medication_save_undone
    SavedNotice.LAB_DELETED -> R.string.lab_deleted
    SavedNotice.LAB_RESTORED -> R.string.lab_restored
    SavedNotice.LAB_SAVE_UNDONE -> R.string.lab_save_undone
    SavedNotice.ULTRASOUND_DELETED -> R.string.ultrasound_deleted
    SavedNotice.ULTRASOUND_RESTORED -> R.string.ultrasound_restored
    SavedNotice.ULTRASOUND_SAVE_UNDONE -> R.string.ultrasound_save_undone
    SavedNotice.VISIT_DELETED -> R.string.visit_deleted
    SavedNotice.VISIT_RESTORED -> R.string.visit_restored
    SavedNotice.VISIT_SAVE_UNDONE -> R.string.visit_save_undone
    SavedNotice.PREVIOUS_PREGNANCY_DELETED -> R.string.previous_pregnancy_deleted
    SavedNotice.PREVIOUS_PREGNANCY_RESTORED -> R.string.previous_pregnancy_restored
    SavedNotice.PREVIOUS_PREGNANCY_SAVE_UNDONE -> R.string.previous_pregnancy_save_undone
    SavedNotice.ATTACHMENT_DELETED -> R.string.attachment_deleted
    SavedNotice.ATTACHMENT_RESTORED -> R.string.attachment_restored
    SavedNotice.VISIT, null -> R.string.visit_saved
}
enum class PatientEditorKind { PROFILE, HISTORIES, PREGNANCY, STATUS }

data class PendingDeletion(
    val patientId: String,
    val type: DeletableType,
    val recordId: String,
)

enum class UndoAction {
    DELETE,
    SAVE,
}

enum class UndoPhase {
    READY,
    RUNNING,
}

data class UndoOffer(
    val token: String,
    val patientId: String,
    val type: DeletableType,
    val action: UndoAction,
    val label: String,
    val phase: UndoPhase = UndoPhase.READY,
    val patientName: String = "",
    val recordId: String = "",
)

enum class DeleteMessageKind {
    /** The delete was rejected; the record is still there. */
    DELETE_FAILED,

    /** An Undo failed; the record is still in the state it had before the Undo (deleted, or saved). */
    UNDO_FAILED,

    /** Undo of a save was refused because that record is being edited. */
    UNDO_BLOCKED,

    /** The record is deleted but its payload file could not be removed; the next unlock sweeps it up. */
    CLEANUP_FAILED,
}

/** A visible failure message for the shared delete/Undo layer. Only shown for the patient it belongs to. */
data class DeleteMessage(
    val kind: DeleteMessageKind,
    val patientId: String,
    val type: DeletableType,
    val label: String,
    val action: UndoAction? = null,
    val error: SaveUiError? = null,
    val patientName: String = "",
)

/**
 * A failure report must not vanish because the clinician moved on: it names the patient and the record, so it stays
 * through a patient switch until dismissed or replaced. A "finish your edits" refusal is only about the open screen.
 */
val DeleteMessageKind.survivesPatientSwitch: Boolean get() = this != DeleteMessageKind.UNDO_BLOCKED

/**
 * The message left on screen after an unrelated success (new offer, new confirmation, committed delete, started Undo)
 * for [patientId]. A failure that belongs to another patient is never wiped by it: it goes only by dismiss or by
 * another failure. A message of this patient, or a "finish your edits" refusal, is cleared as before.
 */
internal fun AncState.deleteMessageAfterSuccessFor(patientId: String): DeleteMessage? =
    deleteMessage?.takeIf { it.patientId != patientId && it.kind.survivesPatientSwitch }

/** The patient a route belongs to, or null for the patient list. */
fun AncRoute.patientIdOrNull(): String? = when (this) {
    AncRoute.PatientList -> null
    is AncRoute.Workspace -> patientId
    is AncRoute.VisitEditor -> patientId
    is AncRoute.PatientEditor -> patientId
    is AncRoute.OtherHistoriesEditor -> patientId
    is AncRoute.PreviousPregnancyEditor -> patientId
    is AncRoute.StatusEditor -> patientId
    is AncRoute.DatingEditor -> patientId
    is AncRoute.UltrasoundEditor -> patientId
    is AncRoute.LabEditor -> patientId
    is AncRoute.LabBatchEditor -> patientId
    is AncRoute.MedicationEditor -> patientId
}

/** A failed Undo may restart its 8 s window at most this many times. */
const val MAX_UNDO_RESTARTS = 1

fun undoMayRestart(failuresSoFar: Int): Boolean = failuresSoFar <= MAX_UNDO_RESTARTS - 1

/** True only for the live READY offer with this token (a RUNNING offer ignores further taps). */
fun AncState.canRequestUndo(token: String): Boolean =
    undoOffer?.let { it.token == token && it.phase == UndoPhase.READY } == true

/** True when the offer with this token is the live RUNNING one (the reducer accepted the request). */
fun AncState.canRunUndo(token: String): Boolean =
    undoOffer?.let { it.token == token && it.phase == UndoPhase.RUNNING } == true

/** True while an Undo that would remove or restore this record is in flight: the record must not be opened for edit. */
fun AncState.undoRunningFor(recordId: String): Boolean =
    undoOffer?.let { it.phase == UndoPhase.RUNNING && it.recordId == recordId } == true

/** Medication saves are blocked while an Undo is in flight. */
val AncState.undoRunning: Boolean get() = undoOffer?.phase == UndoPhase.RUNNING

/** What a vault lock does to the delete/undo UI state. */
fun AncState.clearedForLock(): AncState =
    copy(deleteConfirmation = null, undoOffer = null, deleteRunning = false, deleteMessage = null, noticePatientId = null)

/**
 * Keeps the delete/Undo UI state tied to what is on screen: a dialog for a record that is gone closes, and the
 * offer, failure message and delete/restore notice of one patient never follow the clinician to another patient.
 */
internal fun AncState.withDeleteUiConsistent(): AncState {
    var next = this
    val open = route.patientIdOrNull()
    val offer = next.undoOffer
    if (open != null && offer != null && offer.patientId != open) next = next.copy(undoOffer = null)
    val message = next.deleteMessage
    if (open != null && message != null && message.patientId != open && !message.kind.survivesPatientSwitch) next = next.copy(deleteMessage = null)
    val noticeOwner = next.noticePatientId
    if (noticeOwner != null && open != null && noticeOwner != open) {
        next = next.copy(savedNotice = false, notice = null, noticePatientId = null)
    }
    val pending = next.deleteConfirmation
    if (pending != null) {
        val exists = next.patient(pending.patientId)
            ?.let { DeletableRegistry.of<DeletableRecord>(pending.type).record(it, pending.recordId) } != null
        if (!exists) next = next.copy(deleteConfirmation = null)
    }
    return next
}

data class PendingSaveToken(
    val operationUuid: String,
    val editorSessionId: String,
)

data class DraftValues(
    val date: String = "",
    val bloodPressure: String = "",
    val weight: String = "",
    val findings: String = "",
    val notes: String = "",
    val linkedUltrasoundId: String = "",
    val systolic: String = "",
    val diastolic: String = "",
    val fundalHeight: String = "",
    val fetalHeartRate: String = "",
    val oedema: String = "",
    val fetalMovements: String = "",
    val urineProtein: String = "",
)

data class VisitDraft(
    val editorSessionId: String,
    val patientId: String,
    val visitId: String?,
    val baseRevision: Int?,
    val original: DraftValues,
    val values: DraftValues,
    val errors: Set<DraftError> = emptySet(),
) {
    val isDirty: Boolean get() = values != original
}

data class PatientDraftValues(
    val caseId: String = "",
    val displayName: String = "",
    val dateOfBirth: String = "",
    val phone: String = "",
    val ageText: String = "",
    val addressText: String = "",
    val bloodGroupText: String = "",
    val riskLevel: String = "",
    val gravida: String = "",
    val parity: String = "",
    val abortion: String = "",
    val obstetricNotes: String = "",
    val clinicalAttention: String = "",
    val lmpDate: String = "",
    val datingMethod: String = "",
    val transferDate: String = "",
    val embryoAgeDays: String = "",
    val agreedEdd: String = "",
    val scanDate: String = "",
    val scanGaWeeks: String = "",
    val scanGaDays: String = "",
)

data class PatientDraft(
    val editorSessionId: String,
    val patientId: String,
    val baseRevision: Long?,
    val createdAt: String?,
    val original: PatientDraftValues,
    val values: PatientDraftValues,
    val errors: Set<PatientValidationError> = emptySet(),
) {
    val isDirty: Boolean get() = values != original
}

data class HistoriesDraftValues(
    val medicalHistory: String = "",
    val surgicalHistory: String = "",
    val familyHistory: String = "",
    val allergyHistory: String = "",
)

data class HistoriesDraft(
    val editorSessionId: String,
    val patientId: String,
    val baseRevision: Long,
    val original: HistoriesDraftValues,
    val values: HistoriesDraftValues,
    val errors: Set<PatientValidationError> = emptySet(),
) {
    val isDirty: Boolean get() = values != original
}

data class PreviousPregnancyDraftValues(
    val dateText: String = "",
    val outcomeText: String = "",
    val pregnancyType: String = "",
    val otherDetails: String = "",
    val placeAndDoctor: String = "",
    val deliveryBy: String = "",
    val outcome: String = "",
    val birthMethod: String = "",
    val lossManagement: String = "",
    val terminationMethod: String = "",
    val clinicianId: String = "",
)

data class PreviousPregnancyDraft(
    val editorSessionId: String,
    val patientId: String,
    val pregnancyId: String,
    val basePatientRevision: Long,
    val basePregnancyRevision: Long?,
    val recordedAt: String?,
    val original: PreviousPregnancyDraftValues,
    val values: PreviousPregnancyDraftValues,
    val errors: Set<PatientValidationError> = emptySet(),
) {
    val isDirty: Boolean get() = values != original
}

data class PatientStatusDraft(
    val editorSessionId: String,
    val patientId: String,
    val baseRevision: Long,
    val targetStatus: String,
    val reason: String = "",
    val errors: Set<PatientValidationError> = emptySet(),
) {
    val isDirty: Boolean get() = reason.isNotEmpty()
}

data class DatingDraftValues(
    val lmpDate: String = "",
    val ultrasoundEdd: String = "",
    val agreedEdd: String = "",
    val datingMethod: String = "",
    val transferDate: String = "",
    val embryoAgeDays: String = "",
    val notes: String = "",
)

data class DatingDraft(
    val editorSessionId: String,
    val patientId: String,
    val basePatientRevision: Long,
    val original: DatingDraftValues,
    val values: DatingDraftValues,
    val errors: Set<ClinicalValidationError> = emptySet(),
) {
    val isDirty: Boolean get() = values != original
}

data class UltrasoundDraftValues(
    val date: String = "",
    val gaWeeks: String = "",
    val gaDays: String = "",
    val fetalNumber: String = "",
    val presentation: String = "",
    val placentalSite: String = "",
    val amnioticFluid: String = "",
    val fetalHeartRate: String = "",
    val biometryBpd: String = "",
    val biometryHc: String = "",
    val biometryAc: String = "",
    val biometryFl: String = "",
    val estimatedFetalWeight: String = "",
    val impression: String = "",
    val linkedVisitId: String = "",
    val imageUris: List<String> = emptyList(),
)

data class UltrasoundDraft(
    val editorSessionId: String,
    val patientId: String,
    val scanId: String,
    val basePatientRevision: Long,
    val baseScanRevision: Long?,
    val original: UltrasoundDraftValues,
    val values: UltrasoundDraftValues,
    val errors: Set<ClinicalValidationError> = emptySet(),
) {
    val isDirty: Boolean get() = values != original
}

data class LabDraftValues(
    val date: String = "",
    val testCategory: String = "",
    val testName: String = "",
    val result: String = "",
    val unit: String = "",
    val referenceRange: String = "",
    val isAbnormal: Boolean = false,
    val notes: String = "",
    val labName: String = "",
)

data class LabDraft(
    val editorSessionId: String,
    val patientId: String,
    val labId: String,
    val basePatientRevision: Long,
    val baseLabRevision: Long?,
    val original: LabDraftValues,
    val values: LabDraftValues,
    val errors: Set<ClinicalValidationError> = emptySet(),
) {
    val isDirty: Boolean get() = values != original
}

data class MedicationDraftValues(
    val name: String = "",
    val dosage: String = "",
    val route: String = "",
    val frequency: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "",
    val isProphylaxis: Boolean = false,
    val notes: String = "",
    val isNonCompliant: Boolean = false,
    val adverseNotes: String = "",
)

data class MedicationDraft(
    val editorSessionId: String,
    val patientId: String,
    val medicationId: String,
    val basePatientRevision: Long,
    val baseMedicationRevision: Long?,
    val original: MedicationDraftValues,
    val values: MedicationDraftValues,
    val errors: Set<ClinicalValidationError> = emptySet(),
) {
    val isDirty: Boolean get() = values != original
}

data class AncState(
    val patients: List<DemoPatient>,
    val vaultState: VaultState = VaultState.Locked,
    val route: AncRoute = AncRoute.PatientList,
    val searchQuery: String = "",
    val draft: VisitDraft? = null,
    val patientDraft: PatientDraft? = null,
    val historiesDraft: HistoriesDraft? = null,
    val pregnancyDraft: PreviousPregnancyDraft? = null,
    val statusDraft: PatientStatusDraft? = null,
    val datingDraft: DatingDraft? = null,
    val ultrasoundDraft: UltrasoundDraft? = null,
    val labDraft: LabDraft? = null,
    val medicationDraft: MedicationDraft? = null,
    val labBatchDraft: LabBatchDraft? = null,
    val showDiscardDialog: Boolean = false,
    val pendingAction: PendingAction? = null,
    val showNewPatientInfo: Boolean = false,
    val savedNotice: Boolean = false,
    val notice: SavedNotice? = null,
    val savePhase: SavePhase = SavePhase.IDLE,
    val saveError: SaveUiError? = null,
    val pendingSave: PendingSaveToken? = null,
    val attachmentError: AttachmentError? = null,
    val draftRecoveryPrompt: DraftRecoveryPrompt? = null,
    val deleteConfirmation: PendingDeletion? = null,
    val undoOffer: UndoOffer? = null,
    val deleteRunning: Boolean = false,
    val deleteMessage: DeleteMessage? = null,
    val noticePatientId: String? = null,
) {
    fun patient(patientId: String): DemoPatient? = patients.firstOrNull { it.id == patientId }

    fun visiblePatients(): List<DemoPatient> {
        val query = searchQuery.trim()
        if (query.isEmpty()) return patients
        return patients.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                it.caseId.contains(query, ignoreCase = true) ||
                (it.phone != null && it.phone.contains(query, ignoreCase = true)) ||
                (it.bloodGroupText != null && it.bloodGroupText.contains(query, ignoreCase = true))
        }
    }

    val hasDirtyDraft: Boolean
        get() = draft?.isDirty == true ||
            patientDraft?.isDirty == true ||
            historiesDraft?.isDirty == true ||
            pregnancyDraft?.isDirty == true ||
            statusDraft?.isDirty == true ||
            datingDraft?.isDirty == true ||
            ultrasoundDraft?.isDirty == true ||
            labDraft?.isDirty == true ||
            medicationDraft?.isDirty == true ||
            // A batch is unsaved work from the moment it holds a test, even before a result is typed.
            labBatchDraft?.items?.isNotEmpty() == true
}

sealed interface AncEvent {
    data class SearchChanged(val query: String) : AncEvent
    data object ClearSearch : AncEvent
    data object ShowNewPatientInfo : AncEvent
    data object HideNewPatientInfo : AncEvent
    data class OpenWorkspace(val patientId: String) : AncEvent
    data class SelectSection(val section: WorkspaceSection) : AncEvent
    data class StartVisit(val patientId: String, val visitId: String?, val editorSessionId: String) : AncEvent
    data class StartPatientEditor(
        val patientId: String?,
        val newPatientId: String,
        val editorSessionId: String,
    ) : AncEvent
    data class StartHistoriesEditor(val patientId: String, val editorSessionId: String) : AncEvent
    data class StartPregnancyEditor(
        val patientId: String,
        val pregnancyId: String?,
        val newPregnancyId: String,
        val editorSessionId: String,
    ) : AncEvent
    data class StartStatusEditor(
        val patientId: String,
        val targetStatus: String,
        val editorSessionId: String,
    ) : AncEvent
    data class DraftChanged(val field: DraftField, val value: String) : AncEvent
    data class PatientDraftChanged(val field: PatientDraftField, val value: String) : AncEvent
    data class HistoriesDraftChanged(val field: HistoriesDraftField, val value: String) : AncEvent
    data class PregnancyDraftChanged(val field: PregnancyDraftField, val value: String) : AncEvent
    data class StatusReasonChanged(val value: String) : AncEvent
    data object SubmitPatientProfile : AncEvent
    data object SubmitOtherHistories : AncEvent
    data object SubmitPreviousPregnancy : AncEvent
    data object SubmitPatientStatus : AncEvent
    data class StartDatingEditor(val patientId: String, val editorSessionId: String) : AncEvent
    data class StartUltrasoundEditor(
        val patientId: String,
        val scanId: String?,
        val newScanId: String,
        val editorSessionId: String,
    ) : AncEvent
    data class StartLabEditor(
        val patientId: String,
        val labId: String?,
        val newLabId: String,
        val editorSessionId: String,
        val defaultDate: String = "",
        val defaultLabName: String = "",
    ) : AncEvent
    data class StartMedicationEditor(
        val patientId: String,
        val medicationId: String?,
        val newMedicationId: String,
        val editorSessionId: String,
        val defaultStartDate: String = "",
        /** "Save and add another": open the blank editor but keep the just-shown saved notice. */
        val keepSavedNotice: Boolean = false,
    ) : AncEvent
    data class DatingDraftChanged(val field: DatingDraftField, val value: String) : AncEvent
    data class UltrasoundDraftChanged(val field: UltrasoundDraftField, val value: String) : AncEvent
    data class LabDraftChanged(val field: LabDraftField, val value: String) : AncEvent
    data class LabAbnormalChanged(val isAbnormal: Boolean) : AncEvent
    data class MedicationDraftChanged(val field: MedicationDraftField, val value: String) : AncEvent
    data object SubmitDating : AncEvent
    data object SubmitUltrasound : AncEvent
    data object SubmitLab : AncEvent
    data object SubmitLabAndAddAnother : AncEvent
    data object SubmitMedication : AncEvent
    data object SubmitMedicationAndAddAnother : AncEvent
    data class AddAttachment(
        val patientId: String,
        val sourceUri: android.net.Uri,
        val category: String,
        val title: String?,
        val linkedRecordId: String? = null,
        val preset: com.sherifradwan.ancnative.storage.PhotoQualityPreset =
            com.sherifradwan.ancnative.storage.PhotoQualityPreset.defaultFor(category),
    ) : AncEvent
    data class ImportScanPhoto(
        val patientId: String,
        val sourceUri: android.net.Uri,
        val preset: com.sherifradwan.ancnative.storage.PhotoQualityPreset =
            com.sherifradwan.ancnative.storage.PhotoQualityPreset.DETAILED,
    ) : AncEvent
    data class ScanPhotoImported(val patientId: String, val ref: String) : AncEvent
    data class AttachmentFailed(val error: AttachmentError) : AncEvent
    data object DismissAttachmentError : AncEvent
    data class SaveValidationFailed(val errors: Set<DraftError>) : AncEvent
    data class ClinicalValidationFailed(
        val recordKind: ClinicalRecordKind,
        val errors: Set<ClinicalValidationError>,
    ) : AncEvent
    data class PatientValidationFailed(
        val editor: PatientEditorKind,
        val errors: Set<PatientValidationError>,
    ) : AncEvent
    data class SaveStarted(val token: PendingSaveToken) : AncEvent
    data class SaveReconciling(val token: PendingSaveToken) : AncEvent
    data class SaveFailed(
        val token: PendingSaveToken,
        val error: SaveUiError,
        val unresolved: Boolean,
    ) : AncEvent
    data class SaveCommitted(
        val token: PendingSaveToken,
        val patients: List<DemoPatient>,
        val patientId: String,
    ) : AncEvent
    data class RecordCommitted(
        val token: PendingSaveToken,
        val patients: List<DemoPatient>,
        val destination: AncRoute,
        val notice: SavedNotice,
    ) : AncEvent
    data object CancelEditor : AncEvent
    data object Back : AncEvent
    data object ConfirmDiscard : AncEvent
    data object KeepEditing : AncEvent
    data object DismissNotice : AncEvent
    data class DraftRecoveryFound(val prompt: DraftRecoveryPrompt) : AncEvent
    data object AcceptDraftRecovery : AncEvent
    data object DiscardDraftRecovery : AncEvent
    data class RequestDelete(val patientId: String, val type: DeletableType, val recordId: String) : AncEvent
    data object CancelDelete : AncEvent
    data object ConfirmDelete : AncEvent
    data object DeleteStarted : AncEvent
    data object DismissDeleteMessage : AncEvent
    data class DeleteCommitted(
        val patients: List<DemoPatient>,
        val offer: UndoOffer? = null,
        val notice: SavedNotice? = null,
        val patientId: String? = null,
    ) : AncEvent
    data class DeleteFailed(
        val error: SaveUiError? = null,
        val patientId: String = "",
        val type: DeletableType? = null,
        val label: String = "",
        val patientName: String = "",
    ) : AncEvent
    data class UndoOffered(val offer: UndoOffer) : AncEvent
    data class UndoRequested(val token: String) : AncEvent
    data class UndoCommitted(
        val token: String,
        val patients: List<DemoPatient>,
        val notice: SavedNotice? = null,
        val patientId: String? = null,
    ) : AncEvent
    data class UndoFailed(
        val token: String,
        val error: SaveUiError? = null,
        /** The offer the coordinator ran, so the failure is still reported if the UI has dropped it. */
        val offer: UndoOffer? = null,
    ) : AncEvent
    data class UndoExpired(val token: String) : AncEvent
    data class StartLabBatch(
        val patientId: String,
        val sessionId: String,
        val basePatientRevision: Long,
        val date: String,
        val labName: String?,
    ) : AncEvent
    data class LabBatchItemsAdded(val items: List<LabBatchItem>) : AncEvent
    data object SubmitLabBatch : AncEvent
    data class StartLabBatchRequest(
        val patientId: String,
        val date: String? = null,
        val labName: String? = null,
    ) : AncEvent
    data class LabBatchItemRemoved(val labId: String) : AncEvent
    data class LabBatchResultChanged(val labId: String, val value: String) : AncEvent
    data class LabBatchRangeChanged(val labId: String, val value: String) : AncEvent
    data class LabBatchFlagChanged(val labId: String, val isAbnormal: Boolean) : AncEvent
    data class LabBatchNotesChanged(val labId: String, val value: String) : AncEvent
    data class LabBatchTabSelected(val tab: LabBatchTab) : AncEvent
    data class LabBatchCursorMoved(val index: Int) : AncEvent
    data class LabBatchDateChanged(val value: String) : AncEvent
    data class LabBatchLabNameChanged(val value: String?) : AncEvent
    data object CancelLabBatch : AncEvent

    /**
     * A committed delete whose payload file could not be removed. Carries the record it belonged to, because the offer
     * is usually gone by the time the asynchronous cleanup reports.
     */
    data class PayloadCleanupFailed(
        val token: String,
        val error: SaveUiError,
        val patientId: String,
        val type: DeletableType,
        val label: String,
        val patientName: String = "",
    ) : AncEvent
}

object AncReducer {
    fun reduce(state: AncState, event: AncEvent): AncState {
        val next = reduceInternal(state, event).withDeleteUiConsistent()
        val prompt = next.draftRecoveryPrompt
        return if (prompt != null && activeEditorSessionId(next) != prompt.editorSessionId) {
            next.copy(draftRecoveryPrompt = null)
        } else {
            next
        }
    }

    private fun isValueChangeEvent(event: AncEvent): Boolean = when (event) {
        is AncEvent.DraftChanged,
        is AncEvent.PatientDraftChanged,
        is AncEvent.HistoriesDraftChanged,
        is AncEvent.PregnancyDraftChanged,
        is AncEvent.StatusReasonChanged,
        is AncEvent.DatingDraftChanged,
        is AncEvent.UltrasoundDraftChanged,
        is AncEvent.LabDraftChanged,
        is AncEvent.LabAbnormalChanged,
        is AncEvent.MedicationDraftChanged,
        is AncEvent.ScanPhotoImported -> true
        else -> false
    }

    private fun reduceInternal(state: AncState, event: AncEvent): AncState {
        if (state.draftRecoveryPrompt != null && isValueChangeEvent(event)) {
            return state
        }
        return when (event) {
            is AncEvent.SearchChanged -> state.copy(searchQuery = event.query)
            AncEvent.ClearSearch -> state.copy(searchQuery = "")
            AncEvent.ShowNewPatientInfo -> state.copy(showNewPatientInfo = true)
            AncEvent.HideNewPatientInfo -> state.copy(showNewPatientInfo = false)
            is AncEvent.OpenWorkspace -> navigate(state, AncRoute.Workspace(event.patientId))
            is AncEvent.SelectSection -> {
                val route = state.route as? AncRoute.Workspace
                if (route == null) state else state.copy(route = route.copy(section = event.section), savedNotice = false, notice = null)
            }
            is AncEvent.StartVisit -> startVisit(state, event)
            is AncEvent.StartPatientEditor -> startPatientEditor(state, event)
            is AncEvent.StartHistoriesEditor -> startHistoriesEditor(state, event)
            is AncEvent.StartPregnancyEditor -> startPregnancyEditor(state, event)
            is AncEvent.StartStatusEditor -> startStatusEditor(state, event)
            is AncEvent.StartDatingEditor -> startDatingEditor(state, event)
            is AncEvent.StartUltrasoundEditor -> startUltrasoundEditor(state, event)
            is AncEvent.StartLabEditor -> startLabEditor(state, event)
            is AncEvent.StartMedicationEditor -> startMedicationEditor(state, event)
            is AncEvent.DraftChanged -> updateDraft(state, event)
            is AncEvent.PatientDraftChanged -> updatePatientDraft(state, event)
            is AncEvent.HistoriesDraftChanged -> updateHistoriesDraft(state, event)
            is AncEvent.PregnancyDraftChanged -> updatePregnancyDraft(state, event)
            is AncEvent.DatingDraftChanged -> updateDatingDraft(state, event)
            is AncEvent.UltrasoundDraftChanged -> updateUltrasoundDraft(state, event)
            is AncEvent.LabDraftChanged -> updateLabDraft(state, event)
            is AncEvent.LabAbnormalChanged -> updateLabAbnormal(state, event.isAbnormal)
            is AncEvent.MedicationDraftChanged -> updateMedicationDraft(state, event)
            is AncEvent.StatusReasonChanged -> state.copy(
                statusDraft = state.statusDraft?.copy(reason = event.value, errors = emptySet()),
                savePhase = SavePhase.IDLE,
                saveError = null,
            )
            AncEvent.SubmitPatientProfile,
            AncEvent.SubmitOtherHistories,
            AncEvent.SubmitPreviousPregnancy,
            AncEvent.SubmitPatientStatus,
            AncEvent.SubmitDating,
            AncEvent.SubmitUltrasound,
            AncEvent.SubmitLab,
            AncEvent.SubmitLabAndAddAnother,
            AncEvent.SubmitLabBatch,
            is AncEvent.StartLabBatchRequest,
            AncEvent.SubmitMedication,
            AncEvent.SubmitMedicationAndAddAnother,
            -> state
            is AncEvent.SaveValidationFailed -> state.copy(
                draft = state.draft?.copy(errors = event.errors),
                savePhase = SavePhase.IDLE,
                saveError = null,
            )
            is AncEvent.ClinicalValidationFailed -> if (DeletableRegistry.isDeleteOrRestoreKind(event.recordKind)) {
                // Delete and restore commands (every deletable kind) have no draft, so there is nothing to mark.
                state
            } else when (event.recordKind) {
                ClinicalRecordKind.DATING -> state.copy(datingDraft = state.datingDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
                ClinicalRecordKind.ULTRASOUND_CREATE, ClinicalRecordKind.ULTRASOUND_UPDATE -> state.copy(ultrasoundDraft = state.ultrasoundDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
                ClinicalRecordKind.LAB_CREATE, ClinicalRecordKind.LAB_UPDATE -> state.copy(labDraft = state.labDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
                ClinicalRecordKind.MEDICATION_CREATE, ClinicalRecordKind.MEDICATION_UPDATE -> state.copy(medicationDraft = state.medicationDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
                // A validation failure is not a storage error: it belongs on the draft, like every other editor.
                ClinicalRecordKind.LAB_BATCH_SAVE -> state.copy(
                    labBatchDraft = state.labBatchDraft?.copy(errors = event.errors),
                    savePhase = SavePhase.IDLE,
                    saveError = null,
                )
                ClinicalRecordKind.LAB_BATCH_DELETE,
                ClinicalRecordKind.LAB_BATCH_RESTORE,
                -> state.copy(savePhase = SavePhase.IDLE, saveError = null)
                else -> error("Unhandled non-deletable clinical record kind: ${event.recordKind}")
            }
            is AncEvent.PatientValidationFailed -> validationFailed(state, event)
            is AncEvent.SaveStarted -> if (state.canAccept(event.token)) {
                state.copy(
                    savePhase = SavePhase.PENDING,
                    saveError = null,
                    pendingSave = event.token,
                    savedNotice = false,
                    notice = null,
                )
            } else state
            is AncEvent.SaveReconciling -> if (state.pendingSave == event.token) {
                state.copy(savePhase = SavePhase.RECONCILING, saveError = null)
            } else state
            is AncEvent.SaveFailed -> if (state.pendingSave == event.token) {
                state.copy(
                    savePhase = if (event.unresolved) SavePhase.UNRESOLVED else SavePhase.ERROR,
                    saveError = event.error,
                    pendingSave = if (event.unresolved) event.token else null,
                )
            } else state
            is AncEvent.SaveCommitted -> if (state.pendingSave == event.token) {
                state.clearDrafts().copy(
                    patients = event.patients,
                    route = AncRoute.Workspace(event.patientId, WorkspaceSection.VISITS),
                    savedNotice = true,
                    notice = SavedNotice.VISIT,
                    savePhase = SavePhase.IDLE,
                    saveError = null,
                    pendingSave = null,
                )
            } else state
            is AncEvent.RecordCommitted -> if (state.pendingSave == event.token) {
                state.clearDrafts().copy(
                    patients = event.patients,
                    route = event.destination,
                    savedNotice = true,
                    notice = event.notice,
                    savePhase = SavePhase.IDLE,
                    saveError = null,
                    pendingSave = null,
                )
            } else state
            AncEvent.CancelEditor, AncEvent.Back -> requestBack(state)
            AncEvent.ConfirmDiscard -> confirmDiscard(state)
            AncEvent.KeepEditing -> state.copy(showDiscardDialog = false, pendingAction = null)
            AncEvent.DismissNotice -> state.copy(savedNotice = false, notice = null)
            is AncEvent.AddAttachment, is AncEvent.ImportScanPhoto -> state
            is AncEvent.ScanPhotoImported -> {
                val draft = state.ultrasoundDraft
                if (draft == null || draft.patientId != event.patientId) state
                else state.copy(
                    ultrasoundDraft = draft.copy(
                        values = draft.values.copy(imageUris = (draft.values.imageUris + event.ref).distinct()),
                    ),
                )
            }
            is AncEvent.AttachmentFailed -> state.copy(attachmentError = event.error)
            AncEvent.DismissAttachmentError -> state.copy(attachmentError = null)
            is AncEvent.DraftRecoveryFound -> {
                if (state.draftRecoveryPrompt == null && activeEditorSessionId(state) == event.prompt.editorSessionId) {
                    state.copy(draftRecoveryPrompt = event.prompt)
                } else {
                    state
                }
            }
            AncEvent.AcceptDraftRecovery -> {
                val prompt = state.draftRecoveryPrompt ?: return state
                applyDraftRecord(state, prompt.record).copy(draftRecoveryPrompt = null)
            }
            AncEvent.DiscardDraftRecovery -> state.copy(draftRecoveryPrompt = null)
            is AncEvent.RequestDelete -> {
                val patient = state.patient(event.patientId)
                if (patient == null || patient.status == "archived" || state.hasDirtyDraft || state.savePhase.blocksEditing() || state.undoRunning || state.deleteRunning) {
                    state
                } else {
                    state.copy(
                        deleteConfirmation = PendingDeletion(
                            patientId = event.patientId,
                            type = event.type,
                            recordId = event.recordId,
                        ),
                        deleteMessage = state.deleteMessageAfterSuccessFor(event.patientId),
                    )
                }
            }
            AncEvent.CancelDelete -> state.copy(deleteConfirmation = null)
            AncEvent.ConfirmDelete -> {
                val pending = state.deleteConfirmation ?: return state
                if (state.hasDirtyDraft || state.savePhase.blocksEditing() || state.undoRunning || state.deleteRunning) return state
                val hook = EditorHooks.of(pending.type)
                val next = if (hook != null && hook.isOpenFor(state, pending.recordId)) {
                    hook.closeEditor(state, pending.patientId)
                } else {
                    state
                }
                next.copy(deleteConfirmation = null)
            }
            AncEvent.DeleteStarted -> state.copy(deleteRunning = true)
            AncEvent.DismissDeleteMessage -> state.copy(deleteMessage = null)
            is AncEvent.DeleteCommitted -> state.copy(
                patients = event.patients,
                undoOffer = event.offer,
                deleteConfirmation = null,
                deleteRunning = false,
                deleteMessage = state.deleteMessageAfterSuccessFor(event.patientId ?: event.offer?.patientId ?: ""),
                savedNotice = event.notice != null,
                notice = event.notice ?: state.notice,
                noticePatientId = if (event.notice != null) event.patientId else state.noticePatientId,
            )
            is AncEvent.DeleteFailed -> state.copy(
                deleteConfirmation = null,
                deleteRunning = false,
                saveError = event.error,
                deleteMessage = event.type?.let {
                    DeleteMessage(
                        kind = DeleteMessageKind.DELETE_FAILED,
                        patientId = event.patientId,
                        type = it,
                        label = event.label,
                        action = UndoAction.DELETE,
                        error = event.error,
                        patientName = event.patientName,
                    )
                },
            )
            is AncEvent.UndoOffered -> state.copy(undoOffer = event.offer, deleteMessage = state.deleteMessageAfterSuccessFor(event.offer.patientId))
            is AncEvent.UndoRequested -> {
                val offer = state.undoOffer
                if (offer == null || !state.canRequestUndo(event.token) || state.deleteRunning) {
                    state
                } else {
                    val hook = EditorHooks.of(offer.type)
                    val editorOpen = offer.action == UndoAction.SAVE && hook != null && hook.isOpenFor(state, offer.recordId)
                    if (editorOpen && (state.hasDirtyDraft || state.savePhase.blocksEditing())) {
                        // Undoing this save would delete a record that has unsaved edits open: refuse and say so.
                        state.copy(
                            deleteMessage = DeleteMessage(
                                kind = DeleteMessageKind.UNDO_BLOCKED,
                                patientId = offer.patientId,
                                type = offer.type,
                                label = offer.label,
                                action = offer.action,
                            ),
                        )
                    } else {
                        val closed = if (editorOpen && hook != null) hook.closeEditor(state, offer.patientId) else state
                        closed.copy(undoOffer = offer.copy(phase = UndoPhase.RUNNING), deleteMessage = state.deleteMessageAfterSuccessFor(offer.patientId))
                    }
                }
            }
            is AncEvent.UndoCommitted -> {
                var nextState = state
                for (hook in EditorHooks.all()) {
                    nextState = hook.rebaseCreateDraft(nextState, event.patients)
                }
                nextState.copy(
                    patients = event.patients,
                    undoOffer = if (nextState.undoOffer?.token == event.token) null else nextState.undoOffer,
                    savedNotice = event.notice != null,
                    notice = event.notice ?: nextState.notice,
                    noticePatientId = if (event.notice != null) event.patientId else nextState.noticePatientId,
                )
            }
            is AncEvent.UndoFailed -> {
                val offer = state.undoOffer
                if (state.vaultState !is VaultState.Unlocked) {
                    // A late failure after a lock must not write into the locked state.
                    state
                } else if (offer != null && offer.token == event.token) {
                    state.copy(
                        undoOffer = offer.copy(phase = UndoPhase.READY),
                        saveError = event.error,
                        deleteMessage = DeleteMessage(
                            kind = DeleteMessageKind.UNDO_FAILED,
                            patientId = offer.patientId,
                            type = offer.type,
                            label = offer.label,
                            action = offer.action,
                            error = event.error,
                            patientName = offer.patientName,
                        ),
                    )
                } else if (event.offer != null) {
                    // The offer was dropped (another patient opened, or a new offer replaced it): still say so.
                    val lost = event.offer
                    // saveError belongs to the open screen's own editor: do not overwrite it with another patient's failure.
                    state.copy(
                        deleteMessage = DeleteMessage(
                            kind = DeleteMessageKind.UNDO_FAILED,
                            patientId = lost.patientId,
                            type = lost.type,
                            label = lost.label,
                            action = lost.action,
                            error = event.error,
                            patientName = lost.patientName,
                        ),
                    )
                } else {
                    state
                }
            }
            is AncEvent.UndoExpired -> {
                if (state.undoOffer?.token == event.token) state.copy(undoOffer = null) else state
            }
            is AncEvent.StartLabBatch -> {
                if (state.savePhase.blocksEditing() || state.hasDirtyDraft) state
                else state.copy(
                    labBatchDraft = LabBatchDraft(
                        patientId = event.patientId,
                        editorSessionId = event.sessionId,
                        basePatientRevision = event.basePatientRevision,
                        date = event.date,
                        labName = event.labName,
                    ),
                    route = AncRoute.LabBatchEditor(event.patientId),
                )
            }
            is AncEvent.LabBatchItemsAdded -> state.copy(
                labBatchDraft = state.labBatchDraft?.add(event.items),
            )
            is AncEvent.LabBatchItemRemoved -> state.copy(
                labBatchDraft = state.labBatchDraft?.remove(event.labId),
            )
            is AncEvent.LabBatchResultChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.withItem(event.labId) { it.copy(result = event.value, isAbnormal = false) },
            )
            is AncEvent.LabBatchRangeChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.withItem(event.labId) { it.copy(referenceRange = event.value) },
            )
            is AncEvent.LabBatchFlagChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.withItem(event.labId) { it.copy(isAbnormal = event.isAbnormal) },
            )
            is AncEvent.LabBatchNotesChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.withItem(event.labId) { it.copy(notes = event.value) },
            )
            is AncEvent.LabBatchTabSelected -> state.copy(
                labBatchDraft = state.labBatchDraft?.copy(tab = event.tab),
            )
            is AncEvent.LabBatchCursorMoved -> state.copy(
                labBatchDraft = state.labBatchDraft?.moveCursor(event.index),
            )
            is AncEvent.LabBatchDateChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.copy(date = event.value),
            )
            is AncEvent.LabBatchLabNameChanged -> state.copy(
                labBatchDraft = state.labBatchDraft?.copy(labName = event.value),
            )
            is AncEvent.CancelLabBatch -> {
                // Only the batch is discarded: cancelling it must not wipe an unrelated editor draft (D9).
                val patientId = state.labBatchDraft?.patientId ?: state.route.patientIdOrNull()
                val cleared = state.copy(labBatchDraft = null, savePhase = SavePhase.IDLE, saveError = null)
                if (patientId == null) cleared else cleared.copy(route = AncRoute.Workspace(patientId, WorkspaceSection.LABORATORY))
            }
            is AncEvent.PayloadCleanupFailed -> {
                if (state.vaultState !is VaultState.Unlocked) {
                    // A late cleanup failure after a lock must not write into the locked state.
                    state
                } else {
                    val shown = state.deleteMessage
                    val sameRecordFailure = shown != null &&
                        shown.kind == DeleteMessageKind.UNDO_FAILED &&
                        shown.patientId == event.patientId &&
                        shown.type == event.type &&
                        shown.label == event.label
                    if (sameRecordFailure) {
                        // A failed Undo of this record is the more important message; the sweep retries the file.
                        state
                    } else {
                        // The delete stands; only the payload file is still on disk. Say so and let the next sweep retry.
                        state.copy(
                            deleteMessage = DeleteMessage(
                                kind = DeleteMessageKind.CLEANUP_FAILED,
                                patientId = event.patientId,
                                type = event.type,
                                label = event.label,
                                error = event.error,
                                patientName = event.patientName,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun navigate(state: AncState, destination: AncRoute): AncState {
        if (state.savePhase.blocksEditing()) return state
        return if (state.hasDirtyDraft) {
            state.copy(showDiscardDialog = true, pendingAction = PendingAction.Navigate(destination))
        } else {
            state.clearDrafts().copy(route = destination, savedNotice = false, notice = null)
        }
    }

    private fun startVisit(state: AncState, event: AncEvent.StartVisit): AncState {
        if (state.savePhase.blocksEditing()) return state
        val patient = state.patient(event.patientId) ?: return state
        if (patient.status == "archived") return state
        if (event.visitId != null && state.undoRunningFor(event.visitId)) return state
        val visit = event.visitId?.let { id -> patient.visits.firstOrNull { it.id == id } }
        if (event.visitId != null && visit == null) return state
        if (state.hasDirtyDraft) {
            return state.copy(
                showDiscardDialog = true,
                pendingAction = PendingAction.StartVisit(event.patientId, event.visitId, event.editorSessionId),
            )
        }
        val values = visit?.toDraftValues() ?: DraftValues()
        return state.clearDrafts().copy(
            route = AncRoute.VisitEditor(event.patientId),
            draft = VisitDraft(
                editorSessionId = event.editorSessionId,
                patientId = event.patientId,
                visitId = visit?.id,
                baseRevision = visit?.revision?.toInt(),
                original = values,
                values = values,
            ),
            savedNotice = false,
            notice = null,
        )
    }

    private fun startPatientEditor(state: AncState, event: AncEvent.StartPatientEditor): AncState {
        if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
        val patient = event.patientId?.let(state::patient)
        if (event.patientId != null && (patient == null || patient.status == "archived")) return state
        val values = patient?.toPatientDraftValues() ?: run {
            val year = java.time.LocalDate.now().year
            val nextSeq = (state.patients.size + 1).toString().padStart(3, '0')
            PatientDraftValues(caseId = "ANC-$year-$nextSeq")
        }
        val patientId = patient?.id ?: event.newPatientId
        return state.clearDrafts().copy(
            route = AncRoute.PatientEditor(patientId, patient == null),
            patientDraft = PatientDraft(
                editorSessionId = event.editorSessionId,
                patientId = patientId,
                baseRevision = patient?.revision,
                createdAt = patient?.createdAt,
                original = values,
                values = values,
            ),
            savedNotice = false,
            notice = null,
        )
    }

    private fun startHistoriesEditor(state: AncState, event: AncEvent.StartHistoriesEditor): AncState {
        if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
        val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
        val values = HistoriesDraftValues(
            medicalHistory = patient.medicalHistory.orEmpty(),
            surgicalHistory = patient.surgicalHistory.orEmpty(),
            familyHistory = patient.familyHistory.orEmpty(),
            allergyHistory = patient.allergyHistory.orEmpty(),
        )
        return state.clearDrafts().copy(
            route = AncRoute.OtherHistoriesEditor(patient.id),
            historiesDraft = HistoriesDraft(event.editorSessionId, patient.id, patient.revision, values, values),
            savedNotice = false,
            notice = null,
        )
    }

    private fun startPregnancyEditor(state: AncState, event: AncEvent.StartPregnancyEditor): AncState {
        if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
        val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
        if (event.pregnancyId != null && state.undoRunningFor(event.pregnancyId)) return state
        val pregnancy = event.pregnancyId?.let { id -> patient.previousPregnancies.firstOrNull { it.id == id } }
        if (event.pregnancyId != null && pregnancy == null) return state
        val values = pregnancy?.toDraftValues() ?: PreviousPregnancyDraftValues()
        val pregnancyId = pregnancy?.id ?: event.newPregnancyId
        return state.clearDrafts().copy(
            route = AncRoute.PreviousPregnancyEditor(patient.id, pregnancyId),
            pregnancyDraft = PreviousPregnancyDraft(
                editorSessionId = event.editorSessionId,
                patientId = patient.id,
                pregnancyId = pregnancyId,
                basePatientRevision = patient.revision,
                basePregnancyRevision = pregnancy?.revision,
                recordedAt = pregnancy?.recordedAt,
                original = values,
                values = values,
            ),
            savedNotice = false,
            notice = null,
        )
    }

    private fun startStatusEditor(state: AncState, event: AncEvent.StartStatusEditor): AncState {
        if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
        val patient = state.patient(event.patientId) ?: return state
        if (
            (event.targetStatus == "archived" && patient.status == "archived") ||
            (event.targetStatus == "active" && patient.status != "archived")
        ) return state
        return state.clearDrafts().copy(
            route = AncRoute.StatusEditor(patient.id, event.targetStatus),
            statusDraft = PatientStatusDraft(
                editorSessionId = event.editorSessionId,
                patientId = patient.id,
                baseRevision = patient.revision,
                targetStatus = event.targetStatus,
            ),
            savedNotice = false,
            notice = null,
        )
    }

    private fun startDatingEditor(state: AncState, event: AncEvent.StartDatingEditor): AncState {
        if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
        val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
        val values = patient.dating?.toDraftValues() ?: DatingDraftValues()
        return state.clearDrafts().copy(
            route = AncRoute.DatingEditor(patient.id),
            datingDraft = DatingDraft(
                editorSessionId = event.editorSessionId,
                patientId = patient.id,
                basePatientRevision = patient.revision,
                original = values,
                values = values,
            ),
            savedNotice = false,
            notice = null,
        )
    }

    private fun startUltrasoundEditor(state: AncState, event: AncEvent.StartUltrasoundEditor): AncState {
        if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
        if (event.scanId != null && state.undoRunningFor(event.scanId)) return state
        val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
        val scan = event.scanId?.let { id -> patient.ultrasoundScans.firstOrNull { it.id == id } }
        if (event.scanId != null && scan == null) return state
        val values = scan?.toDraftValues() ?: UltrasoundDraftValues()
        val scanId = scan?.id ?: event.newScanId
        return state.clearDrafts().copy(
            route = AncRoute.UltrasoundEditor(patient.id, scanId, isNew = scan == null),
            ultrasoundDraft = UltrasoundDraft(
                editorSessionId = event.editorSessionId,
                patientId = patient.id,
                scanId = scanId,
                basePatientRevision = patient.revision,
                baseScanRevision = scan?.revision,
                original = values,
                values = values,
            ),
            savedNotice = false,
            notice = null,
        )
    }

    private fun startLabEditor(state: AncState, event: AncEvent.StartLabEditor): AncState {
        if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
        if (event.labId != null && state.undoRunningFor(event.labId)) return state
        val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
        val lab = event.labId?.let { id -> patient.labInvestigations.firstOrNull { it.id == id } }
        if (event.labId != null && lab == null) return state
        val values = lab?.toDraftValues() ?: LabDraftValues(
            date = event.defaultDate,
            labName = event.defaultLabName,
        )
        val labId = lab?.id ?: event.newLabId
        return state.clearDrafts().copy(
            route = AncRoute.LabEditor(patient.id, labId, isNew = lab == null),
            labDraft = LabDraft(
                editorSessionId = event.editorSessionId,
                patientId = patient.id,
                labId = labId,
                basePatientRevision = patient.revision,
                baseLabRevision = lab?.revision,
                original = values,
                values = values,
            ),
            savedNotice = false,
            notice = null,
        )
    }

    private fun startMedicationEditor(state: AncState, event: AncEvent.StartMedicationEditor): AncState {
        if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
        if (event.medicationId != null && state.undoRunningFor(event.medicationId)) return state
        val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
        val med = event.medicationId?.let { id -> patient.medications.firstOrNull { it.id == id } }
        val defaultStart = event.defaultStartDate.ifEmpty { java.time.LocalDate.now().toString() }
        val values = med?.toDraftValues() ?: MedicationDraftValues(
            route = "Oral",
            status = MEDICATION_STATUS_ACTIVE,
            startDate = defaultStart,
        )
        val medicationId = med?.id ?: event.newMedicationId
        return state.clearDrafts().copy(
            route = AncRoute.MedicationEditor(patient.id, medicationId, isNew = med == null),
            medicationDraft = MedicationDraft(
                editorSessionId = event.editorSessionId,
                patientId = patient.id,
                medicationId = medicationId,
                basePatientRevision = patient.revision,
                baseMedicationRevision = med?.revision,
                original = values,
                values = values,
            ),
            savedNotice = if (event.keepSavedNotice) state.savedNotice else false,
            notice = if (event.keepSavedNotice) state.notice else null,
        )
    }

    private fun updateDraft(state: AncState, event: AncEvent.DraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.draft ?: return state
        val values = when (event.field) {
            DraftField.DATE -> draft.values.copy(date = event.value)
            DraftField.BLOOD_PRESSURE -> {
                val raw = event.value.trim()
                if (raw.isEmpty()) {
                    draft.values.copy(
                        bloodPressure = "",
                        systolic = "",
                        diastolic = "",
                    )
                } else {
                    val parsed = parseBloodPressure(raw)
                    draft.values.copy(
                        bloodPressure = raw,
                        systolic = parsed?.systolic?.toString() ?: draft.values.systolic,
                        diastolic = parsed?.diastolic?.toString() ?: draft.values.diastolic,
                    )
                }
            }
            DraftField.WEIGHT -> draft.values.copy(weight = event.value)
            DraftField.FINDINGS -> draft.values.copy(findings = event.value)
            DraftField.NOTES -> draft.values.copy(notes = event.value)
            DraftField.LINKED_ULTRASOUND -> draft.values.copy(linkedUltrasoundId = event.value)
            DraftField.SYSTOLIC -> {
                val s = event.value.trim()
                val d = draft.values.diastolic.trim()
                val bp = if (s.isNotEmpty() && d.isNotEmpty()) "$s/$d" else ""
                draft.values.copy(systolic = s, bloodPressure = bp)
            }
            DraftField.DIASTOLIC -> {
                val s = draft.values.systolic.trim()
                val d = event.value.trim()
                val bp = if (s.isNotEmpty() && d.isNotEmpty()) "$s/$d" else ""
                draft.values.copy(diastolic = d, bloodPressure = bp)
            }
            DraftField.FUNDAL_HEIGHT -> draft.values.copy(fundalHeight = event.value)
            DraftField.FETAL_HEART_RATE -> draft.values.copy(fetalHeartRate = event.value)
            DraftField.OEDEMA -> draft.values.copy(oedema = event.value)
            DraftField.FETAL_MOVEMENTS -> draft.values.copy(fetalMovements = event.value)
            DraftField.URINE_PROTEIN -> draft.values.copy(urineProtein = event.value)
        }
        return state.copy(draft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun updatePatientDraft(state: AncState, event: AncEvent.PatientDraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.patientDraft ?: return state
        val values = when (event.field) {
            PatientDraftField.CASE_ID -> draft.values.copy(caseId = event.value)
            PatientDraftField.DISPLAY_NAME -> draft.values.copy(displayName = event.value)
            PatientDraftField.DATE_OF_BIRTH -> draft.values.copy(dateOfBirth = event.value)
            PatientDraftField.PHONE -> draft.values.copy(phone = event.value)
            PatientDraftField.AGE_TEXT -> draft.values.copy(ageText = event.value)
            PatientDraftField.ADDRESS_TEXT -> draft.values.copy(addressText = event.value)
            PatientDraftField.BLOOD_GROUP -> draft.values.copy(bloodGroupText = event.value)
            PatientDraftField.RISK_LEVEL -> draft.values.copy(riskLevel = event.value)
            PatientDraftField.GRAVIDA -> draft.values.copy(gravida = event.value)
            PatientDraftField.PARITY -> draft.values.copy(parity = event.value)
            PatientDraftField.ABORTION -> draft.values.copy(abortion = event.value)
            PatientDraftField.OBSTETRIC_NOTES -> draft.values.copy(obstetricNotes = event.value)
            PatientDraftField.CLINICAL_ATTENTION -> draft.values.copy(clinicalAttention = event.value)
            PatientDraftField.LMP_DATE -> draft.values.copy(lmpDate = event.value)
            PatientDraftField.DATING_METHOD -> draft.values.copy(datingMethod = event.value)
            PatientDraftField.TRANSFER_DATE -> draft.values.copy(transferDate = event.value)
            PatientDraftField.EMBRYO_AGE_DAYS -> draft.values.copy(embryoAgeDays = event.value)
            PatientDraftField.AGREED_EDD -> draft.values.copy(agreedEdd = event.value)
            PatientDraftField.SCAN_DATE -> draft.values.copy(scanDate = event.value)
            PatientDraftField.SCAN_GA_WEEKS -> draft.values.copy(scanGaWeeks = event.value)
            PatientDraftField.SCAN_GA_DAYS -> draft.values.copy(scanGaDays = event.value)
        }
        return state.copy(patientDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun updateHistoriesDraft(state: AncState, event: AncEvent.HistoriesDraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.historiesDraft ?: return state
        val values = when (event.field) {
            HistoriesDraftField.MEDICAL -> draft.values.copy(medicalHistory = event.value)
            HistoriesDraftField.SURGICAL -> draft.values.copy(surgicalHistory = event.value)
            HistoriesDraftField.FAMILY -> draft.values.copy(familyHistory = event.value)
            HistoriesDraftField.ALLERGY -> draft.values.copy(allergyHistory = event.value)
        }
        return state.copy(historiesDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun updatePregnancyDraft(state: AncState, event: AncEvent.PregnancyDraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.pregnancyDraft ?: return state
        val values = when (event.field) {
            PregnancyDraftField.DATE_TEXT -> draft.values.copy(dateText = event.value)
            PregnancyDraftField.PREGNANCY_TYPE -> draft.values.copy(pregnancyType = event.value)
            PregnancyDraftField.OTHER_DETAILS -> draft.values.copy(otherDetails = event.value)
            PregnancyDraftField.PLACE_AND_DOCTOR -> draft.values.copy(placeAndDoctor = event.value)
            PregnancyDraftField.DELIVERY_BY -> PreviousPregnancyFormRules.deliveryColumns(
                draft.values.outcome, draft.original.outcome, draft.values.deliveryBy, draft.values.birthMethod, event.value,
            ).let { draft.values.copy(deliveryBy = it.deliveryBy.orEmpty(), birthMethod = it.birthMethod.orEmpty()) }
            PregnancyDraftField.OUTCOME -> PreviousPregnancyFormRules.deliveryColumns(
                event.value, draft.original.outcome, draft.values.deliveryBy, draft.values.birthMethod,
                originalDeliveryBy = draft.original.deliveryBy, originalBirthMethod = draft.original.birthMethod,
            ).let { columns -> draft.values.copy(
                outcome = event.value,
                outcomeText = if (event.value.isNotBlank()) "" else draft.values.outcomeText,
                deliveryBy = columns.deliveryBy.orEmpty(),
                birthMethod = columns.birthMethod.orEmpty(),
                lossManagement = draft.values.lossManagement.takeIf {
                    event.value == "EarlyPregnancyLoss"
                }.orEmpty(),
                terminationMethod = draft.values.terminationMethod.takeIf {
                    event.value == "TerminationOfPregnancy"
                }.orEmpty(),
            ) }
            PregnancyDraftField.LOSS_MANAGEMENT -> draft.values.copy(lossManagement = event.value)
            PregnancyDraftField.TERMINATION_METHOD -> draft.values.copy(terminationMethod = event.value)
        }
        return state.copy(pregnancyDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun updateDatingDraft(state: AncState, event: AncEvent.DatingDraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.datingDraft ?: return state
        val values = applyDatingChange(draft.values, event.field, event.value)
        return state.copy(datingDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun updateUltrasoundDraft(state: AncState, event: AncEvent.UltrasoundDraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.ultrasoundDraft ?: return state
        val values = when (event.field) {
            UltrasoundDraftField.DATE -> draft.values.copy(date = event.value)
            UltrasoundDraftField.GA_WEEKS -> draft.values.copy(gaWeeks = event.value)
            UltrasoundDraftField.GA_DAYS -> draft.values.copy(gaDays = event.value)
            UltrasoundDraftField.FETAL_NUMBER -> draft.values.copy(fetalNumber = event.value)
            UltrasoundDraftField.PRESENTATION -> draft.values.copy(presentation = event.value)
            UltrasoundDraftField.PLACENTAL_SITE -> draft.values.copy(placentalSite = event.value)
            UltrasoundDraftField.AMNIOTIC_FLUID -> draft.values.copy(amnioticFluid = event.value)
            UltrasoundDraftField.FETAL_HEART_RATE -> draft.values.copy(fetalHeartRate = event.value)
            UltrasoundDraftField.BIOMETRY_BPD -> draft.values.copy(biometryBpd = event.value)
            UltrasoundDraftField.BIOMETRY_HC -> draft.values.copy(biometryHc = event.value)
            UltrasoundDraftField.BIOMETRY_AC -> draft.values.copy(biometryAc = event.value)
            UltrasoundDraftField.BIOMETRY_FL -> draft.values.copy(biometryFl = event.value)
            UltrasoundDraftField.ESTIMATED_FETAL_WEIGHT -> draft.values.copy(estimatedFetalWeight = event.value)
            UltrasoundDraftField.IMPRESSION -> draft.values.copy(impression = event.value)
            UltrasoundDraftField.LINKED_VISIT -> draft.values.copy(linkedVisitId = event.value)
            UltrasoundDraftField.IMAGE_URIS -> draft.values.copy(
                imageUris = if (event.value.isBlank()) emptyList() else event.value.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            )
        }
        return state.copy(ultrasoundDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun updateLabDraft(state: AncState, event: AncEvent.LabDraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.labDraft ?: return state
        val values = when (event.field) {
            LabDraftField.DATE -> draft.values.copy(date = event.value)
            LabDraftField.CATEGORY -> draft.values.copy(testCategory = event.value)
            LabDraftField.NAME -> draft.values.copy(testName = event.value)
            LabDraftField.RESULT -> draft.values.copy(result = event.value)
            LabDraftField.UNIT -> draft.values.copy(unit = event.value)
            LabDraftField.REFERENCE_RANGE -> draft.values.copy(referenceRange = event.value)
            LabDraftField.NOTES -> draft.values.copy(notes = event.value)
            LabDraftField.LAB_NAME -> draft.values.copy(labName = event.value)
        }
        return state.copy(labDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun updateLabAbnormal(state: AncState, isAbnormal: Boolean): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.labDraft ?: return state
        return state.copy(
            labDraft = draft.copy(values = draft.values.copy(isAbnormal = isAbnormal), errors = emptySet()),
            savePhase = SavePhase.IDLE,
            saveError = null,
        )
    }

    private fun updateMedicationDraft(state: AncState, event: AncEvent.MedicationDraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.medicationDraft ?: return state
        val values = when (event.field) {
            MedicationDraftField.NAME -> draft.values.copy(name = event.value)
            MedicationDraftField.DOSAGE -> draft.values.copy(dosage = event.value)
            MedicationDraftField.ROUTE -> draft.values.copy(route = event.value)
            MedicationDraftField.FREQUENCY -> draft.values.copy(frequency = event.value)
            MedicationDraftField.START_DATE -> draft.values.copy(startDate = event.value)
            MedicationDraftField.END_DATE -> draft.values.copy(endDate = event.value)
            MedicationDraftField.STATUS -> draft.values.copy(
                status = event.value.ifBlank { MEDICATION_STATUS_ACTIVE },
                isNonCompliant = (event.value == MEDICATION_STATUS_NON_COMPLIANT),
            )
            MedicationDraftField.NOTES -> draft.values.copy(notes = event.value)
            MedicationDraftField.ADVERSE_NOTES -> draft.values.copy(adverseNotes = event.value)
        }
        return state.copy(medicationDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun validationFailed(state: AncState, event: AncEvent.PatientValidationFailed): AncState = when (event.editor) {
        PatientEditorKind.PROFILE -> state.copy(patientDraft = state.patientDraft?.copy(errors = event.errors))
        PatientEditorKind.HISTORIES -> state.copy(historiesDraft = state.historiesDraft?.copy(errors = event.errors))
        PatientEditorKind.PREGNANCY -> state.copy(pregnancyDraft = state.pregnancyDraft?.copy(errors = event.errors))
        PatientEditorKind.STATUS -> state.copy(statusDraft = state.statusDraft?.copy(errors = event.errors))
    }.copy(savePhase = SavePhase.IDLE, saveError = null)

    private fun requestBack(state: AncState): AncState {
        if (state.savePhase.blocksEditing()) return state
        val destination = when (val route = state.route) {
            AncRoute.PatientList -> return state
            is AncRoute.Workspace -> AncRoute.PatientList
            is AncRoute.VisitEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.VISITS)
            is AncRoute.PatientEditor -> if (route.isNew) AncRoute.PatientList else AncRoute.Workspace(route.patientId)
            is AncRoute.OtherHistoriesEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.OTHER_HISTORIES)
            is AncRoute.PreviousPregnancyEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.OBSTETRIC_HISTORY)
            is AncRoute.StatusEditor -> AncRoute.PatientList
            is AncRoute.DatingEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.DATING)
            is AncRoute.UltrasoundEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.ULTRASOUND)
            is AncRoute.LabEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.LABORATORY)
            is AncRoute.LabBatchEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.LABORATORY)
            is AncRoute.MedicationEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.MEDICATIONS)
        }
        return if (state.hasDirtyDraft) {
            state.copy(showDiscardDialog = true, pendingAction = PendingAction.Navigate(destination))
        } else {
            state.clearDrafts().copy(route = destination, savedNotice = false, notice = null)
        }
    }

    private fun confirmDiscard(state: AncState): AncState {
        if (state.savePhase.blocksEditing()) return state
        val action = state.pendingAction
        val discarded = state.clearDrafts().copy(savedNotice = false, notice = null)
        return when (action) {
            is PendingAction.Navigate -> discarded.copy(route = action.destination)
            is PendingAction.StartVisit -> startVisit(
                discarded,
                AncEvent.StartVisit(action.patientId, action.visitId, action.editorSessionId),
            )
            null -> discarded
        }
    }

    private fun AncState.clearDrafts(): AncState = copy(
        draft = null,
        patientDraft = null,
        historiesDraft = null,
        pregnancyDraft = null,
        statusDraft = null,
        datingDraft = null,
        ultrasoundDraft = null,
        labDraft = null,
        medicationDraft = null,
        labBatchDraft = null,
        showDiscardDialog = false,
        pendingAction = null,
        savePhase = SavePhase.IDLE,
        saveError = null,
        pendingSave = null,
        draftRecoveryPrompt = null,
    )

    private fun DemoVisit.toDraftValues(): DraftValues {
        val parsedBp = parseBloodPressure(bloodPressure)
        val parsedFindings = parseVisitFindings(findings)
        return DraftValues(
            date = date,
            bloodPressure = bloodPressure.orEmpty(),
            weight = weight.orEmpty(),
            findings = findings.orEmpty(),
            notes = notes.orEmpty(),
            linkedUltrasoundId = linkedUltrasoundId.orEmpty(),
            systolic = parsedBp?.systolic?.toString().orEmpty(),
            diastolic = parsedBp?.diastolic?.toString().orEmpty(),
            fundalHeight = parsedFindings.fundalHeight.orEmpty(),
            fetalHeartRate = parsedFindings.fetalHeartRate.orEmpty(),
            oedema = parsedFindings.oedema.orEmpty(),
            fetalMovements = parsedFindings.fetalMovements.orEmpty(),
            urineProtein = parsedFindings.urineProtein.orEmpty(),
        )
    }

    private fun DemoPatient.toPatientDraftValues() = PatientDraftValues(
        caseId = caseId,
        displayName = displayName,
        dateOfBirth = dateOfBirth.orEmpty(),
        phone = phone.orEmpty(),
        ageText = ageText.orEmpty(),
        addressText = addressText.orEmpty(),
        bloodGroupText = bloodGroupText.orEmpty(),
        riskLevel = riskLevel.orEmpty(),
        gravida = gravida?.toString().orEmpty(),
        parity = parity?.toString().orEmpty(),
        abortion = abortion?.toString().orEmpty(),
        obstetricNotes = obstetricNotes.orEmpty(),
        clinicalAttention = clinicalAttention.orEmpty(),
        lmpDate = dating?.lmpDate.orEmpty(),
    )

    private fun DemoPreviousPregnancy.toDraftValues() = PreviousPregnancyDraftValues(
        dateText = dateText.orEmpty(),
        outcomeText = outcomeText.orEmpty(),
        pregnancyType = pregnancyType.orEmpty(),
        otherDetails = otherDetails.orEmpty(),
        placeAndDoctor = placeAndDoctor.orEmpty(),
        deliveryBy = deliveryBy.orEmpty(),
        outcome = outcome.orEmpty(),
        birthMethod = birthMethod.orEmpty(),
        lossManagement = lossManagement.orEmpty(),
        terminationMethod = terminationMethod.orEmpty(),
        clinicianId = clinicianId,
    )

    private fun DemoPatientDating.toDraftValues() = DatingDraftValues(
        lmpDate = lmpDate.orEmpty(),
        ultrasoundEdd = ultrasoundEdd.orEmpty(),
        agreedEdd = agreedEdd.orEmpty(),
        datingMethod = datingMethod.orEmpty(),
        transferDate = transferDate.orEmpty(),
        embryoAgeDays = embryoAgeDays?.toString().orEmpty(),
        notes = notes.orEmpty(),
    )

    private fun DemoUltrasoundScan.toDraftValues() = UltrasoundDraftValues(
        date = date,
        gaWeeks = gestationalAgeWeeks?.toString().orEmpty(),
        gaDays = gestationalAgeDays?.toString().orEmpty(),
        fetalNumber = fetalNumber.orEmpty(),
        presentation = presentation.orEmpty(),
        placentalSite = placentalSite.orEmpty(),
        amnioticFluid = amnioticFluid.orEmpty(),
        fetalHeartRate = fetalHeartRate.orEmpty(),
        biometryBpd = biometryBpd.orEmpty(),
        biometryHc = biometryHc.orEmpty(),
        biometryAc = biometryAc.orEmpty(),
        biometryFl = biometryFl.orEmpty(),
        estimatedFetalWeight = estimatedFetalWeight.orEmpty(),
        impression = impression.orEmpty(),
        linkedVisitId = linkedVisitId.orEmpty(),
        imageUris = imageUris,
    )

    private fun DemoLabInvestigation.toDraftValues() = LabDraftValues(
        date = date,
        testCategory = testCategory,
        testName = testName,
        result = result,
        unit = unit.orEmpty(),
        referenceRange = referenceRange.orEmpty(),
        isAbnormal = isAbnormal,
        notes = notes.orEmpty(),
        labName = labName.orEmpty(),
    )

    private fun DemoMedicationRecord.toDraftValues(): MedicationDraftValues {
        val effStatus = effectiveMedicationStatus(status, isNonCompliant)
        return MedicationDraftValues(
            name = name,
            dosage = dosage,
            route = route,
            frequency = frequency,
            startDate = startDate,
            endDate = endDate.orEmpty(),
            status = effStatus.ifBlank { MEDICATION_STATUS_ACTIVE },
            isProphylaxis = isProphylaxis,
            notes = notes.orEmpty(),
            isNonCompliant = (effStatus == MEDICATION_STATUS_NON_COMPLIANT),
            adverseNotes = adverseNotes.orEmpty(),
        )
    }

    private fun AncState.canAccept(token: PendingSaveToken): Boolean =
        (savePhase == SavePhase.IDLE || savePhase == SavePhase.ERROR) && activeEditorSessionId(this) == token.editorSessionId
}
