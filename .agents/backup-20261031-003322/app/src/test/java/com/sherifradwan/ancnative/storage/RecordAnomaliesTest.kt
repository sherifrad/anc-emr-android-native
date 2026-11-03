package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.PatientVocabulary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordAnomaliesTest {
    private fun patient(
        id: String = "P1",
        revision: Long = 1,
        visits: List<DemoVisit> = emptyList(),
        pregnancies: List<DemoPreviousPregnancy> = emptyList(),
        status: String? = null,
        riskLevel: String? = null,
        bloodGroupText: String? = null,
        gravida: Long? = null,
        parity: Long? = null,
        abortion: Long? = null,
    ) = DemoPatient(
        id = id,
        caseId = "C-" + id,
        displayName = "Name " + id,
        dateOfBirth = null,
        phone = null,
        visits = visits,
        status = status,
        riskLevel = riskLevel,
        bloodGroupText = bloodGroupText,
        gravida = gravida,
        parity = parity,
        abortion = abortion,
        revision = revision,
        previousPregnancies = pregnancies,
    )

    private fun visit(revision: Int = 1) = DemoVisit(
        id = "V1", date = "2026-01-01", bloodPressure = null, weight = null,
        findings = null, notes = null, revision = revision, isSyntheticEdit = false,
    )

    private fun pregnancy(
        patientId: String = "P1",
        modelVersion: Int = 3,
        revision: Long = 1,
        provenanceKind: String = PatientVocabulary.CLINICIAN_ENTRY,
        clinicianId: String = "doc",
    ) = DemoPreviousPregnancy(
        id = "PP1", patientId = patientId, modelVersion = modelVersion,
        dateText = null, outcomeText = null, pregnancyType = null, otherDetails = null,
        placeAndDoctor = null, deliveryBy = null, outcome = null, birthMethod = null,
        lossManagement = null, terminationMethod = null, provenanceKind = provenanceKind,
        clinicianId = clinicianId, recordedAt = "2026-01-01", updatedAt = "2026-01-01",
        revision = revision,
    )

    private fun snap(vararg p: DemoPatient) = RecordSnapshot(p.toList())

    // --- blocking rules, one each ---
    @Test fun patientRevisionZeroBlocks() {
        val r = findBlockingRecordProblems(snap(patient(revision = 0)))
        assertEquals(1, r.size)
        assertTrue(r[0].contains("P1"))
    }

    @Test fun visitRevisionZeroBlocks() {
        val r = findBlockingRecordProblems(snap(patient(visits = listOf(visit(revision = 0)))))
        assertEquals(1, r.size)
        assertTrue(r[0].contains("P1"))
    }

    @Test fun pregnancyRevisionZeroBlocks() {
        val r = findBlockingRecordProblems(snap(patient(pregnancies = listOf(pregnancy(revision = 0)))))
        assertEquals(1, r.size)
        assertTrue(r[0].contains("P1"))
    }

    @Test fun pregnancyForOtherPatientBlocks() {
        val r = findBlockingRecordProblems(snap(patient(pregnancies = listOf(pregnancy(patientId = "P2")))))
        assertEquals(1, r.size)
        assertTrue(r[0].contains("P1"))
    }

    @Test fun pregnancyModelVersionNot3Blocks() {
        val r = findBlockingRecordProblems(snap(patient(pregnancies = listOf(pregnancy(modelVersion = 2)))))
        assertEquals(1, r.size)
        assertTrue(r[0].contains("P1"))
    }

    @Test fun oddValuesNeverBlock() {
        val p = patient(
            status = "weird", riskLevel = "Not high risk", bloodGroupText = "O neg",
            gravida = 2, parity = null, abortion = 0,
            pregnancies = listOf(pregnancy(provenanceKind = "IMPORT", clinicianId = " ")),
        )
        assertTrue(findBlockingRecordProblems(snap(p)).isEmpty())
    }

    // --- anomalies, one each ---
    @Test fun bloodGroupAnomaly() {
        val s = snap(patient(bloodGroupText = "O neg"))
        assertEquals(listOf(RecordAnomaly("P1", "bloodGroupText", "O neg")), findRecordAnomalies(s))
        assertTrue(findBlockingRecordProblems(s).isEmpty())
    }

    @Test fun riskLevelAnomaly() {
        assertEquals(
            listOf(RecordAnomaly("P1", "riskLevel", "Not high risk")),
            findRecordAnomalies(snap(patient(riskLevel = "Not high risk"))),
        )
    }

    @Test fun statusAnomaly() {
        assertEquals(
            listOf(RecordAnomaly("P1", "status", "deleted")),
            findRecordAnomalies(snap(patient(status = "deleted"))),
        )
    }

    @Test fun partialGpaAnomaly() {
        assertEquals(
            listOf(RecordAnomaly("P1", "gpa", "2/null/0")),
            findRecordAnomalies(snap(patient(gravida = 2, parity = null, abortion = 0))),
        )
    }

    @Test fun gpaOutOfRangeAnomaly() {
        assertEquals(
            listOf(RecordAnomaly("P1", "gpa", "-1/0/0")),
            findRecordAnomalies(snap(patient(gravida = -1, parity = 0, abortion = 0))),
        )
        val big = PatientVocabulary.MAX_SAFE_GPA + 1
        assertEquals(
            listOf(RecordAnomaly("P1", "gpa", "1/" + big + "/0")),
            findRecordAnomalies(snap(patient(gravida = 1, parity = big, abortion = 0))),
        )
    }

    @Test fun gpaBoundariesAreFine() {
        val max = PatientVocabulary.MAX_SAFE_GPA
        assertTrue(findRecordAnomalies(snap(patient(gravida = 0, parity = 0, abortion = 0))).isEmpty())
        assertTrue(findRecordAnomalies(snap(patient(gravida = max, parity = max, abortion = max))).isEmpty())
    }

    @Test fun provenanceAnomaly() {
        assertEquals(
            listOf(RecordAnomaly("P1", "previousPregnancy.provenanceKind", "IMPORT")),
            findRecordAnomalies(snap(patient(pregnancies = listOf(pregnancy(provenanceKind = "IMPORT"))))),
        )
    }

    @Test fun blankClinicianAnomaly() {
        val r = findRecordAnomalies(snap(patient(pregnancies = listOf(pregnancy(clinicianId = "  ")))))
        assertEquals(1, r.size)
        assertEquals("P1", r[0].patientId)
        assertEquals("previousPregnancy.clinicianId", r[0].field)
    }

    @Test fun nullsAreNeverAnomalies() {
        assertTrue(findRecordAnomalies(snap(patient())).isEmpty())
    }

    @Test fun validValuesAreNotAnomalies() {
        val p = patient(
            status = "active", riskLevel = "High Risk", bloodGroupText = "AB-",
            gravida = 3, parity = 1, abortion = 1,
        )
        assertTrue(findRecordAnomalies(snap(p)).isEmpty())
    }

    @Test fun orderIsPatientsThenFieldsAsListed() {
        val a = patient(
            id = "P1", status = "x", riskLevel = "y", bloodGroupText = "z",
            gravida = 1, parity = null, abortion = null,
            pregnancies = listOf(pregnancy(provenanceKind = "IMPORT", clinicianId = "")),
        )
        val b = patient(id = "P2", status = "q")
        val r = findRecordAnomalies(snap(a, b))
        assertEquals(
            listOf(
                "P1:status", "P1:riskLevel", "P1:bloodGroupText", "P1:gpa",
                "P1:previousPregnancy.provenanceKind", "P1:previousPregnancy.clinicianId",
                "P2:status",
            ),
            r.map { it.patientId + ":" + it.field },
        )
    }

    @Test fun demoFixturesAreClean() {
        val s = RecordSnapshot(DemoFixtures.patients)
        assertTrue(findBlockingRecordProblems(s).isEmpty())
        assertTrue(findRecordAnomalies(s).isEmpty())
    }
}
