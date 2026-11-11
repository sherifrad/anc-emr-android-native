package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.storage.ClinicalRecordKind
import com.sherifradwan.ancnative.storage.DeletableType

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
    data class StartDeliveryPlanEditor(val patientId: String, val editorSessionId: String) : AncEvent
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
    data class DeliveryPlanFieldChanged(val field: DeliveryPlanField, val value: String) : AncEvent
    data class DeliveryOutcomeToggled(val delivered: Boolean) : AncEvent
    data class DeliveryOutcomeFieldChanged(val field: DeliveryOutcomeField, val value: String) : AncEvent
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
        /** Catalog tile values for a new draft; ignored when [medicationId] names a stored row. */
        val prefill: MedicationPrefill? = null,
    ) : AncEvent
    /** One-tap stop: saves the stored row as Discontinued with today's end date, without a draft or sheet. */
    data class StopMedication(
        val patientId: String,
        val medicationId: String,
        val editorSessionId: String,
    ) : AncEvent
    /** Undo of a one-tap stop: saves the row again with its earlier status and end date. */
    data class UndoMedicationStop(val token: String, val editorSessionId: String) : AncEvent
    data class MedicationStopUndoExpired(val token: String) : AncEvent
    /** Closes the cause shown after a failed one-tap stop or Undo. */
    data object DismissMedicationStopFailure : AncEvent
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
        /**
         * When the store committed, on the monotonic clock the Meds tab's Undo bar counts from
         * (`SystemClock.elapsedRealtime()` in the app). Only a one-tap stop reads it.
         */
        val committedAtMillis: Long = 0L,
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
