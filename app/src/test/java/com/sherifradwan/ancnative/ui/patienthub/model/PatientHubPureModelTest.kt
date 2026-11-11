package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.FhrLevel
import com.sherifradwan.ancnative.model.SfhConcordance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientHubPureModelTest {

    @Test
    fun testPartitionReportIntoCompositeGroups_cbcIdentified() {
        val labs = listOf(
            DemoLabInvestigation(
                id = "1",
                patientId = "p1",
                date = "2026-11-01",
                testCategory = "Hematology",
                testName = "Hemoglobin",
                result = "11.2",
                unit = "g/dL",
                referenceRange = "11.5-15.0",
                isAbnormal = true,
                notes = null,
            ),
            DemoLabInvestigation(
                id = "2",
                patientId = "p1",
                date = "2026-11-01",
                testCategory = "Hematology",
                testName = "Platelet Count",
                result = "240",
                unit = "x10^9/L",
                referenceRange = "150-450",
                isAbnormal = false,
                notes = null,
            ),
            DemoLabInvestigation(
                id = "3",
                patientId = "p1",
                date = "2026-11-01",
                testCategory = "Biochemistry",
                testName = "Blood Glucose",
                result = "95",
                unit = "mg/dL",
                referenceRange = "70-110",
                isAbnormal = false,
                notes = null,
            ),
        )

        val groups = partitionReportIntoCompositeGroups(labs)
        assertEquals(2, groups.size)
        val cbcGroup = groups.first { it.isComposite }
        assertEquals("Complete Blood Count (CBC)", cbcGroup.title)
        assertEquals(2, cbcGroup.items.size)
        assertTrue(cbcGroup.isAnyAbnormal)
        assertEquals(1, cbcGroup.abnormalCount)

        val standaloneGroup = groups.first { !it.isComposite }
        assertEquals("Individual Investigations", standaloneGroup.title)
        assertEquals(1, standaloneGroup.items.size)
        assertEquals("Blood Glucose", standaloneGroup.items[0].testName)
    }

    @Test
    fun testGroupLabsIntoReports_groupsByDateAndLabName() {
        val labs = listOf(
            DemoLabInvestigation(id = "1", patientId = "p1", date = "2026-11-01", testCategory = "Blood", testName = "CBC", result = "Normal", unit = null, referenceRange = null, isAbnormal = false, notes = null, labName = "Central Lab"),
            DemoLabInvestigation(id = "2", patientId = "p1", date = "2026-11-01", testCategory = "Urine", testName = "Protein", result = "Nil", unit = null, referenceRange = null, isAbnormal = false, notes = null, labName = "Central Lab"),
            DemoLabInvestigation(id = "3", patientId = "p1", date = "2026-10-15", testCategory = "Blood", testName = "FBS", result = "88", unit = null, referenceRange = null, isAbnormal = false, notes = null, labName = "City Lab"),
        )

        val reports = groupLabsIntoReports(labs)
        assertEquals(2, reports.size)
        assertEquals("2026-11-01", reports[0].date)
        assertEquals("Central Lab", reports[0].labName)
        assertEquals(2, reports[0].items.size)

        assertEquals("2026-10-15", reports[1].date)
        assertEquals("City Lab", reports[1].labName)
        assertEquals(1, reports[1].items.size)
    }

    @Test
    fun testDeriveVitalsSnapshot_severeHypertensionAndPreEclampsia() {
        val snapshot = deriveVitalsSnapshot(
            bloodPressure = "165/112",
            weight = "78.5 kg",
            findings = "FHR: 142 bpm · Urine: +2",
            gaWeeks = 34,
            bookingWeightKg = 72.0,
        )

        assertEquals(BpLevel.SEVERE, snapshot.bpLevel)
        assertTrue(snapshot.isPreEclampsiaWarning)
        assertEquals("142", snapshot.fhrText)
        assertEquals(FhrLevel.NORMAL, snapshot.fhrLevel)
        assertEquals("+2", snapshot.urineProtein)
        assertEquals("Δ +6.5 kg", snapshot.weightDeltaText)
    }

    @Test
    fun testDeriveVitalsSnapshot_normalVitals() {
        val snapshot = deriveVitalsSnapshot(
            bloodPressure = "118/76",
            weight = "65.0 kg",
            findings = "FHR: 138 bpm",
            gaWeeks = 28,
            bookingWeightKg = 65.0,
        )

        assertEquals(BpLevel.NORMAL, snapshot.bpLevel)
        assertFalse(snapshot.isPreEclampsiaWarning)
        assertEquals(FhrLevel.NORMAL, snapshot.fhrLevel)
        assertNull(snapshot.weightDeltaText)
    }

    @Test
    fun testBiometryValues_extractionAndEmptyCheck() {
        val emptyScan = DemoUltrasoundScan(
            id = "s1", patientId = "p1", date = "2026-11-01",
            gestationalAgeWeeks = null, gestationalAgeDays = null,
            fetalNumber = null, presentation = null,
            biometryBpd = null, biometryHc = null, biometryAc = null, biometryFl = null,
            estimatedFetalWeight = null, fetalHeartRate = null, amnioticFluid = null, placentalSite = null,
            impression = null,
        )
        val emptyValues = extractBiometry(emptyScan)
        assertTrue(emptyValues.isEmpty)
        assertFalse(emptyValues.hasAnyData)

        val populatedScan = DemoUltrasoundScan(
            id = "s2", patientId = "p1", date = "2026-11-01",
            gestationalAgeWeeks = 32, gestationalAgeDays = 4,
            fetalNumber = "Single", presentation = "Cephalic",
            biometryBpd = "84.2", biometryHc = "302.1", biometryAc = "289.4", biometryFl = "65.0",
            estimatedFetalWeight = "2150g", fetalHeartRate = "140", amnioticFluid = "Normal (AFI 14)", placentalSite = "Anterior High",
            impression = "Normal anatomy",
        )
        val populatedValues = extractBiometry(populatedScan)
        assertFalse(populatedValues.isEmpty)
        assertTrue(populatedValues.hasAnyData)
        assertEquals("84.2", populatedValues.bpd)
        assertEquals("302.1", populatedValues.hc)
        assertEquals("289.4", populatedValues.ac)
        assertEquals("65.0", populatedValues.fl)
        assertEquals("2150g", populatedValues.efw)
    }

    @Test
    fun testFilterMedicationTiles_searchAndTabs() {
        val tiles = listOf(
            MedicationTile("1", "Paracetamol", "500 mg", "Oral", "Once daily"),
            MedicationTile("2", "Ferrous Sulfate", "200 mg", "Oral", "Once daily"),
            MedicationTile("3", "Calcium Carbonate", "500 mg", "Oral", "Twice daily"),
        )
        val pastMeds = setOf("paracetamol")

        val commonAll = filterMedicationTiles(tiles, selectedTab = "common", searchQuery = "", pastMedicationNames = pastMeds)
        assertEquals(3, commonAll.size)

        val searchResult = filterMedicationTiles(tiles, selectedTab = "common", searchQuery = "ferr", pastMedicationNames = pastMeds)
        assertEquals(1, searchResult.size)
        assertEquals("Ferrous Sulfate", searchResult[0].name)

        val recentResult = filterMedicationTiles(tiles, selectedTab = "recent", searchQuery = "", pastMedicationNames = pastMeds)
        assertEquals(1, recentResult.size)
        assertEquals("Paracetamol", recentResult[0].name)
    }

    @Test
    fun recentMatchesStoredNamesWhateverTheirCaseOrSpaces() {
        val tiles = listOf(
            MedicationTile("1", "Paracetamol", "500 mg", "Oral", "Once daily"),
            MedicationTile("2", "Ferrous Sulfate", "200 mg", "Oral", "Once daily"),
        )
        // As the picker passes them: the patient's stored names, untouched.
        val stored = setOf("Ferrous Sulfate ", "PARACETAMOL")
        val recent = filterMedicationTiles(tiles, selectedTab = "recent", searchQuery = "", pastMedicationNames = stored)
        assertEquals(listOf("1", "2"), recent.map { it.id })
        val recentSearch = filterMedicationTiles(tiles, selectedTab = "recent", searchQuery = " FERR ", pastMedicationNames = stored)
        assertEquals(listOf("2"), recentSearch.map { it.id })
    }

    @Test
    fun searchWithNoMatchIsEmptyInBothSources() {
        val tiles = listOf(MedicationTile("1", "Paracetamol", "500 mg", "Oral", "Once daily"))
        assertTrue(filterMedicationTiles(tiles, "common", "zzz", emptySet()).isEmpty())
        assertTrue(filterMedicationTiles(tiles, "recent", "", emptySet()).isEmpty())
    }

    private fun medRecord(name: String, status: String, nonCompliant: Boolean = false, route: String = "Oral") = com.sherifradwan.ancnative.model.DemoMedicationRecord(
        id = name, patientId = "p", name = name, dosage = "1", route = route, frequency = "Once daily",
        startDate = "2026-01-01", endDate = null, status = status, notes = null, isProphylaxis = false,
        isNonCompliant = nonCompliant, revision = 1,
    )

    @Test
    fun aTileCountsAsActiveWhateverItsSpacesOrCase() {
        val meds = listOf(
            medRecord("Folic Acid", "Active"),
            medRecord("Labetalol", "Active", nonCompliant = true),
            medRecord("Aspirin", "Discontinued"),
        )
        val active = activeMedicationKeys(meds)
        assertEquals(setOf("folic acid|oral", "labetalol|oral"), active)
        assertTrue(MedicationTile("1", "  folic ACID ", "5 mg", "Oral", "Once daily").isAlreadyActive(active))
        assertTrue("non-compliant is still active", MedicationTile("2", "LABETALOL", "", "Oral", "").isAlreadyActive(active))
        assertFalse("stopped is not active", MedicationTile("3", "Aspirin", "75 mg", "Oral", "Once daily").isAlreadyActive(active))
        assertFalse(MedicationTile("4", "Folic", "5 mg", "Oral", "Once daily").isAlreadyActive(active))
        assertEquals("folic acid", medicationNameKey("\tFolic ACID \n"))
    }

    @Test
    fun tilesAreTheListThenTheDoctorsOwnNamesWithBlankDoseAndFrequency() {
        val tiles = medicationTiles(
            listOf(com.sherifradwan.ancnative.model.catalog.MedicationCatalogItem("c1", "Folic Acid", "5 mg", "Oral", "Once daily")),
            listOf(com.sherifradwan.ancnative.model.catalog.CustomChoiceItem(id = "x", list = com.sherifradwan.ancnative.model.catalog.CustomChoiceList.MEDICATION_NAME, label = "My drug")),
        )
        assertEquals(listOf("c1", "custom_x"), tiles.map { it.id })
        assertEquals(MedicationTile("custom_x", "My drug", "", "Oral", "", isCustom = true), tiles[1])
    }

    @Test
    fun theAlreadyActiveCheckMatchesNameAndRouteTogether() {
        val active = activeMedicationKeys(
            listOf(
                medRecord("Ferrous Sulfate", "Active"),
                medRecord("Aspirin", "Discontinued"),
                medRecord("Labetalol", "Active", nonCompliant = true),
            ),
        )
        assertEquals(setOf("ferrous sulfate|oral", "labetalol|oral"), active)
        assertTrue("same name and route, spaces and case ignored", MedicationTile("1", " Ferrous SULFATE ", "200 mg", " oral ", "Once daily").isAlreadyActive(active))
        assertFalse("same name on another route is a different drug", MedicationTile("2", "ferrous sulfate", "100 mg", "IM", "Once daily").isAlreadyActive(active))
        assertFalse("stopped does not count on any route", MedicationTile("3", "Aspirin", "75 mg", "Oral", "Once daily").isAlreadyActive(active))
        assertTrue("non-compliant is still active", MedicationTile("4", "labetalol", "", "Oral", "").isAlreadyActive(active))
        assertEquals("ferrous sulfate|oral", medicationNameRouteKey("\tFerrous sulfate ", " ORAL "))
        val both = activeMedicationKeys(listOf(medRecord("Iron", "Active"), medRecord("Iron", "Active", route = "IM")))
        assertTrue(MedicationTile("5", "Iron", "", "Oral", "").isAlreadyActive(both))
        assertTrue(MedicationTile("6", "Iron", "", "IM", "").isAlreadyActive(both))
        assertFalse(MedicationTile("7", "Iron", "", "IV", "").isAlreadyActive(both))
    }
}
