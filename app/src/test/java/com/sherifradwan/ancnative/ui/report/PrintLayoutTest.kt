package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Layout and formatting unit tests for [PatientPrintHtmlGenerator] under Layout B3.
 */
class PrintLayoutTest {

    private val AS_OF_DATE = LocalDate.of(2026, 10, 31)

    private val VISITS_TITLE = "Antenatal care visits log"

    private fun date(i: Int): String = LocalDate.of(2026, 1, 1).plusDays(i.toLong()).toString()

    private fun visit(i: Int) = DemoVisit(
        id = "r3x-visit-$i", date = date(i), bloodPressure = "120/80", weight = "68.5",
        findings = "Fundal height ok", notes = null, revision = 1, isSyntheticEdit = false,
    )

    private fun scan(i: Int) = DemoUltrasoundScan(
        id = "r3x-scan-$i", patientId = "r3x-print-1", date = date(i),
        gestationalAgeWeeks = 20, gestationalAgeDays = 1, fetalNumber = null,
        presentation = "Cephalic", placentalSite = "Anterior", amnioticFluid = "Normal",
        fetalHeartRate = null, biometryBpd = null, biometryHc = null, biometryAc = null,
        biometryFl = null, estimatedFetalWeight = "1200", impression = "Normal growth",
    )

    private fun lab(i: Int) = DemoLabInvestigation(
        id = "r3x-lab-$i", patientId = "r3x-print-1", date = date(i), testCategory = "Blood",
        testName = "Test $i", result = "12", unit = "g/dL", referenceRange = "11-15",
        isAbnormal = false, notes = null,
    )

    private fun med(i: Int) = DemoMedicationRecord(
        id = "r3x-med-$i", patientId = "r3x-print-1", name = "Drug$i", dosage = "5 mg",
        route = "PO", frequency = "OD", startDate = "2026-01-01", endDate = null,
        status = "Active", notes = "after meals", isProphylaxis = false,
    )

    private fun patient(
        visits: Int = 0,
        scans: Int = 0,
        labs: Int = 0,
        meds: Int = 0,
    ) = DemoPatient(
        id = "r3x-print-1",
        caseId = "R3X-PRINT-1",
        displayName = "Layout Test Patient",
        dateOfBirth = null,
        phone = null,
        visits = (0 until visits).map(::visit),
        riskLevel = "Low Risk",
        ageText = "29",
        bloodGroupText = "O+",
    ).copy(
        medications = (0 until meds).map(::med),
        ultrasoundScans = (0 until scans).map(::scan),
        labInvestigations = (0 until labs).map(::lab),
    )

    private fun html(p: DemoPatient): String = PatientPrintHtmlGenerator.generateHtml(
        context = null,
        patient = p,
        asOfDate = AS_OF_DATE,
    )

    private fun withoutHtmlComments(html: String): String =
        Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL).replace(html, "")

    /** The section's table: from the title heading to the first closing table tag after it. */
    private fun tableAfter(html: String, title: String): String {
        val start = html.indexOf(title)
        assertTrue("title '$title' missing", start >= 0)
        val end = html.indexOf("</table>", start)
        assertTrue("table after '$title' missing", end > start)
        return html.substring(start, end)
    }

    private fun moreRowColspans(table: String): List<Int> =
        Regex("""<tr class="empty-prior-row"><td colspan="(\d+)"""").findAll(table)
            .map { it.groupValues[1].toInt() }.toList()

    private fun columnCount(table: String): Int = Regex("<th[\\s>]").findAll(table).count()

    private fun rowCount(table: String, marker: String): Int = table.split(marker).size - 1

    /** Bodies of every CSS rule whose selector mentions [selectorPart]. */
    private fun cssRuleBodies(html: String, selectorPart: String): List<String> {
        val css = html.substringAfter("<style>", "").substringBefore("</style>", "")
        return Regex("([^{}]+)\\{([^}]*)\\}").findAll(css)
            .filter { it.groupValues[1].contains(selectorPart) }
            .map { it.groupValues[2] }.toList()
    }

    @Test
    fun q115_tableRules_noBreakWord() {
        val generated = html(patient())
        for (selector in listOf("table.clinical-table th", "table.clinical-table td")) {
            val bodies = cssRuleBodies(generated, selector)
            assertTrue("no CSS rule found for $selector", bodies.isNotEmpty())
            for (body in bodies) {
                assertFalse("$selector still has word-break: break-word", body.contains("word-break: break-word"))
            }
        }
    }

    @Test
    fun q115_dateCell_isNoWrap() {
        val bodies = cssRuleBodies(html(patient()), ".col-date")
        assertTrue("no .col-date rule", bodies.isNotEmpty())
        assertTrue(".col-date must be white-space: nowrap", bodies.any { it.contains("white-space: nowrap") })
    }

    @Test
    fun q116_stampText_printedOnce() {
        val rendered = withoutHtmlComments(html(patient()))
        assertEquals(1, Regex.escape("""<div class="stamp-box">Stamp</div>""").toRegex().findAll(rendered).count())
    }

    private fun assertCap(
        title: String,
        cap: Int,
        rowMarker: String,
        hidden: (Int) -> String,
        build: (Int) -> DemoPatient,
    ) {
        val over = tableAfter(html(build(cap + 3)), title)
        assertTrue("${cap + 3} rows must say '${hidden(3)}'", over.contains(hidden(3)))
        assertEquals(cap, rowCount(over, rowMarker))
        assertEquals("hidden count colspan must equal the column count", listOf(columnCount(over)), moreRowColspans(over))

        val one = tableAfter(html(build(cap + 1)), title)
        assertTrue("${cap + 1} rows must say '${hidden(1)}'", one.contains(hidden(1)))

        val exact = tableAfter(html(build(cap)), title)
        assertFalse("exactly $cap rows must not show a hidden count", exact.contains("not shown"))
        assertEquals(cap, rowCount(exact, rowMarker))

        val under = tableAfter(html(build(cap - 1)), title)
        assertFalse("fewer than $cap rows must not show a hidden count", under.contains("not shown"))
        assertTrue(moreRowColspans(under).isEmpty())
    }

    @Test
    fun q115_noTdTextNavyNowrapRule() {
        val generated = html(patient())
        assertFalse("no td.text-navy rule should exist", generated.contains("td.text-navy"))
        assertFalse("no text-navy nowrap rule should exist", Regex("""text-navy\s*\{[^}]*white-space:\s*nowrap""").containsMatchIn(generated))
    }

    @Test
    fun q117_visits_capAndMoreRow() {
        assertCap(VISITS_TITLE, 12, "col-date nowrap", { "$it earlier visits not shown" }) { patient(visits = it) }
    }

    @Test
    fun q117_emptyStates_unchanged() {
        val generated = html(patient())
        assertTrue(generated.contains("No clinical visits recorded yet"))
        assertTrue(generated.contains("No laboratory test results recorded yet"))
        assertTrue(generated.contains("No current medications recorded"))
        assertTrue(generated.contains("No previous pregnancies or deliveries recorded"))
        assertFalse(generated.contains("not shown"))
    }
}
