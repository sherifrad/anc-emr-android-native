package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.state.RegistrationStep.DATING
import com.sherifradwan.ancnative.state.RegistrationStep.HISTORIES
import com.sherifradwan.ancnative.state.RegistrationStep.IDENTITY
import com.sherifradwan.ancnative.state.RegistrationStep.OBSTETRIC
import com.sherifradwan.ancnative.state.RegistrationStep.PREGNANCIES
import com.sherifradwan.ancnative.storage.ClinicalRecordKind
import com.sherifradwan.ancnative.storage.DraftEditorKind
import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.DraftScreenKind
import com.sherifradwan.ancnative.storage.PatientCommandPreparation
import com.sherifradwan.ancnative.storage.SavePatientProfileCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reducer-level oracles S5-1 to S5-6, S5-8, S5-9, S5-11 and S5-12 of the registration flow driver. */
class RegistrationFlowStateTest {
    private val ids1 = FlowIds("s1", "n1")
    private val ids2 = FlowIds("s2", "n2")
    private val ids3 = FlowIds("s3", "n3")

    private fun dating() = DemoPatientDating(
        patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
        datingMethod = "LMP", notes = null,
    )

    private fun pregnancy(id: String) = DemoPreviousPregnancy(
        id = id, patientId = "p1", modelVersion = 3, dateText = null, outcomeText = null,
        pregnancyType = null, otherDetails = null, placeAndDoctor = "Clinic", deliveryBy = null,
        outcome = null, birthMethod = null, lossManagement = null, terminationMethod = null,
        provenanceKind = "test", clinicianId = "c1", recordedAt = "", updatedAt = "", revision = 1,
    )

    private fun patient(
        id: String = "p1",
        history: String? = null,
        g: Long? = 1,
        pregnancies: List<DemoPreviousPregnancy> = emptyList(),
        status: String? = "active",
        age: String? = "30",
        withDating: Boolean = true,
    ) = DemoPatient(
        id = id, caseId = "c-$id", displayName = "Mona", dateOfBirth = null, phone = null, visits = emptyList(),
        status = status, ageText = age, gravida = g, parity = if (g == null) null else 0, abortion = if (g == null) null else 0,
        dating = if (withDating) dating().copy(patientId = id) else null, medicalHistory = history,
        previousPregnancies = pregnancies,
    )

    private fun reduce(state: AncState, event: AncEvent) = AncReducer.reduce(state, event)

    private fun begin(state: AncState, step: RegistrationStep, ids: FlowIds = ids1, id: String = "p1") =
        reduce(state, AncEvent.Flow(FlowEvent.Begin(id, step), ids.editorSessionId, ids.newPregnancyId))

    private fun flowEvent(state: AncState, move: FlowEvent, ids: FlowIds) =
        reduce(state, AncEvent.Flow(move, ids.editorSessionId, ids.newPregnancyId))

    private fun next(state: AncState, ids: FlowIds) =
        reduce(state, AncEvent.RegistrationNext(ids.editorSessionId, ids.newPregnancyId))

    private fun atStep(step: RegistrationStep, p: DemoPatient = patient()): AncState =
        begin(AncState(patients = listOf(p)), step)

    private fun dirtyDating(state: AncState) =
        reduce(state, AncEvent.DatingDraftChanged(DatingDraftField.LMP_DATE, "2026-07-01"))

    /** Simulates the storage layer: the save starts, then commits with [patients] and the editor's own destination. */
    private fun commit(state: AncState, patients: List<DemoPatient>, operation: String = "op"): AncState {
        val token = PendingSaveToken(operation, activeEditorSessionId(state)!!)
        val started = reduce(state, AncEvent.SaveStarted(token))
        assertEquals(token, started.pendingSave)
        return reduce(
            started,
            AncEvent.RecordCommitted(token, patients, AncRoute.Workspace("p1"), SavedNotice.PATIENT),
        )
    }

    // S5-1

    @Test
    fun s5_1_a_new_patient_editor_starts_the_flow_at_identity_with_the_new_id() {
        val state = reduce(AncState(patients = emptyList()), AncEvent.StartPatientEditor(null, "new-1", "s1"))
        assertEquals(
            RegistrationFlow(
                patientId = "new-1",
                current = IDENTITY,
                statuses = RegistrationStep.entries.associateWith { StepStatus.PENDING },
                activeEditorSessionId = "s1",
            ),
            state.registrationFlow,
        )
        assertEquals(AncRoute.PatientEditor("new-1", true), state.route)
        assertEquals("s1", state.patientDraft?.editorSessionId)
        assertNull(state.awaitingAdvance)
    }

    @Test
    fun s5_1_a_new_patient_editor_replaces_the_flow_of_another_patient() {
        val first = reduce(AncState(patients = emptyList()), AncEvent.StartPatientEditor(null, "new-1", "s1"))
        val second = reduce(first, AncEvent.StartPatientEditor(null, "new-2", "s2"))
        assertEquals("s2", second.patientDraft?.editorSessionId)
        assertEquals(AncRoute.PatientEditor("new-2", true), second.route)
        assertEquals(
            RegistrationFlow(
                patientId = "new-2",
                current = IDENTITY,
                statuses = RegistrationStep.entries.associateWith { StepStatus.PENDING },
                activeEditorSessionId = "s2",
            ),
            second.registrationFlow,
        )
    }

    @Test
    fun s5_1_hub_entry_points_start_no_flow() {
        val state = AncState(patients = listOf(patient(history = "h", pregnancies = listOf(pregnancy("pp1")))))
        val edits = listOf(
            AncEvent.StartPatientEditor("p1", "p1", "s1"),
            AncEvent.StartDatingEditor("p1", "s1"),
            AncEvent.StartHistoriesEditor("p1", "s1"),
            AncEvent.StartPregnancyEditor("p1", "pp1", "n1", "s1"),
            AncEvent.StartPregnancyEditor("p1", null, "n1", "s1"),
            AncEvent.StartVisit("p1", null, "s1"),
        )
        for (event in edits) {
            val started = reduce(state, event)
            assertNotEquals(event.toString(), state, started)
            assertNull(event.toString(), started.registrationFlow)
            assertNull(event.toString(), started.awaitingAdvance)
        }
    }

    @Test
    fun s5_1_locking_clears_the_flow_and_the_waiting_advance() {
        val locked = atStep(DATING)
            .copy(awaitingAdvance = AwaitingAdvance("s1", ids2))
            .clearedForLock()
        assertNull(locked.registrationFlow)
        assertNull(locked.awaitingAdvance)
    }

    // S5-2

    @Test
    fun s5_2_next_with_edits_records_the_wait_and_moves_nothing() {
        val editing = dirtyDating(atStep(DATING))
        val waiting = next(editing, ids2)
        assertEquals(editing.copy(awaitingAdvance = AwaitingAdvance("s1", ids2)), waiting)
        assertEquals(DATING, waiting.registrationFlow?.current)
        assertEquals(AncRoute.DatingEditor("p1"), waiting.route)
    }

    @Test
    fun s5_2_the_matching_commit_moves_to_the_next_editor_in_one_transition() {
        val waiting = next(dirtyDating(atStep(DATING)), ids2)
        val saved = patient().let { it.copy(dating = it.dating?.copy(lmpDate = "2026-07-01"), revision = 2) }
        val moved = commit(waiting, listOf(saved))
        assertEquals(AncRoute.PatientEditor("p1", false), moved.route)
        assertEquals(
            RegistrationFlow(
                patientId = "p1",
                current = OBSTETRIC,
                statuses = mapOf(
                    IDENTITY to StepStatus.DONE,
                    DATING to StepStatus.DONE,
                    OBSTETRIC to StepStatus.DONE,
                    HISTORIES to StepStatus.PENDING,
                    PREGNANCIES to StepStatus.DONE,
                ),
                activeEditorSessionId = "s2",
            ),
            moved.registrationFlow,
        )
        assertNull(moved.datingDraft)
        assertEquals("s2", moved.patientDraft?.editorSessionId)
        assertEquals(2L, moved.patientDraft?.baseRevision)
        assertNull(moved.awaitingAdvance)
        assertNull(moved.pendingSave)
        assertEquals(SavePhase.IDLE, moved.savePhase)
        assertFalse(moved.savedNotice)
        assertEquals(listOf(saved), moved.patients)
    }

    @Test
    fun s5_2_a_commit_of_another_session_never_advances() {
        val waiting = next(dirtyDating(atStep(DATING)), ids2)
            .copy(awaitingAdvance = AwaitingAdvance("older-session", ids2))
        val committed = commit(waiting, listOf(patient().copy(revision = 2)))
        assertEquals(AncRoute.Workspace("p1"), committed.route)
        assertNull(committed.registrationFlow)
        assertNull(committed.awaitingAdvance)
        assertNull(committed.datingDraft)
        assertNull(committed.patientDraft)
    }

    @Test
    fun s5_2_a_failed_save_keeps_flow_and_editor_and_drops_the_wait() {
        val waiting = next(dirtyDating(atStep(DATING)), ids2)
        val token = PendingSaveToken("op", "s1")
        val failed = reduce(
            reduce(waiting, AncEvent.SaveStarted(token)),
            AncEvent.SaveFailed(token, SaveUiError.STORAGE_ERROR, unresolved = false),
        )
        assertEquals(waiting.registrationFlow, failed.registrationFlow)
        assertEquals(waiting.datingDraft, failed.datingDraft)
        assertEquals(AncRoute.DatingEditor("p1"), failed.route)
        assertNull(failed.awaitingAdvance)
        assertEquals(SavePhase.ERROR, failed.savePhase)
    }

    @Test
    fun s5_2_an_unresolved_save_keeps_flow_and_editor_and_drops_the_wait() {
        val waiting = next(dirtyDating(atStep(DATING)), ids2)
        val token = PendingSaveToken("op", "s1")
        val unresolved = reduce(
            reduce(waiting, AncEvent.SaveStarted(token)),
            AncEvent.SaveFailed(token, SaveUiError.OUTCOME_UNRESOLVED, unresolved = true),
        )
        assertEquals(waiting.registrationFlow, unresolved.registrationFlow)
        assertEquals(waiting.datingDraft, unresolved.datingDraft)
        assertEquals(AncRoute.DatingEditor("p1"), unresolved.route)
        assertNull(unresolved.awaitingAdvance)
        assertEquals(SavePhase.UNRESOLVED, unresolved.savePhase)
    }

    @Test
    fun s5_2_a_failure_of_another_token_leaves_the_wait_alone() {
        val waiting = next(dirtyDating(atStep(DATING)), ids2)
        val token = PendingSaveToken("op", "s1")
        val started = reduce(waiting, AncEvent.SaveStarted(token))
        val other = reduce(started, AncEvent.SaveFailed(PendingSaveToken("op", "elsewhere"), SaveUiError.STORAGE_ERROR, false))
        assertEquals(started, other)
    }

    @Test
    fun s5_2_validation_failures_keep_flow_and_editor_and_drop_the_wait() {
        val waiting = next(dirtyDating(atStep(DATING)), ids2)
        val rejected = reduce(
            waiting,
            AncEvent.ClinicalValidationFailed(ClinicalRecordKind.DATING, setOf(ClinicalValidationError.INVALID_DATE)),
        )
        assertEquals(waiting.registrationFlow, rejected.registrationFlow)
        assertEquals(setOf(ClinicalValidationError.INVALID_DATE), rejected.datingDraft?.errors)
        assertEquals(waiting.datingDraft?.values, rejected.datingDraft?.values)
        assertEquals(AncRoute.DatingEditor("p1"), rejected.route)
        assertNull(rejected.awaitingAdvance)

        val onProfile = next(
            reduce(atStep(OBSTETRIC), AncEvent.PatientDraftChanged(PatientDraftField.OBSTETRIC_NOTES, "n")),
            ids2,
        )
        val profileRejected = reduce(
            onProfile,
            AncEvent.PatientValidationFailed(PatientEditorKind.PROFILE, setOf(PatientValidationError.GPA_ALL_OR_NONE)),
        )
        assertEquals(onProfile.registrationFlow, profileRejected.registrationFlow)
        assertEquals(setOf(PatientValidationError.GPA_ALL_OR_NONE), profileRejected.patientDraft?.errors)
        assertNull(profileRejected.awaitingAdvance)

        val visit = reduce(
            waiting,
            AncEvent.SaveValidationFailed(setOf(DraftError.DATE_REQUIRED)),
        )
        assertNull(visit.awaitingAdvance)
    }

    // S5-3

    @Test
    fun s5_3_a_second_next_while_saving_changes_nothing() {
        val waiting = next(dirtyDating(atStep(DATING)), ids2)
        val token = PendingSaveToken("op", "s1")
        val pending = reduce(waiting, AncEvent.SaveStarted(token))
        assertEquals(SavePhase.PENDING, pending.savePhase)
        assertEquals(pending, next(pending, ids3))

        val reconciling = reduce(pending, AncEvent.SaveReconciling(token))
        assertEquals(SavePhase.RECONCILING, reconciling.savePhase)
        assertEquals(reconciling, next(reconciling, ids3))

        val unresolved = reduce(pending, AncEvent.SaveFailed(token, SaveUiError.OUTCOME_UNRESOLVED, unresolved = true))
        assertEquals(SavePhase.UNRESOLVED, unresolved.savePhase)
        assertEquals(unresolved, next(unresolved, ids3))

        val tokenOnly = dirtyDating(atStep(DATING)).copy(pendingSave = token)
        assertEquals(tokenOnly, next(tokenOnly, ids3))
    }

    @Test
    fun s5_3_a_double_tap_before_the_save_starts_replaces_the_wait_not_the_session() {
        val first = next(dirtyDating(atStep(DATING)), ids2)
        val second = next(first, ids3)
        assertEquals(AwaitingAdvance("s1", ids3), second.awaitingAdvance)
        assertEquals(first.copy(awaitingAdvance = second.awaitingAdvance), second)
    }

    // S5-4

    @Test
    fun s5_4_a_clean_next_with_stored_data_marks_done_and_opens_the_next_editor() {
        val moved = next(atStep(DATING), ids2)
        assertEquals(AncRoute.PatientEditor("p1", false), moved.route)
        assertEquals(OBSTETRIC, moved.registrationFlow?.current)
        assertEquals(StepStatus.DONE, moved.registrationFlow?.statuses?.get(DATING))
        assertEquals("s2", moved.patientDraft?.editorSessionId)
        assertEquals("s2", moved.registrationFlow?.activeEditorSessionId)
        assertNull(moved.datingDraft)
        assertNull(moved.pendingSave)
        assertNull(moved.awaitingAdvance)
        assertEquals(SavePhase.IDLE, moved.savePhase)
    }

    @Test
    fun s5_4_a_clean_next_without_stored_data_marks_skipped() {
        val moved = next(atStep(HISTORIES), ids2)
        assertEquals(StepStatus.SKIPPED, moved.registrationFlow?.statuses?.get(HISTORIES))
        assertEquals(PREGNANCIES, moved.registrationFlow?.current)
        assertEquals(AncRoute.PregnancyCountPrompt("p1"), moved.route)
        assertNull(moved.historiesDraft)
        assertNull(moved.registrationFlow?.activeEditorSessionId)

        val withHistory = next(atStep(HISTORIES, patient(history = "asthma", g = 3)), ids2)
        assertEquals(StepStatus.DONE, withHistory.registrationFlow?.statuses?.get(HISTORIES))
    }

    @Test
    fun s5_4_a_clean_next_on_identity_without_data_waits_for_the_save_and_stays() {
        val newPatient = reduce(AncState(patients = emptyList()), AncEvent.StartPatientEditor(null, "new-1", "s1"))
        val waiting = next(newPatient, ids2)
        assertEquals(AwaitingAdvance("s1", ids2), waiting.awaitingAdvance)
        assertEquals(newPatient.copy(awaitingAdvance = waiting.awaitingAdvance), waiting)

        val noAge = atStep(IDENTITY, patient(age = null))
        val noAgeWaiting = next(noAge, ids2)
        assertEquals(AwaitingAdvance("s1", ids2), noAgeWaiting.awaitingAdvance)
        assertEquals(noAge.copy(awaitingAdvance = noAgeWaiting.awaitingAdvance), noAgeWaiting)
        assertEquals(IDENTITY, noAgeWaiting.registrationFlow?.current)
    }

    // S6-1

    private fun rejectedErrors(state: AncState): Set<PatientValidationError> {
        val prepared = SavePatientProfileCommand.prepare(
            state, "op", "2026-11-10T10:00:00Z", forceAgeRequired = state.forcesAgeRequired(),
        )
        return (prepared as PatientCommandPreparation.Rejected).errors
    }

    @Test
    fun s6_1_next_on_untouched_identity_shows_the_name_and_age_errors_and_clears_the_wait() {
        val newPatient = reduce(AncState(patients = emptyList()), AncEvent.StartPatientEditor(null, "new-1", "s1"))
        val waiting = next(newPatient, ids2)
        assertNotNull(waiting.awaitingAdvance)
        val newErrors = rejectedErrors(waiting)
        assertTrue(PatientValidationError.FULL_NAME_REQUIRED in newErrors)
        assertTrue(PatientValidationError.AGE_REQUIRED in newErrors)
        val shown = reduce(waiting, AncEvent.PatientValidationFailed(PatientEditorKind.PROFILE, newErrors))
        assertTrue(shown.patientDraft!!.errors.containsAll(newErrors))
        assertNull(shown.awaitingAdvance)

        val legacyWaiting = next(atStep(IDENTITY, patient(age = null)), ids2)
        assertNotNull(legacyWaiting.awaitingAdvance)
        val legacyErrors = rejectedErrors(legacyWaiting)
        assertTrue(PatientValidationError.AGE_REQUIRED in legacyErrors)
        val legacyShown = reduce(legacyWaiting, AncEvent.PatientValidationFailed(PatientEditorKind.PROFILE, legacyErrors))
        assertTrue(PatientValidationError.AGE_REQUIRED in legacyShown.patientDraft!!.errors)
        assertNull(legacyShown.awaitingAdvance)
        assertEquals(IDENTITY, legacyShown.registrationFlow?.current)
    }

    // S6-2

    @Test
    fun s6_2_next_on_untouched_identity_with_name_and_age_advances_without_a_save() {
        val moved = next(atStep(IDENTITY), ids2)
        assertNull(moved.awaitingAdvance)
        assertEquals(StepStatus.DONE, moved.registrationFlow?.statuses?.get(IDENTITY))
        assertEquals(DATING, moved.registrationFlow?.current)
        assertEquals(AncRoute.DatingEditor("p1"), moved.route)
        assertNull(moved.pendingSave)
        assertNull(moved.patientDraft)
    }

    @Test
    fun s6_1_next_on_untouched_identity_with_a_partial_gpa_and_no_age_reports_the_gpa_error_too() {
        val partial = patient(age = null).copy(gravida = 2, parity = null, abortion = null)
        val waiting = next(atStep(IDENTITY, partial), ids2)
        assertNotNull(waiting.awaitingAdvance)
        val errors = rejectedErrors(waiting)
        assertTrue(PatientValidationError.GPA_ALL_OR_NONE in errors)
        val shown = reduce(waiting, AncEvent.PatientValidationFailed(PatientEditorKind.PROFILE, errors))
        assertTrue(shown.patientDraft!!.errors.containsAll(errors))
        assertNull(shown.awaitingAdvance)
        assertEquals(IDENTITY, shown.registrationFlow?.current)
    }

    // S5-5

    @Test
    fun s5_5_skip_with_edits_asks_first_and_the_confirmed_skip_opens_the_next_editor() {
        val editing = reduce(atStep(OBSTETRIC), AncEvent.PatientDraftChanged(PatientDraftField.OBSTETRIC_NOTES, "n"))
        val asked = flowEvent(editing, FlowEvent.Skip, ids2)
        assertTrue(asked.showDiscardDialog)
        assertEquals(PendingAction.FlowMove(FlowEvent.Skip, "s2", "n2"), asked.pendingAction)
        assertEquals(editing.copy(showDiscardDialog = true, pendingAction = asked.pendingAction), asked)

        val moved = reduce(asked, AncEvent.ConfirmDiscard)
        assertEquals(AncRoute.OtherHistoriesEditor("p1"), moved.route)
        assertEquals(HISTORIES, moved.registrationFlow?.current)
        assertEquals(StepStatus.SKIPPED, moved.registrationFlow?.statuses?.get(OBSTETRIC))
        assertEquals("s2", moved.historiesDraft?.editorSessionId)
        assertEquals("s2", moved.registrationFlow?.activeEditorSessionId)
        assertNull(moved.patientDraft)
        assertFalse(moved.showDiscardDialog)
        assertNull(moved.pendingAction)
        assertNull(moved.awaitingAdvance)
    }

    @Test
    fun s5_5_back_with_edits_asks_first_and_the_confirmed_back_opens_the_earlier_editor() {
        val editing = reduce(atStep(OBSTETRIC), AncEvent.PatientDraftChanged(PatientDraftField.OBSTETRIC_NOTES, "n"))
        val asked = flowEvent(editing, FlowEvent.Back, ids2)
        assertEquals(PendingAction.FlowMove(FlowEvent.Back, "s2", "n2"), asked.pendingAction)
        assertTrue(asked.showDiscardDialog)
        assertEquals(OBSTETRIC, asked.registrationFlow?.current)

        val moved = reduce(asked, AncEvent.ConfirmDiscard)
        assertEquals(AncRoute.DatingEditor("p1"), moved.route)
        assertEquals(DATING, moved.registrationFlow?.current)
        assertEquals(editing.registrationFlow?.statuses, moved.registrationFlow?.statuses)
        assertEquals("s2", moved.datingDraft?.editorSessionId)
        assertNull(moved.patientDraft)
    }

    @Test
    fun s5_5_keeping_the_edits_drops_the_pending_move() {
        val editing = reduce(atStep(OBSTETRIC), AncEvent.PatientDraftChanged(PatientDraftField.OBSTETRIC_NOTES, "n"))
        val kept = reduce(flowEvent(editing, FlowEvent.Skip, ids2), AncEvent.KeepEditing)
        assertEquals(editing, kept)
    }

    @Test
    fun s5_5_clean_back_and_skip_move_directly_with_fresh_sessions() {
        val atObstetric = atStep(OBSTETRIC)
        val skipped = flowEvent(atObstetric, FlowEvent.Skip, ids2)
        assertFalse(skipped.showDiscardDialog)
        assertNull(skipped.pendingAction)
        assertEquals(AncRoute.OtherHistoriesEditor("p1"), skipped.route)
        assertEquals(StepStatus.SKIPPED, skipped.registrationFlow?.statuses?.get(OBSTETRIC))

        val back = flowEvent(atObstetric, FlowEvent.Back, ids2)
        assertEquals(AncRoute.DatingEditor("p1"), back.route)
        assertEquals(atObstetric.registrationFlow?.statuses, back.registrationFlow?.statuses)

        // A frozen command of the old step is keyed by its session id; the new editor never shares it.
        assertNotEquals(activeEditorSessionId(atObstetric), activeEditorSessionId(skipped))
        assertNotEquals(activeEditorSessionId(atObstetric), activeEditorSessionId(back))
    }

    @Test
    fun s5_5_a_move_clears_a_waiting_advance() {
        val waiting = atStep(OBSTETRIC).copy(awaitingAdvance = AwaitingAdvance("s1", ids3))
        assertNull(flowEvent(waiting, FlowEvent.Skip, ids2).awaitingAdvance)
        assertNull(flowEvent(waiting, FlowEvent.Back, ids2).awaitingAdvance)
    }

    @Test
    fun s5_5_a_move_while_saving_changes_nothing() {
        val pending = reduce(
            dirtyDating(atStep(DATING)),
            AncEvent.SaveStarted(PendingSaveToken("op", "s1")),
        )
        assertEquals(pending, flowEvent(pending, FlowEvent.Skip, ids2))
        assertEquals(pending, flowEvent(pending, FlowEvent.Back, ids2))
    }

    // S5-6

    @Test
    fun s5_6_system_back_cancel_and_open_workspace_end_the_flow() {
        val atDating = atStep(DATING)
        for (event in listOf(AncEvent.Back, AncEvent.CancelEditor, AncEvent.OpenWorkspace("p1"))) {
            val left = reduce(atDating, event)
            assertNull(event.toString(), left.registrationFlow)
            assertNull(event.toString(), left.awaitingAdvance)
            assertNull(event.toString(), left.datingDraft)
            assertTrue(event.toString(), left.route is AncRoute.Workspace)
        }
    }

    @Test
    fun s5_6_back_with_edits_keeps_the_flow_until_the_discard_is_confirmed() {
        val editing = dirtyDating(atStep(DATING))
        val asked = reduce(editing, AncEvent.Back)
        assertTrue(asked.showDiscardDialog)
        assertEquals(editing.registrationFlow, asked.registrationFlow)
        val left = reduce(asked, AncEvent.ConfirmDiscard)
        assertNull(left.registrationFlow)
        assertEquals(AncRoute.Workspace("p1", WorkspaceSection.DATING), left.route)
    }

    @Test
    fun s5_6_back_on_identity_leaves_through_the_normal_back_path() {
        val newPatient = reduce(AncState(patients = emptyList()), AncEvent.StartPatientEditor(null, "new-1", "s1"))
        val toList = flowEvent(newPatient, FlowEvent.Back, ids2)
        assertEquals(AncRoute.PatientList, toList.route)
        assertNull(toList.registrationFlow)
        assertNull(toList.patientDraft)

        val savedPatient = atStep(IDENTITY, patient(age = null))
        val toHub = flowEvent(savedPatient, FlowEvent.Back, ids2)
        assertEquals(AncRoute.Workspace("p1"), toHub.route)
        assertNull(toHub.registrationFlow)

        val dirty = reduce(newPatient, AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, "Mona"))
        val asked = flowEvent(dirty, FlowEvent.Back, ids2)
        assertEquals(PendingAction.Navigate(AncRoute.PatientList), asked.pendingAction)
        assertEquals(dirty.registrationFlow, asked.registrationFlow)
    }

    @Test
    fun s5_6_a_hub_started_editor_for_another_patient_or_kind_ends_the_flow() {
        val state = AncState(patients = listOf(patient(), patient(id = "p2", history = "h")))
            .let { begin(it, DATING) }
        val otherPatient = reduce(state, AncEvent.StartDatingEditor("p2", "other"))
        assertNull(otherPatient.registrationFlow)
        assertEquals(AncRoute.DatingEditor("p2"), otherPatient.route)

        val visit = reduce(state, AncEvent.StartVisit("p1", null, "other"))
        assertNull(visit.registrationFlow)

        val openedOther = reduce(state, AncEvent.OpenWorkspace("p2"))
        assertNull(openedOther.registrationFlow)
    }

    @Test
    fun s5_6_unrelated_events_keep_the_flow() {
        val state = atStep(DATING)
        val searched = reduce(state, AncEvent.SearchChanged("mo"))
        assertEquals(state.registrationFlow, searched.registrationFlow)
        val typed = dirtyDating(state)
        assertEquals(state.registrationFlow, typed.registrationFlow)
        assertEquals(state.registrationFlow, reduce(typed, AncEvent.DismissNotice).registrationFlow)
    }

    @Test
    fun s5_6_system_back_from_the_count_prompt_ends_the_flow() {
        val prompt = atStep(PREGNANCIES, patient(g = 3))
        assertEquals(AncRoute.PregnancyCountPrompt("p1"), prompt.route)
        val left = reduce(prompt, AncEvent.Back)
        assertNull(left.registrationFlow)
        assertEquals(AncRoute.Workspace("p1"), left.route)
    }

    @Test
    fun s5_6_accepting_a_recovered_draft_keeps_the_flow_on_the_new_session() {
        val editing = atStep(DATING)
        val prompt = DraftRecoveryPrompt(
            slot = DraftSlot("p1", DraftScreenKind.DATING),
            editorSessionId = "s1",
            record = DraftRecord("p1", DraftScreenKind.DATING, DraftEditorKind.EDIT, null, 1L, mapOf("lmpDate" to "2026-08-01"), 0L),
            isStale = false,
        )
        val accepted = reduce(editing.copy(draftRecoveryPrompt = prompt), AncEvent.AcceptDraftRecovery)
        val newSession = activeEditorSessionId(accepted)
        assertNotNull(newSession)
        assertNotEquals("s1", newSession)
        assertEquals(newSession, accepted.registrationFlow?.activeEditorSessionId)
        assertEquals(DATING, accepted.registrationFlow?.current)
        assertNull(accepted.draftRecoveryPrompt)
    }

    // S5-8

    private fun pregnancyPatient(existing: Int, g: Long = 4) =
        patient(g = g, pregnancies = (1..existing).map { pregnancy("pp$it") })

    private fun withPregnancies(p: DemoPatient, vararg ids: String) =
        p.copy(previousPregnancies = p.previousPregnancies + ids.map { pregnancy(it) }, revision = p.revision + 1)

    @Test
    fun s5_8_entering_the_step_shows_the_count_prompt() {
        val prompt = atStep(PREGNANCIES, pregnancyPatient(existing = 1))
        assertEquals(AncRoute.PregnancyCountPrompt("p1"), prompt.route)
        assertEquals(PREGNANCIES, prompt.registrationFlow?.current)
        assertNull(prompt.registrationFlow?.pregnancyTarget)
        assertNull(prompt.registrationFlow?.activeEditorSessionId)
        assertNull(activeEditorSessionId(prompt))
    }

    @Test
    fun s5_8_a_count_with_nothing_remaining_finishes() {
        val prompt = atStep(PREGNANCIES, pregnancyPatient(existing = 1))
        val finished = flowEvent(prompt, FlowEvent.PregnancyCountChosen(1), ids2)
        assertEquals(AncRoute.Workspace("p1"), finished.route)
        assertNull(finished.registrationFlow)
        assertNull(finished.pregnancyDraft)

        val zero = flowEvent(prompt, FlowEvent.PregnancyCountChosen(0), ids2)
        assertEquals(AncRoute.Workspace("p1"), zero.route)
        assertNull(zero.registrationFlow)
    }

    @Test
    fun s5_8_target_three_with_one_stored_walks_two_editors_with_fresh_drafts() {
        val prompt = atStep(PREGNANCIES, pregnancyPatient(existing = 1))
        val first = flowEvent(prompt, FlowEvent.PregnancyCountChosen(3), ids2)
        assertEquals(AncRoute.PreviousPregnancyEditor("p1", "n2"), first.route)
        assertEquals(2, first.registrationFlow?.pregnancyTarget)
        assertEquals(0, first.registrationFlow?.pregnancyIndex)
        assertEquals("s2", first.registrationFlow?.activeEditorSessionId)
        assertEquals("s2", first.pregnancyDraft?.editorSessionId)
        assertEquals("n2", first.pregnancyDraft?.pregnancyId)

        val filled = reduce(first, AncEvent.PregnancyDraftChanged(PregnancyDraftField.PLACE_AND_DOCTOR, "Clinic A"))
        val waiting = next(filled, ids3)
        assertEquals(AwaitingAdvance("s2", ids3), waiting.awaitingAdvance)
        assertEquals(0, waiting.registrationFlow?.pregnancyIndex)

        val afterFirst = commit(waiting, listOf(withPregnancies(pregnancyPatient(existing = 1), "n2")))
        assertEquals(AncRoute.PreviousPregnancyEditor("p1", "n3"), afterFirst.route)
        assertEquals(1, afterFirst.registrationFlow?.pregnancyIndex)
        assertEquals(2, afterFirst.registrationFlow?.pregnancyTarget)
        assertEquals("s3", afterFirst.pregnancyDraft?.editorSessionId)
        assertEquals("n3", afterFirst.pregnancyDraft?.pregnancyId)
        // Pregnancy 2 starts empty: it does not inherit anything typed for pregnancy 1.
        assertEquals(PreviousPregnancyDraftValues(), afterFirst.pregnancyDraft?.values)
        assertEquals(PreviousPregnancyDraftValues(), afterFirst.pregnancyDraft?.original)
        assertFalse(afterFirst.hasDirtyDraft)
        assertNull(afterFirst.awaitingAdvance)

        val filledSecond = reduce(afterFirst, AncEvent.PregnancyDraftChanged(PregnancyDraftField.PLACE_AND_DOCTOR, "Clinic B"))
        val waitingSecond = next(filledSecond, FlowIds("s4", "n4"))
        val done = commit(
            waitingSecond,
            listOf(withPregnancies(pregnancyPatient(existing = 1), "n2", "n3")),
            operation = "op2",
        )
        assertEquals(AncRoute.Workspace("p1"), done.route)
        assertNull(done.registrationFlow)
        assertNull(done.pregnancyDraft)
        assertNull(done.awaitingAdvance)
        assertEquals(3, done.patient("p1")?.previousPregnancies?.size)
    }

    @Test
    fun s5_8_the_index_moves_only_when_a_pregnancy_is_committed() {
        val walking = flowEvent(
            atStep(PREGNANCIES, pregnancyPatient(existing = 0)),
            FlowEvent.PregnancyCountChosen(3),
            ids2,
        )
        val filled = reduce(walking, AncEvent.PregnancyDraftChanged(PregnancyDraftField.PLACE_AND_DOCTOR, "Clinic"))
        val waiting = next(filled, ids3)
        assertEquals(0, waiting.registrationFlow?.pregnancyIndex)
        val token = PendingSaveToken("op", "s2")
        val failed = reduce(
            reduce(waiting, AncEvent.SaveStarted(token)),
            AncEvent.SaveFailed(token, SaveUiError.STORAGE_ERROR, unresolved = false),
        )
        assertEquals(0, failed.registrationFlow?.pregnancyIndex)
        assertEquals(filled.pregnancyDraft, failed.pregnancyDraft)
        assertEquals(AncRoute.PreviousPregnancyEditor("p1", "n2"), failed.route)
    }

    @Test
    fun s5_8_next_on_an_untouched_pregnancy_editor_is_not_a_pregnancy_and_waits_for_the_save_check() {
        val walking = flowEvent(
            atStep(PREGNANCIES, pregnancyPatient(existing = 0)),
            FlowEvent.PregnancyCountChosen(3),
            ids2,
        )
        val waiting = next(walking, ids3)
        assertEquals(walking.copy(awaitingAdvance = AwaitingAdvance("s2", ids3)), waiting)
        val rejected = reduce(
            waiting,
            AncEvent.PatientValidationFailed(PatientEditorKind.PREGNANCY, setOf(PatientValidationError.PLACE_AND_DOCTOR_REQUIRED)),
        )
        assertEquals(setOf(PatientValidationError.PLACE_AND_DOCTOR_REQUIRED), rejected.pregnancyDraft?.errors)
        assertEquals(walking.registrationFlow, rejected.registrationFlow)
        assertEquals(AncRoute.PreviousPregnancyEditor("p1", "n2"), rejected.route)
        assertNull(rejected.awaitingAdvance)
    }

    @Test
    fun s5_8_skip_inside_the_walk_leaves_the_step() {
        val walking = flowEvent(
            atStep(PREGNANCIES, pregnancyPatient(existing = 0)),
            FlowEvent.PregnancyCountChosen(2),
            ids2,
        )
        val left = flowEvent(walking, FlowEvent.Skip, ids3)
        assertEquals(AncRoute.Workspace("p1"), left.route)
        assertNull(left.registrationFlow)
        assertNull(left.pregnancyDraft)
    }

    @Test
    fun s5_8_back_inside_the_walk_returns_to_the_prompt_and_a_new_count_starts_over() {
        val walking = flowEvent(
            atStep(PREGNANCIES, pregnancyPatient(existing = 0)),
            FlowEvent.PregnancyCountChosen(3),
            ids2,
        )
        val filled = reduce(walking, AncEvent.PregnancyDraftChanged(PregnancyDraftField.PLACE_AND_DOCTOR, "Clinic"))
        val committed = commit(next(filled, ids3), listOf(withPregnancies(pregnancyPatient(existing = 0), "n2")))
        assertEquals(1, committed.registrationFlow?.pregnancyIndex)

        val back = flowEvent(committed, FlowEvent.Back, FlowIds("s4", "n4"))
        assertEquals(AncRoute.PregnancyCountPrompt("p1"), back.route)
        assertEquals(PREGNANCIES, back.registrationFlow?.current)
        assertNull(back.registrationFlow?.pregnancyTarget)
        assertEquals(1, back.registrationFlow?.pregnancyIndex)
        assertNull(back.pregnancyDraft)

        val chosen = flowEvent(back, FlowEvent.PregnancyCountChosen(3), FlowIds("s5", "n5"))
        // One pregnancy is stored now, so two remain; the walk restarts at index 0.
        assertEquals(2, chosen.registrationFlow?.pregnancyTarget)
        assertEquals(0, chosen.registrationFlow?.pregnancyIndex)
        assertEquals(AncRoute.PreviousPregnancyEditor("p1", "n5"), chosen.route)
    }

    @Test
    fun s5_8_back_from_the_prompt_resets_the_walk_and_opens_histories() {
        val prompt = atStep(PREGNANCIES, pregnancyPatient(existing = 0))
        val back = flowEvent(prompt, FlowEvent.Back, ids2)
        assertEquals(AncRoute.OtherHistoriesEditor("p1"), back.route)
        assertEquals(HISTORIES, back.registrationFlow?.current)
        assertNull(back.registrationFlow?.pregnancyTarget)
        assertEquals(0, back.registrationFlow?.pregnancyIndex)
        assertEquals("s2", back.historiesDraft?.editorSessionId)
    }

    // S5-9

    /** What storage would hold after the editor's own save command committed: built from the real command. */
    private fun storedBySave(state: AncState, source: DemoPatient?, revision: Long): DemoPatient {
        val prepared = SavePatientProfileCommand.prepare(
            state, "op-$revision", "2026-11-10T10:00:00Z", forceAgeRequired = state.forcesAgeRequired(),
        )
        val command = (prepared as PatientCommandPreparation.Ready).command as SavePatientProfileCommand
        return DemoPatient(
            id = command.patientId,
            caseId = command.caseId,
            displayName = command.displayName,
            dateOfBirth = command.dateOfBirth,
            phone = command.phone,
            visits = emptyList(),
            createdAt = command.createdAt,
            status = "active",
            riskLevel = command.riskLevel,
            ageText = command.ageText,
            addressText = command.addressText,
            bloodGroupText = command.bloodGroupText,
            gravida = command.gravida,
            parity = command.parity,
            abortion = command.abortion,
            obstetricNotes = command.obstetricNotes,
            clinicalAttention = command.clinicalAttention,
            revision = revision,
            dating = source?.dating,
        )
    }

    @Test
    fun s5_9_identity_obstetric_back_back_back_identity_keeps_every_profile_field() {
        var state = reduce(AncState(patients = emptyList()), AncEvent.StartPatientEditor(null, "p1", "s1"))
        for ((field, value) in listOf(
            PatientDraftField.DISPLAY_NAME to "Mona Said",
            PatientDraftField.AGE_TEXT to "31",
            PatientDraftField.PHONE to "0100",
            PatientDraftField.ADDRESS_TEXT to "Cairo",
        )) {
            state = reduce(state, AncEvent.PatientDraftChanged(field, value))
        }
        state = next(state, ids2)
        state = commit(state, listOf(storedBySave(state, null, 1)))
        assertEquals(DATING, state.registrationFlow?.current)
        assertEquals(AncRoute.DatingEditor("p1"), state.route)

        state = flowEvent(state, FlowEvent.Skip, ids3)
        assertEquals(OBSTETRIC, state.registrationFlow?.current)
        assertEquals("Mona Said", state.patientDraft?.values?.displayName)
        for ((field, value) in listOf(
            PatientDraftField.GRAVIDA to "2",
            PatientDraftField.PARITY to "1",
            PatientDraftField.ABORTION to "0",
            PatientDraftField.RISK_LEVEL to "Moderate Risk",
            PatientDraftField.BLOOD_GROUP to "A+",
            PatientDraftField.CLINICAL_ATTENTION to "Watch BP",
        )) {
            state = reduce(state, AncEvent.PatientDraftChanged(field, value))
        }
        val obstetricIds = FlowIds("s4", "n4")
        state = next(state, obstetricIds)
        val obstetricValues = state.patientDraft!!.values
        state = commit(state, listOf(storedBySave(state, state.patient("p1"), 2)), operation = "op2")
        assertEquals(HISTORIES, state.registrationFlow?.current)

        state = flowEvent(state, FlowEvent.Back, FlowIds("s5", "n5"))
        assertEquals(OBSTETRIC, state.registrationFlow?.current)
        state = flowEvent(state, FlowEvent.Back, FlowIds("s6", "n6"))
        assertEquals(DATING, state.registrationFlow?.current)
        state = flowEvent(state, FlowEvent.Back, FlowIds("s7", "n7"))
        assertEquals(IDENTITY, state.registrationFlow?.current)
        assertEquals(AncRoute.PatientEditor("p1", false), state.route)
        val draft = state.patientDraft!!
        assertEquals("s7", draft.editorSessionId)
        assertEquals(draft.original, draft.values)
        assertEquals(2L, draft.baseRevision)
        assertEquals(obstetricValues, draft.values)
        assertEquals("Mona Said", draft.values.displayName)
        assertEquals("31", draft.values.ageText)
        assertEquals("0100", draft.values.phone)
        assertEquals("Cairo", draft.values.addressText)
        assertEquals("2", draft.values.gravida)
        assertEquals("Moderate Risk", draft.values.riskLevel)
        assertEquals("A+", draft.values.bloodGroupText)
        assertEquals("Watch BP", draft.values.clinicalAttention)
    }

    @Test
    fun s5_9_finish_sets_the_hub_and_clears_the_flow() {
        val last = atStep(PREGNANCIES, pregnancyPatient(existing = 3))
        val finished = flowEvent(last, FlowEvent.Finish, ids2)
        assertEquals(AncRoute.Workspace("p1"), finished.route)
        assertNull(finished.registrationFlow)
        assertNull(finished.awaitingAdvance)
        assertFalse(finished.savedNotice)
        assertNull(finished.notice)
        assertFalse(finished.hasDirtyDraft)
    }

    // S5-11

    @Test
    fun s5_11_skip_on_identity_is_a_no_op_even_with_edits() {
        val editing = reduce(atStep(IDENTITY), AncEvent.PatientDraftChanged(PatientDraftField.ADDRESS_TEXT, "x"))
        val after = flowEvent(editing, FlowEvent.Skip, ids2)
        assertEquals(editing, after)
        assertFalse(after.showDiscardDialog)
        assertEquals("s1", activeEditorSessionId(after))
    }

    @Test
    fun s5_11_finish_before_the_last_step_is_a_no_op_even_with_edits() {
        val editing = reduce(atStep(HISTORIES), AncEvent.HistoriesDraftChanged(HistoriesDraftField.MEDICAL, "x"))
        val after = flowEvent(editing, FlowEvent.Finish, ids2)
        assertEquals(editing, after)
        assertEquals("s1", activeEditorSessionId(after))
    }

    @Test
    fun s5_11_a_count_outside_the_last_step_is_a_no_op() {
        val editing = reduce(atStep(HISTORIES), AncEvent.HistoriesDraftChanged(HistoriesDraftField.MEDICAL, "x"))
        assertEquals(editing, flowEvent(editing, FlowEvent.PregnancyCountChosen(3), ids2))
    }

    @Test
    fun s5_11_flow_events_without_a_flow_change_nothing() {
        val plain = AncState(patients = listOf(patient()))
        for (move in listOf(FlowEvent.Back, FlowEvent.Skip, FlowEvent.Finish, FlowEvent.PregnancyCountChosen(2))) {
            assertSame(move.toString(), plain, flowEvent(plain, move, ids1))
        }
        assertSame(plain, next(plain, ids1))
    }

    @Test
    fun s5_11_skip_on_the_last_step_leaves_for_the_hub_with_drafts_cleared() {
        val walking = flowEvent(
            atStep(PREGNANCIES, pregnancyPatient(existing = 0)),
            FlowEvent.PregnancyCountChosen(2),
            ids2,
        )
        val left = flowEvent(walking, FlowEvent.Skip, ids3)
        assertEquals(AncRoute.Workspace("p1"), left.route)
        assertNull(left.registrationFlow)
        assertNull(left.pregnancyDraft)
        assertFalse(left.hasDirtyDraft)

        val editing = reduce(walking, AncEvent.PregnancyDraftChanged(PregnancyDraftField.PLACE_AND_DOCTOR, "x"))
        val asked = flowEvent(editing, FlowEvent.Skip, ids3)
        assertTrue(asked.showDiscardDialog)
        val confirmed = reduce(asked, AncEvent.ConfirmDiscard)
        assertEquals(AncRoute.Workspace("p1"), confirmed.route)
        assertNull(confirmed.registrationFlow)
        assertNull(confirmed.pregnancyDraft)
        assertNull(confirmed.pendingAction)
    }

    @Test
    fun s5_11_next_on_the_last_step_exits_to_the_hub_after_the_final_save() {
        val walking = flowEvent(
            atStep(PREGNANCIES, pregnancyPatient(existing = 0)),
            FlowEvent.PregnancyCountChosen(1),
            ids2,
        )
        val filled = reduce(walking, AncEvent.PregnancyDraftChanged(PregnancyDraftField.PLACE_AND_DOCTOR, "x"))
        val done = commit(next(filled, ids3), listOf(withPregnancies(pregnancyPatient(existing = 0), "n2")))
        assertEquals(AncRoute.Workspace("p1"), done.route)
        assertNull(done.registrationFlow)
        assertNull(done.pregnancyDraft)
        assertNull(done.awaitingAdvance)
        assertFalse(done.hasDirtyDraft)
        assertFalse(done.savedNotice)
    }

    // S5-12 and S5-7 (state side)

    @Test
    fun s5_12_a_pregnancy_editor_opened_from_the_hub_ends_the_flow() {
        val state = atStep(DATING, patient(pregnancies = listOf(pregnancy("pp1"))))
        val hubEdit = reduce(state, AncEvent.StartPregnancyEditor("p1", "pp1", "n9", "hub-session"))
        assertEquals(AncRoute.PreviousPregnancyEditor("p1", "pp1"), hubEdit.route)
        assertEquals("hub-session", hubEdit.pregnancyDraft?.editorSessionId)
        assertNull(hubEdit.registrationFlow)
        assertNull(hubEdit.awaitingAdvance)
    }

    @Test
    fun s5_12_next_is_ignored_when_the_route_does_not_match_the_current_step() {
        val atDating = atStep(DATING)
        val mismatched = atDating.copy(registrationFlow = atDating.registrationFlow!!.copy(current = HISTORIES))
        val editing = dirtyDating(mismatched)
        val after = next(editing, ids2)
        assertNull(after.awaitingAdvance)
        assertNull(after.registrationFlow)
        assertEquals(editing.datingDraft, after.datingDraft)
        assertEquals(AncRoute.DatingEditor("p1"), after.route)
    }

    @Test
    fun s5_12_next_is_ignored_for_a_stale_session_without_a_draft_and_during_recovery() {
        val atDating = dirtyDating(atStep(DATING))
        val staleSession = atDating.copy(registrationFlow = atDating.registrationFlow!!.copy(activeEditorSessionId = "old"))
        val ignored = reduce(staleSession, AncEvent.RegistrationNext("s2", "n2"))
        assertNull(ignored.awaitingAdvance)
        assertNull(ignored.registrationFlow)
        assertEquals(atDating.datingDraft, ignored.datingDraft)

        val prompt = atStep(PREGNANCIES, pregnancyPatient(existing = 0))
        assertEquals(prompt, next(prompt, ids2))

        val recovering = atDating.copy(
            draftRecoveryPrompt = DraftRecoveryPrompt(
                slot = DraftSlot("p1", DraftScreenKind.DATING),
                editorSessionId = "s1",
                record = DraftRecord("p1", DraftScreenKind.DATING, DraftEditorKind.EDIT, null, 1L, emptyMap(), 0L),
                isStale = false,
            ),
        )
        assertEquals(recovering, next(recovering, ids2))
    }

    @Test
    fun s5_7_an_archived_patient_mid_flow_clears_the_flow_and_lands_on_the_hub() {
        val state = atStep(DATING)
        val archived = state.copy(patients = listOf(patient(status = "archived")))
        val moved = next(archived, ids2)
        assertEquals(AncRoute.Workspace("p1"), moved.route)
        assertNull(moved.registrationFlow)
        assertNull(moved.datingDraft)
        assertNull(moved.patientDraft)

        val skipped = flowEvent(archived, FlowEvent.Skip, ids2)
        assertEquals(AncRoute.Workspace("p1"), skipped.route)
        assertNull(skipped.registrationFlow)
    }

    @Test
    fun s5_7_a_commit_for_an_archived_patient_lands_on_the_hub_without_a_flow() {
        val waiting = next(dirtyDating(atStep(DATING)), ids2)
        val committed = commit(waiting, listOf(patient(status = "archived").copy(revision = 2)))
        assertEquals(AncRoute.Workspace("p1"), committed.route)
        assertNull(committed.registrationFlow)
        assertNull(committed.awaitingAdvance)
        assertNull(committed.patientDraft)
        assertNull(committed.datingDraft)
    }

    // Begin from the hub

    @Test
    fun begin_from_the_hub_opens_the_chosen_step_with_derived_statuses() {
        val state = begin(AncState(patients = listOf(patient())), HISTORIES)
        assertEquals(AncRoute.OtherHistoriesEditor("p1"), state.route)
        assertEquals(
            RegistrationFlow(
                patientId = "p1",
                current = HISTORIES,
                statuses = mapOf(
                    IDENTITY to StepStatus.DONE,
                    DATING to StepStatus.DONE,
                    OBSTETRIC to StepStatus.DONE,
                    HISTORIES to StepStatus.PENDING,
                    PREGNANCIES to StepStatus.DONE,
                ),
                activeEditorSessionId = "s1",
            ),
            state.registrationFlow,
        )
    }

    @Test
    fun begin_changes_nothing_when_every_step_is_done_or_the_patient_is_unusable() {
        val complete = AncState(patients = listOf(patient(history = "h")))
        assertSame(complete, begin(complete, IDENTITY))
        val archived = AncState(patients = listOf(patient(status = "archived")))
        assertSame(archived, begin(archived, IDENTITY))
        val none = AncState(patients = emptyList())
        assertSame(none, begin(none, IDENTITY))
    }

    @Test
    fun begin_changes_nothing_while_a_flow_or_a_dirty_draft_exists() {
        val flowing = atStep(DATING)
        assertSame(flowing, begin(flowing, HISTORIES, ids2))
        val dirty = AncState(patients = listOf(patient()))
            .let { reduce(it, AncEvent.StartDatingEditor("p1", "x")) }
            .let(::dirtyDating)
        assertSame(dirty, begin(dirty, HISTORIES, ids2))
    }

    @Test
    fun flow_next_and_pregnancy_committed_cannot_be_sent_as_flow_events() {
        val state = atStep(DATING)
        assertSame(state, flowEvent(state, FlowEvent.Next(hasData = true), ids2))
        assertSame(state, flowEvent(state, FlowEvent.PregnancyCommitted, ids2))
    }

    // Review round 1

    private fun walkingAtSecondPregnancy(): AncState {
        val walking = flowEvent(
            atStep(PREGNANCIES, pregnancyPatient(existing = 0)),
            FlowEvent.PregnancyCountChosen(3),
            ids2,
        )
        val filled = reduce(walking, AncEvent.PregnancyDraftChanged(PregnancyDraftField.PLACE_AND_DOCTOR, "Clinic"))
        return commit(next(filled, ids3), listOf(withPregnancies(pregnancyPatient(existing = 0), "n2")))
    }

    private fun pregnancyRecord(recordId: String?) = DraftRecord(
        patientId = "p1",
        screenKind = DraftScreenKind.PREVIOUS_PREGNANCY,
        editorKind = DraftEditorKind.CREATE,
        recordId = recordId,
        baseRevision = null,
        fields = mapOf("placeAndDoctor" to "Typed for pregnancy one"),
        updatedAtMillis = 0L,
    )

    @Test
    fun a_pregnancy_record_of_another_pregnancy_is_not_applied_to_the_open_one() {
        val second = walkingAtSecondPregnancy()
        assertEquals("n3", second.pregnancyDraft?.pregnancyId)
        assertFalse(draftRecordBelongsToOpenEditor(second, pregnancyRecord("n2")))
        assertFalse(draftRecordBelongsToOpenEditor(second, pregnancyRecord(null)))
        assertSame(second, applyDraftRecord(second, pregnancyRecord("n2")))
        assertFalse(second.hasDirtyDraft)
    }

    @Test
    fun a_pregnancy_record_with_the_matching_id_is_still_applied() {
        val second = walkingAtSecondPregnancy()
        assertTrue(draftRecordBelongsToOpenEditor(second, pregnancyRecord("n3")))
        val applied = applyDraftRecord(second, pregnancyRecord("n3"))
        assertEquals("Typed for pregnancy one", applied.pregnancyDraft?.values?.placeAndDoctor)
        assertTrue(applied.hasDirtyDraft)
    }

    @Test
    fun records_of_other_kinds_belong_to_the_open_editor_by_slot_alone() {
        val dating = atStep(DATING)
        val record = DraftRecord("p1", DraftScreenKind.DATING, DraftEditorKind.EDIT, null, 1L, emptyMap(), 0L)
        assertTrue(draftRecordBelongsToOpenEditor(dating, record))
    }

    @Test
    fun back_with_edits_after_a_next_drops_the_wait_and_keeping_the_edits_keeps_it_dropped() {
        val waiting = next(dirtyDating(atStep(DATING)), ids2)
        assertEquals(AwaitingAdvance("s1", ids2), waiting.awaitingAdvance)
        val asked = reduce(waiting, AncEvent.Back)
        assertTrue(asked.showDiscardDialog)
        assertNull(asked.awaitingAdvance)
        val kept = reduce(asked, AncEvent.KeepEditing)
        assertNull(kept.awaitingAdvance)
        assertEquals(waiting.datingDraft, kept.datingDraft)
        assertEquals(waiting.registrationFlow, kept.registrationFlow)
    }

    @Test
    fun back_on_identity_with_edits_after_a_next_drops_the_wait() {
        val newPatient = reduce(AncState(patients = emptyList()), AncEvent.StartPatientEditor(null, "new-1", "s1"))
        val waiting = next(reduce(newPatient, AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, "Mona")), ids2)
        assertEquals(AwaitingAdvance("s1", ids2), waiting.awaitingAdvance)
        val asked = flowEvent(waiting, FlowEvent.Back, ids3)
        assertTrue(asked.showDiscardDialog)
        assertNull(asked.awaitingAdvance)
        assertNull(reduce(asked, AncEvent.KeepEditing).awaitingAdvance)
    }

    @Test
    fun a_move_starts_the_next_editor_with_a_clean_save_phase() {
        val failed = atStep(DATING).copy(savePhase = SavePhase.ERROR, saveError = SaveUiError.STORAGE_ERROR)
        val moved = flowEvent(failed, FlowEvent.Skip, ids2)
        assertEquals(AncRoute.PatientEditor("p1", false), moved.route)
        assertEquals(SavePhase.IDLE, moved.savePhase)
        assertNull(moved.saveError)
    }

    @Test
    fun a_count_while_the_walk_is_running_is_a_no_op() {
        val walking = flowEvent(
            atStep(PREGNANCIES, pregnancyPatient(existing = 0)),
            FlowEvent.PregnancyCountChosen(3),
            ids2,
        )
        assertSame(walking, flowEvent(walking, FlowEvent.PregnancyCountChosen(1), ids3))
    }

    @Test
    fun the_flow_ends_when_the_route_is_a_flow_screen_of_a_different_step() {
        val atDating = atStep(DATING)
        val wrongStep = atDating.copy(registrationFlow = atDating.registrationFlow!!.copy(current = HISTORIES))
        assertNull(reduce(wrongStep, AncEvent.SearchChanged("x")).registrationFlow)
    }
}
