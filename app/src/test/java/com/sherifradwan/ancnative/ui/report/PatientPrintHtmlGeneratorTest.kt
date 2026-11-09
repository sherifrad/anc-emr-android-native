package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.ui.parseDeliveryNotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Round 2e / Q17 tests (R4-2, .agents/pipeline/SPEC_FIX_R2.md addendum): the printed
 * report must classify risk and Rh status with the SHARED rules isHighRisk /
 * isRhNegative from ClinicalFormat.kt, exactly like the screens do after Q7.
 * Markup and wording stay as they are today - the change is the two rule lines
 * (today: riskLevel.contains("High"), bloodGroupText.contains("-")) only.
 *
 * Round 2f additions: Q18 (R5-1, printed Age cell uses the shared formatAgeYears
 * rule; every spec example asserted on the exact Age cell) below the Q17 tests.
 *
 * Expectations today: "High Risk"/"high"/"Low Risk"/"O-"/"AB-"/"pending"/null already
 * print correctly (these assertions pass now and must keep passing byte-identically);
 * the LEGACY/simple-text values misclassify today and must FAIL this test until the
 * generator uses the shared rules: "Not high risk", "Moderate Risk" badges are fine
 * already - "Not high risk" is flagged High Risk today; "O neg", "AB Negative",
 * "O −" (U+2212) get NO Rh badge today; "A+, antibody screen negative" gets a FALSE
 * Rh badge today (a hyphen inside the antibody note, not the group).
 *
 * SAFE assumptions:
 *  - context = null short-circuits Android logo decoding, so this runs as a plain
 *    JVM unit test without Robolectric.
 *  - riskLevel null keeps today's "Low Risk" default -> Normal Risk badge.
 *  - The two rule PRESENCE/ABSENCE assertions below pin the exact validated markup
 *    (High Risk + O- and Low Risk + O+), which is today's byte output for those
 *    validated combinations; a correct implementation differs from today only for
 *    the misclassified values listed above.
 */
class PatientPrintHtmlGeneratorTest {

    private val HIGH_RISK_BADGE = """<span class="badge badge-danger">High Risk</span>"""
    private val NORMAL_RISK_BADGE = """<span class="badge badge-success">Normal Risk</span>"""
    private val RH_BADGE = """<span class="badge badge-danger">Rh- Negative</span>"""
    private val ANTI_D_LINE = "Anti-D Prophylaxis + Matched Packed RBCs Cross-match"
    private val ROUTINE_LINE = "Routine Obstetric Hospital Protocol"
    private val AS_OF_DATE = LocalDate.of(2026, 10, 28)

    private fun patient(riskLevel: String?, bloodGroupText: String?, ageText: String? = null): DemoPatient = DemoPatient(
        id = "r2e-print-1",
        caseId = "R2E-PRINT-1",
        displayName = "Print Test Patient",
        dateOfBirth = null,
        phone = null,
        visits = emptyList(),
        riskLevel = riskLevel,
        ageText = ageText,
        bloodGroupText = bloodGroupText,
    )

    private fun htmlFor(riskLevel: String?, bloodGroupText: String?, ageText: String? = null): String =
        PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = patient(riskLevel, bloodGroupText, ageText),
            asOfDate = AS_OF_DATE,
        )

    // ---- Q17 risk-level badge (exactly one of the two badges is present) ----

    @Test
    fun q17_riskLevelsValidated_highRiskBadge() {
        for (risk in listOf("High Risk", "high")) {
            val html = htmlFor(risk, "O+")
            assertTrue("'$risk' must show the High Risk badge", html.contains(HIGH_RISK_BADGE))
            assertFalse("'$risk' must NOT show the Normal Risk badge", html.contains(NORMAL_RISK_BADGE))
        }
    }

    @Test
    fun q17_riskLevelsNonHigh_normalRiskBadge() {
        for (risk in listOf("Low Risk", "Moderate Risk", "Not high risk")) {
            val html = htmlFor(risk, "O+")
            assertTrue("'$risk' must show the Normal Risk badge", html.contains(NORMAL_RISK_BADGE))
            assertFalse("'$risk' must NOT show the High Risk badge", html.contains(HIGH_RISK_BADGE))
        }
    }

    // ---- Q17 Rh badge + anti-D line for Rh-negative values ----

    @Test
    fun q17_rhNegative_values_showRhBadgeAndAntiD() {
        for (group in listOf("O-", "AB-", "O neg", "AB Negative", "O −")) {
            val html = htmlFor("Low Risk", group)
            assertTrue(
                "'$group' must show the Rh- Negative badge",
                html.contains(RH_BADGE),
            )
            assertTrue(
                "'$group' must show the Anti-D cross-match precautions line",
                html.contains(ANTI_D_LINE),
            )
            assertFalse(
                "'$group' must not show the routine-protocol line instead",
                html.contains(ROUTINE_LINE),
            )
        }
    }

    // ---- Q17 Rh-positive / unknown values: neither badge nor anti-D ----

    @Test
    fun q17_rhPositiveOrUnknown_noRhBadgeNoAntiD() {
        for (group in listOf("O+", "A+", "A+, antibody screen negative", "pending", null)) {
            val html = htmlFor("Low Risk", group)
            assertFalse(
                "'$group' must NOT show the Rh- Negative badge",
                html.contains(RH_BADGE),
            )
            assertFalse(
                "'$group' must NOT show the Anti-D precautions line",
                html.contains(ANTI_D_LINE),
            )
            assertTrue("'$group' keeps the routine-protocol line", html.contains(ROUTINE_LINE))
        }
    }

    // ---- Q17 validated patients still print exactly today's validated markup ----

    @Test
    fun q17_validatedHighRiskRhNegative_highRiskBadgeRhBadgeAntiD() {
        val html = htmlFor("High Risk", "O-")
        assertTrue(html.contains(HIGH_RISK_BADGE))
        assertFalse(html.contains(NORMAL_RISK_BADGE))
        assertTrue(html.contains(RH_BADGE))
        assertTrue(html.contains(ANTI_D_LINE))
        assertFalse(html.contains(ROUTINE_LINE))
    }

    @Test
    fun q17_validatedLowRiskRhPositive_normalRiskBadgeOnly() {
        val html = htmlFor("Low Risk", "O+")
        assertTrue(html.contains(NORMAL_RISK_BADGE))
        assertFalse(html.contains(HIGH_RISK_BADGE))
        assertFalse(html.contains(RH_BADGE))
        assertFalse(html.contains(ANTI_D_LINE))
        assertTrue(html.contains(ROUTINE_LINE))
    }

    // ---- Q17 boundaries: the two rules are independent of each other ----

    @Test
    fun q17_boundary_notHighRiskButRhNegative_normalBadgeWithRhBadge() {
        val html = htmlFor("Not high risk", "O-")
        assertTrue("risk wording must not leak into the Rh rule", html.contains(NORMAL_RISK_BADGE))
        assertTrue(html.contains(RH_BADGE))
        assertTrue(html.contains(ANTI_D_LINE))
    }

    @Test
    fun q17_boundary_highRiskWithPendingBlood_highBadgeWithoutRhBadge() {
        val html = htmlFor("high", "pending")
        assertTrue("risk wording must not leak into the Rh rule", html.contains(HIGH_RISK_BADGE))
        assertFalse(html.contains(RH_BADGE))
        assertFalse(html.contains(ANTI_D_LINE))
    }

    // =====================================================================
    // Round 2f oracles (Q18 / R5-1, .agents/pipeline/SPEC_FIX_R2.md addendum).
    // The printed Age value must use the shared formatAgeYears rule:
    //   valid age   -> "n yrs"
    //   non-blank invalid text -> the trimmed text exactly as typed, no " yrs"
    //     (never hide what the clinician typed on a printed record)
    //   null/blank  -> "-"
    // Every example below is the exact input/expected pair from the Q18 list
    // in SPEC_FIX_R2.md. Assertions pin the exact Age cell:
    //   <span class="data-label">Age</span> ...\n... <span class="data-value">VALUE</span>
    // markup as it is today (only the value logic may change, not the markup).
    //
    // Expected failures today (ageText non-blank blindly gets " yrs"): the six
    // non-blank-invalid inputs "29 years", "Age: 29", " 29 ", "unknown",
    // "290", "29 years 3 months" all get " yrs" appended today.
    // "29" and the blank/null cases already print correctly today and must
    // keep passing unchanged.
    // =====================================================================

    private fun ageValue(html: String, ageTextInput: String?): String {
        val regex = Regex(
            """<span class="data-label">Age</span>\s*<span class="data-value">([^<]+)</span>""",
        )
        val m = regex.find(html)
        assertTrue("Age cell must be present exactly once (input: $ageTextInput)", m != null)
        return m!!.groupValues[1]
    }

    // ---- Q18 valid ages (formatAgeYears accepts) -> "n yrs" ----

    @Test
    fun q18_bareNumber_ageCellExact() {
        val html = htmlFor("Low Risk", "O+", "29")
        assertEquals("29 yrs", ageValue(html, "29"))
    }

    @Test
    fun q18_ageTextWithFullYear_goesToNumberOnly() {
        val html = htmlFor("Low Risk", "O+", "29 years")
        assertEquals("29 yrs", ageValue(html, "29 years"))
    }

    @Test
    fun q18_ageLabelWithColon_goesToNumberOnly() {
        val html = htmlFor("Low Risk", "O+", "Age: 29")
        assertEquals("29 yrs", ageValue(html, "Age: 29"))
    }

    @Test
    fun q18_paddedNumber_trimmedThenNumberOnly() {
        val html = htmlFor("Low Risk", "O+", " 29 ")
        assertEquals("29 yrs", ageValue(html, " 29 "))
    }

    // ---- Q18 non-blank non-valid text -> trimmed text exactly as typed, no " yrs" ----

    @Test
    fun q18_unknownWord_printedAsTyped() {
        val html = htmlFor("Low Risk", "O+", "unknown")
        assertEquals("unknown", ageValue(html, "unknown"))
    }

    @Test
    fun q18_outOfRangeValue_printedAsTyped() {
        val html = htmlFor("Low Risk", "O+", "290")
        assertEquals("290", ageValue(html, "290"))
    }

    @Test
    fun q18_mixedText_printedAsTyped() {
        val html = htmlFor("Low Risk", "O+", "29 years 3 months")
        assertEquals("29 years 3 months", ageValue(html, "29 years 3 months"))
    }

    // ---- Q18 blank / null -> "-" ----

    @Test
    fun q18_blankString_isDash() {
        val html = htmlFor("Low Risk", "O+", "")
        assertEquals("-", ageValue(html, ""))
    }

    @Test
    fun q18_null_isDash() {
        val html = htmlFor("Low Risk", "O+", null)
        assertEquals("-", ageValue(html, null))
    }

    // =====================================================================
    // Round 3a / Q19: the printed report never invents a clinical finding.
    // Missing = null, empty or whitespace only -> exactly "Not recorded".
    // (Q19(e) supersedes the Q17 case "riskLevel null -> Normal Risk": null was
    // removed from q17_riskLevelsNonHigh_normalRiskBadge, see q19e below.)
    // =====================================================================

    private val NOT_RECORDED = "Not recorded"
    private val MISSING = listOf(null, "", "   ")
    private val PLAN_SENTENCE = "Elective Cesarean Section (CS) / Spontaneous Vaginal Delivery (SVD)"

    private fun q19Html(
        allergy: String? = "x",
        medical: String? = "x",
        surgical: String? = "x",
        notes: String? = null,
        dating: DemoPatientDating? = null,
        risk: String? = "Low Risk",
        scans: List<DemoUltrasoundScan> = emptyList(),
    ): String = PatientPrintHtmlGenerator.generateHtml(
        context = null,
        patient = patient(risk, "O+").copy(
            allergyHistory = allergy,
            medicalHistory = medical,
            surgicalHistory = surgical,
            obstetricNotes = notes,
            dating = dating,
            ultrasoundScans = scans,
        ),
        asOfDate = AS_OF_DATE,
    )

    private fun scan(presentation: String?, site: String?, fluid: String?) = DemoUltrasoundScan(
        id = "q19-scan", patientId = "r2e-print-1", date = "2026-09-01",
        gestationalAgeWeeks = null, gestationalAgeDays = null, fetalNumber = null,
        presentation = presentation, placentalSite = site, amnioticFluid = fluid,
        fetalHeartRate = null, biometryBpd = null, biometryHc = null, biometryAc = null,
        biometryFl = null, estimatedFetalWeight = null, impression = null,
    )

    private fun dating(agreedEdd: String?) = DemoPatientDating(
        patientId = "r2e-print-1", lmpDate = null, ultrasoundEdd = null, agreedEdd = agreedEdd,
        datingMethod = null, notes = null,
    )

    private fun rowValue(html: String, label: String): String {
        val m = Regex(label + """</span>\s*<span class="v[^"]*">([^<]*)</span>""").find(html)
        assertTrue("row '$label' must be present", m != null)
        return m!!.groupValues[1]
    }

    private fun notRecordedCells(html: String): Int =
        Regex(Regex.escape("""<td class="text-center">$NOT_RECORDED</td>""")).findAll(html).count()

    // (a) ultrasound cells
    @Test
    fun q19a_missingScanCells_notRecorded_noInventedFindings() {
        for (v in MISSING) {
            val html = q19Html(scans = listOf(scan(v, v, v)))
            assertEquals("'$v': three Not recorded cells", 3, notRecordedCells(html))
            assertFalse(html.contains("Cephalic"))
            assertFalse(html.contains("Posterior Grade I"))
            assertFalse(html.contains("Adequate (AFI 14cm)"))
        }
    }

    @Test
    fun q19a_presentValues_printedUnchanged() {
        val html = q19Html(scans = listOf(scan("Breech", "Anterior", "Reduced")))
        assertTrue(html.contains("""<td class="text-center">Breech</td>"""))
        assertTrue(html.contains("""<td class="text-center">Anterior</td>"""))
        assertTrue(html.contains("""<td class="text-center">Reduced</td>"""))
        assertEquals(0, notRecordedCells(html))
        assertFalse(html.contains("Cephalic"))
    }

    @Test
    fun q19a_onlyOneMissing_onlyThatCellNotRecorded() {
        val html = q19Html(scans = listOf(scan("Breech", null, "Reduced")))
        assertEquals(1, notRecordedCells(html))
        assertFalse(html.contains("Posterior Grade I"))
    }

    // (b) allergies
    @Test
    fun q19b_allergyMissing_notRecorded_noNkda() {
        for (v in MISSING) {
            val html = q19Html(allergy = v)
            assertEquals("'$v'", NOT_RECORDED, rowValue(html, "Drug Allergies:"))
            assertFalse(html.contains("NKDA"))
        }
    }

    @Test
    fun q19b_allergyPresent_exactWithRedStyling() {
        val html = q19Html(allergy = "Penicillin rash")
        assertEquals("Penicillin rash", rowValue(html, "Drug Allergies:"))
        assertTrue(html.contains("""<span class="v text-rose font-bold">Penicillin rash</span>"""))
        assertFalse(html.contains("NKDA"))
    }

    @Test
    fun q19b_nkdaTypedByClinician_isPrinted() {
        assertEquals("NKDA", rowValue(q19Html(allergy = "NKDA"), "Drug Allergies:"))
    }

    @Test
    fun q19b_nkda_isNeutralNotAlertStyled() {
        val html = q19Html(allergy = "NKDA")
        assertTrue(html.contains("""<span class="v ">NKDA</span>"""))
        assertFalse(html.contains("""text-rose font-bold">NKDA"""))
    }

    @Test
    fun q19b_allergyMixedWithNkda_keepsAlertStyle() {
        assertTrue(q19Html(allergy = "NKDA but sulfa rash").contains("""text-rose font-bold">NKDA but sulfa rash"""))
    }

    // (c) medical / surgical history
    @Test
    fun q19c_historyMissing_notRecorded_noNone() {
        for (v in MISSING) {
            val html = q19Html(medical = v, surgical = v)
            assertEquals("'$v'", NOT_RECORDED, rowValue(html, "Medical History:"))
            assertEquals("'$v'", NOT_RECORDED, rowValue(html, "Surgical History:"))
            assertFalse(html.contains(">None<"))
        }
    }

    @Test
    fun q19c_historyPresent_exact() {
        val html = q19Html(medical = "Hypothyroidism", surgical = "Appendectomy 2015")
        assertEquals("Hypothyroidism", rowValue(html, "Medical History:"))
        assertEquals("Appendectomy 2015", rowValue(html, "Surgical History:"))
    }

    // (d) target delivery timing
    @Test
    fun q19d_agreedEddMissing_notRecorded() {
        for (v in MISSING) {
            val html = q19Html(dating = dating(v))
            assertEquals("'$v'", NOT_RECORDED, rowValue(html, "Target Delivery Timing:"))
            assertFalse(html.contains("At 39 completed weeks"))
        }
        val noDating = q19Html(dating = null)
        assertEquals(NOT_RECORDED, rowValue(noDating, "Target Delivery Timing:"))
        assertFalse(noDating.contains("At 39 completed weeks"))
    }

    @Test
    fun q19d_agreedEddPresent_printed() {
        assertEquals("08/11/2026", rowValue(q19Html(dating = dating("2026-11-08")), "Target Delivery Timing:"))
    }

    // (e) risk level missing
    @Test
    fun q19e_riskMissing_riskNotRecordedBadgeOnly() {
        for (v in MISSING) {
            val html = q19Html(risk = v)
            assertTrue("'$v'", html.contains("""<span class="badge">Risk not recorded</span>"""))
            assertFalse(html.contains(HIGH_RISK_BADGE))
            assertFalse(html.contains(NORMAL_RISK_BADGE))
        }
    }

    @Test
    fun q19e_riskPresent_noRiskNotRecordedBadge() {
        for (v in listOf("High Risk", "Low Risk", "Moderate Risk")) {
            assertFalse(q19Html(risk = v).contains("Risk not recorded"))
        }
    }

    // (f) planned delivery mode comes from the parsed Delivery plan
    @Test
    fun q19f_plannedDeliveryMode_isParsedPlanExactly() {
        val notes = "Elective repeat caesarean booked at 39 weeks"
        val expected = parseDeliveryNotes(notes).plan
        assertTrue("fixture must yield a plan", expected != null)
        val html = q19Html(notes = notes)
        assertEquals(expected, rowValue(html, "Planned Delivery Mode:"))
        assertFalse(html.contains(PLAN_SENTENCE))
    }

    @Test
    fun q19f_noPlan_notRecorded() {
        for (v in MISSING) {
            assertTrue(parseDeliveryNotes(v).plan == null)
            val html = q19Html(notes = v)
            assertEquals("'$v'", NOT_RECORDED, rowValue(html, "Planned Delivery Mode:"))
            assertFalse(html.contains(PLAN_SENTENCE))
        }
    }

    // (g) owner wording unchanged
    @Test
    fun q19g_ownerWordingUnchanged() {
        assertTrue(htmlFor("Low Risk", "O-").contains(ANTI_D_LINE))
        assertTrue(htmlFor("Low Risk", "O+").contains(ROUTINE_LINE))
    }

    // =====================================================================
    // Round 3a-2 / Q19(h)-(k): no invented table text.
    // =====================================================================

    private fun htmlWith(p: (DemoPatient) -> DemoPatient): String =
        PatientPrintHtmlGenerator.generateHtml(context = null, patient = p(patient("Low Risk", "O+")), asOfDate = AS_OF_DATE)

    private fun visit(findings: String?, notes: String?) = com.sherifradwan.ancnative.model.DemoVisit(
        id = "q19h", date = "2026-09-01", bloodPressure = null, weight = null,
        findings = findings, notes = notes, revision = 1, isSyntheticEdit = false,
    )

    private fun prevPreg(place: String?, other: String?) = com.sherifradwan.ancnative.model.DemoPreviousPregnancy(
        id = "q19i", patientId = "r2e-print-1", modelVersion = 1, dateText = "2020", outcomeText = null,
        pregnancyType = null, otherDetails = other, placeAndDoctor = place, deliveryBy = null, outcome = "LiveBirth",
        birthMethod = null, lossManagement = null, terminationMethod = null, provenanceKind = "t",
        clinicianId = "c", recordedAt = "2026-01-01", updatedAt = "2026-01-01", revision = 1,
    )

    private fun med(notes: String?) = com.sherifradwan.ancnative.model.DemoMedicationRecord(
        id = "q19j", patientId = "r2e-print-1", name = "Folic acid", dosage = "5 mg", route = "PO",
        frequency = "OD", startDate = "2026-01-01", endDate = null, status = "active", notes = notes,
        isProphylaxis = false,
    )

    private fun scanWithImage(impression: String?) = scan(null, null, null).copy(
        impression = impression, imageUris = listOf("content://x/1"),
    )

    @Test
    fun medicationPrintsNameThenOneInstructionsLineWithNothingInvented() {
        val full = htmlWith { it.copy(medications = listOf(med("1 tab twice daily"))) }
        assertTrue(full.contains("""<span class="text-muted med-instructions">5 mg · PO · 1 tab twice daily</span>"""))
        for (v in MISSING) {
            val html = htmlWith { it.copy(medications = listOf(med(v).copy(dosage = "", route = ""))) }
            assertFalse("'$v'", html.contains("med-instructions"))
            assertFalse("'$v'", html.contains("Once daily"))
            assertTrue("'$v'", html.contains("Folic acid"))
        }
    }

    @Test
    fun q19h_visitNotesMissing_notRecorded_noInventedCourse() {
        for (v in MISSING) {
            val html = htmlWith { it.copy(visits = listOf(visit(v, v))) }
            assertTrue("'$v'", html.contains("""<td class="text-center notes-cell">$NOT_RECORDED</td>"""))
            assertFalse("'$v'", html.contains("Antenatal course progressing favorably"))
        }
    }

    @Test
    fun q19h_visitFindingsThenNotes_printed() {
        val a = htmlWith { it.copy(visits = listOf(visit("Fundal height ok", "side note"))) }
        assertTrue(a.contains("""<td class="text-center notes-cell">Fundal height ok</td>"""))
        assertFalse(a.contains("side note"))
        val b = htmlWith { it.copy(visits = listOf(visit(null, "side note"))) }
        assertTrue(b.contains("""<td class="text-center notes-cell">side note</td>"""))
        val c = htmlWith { it.copy(visits = listOf(visit("  ", "side note"))) }
        assertTrue(c.contains("""<td class="text-center notes-cell">side note</td>"""))
        val typed = htmlWith { it.copy(visits = listOf(visit("Antenatal course progressing favorably", null))) }
        assertTrue(typed.contains("Antenatal course progressing favorably"))
    }

    @Test
    fun q19i_prevPregnancyDetailsMissing_notRecorded_noUncomplicated() {
        for (v in MISSING) {
            val html = htmlWith { it.copy(previousPregnancies = listOf(prevPreg(v, v))) }
            assertTrue("'$v'", html.contains("""<td class="text-center">$NOT_RECORDED</td>"""))
            assertFalse("'$v'", html.contains("Uncomplicated"))
        }
    }

    @Test
    fun q19i_prevPregnancyPlaceThenOther_printed() {
        val a = htmlWith { it.copy(previousPregnancies = listOf(prevPreg("City Hospital, Dr A", "other"))) }
        assertTrue(a.contains("""<td class="text-center">City Hospital, Dr A</td>"""))
        assertFalse(a.contains("""<td class="text-center">other</td>"""))
        val b = htmlWith { it.copy(previousPregnancies = listOf(prevPreg(" ", "other"))) }
        assertTrue(b.contains("""<td class="text-center">other</td>"""))
        val typed = htmlWith { it.copy(previousPregnancies = listOf(prevPreg("Uncomplicated", null))) }
        assertTrue(typed.contains("""<td class="text-center">Uncomplicated</td>"""))
    }

    @Test
    fun q19k_imageLabelNoImpression_noParensNoInvention() {
        for (v in MISSING) {
            val html = htmlWith { it.copy(ultrasoundScans = listOf(scanWithImage(v))) }
            assertTrue("'$v'", html.contains("""<div class="photo-caption">Ultrasound Scan 1.1 - 2026-09-01</div>"""))
            assertFalse("'$v'", html.contains("Fetal Growth Surveillance"))
        }
    }

    @Test
    fun q19k_imageLabelWithImpression_unchanged() {
        val html = htmlWith { it.copy(ultrasoundScans = listOf(scanWithImage("Normal growth"))) }
        assertTrue(html.contains("""<div class="photo-caption">Ultrasound Scan 1.1 - 2026-09-01 (Normal growth)</div>"""))
    }

    // =====================================================================
    // Dating summary in the printed report (overview_dating_spec.md U6).
    // Assertions read the text of the "Pregnancy Dating" box only, with tags
    // stripped and non-breaking spaces folded, so markup stays the builder's choice.
    // =====================================================================

    private fun datingBoxText(html: String): String {
        val start = html.indexOf("<span>Pregnancy Dating</span>")
        assertTrue("Pregnancy Dating title must be printed", start >= 0)
        val rest = html.substring(start + 1)
        val end = rest.indexOf("""class="section-title"""").let { if (it < 0) rest.length else it }
        return rest.substring(0, end)
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
    }

    private fun printDating(d: DemoPatientDating, scanDate: String? = "2026-08-27"): String =
        PatientPrintHtmlGenerator.generateHtml(
            context = null,
            patient = patient("Low Risk", "O+").copy(
                dating = d,
                ultrasoundScans = listOfNotNull(scanDate?.let { scan(null, null, null).copy(date = it) }),
            ),
            asOfDate = AS_OF_DATE,
        )

    private fun printDatingFields(
        lmp: String? = null,
        us: String? = null,
        agreed: String? = null,
        method: String? = null,
        transfer: String? = null,
        embryoAge: Int? = null,
    ) = DemoPatientDating(
        patientId = "r2e-print-1", lmpDate = lmp, ultrasoundEdd = us, agreedEdd = agreed,
        datingMethod = method, notes = null, transferDate = transfer, embryoAgeDays = embryoAge,
    )

    @Test
    fun dating_lmpPatient_printsLmpRowDmyAndEdd() {
        val html = printDating(printDatingFields(lmp = "2026-06-08", agreed = "2027-03-15", method = "LMP"))
        val box = datingBoxText(html)
        assertTrue(box, Regex("""LMP:? 08/06/2026""").containsMatchIn(box))
        assertTrue(box, Regex("""EDD:? 15/03/2027""").containsMatchIn(box))
        assertFalse(box, box.contains("2026-06-08"))
        assertFalse(box, box.contains("Confirmed EDD"))
        assertFalse(box, Regex("""\bET\b""").containsMatchIn(box))
    }

    @Test
    fun dating_ivfTransferPatient_printsEtWithTransferDateAndNeverLmp() {
        val html = printDating(
            printDatingFields(lmp = "2026-06-01", agreed = "2027-03-08", method = "IVF_TRANSFER", transfer = "2026-06-20", embryoAge = 5),
        )
        val box = datingBoxText(html)
        assertTrue(box, Regex("""ET:? 20/06/2026""").containsMatchIn(box))
        assertFalse(box, Regex("""\bLMP\b""").containsMatchIn(box))
        assertFalse(box, box.contains("01/06/2026"))
        assertTrue(box, Regex("""EDD:? 08/03/2027""").containsMatchIn(box))
    }

    @Test
    fun dating_methodPrintsAsLabelNeverTheRawCode() {
        val ivf = printDating(
            printDatingFields(agreed = "2027-03-08", method = "IVF_TRANSFER", transfer = "2026-06-20", embryoAge = 5),
        )
        assertTrue(Regex("""Method:? ET\b""").containsMatchIn(datingBoxText(ivf)))
        assertFalse(ivf.contains("IVF_TRANSFER"))

        val us = printDating(printDatingFields(lmp = "2026-06-08", us = "2027-03-24", agreed = "2027-03-24", method = "Ultrasound"))
        assertTrue(Regex("""Method:? by ultrasound""").containsMatchIn(datingBoxText(us)))

        val clinical = printDating(printDatingFields(agreed = "2027-04-02", method = "Clinical"))
        assertTrue(Regex("""Method:? clinical""").containsMatchIn(datingBoxText(clinical)))
    }

    @Test
    fun dating_discordance_printsBothDueDatesAndGap() {
        val html = printDating(
            printDatingFields(lmp = "2026-06-08", us = "2027-03-24", agreed = "2027-03-15", method = "LMP"),
        )
        val box = datingBoxText(html)
        assertTrue(box, box.contains("LMP EDD 15/03/2027"))
        assertTrue(box, box.contains("US EDD 24/03/2027"))
        assertTrue(box, box.contains("gap 9 d"))
        assertFalse(box, box.contains("2027-03-24"))
    }

    @Test
    fun dating_noDiscordance_scanEddLineAbsent() {
        val html = printDating(
            printDatingFields(lmp = "2026-06-08", us = "2027-03-18", agreed = "2027-03-15", method = "LMP"),
        )
        val box = datingBoxText(html)
        assertFalse(box, box.contains("US EDD"))
        assertFalse(box, box.contains("gap"))
        assertTrue(box, box.contains("15/03/2027"))
    }

    @Test
    fun dating_referredPatient_printsConfirmedEddAndNoLmpOrEtRow() {
        val html = printDating(printDatingFields(agreed = "2027-04-02", method = "Clinical"), scanDate = null)
        val box = datingBoxText(html)
        assertTrue(box, Regex("""Confirmed EDD:? 02/04/2027""").containsMatchIn(box))
        assertFalse(box, Regex("""\bLMP\b""").containsMatchIn(box))
        assertFalse(box, Regex("""\bET\b""").containsMatchIn(box))
    }

    @Test
    fun dating_plainEdd_isNotLabelledConfirmed() {
        val html = printDating(printDatingFields(lmp = "2026-06-08", method = "LMP"), scanDate = null)
        assertFalse(datingBoxText(html).contains("Confirmed EDD"))
    }

    // D5 and D7 in print: the scan due date and the gap only appear for a discordant non-IVF, non-Ultrasound patient.
    @Test
    fun dating_ivfPatientNeverPrintsScanEddOrGap() {
        for (m in listOf("IVF_TRANSFER", "IVF")) {
            val html = printDating(
                printDatingFields(
                    lmp = "2026-06-08", us = "2027-04-09", agreed = "2027-03-08",
                    method = m, transfer = "2026-06-20", embryoAge = 5,
                ),
            )
            val box = datingBoxText(html)
            assertFalse("$m: $box", box.contains("US EDD"))
            assertFalse("$m: $box", box.contains("gap"))
        }
    }

    @Test
    fun dating_ultrasoundMethodNeverPrintsScanEddOrGap() {
        for (agreed in listOf(null, "2027-03-15")) {
            val html = printDating(
                printDatingFields(lmp = "2026-06-08", us = "2027-04-09", agreed = agreed, method = "Ultrasound"),
            )
            val box = datingBoxText(html)
            assertFalse("agreed $agreed: $box", box.contains("US EDD"))
            assertFalse("agreed $agreed: $box", box.contains("gap"))
        }
    }

    @Test
    fun dating_scanEddEarlierThanLmpEdd_printsPositiveGap() {
        val html = printDating(
            printDatingFields(lmp = "2026-06-08", us = "2027-03-06", agreed = "2027-03-15", method = "LMP"),
        )
        val box = datingBoxText(html)
        assertTrue(box, box.contains("LMP EDD 15/03/2027"))
        assertTrue(box, box.contains("US EDD 06/03/2027"))
        assertTrue(box, box.contains("gap 9 d"))
        assertFalse(box, box.contains("gap -"))
    }

    // U9: "Target Delivery Timing" is the shared EDD in dd/mm/yyyy.
    @Test
    fun targetDeliveryTiming_lmpOnlyPatientShowsTheLmpEddWhateverTheAgreedEddIsMissingAs() {
        for (agreed in MISSING) {
            val html = printDating(printDatingFields(lmp = "2026-06-08", agreed = agreed, method = "LMP"), scanDate = null)
            assertEquals("agreed '$agreed'", "15/03/2027", rowValue(html, "Target Delivery Timing:"))
        }
    }

    @Test
    fun targetDeliveryTiming_malformedAgreedEddFallsBackToTheMethodEdd() {
        val html = printDating(printDatingFields(lmp = "2026-06-08", agreed = "garbage", method = "LMP"), scanDate = null)
        assertEquals("15/03/2027", rowValue(html, "Target Delivery Timing:"))
    }

    @Test
    fun targetDeliveryTiming_agreedEddWinsAndIsNeverRawIso() {
        val html = printDating(printDatingFields(lmp = "2026-06-08", agreed = "2027-03-20", method = "LMP"), scanDate = null)
        assertEquals("20/03/2027", rowValue(html, "Target Delivery Timing:"))
        assertFalse(html.contains("2027-03-20"))
    }

    @Test
    fun targetDeliveryTiming_scanOnlyDatingShowsTheScanEdd() {
        val html = printDating(printDatingFields(us = "2027-03-24"), scanDate = null)
        assertEquals("24/03/2027", rowValue(html, "Target Delivery Timing:"))
    }

    @Test
    fun targetDeliveryTiming_ivfPatientShowsTheTransferEdd() {
        val html = printDating(
            printDatingFields(method = "IVF_TRANSFER", transfer = "2026-06-20", embryoAge = 5),
            scanDate = null,
        )
        assertEquals("08/03/2027", rowValue(html, "Target Delivery Timing:"))
    }

    @Test
    fun targetDeliveryTiming_noEddAnywhereIsNotRecorded() {
        val html = printDating(printDatingFields(agreed = "garbage", method = "LMP"), scanDate = null)
        assertEquals(NOT_RECORDED, rowValue(html, "Target Delivery Timing:"))
    }
}
