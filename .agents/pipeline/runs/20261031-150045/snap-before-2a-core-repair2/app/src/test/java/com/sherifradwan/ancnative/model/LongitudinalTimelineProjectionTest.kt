package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LongitudinalTimelineProjectionTest {

    private fun createVisit(
        id: String,
        date: String,
        linkedUltrasoundId: String? = null,
    ): DemoVisit {
        return DemoVisit(
            id = id,
            date = date,
            bloodPressure = "120/80",
            weight = "65",
            findings = "Normal findings",
            notes = "Routine ANC check",
            revision = 1,
            isSyntheticEdit = false,
            linkedUltrasoundId = linkedUltrasoundId,
        )
    }

    private fun createBasePatient(
        id: String = "P1",
        visits: List<DemoVisit> = emptyList(),
        scans: List<DemoUltrasoundScan> = emptyList(),
    ): DemoPatient {
        return DemoPatient(
            id = id,
            caseId = "CASE-001",
            displayName = "Test Patient",
            dateOfBirth = "1995-01-01",
            phone = "01000000000",
            status = "active",
            dating = null,
            previousPregnancies = emptyList(),
            visits = visits,
            ultrasoundScans = scans,
            labInvestigations = emptyList(),
            medications = emptyList(),
        )
    }

    @Test
    fun testLinkedScanDoesNotProduceDuplicateTimelineNode() {
        val scan = DemoUltrasoundScan(
            id = "US-1",
            patientId = "P1",
            date = "2026-09-15",
            gestationalAgeWeeks = 24,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Posterior",
            amnioticFluid = "Normal",
            fetalHeartRate = "144",
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = "700",
            impression = "Normal anatomy",
            linkedVisitId = "V-1",
        )
        val visit = createVisit(id = "V-1", date = "2026-09-15", linkedUltrasoundId = "US-1")
        val patient = createBasePatient(visits = listOf(visit), scans = listOf(scan))

        val timeline = LongitudinalTimelineProjection.build(patient)

        // Exactly 1 node produced (the visit node), with nested scan attached
        assertEquals(1, timeline.size)
        val event = timeline[0]
        assertEquals(TimelineEventKind.VISIT, event.kind)
        assertEquals("visit:V-1", event.eventKey)
        assertNotNull(event.visit)
        assertNotNull(event.linkedScan)
        assertEquals("US-1", event.linkedScan!!.id)
        assertNull(event.linkIntegrityWarning)
    }

    @Test
    fun testStandaloneScanProducesIndependentTimelineNode() {
        val visit = createVisit(id = "V-1", date = "2026-10-01")
        val scan = DemoUltrasoundScan(
            id = "US-STANDALONE",
            patientId = "P1",
            date = "2026-09-01",
            gestationalAgeWeeks = 20,
            gestationalAgeDays = 0,
            fetalNumber = "Singleton",
            presentation = null,
            placentalSite = null,
            amnioticFluid = null,
            fetalHeartRate = null,
            biometryBpd = null,
            biometryHc = null,
            biometryAc = null,
            biometryFl = null,
            estimatedFetalWeight = null,
            impression = "Anomaly scan",
            linkedVisitId = null,
        )
        val patient = createBasePatient(visits = listOf(visit), scans = listOf(scan))

        val timeline = LongitudinalTimelineProjection.build(patient)

        // Exactly 2 nodes produced
        assertEquals(2, timeline.size)
        assertEquals("visit:V-1", timeline[0].eventKey)
        assertEquals(TimelineEventKind.VISIT, timeline[0].kind)
        assertEquals("scan:US-STANDALONE", timeline[1].eventKey)
        assertEquals(TimelineEventKind.STANDALONE_ULTRASOUND, timeline[1].kind)
    }

    @Test
    fun testChronologicalOrderingNewestFirst() {
        val v1 = createVisit(id = "V-OLD", date = "2026-01-10")
        val v2 = createVisit(id = "V-NEW", date = "2026-08-20")
        val v3 = createVisit(id = "V-MID", date = "2026-04-15")
        val patient = createBasePatient(visits = listOf(v1, v2, v3))

        val timeline = LongitudinalTimelineProjection.build(patient)

        assertEquals("visit:V-NEW", timeline[0].eventKey)
        assertEquals("visit:V-MID", timeline[1].eventKey)
        assertEquals("visit:V-OLD", timeline[2].eventKey)
    }

    @Test
    fun testUndatedAndInvalidDatesSortedLastWithDeterministicTieBreak() {
        val vValid = createVisit(id = "V-1", date = "2026-05-01")
        val vUndated = createVisit(id = "V-B", date = "")
        val vInvalid = createVisit(id = "V-A", date = "invalid-date")
        val patient = createBasePatient(visits = listOf(vUndated, vValid, vInvalid))

        val timeline = LongitudinalTimelineProjection.build(patient)

        assertEquals(3, timeline.size)
        // Valid date first
        assertEquals("visit:V-1", timeline[0].eventKey)
        // Undated/invalid sorted last, tie broken by eventKey alphabetically
        assertEquals("visit:V-A", timeline[1].eventKey)
        assertEquals("visit:V-B", timeline[2].eventKey)
    }

    @Test
    fun testConflictDetectionWhenMultipleVisitsReferenceSameScan() {
        val scan = DemoUltrasoundScan(
            id = "US-SHARED",
            patientId = "P1",
            date = "2026-05-10",
            gestationalAgeWeeks = null,
            gestationalAgeDays = null,
            fetalNumber = null,
            presentation = null,
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
        )
        val v1 = createVisit(id = "V-1", date = "2026-05-10", linkedUltrasoundId = "US-SHARED")
        val v2 = createVisit(id = "V-2", date = "2026-05-11", linkedUltrasoundId = "US-SHARED")
        val patient = createBasePatient(visits = listOf(v1, v2), scans = listOf(scan))

        val timeline = LongitudinalTimelineProjection.build(patient)

        assertEquals(2, timeline.size)
        assertTrue(timeline[0].linkIntegrityWarning?.contains("Conflict") == true)
        assertTrue(timeline[1].linkIntegrityWarning?.contains("Conflict") == true)
    }

    @Test
    fun testPatientOwnershipCheckIgnoresForeignScans() {
        val foreignScan = DemoUltrasoundScan(
            id = "US-FOREIGN",
            patientId = "P_OTHER_PATIENT",
            date = "2026-05-10",
            gestationalAgeWeeks = null,
            gestationalAgeDays = null,
            fetalNumber = null,
            presentation = null,
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
        )
        val visit = createVisit(id = "V-1", date = "2026-05-10", linkedUltrasoundId = "US-FOREIGN")
        val patient = createBasePatient(id = "P1", visits = listOf(visit), scans = listOf(foreignScan))

        val timeline = LongitudinalTimelineProjection.build(patient)

        // Foreign scan is not embedded and is flagged as not found
        assertEquals(1, timeline.size)
        assertNull(timeline[0].linkedScan)
        assertTrue(timeline[0].linkIntegrityWarning?.contains("not found") == true)
    }
}
