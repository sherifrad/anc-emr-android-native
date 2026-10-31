package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.ui.projectDashboardRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Round 3s / Q102-Q103 cross-screen consistency test (SPEC_FIX_R3.md, section "Round 3s"):
 *
 * Asserts that for patients across different obstetric histories:
 *   projectDashboardRow(patient).prevSummary == previousDeliverySummary(patient)
 * and that the generated print HTML contains that same summary string.
 * (One rule, three screens agree).
 */
class PrintObstetricSummaryTest {

    private val AS_OF_DATE = LocalDate.of(2026, 10, 31)

    private fun preg(
        id: String,
        birthMethod: String? = null,
        deliveryBy: String? = null,
        outcomeText: String? = null,
        outcome: String? = "LiveBirth",
    ): DemoPreviousPregnancy = DemoPreviousPregnancy(
        id = id,
        patientId = "p-cross-screen",
        modelVersion = 3,
        dateText = "2024-01-01",
        outcomeText = outcomeText,
        pregnancyType = "Singleton",
        otherDetails = null,
        placeAndDoctor = null,
        deliveryBy = deliveryBy,
        outcome = outcome,
        birthMethod = birthMethod,
        lossManagement = null,
        terminationMethod = null,
        provenanceKind = "CLINICIAN_ENTRY",
        clinicianId = "clinician-1",
        recordedAt = "2024-01-01",
        updatedAt = "2024-01-01",
        revision = 1,
    )

    private fun patient(
        gravida: Long? = 1L,
        parity: Long? = 0L,
        abortion: Long? = 0L,
        pregnancies: List<DemoPreviousPregnancy> = emptyList(),
    ): DemoPatient = DemoPatient(
        id = "p-cross-screen",
        caseId = "CASE-CROSS-1",
        displayName = "Cross Screen Patient",
        dateOfBirth = null,
        phone = null,
        visits = emptyList(),
        riskLevel = "Low Risk",
        ageText = "28",
        bloodGroupText = "O+",
        gravida = gravida,
        parity = parity,
        abortion = abortion,
        previousPregnancies = pregnancies,
    )

    @Test
    fun mixedRecords_listRowAndPrintReportAgreeWithSharedRule() {
        val patient = patient(
            gravida = 4L,
            parity = 2L,
            abortion = 1L,
            pregnancies = listOf(
                preg(id = "p1", birthMethod = "Cesarean", deliveryBy = "CS", outcome = "LiveBirth"),
                preg(id = "p2", birthMethod = "Vaginal", deliveryBy = "Vaginal", outcome = "LiveBirth"),
                preg(id = "p3", outcome = "Stillbirth"),
            ),
        )

        val expectedSummary = previousDeliverySummary(patient)
        assertEquals("Prev 1 CS, 1 SVD · 1 IUFD", expectedSummary)

        // 1. PatientListScreen dashboard row projection
        val row = projectDashboardRow(patient)
        assertEquals(expectedSummary, row.prevSummary)

        // 2. PatientPrintHtmlGenerator generated HTML
        val html = PatientPrintHtmlGenerator.generateHtml(context = null, patient = patient, asOfDate = AS_OF_DATE)
        assertTrue("Print HTML must contain shared summary '$expectedSummary'", html.contains(expectedSummary))
        assertFalse("Old print format with '+' must not appear", html.contains("Prev 1 CS + 1 SVD"))
    }

    @Test
    fun primigravida_listRowAndPrintReportAgreeWithSharedRule() {
        val patient = patient(gravida = 1L, parity = 0L, abortion = 0L, pregnancies = emptyList())

        val expectedSummary = previousDeliverySummary(patient)
        assertEquals("Primigravida", expectedSummary)

        val row = projectDashboardRow(patient)
        assertEquals(expectedSummary, row.prevSummary)

        val html = PatientPrintHtmlGenerator.generateHtml(context = null, patient = patient, asOfDate = AS_OF_DATE)
        assertTrue("Print HTML must contain shared summary '$expectedSummary'", html.contains(expectedSummary))
        assertFalse("Old fallback 'Primigravida / N/A' must not appear", html.contains("Primigravida / N/A"))
    }

    @Test
    fun cesareanOnly_listRowAndPrintReportAgreeWithSharedRule() {
        val patient = patient(
            gravida = 3L,
            parity = 2L,
            abortion = 0L,
            pregnancies = listOf(
                preg(id = "p1", birthMethod = "Cesarean"),
                preg(id = "p2", deliveryBy = "CS"),
            ),
        )

        val expectedSummary = previousDeliverySummary(patient)
        assertEquals("Prev 2 CS", expectedSummary)

        val row = projectDashboardRow(patient)
        assertEquals(expectedSummary, row.prevSummary)

        val html = PatientPrintHtmlGenerator.generateHtml(context = null, patient = patient, asOfDate = AS_OF_DATE)
        assertTrue("Print HTML must contain shared summary '$expectedSummary'", html.contains(expectedSummary))
    }

    @Test
    fun vaginalOnly_listRowAndPrintReportAgreeWithSharedRule() {
        val patient = patient(
            gravida = 2L,
            parity = 1L,
            abortion = 0L,
            pregnancies = listOf(
                preg(id = "p1", birthMethod = "Vaginal"),
            ),
        )

        val expectedSummary = previousDeliverySummary(patient)
        assertEquals("Prev 1 SVD", expectedSummary)

        val row = projectDashboardRow(patient)
        assertEquals(expectedSummary, row.prevSummary)

        val html = PatientPrintHtmlGenerator.generateHtml(context = null, patient = patient, asOfDate = AS_OF_DATE)
        assertTrue("Print HTML must contain shared summary '$expectedSummary'", html.contains(expectedSummary))
    }

    @Test
    fun priorMiscarriages_listRowAndPrintReportAgreeWithSharedRule() {
        val patient = patient(
            gravida = 3L,
            parity = 0L,
            abortion = 2L,
            pregnancies = listOf(
                preg(id = "p1", outcome = "EarlyPregnancyLoss"),
                preg(id = "p2", outcomeText = "Miscarriage at 9 weeks"),
            ),
        )

        val expectedSummary = previousDeliverySummary(patient)
        assertEquals("2 Prior Miscarriages", expectedSummary)

        val row = projectDashboardRow(patient)
        assertEquals(expectedSummary, row.prevSummary)

        val html = PatientPrintHtmlGenerator.generateHtml(context = null, patient = patient, asOfDate = AS_OF_DATE)
        assertTrue("Print HTML must contain shared summary '$expectedSummary'", html.contains(expectedSummary))
    }

    @Test
    fun priorStillbirthOnly_listRowAndPrintReportAgreeWithSharedRule() {
        val patient = patient(
            gravida = 2L,
            parity = 1L,
            abortion = 0L,
            pregnancies = listOf(
                preg(id = "p1", deliveryBy = "Hysterotomy", outcome = "Stillbirth"),
            ),
        )

        val expectedSummary = previousDeliverySummary(patient)
        assertEquals("Prior Stillbirth (IUFD)", expectedSummary)

        val row = projectDashboardRow(patient)
        assertEquals(expectedSummary, row.prevSummary)

        val html = PatientPrintHtmlGenerator.generateHtml(context = null, patient = patient, asOfDate = AS_OF_DATE)
        assertTrue("Print HTML must contain shared summary '$expectedSummary'", html.contains(expectedSummary))
    }
}
