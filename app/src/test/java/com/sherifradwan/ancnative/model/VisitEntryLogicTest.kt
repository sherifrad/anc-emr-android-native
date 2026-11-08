package com.sherifradwan.ancnative.model

import com.sherifradwan.ancnative.model.codex.CustomChoiceList
import com.sherifradwan.ancnative.state.DraftValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitEntryLogicTest {

    // -------------------------------------------------------------
    // Oracle O1: Blood Pressure Flagging & Invariance
    // -------------------------------------------------------------

    @Test
    fun bpFlagging_evaluatesCorrectlyAtAndAboveThresholds() {
        // Normal ranges
        assertFalse(isHighBloodPressure(110, 70))
        assertFalse(isHighBloodPressure(120, 80))
        assertFalse(isHighBloodPressure(139, 89))

        // Systolic threshold (>= 140)
        assertTrue(isHighBloodPressure(140, 70))
        assertTrue(isHighBloodPressure(145, 85))

        // Diastolic threshold (>= 90)
        assertTrue(isHighBloodPressure(120, 90))
        assertTrue(isHighBloodPressure(130, 95))

        // Both elevated
        assertTrue(isHighBloodPressure(150, 95))
        assertTrue(isHighBloodPressure(160, 110))

        // Single-sided elevated (SF-1)
        assertTrue(isHighBloodPressure(185, null))
        assertTrue(isHighBloodPressure(null, 105))
        assertFalse(isHighBloodPressure(120, null))
        assertFalse(isHighBloodPressure(null, 75))

        // String overload
        assertFalse(isHighBloodPressure("118/76"))
        assertFalse(isHighBloodPressure("139/89"))
        assertTrue(isHighBloodPressure("140/80"))
        assertTrue(isHighBloodPressure("120/90"))
        assertTrue(isHighBloodPressure("150/95"))
        assertFalse(isHighBloodPressure(null))
        assertFalse(isHighBloodPressure(""))
    }

    // -------------------------------------------------------------
    // Oracle O2: Normal Findings Fill Rules (Strict Isolation)
    // -------------------------------------------------------------

    @Test
    fun normalFindingsFill_fillsOnlyBlankFindings_andNeverTouchesVitals() {
        val initial = VisitFormValues(
            date = "2026-11-01",
            systolic = "150",
            diastolic = "95",
            weight = "76.5",
            fundalHeight = "30",
            oedema = "",
            fetalMovements = "",
            urineProtein = "",
            notes = "Doctor note",
        )

        val result = fillNormalVisitFindings(initial)

        // Count of modified fields is 3
        assertEquals(3, result.filledCount)

        // Filled values
        assertEquals("None", result.values.oedema)
        assertEquals("Good", result.values.fetalMovements)
        assertEquals("Negative", result.values.urineProtein)

        // Vitals isolation: strictly unchanged!
        assertEquals("150", result.values.systolic)
        assertEquals("95", result.values.diastolic)
        assertEquals("76.5", result.values.weight)
        assertEquals("30", result.values.fundalHeight)
        assertEquals("2026-11-01", result.values.date)
        assertEquals("Doctor note", result.values.notes)
    }

    @Test
    fun normalFindingsFill_withDraftValues_provesVitalsStrictlyUntouched() {
        // G-4: Verifies against DraftValues model directly
        val draft = DraftValues(
            date = "2026-11-01",
            bloodPressure = "150/95",
            systolic = "150",
            diastolic = "95",
            weight = "76.5",
            fundalHeight = "30",
            oedema = "",
            fetalMovements = "",
            urineProtein = "",
            notes = "Doctor note",
        )

        val formValues = VisitFormValues(
            date = draft.date,
            systolic = draft.systolic,
            diastolic = draft.diastolic,
            weight = draft.weight,
            fundalHeight = draft.fundalHeight,
            oedema = draft.oedema,
            fetalMovements = draft.fetalMovements,
            urineProtein = draft.urineProtein,
            notes = draft.notes,
        )

        val result = fillNormalVisitFindings(formValues)
        val updatedDraft = draft.copy(
            oedema = result.values.oedema,
            fetalMovements = result.values.fetalMovements,
            urineProtein = result.values.urineProtein,
        )

        assertEquals(3, result.filledCount)
        assertEquals("150/95", updatedDraft.bloodPressure)
        assertEquals("150", updatedDraft.systolic)
        assertEquals("95", updatedDraft.diastolic)
        assertEquals("76.5", updatedDraft.weight)
        assertEquals("30", updatedDraft.fundalHeight)
        assertEquals("2026-11-01", updatedDraft.date)
        assertEquals("Doctor note", updatedDraft.notes)
    }

    @Test
    fun normalFindingsFill_preservesExistingValuesWithoutOverwriting() {
        val initial = VisitFormValues(
            oedema = "Mild",
            fetalMovements = "Reduced",
            urineProtein = "+",
            systolic = "120",
            diastolic = "80",
            weight = "70.0",
        )

        val result = fillNormalVisitFindings(initial)

        assertEquals(0, result.filledCount)
        assertEquals("Mild", result.values.oedema)
        assertEquals("Reduced", result.values.fetalMovements)
        assertEquals("+", result.values.urineProtein)
    }

    @Test
    fun normalFindingsFill_partialFillOnlyPopulatesBlanks() {
        val initial = VisitFormValues(
            oedema = "Marked",
            fetalMovements = "",
            urineProtein = "",
        )

        val result = fillNormalVisitFindings(initial)

        assertEquals(2, result.filledCount)
        assertEquals("Marked", result.values.oedema)
        assertEquals("Good", result.values.fetalMovements)
        assertEquals("Negative", result.values.urineProtein)
    }

    // -------------------------------------------------------------
    // Oracle O3: "Same as Last" Historical Extraction
    // -------------------------------------------------------------

    @Test
    fun sameAsLast_extractsLatestPriorVisitVitals() {
        val v1 = DemoVisit(
            id = "v1",
            date = "2026-08-01",
            bloodPressure = "110/70",
            weight = "65.0 kg",
            findings = "FH 20 cm · Oedema none · Movements good · Urine negative",
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )
        val v2 = DemoVisit(
            id = "v2",
            date = "2026-09-01",
            bloodPressure = "118/76",
            weight = "68.5 kg",
            findings = "FH 24 cm · Oedema none · Movements good · Urine negative",
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )
        val v3 = DemoVisit(
            id = "v3",
            date = "2026-10-01",
            bloodPressure = "122/78",
            weight = "72.0 kg",
            findings = "FH 28 cm · Oedema mild · Movements good · Urine trace",
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )

        // For a new visit (no exclude id), returns v3 vitals
        val lastForNew = resolveLastVisitVitals(listOf(v1, v2, v3), excludeVisitId = null)
        assertEquals("122/78", lastForNew.bloodPressure)
        assertEquals("122", lastForNew.systolic)
        assertEquals("78", lastForNew.diastolic)
        assertEquals("72.0", lastForNew.weightKg)
        assertEquals("28", lastForNew.fundalHeightCm)

        // When editing v3, excludes v3 and returns v2 vitals
        val lastForEdit = resolveLastVisitVitals(listOf(v1, v2, v3), excludeVisitId = "v3")
        assertEquals("118/76", lastForEdit.bloodPressure)
        assertEquals("118", lastForEdit.systolic)
        assertEquals("76", lastForEdit.diastolic)
        assertEquals("68.5", lastForEdit.weightKg)
        assertEquals("24", lastForEdit.fundalHeightCm)

        // When list is empty, returns empty/null safely
        val emptyLast = resolveLastVisitVitals(emptyList(), excludeVisitId = null)
        assertNull(emptyLast.bloodPressure)
        assertNull(emptyLast.systolic)
        assertNull(emptyLast.diastolic)
        assertNull(emptyLast.weightKg)
        assertNull(emptyLast.fundalHeightCm)
    }

    // -------------------------------------------------------------
    // Oracle O4: "Matches GA" Fundal Height Estimation
    // -------------------------------------------------------------

    @Test
    fun matchesGa_estimatesFundalHeightWithinPlausibleRange() {
        assertEquals(28, estimateFundalHeightForGa(28))
        assertEquals(34, estimateFundalHeightForGa(34))
        assertEquals(20, estimateFundalHeightForGa(20))
        assertEquals(10, estimateFundalHeightForGa(10))
        assertEquals(50, estimateFundalHeightForGa(50))

        // Outside plausible range (10..50 cm)
        assertNull(estimateFundalHeightForGa(8))
        assertNull(estimateFundalHeightForGa(52))
        assertNull(estimateFundalHeightForGa(null))
    }

    // -------------------------------------------------------------
    // Oracle O5: Structured Findings Serialization & Round-trip
    // -------------------------------------------------------------

    @Test
    fun findings_serializationAndRoundTripParsing() {
        val original = VisitFormValues(
            fundalHeight = "28",
            oedema = "Mild",
            fetalMovements = "Good",
            urineProtein = "Trace",
        )

        val serialized = buildVisitFindings(
            fundalHeight = original.fundalHeight,
            oedema = original.oedema,
            fetalMovements = original.fetalMovements,
            urineProtein = original.urineProtein,
            additionalFindings = "Cephalic presentation",
        )

        val parsed = parseVisitFindings(serialized)
        assertEquals("28", parsed.fundalHeight)
        assertEquals("Mild", parsed.oedema)
        assertEquals("Good", parsed.fetalMovements)
        assertEquals("Trace", parsed.urineProtein)
        assertEquals("Cephalic presentation", parsed.additionalFindings)
    }

    @Test
    fun findings_adversarialCases_tokenExactAndCommaPreservation() {
        // G-3: Comma inside free text must NOT be split into separate tokens
        val serializedWithComma = buildVisitFindings(
            fundalHeight = "30",
            oedema = "None",
            fetalMovements = "Good",
            urineProtein = "Negative",
            additionalFindings = "Cephalic presentation, intact perineum",
        )
        val parsedComma = parseVisitFindings(serializedWithComma)
        assertEquals("30", parsedComma.fundalHeight)
        assertEquals("None", parsedComma.oedema)
        assertEquals("Good", parsedComma.fetalMovements)
        assertEquals("Negative", parsedComma.urineProtein)
        assertEquals("Cephalic presentation, intact perineum", parsedComma.additionalFindings)

        // Structured token takes precedence over contradictory legacy phrase in free-text
        val contradictoryText = "FH 28 cm · Oedema marked · Movements good · Urine negative · no edema previously"
        val parsedContradiction = parseVisitFindings(contradictoryText)
        assertEquals("28", parsedContradiction.fundalHeight)
        assertEquals("Marked", parsedContradiction.oedema) // NOT "None"
        assertEquals("Good", parsedContradiction.fetalMovements)
        assertEquals("Negative", parsedContradiction.urineProtein)
        assertEquals("no edema previously", parsedContradiction.additionalFindings)

        // Free text with words like 'culture' or 'assessment' must not be absorbed into clinical fields
        val clinicNotes = "FH 28 cm · Oedema none · Movements good · Urine negative · Urine culture sent, oedema assessment pending"
        val parsedNotes = parseVisitFindings(clinicNotes)
        assertEquals("28", parsedNotes.fundalHeight)
        assertEquals("None", parsedNotes.oedema)
        assertEquals("Good", parsedNotes.fetalMovements)
        assertEquals("Negative", parsedNotes.urineProtein)
        assertEquals("Urine culture sent, oedema assessment pending", parsedNotes.additionalFindings)

        // 3-digit out-of-range FH (e.g. FH 100 cm) is rejected and not truncated to 10
        val outOfRangeFh = "FH 100 cm · Oedema none · Movements good · Urine negative"
        val parsedOutOfRange = parseVisitFindings(outOfRangeFh)
        assertNull(parsedOutOfRange.fundalHeight)
        assertEquals("None", parsedOutOfRange.oedema)
    }

    @Test
    fun findings_parsesLegacyFreeTextWithoutStructuredPrefix() {
        val legacy = "Fundal height 24 cm, no edema, urine dipstick negative."
        val parsed = parseVisitFindings(legacy)
        assertEquals("24", parsed.fundalHeight)
        assertEquals("None", parsed.oedema)
        assertEquals("Negative", parsed.urineProtein)
    }

    // -------------------------------------------------------------
    // Oracle O6: Flagged Findings Detection
    // -------------------------------------------------------------

    @Test
    fun flaggedFindings_detectsHighBpAndAbnormalVitals() {
        // High BP + Marked oedema + Reduced movements + Proteinuria
        val flags1 = detectFlaggedFindings(
            systolic = "150",
            diastolic = "95",
            oedema = "Marked",
            fetalMovements = "Reduced",
            urineProtein = "++",
        )
        assertEquals(listOf("High BP", "Oedema marked", "Movements reduced", "Urine ++"), flags1)

        // Normal vitals -> nothing flagged
        val flagsNormal = detectFlaggedFindings(
            systolic = "118",
            diastolic = "76",
            oedema = "None",
            fetalMovements = "Good",
            urineProtein = "Negative",
        )
        assertTrue(flagsNormal.isEmpty())

        // High systolic only (SF-1)
        val flagsSysOnly = detectFlaggedFindings(
            systolic = "185",
            diastolic = "",
            oedema = "None",
            fetalMovements = "Good",
            urineProtein = "Negative",
        )
        assertEquals(listOf("High BP"), flagsSysOnly)

        // High diastolic only
        val flagsDiaOnly = detectFlaggedFindings(
            systolic = "125",
            diastolic = "92",
            oedema = "None",
            fetalMovements = "Good",
            urineProtein = "Negative",
        )
        assertEquals(listOf("High BP"), flagsDiaOnly)

        // Mild oedema is also flagged (SF-3)
        val flagsMild = detectFlaggedFindings(
            systolic = "120",
            diastolic = "80",
            oedema = "Mild",
            fetalMovements = "Good",
            urineProtein = "Negative",
        )
        assertEquals(listOf("Oedema mild"), flagsMild)

        // Trace urine is flagged (SF-3)
        val flagsTrace = detectFlaggedFindings(
            systolic = "120",
            diastolic = "80",
            oedema = "None",
            fetalMovements = "Good",
            urineProtein = "Trace",
        )
        assertEquals(listOf("Urine trace"), flagsTrace)
    }

    // -------------------------------------------------------------
    // Oracle O7: Summary Formatter Matches Mock Specification
    // -------------------------------------------------------------

    @Test
    fun summaryFormatter_formatsExpectedOneLineSummary() {
        val summary = formatVisitSummary(
            dateText = "1 Nov 2026",
            gaText = "28w 0d",
            systolic = "118",
            diastolic = "76",
            weightKg = "74.5",
            fundalHeightCm = "27",
            oedema = "None",
            fetalMovements = "Good",
            urineProtein = "Negative",
        )
        assertEquals(
            "1 Nov 2026 · 28w 0d · BP 118/76 · Wt 74.5 kg · FH 27 cm · Oedema none · Movements good · Urine negative",
            summary,
        )

        // SF-6: partial BP formats with placeholder rather than disappearing
        val partialSummary = formatVisitSummary(
            dateText = "1 Nov 2026",
            gaText = "28w 0d",
            systolic = "150",
            diastolic = "",
            weightKg = "74.5",
            fundalHeightCm = "27",
            oedema = "None",
            fetalMovements = "Good",
            urineProtein = "Negative",
        )
        assertTrue(partialSummary.contains("BP 150/—"))
    }

    // -------------------------------------------------------------
    // Oracle O8: Custom Choices Lists for Visit Entry
    // -------------------------------------------------------------

    @Test
    fun customChoiceList_includesVisitEnums_andPreservesDatingInvariance() {
        assertTrue(CustomChoiceList.entries.contains(CustomChoiceList.VISIT_OEDEMA))
        assertTrue(CustomChoiceList.entries.contains(CustomChoiceList.VISIT_MOVEMENTS))
        assertTrue(CustomChoiceList.entries.contains(CustomChoiceList.VISIT_URINE))
        assertTrue(CustomChoiceList.entries.contains(CustomChoiceList.VISIT_BP))
        assertTrue(CustomChoiceList.entries.contains(CustomChoiceList.VISIT_WEIGHT))
        assertTrue(CustomChoiceList.entries.contains(CustomChoiceList.VISIT_FUNDAL_HEIGHT))

        // Clinical safety: dating methods must never be in custom choice lists
        val names = CustomChoiceList.entries.map { it.name }
        assertFalse(names.any { it.contains("DATING", ignoreCase = true) })
        assertFalse(names.any { it.contains("LMP", ignoreCase = true) })
        assertFalse(names.any { it.contains("IVF", ignoreCase = true) })
    }

    // -------------------------------------------------------------
    // Oracle O9: Fetal Heart Rate (FHR) & Bedside Clinical Safety
    // -------------------------------------------------------------

    @Test
    fun fhrRoundTrip_serializesAndParsesCorrectly() {
        val serialized = buildVisitFindings(
            fundalHeight = "32",
            fetalHeartRate = "142",
            oedema = "None",
            fetalMovements = "Good",
            urineProtein = "Negative",
        )
        assertTrue(serialized.contains("FHR 142 bpm"))
        assertTrue(serialized.contains("FH 32 cm"))

        val parsed = parseVisitFindings(serialized)
        assertEquals("32", parsed.fundalHeight)
        assertEquals("142", parsed.fetalHeartRate)
        assertEquals("None", parsed.oedema)
        assertEquals("Good", parsed.fetalMovements)
        assertEquals("Negative", parsed.urineProtein)

        // Legacy narrative parsing test
        val legacyText = "Fundal height 30 cm, FHR 150 bpm, active fetal movements, no oedema, urine dipstick negative"
        val parsedLegacy = parseVisitFindings(legacyText)
        assertEquals("30", parsedLegacy.fundalHeight)
        assertEquals("150", parsedLegacy.fetalHeartRate)
        assertEquals("Good", parsedLegacy.fetalMovements)
        assertEquals("None", parsedLegacy.oedema)
        assertEquals("Negative", parsedLegacy.urineProtein)
    }

    @Test
    fun fhrClassification_identifiesNormalAndAbnormalRanges() {
        assertEquals(FhrLevel.NORMAL, fhrLevel(110))
        assertEquals(FhrLevel.NORMAL, fhrLevel(140))
        assertEquals(FhrLevel.NORMAL, fhrLevel(160))

        assertEquals(FhrLevel.BRADYCARDIA, fhrLevel(105))
        assertEquals(FhrLevel.BRADYCARDIA, fhrLevel(90))

        assertEquals(FhrLevel.TACHYCARDIA, fhrLevel(165))
        assertEquals(FhrLevel.TACHYCARDIA, fhrLevel(180))

        assertNull(fhrLevel(null as Int?))
        assertNull(fhrLevel(""))
    }

    @Test
    fun sfhConcordance_evaluatesDiscrepanciesCorrectly() {
        // Within +- 2 cm of GA in weeks (20..42 weeks)
        assertEquals(SfhConcordance.CONCORDANT, checkSfhConcordance(sfhCm = 32, gaWeeks = 32))
        assertEquals(SfhConcordance.CONCORDANT, checkSfhConcordance(sfhCm = 34, gaWeeks = 32))
        assertEquals(SfhConcordance.CONCORDANT, checkSfhConcordance(sfhCm = 30, gaWeeks = 32))

        // Large for dates (> +2 cm)
        assertEquals(SfhConcordance.DISCREPANT_LARGE, checkSfhConcordance(sfhCm = 36, gaWeeks = 32))

        // Small for dates (< -2 cm)
        assertEquals(SfhConcordance.DISCREPANT_SMALL, checkSfhConcordance(sfhCm = 28, gaWeeks = 32))

        // Out of range GA
        assertNull(checkSfhConcordance(sfhCm = 15, gaWeeks = 14))
    }

    @Test
    fun preEclampsiaWarning_detectsRiskWhenElevatedBpAndProteinPresent() {
        // High BP (140/90) + Protein (+) -> Risk
        assertTrue(isPreEclampsiaWarning(140, 90, "+"))
        assertTrue(isPreEclampsiaWarning(150, 95, "++"))
        assertTrue(isPreEclampsiaWarning(160, 100, "Trace"))

        // High BP without protein -> No pre-eclampsia flag (just high BP)
        assertFalse(isPreEclampsiaWarning(140, 90, "Negative"))
        assertFalse(isPreEclampsiaWarning(145, 95, "None"))

        // Normal BP with protein -> No pre-eclampsia flag
        assertFalse(isPreEclampsiaWarning(115, 75, "+"))
    }

    @Test
    fun normalFindingsFill_leavesFhrUntouched() {
        val form = VisitFormValues(
            date = "2026-11-08",
            systolic = "120",
            diastolic = "80",
            weight = "68",
            fundalHeight = "32",
            fetalHeartRate = "144",
            oedema = "",
            fetalMovements = "",
            urineProtein = "",
        )
        val result = fillNormalVisitFindings(form)
        assertEquals("144", result.values.fetalHeartRate)
        assertEquals("32", result.values.fundalHeight)
        assertEquals("None", result.values.oedema)
        assertEquals("Good", result.values.fetalMovements)
        assertEquals("Negative", result.values.urineProtein)
    }

    @Test
    fun formatVisitSummary_includesFhrWhenPresent() {
        val summary = formatVisitSummary(
            dateText = "8 Nov 2026",
            gaText = "32w 4d",
            systolic = "120",
            diastolic = "80",
            weightKg = "70.0",
            fundalHeightCm = "32",
            oedema = "None",
            fetalMovements = "Good",
            urineProtein = "Negative",
            fetalHeartRate = "142",
        )
        assertTrue(summary.contains("FHR 142 bpm"))
    }
}
