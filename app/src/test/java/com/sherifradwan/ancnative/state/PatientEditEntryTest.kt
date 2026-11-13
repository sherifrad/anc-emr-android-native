package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.FlowEvent
import com.sherifradwan.ancnative.state.RegistrationStep.DATING
import com.sherifradwan.ancnative.state.RegistrationStep.IDENTITY
import com.sherifradwan.ancnative.state.RegistrationStep.OBSTETRIC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientEditEntryTest {
    @Test
    fun complete_patient_gives_start_patient_editor() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = "30", gravida = 1, parity = 0,
            abortion = 0,
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
                datingMethod = "LMP", notes = null,
            ),
        )
        val event = editPatientEvent(patient, "s1", "g1")
        assertEquals(AncEvent.StartPatientEditor("p1", "p1", "s1"), event)
    }

    @Test
    fun patient_with_null_ageText_gives_begin_at_identity() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = null, gravida = 1, parity = 0,
            abortion = 0,
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
                datingMethod = "LMP", notes = null,
            ),
        )
        val event = editPatientEvent(patient, "s1", "g1")
        assertEquals(AncEvent.Flow(FlowEvent.Begin("p1", IDENTITY), "s1", "g1"), event)
    }

    @Test
    fun patient_with_null_dating_gives_begin_at_dating() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = "30", gravida = 1, parity = 0,
            abortion = 0,
            dating = null,
        )
        val event = editPatientEvent(patient, "s1", "g1")
        assertEquals(AncEvent.Flow(FlowEvent.Begin("p1", DATING), "s1", "g1"), event)
    }

    @Test
    fun patient_with_null_gravida_gives_begin_at_obstetric() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = "30", gravida = null, parity = 0,
            abortion = 0,
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
                datingMethod = "LMP", notes = null,
            ),
        )
        val event = editPatientEvent(patient, "s1", "g1")
        assertEquals(AncEvent.Flow(FlowEvent.Begin("p1", OBSTETRIC), "s1", "g1"), event)
    }

    @Test
    fun archived_patient_gives_null() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "archived", ageText = null, gravida = 1, parity = 0,
            abortion = 0,
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
                datingMethod = "LMP", notes = null,
            ),
        )
        assertNull(editPatientEvent(patient, "s1", "g1"))
    }

    @Test
    fun reducing_incomplete_event_sets_registration_flow() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = "30", gravida = 1, parity = 0,
            abortion = 0,
            dating = null,
        )
        val event = editPatientEvent(patient, "s1", "g1")!!
        val state = AncReducer.reduce(AncState(patients = listOf(patient)), event)
        assertEquals("p1", state.registrationFlow?.patientId)
        assertEquals(DATING, state.registrationFlow?.current)
        assertEquals(AncRoute.DatingEditor("p1"), state.route)
    }

    @Test
    fun reducing_complete_event_leaves_registration_flow_null() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = "30", gravida = 1, parity = 0,
            abortion = 0,
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
                datingMethod = "LMP", notes = null,
            ),
        )
        val event = editPatientEvent(patient, "s1", "g1")!!
        val state = AncReducer.reduce(AncState(patients = listOf(patient)), event)
        assertNull(state.registrationFlow)
        assertTrue(state.route is AncRoute.PatientEditor)
    }

    @Test
    fun reducing_identity_incomplete_event_routes_to_patient_editor() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = null, gravida = 1, parity = 0,
            abortion = 0,
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
                datingMethod = "LMP", notes = null,
            ),
        )
        val event = editPatientEvent(patient, "s1", "g1")!!
        val state = AncReducer.reduce(
            AncState(patients = listOf(patient), route = AncRoute.Workspace("p1")),
            event,
        )
        assertEquals(IDENTITY, state.registrationFlow?.current)
        assertTrue(state.route is AncRoute.PatientEditor)
    }

    @Test
    fun reducing_obstetric_incomplete_event_routes_to_patient_editor() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = "30", gravida = null, parity = 0,
            abortion = 0,
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
                datingMethod = "LMP", notes = null,
            ),
        )
        val event = editPatientEvent(patient, "s1", "g1")!!
        val state = AncReducer.reduce(
            AncState(patients = listOf(patient), route = AncRoute.Workspace("p1")),
            event,
        )
        assertEquals(OBSTETRIC, state.registrationFlow?.current)
        assertTrue(state.route is AncRoute.PatientEditor)
    }

    @Test
    fun start_patient_editor_on_incomplete_patient_routes_to_editor_without_flow() {
        val patient = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = null, gravida = 1, parity = 0,
            abortion = 0,
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
                datingMethod = "LMP", notes = null,
            ),
        )
        val state = AncReducer.reduce(
            AncState(patients = listOf(patient), route = AncRoute.Workspace("p1")),
            AncEvent.StartPatientEditor("p1", "p1", "s1"),
        )
        assertTrue(state.route is AncRoute.PatientEditor)
        assertNull(state.registrationFlow)
    }

    @Test
    fun registration_row_and_edit_entry_rule_agree() {
        val complete = DemoPatient(
            id = "p1", caseId = "c", displayName = "Mona", dateOfBirth = null, phone = null,
            visits = emptyList(), status = "active", ageText = "30", gravida = 1, parity = 0,
            abortion = 0,
            dating = DemoPatientDating(
                patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
                datingMethod = "LMP", notes = null,
            ),
        )
        val identityIncomplete = complete.copy(ageText = null)
        val obstetricIncomplete = complete.copy(gravida = null)
        assertTrue(!isRegistrationIncomplete(complete))
        assertTrue(editPatientEvent(complete, "s1", "g1") is AncEvent.StartPatientEditor)
        for (patient in listOf(identityIncomplete, obstetricIncomplete)) {
            assertTrue(isRegistrationIncomplete(patient))
            assertTrue(editPatientEvent(patient, "s1", "g1") is AncEvent.Flow)
        }
    }
}
