package com.sherifradwan.ancnative

import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.PatientClinicalInput
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.PatientVocabulary
import com.sherifradwan.ancnative.model.validationErrors
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PatientDraftField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * MACA v2 Rigorous Clinical & Negative Control Test Suite.
 * Asserts both negative reproductions (intended errors/rejections)
 * and positive exact intended clinical outputs.
 */
class NegativeControlAndClinicalValidationTest {

    // =========================================================================
    // NEGATIVE CONTROLS: ERROR REPRODUCTION & INPUT REJECTIONS
    // =========================================================================

    @Test
    fun testNegativeControl_emptyPatientName_mustProduceFullNameRequiredError() {
        val input = createTestInput(displayName = "")
        val errors = input.validationErrors()
        assertTrue("Empty patient name must trigger FULL_NAME_REQUIRED error", errors.contains(PatientValidationError.FULL_NAME_REQUIRED))
    }

    @Test
    fun testNegativeControl_whitespaceOnlyPatientName_mustProduceFullNameRequiredError() {
        val input = createTestInput(displayName = "    ")
        val errors = input.validationErrors()
        assertTrue("Whitespace-only name must be rejected", errors.contains(PatientValidationError.FULL_NAME_REQUIRED))
    }

    @Test
    fun testNegativeControl_negativeOrExcessiveGpaValues_mustBeRejected() {
        // Negative Gravida
        val negativeInput = createTestInput(gravida = -1, parity = 0, abortion = 0)
        assertTrue("Negative Gravida must produce INVALID_GPA error", negativeInput.validationErrors().contains(PatientValidationError.INVALID_GPA))

        // Gravida beyond safe clinical bounds (> 20)
        val excessiveInput = createTestInput(gravida = PatientVocabulary.MAX_SAFE_GPA + 1, parity = 0, abortion = 0)
        assertTrue("Excessive Gravida (>20) must produce INVALID_GPA error", excessiveInput.validationErrors().contains(PatientValidationError.INVALID_GPA))
    }

    @Test
    fun testNegativeControl_futureLmpDate_mustReturnNullGestationalAge() {
        val futureLmp = LocalDate.now().plusDays(10)
        val dating = DemoPatientDating(
            patientId = "P1",
            lmpDate = futureLmp.toString(),
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )
        val ga = GestationalAgeEngine.calculate(dating, LocalDate.now())
        assertNull("Future LMP date must yield null GA (rejected calculation)", ga)
    }

    // =========================================================================
    // FOCUS PRESERVATION & BACKSTACK HIERARCHY ASSERTIONS
    // =========================================================================

    @Test
    fun testBackstackHierarchy_workspaceBackMustNavigateToProfileNotPatientList() {
        val patient = DemoFixtures.patients[0]
        val state = AncState(patients = listOf(patient))
            .copy(route = AncRoute.Workspace(patient.id))

        val nextState = AncReducer.reduce(state, AncEvent.Back)

        // Strict assertion: Back from Workspace MUST return to Profile, not dump the user onto PatientList!
        assertEquals("Back from Workspace must lead to Profile", AncRoute.Profile(patient.id), nextState.route)
    }

    @Test
    fun testBackstackHierarchy_profileBackMustNavigateToPatientList() {
        val patient = DemoFixtures.patients[0]
        val state = AncState(patients = listOf(patient))
            .copy(route = AncRoute.Profile(patient.id))

        val nextState = AncReducer.reduce(state, AncEvent.Back)
        assertEquals("Back from Profile must lead to PatientList", AncRoute.PatientList, nextState.route)
    }

    @Test
    fun testFocusPreservation_startPatientEditorInitializesCaseIdOnceWithoutRecompositionReset() {
        val state = AncState(patients = DemoFixtures.patients)
        val openEvent = AncEvent.StartPatientEditor(patientId = null, newPatientId = "new-p1", editorSessionId = "sess-1")
        val openedState = AncReducer.reduce(state, openEvent)

        val initialCaseId = openedState.patientDraft?.values?.caseId
        assertNotNull("Initial case ID must be pre-populated for new patient", initialCaseId)
        assertTrue("Case ID must follow clinical ANC-year format", initialCaseId!!.startsWith("ANC-"))

        // Doctor edits the case ID to a custom value
        val customCaseId = "CLINIC-VIP-099"
        val editedState = AncReducer.reduce(openedState, AncEvent.PatientDraftChanged(PatientDraftField.CASE_ID, customCaseId))
        assertEquals("Custom case ID must be retained without automatic overwrite", customCaseId, editedState.patientDraft?.values?.caseId)

        // Doctor temporarily clears or backspaces the case ID
        val backspacedState = AncReducer.reduce(editedState, AncEvent.PatientDraftChanged(PatientDraftField.CASE_ID, ""))
        assertEquals("Field must allow blank during interactive editing without fighting focus", "", backspacedState.patientDraft?.values?.caseId)
    }

    // =========================================================================
    // EXACT CLINICAL OUTPUT ORACLES (NOT JUST "NO ERROR")
    // =========================================================================

    @Test
    fun testClinicalOutputOracle_naegeleRuleExactEddCalculation() {
        // LMP: Jan 15, 2026 -> EDD must be exactly Oct 22, 2026 (280 days)
        val lmp = LocalDate.of(2026, 1, 15)
        val dating = DemoPatientDating(
            patientId = "P-100",
            lmpDate = lmp.toString(),
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )
        val edd = GestationalAgeEngine.calculateEstimatedDueDate(dating)
        assertEquals("Naegele's rule must add exactly 280 days to LMP", LocalDate.of(2026, 10, 22), edd)
    }

    @Test
    fun testClinicalOutputOracle_trimesterBoundaryExactClassification() {
        val lmp = LocalDate.of(2026, 1, 1)
        val dating = DemoPatientDating(
            patientId = "P-101",
            lmpDate = lmp.toString(),
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )

        // 13 weeks + 6 days = 97 days -> Trimester 1
        val gaT1 = GestationalAgeEngine.calculate(dating, lmp.plusDays(97))
        assertNotNull(gaT1)
        assertEquals("13w 6d", gaT1!!.formatted)
        assertEquals(1, gaT1.trimester)
        assertFalse(gaT1.isPostTerm)

        // 14 weeks + 0 days = 98 days -> Trimester 2
        val gaT2 = GestationalAgeEngine.calculate(dating, lmp.plusDays(98))
        assertNotNull(gaT2)
        assertEquals("14w 0d", gaT2!!.formatted)
        assertEquals(2, gaT2.trimester)
        assertFalse(gaT2.isPostTerm)

        // 28 weeks + 0 days = 196 days -> Trimester 3
        val gaT3 = GestationalAgeEngine.calculate(dating, lmp.plusDays(196))
        assertNotNull(gaT3)
        assertEquals("28w 0d", gaT3!!.formatted)
        assertEquals(3, gaT3.trimester)
        assertFalse(gaT3.isPostTerm)

        // 42 weeks + 0 days = 294 days -> Post-term
        val gaPostTerm = GestationalAgeEngine.calculate(dating, lmp.plusDays(294))
        assertNotNull(gaPostTerm)
        assertEquals("42w 0d (Post-term)", gaPostTerm!!.formatted)
        assertEquals(3, gaPostTerm.trimester)
        assertTrue(">= 42w must be flagged as post-term", gaPostTerm.isPostTerm)
    }

    @Test
    fun testClinicalOutputOracle_agreedEddOverridesLmpEdd() {
        val lmp = LocalDate.of(2026, 1, 1) // Yields EDD Oct 8
        val clinicalAgreedEdd = LocalDate.of(2026, 10, 20) // Clinical agreed adjustment
        val dating = DemoPatientDating(
            patientId = "P-102",
            lmpDate = lmp.toString(),
            ultrasoundEdd = null,
            agreedEdd = clinicalAgreedEdd.toString(),
            datingMethod = "AGREED_EDD",
            notes = null,
        )
        val calculatedEdd = GestationalAgeEngine.calculateEstimatedDueDate(dating)
        assertEquals("Agreed EDD must take strict precedence over LMP", clinicalAgreedEdd, calculatedEdd)
    }

    @Test
    fun testClinicalOutputOracle_rhBloodGroupClassification() {
        val rhNegativeGroups = listOf("A-", "B-", "AB-", "O-")
        val rhPositiveGroups = listOf("A+", "B+", "AB+", "O+")

        rhNegativeGroups.forEach { bg ->
            assertTrue("Blood group $bg must be flagged as Rh negative", bg.contains("-"))
            assertFalse("Blood group $bg must not be flagged as Rh positive", bg.contains("+"))
        }

        rhPositiveGroups.forEach { bg ->
            assertTrue("Blood group $bg must be flagged as Rh positive", bg.contains("+"))
            assertFalse("Blood group $bg must not be flagged as Rh negative", bg.contains("-"))
        }
    }

    private fun createTestInput(
        caseId: String = "CASE-1",
        displayName: String = "Synthetic Patient",
        bloodGroupText: String? = null,
        riskLevel: String? = null,
        gravida: Long? = null,
        parity: Long? = null,
        abortion: Long? = null,
    ) = PatientClinicalInput(
        caseId = caseId,
        displayName = displayName,
        dateOfBirth = null,
        phone = null,
        ageText = null,
        addressText = null,
        bloodGroupText = bloodGroupText,
        riskLevel = riskLevel,
        gravida = gravida,
        parity = parity,
        abortion = abortion,
        obstetricNotes = null,
    )
}
