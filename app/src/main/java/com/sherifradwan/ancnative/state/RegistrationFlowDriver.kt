package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.RegistrationStep.DATING
import com.sherifradwan.ancnative.state.RegistrationStep.HISTORIES
import com.sherifradwan.ancnative.state.RegistrationStep.IDENTITY
import com.sherifradwan.ancnative.state.RegistrationStep.OBSTETRIC
import com.sherifradwan.ancnative.state.RegistrationStep.PREGNANCIES

/**
 * Wires the pure registration flow ([RegistrationFlow.kt]) into [AncState]. Every flow move is one atomic transition:
 * the flow, the route and the editor draft change together or not at all. Ids come from the caller (the ViewModel),
 * so the reducer stays pure.
 */
data class FlowIds(val editorSessionId: String, val newPregnancyId: String)

/** Next saved the editor of [fromSessionId]; when that exact save commits, the flow moves on with [nextIds]. */
data class AwaitingAdvance(val fromSessionId: String, val nextIds: FlowIds)

/** The start event of the editor a flow step shows, or null for the pregnancy step while its count prompt is due. */
internal fun editorEventFor(flow: RegistrationFlow, patient: DemoPatient, ids: FlowIds): AncEvent? =
    when (flow.current) {
        IDENTITY, OBSTETRIC -> AncEvent.StartPatientEditor(patient.id, patient.id, ids.editorSessionId)
        DATING -> AncEvent.StartDatingEditor(patient.id, ids.editorSessionId)
        HISTORIES -> AncEvent.StartHistoriesEditor(patient.id, ids.editorSessionId)
        PREGNANCIES ->
            if (flow.pregnancyTarget == null) null
            else AncEvent.StartPregnancyEditor(patient.id, null, ids.newPregnancyId, ids.editorSessionId)
    }

/** True when [this] is the screen of [step] for [patientId] (both pregnancy screens belong to the pregnancy step). */
internal fun AncRoute.matchesStep(step: RegistrationStep, patientId: String): Boolean = when (step) {
    IDENTITY, OBSTETRIC -> this is AncRoute.PatientEditor && this.patientId == patientId
    DATING -> this is AncRoute.DatingEditor && this.patientId == patientId
    HISTORIES -> this is AncRoute.OtherHistoriesEditor && this.patientId == patientId
    PREGNANCIES ->
        (this is AncRoute.PreviousPregnancyEditor && this.patientId == patientId) ||
            (this is AncRoute.PregnancyCountPrompt && this.patientId == patientId)
}

/** True in the flow's Identity step, where age is required for every patient. */
fun AncState.forcesAgeRequired(): Boolean = registrationFlow?.current == IDENTITY

/** The flow when the open screen is the one it shows, else null. Every flow-aware screen asks this one function. */
fun AncState.activeFlowForRoute(): RegistrationFlow? =
    registrationFlow?.takeIf {
        route.matchesStep(it.current, it.patientId) && activeEditorSessionId(this) == it.activeEditorSessionId
    }

enum class SystemBack { KEEP_EDITING, FLOW_BACK, BACK }

/** What the system Back gesture does. The discard dialog wins, then the flow's own Back, then the plain back. */
fun systemBackKind(state: AncState): SystemBack = when {
    state.showDiscardDialog -> SystemBack.KEEP_EDITING
    state.activeFlowForRoute() != null -> SystemBack.FLOW_BACK
    else -> SystemBack.BACK
}

/**
 * Opens the editor (or the pregnancy count prompt) of [flow]'s current step and commits [flow] with it. Returns null
 * when the existing start logic refuses (saving, dirty draft, patient missing or archived): the state is then unchanged.
 */
internal fun startFlowEditor(state: AncState, flow: RegistrationFlow, ids: FlowIds): AncState? {
    val patient = state.patient(flow.patientId) ?: return null
    val event = editorEventFor(flow, patient, ids)
    val started = if (event == null) openCountPrompt(state, patient) else startEditorFor(state, event)
    if (started === state) return null
    val session = if (event == null) null else ids.editorSessionId
    return started.copy(
        registrationFlow = flow.copy(activeEditorSessionId = session),
        awaitingAdvance = null,
        savePhase = SavePhase.IDLE,
        saveError = null,
    )
}

private fun startEditorFor(state: AncState, event: AncEvent): AncState = when (event) {
    is AncEvent.StartPatientEditor -> AncReducer.startPatientEditor(state, event)
    is AncEvent.StartDatingEditor -> AncReducer.startDatingEditor(state, event)
    is AncEvent.StartHistoriesEditor -> AncReducer.startHistoriesEditor(state, event)
    is AncEvent.StartPregnancyEditor -> AncReducer.startPregnancyEditor(state, event)
    else -> state
}

private fun AncState.withoutDrafts(): AncState = with(AncReducer) { clearDrafts() }

private fun openCountPrompt(state: AncState, patient: DemoPatient): AncState {
    if (state.savePhase.blocksEditing() || state.hasDirtyDraft || patient.status == "archived") return state
    return state.withoutDrafts().copy(
        route = AncRoute.PregnancyCountPrompt(patient.id),
        savedNotice = false,
        notice = null,
    )
}

/** The one way out of the flow: drafts cleared, flow cleared, back on the patient hub, no notice. */
private fun exitFlow(state: AncState, patientId: String): AncState = state.withoutDrafts().copy(
    registrationFlow = null,
    awaitingAdvance = null,
    route = AncRoute.Workspace(patientId),
    savedNotice = false,
    notice = null,
)

private fun isNoOp(flow: RegistrationFlow, event: FlowEvent): Boolean =
    (reduce(flow, event) as? FlowResult.Continue)?.flow == flow

/** Applies [event] to the flow and opens the next editor in the same transition; leaves for the hub on exit. */
internal fun applyFlowMove(state: AncState, event: FlowEvent, ids: FlowIds): AncState {
    val flow = state.registrationFlow ?: return state
    if (isNoOp(flow, event)) return state
    return when (val result = reduce(flow, event)) {
        FlowResult.Exit -> exitFlow(state, flow.patientId)
        is FlowResult.Continue -> startFlowEditor(state, result.flow, ids) ?: exitFlow(state, flow.patientId)
    }
}

/** A new patient's editor opens the flow at the Identity step; a flow of another patient is replaced. */
internal fun AncState.withNewPatientFlow(patientId: String, editorSessionId: String): AncState {
    if (registrationFlow?.patientId == patientId) return this
    val begun = (reduce(null, FlowEvent.Begin(patientId)) as FlowResult.Continue).flow
    return copy(registrationFlow = begun.copy(activeEditorSessionId = editorSessionId))
}

private fun beginFlow(state: AncState, begin: FlowEvent.Begin, ids: FlowIds): AncState {
    if (state.registrationFlow != null) return state
    val patient = state.patient(begin.patientId)?.takeIf { it.status != "archived" } ?: return state
    if (deriveStepStatuses(patient).values.all { it == StepStatus.DONE }) return state
    val result = reduce(null, FlowEvent.Begin(patient.id, begin.startStep, patient)) as? FlowResult.Continue
        ?: return state
    return startFlowEditor(state, result.flow, ids) ?: state
}

/** Begin, Back, Skip, Finish and PregnancyCountChosen. Next goes through [reduceRegistrationNext]. */
internal fun reduceFlowEvent(state: AncState, event: AncEvent.Flow): AncState {
    val ids = FlowIds(event.editorSessionId, event.newPregnancyId)
    val move = event.event
    if (move is FlowEvent.Begin) return beginFlow(state, move, ids)
    if (move is FlowEvent.Next || move == FlowEvent.PregnancyCommitted) return state
    val flow = state.registrationFlow ?: return state
    val counted = if (move is FlowEvent.PregnancyCountChosen) {
        move.copy(existingCount = state.patient(flow.patientId)?.previousPregnancies?.size ?: 0)
    } else {
        move
    }
    if (isNoOp(flow, counted)) return state
    if (state.savePhase.blocksEditing()) return state
    if (counted == FlowEvent.Back && reduce(flow, counted) == FlowResult.Exit) return AncReducer.requestBack(state)
    if (state.hasDirtyDraft) {
        return state.copy(
            showDiscardDialog = true,
            pendingAction = PendingAction.FlowMove(counted, ids.editorSessionId, ids.newPregnancyId),
            awaitingAdvance = null,
        )
    }
    return applyFlowMove(state, counted, ids)
}

/**
 * Next. With nothing to save the flow moves at once. With edits, on a pregnancy editor (always saved) or on an Identity
 * step without stored data (the save is what reports the missing name and age), the reducer only records
 * [AwaitingAdvance] and the ViewModel runs the editor's own save. The move happens inside the
 * [AncEvent.RecordCommitted] transition of that save.
 */
internal fun reduceRegistrationNext(state: AncState, event: AncEvent.RegistrationNext): AncState {
    val flow = state.registrationFlow ?: return state
    if (state.savePhase.blocksEditing() || state.pendingSave != null || state.draftRecoveryPrompt != null) return state
    val session = activeEditorSessionId(state) ?: return state
    if (session != flow.activeEditorSessionId || !state.route.matchesStep(flow.current, flow.patientId)) return state
    val ids = FlowIds(event.editorSessionId, event.newPregnancyId)
    val hasData = hasStepData(state, flow)
    if (state.hasDirtyDraft || flow.current == PREGNANCIES || (flow.current == IDENTITY && !hasData)) {
        return state.copy(awaitingAdvance = AwaitingAdvance(session, ids))
    }
    return applyFlowMove(state, FlowEvent.Next(hasData = hasData), ids)
}

private fun hasStepData(state: AncState, flow: RegistrationFlow): Boolean =
    state.patient(flow.patientId)?.let { deriveStepStatuses(it)[flow.current] == StepStatus.DONE } == true

/** Runs inside the RecordCommitted transition, after the drafts were cleared and the patients replaced. */
internal fun AncState.advanceFlowAfterCommit(token: PendingSaveToken): AncState {
    val waiting = awaitingAdvance ?: return this
    val flow = registrationFlow ?: return this
    if (waiting.fromSessionId != token.editorSessionId || flow.activeEditorSessionId != token.editorSessionId) return this
    val move = if (flow.current == PREGNANCIES) FlowEvent.PregnancyCommitted else FlowEvent.Next(hasData = hasStepData(this, flow))
    return applyFlowMove(copy(awaitingAdvance = null), move, waiting.nextIds)
}

/** Accepting a recovered draft gives the editor a new session id; the flow follows it. */
internal fun AncState.withFlowSessionFollowing(previousSessionId: String): AncState {
    val flow = registrationFlow ?: return this
    if (flow.activeEditorSessionId != previousSessionId) return this
    return copy(registrationFlow = flow.copy(activeEditorSessionId = activeEditorSessionId(this)))
}

private fun AncEvent.endsWaitingFor(waiting: AwaitingAdvance): Boolean = when (this) {
    is AncEvent.SaveFailed -> token.editorSessionId == waiting.fromSessionId
    is AncEvent.PatientValidationFailed,
    is AncEvent.ClinicalValidationFailed,
    is AncEvent.SaveValidationFailed,
    -> true
    else -> false
}

/**
 * Post-pass after every event: a failed or rejected save drops [AncState.awaitingAdvance], and the flow lives only
 * while the route is one of its screens for its patient and the open draft is the one the flow started.
 */
internal fun AncState.withFlowConsistent(event: AncEvent): AncState {
    val waiting = awaitingAdvance
    val settled = if (waiting != null && event.endsWaitingFor(waiting)) copy(awaitingAdvance = null) else this
    val flow = settled.registrationFlow
    val alive = flow != null &&
        settled.route.matchesStep(flow.current, flow.patientId) &&
        activeEditorSessionId(settled) == flow.activeEditorSessionId
    if (alive || (flow == null && settled.awaitingAdvance == null)) return settled
    return settled.copy(registrationFlow = null, awaitingAdvance = null)
}
