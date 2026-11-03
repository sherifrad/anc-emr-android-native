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
        assertEquals("2026-11-08", rowValue(q19Html(dating = dating("2026-11-08")), "Target Delivery Timing:"))
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
}
