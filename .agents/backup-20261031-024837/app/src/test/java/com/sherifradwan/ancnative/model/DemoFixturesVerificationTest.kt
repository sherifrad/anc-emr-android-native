package com.sherifradwan.ancnative.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoFixturesVerificationTest {

    @Test
    fun syntheticFixturesContainCompleteLongitudinalCaseForDemo002() {
        val patients = DemoFixtures.patients
        assertEquals(4, patients.size)

        val demo002 = patients.firstOrNull { it.caseId == "DEMO-ANC-002" }
        assertNotNull("DEMO-ANC-002 must be present in synthetic fixtures", demo002)
        demo002!!

        assertEquals("نور المثال", demo002.displayName)
        assertEquals("34", demo002.ageText)
        assertEquals("High Risk", demo002.riskLevel)
        assertEquals("A+", demo002.bloodGroupText)
        assertTrue(demo002.clinicalAttention?.startsWith("[ALERT]") == true)

        // History assertions
        assertEquals("No significant medical history", demo002.medicalHistory)
        assertEquals("Appendectomy (2018)", demo002.surgicalHistory)
        assertEquals("Family history of Type 2 Diabetes (maternal grandmother)", demo002.familyHistory)
        assertEquals("No known drug allergies (NKDA)", demo002.allergyHistory)

        // Dating engine integration
        val dating = demo002.dating
        assertNotNull("Dating must be present", dating)
        assertEquals("2026-02-13", dating!!.lmpDate)
        assertEquals("2026-11-20", dating.ultrasoundEdd)
        assertEquals("2026-11-20", dating.agreedEdd)
        assertEquals("Ultrasound", dating.datingMethod)
        assertEquals("Ultrasound dating at 12w confirmed EDD 2026-11-20 (aligned with reported regular LMP 2026-02-13).", dating.notes)

        // 5 Sequential longitudinal visits
        assertEquals(5, demo002.visits.size)
        val sortedVisits = demo002.visits.sortedBy { it.date }
        assertEquals("2026-04-10", sortedVisits[0].date)
        assertEquals("2026-05-08", sortedVisits[1].date)
        assertEquals("2026-07-03", sortedVisits[2].date)
        assertEquals("2026-08-28", sortedVisits[3].date)
        assertEquals("2026-10-10", sortedVisits[4].date)

        // 4 Ultrasound scans (3 linked, 1 unlinked)
        assertEquals(4, demo002.ultrasoundScans.size)
        val linkedScans = demo002.ultrasoundScans.filter { it.linkedVisitId != null }
        val unlinkedScans = demo002.ultrasoundScans.filter { it.linkedVisitId == null }
        assertEquals(3, linkedScans.size)
        assertEquals(1, unlinkedScans.size)

        val scan1 = demo002.ultrasoundScans.find { it.date == "2026-05-08" }!!
        assertEquals(12, scan1.gestationalAgeWeeks)
        assertEquals(0, scan1.gestationalAgeDays)

        val scan2 = demo002.ultrasoundScans.find { it.date == "2026-07-03" }!!
        assertEquals(20, scan2.gestationalAgeWeeks)
        assertEquals(0, scan2.gestationalAgeDays)

        // Verify bidirectional visit <-> scan links
        linkedScans.forEach { scan ->
            val visit = demo002.visits.find { it.id == scan.linkedVisitId }
            assertNotNull("Linked visit must exist", visit)
            assertEquals("Visit must link back to scan", scan.id, visit!!.linkedUltrasoundId)
        }

        // Verify scan image attachments
        val scanWithMultipleImages = demo002.ultrasoundScans.find { it.imageUris.size > 1 }
        assertNotNull("Scan with multiple images should exist", scanWithMultipleImages)
        assertEquals(2, scanWithMultipleImages!!.imageUris.size)

        // 4 Lab investigations
        assertEquals(4, demo002.labInvestigations.size)
        val ogttLab = demo002.labInvestigations.find { it.testCategory == "Biochemistry" }
        assertNotNull("OGTT must be present", ogttLab)
        assertTrue(ogttLab!!.testName.contains("2h Post-Load"))

        val abnormalLab = demo002.labInvestigations.find { it.isAbnormal }
        assertNotNull("Abnormal lab (Hb 10.6) must be flagged", abnormalLab)
        assertEquals("10.6", abnormalLab!!.result)
        assertEquals("Al Borg Medical Labs", abnormalLab.labName)

        // 4 Multi-drug medications
        assertEquals(4, demo002.medications.size)
        val nonCompliantMed = demo002.medications.find { it.isNonCompliant }
        assertNotNull("Non-compliant medication (Calcium) must be present", nonCompliantMed)
        assertEquals("Calcium + Vitamin D3", nonCompliantMed!!.name)
        assertNotNull("Adverse effects notes must be recorded", nonCompliantMed.adverseNotes)
        assertTrue(nonCompliantMed.adverseNotes!!.contains("constipation", ignoreCase = true))

        val prophylaxisMed = demo002.medications.find { it.name == "Low-Dose Aspirin" }
        assertNotNull("Aspirin must be present", prophylaxisMed)
        assertTrue("Aspirin is prophylaxis", prophylaxisMed!!.isProphylaxis)
        assertFalse("Aspirin is compliant", prophylaxisMed.isNonCompliant)

        // Gestational age at latest visit date (2026-10-10) with EDD 2026-11-20
        // Days to EDD: 2026-11-20 minus 2026-10-10 = 41 days.
        // 280 - 41 = 239 days = 34w 1d.
        val gaAtLatestVisit = dating.calculateGestationalAge(LocalDate.of(2026, 10, 10))
        assertEquals("34w 1d", gaAtLatestVisit)
    }

    @Test
    fun syntheticFixturesContainComprehensiveMultiLossCaseForDemo004() {
        val demo004 = DemoFixtures.patients.firstOrNull { it.caseId == "DEMO-ANC-004" }
        assertNotNull("DEMO-ANC-004 must be present in synthetic fixtures", demo004)
        demo004!!

        assertEquals("فريدة مصطفى أحمد", demo004.displayName)
        assertEquals("High Risk", demo004.riskLevel)
        assertEquals("O-", demo004.bloodGroupText)
        assertEquals(5L, demo004.gravida)
        assertEquals(1L, demo004.parity)
        assertEquals(3L, demo004.abortion)

        // Multiple deaths and losses in previous pregnancies
        assertEquals(4, demo004.previousPregnancies.size)
        val stillbirth = demo004.previousPregnancies.find { it.outcome == "Stillbirth" }
        assertNotNull("Stillbirth / IUFD must be recorded", stillbirth)
        assertTrue(stillbirth!!.outcomeText!!.contains("IUFD", ignoreCase = true))

        val miscarriages = demo004.previousPregnancies.filter { it.outcome == "EarlyPregnancyLoss" }
        assertEquals(2, miscarriages.size)

        // Multiple ultrasound examinations (7 scans)
        assertEquals(7, demo004.ultrasoundScans.size)

        // Multiple antenatal visits (9 visits)
        assertEquals(9, demo004.visits.size)

        // Complete lab & medication profiles
        assertEquals(6, demo004.labInvestigations.size)
        assertEquals(5, demo004.medications.size)
    }
}
