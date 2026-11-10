package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.state.RegistrationStep.DATING
import com.sherifradwan.ancnative.state.RegistrationStep.HISTORIES
import com.sherifradwan.ancnative.state.RegistrationStep.IDENTITY
import com.sherifradwan.ancnative.state.RegistrationStep.OBSTETRIC
import com.sherifradwan.ancnative.state.RegistrationStep.PREGNANCIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Oracle S5-7 (editor events and start logic) and the helpers of S5-8 and S5-10. */
class RegistrationFlowDriverTest {
    private val ids = FlowIds("session-x", "pregnancy-x")
    private val patient = DemoPatient(
        id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null, visits = emptyList(),
        status = "active", ageText = "30", gravida = 1, parity = 0, abortion = 0,
        dating = DemoPatientDating(
            patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
            datingMethod = "LMP", notes = null,
        ),
    )

    private fun flow(step: RegistrationStep, target: Int? = null) = RegistrationFlow(
        patientId = "p1",
        current = step,
        statuses = RegistrationStep.entries.associateWith { StepStatus.PENDING },
        pregnancyTarget = target,
    )

    @Test
    fun s5_7_identity_and_obstetric_both_open_the_patient_editor_of_the_stored_patient() {
        val expected = AncEvent.StartPatientEditor("p1", "p1", "session-x")
        assertEquals(expected, editorEventFor(flow(IDENTITY), patient, ids))
        assertEquals(expected, editorEventFor(flow(OBSTETRIC), patient, ids))
    }

    @Test
    fun s5_7_dating_and_histories_open_their_own_editors_with_the_given_session() {
        assertEquals(AncEvent.StartDatingEditor("p1", "session-x"), editorEventFor(flow(DATING), patient, ids))
        assertEquals(AncEvent.StartHistoriesEditor("p1", "session-x"), editorEventFor(flow(HISTORIES), patient, ids))
    }

    @Test
    fun s5_7_pregnancies_have_no_editor_until_a_count_was_chosen() {
        assertNull(editorEventFor(flow(PREGNANCIES), patient, ids))
        assertEquals(
            AncEvent.StartPregnancyEditor("p1", null, "pregnancy-x", "session-x"),
            editorEventFor(flow(PREGNANCIES, target = 2), patient, ids),
        )
    }

    @Test
    fun s5_7_start_commits_the_flow_with_the_editor_session() {
        val state = AncState(patients = listOf(patient))
        val started = startFlowEditor(state, flow(DATING), ids)!!
        assertEquals(AncRoute.DatingEditor("p1"), started.route)
        assertEquals("session-x", started.datingDraft?.editorSessionId)
        assertEquals(flow(DATING).copy(activeEditorSessionId = "session-x"), started.registrationFlow)
        assertNull(started.awaitingAdvance)
    }

    @Test
    fun s5_7_start_at_the_pregnancy_step_without_a_count_opens_the_prompt_with_no_session() {
        val state = AncState(patients = listOf(patient))
        val started = startFlowEditor(state, flow(PREGNANCIES), ids)!!
        assertEquals(AncRoute.PregnancyCountPrompt("p1"), started.route)
        assertNull(started.registrationFlow?.activeEditorSessionId)
        assertNull(activeEditorSessionId(started))
        assertFalse(started.hasDirtyDraft)
    }

    @Test
    fun s5_7_start_returns_null_and_changes_nothing_when_the_start_logic_refuses() {
        val state = AncState(patients = listOf(patient))
        assertNull(startFlowEditor(state, flow(DATING).copy(patientId = "missing"), ids))
        assertNull(startFlowEditor(state.copy(patients = listOf(patient.copy(status = "archived"))), flow(DATING), ids))
        assertNull(startFlowEditor(state.copy(patients = listOf(patient.copy(status = "archived"))), flow(PREGNANCIES), ids))
        assertNull(startFlowEditor(state.copy(savePhase = SavePhase.PENDING), flow(DATING), ids))
        assertNull(startFlowEditor(state.copy(savePhase = SavePhase.UNRESOLVED), flow(PREGNANCIES), ids))
        val dirty = AncReducer.reduce(
            AncReducer.reduce(state, AncEvent.StartDatingEditor("p1", "other")),
            AncEvent.DatingDraftChanged(DatingDraftField.LMP_DATE, "2026-07-01"),
        )
        assertTrue(dirty.hasDirtyDraft)
        assertNull(startFlowEditor(dirty, flow(HISTORIES), ids))
        assertNull(startFlowEditor(dirty, flow(PREGNANCIES), ids))
    }

    @Test
    fun s5_7_the_route_matches_exactly_the_screens_of_each_step() {
        assertTrue(AncRoute.PatientEditor("p1", false).matchesStep(IDENTITY, "p1"))
        assertTrue(AncRoute.PatientEditor("p1", true).matchesStep(OBSTETRIC, "p1"))
        assertTrue(AncRoute.DatingEditor("p1").matchesStep(DATING, "p1"))
        assertTrue(AncRoute.OtherHistoriesEditor("p1").matchesStep(HISTORIES, "p1"))
        assertTrue(AncRoute.PreviousPregnancyEditor("p1", "x").matchesStep(PREGNANCIES, "p1"))
        assertTrue(AncRoute.PregnancyCountPrompt("p1").matchesStep(PREGNANCIES, "p1"))
        assertFalse(AncRoute.DatingEditor("p1").matchesStep(HISTORIES, "p1"))
        assertFalse(AncRoute.PatientEditor("p1", false).matchesStep(DATING, "p1"))
        assertFalse(AncRoute.DatingEditor("p2").matchesStep(DATING, "p1"))
        assertFalse(AncRoute.Workspace("p1").matchesStep(DATING, "p1"))
        assertFalse(AncRoute.PatientList.matchesStep(IDENTITY, "p1"))
    }

    @Test
    fun s5_10_only_the_identity_step_forces_the_age() {
        val plain = AncState(patients = listOf(patient))
        assertFalse(plain.forcesAgeRequired())
        for (step in RegistrationStep.entries) {
            assertEquals(step.name, step == IDENTITY, plain.copy(registrationFlow = flow(step)).forcesAgeRequired())
        }
    }
}
