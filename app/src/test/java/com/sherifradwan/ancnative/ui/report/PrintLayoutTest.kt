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
 * Round 3x / Q115-Q117 (.agents/pipeline/SPEC_FIX_R3.md, section "Round 3x"): printed
 * cells never break inside a word, number or date; the stamp label is printed once; a
 * list that is cut says "and K more not shown". Builders copy the shapes of
 * PrintSectionTitlesTest.kt. context = null keeps the test JVM-only.
 */
class PrintLayoutTest {

    private val AS_OF_DATE = LocalDate.of(2026, 10, 31)

    private val VISITS_TITLE = "Antenatal Care Visits Log"
    private val SCANS_TITLE = "Ultrasound Biometry & Fetal Surveillance"
    private val LABS_TITLE = "Laboratory Investigations"
    private val MEDS_TITLE = "Active Medications"

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
        status = "active", notes = "after meals", isProphylaxis = false,
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

    /** The section's table: from the title span to the first closing table tag after it. */
    private fun tableAfter(html: String, title: String): String {
        val start = html.indexOf("<span>$title</span>")
        assertTrue("title '$title' missing", start >= 0)
        val end = html.indexOf("</table>", start)
        assertTrue("table after '$title' missing", end > start)
        return html.substring(start, end)
    }

    private fun moreRowColspans(table: String): List<Int> =
        Regex("<tr class=.more-row.><td colspan=.(\\d+).").findAll(table)
            .map { it.groupValues[1].toInt() }.toList()

    private fun columnCount(table: String): Int = Regex("<th[\\s>]").findAll(table).count()

    private fun rowCount(table: String, marker: String): Int = table.split(marker).size - 1

    /** Bodies of every CSS rule whose selector mentions [selectorPart]. */
    private fun cssRuleBodies(html: String, selectorPart: String): List<String> =
        Regex("([^{}]*)\\{([^}]*)\\}").findAll(html)
            .filter { it.groupValues[1].contains(selectorPart) }
            .map { it.groupValues[2] }.toList()

    @Test
    fun q115_tableRules_noBreakWord() {
        val generated = html(patient())
        for (selector in listOf(".clinical-table th", ".clinical-table td", ".mini-table")) {
            val bodies = cssRuleBodies(generated, selector)
            assertTrue("no CSS rule found for $selector", bodies.isNotEmpty())
            for (body in bodies) {
                assertFalse("$selector still has word-break: break-word", body.contains("word-break: break-word"))
            }
        }
    }

    @Test
    fun q115_dateCell_isNoWrap() {
        val bodies = cssRuleBodies(html(patient()), ".date-cell")
        assertTrue("no .date-cell rule", bodies.isNotEmpty())
        assertTrue(".date-cell must be white-space: nowrap", bodies.any { it.contains("white-space: nowrap") })
    }

    @Test
    fun q116_stampText_printedOnce() {
        val rendered = withoutHtmlComments(html(patient()))
        assertEquals(1, Regex.escape("Official Stamp & Signature").toRegex().findAll(rendered).count())
    }

    private fun assertCap(
        title: String,
        cap: Int,
        rowMarker: String,
        build: (Int) -> DemoPatient,
    ) {
        val over = tableAfter(html(build(cap + 3)), title)
        assertTrue("${cap + 3} rows must say 'and 3 more not shown'", over.contains("and 3 more not shown"))
        assertEquals(cap, rowCount(over, rowMarker))
        assertEquals("more-row colspan must equal the column count", listOf(columnCount(over)), moreRowColspans(over))

        val one = tableAfter(html(build(cap + 1)), title)
        assertTrue("${cap + 1} rows must say 'and 1 more not shown'", one.contains("and 1 more not shown"))

        val exact = tableAfter(html(build(cap)), title)
        assertFalse("exactly $cap rows must not say 'more not shown'", exact.contains("more not shown"))
        assertEquals(cap, rowCount(exact, rowMarker))

        val under = tableAfter(html(build(cap - 1)), title)
        assertFalse("fewer than $cap rows must not say 'more not shown'", under.contains("more not shown"))
        assertTrue(moreRowColspans(under).isEmpty())
    }

    private fun prevPreg(i: Int) = com.sherifradwan.ancnative.model.DemoPreviousPregnancy(
        id = "r3x-prev-$i", patientId = "r3x-print-1", modelVersion = 1, dateText = "2024",
        outcomeText = null, pregnancyType = null, otherDetails = "normal",
        placeAndDoctor = "Hospital", deliveryBy = null, outcome = "LiveBirth",
        birthMethod = "Vaginal", lossManagement = null, terminationMethod = null,
        provenanceKind = "t", clinicianId = "c", recordedAt = "2026-01-01",
        updatedAt = "2026-01-01", revision = 1,
    )

    @Test
    fun q115_fiveTables_insideTableScroll() {
        val p = patient(visits = 1, scans = 1, labs = 1, meds = 1).copy(
            previousPregnancies = listOf(prevPreg(1)),
        )
        val generated = html(p)
        val tableScrollMatches = Regex("""<div class="table-scroll">\s*<table class="clinical-table""").findAll(generated).count()
        assertEquals(5, tableScrollMatches)
    }

    @Test
    fun q115_narrowBlock_tableScrollAndMinWidth_printRulesDoNot() {
        val generated = html(patient())
        val narrowStart = generated.indexOf("@media screen and (max-width: 540px)")
        assertTrue("narrow @media block missing", narrowStart >= 0)
        val printStart = generated.indexOf("@media print")
        assertTrue("print @media block missing", printStart > narrowStart)
        val narrowBlock = generated.substring(narrowStart, printStart)

        assertTrue(".table-scroll in narrow block", narrowBlock.contains(".table-scroll"))
        assertTrue("overflow-x: auto in narrow .table-scroll", Regex("""\.table-scroll\s*\{[^}]*overflow-x:\s*auto""").containsMatchIn(narrowBlock))
        assertTrue(".clinical-table in narrow block", narrowBlock.contains(".clinical-table"))
        assertTrue("min-width in narrow .clinical-table", Regex("""\.clinical-table\s*\{[^}]*min-width:\s*\d+px""").containsMatchIn(narrowBlock))

        val printEnd = generated.indexOf(".clinic-header", printStart)
        val printBlock = generated.substring(printStart, printEnd)
        assertFalse(".table-scroll must not be in print block", printBlock.contains(".table-scroll"))
        assertFalse("clinical-table min-width must not be in print block", Regex("""\.clinical-table\s*\{[^}]*min-width""").containsMatchIn(printBlock))
    }

    @Test
    fun q115_noTdTextNavyNowrapRule() {
        val generated = html(patient())
        assertFalse("no td.text-navy rule should exist", generated.contains("td.text-navy"))
        assertFalse("no text-navy nowrap rule should exist", Regex("""text-navy\s*\{[^}]*white-space:\s*nowrap""").containsMatchIn(generated))
    }

    @Test
    fun q115_labAndMedNames_doNotCarryNowrap() {
        val p = patient(visits = 1, scans = 1, labs = 1, meds = 1)
        val generated = html(p)

        // Lab name cell wraps: no nowrap
        val labRow = Regex("""<tr>\s*<td[^>]*class="[^"]*date-cell[^"]*"[^>]*>.*?Test 0</td>""", RegexOption.DOT_MATCHES_ALL).find(generated)?.value
        assertTrue("lab row found", labRow != null)
        val labNameCell = Regex("""<td[^>]*>Test 0</td>""").find(labRow!!)?.value
        assertTrue("lab name cell found", labNameCell != null)
        assertFalse("lab name cell must not carry nowrap", labNameCell!!.contains("nowrap"))

        // Medication name cell wraps: no nowrap
        val medRow = Regex("""<tr>\s*<td[^>]*><span class="font-bold text-navy">Drug0</span>.*?</tr>""", RegexOption.DOT_MATCHES_ALL).find(generated)?.value
        assertTrue("med row found", medRow != null)
        val medNameCell = Regex("""<td[^>]*><span class="font-bold text-navy">Drug0</span>""").find(medRow!!)?.value
        assertTrue("med name cell found", medNameCell != null)
        assertFalse("med name cell must not carry nowrap", medNameCell!!.contains("nowrap"))

        // BP cell and EFW cell DO carry nowrap-cell
        val bpCell = Regex("""<td[^>]*>120/80</td>""").find(generated)?.value
        assertTrue("bp cell found", bpCell != null)
        assertTrue("bp cell must carry nowrap-cell", bpCell!!.contains("nowrap-cell"))

        val efwCell = Regex("""<td[^>]*>1200 g</td>""").find(generated)?.value
        assertTrue("efw cell found", efwCell != null)
        assertTrue("efw cell must carry nowrap-cell", efwCell!!.contains("nowrap-cell"))
    }

    @Test
    fun q117_visits_capAndMoreRow() {
        assertCap(VISITS_TITLE, 12, "date-cell") { patient(visits = it) }
    }

    @Test
    fun q117_scans_capAndMoreRow() {
        assertCap(SCANS_TITLE, 7, "date-cell") { patient(scans = it) }
    }

    @Test
    fun q117_labs_capAndMoreRow() {
        assertCap(LABS_TITLE, 10, "date-cell") { patient(labs = it) }
    }

    @Test
    fun q117_medications_capAndMoreRow() {
        assertCap(MEDS_TITLE, 8, "Drug") { patient(meds = it) }
    }

    @Test
    fun q117_emptyStates_unchanged() {
        val generated = html(patient())
        assertTrue(generated.contains("No clinical visits recorded yet"))
        assertTrue(generated.contains("No ultrasound scans recorded yet"))
        assertTrue(generated.contains("No laboratory test results recorded yet"))
        assertTrue(generated.contains("No active medications or supplements recorded"))
        assertFalse(generated.contains("more not shown"))
    }

    @Test
    fun q117_moreRow_isLeftAligned() {
        val bodies = cssRuleBodies(html(patient()), ".more-row")
        assertTrue("no .more-row rule in CSS", bodies.isNotEmpty())
        assertTrue(".more-row must be text-align: left", bodies.any { it.contains("text-align: left") })
    }
}
