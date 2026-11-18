package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.model.parseDeliveryNotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Round 3o tests (Q92/Q93).
 *
 * Q92: escapeHtml is the ONE escaper - null -> "", & first, then < > " '.
 * Q93: every printed patient/settings text is escaped exactly once at the
 * point it enters the HTML; no raw "<b>" may survive from any user field,
 * the escaped marker appears once per printed section, and the generator's
 * own markup (tables, badges, logo fallback) stays untouched.
 */
class PatientPrintHtmlEscapeTest {

    private val MARKER = "<b>&\"'"
    private val ESCAPED = "&lt;b&gt;&amp;&quot;&#39;"
    private val AS_OF_DATE = LocalDate.of(2026, 10, 28)

    // ---- Q92: escapeHtml unit cases ----

    @Test
    fun q92_null_isEmpty() {
        assertEquals("", escapeHtml(null))
    }

    @Test
    fun q92_empty_isEmpty() {
        assertEquals("", escapeHtml(""))
    }

    @Test
    fun q92_lessThan() {
        assertEquals("a&lt;b", escapeHtml("a<b"))
    }

    @Test
    fun q92_ampersand() {
        assertEquals("5 &amp; 6", escapeHtml("5 & 6"))
    }

    @Test
    fun q92_doubleQuoteAndApostrophe() {
        assertEquals("&quot;x&quot; &#39;y&#39;", escapeHtml("\"x\" 'y'"))
    }

    @Test
    fun q92_alreadyEscaped_isEscapedAgainNotUndone() {
        assertEquals("&amp;lt;", escapeHtml("&lt;"))
    }

    @Test
    fun q92_arabicAndUnicode_unchanged() {
        assertEquals("زيادة الوزن · حديدي ٢٠", escapeHtml("زيادة الوزن · حديدي ٢٠"))
    }

    @Test
    fun q92_markerEscapesInEntityOrder() {
        assertEquals(ESCAPED, escapeHtml(MARKER))
    }

    // ---- Q93: generator behaviour on an all-marked record ----

    private fun visit(date: String, bp: String?, weight: String?, findings: String?, notes: String?) = DemoVisit(
        id = "q93-v", date = date, bloodPressure = bp, weight = weight,
        findings = findings, notes = notes, revision = 1, isSyntheticEdit = false,
    )

    private fun scan() = DemoUltrasoundScan(
        id = "q93-s", patientId = "q93", date = MARKER,
        gestationalAgeWeeks = null, gestationalAgeDays = null, fetalNumber = MARKER,
        presentation = MARKER, placentalSite = MARKER, amnioticFluid = MARKER,
        fetalHeartRate = MARKER, biometryBpd = MARKER, biometryHc = MARKER,
        biometryAc = MARKER, biometryFl = MARKER, estimatedFetalWeight = MARKER,
        impression = "BP <90 systolic $MARKER", imageUris = listOf("content://x/1"),
    )

    private fun dating() = DemoPatientDating(
        patientId = "q93", lmpDate = MARKER, ultrasoundEdd = MARKER,
        agreedEdd = MARKER, datingMethod = MARKER, notes = MARKER,
    )

    private fun lab(abnormal: Boolean) = DemoLabInvestigation(
        id = "q93-l", patientId = "q93", date = MARKER, testCategory = "CBC",
        testName = MARKER, result = MARKER, unit = MARKER, referenceRange = MARKER,
        isAbnormal = abnormal, notes = MARKER,
    )

    private fun med() = DemoMedicationRecord(
        id = "q93-m", patientId = "q93", name = MARKER, dosage = MARKER,
        route = MARKER, frequency = MARKER, startDate = "2026-01-01",
        endDate = null, status = "Active", notes = MARKER, isProphylaxis = true,
    )

    private fun prevPreg() = DemoPreviousPregnancy(
        id = "q93-pp", patientId = "q93", modelVersion = 1, dateText = MARKER,
        outcomeText = MARKER, pregnancyType = "Singleton", otherDetails = MARKER,
        placeAndDoctor = MARKER, deliveryBy = MARKER, outcome = "LiveBirth",
        birthMethod = MARKER, lossManagement = MARKER, terminationMethod = MARKER,
        provenanceKind = "CLINICIAN_ENTRY", clinicianId = "c",
        recordedAt = "2026-01-01", updatedAt = "2026-01-01", revision = 1,
    )

    private fun markedPatient(): DemoPatient = DemoPatient(
        id = "q93",
        caseId = MARKER,
        displayName = MARKER,
        dateOfBirth = null,
        phone = MARKER,
        visits = listOf(
            visit(MARKER, MARKER, MARKER, "BP <90 systolic $MARKER", MARKER),
            visit(MARKER, MARKER, MARKER, MARKER, MARKER),
        ),
        riskLevel = MARKER,
        ageText = MARKER,
        addressText = MARKER,
        bloodGroupText = MARKER,
        medicalHistory = MARKER,
        surgicalHistory = MARKER,
        familyHistory = MARKER,
        allergyHistory = MARKER,
        gravida = 1,
        parity = 1,
        abortion = 0,
        obstetricNotes = "Plan: $MARKER",
        clinicalAttention = MARKER,
        previousPregnancies = listOf(prevPreg()),
        dating = dating(),
        ultrasoundScans = listOf(scan()),
        labInvestigations = listOf(lab(abnormal = true), lab(abnormal = false)),
        medications = listOf(med()),
    )

    private val html: String = PatientPrintHtmlGenerator.generateHtml(
        context = null,
        patient = markedPatient(),
        settings = SecuritySettingsState(clinicName = MARKER),
        asOfDate = AS_OF_DATE,
    )

    private fun countOf(pattern: String): Int = Regex(pattern).findAll(html).count()

    private fun escapedRow(label: String) {
        assertTrue(
            "row '$label' must print the escaped marker",
            Regex("""<span class="data-key">""" + Regex.escape(label) + """</span>\s*<span class="data-val[^"]*">$ESCAPED</span>""").containsMatchIn(html),
        )
    }

    @Test
    fun q93_noRawMarkerAnywhere() {
        assertFalse("raw marker must never reach the HTML", html.contains(MARKER))
        assertFalse("no raw <b> from any user field may survive", html.contains("<b>"))
        assertFalse("escape must happen exactly once", html.contains("&amp;lt;"))
        assertFalse("escape must happen exactly once", html.contains("&amp;amp;"))
    }

    @Test
    fun q93_bpUnderNinetyNote_printsAsEscapedText() {
        assertTrue("\"BP <90\" in a scan impression must print as \"BP &lt;90\"", html.contains("BP &lt;90"))
        assertTrue(html.contains("BP &lt;90 systolic"))
    }

    @Test
    fun q93_patientIdentitySection_escapedOnce() {
        assertTrue(html.contains("<title>Clinical record - $ESCAPED - $ESCAPED</title>"))
        assertTrue(countOf("""<div class="patient-name">$ESCAPED</div>""") == 1)
        assertTrue(html.contains("<span>Phone: <strong>$ESCAPED</strong></span>"))
        assertTrue(countOf("""Patient ID: <strong>$ESCAPED</strong>""") >= 2)
        assertTrue(
            Regex("""<span class="vital-label">Age</span>\s*<span class="vital-value">$ESCAPED</span>""").containsMatchIn(html),
        )
        assertTrue(
            Regex("""<span class="vital-label">Blood group</span>\s*<span class="vital-value[^"]*">$ESCAPED</span>""").containsMatchIn(html),
        )
        assertTrue(
            Regex("""<span class="risk-pill risk-unrecorded">$ESCAPED</span>""").containsMatchIn(html),
        )
        assertTrue(
            Regex("""<div class="attention-box"><span class="k">Clinical attention</span><span class="v">$ESCAPED</span></div>""").containsMatchIn(html),
        )
    }

    @Test
    fun q93_settingsClinicName_neverLeaksRaw() {
        assertTrue(countOf("""<h1>$ESCAPED</h1>""") == 1)
        assertTrue(countOf(Regex.escape("Confidential medical record · $ESCAPED ·")) == 1)
        assertTrue(html.contains("<title>Clinical record - $ESCAPED - $ESCAPED</title>"))
        assertFalse(html.contains("<b>&"))
    }

    private fun datingBox(page: String): String {
        val start = page.indexOf("Pregnancy dating")
        assertTrue(start >= 0)
        val rest = page.substring(start)
        val end = rest.indexOf("ga-banner")
        assertTrue("dating matrix must be followed by the GA banner", end > 0)
        return rest.substring(0, end)
    }

    @Test
    fun q93_datingSection_escapedOnce() {
        val box = datingBox(html)
        assertFalse(box.contains(MARKER))
        assertFalse(box.contains("&amp;lt;"))
        assertTrue(box, box.contains("""<span class="data-key">LMP</span><span class="data-val col-date">$ESCAPED</span>"""))
        assertTrue(box, box.contains("""<span class="data-key">Method</span><span class="data-val">$ESCAPED</span>"""))
        assertEquals(2, Regex("""<span class="data-val[^"]*">$ESCAPED</span>""").findAll(box).count())
        val eddCells = Regex("""<span class="data-key">EDD</span><span class="data-val col-date bold-val">([^<]*)</span>""").findAll(box).toList()
        assertEquals(box, 1, eddCells.size)
        assertEquals("Not recorded", eddCells.single().groupValues[1])
    }

    @Test
    fun q93_datingSection_malformedEddsFallBackToTheMethodEddInTheRoseCell() {
        val dating = DemoPatientDating(
            patientId = "q93", lmpDate = "2026-06-08", ultrasoundEdd = MARKER,
            agreedEdd = MARKER, datingMethod = "LMP", notes = MARKER,
        )
        val page = PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = markedPatient().copy(dating = dating),
            settings = SecuritySettingsState(clinicName = MARKER),
            asOfDate = AS_OF_DATE,
        )
        val box = datingBox(page)
        assertTrue(box, box.contains("""<span class="data-key">EDD</span><span class="data-val col-date bold-val">15/03/2027</span>"""))
        assertFalse(box.contains(MARKER))
        assertFalse(box.contains("&amp;lt;"))
        assertFalse(page.contains("&amp;amp;"))
    }

    @Test
    fun q93_historyRows_escapedOnce() {
        escapedRow("Drug allergies")
        escapedRow("Medical history")
        escapedRow("Surgical history")
        escapedRow("Family history")
    }

    @Test
    fun q93_previousPregnanciesSection_escapedOnce() {
        assertTrue(countOf("""<td class="col-date bold-val">$ESCAPED</td>""") >= 1)
        assertTrue(html.contains("""<td class="bold-val">Singleton · $ESCAPED</td>"""))
        assertTrue(html.contains("""<td>$ESCAPED</td>"""))
        assertTrue(html.contains("""<td>$ESCAPED · $ESCAPED</td>"""))
    }

    @Test
    fun q93_visitsSection_escapedOnce() {
        assertTrue(countOf("""col-date nowrap">$ESCAPED</span>""") >= 2)
        assertTrue(countOf(Regex.escape("$ESCAPED kg")) >= 2)
        assertTrue(countOf("""col-bp nowrap">$ESCAPED</span>""") >= 2)
    }

    @Test
    fun q93_scansSection_escapedOnce() {
        assertTrue(html.contains("""<span class="scan-primary"><strong>$ESCAPED</strong>, $ESCAPED</span>"""))
        assertTrue(html.contains("""<span class="u">BPD $ESCAPED</span>"""))
        assertTrue(html.contains("""<span class="u">HC $ESCAPED</span>"""))
        assertTrue(html.contains("""<span class="u">AC $ESCAPED</span>"""))
        assertTrue(html.contains("""<span class="u">FL $ESCAPED</span>"""))
        assertTrue(html.contains("""<span class="u">EFW $ESCAPED</span>"""))
        assertTrue(html.contains("""Fluid: $ESCAPED · Placenta: $ESCAPED · FHR: $ESCAPED"""))
    }

    @Test
    fun q93_labsSection_escapedOnce() {
        assertTrue(html.contains("""<td class="col-date">$ESCAPED</td>"""))
        assertTrue(html.contains("""<strong class="lab-abnormal">$ESCAPED $ESCAPED *</strong>"""))
        assertTrue(html.contains("""<td>$ESCAPED $ESCAPED</td>"""))
    }

    @Test
    fun q93_medicationsSection_escapedOnce() {
        assertTrue(
            html.contains("""<span class="bold-val">$ESCAPED</span> <span class="med-line">$ESCAPED · $ESCAPED · $ESCAPED · Prophylaxis</span> <span class="med-date">From 01/01/2026</span>"""),
        )
    }

    @Test
    fun q93_deliveryPlanSection_escapedOnce() {
        val plan = parseDeliveryNotes("Plan: $MARKER").plan
        assertTrue("fixture plan must survive parsing", plan != null && plan.contains(MARKER))
        assertTrue(
            "the parsed plan must print escaped in the Planned delivery row",
            Regex("""<span class="data-key">Planned delivery</span>\s*<span class="data-val[^"]*">""" + Regex.escape(escapeHtml(plan)) + "</span>")
                .containsMatchIn(html),
        )
        assertFalse(html.contains("Planned Delivery Mode:"))
    }

    @Test
    fun q93_generatorOwnMarkup_stillPresent() {
        assertTrue(countOf("""<table class="clinical-table">""") >= 2)
        assertFalse(html.contains("IMAGING ATTACHMENTS"))
        assertTrue(html.contains("Current medications"))
        assertTrue(html.contains("Delivery plan"))
        assertTrue(html.contains("""<div class="clinic-mark">DR</div>"""))
        assertTrue(html.contains("""Clinical record</div>"""))
        assertTrue(html.contains("""class="section-heading""""))
    }
}
