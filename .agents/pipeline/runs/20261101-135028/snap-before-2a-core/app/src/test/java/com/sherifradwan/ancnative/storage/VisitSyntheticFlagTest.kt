package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoVisit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VisitSyntheticFlagTest {
    @Test
    fun withCommittedVisitMarksTheDoctorSVisitAsReal() {
        val patient = demoPatient()
        val command = saveVisitCommand(visitId = "visit-new")

        val committed = patient.withCommittedVisit(command, revision = 1)
        val saved = committed.visits.single { it.id == "visit-new" }

        assertFalse(saved.isSyntheticEdit)
    }

    @Test
    fun withCommittedVisitKeepsEveryCommandFieldIntact() {
        val patient = demoPatient()
        val command = saveVisitCommand(
            visitId = "visit-new",
            bloodPressure = "118/76",
            weight = "63",
            findings = "Antenatal course progressing favorably",
            notes = "Iron continued",
        )

        val committed = patient.withCommittedVisit(command, revision = 2)
        val saved = committed.visits.single { it.id == "visit-new" }

        assertEquals("2026-10-31", saved.date)
        assertEquals("118/76", saved.bloodPressure)
        assertEquals("63", saved.weight)
        assertEquals("Antenatal course progressing favorably", saved.findings)
        assertEquals("Iron continued", saved.notes)
        assertEquals(2, saved.revision)
    }

    @Test
    fun withCommittedVisitKeepsExistingVisitsUnchangedAndSortsByDateDescending() {
        val patient = demoPatient()
        val command = saveVisitCommand(visitId = "visit-new", date = "2026-10-31")

        val committed = patient.withCommittedVisit(command, revision = 1)

        assertEquals(listOf("visit-new", "visit-old"), committed.visits.map { it.id })
        assertEquals(patient.visits.single(), committed.visits.single { it.id == "visit-old" })
    }

    private fun demoPatient() = DemoPatient(
        id = "00000000-0000-4000-8000-0000000000a1",
        caseId = "CASE-1",
        displayName = "Test Patient",
        dateOfBirth = "1996-05-05",
        phone = null,
        visits = listOf(
            DemoVisit(
                id = "visit-old",
                date = "2026-10-01",
                bloodPressure = "120/80",
                weight = "70",
                findings = "Fine",
                notes = "Review next month",
                revision = 3,
                isSyntheticEdit = true,
            ),
        ),
    )

    private fun saveVisitCommand(
        visitId: String,
        date: String = "2026-10-31",
        bloodPressure: String? = null,
        weight: String? = null,
        findings: String? = null,
        notes: String? = null,
    ): SaveVisitCommand = SaveVisitCommand(
        operationUuid = "00000000-0000-4000-8000-0000000000a2",
        editorSessionId = "unit-editor",
        patientId = "00000000-0000-4000-8000-0000000000a1",
        visitId = visitId,
        expectedRevision = null,
        date = date,
        bloodPressure = bloodPressure,
        weight = weight,
        findings = findings,
        notes = notes,
        fingerprint = "",
    )
}
