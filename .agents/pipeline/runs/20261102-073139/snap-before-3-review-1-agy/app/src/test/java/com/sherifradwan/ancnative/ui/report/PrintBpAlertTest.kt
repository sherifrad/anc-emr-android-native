package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoVisit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Round 3q / Q99 (SPEC_FIX_R3.md, section "Round 3q"): print report shows BP alerts in visits table.
 *
 * In the visits table the blood-pressure cell prints, under the value, a short label when
 * bpLevel(v.bloodPressure) is RAISED ("Raised BP - recheck") or SEVERE ("Severe-range BP - urgent assessment"),
 * as small text with CSS class bp-alert (SEVERE also bp-alert-severe; add the two small CSS rules,
 * red for severe, amber for raised); NORMAL prints nothing extra.
 */
class PrintBpAlertTest {

    private val AS_OF_DATE = LocalDate.of(2026, 10, 31)

    private fun visit(id: String, date: String, bp: String) = DemoVisit(
        id = id,
        date = date,
        bloodPressure = bp,
        weight = null,
        findings = null,
        notes = null,
        revision = 1,
        isSyntheticEdit = false,
    )

    private fun patientWithVisits(visits: List<DemoVisit>) = DemoPatient(
        id = "p-bp-alert-test",
        caseId = "CASE-BP-1",
        displayName = "BP Alert Patient",
        dateOfBirth = null,
        phone = null,
        visits = visits,
        riskLevel = "Low Risk",
        ageText = "28",
        bloodGroupText = "O+",
    )

    private fun countOccurrences(text: String, substr: String): Int {
        var count = 0
        var idx = 0
        while (true) {
            val next = text.indexOf(substr, idx)
            if (next == -1) break
            count++
            idx = next + substr.length
        }
        return count
    }

    @Test
    fun visitsTable_printsAlertsForRaisedAndSevere_andNoneForNormal() {
        val patient = patientWithVisits(
            listOf(
                visit("v1", "2026-08-01", "120/80"),
                visit("v2", "2026-09-01", "145/92"),
                visit("v3", "2026-10-01", "165/105"),
            )
        )

        val html = PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = patient,
            asOfDate = AS_OF_DATE,
        )

        // Contains "Raised BP - recheck" once
        assertEquals(
            "Raised BP label must appear exactly once",
            1,
            countOccurrences(html, "Raised BP - recheck"),
        )

        // Contains "Severe-range BP - urgent assessment" once
        assertEquals(
            "Severe BP label must appear exactly once",
            1,
            countOccurrences(html, "Severe-range BP - urgent assessment"),
        )

        // bp-alert-severe CSS class is present
        assertTrue(
            "HTML must contain bp-alert-severe CSS class",
            html.contains("bp-alert-severe"),
        )

        // General bp-alert CSS class is present
        assertTrue(
            "HTML must contain bp-alert CSS class",
            html.contains("bp-alert"),
        )

        // Normal BP visit (120/80) prints no alert
        val normalPatient = patientWithVisits(listOf(visit("v0", "2026-08-01", "120/80")))
        val normalHtml = PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = normalPatient,
            asOfDate = AS_OF_DATE,
        )
        assertFalse(
            "Normal BP must not produce Raised BP alert",
            normalHtml.contains("Raised BP - recheck"),
        )
        assertFalse(
            "Normal BP must not produce Severe BP alert",
            normalHtml.contains("Severe-range BP - urgent assessment"),
        )
        assertFalse(
            "Normal BP must not produce bp-alert-severe in HTML",
            normalHtml.contains("bp-alert-severe"),
        )
    }
}
