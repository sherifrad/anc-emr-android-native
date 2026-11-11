package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.PatientValidationError
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
    OUTCOME_TEXT,
    PREGNANCY_TYPE,
    OTHER_DETAILS,
    PLACE_AND_DOCTOR,
    DELIVERY_BY,
    OUTCOME,
    BIRTH_METHOD,
    LOSS_MANAGEMENT,
    TERMINATION_METHOD,
    CLINICIAN_ID,
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

/**
 * The medication draft the open workspace shows in its sheet, or null. While it is shown the sheet also hosts the
 * app's Undo bar, so the bar is never hidden under the modal sheet.
 */
fun AncState.medicationSheetDraft(): MedicationDraft? =
    medicationDraft?.takeIf { it.patientId == (route as? AncRoute.Workspace)?.patientId }

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
    copy(
        deleteConfirmation = null,
        undoOffer = null,
        deleteRunning = false,
        deleteMessage = null,
        noticePatientId = null,
        medicationStopUndo = null,
        medicationQuickSave = null,
    )

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
    /**
     * Set by the first field change. Only the sheet's discard prompt reads it: it is not part of [isDirty] and is
     * not saved with the draft, so crash recovery still goes by [isDirty].
     */
    val touched: Boolean = false,
) {
    val isDirty: Boolean get() = values != original
}

/** The route a new medication draft starts with, and the one shown when a row has none. */
val DEFAULT_MEDICATION_ROUTE: String = ClinicalVocabulary.medicationRoutes.first()

/** What a catalog tile puts into a new medication draft, on top of the blank new-drug defaults. */
data class MedicationPrefill(
    val name: String,
    val dosage: String,
    val route: String,
    val frequency: String,
)

/** How long the Meds tab offers Undo after a one-tap stop. */
const val MEDICATION_STOP_UNDO_MILLIS = 10_000L

/**
 * The Meds-tab Undo bar after a one-tap stop. Undo saves the same row again with these earlier values and the row's
 * revision at that moment. [token] is the stop save's editor session id. [committedAtMillis] is when the stop
 * committed, so the bar counts its 10 s from then however often it is recomposed, rotated or re-shown.
 */
data class MedicationStopUndo(
    val token: String,
    val patientId: String,
    val medicationId: String,
    val medicationName: String,
    val previousStatus: String,
    val previousEndDate: String,
    val previousNonCompliant: Boolean,
    val committedAtMillis: Long = 0L,
)

/** How much of the Undo window is left at [nowMillis] (same clock as [MedicationStopUndo.committedAtMillis]); 0 when it is over. */
fun MedicationStopUndo.remainingMillis(nowMillis: Long, windowMillis: Long = MEDICATION_STOP_UNDO_MILLIS): Long =
    (windowMillis - (nowMillis - committedAtMillis)).coerceIn(0L, windowMillis)

/**
 * A one-tap stop, or its Undo, while it is being saved. [draft] is the stored row with the new values; it is never
 * shown, autosaved or recovered, and it goes through the normal medication save command. [undoOnSuccess] is the bar
 * to show when a stop commits; it is null for the save that is itself the Undo.
 */
data class MedicationQuickSave(
    val draft: MedicationDraft,
    val undoOnSuccess: MedicationStopUndo?,
)

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
    /** A one-tap stop or its Undo in flight; it stays after a failed save so the Meds tab can show the cause. */
    val medicationQuickSave: MedicationQuickSave? = null,
    val medicationStopUndo: MedicationStopUndo? = null,
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
