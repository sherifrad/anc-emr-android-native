package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.state.RegistrationStep.DATING
import com.sherifradwan.ancnative.state.RegistrationStep.HISTORIES
import com.sherifradwan.ancnative.state.RegistrationStep.IDENTITY
import com.sherifradwan.ancnative.state.RegistrationStep.OBSTETRIC
import com.sherifradwan.ancnative.state.RegistrationStep.PREGNANCIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationFlowDeriveTest {
    private fun dating() = DemoPatientDating(
        patientId = "p1", lmpDate = "2026-06-01", ultrasoundEdd = null, agreedEdd = null,
        datingMethod = "LMP", notes = null,
    )

    private fun emptyDating() = DemoPatientDating(
        patientId = "p1", lmpDate = null, ultrasoundEdd = null, agreedEdd = null,
        datingMethod = null, notes = null,
    )

    private fun pregnancy(n: Int) = DemoPreviousPregnancy(
        id = "pp$n", patientId = "p1", modelVersion = 1, dateText = null, outcomeText = null,
        pregnancyType = null, otherDetails = null, placeAndDoctor = null, deliveryBy = null,
        outcome = null, birthMethod = null, lossManagement = null, terminationMethod = null,
        provenanceKind = "test", clinicianId = "c1", recordedAt = "", updatedAt = "", revision = 1,
    )

    private fun patient(
        name: String = "Mona",
        phone: String? = "0100",
        g: Long? = 1, p: Long? = 0, a: Long? = 0,
        dating: DemoPatientDating? = dating(),
        status: String? = "active",
        history: String? = null,
        pregnancies: Int = 0,
    ) = DemoPatient(
        id = "p1", caseId = "c", displayName = name, dateOfBirth = null, phone = phone, visits = emptyList(),
        status = status, gravida = g, parity = p, abortion = a, dating = dating, medicalHistory = history,
        previousPregnancies = (1..pregnancies).map { pregnancy(it) },
    )

    @Test
    fun rf6_identity_done_and_blank_phone_negative() {
        assertEquals(StepStatus.DONE, deriveStepStatuses(patient())[IDENTITY])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(phone = "   "))[IDENTITY])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(phone = null))[IDENTITY])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(name = " "))[IDENTITY])
    }

    @Test
    fun rf6_dating_done_and_null_negative() {
        assertEquals(StepStatus.DONE, deriveStepStatuses(patient())[DATING])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(dating = null))[DATING])
    }

    @Test
    fun rf6_dating_row_with_every_field_blank_is_not_done_and_incomplete() {
        val blank = patient(dating = emptyDating())
        assertEquals(StepStatus.PENDING, deriveStepStatuses(blank)[DATING])
        assertTrue(isRegistrationIncomplete(blank))
    }

    @Test
    fun rf6_dating_with_only_agreed_edd_is_done_and_complete() {
        val p = patient(dating = emptyDating().copy(agreedEdd = "2027-03-08"))
        assertEquals(StepStatus.DONE, deriveStepStatuses(p)[DATING])
        assertFalse(isRegistrationIncomplete(p))
    }

    @Test
    fun rf6_dating_with_only_scan_edd_is_done_and_complete() {
        val p = patient(dating = emptyDating().copy(ultrasoundEdd = "2027-03-08"))
        assertEquals(StepStatus.DONE, deriveStepStatuses(p)[DATING])
        assertFalse(isRegistrationIncomplete(p))
    }

    @Test
    fun rf6_obstetric_gravida_zero_is_recorded() {
        assertEquals(StepStatus.DONE, deriveStepStatuses(patient(g = 0, p = 0, a = 0))[OBSTETRIC])
    }

    @Test
    fun rf6_obstetric_done_and_g_set_p_null_negative() {
        assertEquals(StepStatus.DONE, deriveStepStatuses(patient(g = 2, p = 1, a = 0))[OBSTETRIC])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(g = 2, p = null, a = 0))[OBSTETRIC])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(g = null, p = null, a = null))[OBSTETRIC])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(g = 2, p = 1, a = null))[OBSTETRIC])
    }

    @Test
    fun rf6_histories_done_and_all_blank_negative() {
        assertEquals(StepStatus.DONE, deriveStepStatuses(patient(history = "Asthma"))[HISTORIES])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(history = null))[HISTORIES])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(history = "  "))[HISTORIES])
        val allergyOnly = patient().copy(allergyHistory = "Penicillin")
        assertEquals(StepStatus.DONE, deriveStepStatuses(allergyOnly)[HISTORIES])
    }

    @Test
    fun rf6_pregnancies_done_and_two_for_gravida_four_negative() {
        assertEquals(StepStatus.DONE, deriveStepStatuses(patient(g = 4, p = 2, a = 1, pregnancies = 3))[PREGNANCIES])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(g = 4, p = 2, a = 1, pregnancies = 2))[PREGNANCIES])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(g = 4, p = 2, a = 1, pregnancies = 0))[PREGNANCIES])
    }

    @Test
    fun rf6_pregnancies_gravida_one_with_none_is_done_and_g_three_needs_two() {
        assertEquals(StepStatus.DONE, deriveStepStatuses(patient(g = 1, pregnancies = 0))[PREGNANCIES])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(g = 3, p = 1, a = 1, pregnancies = 1))[PREGNANCIES])
        assertEquals(StepStatus.DONE, deriveStepStatuses(patient(g = 3, p = 1, a = 1, pregnancies = 2))[PREGNANCIES])
    }

    @Test
    fun rf6_pregnancies_unknown_gravida_stays_pending() {
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(g = null, p = null, a = null, pregnancies = 0))[PREGNANCIES])
        assertEquals(StepStatus.PENDING, deriveStepStatuses(patient(g = null, p = 1, a = 1, pregnancies = 2))[PREGNANCIES])
    }

    @Test
    fun rf7_incomplete_cases() {
        assertTrue(isRegistrationIncomplete(patient(phone = null)))
        assertTrue(isRegistrationIncomplete(patient(phone = "   ")))
        assertTrue(isRegistrationIncomplete(patient(dating = null)))
        assertTrue(isRegistrationIncomplete(patient(g = 1, p = null, a = null)))
        assertTrue(isRegistrationIncomplete(patient(g = null, p = 1, a = 0)))
    }

    @Test
    fun rf7_complete_patient_without_histories_or_pregnancies_is_not_incomplete() {
        val complete = patient()
        val statuses = deriveStepStatuses(complete)
        assertEquals(StepStatus.PENDING, statuses[HISTORIES])
        assertEquals(StepStatus.DONE, statuses[PREGNANCIES])
        assertFalse(isRegistrationIncomplete(complete))
    }

    @Test
    fun rf7_archived_is_never_incomplete() {
        assertFalse(isRegistrationIncomplete(patient(status = "archived", phone = null, dating = null, g = null, p = null, a = null)))
    }

    @Test
    fun rf8_suggested_pregnancy_count() {
        assertEquals(0, suggestedPregnancyCount(1, 0, 0))
        assertEquals(2, suggestedPregnancyCount(3, 1, 1))
        assertEquals(0, suggestedPregnancyCount(null, null, null))
        assertEquals(0, suggestedPregnancyCount(0, 5, 5))
        assertEquals(3, suggestedPregnancyCount(null, 2, 1))
        assertEquals(20, suggestedPregnancyCount(50, 0, 0))
        assertEquals(20, suggestedPregnancyCount(null, 15, 15))
    }

    private fun assertRowFlag(patient: DemoPatient, expected: Boolean) {
        val flag = com.sherifradwan.ancnative.ui.projectDashboardRow(patient).isIncomplete
        assertEquals(isRegistrationIncomplete(patient), flag)
        assertEquals(expected, flag)
    }

    @Test
    fun s2_row_flag_complete_patient_false() = assertRowFlag(patient(), false)

    @Test
    fun s2_row_flag_blank_phone_true() = assertRowFlag(patient(phone = "  "), true)

    @Test
    fun s2_row_flag_no_dating_true() = assertRowFlag(patient(dating = null), true)

    @Test
    fun s2_row_flag_archived_false() = assertRowFlag(patient(phone = null, dating = null, status = "archived"), false)
}
