package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.storage.ClinicalRecordKind
import com.sherifradwan.ancnative.storage.VaultState

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
    data class Profile(val patientId: String) : AncRoute
    data class VisitEditor(val patientId: String) : AncRoute
    data class PatientEditor(val patientId: String, val isNew: Boolean) : AncRoute
    data class OtherHistoriesEditor(val patientId: String) : AncRoute
    data class PreviousPregnancyEditor(val patientId: String, val pregnancyId: String) : AncRoute
    data class StatusEditor(val patientId: String, val targetStatus: String) : AncRoute
    data class DatingEditor(val patientId: String) : AncRoute
    data class UltrasoundEditor(val patientId: String, val scanId: String, val isNew: Boolean) : AncRoute
    data class LabEditor(val patientId: String, val labId: String, val isNew: Boolean) : AncRoute
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

enum class DraftField { DATE, BLOOD_PRESSURE, WEIGHT, FINDINGS, NOTES, LINKED_ULTRASOUND }
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
    IS_NON_COMPLIANT,
    ADVERSE_NOTES,
}

enum class DraftError { DATE_REQUIRED, INVALID_DATE, STALE_REVISION, SAVE_FAILED }
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
enum class SavedNotice { VISIT, PATIENT, HISTORIES, PREGNANCY, ARCHIVED, UNARCHIVED, DATING, ULTRASOUND, LABORATORY, MEDICATIONS }
enum class PatientEditorKind { PROFILE, HISTORIES, PREGNANCY, STATUS }

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
    val showDiscardDialog: Boolean = false,
    val pendingAction: PendingAction? = null,
    val showNewPatientInfo: Boolean = false,
    val savedNotice: Boolean = false,
    val notice: SavedNotice? = null,
    val savePhase: SavePhase = SavePhase.IDLE,
    val saveError: SaveUiError? = null,
    val pendingSave: PendingSaveToken? = null,
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
            medicationDraft?.isDirty == true
}

sealed interface AncEvent {
    data class SearchChanged(val query: String) : AncEvent
    data object ClearSearch : AncEvent
    data object ShowNewPatientInfo : AncEvent
    data object HideNewPatientInfo : AncEvent
    data class OpenWorkspace(val patientId: String) : AncEvent
    data class OpenProfile(val patientId: String) : AncEvent
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
    ) : AncEvent
    data class DatingDraftChanged(val field: DatingDraftField, val value: String) : AncEvent
    data class UltrasoundDraftChanged(val field: UltrasoundDraftField, val value: String) : AncEvent
    data class LabDraftChanged(val field: LabDraftField, val value: String) : AncEvent
    data class LabAbnormalChanged(val isAbnormal: Boolean) : AncEvent
    data class MedicationDraftChanged(val field: MedicationDraftField, val value: String) : AncEvent
    data class MedicationProphylaxisChanged(val isProphylaxis: Boolean) : AncEvent
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
    ) : AncEvent
    data class DeleteAttachment(
        val patientId: String,
        val attachmentId: String,
        val fileName: String,
    ) : AncEvent
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
}

object AncReducer {
    fun reduce(state: AncState, event: AncEvent): AncState = when (event) {
        is AncEvent.SearchChanged -> state.copy(searchQuery = event.query)
        AncEvent.ClearSearch -> state.copy(searchQuery = "")
        AncEvent.ShowNewPatientInfo -> state.copy(showNewPatientInfo = true)
        AncEvent.HideNewPatientInfo -> state.copy(showNewPatientInfo = false)
        is AncEvent.OpenWorkspace -> navigate(state, AncRoute.Workspace(event.patientId))
        is AncEvent.OpenProfile -> navigate(state, AncRoute.Profile(event.patientId))
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
        is AncEvent.MedicationProphylaxisChanged -> updateMedicationProphylaxis(state, event.isProphylaxis)
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
        AncEvent.SubmitMedication,
        AncEvent.SubmitMedicationAndAddAnother,
        -> state
        is AncEvent.SaveValidationFailed -> state.copy(
            draft = state.draft?.copy(errors = event.errors),
            savePhase = SavePhase.IDLE,
            saveError = null,
        )
        is AncEvent.ClinicalValidationFailed -> when (event.recordKind) {
            ClinicalRecordKind.DATING -> state.copy(datingDraft = state.datingDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
            ClinicalRecordKind.ULTRASOUND_CREATE, ClinicalRecordKind.ULTRASOUND_UPDATE -> state.copy(ultrasoundDraft = state.ultrasoundDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
            ClinicalRecordKind.LAB_CREATE, ClinicalRecordKind.LAB_UPDATE -> state.copy(labDraft = state.labDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
            ClinicalRecordKind.MEDICATION_CREATE, ClinicalRecordKind.MEDICATION_UPDATE -> state.copy(medicationDraft = state.medicationDraft?.copy(errors = event.errors), savePhase = SavePhase.IDLE, saveError = null)
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
        is AncEvent.AddAttachment, is AncEvent.DeleteAttachment -> state
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
                baseRevision = visit?.revision,
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
        val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
        val med = event.medicationId?.let { id -> patient.medications.firstOrNull { it.id == id } }
        val defaultStart = event.defaultStartDate.ifEmpty { java.time.LocalDate.now().toString() }
        val values = med?.toDraftValues() ?: MedicationDraftValues(
            route = "Oral",
            status = "Active",
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
            savedNotice = false,
            notice = null,
        )
    }

    private fun updateDraft(state: AncState, event: AncEvent.DraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.draft ?: return state
        val values = when (event.field) {
            DraftField.DATE -> draft.values.copy(date = event.value)
            DraftField.BLOOD_PRESSURE -> draft.values.copy(bloodPressure = event.value)
            DraftField.WEIGHT -> draft.values.copy(weight = event.value)
            DraftField.FINDINGS -> draft.values.copy(findings = event.value)
            DraftField.NOTES -> draft.values.copy(notes = event.value)
            DraftField.LINKED_ULTRASOUND -> draft.values.copy(linkedUltrasoundId = event.value)
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
            PregnancyDraftField.OUTCOME_TEXT -> draft.values.copy(outcomeText = event.value)
            PregnancyDraftField.PREGNANCY_TYPE -> draft.values.copy(pregnancyType = event.value)
            PregnancyDraftField.OTHER_DETAILS -> draft.values.copy(otherDetails = event.value)
            PregnancyDraftField.PLACE_AND_DOCTOR -> draft.values.copy(placeAndDoctor = event.value)
            PregnancyDraftField.DELIVERY_BY -> draft.values.copy(deliveryBy = event.value)
            PregnancyDraftField.OUTCOME -> draft.values.copy(
                outcome = event.value,
                birthMethod = draft.values.birthMethod.takeIf {
                    event.value == "LiveBirth" || event.value == "Stillbirth"
                }.orEmpty(),
                lossManagement = draft.values.lossManagement.takeIf {
                    event.value == "EarlyPregnancyLoss"
                }.orEmpty(),
                terminationMethod = draft.values.terminationMethod.takeIf {
                    event.value == "TerminationOfPregnancy"
                }.orEmpty(),
            )
            PregnancyDraftField.BIRTH_METHOD -> draft.values.copy(birthMethod = event.value)
            PregnancyDraftField.LOSS_MANAGEMENT -> draft.values.copy(lossManagement = event.value)
            PregnancyDraftField.TERMINATION_METHOD -> draft.values.copy(terminationMethod = event.value)
            PregnancyDraftField.CLINICIAN_ID -> draft.values.copy(clinicianId = event.value)
        }
        return state.copy(pregnancyDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun updateDatingDraft(state: AncState, event: AncEvent.DatingDraftChanged): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.datingDraft ?: return state
        val values = when (event.field) {
            DatingDraftField.LMP_DATE -> draft.values.copy(lmpDate = event.value)
            DatingDraftField.ULTRASOUND_EDD -> draft.values.copy(ultrasoundEdd = event.value)
            DatingDraftField.AGREED_EDD -> draft.values.copy(agreedEdd = event.value)
            DatingDraftField.DATING_METHOD -> draft.values.copy(datingMethod = event.value)
            DatingDraftField.TRANSFER_DATE -> draft.values.copy(transferDate = event.value)
            DatingDraftField.EMBRYO_AGE_DAYS -> draft.values.copy(embryoAgeDays = event.value)
            DatingDraftField.NOTES -> draft.values.copy(notes = event.value)
        }
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
            MedicationDraftField.STATUS -> draft.values.copy(status = event.value)
            MedicationDraftField.NOTES -> draft.values.copy(notes = event.value)
            MedicationDraftField.IS_NON_COMPLIANT -> draft.values.copy(isNonCompliant = event.value.toBooleanStrictOrNull() ?: false)
            MedicationDraftField.ADVERSE_NOTES -> draft.values.copy(adverseNotes = event.value)
        }
        return state.copy(medicationDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
    }

    private fun updateMedicationProphylaxis(state: AncState, isProphylaxis: Boolean): AncState {
        if (state.savePhase.blocksEditing()) return state
        val draft = state.medicationDraft ?: return state
        return state.copy(
            medicationDraft = draft.copy(values = draft.values.copy(isProphylaxis = isProphylaxis), errors = emptySet()),
            savePhase = SavePhase.IDLE,
            saveError = null,
        )
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
            is AncRoute.Workspace -> AncRoute.Profile(route.patientId)
            is AncRoute.Profile -> AncRoute.PatientList
            is AncRoute.VisitEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.VISITS)
            is AncRoute.PatientEditor -> if (route.isNew) AncRoute.PatientList else AncRoute.Profile(route.patientId)
            is AncRoute.OtherHistoriesEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.OTHER_HISTORIES)
            is AncRoute.PreviousPregnancyEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.OBSTETRIC_HISTORY)
            is AncRoute.StatusEditor -> AncRoute.PatientList
            is AncRoute.DatingEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.DATING)
            is AncRoute.UltrasoundEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.ULTRASOUND)
            is AncRoute.LabEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.LABORATORY)
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
        showDiscardDialog = false,
        pendingAction = null,
        savePhase = SavePhase.IDLE,
        saveError = null,
        pendingSave = null,
    )

    private fun DemoVisit.toDraftValues() = DraftValues(
        date = date,
        bloodPressure = bloodPressure.orEmpty(),
        weight = weight.orEmpty(),
        findings = findings.orEmpty(),
        notes = notes.orEmpty(),
        linkedUltrasoundId = linkedUltrasoundId.orEmpty(),
    )

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

    private fun DemoMedicationRecord.toDraftValues() = MedicationDraftValues(
        name = name,
        dosage = dosage,
        route = route,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate.orEmpty(),
        status = status,
        isProphylaxis = isProphylaxis,
        notes = notes.orEmpty(),
        isNonCompliant = isNonCompliant,
        adverseNotes = adverseNotes.orEmpty(),
    )

    private fun AncState.canAccept(token: PendingSaveToken): Boolean =
        (savePhase == SavePhase.IDLE || savePhase == SavePhase.ERROR) && activeEditorSessionId() == token.editorSessionId

    private fun AncState.activeEditorSessionId(): String? =
        draft?.editorSessionId ?: patientDraft?.editorSessionId ?: historiesDraft?.editorSessionId
            ?: pregnancyDraft?.editorSessionId ?: statusDraft?.editorSessionId
            ?: datingDraft?.editorSessionId ?: ultrasoundDraft?.editorSessionId
            ?: labDraft?.editorSessionId ?: medicationDraft?.editorSessionId

    private fun SavePhase.blocksEditing(): Boolean =
        this == SavePhase.PENDING || this == SavePhase.RECONCILING || this == SavePhase.UNRESOLVED
}
