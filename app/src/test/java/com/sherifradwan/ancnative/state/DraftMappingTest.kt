package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.storage.DraftEditorKind
import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.DraftScreenKind
import com.sherifradwan.ancnative.storage.NEW_PATIENT_DRAFT_KEY
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.storage.decodeDraftFields
import com.sherifradwan.ancnative.storage.encodeDraftFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftMappingTest {
    private val unlocked = VaultState.Unlocked(1L)
    private fun base() = AncState(patients = emptyList(), vaultState = unlocked)

    private fun visit(values: DraftValues, session: String = "s1") = base().copy(
        draft = VisitDraft(session, "p1", "v1", 3, DraftValues(), values),
    )

    private fun newVisit(values: DraftValues) = base().copy(
        draft = VisitDraft("s1", "p1", null, null, DraftValues(), values),
    )

    private fun patient(values: PatientDraftValues, revision: Long? = 4L, id: String = "p1", session: String = "s1") =
        base().copy(patientDraft = PatientDraft(session, id, revision, null, PatientDraftValues(), values))

    private fun histories(values: HistoriesDraftValues, session: String = "s1") = base().copy(
        historiesDraft = HistoriesDraft(session, "p1", 5L, HistoriesDraftValues(), values),
    )

    private fun pregnancy(values: PreviousPregnancyDraftValues, session: String = "s1") = base().copy(
        pregnancyDraft = PreviousPregnancyDraft(session, "p1", "pr1", 5L, 7L, null, PreviousPregnancyDraftValues(), values),
    )

    private fun status(reason: String, session: String = "s1") = base().copy(
        statusDraft = PatientStatusDraft(session, "p1", 6L, "archived", reason),
    )

    private fun dating(values: DatingDraftValues, session: String = "s1") = base().copy(
        datingDraft = DatingDraft(session, "p1", 8L, DatingDraftValues(), values),
    )

    private fun ultrasound(values: UltrasoundDraftValues, session: String = "s1") = base().copy(
        ultrasoundDraft = UltrasoundDraft(session, "p1", "u1", 9L, 2L, UltrasoundDraftValues(), values),
    )

    private fun lab(values: LabDraftValues, session: String = "s1") = base().copy(
        labDraft = LabDraft(session, "p1", "l1", 9L, 3L, LabDraftValues(), values),
    )

    private fun medication(values: MedicationDraftValues, session: String = "s1") = base().copy(
        medicationDraft = MedicationDraft(session, "p1", "m1", 9L, 4L, MedicationDraftValues(), values),
    )

    private val visitValues = DraftValues("2026-10-01", "120/80", "70", "findings", "notes", "u9")
    private val patientValues = PatientDraftValues(displayName = "Mona", phone = "0100", gravida = "2")
    private val historiesValues = HistoriesDraftValues("m", "s", "f", "a")
    private val pregnancyValues = PreviousPregnancyDraftValues(dateText = "2020", outcome = "live", clinicianId = "c1")
    private val datingValues = DatingDraftValues(lmpDate = "2026-01-01", agreedEdd = "2026-10-08", notes = "n")
    private val ultrasoundValues = UltrasoundDraftValues(date = "2026-05-01", gaWeeks = "20", impression = "ok", imageUris = listOf("file:///a.jpg"))
    private val labValues = LabDraftValues(date = "2026-05-01", testName = "Hb", result = "11", isAbnormal = true)
    private val medicationValues = MedicationDraftValues(name = "Iron", dosage = "1", isProphylaxis = true, isNonCompliant = true)

    private fun record(state: AncState) = draftRecordFor(state, 1234L)

    @Test
    fun noEditorGivesNoSlotAndNoRecord() {
        assertNull(activeDraftSlot(base()))
        assertNull(record(base()))
    }

    @Test
    fun untouchedEditorsGiveNoRecord() {
        val clean = listOf(
            base().copy(draft = VisitDraft("s", "p1", "v1", 3, DraftValues(), DraftValues())),
            base().copy(patientDraft = PatientDraft("s", "p1", 4L, null, PatientDraftValues(), PatientDraftValues())),
            histories(HistoriesDraftValues()),
            pregnancy(PreviousPregnancyDraftValues()),
            status(""),
            dating(DatingDraftValues()),
            ultrasound(UltrasoundDraftValues()),
            lab(LabDraftValues()),
            medication(MedicationDraftValues()),
        )
        clean.forEach { state ->
            assertNotNull(activeDraftSlot(state))
            assertNull(record(state))
        }
    }

    @Test
    fun visitRecordHasSlotKindIdsAndFields() {
        val state = visit(visitValues)
        val r = record(state)!!
        assertEquals(DraftSlot("p1", DraftScreenKind.VISIT), activeDraftSlot(state))
        assertEquals("p1", r.patientId)
        assertEquals(DraftScreenKind.VISIT, r.screenKind)
        assertEquals(DraftEditorKind.EDIT, r.editorKind)
        assertEquals("v1", r.recordId)
        assertEquals(3L, r.baseRevision)
        assertEquals(1234L, r.updatedAtMillis)
        assertEquals(
            mapOf(
                "date" to "2026-10-01", "bloodPressure" to "120/80",
                "systolic" to "", "diastolic" to "",
                "weight" to "70", "fundalHeight" to "",
                "oedema" to "", "fetalMovements" to "", "urineProtein" to "",
                "findings" to "findings", "notes" to "notes", "linkedUltrasoundId" to "u9",
            ),
            r.fields,
        )
    }

    @Test
    fun visitWithoutIdIsCreate() {
        val r = record(newVisit(visitValues))!!
        assertEquals(DraftEditorKind.CREATE, r.editorKind)
        assertNull(r.recordId)
        assertNull(r.baseRevision)
    }

    @Test
    fun patientProfileEditAndNew() {
        val edit = record(patient(patientValues))!!
        assertEquals(DraftScreenKind.PATIENT_PROFILE, edit.screenKind)
        assertEquals(DraftEditorKind.EDIT, edit.editorKind)
        assertEquals("p1", edit.patientId)
        assertNull(edit.recordId)
        assertEquals(4L, edit.baseRevision)
        assertEquals("Mona", edit.fields["displayName"])
        assertEquals("", edit.fields["caseId"])
        assertEquals(13, edit.fields.size)
        assertEquals(13, record(patient(patientValues, revision = null, id = "generated-id"))!!.fields.size)

        val fresh = patient(patientValues, revision = null, id = "generated-id")
        assertEquals(DraftSlot(NEW_PATIENT_DRAFT_KEY, DraftScreenKind.PATIENT_PROFILE), activeDraftSlot(fresh))
        val created = record(fresh)!!
        assertEquals(NEW_PATIENT_DRAFT_KEY, created.patientId)
        assertEquals(DraftEditorKind.CREATE, created.editorKind)
        assertNull(created.baseRevision)
    }

    @Test
    fun oldPatientDraftWithRemovedDatingKeysStillLoads() {
        val removedKeys = mapOf(
            "lmpDate" to "2026-01-01",
            "datingMethod" to "Ultrasound",
            "transferDate" to "2026-03-01",
            "embryoAgeDays" to "5",
            "agreedEdd" to "2026-11-20",
            "scanDate" to "2026-03-01",
            "scanGaWeeks" to "8",
            "scanGaDays" to "3",
        )
        val full = PatientDraftValues(
            caseId = "c1", displayName = "Mona", dateOfBirth = "1990-01-01", phone = "0100", ageText = "31",
            addressText = "Cairo", bloodGroupText = "A+", riskLevel = "Moderate Risk", gravida = "2", parity = "1",
            abortion = "0", obstetricNotes = "n", clinicalAttention = "Watch BP",
        )
        for ((revision, id) in listOf<Pair<Long?, String>>(4L to "p1", null to "generated-id")) {
            val typed = patient(full, revision = revision, id = id)
            val current = record(typed)!!
            assertEquals(13, current.fields.size)
            val decoded = decodeDraftFields(encodeDraftFields(current.fields + removedKeys)!!)!!
            assertTrue(decoded.keys.containsAll(removedKeys.keys))
            val restored = applyDraftRecord(freshOf(typed), current.copy(fields = decoded)).patientDraft!!.values
            assertEquals(full, restored)
            removedKeys.keys.forEach { assertFalse("$it must not be saved again", it in record(patient(restored, revision, id))!!.fields) }
        }
    }

    @Test
    fun historiesRecord() {
        val r = record(histories(historiesValues))!!
        assertEquals(DraftScreenKind.HISTORIES, r.screenKind)
        assertEquals(DraftEditorKind.EDIT, r.editorKind)
        assertNull(r.recordId)
        assertEquals(5L, r.baseRevision)
        assertEquals(
            mapOf("medicalHistory" to "m", "surgicalHistory" to "s", "familyHistory" to "f", "allergyHistory" to "a"),
            r.fields,
        )
    }

    @Test
    fun previousPregnancyRecord() {
        val r = record(pregnancy(pregnancyValues))!!
        assertEquals(DraftScreenKind.PREVIOUS_PREGNANCY, r.screenKind)
        assertEquals("pr1", r.recordId)
        assertEquals(DraftEditorKind.EDIT, r.editorKind)
        assertEquals(7L, r.baseRevision)
        assertEquals("2020", r.fields["dateText"])
        assertEquals("live", r.fields["outcome"])
        assertEquals("c1", r.fields["clinicianId"])
        assertEquals(11, r.fields.size)
        val created = record(
            base().copy(
                pregnancyDraft = PreviousPregnancyDraft("s", "p1", "pr2", 5L, null, null, PreviousPregnancyDraftValues(), pregnancyValues),
            ),
        )!!
        assertEquals(DraftEditorKind.CREATE, created.editorKind)
        assertNull(created.baseRevision)
    }

    @Test
    fun patientStatusStoresTargetAndReason() {
        val r = record(status("closed case"))!!
        assertEquals(DraftScreenKind.PATIENT_STATUS, r.screenKind)
        assertNull(r.recordId)
        assertEquals(6L, r.baseRevision)
        assertEquals("archived", r.fields["targetStatus"])
        assertEquals("closed case", r.fields["reason"])
    }

    @Test
    fun datingRecord() {
        val r = record(dating(datingValues))!!
        assertEquals(DraftScreenKind.DATING, r.screenKind)
        assertNull(r.recordId)
        assertEquals(8L, r.baseRevision)
        assertEquals("2026-10-08", r.fields["agreedEdd"])
        assertEquals(7, r.fields.size)
    }

    @Test
    fun ultrasoundRecordNeverStoresImageUris() {
        val r = record(ultrasound(ultrasoundValues))!!
        assertEquals(DraftScreenKind.ULTRASOUND, r.screenKind)
        assertEquals("u1", r.recordId)
        assertEquals(2L, r.baseRevision)
        assertEquals("20", r.fields["gaWeeks"])
        assertFalse(r.fields.containsKey("imageUris"))
        assertFalse(r.fields.values.any { it.contains("a.jpg") })
        assertEquals(15, r.fields.size)
    }

    @Test
    fun labRecordStoresBooleanAsText() {
        val r = record(lab(labValues))!!
        assertEquals(DraftScreenKind.LAB, r.screenKind)
        assertEquals("l1", r.recordId)
        assertEquals(3L, r.baseRevision)
        assertEquals("true", r.fields["isAbnormal"])
        assertEquals("Hb", r.fields["testName"])
        assertEquals(9, r.fields.size)
    }

    @Test
    fun medicationRecordStoresBooleansAsText() {
        val r = record(medication(medicationValues))!!
        assertEquals(DraftScreenKind.MEDICATION, r.screenKind)
        assertEquals("m1", r.recordId)
        assertEquals(4L, r.baseRevision)
        assertEquals("true", r.fields["isProphylaxis"])
        assertEquals("true", r.fields["isNonCompliant"])
        assertEquals("Iron", r.fields["name"])
        assertEquals(11, r.fields.size)
    }

    @Test
    fun roundTripOntoFreshCleanEditor() {
        val typed = listOf(
            visit(visitValues), patient(patientValues), histories(historiesValues), pregnancy(pregnancyValues),
            status("why"), dating(datingValues), ultrasound(ultrasoundValues), lab(labValues), medication(medicationValues),
        )
        typed.forEach { state ->
            val r = record(state)!!
            val fresh = freshOf(state)
            val restored = applyDraftRecord(fresh, r)
            assertEquals(r.fields, record(restored)!!.fields)
            assertEquals(activeDraftSlot(state), activeDraftSlot(restored))
            assertTrue(restored.hasDirtyDraft)
            val after = sessionOf(restored)
            assertNotEquals(sessionOf(fresh), after)
            assertNotEquals(sessionOf(state), after)
            assertNoErrors(restored)
        }
    }

    @Test
    fun applyKeepsFreshEditorOriginalIdsAndImageUris() {
        val r = record(ultrasound(ultrasoundValues))!!
        val clean = UltrasoundDraftValues(imageUris = listOf("file:///fresh.jpg"))
        val fresh = base().copy(
            ultrasoundDraft = UltrasoundDraft("fresh", "p1", "u1", 9L, 2L, clean, clean),
        )
        val d = applyDraftRecord(fresh, r).ultrasoundDraft!!
        assertEquals(listOf("file:///fresh.jpg"), d.values.imageUris)
        assertEquals(clean, d.original)
        assertEquals("u1", d.scanId)
        assertEquals(2L, d.baseScanRevision)
        assertEquals("ok", d.values.impression)
    }

    @Test
    fun applyClearsErrorsAndKeepsMissingFields() {
        val kept = DraftValues(notes = "keep")
        val withErrors = base().copy(
            draft = VisitDraft("fresh", "p1", "v1", 3, kept, kept, setOf(DraftError.entries.first())),
        )
        val r = DraftRecord("p1", DraftScreenKind.VISIT, DraftEditorKind.EDIT, "v1", 3L, mapOf("weight" to "81", "unknownField" to "x"), 1L)
        val d = applyDraftRecord(withErrors, r).draft!!
        assertEquals("81", d.values.weight)
        assertEquals("keep", d.values.notes)
        assertTrue(d.errors.isEmpty())
    }

    @Test
    fun nonMatchingSlotLeavesStateEqual() {
        val state = visit(DraftValues())
        val otherKind = DraftRecord("p1", DraftScreenKind.LAB, DraftEditorKind.EDIT, null, null, mapOf("result" to "1"), 1L)
        val otherPatient = DraftRecord("p2", DraftScreenKind.VISIT, DraftEditorKind.EDIT, "v1", 3L, mapOf("weight" to "1"), 1L)
        assertEquals(state, applyDraftRecord(state, otherKind))
        assertEquals(state, applyDraftRecord(state, otherPatient))
        assertEquals(base(), applyDraftRecord(base(), otherKind))
    }

    @Test
    fun arabicAndEmojiSurviveMappingAndCodec() {
        val text = "ألم في البطن 🤰 مع نزيف \"خفيف\"\nسطر ثان"
        val typed = visit(DraftValues(notes = text))
        val r = record(typed)!!
        assertEquals(text, r.fields["notes"])
        val decoded = decodeDraftFields(encodeDraftFields(r.fields)!!)!!
        assertEquals(text, decoded["notes"])
        val restored = applyDraftRecord(freshOf(typed), r.copy(fields = decoded))
        assertEquals(text, restored.draft!!.values.notes)
    }

    @Test
    fun cleanOpenThenCloseGivesNull() {
        assertNull(closedDraftSlot(base(), visit(DraftValues())))
        assertNull(closedDraftSlot(visit(DraftValues()), base()))
        assertNull(closedDraftSlot(patient(PatientDraftValues()), base()))
        assertNull(closedDraftSlot(histories(HistoriesDraftValues()), base()))
        assertNull(closedDraftSlot(pregnancy(PreviousPregnancyDraftValues()), base()))
        assertNull(closedDraftSlot(status(""), base()))
        assertNull(closedDraftSlot(dating(DatingDraftValues()), base()))
        assertNull(closedDraftSlot(ultrasound(UltrasoundDraftValues()), base()))
        assertNull(closedDraftSlot(lab(LabDraftValues()), base()))
        assertNull(closedDraftSlot(medication(MedicationDraftValues()), base()))
    }

    @Test
    fun dirtyThenCloseGivesSlotForAllNineKinds() {
        assertEquals(DraftSlot("p1", DraftScreenKind.VISIT), closedDraftSlot(visit(visitValues), base()))
        assertEquals(DraftSlot("p1", DraftScreenKind.PATIENT_PROFILE), closedDraftSlot(patient(patientValues), base()))
        assertEquals(DraftSlot("p1", DraftScreenKind.HISTORIES), closedDraftSlot(histories(historiesValues), base()))
        assertEquals(DraftSlot("p1", DraftScreenKind.PREVIOUS_PREGNANCY), closedDraftSlot(pregnancy(pregnancyValues), base()))
        assertEquals(DraftSlot("p1", DraftScreenKind.PATIENT_STATUS), closedDraftSlot(status("relocated"), base()))
        assertEquals(DraftSlot("p1", DraftScreenKind.DATING), closedDraftSlot(dating(datingValues), base()))
        assertEquals(DraftSlot("p1", DraftScreenKind.ULTRASOUND), closedDraftSlot(ultrasound(ultrasoundValues), base()))
        assertEquals(DraftSlot("p1", DraftScreenKind.LAB), closedDraftSlot(lab(labValues), base()))
        assertEquals(DraftSlot("p1", DraftScreenKind.MEDICATION), closedDraftSlot(medication(medicationValues), base()))
    }

    @Test
    fun newPatientSlotClosedGivesNewPatientDraftKey() {
        val newPat = patient(patientValues, revision = null, id = "temp")
        assertEquals(DraftSlot(NEW_PATIENT_DRAFT_KEY, DraftScreenKind.PATIENT_PROFILE), closedDraftSlot(newPat, base()))
    }

    @Test
    fun dirtyThenCleanGivesSlot() {
        assertEquals(DraftSlot("p1", DraftScreenKind.LAB), closedDraftSlot(lab(labValues), lab(LabDraftValues())))
        assertEquals(DraftSlot("p1", DraftScreenKind.VISIT), closedDraftSlot(visit(visitValues), visit(DraftValues())))
    }

    @Test
    fun chainedSaveAndAddAnotherLabAndMedication() {
        val dirtyLab = lab(labValues)
        val freshCleanLab = lab(LabDraftValues(), session = "s_lab2")
        assertEquals(DraftSlot("p1", DraftScreenKind.LAB), closedDraftSlot(dirtyLab, freshCleanLab))

        val dirtyMed = medication(medicationValues)
        val freshCleanMed = medication(MedicationDraftValues(), session = "s_med2")
        assertEquals(DraftSlot("p1", DraftScreenKind.MEDICATION), closedDraftSlot(dirtyMed, freshCleanMed))
    }

    @Test
    fun noClosedSlotWhileSameEditorStaysDirty() {
        val before = visit(visitValues)
        assertNull(closedDraftSlot(before, visit(visitValues.copy(notes = "more"))))
        assertNull(closedDraftSlot(before, before))
        assertNull(closedDraftSlot(base(), before))
    }

    @Test
    fun lockOrAnyVaultChangeIsNotADiscard() {
        val before = visit(visitValues)
        assertNull(closedDraftSlot(before, base().copy(vaultState = VaultState.Locked)))
        assertNull(closedDraftSlot(before, base().copy(vaultState = VaultState.Closing())))
        assertNull(closedDraftSlot(before.copy(vaultState = VaultState.Locked), base()))
    }

    /** A freshly opened, clean editor of the same screen and record as [typed], with a new session id. */
    private fun freshOf(typed: AncState): AncState = typed.copy(
        draft = typed.draft?.let { it.copy(editorSessionId = "fresh", values = it.original) },
        patientDraft = typed.patientDraft?.let { it.copy(editorSessionId = "fresh", values = it.original) },
        historiesDraft = typed.historiesDraft?.let { it.copy(editorSessionId = "fresh", values = it.original) },
        pregnancyDraft = typed.pregnancyDraft?.let { it.copy(editorSessionId = "fresh", values = it.original) },
        statusDraft = typed.statusDraft?.copy(editorSessionId = "fresh", reason = ""),
        datingDraft = typed.datingDraft?.let { it.copy(editorSessionId = "fresh", values = it.original) },
        ultrasoundDraft = typed.ultrasoundDraft?.let { it.copy(editorSessionId = "fresh", values = it.original) },
        labDraft = typed.labDraft?.let { it.copy(editorSessionId = "fresh", values = it.original) },
        medicationDraft = typed.medicationDraft?.let { it.copy(editorSessionId = "fresh", values = it.original) },
    )

    private fun sessionOf(s: AncState): String? =
        s.draft?.editorSessionId ?: s.patientDraft?.editorSessionId ?: s.historiesDraft?.editorSessionId
            ?: s.pregnancyDraft?.editorSessionId ?: s.statusDraft?.editorSessionId ?: s.datingDraft?.editorSessionId
            ?: s.ultrasoundDraft?.editorSessionId ?: s.labDraft?.editorSessionId ?: s.medicationDraft?.editorSessionId

    private fun assertNoErrors(s: AncState) {
        assertTrue(s.draft?.errors.orEmpty().isEmpty())
        assertTrue(s.patientDraft?.errors.orEmpty().isEmpty())
        assertTrue(s.historiesDraft?.errors.orEmpty().isEmpty())
        assertTrue(s.pregnancyDraft?.errors.orEmpty().isEmpty())
        assertTrue(s.statusDraft?.errors.orEmpty().isEmpty())
        assertTrue(s.datingDraft?.errors.orEmpty().isEmpty())
        assertTrue(s.ultrasoundDraft?.errors.orEmpty().isEmpty())
        assertTrue(s.labDraft?.errors.orEmpty().isEmpty())
        assertTrue(s.medicationDraft?.errors.orEmpty().isEmpty())
    }
}
