package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoAttachment
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
 * Verification of the approved Print Layout B3 contract according to BUILD_PLAN.md:
 * - Visits table 4-column layout without findings column.
 * - Scan outcome rendering (bold type, presentation, measurement line, extras).
 * - Figure references for selected pictures only.
 * - Dash fallback for visits without scans.
 * - Conditional unlinked scans section.
 * - Lab comparison table (dates as columns, H/L/asterisk flags), singles table, no reference ranges.
 * - Medications compact list with active count.
 * - Attached images section with data URI img and placeholder fallback.
 * - CSS font sizes >= 9pt.
 * - No em dash characters in the output.
 * - Strict HTML escaping of dynamic values.
 */
class PrintV2LayoutTest {

    private val asOfDate = LocalDate.of(2026, 10, 28)

    private fun testPatient(
        visits: List<DemoVisit> = emptyList(),
        scans: List<DemoUltrasoundScan> = emptyList(),
        labs: List<DemoLabInvestigation> = emptyList(),
        meds: List<DemoMedicationRecord> = emptyList(),
        attachments: List<DemoAttachment> = emptyList(),
    ) = DemoPatient(
        id = "p-v2-1",
        caseId = "P-V2-001",
        displayName = "Fatima Al-Mansoor",
        dateOfBirth = "1994-05-12",
        phone = "01012345678",
        visits = visits,
        riskLevel = "Low Risk",
        ageText = "32",
        bloodGroupText = "A+",
    ).copy(
        ultrasoundScans = scans,
        labInvestigations = labs,
        medications = meds,
        attachments = attachments,
    )

    @Test
    fun visitsTable_hasFourColumnsAndNoFindingsColumn() {
        val visit = DemoVisit(
            id = "v-1",
            date = "2026-09-01",
            bloodPressure = "115/75",
            weight = "64.0",
            findings = "Clinical findings must not print in table",
            notes = "Internal clinician note",
            revision = 1,
            isSyntheticEdit = false,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(visits = listOf(visit)),
            asOfDate = asOfDate,
        )

        val visitsSection = html.substringAfter("Antenatal care visits log").substringBefore("</section>")
        val thead = visitsSection.substringAfter("<thead>").substringBefore("</thead>")
        val headers = Regex("""<th[^>]*>(.*?)</th>""").findAll(thead).map { it.groupValues[1].trim() }.toList()

        assertEquals(listOf("Visit and date", "GA", "BP and weight", "Scan outcome"), headers)
        assertFalse("No clinical findings header", thead.contains("findings", ignoreCase = true))
        assertFalse("No notes header", thead.contains("notes", ignoreCase = true))
        assertFalse("Visit findings text must not appear in the visits table", visitsSection.contains("Clinical findings must not print in table"))
    }

    @Test
    fun scanOutcome_showsBoldTypePresentationMeasurementsAndExtras() {
        val scan = DemoUltrasoundScan(
            id = "s-1",
            patientId = "p-v2-1",
            date = "2026-09-01",
            linkedVisitId = "v-1",
            gestationalAgeWeeks = 28,
            gestationalAgeDays = 3,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Posterior",
            amnioticFluid = "Reduced",
            fetalHeartRate = "142 bpm",
            biometryBpd = "52 mm",
            biometryHc = "190 mm",
            biometryAc = "165 mm",
            biometryFl = "38 mm",
            estimatedFetalWeight = "480",
            impression = "Normal fetal anatomy",
        )
        val visit = DemoVisit(
            id = "v-1",
            date = "2026-09-01",
            bloodPressure = "118/76",
            weight = "65",
            findings = null,
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(visits = listOf(visit), scans = listOf(scan)),
            asOfDate = asOfDate,
        )

        assertTrue("Bold type and presentation", html.contains("""<span class="scan-primary"><strong>Singleton</strong>, Cephalic</span>"""))
        assertTrue(
            "Measurement line",
            html.contains("""<span class="scan-measurements"><span class="u">BPD 52 mm</span>, <span class="u">HC 190 mm</span>, <span class="u">AC 165 mm</span>, <span class="u">FL 38 mm</span>, <span class="u">EFW 480 g</span></span>"""),
        )
        assertTrue(
            "Extras line",
            html.contains("""<span class="subtext">Fluid: Reduced · Placenta: Posterior · FHR: 142 bpm · Normal fetal anatomy</span>"""),
        )
    }

    @Test
    fun figureReferences_appearOnlyForSelectedPictures() {
        val scan = DemoUltrasoundScan(
            id = "s-1",
            patientId = "p-v2-1",
            date = "2026-09-01",
            linkedVisitId = "v-1",
            gestationalAgeWeeks = 20,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Anterior",
            amnioticFluid = "Normal",
            fetalHeartRate = "140",
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = null,
            imageUris = listOf("file:///photos/scan1.jpg"),
        )
        val visit = DemoVisit(
            id = "v-1",
            date = "2026-09-01",
            bloodPressure = "120/80",
            weight = "60",
            findings = null,
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )
        val patient = testPatient(visits = listOf(visit), scans = listOf(scan))
        val candidateKey = "scan:s-1:0"

        val htmlUnselected = PatientPrintHtmlGenerator.generateHtml(
            patient = patient,
            asOfDate = asOfDate,
            selectedPictureKeys = emptySet(),
        )
        assertFalse("No fig-ref when unselected", htmlUnselected.contains("""<span class="fig-ref">"""))
        assertFalse(htmlUnselected.contains("Fig 1"))

        val htmlSelected = PatientPrintHtmlGenerator.generateHtml(
            patient = patient,
            asOfDate = asOfDate,
            selectedPictureKeys = setOf(candidateKey),
        )
        assertTrue("Fig-ref present when selected", htmlSelected.contains("""<span class="fig-ref">Fig 1</span>"""))
    }

    @Test
    fun visitWithoutScan_showsDash() {
        val visit = DemoVisit(
            id = "v-noscan",
            date = "2026-09-01",
            bloodPressure = "110/70",
            weight = "59",
            findings = null,
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(visits = listOf(visit)),
            asOfDate = asOfDate,
        )
        val visitsSection = html.substringAfter("Antenatal care visits log").substringBefore("</section>")
        val visitRow = visitsSection.substringAfter("<tbody>").substringBefore("</tbody>")
        assertTrue("Visit row scan outcome cell is dash", visitRow.contains("<td>-</td>"))
    }

    @Test
    fun unlinkedScansSection_appearsOnlyWhenThereAreUnlinkedScans() {
        val linkedScan = DemoUltrasoundScan(
            id = "s-linked",
            patientId = "p-v2-1",
            date = "2026-09-01",
            linkedVisitId = "v-1",
            gestationalAgeWeeks = 20,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = null,
        )
        val unlinkedScan = DemoUltrasoundScan(
            id = "s-unlinked",
            patientId = "p-v2-1",
            date = "2026-08-15",
            linkedVisitId = null,
            gestationalAgeWeeks = 18,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = "Target scan",
        )
        val visit = DemoVisit(
            id = "v-1",
            date = "2026-09-01",
            bloodPressure = "120/80",
            weight = "60",
            findings = null,
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )

        val htmlOnlyLinked = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(visits = listOf(visit), scans = listOf(linkedScan)),
            asOfDate = asOfDate,
        )
        assertFalse(
            "Unlinked scans section absent when all scans are linked",
            htmlOnlyLinked.contains("Other scans"),
        )

        val htmlWithUnlinked = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(visits = listOf(visit), scans = listOf(linkedScan, unlinkedScan)),
            asOfDate = asOfDate,
        )
        assertTrue(
            "Unlinked scans section present when unlinked scans exist",
            htmlWithUnlinked.contains("Other scans"),
        )
    }

    @Test
    fun labComparisonTable_datesAsColumnsAndAbnormalMarks_andSinglesTable_noReferenceRange() {
        val repeatedHigh = DemoLabInvestigation(
            id = "l-1",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Complete Blood Count",
            testName = "Hemoglobin",
            result = "18.5",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = true,
            notes = null,
        )
        val repeatedLow = DemoLabInvestigation(
            id = "l-2",
            patientId = "p-v2-1",
            date = "2026-09-15",
            testCategory = "Complete Blood Count",
            testName = "Hemoglobin",
            result = "9.2",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = true,
            notes = null,
        )
        val repeatedAsterisk = DemoLabInvestigation(
            id = "l-3",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Complete Blood Count",
            testName = "Blood Film",
            result = "Toxic granulation",
            unit = null,
            referenceRange = "Negative",
            isAbnormal = true,
            notes = null,
        )
        val repeatedAsteriskDate2 = DemoLabInvestigation(
            id = "l-4",
            patientId = "p-v2-1",
            date = "2026-09-15",
            testCategory = "Complete Blood Count",
            testName = "Blood Film",
            result = "Normal",
            unit = null,
            referenceRange = "Negative",
            isAbnormal = false,
            notes = null,
        )
        val singleLab = DemoLabInvestigation(
            id = "l-5",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Biochemistry",
            testName = "Fasting Blood Sugar",
            result = "95",
            unit = "mg/dL",
            referenceRange = "70-100 mg/dL",
            isAbnormal = false,
            notes = null,
        )

        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(labs = listOf(repeatedHigh, repeatedLow, repeatedAsterisk, repeatedAsteriskDate2, singleLab)),
            asOfDate = asOfDate,
        )

        val labsSection = html.substringAfter("Laboratory investigations").substringBefore("</section>")

        assertTrue("Comparison table category", labsSection.contains("""<h3 class="lab-panel-title">Complete Blood Count</h3>"""))
        assertTrue("Date column header 1", labsSection.contains("""<th class="col-date"""") && labsSection.contains("01/09/2026"))
        assertTrue("Date column header 2", labsSection.contains("15/09/2026"))

        assertTrue("High flag mark", labsSection.contains("""<strong class="lab-abnormal">18.5 H</strong>"""))
        assertTrue("Low flag mark", labsSection.contains("""<strong class="lab-abnormal">9.2 L</strong>"""))
        assertTrue("Asterisk flag mark", labsSection.contains("""<strong class="lab-abnormal">Toxic granulation *</strong>"""))

        assertTrue("Singles table title", labsSection.contains("""<h3 class="lab-panel-title">Single tests</h3>"""))
        assertTrue("Singles row test name", labsSection.contains("Fasting Blood Sugar"))
        assertTrue("Singles row result", labsSection.contains("95 mg/dL"))

        assertFalse("No reference range column or text 11.5-15.5", html.contains("11.5-15.5"))
        assertFalse("No reference range column or text 70-100", html.contains("70-100"))
        assertFalse("No reference range header", labsSection.contains("Reference range", ignoreCase = true))
    }

    @Test
    fun medicationsCompactList_showsActiveCount() {
        val med1 = DemoMedicationRecord(
            id = "m-1",
            patientId = "p-v2-1",
            name = "Folic Acid",
            dosage = "5 mg",
            route = "PO",
            frequency = "OD",
            startDate = "2026-02-01",
            endDate = null,
            status = "Active",
            notes = "after lunch",
            isProphylaxis = true,
        )
        val med2 = DemoMedicationRecord(
            id = "m-2",
            patientId = "p-v2-1",
            name = "Iron Polymaltose",
            dosage = "100 mg",
            route = "PO",
            frequency = "OD",
            startDate = "2026-03-01",
            endDate = null,
            status = "Active",
            notes = null,
            isProphylaxis = false,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(meds = listOf(med1, med2)),
            asOfDate = asOfDate,
        )

        assertTrue(
            "Medications section heading with active count",
            html.contains("""<h2 class="section-heading">Current medications <span class="med-count">(2 active)</span></h2>"""),
        )
        assertTrue("Compact med-grid list", html.contains("""<ul class="med-grid">"""))
        assertTrue(html.contains("""<span class="bold-val">Folic Acid</span>"""))
        assertTrue(html.contains("""<span class="bold-val">Iron Polymaltose</span>"""))
    }

    @Test
    fun imagesSection_selectedPictures_dataUriAndPlaceholder_andCount() {
        val scan = DemoUltrasoundScan(
            id = "s-img",
            patientId = "p-v2-1",
            date = "2026-09-01",
            linkedVisitId = null,
            gestationalAgeWeeks = 24,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = null,
            imageUris = listOf("file:///photos/s1.jpg", "file:///photos/s2.jpg"),
        )
        val patient = testPatient(scans = listOf(scan))
        val keys = setOf("scan:s-img:0", "scan:s-img:1")

        val htmlEmpty = PatientPrintHtmlGenerator.generateHtml(
            patient = patient,
            asOfDate = asOfDate,
            selectedPictureKeys = emptySet(),
        )
        assertFalse("Images section absent when no pictures selected", htmlEmpty.contains("Attached images"))

        val htmlWithSelection = PatientPrintHtmlGenerator.generateHtml(
            patient = patient,
            asOfDate = asOfDate,
            selectedPictureKeys = keys,
            pictureDataUri = { candidate ->
                if (candidate.key == "scan:s-img:0") "data:image/jpeg;base64,mockImageData123" else null
            },
        )

        assertTrue("Attached images section present", htmlWithSelection.contains("""<section class="attached-images">"""))
        assertTrue(
            "Uses provided data URI with alt text",
            htmlWithSelection.contains("""<img class="figure-image" src="data:image/jpeg;base64,mockImageData123" alt="Fig 1 - Ultrasound 01/09/2026 (01/09/2026)" />"""),
        )
        assertTrue(
            "Falls back to placeholder when resolver returns null",
            htmlWithSelection.contains("""<div class="image-placeholder">Image not available</div>"""),
        )
        assertTrue(
            "Shows picture count line",
            htmlWithSelection.contains("""<div class="image-count-line">Pictures selected for this print: <span class="col-date">2</span></div>"""),
        )
    }

    @Test
    fun cssFontSizes_noTextBelowNinePt() {
        val html = PatientPrintHtmlGenerator.generateHtml(patient = testPatient(), asOfDate = asOfDate)
        val style = html.substringAfter("<style>").substringBefore("</style>")
        val fontSizes = Regex("""font-size:\s*([0-9.]+)pt""").findAll(style).map { it.groupValues[1].toDouble() }.toList()

        assertTrue("CSS must contain font-size rules", fontSizes.isNotEmpty())
        for (size in fontSizes) {
            assertTrue("Font size $size pt is below the 9pt floor", size >= 9.0)
        }
    }

    @Test
    fun noEmDashCharactersInOutput() {
        val visit = DemoVisit(
            id = "v-1",
            date = "2026-09-01",
            bloodPressure = "120/80",
            weight = "65",
            findings = null,
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )
        val scan = DemoUltrasoundScan(
            id = "s-1",
            patientId = "p-v2-1",
            date = "2026-09-01",
            linkedVisitId = "v-1",
            gestationalAgeWeeks = 20,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Posterior",
            amnioticFluid = "Normal",
            fetalHeartRate = "140",
            biometryBpd = "45",
            biometryHc = "170",
            biometryAc = "150",
            biometryFl = "32",
            estimatedFetalWeight = "350",
            impression = "Normal",
        )
        val med = DemoMedicationRecord(
            id = "m-1",
            patientId = "p-v2-1",
            name = "Folic Acid",
            dosage = "5 mg",
            route = "PO",
            frequency = "OD",
            startDate = "2026-01-01",
            endDate = null,
            status = "active",
            notes = null,
            isProphylaxis = false,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(visits = listOf(visit), scans = listOf(scan), meds = listOf(med)),
            asOfDate = asOfDate,
        )
        assertFalse("Output must contain no em dash (—) character", html.contains("—"))
        assertFalse("Output must contain no Unicode em dash (\\u2014)", html.contains("\u2014"))
    }

    @Test
    fun allDynamicValuesEscaped_inScanLabAndPicture() {
        val scanScript = "<script>alert('scan')</script>"
        val labScript = "<script>alert('lab')</script>"
        val picScript = "<script>alert('pic')</script>"
        val scan = DemoUltrasoundScan(
            id = "s-xss",
            patientId = "p-v2-1",
            date = "2026-09-01",
            linkedVisitId = "v-1",
            gestationalAgeWeeks = 20,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = scanScript,
        )
        val visit = DemoVisit(
            id = "v-1",
            date = "2026-09-01",
            bloodPressure = "120/80",
            weight = "60",
            findings = null,
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )
        val lab = DemoLabInvestigation(
            id = "l-xss",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "General",
            testName = labScript,
            result = "Positive",
            unit = null,
            referenceRange = null,
            isAbnormal = false,
            notes = null,
        )
        val attachment = DemoAttachment(
            id = "att-xss",
            patientId = "p-v2-1",
            fileName = "scan.jpg",
            filePath = "/files/scan.jpg",
            category = "Ultrasound",
            mimeType = "image/jpeg",
            fileSizeBytes = 1024,
            sha256Hash = "hash123",
            title = picScript,
            capturedAt = "2026-09-01",
            linkedRecordId = null,
        )

        val patient = testPatient(
            visits = listOf(visit),
            scans = listOf(scan),
            labs = listOf(lab),
            attachments = listOf(attachment),
        )

        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = patient,
            asOfDate = asOfDate,
            selectedPictureKeys = setOf("att:att-xss"),
        )

        assertFalse("Raw <script> must never exist in the generated HTML", html.contains("<script>"))
        assertFalse("Raw </script> must never exist in the generated HTML", html.contains("</script>"))
        assertTrue("Scan impression escaped", html.contains("&lt;script&gt;alert(&#39;scan&#39;)&lt;/script&gt;"))
        assertTrue("Lab test name escaped", html.contains("&lt;script&gt;alert(&#39;lab&#39;)&lt;/script&gt;"))
        assertTrue("Picture label escaped", html.contains("&lt;script&gt;alert(&#39;pic&#39;)&lt;/script&gt;"))
    }

    @Test
    fun unlinkedScans_whenFourteenVisitsAndScanLinkedToVisitOne_appearsUnderOtherScansInHtml() {
        val visits = (1..14).map { i ->
            val dayStr = i.toString().padStart(2, '0')
            DemoVisit(
                id = "v-$i",
                date = "2026-01-$dayStr",
                bloodPressure = "120/80",
                weight = "60.0",
                findings = null,
                notes = null,
                revision = 1,
                isSyntheticEdit = false,
            )
        }
        val scanVisit1 = DemoUltrasoundScan(
            id = "s-visit-1",
            patientId = "p-v2-1",
            date = "2026-01-01",
            linkedVisitId = "v-1",
            gestationalAgeWeeks = 12,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = "20 mm",
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = "First trimester scan",
        )
        val patient = testPatient(visits = visits, scans = listOf(scanVisit1))
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = patient,
            asOfDate = asOfDate,
        )

        // 14 visits with limit 12 means visits 1 and 2 are hidden
        assertTrue("Visits table indicates earlier visits not shown", html.contains("2 earlier visits not shown"))

        // Scan linked to hidden visit 1 must not be silently lost: it must appear under "Other scans"
        assertTrue("Heading 'Other scans' is present", html.contains("<h2 class=\"section-heading\">Other scans</h2>"))
        val otherScansSection = html.substringAfter("Other scans").substringBefore("</section>")
        assertTrue("Scan linked to visit #1 appears under 'Other scans'", otherScansSection.contains("01/01/2026"))
        assertTrue("Scan impression is rendered under 'Other scans'", otherScansSection.contains("First trimester scan"))

        val visitsSection = html.substringAfter("Antenatal care visits log").substringBefore("</section>")
        assertFalse("Visit 1 date is hidden from the visits table", visitsSection.contains("01/01/2026"))
    }

    @Test
    fun unlinkedScans_whenScanLinkedToShownVisitIsNotLatest_appearsUnderOtherScans() {
        val visit = DemoVisit(
            id = "v-shown",
            date = "2026-09-01",
            bloodPressure = "120/80",
            weight = "65.0",
            findings = null,
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )
        val olderScan = DemoUltrasoundScan(
            id = "s-older",
            patientId = "p-v2-1",
            date = "2026-08-15",
            linkedVisitId = "v-shown",
            gestationalAgeWeeks = 22,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = "Older interval scan",
        )
        val latestScan = DemoUltrasoundScan(
            id = "s-latest",
            patientId = "p-v2-1",
            date = "2026-09-01",
            linkedVisitId = "v-shown",
            gestationalAgeWeeks = 24,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = "Latest visit scan",
        )
        val patient = testPatient(visits = listOf(visit), scans = listOf(olderScan, latestScan))
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = patient,
            asOfDate = asOfDate,
        )

        val visitsSection = html.substringAfter("Antenatal care visits log").substringBefore("</section>")
        assertTrue("Visits log shows latest scan", visitsSection.contains("Latest visit scan"))
        assertFalse("Visits log does not show superseded scan", visitsSection.contains("Older interval scan"))

        assertTrue("Heading 'Other scans' is present", html.contains("<h2 class=\"section-heading\">Other scans</h2>"))
        val otherScansSection = html.substringAfter("Other scans").substringBefore("</section>")
        assertTrue("Superseded scan appears under 'Other scans'", otherScansSection.contains("Older interval scan"))
        assertTrue("Superseded scan date appears under 'Other scans'", otherScansSection.contains("15/08/2026"))
        assertFalse("Latest scan does not appear under 'Other scans'", otherScansSection.contains("Latest visit scan"))
    }

    @Test
    fun labComparisonTable_sameDayDuplicateHbResults_appearInOneCellJoinedBySlash() {
        val hb1 = DemoLabInvestigation(
            id = "l-1",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Complete Blood Count",
            testName = "Hemoglobin",
            result = "10.2",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = true,
            notes = null,
        )
        val hb2 = DemoLabInvestigation(
            id = "l-2",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Complete Blood Count",
            testName = "Hemoglobin",
            result = "10.8",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = true,
            notes = null,
        )
        val hb3 = DemoLabInvestigation(
            id = "l-3",
            patientId = "p-v2-1",
            date = "2026-09-15",
            testCategory = "Complete Blood Count",
            testName = "Hemoglobin",
            result = "12.0",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = false,
            notes = null,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(labs = listOf(hb1, hb2, hb3)),
            asOfDate = asOfDate,
        )

        val labsSection = html.substringAfter("Laboratory investigations").substringBefore("</section>")
        assertTrue(
            "Same-day duplicate Hb results both appear in cell joined by ' / '",
            labsSection.contains("""<td class="col-num lab-multi"><strong class="lab-abnormal">10.2 L</strong> / <strong class="lab-abnormal">10.8 L</strong></td>"""),
        )
    }

    @Test
    fun fixedFooter_containsEscapedPatientNameWithInjectedTags_patientId_printDate_andNoPageNumbering() {
        val patientWithInjectedTag = testPatient().copy(
            displayName = "Salma <b>Ali</b>",
            caseId = "P-12345",
        )
        val testPrintDate = LocalDate.of(2026, 11, 18)
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = patientWithInjectedTag,
            asOfDate = testPrintDate,
        )

        val footerHtml = html.substringAfter("<footer class=\"page-footer\">").substringBefore("</footer>")

        // 1. Escaped patient name: <b> escaped to &lt;b&gt;
        assertTrue("Footer contains escaped patient name", footerHtml.contains("Salma &lt;b&gt;Ali&lt;/b&gt;"))
        assertFalse("Footer must not contain raw <b> tag", footerHtml.contains("<b>Ali</b>"))

        // 2. dir=rtl only for Arabic (Salma Ali is English, so no dir="rtl")
        assertTrue("English name does not have dir=rtl in footer", footerHtml.contains("<span>Salma &lt;b&gt;Ali&lt;/b&gt;</span>"))

        // 3. Patient ID
        assertTrue("Footer contains patient ID", footerHtml.contains("P-12345"))

        // 4. Print date
        assertTrue("Footer contains print date", footerHtml.contains("18/11/2026"))

        // 5. No 'Page' numbering
        assertFalse("Footer must not contain 'Page' numbering", footerHtml.contains("Page"))

        // Also verify Arabic name sets dir="rtl"
        val arabicPatient = testPatient().copy(
            displayName = "سلمى <b>علي</b>",
            caseId = "P-67890",
        )
        val arabicHtml = PatientPrintHtmlGenerator.generateHtml(
            patient = arabicPatient,
            asOfDate = testPrintDate,
        )
        val arabicFooter = arabicHtml.substringAfter("<footer class=\"page-footer\">").substringBefore("</footer>")
        assertTrue("Arabic name has dir=rtl in footer", arabicFooter.contains("<span dir=\"rtl\">سلمى &lt;b&gt;علي&lt;/b&gt;</span>"))
        assertTrue("Arabic footer contains patient ID", arabicFooter.contains("P-67890"))
        assertTrue("Arabic footer contains print date", arabicFooter.contains("18/11/2026"))
        assertFalse("Arabic footer must not contain 'Page' numbering", arabicFooter.contains("Page"))
    }

    @Test
    fun nullPictureResolver_printsImageNotAvailable_withFigNAndCaptionKept() {
        val scan = DemoUltrasoundScan(
            id = "s-null-res",
            patientId = "p-v2-1",
            date = "2026-09-01",
            gestationalAgeWeeks = 20,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = null,
            linkedVisitId = null,
            imageUris = listOf("file:///photos/scan_photo.jpg"),
        )
        val candidateKey = "scan:s-null-res:0"
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(scans = listOf(scan)),
            asOfDate = asOfDate,
            selectedPictureKeys = setOf(candidateKey),
            pictureDataUri = { null },
        )

        val imagesSection = html.substringAfter("Attached images").substringBefore("</section>")
        assertTrue("Missing image prints 'Image not available'", imagesSection.contains("""<div class="image-placeholder">Image not available</div>"""))
        assertTrue("Fig 1 is kept", imagesSection.contains("""<div class="figure-title"><strong>Fig 1</strong></div>"""))
        assertTrue("Figcaption is kept", imagesSection.contains("""<figcaption class="figure-caption">"""))
        assertTrue("Source label is kept", imagesSection.contains("Source: Ultrasound 01/09/2026"))
        assertFalse("No figure-image img tag when picture resolver returns null", imagesSection.contains("<img class=\"figure-image\""))
    }

    @Test
    fun scanOutcome_fluidNormalPrinted() {
        val scan = DemoUltrasoundScan(
            id = "s-fluid-norm",
            patientId = "p-v2-1",
            date = "2026-09-01",
            linkedVisitId = "v-1",
            gestationalAgeWeeks = 24,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Posterior",
            amnioticFluid = "Normal",
            fetalHeartRate = "140",
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = null,
        )
        val visit = DemoVisit(
            id = "v-1",
            date = "2026-09-01",
            bloodPressure = "120/80",
            weight = "65",
            findings = null,
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(visits = listOf(visit), scans = listOf(scan)),
            asOfDate = asOfDate,
        )
        val visitsSection = html.substringAfter("Antenatal care visits log").substringBefore("</section>")
        assertTrue("Fluid: Normal must be printed in scan outcome extras", visitsSection.contains("Fluid: Normal"))
        assertTrue("Extras line contains Fluid: Normal", visitsSection.contains("""<span class="subtext">Fluid: Normal · Placenta: Posterior · FHR: 140</span>"""))
    }

    @Test
    fun visitsTable_visitNotePrintedOnceUnderScanOutcome_andNotAtAllWhenBlank() {
        val visitWithNote = DemoVisit(
            id = "v-note",
            date = "2026-09-01",
            bloodPressure = "120/80",
            weight = "65",
            findings = null,
            notes = "Patient complains of mild heartburn, advised antacids",
            revision = 1,
            isSyntheticEdit = false,
        )
        val scanWithNote = DemoUltrasoundScan(
            id = "s-note",
            patientId = "p-v2-1",
            date = "2026-09-01",
            linkedVisitId = "v-note",
            gestationalAgeWeeks = 24,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = "Adequate fetal growth",
        )
        val visitBlankNote = DemoVisit(
            id = "v-blank",
            date = "2026-09-15",
            bloodPressure = "118/76",
            weight = "66",
            findings = null,
            notes = "   ",
            revision = 1,
            isSyntheticEdit = false,
        )
        val visitNullNote = DemoVisit(
            id = "v-null",
            date = "2026-09-29",
            bloodPressure = "122/80",
            weight = "67",
            findings = null,
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )

        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(
                visits = listOf(visitWithNote, visitBlankNote, visitNullNote),
                scans = listOf(scanWithNote),
            ),
            asOfDate = asOfDate,
        )
        val visitsSection = html.substringAfter("Antenatal care visits log").substringBefore("</section>")

        // 1. Visit with note prints once under scan outcome
        val noteSubtext = """<span class="subtext">Note: Patient complains of mild heartburn, advised antacids</span>"""
        assertTrue("Visit note is printed under scan outcome", visitsSection.contains(noteSubtext))
        assertEquals("Visit note appears exactly once in the table", 1, visitsSection.split("Note: Patient complains of mild heartburn").size - 1)

        // 2. Visits with blank or null notes do not print Note:
        assertEquals("Only one Note: line is printed across the entire visits table", 1, visitsSection.split("Note:").size - 1)
    }

    @Test
    fun visitsTable_visitWithNoteAndNoScan_printsDashPlusNote() {
        val visitNoScanWithNote = DemoVisit(
            id = "v-noscan-note",
            date = "2026-09-01",
            bloodPressure = "120/80",
            weight = "65",
            findings = null,
            notes = "Blood glucose profile reviewed, target HbA1c achieved",
            revision = 1,
            isSyntheticEdit = false,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(visits = listOf(visitNoScanWithNote)),
            asOfDate = asOfDate,
        )
        val visitsSection = html.substringAfter("Antenatal care visits log").substringBefore("</section>")
        val visitRow = visitsSection.substringAfter("<tbody>").substringBefore("</tbody>")

        assertTrue(
            "Dash plus note line when visit has note but no scan",
            visitRow.contains("<td>-\n<span class=\"subtext\">Note: Blood glucose profile reviewed, target HbA1c achieved</span></td>"),
        )
    }

    @Test
    fun dynamicValuesEscaped_visitAndLabNotesWithScriptTags() {
        val scriptNote = "<script>alert('xss-note')</script>"
        val visit = DemoVisit(
            id = "v-xss",
            date = "2026-09-01",
            bloodPressure = "120/80",
            weight = "60",
            findings = null,
            notes = scriptNote,
            revision = 1,
            isSyntheticEdit = false,
        )
        val lab = DemoLabInvestigation(
            id = "l-xss-note",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Biochemistry",
            testName = "Serum Ferritin",
            result = "30",
            unit = "ng/mL",
            referenceRange = null,
            isAbnormal = false,
            notes = scriptNote,
            labName = "<b>CleanLab</b>",
        )
        val patient = testPatient(visits = listOf(visit), labs = listOf(lab))
        val html = PatientPrintHtmlGenerator.generateHtml(patient = patient, asOfDate = asOfDate)

        assertFalse("Raw <script> must never exist", html.contains("<script>"))
        assertFalse("Raw </script> must never exist", html.contains("</script>"))
        assertTrue("Visit note script escaped", html.contains("&lt;script&gt;alert(&#39;xss-note&#39;)&lt;/script&gt;"))
        assertTrue("Lab note script escaped", html.contains("&lt;script&gt;alert(&#39;xss-note&#39;)&lt;/script&gt;"))
        assertTrue("Lab name escaped", html.contains("&lt;b&gt;CleanLab&lt;/b&gt;"))
    }

    @Test
    fun labComparisonTable_labNoteWithLabNameUnderCorrectTable() {
        val hema1 = DemoLabInvestigation(
            id = "l-h1",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.0",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = false,
            notes = "Specimen slightly hemolyzed",
            labName = "Alpha Diagnostics",
        )
        val hema2 = DemoLabInvestigation(
            id = "l-h2",
            patientId = "p-v2-1",
            date = "2026-09-15",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.8",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = false,
            notes = null,
            labName = null,
        )
        val bio1 = DemoLabInvestigation(
            id = "l-b1",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Biochemistry",
            testName = "ALT",
            result = "22",
            unit = "U/L",
            referenceRange = "7-35",
            isAbnormal = false,
            notes = null,
            labName = null,
        )
        val bio2 = DemoLabInvestigation(
            id = "l-b2",
            patientId = "p-v2-1",
            date = "2026-09-15",
            testCategory = "Biochemistry",
            testName = "ALT",
            result = "25",
            unit = "U/L",
            referenceRange = "7-35",
            isAbnormal = false,
            notes = null,
            labName = null,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(labs = listOf(hema1, hema2, bio1, bio2)),
            asOfDate = asOfDate,
        )

        val labsSection = html.substringAfter("Laboratory investigations").substringBefore("</section>")
        val hemaTable = labsSection.substringAfter("Hematology").substringBefore("Biochemistry")
        val bioTable = labsSection.substringAfter("Biochemistry")

        assertTrue("Hematology has notes block", hemaTable.contains("""<div class="lab-notes">"""))
        assertTrue("Notes title is Notes", hemaTable.contains("""<div class="lab-notes-title">Notes</div>"""))
        assertTrue(
            "Note line with date, test, text and lab name in parentheses",
            hemaTable.contains("""<div class="lab-note-line">01/09/2026, Hemoglobin: Specimen slightly hemolyzed (Alpha Diagnostics)</div>"""),
        )

        assertFalse("Biochemistry has no notes block", bioTable.contains("""<div class="lab-notes">"""))
    }

    @Test
    fun labComparisonTable_twoNotesSortedByDate() {
        val hemaLater = DemoLabInvestigation(
            id = "l-h-later",
            patientId = "p-v2-1",
            date = "2026-09-20",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "12.0",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = false,
            notes = "Second sample post-infusion",
            labName = "Central Hospital",
        )
        val hemaEarlier = DemoLabInvestigation(
            id = "l-h-earlier",
            patientId = "p-v2-1",
            date = "2026-09-05",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "10.5",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = true,
            notes = "Initial baseline sample",
            labName = "Central Hospital",
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(labs = listOf(hemaLater, hemaEarlier)),
            asOfDate = asOfDate,
        )

        val labsSection = html.substringAfter("Laboratory investigations").substringBefore("</section>")
        val earlierPos = labsSection.indexOf("05/09/2026, Hemoglobin: Initial baseline sample (Central Hospital)")
        val laterPos = labsSection.indexOf("20/09/2026, Hemoglobin: Second sample post-infusion (Central Hospital)")

        assertTrue("Earlier note is present", earlierPos >= 0)
        assertTrue("Later note is present", laterPos >= 0)
        assertTrue("Earlier note appears before later note in notes block", earlierPos < laterPos)
    }

    @Test
    fun labComparisonTable_noNotesBlockWithoutNotes() {
        val hema1 = DemoLabInvestigation(
            id = "l-1",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "12.0",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = false,
            notes = null,
        )
        val hema2 = DemoLabInvestigation(
            id = "l-2",
            patientId = "p-v2-1",
            date = "2026-09-15",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "12.5",
            unit = "g/dL",
            referenceRange = "11.5-15.5",
            isAbnormal = false,
            notes = "   ",
        )
        val single = DemoLabInvestigation(
            id = "l-3",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Serology",
            testName = "Rubella IgG",
            result = "Positive",
            unit = null,
            referenceRange = null,
            isAbnormal = false,
            notes = null,
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(labs = listOf(hema1, hema2, single)),
            asOfDate = asOfDate,
        )
        val labsSection = html.substringAfter("Laboratory investigations").substringBefore("</section>")
        assertFalse("No notes block rendered when all notes are null or blank", labsSection.contains("""class="lab-notes""""))
        assertFalse("No notes title rendered when all notes are null or blank", labsSection.contains("""class="lab-notes-title""""))
    }

    @Test
    fun labSinglesTable_singleTestNotes() {
        val singleWithNote = DemoLabInvestigation(
            id = "l-single",
            patientId = "p-v2-1",
            date = "2026-09-01",
            testCategory = "Biochemistry",
            testName = "Serum Ferritin",
            result = "15",
            unit = "ng/mL",
            referenceRange = "20-200",
            isAbnormal = true,
            notes = "Low ferritin, oral iron therapy prescribed",
            labName = "Metropolis Lab",
        )
        val html = PatientPrintHtmlGenerator.generateHtml(
            patient = testPatient(labs = listOf(singleWithNote)),
            asOfDate = asOfDate,
        )
        val labsSection = html.substringAfter("Laboratory investigations").substringBefore("</section>")
        val singlesSection = labsSection.substringAfter("Single tests")

        assertTrue("Singles table has notes block", singlesSection.contains("""<div class="lab-notes">"""))
        assertTrue("Notes title present", singlesSection.contains("""<div class="lab-notes-title">Notes</div>"""))
        assertTrue(
            "Note line for single test formatted correctly",
            singlesSection.contains("""<div class="lab-note-line">01/09/2026, Serum Ferritin: Low ferritin, oral iron therapy prescribed (Metropolis Lab)</div>"""),
        )
    }
}
