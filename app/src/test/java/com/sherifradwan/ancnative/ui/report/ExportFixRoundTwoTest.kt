package com.sherifradwan.ancnative.ui.report

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.DeliveryNoteSections
import com.sherifradwan.ancnative.model.composeDeliveryNotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExportFixRoundTwoTest {

    private val asOf = LocalDate.of(2026, 10, 28)

    private fun basePatient(
        id: String = "p1",
        name: String = "Test Patient",
        visits: List<DemoVisit> = emptyList(),
        scans: List<DemoUltrasoundScan> = emptyList(),
        labs: List<DemoLabInvestigation> = emptyList(),
        meds: List<DemoMedicationRecord> = emptyList(),
        previous: List<DemoPreviousPregnancy> = emptyList(),
        riskLevel: String? = null,
        bloodGroupText: String? = null,
        dateOfBirth: String? = null,
        addressText: String? = null,
        clinicalAttention: String? = null,
        familyHistory: String? = null,
        obstetricNotes: String? = null,
        status: String? = null,
        delivered: Boolean = false,
        deliveryDate: String? = null,
        deliveryMode: String? = null,
        deliveryOutcome: String? = null,
        dating: DemoPatientDating? = null,
    ): DemoPatient {
        return DemoPatient(
            id = id,
            caseId = "CASE-$id",
            displayName = name,
            dateOfBirth = dateOfBirth,
            phone = null,
            visits = visits,
            riskLevel = riskLevel,
            ageText = "30",
            bloodGroupText = bloodGroupText,
        ).copy(
            ultrasoundScans = scans,
            labInvestigations = labs,
            medications = meds,
            previousPregnancies = previous,
            addressText = addressText,
            clinicalAttention = clinicalAttention,
            familyHistory = familyHistory,
            obstetricNotes = obstetricNotes,
            status = status,
            delivered = delivered,
            deliveryDate = deliveryDate,
            deliveryMode = deliveryMode,
            deliveryOutcome = deliveryOutcome,
            dating = dating,
        )
    }

    private fun html(patient: DemoPatient): String =
        PatientPrintHtmlGenerator.generateHtml(context = null, patient = patient, asOfDate = asOf)

    private fun share(patient: DemoPatient): String = patientShareText(patient, asOf)

    private fun visit(id: String, date: String, weight: String? = null): DemoVisit =
        DemoVisit(
            id = id,
            date = date,
            bloodPressure = "120/80",
            weight = weight,
            findings = "ok",
            notes = null,
            revision = 1,
            isSyntheticEdit = false,
        )

    private fun scan(
        id: String,
        date: String,
        placentalSite: String? = null,
        amnioticFluid: String? = null,
        presentation: String? = null,
        fetalNumber: String? = null,
        fetalHeartRate: String? = null,
        impression: String? = null,
    ): DemoUltrasoundScan =
        DemoUltrasoundScan(
            id = id,
            patientId = "p1",
            date = date,
            gestationalAgeWeeks = 20,
            gestationalAgeDays = 0,
            fetalNumber = fetalNumber,
            presentation = presentation,
            placentalSite = placentalSite,
            amnioticFluid = amnioticFluid,
            fetalHeartRate = fetalHeartRate,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = impression,
        )

    private fun med(id: String, name: String, status: String): DemoMedicationRecord =
        DemoMedicationRecord(
            id = id,
            patientId = "p1",
            name = name,
            dosage = "1 tab",
            route = "PO",
            frequency = "daily",
            startDate = "2026-01-01",
            endDate = null,
            status = status,
            notes = null,
            isProphylaxis = false,
        )

    @Test
    fun oracleA_printPagingMarkersAndRules() {
        val h = html(basePatient())
        assertFalse(h.contains("Page 1"))
        assertFalse(h.contains("Page 2"))
        assertFalse(h.contains("Page:"))
        assertTrue(h.contains("@page"))
        assertTrue(h.contains("break-inside: avoid"))
        assertTrue(h.contains("CONTINUED"))
    }

    @Test
    fun oracleB_weightFormattingInVisitCells() {
        val p = basePatient(
            visits = listOf(
                visit("v1", "2026-10-01", weight = "63 kg"),
                visit("v2", "2026-10-08", weight = "68.5"),
                visit("v3", "2026-10-15", weight = ""),
            ),
        )
        val h = html(p)
        assertTrue(h.contains("""<td class="text-center nowrap-cell">63 kg</td>"""))
        assertFalse(h.contains("kg kg"))
        assertTrue(h.contains("""<td class="text-center nowrap-cell">68.5 kg</td>"""))
        assertTrue(h.contains("""<td class="text-center nowrap-cell">-</td>"""))
    }

    @Test
    fun oracleC_archivedDeliveredShowsOutcomeInHighlightAndArchivedInBanner() {
        val p = basePatient(
            status = "archived",
            delivered = true,
            deliveryDate = "2026-10-08",
            deliveryMode = "vaginal",
            deliveryOutcome = "live_birth",
        )
        val h = html(p)
        val highlight = h.substringAfter("class=\"ga-highlight\">").substringBefore("</div>")
        val banner = h.substringAfter("class=\"status-banner").substringAfter(">").substringBefore("</div>")
        assertFalse(highlight.contains("Archived record"))
        assertTrue(banner.contains("Archived record"))
    }

    @Test
    fun oracleD_reportDeliveryRowsAndLabelsInHtmlAndShare() {
        val dating = DemoPatientDating(
            patientId = "p1",
            lmpDate = "2026-01-21",
            ultrasoundEdd = null,
            agreedEdd = "2026-10-28",
            datingMethod = "LMP",
            notes = null,
        )
        val eddText = "28/10/2026"

        val delivered = basePatient(
            delivered = true,
            deliveryDate = "2026-10-08",
            deliveryMode = "vaginal",
            deliveryOutcome = "live_birth",
        )
        val deliveredRows = reportDeliveryRows(delivered, eddText)
        assertEquals("Delivery Outcome", deliveredRows[0].label)

        val rhNeg = basePatient(bloodGroupText = "O-", dating = dating)
        val rhRows = reportDeliveryRows(rhNeg, eddText)
        assertTrue(rhRows.any { it.label == "Blood bank precautions" && it.value.contains("Anti-D Prophylaxis") })
        assertTrue(rhRows.any { it.label == "Estimated due date" && it.value == eddText })

        val noPlan = basePatient(dating = dating)
        val noPlanRows = reportDeliveryRows(noPlan, null)
        assertTrue(noPlanRows.any { it.label == "Plan" && it.value == NOT_RECORDED })
        assertFalse(noPlanRows.any { it.label == "Estimated due date" })

        val fullNotes = composeDeliveryNotes(
            DeliveryNoteSections(
                hospital = "City Hospital",
                fees = "5000 EGP",
                plan = "vaginal",
                postpartum = "Review in 6 weeks",
            ),
        )
        val fullPlan = basePatient(obstetricNotes = fullNotes, dating = dating)
        val fullRows = reportDeliveryRows(fullPlan, eddText)
        assertTrue(fullRows.any { it.label == "Hospital" && it.value == "City Hospital" })
        assertTrue(fullRows.any { it.label == "Postpartum" && it.value == "Review in 6 weeks" })
        assertFalse(fullRows.any { it.value.contains("5000") })

        val hFull = html(fullPlan)
        val sFull = share(fullPlan)
        assertTrue(hFull.contains("Hospital:"))
        assertTrue(hFull.contains("Postpartum:"))
        assertFalse(hFull.contains("5000"))
        assertTrue(sFull.contains("Hospital: City Hospital"))
        assertTrue(sFull.contains("Postpartum: Review in 6 weeks"))
        assertFalse(sFull.contains("5000"))
    }

    @Test
    fun oracleE_shareScanFluidNoTrailingNotRecordedAndEmptyLine() {
        val withFluid = basePatient(
            scans = listOf(scan("s1", "2026-10-01", placentalSite = "Anterior", amnioticFluid = "Normal")),
        )
        val sFluid = share(withFluid)
        assertTrue(sFluid.contains("Placenta: Anterior"))
        assertTrue(sFluid.contains("Fluid: Normal"))
        assertFalse(sFluid.contains("Fluid: Normal | Not recorded"))

        val emptyScan = basePatient(
            scans = listOf(scan("s2", "2026-10-02").copy(gestationalAgeWeeks = null, gestationalAgeDays = null)),
        )
        val sEmpty = share(emptyScan)
        assertTrue(sEmpty.contains("02/10/2026 | Not recorded"))
    }

    @Test
    fun oracleG_previousPregnancyBlankFieldsExamples() {
        val prev1 = DemoPreviousPregnancy(
            id = "pp1",
            patientId = "p1",
            modelVersion = 1,
            dateText = "2018",
            outcomeText = "",
            pregnancyType = "Singleton",
            otherDetails = null,
            placeAndDoctor = null,
            deliveryBy = "CS",
            outcome = "LiveBirth",
            birthMethod = "",
            lossManagement = null,
            terminationMethod = null,
            provenanceKind = "t",
            clinicianId = "c",
            recordedAt = "2026-01-01",
            updatedAt = "2026-01-01",
            revision = 1,
        )
        val prev2 = DemoPreviousPregnancy(
            id = "pp2",
            patientId = "p1",
            modelVersion = 1,
            dateText = "2016",
            outcomeText = "",
            pregnancyType = null,
            otherDetails = null,
            placeAndDoctor = null,
            deliveryBy = null,
            outcome = "LiveBirth",
            birthMethod = null,
            lossManagement = null,
            terminationMethod = null,
            provenanceKind = "t",
            clinicianId = "c",
            recordedAt = "2026-01-01",
            updatedAt = "2026-01-01",
            revision = 1,
        )
        val h = html(basePatient(previous = listOf(prev1, prev2)))
        assertTrue(h.contains("Singleton"))
        assertTrue(h.contains("CS"))
        assertTrue(h.contains("Live Birth"))
    }

    @Test
    fun visitsPrintLastTwelveNumbersAndNotFirstThree() {
        val visits = (1..15).map { i ->
            visit("v$i", LocalDate.of(2026, 1, i.coerceAtMost(28)).toString(), weight = "60")
        }
        val h = html(basePatient(visits = visits))
        for (n in 4..15) {
            assertTrue("visit $n", h.contains("""<td class="text-center font-bold nowrap-cell">$n</td>"""))
        }
        assertFalse(h.contains("""<td class="text-center font-bold nowrap-cell">1</td>"""))
        assertFalse(h.contains("<td class=\"date-cell\">2</td>"))
        assertFalse(h.contains("<td class=\"date-cell\">3</td>"))
    }

    @Test
    fun medicationsGroupedByStatusAndStoppedTitleAbsentWhenNone() {
        val withBoth = basePatient(
            meds = listOf(
                med("m1", "Folic Acid", "Active"),
                med("m2", "Old Drug", "Discontinued"),
            ),
        )
        val hBoth = html(withBoth)
        val currentTitle = hBoth.indexOf("Current Medications")
        val stoppedTitle = hBoth.indexOf("Stopped Medications")
        val folic = hBoth.indexOf("Folic Acid")
        val old = hBoth.indexOf("Old Drug")
        assertTrue(currentTitle >= 0)
        assertTrue(stoppedTitle > currentTitle)
        assertTrue(folic > currentTitle && folic < stoppedTitle)
        assertTrue(old > stoppedTitle)

        val onlyActive = basePatient(meds = listOf(med("m3", "Iron", "Active")))
        val hActive = html(onlyActive)
        assertTrue(hActive.contains("Current Medications"))
        assertFalse(hActive.contains("Stopped Medications"))
    }

    @Test
    fun dateOfBirthFormattedAndOptionalFieldsPresentOrAbsent() {
        val p = basePatient(
            dateOfBirth = "1995-01-01",
            addressText = "12 Nile Street",
            clinicalAttention = "Monitor BP",
        )
        val h = html(p)
        assertTrue(h.contains("Date of birth: 01/01/1995"))
        assertTrue(h.contains("12 Nile Street"))
        assertTrue(h.contains("Monitor BP"))

        val blank = basePatient(dateOfBirth = null, addressText = null, clinicalAttention = null)
        val hBlank = html(blank)
        assertFalse(hBlank.contains("Date of birth:"))
        assertFalse(hBlank.contains("Address:"))
        assertFalse(hBlank.contains("Clinical attention:"))
    }

    @Test
    fun familyHistoryNotRecordedWhenBlank() {
        val h = html(basePatient(familyHistory = null))
        assertTrue(h.contains("Family History"))
        assertTrue(h.contains("Not recorded"))
    }

    @Test
    fun deliveryPlanShowsHospitalAndPostpartumAndShareStatusRules() {
        val notes = composeDeliveryNotes(
            DeliveryNoteSections(
                hospital = "City Hospital",
                fees = "5000 EGP",
                plan = "vaginal",
                postpartum = "Review in 6 weeks",
            ),
        )
        val delivered = basePatient(
            delivered = true,
            deliveryDate = "2026-10-08",
            deliveryMode = "vaginal",
            deliveryOutcome = "live_birth",
            obstetricNotes = notes,
            dating = DemoPatientDating(
                patientId = "p1",
                lmpDate = "2026-01-21",
                ultrasoundEdd = null,
                agreedEdd = "2026-10-28",
                datingMethod = "LMP",
                notes = null,
            ),
        )
        val h = html(delivered)
        assertTrue(h.contains("Hospital:"))
        assertTrue(h.contains("Postpartum:"))
        assertFalse(h.contains("5000"))

        val s = share(delivered)
        assertTrue(s.contains("Status: Delivered"))
        assertFalse(s.contains("Gestational age today"))
    }

    @Test
    fun p4_shareTextWeightsUseFormatWeightKg() {
        val patient = basePatient(
            visits = listOf(
                visit("v1", "2026-10-01", weight = "68.5"),
                visit("v2", "2026-10-02", weight = "63 kg"),
                visit("v3", "2026-10-03", weight = ""),
            ),
        )
        val s = share(patient)
        assertTrue(s.contains(" | Wt: 68.5 kg | "))
        assertTrue(s.contains(" | Wt: 63 kg | "))
        assertFalse(s.contains("kg kg"))
        val blankWeightLine = s.lines().first { it.contains("03/10/2026") }
        assertFalse(blankWeightLine.contains("Wt:"))
    }

    @Test
    fun p5_shareTextVisitsNewestFirstWithUndatedFirst() {
        val patient = basePatient(
            visits = listOf(
                visit("v1", "2026-01-10"),
                visit("v2", "2026-03-01"),
                visit("v3", "2026-02-01"),
                visit("v4", ""),
            ),
        )
        val s = share(patient)
        val visitsBlock = s.substringAfter("VISITS (").substringBefore("ULTRASOUND SCANS")
        val undated = visitsBlock.indexOf("Not recorded")
        val march = visitsBlock.indexOf("01/03/2026")
        val february = visitsBlock.indexOf("01/02/2026")
        val january = visitsBlock.indexOf("10/01/2026")
        assertTrue(undated >= 0)
        assertTrue(march >= 0)
        assertTrue(february >= 0)
        assertTrue(january >= 0)
        assertTrue(undated < march)
        assertTrue(march < february)
        assertTrue(february < january)
    }

    @Test
    fun p6_blankIdentityFieldsPrintPlaceholders() {
        val h = html(basePatient(bloodGroupText = ""))
        assertTrue(h.contains("Phone: Unspecified"))
        val bloodGroupCell = h
            .substringAfter("<span class=\"data-label\">Blood Group</span>")
            .substringBefore("</div>")
        assertEquals("-", bloodGroupCell.replace(Regex("<[^>]*>"), "").trim())
    }

    @Test
    fun p123_printCssKeepsTablesWholeAndMarginsOnEverySheet() {
        val h = html(basePatient())
        val printCss = h.substringAfter("@media print")
        assertTrue(printCss.contains("table-header-group"))
        assertTrue(printCss.contains("@page { size: A4; margin: 8mm 0 8mm 0; }"))
        assertFalse(printCss.contains("tr, .section-box"))
    }
}
