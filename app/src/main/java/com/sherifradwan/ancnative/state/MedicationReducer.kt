package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_DISCONTINUED
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_NON_COMPLIANT
import com.sherifradwan.ancnative.model.MedicationOnOff
import com.sherifradwan.ancnative.model.effectiveMedicationStatus
import com.sherifradwan.ancnative.model.isOngoingMedication
import com.sherifradwan.ancnative.model.medicationEditorChoice

/**
 * Opens the medication sheet in one step; the navigation route does not change. A start is ignored while a save
 * runs, while any medication draft is open, or while another draft has unsaved changes.
 *
 * - Edit (a stored [AncEvent.StartMedicationEditor.medicationId]): `original` and `values` are both the stored row.
 * - Add from a catalog tile ([AncEvent.StartMedicationEditor.prefill]): `original` is the blank new-drug defaults
 *   and `values` is the tile on top of them, so the draft is dirty (and recoverable) from the start.
 * - Blank custom drug: both are the blank defaults.
 *
 * A new draft started by "Save and add another" ([AncEvent.StartMedicationEditor.keepSavedNotice]) is marked
 * [MedicationDraft.chainedAdd].
 */
internal fun startMedicationEditor(state: AncState, event: AncEvent.StartMedicationEditor): AncState {
    if (state.savePhase.blocksEditing() || state.hasDirtyDraft || state.medicationDraft != null) return state
    if (event.medicationId != null && state.undoRunningFor(event.medicationId)) return state
    val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
    val med = event.medicationId?.let { id -> patient.medications.firstOrNull { it.id == id } }
    if (event.medicationId != null && med == null) return state
    val blank = MedicationDraftValues(
        route = DEFAULT_MEDICATION_ROUTE,
        status = MEDICATION_STATUS_ACTIVE,
        startDate = event.defaultStartDate.ifEmpty { today() },
    )
    val original = med?.toDraftValues() ?: blank
    val values = when {
        med != null -> original
        event.prefill != null -> blank.copy(
            name = event.prefill.name,
            dosage = event.prefill.dosage,
            route = event.prefill.route.ifBlank { blank.route },
            frequency = event.prefill.frequency,
        )
        else -> blank
    }
    return state.clearDrafts().copy(
        medicationDraft = MedicationDraft(
            editorSessionId = event.editorSessionId,
            patientId = patient.id,
            medicationId = med?.id ?: event.newMedicationId,
            basePatientRevision = patient.revision,
            baseMedicationRevision = med?.revision,
            original = original,
            values = values,
            chainedAdd = event.keepSavedNotice && med == null,
        ),
        savedNotice = if (event.keepSavedNotice) state.savedNotice else false,
        notice = if (event.keepSavedNotice) state.notice else null,
    )
}

/** True when a one-tap stop or its Undo may start a save now: nothing else is open, saving or being undone. */
internal fun AncState.canQuickSaveMedication(): Boolean =
    !savePhase.blocksEditing() && !hasDirtyDraft && medicationDraft == null && !undoRunning && !deleteRunning

/** One-tap stop: no draft and no sheet. The stored row is saved as Discontinued, ending today, at its current revision. */
internal fun stopMedication(state: AncState, event: AncEvent.StopMedication): AncState {
    if (!state.canQuickSaveMedication()) return state
    val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
    val med = patient.medications.firstOrNull { it.id == event.medicationId } ?: return state
    if (!isOngoingMedication(med.status, med.isNonCompliant)) return state
    val stored = med.toDraftValues()
    val stopped = stored.copy(status = MEDICATION_STATUS_DISCONTINUED, isNonCompliant = false, endDate = today())
    return state.copy(
        medicationQuickSave = MedicationQuickSave(
            draft = quickSaveDraft(event.editorSessionId, patient, med, stored, stopped),
            undoOnSuccess = MedicationStopUndo(
                token = event.editorSessionId,
                patientId = patient.id,
                medicationId = med.id,
                medicationName = med.name,
                previousStatus = stored.status,
                previousEndDate = stored.endDate,
                previousNonCompliant = stored.isNonCompliant,
            ),
        ),
        medicationStopUndo = null,
        savePhase = SavePhase.IDLE,
        saveError = null,
        savedNotice = false,
        notice = null,
    )
}

/** Undo of a one-tap stop: a second normal save of the same row, at the revision the stop produced. */
internal fun undoMedicationStop(state: AncState, event: AncEvent.UndoMedicationStop): AncState {
    val undo = state.medicationStopUndo?.takeIf { it.token == event.token } ?: return state
    if (!state.canQuickSaveMedication()) return state
    val patient = state.patient(undo.patientId)?.takeIf { it.status != "archived" }
    val med = patient?.medications?.firstOrNull { it.id == undo.medicationId }
    if (patient == null || med == null) return state.copy(medicationStopUndo = null)
    val stored = med.toDraftValues()
    val restored = stored.copy(
        status = undo.previousStatus,
        endDate = undo.previousEndDate,
        isNonCompliant = undo.previousNonCompliant,
    )
    return state.copy(
        medicationQuickSave = MedicationQuickSave(
            draft = quickSaveDraft(event.editorSessionId, patient, med, stored, restored),
            undoOnSuccess = null,
        ),
        medicationStopUndo = null,
        savePhase = SavePhase.IDLE,
        saveError = null,
        savedNotice = false,
        notice = null,
    )
}

internal fun quickSaveDraft(
    editorSessionId: String,
    patient: DemoPatient,
    med: DemoMedicationRecord,
    original: MedicationDraftValues,
    values: MedicationDraftValues,
) = MedicationDraft(
    editorSessionId = editorSessionId,
    patientId = patient.id,
    medicationId = med.id,
    basePatientRevision = patient.revision,
    baseMedicationRevision = med.revision,
    original = original,
    values = values,
)

/**
 * The one-tap stop's Undo bar and its failure message belong to the Meds tab of one patient: both go when another
 * action starts or the open patient changes.
 */
internal fun AncState.withMedicationStopUiKept(event: AncEvent): AncState {
    var next = this
    val undo = next.medicationStopUndo
    if (undo != null && (event.startsAnotherAction() || route.patientIdOrNull() != undo.patientId)) {
        next = next.copy(medicationStopUndo = null)
    }
    val failed = next.medicationQuickSave
    // A tab change alone keeps the cause: the doctor has not acted on it yet and finds it again on the Meds tab.
    val actedElsewhere = event.startsAnotherAction() && event !is AncEvent.SelectSection
    if (failed != null && (actedElsewhere || route.patientIdOrNull() != failed.draft.patientId)) {
        next = next.withoutFailedMedicationQuickSave()
    }
    return next
}

/** Drops a one-tap stop (or Undo) whose save failed, with its message. One that is still saving, or unresolved, stays. */
internal fun AncState.withoutFailedMedicationQuickSave(): AncState =
    if (medicationQuickSave != null && medicationDraft == null && savePhase == SavePhase.ERROR) {
        copy(medicationQuickSave = null, savePhase = SavePhase.IDLE, saveError = null)
    } else {
        this
    }

internal fun updateMedicationDraft(state: AncState, event: AncEvent.MedicationDraftChanged): AncState {
    if (state.savePhase.blocksEditing()) return state
    val draft = state.medicationDraft ?: return state
    val values = when (event.field) {
        MedicationDraftField.NAME -> draft.values.copy(name = event.value)
        MedicationDraftField.DOSAGE -> draft.values.copy(dosage = event.value)
        MedicationDraftField.ROUTE -> draft.values.copy(route = event.value)
        MedicationDraftField.FREQUENCY -> draft.values.copy(frequency = event.value)
        MedicationDraftField.START_DATE -> draft.values.copy(startDate = event.value)
        MedicationDraftField.END_DATE -> draft.values.copy(endDate = event.value)
        MedicationDraftField.STATUS -> withMedicationStatus(draft.original, draft.values, event.value.ifBlank { MEDICATION_STATUS_ACTIVE })
        MedicationDraftField.NOTES -> draft.values.copy(notes = event.value)
        MedicationDraftField.ADVERSE_NOTES -> draft.values.copy(adverseNotes = event.value)
    }
    return state.copy(
        medicationDraft = draft.copy(values = values, errors = emptySet(), touched = true),
        savePhase = SavePhase.IDLE,
        saveError = null,
    )
}

/**
 * A change to Stopped fills a blank end date, so the one date field is never empty: with the stored stop date when
 * the row was stored as stopped (so Active and back to Stopped loses nothing), otherwise with today. A change from
 * Stopped back to Active keeps the start date and clears the end date, as an active drug has none.
 */
internal fun withMedicationStatus(
    original: MedicationDraftValues,
    values: MedicationDraftValues,
    status: String,
): MedicationDraftValues {
    val next = values.copy(status = status, isNonCompliant = status == MEDICATION_STATUS_NON_COMPLIANT)
    val wasStopped = medicationEditorChoice(values.status) == MedicationOnOff.STOPPED
    val storedStopDate = original.endDate.takeIf { medicationEditorChoice(original.status) == MedicationOnOff.STOPPED }
    return when (medicationEditorChoice(status)) {
        MedicationOnOff.STOPPED ->
            if (next.endDate.isBlank()) next.copy(endDate = storedStopDate?.ifBlank { null } ?: today()) else next
        MedicationOnOff.ON -> if (wasStopped) next.copy(endDate = "") else next
        null -> next
    }
}

internal fun DemoMedicationRecord.toDraftValues(): MedicationDraftValues {
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
