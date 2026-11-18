package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.LabResultFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportDataTest {

    private fun patient(
        visits: List<DemoVisit> = emptyList(),
        scans: List<DemoUltrasoundScan> = emptyList(),
        labs: List<DemoLabInvestigation> = emptyList(),
        attachments: List<DemoAttachment> = emptyList(),
    ): DemoPatient = DemoPatient(
        id = "patient-1",
        caseId = "CASE-100",
        displayName = "Fatma Ahmed",
        dateOfBirth = "1995-04-12",
        phone = "01012345678",
        visits = visits,
        ultrasoundScans = scans,
        labInvestigations = labs,
        attachments = attachments,
    )

    private fun visit(
        id: String,
        date: String = "2026-02-01",
        linkedUltrasoundId: String? = null,
    ): DemoVisit = DemoVisit(
        id = id,
        date = date,
        bloodPressure = "120/80",
        weight = "65.0",
        findings = "Normal findings",
        notes = null,
        linkedUltrasoundId = linkedUltrasoundId,
    )

    private fun scan(
        id: String,
        date: String = "2026-02-01",
        linkedVisitId: String? = null,
        fetalNumber: String? = "Singleton",
        presentation: String? = "Cephalic",
        placentalSite: String? = "Anterior",
        amnioticFluid: String? = "Normal",
        fetalHeartRate: String? = "140 bpm",
        biometryBpd: String? = "45 mm",
        biometryHc: String? = "165 mm",
        biometryAc: String? = "150 mm",
        biometryFl: String? = "35 mm",
        estimatedFetalWeight: String? = "380 g",
        impression: String? = "Normal interval growth",
        imageUris: List<String> = emptyList(),
    ): DemoUltrasoundScan = DemoUltrasoundScan(
        id = id,
        patientId = "patient-1",
        date = date,
        gestationalAgeWeeks = 20,
        gestationalAgeDays = 3,
        fetalNumber = fetalNumber,
        presentation = presentation,
        placentalSite = placentalSite,
        amnioticFluid = amnioticFluid,
        fetalHeartRate = fetalHeartRate,
        biometryBpd = biometryBpd,
        biometryHc = biometryHc,
        biometryAc = biometryAc,
        biometryFl = biometryFl,
        estimatedFetalWeight = estimatedFetalWeight,
        impression = impression,
        linkedVisitId = linkedVisitId,
        imageUris = imageUris,
    )

    private fun lab(
        id: String,
        date: String,
        testCategory: String = "Hematology",
        testName: String,
        result: String,
        unit: String? = "g/dL",
        referenceRange: String? = "11.0 - 15.0",
        isAbnormal: Boolean = false,
    ): DemoLabInvestigation = DemoLabInvestigation(
        id = id,
        patientId = "patient-1",
        date = date,
        testCategory = testCategory,
        testName = testName,
        result = result,
        unit = unit,
        referenceRange = referenceRange,
        isAbnormal = isAbnormal,
        notes = null,
    )

    private fun attachment(
        id: String,
        capturedAt: String = "2026-02-01T10:00:00",
        linkedRecordId: String? = null,
        title: String? = null,
        fileName: String = "attachment.jpg",
        filePath: String = "/vault/attachment.jpg",
    ): DemoAttachment = DemoAttachment(
        id = id,
        patientId = "patient-1",
        fileName = fileName,
        filePath = filePath,
        category = "CLINICAL",
        fileSizeBytes = 1024L,
        sha256Hash = "hash-$id",
        title = title,
        capturedAt = capturedAt,
        linkedRecordId = linkedRecordId,
    )

    // --- RULE 1: Latest scan wins & link mechanisms ---

    @Test
    fun latestScan_whenTwoScansLinkToOneVisit_latestDateWins() {
        val targetVisit = visit(id = "v-1", date = "2026-03-01")
        val scanOld = scan(id = "scan-early", date = "2026-01-15", linkedVisitId = "v-1")
        val scanNew = scan(id = "scan-late", date = "2026-02-28", linkedVisitId = "v-1")
        val p = patient(visits = listOf(targetVisit), scans = listOf(scanOld, scanNew))

        val resolved = latestScanForVisit(p, targetVisit)
        assertNotNull(resolved)
        assertEquals("scan-late", resolved?.id)
    }

    @Test
    fun latestScan_whenTwoScansHaveSameDate_latestIdWins() {
        val targetVisit = visit(id = "v-1", date = "2026-03-01")
        val scanA = scan(id = "scan-1", date = "2026-02-01", linkedVisitId = "v-1")
        val scanB = scan(id = "scan-2", date = "2026-02-01", linkedVisitId = "v-1")
        val p = patient(visits = listOf(targetVisit), scans = listOf(scanA, scanB))

        val resolved = latestScanForVisit(p, targetVisit)
        assertEquals("scan-2", resolved?.id)
    }

    @Test
    fun latestScan_linkByScanLinkedVisitId() {
        val targetVisit = visit(id = "v-10", date = "2026-02-10", linkedUltrasoundId = null)
        val matchingScan = scan(id = "scan-10", date = "2026-02-10", linkedVisitId = "v-10")
        val p = patient(visits = listOf(targetVisit), scans = listOf(matchingScan))

        val resolved = latestScanForVisit(p, targetVisit)
        assertEquals("scan-10", resolved?.id)
    }

    @Test
    fun latestScan_linkByVisitLinkedUltrasoundId() {
        val targetVisit = visit(id = "v-20", date = "2026-02-20", linkedUltrasoundId = "scan-20")
        val matchingScan = scan(id = "scan-20", date = "2026-02-20", linkedVisitId = null)
        val p = patient(visits = listOf(targetVisit), scans = listOf(matchingScan))

        val resolved = latestScanForVisit(p, targetVisit)
        assertEquals("scan-20", resolved?.id)
    }

    @Test
    fun latestScan_linkByBothMechanisms_latestWins() {
        val targetVisit = visit(id = "v-30", date = "2026-03-01", linkedUltrasoundId = "scan-old-by-visit")
        val scanOld = scan(id = "scan-old-by-visit", date = "2026-01-01", linkedVisitId = null)
        val scanNew = scan(id = "scan-new-by-scan", date = "2026-02-15", linkedVisitId = "v-30")
        val p = patient(visits = listOf(targetVisit), scans = listOf(scanOld, scanNew))

        val resolved = latestScanForVisit(p, targetVisit)
        assertEquals("scan-new-by-scan", resolved?.id)
    }

    // --- RULE 2: Unlinked scans exclude linked ones & sort oldest first ---

    @Test
    fun unlinkedScans_excludesLinkedScans_andSortsOldestFirst() {
        val targetVisit = visit(id = "v-1", date = "2026-02-01")
        val linkedScan = scan(id = "s-linked", date = "2026-02-01", linkedVisitId = "v-1")
        val scanOld = scan(id = "s-old", date = "2026-01-10", linkedVisitId = null)
        val scanMid = scan(id = "s-mid", date = "2026-02-15", linkedVisitId = null)
        val scanNew = scan(id = "s-new", date = "2026-03-20", linkedVisitId = null)
        val p = patient(
            visits = listOf(targetVisit),
            scans = listOf(scanNew, linkedScan, scanOld, scanMid),
        )

        val unlinked = unlinkedScans(p, listOf(targetVisit))
        assertEquals(listOf("s-old", "s-mid", "s-new"), unlinked.map { it.id })
        assertFalse(unlinked.any { it.id == "s-linked" })
    }

    @Test
    fun unlinkedScans_whenTwoScansLinkToOneVisit_supersededScanIsIncludedInUnlinked() {
        val targetVisit = visit(id = "v-1", date = "2026-02-01")
        val olderLinked = scan(id = "s-older", date = "2026-01-01", linkedVisitId = "v-1")
        val newerLinked = scan(id = "s-newer", date = "2026-02-01", linkedVisitId = "v-1")
        val p = patient(visits = listOf(targetVisit), scans = listOf(olderLinked, newerLinked))

        val visitScan = latestScanForVisit(p, targetVisit)
        assertEquals("s-newer", visitScan?.id)

        val unlinked = unlinkedScans(p, listOf(targetVisit))
        assertEquals(listOf("s-older"), unlinked.map { it.id })
    }

    @Test
    fun unlinkedScans_whenScanLinkedToHiddenVisit_isIncludedInUnlinked() {
        val hiddenVisit = visit(id = "v-hidden", date = "2026-01-01")
        val shownVisit = visit(id = "v-shown", date = "2026-02-01")
        val scanLinkedToHidden = scan(id = "s-hidden-v", date = "2026-01-01", linkedVisitId = "v-hidden")
        val scanLinkedToShown = scan(id = "s-shown-v", date = "2026-02-01", linkedVisitId = "v-shown")
        val p = patient(
            visits = listOf(hiddenVisit, shownVisit),
            scans = listOf(scanLinkedToHidden, scanLinkedToShown),
        )

        val unlinked = unlinkedScans(p, shownVisits = listOf(shownVisit))
        assertEquals(listOf("s-hidden-v"), unlinked.map { it.id })
        assertFalse(unlinked.any { it.id == "s-shown-v" })
    }

    @Test
    fun unlinkedScans_whenScanLinkedToShownVisitIsNotLatest_isIncludedInUnlinked() {
        val shownVisit = visit(id = "v-shown", date = "2026-02-01")
        val olderScan = scan(id = "s-older", date = "2026-01-15", linkedVisitId = "v-shown")
        val latestScan = scan(id = "s-latest", date = "2026-02-01", linkedVisitId = "v-shown")
        val p = patient(visits = listOf(shownVisit), scans = listOf(olderScan, latestScan))

        val unlinked = unlinkedScans(p, shownVisits = listOf(shownVisit))
        assertEquals(listOf("s-older"), unlinked.map { it.id })
        assertFalse(unlinked.any { it.id == "s-latest" })
    }

    // --- RULE 3: Scan outcome formatting ---

    @Test
    fun scanOutcome_fetalNumberMapping() {
        val singletonScan = scan(id = "s-1", fetalNumber = "Singleton")
        assertEquals("Singleton", scanOutcomeFor(singletonScan).typeLabel)

        val singletonCaseScan = scan(id = "s-1b", fetalNumber = "singleton")
        assertEquals("Singleton", scanOutcomeFor(singletonCaseScan).typeLabel)

        val twinsScan = scan(id = "s-2", fetalNumber = "Twins")
        assertEquals("Multiple pregnancy: twins", scanOutcomeFor(twinsScan).typeLabel)

        val higherOrderScan = scan(id = "s-3", fetalNumber = "Higher order")
        assertEquals("Multiple pregnancy: higher order", scanOutcomeFor(higherOrderScan).typeLabel)

        val nullScan = scan(id = "s-4", fetalNumber = null)
        assertNull(scanOutcomeFor(nullScan).typeLabel)

        val blankScan = scan(id = "s-5", fetalNumber = "   ")
        assertNull(scanOutcomeFor(blankScan).typeLabel)
    }

    @Test
    fun scanOutcome_presentationAsStored() {
        val cephalic = scan(id = "s-1", presentation = "Cephalic")
        assertEquals("Cephalic", scanOutcomeFor(cephalic).presentation)

        val breech = scan(id = "s-2", presentation = "Breech")
        assertEquals("Breech", scanOutcomeFor(breech).presentation)

        val blank = scan(id = "s-3", presentation = "  ")
        assertNull(scanOutcomeFor(blank).presentation)
    }

    @Test
    fun scanOutcome_blankMeasurementsAreDropped() {
        val scanWithBlanks = scan(
            id = "s-1",
            biometryBpd = "45 mm",
            biometryHc = null,
            biometryAc = "   ",
            biometryFl = "32 mm",
            estimatedFetalWeight = null,
        )
        val outcome = scanOutcomeFor(scanWithBlanks)
        assertEquals(listOf("BPD" to "45 mm", "FL" to "32 mm"), outcome.measurements)
    }

    @Test
    fun scanOutcome_estimatedFetalWeightUnits() {
        val rawNumber = scan(id = "s-1", estimatedFetalWeight = "1500")
        assertEquals("1500 g", scanOutcomeFor(rawNumber).measurements.first { it.first == "EFW" }.second)

        val storedWithGram = scan(id = "s-2", estimatedFetalWeight = "1500 g")
        assertEquals("1500 g", scanOutcomeFor(storedWithGram).measurements.first { it.first == "EFW" }.second)

        val storedWithKg = scan(id = "s-3", estimatedFetalWeight = "1.5 kg")
        assertEquals("1.5 kg", scanOutcomeFor(storedWithKg).measurements.first { it.first == "EFW" }.second)
    }

    @Test
    fun scanOutcome_extras_onlyWhenPresentAndAmnioticFluidNotNormal() {
        val normalScan = scan(
            id = "s-norm",
            amnioticFluid = "Normal",
            placentalSite = null,
            fetalHeartRate = null,
            impression = null,
        )
        assertTrue(scanOutcomeFor(normalScan).extras.isEmpty())

        val abnormalFluidScan = scan(
            id = "s-fluid",
            amnioticFluid = "Oligohydramnios",
            placentalSite = "Posterior",
            fetalHeartRate = "142 bpm",
            impression = "Adequate interval growth",
        )
        assertEquals(
            listOf("Fluid: Oligohydramnios", "Placenta: Posterior", "FHR: 142 bpm", "Adequate interval growth"),
            scanOutcomeFor(abnormalFluidScan).extras,
        )

        val fluidCaseInsensitive = scan(
            id = "s-fluid-case",
            amnioticFluid = "normal",
            placentalSite = "Fundal",
            fetalHeartRate = null,
            impression = null,
        )
        assertEquals(listOf("Placenta: Fundal"), scanOutcomeFor(fluidCaseInsensitive).extras)
    }

    // --- RULE 4: Labs grouping, comparison vs singles, chunking, and flags ---

    @Test
    fun labReport_repeatedTestIsComparisonRow_singleDateIsSingle() {
        val p = patient(
            labs = listOf(
                lab("l-1", date = "2026-01-01", testName = "Hemoglobin", result = "12.0", unit = "g/dL"),
                lab("l-2", date = "2026-02-01", testName = "Hemoglobin", result = "11.5", unit = "g/dL"),
                lab("l-3", date = "2026-01-01", testName = "Blood Group", result = "A+", unit = null),
            ),
        )
        val report = labReport(p)

        assertEquals(1, report.comparisons.size)
        val compTable = report.comparisons.first()
        assertEquals(1, compTable.rows.size)
        assertEquals("Hemoglobin (g/dL)", compTable.rows.first().label)
        assertEquals(listOf("01/01/2026", "01/02/2026"), compTable.dates)

        assertEquals(1, report.singles.size)
        assertEquals("Blood Group", report.singles.first().test)
        assertEquals("01/01/2026", report.singles.first().date)
        assertEquals("A+", report.singles.first().result)
        assertNull(report.singles.first().unit)
    }

    @Test
    fun labReport_testOnSameDateTwice_isNotRepeated_goesToSingles() {
        val p = patient(
            labs = listOf(
                lab("l-1", date = "2026-01-01", testName = "Blood Glucose", result = "95", unit = "mg/dL"),
                lab("l-2", date = "2026-01-01", testName = "Blood Glucose", result = "120", unit = "mg/dL"),
            ),
        )
        val report = labReport(p)

        assertTrue(report.comparisons.isEmpty())
        assertEquals(2, report.singles.size)
        assertEquals(listOf("Blood Glucose", "Blood Glucose"), report.singles.map { it.test })
        assertEquals(listOf("95", "120"), report.singles.map { it.result })
        assertEquals(listOf("mg/dL", "mg/dL"), report.singles.map { it.unit })
    }

    @Test
    fun labReport_singleLab_preservesTrimmedUnit_andBlankBecomesNull() {
        val p = patient(
            labs = listOf(
                lab("l-1", date = "2026-01-01", testName = "Ferritin", result = " 45 ", unit = " ng/mL "),
                lab("l-2", date = "2026-01-02", testName = "CRP", result = "12", unit = "   "),
                lab("l-3", date = "2026-01-03", testName = "Rubella IgG", result = "Positive", unit = null),
            ),
        )
        val report = labReport(p)

        assertTrue(report.comparisons.isEmpty())
        assertEquals(3, report.singles.size)

        assertEquals("45", report.singles[0].result)
        assertEquals("ng/mL", report.singles[0].unit)

        assertEquals("12", report.singles[1].result)
        assertNull(report.singles[1].unit)

        assertEquals("Positive", report.singles[2].result)
        assertNull(report.singles[2].unit)
    }

    @Test
    fun labReport_groupingByCategory() {
        val p = patient(
            labs = listOf(
                lab("l-1", date = "2026-01-01", testCategory = "Hematology", testName = "Platelets", result = "250"),
                lab("l-2", date = "2026-02-01", testCategory = "Hematology", testName = "Platelets", result = "240"),
                lab("l-3", date = "2026-01-01", testCategory = "Biochemistry", testName = "Creatinine", result = "0.8"),
                lab("l-4", date = "2026-02-01", testCategory = "Biochemistry", testName = "Creatinine", result = "0.9"),
            ),
        )
        val report = labReport(p)

        assertEquals(2, report.comparisons.size)
        val categories = report.comparisons.map { it.category }
        assertTrue(categories.contains("Hematology"))
        assertTrue(categories.contains("Biochemistry"))
    }

    @Test
    fun labReport_moreThanSixDates_splitsIntoSecondTableInDateOrder() {
        val dates = listOf(
            "2026-01-01", "2026-01-05", "2026-01-10",
            "2026-01-15", "2026-01-20", "2026-01-25",
            "2026-02-01", "2026-02-05",
        )
        val labs = dates.mapIndexed { idx, d ->
            lab("l-$idx", date = d, testCategory = "Biochemistry", testName = "Uric Acid", result = "4.$idx")
        }
        val report = labReport(patient(labs = labs))

        assertEquals(2, report.comparisons.size)
        val table1 = report.comparisons[0]
        val table2 = report.comparisons[1]

        assertEquals(6, table1.dates.size)
        assertEquals(2, table2.dates.size)
        assertEquals(listOf("01/01/2026", "05/01/2026", "10/01/2026", "15/01/2026", "20/01/2026", "25/01/2026"), table1.dates)
        assertEquals(listOf("01/02/2026", "05/02/2026"), table2.dates)
        assertEquals(6, table1.rows.first().cells.size)
        assertEquals(2, table2.rows.first().cells.size)
    }

    @Test
    fun labReport_missingDateForRow_givesNullCell() {
        val labs = listOf(
            lab("l-1", date = "2026-01-01", testCategory = "Hematology", testName = "Hb", result = "12.0"),
            lab("l-2", date = "2026-01-10", testCategory = "Hematology", testName = "Hb", result = "11.8"),
            lab("l-3", date = "2026-01-01", testCategory = "Hematology", testName = "WBC", result = "7.0"),
            lab("l-4", date = "2026-01-20", testCategory = "Hematology", testName = "WBC", result = "8.2"),
        )
        val report = labReport(patient(labs = labs))

        assertEquals(1, report.comparisons.size)
        val table = report.comparisons.first()
        assertEquals(listOf("01/01/2026", "10/01/2026", "20/01/2026"), table.dates)

        val hbRow = table.rows.first { it.label.startsWith("Hb") }
        assertEquals(3, hbRow.cells.size)
        assertNotNull(hbRow.cells[0])
        assertNotNull(hbRow.cells[1])
        assertNull(hbRow.cells[2])

        val wbcRow = table.rows.first { it.label.startsWith("WBC") }
        assertEquals(3, wbcRow.cells.size)
        assertNotNull(wbcRow.cells[0])
        assertNull(wbcRow.cells[1])
        assertNotNull(wbcRow.cells[2])
    }

    @Test
    fun labReport_abnormalFlags_highLowAndAsterisk() {
        val highLab = lab("l-1", date = "2026-01-01", testName = "Hb-High", result = "16.5", referenceRange = "11.0 - 15.0", isAbnormal = true)
        val lowLab = lab("l-2", date = "2026-01-01", testName = "Hb-Low", result = "9.5", referenceRange = "11.0 - 15.0", isAbnormal = true)
        val asteriskLabNonNumeric = lab("l-3", date = "2026-01-01", testName = "Serology", result = "Reactive", referenceRange = "Non-reactive", isAbnormal = true)
        val normalLab = lab("l-4", date = "2026-01-01", testName = "Hb-Norm", result = "12.5", referenceRange = "11.0 - 15.0", isAbnormal = false)

        val singlesReport = labReport(patient(labs = listOf(highLab, lowLab, asteriskLabNonNumeric, normalLab)))
        val highSingle = singlesReport.singles.first { it.test == "Hb-High" }
        val lowSingle = singlesReport.singles.first { it.test == "Hb-Low" }
        val astSingle = singlesReport.singles.first { it.test == "Serology" }
        val normSingle = singlesReport.singles.first { it.test == "Hb-Norm" }

        assertEquals("H", highSingle.flagMark)
        assertEquals("L", lowSingle.flagMark)
        assertEquals("*", astSingle.flagMark)
        assertNull(normSingle.flagMark)

        val repeatedLabs = listOf(
            highLab,
            highLab.copy(id = "l-1-rep", date = "2026-02-01"),
            lowLab,
            lowLab.copy(id = "l-2-rep", date = "2026-02-01"),
            asteriskLabNonNumeric,
            asteriskLabNonNumeric.copy(id = "l-3-rep", date = "2026-02-01"),
        )
        val compReport = labReport(patient(labs = repeatedLabs))
        val rows = compReport.comparisons.flatMap { it.rows }

        val highCell = rows.first { it.label.startsWith("Hb-High") }.cells.first()
        assertEquals(LabResultFlag.HIGH, highCell?.flag)
        assertFalse(highCell?.abnormalNoDirection ?: true)

        val lowCell = rows.first { it.label.startsWith("Hb-Low") }.cells.first()
        assertEquals(LabResultFlag.LOW, lowCell?.flag)
        assertFalse(lowCell?.abnormalNoDirection ?: true)

        val astCell = rows.first { it.label.startsWith("Serology") }.cells.first()
        assertNull(astCell?.flag)
        assertTrue(astCell?.abnormalNoDirection ?: false)
    }

    @Test
    fun labReport_sameDayDuplicateHbResults_combinedInOneCellJoinedBySlashWithFlags() {
        val hb1 = lab("l-1", date = "2026-01-01", testName = "Hemoglobin", result = "9.5", referenceRange = "11.0 - 15.0", isAbnormal = true)
        val hb2 = lab("l-2", date = "2026-01-01", testName = "Hemoglobin", result = "16.5", referenceRange = "11.0 - 15.0", isAbnormal = true)
        val hb3 = lab("l-3", date = "2026-02-01", testName = "Hemoglobin", result = "12.0", referenceRange = "11.0 - 15.0", isAbnormal = false)

        val report = labReport(patient(labs = listOf(hb1, hb2, hb3)))
        assertEquals(1, report.comparisons.size)
        val hbRow = report.comparisons.first().rows.first { it.label.startsWith("Hemoglobin") }
        val date1Cell = hbRow.cells[0]
        assertNotNull(date1Cell)
        assertEquals("9.5 L / 16.5 H", date1Cell?.value)
        assertEquals(2, date1Cell?.entries?.size)
        assertEquals("9.5", date1Cell?.entries?.get(0)?.value)
        assertEquals(LabResultFlag.LOW, date1Cell?.entries?.get(0)?.flag)
        assertEquals("16.5", date1Cell?.entries?.get(1)?.value)
        assertEquals(LabResultFlag.HIGH, date1Cell?.entries?.get(1)?.flag)
    }

    @Test
    fun labReport_sameDayDuplicateResults_withAsteriskFlag() {
        val lab1 = lab("l-1", date = "2026-01-01", testName = "Serology", result = "Reactive", referenceRange = "Non-reactive", isAbnormal = true)
        val lab2 = lab("l-2", date = "2026-01-01", testName = "Serology", result = "Weakly reactive", referenceRange = "Non-reactive", isAbnormal = true)
        val lab3 = lab("l-3", date = "2026-02-01", testName = "Serology", result = "Negative", referenceRange = "Non-reactive", isAbnormal = false)

        val report = labReport(patient(labs = listOf(lab1, lab2, lab3)))
        assertEquals(1, report.comparisons.size)
        val row = report.comparisons.first().rows.first { it.label.startsWith("Serology") }
        val date1Cell = row.cells[0]
        assertNotNull(date1Cell)
        assertEquals("Reactive * / Weakly reactive *", date1Cell?.value)
        assertEquals(2, date1Cell?.entries?.size)
        assertTrue(date1Cell?.entries?.get(0)?.abnormalNoDirection == true)
        assertTrue(date1Cell?.entries?.get(1)?.abnormalNoDirection == true)
    }

    @Test
    fun labReport_noReferenceRangeTextAnywhereInOutput() {
        val uniqueRange = "99.1 - 99.9 secret-range"
        val p = patient(
            labs = listOf(
                lab("l-1", date = "2026-01-01", testName = "TestA", result = "50", referenceRange = uniqueRange),
                lab("l-2", date = "2026-02-01", testName = "TestA", result = "55", referenceRange = uniqueRange),
                lab("l-3", date = "2026-01-01", testName = "TestB", result = "70", referenceRange = uniqueRange),
            ),
        )
        val report = labReport(p)

        for (table in report.comparisons) {
            assertFalse(table.category.contains("secret-range"))
            for (date in table.dates) assertFalse(date.contains("secret-range"))
            for (row in table.rows) {
                assertFalse(row.label.contains("secret-range"))
                for (cell in row.cells) {
                    if (cell != null) assertFalse(cell.value.contains("secret-range"))
                }
            }
        }
        for (single in report.singles) {
            assertFalse(single.test.contains("secret-range"))
            assertFalse(single.result.contains("secret-range"))
            assertFalse(single.unit?.contains("secret-range") == true)
            assertFalse(single.date.contains("secret-range"))
        }
    }

    // --- RULE 5: Picture candidates & figure numbering ---

    @Test
    fun pictureCandidates_fromScanImagesAndAttachments() {
        val scan1 = scan(id = "s-100", date = "2026-02-01", imageUris = listOf("uri://scan/a.jpg", "uri://scan/b.jpg"))
        val visit1 = visit(id = "v-100", date = "2026-01-15")
        val attScan = attachment(id = "att-1", capturedAt = "2026-02-01T09:00:00", linkedRecordId = "s-100", fileName = "scan_att.jpg")
        val attVisit = attachment(id = "att-2", capturedAt = "2026-01-15T11:00:00", linkedRecordId = "v-100", fileName = "visit_att.jpg")
        val attProfileTitle = attachment(id = "att-3", capturedAt = "2026-01-01T08:00:00", linkedRecordId = null, title = "National ID", fileName = "id.jpg")
        val attProfileDefault = attachment(id = "att-4", capturedAt = "2026-01-02T08:00:00", linkedRecordId = null, title = null, fileName = "profile.jpg")

        val p = patient(
            visits = listOf(visit1),
            scans = listOf(scan1),
            attachments = listOf(attScan, attVisit, attProfileTitle, attProfileDefault),
        )
        val candidates = pictureCandidates(p)

        assertEquals(6, candidates.size)
        assertEquals(
            listOf("att:att-3", "att:att-4", "att:att-2", "att:att-1", "scan:s-100:0", "scan:s-100:1"),
            candidates.map { it.key },
        )

        val scanCandidateA = candidates.first { it.key == "scan:s-100:0" }
        assertEquals("Ultrasound 01/02/2026", scanCandidateA.sourceLabel)
        assertEquals("s-100", scanCandidateA.linkedRecordId)
        assertEquals("uri://scan/a.jpg", scanCandidateA.location)

        val scanCandidateB = candidates.first { it.key == "scan:s-100:1" }
        assertEquals("uri://scan/b.jpg", scanCandidateB.location)

        val attScanCandidate = candidates.first { it.key == "att:att-1" }
        assertEquals("Ultrasound 01/02/2026", attScanCandidate.sourceLabel)
        assertEquals("s-100", attScanCandidate.linkedRecordId)

        val attVisitCandidate = candidates.first { it.key == "att:att-2" }
        assertEquals("Visit #1", attVisitCandidate.sourceLabel)
        assertEquals("v-100", attVisitCandidate.linkedRecordId)

        val attProfileTitleCandidate = candidates.first { it.key == "att:att-3" }
        assertEquals("National ID", attProfileTitleCandidate.sourceLabel)
        assertNull(attProfileTitleCandidate.linkedRecordId)

        val attProfileDefaultCandidate = candidates.first { it.key == "att:att-4" }
        assertEquals("Patient profile", attProfileDefaultCandidate.sourceLabel)
        assertNull(attProfileDefaultCandidate.linkedRecordId)
    }

    @Test
    fun figuresFor_numberedOnlyForSelectedKeysInDateOrder_andFigureRefsFor() {
        val v1 = visit(id = "v-1", date = "2026-03-10")
        val v2 = visit(id = "v-2", date = "2026-02-05")
        val scanLateTie = scan(id = "s-2", date = "2026-03-10", imageUris = listOf("f-tie.jpg"))
        val scanMalformedDmy = scan(id = "s-bad-dmy", date = "01/02/2026", imageUris = listOf("f-dmy.jpg"))
        val scanEarly = scan(id = "s-1", date = "2026-01-15", imageUris = listOf("f-early.jpg"))
        val attMid = attachment(id = "a-mid", capturedAt = "2026-02-05T09:00:00", linkedRecordId = "v-2", fileName = "f-mid.jpg")
        val attLate = attachment(id = "a-late", capturedAt = "2026-03-10T10:00:00", linkedRecordId = "v-1", fileName = "f-late.jpg")
        val attMalformed = attachment(id = "a-bad-text", capturedAt = "not-a-date", linkedRecordId = null, fileName = "f-bad.jpg")

        val p = patient(
            visits = listOf(v1, v2),
            scans = listOf(scanMalformedDmy, scanLateTie, scanEarly),
            attachments = listOf(attMalformed, attLate, attMid),
        )
        val candidates = pictureCandidates(p)
        assertEquals(
            listOf("scan:s-1:0", "att:a-mid", "att:a-late", "scan:s-2:0", "att:a-bad-text", "scan:s-bad-dmy:0"),
            candidates.map { it.key },
        )

        val selected = setOf("scan:s-bad-dmy:0", "scan:s-2:0", "att:a-late", "scan:s-1:0", "att:a-bad-text")
        val figures = figuresFor(candidates, selected)

        assertEquals(
            listOf(
                1 to "scan:s-1:0",
                2 to "att:a-late",
                3 to "scan:s-2:0",
                4 to "att:a-bad-text",
                5 to "scan:s-bad-dmy:0",
            ),
            figures.map { it.number to it.candidate.key },
        )

        assertEquals(listOf(1), figureRefsFor("s-1", figures))
        assertEquals(listOf(2), figureRefsFor("v-1", figures))
        assertEquals(listOf(3), figureRefsFor("s-2", figures))
        assertEquals(listOf(5), figureRefsFor("s-bad-dmy", figures))
        assertEquals(emptyList<Int>(), figureRefsFor("v-2", figures))
        assertEquals(emptyList<Int>(), figureRefsFor("", figures))
    }

    @Test
    fun figuresFor_preservesGivenCandidateOrderWithoutResorting() {
        val c1 = PictureCandidate(key = "k-1", sourceLabel = "Visit #1", date = "10/03/2026", linkedRecordId = "v-1", location = "f1.jpg")
        val c2 = PictureCandidate(key = "k-2", sourceLabel = "Ultrasound 15/01/2026", date = "15/01/2026", linkedRecordId = "s-1", location = "f2.jpg")
        val c3 = PictureCandidate(key = "k-3", sourceLabel = "Visit #2", date = "05/02/2026", linkedRecordId = "v-2", location = "f3.jpg")

        val figures = figuresFor(listOf(c1, c2, c3), setOf("k-1", "k-3"))
        assertEquals(listOf(1 to "k-1", 2 to "k-3"), figures.map { it.number to it.candidate.key })
    }

    // --- RULE 6: Empty patient & odd/malformed data safety ---

    @Test
    fun emptyPatient_returnsEmptyStructures() {
        val emptyP = patient()
        val dummyVisit = visit("v-none")

        assertNull(latestScanForVisit(emptyP, dummyVisit))
        assertTrue(unlinkedScans(emptyP, emptyList()).isEmpty())

        val labs = labReport(emptyP)
        assertTrue(labs.comparisons.isEmpty())
        assertTrue(labs.singles.isEmpty())

        assertTrue(pictureCandidates(emptyP).isEmpty())
        assertTrue(figuresFor(emptyList(), emptySet()).isEmpty())
        assertTrue(figureRefsFor("v-1", emptyList()).isEmpty())
    }

    @Test
    fun oddData_neverThrows() {
        val oddVisit = visit(id = "v-odd", date = "unparseable-date").copy(
            bloodPressure = "   ",
            weight = "",
        )
        val oddScan = scan(
            id = "s-odd",
            date = "32/13/2026",
            fetalNumber = "   ",
            presentation = " ",
            amnioticFluid = " ",
            placentalSite = "",
            fetalHeartRate = "  ",
            biometryBpd = "",
            biometryHc = " ",
            biometryAc = null,
            biometryFl = "   ",
            estimatedFetalWeight = "   ",
            impression = " ",
            imageUris = listOf("", "   ", "valid-uri"),
        )
        val oddLab = lab(
            id = "l-odd",
            date = "bad-date-time",
            testCategory = " ",
            testName = "Blood Test",
            result = "NotANumber",
            unit = " ",
            referenceRange = "unparseable-range",
            isAbnormal = true,
        )
        val oddAttachment = attachment(
            id = "a-odd",
            capturedAt = "non-iso-captured",
            linkedRecordId = "   ",
            title = "   ",
            fileName = "   ",
            filePath = "fallback-path.png",
        )

        val oddPatient = patient(
            visits = listOf(oddVisit),
            scans = listOf(oddScan),
            labs = listOf(oddLab),
            attachments = listOf(oddAttachment),
        )

        val outcome = scanOutcomeFor(oddScan)
        assertNull(outcome.typeLabel)
        assertNull(outcome.presentation)
        assertTrue(outcome.measurements.isEmpty())
        assertTrue(outcome.extras.isEmpty())

        val latestScan = latestScanForVisit(oddPatient, oddVisit)
        assertNull(latestScan)

        val unlinked = unlinkedScans(oddPatient, oddPatient.visits)
        assertEquals(1, unlinked.size)

        val labsResult = labReport(oddPatient)
        assertEquals(1, labsResult.singles.size)
        assertEquals("NotANumber", labsResult.singles.first().result)
        assertNull(labsResult.singles.first().unit)
        assertEquals("*", labsResult.singles.first().flagMark)

        val candidates = pictureCandidates(oddPatient)
        assertEquals(2, candidates.size)

        val figures = figuresFor(candidates, setOf(candidates.first().key))
        assertEquals(1, figures.size)

        val refs = figureRefsFor(candidates.first().linkedRecordId.orEmpty(), figures)
        assertNotNull(refs)
    }
}
