package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.DeletableType

interface EditorHook {
    fun isOpenFor(state: AncState, recordId: String): Boolean
    fun closeEditor(state: AncState, patientId: String): AncState
    fun rebaseCreateDraft(state: AncState, patients: List<DemoPatient>): AncState
}

object MedicationEditorHook : EditorHook {
    override fun isOpenFor(state: AncState, recordId: String): Boolean =
        state.medicationDraft?.medicationId == recordId

    override fun closeEditor(state: AncState, patientId: String): AncState =
        state.copy(
            medicationDraft = null,
            route = if (state.route is AncRoute.MedicationEditor) {
                AncRoute.Workspace(patientId, WorkspaceSection.MEDICATIONS)
            } else {
                state.route
            },
        )

    override fun rebaseCreateDraft(state: AncState, patients: List<DemoPatient>): AncState {
        var nextDraft = state.medicationDraft
        if (nextDraft != null && nextDraft.baseMedicationRevision == null) {
            val p = patients.firstOrNull { it.id == nextDraft.patientId }
            if (p != null) {
                nextDraft = nextDraft.copy(basePatientRevision = p.revision)
            }
        }
        return state.copy(medicationDraft = nextDraft)
    }
}

object LabEditorHook : EditorHook {
    override fun isOpenFor(state: AncState, recordId: String): Boolean =
        state.labDraft?.labId == recordId

    override fun closeEditor(state: AncState, patientId: String): AncState =
        state.copy(
            labDraft = null,
            route = if (state.route is AncRoute.LabEditor) {
                AncRoute.Workspace(patientId, WorkspaceSection.LABORATORY)
            } else {
                state.route
            },
        )

    override fun rebaseCreateDraft(state: AncState, patients: List<DemoPatient>): AncState {
        var nextDraft = state.labDraft
        if (nextDraft != null && nextDraft.baseLabRevision == null) {
            val p = patients.firstOrNull { it.id == nextDraft.patientId }
            if (p != null) {
                nextDraft = nextDraft.copy(basePatientRevision = p.revision)
            }
        }
        return state.copy(labDraft = nextDraft)
    }
}

object UltrasoundEditorHook : EditorHook {
    override fun isOpenFor(state: AncState, recordId: String): Boolean =
        state.ultrasoundDraft?.scanId == recordId

    override fun closeEditor(state: AncState, patientId: String): AncState =
        state.copy(
            ultrasoundDraft = null,
            route = if (state.route is AncRoute.UltrasoundEditor) {
                AncRoute.Workspace(patientId, WorkspaceSection.ULTRASOUND)
            } else {
                state.route
            },
        )

    override fun rebaseCreateDraft(state: AncState, patients: List<DemoPatient>): AncState {
        var nextDraft = state.ultrasoundDraft
        if (nextDraft != null && nextDraft.baseScanRevision == null) {
            val p = patients.firstOrNull { it.id == nextDraft.patientId }
            if (p != null) {
                nextDraft = nextDraft.copy(basePatientRevision = p.revision)
            }
        }
        return state.copy(ultrasoundDraft = nextDraft)
    }
}

object VisitEditorHook : EditorHook {
    override fun isOpenFor(state: AncState, recordId: String): Boolean =
        state.draft?.visitId == recordId

    override fun closeEditor(state: AncState, patientId: String): AncState =
        state.copy(
            draft = null,
            route = if (state.route is AncRoute.VisitEditor) {
                AncRoute.Workspace(patientId, WorkspaceSection.VISITS)
            } else {
                state.route
            },
        )

    override fun rebaseCreateDraft(state: AncState, patients: List<DemoPatient>): AncState = state
}

object EditorHooks {
    private val hooks: Map<DeletableType, EditorHook> = mapOf(
        DeletableType.MEDICATION to MedicationEditorHook,
        DeletableType.LAB to LabEditorHook,
        DeletableType.ULTRASOUND to UltrasoundEditorHook,
        DeletableType.VISIT to VisitEditorHook,
    )

    fun of(type: DeletableType): EditorHook? = hooks[type]

    fun all(): Collection<EditorHook> = hooks.values
}
