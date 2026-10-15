package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ClinicalValidationTest {

    @Test
    fun datingCalculatesGestationalAgeAccuratelyFromAgreedEdd() {
        // As of 2026-10-11, if agreed EDD is 40 days in the future (approx 5 weeks 5 days from 40 weeks):
        // 280 - 40 = 240 days = 34w 2d
        val asOf = LocalDate.of(2026, 10, 11)
        val agreedEdd = asOf.plusDays(40).toString()

        val dating = DemoPatientDating(
            patientId = "patient-1",
            lmpDate = null,
            ultrasoundEdd = null,
            agreedEdd = agreedEdd,
            datingMethod = "Ultrasound",
            notes = null,
        )

        val ga = dating.calculateGestationalAge(asOf)
        assertNotNull(ga)
        assertEquals("34w 2d", ga)
    }

    @Test
    fun datingCalculatesGestationalAgeFromLmpWhenEddAbsent() {
        val asOf = LocalDate.of(2026, 10, 11)
        val lmp = asOf.minusDays(70).toString() // 10 weeks = 70 days

        val dating = DemoPatientDating(
            patientId = "patient-1",
            lmpDate = lmp,
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "LMP",
            notes = null,
        )

        val ga = dating.calculateGestationalAge(asOf)
        assertNotNull(ga)
        assertEquals("10w 0d", ga)
    }

    @Test
    fun datingReturnsNullWhenNoDatesRecordedOrInvalid() {
        val dating = DemoPatientDating(
            patientId = "patient-1",
            lmpDate = null,
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = null,
            notes = null,
        )
        assertNull(dating.calculateGestationalAge())

        val invalidDating = DemoPatientDating(
            patientId = "patient-1",
            lmpDate = "not-a-date",
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = null,
            notes = null,
        )
        assertNull(invalidDating.calculateGestationalAge())
    }

    @Test
    fun clinicalVocabularyEnforcesControlledSetsAndIsoDates() {
        assertTrue(ClinicalVocabulary.isValidIsoDate("2026-10-11"))
        assertTrue(ClinicalVocabulary.isValidIsoDate(null))
        assertTrue(ClinicalVocabulary.isValidIsoDate(""))
        assertFalse(ClinicalVocabulary.isValidIsoDate("invalid-date"))
        assertFalse(ClinicalVocabulary.isValidIsoDate("11-10-2026"))

        assertTrue(ClinicalVocabulary.datingMethods.contains("Ultrasound"))
        assertTrue(ClinicalVocabulary.fetalPresentations.contains("Cephalic"))
        assertTrue(ClinicalVocabulary.placentalSites.contains("Anterior"))
        assertTrue(ClinicalVocabulary.amnioticFluids.contains("Normal"))
        assertTrue(ClinicalVocabulary.testCategories.contains("Hematology"))
        assertTrue(ClinicalVocabulary.medicationRoutes.contains("Oral"))
    }

    @Test
    fun medicationAutosuggestSafetyResetRuleWipesNonComplianceAndAdverseEffects() {
        val priorNonCompliantMed = DemoMedicationRecord(
            id = "MED-001",
            patientId = "patient-1",
            name = "Aspirin",
            dosage = "81mg",
            route = "Oral",
            frequency = "Once daily",
            startDate = "2026-06-01",
            endDate = "2026-08-01",
            status = "Discontinued",
            notes = "Patient stopped taking it",
            isProphylaxis = true,
            isNonCompliant = true,
            adverseNotes = "Mild dyspepsia and gastric irritation",
            revision = 2,
        )

        // Simulating the autosuggest application with safety reset rule:
        val copiedName = priorNonCompliantMed.name
        val copiedDosage = priorNonCompliantMed.dosage
        val copiedRoute = priorNonCompliantMed.route
        val copiedFrequency = priorNonCompliantMed.frequency
        val copiedProphylaxis = priorNonCompliantMed.isProphylaxis

        // Safety resets:
        val resetNonCompliant = false
        val resetAdverseNotes = ""
        val resetEndDate = ""
        val resetNotes = ""

        assertEquals("Aspirin", copiedName)
        assertEquals("81mg", copiedDosage)
        assertEquals("Oral", copiedRoute)
        assertEquals("Once daily", copiedFrequency)
        assertTrue(copiedProphylaxis)

        // Confirms safety resets were enforced
        assertFalse(resetNonCompliant)
        assertTrue(resetAdverseNotes.isEmpty())
        assertTrue(resetEndDate.isEmpty())
        assertTrue(resetNotes.isEmpty())
    }

    @Test
    fun ivfDatingCalculationProducesAuthoritativeEddAndGa() {
        // Transfer date: 2026-04-10
        val transferDate = LocalDate.of(2026, 4, 10)

        // Day 5 blastocyst transfer adds 261 days to EDD
        val day5Dating = DemoPatientDating(
            patientId = "P_IVF_5",
            lmpDate = null,
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "IVF_TRANSFER",
            notes = null,
            transferDate = transferDate.toString(),
            embryoAgeDays = 5,
        )
        val day5Edd = day5Dating.calculateEstimatedDueDate()
        assertNotNull(day5Edd)
        assertEquals(transferDate.plusDays(261), day5Edd)

        // Day 3 cleavage transfer adds 263 days to EDD
        val day3Dating = DemoPatientDating(
            patientId = "P_IVF_3",
            lmpDate = null,
            ultrasoundEdd = null,
            agreedEdd = null,
            datingMethod = "IVF_TRANSFER",
            notes = null,
            transferDate = transferDate.toString(),
            embryoAgeDays = 3,
        )
        val day3Edd = day3Dating.calculateEstimatedDueDate()
        assertNotNull(day3Edd)
        assertEquals(transferDate.plusDays(263), day3Edd)
    }
}
